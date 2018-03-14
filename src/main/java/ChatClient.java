import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//TODO: Write only the HTML (content of the response to a file)

public class ChatClient {

  private static final String CR = "\r";
  private static final String LF = "\n";
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
  public void runAndSaveResult(HttpCommand command) throws IOException {
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
  private void runAndSaveResultHTTP_1_0(HttpCommand command) throws IOException {
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
  private void runAndSaveResultHTTP_1_1(HttpCommand command) throws IOException {
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
  private ServerResponse executeCommand(HttpCommand command, String path) throws IOException {
    //TODO: Support the 100 continue reponse!!!!
    //TODO: Account for the content type
    //TODO: Account for the char-set
    //TODO: Account for the character encoding.

    Preconditions.checkNotNull(clientSocket);
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    String sentence = createCommandString(command, path);
    StringBuilder contentBuilder = new StringBuilder();
    // Send request to the server
    outToServer.writeBytes(sentence);
    outToServer.flush();

    //Process the header
    List<String> headerStrings = new ArrayList<>();
    String lastLine = "";
    while (true) {
      lastLine = inFromServer.readLine();
      if (lastLine == null || Objects.equals(lastLine, "") || Objects.equals(lastLine, CR + LF)) {
        break;
      }
      headerStrings.add(lastLine);
    }

    //Build the response header of the server response.
    ServerResponse.ResponseHeader responseHeader = new ServerResponse.ResponseHeader(headerStrings);

    while (true) {

      if (responseHeader.getTransferEncoding() != null && responseHeader.getTransferEncoding().equals("chunked")) {
        String response = processChunkedEncoding(inFromServer);
        contentBuilder.append(response);
        if (response.equals("")) {
          break;
        }
      } else if (responseHeader.getContentLength() != null) {
        contentBuilder.append(processWithContentLength(inFromServer, responseHeader.getContentLength()));
        break;
      } else {
        // Use in case of HTTP/1.0
        lastLine = "";
        while (lastLine != null && !lastLine.equals(CR + LF)) {
          lastLine = inFromServer.readLine(); // Read from the server
          contentBuilder.append(lastLine);
        }
      }
    }

    String contentString = contentBuilder.toString();
    if (responseHeader.getCharSet() != null) {
      Charset usedCharSet = findCharSet(responseHeader.getCharSet());
      contentString = new String(contentString.getBytes("UTF-16"), usedCharSet);
    }
    return new
            ServerResponse(responseHeader, contentString);
  }

  private Charset findCharSet(String string) {
    switch (string.trim().toUpperCase()) {
      case "ISO-8859-1":
        return StandardCharsets.ISO_8859_1;
      case "UTF-8":
        return StandardCharsets.UTF_8;
      case "UTF-16":
        return StandardCharsets.UTF_16;
      case "UTF-16BE":
        return StandardCharsets.UTF_16BE;
      case "UTF-16LE":
        return StandardCharsets.UTF_16LE;
      case "US-ASCII":
        return StandardCharsets.US_ASCII;
      default:
        return null;
    }
  }

  /**
   * Process the response from the server when using chunked encoding.
   *
   * @param inFromServer
   * @return
   * @throws IOException
   */
  private String processChunkedEncoding(BufferedReader inFromServer) throws IOException {
    StringBuilder result = new StringBuilder();
    String lengthLine = inFromServer.readLine();
    // Get the part until the ;
    String[] lengthLineArray = lengthLine.split(";");
    if (lengthLineArray.length < 1) {
      //TODO: Change exception thrown
      throw new IllegalStateException();
    }
    if (lengthLineArray[0].equals("")) {
      return "";
    }
    int messageLength = Integer.parseInt(lengthLineArray[0], 16);

    for (int i = 0; i < messageLength; i++) {
      result.append((char) inFromServer.read());
    }
    // Process the headers and 0 line
    String lastline = "";
    while (!Objects.equals(lastline, CR + LF) && lastline != null && !Objects.equals(lastline, "0")) {
      lastline = inFromServer.readLine();
      result.append(lastline);
    }

    return result.toString();
  }

  /**
   * Process the content of a response when given the content length.
   *
   * @param inFromServer
   * @param contentLength
   * @return
   * @throws IOException
   */
  private String processWithContentLength(BufferedReader inFromServer, int contentLength) throws IOException {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < contentLength; i++) {
      result.append((char) inFromServer.read());
    }
    return result.toString();
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
