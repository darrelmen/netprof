package mitll.langtest.server;

import ag.experiment.AutoGradeExperiment;
import mira.classifier.Classifier;
import mitll.langtest.server.database.Export;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AutoCRT support -- basically wrapping Jacob's work that lives in mira.jar <br></br>
 * Does some work to make a lm and lattice file suitable for doing small vocabulary decoding.
 *
 * User: GO22670
 * Date: 1/10/13
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class AutoCRT {
  private static Logger logger = Logger.getLogger(AutoCRT.class);

  private static final double MINIMUM_FLASHCARD_PRON_SCORE = 0.19;
  private Classifier<AutoGradeExperiment.Event> classifier = null;
  private Map<String, Export.ExerciseExport> exerciseIDToExport;
  private String installPath;
  private String mediaDir;
  private final Export exporter;
  private AutoCRTScoring db;

  public AutoCRT(Export exporter, AutoCRTScoring db, String installPath, String relativeConfigDir,
                 String backgroundFile) {
    this.installPath = installPath;
    this.mediaDir = relativeConfigDir;
    this.exporter = exporter;
    this.db = db;
  }

  /**
   * Get an auto crt reco output and score given an audio answer.
   *
   * @see LangTestDatabaseImpl#getAudioAnswer
   * @param exerciseID
   * @param e
   * @param audioFile
   * @param questionID
   * @param answer
   */
  public void getAutoCRTDecodeOutput(String exerciseID, int questionID, Exercise e, File audioFile,
                                     AudioAnswer answer) {
    List<String> exportedAnswers = getExportedAnswers(exerciseID, questionID);
    logger.info("got answers " + new HashSet<String>(exportedAnswers));

    PretestScore asrScoreForAudio = db.getASRScoreForAudio(audioFile, exportedAnswers);

    String recoSentence = asrScoreForAudio.getRecoSentence();
    logger.info("reco sentence was '" + recoSentence + "'");

    String annotatedResponse = getAnnotatedResponse(exerciseID, questionID, recoSentence);

    double scoreForAnswer = (recoSentence.length() > 0) ? getScoreForExercise(e, questionID, recoSentence) :0.0d;

    answer.setDecodeOutput(annotatedResponse);
    answer.setScore(scoreForAnswer);
  }

  /**
   * @see LangTestDatabaseImpl#getAudioAnswer(String, int, int, int, java.io.File, mitll.langtest.server.AudioCheck.ValidityAndDur, String, boolean)
   * @see LangTestDatabaseImpl#isMatch
   * @param e
   * @param audioFile
   * @param answer
   */
  public void getFlashcardAnswer(Exercise e,
                                 File audioFile,
                                 AudioAnswer answer) {
    List<String> foregroundSentences = getRefSentences(e);
    List<String> foreground = new ArrayList<String>();
    for (String ref : foregroundSentences) {
      foreground.add(removePunct(ref));
    }

    logger.debug("getFlashcardAnswer : foreground " + foreground);
    PretestScore asrScoreForAudio = db.getASRScoreForAudio(audioFile, foreground);

    String recoSentence =
      asrScoreForAudio != null && asrScoreForAudio.getRecoSentence() != null ?
        asrScoreForAudio.getRecoSentence().toLowerCase().trim() : "";
    boolean isCorrect = recoSentence != null && isCorrect(foregroundSentences, recoSentence);
    double scoreForAnswer = (asrScoreForAudio == null || asrScoreForAudio.getHydecScore() == -1) ? -1 : asrScoreForAudio.getHydecScore();
    answer.setCorrect(isCorrect && scoreForAnswer > MINIMUM_FLASHCARD_PRON_SCORE);
    if (!isCorrect) {
      logger.info("incorrect response for exercise #" +e.getID() +
        " reco sentence was '" + recoSentence + "' vs " + "'"+foregroundSentences +"' pron score was " + scoreForAnswer);
    }

    answer.setDecodeOutput(recoSentence);
    answer.setScore(scoreForAnswer);
  }

  private boolean isCorrect(List<String> answerSentences, String recoSentence) {
    for (String answer : answerSentences) {
      String converted = answer.replaceAll("-", " ").replaceAll("\\.", "").toLowerCase();
      if (converted.equalsIgnoreCase(recoSentence)) return true;
    }
    return false;
  }

  /**
   * @param other
   * @return
   */
  private List<String> getRefSentences(Exercise other) {
    List<String> refs = new ArrayList<String>();
    for (String ref : other.getRefSentences()) {
      refs.add(ref.trim().toUpperCase());
    }
    return refs;
  }

  /**
   * Mark words in the response as right or wrong depending on their overlap with answers marked
   * good or bad.
   * Mainly for demo purposes.
   * @param exercise
   * @param questionID
   * @param recoSentence
   * @return to show to user
   */
  private String getAnnotatedResponse(String exercise, int questionID, String recoSentence) {
    List<String> good = new ArrayList<String>();
    List<String> bad = new ArrayList<String>();
    for (Export.ResponseAndGrade resp : getExportForExercise(exercise, questionID).rgs) {
      if (resp.grade >= 0.6) good.add(resp.response);
      else bad.add(resp.response);
    }

    Set<String> goodTokens = getTokenSet(good);
    Set<String> badTokens = getTokenSet(bad);

    StringBuilder sb = new StringBuilder();
    for (String recoToken : recoSentence.split("\\s")) {
      if (goodTokens.contains(recoToken)) {
        sb.append("<u>"+recoToken +"</u> ");
      } else if (badTokens.contains(recoToken)) {
        sb.append("<s>"+recoToken +"</s> ");
      } else sb.append(recoToken + " ");
    }
    return sb.toString().trim();
  }

  /**
   * For this exercise and question, score the answer.<br></br>
   * Do this by getting all other answers to this question and the answer key and given this information
   * and the answer, ask the classifier to score the answer.
   * @see LangTestDatabaseImpl#getScoreForAnswer(mitll.langtest.shared.Exercise, int, String)
   * @param e for this exercise
   * @param questionID for this question (when multiple questions in an exercise)
   * @param answer to score (correct->incorrect)
   * @return 0-1
   */
  public double getScoreForExercise(Exercise e, int questionID, String answer) {
    return getScoreForExercise(e.getID(), questionID, answer);
  }
  private double getScoreForExercise(String id, int questionID, String answer) {
    getClassifier();
    String key = id + "_" + questionID;
    Export.ExerciseExport exerciseExport = getExportForExercise(key);
    if (exerciseExport == null) {
      logger.error("couldn't find exercise id " + key + " in " + exerciseIDToExport.keySet());
      return 0d;
    }
    else {
      double score = AutoGradeExperiment.getScore(getClassifier(), answer, exerciseExport);
      logger.info("Score was " + score + " for " + exerciseExport);
      return score;
    }
  }

  private Set<String> getTokenSet(List<String> exportedAnswers) {
    Set<String> tokens = new HashSet<String>();
    for (String l : exportedAnswers) {
      for (String t : l.split("\\s")) {
        String tt = removePunct(t);
        if (tt.trim().length() > 0) {
          tokens.add(tt.trim());
        }}}
    return tokens;
  }

  private String removePunct(String t) {
    return t.replaceAll("\\p{P}","");
  }

  /**
   * @see #getAutoCRTDecodeOutput(String, int, mitll.langtest.shared.Exercise, java.io.File, mitll.langtest.shared.AudioAnswer)
   * @param id
   * @param questionID
   * @return
   */
  private List<String> getExportedAnswers(String id, int questionID) {
    getClassifier();

    List<String> answers = new ArrayList<String>();
    for (Export.ResponseAndGrade resp : getExportForExercise(id, questionID).rgs) answers.add(resp.response);
    return answers;
  }

  private Export.ExerciseExport getExportForExercise(String id, int questionID) {
    return getExportForExercise(id + "_" + questionID);
  }
  private Export.ExerciseExport getExportForExercise(String key) {
    return exerciseIDToExport.get(key);
  }

  /**
   * Make a classifier given the export date, which has answers and their grades.<br></br>
   * The export data also includes the answer key for each question.<br></br>
   * This uses Jacob's classifier.
   * @see Export.ExerciseExport
   * @see AutoGradeExperiment
   * @return a mira classifier
   */
  private Classifier<AutoGradeExperiment.Event> getClassifier() {
    if (classifier != null) return classifier;
    Set<String> allAnswers = new HashSet<String>();
    List<Export.ExerciseExport> export = exporter.getExport(true, false);
    exerciseIDToExport = new HashMap<String, Export.ExerciseExport>();
    for (Export.ExerciseExport exp : export) {
      exerciseIDToExport.put(exp.id,exp);
      for (Export.ResponseAndGrade rg : exp.rgs) allAnswers.add(rg.response);
    }
    String[] args = new String[6];

    String configDir = (installPath != null ? installPath + File.separator : "") + mediaDir + File.separator;
    String config = configDir + "runAutoGradeWinNoBad.cfg";     // TODO use template for deploy/platform specific config
    if (!new File(config).exists()) logger.error("couldn't find " + config);
    args[0] = "-C";
    args[1] = config;
    args[2] = "-log";
    args[3] =  configDir + "out.log";
    args[4] = "-blacklist-file";
    args[5] = configDir + "blacklist.txt";

    ag.experiment.AutoGradeExperiment.main(args);
    classifier = AutoGradeExperiment.getClassifierFromExport(export);
    return classifier;
  }
}
