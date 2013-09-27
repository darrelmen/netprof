package mitll.langtest.server.audio;

import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.Scores;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 5:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class CheckTamas extends SplitAudio {
  private static Logger logger = Logger.getLogger(CheckTamas.class);

  public void checkTamas() {
    File file = new File("89");
    file = new File(file, "0");
    file = new File(file, "subject-6");
    File[] files = file.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        String name = pathname.getName();
        return name.endsWith(".wav") && !name.contains("16K");
      }
    });
    logger.info("got " + files.length + " files ");

    String language = "english";
    final String configDir = getConfigDir(language);


    try {
      final Map<String, String> properties = getProperties(language, configDir);
      ASRScoring scoring = getAsrScoring(".", null, properties);

      for (File fastFile : files) {
        String fastName = fastFile.getName().replaceAll(".wav", "");
        Scores fast = getAlignmentScoresNoDouble(scoring, "fifty", fastName, fastFile.getParent(), getConverted(fastFile));
        logger.info("score for " + fastFile.getName() + " score " + fast);
      }
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  protected void recordMissing(FileWriter missingFast, FileWriter missingSlow, String name) {
    try {
      recordMissingFast(missingFast, name);
      recordMissingFast(missingSlow, name);
    } catch (IOException e) {
      SplitAudio.logger.error("got " + e, e);
    }
  }
}
