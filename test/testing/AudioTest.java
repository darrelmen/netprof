package testing;

import mitll.langtest.server.audio.DynamicRange;
import org.junit.Test;

import java.io.File;

/**
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

          //break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
