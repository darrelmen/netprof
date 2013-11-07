package mitll.langtest.server.audio;

import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Mandarin extends SplitAudio {
  public static final float LOW_SCORE_THRESHOLD = 0.2f;
  private static Logger logger = Logger.getLogger(Mandarin.class);

  public void correctMandarin(Map<String, Exercise> idToEx) {
    Map<String, String> englishToCorrection = getCorrection();

    int c = 0;
    int diff = 0;
    for (Exercise e : idToEx.values()) {
      String eng = e.getEnglishSentence();
      String chinese = englishToCorrection.get(eng);
      if (chinese != null) {
        c++;
        if (!e.getRefSentence().equals(chinese)) diff++;
        e.setRefSentence(chinese);
      }
    }
    logger.debug("diff " + diff + " count " + c + " vs " + idToEx.size());
  }

  private Map<String, String> getCorrection() {
    Map<String, String> englishToCorrection = new HashMap<String, String>();
    File file = new File("C:\\Users\\go22670\\DLITest\\bootstrap\\chineseAudio\\melot\\segmented_mandarin.tsv");
    try {
      if (!file.exists()) {
        logger.error("can't find '" + file + "'");
        return null;
      } /*else {
        // logger.debug("found file at " + file.getAbsolutePath());
      }*/
      BufferedReader reader = getReader(file);

      String line;

      int error = 0;
      int error2 = 0;
      int c = 0;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) continue;
        //  if (c++ < 10) logger.debug("line " +line);
        String[] split = line.split("\\t");
        try {
          englishToCorrection.put(split[0], split[1]);
        } catch (Exception e) {
          if (error++ < 10)
            logger.error("reading " + file.getAbsolutePath() + " line '" + line + "' split len " + split.length, e);
          error2++;
          //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
      reader.close();
      if (error2 > 0) logger.error("got " + error2 + " errors");
    } catch (Exception e) {
      logger.error("reading " + file.getAbsolutePath() + " got " + e, e);
    }

    return englishToCorrection;
  }

  public Map<String, String> getCorrection2() {
    Map<String, String> englishToCorrection = new HashMap<String, String>();
    File file = new File("C:\\Users\\go22670\\DLITest\\bootstrap\\chineseAudio\\melot\\segmented_mandarin.tsv");
    File file2 = new File("C:\\Users\\go22670\\DLITest\\bootstrap\\chineseAudio\\melot\\segmented_mandarin_utf8.tsv");

    try {
      if (!file.exists()) {
        logger.error("can't find '" + file + "'");
        return null;
      } /*else {
        // logger.debug("found file at " + file.getAbsolutePath());
      }*/
      FileOutputStream resourceAsStream = new FileOutputStream(file2);
      BufferedWriter utf8 = new BufferedWriter(new OutputStreamWriter(resourceAsStream, "UTF8"));
      BufferedReader reader = getReader(file);

      String line;

   /*   int error = 0;
      int error2 = 0;
      int c = 0;*/
      while ((line = reader.readLine()) != null) {
        utf8.write(line);
        utf8.write("\n");
      }
      reader.close();
      utf8.close();
      //if (error2 > 0) logger.error("got " + error2 + " errors");
    } catch (Exception e) {
      logger.error("reading " + file.getAbsolutePath() + " got " + e, e);
    }

    return englishToCorrection;
  }

  protected BufferedReader getReader(File lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
    return new BufferedReader(new InputStreamReader(resourceAsStream,"UTF16"));
  }

  protected void recordMissing(FileWriter missingFast, FileWriter missingSlow, String name) {
    try {
      recordMissingFast(missingFast, name);
      recordMissingFast(missingSlow, name);
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }
}
