import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileProcessor {

  public static void writeToFile(String content, String directoryPath, String fileName) {
    //Create the directories if they don't exist.
    File directory = new File(directoryPath);
    if (!directory.exists()){
      directory.mkdirs();
    }
    writeToFile(content, new File(directoryPath+fileName));
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
}
