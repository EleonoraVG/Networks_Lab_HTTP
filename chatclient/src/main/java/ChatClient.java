import Helpers.FileProcessor;
import Helpers.HTTPReader;
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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static Helpers.HTTPReader.*;
import static Objects.HTTPVersion.HTTP_1_1;


public class ChatClient {

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
    ServerResponse response;
    DataInputStream inFromServer = null;
    DataOutputStream outToServer = null;

    // Establish an initial connection in case of HTTP/1.1
    if (HTTPVersion.equals(HTTP_1_1)) {
      startConnection();
      inFromServer = new DataInputStream(clientSocket.getInputStream());
      outToServer = new DataOutputStream(clientSocket.getOutputStream());
      response = executeCommand(inFromServer, outToServer, createRequest(command, ipAddress.getHostName() + "/"));
    } else {
      // Create a new connection for every command in case of HTTP/1.0
      response = executeCommandHTTP10(createRequest(command, ipAddress.getHostName() + "/"));
    }

    //TODO: Support other types
    // The first response should be text.
    if (response.isText()) {

      // Print the response header
      System.out.println(response.getResponseHeader().getHeaderText());

      // Create and print a string from the html in the right encoding.
      String text = new String(response.getContent(), response.getResponseHeader().getCharSet());
      System.out.println(text);

      // Create a new HTML processor
      HtmlProcessor htmlProcessor = new HtmlProcessor(text);

      // Write the HTML text to file.
      FileProcessor.writeToFile(htmlProcessor.retrieveRelativeImagePathsHtml(),
              responseDirPath + ipAddress.getHostName() + "-response" + "." + response.getTextType());

      // In case of a get command retrieve the new page.
      if (command.equals(HTTPCommand.GET)) {

        // Retrieve imageLocations from the html.
        List<String> imageLocations = htmlProcessor
                .retrieveImageLocations().stream()
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());

        for (String imgLoc : imageLocations) {
          //TODO: clean this up.
          String commandString;
          if (HTTPVersion.equals(HTTP_1_1)) {
            commandString = "GET" + SPACE + "http://" + ipAddress.getHostName() + "/" + imgLoc + SPACE + HTTPVersion.toString() + ENDOFLINE + "Host:" + SPACE + ipAddress.getHostName() + ENDOFLINE + ENDOFLINE;
            response = executeCommand(inFromServer, outToServer, commandString);
          } else {
            commandString = "GET" + SPACE + "http://" + ipAddress.getHostName() + "/" + imgLoc + SPACE + HTTPVersion.toString() + CR.toString() + LF.toString() + CR.toString() + LF.toString();
            response = executeCommandHTTP10(commandString);
          }

          // Print the header of the response
          System.out.println(response.getResponseHeader().getHeaderText());

          // Write the image to file.
          if (!response.isImage()) {
            throw new IllegalStateException("The responseHeader does not indicate this as an image.");
          } else {
            // Process the image contents
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContent()));
            FileProcessor.writeImageToFile(image, response.getImageType(), responseDirPath + imgLoc);
          }
        }
      }
    }

    // Close the connection after all commands are executed.
    if (HTTPVersion.equals(HTTP_1_1)) {
      inFromServer.close();
      outToServer.close();
      closeConnection();
    }
  }

  /**
   * Execute a HTTP10 command.
   * First establish a connection to the server.]
   * Execute the command.
   * Close the connection to the server.
   *
   * @param commandString
   * @return
   * @throws IOException
   */
  private ServerResponse executeCommandHTTP10(String commandString) throws IOException {

    // Establish a connection
    startConnection();
    DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

    ServerResponse response = executeCommand(inFromServer, outToServer, commandString);

    // Close the connection
    inFromServer.close();
    outToServer.close();
    closeConnection();

    return response;
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

    // Send request to the server
    outToServer.writeBytes(commandString);
    outToServer.flush();

    // Process the header
    ServerResponse.ResponseHeader responseHeader = readServerResponseHeader(inFromServer);

    if (responseHeader.getStatusCode().getCode() == 100) {
      // Retry the command.
      // Only possible with HTTP/1.1
      executeCommand(inFromServer, outToServer, commandString);
    }

    // Read the contents
    ByteArrayOutputStream contentBytesStream = new ByteArrayOutputStream();

    byte[] response;
    if (responseHeader.getTransferEncoding() != null && responseHeader.getTransferEncoding().equals("chunked")) {

      // Process chunks until and empty chunk arrives
      response = HTTPReader.readChunkFromServer(inFromServer, responseHeader.getCharSet());
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
      byte[] line = HTTPReader.readOneLine(inFromServer);
      while (line != null && line.length != 0) {
        contentBytesStream.write(line);
        line = readOneLine(inFromServer);
      }

    }

    // Clear out the in from server data stream and close the bytes stream.
    while (inFromServer.available() != 0) {
      readOneLine(inFromServer);
    }
    contentBytesStream.flush();
    contentBytesStream.close();

    return new ServerResponse(responseHeader, contentBytesStream.toByteArray());
  }

  /**
   * Read a responseHeader from the server.
   *
   * @param inFromServer
   * @return
   * @throws IOException
   */
  private ServerResponse.ResponseHeader readServerResponseHeader(DataInputStream inFromServer) throws IOException {
    List<String> headerStrings = readHeader(inFromServer);
    return new ServerResponse.ResponseHeader(headerStrings);
  }

  /**
   * Create the HTTP command string that will be send to the Server.
   *
   * @param command HTTP command for the initial request line
   * @param path    path in the initial request line.
   * @throws IOException Throw if error occurs while reading user input.
   */
  private String createRequest(HTTPCommand command, String path) throws IOException {
    Preconditions.checkNotNull(command);
    String result;
    if (!Pattern.matches("http://.*", path)) {
      result = command.toString() + SPACE + "http://" + path + SPACE + HTTPVersion.toString() + ENDOFLINE;
    } else {
      result = command.toString() + SPACE + path + SPACE + HTTPVersion.toString() + ENDOFLINE;
    }
    if (HTTPVersion.equals(HTTP_1_1)) {
      result += "Host: " + ipAddress.getHostName() + ENDOFLINE;
    }

    // Read user input for the content of the POST or PUT command.
    if (command.equals(HTTPCommand.POST) || command.equals(HTTPCommand.PUT)) {

      // Read one line of user input.
      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
      System.out.println("Write content: ");
      String input = inFromUser.readLine();
      inFromUser.close();

      // Append the content of the user input to the HTTP request
      result += "Content-Length:" + SPACE + input.trim().length() + ENDOFLINE + ENDOFLINE + input + ENDOFLINE;
    }

    result += ENDOFLINE + ENDOFLINE;
    return result;
  }

  /**
   * Create a stream socket and connect to the specified port number at the ip address.
   *
   * @throws IOException Thrown if error occurred during socket creation.
   */
  public static Socket startConnectionSocket(InetAddress ipAddress, int port) throws IOException {
    try {
      return new Socket(ipAddress, port);
    } catch (IOException e) {
      System.out.println("Something went wrong in creating and connecting the socket.");
      throw e;
    }
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
    private String responseDirPath = "websites/";
    private HTTPVersion HTTPVersion = HTTP_1_1;

    // IpAddress has to be set before building the ChatClient.
    private InetAddress ipAddress;

    public ChatClient.Builder setIpAddress(String string) throws UnknownHostException {
      Preconditions.checkNotNull(string);
      InetAddress inetAddress;
      if (Pattern.matches("localhost.*", string.toLowerCase().trim())) {
        inetAddress = InetAddress.getLocalHost();
      } else {
        inetAddress = InetAddress.getByName(string);
      }

      Preconditions.checkNotNull(inetAddress);
      this.ipAddress = inetAddress;
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

