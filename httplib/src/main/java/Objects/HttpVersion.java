package Objects;

public enum HttpVersion {
  HTTP_1_1("HTTP/1.1"),HTTP_1_0("HTTP/1.0");

  private final String stringVersion;

  private HttpVersion(String value){
    stringVersion = value;
  }

  @Override
  public String toString() {
    return stringVersion;
  }
}
