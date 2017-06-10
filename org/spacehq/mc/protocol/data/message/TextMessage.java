package org.spacehq.mc.protocol.data.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;

public class TextMessage
  extends Message
{
  private String text;
  
  public TextMessage(String text)
  {
    this.text = text;
  }
  
  public String getText()
  {
    return this.text;
  }
  
  public TextMessage clone()
  {
    return (TextMessage)new TextMessage(getText()).setStyle(getStyle().clone()).setExtra(getExtra());
  }
  
  public JsonElement toJson()
  {
    if ((getStyle().isDefault()) && (getExtra().isEmpty())) {
      return new JsonPrimitive(this.text);
    }
    JsonElement e = super.toJson();
    if (e.isJsonObject())
    {
      JsonObject json = e.getAsJsonObject();
      json.addProperty("text", this.text);
      return json;
    }
    return e;
  }
  
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TextMessage that = (TextMessage)o;
    if (!this.text.equals(that.text)) {
      return false;
    }
    return true;
  }
  
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + this.text.hashCode();
    return result;
  }
}
