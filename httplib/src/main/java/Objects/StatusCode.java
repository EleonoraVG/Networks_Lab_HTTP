package Objects;


public enum StatusCode {
  STATUS_CODE_100("100 Continue", 100),
  STATUS_CODE_200("200 OK", 200),
  STATUS_CODE_404("400 Bad Request", 404),
  STATUS_CODE_500("500 Server Error", 500),
  STATUS_CODE_304("304 Not Modified", 304);

  private final String stringStatus;
  private final int code;

  StatusCode(String value, int code) {
    stringStatus = value;
    this.code = code;
  }

  @Override
  public String toString() {
    return stringStatus;
  }
}
