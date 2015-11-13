package testing;

import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.DynamicRange;
import org.junit.Test;

import java.io.File;

/**
 *
 *    String highPassFilterFile = conversion.getHighPassFilterFile(file.getAbsolutePath());
 DynamicRange.RMSInfo dynamicRange = new DynamicRange().getDynamicRange(new File(highPassFilterFile));
 * Created by go22670 on 10/30/15.
 */
public class AudioTest {

  @Test
  public void testRMS() {
    String file = "/Users/go22670/netPron2/audio/";
    try {
      for (File f : new File(file).listFiles()) {
        if (f.getName().endsWith(".wav")) {
          System.out.println("Got " + f.getName());
          new DynamicRange().getDynamicRange(f);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testRMS2() {
    String file = "/Users/go22670/netPron2/snrs-no-hp";
    try {
      File file1 = new File(file);
      File[] files = file1.listFiles();
      for (File f : files) {
        if (f.getName().endsWith(".wav")) {
          System.out.println("Got " + f.getName());
          String highPassFilterFile = new AudioConversion(null).getHighPassFilterFile(f.getAbsolutePath());
          File file2 = new File(highPassFilterFile);
        // File file2=f;
          DynamicRange.RMSInfo dynamicRange = new DynamicRange().getDynamicRange(file2);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
