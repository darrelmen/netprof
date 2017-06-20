package mitll.testing;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.DynamicRange;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * String highPassFilterFile = conversion.getHighPassFilterFile(file.getAbsolutePath());
 * DynamicRange.RMSInfo dynamicRange = new DynamicRange().getDynamicRange(new File(highPassFilterFile));
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/30/15.
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
          String highPassFilterFile = new AudioConversion(false,26).getHighPassFilterFile(f.getAbsolutePath());
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
          AudioCheck audioCheck = new AudioCheck(false,26);
          double durationInSeconds = audioCheck.getDurationInSeconds(f);
          //  System.out.println("before " + durationInSeconds);
       //   File replacement = File.createTempFile("dude", ".wav");
          File replacement = new File(f.getParent(),"trim_"+f.getName());
          FileUtils.copyFile(f, replacement);
          System.out.println("2 before " + audioCheck.getDurationInSeconds(replacement));

          //System.out.println("Got " + replacement.getName());
          // String trimmed = new AudioConversion(null).doTrimSilence(replacement.getAbsolutePath());
          /*File trimmedFile =*/
          double v = new AudioConversion(false,26).trimSilence(replacement).getDuration();
          //   File file2 = new File(trimmed);
          //   System.out.println("after " + trimmed + " exists " + file2.exists()+
          //       ": "+new AudioCheck(null).getDurationInSeconds(file2) + " : " +v);

          System.out.println("after " + replacement + " exists " + replacement.exists() +
              ": " + audioCheck.getDurationInSeconds(replacement) + " : " + v);

          //File destFile = new File(replacement + "copy");
          for (int i = 0; i < 10; i++) {
            replacement = copyAndTrim(audioCheck, replacement);
          }
//          File file3 = copyAndTrim(audioCheck, file2);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public File copyAndTrim(AudioCheck audioCheck, File replacement) throws IOException {
    File destFile = new File(replacement.getParent(),"trim_"+replacement.getName());

    FileUtils.copyFile(replacement, destFile);

    double v2 = new AudioConversion(false,26).trimSilence(destFile).getDuration();

    double v3 = audioCheck.getDurationInSeconds(destFile);

    System.out.println("got " + v2 + " : " + v3);

    return destFile;
  }
}
