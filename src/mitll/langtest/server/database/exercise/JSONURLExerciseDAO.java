package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Read from a domino url - either documents or exam
 * <p>
 * For exam, maybe we need a "latestExam" link?
 * <p>
 * Created by go22670 on 2/10/16.
 */
public class JSONURLExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = Logger.getLogger(JSONURLExerciseDAO.class);
  private final Collection<String> typeOrder;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public JSONURLExerciseDAO(
      ServerProperties serverProps,
      UserListManager userListManager,
      boolean addDefects) {
    super(serverProps, userListManager, addDefects);
    this.typeOrder = serverProps.getTypes();

    new DominoReader().readProjectInfo(serverProps);
  }

  @Override
  List<CommonExercise> readExercises() {
    try {
      List<CommonExercise> exercises = getExercisesFromArray(getJSON());
      populateSections(exercises);
      return exercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  /**
   * E.g read from http://domino-devel:9000/domino-ws/v1/projects/354/documents
   * @return
   */
  private String getJSON() {
    logger.info(serverProps.getLanguage() + " Reading from " + serverProps.getLessonPlan());
    this.now = System.currentTimeMillis();
    return new HTTPClient().readFromGET(serverProps.getLessonPlan());
  }

  private List<CommonExercise> getExercisesFromArray(String json) {
    JSONArray content = JSONArray.fromObject(json);
    List<CommonExercise> exercises = new ArrayList<>();
    for (int i = 0; i < content.size(); i++) {
      exercises.add(toExercise(content.getJSONObject(i)));
    }

    return exercises;
  }

  private static final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  private long now = System.currentTimeMillis();

  /**
   * Grab info from the json object -
   * expect nested objects "content" and "metadata"
   *
   * e.g.
   *
   *
   * id: 45678,
   * content: {
   *    trans: "absentminded ; distracted",
   *    meaning: "",
   *    orient: "",
   *    pass: "distraÃ­do",
   *    translit: "",
   *    q-lst: {
   *     0: {
   *      qNum: 0,
   *      stem: "El hombre estÃ¡ distraÃ­do.",
   *      trans: "The man is distracted."
   *      }
   *    }
   * },
   * metadata: {
   *    chapter: "28",
   *    unit: "3",
   *    npDID: "50264"
   * },
   * updateTime: "2016-04-14T15:05:12.009Z",
   * }
   * @param jsonObject
   * @return
   */
  private Exercise toExercise(JSONObject jsonObject) {
    JSONObject metadata = jsonObject.getJSONObject("metadata");
    JSONObject content  = jsonObject.getJSONObject("content");
    String updateTime   = jsonObject.getString("updateTime");
    String dominoID     = "" + jsonObject.getInt("id");
    String npDID = isLegacyExercise(metadata) ? metadata.getString("npDID") : dominoID;

    String fl = noMarkup(content.getString("pass"));
    String english = noMarkup(content.getString("trans"));
    String meaning = noMarkup(content.getString("meaning"));
    String transliteration = noMarkup(content.getString("translit"));

    Exercise exercise = new Exercise(
        npDID,
        english,
        fl,
        meaning,
        transliteration,
        dominoID);

    addContextSentences(content, exercise);

    exercise.setUpdateTime(getUpdateTimestamp(updateTime, npDID));
    //if (!isLegacy) logger.info("NOT LEGACY " + exercise);

    addUnitAndChapterInfo(metadata, exercise);

    return exercise;
  }

  private boolean isLegacyExercise(JSONObject metadata) {
    return metadata.containsKey("npDID");
  }

  private long getUpdateTimestamp(String updateTime, String npDID) {
    long updateMillis = now;
    try {
      Date update = dateFmt.parse(updateTime);
      updateMillis = update.getTime();
    } catch (ParseException e) {
      logger.warn(e.getMessage() + " : can't parse date '" + updateTime + "' for " + npDID);
    }
    return updateMillis;
  }

  private void addUnitAndChapterInfo(JSONObject metadata, Exercise exercise) {
    for (String type : typeOrder) {
      try {
        String unitOrChapter = metadata.getString(type.toLowerCase());
        exercise.addUnitToValue(type, noMarkup(unitOrChapter));
      } catch (Exception e) {
        logger.error("couldn't find unit/chapter '" + type + "' in content - see typeOrder property");
      }
    }
  }

  private void addContextSentences(JSONObject content, Exercise exercise) {
    JSONObject contextSentences = content.getJSONObject("q-lst");
    for (Object key : contextSentences.keySet()) {
      JSONObject pair = contextSentences.getJSONObject(key.toString());
      String stem = pair.getString("stem");
      String trans = pair.getString("trans");
      exercise.addContext(stem, trans);
    }
  }

  private String noMarkup(String source) {
    return source.replaceAll("\\<.*?>", "");
  }
}
