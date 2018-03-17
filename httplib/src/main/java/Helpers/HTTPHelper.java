package Helpers;

import Objects.ServerResponse.ResponseHeader;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//TODO: Comments
public abstract class HTTPHelper {
  private static final Character CR = '\r';
  private static final Character LF = '\n';

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

  public static ServerSocket startServerSocket(InetAddress ipAddress, int port) throws IOException {
    int backlog = 50;  // The maximum queue length for incomming connection indications.
    return new ServerSocket(port, backlog, ipAddress);
  }

  public static ResponseHeader readHeader(DataInputStream inFromServer) throws IOException {
    List<String> headerStrings = new ArrayList<>();
    boolean headerDone = false;
    while (!headerDone) {
      byte[] line = readOneLine(inFromServer);
      if (line.length == 0) {
        headerDone = true;
      }
      headerStrings.add(new String(line, StandardCharsets.UTF_8));
    }

    //Build the response header of the server response.
    return new ResponseHeader(headerStrings);
  }

  /**
   * Read bytes from the server until the end of a line.
   *
   * @param inFromServer
   * @return a byte array containing the bytes of one line.
   * @throws IOException
   */
  public static byte[] readOneLine(DataInputStream inFromServer) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int i;
    while ((i = inFromServer.read()) != -1) {

      // Check for the end of a line.
      boolean lineEnded = false;
      if (i == CR) {
        int nextChar = inFromServer.read();
        if (nextChar == LF) {
          lineEnded = true;
        }
      } else if (i == LF) {
        lineEnded = true;
      }

      // Break if the line ended.
      if (lineEnded) {
        break;
      }
      // Write to the byteArrayOutputSteam otherwise.
      byteArrayOutputStream.write(i);
    }
    byteArrayOutputStream.flush();
    byteArrayOutputStream.close();
    return byteArrayOutputStream.toByteArray();
  }
}
