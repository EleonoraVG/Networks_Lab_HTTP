package Helpers;

import com.google.common.io.Files;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileProcessor {
  /**
   * Create a file at the given location,
   * If the directory for the file does not exist then it will be created
   * @param filePath
   * @return
   */
  private static File createFileWithDirs(String filePath) {
    //Create the directories if they don't exist.
    File file = new File(filePath);
    file.getParentFile().mkdirs();
    return file;
  }

  /**
   * Append to an existing file.
   */
  public static void appendToFile(byte[] content, String existingFilePath) throws IOException{
   java.nio.file.Files.write(Paths.get(existingFilePath),content, StandardOpenOption.APPEND);
  }

  /**
   * Write the byte contents to a file at the given location.
   * Create new directories if necessary.
   * @param content
   * @param filePath
   * @throws IOException
   */
  public static void writeToFile(byte[] content, String filePath) throws IOException {
    File file = createFileWithDirs(filePath);
    Files.write(content, file);
  }

  /**
   * Write the String to a file at the given location.
   * Create new directories if necessary.
   * @param content
   * @param filePath
   */
  public static void writeToFile(String content, String filePath) {
    File file = createFileWithDirs(filePath);
    writeToFile(content, file);
  }

  /**
   * Write String contents to a given file.
   * @param content
   * @param file
   */
  public static void writeToFile(String content, File file) {
    try {
      FileWriter writer = new FileWriter(file, false);
      writer.write(content);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * Write an image to file using the imageIO write.
   * @param image
   * @param imageType
   * @param path
   * @throws IOException
   */
  public static void writeImageToFile(BufferedImage image, String imageType, String path) throws IOException {
    ImageIO.write(image, imageType, createFileWithDirs(path));
  }
}
