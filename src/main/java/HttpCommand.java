public enum HttpCommand {

  HEAD, GET, PUT, POST;

  @Override
  public String toString() {
    return super.toString();
  }

  public static HttpCommand stringToCommand(String string) {
    HttpCommand command;
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
}
