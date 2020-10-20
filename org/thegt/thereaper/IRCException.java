package org.thegt.thereaper;

public class IRCException extends Exception {
  private static final long serialVersionUID = -8059149621032780961L;
  private String message;

  public IRCException() {
    this("");
  }

  public IRCException(String message) {
    this.message = message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }
}
