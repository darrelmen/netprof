package mitll.langtest.server.audio;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.AudioAnswer;
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
 *
 * User: GO22670
 * Date: 5/16/12
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
  private static final double LOG_OF_TEN = Math.log(10.0);

  private static final short clippedThreshold  = 32704; // 32768-64
  private static final short clippedThresholdMinus  = -32704; // 32768-64
  private static final short ct = 32112;
 // private static final short clippedThreshold2 = 32752; // 32768-16
 // private static final short clippedThreshold2Minus = -32752; // 32768-16
  private static final float MAX_VALUE = 32768.0f;
  private final int MIN_DYNAMIC_RANGE;
  ServerProperties props;

  public AudioCheck(ServerProperties props) {
    this.props = props;
    this.MIN_DYNAMIC_RANGE = props == null ? 26: props.getMinDynamicRange();
  }

  private double dB(double power) {
    return 20.0 * Math.log(power < 0.0001f ? 0.0001f : power) / LOG_OF_TEN;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getImageForAudioFile
   * @param file
   * @return
   */
  public double getDurationInSeconds(String file) { return getDurationInSeconds(new File(file)); }

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
   * @param file
   * @return
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
        logger.error("got " +e,e);
      }
      logger.error("got " +e,e);
    }
    return 0d;
  }

  private double getDurationInSeconds(AudioInputStream audioInputStream) {
    long frames = audioInputStream.getFrameLength();
    AudioFormat format = audioInputStream.getFormat();
    return (frames + 0.0d) / format.getFrameRate();
  }

  public ValidityAndDur checkWavFileRejectAnyTooLoud(File file) {
    ValidityAndDur validityAndDur = checkWavFileWithClipThreshold(file, false);
    addDynamicRange(file, /*isBrowser,*/ validityAndDur);
    return validityAndDur;
  }

  /**
   * TODO : consider passing in more isBrowser
   * @see AudioConversion#isValid
   * @param file
   * @return
   */
  public ValidityAndDur checkWavFile(File file) {
    ValidityAndDur validityAndDur = checkWavFileWithClipThreshold(file, true);
    addDynamicRange(file, /*false, */validityAndDur);
    return validityAndDur;
  }

  private void addDynamicRange(File file, /*boolean isBrowser,*/ ValidityAndDur validityAndDur) {
    String highPassFilterFile = new AudioConversion(props).getHighPassFilterFile(file.getAbsolutePath());
    DynamicRange.RMSInfo dynamicRange = new DynamicRange().getDynamicRange(new File(highPassFilterFile));
    if (dynamicRange.maxMin < MIN_DYNAMIC_RANGE) {
      logger.warn("file " + file.getName() + " doesn't meet dynamic range threshold (" + MIN_DYNAMIC_RANGE+
          "):\n" + dynamicRange);
      validityAndDur.validity = AudioAnswer.Validity.SNR_TOO_LOW;
    }
    validityAndDur.setMaxMinRange(dynamicRange.maxMin);
  }

  /**
   * Verify audio messages
   *
   * @see AudioConversion#isValid(File, boolean)
   * @param file audio byte array with header
   * @return true if well formed
   */
  private ValidityAndDur checkWavFileWithClipThreshold(File file, boolean usePercent){
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
      assert(fsize == 2);
      assert(format.getChannels() == 1);
      double dur = getDurationInSeconds(ais);

      long frameLength = ais.getFrameLength();
      if (frameLength < MinRecordLength) {
        logger.debug("INFO: audio recording too short (Length: " + frameLength + ") < min (" +MinRecordLength+ ") ");
        return new ValidityAndDur(AudioAnswer.Validity.TOO_SHORT, dur);
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
        fpower  = (float) dB((double) fpower);

        pm += fpower;
        p2 += fpower * fpower;
        n  += (float) fsize / 2.0f;
      }

      float clippedRatio = ((float)countClipped) / (float)frameLength;
      float clippedRatio2 = ((float)cc) / (float)frameLength;
      boolean wasClipped = usePercent ? clippedRatio > CLIPPED_RATIO : countClipped > 1;
      boolean wasClipped2 = usePercent ? clippedRatio2 > CLIPPED_RATIO : cc > 1;
/*      logger.info("of " + total +" got " +countClipped + " out of " + n +"  or " + clippedRatio  + "/" +clippedRatio2+
        " not " + notClippedRatio +" wasClipped = " + wasClipped);*/

      float mean = pm / n;
      float var  = p2 / n - mean * mean;
      double std = Math.sqrt(var);
      final boolean validAudio = mean > PowerThreshold || std > VarianceThreshold;

      if (wasClipped || !validAudio) {
        logger.info("checkWavFile: audio recording (Length: " + frameLength + ") " +
          "mean power = " + mean + " (dB) vs " + PowerThreshold +
          ", std = " + std + " vs " + VarianceThreshold+
          " valid = " + validAudio +
                " was clipped " + wasClipped + " (" + (clippedRatio * 100f) + "% samples clipped, # clipped = " + countClipped+ ") " +
            " was clipped " + wasClipped2 + " (" + (clippedRatio2 * 100f) + "% samples clipped, # clipped = " + cc+ ")" +
                " max = " + max + "/" +nmax
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

      ValidityAndDur validityAndDur = new ValidityAndDur(validity, dur);

      //if (validityAndDur.validity != AudioAnswer.Validity.OK) {
        //logger.info("validity " + validityAndDur);
      //}
      return validityAndDur;
    } catch (Exception e) {
      logger.error("Got " +e,e);
    } finally {
      try {
        if (ais != null) ais.close();
      } catch (IOException e) {
       logger.error("Got " +e,e);
      }
    }

    return new ValidityAndDur(AudioAnswer.Validity.INVALID);
  }

  public static class ValidityAndDur {
    private AudioAnswer.Validity validity;
    public final int durationInMillis;
    private double maxMinRange;

    public ValidityAndDur(AudioAnswer.Validity validity) {
      this(validity, 0d);
    }

    public ValidityAndDur(AudioAnswer.Validity validity, double dur) {
      this.validity = validity;
      this.durationInMillis = (int) (1000d * dur);
    }

    //public void setValidity(AudioAnswer.Validity validity) { this.validity = validity;}

    public AudioAnswer.Validity getValidity() {
      return validity;
    }

    public double getMaxMinRange() {
      return maxMinRange;
    }

    public void setMaxMinRange(double maxMinRange) {
      this.maxMinRange = maxMinRange;
    }

    public String toString() { return "valid " + getValidity() + " dur " + durationInMillis + " max min " + maxMinRange; }
  }
}
