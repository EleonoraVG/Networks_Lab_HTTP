package Objects;


public enum StatusCode {
  STATUS_CODE_100(100),
  STATUS_CODE_200(200),
  STATUS_CODE_400(400),
  STATUS_CODE_500(500),
  STATUS_CODE_304(304);

  private final String stringStatus;
  private final int code;

  StatusCode(int code) {
    switch (code) {
      case 100:
        stringStatus = "100 Continue";
        break;
      case 200:
        stringStatus = "200 OK";
        break;
      case 400:
        stringStatus = "400 Bad Request";
        break;
      case 500:
        stringStatus = "500 Server Error";
        break;
      case 304:
        stringStatus = "304 Not Modified";
        break;
      default:
        throw new IllegalArgumentException();
    }
    this.code = code;
  }

  @Override
  public String toString() {
    return stringStatus;
  }

  public static StatusCode getStatusCodeForInt(int code) {
    switch (code) {
      case 100:
        return STATUS_CODE_100;
      case 200:
        return STATUS_CODE_200;
      case 400:
        return STATUS_CODE_400;
      case 500:
        return STATUS_CODE_500;
      case 304:
        return STATUS_CODE_304;
      default:
        throw new IllegalArgumentException();
    }
  }

  public int getCode() {
    return code;
  }
}
