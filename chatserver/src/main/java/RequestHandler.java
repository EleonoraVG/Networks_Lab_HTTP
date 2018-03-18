import Helpers.HTTPReader;
import Objects.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static Constants.HTTPConstants.ENDOFLINE;
import static Constants.HTTPConstants.SPACE;
import static Helpers.HTTPReader.readHeader;

/**
 * Handle incoming requests to the server, by processing of the request header and creating a response.
 * Also schedules a new task for responding to the client.
 */
public class RequestHandler implements Runnable {

  private static final String serverDir = "websites";
  private static final String startFilePath = "HelloWorld.html";

  private Socket clientSocket;
  private ExecutorService threadPool;

  public RequestHandler(Socket clientSocket, ExecutorService threadPool) {
    this.clientSocket = clientSocket;
    this.threadPool = threadPool;
  }

  public void run() {
    try {

      DataInputStream inFromClient = new DataInputStream(clientSocket.getInputStream());
      ClientRequest.Builder requestBuilder = ClientRequest.newBuilder();

      // Process the request header
      RequestHeader header = readRequestHeader(inFromClient);
      requestBuilder.setRequestHeader(header);

      if (header.RequestHasMessageBody()) {
        requestBuilder.setContent(retrieveContentInHeader(header, inFromClient));
      }

      // Make sure the client has finished sending the request.
      while (inFromClient.available() != 0) inFromClient.read();

      ClientRequest request = requestBuilder.build();

      ServerResponse serverResponse = createResponse(request);

      // Schedule a new task for responding to the client over the socket.
      threadPool.execute(new RequestResponder(serverResponse, request, clientSocket, threadPool));

    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private ServerResponse createResponse(ClientRequest clientRequest) {
    try {

      System.out.println(clientRequest.getRequestHeader().getRequestText());
      List<String> headerStrings = new ArrayList<>();
      byte[] content = new byte[]{};

      // add the initial response line
      headerStrings.add(clientRequest.getRequestHeader().getVersion().toString() + SPACE + StatusCode.STATUS_CODE_200.toString());
      // Add the date
      headerStrings.add("Date:" + SPACE + formatDateInImfFixDate(Instant.now().atZone(ZoneId.of("GMT"))));

      // Process a GET request
      if (clientRequest.getRequestHeader().getCommand() == HTTPCommand.GET) {

        String path = clientRequest.getRequestHeader().getPath();
        if (path == null || path.equals("/") || path.equals(SPACE)) {
          // Retrieve the starting page
          path = serverDir + "/" + startFilePath;
        } else {
          path = serverDir + path;
        }
        content = retrieveContentFromFile(path);
        headerStrings.add("Content-Length:" + SPACE + content.length);

        String[] splitPath = path.split("\\.");
        headerStrings.add(
                "Content-Type:" + SPACE + "text/" + splitPath[splitPath.length - 1].trim() + ";"
                        + SPACE + Charset.defaultCharset());

        ServerResponse.ResponseHeader responseHeader = new ServerResponse.ResponseHeader(headerStrings);
        return new ServerResponse(responseHeader, content);
      } else {
        headerStrings.add("Content-length:" + SPACE + content.length);
        System.out.println("clientRequest: " + clientRequest.getRequestHeader().getRequestText());
        System.out.println("Not a get request");
        return new ServerResponse(new ServerResponse.ResponseHeader(headerStrings), new byte[]{});
      }
    } catch (IOException e) {
      StatusCode statusCode = StatusCode.STATUS_CODE_404;
      List<String> header = new ArrayList<>();
      header.add("HTTP/1.1" + statusCode.toString() + SPACE + clientRequest.getRequestHeader().getPath());

      return new ServerResponse(new ServerResponse.ResponseHeader(header), new byte[]{});
//    } catch (NullPointerException e) {
//      System.out.println(e.getMessage());
//      List<String> header = new ArrayList<>();
//      header.add("HTTP/1.1" + SPACE + StatusCode.STATUS_CODE_500.toString());
//      return new ServerResponse(new ServerResponse.ResponseHeader(header), new byte[]{});

    }
  }

  private String formatDateInImfFixDate(ZonedDateTime dateTime) {
    String dayOfTheWeek = dateTime.getDayOfWeek().name();

    return dayOfTheWeek.substring(0, 1) + dayOfTheWeek.substring(1, 3).toLowerCase() + "," + SPACE
            + dateTime.getDayOfMonth() + SPACE
            + dateTime.getMonth().name().substring(0, 1) + dateTime.getMonth().name().substring(1, 3).toLowerCase() + SPACE
            + dateTime.getYear() + SPACE
            + dateTime.getHour() + ":"
            + dateTime.getMinute() + ":"
            + dateTime.getSecond() + SPACE
            + "GMT";
  }

  private byte[] retrieveContentFromFile(String path) throws IOException {
    return Files.readAllBytes(Paths.get(path));
  }

  private byte[] retrieveContentInHeader(RequestHeader header, DataInputStream inFromClient) throws IOException {
    byte[] content = null;
    if (header.getContentLength() != null) {
      content = HTTPReader.processWithContentLength(inFromClient, header.getContentLength());
    } else if (header.getTransferEncoding() != null) {
      //Use transfer transcoding
      content = HTTPReader.readChunkFromServer(inFromClient, Charset.defaultCharset());
    }
    return content;
  }

  public static RequestHeader readRequestHeader(DataInputStream inFromClient) throws IOException {
    return new RequestHeader(readHeader(inFromClient));
  }
}
