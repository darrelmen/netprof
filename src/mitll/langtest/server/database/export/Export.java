package mitll.langtest.server.database.export;

import mitll.langtest.server.amas.FileExerciseDAO;
import mitll.langtest.server.autocrt.AutoCRT;
import mitll.langtest.server.database.GradeDAO;
import mitll.langtest.server.database.ResultDAO;
import mitll.langtest.server.export.ExerciseExport;
import mitll.langtest.server.export.ResponseAndGrade;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.amas.QAPair;
import mitll.langtest.shared.grade.Grade;
import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

/**
 * Export graded results to give to classifier training.
 * <p>
 * See Jacob's mira3 classifier.
 * <p>
 * User: GO22670
 * Date: 11/29/12
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Export {
  private static final Logger logger = Logger.getLogger(Export.class);
  public static final int MIN_LENGTH = 2;

  private FileExerciseDAO exerciseDAO = null;
  private ResultDAO resultDAO = null;
  private GradeDAO gradeDAO = null;

  public Export(FileExerciseDAO exerciseDAO, ResultDAO resultDAO, GradeDAO gradeDAO) {
    this.exerciseDAO = exerciseDAO;
    this.resultDAO = resultDAO;
    this.gradeDAO = gradeDAO;
  }

  /**
   * @return
   * @see AutoCRT#getExportedGradedItems()
   */
  public ExportPair getExport() {
    //  boolean useFLQ = true;
    Collection<Grade> grades = gradeDAO.getGrades();
    int max = getMaxGrade(grades);

    Map<Integer, List<Grade>> idToGrade = getIdToGrade(grades);
    logger.debug("getExport : got " + idToGrade.size() + " grades, max was " + max);

    Map<String, List<Result>> exerciseToResult = populateMapOfExerciseIdToResults();
    logger.debug("getExport : got " + exerciseToResult.size() + " exercise with results");

    List<AmasExerciseImpl> exercises = getExercises();
    logger.debug("getExport : got " + exercises.size() + " exercises");

    List<ExerciseExport> allExports = new ArrayList<ExerciseExport>();
    List<ExerciseExport> predefExports = new ArrayList<ExerciseExport>();
    // int c = 0;
    for (AmasExerciseImpl e : exercises) {
      ExportPair exports = getExports(idToGrade, getResultsForExercise(exerciseToResult, e), e, true, max);

      allExports.addAll(exports.allExports);
      predefExports.addAll(exports.predefOnly);
    }
    logger.debug("getExport : produced " + allExports.size() + " exports");

    return new ExportPair(allExports, predefExports);
  }

  /**
   * @param exerciseToResult
   * @param e
   * @return
   * @see #getExport()
   */
  private List<Result> getResultsForExercise(Map<String, List<Result>> exerciseToResult, AmasExerciseImpl e) {
    List<Result> results1 = exerciseToResult.get(e.getID());
    Set<String> strings = exerciseToResult.keySet();
    if (results1 == null) {
      results1 = exerciseToResult.get(e.getAltID());
      if (results1 == null) {
        if (!e.getID().startsWith("AM") /*&& c++ < 40*/ && !exerciseToResult.isEmpty()) {
          logger.debug("couldn't find '" + e.getID() + "'/'" + e.getAltID() + "' in (" + strings.size() + ") " + strings);
        }
        results1 = Collections.emptyList();
      }
    } else {
//        if (c++ < 20) {
//         logger.debug("found match for " +e.getID() + " - student answers = " + results1.size());
//      }
    }
    return results1;
  }

  /**
   * Paul says : 6 is now a 5
   *
   * @param grades
   * @return
   * @see #getExport
   */
  private int getMaxGrade(Collection<Grade> grades) {
    int max = 5;
    // if (grades.isEmpty()) max = 5;
    for (Grade g : grades) {
      if (g.grade > max) {
        g.grade = 5;
      }
    }
    return max;
  }

  /**
   * Only unique results...?
   *
   * @return
   * @see #getExport
   */
  private Map<String, List<Result>> populateMapOfExerciseIdToResults() {
    Map<String, List<Result>> exerciseToResult = new HashMap<String, List<Result>>();
    Collection<Result> results = resultDAO.getResults();
    logger.debug("populateMapOfExerciseIdToResults : got " + results.size() + " results");

    for (Result r : results) {
      List<Result> res = exerciseToResult.get(r.getExerciseID());
      if (res == null) exerciseToResult.put(r.getExerciseID(), res = new ArrayList<Result>());
      res.add(r);
    }
    return exerciseToResult;
  }

  /**
   * @return
   * @see #getExport
   */
  private List<AmasExerciseImpl> getExercises() {
    return exerciseDAO.getRawExercises();
  }

  private int count;

  /**
   * Only add a response-grade pair for distinct answers.
   * <p>
   * Complicated.  To figure out spoken/written, flq/english we have to go back and join against the schedule.
   * Unless the result column is set.
   *
   * @param exercise
   * @param useFLQ
   * @param correctGrade
   * @return
   * @see #getExport
   */
  private ExportPair getExports(Map<Integer, List<Grade>> idToGrade,
                                List<Result> resultsForExercise,
                                AmasExerciseImpl exercise, boolean useFLQ,
                                int correctGrade) {
    //  boolean debug = false;
    Map<Integer, ExerciseExport> qidToExport = populateIdToExportMap(exercise);
    Map<Integer, ExerciseExport> preDefqidToExport = populateIdToExportMap(exercise);
    //  logger.debug("exercise " + exercise.getID() + "/" +exercise.getAltID() + " got qid->export " +qidToExport.size() + " items ");
    Set<String> predefinedAnswers = addPredefinedAnswers(exercise, useFLQ, qidToExport, preDefqidToExport, correctGrade);
/*    if (exercise.getID().contains("012")) {
      logger.debug("exercise " + exercise.getID() + "/" + exercise.getAltID() + " got qid->export " + qidToExport.size() + " items, got " + resultsForExercise.size() + " resultsForExercise, predef " + predefinedAnswers.size());
    }*/

    List<ExerciseExport> ret = new ArrayList<ExerciseExport>();
    Set<ExerciseExport> valid = new HashSet<ExerciseExport>();

    if (resultsForExercise.isEmpty() && predefinedAnswers.size() > 0) {
      Set<ExerciseExport> toExport = new HashSet<ExerciseExport>(qidToExport.values());
      ret.addAll(toExport);
    }

    // find results, after join with schedule, add join with the grade
    Set<String> distinctAnswers = new HashSet<String>(predefinedAnswers);
    for (Result r : resultsForExercise) {
      ExerciseExport exerciseExport = qidToExport.get(r.getQid());
      if (exerciseExport == null) {
        logger.warn("getExports : for " + r.getCompoundID() + " can't find r qid " + r.getQid() + " in keys " + qidToExport.keySet());
      } else {
        int rid = r.getUniqueID();
        List<Grade> gradesForResult = idToGrade.get(rid);
        if (gradesForResult == null) {
          if (r.getAudioType().equals("arabic_text")) {
            if (count++ < 100) logger.warn("for " + exercise.getID() + " no grades for result " + r);
          }
        } else {
          if (gradesForResult.size() > 1) {
            logger.warn("for " + exercise.getID() + " only expecting one grade for " + r + " but there were " + gradesForResult.size());
          }
          for (Grade g : gradesForResult) {
            if (g.grade > 0) {  // filter out bad items (valid grades are 1-5)
              String answer = r.getAnswer();
              if (!answer.endsWith(".wav")) {
                //logger.info("Skipping wav file " + answer);
                String s = removePunct(answer);

                if (!s.isEmpty()) {
                  boolean pureAscii = isPureAscii(s);
                  if (pureAscii) {
                    if (isInt(s)) { // ints are OK
                      if (!distinctAnswers.contains(s)) {
                        distinctAnswers.add(s);
                        addRG(correctGrade, ret, valid, exerciseExport, g, answer, rid);
                      }
                    } else {
                      // logger.warn("Discarding english " + s);
                    }
                  } else {
                    if (!distinctAnswers.contains(s)) {
                      distinctAnswers.add(s);
                      addRG(correctGrade, ret, valid, exerciseExport, g, answer, rid);
                    }
                  }
                } else {
                  logger.warn("getExports : skipping result " + r.getCompoundID() + " with empty answer.");
                }
              }
            }
            //else logger.warn("huh? found grade of " + g);
          }
        }
      }
      // } else {
      //   if (debug) logger.debug("\tSkipping result " + r + " since not match to " + useFLQ + " and " + useSpoken);
      // }
    }
    if (ret.isEmpty()) {
      Set<ExerciseExport> toExport = new HashSet<ExerciseExport>(qidToExport.values());
      logger.warn("Adding " + toExport.size() + " predef answers for " + exercise.getID());
      ret.addAll(toExport);
    }

    List<ExerciseExport> copy = new ArrayList<>();
    for (ExerciseExport exp : preDefqidToExport.values()) copy.add(exp);

    return new ExportPair(ret, copy);
  }

  public static class ExportPair {
    private List<ExerciseExport> allExports;
    private List<ExerciseExport> predefOnly;

    /**
     * @param allExports
     * @param predefOnly
     * @see Export#getExport()
     * @see Export#getExports(Map, List, AmasExerciseImpl, boolean, int)
     */
    public ExportPair(List<ExerciseExport> allExports, List<ExerciseExport> predefOnly) {
      this.allExports = allExports;
      this.predefOnly = predefOnly;
    }

    public List<ExerciseExport> getAllExports() {
      return allExports;
    }

    public List<ExerciseExport> getPredefOnly() {
      return predefOnly;
    }
  }

  private boolean isInt(String s) {
    boolean isInt = false;
    try {
      Integer.parseInt(s);
      isInt = true;
    } catch (NumberFormatException e) {
    }
    return isInt;
  }

  /**
   * Optionally mark the response with the resultid...
   *
   * @param correctGrade
   * @param ret
   * @param valid
   * @param exerciseExport
   * @param g
   * @param answer
   * @param rid
   */
  private void addRG(int correctGrade, List<ExerciseExport> ret, Set<ExerciseExport> valid,
                     ExerciseExport exerciseExport, Grade g, String answer, int rid) {
    //ResponseAndGrade responseAndGrade =
    exerciseExport.addRG(answer, g.grade, correctGrade);
    //responseAndGrade.setResultID(rid);
    if (!valid.contains(exerciseExport)) {
      ret.add(exerciseExport);
      valid.add(exerciseExport);
    }
  }

  /**
   * This is pretty messed up...
   *
   * @param exercise
   * @return
   * @see #getExports
   */
  private Map<Integer, ExerciseExport> populateIdToExportMap(AmasExerciseImpl exercise) {
    Map<Integer, ExerciseExport> qidToExport = new HashMap<Integer, ExerciseExport>();
    int qid = 0;
    for (QAPair qaPair : exercise.getQuestions()) {
//      String answer = qaPair.getAnswer();
//      String key = collapseWhitespace(answer);
//      if (answer.isEmpty()) {
//        logger.warn("huh? answer for " + exercise.getID() + " is empty.");
//        for (String alt : qaPair.getAlternateAnswers()) {
//          if (!removePunct(alt).isEmpty()) {
//            ExerciseExport e1 = new ExerciseExport(exercise.getID() + "_" + ++qid, key);
//            qidToExport.put(qid, e1);
//            addAlternates(qaPair, e1);
//            break;
//          } else {
//            logger.info("alt answer for " + qaPair + " is empty");
//          }
//        }
//      } else {
      StringBuilder builder = new StringBuilder();
      for (String a : qaPair.getAlternateAnswers()) builder.append(a).append("||");
      String key = builder.toString();
      if (!key.isEmpty()) key = key.substring(0, key.length() - 2);
      ExerciseExport e1 = new ExerciseExport(exercise.getID() + "_" + ++qid, key);
      qidToExport.put(qid, e1);
      addAlternates(qaPair, e1);
//      }
    }
    return qidToExport;
  }

  private static String collapseWhitespace(String value) {
    // Replace all whitespace blocks with single spaces.
    return value.replaceAll("\\s+", " ");
  }

  private void addAlternates(QAPair q, ExerciseExport e1) {
    for (String alt : q.getAlternateAnswers()) {
      addAltKey(e1, alt);
    }
  }

  private void addAltKey(ExerciseExport e1, String alt) {
    alt = collapseWhitespace(alt);
    if (!alt.isEmpty() && !e1.key.contains(alt)) {
      e1.key.add(alt);
    }
  }

  /**
   * If the exercise already has predefined answers, add those
   *
   * @param exercise
   * @param useFLQ
   * @param qidToExport
   * @param correctGrade
   * @see #getExports
   */
  private Set<String> addPredefinedAnswers(AmasExerciseImpl exercise, boolean useFLQ,
                                           Map<Integer, ExerciseExport> qidToExport,
                                           Map<Integer, ExerciseExport> preDefqidToExport,
                                           int correctGrade) {
    int qid;
    List<QAPair> qaPairs = useFLQ ? exercise.getForeignLanguageQuestions() : exercise.getEnglishQuestions();
    qid = 1;
    //  int count = 0;
    Set<String> answers = new HashSet<String>();
    for (QAPair q : qaPairs) {
      ExerciseExport exerciseExport = qidToExport.get(qid);
      ExerciseExport preDef = preDefqidToExport.get(qid);

      if (exerciseExport == null) {
        logger.error("no qid " + qid + " in " + qidToExport.keySet() + " for " + exercise);
      } else {
        // questions are wrong
        String question = q.getQuestion();
        //  String answer1 = q.getAnswer();
        if (question.trim().isEmpty()) {
          logger.warn("huh? question is empty for " + q + " in " + exercise.getID());
        } else {
          if (question.length() < 3) {
            logger.warn("added " + new ResponseAndGrade(question, 1, correctGrade) + " for " + q);
          }
          exerciseExport.addRG(question, 1, correctGrade);
          preDef.addRG(question, 1, correctGrade);
          answers.add(question);
          //      count++;
        }

        // key answers are correct
  /*      if (answer1.trim().isEmpty()) {
          logger.warn("huh? answer is empty for " + q + " in " + exercise.getID());
        } else {
          if (answer1.length() < 3) {
            logger.warn("note, short answer (" + answer1 + ") added " + new ResponseAndGrade(answer1, correctGrade, correctGrade) + " for " + q);
          }
          exerciseExport.addRG(answer1, correctGrade, correctGrade);
          preDef.addRG(answer1, correctGrade, correctGrade);

          answers.add(answer1);
          //    count++;
        }*/

        for (String answer : q.getAlternateAnswers()) {
          String removed = removePunct(answer.trim());
          if (answer.trim().isEmpty() || removed.isEmpty()) {
            logger.warn("huh? alternate answer is empty??? for " + q + " in " + exercise.getID());
            logger.warn("exercise " + exercise);
          } else {
            if (answer.length() < MIN_LENGTH) {
              logger.warn("for short answer " + answer + " len " + answer.length() +
                  " added " + new ResponseAndGrade(answer, correctGrade, correctGrade) + " for " + q);
            }
            exerciseExport.addRG(answer, correctGrade, correctGrade);
            preDef.addRG(answer, correctGrade, correctGrade);

            answers.add(answer);
//            count++;
          }
        }
        //logger.debug("for " + exercise.getID() + " export is " + exerciseExport);
      }
      qid++;
    }
    if (answers.size() == 0) {
      logger.warn("for " + exercise.getID() + " added " + answers.size() + " predefined answers from " + qaPairs);
    } else {
//      logger.info("for " + exercise.getID() + " added " + count + " predefined answers from " + qaPairs);
    }
    return answers;
  }

  private static boolean isPureAscii(String v) {
    byte bytearray[] = v.getBytes();
    CharsetDecoder d = Charset.forName("US-ASCII").newDecoder();
    try {
      CharBuffer r = d.decode(ByteBuffer.wrap(bytearray));
      r.toString();
    } catch (CharacterCodingException e) {
      return false;
    }
    return true;
  }

  /**
   * Replace elipsis with space. Then remove all punct.
   *
   * @param t
   * @return
   */
  private String removePunct(String t) {
    return t.replaceAll("\\.\\.\\.", " ").replaceAll("\\p{P}", "").trim().replace("\u00A0", "");
  }

  /**
   * 03/24/15 Paul : Only take the latest grade
   *
   * @param grades
   * @return
   * @see #getExport
   */
  private Map<Integer, List<Grade>> getIdToGrade(Collection<Grade> grades) {
    Map<Integer, List<Grade>> idToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = idToGrade.get(g.resultID);
      if (gradesForResult == null) {
        idToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
        gradesForResult.add(g);
      } else {
        Grade grade = gradesForResult.get(0);
        if (g.id > grade.id || grade.grade < 1) {
//          logger.info("replace " +grade.id + " with " +g.id);
          gradesForResult.set(0, g);
        }
      }
    }
    return idToGrade;
  }
}
