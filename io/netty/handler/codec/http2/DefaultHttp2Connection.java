package io.netty.handler.codec.http2;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultHttp2Connection
  implements Http2Connection
{
  private final Set<Http2Connection.Listener> listeners = new HashSet(4);
  private final IntObjectMap<Http2Stream> streamMap = new IntObjectHashMap();
  private final ConnectionStream connectionStream = new ConnectionStream();
  private final Set<Http2Stream> activeStreams = new LinkedHashSet();
  private final DefaultEndpoint<Http2LocalFlowController> localEndpoint;
  private final DefaultEndpoint<Http2RemoteFlowController> remoteEndpoint;
  private final Http2StreamRemovalPolicy removalPolicy;
  
  public DefaultHttp2Connection(boolean server)
  {
    this(server, Http2CodecUtil.immediateRemovalPolicy());
  }
  
  public DefaultHttp2Connection(boolean server, Http2StreamRemovalPolicy removalPolicy)
  {
    this.removalPolicy = ((Http2StreamRemovalPolicy)ObjectUtil.checkNotNull(removalPolicy, "removalPolicy"));
    this.localEndpoint = new DefaultEndpoint(server);
    this.remoteEndpoint = new DefaultEndpoint(!server);
    
    removalPolicy.setAction(new Http2StreamRemovalPolicy.Action()
    {
      public void removeStream(Http2Stream stream)
      {
        DefaultHttp2Connection.this.removeStream((DefaultHttp2Connection.DefaultStream)stream);
      }
    });
    this.streamMap.put(this.connectionStream.id(), this.connectionStream);
  }
  
  public void addListener(Http2Connection.Listener listener)
  {
    this.listeners.add(listener);
  }
  
  public void removeListener(Http2Connection.Listener listener)
  {
    this.listeners.remove(listener);
  }
  
  public boolean isServer()
  {
    return this.localEndpoint.isServer();
  }
  
  public Http2Stream connectionStream()
  {
    return this.connectionStream;
  }
  
  public Http2Stream requireStream(int streamId)
    throws Http2Exception
  {
    Http2Stream stream = stream(streamId);
    if (stream == null) {
      throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream does not exist %d", new Object[] { Integer.valueOf(streamId) });
    }
    return stream;
  }
  
  public Http2Stream stream(int streamId)
  {
    return (Http2Stream)this.streamMap.get(streamId);
  }
  
  public int numActiveStreams()
  {
    return this.activeStreams.size();
  }
  
  public Set<Http2Stream> activeStreams()
  {
    return Collections.unmodifiableSet(this.activeStreams);
  }
  
  public void deactivate(Http2Stream stream)
  {
    deactivateInternal((DefaultStream)stream);
  }
  
  public Http2Connection.Endpoint<Http2LocalFlowController> local()
  {
    return this.localEndpoint;
  }
  
  public Http2Connection.Endpoint<Http2RemoteFlowController> remote()
  {
    return this.remoteEndpoint;
  }
  
  public boolean isGoAway()
  {
    return (goAwaySent()) || (goAwayReceived());
  }
  
  public Http2Stream createLocalStream(int streamId)
    throws Http2Exception
  {
    return local().createStream(streamId);
  }
  
  public Http2Stream createRemoteStream(int streamId)
    throws Http2Exception
  {
    return remote().createStream(streamId);
  }
  
  public boolean goAwayReceived()
  {
    return this.localEndpoint.lastKnownStream >= 0;
  }
  
  public void goAwayReceived(int lastKnownStream)
  {
    this.localEndpoint.lastKnownStream(lastKnownStream);
  }
  
  public boolean goAwaySent()
  {
    return this.remoteEndpoint.lastKnownStream >= 0;
  }
  
  public void goAwaySent(int lastKnownStream)
  {
    this.remoteEndpoint.lastKnownStream(lastKnownStream);
  }
  
  private void removeStream(DefaultStream stream)
  {
    for (Http2Connection.Listener listener : this.listeners) {
      listener.streamRemoved(stream);
    }
    this.streamMap.remove(stream.id());
    stream.parent().removeChild(stream);
  }
  
  private void activateInternal(DefaultStream stream)
  {
    if (this.activeStreams.add(stream))
    {
      DefaultEndpoint.access$308(stream.createdBy());
      for (Http2Connection.Listener listener : this.listeners) {
        listener.streamActive(stream);
      }
    }
  }
  
  private void deactivateInternal(DefaultStream stream)
  {
    if (this.activeStreams.remove(stream))
    {
      DefaultEndpoint.access$310(stream.createdBy());
      for (Http2Connection.Listener listener : this.listeners) {
        listener.streamInactive(stream);
      }
      this.removalPolicy.markForRemoval(stream);
    }
  }
  
  private class DefaultStream
    implements Http2Stream
  {
    private final int id;
    private Http2Stream.State state = Http2Stream.State.IDLE;
    private short weight = 16;
    private DefaultStream parent;
    private IntObjectMap<DefaultStream> children = DefaultHttp2Connection.access$400();
    private int totalChildWeights;
    private boolean resetSent;
    private DefaultHttp2Connection.PropertyMap data;
    
    DefaultStream(int id)
    {
      this.id = id;
      this.data = new DefaultHttp2Connection.LazyPropertyMap(this);
    }
    
    public final int id()
    {
      return this.id;
    }
    
    public final Http2Stream.State state()
    {
      return this.state;
    }
    
    public boolean isResetSent()
    {
      return this.resetSent;
    }
    
    public Http2Stream resetSent()
    {
      this.resetSent = true;
      return this;
    }
    
    public Object setProperty(Object key, Object value)
    {
      return this.data.put(key, value);
    }
    
    public <V> V getProperty(Object key)
    {
      return (V)this.data.get(key);
    }
    
    public <V> V removeProperty(Object key)
    {
      return (V)this.data.remove(key);
    }
    
    public final boolean isRoot()
    {
      return this.parent == null;
    }
    
    public final short weight()
    {
      return this.weight;
    }
    
    public final int totalChildWeights()
    {
      return this.totalChildWeights;
    }
    
    public final DefaultStream parent()
    {
      return this.parent;
    }
    
    public final boolean isDescendantOf(Http2Stream stream)
    {
      Http2Stream next = parent();
      while (next != null)
      {
        if (next == stream) {
          return true;
        }
        next = next.parent();
      }
      return false;
    }
    
    public final boolean isLeaf()
    {
      return numChildren() == 0;
    }
    
    public final int numChildren()
    {
      return this.children.size();
    }
    
    public final Collection<? extends Http2Stream> children()
    {
      return this.children.values();
    }
    
    public final boolean hasChild(int streamId)
    {
      return child(streamId) != null;
    }
    
    public final Http2Stream child(int streamId)
    {
      return (Http2Stream)this.children.get(streamId);
    }
    
    public Http2Stream setPriority(int parentStreamId, short weight, boolean exclusive)
      throws Http2Exception
    {
      if ((weight < 1) || (weight > 256)) {
        throw new IllegalArgumentException(String.format("Invalid weight: %d.  Must be between %d and %d (inclusive).", new Object[] { Short.valueOf(weight), Short.valueOf(1), Short.valueOf(256) }));
      }
      DefaultStream newParent = (DefaultStream)DefaultHttp2Connection.this.stream(parentStreamId);
      if (newParent == null) {
        newParent = createdBy().createStream(parentStreamId);
      } else if (this == newParent) {
        throw new IllegalArgumentException("A stream cannot depend on itself");
      }
      weight(weight);
      if ((newParent != parent()) || (exclusive))
      {
        List<DefaultHttp2Connection.ParentChangedEvent> events;
        if (newParent.isDescendantOf(this))
        {
          List<DefaultHttp2Connection.ParentChangedEvent> events = new ArrayList(2 + (exclusive ? newParent.numChildren() : 0));
          this.parent.takeChild(newParent, false, events);
        }
        else
        {
          events = new ArrayList(1 + (exclusive ? newParent.numChildren() : 0));
        }
        newParent.takeChild(this, exclusive, events);
        DefaultHttp2Connection.this.notifyParentChanged(events);
      }
      return this;
    }
    
    public Http2Stream open(boolean halfClosed)
      throws Http2Exception
    {
      switch (DefaultHttp2Connection.2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[this.state.ordinal()])
      {
      case 1: 
        this.state = (halfClosed ? Http2Stream.State.HALF_CLOSED_REMOTE : isLocal() ? Http2Stream.State.HALF_CLOSED_LOCAL : Http2Stream.State.OPEN);
        break;
      case 2: 
        this.state = Http2Stream.State.HALF_CLOSED_REMOTE;
        break;
      case 3: 
        this.state = Http2Stream.State.HALF_CLOSED_LOCAL;
        break;
      default: 
        throw Http2Exception.streamError(this.id, Http2Error.PROTOCOL_ERROR, "Attempting to open a stream in an invalid state: " + this.state, new Object[0]);
      }
      DefaultHttp2Connection.this.activateInternal(this);
      return this;
    }
    
    public Http2Stream close()
    {
      if (this.state == Http2Stream.State.CLOSED) {
        return this;
      }
      this.state = Http2Stream.State.CLOSED;
      DefaultHttp2Connection.this.deactivateInternal(this);
      return this;
    }
    
    public Http2Stream closeLocalSide()
    {
      switch (DefaultHttp2Connection.2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[this.state.ordinal()])
      {
      case 4: 
        this.state = Http2Stream.State.HALF_CLOSED_LOCAL;
        notifyHalfClosed(this);
        break;
      case 5: 
        break;
      default: 
        close();
      }
      return this;
    }
    
    public Http2Stream closeRemoteSide()
    {
      switch (DefaultHttp2Connection.2.$SwitchMap$io$netty$handler$codec$http2$Http2Stream$State[this.state.ordinal()])
      {
      case 4: 
        this.state = Http2Stream.State.HALF_CLOSED_REMOTE;
        notifyHalfClosed(this);
        break;
      case 6: 
        break;
      default: 
        close();
      }
      return this;
    }
    
    private void notifyHalfClosed(Http2Stream stream)
    {
      for (Http2Connection.Listener listener : DefaultHttp2Connection.this.listeners) {
        listener.streamHalfClosed(stream);
      }
    }
    
    public final boolean remoteSideOpen()
    {
      return (this.state == Http2Stream.State.HALF_CLOSED_LOCAL) || (this.state == Http2Stream.State.OPEN) || (this.state == Http2Stream.State.RESERVED_REMOTE);
    }
    
    public final boolean localSideOpen()
    {
      return (this.state == Http2Stream.State.HALF_CLOSED_REMOTE) || (this.state == Http2Stream.State.OPEN) || (this.state == Http2Stream.State.RESERVED_LOCAL);
    }
    
    final DefaultHttp2Connection.DefaultEndpoint<? extends Http2FlowController> createdBy()
    {
      return DefaultHttp2Connection.this.localEndpoint.createdStreamId(this.id) ? DefaultHttp2Connection.this.localEndpoint : DefaultHttp2Connection.this.remoteEndpoint;
    }
    
    final boolean isLocal()
    {
      return DefaultHttp2Connection.this.localEndpoint.createdStreamId(this.id);
    }
    
    final void weight(short weight)
    {
      short oldWeight;
      if (weight != this.weight)
      {
        if (this.parent != null)
        {
          int delta = weight - this.weight;
          this.parent.totalChildWeights += delta;
        }
        oldWeight = this.weight;
        this.weight = weight;
        for (Http2Connection.Listener l : DefaultHttp2Connection.this.listeners) {
          l.onWeightChanged(this, oldWeight);
        }
      }
    }
    
    final IntObjectMap<DefaultStream> removeAllChildren()
    {
      this.totalChildWeights = 0;
      IntObjectMap<DefaultStream> prevChildren = this.children;
      this.children = DefaultHttp2Connection.access$400();
      return prevChildren;
    }
    
    final void takeChild(DefaultStream child, boolean exclusive, List<DefaultHttp2Connection.ParentChangedEvent> events)
    {
      DefaultStream oldParent = child.parent();
      events.add(new DefaultHttp2Connection.ParentChangedEvent(child, oldParent));
      DefaultHttp2Connection.this.notifyParentChanging(child, this);
      child.parent = this;
      if ((exclusive) && (!this.children.isEmpty())) {
        for (DefaultStream grandchild : removeAllChildren().values()) {
          child.takeChild(grandchild, false, events);
        }
      }
      if (this.children.put(child.id(), child) == null) {
        this.totalChildWeights += child.weight();
      }
      if ((oldParent != null) && (oldParent.children.remove(child.id()) != null)) {
        oldParent.totalChildWeights -= child.weight();
      }
    }
    
    final void removeChild(DefaultStream child)
    {
      if (this.children.remove(child.id()) != null)
      {
        List<DefaultHttp2Connection.ParentChangedEvent> events = new ArrayList(1 + child.children.size());
        events.add(new DefaultHttp2Connection.ParentChangedEvent(child, child.parent()));
        DefaultHttp2Connection.this.notifyParentChanging(child, null);
        child.parent = null;
        this.totalChildWeights -= child.weight();
        for (DefaultStream grandchild : child.children.values()) {
          takeChild(grandchild, false, events);
        }
        DefaultHttp2Connection.this.notifyParentChanged(events);
      }
    }
  }
  
  private static abstract interface PropertyMap
  {
    public abstract Object put(Object paramObject1, Object paramObject2);
    
    public abstract <V> V get(Object paramObject);
    
    public abstract <V> V remove(Object paramObject);
  }
  
  private static final class DefaultProperyMap
    implements DefaultHttp2Connection.PropertyMap
  {
    private final Map<Object, Object> data;
    
    DefaultProperyMap(int initialSize)
    {
      this.data = new HashMap(initialSize);
    }
    
    public Object put(Object key, Object value)
    {
      return this.data.put(key, value);
    }
    
    public <V> V get(Object key)
    {
      return (V)this.data.get(key);
    }
    
    public <V> V remove(Object key)
    {
      return (V)this.data.remove(key);
    }
  }
  
  private static final class LazyPropertyMap
    implements DefaultHttp2Connection.PropertyMap
  {
    private static final int DEFAULT_INITIAL_SIZE = 4;
    private final DefaultHttp2Connection.DefaultStream stream;
    
    LazyPropertyMap(DefaultHttp2Connection.DefaultStream stream)
    {
      this.stream = stream;
    }
    
    public Object put(Object key, Object value)
    {
      DefaultHttp2Connection.DefaultStream.access$1202(this.stream, new DefaultHttp2Connection.DefaultProperyMap(4));
      return DefaultHttp2Connection.DefaultStream.access$1200(this.stream).put(key, value);
    }
    
    public <V> V get(Object key)
    {
      DefaultHttp2Connection.DefaultStream.access$1202(this.stream, new DefaultHttp2Connection.DefaultProperyMap(4));
      return (V)DefaultHttp2Connection.DefaultStream.access$1200(this.stream).get(key);
    }
    
    public <V> V remove(Object key)
    {
      DefaultHttp2Connection.DefaultStream.access$1202(this.stream, new DefaultHttp2Connection.DefaultProperyMap(4));
      return (V)DefaultHttp2Connection.DefaultStream.access$1200(this.stream).remove(key);
    }
  }
  
  private static IntObjectMap<DefaultStream> newChildMap()
  {
    return new IntObjectHashMap(4);
  }
  
  private static final class ParentChangedEvent
  {
    private final Http2Stream stream;
    private final Http2Stream oldParent;
    
    ParentChangedEvent(Http2Stream stream, Http2Stream oldParent)
    {
      this.stream = stream;
      this.oldParent = oldParent;
    }
    
    public void notifyListener(Http2Connection.Listener l)
    {
      l.priorityTreeParentChanged(this.stream, this.oldParent);
    }
  }
  
  private void notifyParentChanged(List<ParentChangedEvent> events)
  {
    ParentChangedEvent event;
    for (int i = 0; i < events.size(); i++)
    {
      event = (ParentChangedEvent)events.get(i);
      for (Http2Connection.Listener l : this.listeners) {
        event.notifyListener(l);
      }
    }
  }
  
  private void notifyParentChanging(Http2Stream stream, Http2Stream newParent)
  {
    for (Http2Connection.Listener l : this.listeners) {
      l.priorityTreeParentChanging(stream, newParent);
    }
  }
  
  private final class ConnectionStream
    extends DefaultHttp2Connection.DefaultStream
  {
    ConnectionStream()
    {
      super(0);
    }
    
    public Http2Stream setPriority(int parentStreamId, short weight, boolean exclusive)
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream open(boolean halfClosed)
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream close()
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream closeLocalSide()
    {
      throw new UnsupportedOperationException();
    }
    
    public Http2Stream closeRemoteSide()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  private final class DefaultEndpoint<F extends Http2FlowController>
    implements Http2Connection.Endpoint<F>
  {
    private final boolean server;
    private int nextStreamId;
    private int lastStreamCreated;
    private int lastKnownStream = -1;
    private boolean pushToAllowed = true;
    private F flowController;
    private int maxStreams;
    private int numActiveStreams;
    
    DefaultEndpoint(boolean server)
    {
      this.server = server;
      
      this.nextStreamId = (server ? 2 : 1);
      
      this.pushToAllowed = (!server);
      this.maxStreams = Integer.MAX_VALUE;
    }
    
    public int nextStreamId()
    {
      return this.nextStreamId > 1 ? this.nextStreamId : this.nextStreamId + 2;
    }
    
    public boolean createdStreamId(int streamId)
    {
      boolean even = (streamId & 0x1) == 0;
      return this.server == even;
    }
    
    public boolean acceptingNewStreams()
    {
      return (nextStreamId() > 0) && (this.numActiveStreams + 1 <= this.maxStreams);
    }
    
    public DefaultHttp2Connection.DefaultStream createStream(int streamId)
      throws Http2Exception
    {
      checkNewStreamAllowed(streamId);
      
      DefaultHttp2Connection.DefaultStream stream = new DefaultHttp2Connection.DefaultStream(DefaultHttp2Connection.this, streamId);
      
      this.nextStreamId = (streamId + 2);
      this.lastStreamCreated = streamId;
      
      addStream(stream);
      return stream;
    }
    
    public boolean isServer()
    {
      return this.server;
    }
    
    public DefaultHttp2Connection.DefaultStream reservePushStream(int streamId, Http2Stream parent)
      throws Http2Exception
    {
      if (parent == null) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Parent stream missing", new Object[0]);
      }
      if (isLocal() ? !parent.localSideOpen() : !parent.remoteSideOpen()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Stream %d is not open for sending push promise", new Object[] { Integer.valueOf(parent.id()) });
      }
      if (!opposite().allowPushTo()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server push not allowed to opposite endpoint.", new Object[0]);
      }
      checkNewStreamAllowed(streamId);
      
      DefaultHttp2Connection.DefaultStream stream = new DefaultHttp2Connection.DefaultStream(DefaultHttp2Connection.this, streamId);
      DefaultHttp2Connection.DefaultStream.access$1302(stream, isLocal() ? Http2Stream.State.RESERVED_LOCAL : Http2Stream.State.RESERVED_REMOTE);
      
      this.nextStreamId = (streamId + 2);
      this.lastStreamCreated = streamId;
      
      addStream(stream);
      return stream;
    }
    
    private void addStream(DefaultHttp2Connection.DefaultStream stream)
    {
      DefaultHttp2Connection.this.streamMap.put(stream.id(), stream);
      List<DefaultHttp2Connection.ParentChangedEvent> events = new ArrayList(1);
      DefaultHttp2Connection.this.connectionStream.takeChild(stream, false, events);
      for (Http2Connection.Listener listener : DefaultHttp2Connection.this.listeners) {
        listener.streamAdded(stream);
      }
      DefaultHttp2Connection.this.notifyParentChanged(events);
    }
    
    public void allowPushTo(boolean allow)
    {
      if ((allow) && (this.server)) {
        throw new IllegalArgumentException("Servers do not allow push");
      }
      this.pushToAllowed = allow;
    }
    
    public boolean allowPushTo()
    {
      return this.pushToAllowed;
    }
    
    public int numActiveStreams()
    {
      return this.numActiveStreams;
    }
    
    public int maxStreams()
    {
      return this.maxStreams;
    }
    
    public void maxStreams(int maxStreams)
    {
      this.maxStreams = maxStreams;
    }
    
    public int lastStreamCreated()
    {
      return this.lastStreamCreated;
    }
    
    public int lastKnownStream()
    {
      return this.lastKnownStream >= 0 ? this.lastKnownStream : this.lastStreamCreated;
    }
    
    private void lastKnownStream(int lastKnownStream)
    {
      boolean alreadyNotified = DefaultHttp2Connection.this.isGoAway();
      this.lastKnownStream = lastKnownStream;
      if (!alreadyNotified) {
        notifyGoingAway();
      }
    }
    
    private void notifyGoingAway()
    {
      for (Http2Connection.Listener listener : DefaultHttp2Connection.this.listeners) {
        listener.goingAway();
      }
    }
    
    public F flowController()
    {
      return this.flowController;
    }
    
    public void flowController(F flowController)
    {
      this.flowController = ((Http2FlowController)ObjectUtil.checkNotNull(flowController, "flowController"));
    }
    
    public Http2Connection.Endpoint<? extends Http2FlowController> opposite()
    {
      return isLocal() ? DefaultHttp2Connection.this.remoteEndpoint : DefaultHttp2Connection.this.localEndpoint;
    }
    
    private void checkNewStreamAllowed(int streamId)
      throws Http2Exception
    {
      if (DefaultHttp2Connection.this.isGoAway()) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Cannot create a stream since the connection is going away", new Object[0]);
      }
      verifyStreamId(streamId);
      if (!acceptingNewStreams()) {
        throw Http2Exception.connectionError(Http2Error.REFUSED_STREAM, "Maximum streams exceeded for this endpoint.", new Object[0]);
      }
    }
    
    private void verifyStreamId(int streamId)
      throws Http2Exception
    {
      if (streamId < 0) {
        throw new Http2NoMoreStreamIdsException();
      }
      if (streamId < this.nextStreamId) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Request stream %d is behind the next expected stream %d", new Object[] { Integer.valueOf(streamId), Integer.valueOf(this.nextStreamId) });
      }
      if (!createdStreamId(streamId)) {
        throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Request stream %d is not correct for %s connection", new Object[] { Integer.valueOf(streamId), this.server ? "server" : "client" });
      }
    }
    
    private boolean isLocal()
    {
      return this == DefaultHttp2Connection.this.localEndpoint;
    }
  }
}
