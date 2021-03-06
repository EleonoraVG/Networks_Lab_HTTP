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

import static Constants.HTTPConstants.CR;
import static Constants.HTTPConstants.LF;

/**
 * A Helper class for reading bytes from the server,
 * according to the HTTP protocol.
 */
public abstract class HTTPReader {

  public static List<String> readHeader(DataInputStream inputStream) throws IOException {
    List<String> headerStrings = new ArrayList<>();

    // Read until the response is not an empty string.
    byte[] firstLineBytes = readOneLine(inputStream);
    String firstLine = new String(firstLineBytes);
    while (firstLine.isEmpty()) {
      firstLineBytes = readOneLine(inputStream);
      firstLine = new String(firstLineBytes);
    }
    headerStrings.add(firstLine);

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

  /**
   * Read in a request header from the client
   * @param inFromClient the data input stream from the client.
   * @return a request header
   * @throws IOException
   */
   public static RequestHeader readRequestHeader(DataInputStream inFromClient) throws IOException {
    return new RequestHeader(readHeader(inFromClient));
  }

  public static byte[] retrieveContentInRequest(RequestHeader header, DataInputStream inFromClient) throws IOException {
    byte[] content = null;
    if (header.getContentLength() != null) {
      content = HTTPReader.processWithContentLength(inFromClient, header.getContentLength());
    } else if (header.getTransferEncoding() != null) {

      //Use transfer encoding
      content = HTTPReader.readChunkFromServer(inFromClient, Charset.defaultCharset());
    }
    return content;
  }
}
