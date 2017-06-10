package org.spacehq.opennbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.spacehq.opennbt.tag.TagCreateException;
import org.spacehq.opennbt.tag.TagRegistry;
import org.spacehq.opennbt.tag.builtin.CompoundTag;
import org.spacehq.opennbt.tag.builtin.Tag;

public class NBTIO
{
  public static CompoundTag readFile(String path)
    throws IOException
  {
    return readFile(new File(path));
  }
  
  public static CompoundTag readFile(File file)
    throws IOException
  {
    return readFile(file, true);
  }
  
  public static CompoundTag readFile(String path, boolean compressed)
    throws IOException
  {
    return readFile(new File(path), compressed);
  }
  
  public static CompoundTag readFile(File file, boolean compressed)
    throws IOException
  {
    InputStream in = new FileInputStream(file);
    if (compressed) {
      in = new GZIPInputStream(in);
    }
    Tag tag = readTag(new DataInputStream(in));
    if (!(tag instanceof CompoundTag)) {
      throw new IOException("Root tag is not a CompoundTag!");
    }
    return (CompoundTag)tag;
  }
  
  public static void writeFile(CompoundTag tag, String path)
    throws IOException
  {
    writeFile(tag, new File(path));
  }
  
  public static void writeFile(CompoundTag tag, File file)
    throws IOException
  {
    writeFile(tag, file, true);
  }
  
  public static void writeFile(CompoundTag tag, String path, boolean compressed)
    throws IOException
  {
    writeFile(tag, new File(path), compressed);
  }
  
  public static void writeFile(CompoundTag tag, File file, boolean compressed)
    throws IOException
  {
    if (!file.exists())
    {
      if ((file.getParentFile() != null) && (!file.getParentFile().exists())) {
        file.getParentFile().mkdirs();
      }
      file.createNewFile();
    }
    OutputStream out = new FileOutputStream(file);
    if (compressed) {
      out = new GZIPOutputStream(out);
    }
    writeTag(new DataOutputStream(out), tag);
    out.close();
  }
  
  public static Tag readTag(DataInputStream in)
    throws IOException
  {
    int id = in.readUnsignedByte();
    if (id == 0) {
      return null;
    }
    String name = in.readUTF();
    try
    {
      tag = TagRegistry.createInstance(id, name);
    }
    catch (TagCreateException e)
    {
      Tag tag;
      throw new IOException("Failed to create tag.", e);
    }
    Tag tag;
    tag.read(in);
    return tag;
  }
  
  public static void writeTag(DataOutputStream out, Tag tag)
    throws IOException
  {
    out.writeByte(TagRegistry.getIdFor(tag.getClass()));
    out.writeUTF(tag.getName());
    tag.write(out);
  }
}
