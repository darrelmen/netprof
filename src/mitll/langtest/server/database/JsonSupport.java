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

import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/7/15.
 */
public class JsonSupport<T extends Shell>  {
  private static final Logger logger = Logger.getLogger(JsonSupport.class);

  private final SectionHelper<CommonExercise> sectionHelper;
  private final ResultDAO resultDAO;
  private final RefResultDAO refResultDAO;
  private final AudioDAO audioDAO;
  private final PhoneDAO phoneDAO;
  private final String configDir;
  private final String installPath;

  /**
   * @param sectionHelper
   * @param resultDAO
   * @param refResultDAO
   * @param audioDAO
   * @param phoneDAO
   * @param configDir
   * @param installPath
   * @see mitll.langtest.server.database.DatabaseImpl#setInstallPath(String, String, String)
   */
  public JsonSupport(SectionHelper<CommonExercise> sectionHelper, ResultDAO resultDAO, RefResultDAO refResultDAO, AudioDAO audioDAO,
                     PhoneDAO phoneDAO, String configDir, String installPath) {
    this.sectionHelper = sectionHelper;
    this.resultDAO = resultDAO;
    this.refResultDAO = refResultDAO;
    this.audioDAO = audioDAO;
    this.phoneDAO = phoneDAO;
    this.configDir = configDir;
    this.installPath = installPath;
  }

  /**
   * @param userid
   * @param typeToSection
   * @return
   * @paramx collator
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public JSONObject getJsonScoreHistory(long userid,
                                        Map<String, Collection<String>> typeToSection,
                                        ExerciseSorter sorter) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToSection);
    List<String> allIDs = new ArrayList<String>();

/*    Map<String, CollationKey> idToKey = new HashMap<String, CollationKey>();
    for (CommonExercise exercise : exercisesForState) {
      String id = exercise.getID();
      allIDs.add(id);
      CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
      idToKey.put(id, collationKey);
    }*/

    Map<String, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise exercise : exercisesForState) {
      String id = exercise.getID();
      allIDs.add(id);
      idToEx.put(id, exercise);
    }

    //List<ExerciseCorrectAndScore> exerciseCorrectAndScores = resultDAO.getExerciseCorrectAndScores(userid, allIDs, idToKey);
    List<ExerciseCorrectAndScore> exerciseCorrectAndScores =
        resultDAO.getExerciseCorrectAndScoresByPhones(userid, allIDs, idToEx, sorter);

    return addJsonHistory(exerciseCorrectAndScores);
  }

  /**
   * @param typeToSection
   * @return
   * @see DatabaseImpl#getJsonRefResult(Map)
   */
  public JSONObject getJsonRefResults(Map<String, Collection<String>> typeToSection) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToSection);
    List<String> allIDs = new ArrayList<String>();

    Map<String, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise exercise : exercisesForState) {
      String id = exercise.getID();
      allIDs.add(id);
      idToEx.put(id, exercise);
    }
    return refResultDAO.getJSONScores(allIDs);
  }

  /**
   * A special, slimmed down history just for the Appen recording app.
   * @see mitll.langtest.server.ScoreServlet#getRecordHistory
   * @param userid
   * @param typeToSection
   * @param collator
   * @return
   */
/*  public JSONObject getJsonScoreHistoryRecorded(long userid,
                                                Map<String, Collection<String>> typeToSection,
                                                Collator collator
  ) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToSection);

    final Map<String, CollationKey> idToKey = new HashMap<String, CollationKey>();
    for (CommonExercise exercise : exercisesForState) {
      String id = exercise.getID();
      idToKey.put(id, collator.getCollationKey(exercise.getForeignLanguage()));
    }

    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForState);
    final Set<String> recordedForUser = audioDAO.getRecordedRegularForUser(userid);

    Collections.sort(copy, new Comparator<CommonExercise>() {
      @Override
      public int compare(CommonExercise o1, CommonExercise o2) {
        String id = o1.getID();
        String id1 = o2.getID();

        boolean firstRecorded = recordedForUser.contains(id);
        boolean secondRecorded = recordedForUser.contains(id1);
        if (!firstRecorded && secondRecorded) {
          return -1;
        } else if (firstRecorded && !secondRecorded) {
          return +1;
        } else {
          CollationKey fl = idToKey.get(id);
          CollationKey otherFL = idToKey.get(id1);
          return fl.compareTo(otherFL);
        }
      }
    });


    JSONArray scores = new JSONArray();
    for (CommonExercise ex : copy) {
      //logger.debug("for " + ex);
      JSONObject exAndScores = new JSONObject();
      exAndScores.put("ex", ex.getID());
      boolean wasRecorded = recordedForUser.contains(ex.getID());
      exAndScores.put("s", wasRecorded ? "100" : "0");

      JSONArray history = new JSONArray();
      if (wasRecorded) {
        history.add("Y");
      }
      exAndScores.put("h", history);

      scores.add(exAndScores);
    }

    JSONObject container = new JSONObject();
    container.put("scores", scores);
    container.put("lastCorrect", ""+recordedForUser.size());
    container.put("lastIncorrect", Integer.toString(0));
    return container;
  }*/

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
   * @see DatabaseImpl#getJsonPhoneReport(long, Map)
   */
  public JSONObject getJsonPhoneReport(long userid, Map<String, Collection<String>> typeToValues) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToValues);

    long then = System.currentTimeMillis();
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    long now = System.currentTimeMillis();

    if (now - then > 500) logger.warn("took " + (now - then) + " millis to get ex->audio map");

    List<String> ids = new ArrayList<String>();
    Map<String, String> idToRef = new HashMap<String, String>();
    for (CommonExercise exercise : exercisesForState) {
      List<AudioAttribute> audioAttributes = exToAudio.get(exercise.getID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(exercise, installPath, configDir, audioAttributes);
      }
      String id = exercise.getID();
      ids.add(id);
      idToRef.put(id, exercise.getRefAudio());
    }

    now = System.currentTimeMillis();

    if (now - then > 300) logger.warn("getJsonPhoneReport : took " + (now - then) + " millis to attach audio again!");

    return phoneDAO.getWorstPhonesJson(userid, ids, idToRef);
  }
}
