package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpPostMultipartRequestDecoder
  implements InterfaceHttpPostRequestDecoder
{
  private final HttpDataFactory factory;
  private final HttpRequest request;
  private Charset charset;
  private boolean isLastChunk;
  private final List<InterfaceHttpData> bodyListHttpData = new ArrayList();
  private final Map<String, List<InterfaceHttpData>> bodyMapHttpData = new TreeMap(CaseIgnoringComparator.INSTANCE);
  private ByteBuf undecodedChunk;
  private int bodyListHttpDataRank;
  private String multipartDataBoundary;
  private String multipartMixedBoundary;
  private HttpPostRequestDecoder.MultiPartStatus currentStatus = HttpPostRequestDecoder.MultiPartStatus.NOTSTARTED;
  private Map<CharSequence, Attribute> currentFieldAttributes;
  private FileUpload currentFileUpload;
  private Attribute currentAttribute;
  private boolean destroyed;
  private int discardThreshold = 10485760;
  
  public HttpPostMultipartRequestDecoder(HttpRequest request)
  {
    this(new DefaultHttpDataFactory(16384L), request, HttpConstants.DEFAULT_CHARSET);
  }
  
  public HttpPostMultipartRequestDecoder(HttpDataFactory factory, HttpRequest request)
  {
    this(factory, request, HttpConstants.DEFAULT_CHARSET);
  }
  
  public HttpPostMultipartRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset)
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
    
    setMultipart((String)this.request.headers().getAndConvert(HttpHeaderNames.CONTENT_TYPE));
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
  
  private void setMultipart(String contentType)
  {
    String[] dataBoundary = HttpPostRequestDecoder.getMultipartDataBoundary(contentType);
    if (dataBoundary != null)
    {
      this.multipartDataBoundary = dataBoundary[0];
      if ((dataBoundary.length > 1) && (dataBoundary[1] != null)) {
        this.charset = Charset.forName(dataBoundary[1]);
      }
    }
    else
    {
      this.multipartDataBoundary = null;
    }
    this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.HEADERDELIMITER;
  }
  
  private void checkDestroyed()
  {
    if (this.destroyed) {
      throw new IllegalStateException(HttpPostMultipartRequestDecoder.class.getSimpleName() + " was destroyed already");
    }
  }
  
  public boolean isMultipart()
  {
    checkDestroyed();
    return true;
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
  
  public HttpPostMultipartRequestDecoder offer(HttpContent content)
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
    parseBodyMultipart();
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
  
  private void parseBodyMultipart()
  {
    if ((this.undecodedChunk == null) || (this.undecodedChunk.readableBytes() == 0)) {
      return;
    }
    InterfaceHttpData data = decodeMultipart(this.currentStatus);
    while (data != null)
    {
      addHttpData(data);
      if ((this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE) || (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.EPILOGUE)) {
        break;
      }
      data = decodeMultipart(this.currentStatus);
    }
  }
  
  private InterfaceHttpData decodeMultipart(HttpPostRequestDecoder.MultiPartStatus state)
  {
    switch (state)
    {
    case NOTSTARTED: 
      throw new HttpPostRequestDecoder.ErrorDataDecoderException("Should not be called with the current getStatus");
    case PREAMBLE: 
      throw new HttpPostRequestDecoder.ErrorDataDecoderException("Should not be called with the current getStatus");
    case HEADERDELIMITER: 
      return findMultipartDelimiter(this.multipartDataBoundary, HttpPostRequestDecoder.MultiPartStatus.DISPOSITION, HttpPostRequestDecoder.MultiPartStatus.PREEPILOGUE);
    case DISPOSITION: 
      return findMultipartDisposition();
    case FIELD: 
      Charset localCharset = null;
      Attribute charsetAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderValues.CHARSET);
      if (charsetAttribute != null) {
        try
        {
          localCharset = Charset.forName(charsetAttribute.getValue());
        }
        catch (IOException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
      }
      Attribute nameAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderValues.NAME);
      if (this.currentAttribute == null)
      {
        try
        {
          this.currentAttribute = this.factory.createAttribute(this.request, cleanString(nameAttribute.getValue()));
        }
        catch (NullPointerException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        catch (IllegalArgumentException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        catch (IOException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        if (localCharset != null) {
          this.currentAttribute.setCharset(localCharset);
        }
      }
      try
      {
        loadFieldMultipart(this.multipartDataBoundary);
      }
      catch (HttpPostRequestDecoder.NotEnoughDataDecoderException ignored)
      {
        return null;
      }
      Attribute finalAttribute = this.currentAttribute;
      this.currentAttribute = null;
      this.currentFieldAttributes = null;
      
      this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.HEADERDELIMITER;
      return finalAttribute;
    case FILEUPLOAD: 
      return getFileUpload(this.multipartDataBoundary);
    case MIXEDDELIMITER: 
      return findMultipartDelimiter(this.multipartMixedBoundary, HttpPostRequestDecoder.MultiPartStatus.MIXEDDISPOSITION, HttpPostRequestDecoder.MultiPartStatus.HEADERDELIMITER);
    case MIXEDDISPOSITION: 
      return findMultipartDisposition();
    case MIXEDFILEUPLOAD: 
      return getFileUpload(this.multipartMixedBoundary);
    case PREEPILOGUE: 
      return null;
    case EPILOGUE: 
      return null;
    }
    throw new HttpPostRequestDecoder.ErrorDataDecoderException("Shouldn't reach here.");
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
      catch (IndexOutOfBoundsException e1)
      {
        throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e1);
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
  
  private InterfaceHttpData findMultipartDelimiter(String delimiter, HttpPostRequestDecoder.MultiPartStatus dispositionStatus, HttpPostRequestDecoder.MultiPartStatus closeDelimiterStatus)
  {
    int readerIndex = this.undecodedChunk.readerIndex();
    try
    {
      skipControlCharacters();
    }
    catch (HttpPostRequestDecoder.NotEnoughDataDecoderException ignored)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      return null;
    }
    skipOneLine();
    String newline;
    try
    {
      newline = readDelimiter(delimiter);
    }
    catch (HttpPostRequestDecoder.NotEnoughDataDecoderException ignored)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      return null;
    }
    if (newline.equals(delimiter))
    {
      this.currentStatus = dispositionStatus;
      return decodeMultipart(dispositionStatus);
    }
    if (newline.equals(delimiter + "--"))
    {
      this.currentStatus = closeDelimiterStatus;
      if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.HEADERDELIMITER)
      {
        this.currentFieldAttributes = null;
        return decodeMultipart(HttpPostRequestDecoder.MultiPartStatus.HEADERDELIMITER);
      }
      return null;
    }
    this.undecodedChunk.readerIndex(readerIndex);
    throw new HttpPostRequestDecoder.ErrorDataDecoderException("No Multipart delimiter found");
  }
  
  private InterfaceHttpData findMultipartDisposition()
  {
    int readerIndex = this.undecodedChunk.readerIndex();
    if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.DISPOSITION) {
      this.currentFieldAttributes = new TreeMap(CaseIgnoringComparator.INSTANCE);
    }
    while (!skipOneLine())
    {
      String newline;
      try
      {
        skipControlCharacters();
        newline = readLine();
      }
      catch (HttpPostRequestDecoder.NotEnoughDataDecoderException ignored)
      {
        this.undecodedChunk.readerIndex(readerIndex);
        return null;
      }
      String[] contents = splitMultipartHeader(newline);
      if (HttpHeaderNames.CONTENT_DISPOSITION.equalsIgnoreCase(contents[0]))
      {
        boolean checkSecondArg;
        boolean checkSecondArg;
        if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.DISPOSITION) {
          checkSecondArg = HttpHeaderValues.FORM_DATA.equalsIgnoreCase(contents[1]);
        } else {
          checkSecondArg = (HttpHeaderValues.ATTACHMENT.equalsIgnoreCase(contents[1])) || (HttpHeaderValues.FILE.equalsIgnoreCase(contents[1]));
        }
        if (checkSecondArg) {
          for (int i = 2; i < contents.length; i++)
          {
            String[] values = StringUtil.split(contents[i], '=', 2);
            Attribute attribute;
            try
            {
              String name = cleanString(values[0]);
              String value = values[1];
              if (HttpHeaderValues.FILENAME.contentEquals(name)) {
                value = value.substring(1, value.length() - 1);
              } else {
                value = cleanString(value);
              }
              attribute = this.factory.createAttribute(this.request, name, value);
            }
            catch (NullPointerException e)
            {
              throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
            }
            catch (IllegalArgumentException e)
            {
              throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
            }
            this.currentFieldAttributes.put(attribute.getName(), attribute);
          }
        }
      }
      else if (HttpHeaderNames.CONTENT_TRANSFER_ENCODING.equalsIgnoreCase(contents[0]))
      {
        Attribute attribute;
        try
        {
          attribute = this.factory.createAttribute(this.request, HttpHeaderNames.CONTENT_TRANSFER_ENCODING.toString(), cleanString(contents[1]));
        }
        catch (NullPointerException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        catch (IllegalArgumentException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        this.currentFieldAttributes.put(HttpHeaderNames.CONTENT_TRANSFER_ENCODING.toString(), attribute);
      }
      else if (HttpHeaderNames.CONTENT_LENGTH.equalsIgnoreCase(contents[0]))
      {
        Attribute attribute;
        try
        {
          attribute = this.factory.createAttribute(this.request, HttpHeaderNames.CONTENT_LENGTH.toString(), cleanString(contents[1]));
        }
        catch (NullPointerException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        catch (IllegalArgumentException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        this.currentFieldAttributes.put(HttpHeaderNames.CONTENT_LENGTH.toString(), attribute);
      }
      else if (HttpHeaderNames.CONTENT_TYPE.equalsIgnoreCase(contents[0]))
      {
        if (HttpHeaderValues.MULTIPART_MIXED.equalsIgnoreCase(contents[1]))
        {
          if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.DISPOSITION)
          {
            String values = StringUtil.substringAfter(contents[2], '=');
            this.multipartMixedBoundary = ("--" + values);
            this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.MIXEDDELIMITER;
            return decodeMultipart(HttpPostRequestDecoder.MultiPartStatus.MIXEDDELIMITER);
          }
          throw new HttpPostRequestDecoder.ErrorDataDecoderException("Mixed Multipart found in a previous Mixed Multipart");
        }
        for (int i = 1; i < contents.length; i++) {
          if (contents[i].toLowerCase().startsWith(HttpHeaderValues.CHARSET.toString()))
          {
            String values = StringUtil.substringAfter(contents[i], '=');
            Attribute attribute;
            try
            {
              attribute = this.factory.createAttribute(this.request, HttpHeaderValues.CHARSET.toString(), cleanString(values));
            }
            catch (NullPointerException e)
            {
              throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
            }
            catch (IllegalArgumentException e)
            {
              throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
            }
            this.currentFieldAttributes.put(HttpHeaderValues.CHARSET.toString(), attribute);
          }
          else
          {
            Attribute attribute;
            try
            {
              attribute = this.factory.createAttribute(this.request, cleanString(contents[0]), contents[i]);
            }
            catch (NullPointerException e)
            {
              throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
            }
            catch (IllegalArgumentException e)
            {
              throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
            }
            this.currentFieldAttributes.put(attribute.getName(), attribute);
          }
        }
      }
      else
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException("Unknown Params: " + newline);
      }
    }
    Attribute filenameAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderValues.FILENAME);
    if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.DISPOSITION)
    {
      if (filenameAttribute != null)
      {
        this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.FILEUPLOAD;
        
        return decodeMultipart(HttpPostRequestDecoder.MultiPartStatus.FILEUPLOAD);
      }
      this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.FIELD;
      
      return decodeMultipart(HttpPostRequestDecoder.MultiPartStatus.FIELD);
    }
    if (filenameAttribute != null)
    {
      this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.MIXEDFILEUPLOAD;
      
      return decodeMultipart(HttpPostRequestDecoder.MultiPartStatus.MIXEDFILEUPLOAD);
    }
    throw new HttpPostRequestDecoder.ErrorDataDecoderException("Filename not found");
  }
  
  protected InterfaceHttpData getFileUpload(String delimiter)
  {
    Attribute encoding = (Attribute)this.currentFieldAttributes.get(HttpHeaderNames.CONTENT_TRANSFER_ENCODING.toString());
    Charset localCharset = this.charset;
    
    HttpPostBodyUtil.TransferEncodingMechanism mechanism = HttpPostBodyUtil.TransferEncodingMechanism.BIT7;
    if (encoding != null)
    {
      String code;
      try
      {
        code = encoding.getValue().toLowerCase();
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
      if (code.equals(HttpPostBodyUtil.TransferEncodingMechanism.BIT7.value()))
      {
        localCharset = CharsetUtil.US_ASCII;
      }
      else if (code.equals(HttpPostBodyUtil.TransferEncodingMechanism.BIT8.value()))
      {
        localCharset = CharsetUtil.ISO_8859_1;
        mechanism = HttpPostBodyUtil.TransferEncodingMechanism.BIT8;
      }
      else if (code.equals(HttpPostBodyUtil.TransferEncodingMechanism.BINARY.value()))
      {
        mechanism = HttpPostBodyUtil.TransferEncodingMechanism.BINARY;
      }
      else
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException("TransferEncoding Unknown: " + code);
      }
    }
    Attribute charsetAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderValues.CHARSET.toString());
    if (charsetAttribute != null) {
      try
      {
        localCharset = Charset.forName(charsetAttribute.getValue());
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
    }
    if (this.currentFileUpload == null)
    {
      Attribute filenameAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderValues.FILENAME);
      Attribute nameAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderValues.NAME);
      Attribute contentTypeAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderNames.CONTENT_TYPE);
      if (contentTypeAttribute == null) {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException("Content-Type is absent but required");
      }
      Attribute lengthAttribute = (Attribute)this.currentFieldAttributes.get(HttpHeaderNames.CONTENT_LENGTH);
      long size;
      try
      {
        size = lengthAttribute != null ? Long.parseLong(lengthAttribute.getValue()) : 0L;
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
      catch (NumberFormatException ignored)
      {
        size = 0L;
      }
      try
      {
        this.currentFileUpload = this.factory.createFileUpload(this.request, cleanString(nameAttribute.getValue()), cleanString(filenameAttribute.getValue()), contentTypeAttribute.getValue(), mechanism.value(), localCharset, size);
      }
      catch (NullPointerException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
    }
    try
    {
      readFileUploadByteMultipart(delimiter);
    }
    catch (HttpPostRequestDecoder.NotEnoughDataDecoderException e)
    {
      return null;
    }
    if (this.currentFileUpload.isCompleted())
    {
      if (this.currentStatus == HttpPostRequestDecoder.MultiPartStatus.FILEUPLOAD)
      {
        this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.HEADERDELIMITER;
        this.currentFieldAttributes = null;
      }
      else
      {
        this.currentStatus = HttpPostRequestDecoder.MultiPartStatus.MIXEDDELIMITER;
        cleanMixedAttributes();
      }
      FileUpload fileUpload = this.currentFileUpload;
      this.currentFileUpload = null;
      return fileUpload;
    }
    return null;
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
  
  private void cleanMixedAttributes()
  {
    this.currentFieldAttributes.remove(HttpHeaderValues.CHARSET);
    this.currentFieldAttributes.remove(HttpHeaderNames.CONTENT_LENGTH);
    this.currentFieldAttributes.remove(HttpHeaderNames.CONTENT_TRANSFER_ENCODING);
    this.currentFieldAttributes.remove(HttpHeaderNames.CONTENT_TYPE);
    this.currentFieldAttributes.remove(HttpHeaderValues.FILENAME);
  }
  
  private String readLineStandard()
  {
    int readerIndex = this.undecodedChunk.readerIndex();
    try
    {
      ByteBuf line = Unpooled.buffer(64);
      while (this.undecodedChunk.isReadable())
      {
        byte nextByte = this.undecodedChunk.readByte();
        if (nextByte == 13)
        {
          nextByte = this.undecodedChunk.getByte(this.undecodedChunk.readerIndex());
          if (nextByte == 10)
          {
            this.undecodedChunk.readByte();
            return line.toString(this.charset);
          }
          line.writeByte(13);
        }
        else
        {
          if (nextByte == 10) {
            return line.toString(this.charset);
          }
          line.writeByte(nextByte);
        }
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e);
    }
    this.undecodedChunk.readerIndex(readerIndex);
    throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
  }
  
  private String readLine()
  {
    HttpPostBodyUtil.SeekAheadOptimize sao;
    try
    {
      sao = new HttpPostBodyUtil.SeekAheadOptimize(this.undecodedChunk);
    }
    catch (HttpPostBodyUtil.SeekAheadNoBackArrayException ignored)
    {
      return readLineStandard();
    }
    int readerIndex = this.undecodedChunk.readerIndex();
    try
    {
      ByteBuf line = Unpooled.buffer(64);
      while (sao.pos < sao.limit)
      {
        byte nextByte = sao.bytes[(sao.pos++)];
        if (nextByte == 13)
        {
          if (sao.pos < sao.limit)
          {
            nextByte = sao.bytes[(sao.pos++)];
            if (nextByte == 10)
            {
              sao.setReadPosition(0);
              return line.toString(this.charset);
            }
            sao.pos -= 1;
            line.writeByte(13);
          }
          else
          {
            line.writeByte(nextByte);
          }
        }
        else
        {
          if (nextByte == 10)
          {
            sao.setReadPosition(0);
            return line.toString(this.charset);
          }
          line.writeByte(nextByte);
        }
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e);
    }
    this.undecodedChunk.readerIndex(readerIndex);
    throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
  }
  
  private String readDelimiterStandard(String delimiter)
  {
    int readerIndex = this.undecodedChunk.readerIndex();
    try
    {
      StringBuilder sb = new StringBuilder(64);
      int delimiterPos = 0;
      int len = delimiter.length();
      while ((this.undecodedChunk.isReadable()) && (delimiterPos < len))
      {
        byte nextByte = this.undecodedChunk.readByte();
        if (nextByte == delimiter.charAt(delimiterPos))
        {
          delimiterPos++;
          sb.append((char)nextByte);
        }
        else
        {
          this.undecodedChunk.readerIndex(readerIndex);
          throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
        }
      }
      if (this.undecodedChunk.isReadable())
      {
        byte nextByte = this.undecodedChunk.readByte();
        if (nextByte == 13)
        {
          nextByte = this.undecodedChunk.readByte();
          if (nextByte == 10) {
            return sb.toString();
          }
          this.undecodedChunk.readerIndex(readerIndex);
          throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
        }
        if (nextByte == 10) {
          return sb.toString();
        }
        if (nextByte == 45)
        {
          sb.append('-');
          
          nextByte = this.undecodedChunk.readByte();
          if (nextByte == 45)
          {
            sb.append('-');
            if (this.undecodedChunk.isReadable())
            {
              nextByte = this.undecodedChunk.readByte();
              if (nextByte == 13)
              {
                nextByte = this.undecodedChunk.readByte();
                if (nextByte == 10) {
                  return sb.toString();
                }
                this.undecodedChunk.readerIndex(readerIndex);
                throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
              }
              if (nextByte == 10) {
                return sb.toString();
              }
              this.undecodedChunk.readerIndex(this.undecodedChunk.readerIndex() - 1);
              return sb.toString();
            }
            return sb.toString();
          }
        }
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e);
    }
    this.undecodedChunk.readerIndex(readerIndex);
    throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
  }
  
  private String readDelimiter(String delimiter)
  {
    HttpPostBodyUtil.SeekAheadOptimize sao;
    try
    {
      sao = new HttpPostBodyUtil.SeekAheadOptimize(this.undecodedChunk);
    }
    catch (HttpPostBodyUtil.SeekAheadNoBackArrayException ignored)
    {
      return readDelimiterStandard(delimiter);
    }
    int readerIndex = this.undecodedChunk.readerIndex();
    int delimiterPos = 0;
    int len = delimiter.length();
    try
    {
      StringBuilder sb = new StringBuilder(64);
      while ((sao.pos < sao.limit) && (delimiterPos < len))
      {
        byte nextByte = sao.bytes[(sao.pos++)];
        if (nextByte == delimiter.charAt(delimiterPos))
        {
          delimiterPos++;
          sb.append((char)nextByte);
        }
        else
        {
          this.undecodedChunk.readerIndex(readerIndex);
          throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
        }
      }
      if (sao.pos < sao.limit)
      {
        byte nextByte = sao.bytes[(sao.pos++)];
        if (nextByte == 13)
        {
          if (sao.pos < sao.limit)
          {
            nextByte = sao.bytes[(sao.pos++)];
            if (nextByte == 10)
            {
              sao.setReadPosition(0);
              return sb.toString();
            }
            this.undecodedChunk.readerIndex(readerIndex);
            throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
          }
          this.undecodedChunk.readerIndex(readerIndex);
          throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
        }
        if (nextByte == 10)
        {
          sao.setReadPosition(0);
          return sb.toString();
        }
        if (nextByte == 45)
        {
          sb.append('-');
          if (sao.pos < sao.limit)
          {
            nextByte = sao.bytes[(sao.pos++)];
            if (nextByte == 45)
            {
              sb.append('-');
              if (sao.pos < sao.limit)
              {
                nextByte = sao.bytes[(sao.pos++)];
                if (nextByte == 13)
                {
                  if (sao.pos < sao.limit)
                  {
                    nextByte = sao.bytes[(sao.pos++)];
                    if (nextByte == 10)
                    {
                      sao.setReadPosition(0);
                      return sb.toString();
                    }
                    this.undecodedChunk.readerIndex(readerIndex);
                    throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
                  }
                  this.undecodedChunk.readerIndex(readerIndex);
                  throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
                }
                if (nextByte == 10)
                {
                  sao.setReadPosition(0);
                  return sb.toString();
                }
                sao.setReadPosition(1);
                return sb.toString();
              }
              sao.setReadPosition(0);
              return sb.toString();
            }
          }
        }
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e);
    }
    this.undecodedChunk.readerIndex(readerIndex);
    throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
  }
  
  private void readFileUploadByteMultipartStandard(String delimiter)
  {
    int readerIndex = this.undecodedChunk.readerIndex();
    
    boolean newLine = true;
    int index = 0;
    int lastPosition = this.undecodedChunk.readerIndex();
    boolean found = false;
    while (this.undecodedChunk.isReadable())
    {
      byte nextByte = this.undecodedChunk.readByte();
      if (newLine)
      {
        if (nextByte == delimiter.codePointAt(index))
        {
          index++;
          if (delimiter.length() == index)
          {
            found = true;
            break;
          }
        }
        else
        {
          newLine = false;
          index = 0;
          if (nextByte == 13)
          {
            if (this.undecodedChunk.isReadable())
            {
              nextByte = this.undecodedChunk.readByte();
              if (nextByte == 10)
              {
                newLine = true;
                index = 0;
                lastPosition = this.undecodedChunk.readerIndex() - 2;
              }
              else
              {
                lastPosition = this.undecodedChunk.readerIndex() - 1;
                
                this.undecodedChunk.readerIndex(lastPosition);
              }
            }
          }
          else if (nextByte == 10)
          {
            newLine = true;
            index = 0;
            lastPosition = this.undecodedChunk.readerIndex() - 1;
          }
          else
          {
            lastPosition = this.undecodedChunk.readerIndex();
          }
        }
      }
      else if (nextByte == 13)
      {
        if (this.undecodedChunk.isReadable())
        {
          nextByte = this.undecodedChunk.readByte();
          if (nextByte == 10)
          {
            newLine = true;
            index = 0;
            lastPosition = this.undecodedChunk.readerIndex() - 2;
          }
          else
          {
            lastPosition = this.undecodedChunk.readerIndex() - 1;
            
            this.undecodedChunk.readerIndex(lastPosition);
          }
        }
      }
      else if (nextByte == 10)
      {
        newLine = true;
        index = 0;
        lastPosition = this.undecodedChunk.readerIndex() - 1;
      }
      else
      {
        lastPosition = this.undecodedChunk.readerIndex();
      }
    }
    ByteBuf buffer = this.undecodedChunk.copy(readerIndex, lastPosition - readerIndex);
    if (found) {
      try
      {
        this.currentFileUpload.addContent(buffer, true);
        
        this.undecodedChunk.readerIndex(lastPosition);
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
    } else {
      try
      {
        this.currentFileUpload.addContent(buffer, false);
        
        this.undecodedChunk.readerIndex(lastPosition);
        throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
    }
  }
  
  private void readFileUploadByteMultipart(String delimiter)
  {
    HttpPostBodyUtil.SeekAheadOptimize sao;
    try
    {
      sao = new HttpPostBodyUtil.SeekAheadOptimize(this.undecodedChunk);
    }
    catch (HttpPostBodyUtil.SeekAheadNoBackArrayException ignored)
    {
      readFileUploadByteMultipartStandard(delimiter);
      return;
    }
    int readerIndex = this.undecodedChunk.readerIndex();
    
    boolean newLine = true;
    int index = 0;
    int lastrealpos = sao.pos;
    
    boolean found = false;
    while (sao.pos < sao.limit)
    {
      byte nextByte = sao.bytes[(sao.pos++)];
      if (newLine)
      {
        if (nextByte == delimiter.codePointAt(index))
        {
          index++;
          if (delimiter.length() == index)
          {
            found = true;
            break;
          }
        }
        else
        {
          newLine = false;
          index = 0;
          if (nextByte == 13)
          {
            if (sao.pos < sao.limit)
            {
              nextByte = sao.bytes[(sao.pos++)];
              if (nextByte == 10)
              {
                newLine = true;
                index = 0;
                lastrealpos = sao.pos - 2;
              }
              else
              {
                sao.pos -= 1;
                
                lastrealpos = sao.pos;
              }
            }
          }
          else if (nextByte == 10)
          {
            newLine = true;
            index = 0;
            lastrealpos = sao.pos - 1;
          }
          else
          {
            lastrealpos = sao.pos;
          }
        }
      }
      else if (nextByte == 13)
      {
        if (sao.pos < sao.limit)
        {
          nextByte = sao.bytes[(sao.pos++)];
          if (nextByte == 10)
          {
            newLine = true;
            index = 0;
            lastrealpos = sao.pos - 2;
          }
          else
          {
            sao.pos -= 1;
            
            lastrealpos = sao.pos;
          }
        }
      }
      else if (nextByte == 10)
      {
        newLine = true;
        index = 0;
        lastrealpos = sao.pos - 1;
      }
      else
      {
        lastrealpos = sao.pos;
      }
    }
    int lastPosition = sao.getReadPosition(lastrealpos);
    ByteBuf buffer = this.undecodedChunk.copy(readerIndex, lastPosition - readerIndex);
    if (found) {
      try
      {
        this.currentFileUpload.addContent(buffer, true);
        
        this.undecodedChunk.readerIndex(lastPosition);
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
    } else {
      try
      {
        this.currentFileUpload.addContent(buffer, false);
        
        this.undecodedChunk.readerIndex(lastPosition);
        throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
      }
      catch (IOException e)
      {
        throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
      }
    }
  }
  
  private void loadFieldMultipartStandard(String delimiter)
  {
    int readerIndex = this.undecodedChunk.readerIndex();
    try
    {
      boolean newLine = true;
      int index = 0;
      int lastPosition = this.undecodedChunk.readerIndex();
      boolean found = false;
      while (this.undecodedChunk.isReadable())
      {
        byte nextByte = this.undecodedChunk.readByte();
        if (newLine)
        {
          if (nextByte == delimiter.codePointAt(index))
          {
            index++;
            if (delimiter.length() == index)
            {
              found = true;
              break;
            }
          }
          else
          {
            newLine = false;
            index = 0;
            if (nextByte == 13)
            {
              if (this.undecodedChunk.isReadable())
              {
                nextByte = this.undecodedChunk.readByte();
                if (nextByte == 10)
                {
                  newLine = true;
                  index = 0;
                  lastPosition = this.undecodedChunk.readerIndex() - 2;
                }
                else
                {
                  lastPosition = this.undecodedChunk.readerIndex() - 1;
                  this.undecodedChunk.readerIndex(lastPosition);
                }
              }
              else
              {
                lastPosition = this.undecodedChunk.readerIndex() - 1;
              }
            }
            else if (nextByte == 10)
            {
              newLine = true;
              index = 0;
              lastPosition = this.undecodedChunk.readerIndex() - 1;
            }
            else
            {
              lastPosition = this.undecodedChunk.readerIndex();
            }
          }
        }
        else if (nextByte == 13)
        {
          if (this.undecodedChunk.isReadable())
          {
            nextByte = this.undecodedChunk.readByte();
            if (nextByte == 10)
            {
              newLine = true;
              index = 0;
              lastPosition = this.undecodedChunk.readerIndex() - 2;
            }
            else
            {
              lastPosition = this.undecodedChunk.readerIndex() - 1;
              this.undecodedChunk.readerIndex(lastPosition);
            }
          }
          else
          {
            lastPosition = this.undecodedChunk.readerIndex() - 1;
          }
        }
        else if (nextByte == 10)
        {
          newLine = true;
          index = 0;
          lastPosition = this.undecodedChunk.readerIndex() - 1;
        }
        else
        {
          lastPosition = this.undecodedChunk.readerIndex();
        }
      }
      if (found)
      {
        try
        {
          this.currentAttribute.addContent(this.undecodedChunk.copy(readerIndex, lastPosition - readerIndex), true);
        }
        catch (IOException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        this.undecodedChunk.readerIndex(lastPosition);
      }
      else
      {
        try
        {
          this.currentAttribute.addContent(this.undecodedChunk.copy(readerIndex, lastPosition - readerIndex), false);
        }
        catch (IOException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        this.undecodedChunk.readerIndex(lastPosition);
        throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e);
    }
  }
  
  private void loadFieldMultipart(String delimiter)
  {
    HttpPostBodyUtil.SeekAheadOptimize sao;
    try
    {
      sao = new HttpPostBodyUtil.SeekAheadOptimize(this.undecodedChunk);
    }
    catch (HttpPostBodyUtil.SeekAheadNoBackArrayException ignored)
    {
      loadFieldMultipartStandard(delimiter);
      return;
    }
    int readerIndex = this.undecodedChunk.readerIndex();
    try
    {
      boolean newLine = true;
      int index = 0;
      
      int lastrealpos = sao.pos;
      boolean found = false;
      while (sao.pos < sao.limit)
      {
        byte nextByte = sao.bytes[(sao.pos++)];
        if (newLine)
        {
          if (nextByte == delimiter.codePointAt(index))
          {
            index++;
            if (delimiter.length() == index)
            {
              found = true;
              break;
            }
          }
          else
          {
            newLine = false;
            index = 0;
            if (nextByte == 13)
            {
              if (sao.pos < sao.limit)
              {
                nextByte = sao.bytes[(sao.pos++)];
                if (nextByte == 10)
                {
                  newLine = true;
                  index = 0;
                  lastrealpos = sao.pos - 2;
                }
                else
                {
                  sao.pos -= 1;
                  lastrealpos = sao.pos;
                }
              }
            }
            else if (nextByte == 10)
            {
              newLine = true;
              index = 0;
              lastrealpos = sao.pos - 1;
            }
            else
            {
              lastrealpos = sao.pos;
            }
          }
        }
        else if (nextByte == 13)
        {
          if (sao.pos < sao.limit)
          {
            nextByte = sao.bytes[(sao.pos++)];
            if (nextByte == 10)
            {
              newLine = true;
              index = 0;
              lastrealpos = sao.pos - 2;
            }
            else
            {
              sao.pos -= 1;
              lastrealpos = sao.pos;
            }
          }
        }
        else if (nextByte == 10)
        {
          newLine = true;
          index = 0;
          lastrealpos = sao.pos - 1;
        }
        else
        {
          lastrealpos = sao.pos;
        }
      }
      int lastPosition = sao.getReadPosition(lastrealpos);
      if (found)
      {
        try
        {
          this.currentAttribute.addContent(this.undecodedChunk.copy(readerIndex, lastPosition - readerIndex), true);
        }
        catch (IOException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        this.undecodedChunk.readerIndex(lastPosition);
      }
      else
      {
        try
        {
          this.currentAttribute.addContent(this.undecodedChunk.copy(readerIndex, lastPosition - readerIndex), false);
        }
        catch (IOException e)
        {
          throw new HttpPostRequestDecoder.ErrorDataDecoderException(e);
        }
        this.undecodedChunk.readerIndex(lastPosition);
        throw new HttpPostRequestDecoder.NotEnoughDataDecoderException();
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      this.undecodedChunk.readerIndex(readerIndex);
      throw new HttpPostRequestDecoder.NotEnoughDataDecoderException(e);
    }
  }
  
  private static String cleanString(String field)
  {
    StringBuilder sb = new StringBuilder(field.length());
    for (int i = 0; i < field.length(); i++)
    {
      char nextChar = field.charAt(i);
      if (nextChar == ':') {
        sb.append(32);
      } else if (nextChar == ',') {
        sb.append(32);
      } else if (nextChar == '=') {
        sb.append(32);
      } else if (nextChar == ';') {
        sb.append(32);
      } else if (nextChar == '\t') {
        sb.append(32);
      } else if (nextChar != '"') {
        sb.append(nextChar);
      }
    }
    return sb.toString().trim();
  }
  
  private boolean skipOneLine()
  {
    if (!this.undecodedChunk.isReadable()) {
      return false;
    }
    byte nextByte = this.undecodedChunk.readByte();
    if (nextByte == 13)
    {
      if (!this.undecodedChunk.isReadable())
      {
        this.undecodedChunk.readerIndex(this.undecodedChunk.readerIndex() - 1);
        return false;
      }
      nextByte = this.undecodedChunk.readByte();
      if (nextByte == 10) {
        return true;
      }
      this.undecodedChunk.readerIndex(this.undecodedChunk.readerIndex() - 2);
      return false;
    }
    if (nextByte == 10) {
      return true;
    }
    this.undecodedChunk.readerIndex(this.undecodedChunk.readerIndex() - 1);
    return false;
  }
  
  private static String[] splitMultipartHeader(String sb)
  {
    ArrayList<String> headers = new ArrayList(1);
    
    int nameStart = HttpPostBodyUtil.findNonWhitespace(sb, 0);
    for (int nameEnd = nameStart; nameEnd < sb.length(); nameEnd++)
    {
      char ch = sb.charAt(nameEnd);
      if ((ch == ':') || (Character.isWhitespace(ch))) {
        break;
      }
    }
    for (int colonEnd = nameEnd; colonEnd < sb.length(); colonEnd++) {
      if (sb.charAt(colonEnd) == ':')
      {
        colonEnd++;
        break;
      }
    }
    int valueStart = HttpPostBodyUtil.findNonWhitespace(sb, colonEnd);
    int valueEnd = HttpPostBodyUtil.findEndOfString(sb);
    headers.add(sb.substring(nameStart, nameEnd));
    String svalue = sb.substring(valueStart, valueEnd);
    String[] values;
    String[] values;
    if (svalue.indexOf(';') >= 0) {
      values = splitMultipartHeaderValues(svalue);
    } else {
      values = StringUtil.split(svalue, ',');
    }
    for (String value : values) {
      headers.add(value.trim());
    }
    String[] array = new String[headers.size()];
    for (int i = 0; i < headers.size(); i++) {
      array[i] = ((String)headers.get(i));
    }
    return array;
  }
  
  private static String[] splitMultipartHeaderValues(String svalue)
  {
    List<String> values = new ArrayList(1);
    boolean inQuote = false;
    boolean escapeNext = false;
    int start = 0;
    for (int i = 0; i < svalue.length(); i++)
    {
      char c = svalue.charAt(i);
      if (inQuote)
      {
        if (escapeNext) {
          escapeNext = false;
        } else if (c == '\\') {
          escapeNext = true;
        } else if (c == '"') {
          inQuote = false;
        }
      }
      else if (c == '"')
      {
        inQuote = true;
      }
      else if (c == ';')
      {
        values.add(svalue.substring(start, i));
        start = i + 1;
      }
    }
    values.add(svalue.substring(start));
    return (String[])values.toArray(new String[values.size()]);
  }
}
