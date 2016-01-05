package mitll.langtest.server.autocrt;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;
import mitll.langtest.shared.exercise.CommonExercise;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by go22670 on 8/19/15.
 */
public class MiraClassifier {
  private static final Logger logger = Logger.getLogger(MiraClassifier.class);

  private static final String LANGUAGE = "language";
  private static final String QUESTION = "question";
  private static final String ANSKEY = "anskey";
  private static final String RESPONSE = "response";
  private static final boolean SHOW_DETAIL = false;

  /**
   * Mira expects forward slashes to separate possible answers.
   *
   * @param exercise
   * @param questionID
   * @param answer
   * @param url
   * @param additionalCorrect
   * @return
   * @see AutoCRT#getScoreForExercise(CommonExercise, int, String, float)
   */
  public Info getMiraScore(AmasExerciseImpl exercise, int questionID, String answer, String miraFlavor, String url,
                           Collection<String> additionalCorrect) {
    String id = exercise.getID();
    List<QAPair> foreignLanguageQuestions = exercise.getForeignLanguageQuestions();

    return getMiraScore(questionID, foreignLanguageQuestions, answer, miraFlavor, url, additionalCorrect, id);
  }

  /**
   *
   * @param questionID
   * @param foreignLanguageQuestions
   * @param answer
   * @param miraFlavor
   * @param url
   * @param additionalCorrect
   * @param id
   * @return
   */
  public Info getMiraScore(int questionID, List<QAPair> foreignLanguageQuestions, String answer,
                           String miraFlavor, String url, Collection<String> additionalCorrect, String id) {
    QAPair qaPair = foreignLanguageQuestions.get(questionID - 1);

    return getMiraScore(questionID, answer, miraFlavor, url, additionalCorrect, id, qaPair);
  }

  public Info getMiraScore(int questionID, String answer, String miraFlavor, String url,
                           Collection<String> additionalCorrect, String id, QAPair qaPair) {
    String question = qaPair.getQuestion();
    List<String> alternateAnswers = qaPair.getAlternateAnswers();

    return getMiraScore(questionID, answer, miraFlavor, url, id, question, alternateAnswers, additionalCorrect);
  }

  public Info getMiraScore(int questionID, String answer, String miraFlavor, String url, String id,
                           String question, List<String> alternateAnswers, Collection<String> additionalCorrect) {
    JSONObject object = new JSONObject();
    object.put(LANGUAGE, miraFlavor);
    object.put(QUESTION, question);

    Set<String> answers = new HashSet<>();

    answers.addAll(alternateAnswers);
    int before = answers.size();
    answers.addAll(additionalCorrect);
    int after = answers.size();

    StringBuilder builder = new StringBuilder();
    for (String a : answers) builder.append(a).append("/");
    String value = builder.toString();

    if (!value.isEmpty()) value = value.substring(0, value.length() - 1);

    object.put(ANSKEY, value);
    object.put(RESPONSE, answer);

    double grade = 0;
    double regressed = 0;
    try {
      String scoreInfo;
      try {
        if (SHOW_DETAIL) {
          logger.debug("mira score " + id + "/" + questionID + " : '" + answer + "' against " + answers.size() + " answers in answer key" +
              (after - before > 0 ?
                  " (with " + (after - before) + " additional graded)" : ""));
        }
        scoreInfo = new HTTPClient(url).sendAndReceive(object.toString());
      } catch (IOException e) {
        logger.info("Got " + e + " so trying dev host " + ServerProperties.MIRA_DEVEL_HOST);
        scoreInfo = new HTTPClient(ServerProperties.MIRA_DEVEL_HOST, true).sendAndReceive(object.toString());
      }

      JSONObject scoreJSON = JSONObject.fromObject(scoreInfo);

      //logger.debug("got " + scoreJSON);

      if (scoreJSON.get("error") != null) {
        logger.error("Got error " + scoreJSON.get("error") + " language was '" + miraFlavor + "'");
      } else {
        grade = scoreJSON.getDouble("grade");
        regressed = scoreJSON.getDouble("regressed");

        logger.debug("mira score " + id + "/" + questionID + " : '" + answer + "' against " + answers.size() + " answers in answer key" +
            (after - before > 0 ?
                " (with " + (after - before) + " additional graded)" : "") + " grade " + grade + " regressed " + regressed);
      }
    } catch (Exception e) {
      logger.error("Calling  " + url + " Got " + e, e);
      grade = 0;
    }

    return new Info(grade, regressed, answers.size());
  }

  public static class Info {
    private double grade;
    private double regressed;
    private int answerKeySize;

    public Info(double grade, double regressed, int answerKeySize) {
      this.grade = grade;
      this.regressed = regressed;
      this.answerKeySize = answerKeySize;
    }

    public double getGrade() {
      return grade;
    }

    public double getRegressed() {
      return regressed;
    }

    public int getAnswerKeySize() {
      return answerKeySize;
    }
  }
}
