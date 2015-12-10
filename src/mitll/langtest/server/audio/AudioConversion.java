/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.audio;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.Collection;

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
  private static final float SIXTEEN_K = 16000f;
  private static final float TRIM_SILENCE_BEFORE_AND_AFTER = 0.25f;
  private static final String T_VALUE = "" + 7;
  public static final String LAME = "lame";
  private AudioCheck audioCheck;
  private static final boolean DEBUG = false;

  private File tempDir;
  private final String soxPath;

  /**
   * @param props
   */
  public AudioConversion(ServerProperties props) {
    soxPath = getSox();
    try {
      audioCheck = new AudioCheck(props);
      tempDir = createTempDirectory();
    } catch (IOException e) {
      logger.error("couldn't make temp dir?");
    }
  }

  /**
   * Converts base 64 string into bytes and writes them to a file.
   * This is what we send over from FLASH when recording.  TODO : Ideally we could post the bytes without encoding.
   *
   * @param base64EncodedString      audio bytes from the client
   * @param file                     where we want to write the wav file to
   * @param useSensitiveTooLoudCheck
   * @return true if audio is valid (not too short, not silence)
   * @see AudioFileHelper#writeAudioFile(String, String, CommonExercise, int, int, int, String, boolean, boolean, boolean, String, String, boolean, boolean)
   * @see mitll.langtest.server.audio.AudioFileHelper#getAlignment
   */
  public AudioCheck.ValidityAndDur convertBase64ToAudioFiles(String base64EncodedString, File file, boolean useSensitiveTooLoudCheck) {
    file.getParentFile().mkdirs();
    byte[] byteArray = getBytesFromBase64String(base64EncodedString);

    writeToFile(byteArray, file);

    if (!file.exists()) {
      logger.error("writeAudioFile : huh? can't find " + file.getAbsolutePath());
    }
    AudioCheck.ValidityAndDur valid = isValid(file, useSensitiveTooLoudCheck);
    if (valid.getValidity() == AudioAnswer.Validity.OK) {
      valid.setDuration(trimSilence(file, false).getDuration());
    }
    return valid;
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
    File wavFile = convertTo16Khz(new File(pathname));
    return removeSuffix(wavFile.getName());
  }

  public String getHighPassFilterFile(String wavFile) {
    try {
      return doHighPassFilter(wavFile);
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
    return null;
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
      if (sampleRate != SIXTEEN_K) {
        long then = System.currentTimeMillis();
        //     String convertTo16KHZ = new AudioConverter().convertTo16KHZ(binPath, wavFile.getAbsolutePath());
        String convertTo16KHZ = convertTo16KHZ(wavFile.getAbsolutePath());
        wavFile = copyFile(wavFile, convertTo16KHZ, SIXTEEN_K_SUFFIX);

        long now = System.currentTimeMillis();
        logger.info("convertTo16Khz : took " + (now - then) + " millis to convert original " + orig.getName() + " at " + sampleRate +
            " to 16K wav file : " + wavFile.getName());

      }
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
    return wavFile;
  }

  /**
   * Note that wavFile input will be changed if trim is successful.
   * <p>
   * Trimmed file will be empty if it's not successful.
   *
   * @param wavFile
   * @param makeCopyOfOriginal
   * @return
   * @see #convertBase64ToAudioFiles(String, File, boolean)
   */
  public TrimInfo trimSilence(final File wavFile, boolean makeCopyOfOriginal) {
  //  logger.info("trimSilence " + wavFile.getAbsolutePath());
    if (!wavFile.exists()) {
      logger.error("trimSilence " + wavFile + " doesn't exist");
      return new TrimInfo();
    }
    try {
      long then = System.currentTimeMillis();
      String trimmed = doTrimSilence(wavFile.getAbsolutePath());

      double durationInSeconds = audioCheck.getDurationInSeconds(wavFile);
      double durationInSecondsTrimmed = audioCheck.getDurationInSeconds(trimmed);
      double diff = durationInSeconds - durationInSecondsTrimmed;
      if (durationInSecondsTrimmed > 0.1 && diff > 0.1) {

/*        if (makeCopyOfOriginal) {
          File replacement = new File(wavFile.getParent(), "orig_" + wavFile.getName());
          FileUtils.copyFile(wavFile, replacement);
        }*/

        FileUtils.copyFile(new File(trimmed), wavFile);

        long now = System.currentTimeMillis();
        if (now - then > 0) {
          logger.info("trimSilence : took " + (now - then) + " millis to convert original " + wavFile.getName() +
              " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");
        }
        return new TrimInfo(durationInSecondsTrimmed, true);
      } else {
        long now = System.currentTimeMillis();
/*        logger.info("trimSilence : took " + (now - then) + " millis to NOT convert original " + wavFile.getName() +
            " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");*/

        return new TrimInfo(durationInSeconds, false);
      }
    } catch (IOException e) {
      logger.error("trimSilence on " + wavFile.getAbsolutePath() + " got " + e, e);
      return new TrimInfo();
    }
  }

  public static class TrimInfo {
    private final double duration;
    private final boolean didTrim;

    public TrimInfo() {
      duration = 0;
      didTrim = false;
    }

    public TrimInfo(double duration, boolean didTrim) {
      this.duration = duration;
      this.didTrim = didTrim;
    }

    public double getDuration() {
      return duration;
    }

    public boolean didTrim() {
      return didTrim;
    }

    public String toString() {
      return " dur " + duration + " did trim " + didTrim;
    }
  }

  private File copyFile(final File wavFile, final String sourceFile, final String suffix) throws IOException {
    logger.info("orig " + wavFile.getName() + " source " + sourceFile + " suffix " + suffix);
    String name1 = wavFile.getName();
    String dest = wavFile.getParent() + File.separator + removeSuffix(name1) + suffix + ".wav";

    File replacement = new File(dest);
    File srcFile = new File(sourceFile);
    logger.info("srcFile " + srcFile + " exists " + srcFile.exists());
    FileUtils.copyFile(srcFile, replacement);

    // cleanup
    String parent = srcFile.getParent();
    File file = new File(parent);
    FileUtils.deleteDirectory(file);

    return replacement;
  }

  /**
   * @param pathToAudioFile
   * @return
   * @throws IOException
   * @see #convertTo16Khz(File)
   */
  private String convertTo16KHZ(String pathToAudioFile) throws IOException {
    return sampleAt16KHZ(pathToAudioFile, createTempDirectory());
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

  private String sampleAt16KHZ(String pathToAudioFile, File tempDirectory) throws IOException {
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

  /**
   * sox audio-in audio-out silence 1 0.01 -90d gain -3 highpass 80.0
   *
   * @param pathToAudioFile
   * @return
   * @throws IOException
   */
  private String doHighPassFilter(String pathToAudioFile) throws IOException {
    checkTempDir();
    final String tempForWavz = tempDir + File.separator + "tempHighPass.wav";

    ProcessBuilder soxFirst = new ProcessBuilder(
        soxPath,
        pathToAudioFile,
        tempForWavz,
        "silence", "1", "0.01", "-90d", "gain", "-3", "highpass", "80.0");

    if (!new ProcessRunner().runProcess(soxFirst)) {
      boolean exists = tempDir.exists();
      logger.info("Exists " + exists);
      logger.error("couldn't do high pass filter on " + pathToAudioFile);
      String asRunnable = soxFirst.command().toString().replaceAll(",", " ");
      logger.info("path " + asRunnable);
    }

    return tempForWavz;
  }

  private void checkTempDir() throws IOException {
    if (tempDir == null || !tempDir.exists()) {
      tempDir = createTempDirectory();
    }
  }

  /**
   * TODO: Why all the stuff with a temp dir???
   * creating and deleting???
   * <p>
   * sox $sourcewav $outputwav vad -t 6 -p 0.20 reverse vad -t 6 -p 0.20 reverse
   *
   * @param pathToAudioFile
   * @return
   * @throws IOException
   */
  private String doTrimSilence(String pathToAudioFile) throws IOException {
    checkTempDir();
    final String tempTrimmed = tempDir + File.separator + "tempTrim.wav";
//    logger.info("doTrimSilence running sox on " + pathToAudioFile + " to produce " + tempTrimmed);

    ProcessBuilder soxFirst = new ProcessBuilder(
        getSox(),
        pathToAudioFile,
        tempTrimmed,
        "vad", "-t", T_VALUE, "-p", "" + TRIM_SILENCE_BEFORE_AND_AFTER, "reverse", "vad", "-t", T_VALUE, "-p", "" + TRIM_SILENCE_BEFORE_AND_AFTER, "reverse");

    if (!new ProcessRunner().runProcess(soxFirst)) {
      boolean exists = tempDir.exists();
      logger.info("tempDir Exists " + exists);
      logger.info("pathToAudioFile exists " + new File(pathToAudioFile).exists());
      logger.info("tempTrimmed exists     " + new File(tempTrimmed).exists());
      logger.error("couldn't do trim silence on " + pathToAudioFile);
      String asRunnable = soxFirst.command().toString().replaceAll(",", " ");
      logger.info("path " + asRunnable);
    }

//    String asRunnable = soxFirst.command().toString().replaceAll(",", " ");
    //logger.info("path " + asRunnable);

    return tempTrimmed;
  }

  /**
   * @param wavFile
   * @param rawFile
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#scoreRepeatExercise(String, String, String, Collection, String, int, int, boolean, boolean, String, boolean, String, Result, boolean)
   */
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
      logger.error("Got " + e, e);
    } finally {
      try {
        if (fout != null)
          fout.close();
        if (f != null)
          f.close();
      } catch (IOException e) {
        logger.error("Got " + e, e);
      }
    }
  }

  private String removeSuffix(String name1) {
    return name1.substring(0, name1.length() - 4);
  }

  /**
   * @param pathToWav
   * @param realContextPath
   * @param overwrite
   * @param title
   * @param author
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#ensureMP3
   */
  public String ensureWriteMP3(String pathToWav, String realContextPath, boolean overwrite, String title, String author) {
    if (pathToWav == null || pathToWav.equals("null")) throw new IllegalArgumentException("huh? path is null");
    return writeMP3(pathToWav, realContextPath, overwrite, title, author);
  }

  /**
   * @param pathToWav
   * @param realContextPath
   * @param overwrite
   * @param author
   * @return
   * @see #ensureWriteMP3
   */
  private String writeMP3(String pathToWav, String realContextPath, boolean overwrite, String title, String author) {
    File absolutePathToWav = getAbsoluteFile(pathToWav, realContextPath);

    String mp3File = absolutePathToWav.getAbsolutePath().replace(".wav", ".mp3");
    File mp3 = new File(mp3File);
    if (!mp3.exists() || overwrite) {
      if (DEBUG)
        logger.debug("writeMP3 : doing mp3 conversion for " + absolutePathToWav + " path " + pathToWav + " context " + realContextPath);

      String lamePath = getLame();
      if (DEBUG) logger.debug("run lame on " + absolutePathToWav + " making " + mp3File);

      if (!convertFileAndCheck(lamePath, title, absolutePathToWav.getAbsolutePath(), mp3File, author)) {
        return FILE_MISSING;
      }
    }
    return mp3File;
  }

  private String getSox() {
    String sox = getSox(getBinPath());
    if (!new File(sox).exists()) sox = SOX;
    return sox;
  }

  private String getBinPath() {
    String binPath = WINDOWS_SOX_BIN_DIR;
    if (!new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR;
    if (!new File(binPath).exists()) binPath = LINUX_SOX_BIN_DIR_2;
    return binPath;
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
   * @see PathWriter#getPermanentAudioPath(mitll.langtest.server.PathHelper, File, String, boolean, String, String, String, ServerProperties)
   */
  public void normalizeLevels(File absolutePathToWav) {
    try {
      File tempFile = File.createTempFile("normalized_" + removeSuffix(absolutePathToWav.getName()) + "_" + System.currentTimeMillis(), ".wav");
      //logger.debug("sox conversion from " + absolutePathToWav + " to " + tempFile.getAbsolutePath());

      ProcessBuilder soxFirst2 = new ProcessBuilder(soxPath,
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
    lamePath = LAME;
    return lamePath;
  }

  /**
   * @param pathToWav
   * @return
   * @see MP3Support#getWavForMP3(String, String)
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
    //	}

    return testMP3;
  }

  /**
   * @param lamePath
   * @param title
   * @param pathToAudioFile
   * @param mp3File
   * @param author
   * @return
   * @see #ensureWriteMP3
   * @see #writeMP3
   */
  private boolean convertFileAndCheck(String lamePath, String title, String pathToAudioFile, String mp3File, String author) {
    if (DEBUG) logger.debug("convert " + pathToAudioFile + " to " + mp3File);
    if (title.length() > 30) {
      title = title.substring(0, 30);
    }
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File, "--tt", title, "--ta", author);
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
