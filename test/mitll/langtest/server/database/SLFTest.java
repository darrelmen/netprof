package mitll.langtest.server.database;

import mitll.langtest.server.audio.SLFFile;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class SLFTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(SLFTest.class);

  @Test
  public void testSLF() {
    // String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(Arrays.asList("first second third"), true);
    // boolean addSil = true;
    List<String> oneWord = Arrays.asList("first");

    String expected = "VERSION=1.0;N=3 L=2;I=0 W=<s>;I=1 W=</s>;I=2 W=first;J=0 S=0 E=2 l=-1.00;J=1 S=2 E=1 l=-1.00";
    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(oneWord, false, false, false);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals(simpleSLFFile[0], expected);
    }
    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(oneWord, true, false, false);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals(simpleSLFFile[0], expected);

    }

    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(oneWord, false, false, true);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals("VERSION=1.0;N=3 L=4;I=0 W=<s>;I=1 W=</s>;I=2 W=first;J=0 S=0 E=0 l=-1.00;J=1 S=0 E=2 l=-1.00;J=2 S=2 E=1 l=-1.00;J=3 S=1 E=1 l=-1.00", simpleSLFFile[0]);
    }


    List<String> twoWords = Arrays.asList("first second");
    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(twoWords, false, false, false);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals(simpleSLFFile[0], "VERSION=1.0;N=4 L=3;I=0 W=<s>;I=1 W=</s>;I=2 W=first;I=3 W=second;J=0 S=0 E=2 l=-1.00;J=1 S=2 E=3 l=-1.00;J=2 S=3 E=1 l=-1.00");
    }
    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(twoWords, true, false, false);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals(simpleSLFFile[0], "VERSION=1.0;N=5 L=5;I=0 W=<s>;I=1 W=</s>;I=2 W=first;I=3 W=SIL;I=4 W=second;J=0 S=0 E=2 l=-1.00;J=1 S=2 E=3 l=-1.00;J=2 S=2 E=4 l=-1.00;J=3 S=3 E=4 l=-1.00;J=4 S=4 E=1 l=-1.00");
    }
    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(twoWords, false, false, true);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals("VERSION=1.0;N=4 L=5;I=0 W=<s>;I=1 W=</s>;I=2 W=first;I=3 W=second;J=0 S=0 E=0 l=-1.00;J=1 S=0 E=2 l=-1.00;J=2 S=2 E=3 l=-1.00;J=3 S=3 E=1 l=-1.00;J=4 S=1 E=1 l=-1.00", simpleSLFFile[0]);
    }

    List<String> threeWords = Arrays.asList("first second third");
    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(threeWords, false, false, false);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals(simpleSLFFile[0], "VERSION=1.0;N=5 L=4;I=0 W=<s>;I=1 W=</s>;I=2 W=first;I=3 W=second;I=4 W=third;J=0 S=0 E=2 l=-1.00;J=1 S=2 E=3 l=-1.00;J=2 S=3 E=4 l=-1.00;J=3 S=4 E=1 l=-1.00");

    }


    {
      String[] simpleSLFFile = new SLFFile().createSimpleSLFFile(threeWords, true, false, false);

      for (String line : simpleSLFFile) {
        logger.info(line);
      }
      Assert.assertEquals(simpleSLFFile[0], "VERSION=1.0;N=7 L=8;I=0 W=<s>;I=1 W=</s>;I=2 W=first;I=3 W=SIL;I=4 W=second;I=5 W=SIL;I=6 W=third;J=0 S=0 E=2 l=-1.00;J=1 S=2 E=3 l=-1.00;J=2 S=2 E=4 l=-1.00;J=3 S=3 E=4 l=-1.00;J=4 S=4 E=5 l=-1.00;J=5 S=4 E=6 l=-1.00;J=6 S=5 E=6 l=-1.00;J=7 S=6 E=1 l=-1.00");

    }
  }
}
