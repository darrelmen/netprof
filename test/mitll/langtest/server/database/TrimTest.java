package mitll.langtest.server.database;

import mitll.langtest.server.audio.SLFFile;
import mitll.langtest.server.audio.TrimmerAIS;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class TrimTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(TrimTest.class);

  @Test
  public void testTrim() {
    String outputFile = "trimBogusBogus.wav";

    try {
      TrimmerAIS.trimFile("./bogus_bogus.wav", outputFile,0,10000);
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    logger.info("wrote to " +outputFile);

    try {
      String outputFile1 = "trimBogusBogus2.wav";
      TrimmerAIS.trimFile("./bogus_bogus.wav", outputFile1,2000,10000);

      logger.info("wrote to " +outputFile1);

    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      String outputFile1 = "trimBogusBogus3.wav";
      TrimmerAIS.trimFile("./bogus_bogus.wav", outputFile1,2000,40000);

      logger.info("wrote to " +outputFile1);

    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
