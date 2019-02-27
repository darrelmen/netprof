/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.analysis.PhoneReportRequest;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class JsonSupport {
  private static final Logger logger = LogManager.getLogger(JsonSupport.class);
  private static final String SCORE_JSON = "scoreJson";
  //  public static final String NAME = "name";
//  public static final String ID = "id";
  private static final String LISTID = "listid";
  public static final String SCORES = "scores";

  private final ISection<CommonExercise> sectionHelper;
  private final IResultDAO resultDAO;

  private final IPhoneDAO phoneDAO;

  private final Language language;
  private final Project project;
  private final IUserListManager userListManager;

  /**
   * @param sectionHelper
   * @param resultDAO
   * @param phoneDAO
   * @param userListManager
   * @see ProjectManagement#configureProject
   */
  public JsonSupport(ISection<CommonExercise> sectionHelper,
                     IResultDAO resultDAO,
                     IPhoneDAO phoneDAO,
                     IUserListManager userListManager, Project project) {
    this.sectionHelper = sectionHelper;
    this.resultDAO = resultDAO;
    this.phoneDAO = phoneDAO;
    this.language = project.getLanguageEnum();
    this.project = project;
    this.userListManager = userListManager;
  }

  /**
   * @param userid
   * @param typeToSection
   * @param forContext
   * @param sortByLatestScore
   * @return
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  JsonObject getJsonScoreHistory(int userid,
                                 Map<String, Collection<String>> typeToSection,
                                 boolean forContext,
                                 ExerciseSorter sorter,
                                 boolean sortByLatestScore) {
    Collection<String> listid = typeToSection.get(LISTID);
    if (listid == null || listid.isEmpty()) {
      Collection<CommonExercise> exercisesForSelectionState = sectionHelper.getExercisesForSelectionState(typeToSection);
      if (forContext) {
        exercisesForSelectionState = getContextExercises(exercisesForSelectionState);
      }
      return getJsonForExercises(userid, exercisesForSelectionState);
    } else {
//      int id = getListID(listid);
      // typeToSection.remove(LISTID);
      return getJsonForExercises(userid,
          userListManager.getCommonExercisesOnList(project.getID(), getListID(listid)));
    }
  }

  @NotNull
  private List<CommonExercise> getContextExercises(Collection<CommonExercise> exercisesForSelectionState) {
    List<CommonExercise> context = new ArrayList<>();
    exercisesForSelectionState.forEach(ex -> {
      ex.getDirectlyRelated().forEach(cex -> context.add(cex.asCommon()));
    });
    return context;
  }

  private int getListID(Collection<String> listid) {
    int id = 0;
    try {
      String next = listid.iterator().next();
      id = Integer.parseInt(next);
    } catch (NumberFormatException e) {
      logger.warn("huh? couldn't parse ");
    }
    return id;
  }

  private JsonObject getJsonForExercises(int userid, Collection<CommonExercise> exercisesForState) {
    List<Integer> allIDs = new ArrayList<>();

    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise exercise : exercisesForState) {
      int id = exercise.getID();
      allIDs.add(id);
      idToEx.put(id, exercise);
    }

    List<ExerciseCorrectAndScore> exerciseCorrectAndScores =
        resultDAO.getExerciseCorrectAndScoresByPhones(userid, allIDs, idToEx, language);

//    if (true) {
//     // exerciseCorrectAndScores.sort(Comparator.comparingDouble(ExerciseCorrectAndScore::getLatestScore));
//      exerciseCorrectAndScores.forEach(exerciseCorrectAndScore -> logger.info("getJsonForExercises : " + exerciseCorrectAndScore.getId() + " = " + exerciseCorrectAndScore.getLatestScore()));
//    }

    return addJsonHistory(exerciseCorrectAndScores);
  }

  /**
   * scoreJson has the complete scoring json for the last item only.
   *
   * @param exerciseCorrectAndScores
   * @return
   * @see #getJsonScoreHistory
   */
  private JsonObject addJsonHistory(Collection<ExerciseCorrectAndScore> exerciseCorrectAndScores) {
    JsonArray scores = new JsonArray();
    int correct = 0, incorrect = 0;
    for (ExerciseCorrectAndScore ex : exerciseCorrectAndScores) {
      //logger.debug("for " + ex);
      List<CorrectAndScore> correctAndScoresLimited = ex.getCorrectAndScoresLimited();

      JsonArray history = new JsonArray();
      boolean lastCorrect = getHistoryAsJson(history, correctAndScoresLimited);
      boolean empty = correctAndScoresLimited.isEmpty();
      if (!empty) {
        if (lastCorrect) correct++;
        else incorrect++;
      }

      JsonObject exAndScores = new JsonObject();
      exAndScores.addProperty("ex", ex.getId());

      {
        String value = Integer.toString(ex.getLatestScorePercent());
        exAndScores.addProperty("s", value);   // latest score!
      }

      exAndScores.add("h", history);
      exAndScores.add(SCORES, getScoresAsJson(correctAndScoresLimited));
      {
        String value = empty ? "" : correctAndScoresLimited.get(correctAndScoresLimited.size() - 1).getScoreJson();
        JsonParser parser = new JsonParser();
        exAndScores.add(SCORE_JSON, parser.parse(value));
      }
      scores.add(exAndScores);
    }

    JsonObject container = new JsonObject();
    container.add(SCORES, scores);
    container.addProperty("lastCorrect", Integer.toString(correct));
    container.addProperty("lastIncorrect", Integer.toString(incorrect));
    return container;
  }

  /**
   * @param history
   * @param correctAndScoresLimited
   * @return array of Y's and N's
   * @see #addJsonHistory
   */
  private boolean getHistoryAsJson(JsonArray history, Collection<CorrectAndScore> correctAndScoresLimited) {
    boolean lastCorrect = false;
    for (CorrectAndScore cs : correctAndScoresLimited) {
      history.add(cs.isCorrect() ? "Y" : "N");
      lastCorrect = cs.isCorrect();
    }
    return lastCorrect;
  }


  private JsonArray getScoresAsJson(Collection<CorrectAndScore> correctAndScoresLimited) {
    JsonArray history = new JsonArray();
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
  JsonObject getJsonPhoneReport(PhoneReportRequest request) {
    Map<String, Collection<String>> typeToValues = request.getTypeToValues();
    Collection<String> listid = typeToValues.get(LISTID);

    Collection<CommonExercise> exercisesForState;
    if (listid == null || listid.isEmpty()) {
      // remove the session...
      typeToValues.remove(ScoreServlet.HeaderValue.SESSION.name().toLowerCase());
      typeToValues.remove(ScoreServlet.HeaderValue.SENTENCES.name().toLowerCase());

      exercisesForState = sectionHelper.getExercisesForSelectionState(typeToValues);

      if (request.isSentencesOnly()) {
        List<CommonExercise> sentences = new ArrayList<>(exercisesForState.size());
        exercisesForState.forEach(ex -> ex.getDirectlyRelated().forEach(sentence -> sentences.add(sentence.asCommon())));

        logger.info("getJsonPhoneReport from " + exercisesForState.size() + " to " + sentences.size() + " sentences");
        exercisesForState = sentences;
      } else {
        logger.info("getJsonPhoneReport not converting to just sentences");
      }
      logger.info("getJsonPhoneReport : for user " + request.getUserid() + " and" +
          "\n\tsel " +
          typeToValues +
          "\n\tgot " + exercisesForState.size() + " exercises");
    } else {
      exercisesForState = userListManager.getCommonExercisesOnList(project.getID(), getListID(listid));
    }
    List<Integer> ids = exercisesForState.stream().map(HasID::getID).collect(Collectors.toList());
    return phoneDAO.getWorstPhonesJson(ids, project, request);
  }
}