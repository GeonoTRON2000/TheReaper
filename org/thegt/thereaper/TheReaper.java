package org.thegt.thereaper;

public class TheReaper {

  public static void main(String[] args)
  {
    ReaperBot r;
    switch (args.length) {
      case 4:
        r = new ReaperBot(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        break;
      case 5:
        r = new ReaperBot(args[0], Integer.parseInt(args[1]), args[2], args[3], Boolean.valueOf(args[4]).booleanValue());
        break;
      case 6:
        r = new ReaperBot(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4], args[5]);
        break;
      case 7:
        r = new ReaperBot(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4], args[5], Boolean.valueOf(args[6]).booleanValue());
        break;
      case 8:
        r = new ReaperBot(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4], args[5], IRCAuth.valueOf(args[6]), Boolean.valueOf(args[7]).booleanValue());
        break;
      case 9:
        r = new ReaperBot(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4], args[5], IRCAuth.valueOf(args[6]), Boolean.valueOf(args[7]).booleanValue(), Boolean.valueOf(args[8]).booleanValue());
        break;
      default:
        r = new ReaperBot("irc.thegt.org", 6667, "TheReaper", "#thereaper");
    }
    r.run();
  }

}
