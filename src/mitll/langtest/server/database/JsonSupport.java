package mitll.langtest.server.database;

import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

/**
 * Created by go22670 on 4/7/15.
 */
public class JsonSupport {
  private static final Logger logger = Logger.getLogger(JsonSupport.class);

  private final SectionHelper sectionHelper;
  private final ResultDAO resultDAO;
  private final RefResultDAO refResultDAO;
  private final AudioDAO audioDAO;
  private final PhoneDAO phoneDAO;
  private final String configDir;
  private final String installPath;

  /**
   *
   * @param sectionHelper
   * @param resultDAO
   * @param refResultDAO
   * @param audioDAO
   * @param phoneDAO
   * @param configDir
   * @param installPath
   */
  public JsonSupport(SectionHelper sectionHelper, ResultDAO resultDAO,RefResultDAO refResultDAO, AudioDAO audioDAO,
                     PhoneDAO phoneDAO,String configDir, String installPath) {
    this.sectionHelper = sectionHelper;
    this.resultDAO = resultDAO;
    this.refResultDAO = refResultDAO;
    this.audioDAO = audioDAO;
    this.phoneDAO = phoneDAO;
    this.configDir = configDir;
    this.installPath = installPath;
  }
  /**
   * @see mitll.langtest.server.ScoreServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   * @param userid
   * @param typeToSection
   * @paramx collator
   * @return
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

    Map<String, CommonExercise> idToEx = new HashMap<String, CommonExercise>();
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
     * @see DatabaseImpl#getJsonRefResult(Map) 
     * @param typeToSection
     * @return
     */
  public JSONObject getJsonRefResults(Map<String, Collection<String>> typeToSection) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToSection);
    List<String> allIDs = new ArrayList<String>();

    Map<String, CommonExercise> idToEx = new HashMap<String, CommonExercise>();
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
  public JSONObject getJsonScoreHistoryRecorded(long userid,
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
  }

  /**
   * scoreJson has the complete scoring json for the last item only.
   *
   * @see #getJsonScoreHistory
   * @param exerciseCorrectAndScores
   * @return
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
        if (lastCorrect) correct++; else incorrect++;
      }

      JSONObject exAndScores = new JSONObject();
      exAndScores.put("ex", ex.getId());
      exAndScores.put("s", Integer.toString(ex.getAvgScorePercent()));
      exAndScores.put("h", history);
      exAndScores.put("scores", getScoresAsJson(correctAndScoresLimited));
      exAndScores.put("scoreJson",empty?"":correctAndScoresLimited.get(correctAndScoresLimited.size()-1).getScoreJson());
      scores.add(exAndScores);
    }

    JSONObject container = new JSONObject();
    container.put("scores", scores);
    container.put("lastCorrect",   Integer.toString(correct));
    container.put("lastIncorrect", Integer.toString(incorrect));
    return container;
  }

  /**
   *
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


  private JSONArray getScoresAsJson( Collection<CorrectAndScore> correctAndScoresLimited) {
    JSONArray history = new JSONArray();

    for (CorrectAndScore cs : correctAndScoresLimited) {
      history.add(round(cs.getScore()));
    }
    return history;
  }

  private static float round(float d) { return round(d, 3);	}
  private static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }
  /**
   * For all the exercises in a chapter

   Get latest results
   Get phones for latest

   //Score phones
   Sort phone scores – asc

   Map phone->example

   Join phone->word

   Sort word by score asc
   * @return
   * @see mitll.langtest.server.ScoreServlet#getPhoneReport
   */
  public JSONObject getJsonPhoneReport(long userid, Map<String, Collection<String>> typeToValues) {
    Collection<CommonExercise> exercisesForState = sectionHelper.getExercisesForSelectionState(typeToValues);

    long then = System.currentTimeMillis();
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    long now = System.currentTimeMillis();

    if (now-then > 500) logger.warn("took " + (now-then) + " millis to get ex->audio map");

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

    return phoneDAO.getWorstPhonesJson(userid, ids, idToRef);
  }
}
