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
 *
 */

package mitll.langtest.server.audio;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.AudioAnswer;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;

/**
 * Checks for two things -- is the audio long enough ({@link #MinRecordLength} and is
 * it just a recording of silence by looking at average power and variance :
 * ({@link #PowerThreshold} and ({@link #VarianceThreshold}.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/16/12
 * Time: 2:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioCheck {
  private static final Logger logger = Logger.getLogger(AudioCheck.class);

  private static final int MinRecordLength = (10000 / 2); // 10000 = 0.7 second
  private static final int WinSize = 10;
  private static final float PowerThreshold = -79.50f;//-55.0f;
  private static final float VarianceThreshold = 20.0f;
  private static final double CLIPPED_RATIO = 0.005; // 1/2 %
  private static final double CLIPPED_RATIO_TIGHTER = 0.0025; // 1/2 %
  private static final double LOG_OF_TEN = Math.log(10.0);

  private static final short clippedThreshold = 32704; // 32768-64
  private static final short clippedThresholdMinus = -32704; // 32768-64
  private static final short ct = 32112;
  // private static final short clippedThreshold2 = 32752; // 32768-16
  // private static final short clippedThreshold2Minus = -32752; // 32768-16
  private static final float MAX_VALUE = 32768.0f;
  static final ValidityAndDur INVALID_AUDIO = new ValidityAndDur();
  //public static final int CLIPPED_FRAME_COUNT = 1;
  private final int MIN_DYNAMIC_RANGE;
  private final ServerProperties props;

  // TODO :make a server prop
  private final float FORGIVING_MIN_DNR = 18F;

  public AudioCheck(ServerProperties props) {
    this.props = props;
    this.MIN_DYNAMIC_RANGE = props == null ? 26 : props.getMinDynamicRange();
  }

  private double dB(double power) {
    return 20.0 * Math.log(power < 0.0001f ? 0.0001f : power) / LOG_OF_TEN;
  }

  /**
   * @param file
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getImageForAudioFile
   */
  public double getDurationInSeconds(String file) {
    return getDurationInSeconds(new File(file));
  }

  /**
   * @param file
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
   */
  public double getDurationInSeconds(File file) {
    AudioInputStream audioInputStream = null;
    try {
      audioInputStream = AudioSystem.getAudioInputStream(file);
      double dur = getDurationInSeconds(audioInputStream);
      audioInputStream.close();
      return dur;
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
    return (frames + 0.0d) / format.getFrameRate();
  }

  /**
   * @param file
   * @param quietAudioOK
   * @return
   * @see AudioConversion#isValid(File, boolean, boolean)
   */
  ValidityAndDur checkWavFileRejectAnyTooLoud(File file, boolean quietAudioOK) {
    ValidityAndDur validityAndDur = checkWavFileWithClipThreshold(file, false, quietAudioOK);
    if (validityAndDur.isValid()) {
      addDynamicRange(file, validityAndDur);
    }
    return validityAndDur;
  }

  /**
   * TODO : consider passing in more isBrowser
   *
   * @param file
   * @return
   * @see AudioConversion#isValid
   */
  ValidityAndDur checkWavFile(File file, boolean quietAudioOK) {
    ValidityAndDur validityAndDur = checkWavFileWithClipThreshold(file, true, quietAudioOK);
    if (validityAndDur.isValid()) {
      addDynamicRange(file, validityAndDur);
    }
    return validityAndDur;
  }

  private void addDynamicRange(File file, ValidityAndDur validityAndDur) {
    DynamicRange.RMSInfo dynamicRange = getDynamicRange(file);
    if (dynamicRange.maxMin < MIN_DYNAMIC_RANGE) {
      logger.warn("file " + file.getName() + " doesn't meet dynamic range threshold (" + MIN_DYNAMIC_RANGE +
          "):\n" + dynamicRange);
      validityAndDur.validity = AudioAnswer.Validity.SNR_TOO_LOW;
    }
    validityAndDur.setMaxMinRange(dynamicRange.maxMin);
  }

//  public boolean hasValidDynamicRange(File file) {
//    return getDynamicRange(file).maxMin >= MIN_DYNAMIC_RANGE;
//  }
//
// public boolean hasValidDynamicRangeForgiving(File file) {  return getDynamicRange(file).maxMin >= FORGIVING_MIN_DNR;  }

  private DynamicRange.RMSInfo getDynamicRange(File file) {
    String highPassFilterFile = new AudioConversion(props).getHighPassFilterFile(file.getAbsolutePath());
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
   * @param file audio byte array with header
   * @return true if well formed
   * @see AudioConversion#isValid(File, boolean, boolean)
   * @see #checkWavFile(File, boolean)
   * @see #checkWavFileRejectAnyTooLoud(File, boolean)
   */
  private ValidityAndDur checkWavFileWithClipThreshold(File file, boolean usePercent, boolean quietAudioOK) {
    AudioInputStream ais = null;
    try {
      ais = AudioSystem.getAudioInputStream(file);
      AudioFormat format = ais.getFormat();
      //logger.info("file " + file.getName() + " sample rate " + format.getSampleRate());

      boolean bigEndian = format.isBigEndian();
      if (bigEndian) {
        logger.warn("huh? file " + file.getAbsoluteFile() + " is in big endian format?");
      }
      int fsize = format.getFrameSize();
      assert (fsize == 2);
      assert (format.getChannels() == 1);
      double dur = getDurationInSeconds(ais);

      long frameLength = ais.getFrameLength();
      if (frameLength < MinRecordLength) {
        logger.debug("INFO: audio recording too short (Length: " + frameLength + ") < min (" + MinRecordLength + ") ");
        return new ValidityAndDur(AudioAnswer.Validity.TOO_SHORT, dur, false);
      }

      // Verify audio power
      float pm = 0.0f, p2 = 0.0f, n = 0.0f;
      int bufSize = WinSize * fsize;
      byte[] buf = new byte[bufSize];
      int countClipped = 0;
      int cc = 0;

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
            if (tmp > ct) cc++;
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
      float clippedRatio2 = ((float) cc) / (float) frameLength;
      boolean wasClipped = usePercent ? clippedRatio > CLIPPED_RATIO : clippedRatio > CLIPPED_RATIO_TIGHTER;// > CLIPPED_FRAME_COUNT;
      //  boolean wasClipped2 = usePercent ? clippedRatio2 > CLIPPED_RATIO : cc > 1;
/*      logger.info("of " + total +" got " +countClipped + " out of " + n +"  or " + clippedRatio  + "/" +clippedRatio2+
        " not " + notClippedRatio +" wasClipped = " + wasClipped);*/

      float mean = pm / n;
      float var = p2 / n - mean * mean;
      double std = Math.sqrt(var);
      final boolean validAudio = mean > PowerThreshold || std > VarianceThreshold;

      if (wasClipped || !validAudio) {
        logger.info("checkWavFile: audio recording (Length: " + frameLength + " frames) " +
            "mean power = " + mean + " (dB) vs " + PowerThreshold +
            ", std = " + std + " vs " + VarianceThreshold +
            " valid = " + validAudio +
            " was clipped (1) " + wasClipped + " (" + (clippedRatio * 100f) + "% samples clipped, # clipped = " + countClipped + ") " +
            // " was clipped (2) " + wasClipped2 + " (" + (clippedRatio2 * 100f) + "% samples clipped, # clipped = " + cc + ")" +
            " max = " + max + "/" + nmax
        );
      }

      boolean micDisconnected = mean < -79.999 && std < 0.001;

      AudioAnswer.Validity validity = validAudio ?
          (wasClipped ?
              AudioAnswer.Validity.TOO_LOUD :
              AudioAnswer.Validity.OK) :
          micDisconnected ?
              AudioAnswer.Validity.MIC_DISCONNECTED :
              AudioAnswer.Validity.TOO_QUIET;

      ValidityAndDur validityAndDur = new ValidityAndDur(validity, dur, quietAudioOK);

      //if (validityAndDur.validity != AudioAnswer.Validity.OK) {
      //logger.info("validity " + validityAndDur);
      //}
      return validityAndDur;
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

  public float getMinDNR() {
    return FORGIVING_MIN_DNR;
  }

  public float getDNR(File test) {
    DynamicRange.RMSInfo dynamicRange = getDynamicRange(test);
    return dynamicRange == null ? 0f : (float) dynamicRange.maxMin;
  }

  public static class ValidityAndDur {
    private AudioAnswer.Validity validity;
    private boolean isValid = false;
    public int durationInMillis;
    private double maxMinRange;

    ValidityAndDur() {
      this(AudioAnswer.Validity.INVALID, 0d, false);
    }

    public ValidityAndDur(double dur) {
      this(AudioAnswer.Validity.OK, dur, false);
    }

    /**
     * @param validity
     * @param dur
     * @parma quietAudioOK - only useful for automated load testing where we aren't really making recordings
     */
    ValidityAndDur(AudioAnswer.Validity validity, double dur, boolean quietAudioOK) {
      this.validity = validity;
      this.durationInMillis = (int) (1000d * dur);
      isValid = validity ==
          AudioAnswer.Validity.OK ||
          (validity == AudioAnswer.Validity.TOO_QUIET && quietAudioOK);
    }

    public boolean isValid() {
      return isValid; // == AudioAnswer.Validity.OK;
    }

    public AudioAnswer.Validity getValidity() {
      return validity;
    }

    public double getMaxMinRange() {
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

    public String toString() {
      return "valid " + getValidity() + " dur " + durationInMillis + " max min " + maxMinRange;
    }
  }
}
