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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
  private static final int MAX_AUTO_CRT_VOCAB = 200;
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
   * @see LangTestDatabaseImpl#writeAudioFile(String, String, String, String, String, boolean, int)
   * @param exercise
   * @param e
   * @param reqid
   * @param file
   * @param validity
   * @param questionID
   * @param url
   * @return
   */
  public AudioAnswer getAutoCRTAnswer(String exercise, Exercise e, int reqid, File file, AudioAnswer.Validity validity, int questionID, String url) {
    List<String> exportedAnswers = getExportedAnswers(exercise, questionID);
    //for (Exercise.QAPair pair : e.getForeignLanguageQuestions()) exportedAnswers.add(pair.getQuestion());
    logger.info("got answers " + new HashSet<String>(exportedAnswers));

    List<String> background = getBackgroundText(e);
    List<String> vocab = getVocab(background);

    PretestScore asrScoreForAudio = db.getASRScoreForAudio(file, exportedAnswers, background, vocab);

    String recoSentence = asrScoreForAudio.getRecoSentence();
    logger.info("reco sentence was '" + recoSentence + "'");

    String annotatedResponse = getAnnotatedResponse(exercise, questionID, recoSentence);

    double scoreForAnswer = (recoSentence.length() > 0) ? getScoreForExercise(e, questionID, recoSentence) :0.0d;
    return new AudioAnswer(url, validity, annotatedResponse, scoreForAnswer, reqid);
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

    int c = 0;
    for (String answer : getAllExportedAnswers()) {
      //  boolean allDigit = true;
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
      if (result.length() > 0) background.add(result);
      //if (c++ > MAX_EXPORTED_ANSWERS_BKG) break;
    }
    // background.addAll(db.getAllExportedAnswers());

    logger.info("background has " + background.size() + " lines");
    return background;
  }

  /**
   * Get the vocabulary to use when generating a language model. <br></br>
   * Very important to limit the vocabulary (less than 300 words) or else the small vocab dcodr will run out of
   * memory and segfault! <br></br>
   * Remember to add special tokens like silence, pause, and unk
   * @see #MAX_AUTO_CRT_VOCAB
   * @param background sentences
   * @return most frequent vocabulary words
   */
  private List<String> getVocab(List<String> background) {
    List<String> all = new ArrayList<String>();
    all.addAll(Arrays.asList("-pau-", "</s>", "<s>", "<unk>"));

    final Map<String,Integer> sc = new HashMap<String, Integer>();
    for (String l : background) {
      for (String t : l.split("\\s")) {
        String tt = t.replaceAll("\\p{P}","");
        if (tt.trim().length() > 0) {
          Integer c = sc.get(t);
          if (c == null) sc.put(t,1);
          else sc.put(t,c+1);
        }
      }
    }
    List<String> vocab = new ArrayList<String>();
    try {
/*      System.out.println("map : " +sc);

      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("map"), FileExerciseDAO.ENCODING));
      for (Map.Entry<String, Integer> kv : sc.entrySet()) writer.write(kv.getKey() +"\t"+kv.getValue()+"\n");
      writer.close();*/

      vocab = new ArrayList<String>(sc.keySet());
      Collections.sort(vocab, new Comparator<String>() {
        public int compare(String s, String s2) {
          Integer first = sc.get(s);
          Integer second = sc.get(s2);
          return first < second ? +1 : first > second ? -1 : 0;
        }
      });

 /*     writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("vocab"), FileExerciseDAO.ENCODING));
      for (String v : vocab) writer.write(v+"\n");
      writer.close();*/
    } catch (Exception e) {
      e.printStackTrace();
    }

    //System.out.println("map : " +sc);

    all.addAll(vocab.subList(0,Math.min(vocab.size(), MAX_AUTO_CRT_VOCAB)));
    //  System.out.println("vocab " + new HashSet<String>(all));
    return all;
  }

  private Set<String> getTokenSet(List<String> exportedAnswers) {
    Set<String> tokens = new HashSet<String>();
    for (String l : exportedAnswers) {
      for (String t : l.split("\\s")) {
        String tt = t.replaceAll("\\p{P}","");
        if (tt.trim().length() > 0) {
          tokens.add(tt.trim());
        }}}
    return tokens;
  }
  private Collection<String> getAllExportedAnswers() { return allAnswers; }

  /**
   * @see #getAutoCRTAnswer(String, mitll.langtest.shared.Exercise, int, java.io.File, mitll.langtest.shared.AudioAnswer.Validity, int, String)
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
    //  Export exporter = new Export(exerciseDAO,resultDAO,gradeDAO);
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
