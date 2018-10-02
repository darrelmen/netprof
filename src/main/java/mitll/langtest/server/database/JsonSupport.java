/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database;

import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.project.Language;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/7/15.
 */
public class JsonSupport {
  private static final Logger logger = LogManager.getLogger(JsonSupport.class);

  private final ISection<CommonExercise> sectionHelper;
  private final IResultDAO resultDAO;
//  private final IAudioDAO audioDAO;
  private final IPhoneDAO phoneDAO;

  private Language language;
  private Project project;

  /**
   * @param sectionHelper
   * @param resultDAO
   * @param phoneDAO
   * @see IProjectManagement#configureProject
   */
  public JsonSupport(ISection<CommonExercise> sectionHelper,
                     IResultDAO resultDAO,
                     IPhoneDAO phoneDAO,
                     Project project) {
    this.sectionHelper = sectionHelper;
    this.resultDAO = resultDAO;

  //  this.audioDAO = audioDAO;
    this.phoneDAO = phoneDAO;
    this.language = project.getLanguageEnum();
    this.project = project;
  }

  /**
   * @param userid
   * @param typeToSection
   * @return
   * @paramx collator
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  JSONObject getJsonScoreHistory(int userid,
                                 Map<String, Collection<String>> typeToSection,
                                 ExerciseSorter sorter) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToSection);
    List<Integer> allIDs = new ArrayList<>();

/*    Map<String, CollationKey> idToKey = new HashMap<String, CollationKey>();
    for (CommonExercise exercise : exercisesForState) {
      String id = exercise.getOldID();
      allIDs.add(id);
      CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
      idToKey.put(id, collationKey);
    }*/

    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise exercise : exercisesForState) {
      int id = exercise.getID();
      allIDs.add(id);
      idToEx.put(id, exercise);
    }

    //List<ExerciseCorrectAndScore> exerciseCorrectAndScores = resultDAO.getExerciseCorrectAndScores(userid, allIDs, idToKey);
    Collection<ExerciseCorrectAndScore> exerciseCorrectAndScores =
        resultDAO.getExerciseCorrectAndScoresByPhones(userid, allIDs, idToEx, sorter, language);

    return addJsonHistory(exerciseCorrectAndScores);
  }

  /**
   * @param typeToSection
   * @return
   * @seex DatabaseImpl#getJsonRefResult(Map, int)
   */
/*
  JSONObject getJsonRefResults(Map<String, Collection<String>> typeToSection) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToSection);
    List<Integer> allIDs = new ArrayList<>();
    // Map<String, CommonExercise> idToEx = new HashMap<String, CommonExercise>();
    for (CommonExercise exercise : exercisesForState) {
      // String id = exercise.getOldID();
      allIDs.add(exercise.getID());
      // idToEx.put(id, exercise);
    }
    return refResultDAO.getJSONScores(allIDs);
  }
*/

  /**
   * scoreJson has the complete scoring json for the last item only.
   *
   * @param exerciseCorrectAndScores
   * @return
   * @see #getJsonScoreHistory
   */
  private JSONObject addJsonHistory(Collection<ExerciseCorrectAndScore> exerciseCorrectAndScores) {
    JSONArray scores = new JSONArray();
    int correct = 0, incorrect = 0;
    for (ExerciseCorrectAndScore ex : exerciseCorrectAndScores) {
      //logger.debug("for " + ex);
      List<CorrectAndScore> correctAndScoresLimited = ex.getCorrectAndScoresLimited();

      JSONArray history = new JSONArray();
      boolean lastCorrect = getHistoryAsJson(history, correctAndScoresLimited);
      boolean empty = correctAndScoresLimited.isEmpty();
      if (!empty) {
        if (lastCorrect) correct++;
        else incorrect++;
      }

      JSONObject exAndScores = new JSONObject();
      exAndScores.put("ex", ex.getId());
      exAndScores.put("s", Integer.toString(ex.getAvgScorePercent()));
      exAndScores.put("h", history);
      exAndScores.put("scores", getScoresAsJson(correctAndScoresLimited));
      exAndScores.put("scoreJson", empty ? "" : correctAndScoresLimited.get(correctAndScoresLimited.size() - 1).getScoreJson());
      scores.add(exAndScores);
    }

    JSONObject container = new JSONObject();
    container.put("scores", scores);
    container.put("lastCorrect", Integer.toString(correct));
    container.put("lastIncorrect", Integer.toString(incorrect));
    return container;
  }

  /**
   * @param history
   * @param correctAndScoresLimited
   * @return array of Y's and N's
   * @see #addJsonHistory
   */
  private boolean getHistoryAsJson(JSONArray history, Collection<CorrectAndScore> correctAndScoresLimited) {
    boolean lastCorrect = false;
    for (CorrectAndScore cs : correctAndScoresLimited) {
      history.add(cs.isCorrect() ? "Y" : "N");
      lastCorrect = cs.isCorrect();
    }
    return lastCorrect;
  }


  private JSONArray getScoresAsJson(Collection<CorrectAndScore> correctAndScoresLimited) {
    JSONArray history = new JSONArray();
    for (CorrectAndScore cs : correctAndScoresLimited) {
      history.add(round(cs.getScore()));
    }
    return history;
  }

  private static float round(float d) {
    return round(d, 3);
  }

  private static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }

  /**
   * Wasteful... - why do we do attach audio on every call?
   * And exToAudio is wasteful too...
   * <p>
   * For all the exercises in a chapter
   * <p>
   * Get latest results
   * Get phones for latest
   * <p>
   * //Score phones
   * Sort phone scores – asc
   * <p>
   * Map phone->example
   * <p>
   * Join phone->word
   * <p>
   * Sort word by score asc
   *
   * @return
   * @see mitll.langtest.server.ScoreServlet#getPhoneReport
   * @see DatabaseImpl#getJsonPhoneReport
   */
  JSONObject getJsonPhoneReport(int userid, Map<String, Collection<String>> typeToValues, String language) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToValues);

    logger.info("getJsonPhoneReport : for user "+userid + " and" +
        "\n\tsel " +
        typeToValues+
        "\n\tgot " + exercisesForState.size() + " exercises");
 //   long then = System.currentTimeMillis();
   // int projid = project.getID();
//    Map<Integer, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio(projid);
   // long now = System.currentTimeMillis();

   // if (now - then > 500) logger.warn("took " + (now - then) + " millis to get ex->audio map");

    List<Integer> ids = exercisesForState.stream().map(HasID::getID).collect(Collectors.toList());

/*
    List<Integer> ids = new ArrayList<>();
   // Map<Integer, String> exidToRefAudio = new HashMap<>();
    for (CommonExercise exercise : exercisesForState) {
*/
/*      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(exercise, *//*
*/
/*installPath, configDir, *//*
*/
/*audioAttributes, language);
      }*//*

      int id = exercise.getID();
      ids.add(id);
     // exidToRefAudio.put(id, exercise.getRefAudio());
    }

    now = System.currentTimeMillis();
    if (now - then > 100) logger.warn("getJsonPhoneReport : took " + (now - then) + " millis to attach audio again!");
*/

    return phoneDAO.getWorstPhonesJson(userid, ids, language, project);
  }
}