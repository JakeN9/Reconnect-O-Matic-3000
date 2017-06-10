package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Queue;

public class DefaultHttp2RemoteFlowController
  implements Http2RemoteFlowController
{
  private static final Comparator<Http2Stream> WEIGHT_ORDER = new Comparator()
  {
    public int compare(Http2Stream o1, Http2Stream o2)
    {
      return o2.weight() - o1.weight();
    }
  };
  private final Http2Connection connection;
  private int initialWindowSize = 65535;
  private ChannelHandlerContext ctx;
  private boolean needFlush;
  
  public DefaultHttp2RemoteFlowController(Http2Connection connection)
  {
    this.connection = ((Http2Connection)ObjectUtil.checkNotNull(connection, "connection"));
    
    connection.connectionStream().setProperty(FlowState.class, new FlowState(connection.connectionStream(), this.initialWindowSize));
    
    connection.addListener(new Http2ConnectionAdapter()
    {
      public void streamAdded(Http2Stream stream)
      {
        stream.setProperty(DefaultHttp2RemoteFlowController.FlowState.class, new DefaultHttp2RemoteFlowController.FlowState(DefaultHttp2RemoteFlowController.this, stream, 0));
      }
      
      public void streamActive(Http2Stream stream)
      {
        DefaultHttp2RemoteFlowController.state(stream).window(DefaultHttp2RemoteFlowController.this.initialWindowSize);
      }
      
      public void streamInactive(Http2Stream stream)
      {
        DefaultHttp2RemoteFlowController.state(stream).clear();
      }
      
      public void priorityTreeParentChanged(Http2Stream stream, Http2Stream oldParent)
      {
        Http2Stream parent = stream.parent();
        if (parent != null)
        {
          int delta = DefaultHttp2RemoteFlowController.state(stream).streamableBytesForTree();
          if (delta != 0) {
            DefaultHttp2RemoteFlowController.state(parent).incrementStreamableBytesForTree(delta);
          }
        }
      }
      
      public void priorityTreeParentChanging(Http2Stream stream, Http2Stream newParent)
      {
        Http2Stream parent = stream.parent();
        if (parent != null)
        {
          int delta = -DefaultHttp2RemoteFlowController.state(stream).streamableBytesForTree();
          if (delta != 0) {
            DefaultHttp2RemoteFlowController.state(parent).incrementStreamableBytesForTree(delta);
          }
        }
      }
    });
  }
  
  public void initialWindowSize(int newWindowSize)
    throws Http2Exception
  {
    if (newWindowSize < 0) {
      throw new IllegalArgumentException("Invalid initial window size: " + newWindowSize);
    }
    int delta = newWindowSize - this.initialWindowSize;
    this.initialWindowSize = newWindowSize;
    for (Http2Stream stream : this.connection.activeStreams()) {
      state(stream).incrementStreamWindow(delta);
    }
    if (delta > 0) {
      writePendingBytes();
    }
  }
  
  public int initialWindowSize()
  {
    return this.initialWindowSize;
  }
  
  public int windowSize(Http2Stream stream)
  {
    return state(stream).window();
  }
  
  public void incrementWindowSize(ChannelHandlerContext ctx, Http2Stream stream, int delta)
    throws Http2Exception
  {
    if (stream.id() == 0)
    {
      connectionState().incrementStreamWindow(delta);
      writePendingBytes();
    }
    else
    {
      FlowState state = state(stream);
      state.incrementStreamWindow(delta);
      state.writeBytes(state.writableWindow());
      flush();
    }
  }
  
  public void sendFlowControlled(ChannelHandlerContext ctx, Http2Stream stream, Http2RemoteFlowController.FlowControlled payload)
  {
    ObjectUtil.checkNotNull(ctx, "ctx");
    ObjectUtil.checkNotNull(payload, "payload");
    if ((this.ctx != null) && (this.ctx != ctx)) {
      throw new IllegalArgumentException("Writing data from multiple ChannelHandlerContexts is not supported");
    }
    this.ctx = ctx;
    try
    {
      FlowState state = state(stream);
      state.newFrame(payload);
      state.writeBytes(state.writableWindow());
      flush();
    }
    catch (Throwable e)
    {
      payload.error(e);
    }
  }
  
  int streamableBytesForTree(Http2Stream stream)
  {
    return state(stream).streamableBytesForTree();
  }
  
  private static FlowState state(Http2Stream stream)
  {
    ObjectUtil.checkNotNull(stream, "stream");
    return (FlowState)stream.getProperty(FlowState.class);
  }
  
  private FlowState connectionState()
  {
    return state(this.connection.connectionStream());
  }
  
  private int connectionWindow()
  {
    return connectionState().window();
  }
  
  private void flush()
  {
    if (this.needFlush)
    {
      this.ctx.flush();
      this.needFlush = false;
    }
  }
  
  private void writePendingBytes()
  {
    Http2Stream connectionStream = this.connection.connectionStream();
    int connectionWindow = state(connectionStream).window();
    if (connectionWindow > 0)
    {
      writeChildren(connectionStream, connectionWindow);
      for (Http2Stream stream : this.connection.activeStreams()) {
        writeChildNode(state(stream));
      }
      flush();
    }
  }
  
  private int writeChildren(Http2Stream parent, int connectionWindow)
  {
    FlowState state = state(parent);
    if (state.streamableBytesForTree() <= 0) {
      return 0;
    }
    int bytesAllocated = 0;
    if (state.streamableBytesForTree() <= connectionWindow)
    {
      for (Http2Stream child : parent.children())
      {
        state = state(child);
        int bytesForChild = state.streamableBytes();
        if ((bytesForChild > 0) || (state.hasFrame()))
        {
          state.allocate(bytesForChild);
          writeChildNode(state);
          bytesAllocated += bytesForChild;
          connectionWindow -= bytesForChild;
        }
        int childBytesAllocated = writeChildren(child, connectionWindow);
        bytesAllocated += childBytesAllocated;
        connectionWindow -= childBytesAllocated;
      }
      return bytesAllocated;
    }
    Http2Stream[] children = (Http2Stream[])parent.children().toArray(new Http2Stream[parent.numChildren()]);
    Arrays.sort(children, WEIGHT_ORDER);
    int totalWeight = parent.totalChildWeights();
    for (int tail = children.length; tail > 0;)
    {
      int head = 0;
      int nextTail = 0;
      int nextTotalWeight = 0;
      int nextConnectionWindow = connectionWindow;
      for (; (head < tail) && (nextConnectionWindow > 0); head++)
      {
        Http2Stream child = children[head];
        state = state(child);
        int weight = child.weight();
        double weightRatio = weight / totalWeight;
        
        int bytesForTree = Math.min(nextConnectionWindow, (int)Math.ceil(connectionWindow * weightRatio));
        int bytesForChild = Math.min(state.streamableBytes(), bytesForTree);
        if ((bytesForChild > 0) || (state.hasFrame()))
        {
          state.allocate(bytesForChild);
          bytesAllocated += bytesForChild;
          nextConnectionWindow -= bytesForChild;
          bytesForTree -= bytesForChild;
          if (state.streamableBytesForTree() - bytesForChild > 0)
          {
            children[(nextTail++)] = child;
            nextTotalWeight += weight;
          }
          if (state.streamableBytes() - bytesForChild == 0) {
            writeChildNode(state);
          }
        }
        if (bytesForTree > 0)
        {
          int childBytesAllocated = writeChildren(child, bytesForTree);
          bytesAllocated += childBytesAllocated;
          nextConnectionWindow -= childBytesAllocated;
        }
      }
      connectionWindow = nextConnectionWindow;
      totalWeight = nextTotalWeight;
      tail = nextTail;
    }
    return bytesAllocated;
  }
  
  private static void writeChildNode(FlowState state)
  {
    state.writeBytes(state.allocated());
    state.resetAllocated();
  }
  
  final class FlowState
  {
    private final Queue<Frame> pendingWriteQueue;
    private final Http2Stream stream;
    private int window;
    private int pendingBytes;
    private int streamableBytesForTree;
    private int allocated;
    
    FlowState(Http2Stream stream, int initialWindowSize)
    {
      this.stream = stream;
      window(initialWindowSize);
      this.pendingWriteQueue = new ArrayDeque(2);
    }
    
    int window()
    {
      return this.window;
    }
    
    void window(int initialWindowSize)
    {
      this.window = initialWindowSize;
    }
    
    void allocate(int bytes)
    {
      this.allocated += bytes;
    }
    
    int allocated()
    {
      return this.allocated;
    }
    
    void resetAllocated()
    {
      this.allocated = 0;
    }
    
    int incrementStreamWindow(int delta)
      throws Http2Exception
    {
      if ((delta > 0) && (Integer.MAX_VALUE - delta < this.window)) {
        throw Http2Exception.streamError(this.stream.id(), Http2Error.FLOW_CONTROL_ERROR, "Window size overflow for stream: %d", new Object[] { Integer.valueOf(this.stream.id()) });
      }
      int previouslyStreamable = streamableBytes();
      this.window += delta;
      
      int streamableDelta = streamableBytes() - previouslyStreamable;
      if (streamableDelta != 0) {
        incrementStreamableBytesForTree(streamableDelta);
      }
      return this.window;
    }
    
    int writableWindow()
    {
      return Math.min(this.window, DefaultHttp2RemoteFlowController.this.connectionWindow());
    }
    
    int streamableBytes()
    {
      return Math.max(0, Math.min(this.pendingBytes, this.window));
    }
    
    int streamableBytesForTree()
    {
      return this.streamableBytesForTree;
    }
    
    Frame newFrame(Http2RemoteFlowController.FlowControlled payload)
    {
      Frame frame = new Frame(payload);
      this.pendingWriteQueue.offer(frame);
      return frame;
    }
    
    boolean hasFrame()
    {
      return !this.pendingWriteQueue.isEmpty();
    }
    
    Frame peek()
    {
      return (Frame)this.pendingWriteQueue.peek();
    }
    
    void clear()
    {
      for (;;)
      {
        Frame frame = (Frame)this.pendingWriteQueue.poll();
        if (frame == null) {
          break;
        }
        frame.writeError(Http2Exception.streamError(this.stream.id(), Http2Error.INTERNAL_ERROR, "Stream closed before write could take place", new Object[0]));
      }
    }
    
    int writeBytes(int bytes)
    {
      int bytesAttempted = 0;
      while (hasFrame())
      {
        int maxBytes = Math.min(bytes - bytesAttempted, writableWindow());
        bytesAttempted += peek().write(maxBytes);
        if (bytes - bytesAttempted <= 0) {
          break;
        }
      }
      return bytesAttempted;
    }
    
    void incrementStreamableBytesForTree(int numBytes)
    {
      this.streamableBytesForTree += numBytes;
      if (!this.stream.isRoot()) {
        DefaultHttp2RemoteFlowController.state(this.stream.parent()).incrementStreamableBytesForTree(numBytes);
      }
    }
    
    private final class Frame
    {
      final Http2RemoteFlowController.FlowControlled payload;
      
      Frame(Http2RemoteFlowController.FlowControlled payload)
      {
        this.payload = payload;
        
        incrementPendingBytes(payload.size());
      }
      
      private void incrementPendingBytes(int numBytes)
      {
        int previouslyStreamable = DefaultHttp2RemoteFlowController.FlowState.this.streamableBytes();
        DefaultHttp2RemoteFlowController.FlowState.access$312(DefaultHttp2RemoteFlowController.FlowState.this, numBytes);
        
        int delta = DefaultHttp2RemoteFlowController.FlowState.this.streamableBytes() - previouslyStreamable;
        if (delta != 0) {
          DefaultHttp2RemoteFlowController.FlowState.this.incrementStreamableBytesForTree(delta);
        }
      }
      
      int write(int allowedBytes)
      {
        int before = this.payload.size();
        DefaultHttp2RemoteFlowController.access$476(DefaultHttp2RemoteFlowController.this, this.payload.write(Math.max(0, allowedBytes)));
        int writtenBytes = before - this.payload.size();
        try
        {
          DefaultHttp2RemoteFlowController.this.connectionState().incrementStreamWindow(-writtenBytes);
          DefaultHttp2RemoteFlowController.FlowState.this.incrementStreamWindow(-writtenBytes);
        }
        catch (Http2Exception e)
        {
          throw new RuntimeException("Invalid window state when writing frame: " + e.getMessage(), e);
        }
        decrementPendingBytes(writtenBytes);
        if (this.payload.size() == 0) {
          DefaultHttp2RemoteFlowController.FlowState.this.pendingWriteQueue.remove();
        }
        return writtenBytes;
      }
      
      void writeError(Http2Exception cause)
      {
        decrementPendingBytes(this.payload.size());
        this.payload.error(cause);
      }
      
      void decrementPendingBytes(int bytes)
      {
        incrementPendingBytes(-bytes);
      }
    }
  }
}
