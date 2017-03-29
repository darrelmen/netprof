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

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.answer.AudioAnswer;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;

public class AudioConversion extends AudioBase {
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
  private static final String LAME = "lame";
  private static final boolean SPEW = true;
  private static final String MP3 = ".mp3";
  public static final String WAV = ".wav";
  //private static final String OGG = ".ogg";
  private final AudioCheck audioCheck;
  private static final int MIN_WARN_DUR = 30;

  private final String soxPath;
  private final long trimMillisBefore;
  private final long trimMillisAfter;
  private ServerProperties props;

  private static final boolean DEBUG = false;

  /**
   * @param props
   * @see mitll.langtest.server.DatabaseServlet#ensureMP3
   */
  public AudioConversion(ServerProperties props) {
    trimMillisBefore = props.getTrimBefore();
    trimMillisAfter = props.getTrimAfter();
    soxPath = getSox();
    this.props = props;
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
    if (valid.isValid() && props.shouldTrimAudio()) {
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
    return new Trimmer().trimSilence(audioCheck, wavFile);
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
      return null;
    }

    return tempForWavz;
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
   * @param realContextPath
   * @param pathToWav
   * @param overwrite
   * @param trackInfo
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#ensureMP3
   */
  public String ensureWriteMP3(String realContextPath, String pathToWav, boolean overwrite, TrackInfo trackInfo) {
    if (pathToWav == null || pathToWav.equals("null")) throw new IllegalArgumentException("huh? path is null");
    return writeMP3(realContextPath, pathToWav, overwrite, trackInfo);
  }

//  public String ensureWriteMP3Easy(String pathToWav, boolean overwrite, String title, String author) {
//    if (pathToWav == null || pathToWav.equals("null")) throw new IllegalArgumentException("huh? path is null");
//    return writeMP3Easy(pathToWav, new File(pathToWav), overwrite, title, author);
//  }

  private int spew2 = 0;
  private int spew3 = 0;

  /**
   * @param realContextPath
   * @param pathToWav
   * @param overwrite
   * @param trackInfo
   * @return
   * @see #ensureWriteMP3
   */
  private String writeMP3(String realContextPath, String pathToWav, boolean overwrite, TrackInfo trackInfo) {
    File absolutePathToWav = new File(pathToWav); // LAZY - what should it be?
    if (!absolutePathToWav.exists()) {
      if (spew3++ < 100 && spew3 % 100 == 0) {
        logger.info("writeMP3 can't find " + pathToWav +
            " trying under " + realContextPath);
      }
      absolutePathToWav = getAbsoluteFile(realContextPath, pathToWav);
    }
    return writeCompressedVersions(absolutePathToWav, overwrite, trackInfo);
  }

  /**
   * @param absolutePathToWav
   * @param overwrite
   * @param trackInfo
   * @return
   * @see PathWriter#getPermanentAudioPath
   */
  public String writeCompressedVersions(File absolutePathToWav, boolean overwrite, TrackInfo trackInfo) {
    try {
      String mp3File = absolutePathToWav.getAbsolutePath().replace(WAV, MP3);
      if (!writeMP3(absolutePathToWav, overwrite, trackInfo, mp3File)) return FILE_MISSING;
      if (!new ConvertToOGG().writeOGG(absolutePathToWav, overwrite, trackInfo)) return FILE_MISSING;
      return mp3File;
    } catch (Exception e) {
      logger.error("Got " + e, e);
      return FILE_MISSING;
    }
  }

  private boolean writeMP3(File absolutePathToWav, boolean overwrite, TrackInfo trackInfo, String mp3File) {
    File mp3 = new File(mp3File);
    if (!mp3.exists() || overwrite) {
      if (DEBUG)
        logger.debug("writeMP3 : doing mp3 conversion for " + absolutePathToWav);

      if (DEBUG) logger.debug("writeMP3 run lame on " + absolutePathToWav + " making " + mp3File);

      if (!convertToMP3FileAndCheck(getLame(), absolutePathToWav.getAbsolutePath(), mp3File, trackInfo)) {
        if (spew2++ < 10)
          logger.error("writeMP3 File missing for " + absolutePathToWav + " for " + trackInfo.getArtist());
        return false;
      }
      if (DEBUG) logger.info("writeMP3 path is " + mp3.getAbsolutePath() + " : exists = " + mp3.exists());
    }
    return true;
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
   * @see PathWriter#getPermanentAudioPath
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
        logger.error("didn't make " + tempFile + " soxFirst " + soxFirst2.command());
      } else {
        //logger.debug("wrote normalized to " + tempFile.getAbsolutePath());
        copyAndDeleteOriginal(tempFile, absolutePathToWav);
      }

    } catch (IOException e) {
      logger.error("normalizing " + absolutePathToWav + " got " + e, e);
    }
  }

  private File getAbsoluteFile(String realContextPath, String filePath) {
    return getAbsolute(realContextPath, filePath);
  }

  public boolean exists(String realContextPath, String filePath) {
    return getAbsoluteFile(realContextPath, filePath).exists();
  }

  private File getAbsolute(String realContextPath, String filePath) {
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
   * @param pathToAudioFile
   * @param mp3File
   * @param trackInfo
   * @return
   * @see #ensureWriteMP3
   * @see #writeMP3
   */
  private boolean convertToMP3FileAndCheck(String lamePath, String pathToAudioFile, String mp3File, TrackInfo trackInfo) {
    if (DEBUG) logger.debug("convertToMP3FileAndCheck convert " + pathToAudioFile + " to " + mp3File);
    String title = trackInfo.getTitle();
    String author = trackInfo.getArtist();
    if (title != null && title.length() > 30) {
      title = title.substring(0, 30);
    }
    if (title == null) title = "";
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File,
        "--tt", title,
        "--ta", author,
        "--tc", trackInfo.getComment(),
        "--tl", trackInfo.getAlbum());
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
}
