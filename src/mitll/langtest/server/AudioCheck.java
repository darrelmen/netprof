package mitll.langtest.server;

import mitll.langtest.shared.AudioAnswer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
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
  private static final int MinRecordLength = 1*(10000/2); // 10000 = 0.7 second
  private static final int WinSize = 10;
  private static final float PowerThreshold = -55.0f;
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
   * @see mitll.langtest.server.scoring.ASRScoring#scoreRepeatExercise(String, String, String, String, String, int, int, boolean, java.util.List, java.util.List, java.util.List)
   * @param file
   * @return
   */
  public double getDurationInSeconds(File file) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
      long frames = audioInputStream.getFrameLength();
      AudioFormat format = audioInputStream.getFormat();
      double dur = (frames + 0.0d) / format.getFrameRate();
      audioInputStream.close();
      return dur;
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0d;
  }
  /**
   * Verify audio messages
   *
   * @see AudioConversion#isValid(java.io.File)
   * @param file audio byte array with header
   * @return true if well formed
   */
  public AudioAnswer.Validity checkWavFile(File file){
    AudioInputStream ais = null;
    try {
      ais = AudioSystem.getAudioInputStream(file);
      int sSize = ais.getFormat().getFrameSize();
      assert(sSize == 2);
      assert(ais.getFormat().getChannels() == 1);

      if (ais.getFrameLength() < MinRecordLength) {
        System.err.println("INFO: audio recording too short (Length: " + ais.getFrameLength() + ") < min (" +MinRecordLength+ ") ");
        return AudioAnswer.Validity.TOO_SHORT;
      }
      int fsize = ais.getFormat().getFrameSize();

      // Verify audio power
      float pm = 0.0f, p2 = 0.0f, n = 0.0f;
      byte[] buf = new byte[WinSize * fsize];

      while (ais.read(buf) == WinSize * fsize) {
        float fpower = 0.0f/*, ns = 0.0f*/;
        for (int i = 0; i < WinSize * fsize; i += fsize)
          for (int s = 0; s < fsize; s += 2) {
            short tmp = (short) ((buf[i + s] << 8) | buf[i + s + 1]);
            float r = (float) tmp / 32768.0f;
            fpower += r * r;
            //ns += 1.0f;
          }
        fpower /= (float) WinSize;
        fpower = (float) dB(fpower);

        pm += fpower;
        p2 += fpower * fpower;
        n += (float) fsize / 2.0f;
      }

      float mean = pm / n;
      float var = p2 / n - mean * mean;
      final boolean validAudio = mean > PowerThreshold || Math.sqrt(var) > VarianceThreshold;
      System.out.println("INFO: audio recording (Length: " + ais.getFrameLength() + ") " +
        "mean power = " + mean + " (dB), std = " + Math.sqrt(var) + " valid = " + validAudio);
      return validAudio ? AudioAnswer.Validity.OK : AudioAnswer.Validity.TOO_QUIET;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (ais != null) ais.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return AudioAnswer.Validity.INVALID;
  }

  public static void main(String []a ) {
    try {
//      boolean b = new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-006\\0\\subject-1\\answer.wav"));
      double b = new AudioCheck().getDurationInSeconds(new File("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-006\\0\\subject-1\\answer.wav"));
      System.out.println("duration " + b);
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }
}
