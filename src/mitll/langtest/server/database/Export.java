package mitll.langtest.server.database;

import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.grade.Grade;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Export graded results to give to classifier training.
 *
 * See Jacob's mira3 classifier.
 *
 * User: GO22670
 * Date: 11/29/12
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Export {
  private static final Logger logger = Logger.getLogger(Export.class);

  private ExerciseDAO exerciseDAO = null;
  private ResultDAO resultDAO = null;
  private GradeDAO gradeDAO = null;

  public Export(ExerciseDAO exerciseDAO, ResultDAO resultDAO, GradeDAO gradeDAO) {
    this.exerciseDAO = exerciseDAO;
    this.resultDAO = resultDAO;
    this.gradeDAO = gradeDAO;
  }

  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getClassifier
   * @param useFLQ
   * @param useSpoken
   * @return
   */
  public List<ExerciseExport> getExport(boolean useFLQ, boolean useSpoken) {
    Map<Integer, List<Grade>> idToGrade = getIdToGrade(gradeDAO.getGrades());
    logger.debug("getExport : got " +idToGrade.size() + " grades");

    Map<String, List<Result>> exerciseToResult = populateMapOfExerciseIdToResults();
    logger.debug("getExport : got " +exerciseToResult.size() + " exercise with results");

    List<CommonExercise> exercises = getExercises();
    logger.debug("getExport : got " +exercises.size() + " exercises");

    List<ExerciseExport> names = new ArrayList<ExerciseExport>();
    for (CommonExercise e : exercises) {
      List<Result> results1 = exerciseToResult.get(e.getID());
      if (results1 != null) {
        List<ExerciseExport> resultsForExercise = getExports(idToGrade, results1, e, useFLQ, useSpoken);
        names.addAll(resultsForExercise);
      }
    }
    logger.debug("getExport : produced " +names.size() + " exports");

    return names;
  }

  private Map<String, List<Result>> populateMapOfExerciseIdToResults() {
    Map<String,List<Result>> exerciseToResult = new HashMap<String, List<Result>>();
    List<Result> results = resultDAO.getResults();
    logger.debug("populateMapOfExerciseIdToResults : got " +results.size() + " results");

    for (Result r : results) {
      List<Result> res = exerciseToResult.get(r.getId());
      if (res == null) exerciseToResult.put(r.getId(), res = new ArrayList<Result>());
      res.add(r);
    }
    return exerciseToResult;
  }

  public static class ExerciseExport {
    public final String id;
    public List<String> key = new ArrayList<String>();
    public final List<ResponseAndGrade> rgs = new ArrayList<ResponseAndGrade>();
    public ExerciseExport(String id, String key) {
      this.id  = id;
      this.key = Arrays.asList(key.split("\\|\\|"));
    }
    public void addRG(String response, int grade) { rgs.add(new ResponseAndGrade(response,grade)); }

    public String toString() {
      return "id " + id + " " + key.size() + " keys : '" + "" +/*new HashSet<String>(key) +   */
          "' and " + rgs.size() + " responses ";// + new HashSet<ResponseAndGrade>(rgs);
    }
  }

  public static class ResponseAndGrade {
    public final String response;
    public final float grade;
    public ResponseAndGrade(String response, int grade) {
      this.response = response;
      this.grade = ((float) (grade-1))/4f;  // jacob's stuff wants a 0->1 scale
   //   logger.debug("mapping " + grade + " to " + this.grade);
    }
    @Override
    public String toString() { return "grd " + grade + " for '" + "" /*response*/ +"'"; }
  }

  /**
   * @see #getExport(boolean, boolean)
   * @return
   */
  private List<CommonExercise> getExercises() { return exerciseDAO.getRawExercises();  }

  /**
   * Complicated.  To figure out spoken/written, flq/english we have to go back and join against the schedule.
   * Unless the result column is set.
   * @param exercise
   * @param useFLQ
   * @param useSpoken
   * @return
   * @see #getExport(boolean, boolean)
   */
  private List<ExerciseExport> getExports(Map<Integer, List<Grade>> idToGrade,
                                          List<Result> resultsForExercise,
                                          CommonExercise exercise, boolean useFLQ, boolean useSpoken) {
  //  boolean debug = false;
    Map<Integer, ExerciseExport> qidToExport = populateIdToExportMap(exercise.toExercise());
  //  logger.debug("got qid->export " +qidToExport.size() + " items");
    addPredefinedAnswers(exercise.toExercise(), useFLQ, qidToExport);
   // logger.debug("got " +resultsForExercise.size() + " resultsForExercise ");

    List<ExerciseExport> ret = new ArrayList<ExerciseExport>();
    Set<ExerciseExport> valid = new HashSet<ExerciseExport>();

    // find results, after join with schedule, add join with the grade
    for (Result r : resultsForExercise) {
      //if (r.flq == useFLQ && r.spoken == useSpoken) {
        ExerciseExport exerciseExport = qidToExport.get(r.getQid());
        if (exerciseExport == null) {
          logger.warn("getExports : for " + r.getID() +" can't find r qid " + r.getQid() + " in keys " + qidToExport.keySet());
        }
        else {
          List<Grade> gradesForResult = idToGrade.get(r.getUniqueID());
          if (gradesForResult == null) {
            //System.err.println("no grades for result " + r);
          } else {
            for (Grade g : gradesForResult) {
              if (g.grade > 0) {  // filter out bad items (valid grades are 1-5)
                if (r.getAnswer().length() > 0) {
                  exerciseExport.addRG(r.getAnswer(), g.grade);
                  if (!valid.contains(exerciseExport)) {
                    ret.add(exerciseExport);
                    valid.add(exerciseExport);
                  }
                }
                else {
                  logger.warn("getExports : skipping result " + r.getID() + " with empty answer.");
                }
              }
            }
          }
        }
     // } else {
     //   if (debug) logger.debug("\tSkipping result " + r + " since not match to " + useFLQ + " and " + useSpoken);
     // }
    }
    return ret;
  }

  private Map<Integer, ExerciseExport> populateIdToExportMap(Exercise exercise) {
    Map<Integer, ExerciseExport> qidToExport = new HashMap<Integer, ExerciseExport>();
    int qid = 0;
    for (Exercise.QAPair q : exercise.getQuestions()) {
      ExerciseExport e1 = new ExerciseExport(exercise.getID() + "_" + ++qid, q.getAnswer());
      qidToExport.put(qid, e1);
    }
    return qidToExport;
  }

/*
  private Collection<ExerciseExport> getExports(Exercise exercise) {
    List<ExerciseExport> qidToExport = new ArrayList<ExerciseExport>();
    //int qid = 0;
  //  for (Exercise.QAPair q : exercise.getQuestions()) {
      qidToExport.add(new ExerciseExport(exercise.getID(), q.getAnswer()));
      //qidToExport.put(qid, e1);
  //  }
    return qidToExport;
  }
*/


  /**
   * If the exercise already has predefined answers, add those
   * @param exercise
   * @param useFLQ
   * @param qidToExport
   */
  private void addPredefinedAnswers(Exercise exercise, boolean useFLQ, Map<Integer, ExerciseExport> qidToExport) {
    int qid;
    List<Exercise.QAPair> qaPairs = useFLQ ? exercise.getForeignLanguageQuestions() : exercise.getEnglishQuestions();
    qid = 1;
    //int count = 0;
    for (Exercise.QAPair q : qaPairs) {
      ExerciseExport exerciseExport = qidToExport.get(qid);

      if (exerciseExport == null) {
        logger.error("no qid " + qid + " in " + qidToExport.keySet() + " for " + exercise);
      } else {
        for (String answer : q.getAlternateAnswers()) {
          if (answer.length() == 0) {
            logger.warn("huh? alternate answer is empty??? for " + q + " in " + exercise.getID());
          } else {
            exerciseExport.addRG(answer, 5);
            //count++;
          }
        }
        //logger.debug("for " + exercise.getID() + " export is " + exerciseExport);
      }
      qid++;
    }
    //logger.debug("for " + exercise.getID() + " added " + count + " predefined answers");
  }

  private Map<Integer, List<Grade>> getIdToGrade(Collection<Grade> grades) {
    Map<Integer,List<Grade>> idToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = idToGrade.get(g.resultID);
      if (gradesForResult == null) {
        idToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }
      gradesForResult.add(g);
    }
    return idToGrade;
  }
}
