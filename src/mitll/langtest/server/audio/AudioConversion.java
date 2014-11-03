package mitll.langtest.server.audio;

import audio.imagewriter.AudioConverter;
import audio.tools.FileCopier;
import mitll.langtest.client.AudioTag;
import mitll.langtest.shared.AudioAnswer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Does mp3 conversion using a shell call to lame.
 *  Uses ffmpeg to convert to webm format.
 *
 * User: go22670
 * Date: 8/22/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioConversion {
  private static final Logger logger = Logger.getLogger(AudioConversion.class);
  private static final String LAME_PATH_WINDOWS = "C:\\Users\\go22670\\lame\\lame.exe";
  private static final String LAME_PATH_LINUX = "/usr/local/bin/lame";

  private static final String LINUX_SOX_BIN_DIR = "/usr/local/bin";
  private static final String LINUX_SOX_BIN_DIR_2 = "/usr/bin";
  private static final String WINDOWS_SOX_BIN_DIR = "C:\\Users\\go22670\\sox-14-3-2";
  public static final String SIXTEEN_K_SUFFIX = "_16K";
  public static final String FILE_MISSING = "FILE_MISSING";
 // private File oggEncoder;
  private final AudioCheck audioCheck = new AudioCheck();
  private static final boolean DEBUG = false;

  public AudioConversion() {
/*    String os = getOS();
    if (os.equals("linux")) {
      File oggEnc = new File(LINUX_SOX_BIN_DIR_2, "oggenc");
      if (!oggEnc.exists()) {
        oggEnc = new File(LINUX_SOX_BIN_DIR, "oggenc");
        setOggEncoder(os, oggEnc);
      }
      else {
        setOggEncoder(os, oggEnc);
      }
    } else if (os.equals("win")) {
      File oggEnc = new File("bin" + File.separator + "win32", "oggenc2.exe");
      setOggEncoder(os, oggEnc);

    } else if (os.equals("macos")) {
      File oggEnc = new File("bin" + File.separator + "macos", "oggenc");
      setOggEncoder(os, oggEnc);
    }*/
  }

/*
  private void setOggEncoder(String os, File oggEnc) {
    if (!oggEnc.exists()) {
      logger.error("huh? " + os +
          " can't find oggenc at " + oggEnc.getAbsolutePath());
    }
    else {
      oggEncoder = oggEnc;
    }
  }
*/

/*

  private String getOS() {
    String property = System.getProperty("os.name").toLowerCase();
    return property.contains("win") ? "win32" : property
        .contains("mac") ? "macos"
        : property.contains("linux") ? System
        .getProperty("os.arch").contains("64") ? "linux64"
        : "linux" : "linux";
  }
*/


  /**
   * Converts base 64 string into bytes and writes them to a file.
   * This is what we send over from FLASH when recording.  TODO : Ideally we could post the bytes without encoding.
   *
   * @see AudioFileHelper#writeAudioFile(String, String, mitll.langtest.shared.CommonExercise, int, int, int, String, boolean, boolean, boolean, String, String)
   * @see mitll.langtest.server.audio.AudioFileHelper#getAlignment(String, String, String, int)
   * @param base64EncodedString audio bytes from the client
   * @param file where we want to write the wav file to
   * @return true if audio is valid (not too short, not silence)
   */
  public AudioCheck.ValidityAndDur convertBase64ToAudioFiles(String base64EncodedString, File file) {
    file.getParentFile().mkdirs();
    byte[] byteArray = getBytesFromBase64String(base64EncodedString);

    writeToFile(byteArray, file);

    if (!file.exists()) {
      System.err.println("writeAudioFile : huh? can't find " + file.getAbsolutePath());
    }
    return isValid(file);
  }

  /**
   * Decode Base64 string
   *
   * @param base64EncodedByteArray
   * @return
   */
  private byte[] getBytesFromBase64String(String base64EncodedByteArray) {
    Base64 decoder = new Base64();
    byte[] decoded = null;
    //System.out.println("postArray : got " + base64EncodedByteArray.substring(0,Math.min(base64EncodedByteArray.length(), 20)) +"...");

    try {
      decoded = (byte[]) decoder.decode(base64EncodedByteArray);
    } catch (Exception e1) {   // just b/c eclipse seems to insist
      logger.error("got " +e1,e1);
    }
    return decoded;
  }

  private void writeToFile(byte[] byteArray, File file) {
    try {
      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
      outputStream.write(byteArray);
      outputStream.close();
    } catch (Exception e) {
      logger.error("got " +e,e);
    }
  }

  /**
   * @see #convertBase64ToAudioFiles(String, java.io.File)
   * @param file
   * @return
   */
  public AudioCheck.ValidityAndDur isValid(File file) {
    try {
      AudioCheck.ValidityAndDur validityAndDur = audioCheck.checkWavFile(file);
      return validityAndDur;
    } catch (Exception e) {
      logger.error("got " +e,e);
    }
    return new AudioCheck.ValidityAndDur(AudioAnswer.Validity.INVALID);
  }

  /**
   * Checks if the sample rate is not 16K (as required by things like sv).
   * If not, then uses sox to make an audio file with the right sample rate.
   *
   * @param testAudioDir directory for audio
   * @param testAudioFileNoSuffix name without suffix
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
   * @return
   */
  public String convertTo16Khz(String testAudioDir, String testAudioFileNoSuffix) throws UnsupportedAudioFileException {
    String pathname = testAudioDir + File.separator + testAudioFileNoSuffix + ".wav";
    File wavFile = new File(pathname);

    wavFile = convertTo16Khz(wavFile);
    String name1 = wavFile.getName();
    return removeSuffix(name1);
  }

  /**
   * @see #convertTo16Khz(String, String)
   * @param wavFile
   * @return
   * @throws UnsupportedAudioFileException
   */
  private File convertTo16Khz(File wavFile) throws UnsupportedAudioFileException {
    if (!wavFile.exists()) {
      System.err.println("convertTo16Khz " + wavFile + " doesn't exist");
      return wavFile;
    }
    try {
      AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(wavFile);
      float sampleRate = audioFileFormat.getFormat().getSampleRate();
      if (sampleRate != 16000f) {
        String binPath = getBinPath();
        String convertTo16KHZ = new AudioConverter().convertTo16KHZ(binPath, wavFile.getAbsolutePath());
        String name1 = wavFile.getName();
        String sampled = wavFile.getParent() + File.separator + removeSuffix(name1) + SIXTEEN_K_SUFFIX + ".wav";
        if (new FileCopier().copy(convertTo16KHZ, sampled)) {
          wavFile = new File(sampled);

          // cleanup
          String parent = new File(convertTo16KHZ).getParent();
          File file = new File(parent);
          FileUtils.deleteDirectory(file);
        }
      }
    } catch (IOException e) {
      logger.error("Got " +e,e);
    }
    return wavFile;
  }

  private String removeSuffix(String name1) {
    return name1.substring(0, name1.length() - 4);
  }

/*  private boolean writeOGG(String pathToWav) {
    String oggFile = pathToWav.replace(".wav",".ogg");
    return oggEncoder != null && convertFileAndCheck(oggEncoder.getAbsolutePath(), pathToWav, oggFile);

  }*/

  /**
   * Remember to resample wav to 48K before doing lame on it.
   * This is required b/c soundmanager doesn't do audio segment playing properly otherwise (it plays the wrong
   * part of the file.)
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#ensureMP3
   * @see mitll.langtest.server.DatabaseServlet#ensureMP3
   * @see mitll.langtest.server.audio.PathWriter#ensureMP3(mitll.langtest.server.PathHelper, String, boolean)
   * @param pathToWav
   * @param realContextPath
   * @param overwrite
   */
  public String ensureWriteMP3(String pathToWav, String realContextPath, boolean overwrite) {
    if (pathToWav == null || pathToWav.equals("null")) throw new IllegalArgumentException("huh? path is null");

/*    if (AudioTag.COMPRESSED_TYPE.equals("ogg")) {
      writeOGG(pathToWav);
      return pathToWav;
    }
    else {*/
      return writeMP3(pathToWav, realContextPath, overwrite);
  //  }
  }

  /**
   * @see #ensureWriteMP3(String, String, boolean)
   * @param pathToWav
   * @param realContextPath
   * @param overwrite
   * @return
   */
  private String writeMP3(String pathToWav, String realContextPath, boolean overwrite) {
    File absolutePathToWav = getAbsoluteFile(pathToWav, realContextPath);

    String mp3File = absolutePathToWav.getAbsolutePath().replace(".wav", ".mp3");
    File mp3 = new File(mp3File);
    if (!mp3.exists() || overwrite) {
      if (DEBUG) logger.debug("writeMP3 : doing mp3 conversion for " + absolutePathToWav + " path " + pathToWav + " context " + realContextPath);

      try {
        File tempFile = File.createTempFile("fortyEightK",".wav");
       // logger.debug("sox conversion from " + absolutePathToWav + " to " + tempFile.getAbsolutePath());
        ProcessBuilder soxFirst = new ProcessBuilder(getSox(),
            absolutePathToWav.getAbsolutePath(), "-s", "-2", "-c", "1", "-q",tempFile.getAbsolutePath(), "rate", "48000");

        new ProcessRunner().runProcess(soxFirst);

        if (!tempFile.exists()) logger.error("didn't make " + tempFile);

        String lamePath = getLame();
        if (DEBUG)  logger.debug("run lame on " + tempFile + " making " + mp3File);
        if (!convertFileAndCheck(lamePath, tempFile.getAbsolutePath(), mp3File)) {
          return FILE_MISSING;
        }
      } catch (IOException e) {
        logger.error("converting " + pathToWav + " to " + mp3File + " got " + e,e);
      }
    }
    return mp3File;
  }

  private String getSox() {
    return new AudioConverter().getSox(getBinPath());
  }

  /**
   * sox normalize to -3db -- thanks Paul!
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#normalizeLevel(mitll.langtest.shared.AudioAnswer)
   * @see mitll.langtest.server.audio.PathWriter#getPermanentAudioPath(mitll.langtest.server.PathHelper, java.io.File, String, boolean, String)
   * @param absolutePathToWav
   */
  public void normalizeLevels(File absolutePathToWav) {
    try {
      File tempFile = File.createTempFile("normalized_" + removeSuffix(absolutePathToWav.getName()) + "_" + System.currentTimeMillis(), ".wav");
      //logger.debug("sox conversion from " + absolutePathToWav + " to " + tempFile.getAbsolutePath());

      ProcessBuilder soxFirst2 = new ProcessBuilder(getSox(),
        absolutePathToWav.getAbsolutePath(),
        tempFile.getAbsolutePath(),
        "gain","-n", "-3");

      new ProcessRunner().runProcess(soxFirst2);

      if (!tempFile.exists() || tempFile.length() == 0) {
        logger.error("didn't make " + tempFile);
        logger.error("soxFirst " + soxFirst2.command());
      }
      //else {
        //logger.debug("wrote normalized to " + tempFile.getAbsolutePath());
      //}

      FileUtils.copyFile(tempFile, absolutePathToWav);
    } catch (IOException e) {
      logger.error("normalizing " + absolutePathToWav + " got " + e, e);
    }
  }

  private String getBinPath() {
    String binPath = WINDOWS_SOX_BIN_DIR;
    if (! new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR;
    if (! new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR_2;
    return binPath;
  }

  private File getAbsoluteFile(String filePath,  String realContextPath) {
    return getAbsolute(filePath, realContextPath);
  }

  public boolean exists(String filePath, String realContextPath) {
    return getAbsoluteFile(filePath, realContextPath).exists();
  }

  private File getAbsolute(String filePath, String realContextPath) {
    File file = new File(realContextPath, filePath);
//    assert(file.exists());
    return file;
  }

  /**
   * Use lame to write an mp3 file.
   * @param pathToWav
   * @see #convertBase64ToAudioFiles(String, java.io.File)
   */
  private void writeMP3(String pathToWav) {
    String mp3File = pathToWav.replace(".wav", ".mp3");
    String lamePath = getLame();

    if (DEBUG) logger.debug("using " + lamePath + " audio :'" + pathToWav +
      "' mp3 '" + mp3File +
      "'");
    convertFileAndCheck(lamePath, pathToWav, mp3File);
  }

  private String getLame() {
    String lamePath = LAME_PATH_WINDOWS;    // Windows
    if (!new File(lamePath).exists()) {
      lamePath = LAME_PATH_LINUX;
    }
    if (!new File(lamePath).exists()) {
      System.err.println("no lame installed at " + lamePath + " or " +LAME_PATH_WINDOWS);
    }
    return lamePath;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getWavForMP3(String, String)
   * @param pathToWav
   * @return
   */
  public File convertMP3ToWav(String pathToWav) {
    assert(pathToWav.endsWith(".mp3"));
    String mp3File = pathToWav.replace(".mp3",".wav");
    String lamePath = getLame();


    File file = writeWavFromMP3(lamePath, pathToWav, mp3File);
/*    System.out.println("convertMP3ToWav using " +lamePath +" audio :'" +pathToWav +
        "' out '" +mp3File+
        "' = '" +file.getName()+
        "'");*/
    return file;
  }

  private File writeWavFromMP3(String lamePath, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, "--decode", pathToAudioFile, mp3File);
    try {
      //    System.out.println("convertFileAndCheck running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //     System.out.println("convertFileAndCheck exited  lame" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      logger.error("got " +e,e);
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      //  System.err.println("didn't write MP3 : " + testMP3.getAbsolutePath());
    } else {
      //    System.out.println("Wrote to " + testMP3 + " length " + testMP3.getTotalSpace());
    }
    return testMP3;
  }

  /**
   * @see #ensureWriteMP3(String, String, boolean)
   * @see #writeMP3(String)
   * @param lamePath
   * @param pathToAudioFile
   * @param mp3File
   * @return
   */
  private boolean convertFileAndCheck(String lamePath, String pathToAudioFile, String mp3File) {
    if (DEBUG) logger.debug("convert " + pathToAudioFile + " to " + mp3File);
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File);
    try {
      //logger.debug("running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //     System.out.println("convertFileAndCheck exited  lame" + lameProc);
    } catch (IOException e) {
      System.err.println("Couldn't run " + lameProc);
      logger.error("got " +e,e);
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      if (!new File(pathToAudioFile).exists()) {
        logger.error("huh? source file " + pathToAudioFile + " doesn't exist?");
      }
      else {
        logger.error("didn't write MP3 : " + testMP3.getAbsolutePath() +
          " exe path " + lamePath +
          " command was " + lameProc.command());
        //new Exception().printStackTrace();
      }
      return false;
    }// else {
      //logger.debug("Wrote to " + testMP3 + " length " + testMP3.getTotalSpace());
   // }
    return true;
  }
}
