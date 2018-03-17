package Helpers;

import Objects.RequestHeader;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//TODO: Comments
public abstract class HTTPReader {
  private static final Character CR = '\r';
  private static final Character LF = '\n';


  public static ServerSocket startServerSocket(InetAddress ipAddress, int port) throws IOException {
    int backlog = 50;  // The maximum queue length for incomming connection indications.
    return new ServerSocket(port, backlog, ipAddress);
  }




  public static List<String> readHeader(DataInputStream inputStream) throws IOException {
    List<String> headerStrings = new ArrayList<>();
    boolean headerDone = false;
    while (!headerDone) {
      byte[] line = readOneLine(inputStream);
      if (line.length == 0) {
        headerDone = true;
      }
      headerStrings.add(new String(line, StandardCharsets.UTF_8));
    }
    return headerStrings;
  }

  /**
   * Read in one chunk in from the server.
   *
   * @param inputStream
   * @return A byte array containing he contents of the chunk.
   * @throws IOException
   */
  public static byte[] readChunkFromServer(DataInputStream inputStream, Charset charset) throws IOException {

    // Read the first line
    byte[] firstLineBytes = readOneLine(inputStream);
    String firstLine = new String(firstLineBytes, charset);
    String lengthString = firstLine.split(";")[0].trim();

    // Sometimes the first response is an empty string.
    if (firstLine.equals("")) {
      byte[] num = readOneLine(inputStream);
      firstLine = new String(num, charset);
      lengthString = firstLine.split(";")[0].trim();
    }

    // Retrieve the message length.
    int messageLength = Integer.parseInt(lengthString, 16);

    // Read the content of the chunk from the server.
    byte[] result = new byte[messageLength];
    inputStream.readFully(result);

    return result;
  }

  /**
   * Process the content of a response when given the content length.
   *
   * @param inputStream
   * @param contentLength
   * @return
   * @throws IOException
   */
  public static byte[] processWithContentLength(DataInputStream inputStream, int contentLength) throws IOException {
    byte[] result = new byte[contentLength];
    inputStream.readFully(result);
    return result;
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
