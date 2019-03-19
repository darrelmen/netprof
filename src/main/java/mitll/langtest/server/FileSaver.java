/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server;

import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class FileSaver {
  private static final Logger logger = LogManager.getLogger(ScoreServlet.class);

  private static final int BUFFER_SIZE = 4096;
  // about 30 seconds at 16K and sample rate...
  private static final int ONE_MG = 1048576;  // 1 M

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
  public FileSaveResponse writeAudioFile(PathHelper pathHelper,
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

    FileSaveResponse fileSaveResponse = writeToFile(inputStream, saveFile);

    logger.info("writeAudioFile : wrote file " + saveFile.getAbsolutePath() + " exid " + realExID + " by " + userid);
    if (!saveFile.setReadOnly()) {
      logger.warn("writeAudioFile huh? can't mark file read only?");
    }

    setParentPermissions(saveFile);

    return fileSaveResponse;
  }

  public static class FileSaveResponse {
    private File file;

    enum STATUS {OK, TOO_BIG;}

    private STATUS status;

    FileSaveResponse(File file, STATUS status) {
      this.file = file;
      this.status = status;
    }

    public File getFile() {
      return file;
    }

    public STATUS getStatus() {
      return status;
    }

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
   *
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
    boolean b = parentFile.setReadable(true, false);
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
  private FileSaveResponse writeToFile(InputStream inputStream, File saveFile) throws IOException {
    // opens an output stream for writing file
    boolean b = copyToOutput(inputStream, new FileOutputStream(saveFile), ONE_MG);
    return new FileSaveResponse(saveFile, b ? FileSaveResponse.STATUS.TOO_BIG : FileSaveResponse.STATUS.OK);
  }

  /**
   * TODO replace with commons call
   *
   * @param inputStream
   * @param outputStream
   * @param maxBytes
   * @return true if file too big
   * @throws IOException
   */
  private boolean copyToOutput(InputStream inputStream, OutputStream outputStream, int maxBytes) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    int total = 0;
    boolean fileTooBig = false;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
      total += bytesRead;
      if (total > maxBytes) {
        logger.warn("copyToOutput : file too big - stopping!");
        fileTooBig = true;
        break;
      }
    }

    outputStream.close();
    inputStream.close();
    return fileTooBig;
  }
}
