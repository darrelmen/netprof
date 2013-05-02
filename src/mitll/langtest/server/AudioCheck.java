package mitll.langtest.server;

import mitll.langtest.shared.AudioAnswer;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

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
  private static Logger logger = Logger.getLogger(AudioCheck.class);

  private static final int MinRecordLength = 1*(10000/2); // 10000 = 0.7 second
  private static final int WinSize = 10;
  private static final float PowerThreshold = -85.0f;//-55.0f;
  private static final float VarianceThreshold = 20.0f;
  private static final double LOG_OF_TEN = Math.log(10.0);

  private double dB(double power) {
    return 20.0 * Math.log(power < 0.0001f ? 0.0001f : power) / LOG_OF_TEN;
  }

  /**
   * @see LangTestDatabaseImpl#getImageForAudioFile(int, String, String, int, int)
   * @param file
   * @return
   */
  public double getDurationInSeconds(String file) {
    return getDurationInSeconds(new File(file));
  }

  /**
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise
   * @param file
   * @return
   */
  public double getDurationInSeconds(File file) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
      double dur = getDurationInSeconds(audioInputStream);
      audioInputStream.close();
      return dur;
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0d;
  }

  private double getDurationInSeconds(AudioInputStream audioInputStream) {
    long frames = audioInputStream.getFrameLength();


    logger.info("got " + frames + " frames");

    AudioFormat format = audioInputStream.getFormat();
    return (frames + 0.0d) / format.getFrameRate();
  }

  /**
   * Verify audio messages
   *
   * @see AudioConversion#isValid(java.io.File)
   * @param file audio byte array with header
   * @return true if well formed
   */
  public ValidityAndDur checkWavFile(File file){
    AudioInputStream ais = null;
    try {
      ais = AudioSystem.getAudioInputStream(file);
      AudioFormat format = ais.getFormat();
      boolean bigEndian = format.isBigEndian();
      if (bigEndian) {
        logger.warn("huh? file " + file.getAbsoluteFile() + " is in big endian format?");
      }
      logger.info("encoding " + format.getEncoding() + " big endian = " + bigEndian);
     // ByteOrder byteOrder = bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
      int fsize = format.getFrameSize();
      assert(fsize == 2);
      assert(format.getChannels() == 1);
      double dur = getDurationInSeconds(ais);

      long frameLength = ais.getFrameLength();
      if (frameLength < MinRecordLength) {
        logger.debug("INFO: audio recording too short (Length: " + frameLength + ") < min (" +MinRecordLength+ ") ");
        return new ValidityAndDur(AudioAnswer.Validity.TOO_SHORT, dur);
      }


 /*     ShortBuffer sbuf =
        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
      short[] audioShorts = new short[sbuf.capacity()];
      sbuf.get(audioShorts);
*/

      // Verify audio power
      float pm = 0.0f, p2 = 0.0f, n = 0.0f;
      int bufSize = WinSize * fsize;
      byte[] buf = new byte[bufSize];
      float countClipped = 0f;
     // float countNotClipped = 0f;
      long frameIndex = 0;
      long frameIndex2 = 0;
      while (ais.read(buf) == bufSize) {
        float fpower = 0.0f/*, ns = 0.0f*/;
        float fpower2 = 0.0f/*, ns = 0.0f*/;
        for (int i = 0; i < bufSize; i += fsize)
          for (int s = 0; s < fsize; s += 2) {
           // short tmp = (short) ((buf[i + s] << 8) | buf[i + s + 1]);
            short tmp = (short) ((buf[i + s] & 0xFF) | (buf[i + s + 1] << 8));

            float r = ((float) tmp) / 32768.0f;
            if (Math.abs(r) > 0.98f) {
              countClipped++;
       /*       logger.debug("at " + frameIndex + " s " + s + " i " + i +
                " value was " + tmp + " and r " + r);*/
            }
            frameIndex++;
            fpower += r * r;
            //ns += 1.0f;
          }
/*
        ShortBuffer sbuf = ByteBuffer.wrap(buf).order(byteOrder).asShortBuffer();
        short[] audioShorts = new short[sbuf.capacity()];
        sbuf.get(audioShorts);
        for (short sample : audioShorts) {
          float r = ((float) sample) / 32768.0f;
          if (Math.abs(r) > 0.98f) {
            countClipped++;
            logger.debug("at " + frameIndex + //" s " + s + " i " + i+
              " value was " + sample + " and r " + r);
          }
          frameIndex2++;
          fpower2 += r * r;
        }*/

        fpower /= (float) WinSize;
        fpower = (float) dB((double)fpower);

        pm += fpower;
        p2 += fpower * fpower;
        n += (float) fsize / 2.0f;
      }

      float clippedRatio = countClipped / (float)frameLength;
      boolean wasClipped = clippedRatio > 0.005;
/*      logger.info("of " + total +" got " +countClipped + " out of " + n +"  or " + clippedRatio  + "/" +clippedRatio2+
        " not " + notClippedRatio +" wasClipped = " + wasClipped);*/

      float mean = pm / n;
      float var = p2 / n - mean * mean;
      final boolean validAudio = mean > PowerThreshold || Math.sqrt(var) > VarianceThreshold;
      logger.info("checkWavFile: audio recording (Length: " + frameLength + ") " +
        "mean power = " + mean + " (dB), std = " + Math.sqrt(var) + " valid = " + validAudio + " was clipped " + wasClipped + " (" +(clippedRatio*100f)+
        "% samples clipped)");
      ValidityAndDur validityAndDur = new ValidityAndDur(validAudio ? AudioAnswer.Validity.OK : AudioAnswer.Validity.TOO_QUIET, dur);

      if (validityAndDur.validity != AudioAnswer.Validity.OK) {
        logger.info("validity " + validityAndDur);
      }
      return validityAndDur;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (ais != null) ais.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return new ValidityAndDur(AudioAnswer.Validity.INVALID);
  }

  public static class ValidityAndDur {
    public AudioAnswer.Validity validity;
    public int durationInMillis;

    public ValidityAndDur(AudioAnswer.Validity validity) {
      this(validity, 0d);
    }

    public ValidityAndDur(AudioAnswer.Validity validity, double dur) {
      this.validity = validity;
      this.durationInMillis = (int) (1000d * dur);
    }
    public String toString() { return "valid " + validity + " dur " + durationInMillis; }
  }

  public static void main(String []a ) {
    try {
//      boolean b = new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-006\\0\\subject-1\\answer.wav"));
   //   double b = new AudioCheck().getDurationInSeconds(new File("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-006\\0\\subject-1\\answer.wav"));
      new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\answer_1367419336128.wav"));
      new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\answer_1367440890259.wav"));
      new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\answer_1367366259387.wav"));
      new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\answer_1367366259387.wav"));
      new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\bootstrap\\netPron2\\answer_1367352029073.wav"));
   //   System.out.println("duration " + b);
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }
}
