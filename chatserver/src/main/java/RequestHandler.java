import Helpers.HTTPReader;
import Objects.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static Helpers.HTTPReader.readHeader;

/**
 * A callable for handling incoming requests to the server.
 * Callable is used because it supports a return value.
 */
public class RequestHandler implements Runnable {

  private static final Character CR = '\r';
  private static final Character LF = '\n';
  private static final String SPACE = " ";

  private Socket clientSocket;
  private ExecutorService threadPool;

  public RequestHandler(Socket clientSocket, ExecutorService threadPool) {
    this.clientSocket = clientSocket;
    this.threadPool = threadPool;
  }

  public void run() {
    try {
      // Process the request header and schedule responding

      DataInputStream inFromClient = new DataInputStream(clientSocket.getInputStream());
      ClientRequest.Builder requestBuilder = ClientRequest.newBuilder();
      RequestHeader header = readRequestHeader(inFromClient);
      requestBuilder.setRequestHeader(header);
      if (header.RequestHasMessageBody()) {
        requestBuilder.setContent(retrieveContentInHeader(header, inFromClient));
      }
      ClientRequest request = requestBuilder.build();

      // Let the threadPool execute a requestResponder
      //TODO: fuctionality.
      threadPool.execute(new RequestResponder(createResponse(request), request, clientSocket, threadPool));

    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private ServerResponse createResponse(ClientRequest clientRequest) {
    try {
      List<String> result = new ArrayList<String>();
      result.add(clientRequest.getRequestHeader().getVersion().toString() + SPACE + StatusCode.STATUS_CODE_200.toString());
      if (clientRequest.getRequestHeader().getCommand() == HTTPCommand.GET) {
        System.out.println("Get command.");
        //TODO: Reason phrase (optional)

        byte[] content = retrieveContentFromFile("/home/eleonora/Documents/school/3bach /sem2/networks/httpLab/chatserver/src/main/resources/HelloWorld.html");
        result.add("Content-Length:" +content.length);
        result.add(CR.toString()+LF.toString());
        ServerResponse.ResponseHeader responseHeader = new ServerResponse.ResponseHeader(result);
        System.out.println(responseHeader.getHeaderText());

        System.out.println("get executes");

        return new ServerResponse(responseHeader,content);
      } else {
        System.out.println("clientRequest: " + clientRequest.getRequestHeader().getRequestText());
        System.out.println("Not a get request");
        return new ServerResponse(new ServerResponse.ResponseHeader(result),new byte[]{});
      }
    } catch (IOException e) {
      System.out.println("IOExceptionThrown.");
      System.out.println("404 Page Not Found.");
      System.out.println(e.getMessage());
    }

    //TODO: Handle exceptions!!!!
    return null;
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
