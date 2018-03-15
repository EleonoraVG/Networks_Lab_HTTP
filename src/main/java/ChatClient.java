import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

//TODO: Write only the HTML (content of the response to a file)

public class ChatClient {

  private static final Character CR = '\r';
  private static final Character LF = '\n';
  private static final int CRINT = 13;
  private static final int LFINT = 10;
  private static final String SPACE = " ";
  private static final String responseFileName = "response.html";

  private HttpVersion httpVersion;
  private String responseDirPath;
  private String embeddedObjectsDirPath;
  private int port;
  private InetAddress ipAddress;
  @Nullable
  private Socket clientSocket;

  private ChatClient() {
  }

  /**
   * Create a new Builder for ChatClient.
   * Use the builder to build an instance of ChatClient.
   *
   * @return ChatClient.Builder
   */
  public static ChatClient.Builder newBuilder() {
    return new Builder();
  }

  /**
   * Run the Http command and write the results to file.
   * Additionally retrieve the embedded objects if the command is GET.
   *
   * @param command The HTTP command to be executed
   * @throws IOException Thrown when propagated from called functions
   */
  public void runAndSaveResult(HttpCommand command) throws IOException,InterruptedException {
    switch (httpVersion) {
      case HTTP_1_0:
        runAndSaveResultHTTP_1_0(command);
        break;
      case HTTP_1_1:
        runAndSaveResultHTTP_1_1(command);
        break;
    }
  }

  /**
   * Run the httpCommand and retrieve the embedded objects if the issued command was GET.
   * Start and close a new connection with every executed HTTP command.
   * <p>
   * Use for HTTP/1.0
   *
   * @param command The HTTP command to be executed
   * @throws IOException Thrown when propagated from called functions
   */
  private void runAndSaveResultHTTP_1_0(HttpCommand command) throws IOException,InterruptedException {
    startConnection();
    ServerResponse resultInitialCommand = executeCommand(command, ipAddress.getHostName() + "/");
    closeConnection();

    // Write the initial response to file.
    FileProcessor.writeToFile(resultInitialCommand.getContentText(), responseDirPath, responseFileName);
    System.out.println(resultInitialCommand);

    if (command.equals(HttpCommand.GET)) {
      List<String> objectLocations = HtmlProcessor.retrieveEmbeddedObjectLocations(resultInitialCommand.getContentText());
      for (int i = 0; i < objectLocations.size(); i++) {
        startConnection();
        FileProcessor.writeToFile(executeCommand(HttpCommand.GET, objectLocations.get(i)).getContentText(), embeddedObjectsDirPath, "obj" + i);
        closeConnection();
      }
    }
  }

  /**
   * Start the connection to the server, run the httpCommand,
   * retrieve the embedded objects and subsequently close the connection.
   * <p>
   * Use for HTTP/1.1
   *
   * @param command The HTTP command to be executed
   * @throws IOException Thrown when propagated from called functions
   */
  private void runAndSaveResultHTTP_1_1(HttpCommand command) throws IOException,InterruptedException {
    startConnection();
    ServerResponse resultInitialCommand = executeCommand(command, ipAddress.getHostName() + "/");
    System.out.println(resultInitialCommand.getContentText());
    // Write the response to file
    FileProcessor.writeToFile(resultInitialCommand.getContentText(), responseDirPath, responseFileName);
    if (command.equals(HttpCommand.GET)) {
      List<String> objectLocations = HtmlProcessor.retrieveEmbeddedObjectLocations(resultInitialCommand.getContentText());
      for (int i = 0; i < objectLocations.size(); i++) {
        // TODO: Give the byte representation not the string representation! Or get change the representation in execute command to bytes.
        // TODO: Save content in the given path.
        FileProcessor.writeToFile(executeCommand(HttpCommand.GET, objectLocations.get(i)).getContentText(), embeddedObjectsDirPath, "obj" + i);
      }
    }
    closeConnection();
  }

  /**
   * //TODO: Currently assumes that given bytes are characters.
   *
   * @param command HTTP command send to the server
   * @param path    Argument of the HTTP command to the server
   * @return the response string from the server
   * @throws IOException propagated by methods called
   */
  private ServerResponse executeCommand(HttpCommand command, String path) throws IOException,InterruptedException {
    //TODO: Support the 100 continue reponse!!!!
    //TODO: Account for the content type
    //TODO: Account for the char-set
    //TODO: Account for the character encoding.

    Preconditions.checkNotNull(clientSocket);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
    String sentence = createCommandString(command, path);
    StringBuilder contentBuilder = new StringBuilder();
    // Send request to the server
    outToServer.writeBytes(sentence);
    outToServer.flush();

    // Process the header
    byte[] endHeaderBytes = new byte[]{};
    List<String> headerStrings = new ArrayList<>();
    boolean headerDone = false;
    while (headerDone == false) {
      byte[] line = readOneLine(inFromServer);
      if (Arrays.equals(line, endHeaderBytes)) {
        headerDone = true;
      }
      headerStrings.add(new String(line, StandardCharsets.UTF_8));
    }

    //Build the response header of the server response.
    ServerResponse.ResponseHeader responseHeader = new ServerResponse.ResponseHeader(headerStrings);
    System.out.println(responseHeader.getHeaderText());

    // Read the contents
    while (true) {

      if (responseHeader.getTransferEncoding() != null && responseHeader.getTransferEncoding().equals("chunked")) {
        byte[] response = processChunkedEncoding(inFromServer, responseHeader.getCharSet());
        String responseString = new String(response, responseHeader.getCharSet());
        contentBuilder.append(responseString);
        if (response.length== 0) {
          break;
        }
      } else if (responseHeader.getContentLength() != null) {
        contentBuilder.append(new String(processWithContentLength(inFromServer, responseHeader.getContentLength()), responseHeader.getCharSet()));
        break;
      } else {
        //TODO: fix
        break;
//        // Use in case of HTTP/1.0
//        lastLine = "";
//        contentBuilder = new StringBuilder();
//        while (lastLine != null && !lastLine.equals(CR + LF)) {
//          lastLine = inFromServer.readLine(); // Read from the server
//          contentBuilder.append(lastLine);
//        }
      }
    }

    String contentString = contentBuilder.toString();
    return new ServerResponse(responseHeader, contentString);
  }

  private byte[] readOneLine(DataInputStream inFromServer) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int i;
    while ((i = inFromServer.read()) != -1) {

      if (i == CR) {
        int nextChar = inFromServer.read();
        if (nextChar == LF) {
          break;
        }else {
          byteArrayOutputStream.write(i);
        }
      } else if (i == LF) {
        break;
      }
      byteArrayOutputStream.write(i);
    }
    byteArrayOutputStream.flush();
    byteArrayOutputStream.close();
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Process the response from the server when using chunked encoding.
   *
   * @param inFromServer
   * @return
   * @throws IOException
   */
  private byte[] processChunkedEncoding(DataInputStream inFromServer, Charset charset) throws IOException ,InterruptedException{

    //Read the first line
    byte[] firstLineBytes = readOneLine(inFromServer);
    String firstLine = new String(firstLineBytes, charset);
    String lengthString = firstLine.split(";")[0].trim();
    if (firstLine.equals("") ) {
      byte[] num = readOneLine(inFromServer);
      firstLine = new String(num,charset);
      lengthString = firstLine.split(";")[0].trim();
    }
    int messageLength = Integer.parseInt(lengthString, 16);

    System.out.println(messageLength);
    byte[] result = new byte[messageLength];
    inFromServer.readFully(result);
    return result;
  }

  /**
   * Process the content of a response when given the content length.
   *
   * @param inFromServer
   * @param contentLength
   * @return
   * @throws IOException
   */
  private byte[] processWithContentLength(DataInputStream inFromServer, int contentLength) throws IOException {
    System.out.println("ContentLength!");
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    for (int i = 0; i < contentLength; i++) {
      buffer.write(inFromServer.read());
    }
    buffer.flush();
    buffer.close();
    return buffer.toByteArray();
  }

  /**
   * Create the HTTP command string that will be send to the Server.
   *
   * @param command HTTP command for the initial request line
   * @param path    path in the initial request line.
   * @throws IOException Throw if error occurs while reading user input.
   */
  private String createCommandString(HttpCommand command, String path) throws IOException {
    Preconditions.checkNotNull(command);
    String initialRequestLine;
    if (httpVersion.equals(HttpVersion.HTTP_1_0)) {
      //TODO fix appending http:// !!!
      initialRequestLine = command.toString() + SPACE + "http://" + path + SPACE + httpVersion.toString()
              + CR + LF + CR + LF;
    } else {
      initialRequestLine = command.toString() + SPACE + "http://" + path + SPACE + httpVersion.toString() + CR + LF
              + "Host: " + ipAddress.getHostName() + CR + LF + CR + LF;
    }
    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
    String result;
    if (command == HttpCommand.GET || command == HttpCommand.HEAD) {
      result = initialRequestLine;
    } else {
      // The command is either POST or PUT
      try {
        // Read one line of user input.
        String input = inFromUser.readLine();
        result = initialRequestLine + input;
      } catch (IOException e) {
        System.out.println("Error while reading user input");
        throw e;
      }
    }
    return result;
  }

  /**
   * Start a connection and sets the clientSocket field.
   *
   * @throws IOException propagated from startConnectionSocket()
   */
  private void startConnection() throws IOException {
    clientSocket = startConnectionSocket(ipAddress, port);
  }

  /**
   * Close the connection and set the client socket to empty,
   * because a socket cannot be reopened after closing.
   *
   * @throws IOException
   */
  private void closeConnection() throws IOException {
    Preconditions.checkNotNull(clientSocket);
    clientSocket.close();
    clientSocket = null;
  }

  /**
   * Create a stream socket and connect to the specified port number at the ip address.
   *
   * @throws IOException Thrown if error occurred during socket creation.
   */
  private static Socket startConnectionSocket(InetAddress ipAddress, int port) throws IOException {
    try {
      return new Socket(ipAddress, port);
    } catch (IOException e) {
      System.out.println("Something went wrong in creating and connecting the socket.");
      throw e;
    }
  }

  /**
   * Check the that the port is in the expected range from 0 to 65535.
   */
  private static boolean isValidPort(int port) {
    return port >= 0 && port <= 65535; //Do not exceed the maximum port number according to RFC 793
  }

  /**
   * Builder class for setting up the ChatClient class.
   * Port, responseDirPath and embeddedObjectsDirPath have default values
   * The ipAddress has to be set before building the ChatClient.
   */
  public static class Builder {

    // Default values.
    private int port = 80;
    private String responseDirPath = "outputs/";
    private String embeddedObjectsDirPath = "outputs/embeddedObjects"; //TODO change this to write to the src of the image.
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;

    // IpAddress has to be set before building the ChatClient.
    private InetAddress ipAddress;

    public ChatClient.Builder setIpAddress(String string) throws UnknownHostException {
      InetAddress ipAddress = InetAddress.getByName(string);
      Preconditions.checkNotNull(ipAddress);
      this.ipAddress = ipAddress;
      return this;
    }

    public ChatClient.Builder setPort(int port) {
      Preconditions.checkArgument(isValidPort(port));
      this.port = port;
      return this;
    }

    public ChatClient.Builder setHttpVersion(HttpVersion version) {
      httpVersion = version;
      return this;
    }

    public ChatClient.Builder setResponseDirPath(String responseDirPath) {
      Preconditions.checkNotNull(responseDirPath);
      this.responseDirPath = responseDirPath;
      return this;
    }

    public ChatClient.Builder setEmbeddedObjectsDirPath(String responseFilePath) {
      Preconditions.checkNotNull(responseFilePath);
      this.responseDirPath = responseFilePath;
      return this;
    }

    public ChatClient build() {
      ChatClient client = new ChatClient();
      client.ipAddress = ipAddress;
      client.port = port;
      client.embeddedObjectsDirPath = embeddedObjectsDirPath;
      client.responseDirPath = responseDirPath;
      client.httpVersion = httpVersion;
      return client;
    }
  }
}
