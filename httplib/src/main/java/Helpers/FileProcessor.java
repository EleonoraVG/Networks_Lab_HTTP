package Helpers;

import com.google.common.io.Files;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileProcessor {

  public static File createFileWithDirs(String filePath) {
    //Create the directories if they don't exist.
    File file = new File(filePath);
    file.getParentFile().mkdirs();
    return file;
  }

  public static void writeToFile(byte[] content, String filePath) throws IOException {
    File file = createFileWithDirs(filePath);
    Files.write(content, file);
  }

  public static void writeToFile(String content, String filePath) {
    File file = createFileWithDirs(filePath);
    writeToFile(content, file);
  }

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

  public static void writeImageToFile(BufferedImage image, String imageType, String path) throws IOException {
    ImageIO.write(image, imageType, createFileWithDirs(path));
  }
}
