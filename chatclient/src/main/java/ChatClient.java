import Helpers.FileProcessor;
import Helpers.HTTPHelper;
import Helpers.HtmlProcessor;
import Objects.HTTPCommand;
import Objects.HTTPVersion;
import Objects.ServerResponse;
import com.google.common.base.Preconditions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static Helpers.HTTPHelper.readOneLine;
import static Helpers.HTTPHelper.startConnectionSocket;

//TODO: Beautify

public class ChatClient {

  private static final Character CR = '\r';
  private static final Character LF = '\n';
  private static final String SPACE = " ";
  private static final String responseFileName = "response";

  private HTTPVersion HTTPVersion;
  private String responseDirPath;
  private int port;
  private InetAddress ipAddress;
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
   * Start the connection to the server, run the httpCommand,
   * retrieve the embedded objects and subsequently close the connection.
   * <p>
   * Use for HTTP/1.1
   *
   * @param command The HTTP command to be executed
   * @throws IOException Thrown when propagated from called functions
   */
  public void runAndSaveResult(HTTPCommand command) throws IOException {

    DataInputStream inFromServer = null;
    DataOutputStream outToServer = null;

    // Establish an initial connection in case of HTTP/1.1
    if (HTTPVersion.equals(HTTPVersion.HTTP_1_1)) {
      startConnection();
      inFromServer = new DataInputStream(clientSocket.getInputStream());
      outToServer = new DataOutputStream(clientSocket.getOutputStream());
    }
    ServerResponse serverResponse = executeCommand(inFromServer, outToServer, createCommandString(command, ipAddress.getHostName() + "/"));

    if (serverResponse.isText()) {
      // Write the text response to file.
      String text = new String(serverResponse.getContent(), serverResponse.getResponseHeader().getCharSet());
      FileProcessor.writeToFile(HtmlProcessor.MakeAllImgPathsRelativeInHtml(text), responseDirPath + responseFileName + "." + serverResponse.getTextType());

      // Print response text.
      System.out.println(text);

      if (command.equals(HTTPCommand.GET)) {
        List<String> imageLocations = HtmlProcessor.retrieveImageLocations(text);
        imageLocations = imageLocations.stream().filter(x -> !x.isEmpty()).collect(Collectors.toList());
        for (String imgLoc : imageLocations) {
          //TODO: clean this up.
          String commandString;
          if (HTTPVersion.equals(HTTPVersion.HTTP_1_1)) {
            commandString = "GET" + SPACE + "http://" + ipAddress.getHostName() + "/" + imgLoc + SPACE + HTTPVersion.toString() + CR.toString() + LF.toString() + "Host:" + SPACE + ipAddress.getHostName() + CR.toString() + LF.toString() + CR.toString() + LF.toString();
          } else {
            commandString = "GET" + SPACE + "http://" + ipAddress.getHostName() + "/" + imgLoc + SPACE + HTTPVersion.toString() + CR.toString() + LF.toString() + CR.toString() + LF.toString();
          }
          serverResponse = executeCommand(inFromServer, outToServer, commandString);
          System.out.println(serverResponse.getResponseHeader().getHeaderText());
          if (!serverResponse.isImage()) {
            throw new IllegalStateException("The responseHeader does not indicate this as an image.");
          } else {
            // Process the image contents
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(serverResponse.getContent()));
            FileProcessor.writeImageToFile(image, serverResponse.getImageType(), responseDirPath + imgLoc);
          }
        }
      }
    }

    // Close the connection after all commands are executed.
    if (HTTPVersion.equals(HTTPVersion.HTTP_1_1)) {
      inFromServer.close();
      outToServer.close();
      closeConnection();
    }
  }

  /**
   * Write the commandString to the server and return the serverResponse.
   *
   * @param inFromServer  The dataInputStream to read from
   * @param outToServer   The dataOutputStream to write to
   * @param commandString HTTP command send to the server
   * @return the response from the server
   * @throws IOException
   */
  private ServerResponse executeCommand(DataInputStream inFromServer, DataOutputStream outToServer, String commandString) throws IOException {
    //TODO: Account for the character encoding.
    if (HTTPVersion.equals(HTTPVersion.HTTP_1_0)) {
      // HTTP/1.0 opens a new connection for every command.
      startConnection();
      inFromServer = new DataInputStream(clientSocket.getInputStream());
      outToServer = new DataOutputStream(clientSocket.getOutputStream());
    }

    // Send request to the server
    outToServer.writeBytes(commandString);
    outToServer.flush();

    // Process the header
    ServerResponse.ResponseHeader responseHeader = HTTPHelper.readHeader(inFromServer);

    if (responseHeader.getStatusCode().getCode() == 100) {
      // Retry the command.
      // Only possible with HTML.
      executeCommand(inFromServer, outToServer, commandString);
    }

    // Read the contents
    ByteArrayOutputStream contentBytesStream = new ByteArrayOutputStream();

    byte[] response;
    if (responseHeader.getTransferEncoding() != null && responseHeader.getTransferEncoding().equals("chunked")) {

      // Process chunks.
      response = readChunkFromServer(inFromServer, responseHeader.getCharSet());
      while (response.length != 0) {
        contentBytesStream.write(response);
        response = readChunkFromServer(inFromServer, responseHeader.getCharSet());
      }

    } else if (responseHeader.getContentLength() != null) {

      // read from server for given length.
      response = processWithContentLength(inFromServer, responseHeader.getContentLength());
      contentBytesStream.write(response);
    } else {

      // Use in case of HTTP/1.0
      byte[] line = HTTPHelper.readOneLine(inFromServer);
      while (line != null && line.length != 0) {
        contentBytesStream.write(line);
        line = readOneLine(inFromServer);
      }

    }

    // Clear out and close the DataInputStream.
    while (inFromServer.available() != 0) {
      readOneLine(inFromServer);
    }
    contentBytesStream.flush();
    contentBytesStream.close();

    // Http/1.0 closes the connection for every command.
    if (HTTPVersion.equals(HTTPVersion.HTTP_1_0)) {
      inFromServer.close();
      outToServer.close();
      closeConnection();
    }
    return new ServerResponse(responseHeader, contentBytesStream.toByteArray());
  }

  /**
   * Read in one chunk in from the server.
   *
   * @param inFromServer
   * @return A byte array containing he contents of the chunk.
   * @throws IOException
   */
  private byte[] readChunkFromServer(DataInputStream inFromServer, Charset charset) throws IOException {

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
  private String createCommandString(HTTPCommand command, String path) throws IOException {
    Preconditions.checkNotNull(command);
    //TODO fix appending http:// only when necessary !!!
    String result = command.toString() + SPACE + "http://" + path + SPACE + HTTPVersion.toString();
    if (HTTPVersion.equals(HTTPVersion.HTTP_1_1)) {
      result += CR.toString() + LF.toString() + "Host: " + ipAddress.getHostName();
    }

    //TODO: Test POST and PUT methods!!!!
    if (command.equals(HTTPCommand.POST) || command.equals(HTTPCommand.PUT)) {
      try {
        // Read one line of user input.
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Write content: ");
        String input = inFromUser.readLine();
        inFromUser.close();
        result += CR.toString()  +LF.toString()+ "Content-Length:" + SPACE + input.trim().length()+CR.toString()+LF.toString() +CR.toString() +LF.toString()+input;
      } catch (IOException e) {
        System.out.println("Error while reading user input");
        throw e;
      }

    }
    result += CR.toString() + LF.toString() + CR.toString() + LF.toString();
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
    private HTTPVersion HTTPVersion = Objects.HTTPVersion.HTTP_1_1;

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

    public ChatClient.Builder setHTTPVersion(HTTPVersion version) {
      HTTPVersion = version;
      return this;
    }

    public ChatClient.Builder setResponseDirPath(String responseDirPath) {
      Preconditions.checkNotNull(responseDirPath);
      this.responseDirPath = responseDirPath;
      return this;
    }

    public ChatClient build() {
      ChatClient client = new ChatClient();
      client.ipAddress = ipAddress;
      client.port = port;
      client.responseDirPath = responseDirPath;
      client.HTTPVersion = HTTPVersion;
      return client;
    }
  }
}

