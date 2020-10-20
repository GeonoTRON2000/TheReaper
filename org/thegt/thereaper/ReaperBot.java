package org.thegt.thereaper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaperBot implements Runnable {
  private String host;
  private int port;
  private String nickname;
  private String username;
  private String channel;
  private String password;
  private boolean authenticate;
  private IRCAuth authtype;
  private boolean gameChat;
  private Socket sock;
  private BufferedReader reader;
  private BufferedWriter writer;
  
  public ReaperBot(String host, int port, String nickname, String username, String password, String channel, IRCAuth authtype, boolean authenticate, boolean gameChat) {
    this.host = host;
    this.port = port;
    this.nickname = nickname;
    this.username = username;
    this.password = password;
    this.channel = channel;
    this.authtype = authtype;
    this.authenticate = authenticate;
    this.gameChat = gameChat;
  }
  
  public ReaperBot(String host, int port, String nickname, String username, String password, String channel, IRCAuth authtype, boolean authenticate) {
    this(host, port, nickname, username, password, channel, authtype, authenticate, false);
  }
  
  public ReaperBot(String host, int port, String nickname, String username, String password, String channel) {
    this(host, port, nickname, username, password, channel, IRCAuth.NICKSERV, false);
  }
  
  public ReaperBot(String host, int port, String nickname, String username, String password, String channel, boolean gameChat) {
    this(host, port, nickname, username, password, channel, IRCAuth.NICKSERV, false, gameChat);
  }
  
  public ReaperBot(String host, int port, String nickname, String channel) {
    this(host, port, nickname, nickname, "", channel);
  }
  
  public ReaperBot(String host, int port, String nickname, String channel, boolean gameChat) {
    this(host, port, nickname, nickname, "", channel, gameChat);
  }
  
  public void run() {
    try {
      this.sock = new Socket(this.host, this.port);
      this.reader = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
      this.writer = new BufferedWriter(new OutputStreamWriter(this.sock.getOutputStream()));
      login();
      join();
      input();
      messages();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  private void login() throws IRCException, IOException {
    send(new String[] { "NICK " + this.nickname, "USER " + this.username + " 8 * : TheReaper" });
    String ln;
    while ((ln = this.reader.readLine()) != null) {
      if (ln.indexOf("004") > -1) {
        break;
      }
      if (ln.indexOf("433") > -1) {
        throw new IRCException("Nickname in use!");
      }
      if (ln.startsWith("PING ")) {
        send("PONG " + ln.substring(5));
        break;
      }
      System.out.println(ln.trim());
    }
    if (this.authenticate) {
      auth();
    }
  }
  
  private void join() throws IRCException, IOException {
    send("JOIN " + this.channel);
  }
  
  private void messages() throws IOException {
    String ln;
    while ((ln = this.reader.readLine()) != null) {
      message(ln);
    }
  }
  
  private void message(String message) throws IOException {
    Pattern messagePattern = Pattern.compile(":(\\S+)!(\\S+) PRIVMSG (\\S+) ?:(.*)");
    Matcher messageMatcher = messagePattern.matcher(message);
    if (message.toUpperCase().startsWith("PING ")) {
      send("PONG " + message.substring(5));
      System.out.println("I was pinged.");
    }
    else if (messageMatcher.matches()) {
      String username = messageMatcher.group(1).trim();
      String msg = messageMatcher.group(4).trim();
      if (msg.toLowerCase().startsWith("@" + this.nickname.toLowerCase())) {
        String subMsg = msg.substring(msg.toLowerCase().indexOf("@" + this.nickname.toLowerCase()) + this.nickname.length() + 1).trim();
        if (subMsg.startsWith("#")) {
          command(username, subMsg);
        } else {
          chat("That's nice, " + username + ".");
        }
      }
      else {
        if (this.gameChat) {
          Pattern gameChatPattern = Pattern.compile("\\<(\\S+)\\>:? ?(.*)");
          Matcher gameChatMatcher = gameChatPattern.matcher(msg);
          if (gameChatMatcher.matches()) {
            message(":" + gameChatMatcher.group(1) + "!" + messageMatcher.group(2) + " PRIVMSG " + messageMatcher.group(3) + " :" + gameChatMatcher.group(2));
            return;
          }
        }
        System.out.println("Chat received: " + msg);
      }
    }
    else {
      System.out.println(message);
    }
  }
  
  private void command(String user, String cmd) throws IOException {
    String originalCmd = cmd;
    cmd = cmd.toUpperCase();
    if ((cmd.startsWith("#EXEC ")) && (PermissionManager.hasPermission(user, "THEREAPER.EXEC"))) {
      send(originalCmd.substring(6).trim());
    }
    else if ((cmd.startsWith("#CALC ")) && (PermissionManager.hasPermission(user, "THEREAPER.CALC"))) {
      String[] args = cmd.substring(6).trim().split(" ");
      if (args.length < 3) {
        chat("@" + user + " Invalid arguments.");
        return;
      }
      try {
        double d1 = Double.parseDouble(args[0]);
        String operator = args[1].trim();
        double d2 = Double.parseDouble(args[2]);
        switch (operator.charAt(0)) {
          case '+':
            chat("@" + user + " " + Double.toString(d1+d2));
            break;
          case '-':
            chat("@" + user + " " + Double.toString(d1-d2));
            break;
          case '*':
            chat("@" + user + " " + Double.toString(d1*d2));
            break;
          case '/':
            chat("@" + user + " " + Double.toString(d1/d2));
            break;
          case '%':
            chat("@" + user + " " + Integer.toString(((int) d1) % ((int) d2)));
            break;
          case '^':
            chat("@" + user + " " + Integer.toString(((int) d1) ^ ((int) d2)));
            break;
          case '&':
            chat("@" + user + " " + Integer.toString(((int) d1) & ((int) d2)));
            break;
          case '|':
            chat("@" + user + " " + Integer.toString(((int) d1) | ((int) d2)));
            break;
          default:
            chat("@" + user + " Invalid operation.");
        }
      } catch (NumberFormatException e) {
        chat("@" + user + " Invalid calculation syntax.");
      } catch (ArithmeticException e) {
        chat("@" + user + " Arithmetic error while parsing input.");
      }
    } else if ((cmd.startsWith("#GRANT ")) && (PermissionManager.hasPermission(user, "THEREAPER.PERM"))) {
      String[] args = cmd.substring(7).trim().split(" ");
      if (args.length < 2) {
        chat("@" + user + " Invalid arguments.");
        return;
      }
      PermissionManager.grant(args[0], args[1]);
    } else if ((cmd.startsWith("#REVOKE ")) && (PermissionManager.hasPermission(user, "THEREAPER.PERM"))) {
      String[] args = cmd.substring(8).trim().split(" ");
      if (args.length < 2) {
        chat("@" + user + " Invalid arguments.");
        return;
      }
      PermissionManager.revoke(args[0], args[1]);
    } else if ((cmd.startsWith("#OP ")) && (PermissionManager.hasPermission(user, "THEREAPER.OP.GIVE"))) {
      String toOp = originalCmd.substring(4);
      send("MODE " + this.channel + " +o " + toOp);
    } else if ((cmd.startsWith("#DEOP ")) && (PermissionManager.hasPermission(user, "THEREAPER.OP.TAKE"))) {
      String toDeOp = originalCmd.substring(6);
      send("MODE " + this.channel + " -o " + toDeOp);
    } else if ((cmd.startsWith("#CHAN ")) && (PermissionManager.hasPermission(user, "THEREAPER.CHAN"))) {
      String newChan = originalCmd.substring(6);
      send(new String[] { "PART " + this.channel, "JOIN " + newChan });
      this.channel = newChan;
    } else if ((cmd.startsWith("#TELL ")) && (PermissionManager.hasPermission(user, "THEREAPER.TELL"))) {
      String username = originalCmd.substring(6, originalCmd.indexOf(" ", 6)).trim();
      String message = originalCmd.substring(6 + username.length()).trim();
      send("PRIVMSG " + username + " :" + message);
    } else if (cmd.startsWith("#HELP")) {
      String prefix = "@" + user + " ";
      chat(prefix + "TheReaper help:");
      chat(prefix + "- #EXEC <COMMAND>: Causes me to execute <COMMAND>.");
      chat(prefix + "- #CALC <NUMBER1> <OPERATON> <NUMBER2>: Calculates the expression.");
      chat(prefix + "- #OP <USER>: Causes me to op <USER>, only works if I'm op.");
      chat(prefix + "- #DEOP <USER>: Causes me to deop <USER>.");
      chat(prefix + "- #GRANT <USER> <PERMISSION>: Grants <USER> <PERMISSION>.");
      chat(prefix + "- #REVOKE <USER> <PERMISSION>: Revokes <PERMISSION> from <USER>.");
      chat(prefix + "- #CHAN <CHANNEL>: Causes me to move to <CHANNEL>.");
      chat(prefix + "- #TELL <USER> <MESSAGE>: Causes me to send a private message to <USER>.");
      chat(prefix + "- #HELP: Displays this help text.");
    } else {
      chat("@" + user + " Unknown command or permission denied.");
      chat("@" + user + " Try #HELP for a list of commands.");
    }
  }
  
  private void consoleCommand(String cmd) throws IOException {
    String originalCmd = cmd;
    cmd = cmd.toUpperCase();
    if (cmd.startsWith("EXEC ")) {
      send(originalCmd.substring(5).trim());
    } else if (cmd.startsWith("CALC ")) {
      String[] args = cmd.substring(5).trim().split(" ");
      if (args.length < 3) {
        System.out.println("Invalid arguments.");
        return;
      }
      try {
        double d1 = Double.parseDouble(args[0]);
        String operator = args[1].trim();
        double d2 = Double.parseDouble(args[2]);
        String str1;
        switch (operator.charAt(0)) {
          case '+':
            System.out.println(Double.toString(d1+d2));
            break;
          case '-':
            System.out.println(Double.toString(d1-d2));
            break;
          case '*':
            System.out.println(Double.toString(d1*d2));
            break;
          case '/':
            System.out.println(Double.toString(d1/d2));
            break;
          case '%':
            System.out.println(Integer.toString(((int) d1) % ((int) d2)));
            break;
          case '^':
            System.out.println(Integer.toString(((int) d1) ^ ((int) d2)));
            break;
          case '&':
            System.out.println(Integer.toString(((int) d1) & ((int) d2)));
            break;
          case '|':
            System.out.println(Integer.toString(((int) d1) | ((int) d2)));
            break;
          default:
            System.out.println("Invalid operation.");
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid calculation syntax.");
      } catch (ArithmeticException e) {
        System.out.println("Arithmetic error while parsing input.");
      }
    } else if (cmd.startsWith("GRANT ")) {
      String[] args = cmd.substring(6).trim().split(" ");
      if (args.length < 2) {
        System.out.println("Invalid arguments.");
        return;
      }
      PermissionManager.grant(args[0], args[1]);
    } else if (cmd.startsWith("REVOKE ")) {
      String[] args = cmd.substring(7).trim().split(" ");
      if (args.length < 2) {
        System.out.println("Invalid arguments.");
        return;
      }
      PermissionManager.revoke(args[0], args[1]);
    } else if (cmd.startsWith("OP ")) {
      String toOp = originalCmd.substring(3);
      send("MODE " + this.channel + " +o " + toOp);
    }
    else if (cmd.startsWith("DEOP ")) {
      String toDeOp = originalCmd.substring(5);
      send("MODE " + this.channel + " -o " + toDeOp);
    } else if (cmd.startsWith("CHAN ")) {
      String newChan = originalCmd.substring(5);
      send(new String[] { "PART " + this.channel, "JOIN " + newChan });
      this.channel = newChan;
    } else if (cmd.startsWith("TELL ")) {
      String username = originalCmd.substring(5, originalCmd.indexOf(" ", 5)).trim();
      String message = originalCmd.substring(5 + username.length()).trim();
      send("PRIVMSG " + username + " :" + message);
    } else if (cmd.startsWith("HELP")) {
      System.out.println("TheReaper help:");
      System.out.println("- EXEC <COMMAND>: Causes me to execute <COMMAND>.");
      System.out.println("- CALC <NUMBER1> <OPERATON> <NUMBER2>: Calculates the expression.");
      System.out.println("- OP <USER>: Causes me to op <USER>, only works if I'm op.");
      System.out.println("- DEOP <USER>: Causes me to deop <USER>.");
      System.out.println("- GRANT <USER> <PERMISSION>: Grants <USER> <PERMISSION>.");
      System.out.println("- REVOKE <USER> <PERMISSION>: Revokes <PERMISSION> from <USER>.");
      System.out.println("- CHAN <CHANNEL>: Causes me to move to <CHANNEL>.");
      System.out.println("- TELL <USER> <MESSAGE>: Causes me to send a private message to <USER>.");
      System.out.println("- HELP: Displays this help text.");
    } else {
      System.out.println("Unknown command or permission denied.");
      System.out.println("Try HELP for a list of commands.");
    }
  }
  
  private void send(String message) throws IOException {
    this.writer.write(message + "\r\n");
    this.writer.flush();
  }
  
  private void send(String[] messages) throws IOException {
    for (String s : messages) {
      this.writer.write(s + "\r\n");
    }
    this.writer.flush();
  }
  
  private void chat(String message) throws IOException {
    send("PRIVMSG " + this.channel + " :" + message);
  }
  
  private void auth() throws IOException {
    switch (this.authtype)
    {
    case AUTH: 
      send("PRIVMSG nickserv IDENTIFY " + this.password);
      break;
    case NICKSERV: 
      send("PASS " + this.password);
      break;
    }
  }
  
  private void input() {
    new Thread(new Runnable() {
      public void run() {
        Scanner s = new Scanner(System.in);
        while (true) {
          if (s.hasNextLine()) {
            try {
              ReaperBot.this.consoleCommand(s.nextLine().trim());
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }).start();
  }
}
