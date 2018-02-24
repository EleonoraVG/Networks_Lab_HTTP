import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;

public class TCPClient {

  private static final String HTTP_VERSION = "HTTP/1.0";
  private static final String CR = "\r";
  private static final String LF = "\n";
  private static final String SPACE = " ";

  private int port;
  private InetAddress ipAddress;
  private Optional<Socket> clientSocket;

  private TCPClient() {
  }

  /**
   * Create a new Builder for TCPClient.
   * The builder is used to build an instance of TCPClient.
   *
   * @return TCPClient.Builder
   */
  public static TCPClient.Builder newBuilder() {
    return new Builder();
  }

  /**
   * Start the connection to the server, run the TCPClient and subsequently close the connection.
   *
   * @param command
   * @return
   * @throws IOException
   */
  public TCPClient run(HttpCommand command) throws IOException {
    startConnection();
    String resultInitialCommand = executeCommand(command, ipAddress.getHostName() + "/");
    // TODO List<String> embeddedObjectPaths = findEmbeddedObjects();
    closeConnection();
    return this;
  }

  /**
   * @param command HTTP command send to the server
   * @param path    argument of the HTTP command to the server
   * @return the response string from the server
   * @throws IOException propagated by methods called
   */
  private String executeCommand(HttpCommand command, String path) throws IOException {
    Preconditions.checkArgument(clientSocket.isPresent());
    DataOutputStream outToServer = new DataOutputStream(clientSocket.get().getOutputStream());
    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.get().getInputStream()));
    String sentence = createCommandString(command, path);

    outToServer.writeBytes(sentence); // Write to the server
    outToServer.flush();
    StringBuilder stringBuilder = new StringBuilder();
    String lastLine = "";
    while (true) {
      lastLine = inFromServer.readLine(); // Read from server
      if (lastLine == null) {
        // The server has finished the command.
        break;
      }
      stringBuilder.append(lastLine).append(LF);
    }
    String result = stringBuilder.toString();
    System.out.println(result);
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
    String initialRequestLine = command.toString() + SPACE + path + SPACE + HTTP_VERSION + CR + LF + CR + LF;
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
    clientSocket = Optional.of(startConnectionSocket(ipAddress, port));
  }

  /**
   * Close the connection and set the client socket to empty,
   * because a socket cannot be reopened after closing.
   *
   * @throws IOException
   */
  private void closeConnection() throws IOException {
    Preconditions.checkArgument(clientSocket.isPresent());
    clientSocket.get().close();
    clientSocket = Optional.empty();
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
   * Builder class for setting up the TCPClient class.
   */
  public static class Builder {
    private int port = 80; // The default port is 80
    private InetAddress ipAddress;

    public TCPClient.Builder setIpAddress(String string) throws UnknownHostException {
      InetAddress ipAddress = InetAddress.getByName(string);
      checkIpAddressPreconditions(ipAddress);
      this.ipAddress = ipAddress;
      return this;
    }

    public TCPClient.Builder setPort(int port) {
      checkPortPreconditions(port);
      this.port = port;
      return this;
    }

    public TCPClient build() {
      return new TCPClient().setIpAddress(ipAddress).setPort(port);
    }
  }

  /**
   * Check the that the port is in the expected range from 0 to 65535.
   */
  private static void checkPortPreconditions(int port) {
    Preconditions.checkArgument(port >= 0);
    Preconditions.checkArgument(port <= 65535); //Do not exceed the maximum port number according to RFC 793
  }

  /**
   * Check that the given ip address is not null.
   */
  private static void checkIpAddressPreconditions(InetAddress ipAddress) {
    Preconditions.checkNotNull(ipAddress);
  }

  public TCPClient setPort(int port) {
    checkPortPreconditions(port);
    this.port = port;
    return this;
  }

  public TCPClient setIpAddress(InetAddress ipAddress) {
    checkIpAddressPreconditions(ipAddress);
    this.ipAddress = ipAddress;
    return this;
  }
}
