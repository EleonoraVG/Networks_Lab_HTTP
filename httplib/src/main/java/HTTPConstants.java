/**
 * Constants that are useful in dealing with HTTP requests and responses.
 */
public final class HTTPConstants {

  public static final Character CR = '\r';
  public static final Character LF = '\n';
  public static final String SPACE = " ";
  public static final String ENDOFLINE = CR.toString() + LF.toString();

  // Prevent instantiation.
  private HTTPConstants() {
  }
}
