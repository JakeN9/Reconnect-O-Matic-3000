package org.spacehq.mc.protocol.data.message;

public enum ClickAction
{
  RUN_COMMAND,  SUGGEST_COMMAND,  OPEN_URL,  OPEN_FILE;
  
  private ClickAction() {}
  
  public String toString()
  {
    return name().toLowerCase();
  }
  
  public static ClickAction byName(String name)
  {
    name = name.toLowerCase();
    for (ClickAction action : values()) {
      if (action.toString().equals(name)) {
        return action;
      }
    }
    return null;
  }
}
