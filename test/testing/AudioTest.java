package testing;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.DynamicRange;
import org.apache.commons.io.FileUtils;
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

  @Test
  public void testTrim() {
    String file = "/Users/go22670/netPron2/snrs-no-hp";
    try {
      File file1 = new File(file);
      File[] files = file1.listFiles();
      for (File f : files) {
        if (f.getName().endsWith(".wav")) {
          double durationInSeconds = new AudioCheck(null).getDurationInSeconds(f);
        //  System.out.println("before " + durationInSeconds);
          File replacement = File.createTempFile("dude",".wav");
          FileUtils.copyFile(f, replacement);
          System.out.println("2 before " + new AudioCheck(null).getDurationInSeconds(replacement));

          //System.out.println("Got " + replacement.getName());
         // String trimmed = new AudioConversion(null).doTrimSilence(replacement.getAbsolutePath());
          /*File trimmedFile =*/
          double v = new AudioConversion(null).trimSilence(replacement);
       //   File file2 = new File(trimmed);
       //   System.out.println("after " + trimmed + " exists " + file2.exists()+
       //       ": "+new AudioCheck(null).getDurationInSeconds(file2) + " : " +v);

          System.out.println("after " + replacement + " exists " + replacement.exists()+
              ": "+new AudioCheck(null).getDurationInSeconds(replacement)+ " : " + v);


          // File file2=f;
         // DynamicRange.RMSInfo dynamicRange = new DynamicRange().getDynamicRange(file2);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
