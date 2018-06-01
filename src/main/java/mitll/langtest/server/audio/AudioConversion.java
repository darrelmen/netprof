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
  private static final String WAV = ".wav";
  private final AudioCheck audioCheck;
  private static final int MIN_WARN_DUR = 50;

  private final String soxPath;
//  private final long trimMillisBefore;
//  private final long trimMillisAfter;

  private static final boolean DEBUG = true;
  private static final boolean DEBUG_DETAIL = false;
  private boolean trimAudio;
  private static final boolean WARN_MISSING_FILE = false;

  /**
   * @paramx props
   * @seex mitll.langtest.server.DatabaseServlet#ensureMP3
   */
  public AudioConversion(boolean trimAudio, int minDynamicRange) {
//    trimMillisBefore = props.getTrimBefore();
//    trimMillisAfter = props.getTrimAfter();
    this.trimAudio = trimAudio;
    soxPath = getSox();
    audioCheck = new AudioCheck(trimAudio, minDynamicRange);
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

    if (DEBUG) logger.debug("convertBase64ToAudioFiles: write wav file " + file.getAbsolutePath());

    writeToFile(getBytesFromBase64String(base64EncodedString), file);

    if (DEBUG) logger.debug("convertBase64ToAudioFiles: wrote wav file " + file.getAbsolutePath());

    if (!file.exists()) {
      logger.error("convertBase64ToAudioFiles : after writing, can't find file at " + file.getAbsolutePath());
    }
    AudioCheck.ValidityAndDur valid = isValid(file, useSensitiveTooLoudCheck, quietAudioOK);
    if (valid.isValid() && trimAudio) {
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
      logger.error("writeToFile got " + e, e);
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
    return audioCheck.isValid(file, useSensitiveTooLoudCheck, quietAudioOK);
/*    try {
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
    return AudioCheck.INVALID_AUDIO;*/
  }

  /**
   * Checks if the sample rate is not 16K (as required by things like sv).
   * If not, then uses sox to make an audio file with the right sample rate.
   * <p>
   * Sox can take awhile, two threads could touch the same file.
   *
   * @param testAudioDir          directory for audio
   * @param testAudioFileNoSuffix name without suffix
   * @return unique file path - could be same request is made by two different threads
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#scoreRepeatExercise
   */
  public String convertTo16Khz(String testAudioDir, String testAudioFileNoSuffix, long uniqueTimestamp) throws UnsupportedAudioFileException {
    String pathname = testAudioDir + File.separator + testAudioFileNoSuffix + WAV;
    File wavFile = convertTo16Khz(new File(pathname), uniqueTimestamp);
    return removeSuffix(wavFile.getName());
  }

  /**
   * @param wavFile
   * @return
   * @see AudioCheck#getDynamicRange
   */
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
   * @see #convertTo16Khz
   */
  private File convertTo16Khz(File wavFile, long uniqueTimestamp) throws UnsupportedAudioFileException {
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
        String convertTo16KHZ = convertTo16KHZ(wavFile.getAbsolutePath());
        wavFile = copyFileAndDeleteOriginal(wavFile, convertTo16KHZ, SIXTEEN_K_SUFFIX + "_" + uniqueTimestamp);

        long now = System.currentTimeMillis();
        long diff = now - then;

        if (diff > 25) {
          logger.info("convertTo16Khz : took " + diff + " millis to convert original " + orig.getName() + " at " + sampleRate +
              " to 16K wav file : " + wavFile.getName());
        }
      }
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
    return wavFile;
  }

  /**
   * Note that wavFile input will be changed if trim is successful.
   * <p>
   * OBE:  If trimming doesn't really change the length, we leave it alone {@linkx #DIFF_THRESHOLD}.
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
    if (DEBUG)
      logger.info("copyFileAndDeleteOriginal orig " + wavFile.getName() + " source " + sourceFile + " suffix " + suffix);
    String name1 = wavFile.getName();
    String dest = wavFile.getParent() + File.separator + removeSuffix(name1) + suffix + WAV;

    File replacement = new File(dest);
    File srcFile = new File(sourceFile);
    if (DEBUG) logger.info("copyFileAndDeleteOriginal srcFile " + srcFile + " exists " + srcFile.exists());
    copyAndDeleteOriginal(srcFile, replacement);

    return replacement;
  }

  /**
   * @param pathToAudioFile
   * @return
   * @throws IOException
   * @see #convertTo16Khz
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
   * Paul said to do this:
   * <p>
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
   * Rip the header off to make a raw file.
   * assumes 16Khz?
   *
   * @param wavFile
   * @param rawFile
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#scoreRepeatExercise
   */
  public static boolean wav2raw(String wavFile, String rawFile) {
    FileOutputStream fout = null;
    AudioInputStream sourceStream = null;

    File sourceFile = new File(wavFile);
    if (DEBUG)
      logger.info("wav2raw Reading from " + sourceFile + " exists " + sourceFile.exists() + " at " + sourceFile.getAbsolutePath());

    try {
      sourceStream = AudioSystem.getAudioInputStream(sourceFile);
      File outputFile = new File(rawFile);

      String absolutePath = outputFile.getAbsolutePath();
      if (DEBUG) logger.info("wav2raw Writing to " + absolutePath);

      fout = new FileOutputStream(outputFile);

      {
        byte[] b = new byte[1024];

        int nread = 0;
        do {
          if (nread > 0)
            fout.write(b, 0, nread);
          nread = sourceStream.read(b);
        }
        while (nread > 0);
      }

      sourceStream.close();
      fout.close();

      boolean b1 = outputFile.setReadable(true);
      if (!b1) {
        logger.error("wav2raw : can't read the output file " + absolutePath);
      }

      if (DEBUG) {
        logger.info("wav2raw wrote to " + absolutePath + " exists = " + outputFile.exists() + " len " + (outputFile.length() / 1024) + "K");
      }

      return outputFile.exists();
    } catch (Exception e) {
      logger.error("Got " + e, e);
      return false;
    } finally {
      try {
        if (fout != null)
          fout.close();
        if (sourceStream != null)
          sourceStream.close();
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
   * @param overwrite       true if step on existing file.
   * @param trackInfo
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#ensureMP3
   */
  public String ensureWriteMP3(String realContextPath, String pathToWav, boolean overwrite, TrackInfo trackInfo) {
    if (pathToWav == null || pathToWav.equals("null")) throw new IllegalArgumentException("huh? path is null");
    return writeMP3(realContextPath, pathToWav, overwrite, trackInfo);
  }

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
   * @return absolute path to file
   * @see PathWriter#getPermanentAudioPath
   */
  public String writeCompressedVersions(File absolutePathToWav, boolean overwrite, TrackInfo trackInfo) {
    try {
      String absolutePath = absolutePathToWav.getAbsolutePath();
      String mp3File = getMP3ForWav(absolutePath);
      //logger.info("writeCompressedVersions started  writing " + absolutePathToWav.getAbsolutePath() + " over " + overwrite);
      if (!writeMP3(absolutePathToWav, overwrite, trackInfo, mp3File)) return FILE_MISSING;
      if (!new ConvertToOGG().writeOGG(absolutePathToWav, overwrite, trackInfo)) return FILE_MISSING;
      // logger.info("writeCompressedVersions finished writing " + absolutePathToWav.getAbsolutePath() + " over " + overwrite);
      return mp3File;
    } catch (Exception e) {
      logger.error("writeCompressedVersions got " + e, e);
      return FILE_MISSING;
    }
  }

  String getMP3ForWav(String absolutePath) {
    return absolutePath.replace(WAV, MP3);
  }

  private boolean writeMP3(File absolutePathToWav, boolean overwrite, TrackInfo trackInfo, String mp3File) {
    File mp3 = new File(mp3File);
    if (!mp3.exists() || overwrite) {
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
   * @see PathWriter#copyAndNormalize
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

  private File getAbsolute(String realContextPath, String filePath) {
    return new File(realContextPath, filePath);
  }

  private String lamePath = null;

  /**
   * @return
   * @see #writeMP3(File, boolean, TrackInfo, String)
   */
  private String getLame() {
    return lamePath;
  }

  /**
   * @see #AudioConversion
   */
  private void setLame() {
    String lamePath = LAME_PATH_WINDOWS;    // Windows
    if (!new File(lamePath).exists()) {
      lamePath = LAME_PATH_LINUX;
    }
    if (!new File(lamePath).exists()) {
//      logger.error("no lame installed at " + lamePath + " or " + LAME_PATH_WINDOWS);
      lamePath = LAME;
    }
    this.lamePath = lamePath;
  }

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
    if (DEBUG_DETAIL) logger.info("convertToMP3FileAndCheck convert " + pathToAudioFile + " to " + mp3File);

    String title = trackInfo.getTitle();
    if (title != null && title.length() > 30) {
      title = title.substring(0, 30);
    }
    if (title == null) title = "";
    ProcessBuilder lameProc = new ProcessBuilder(lamePath, pathToAudioFile, mp3File,
        "--tt", title,
        "--ta", trackInfo.getArtist(),
        "--tc", trackInfo.getComment(),
        "--tl", trackInfo.getAlbum());
    try {
      if (DEBUG_DETAIL) logger.info("running lame" + lameProc.command());
      new ProcessRunner().runProcess(lameProc);
    } catch (IOException e) {
      //  logger.error("Couldn't run " + lameProc);
      logger.error("for " + lameProc + " got " + e, e);
    }

    File testMP3 = new File(mp3File);
    if (!testMP3.exists()) {
      if (!new File(pathToAudioFile).exists()) {
        if (SPEW && spew++ < 10) {
          logger.error("convertToMP3FileAndCheck huh? source file " + pathToAudioFile + " doesn't exist?");//,
          //new Exception("can't find " + pathToAudioFile));
        }
      } else {
        logger.error("convertToMP3FileAndCheck didn't write MP3 : " + testMP3.getAbsolutePath() +
            " exe path " + lamePath +
            " command was " + lameProc.command());
        try {
          if (!new ProcessRunner().runProcess(lameProc, true)) {
            return false;
          }
        } catch (IOException e) {
          logger.error("convertToMP3FileAndCheck for " + lameProc + " got " + e, e);
        }

      }
      return false;
    }
    return true;
  }

  public String getAbsPathForAudio(String wavFile, String language, String parent, String audioBaseDir) {
    String parent2 = getParentForFilePathUnderBaseAudio(wavFile, language, parent, audioBaseDir);
    File file = new File(parent2, wavFile);
    //  logger.info("parent 2 " + parent2 + " wav " + wavFile + " file " + file);
    return file.getAbsolutePath();
  }

  private int spew4 = 0;

  public String getParentForFilePathUnderBaseAudio(String wavFile, String language, String parent, String audioBaseDir) {
    File test = new File(wavFile);
    if (!test.exists()) {
      if (WARN_MISSING_FILE) {
        logger.warn("ensureMP3 : can't find " + test.getAbsolutePath());// + " under " + parent + " trying config... ");
      }
//      String audioBaseDir = serverProps.getAudioBaseDir();
      parent = audioBaseDir;// + File.separator + language;
      if (DEBUG)
        logger.warn("ensureMP3 : trying " + wavFile + " under " + parent);// + " under " + parent + " trying config... ");
    }

    File fileUnderParent = new File(parent, wavFile);

    if (!fileUnderParent.exists()) {
      parent += ServerProperties.BEST_AUDIO + File.separator + language.toLowerCase();
      File fileUnderParent2 = new File(parent, wavFile);

      if (DEBUG)
        logger.warn("ensureMP3 : trying " + wavFile + " under " + parent);// + " under " + parent + " trying config... ");

      if (!fileUnderParent2.exists()) {
        if (spew4++ < 100) {
          logger.warn("ensureMP3 can't find " + fileUnderParent2.getAbsolutePath(), new Exception());
        }
      } else {
        // logger.info("OK found " + fileUnderParent2.getAbsolutePath() + " " + fileUnderParent2.exists());
      }
    }
    return parent;
  }

  AudioCheck getAudioCheck() {
    return audioCheck;
  }
}
