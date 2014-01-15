package mitll.langtest.server.audio;

import corpus.LTS;
import mitll.langtest.server.database.FileExerciseDAO;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 5:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class CheckLTS {
  private static Logger logger = Logger.getLogger(CheckLTS.class);

  private void checkLTS(List<Exercise> exercises, LTS lts) {
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ltsIssues.txt"), FileExerciseDAO.ENCODING));

      SmallVocabDecoder svd = new SmallVocabDecoder();
      int errors = 0;
      for (Exercise e : exercises) {
        String id = e.getID();

        if (checkLTS(Integer.parseInt(id), writer, svd, lts, e.getEnglishSentence(), e.getRefSentence())) errors++;
      }

      if (errors > 0) logger.error("found " + errors + " lts errors");
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean checkLTS(int id, BufferedWriter writer, SmallVocabDecoder svd, LTS lts, String english, String foreignLanguagePhrase) {
    Collection<String> tokens = svd.getTokens(foreignLanguagePhrase);
    boolean error = false;
    try {

      for (String token : tokens) {
        String[][] process = lts.process(token);
        if (process == null) {
          String message = "couldn't do lts on exercise #" + (id - 1) + " token '" + token +
            "' length " + token.length() + " trim '" + token.trim() +
            "' " +
            " '" + foreignLanguagePhrase + "' english = '" + english + "'";
          logger.error(message);
          //logger.error("\t tokens " + tokens + " num =  " + tokens.size());

          writer.write(message);
          writer.write("\n");
          error = true;
        }
      }
    } catch (Exception e) {
      logger.error("couldn't do lts on " + (id - 1) + " " + foreignLanguagePhrase + " " + english);
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return error;
  }
}
