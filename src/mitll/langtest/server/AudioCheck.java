package mitll.langtest.server;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/16/12
 * Time: 2:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioCheck {
  private static final int MinRecordLength = 2*10000; // 10000 = 0.7 second
  private static final int WinSize = 10;
  private static final float PowerThreshold = -55.0f;
  private static final float VarianceThreshold = 20.0f;
  private static final double LOG_OF_TEN = Math.log(10.0);

  private double dB(double power) {
    return 20.0 * Math.log(power < 0.0001f ? 0.0001f : power) / LOG_OF_TEN;
  }

  /**
   * Verify audio messages
   *
   * @param file audio byte array with header
   * @return true if well formed
   */
  public boolean checkWavFile(File file){
    AudioInputStream ais = null;
    try {
      ais = AudioSystem.getAudioInputStream(file);
      int sSize = ais.getFormat().getFrameSize();
      assert(sSize == 2);
      assert(ais.getFormat().getChannels() == 1);

      if (ais.getFrameLength() < MinRecordLength) {
        System.err.println("INFO: audio recording (Length: " + ais.getFrameLength() + ") ");
        return false;
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
      System.err.println("INFO: audio recording (Length: " + ais.getFrameLength() + ") " +
        "mean power = " + mean + " (dB), std = " + Math.sqrt(var) + " valid = " + validAudio);
      return validAudio;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (ais != null) ais.close();
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }

    return false;
  }

  public static void main(String []a ) {
    try {
      boolean b = new AudioCheck().checkWavFile(new File("C:\\Users\\go22670\\DLITest\\LangTest\\war\\answers\\test\\ac-LC1-006\\0\\subject-1\\answer.wav"));
      System.out.println("vlaue " + b);
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }
}
