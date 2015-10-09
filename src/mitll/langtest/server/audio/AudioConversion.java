package mitll.langtest.server.audio;

import audio.tools.FileCopier;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;

/**
 * Does mp3 conversion using a shell call to lame.
 * Uses ffmpeg to convert to webm format.
 * <p>
 * User: go22670
 * Date: 8/22/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioConversion {
  private static final Logger logger = Logger.getLogger(AudioConversion.class);
  private static final String LAME_PATH_WINDOWS = "C:\\Users\\go22670\\lame\\lame.exe";
  private static final String LAME_PATH_LINUX = "/usr/local/bin/lame";
  private static final String SOX = "sox";

  private static final String LINUX_SOX_BIN_DIR = "/usr/local/bin";
  private static final String LINUX_SOX_BIN_DIR_2 = "/usr/bin";
  private static final String WINDOWS_SOX_BIN_DIR = "C:\\Users\\go22670\\sox-14-3-2";
  public static final String SIXTEEN_K_SUFFIX = "_16K";
  public static final String FILE_MISSING = "FILE_MISSING";
  private final AudioCheck audioCheck = new AudioCheck();
  private static final boolean DEBUG = false;

//  public AudioConversion() {
//  }

  /**
   * Converts base 64 string into bytes and writes them to a file.
   * This is what we send over from FLASH when recording.  TODO : Ideally we could post the bytes without encoding.
   *
   * @param base64EncodedString      audio bytes from the client
   * @param file                     where we want to write the wav file to
   * @param useSensitiveTooLoudCheck
   * @return true if audio is valid (not too short, not silence)
   * @see AudioFileHelper#writeAudioFile(String, String, CommonExercise, int, int, int, String, boolean, boolean, boolean, String, String, boolean, boolean)
   * @see mitll.langtest.server.audio.AudioFileHelper#getAlignment(String, String, String, int)
   */
  public AudioCheck.ValidityAndDur convertBase64ToAudioFiles(String base64EncodedString, File file, boolean useSensitiveTooLoudCheck) {
    file.getParentFile().mkdirs();
    byte[] byteArray = getBytesFromBase64String(base64EncodedString);

    writeToFile(byteArray, file);

    if (!file.exists()) {
      logger.error("writeAudioFile : huh? can't find " + file.getAbsolutePath());
    }
    return isValid(file, useSensitiveTooLoudCheck);
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
      logger.error("got " + e1, e1);
    }
    return decoded;
  }

  private void writeToFile(byte[] byteArray, File file) {
    try {
      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
      outputStream.write(byteArray);
      outputStream.close();
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @param file
   * @param useSensitiveTooLoudCheck
   * @return
   * @see #convertBase64ToAudioFiles(String, File, boolean)
   * @see AudioFileHelper#getAnswer(String, CommonExercise, int, boolean, String, File, String, String, float, int, boolean)
   */
  public AudioCheck.ValidityAndDur isValid(File file, boolean useSensitiveTooLoudCheck) {
    try {
      return useSensitiveTooLoudCheck ? audioCheck.checkWavFileRejectAnyTooLoud(file) : audioCheck.checkWavFile(file);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return new AudioCheck.ValidityAndDur(AudioAnswer.Validity.INVALID);
  }

  /**
   * Checks if the sample rate is not 16K (as required by things like sv).
   * If not, then uses sox to make an audio file with the right sample rate.
   *
   * @param testAudioDir          directory for audio
   * @param testAudioFileNoSuffix name without suffix
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
   */
  public String convertTo16Khz(String testAudioDir, String testAudioFileNoSuffix) throws UnsupportedAudioFileException {
    String pathname = testAudioDir + File.separator + testAudioFileNoSuffix + ".wav";
    File wavFile = new File(pathname);

    wavFile = convertTo16Khz(wavFile);
    String name1 = wavFile.getName();
    return removeSuffix(name1);
  }

  /**
   * @param wavFile
   * @return
   * @throws UnsupportedAudioFileException
   * @see #convertTo16Khz(String, String)
   */
  private File convertTo16Khz(File wavFile) throws UnsupportedAudioFileException {
    if (!wavFile.exists()) {
      logger.error("convertTo16Khz " + wavFile + " doesn't exist");
      return wavFile;
    }
    try {
      AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(wavFile);
      File orig = wavFile;
      float sampleRate = audioFileFormat.getFormat().getSampleRate();
      if (sampleRate != 16000f) {
        long then = System.currentTimeMillis();
        String binPath = getBinPath();
        //     String convertTo16KHZ = new AudioConverter().convertTo16KHZ(binPath, wavFile.getAbsolutePath());
        String convertTo16KHZ = convertTo16KHZ(binPath, wavFile.getAbsolutePath());
        String name1 = wavFile.getName();
        String sampled = wavFile.getParent() + File.separator + removeSuffix(name1) + SIXTEEN_K_SUFFIX + ".wav";
        if (new FileCopier().copy(convertTo16KHZ, sampled)) {
          wavFile = new File(sampled);

          // cleanup
          String parent = new File(convertTo16KHZ).getParent();
          File file = new File(parent);
          FileUtils.deleteDirectory(file);
        }
        long now = System.currentTimeMillis();
        logger.info("convertTo16Khz : took " + (now - then) + " millis to convert original " + orig.getName() + " at " + sampleRate +
            " to 16K wav file : " + wavFile.getName());
      }
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
    return wavFile;
  }

  private String convertTo16KHZ(String binPath, String pathToAudioFile) throws IOException {
    return sampleAt16KHZ(getSox(binPath), pathToAudioFile, createTempDirectory());
  }

  private File createTempDirectory() throws IOException {
    File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

    if (!temp.delete()) {
      throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
    }

    if (!temp.mkdir()) {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    return temp;
  }

  private String sampleAt16KHZ(String soxPath, String pathToAudioFile, File tempDirectory) throws IOException {
    final String tempForWavz = tempDirectory + File.separator + "tempForWavz.wav";

    //log.info("running sox on " + tempForWavz);

    // i.e. sox inputFile -s -2 -c 1 -q tempForWavz.wav rate 16000
    ProcessBuilder soxFirst = new ProcessBuilder(soxPath,
        pathToAudioFile, "-s", "-2", "-c", "1", "-q", tempForWavz, "rate", "16000");
//        logger.info("ENTER running sox on " + tempForWavz + " : " + soxFirst);

    if (!new ProcessRunner().runProcess(soxFirst)) {
      ProcessBuilder soxFirst2 = new ProcessBuilder(soxPath,
          pathToAudioFile, "-c", "1", "-q", tempForWavz, "rate", "16000");
      new ProcessRunner().runProcess(soxFirst2);
    }

    //   log.info("EXIT running sox on " + tempForWavz + " : " + soxFirst);

    return tempForWavz;
  }

  // assumes 16Khz
  public static void wav2raw(String wavFile, String rawFile) {
    FileOutputStream fout = null;
    AudioInputStream f = null;
    try {
      fout = new FileOutputStream(rawFile);
      byte[] b = new byte[1024];
      f = AudioSystem.getAudioInputStream(new File(wavFile));
      int nread = 0;
      do {
        if (nread > 0)
          fout.write(b, 0, nread);
        nread = f.read(b);
      }
      while (nread > 0);
      f.close();
      fout.close();
    } catch (UnsupportedAudioFileException | IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (fout != null)
          fout.close();
        if (f != null)
          f.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

  private String removeSuffix(String name1) {
    return name1.substring(0, name1.length() - 4);
  }

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

  /**
   * @param pathToWav
   * @param realContextPath
   * @param overwrite
   * @param title
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#ensureMP3
   */
  public String ensureWriteMP3(String pathToWav, String realContextPath, boolean overwrite, String title) {
    if (pathToWav == null || pathToWav.equals("null")) throw new IllegalArgumentException("huh? path is null");
    return writeMP3(pathToWav, realContextPath, overwrite, title);
  }

  /**
   * @param pathToWav
   * @param realContextPath
   * @param overwrite
   * @return
   * @see #ensureWriteMP3
   */
  private String writeMP3(String pathToWav, String realContextPath, boolean overwrite, String title) {
    File absolutePathToWav = getAbsoluteFile(pathToWav, realContextPath);

    String mp3File = absolutePathToWav.getAbsolutePath().replace(".wav", ".mp3");
    File mp3 = new File(mp3File);
    if (!mp3.exists() || overwrite) {
      if (DEBUG)
        logger.debug("writeMP3 : doing mp3 conversion for " + absolutePathToWav + " path " + pathToWav + " context " + realContextPath);

      // try {
      //  File tempFile = convertTo48KWav(absolutePathToWav);
      String lamePath = getLame();
      if (DEBUG) logger.debug("run lame on " + absolutePathToWav + " making " + mp3File);

      if (!convertFileAndCheck(lamePath, title, absolutePathToWav.getAbsolutePath(), mp3File)) {
        return FILE_MISSING;
      }
      //  } catch (IOException e) {
      //        logger.error("converting " + pathToWav + " to " + mp3File + " got " + e, e);
      // }
    }
    return mp3File;
  }

  private String getSox() {
    String sox = getSox(getBinPath());
    if (!new File(sox).exists()) sox = "sox";
    return sox;
  }

  private String getSox(String binPath) {
    return getExe(binPath, SOX);
  }

  private String getExe(String binPath, String exe) {
    if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
      return binPath + File.separator + exe + ".exe";
    } else {
      return binPath + File.separator + exe;
    }
  }

  /**
   * sox normalize to -3db -- thanks Paul!
   *
   * @param absolutePathToWav
   * @seex mitll.langtest.server.LangTestDatabaseImpl#normalizeLevel
   * @see PathWriter#getPermanentAudioPath(mitll.langtest.server.PathHelper, java.io.File, String, boolean, String, String)
   */
  public void normalizeLevels(File absolutePathToWav) {
    try {
      File tempFile = File.createTempFile("normalized_" + removeSuffix(absolutePathToWav.getName()) + "_" + System.currentTimeMillis(), ".wav");
      //logger.debug("sox conversion from " + absolutePathToWav + " to " + tempFile.getAbsolutePath());

      ProcessBuilder soxFirst2 = new ProcessBuilder(getSox(),
          absolutePathToWav.getAbsolutePath(),
          tempFile.getAbsolutePath(),
          "gain", "-n", "-3");

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
    if (!new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR;
    if (!new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR_2;
    return binPath;
  }

  private File getAbsoluteFile(String filePath, String realContextPath) {
    return getAbsolute(filePath, realContextPath);
  }

  public boolean exists(String filePath, String realContextPath) {
    return getAbsoluteFile(filePath, realContextPath).exists();
  }

  private File getAbsolute(String filePath, String realContextPath) {
    return new File(realContextPath, filePath);
  }

  private String getLame() {
    String lamePath = LAME_PATH_WINDOWS;    // Windows
    if (!new File(lamePath).exists()) {
      lamePath = LAME_PATH_LINUX;
    }
    if (!new File(lamePath).exists()) {
      logger.error("no lame installed at " + lamePath + " or " + LAME_PATH_WINDOWS);
    }
    lamePath = "lame";
    return lamePath;
  }

  /**
   * @param pathToWav
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#getWavForMP3(String, String)
   */
  public File convertMP3ToWav(String pathToWav) {
    assert (pathToWav.endsWith(".mp3"));
    String mp3File = pathToWav.replace(".mp3", ".wav");
    String lamePath = getLame();


		/*    System.out.println("convertMP3ToWav using " +lamePath +" audio :'" +pathToWav +
        "' out '" +mp3File+
        "' = '" +file.getName()+
        "'");*/
    return writeWavFromMP3(lamePath, pathToWav, mp3File);
  }

  private File writeWavFromMP3(String lamePath, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, "--decode", pathToAudioFile, mp3File);
    try {
      //    System.out.println("convertFileAndCheck running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //     System.out.println("convertFileAndCheck exited  lame" + lameProc);
    } catch (IOException e) {
      logger.error("Couldn't run " + lameProc);
      logger.error("got " + e, e);
    }

    File testMP3 = new File(mp3File);

    //if (!testMP3.exists()) {
    //  logger.error("didn't write MP3 : " + testMP3.getAbsolutePath());
    //	} else {
    //    System.out.println("Wrote to " + testMP3 + " length " + testMP3.getTotalSpace());
    //	}

    return testMP3;
  }

  /**
   * @param lamePath
   * @param title
   * @param pathToAudioFile
   * @param mp3File
   * @return
   * @see #ensureWriteMP3
   * @see #writeMP3
   */
  private boolean convertFileAndCheck(String lamePath, String title, String pathToAudioFile, String mp3File) {
    if (DEBUG) logger.debug("convert " + pathToAudioFile + " to " + mp3File);
    // " --tt \""+title +"\" "
    if (title.length() > 30) {
      title = title.substring(0, 30);
    }
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File, "--tt", "" + title + "");
    try {
      //logger.debug("running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
    } catch (IOException e) {
      //  logger.error("Couldn't run " + lameProc);
      logger.error("for " + lameProc + " got " + e, e);
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      if (!new File(pathToAudioFile).exists()) {
        logger.error("huh? source file " + pathToAudioFile + " doesn't exist?");
      } else {
        logger.error("didn't write MP3 : " + testMP3.getAbsolutePath() +
            " exe path " + lamePath +
            " command was " + lameProc.command());
        try {
          if (!new ProcessRunner().runProcess(lameProc, true)) {
            return false;
          }
        } catch (IOException e) {
          logger.error("for " + lameProc + " got " + e, e);
        }

      }
      return false;
    }
    return true;
  }
}
