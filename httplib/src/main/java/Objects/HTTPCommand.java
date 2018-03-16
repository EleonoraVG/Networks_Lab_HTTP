package Objects;

public enum HTTPCommand {

  HEAD, GET, PUT, POST;

  public static HTTPCommand fromString(String string) {
    HTTPCommand command;
    switch (string) {
      case "HEAD":
        command = HEAD;
        break;
      case "GET":
        command = GET;
        break;
      case "PUT":
        command = PUT;
        break;
      case "POST":
        command = POST;
        break;
      default:
        throw new IllegalArgumentException("Invalid HTTP command: " + string);
    }
    return command;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
