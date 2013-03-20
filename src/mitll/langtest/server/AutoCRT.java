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
import java.util.Collection;
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
  private Classifier<AutoGradeExperiment.Event> classifier = null;
  private Set<String> allAnswers = new HashSet<String>();
  private Map<String, Export.ExerciseExport> exerciseIDToExport;
  private String installPath;
  private String mediaDir;
  private final Export exporter;
  private AutoCRTScoring db;

  public AutoCRT(Export exporter, AutoCRTScoring db, String installPath, String mediaDir) {
    this.installPath = installPath;
    this.mediaDir = mediaDir;
    this.exporter = exporter;
    this.db = db;
  }

  /**
   * Get an auto crt score given an audio answer.
   *
   * @see LangTestDatabaseImpl#writeAudioFile
   * @param exercise
   * @param e
   * @param reqid
   * @param file
   * @param validity
   * @param questionID
   * @param url
   * @param durationInMillis
   * @return
   */
  public AudioAnswer getAutoCRTAnswer(String exercise, Exercise e, int reqid, File file, AudioAnswer.Validity validity,
                                      int questionID, String url, int durationInMillis) {
    List<String> exportedAnswers = getExportedAnswers(exercise, questionID);
    logger.info("got answers " + new HashSet<String>(exportedAnswers));

    List<String> background = getBackgroundText(e);

    PretestScore asrScoreForAudio = db.getASRScoreForAudio(file, exportedAnswers, background);

    String recoSentence = asrScoreForAudio.getRecoSentence();
    logger.info("reco sentence was '" + recoSentence + "'");

    String annotatedResponse = getAnnotatedResponse(exercise, questionID, recoSentence);

    double scoreForAnswer = (recoSentence.length() > 0) ? getScoreForExercise(e, questionID, recoSentence) :0.0d;
    return new AudioAnswer(url, validity, annotatedResponse, scoreForAnswer, reqid, durationInMillis);
  }

  /**
   * @see LangTestDatabaseImpl#writeAudioFile
   * @param e
   * @param reqid
   * @param file
   * @param validity
   * @param url
   * @param durationInMillis
   * @param allExercises
   * @return
   */
  public AudioAnswer getFlashcardAnswer(Exercise e, int reqid, File file, AudioAnswer.Validity validity,
                                       String url, int durationInMillis,
                                      List<Exercise> allExercises) {
    List<String> foregroundSentences = getRefSentences(e);
    if (allExercises.isEmpty()) logger.error("getFlashcardAnswer : huh? no background sentences?");

    List<String> background = getBackground(e, allExercises);
    if (background.isEmpty()) logger.error("huh? background is empty despite having " + allExercises.size() + " ?");

    List<String> foreground = new ArrayList<String>();
    for (String ref : foregroundSentences) {
      foreground.add(removePunct(ref));
    }

    logger.debug("foreground " + foreground + " back " + background.subList(0,Math.min(10,background.size())) +"...");
    PretestScore asrScoreForAudio = db.getASRScoreForAudio(file, foreground, background);

    String recoSentence =
      asrScoreForAudio != null && asrScoreForAudio.getRecoSentence() != null ?
        asrScoreForAudio.getRecoSentence().toLowerCase().trim() : "";
    boolean isCorrect = recoSentence != null && isCorrect(foregroundSentences, recoSentence);
    if (!isCorrect) {
      logger.info("reco sentence was '" + recoSentence + "' vs " + "'"+foregroundSentences +"' correct = " + isCorrect);
    }

    double scoreForAnswer = isCorrect ? 1.0d :0.0d;
    return new AudioAnswer(url, validity, recoSentence, scoreForAnswer, reqid, durationInMillis);
  }

  private List<String> getBackground(Exercise exercise, List<Exercise> allExercises) {
    List<String> sentences = new ArrayList<String>();
    String exerciseID = exercise.getID();
    for (Exercise other : allExercises) {
      if (!other.getID().equals(exercise)) {
        sentences.addAll(getRefSentences(other));
      }
    }
    return getBackgroundSentences(sentences);
  }

  private boolean isCorrect(List<String> answerSentences, String recoSentence) {
    for (String answer : answerSentences) {
      String converted = answer.replaceAll("-", " ").replaceAll("\\.", "").toLowerCase();
      logger.debug("converted is " + converted);
      if (converted.equalsIgnoreCase(recoSentence)) return true;
    }
    return false;
/*    SmallVocabDecoder svDecoderHelper = new SmallVocabDecoder();
    List<String> fvocab = svDecoderHelper.getSimpleVocab(Collections.singletonList(converted), 50);
    if (fvocab.isEmpty()) logger.error("huh? foreground is empty for " +answerSentence);

    List<String> rvocab = svDecoderHelper.getSimpleVocab(Collections.singletonList(recoSentence), 50);
    if (rvocab.isEmpty()) logger.warn("recoSentence is empty for " +recoSentence);

    boolean b = rvocab.containsAll(fvocab);
    if (!b) logger.info("isCorrect - no match : reco " + rvocab + " vs answer " +fvocab);
    return b;*/
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

  /**
   * Get the background sentences to feed into a background language model.
   * The background is the question content (orientation, prompting, etc.) and all other
   * answers to all the other questions.
   * @param e to harvest the question content from
   * @return all background sentences
   */
  private List<String> getBackgroundText(Exercise e) {
    List<String> background = new ArrayList<String>();
    String content = e.getContent().replaceAll("\\<.*?\\>", "").replaceAll("&nbsp;", "");
    for (String line : content.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.length() > 0 && !trimmed.contains("Orient") && !trimmed.contains("Listen")) background.add(trimmed);
    }

    for (Exercise.QAPair pair : e.getForeignLanguageQuestions())  {
      background.add(pair.getQuestion());
    }

    Collection<String> sentences = getAllExportedAnswers();
    background.addAll(getBackgroundSentences(sentences));

    logger.info("background has " + background.size() + " lines");
    return background;
  }

  /**
   * @see #getBackgroundText(mitll.langtest.shared.Exercise)
   * @see #getFlashcardAnswer
   * @param sentences
   * @return
   */
  private List<String> getBackgroundSentences(Collection<String> sentences) {
    if (sentences.isEmpty()) logger.warn("getBackgroundSentences huh? no background sentences?");
    List<String> background = new ArrayList<String>();

    for (String answer : sentences) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < answer.length(); i++) {
        if (!Character.isDigit(answer.charAt(i))) {
          b.append(answer.charAt(i));
        }
        else {
          b.append(" ");
        }
      }
      String result = b.toString().trim();
      if (result.length() > 0) {
        background.add(removePunct(result));
      }
    }
    if (background.isEmpty()) logger.warn("huh? getBackgroundSentences no background sentences despite " +sentences.size()+ " inputs"+
      "?");

    return background;
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

  private Collection<String> getAllExportedAnswers() { return allAnswers; }

  /**
   * @see #getAutoCRTAnswer(String, mitll.langtest.shared.Exercise, int, java.io.File, mitll.langtest.shared.AudioAnswer.Validity, int, String, int)
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
/*  private Export.ExerciseExport getExportForExercise(Exercise e, int questionID) {
    return getExportForExercise(e.getID(), questionID);
  }*/
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
    allAnswers = new HashSet<String>();
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
