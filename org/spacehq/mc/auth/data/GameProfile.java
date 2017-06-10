package org.spacehq.mc.auth.data;

import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.spacehq.mc.auth.exception.property.SignatureValidateException;
import org.spacehq.mc.auth.util.Base64;

public class GameProfile
{
  private UUID id;
  private String name;
  private List<Property> properties;
  private Map<TextureType, Texture> textures;
  
  public GameProfile(String id, String name)
  {
    this((id == null) || (id.equals("")) ? null : UUID.fromString(id), name);
  }
  
  public GameProfile(UUID id, String name)
  {
    if ((id == null) && ((name == null) || (name.equals("")))) {
      throw new IllegalArgumentException("Name and ID cannot both be blank");
    }
    this.id = id;
    this.name = name;
  }
  
  public boolean isComplete()
  {
    return (this.id != null) && (this.name != null) && (!this.name.equals(""));
  }
  
  public UUID getId()
  {
    return this.id;
  }
  
  public String getIdAsString()
  {
    return this.id != null ? this.id.toString() : "";
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public List<Property> getProperties()
  {
    if (this.properties == null) {
      this.properties = new ArrayList();
    }
    return this.properties;
  }
  
  public Property getProperty(String name)
  {
    for (Property property : getProperties()) {
      if (property.getName().equals(name)) {
        return property;
      }
    }
    return null;
  }
  
  public Map<TextureType, Texture> getTextures()
  {
    if (this.textures == null) {
      this.textures = new HashMap();
    }
    return this.textures;
  }
  
  public Texture getTexture(TextureType type)
  {
    return (Texture)getTextures().get(type);
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o != null) && (getClass() == o.getClass()))
    {
      GameProfile that = (GameProfile)o;
      return (this.id != null ? this.id.equals(that.id) : that.id == null) && (this.name != null ? this.name.equals(that.name) : that.name == null);
    }
    return false;
  }
  
  public int hashCode()
  {
    int result = this.id != null ? this.id.hashCode() : 0;
    result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
    return result;
  }
  
  public String toString()
  {
    return "GameProfile{id=" + this.id + ", name=" + this.name + ", properties=" + getProperties() + ", textures=" + getTextures() + "}";
  }
  
  public static class Property
  {
    private String name;
    private String value;
    private String signature;
    
    public Property(String name, String value)
    {
      this(name, value, null);
    }
    
    public Property(String name, String value, String signature)
    {
      this.name = name;
      this.value = value;
      this.signature = signature;
    }
    
    public String getName()
    {
      return this.name;
    }
    
    public String getValue()
    {
      return this.value;
    }
    
    public String getSignature()
    {
      return this.signature;
    }
    
    public boolean hasSignature()
    {
      return this.signature != null;
    }
    
    public boolean isSignatureValid(PublicKey key)
      throws SignatureValidateException
    {
      if (!hasSignature()) {
        return false;
      }
      try
      {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(key);
        sig.update(this.value.getBytes());
        return sig.verify(Base64.decode(this.signature.getBytes("UTF-8")));
      }
      catch (Exception e)
      {
        throw new SignatureValidateException("Could not validate property signature.", e);
      }
    }
    
    public String toString()
    {
      return "Property{name=" + this.name + ", value=" + this.value + ", signature=" + this.signature + "}";
    }
  }
  
  public static enum TextureType
  {
    SKIN,  CAPE;
    
    private TextureType() {}
  }
  
  public static enum TextureModel
  {
    NORMAL,  SLIM;
    
    private TextureModel() {}
  }
  
  public static class Texture
  {
    private String url;
    private Map<String, String> metadata;
    
    public Texture(String url, Map<String, String> metadata)
    {
      this.url = url;
      this.metadata = metadata;
    }
    
    public String getURL()
    {
      return this.url;
    }
    
    public GameProfile.TextureModel getModel()
    {
      String model = this.metadata != null ? (String)this.metadata.get("model") : null;
      return (model != null) && (model.equals("slim")) ? GameProfile.TextureModel.SLIM : GameProfile.TextureModel.NORMAL;
    }
    
    public String getHash()
    {
      String url = this.url.endsWith("/") ? this.url.substring(0, this.url.length() - 1) : this.url;
      int slash = url.lastIndexOf("/");
      int dot = url.lastIndexOf(".");
      if (dot < slash) {
        dot = url.length();
      }
      return url.substring(slash + 1, dot != -1 ? dot : url.length());
    }
    
    public String toString()
    {
      return "ProfileTexture{url=" + this.url + ", model=" + getModel() + ", hash=" + getHash() + "}";
    }
  }
}
