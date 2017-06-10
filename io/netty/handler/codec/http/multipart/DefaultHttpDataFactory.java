package io.netty.handler.codec.http.multipart;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DefaultHttpDataFactory
  implements HttpDataFactory
{
  public static final long MINSIZE = 16384L;
  public static final long MAXSIZE = -1L;
  private final boolean useDisk;
  private final boolean checkSize;
  private long minSize;
  private long maxSize = -1L;
  private Charset charset = HttpConstants.DEFAULT_CHARSET;
  private final Map<HttpRequest, List<HttpData>> requestFileDeleteMap = PlatformDependent.newConcurrentHashMap();
  
  public DefaultHttpDataFactory()
  {
    this.useDisk = false;
    this.checkSize = true;
    this.minSize = 16384L;
  }
  
  public DefaultHttpDataFactory(Charset charset)
  {
    this();
    this.charset = charset;
  }
  
  public DefaultHttpDataFactory(boolean useDisk)
  {
    this.useDisk = useDisk;
    this.checkSize = false;
  }
  
  public DefaultHttpDataFactory(boolean useDisk, Charset charset)
  {
    this(useDisk);
    this.charset = charset;
  }
  
  public DefaultHttpDataFactory(long minSize)
  {
    this.useDisk = false;
    this.checkSize = true;
    this.minSize = minSize;
  }
  
  public DefaultHttpDataFactory(long minSize, Charset charset)
  {
    this(minSize);
    this.charset = charset;
  }
  
  public void setMaxLimit(long maxSize)
  {
    this.maxSize = maxSize;
  }
  
  private List<HttpData> getList(HttpRequest request)
  {
    List<HttpData> list = (List)this.requestFileDeleteMap.get(request);
    if (list == null)
    {
      list = new ArrayList();
      this.requestFileDeleteMap.put(request, list);
    }
    return list;
  }
  
  public Attribute createAttribute(HttpRequest request, String name)
  {
    if (this.useDisk)
    {
      Attribute attribute = new DiskAttribute(name, this.charset);
      attribute.setMaxSize(this.maxSize);
      List<HttpData> fileToDelete = getList(request);
      fileToDelete.add(attribute);
      return attribute;
    }
    if (this.checkSize)
    {
      Attribute attribute = new MixedAttribute(name, this.minSize, this.charset);
      attribute.setMaxSize(this.maxSize);
      List<HttpData> fileToDelete = getList(request);
      fileToDelete.add(attribute);
      return attribute;
    }
    MemoryAttribute attribute = new MemoryAttribute(name);
    attribute.setMaxSize(this.maxSize);
    return attribute;
  }
  
  private static void checkHttpDataSize(HttpData data)
  {
    try
    {
      data.checkSize(data.length());
    }
    catch (IOException ignored)
    {
      throw new IllegalArgumentException("Attribute bigger than maxSize allowed");
    }
  }
  
  public Attribute createAttribute(HttpRequest request, String name, String value)
  {
    if (this.useDisk)
    {
      Attribute attribute;
      try
      {
        attribute = new DiskAttribute(name, value, this.charset);
        attribute.setMaxSize(this.maxSize);
      }
      catch (IOException e)
      {
        attribute = new MixedAttribute(name, value, this.minSize, this.charset);
        attribute.setMaxSize(this.maxSize);
      }
      checkHttpDataSize(attribute);
      List<HttpData> fileToDelete = getList(request);
      fileToDelete.add(attribute);
      return attribute;
    }
    if (this.checkSize)
    {
      Attribute attribute = new MixedAttribute(name, value, this.minSize, this.charset);
      attribute.setMaxSize(this.maxSize);
      checkHttpDataSize(attribute);
      List<HttpData> fileToDelete = getList(request);
      fileToDelete.add(attribute);
      return attribute;
    }
    try
    {
      MemoryAttribute attribute = new MemoryAttribute(name, value, this.charset);
      attribute.setMaxSize(this.maxSize);
      checkHttpDataSize(attribute);
      return attribute;
    }
    catch (IOException e)
    {
      throw new IllegalArgumentException(e);
    }
  }
  
  public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size)
  {
    if (this.useDisk)
    {
      FileUpload fileUpload = new DiskFileUpload(name, filename, contentType, contentTransferEncoding, charset, size);
      
      fileUpload.setMaxSize(this.maxSize);
      checkHttpDataSize(fileUpload);
      List<HttpData> fileToDelete = getList(request);
      fileToDelete.add(fileUpload);
      return fileUpload;
    }
    if (this.checkSize)
    {
      FileUpload fileUpload = new MixedFileUpload(name, filename, contentType, contentTransferEncoding, charset, size, this.minSize);
      
      fileUpload.setMaxSize(this.maxSize);
      checkHttpDataSize(fileUpload);
      List<HttpData> fileToDelete = getList(request);
      fileToDelete.add(fileUpload);
      return fileUpload;
    }
    MemoryFileUpload fileUpload = new MemoryFileUpload(name, filename, contentType, contentTransferEncoding, charset, size);
    
    fileUpload.setMaxSize(this.maxSize);
    checkHttpDataSize(fileUpload);
    return fileUpload;
  }
  
  public void removeHttpDataFromClean(HttpRequest request, InterfaceHttpData data)
  {
    if ((data instanceof HttpData))
    {
      List<HttpData> fileToDelete = getList(request);
      fileToDelete.remove(data);
    }
  }
  
  public void cleanRequestHttpData(HttpRequest request)
  {
    List<HttpData> fileToDelete = (List)this.requestFileDeleteMap.remove(request);
    if (fileToDelete != null)
    {
      for (HttpData data : fileToDelete) {
        data.delete();
      }
      fileToDelete.clear();
    }
  }
  
  public void cleanAllHttpData()
  {
    Iterator<Map.Entry<HttpRequest, List<HttpData>>> i = this.requestFileDeleteMap.entrySet().iterator();
    while (i.hasNext())
    {
      Map.Entry<HttpRequest, List<HttpData>> e = (Map.Entry)i.next();
      i.remove();
      
      List<HttpData> fileToDelete = (List)e.getValue();
      if (fileToDelete != null)
      {
        for (HttpData data : fileToDelete) {
          data.delete();
        }
        fileToDelete.clear();
      }
    }
  }
}
