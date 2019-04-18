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

package mitll.testing;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.DynamicRange;
import mitll.langtest.server.database.DecodeTest;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class AudioTest {
  private static final Logger logger = LogManager.getLogger(AudioTest.class);

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
  public void testCheckMP3() {
    String file = "snrs-no-hp";
    try {
      File file1 = new File(file);
      if (!file1.exists()) logger.warn("Can't find " + file1.getAbsolutePath());
      File[] files = file1.listFiles();
      for (File f : files) {
        if (f.getName().endsWith(".mp3")) {
          AudioCheck audioCheck = new AudioCheck(false,26);
          AudioCheck.ValidityAndDur valid = audioCheck.isValid(f, false, false);
          double durationInSeconds = audioCheck.getDurationInSeconds(f);
          logger.info("for " + f.getName() + " " + valid);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCheck() {
    String file = "snrs-no-hp";
    try {
      File file1 = new File(file);
      if (!file1.exists()) logger.warn("Can't find " + file1.getAbsolutePath());
      File[] files = file1.listFiles();
      for (File f : files) {
        if (f.getName().endsWith(".wav")) {
          AudioCheck audioCheck = new AudioCheck(false,26);
          AudioCheck.ValidityAndDur valid = audioCheck.isValid(f, false, false);
          double durationInSeconds = audioCheck.getDurationInSeconds(f);
          logger.info("for " + f.getName() + " " + valid);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testTrim() {
    String file = "snrs-no-hp";
    try {
      File file1 = new File(file);
      if (!file1.exists()) logger.warn("Can't find " + file1.getAbsolutePath());
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

  private File copyAndTrim(AudioCheck audioCheck, File replacement) throws Exception {
    File destFile = new File(replacement.getParent(),"trim_"+replacement.getName());

    FileUtils.copyFile(replacement, destFile);

    double v2 = new AudioConversion(false,26).trimSilence(destFile).getDuration();

    double v3 = audioCheck.getDurationInSeconds(destFile);

    System.out.println("got " + v2 + " : " + v3);

    return destFile;
  }
}
