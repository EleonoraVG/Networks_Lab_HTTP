import Helpers.FileProcessor;
import Helpers.HTTPReader;
import Objects.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static Constants.HTTPConstants.ENDOFLINE;
import static Constants.HTTPConstants.SPACE;
import static Helpers.HTTPReader.readHeader;
import static Helpers.HTTPReader.readRequestHeader;
import static Helpers.HTTPReader.retrieveContentInRequest;

/**
 * Handle incoming requests to the server, by processing of the request header and creating a response.
 * Also schedules a new task for responding to the client.
 */
public class RequestHandler implements Runnable {

  private static final String serverDir = "serverResources";
  private static final String websiteDir = "websites";
  private static final String startFilePath = "HelloWorld.html";
  private static final String clientInputsDir = "clientInputs";
  private static final String hostName = "eleonora";

  private Socket clientSocket;
  private ExecutorService threadPool;

  public RequestHandler(Socket clientSocket, ExecutorService threadPool) {
    this.clientSocket = clientSocket;
    this.threadPool = threadPool;
  }

  public void run() {

    try {

      // The client socket waits 15 seconds for an action to occur.
      clientSocket.setSoTimeout(15000);

      DataInputStream inFromClient = new DataInputStream(clientSocket.getInputStream());
      ClientRequest.Builder requestBuilder = ClientRequest.newBuilder();

      // Process the request header
      RequestHeader header = readRequestHeader(inFromClient);
      requestBuilder.setRequestHeader(header);

      if (header.isConnectionKeepAlive()) {
        // Keep the socket open if the keep alive header
        // is included in the client request.
        clientSocket.setSoTimeout(0);
      }

      if (header.RequestHasMessageBody() || header.getCommand().equals(HTTPCommand.POST) || header.getCommand().equals(HTTPCommand.PUT)) {
        requestBuilder.setContent(retrieveContentInRequest(header, inFromClient));
      }

      // Make sure all bytes are read from the inputStream.
      while (inFromClient.available() != 0) inFromClient.read();

      ClientRequest request = requestBuilder.build();

      ServerResponse serverResponse = createResponse(request);

      // Schedule a new task for responding to the client over the socket.
      threadPool.execute(new RequestResponder(serverResponse, request, clientSocket, threadPool));

    } catch (Exception e) {
      // If any exception occurred that was not already caught the return the 500 server response.
      threadPool.execute(new RequestResponder(create500Response(), null, clientSocket, threadPool));
    }
  }

  /**
   * Create a response to the client request.
   *
   * @param clientRequest the client request
   * @return a ServerResponse
   */
  private ServerResponse createResponse(ClientRequest clientRequest) {

    List<String> headerStrings = new ArrayList<>();
    byte[] content = new byte[]{};

    System.out.println(clientRequest.getRequestHeader().getRequestText());

    // Check if the client request is valid.
    if (!isValidRequestHeader(clientRequest.getRequestHeader())) {
      return create400Response();
    }

    // Add the initial response line
    headerStrings.add(clientRequest.getRequestHeader().getVersion().toString() + SPACE + StatusCode.STATUS_CODE_200.toString());

    // Add the date
    headerStrings.add(createDateHeaderLine());

    // Process a GET request
    if (clientRequest.getRequestHeader().getCommand().equals(HTTPCommand.GET)) {

      String path = clientRequest.getRequestHeader().getPath();
      if (path == null || path.equals("/") || path.equals(SPACE)) {

        // Retrieve the starting page
        path = serverDir + "/" + websiteDir + "/" + startFilePath;
      } else {

        // Retrieve the page at the given path.
        path = serverDir + "/" + path;
      }

      try {
        content = retrieveContentFromFile(path);
      } catch (IOException e) {
        return create404Response();
      }

      headerStrings.add("Content-Length:" + SPACE + content.length);

      String[] splitPath = path.split("\\.");
      headerStrings.add(
              "Content-Type:" + SPACE + "text/" + splitPath[splitPath.length - 1].trim() + ";"
                      + SPACE + "charset=" + Charset.defaultCharset().toString().toLowerCase());

      return new ServerResponse(new ResponseHeader(headerStrings), content);
    } else if (clientRequest.getRequestHeader().getCommand().equals(HTTPCommand.HEAD)) {
      return new ServerResponse(new ResponseHeader(headerStrings), content);

    } else if (clientRequest.getRequestHeader().getCommand().equals(HTTPCommand.PUT)) {
      try {

        FileProcessor.writeToFile(clientRequest.getContent(), serverDir + clientInputsDir + clientRequest.getRequestHeader().getPath());
      } catch (IOException e) {
        return create404Response();
      }
      return new ServerResponse(new ResponseHeader(headerStrings), content);
    } else if (clientRequest.getRequestHeader().getCommand() == HTTPCommand.POST) {
      try {
        FileProcessor.appendToFile(clientRequest.getContent(), serverDir + clientInputsDir + clientRequest.getRequestHeader().getPath());
      } catch (IOException e) {
        return create404Response();
      }
      return new ServerResponse(new ResponseHeader(headerStrings), content);

    } else {
      headerStrings.add("Content-length:" + SPACE + content.length);
      System.out.println("clientRequest:" + SPACE + clientRequest.getRequestHeader().getRequestText());
      System.out.println("Not a get request");
      return new ServerResponse(new ResponseHeader(headerStrings), content);
    }
  }

  //TODO
  private ServerResponse create302Response() {
    return null;
  }

  private ServerResponse create400Response() {
    List<String> header = new ArrayList<>();
    byte[] content = "400 error".getBytes();
    header.add("HTTP/1.1" + SPACE + StatusCode.STATUS_CODE_400.toString());
    header.add("Content-Length:" + SPACE + content.length);
    header.add("Host:" + SPACE + hostName);
    header.add(createDateHeaderLine());
    return new ServerResponse(new ResponseHeader(header), content);
  }

  private ServerResponse create404Response() {
    List<String> header = new ArrayList<>();
    byte[] content = "404 error".getBytes();
    header.add("HTTP/1.1" + SPACE + StatusCode.STATUS_CODE_404.toString());
    header.add("Content-Length:" + SPACE + content.length);
    header.add("Host:" + SPACE + hostName);
    header.add(createDateHeaderLine());
    return new ServerResponse(new ResponseHeader(header), content);
  }

  private ServerResponse create500Response() {
    List<String> header = new ArrayList<>();
    byte[] content = "500 error".getBytes();
    header.add("HTTP/1.1" + SPACE + StatusCode.STATUS_CODE_500.toString());
    header.add("Content-Length:" + SPACE + content.length);
    header.add("Host:" + SPACE + hostName);
    header.add(createDateHeaderLine());
    return new ServerResponse(new ResponseHeader(header), content);
  }

  /**
   * Create the date line for the header.
   *
   * @return The string for the date line
   */
  private String createDateHeaderLine() {
    return "Date:" + SPACE + formatDateInImfFixDate(Instant.now().atZone(ZoneId.of("GMT")));
  }

  /**
   * Return the date in IMF format.
   *
   * @param dateTime the datetime in GMT.
   * @return A string with the date in IMF format.
   */
  private String formatDateInImfFixDate(ZonedDateTime dateTime) {
    String dayOfTheWeek = dateTime.getDayOfWeek().name();

    return dayOfTheWeek.substring(0, 1) + dayOfTheWeek.substring(1, 3).toLowerCase() + "," + SPACE
            + dateTime.getDayOfMonth() + SPACE
            + dateTime.getMonth().name().substring(0, 1)
            + dateTime.getMonth().name().substring(1, 3).toLowerCase() + SPACE
            + dateTime.getYear() + SPACE
            + dateTime.getHour() + ":"
            + dateTime.getMinute() + ":"
            + dateTime.getSecond() + SPACE
            + "GMT";
  }

  private byte[] retrieveContentFromFile(String path) throws IOException {
    return Files.readAllBytes(Paths.get(path));
  }

  /**
   * Check is the requestHeader is valid.
   *
   * @param header
   * @return true if a valid header, false otherwise
   */
  private boolean isValidRequestHeader(RequestHeader header) {
    if (header.getVersion() == null || header.getCommand() == null || header.getPath() == null) {
      return false;
    }

    if (header.getVersion().equals(HTTPVersion.HTTP_1_1)) {
      if (header.getHost() == null) {
        return false;
      }
      if (header.getContentLength() == null && header.getTransferEncoding() == null) {
        return false;
      }
    }
    return true;
  }
}
