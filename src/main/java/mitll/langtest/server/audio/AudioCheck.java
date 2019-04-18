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

package mitll.langtest.server.audio;

import mitll.langtest.shared.answer.Validity;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class AudioCheck {
  private static final Logger logger = LogManager.getLogger(AudioCheck.class);

  private static final int MinRecordLength = (10000 / 2); // 10000 = 0.7 second
  private static final int WinSize = 10;
  private static final float PowerThreshold = -79.50f;//-55.0f;
  private static final float VarianceThreshold = 20.0f;
  private static final double CLIPPED_RATIO = 0.005; // 1/2 %
  private static final double CLIPPED_RATIO_TIGHTER = 0.0025; // 1/2 %
  private static final double LOG_OF_TEN = Math.log(10.0);

  private static final short clippedThreshold = 32704; // 32768-64
  private static final short clippedThresholdMinus = -32704; // 32768-64
  //private static final short ct = 32112;
  // private static final short clippedThreshold2 = 32752; // 32768-16
  // private static final short clippedThreshold2Minus = -32752; // 32768-16
  private static final float MAX_VALUE = 32768.0F;
  private static final ValidityAndDur INVALID_AUDIO = new ValidityAndDur();
  private static final boolean DEBUG = false;
  public static final int WAV_HEADER_LENGTH = 44;
  private static final boolean DUMP_POWER_INFO = true;
  private final int minDynamicRange;

  // TODO :make a server prop
  private static final float FORGIVING_MIN_DNR = 18F;
  private final boolean trimAudio;

  /**
   * @param trimAudio
   * @param minDynamicRange
   */
  public AudioCheck(boolean trimAudio, int minDynamicRange) {
    this.trimAudio = trimAudio;
    this.minDynamicRange = minDynamicRange;
  }

  /**
   * @param file
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#getImageForAudioFile
   */
  public double getDurationInSeconds(String file) throws UnsupportedAudioFileException {
    return getDurationInSeconds(new File(file));
  }

  /**
   * @param file
   * @return
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#scoreRepeatExercise
   */
  public double getDurationInSeconds(File file) throws UnsupportedAudioFileException {
    AudioInputStream audioInputStream = null;
    try {
      audioInputStream = getAudioInputStream(file);
      double dur = getDurationInSeconds(audioInputStream);
      audioInputStream.close();
      return dur;
    } catch (UnsupportedAudioFileException un) {
      throw un;
    } catch (Exception e) {
      try {
        if (audioInputStream != null) audioInputStream.close();
      } catch (IOException e1) {
        logger.error("got " + e, e);
      }
      logger.error("got " + e, e);
    }
    return 0d;
  }

  private double getDurationInSeconds(AudioInputStream audioInputStream) {
    long frames = audioInputStream.getFrameLength();
    AudioFormat format = audioInputStream.getFormat();
    return Long.valueOf(frames).floatValue() / format.getFrameRate();
  }

  /**
   * @param file
   * @param useSensitiveTooLoudCheck
   * @param quietAudioOK
   * @return
   * @see AudioConversion#isValid(File, boolean, boolean)
   * @see AudioFileHelper#getAnswer
   */
  public AudioCheck.ValidityAndDur isValid(File file, boolean useSensitiveTooLoudCheck, boolean quietAudioOK) {
    try {
      long length = file.length();

      if (length < WAV_HEADER_LENGTH) {
        logger.warn("isValid : audio file " + file.getAbsolutePath() + " length was " + length + " bytes.");
        return new AudioCheck.ValidityAndDur(Validity.TOO_SHORT, 0, false);
      } else {
        return getValidityAndDur(file, !useSensitiveTooLoudCheck, quietAudioOK);
      }
    } catch (Exception e) {
      logger.error("isValid got " + e, e);
    }
    return AudioCheck.INVALID_AUDIO;
  }

  public AudioCheck.ValidityAndDur isValid(String name,
                                           String fileInfo,
                                           int length,
                                           AudioInputStream stream,
                                           boolean useSensitiveTooLoudCheck,
                                           boolean quietAudioOK) {
    try {
      if (length < WAV_HEADER_LENGTH) {
        logger.warn("isValid : audio file " + fileInfo + " length was " + length + " bytes.");
        return new AudioCheck.ValidityAndDur(Validity.TOO_SHORT, 0, false);
      } else {
        return getValidityAndDurStream(name, fileInfo, stream, !useSensitiveTooLoudCheck, quietAudioOK);
      }
    } catch (Exception e) {
      logger.error("isValid got " + e, e);
    }
    return AudioCheck.INVALID_AUDIO;
  }

  @NotNull
  private ValidityAndDur getValidityAndDur(File file, boolean allowMoreClipping, boolean quietAudioOK) {
    ValidityAndDur validityAndDur = checkWavFileWithClipThreshold(file, allowMoreClipping, quietAudioOK);
    if (validityAndDur.isValid()) {
      addDynamicRange(file, validityAndDur);
    }
    return validityAndDur;
  }


  @NotNull
  private ValidityAndDur getValidityAndDurStream(String name,
                                                 String fileInfo,
                                                 AudioInputStream stream,
                                                 boolean allowMoreClipping,
                                                 boolean quietAudioOK) {
    try {
      return getValidityAndDur(name, fileInfo, allowMoreClipping, quietAudioOK, stream, true);
    } catch (IOException e) {
      logger.error("got " + e, e);
      return INVALID_AUDIO;
    }
  }

  public void maybeAddDNR(String fileInfo, AudioInputStream stream, ValidityAndDur validityAndDur) throws IOException {
    if (validityAndDur.isValid()) {
      DynamicRange.RMSInfo dynamicRange = new DynamicRange().getRmsInfo(fileInfo, stream);

      if (dynamicRange.dnr < minDynamicRange) {
        if (DEBUG) {
          logger.info("maybeAddDNR file " + fileInfo + " doesn't meet dynamic range threshold (" + minDynamicRange +
              "): " + dynamicRange.dnr);
        }
        validityAndDur.validity = Validity.SNR_TOO_LOW;
      }
      validityAndDur.setMaxMinRange(dynamicRange.dnr);
    }
    //else {
    // logger.info("file " +fileInfo + " not valid so not doing DNR");
    //}
  }

  private void addDynamicRange(File file, ValidityAndDur validityAndDur) {
    DynamicRange.RMSInfo dynamicRange = getDynamicRange(file);
    if (dynamicRange.dnr < minDynamicRange) {
      logger.warn("addDynamicRange file " + file.getName() + " doesn't meet dynamic range threshold (" + minDynamicRange +
          "):\n" + dynamicRange);
      validityAndDur.validity = Validity.SNR_TOO_LOW;
    }
    validityAndDur.setMaxMinRange(dynamicRange.dnr);
  }

  /**
   * @param file
   * @return
   * @see #addDynamicRange
   * @see #getDNR
   */
  private DynamicRange.RMSInfo getDynamicRange(File file) {
    String highPassFilterFile =
        new AudioConversion(trimAudio, minDynamicRange)
            .getHighPassFilterFile(file.getAbsolutePath());

    if (highPassFilterFile == null) return new DynamicRange.RMSInfo();
    else {
      File highPass = new File(highPassFilterFile);
      DynamicRange.RMSInfo dynamicRange = new DynamicRange().getDynamicRange(highPass);
      deleteParentTempDir(highPass);
      return dynamicRange;
    }
  }

  private void deleteParentTempDir(File srcFile) {
    try {
      FileUtils.deleteDirectory(new File(srcFile.getParent()));
    } catch (IOException e) {
      logger.error("on " + srcFile + " got " + e, e);
    }
  }


  /**
   * Verify audio messages
   *
   * lots of debugging to track down weird thing where would choose mpeg format for wav files.
   * b/c of domino jar :
   * com.googlecode.soundlibs:mp3spi:1.9.5.4
   *
   * @param wavFile           audio byte array with header
   * @param allowMoreClipping
   * @param quietAudioOK
   * @return true if well formed
   * @seex #checkWavFile
   * @seex #checkWavFileRejectAnyTooLoud
   * @see AudioConversion#isValid(File, boolean, boolean)
   */
  private ValidityAndDur checkWavFileWithClipThreshold(File wavFile, boolean allowMoreClipping, boolean quietAudioOK) {
    AudioInputStream ais = null;
    try {
      ais = getAudioInputStream(wavFile);
      return getValidityAndDur(wavFile.getName(), wavFile.getAbsoluteFile().toString(), allowMoreClipping, quietAudioOK, ais, false);
    } catch (Exception e) {
      logger.error("Got " + e, e);
    } finally {
      try {
        if (ais != null) ais.close();
      } catch (IOException e) {
        logger.error("Got " + e, e);
      }
    }

    return INVALID_AUDIO;
  }

  @NotNull
  private ValidityAndDur getValidityAndDur(String name,
                                           String fileInfo,
                                           boolean allowMoreClipping,
                                           boolean quietAudioOK,
                                           AudioInputStream ais,
                                           boolean shortOK) throws IOException {
    AudioFormat format = ais.getFormat();


    if (DEBUG) {
      // AudioFileFormat format2 = AudioSystem.getAudioFileFormat(wavFile);
      logger.info("checkWavFileWithClipThreshold " +
              "\n\twavFile     " + name +
              "\n\tsample rate " + format.getSampleRate() +
              "\n\tformat      " + format +
              "\n\tformat class     " + format.getClass()
          //+
          //"\n\tformat 2     " + format2 +
          //"\n\tformat 2 class     " + format2.getClass()
      );
    }

    long frameLength = ais.getFrameLength();

    if (frameLength == 0) {
      return new ValidityAndDur(Validity.TOO_SHORT, 0D, false);
    }

    boolean bigEndian = format.isBigEndian();
    if (bigEndian) {
      logger.warn("checkWavFileWithClipThreshold huh? wavFile " + fileInfo + " is in big endian format?");
      logger.warn("checkWavFileWithClipThreshold " +
          "\n\tformat     " + format +
          "\n\tframe size " + format.getFrameSize() + " # channels " + format.getChannels());
    }

    int fsize = format.getFrameSize();
    assert (fsize == 2);
    assert (format.getChannels() == 1);
    double dur = getDurationInSeconds(ais);

    if (frameLength < MinRecordLength && !shortOK) {
      logger.warn("checkWavFileWithClipThreshold: audio recording too short" +
          "\n\t(Length:   " + frameLength + ") < min (" + MinRecordLength + ") " +
          "\n\tFrame size " + fsize +
          "\n\tformat     " + format +
          "\n\tformat class " + format.getClass() +
          "\n\tFrame rate " + format.getFrameRate() +
          "\n\tduration   " + dur
      );
      return new ValidityAndDur(Validity.TOO_SHORT, dur, false);
    } else if (DEBUG) {
      logger.info("checkWavFileWithClipThreshold: audio recording too short" +
          "\n\t(Length:   " + frameLength + ") < min (" + MinRecordLength + ") " +
          "\n\tFrame size " + fsize +
          "\n\tformat     " + format +
          "\n\tFrame rate " + format.getFrameRate() +
          "\n\tduration   " + dur
      );
    }

    // Verify audio power
    float pm = 0.0f, p2 = 0.0f, n = 0.0f;
    int bufSize = WinSize * fsize;
    byte[] buf = new byte[bufSize];
    int countClipped = 0;
    // int cc = 0;

    short max = 0;
    short nmax = 0;

    while (ais.read(buf) == bufSize) {
      float fpower = 0.0f;
      for (int i = 0; i < bufSize; i += fsize)
        for (int s = 0; s < fsize; s += 2) {
          // short tmp = (short) ((buf[i + s] << 8) | buf[i + s + 1]); // BIG ENDIAN
          short tmp = (short) ((buf[i + s] & 0xFF) | (buf[i + s + 1] << 8)); // LITTLE ENDIAN

          float r = ((float) tmp) / MAX_VALUE;
          if (tmp > clippedThreshold || tmp < clippedThresholdMinus) {//.abs(r) > 0.98f) {
            countClipped++;
            /*       logger.debug("at " + frameIndex + " s " + s + " i " + i + " value was " + tmp + " and r " + r);*/
          }
          //     if (tmp > ct) cc++;
          if (tmp > max) max = tmp;
          if (tmp < nmax) nmax = tmp;

          fpower += r * r;
        }

      fpower /= (float) WinSize;
      fpower = (float) dB((double) fpower);

      pm += fpower;
      p2 += fpower * fpower;
      n += (float) fsize / 2.0f;
    }

    float clippedRatio = ((float) countClipped) / (float) frameLength;
    //  float clippedRatio2 = ((float) cc) / (float) frameLength;
    boolean wasClipped = allowMoreClipping ? clippedRatio > CLIPPED_RATIO : clippedRatio > CLIPPED_RATIO_TIGHTER;// > CLIPPED_FRAME_COUNT;
    //  boolean wasClipped2 = allowMoreClipping ? clippedRatio2 > CLIPPED_RATIO : cc > 1;
/*      logger.info("of " + total +" got " +countClipped + " out of " + n +"  or " + clippedRatio  + "/" +clippedRatio2+
        " not " + notClippedRatio +" wasClipped = " + wasClipped);*/

    float mean = pm / n;
    float var = p2 / n - mean * mean;
    double std = Math.sqrt(var);
    final boolean validAudio = mean > PowerThreshold || std > VarianceThreshold;

    if (DUMP_POWER_INFO && (wasClipped || !validAudio)) {
      logger.info("checkWavFile: audio recording (Length: " + frameLength + " frames) " +
          "\n\tmean power " + mean + " (dB) vs " + PowerThreshold +
          "\n\tstd        " + std + " vs " + VarianceThreshold +
          "\n\tvalid      " + validAudio +
          "\n\tclipped (1) " + wasClipped + " (" + (clippedRatio * 100f) + "% samples clipped, # clipped = " + countClipped + ") " +
          // " was clipped (2) " + wasClipped2 + " (" + (clippedRatio2 * 100f) + "% samples clipped, # clipped = " + cc + ")" +
          "\n\tmax = " + max + "/" + nmax
      );
    }

    // literally flatline -- 0 = 0
    boolean micDisconnected = max == nmax;// mean < -79.999 && std < 0.001;

    Validity validity = validAudio ?
        (wasClipped ?
            Validity.TOO_LOUD :
            Validity.OK) :
        micDisconnected ?
            Validity.MIC_DISCONNECTED :
            Validity.TOO_QUIET;

    if (validity == Validity.TOO_QUIET) {
      validity = Validity.OK;
    }

    ValidityAndDur validityAndDur = new ValidityAndDur(validity, dur, quietAudioOK);

    //if (validityAndDur.validity != AudioAnswer.Validity.OK) {
    //logger.info("validity " + validityAndDur);
    //}
    return validityAndDur;
  }

  private double dB(double power) {
    return 20.0 * Math.log(power < 0.0001f ? 0.0001f : power) / LOG_OF_TEN;
  }

  private AudioInputStream getAudioInputStream(File wavFile) throws UnsupportedAudioFileException, IOException {
    //logger.info("getAudioInputStream : getting audio input stream for " + wavFile.getAbsolutePath());
    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(wavFile);
    //logger.info("getAudioInputStream : got stream " + audioInputStream + " for " + wavFile.getAbsolutePath());
    return audioInputStream;
  }

  /**
   * @return
   * @see mitll.langtest.server.decoder.RefResultDecoder#decodeOneExercise
   */
  public float getMinDNR() {
    return FORGIVING_MIN_DNR;
  }

  public float getDNR(File test) {
    DynamicRange.RMSInfo dynamicRange = getDynamicRange(test);
    return dynamicRange == null ? 0f : (float) dynamicRange.dnr;
  }

  public static class ValidityAndDur {
    private Validity validity;
    private final boolean isValid;
    private int durationInMillis;
    private double maxMinRange;

    public ValidityAndDur() {
      this(Validity.INVALID, 0d, false);
    }

    public ValidityAndDur(double dur) {
      this(Validity.OK, dur, false);
    }

    /**
     * @param validity
     * @param dur
     * @param quietAudioOK - only useful for automated load testing where we aren't really making recordings
     * @see AudioCheck#isValid(File, boolean, boolean)
     * @see AudioCheck#checkWavFileWithClipThreshold(File, boolean, boolean)
     */
    ValidityAndDur(Validity validity, double dur, boolean quietAudioOK) {
      this.validity = validity;
      this.durationInMillis = (int) (1000d * dur);
      isValid = validity ==
          Validity.OK ||
          (validity == Validity.TOO_QUIET && quietAudioOK);
    }

    public boolean isValid() {
      return isValid;
    }

    public Validity getValidity() {
      return validity;
    }

    public double getDynamicRange() {
      return maxMinRange;
    }

    void setMaxMinRange(double maxMinRange) {
      this.maxMinRange = maxMinRange;
    }

    /**
     * @param duration
     * @see AudioConversion#convertBase64ToAudioFiles
     */
    public void setDuration(double duration) {
      this.durationInMillis = (int) (1000d * duration);
    }

   /* String dump() {
      return getValidity() + "," + durationInMillis + "," + maxMinRange;
    }*/

    public int getDurationInMillis() {
      return durationInMillis;
    }

    public String toString() {
      return "valid " + getValidity() + " dur " + durationInMillis + " dnr " + maxMinRange;
    }
  }
}
