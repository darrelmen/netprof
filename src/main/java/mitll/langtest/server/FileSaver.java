package mitll.langtest.server;

import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class FileSaver {
  private static final Logger logger = LogManager.getLogger(ScoreServlet.class);

  private static final int BUFFER_SIZE = 4096;

  /**
   * After writing the file, it shouldn't be modified any more.
   *
   * @param inputStream
   * @param realExID
   * @param userid
   * @param language
   * @param makeDir
   * @return
   * @throws IOException
   * @see ScoreServlet#getJsonForAudio
   */
  @NotNull
  public File writeAudioFile(PathHelper pathHelper,
                             InputStream inputStream,
                             int realExID,
                             int userid,
                             Language language,
                             boolean makeDir) throws IOException {
    File saveFile = new File(
        pathHelper.getAbsoluteToAnswer(
            language,
            realExID,
            userid));

    if (makeDir) {
      makeFileSaveDir(saveFile);
    }

    writeToFile(inputStream, saveFile);

    // logger.info("writeAudioFile : wrote file " + saveFile.getAbsolutePath() + " proj " + project + " exid " + realExID + " by " + userid);
    if (!saveFile.setReadOnly()) {
      logger.warn("writeAudioFile huh? can't mark file read only?");
    }

    setParentPermissions(saveFile);

    return saveFile;
  }

  // opt/netprof/answers/turkish/747329/1/subject-6/answer_1542409059474.wav
  private void makeFileSaveDir(File saveFile) {
    File parent = new File(saveFile.getParent());
    boolean mkdirs = parent.mkdirs();
    if (!mkdirs && !parent.exists()) {
      logger.error("Couldn't make " + parent.getAbsolutePath() + " : permissions set? chown done ?");
    }
//    else if (mkdirs || parent.exists()) {
//      boolean b = parent.setReadOnly();
//      if (!b) logger.warn("makeFileSaveDir couldn't set read only on " + parent);
//     // parent.setExecutable()
//    }

//    setParentPermissions(saveFile);

  }

  /**
   * So it should be readable in case another process not run as tomcat wants to read the file.
   * @param saveFile
   */
  // /opt/netprof/answers/turkish/747329/1/subject-6/answer_1542409059474.wav
  private void setParentPermissions(File saveFile) {


    File parentFile = saveFile.getParentFile();

    // /opt/netprof/answers/turkish/747329/1/subject-6

    setPermissions(parentFile);
//    boolean b;
//    boolean b1;


    parentFile = parentFile.getParentFile();
    // /opt/netprof/answers/turkish/747329/1

    setPermissions(parentFile);
//
//    b = parentFile.setReadable(true,false);
//    if (!b) logger.warn("makeFileSaveDir couldn't set read only on " + parentFile);
//    b1 = parentFile.setWritable(true);
//    if (!b1) logger.warn("makeFileSaveDir couldn't set write on " + parentFile);

    parentFile = parentFile.getParentFile();
    // /opt/netprof/answers/turkish/747329
    setPermissions(parentFile);

//    b = parentFile.setReadOnly();
//    if (!b) logger.warn("makeFileSaveDir couldn't set read only on " + parentFile);
//    b1 = parentFile.setWritable(true);
//    if (!b1) logger.warn("makeFileSaveDir couldn't set write on " + parentFile);
  }

  private void setPermissions(File parentFile) {
    boolean b = parentFile.setReadable(true,false);
    if (!b) logger.warn("makeFileSaveDir couldn't set read only on " + parentFile);

    boolean b1 = parentFile.setExecutable(true, false);
    if (!b1) logger.warn("makeFileSaveDir couldn't set exec only on " + parentFile);

//    boolean b1 = parentFile.setWritable(true);
//    if (!b1) logger.warn("makeFileSaveDir couldn't set write on " + parentFile);
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
