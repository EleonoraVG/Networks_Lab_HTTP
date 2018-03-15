import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
  public void runAndSaveResult(HttpCommand command) throws IOException, InterruptedException {
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
  private void runAndSaveResultHTTP_1_0(HttpCommand command) throws IOException, InterruptedException {
    startConnection();
    DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    ServerResponse serverResponse = executeCommand(inFromServer, outToServer, command, ipAddress.getHostName() + "/");
    inFromServer.close();
    outToServer.close();
    closeConnection();

    if (serverResponse.getResponseHeader().getContentType() == null || serverResponse.getResponseHeader().getContentType().isText()) {
      // Write the text response to file.
      String text = new String(serverResponse.getContent(), serverResponse.getResponseHeader().getCharSet());
      FileProcessor.writeToFile(text, responseDirPath, responseFileName);
      System.out.println(serverResponse.getResponseHeader());

      if (command.equals(HttpCommand.GET)) {
        List<String> objectLocations = HtmlProcessor.retrieveImageLocations(text);
        for (int i = 0; i < objectLocations.size(); i++) {
          startConnection();
          inFromServer = new DataInputStream(clientSocket.getInputStream());
          outToServer = new DataOutputStream(clientSocket.getOutputStream());
          ServerResponse response = executeCommand(inFromServer, outToServer, HttpCommand.GET, objectLocations.get(i));
          // Process the image contents
          if (!response.getResponseHeader().getContentType().isImage()) {
            throw new IllegalStateException("The responseHeader does not indicate this as an image.");
          }
          BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContent()));
          ImageIO.write(image, "png", new File(embeddedObjectsDirPath + "Obj.png"));
          closeConnection();
        }
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
  private void runAndSaveResultHTTP_1_1(HttpCommand command) throws IOException, InterruptedException {
    startConnection();
    DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    ServerResponse serverResponse = executeCommand(inFromServer, outToServer, command, ipAddress.getHostName() + "/");

    if (serverResponse.getResponseHeader().getContentType() == null || serverResponse.getResponseHeader().getContentType().isText()) {
      // Write the text response to file.
      String text = new String(serverResponse.getContent(), serverResponse.getResponseHeader().getCharSet());
      FileProcessor.writeToFile(text, responseDirPath, responseFileName);
      System.out.println(serverResponse.getResponseHeader());


      // Write the response to file
      FileProcessor.writeToFile(text, responseDirPath, responseFileName);
      if (command.equals(HttpCommand.GET)) {
        List<String> objectLocations = HtmlProcessor.retrieveImageLocations(text);
        for (int i = 0; i < objectLocations.size(); i++) {
          ServerResponse response = executeCommand(inFromServer, outToServer, HttpCommand.GET, objectLocations.get(i));
          // Process the image contents
          if (!serverResponse.getResponseHeader().getContentType().isImage()) {
            throw new IllegalStateException("The responseHeader does not indicate this as an image.");
          }
          // TODO: Save content in the given path.
          BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContent()));
          ImageIO.write(image, response.getResponseHeader().getContentType().getImageType(), new File(embeddedObjectsDirPath + "Obj.png"));
        }
      }
    }
    inFromServer.close();
    outToServer.close();
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
  private ServerResponse executeCommand(DataInputStream inFromServer, DataOutputStream outToServer, HttpCommand
          command, String path) throws IOException, InterruptedException {
    //TODO: Support the 100 continue reponse!!!!
    //TODO: Account for the content type
    //TODO: Account for the character encoding.

    Preconditions.checkNotNull(clientSocket);
    String sentence = createCommandString(command, path);

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
    ByteArrayOutputStream contentBytesStream = new ByteArrayOutputStream();
    while (true) {
      byte[] response;
      if (responseHeader.getTransferEncoding() != null && responseHeader.getTransferEncoding().equals("chunked")) {
        response = processChunkedEncoding(inFromServer, responseHeader.getCharSet());
        contentBytesStream.write(response);
        if (response.length == 0) {
          break;
        }
      } else if (responseHeader.getContentLength() != null) {
        response = processWithContentLength(inFromServer, responseHeader.getContentLength());
        contentBytesStream.write(response);
        break;
      } else {
        //TODO: fix
        byte[] line = readOneLine(inFromServer);
        while (line != null && line.length != 0) {
          contentBytesStream.write(line);
          line = readOneLine(inFromServer);
        }
        break;
      }
    }
    // Clear out the DataInputStream.
    while (inFromServer.available() != 0) {
      readOneLine(inFromServer);
    }
    contentBytesStream.flush();
    contentBytesStream.close();
    return new ServerResponse(responseHeader, contentBytesStream.toByteArray());
  }

  private byte[] readOneLine(DataInputStream inFromServer) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int i;
    while ((i = inFromServer.read()) != -1) {

      if (i == CR) {
        int nextChar = inFromServer.read();
        if (nextChar == LF) {
          break;
        } else {
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
  private byte[] processChunkedEncoding(DataInputStream inFromServer, Charset charset) throws IOException {

    // Read the first line
    byte[] firstLineBytes = readOneLine(inFromServer);
    String firstLine = new String(firstLineBytes, charset);
    String lengthString = firstLine.split(";")[0].trim();

    // Sometimes the first response is an empty string.
    if (firstLine.equals("")) {
      byte[] num = readOneLine(inFromServer);
      firstLine = new String(num, charset);
      lengthString = firstLine.split(";")[0].trim();
    }

    // Retrieve the message length.
    int messageLength = Integer.parseInt(lengthString, 16);

    // Read the content of the chunk from the server.
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
    byte[] result = new byte[contentLength];
    inFromServer.readFully(result);
    return result;
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
      //TODO fix appending http:// only when necessary !!!
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
    private String embeddedObjectsDirPath = "outputs/embeddedObjects/"; //TODO change this to write to the src of the image.
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

