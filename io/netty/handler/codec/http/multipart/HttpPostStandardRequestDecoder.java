package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpPostStandardRequestDecoder
  implements InterfaceHttpPostRequestDecoder
{
  private final HttpDataFactory factory;
  private final HttpRequest request;
  private final Charset charset;
  private boolean isLastChunk;
  private final List<InterfaceHttpData> bodyListHttpData = new ArrayList();
  private final Map<String, List<InterfaceHttpData>> bodyMapHttpData = new TreeMap(CaseIgnoringComparator.INSTANCE);
  private ByteBuf undecodedChunk;
  private int bodyListHttpDataRank;
  private HttpPostRequestDecoder.MultiPartStatus currentStatus = HttpPostRequestDecoder.MultiPartStatus.NOTSTARTED;
  private Attribute currentAttribute;
  private boolean destroyed;
  private int discardThreshold = 10485760;
  
  public HttpPostStandardRequestDecoder(HttpRequest request)
  {
    this(new DefaultHttpDataFactory(16384L), request, HttpConstants.DEFAULT_CHARSET);
  }
  
  public HttpPostStandardRequestDecoder(HttpDataFactory factory, HttpRequest request)
  {
    this(factory, request, HttpConstants.DEFAULT_CHARSET);
  }
  
  public HttpPostStandardRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset)
  {
    if (factory == null) {
      throw new NullPointerException("factory");
    }
    if (request == null) {
      throw new NullPointerException("request");
    }
    if (charset == null) {
      throw new NullPointerException("charset");
    }
    this.request = request;
    this.charset = charset;
    this.factory = factory;
    if ((request instanceof HttpContent))
    {
      offer((HttpContent)request);
    }
    else
    {
      this.undecodedChunk = Unpooled.buffer();
      parseBody();
    }
  }
  
  private void checkDestroyed()
  {
    if (this.destroyed) {
      throw new IllegalStateException(HttpPostStandardRequestDecoder.class.getSimpleName() + " was destroyed already");
    }
  }
  
  public boolean isMultipart()
  {
    checkDestroyed();
    return false;
  }
  
  public void setDiscardThreshold(int discardThreshold)
  {
    if (discardThreshold < 0) {
      throw new IllegalArgumentException("discardThreshold must be >= 0");
    }
    this.discardThreshold = discardThreshold;
  }
  
  public int getDiscardThreshold()
  {
    return this.discardThreshold;
  }
  
  public List<InterfaceHttpData> getBodyHttpDatas()
  {
    checkDestroyed();
    if (!this.isLastChunk) {
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
    }
    return this.bodyListHttpData;
  }
  
  public List<InterfaceHttpData> getBodyHttpDatas(String name)
  {
    checkDestroyed();
    if (!this.isLastChunk) {
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
    }
    return (List)this.bodyMapHttpData.get(name);
  }
  
  public InterfaceHttpData getBodyHttpData(String name)
  {
    checkDestroyed();
    if (!this.isLastChunk) {
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
    }
    List<InterfaceHttpData> list = (List)this.bodyMapHttpData.get(name);
    if (list != null) {
      return (InterfaceHttpData)list.get(0);
    }
    return null;
  }
  
  public HttpPostStandardRequestDecoder offer(HttpContent content)
  {
    checkDestroyed();
    
    ByteBuf buf = content.content();
    if (this.undecodedChunk == null) {
      this.undecodedChunk = buf.copy();
    } else {
      this.undecodedChunk.writeBytes(buf);
    }
    if ((content instanceof LastHttpContent)) {
      this.isLastChunk = true;
    }
    parseBody();
    if ((this.undecodedChunk != null) && (this.undecodedChunk.writerIndex() > this.discardThreshold)) {
      this.undecodedChunk.discardReadBytes();
    }
    return this;
  }
  
  public boolean hasNext()
  {
    checkDestroyed();
    if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.EPILOGUE) {
      if (this.bodyListHttpDataRank >= this.bodyListHttpData.size()) {
        throw new HttpPostRequestDecoder.EndOfDataDecoderException();
      }
    }
    return (!this.bodyListHttpData.isEmpty()) && (this.bodyListHttpDataRank < this.bodyListHttpData.size());
  }
  
  public InterfaceHttpData next()
  {
    checkDestroyed();
    if (hasNext()) {
      return (InterfaceHttpData)this.bodyListHttpData.get(this.bodyListHttpDataRank++);
    }
    return null;
  }
  
  private void parseBody()
  {
    if ((this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE) || (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.EPILOGUE))
    {
      if (this.isLastChunk) {
        this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.EPILOGUE;
      }
      return;
    }
    parseBodyAttributes();
  }
  
  protected void addHttpData(InterfaceHttpData data)
  {
    if (data == null) {
      return;
    }
    List<InterfaceHttpData> datas = (List)this.bodyMapHttpData.get(data.getName());
    if (datas == null)
    {
      datas = new ArrayList(1);
      this.bodyMapHttpData.put(data.getName(), datas);
    }
    datas.add(data);
    this.bodyListHttpData.add(data);
  }
  
  private void parseBodyAttributesStandard()
  {
    int firstpos = this.undecodedChunk.readerIndex();
    int currentpos = firstpos;
    if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.NOTSTARTED) {
      this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
    }
    boolean contRead = true;
    try
    {
      int ampersandpos;
      while ((this.undecodedChunk.isReadable()) && (contRead))
      {
        char read = (char)this.undecodedChunk.readUnsignedByte();
        currentpos++;
        int ampersandpos;
        switch (this.currentStatus)
        {
        case DISPOSITION: 
          if (read == '=')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.FIELD;
            int equalpos = currentpos - 1;
            String key = decodeAttribute(this.undecodedChunk.toString(firstpos, equalpos - firstpos, this.charset), this.charset);
            
            this.currentAttribute = this.factory.createAttribute(this.request, key);
            firstpos = currentpos;
          }
          else if (read == '&')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            ampersandpos = currentpos - 1;
            String key = decodeAttribute(this.undecodedChunk.toString(firstpos, ampersandpos - firstpos, this.charset), this.charset);
            
            this.currentAttribute = this.factory.createAttribute(this.request, key);
            this.currentAttribute.setValue("");
            addHttpData(this.currentAttribute);
            this.currentAttribute = null;
            firstpos = currentpos;
            contRead = true;
          }
          break;
        case FIELD: 
          if (read == '&')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            ampersandpos = currentpos - 1;
            setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = true;
          }
          else if (read == '\r')
          {
            if (this.undecodedChunk.isReadable())
            {
              read = (char)this.undecodedChunk.readUnsignedByte();
              currentpos++;
              if (read == '\n')
              {
                this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
                int ampersandpos = currentpos - 2;
                setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                firstpos = currentpos;
                contRead = false;
              }
              else
              {
                throw new HttpPostRequestDecoder.ErrorDataDecoderException("Bad end of line");
              }
            }
            else
            {
              currentpos--;
            }
          }
          else if (read == '\n')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
            ampersandpos = currentpos - 1;
            setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = false;
          }
          break;
        default: 
          contRead = false;
        }
      }
      if ((this.isLastChunk) && (this.currentAttribute != null))
      {
        ampersandpos = currentpos;
        if (ampersandpos > firstpos) {
          setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
        } else if (!this.currentAttribute.isCompleted()) {
          setFinalBuffer(Unpooled.EMPTY_BUFFER);
        }
        firstpos = currentpos;
        this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.EPILOGUE;
        this.undecodedChunk.readerIndex(firstpos);
        return;
      }
      if ((contRead) && (this.currentAttribute != null))
      {
        if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.FIELD)
        {
          this.currentAttribute.addContent(this.undecodedChunk.copy(firstpos, currentpos - firstpos), false);
          
          firstpos = currentpos;
        }
        this.undecodedChunk.readerIndex(firstpos);
      }
      else
      {
        this.undecodedChunk.readerIndex(firstpos);
      }
    }
    catch (HttpPostRequestDecoder.ErrorDataDecoderException e)
    {
      this.undecodedChunk.readerIndex(firstpos);
      throw e;
    }
    catch (IOException e)
    {
      this.undecodedChunk.readerIndex(firstpos);
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
    }
  }
  
  private void parseBodyAttributes()
  {
    HttpPostBodyUtil.SeekAheadOptimize sao;
    try
    {
      sao = new HttpPostBodyUtil.SeekAheadOptimize(this.undecodedChunk);
    }
    catch (HttpPostBodyUtil.SeekAheadNoBackArrayException ignored)
    {
      parseBodyAttributesStandard();
      return;
    }
    int firstpos = this.undecodedChunk.readerIndex();
    int currentpos = firstpos;
    if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.NOTSTARTED) {
      this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
    }
    boolean contRead = true;
    try
    {
      int ampersandpos;
      while (sao.pos < sao.limit)
      {
        char read = (char)(sao.bytes[(sao.pos++)] & 0xFF);
        currentpos++;
        int ampersandpos;
        switch (this.currentStatus)
        {
        case DISPOSITION: 
          if (read == '=')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.FIELD;
            int equalpos = currentpos - 1;
            String key = decodeAttribute(this.undecodedChunk.toString(firstpos, equalpos - firstpos, this.charset), this.charset);
            
            this.currentAttribute = this.factory.createAttribute(this.request, key);
            firstpos = currentpos;
          }
          else if (read == '&')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            ampersandpos = currentpos - 1;
            String key = decodeAttribute(this.undecodedChunk.toString(firstpos, ampersandpos - firstpos, this.charset), this.charset);
            
            this.currentAttribute = this.factory.createAttribute(this.request, key);
            this.currentAttribute.setValue("");
            addHttpData(this.currentAttribute);
            this.currentAttribute = null;
            firstpos = currentpos;
            contRead = true;
          }
          break;
        case FIELD: 
          if (read == '&')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.DISPOSITION;
            ampersandpos = currentpos - 1;
            setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = true;
          }
          else if (read == '\r')
          {
            if (sao.pos < sao.limit)
            {
              read = (char)(sao.bytes[(sao.pos++)] & 0xFF);
              currentpos++;
              if (read == '\n')
              {
                this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
                int ampersandpos = currentpos - 2;
                sao.setReadPosition(0);
                setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                firstpos = currentpos;
                contRead = false;
                break label512;
              }
              sao.setReadPosition(0);
              throw new HttpPostRequestDecoder.ErrorDataDecoderException("Bad end of line");
            }
            if (sao.limit > 0) {
              currentpos--;
            }
          }
          else if (read == '\n')
          {
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE;
            ampersandpos = currentpos - 1;
            sao.setReadPosition(0);
            setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
            firstpos = currentpos;
            contRead = false;
          }
          break;
        default: 
          sao.setReadPosition(0);
          contRead = false;
          break label512;
        }
      }
      label512:
      if ((this.isLastChunk) && (this.currentAttribute != null))
      {
        ampersandpos = currentpos;
        if (ampersandpos > firstpos) {
          setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
        } else if (!this.currentAttribute.isCompleted()) {
          setFinalBuffer(Unpooled.EMPTY_BUFFER);
        }
        firstpos = currentpos;
        this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.EPILOGUE;
        this.undecodedChunk.readerIndex(firstpos);
        return;
      }
      if ((contRead) && (this.currentAttribute != null))
      {
        if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.FIELD)
        {
          this.currentAttribute.addContent(this.undecodedChunk.copy(firstpos, currentpos - firstpos), false);
          
          firstpos = currentpos;
        }
        this.undecodedChunk.readerIndex(firstpos);
      }
      else
      {
        this.undecodedChunk.readerIndex(firstpos);
      }
    }
    catch (HttpPostRequestDecoder.ErrorDataDecoderException e)
    {
      this.undecodedChunk.readerIndex(firstpos);
      throw e;
    }
    catch (IOException e)
    {
      this.undecodedChunk.readerIndex(firstpos);
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
    }
  }
  
  private void setFinalBuffer(ByteBuf buffer)
    throws IOException
  {
    this.currentAttribute.addContent(buffer, true);
    String value = decodeAttribute(this.currentAttribute.getByteBuf().toString(this.charset), this.charset);
    this.currentAttribute.setValue(value);
    addHttpData(this.currentAttribute);
    this.currentAttribute = null;
  }
  
  private static String decodeAttribute(String s, Charset charset)
  {
    try
    {
      return QueryStringDecoder.decodeComponent(s, charset);
    }
    catch (IllegalArgumentException e)
    {
      throw new HttpPostRequestDecoder.ErrorDataDecoderException("Bad string: '" + s + '\'', e);
    }
  }
  
  void skipControlCharacters()
  {
    HttpPostBodyUtil.SeekAheadOptimize sao;
    try
    {
      sao = new HttpPostBodyUtil.SeekAheadOptimize(this.undecodedChunk);
    }
    catch (HttpPostBodyUtil.SeekAheadNoBackArrayException ignored)
    {
      try
      {
        skipControlCharactersStandard();
      }
      catch (IndexOutOfBoundsException e)
      {
        throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e);
      }
      return;
    }
    while (sao.pos < sao.limit)
    {
      char c = (char)(sao.bytes[(sao.pos++)] & 0xFF);
      if ((!Character.isISOControl(c)) && (!Character.isWhitespace(c)))
      {
        sao.setReadPosition(1);
        return;
      }
    }
    throw new HttpPostRequestDecoder.NotEnoughDataDecoderException("Access out of bounds");
  }
  
  void skipControlCharactersStandard()
  {
    for (;;)
    {
      char c = (char)this.undecodedChunk.readUnsignedByte();
      if ((!Character.isISOControl(c)) && (!Character.isWhitespace(c)))
      {
        this.undecodedChunk.readerIndex(this.undecodedChunk.readerIndex() - 1);
        break;
      }
    }
  }
  
  public void destroy()
  {
    checkDestroyed();
    cleanFiles();
    this.destroyed = true;
    if ((this.undecodedChunk != null) && (this.undecodedChunk.refCnt() > 0))
    {
      this.undecodedChunk.release();
      this.undecodedChunk = null;
    }
    for (int i = this.bodyListHttpDataRank; i < this.bodyListHttpData.size(); i++) {
      ((InterfaceHttpData)this.bodyListHttpData.get(i)).release();
    }
  }
  
  public void cleanFiles()
  {
    checkDestroyed();
    
    this.factory.cleanRequestHttpData(this.request);
  }
  
  public void removeHttpDataFromClean(InterfaceHttpData data)
  {
    checkDestroyed();
    
    this.factory.removeHttpDataFromClean(this.request, data);
  }
}
