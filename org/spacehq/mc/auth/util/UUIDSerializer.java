package org.spacehq.mc.auth.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.UUID;

public class UUIDSerializer
  extends TypeAdapter<UUID>
{
  public void write(JsonWriter out, UUID value)
    throws IOException
  {
    out.value(fromUUID(value));
  }
  
  public UUID read(JsonReader in)
    throws IOException
  {
    return fromString(in.nextString());
  }
  
  public static String fromUUID(UUID value)
  {
    if (value == null) {
      return "";
    }
    return value.toString().replace("-", "");
  }
  
  public static UUID fromString(String value)
  {
    if ((value == null) || (value.equals(""))) {
      return null;
    }
    return UUID.fromString(value.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
  }
}
