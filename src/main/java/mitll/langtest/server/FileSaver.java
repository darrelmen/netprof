package mitll.langtest.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletInputStream;
import java.io.*;

public class FileSaver {
  private static final int BUFFER_SIZE = 4096;

  private static final Logger logger = LogManager.getLogger(ScoreServlet.class);

  /**
   * After writing the file, it shouldn't be modified any more.
   *
   * @param inputStream
   * @param realExID
   * @param userid
   * @return
   * @throws IOException
   * @see ScoreServlet#getJsonForAudio
   */
  @NotNull
  public File writeAudioFile(PathHelper pathHelper, ServletInputStream inputStream,  int realExID, int userid,
                              String language)
      throws IOException {
    String wavPath = pathHelper.getAbsoluteToAnswer(
        language,
        realExID,
        userid);
    File saveFile = new File(wavPath);
    makeFileSaveDir(saveFile);

    writeToFile(inputStream, saveFile);

    // logger.info("writeAudioFile : wrote file " + saveFile.getAbsolutePath() + " proj " + project + " exid " + realExID + " by " + userid);
    if (!saveFile.setReadOnly()) {
      logger.warn("writeAudioFile huh? can't mark file read only?");
    }

    return saveFile;
  }

  private void makeFileSaveDir(File saveFile) {
    File parent = new File(saveFile.getParent());
    boolean mkdirs = parent.mkdirs();
    if (!mkdirs && !parent.exists()) {
      logger.error("Couldn't make " + parent.getAbsolutePath() + " : permissions set? chown done ?");
    }
  }

  /**
   * @param inputStream
   * @param saveFile
   * @throws IOException
   */
  private void writeToFile(InputStream inputStream, File saveFile) throws IOException {
    // opens an output stream for writing file
    copyToOutput(inputStream, new FileOutputStream(saveFile));
  }

  /**
   * TODO replace with commons call
   *
   * @param inputStream
   * @param outputStream
   * @throws IOException
   */
  private void copyToOutput(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }

    outputStream.close();
    inputStream.close();
  }

}
