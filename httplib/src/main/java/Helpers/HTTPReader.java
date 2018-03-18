package Helpers;

import Objects.RequestHeader;
import Objects.ServerResponse.ResponseHeader;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static main.java.HTTPConstants;

/**
 * A Helper class for reading bytes from the server,
 * according to the HTTP protocol.
 */
public abstract class HTTPReader {

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
   * @param inFromServer
   * @return A byte array containing he contents of the chunk.
   * @throws IOException
   */
  public static byte[] readChunkFromServer(DataInputStream inFromServer, Charset charset) throws IOException {

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
  public static byte[] processWithContentLength(DataInputStream inFromServer, int contentLength) throws IOException {
    byte[] result = new byte[contentLength];
    inFromServer.readFully(result);
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
      if (i == HTTPConstants.CR) {
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
