/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 */

package mitll.langtest.server.audio;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.answer.AudioAnswer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioConversion {
  private static final Logger logger = LogManager.getLogger(AudioConversion.class);
  private static final String LAME_PATH_WINDOWS = "lame.exe";
  private static final String LAME_PATH_LINUX = "/usr/local/bin/lame";
  private static final String SOX = "sox";

  private static final String LINUX_SOX_BIN_DIR = "/usr/local/bin";
  private static final String LINUX_SOX_BIN_DIR_2 = "/usr/bin";
  private static final String WINDOWS_SOX_BIN_DIR = "sox-14-3-2";
  public static final String SIXTEEN_K_SUFFIX = "_16K";
  public static final String FILE_MISSING = "FILE_MISSING";
  private static final float SIXTEEN_K = 16000f;
  private static final String T_VALUE = "" + 7;
  private static final String LAME = "lame";
  private static final double DIFF_THRESHOLD = 0.2;
  private static final boolean SPEW = true;
  private static final String MP3 = ".mp3";
  public static final String WAV = ".wav";
  public static final String OGG = ".ogg";
  private final AudioCheck audioCheck;
  private static final boolean DEBUG = false;
  private static final int MIN_WARN_DUR = 30;

  private final String soxPath;
  private final long trimMillisBefore;
  private final long trimMillisAfter;

  /**
   * @param props
   * @see mitll.langtest.server.DatabaseServlet#ensureMP3(String, PathHelper, String, String, String)
   */
  public AudioConversion(ServerProperties props) {
    trimMillisBefore = props.getTrimBefore();
    trimMillisAfter = props.getTrimAfter();
    soxPath = getSox();
    audioCheck = new AudioCheck(props);
    setLame();
  }

  /**
   * Converts base 64 string into bytes and writes them to a file.
   * This is what we send over from FLASH when recording.  TODO : Ideally we could post the bytes without encoding.
   *
   * @param base64EncodedString      audio bytes from the client
   * @param file                     where we want to write the wav file to
   * @param useSensitiveTooLoudCheck
   * @return true if audio is valid (not too short, not silence)
   * @see AudioFileHelper#writeAudioFile
   * @see mitll.langtest.server.audio.AudioFileHelper#getAlignment
   */
  AudioCheck.ValidityAndDur convertBase64ToAudioFiles(String base64EncodedString,
                                                      File file,
                                                      boolean useSensitiveTooLoudCheck,
                                                      boolean quietAudioOK) {
    long then = System.currentTimeMillis();
    file.getParentFile().mkdirs();

    if (DEBUG) logger.debug("writeAudioFile: write wav file " + file.getAbsolutePath());

    writeToFile(getBytesFromBase64String(base64EncodedString), file);

    if (DEBUG) logger.debug("writeAudioFile: wrote wav file " + file.getAbsolutePath());

    if (!file.exists()) {
      logger.error("writeAudioFile : huh? can't find " + file.getAbsolutePath());
    }
    AudioCheck.ValidityAndDur valid = isValid(file, useSensitiveTooLoudCheck, quietAudioOK);
    if (valid.isValid()) {
      valid.setDuration(trimSilence(file).getDuration());
    }

    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > MIN_WARN_DUR) {
      logger.debug("writeAudioFile: took " + diff + " millis to write wav file (" + file.getName() +
          ") " + valid.durationInMillis + " millis long");
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
   * @param quietAudioOK
   * @return
   * @see #convertBase64ToAudioFiles
   * @see AudioFileHelper#getAnswer
   */
  public AudioCheck.ValidityAndDur isValid(File file, boolean useSensitiveTooLoudCheck, boolean quietAudioOK) {
    try {
      if (file.length() < 44) {
        logger.warn("isValid : audio file " + file.getAbsolutePath() + " length was " + file.length() + " bytes.");
        return new AudioCheck.ValidityAndDur(AudioAnswer.Validity.TOO_SHORT, 0, false);
      } else {
        AudioCheck.ValidityAndDur validityAndDur =
            useSensitiveTooLoudCheck ? audioCheck.checkWavFileRejectAnyTooLoud(file, quietAudioOK) :
                audioCheck.checkWavFile(file, quietAudioOK);
        return validityAndDur;
      }
    } catch (Exception e) {
      logger.error("isValid got " + e, e);
    }
    return AudioCheck.INVALID_AUDIO;
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
    String pathname = testAudioDir + File.separator + testAudioFileNoSuffix + WAV;
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
        wavFile = copyFileAndDeleteOriginal(wavFile, convertTo16KHZ, SIXTEEN_K_SUFFIX);

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
   * If trimming doesn't really change the length, we leave it alone {@link #DIFF_THRESHOLD}.
   * <p>
   * Trimmed file will be empty if it's not successful.
   *
   * @param wavFile to trim silence from - will be replaced in place
   * @return
   * @see #convertBase64ToAudioFiles
   */
  public TrimInfo trimSilence(final File wavFile) {
    if (DEBUG) logger.info("trimSilence " + wavFile.getAbsolutePath());
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
      if (durationInSecondsTrimmed > 0.1 && diff > DIFF_THRESHOLD) {
        copyAndDeleteOriginal(trimmed, wavFile);

        if (DEBUG) logger.debug("trimSilence (" + //props.getLanguage() +
            ")  convert original " + wavFile.getName() +
            " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");

        long now = System.currentTimeMillis();
        if (now - then > 0) {
          logger.debug("trimSilence (" + //props.getLanguage() +
              "): took " + (now - then) + " millis to convert original " + wavFile.getName() +
              " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");
        }
        return new TrimInfo(durationInSecondsTrimmed, true);
      } else {
        logger.info("trimSilence : took " + (System.currentTimeMillis() - then) + " millis to NOT convert original " + wavFile.getName() +
            " to trim wav file : " + durationInSeconds + " before, " + durationInSecondsTrimmed + " after.");

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

  private File copyFileAndDeleteOriginal(final File wavFile, final String sourceFile, final String suffix) throws IOException {
//    logger.info("orig " + wavFile.getName() + " source " + sourceFile + " suffix " + suffix);
    String name1 = wavFile.getName();
    String dest = wavFile.getParent() + File.separator + removeSuffix(name1) + suffix + WAV;

    File replacement = new File(dest);
    File srcFile = new File(sourceFile);
//    logger.info("srcFile " + srcFile + " exists " + srcFile.exists());
    copyAndDeleteOriginal(srcFile, replacement);

    return replacement;
  }

  private void copyAndDeleteOriginal(String srcFile, File replacement) throws IOException {
    copyAndDeleteOriginal(new File(srcFile), replacement);
  }

  private void copyAndDeleteOriginal(File srcFile, File replacement) throws IOException {
    FileUtils.copyFile(srcFile, replacement);
    if (DEBUG)
      logger.debug("copyAndDeleteOriginal " + srcFile.getAbsolutePath() + " to " + replacement.getAbsolutePath());
    // cleanup
    deleteParentTempDir(srcFile);
  }

  /**
   * @param prefix
   * @return
   * @throws IOException
   * @see #makeTempFile(String)
   */
  private File makeTempDir(String prefix) throws IOException {
    String prefix1 = "AudioConversion_makeTempDir_for_" + prefix;
    if (DEBUG) logger.info("makeTempDir " + prefix);
    Path audioConversion = Files.createTempDirectory(prefix1);
    if (DEBUG) logger.info("makeTempDir made " + audioConversion);

    File file = audioConversion.toFile();

    if (DEBUG) logger.info("makeTempDir made " + file.getAbsolutePath());

    return file;
  }

  private void deleteParentTempDir(File srcFile) throws IOException {
    FileUtils.deleteDirectory(new File(srcFile.getParent()));
  }

  /**
   * @param pathToAudioFile
   * @return
   * @throws IOException
   * @see #convertTo16Khz(File)
   */
  private String convertTo16KHZ(String pathToAudioFile) throws IOException {
    return sampleAt16KHZ(pathToAudioFile, makeTempFile("convertTo16KHZ"));
  }

  private String sampleAt16KHZ(String pathToAudioFile, String tempForWavz) throws IOException {
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
   * @return audio in a temp dir
   * @throws IOException
   * @see #getHighPassFilterFile(String)
   */
  private String doHighPassFilter(String pathToAudioFile) throws IOException {
    final String tempForWavz = makeTempFile("doHighPassFilter");

    ProcessBuilder soxFirst = new ProcessBuilder(
        soxPath,
        pathToAudioFile,
        tempForWavz,
        "silence", "1", "0.01", "-90d", "gain", "-3", "highpass", "80.0");

    if (!new ProcessRunner().runProcess(soxFirst)) {
      logger.warn("doHighPassFilter couldn't do high pass filter on " + pathToAudioFile);
      String asRunnable = soxFirst.command().toString().replaceAll(",", " ");
      logger.warn("doHighPassFilter path " + asRunnable);
    }

    return tempForWavz;
  }

  private String makeTempFile(String prefix) throws IOException {
    return makeTempDir(prefix) + File.separator + "temp" + prefix + WAV;
  }

  /**
   * TODO: Why all the stuff with a temp dir???
   * creating and deleting???
   * <p>
   * sox $sourcewav $outputwav vad -t 6 -p 0.20 reverse vad -t 6 -p 0.20 reverse
   *
   * @param pathToAudioFile
   * @return file that should be cleaned up
   * @throws IOException
   */
  private String doTrimSilence(String pathToAudioFile) throws IOException {
    final String tempTrimmed = makeTempFile("doTrimSilence");

    if (DEBUG)
      logger.info("doTrimSilence running sox on " + new File(pathToAudioFile).getAbsolutePath() + " to produce " + new File(tempTrimmed).getAbsolutePath());
    String trimBefore = "0.30";// + trimMillisBefore;
    String trimAfter = "0.30";// + trimMillisAfter;
    ProcessBuilder soxFirst = new ProcessBuilder(
        getSox(),
        pathToAudioFile,
        tempTrimmed,
        "vad", "-t", T_VALUE, "-p", trimBefore, "reverse", "vad", "-t", T_VALUE, "-p", trimAfter, "reverse");

//    logger.error("doTrimSilence trim silence on " + pathToAudioFile);
//    String asRunnable = soxFirst.command().toString().replaceAll(",", " ");
    if (DEBUG) logger.info("doTrimSilence " + soxFirst.command());

    if (!new ProcessRunner().runProcess(soxFirst)) {
      //logger.info("tempDir Exists " + exists);
      logger.info("pathToAudioFile exists " + new File(pathToAudioFile).exists());
      logger.info("tempTrimmed exists     " + new File(tempTrimmed).exists());
      logger.error("couldn't do trim silence on " + pathToAudioFile);
      String asRunnable2 = soxFirst.command().toString().replaceAll(",", " ");
      logger.info("path " + asRunnable2);
    }
    if (DEBUG) logger.info("doTrimSilence finished " + soxFirst.command());

    return tempTrimmed;
  }

  /**
   * @param wavFile
   * @param rawFile
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#scoreRepeatExercise
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

//  public String ensureWriteMP3Easy(String pathToWav, boolean overwrite, String title, String author) {
//    if (pathToWav == null || pathToWav.equals("null")) throw new IllegalArgumentException("huh? path is null");
//    return writeMP3Easy(pathToWav, new File(pathToWav), overwrite, title, author);
//  }

  private int spew2 = 0;

  /**
   * @param pathToWav
   * @param realContextPath
   * @param overwrite
   * @param author
   * @return
   * @see #ensureWriteMP3
   */
  private String writeMP3(String pathToWav, String realContextPath, boolean overwrite, String title, String author) {
    File absolutePathToWav = new File(pathToWav); // LAZY - what should it be?
    if (!absolutePathToWav.exists()) {
      logger.info("can't find " +absolutePathToWav.getAbsolutePath());
      absolutePathToWav = getAbsoluteFile(pathToWav, realContextPath);
    }
    return writeMP3Easy(absolutePathToWav, overwrite, title, author);
  }

  /**
   * @see PathWriter#getPermanentAudioPath(File, String, boolean, String, int, String, String, ServerProperties)
   * @param absolutePathToWav
   * @param overwrite
   * @param title
   * @param author
   * @return
   */
  String writeMP3Easy(File absolutePathToWav, boolean overwrite, String title, String author) {
    String mp3File = absolutePathToWav.getAbsolutePath().replace(WAV, MP3);
    File mp3 = new File(mp3File);
    if (!mp3.exists() || overwrite) {
      if (DEBUG)
        logger.debug("writeMP3 : doing mp3 conversion for " + absolutePathToWav);

      if (DEBUG) logger.debug("run lame on " + absolutePathToWav + " making " + mp3File);

      if (!convertToMP3FileAndCheck(getLame(), title, absolutePathToWav.getAbsolutePath(), mp3File, author)) {
        if (spew2++ < 10) logger.error("File missing for " + absolutePathToWav + " for " + author);
        return FILE_MISSING;
      }
      logger.info("writeMP3Easy path is " + mp3.getAbsolutePath() + " : " + mp3.exists());
    }

    String oggFile = absolutePathToWav.getAbsolutePath().replace(WAV, OGG);
    File ogg = new File(oggFile);
    if (!ogg.exists() || overwrite) {
      if (DEBUG)
        logger.debug("writeMP3 : doing ogg conversion for " + absolutePathToWav);

      if (DEBUG) logger.debug("run ogg on " + absolutePathToWav + " making " + oggFile);

      if (!convertToOGGFileAndCheck(getOggenc(), title, absolutePathToWav.getAbsolutePath(), oggFile, author)) {
        logger.error("ogg File missing for " + absolutePathToWav);
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
    String prepended = binPath + File.separator + exe;
    if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
      return prepended + ".exe";
    } else {
      return prepended;
    }
  }

  /**
   * sox normalize to -3db -- thanks Paul!
   *
   * @param absolutePathToWav
   * @seex mitll.langtest.server.LangTestDatabaseImpl#normalizeLevel
   * @see PathWriter#getPermanentAudioPath(mitll.langtest.server.PathHelper, File, String, boolean, String, String, String, ServerProperties)
   */
  void normalizeLevels(File absolutePathToWav) {
    try {
      final File tempFile = new File(makeTempFile("normalizeLevels"));
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

      copyAndDeleteOriginal(tempFile, absolutePathToWav);
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

  private String lamePath = null;

  private String getLame() {
    return lamePath;
  }

  private void setLame() {
    String lamePath = LAME_PATH_WINDOWS;    // Windows
    if (!new File(lamePath).exists()) {
      lamePath = LAME_PATH_LINUX;
    }
    if (!new File(lamePath).exists()) {
      logger.error("no lame installed at " + lamePath + " or " + LAME_PATH_WINDOWS);
      lamePath = LAME;
    } else {
//      logger.info("found  lame at " + new File(lamePath).getAbsolutePath());
    }
    this.lamePath = lamePath;
  }

  private String getOggenc() {
    String property = System.getProperty("os.name").toLowerCase();
    boolean isMac = property.contains("mac");
    boolean isWin = property.contains("win");

    String oggEncPath = isMac ? "bin/macos/oggenc" : isWin ? "bin\\win32\\oggenc.exe" : "/usr/bin/oggenc";
    File file = new File(oggEncPath);
    if (!file.exists()) {
      logger.error("can't find oggenc at " + file.getAbsolutePath());
    }
//    oggEncPath = "oggenc";
    return oggEncPath;
  }

  /**
   * @param pathToWav
   * @return
   * @seex MP3Support#getWavForMP3(String, String)
   */
/*  @Deprecated File convertMP3ToWav(String pathToWav) {
    assert (pathToWav.endsWith(MP3));
    String mp3File = pathToWav.replace(MP3, ".wav");
    return writeWavFromMP3(getLame(), pathToWav, mp3File);
  }*/

  /**
   * @param lamePath
   * @param pathToAudioFile
   * @param mp3File
   * @return
   * @deprecated when would this be a good idea???
   */
/*  private File writeWavFromMP3(String lamePath, String pathToAudioFile, String mp3File) {
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, "--decode", pathToAudioFile, mp3File);
    try {
      //    System.out.println("convertFileAndCheck running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
      //     System.out.println("convertFileAndCheck exited  lame" + lameProc);
    } catch (IOException e) {
      logger.error("Couldn't run " + lameProc);
      logger.error("got " + e, e);
    }

    return new File(mp3File);
  }*/

  private int spew = 0;

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
  private boolean convertToMP3FileAndCheck(String lamePath, String title, String pathToAudioFile, String mp3File, String author) {
    if (DEBUG) logger.debug("convert " + pathToAudioFile + " to " + mp3File);
    if (title != null && title.length() > 30) {
      title = title.substring(0, 30);
    }
    if (title == null) title = "";
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File, "--tt", title, "--ta", author);
    try {
//      logger.debug("running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
    } catch (IOException e) {
      //  logger.error("Couldn't run " + lameProc);
      logger.error("for " + lameProc + " got " + e, e);
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      if (!new File(pathToAudioFile).exists()) {
        if (SPEW && spew++ < 10) logger.error("huh? source file " + pathToAudioFile + " doesn't exist?",
             new Exception("can't find " + pathToAudioFile));
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

  /**
   * @see #writeMP3Easy(String, File, boolean, String, String)
   * @param oggPath
   * @param title
   * @param pathToAudioFile
   * @param oggFile
   * @param author
   * @return
   */
  private boolean convertToOGGFileAndCheck(String oggPath, String title, String pathToAudioFile, String oggFile, String author) {
    if (DEBUG) logger.debug("convert " + pathToAudioFile + " to " + oggFile);
    if (title != null && title.length() > 30) {
      title = title.substring(0, 30);
    }
    if (title == null) title = "";
    ProcessBuilder oggProx = new ProcessBuilder(oggPath, pathToAudioFile, "-o", oggFile, "-t", title, "-a", author);
    try {
      //logger.debug("running lame" + oggProx.command());
      new ProcessRunner().runProcess(oggProx);
    } catch (IOException e) {
      //  logger.error("Couldn't run " + oggProx);
      logger.error("for " + oggProx + " got " + e, e);
    }

    File testFile = new File(oggFile);
    if (!testFile.exists()) {
      if (!new File(pathToAudioFile).exists()) {
        if (SPEW) logger.error("huh? source file " + pathToAudioFile + " doesn't exist?");
      } else {
        logger.error("didn't write OGG : " + testFile.getAbsolutePath() +
            " exe path " + oggPath +
            " command was " + oggProx.command());
        try {
          if (!new ProcessRunner().runProcess(oggProx, true)) {
            return false;
          }
        } catch (IOException e) {
          logger.error("for " + oggProx + " got " + e, e);
        }

      }
      return false;
    }
    return true;
  }
}
