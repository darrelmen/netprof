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
 *
 * For exam, maybe we need a "latestExam" link?
 *
 * Created by go22670 on 2/10/16.
 */
public class JSONURLExerciseDAO extends JSONExerciseDAO {
  private static final Logger logger = Logger.getLogger(JSONURLExerciseDAO.class);
  private static final String DOCUMENTS = "documents";
  private final Collection<String> typeOrder;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public JSONURLExerciseDAO(
      ServerProperties serverProps,
      UserListManager userListManager,
      boolean addDefects) {
    super("", serverProps, userListManager, addDefects);
    this.typeOrder = serverProps.getTypes();

    readProjectInfo(serverProps);
  }

  /**
   * TODO :  Consider reading font from project info
   *
   * name: "ENGLISH",
   defaultWordSpacing: true,
   direction: "LTR",
   systemFontNames: "Times New Roman",
   fontFaceURL: null,
   bitmapped: false,
   langCode: "eng",
   lineHeight: 125,
   fontSize: 14,
   digraph: "EN",
   script: "auto",
   fontWeight: "Normal",
   trigraph: "ENG"

   * @param serverProps
   */
  private void readProjectInfo(ServerProperties serverProps) {
    String baseURL = serverProps.getLessonPlan();

    if (baseURL.endsWith(DOCUMENTS)) {
      baseURL = baseURL.substring(0, baseURL.length() - DOCUMENTS.length());
    }
//    else if (baseURL.endsWith("exam"))
    String projectInfo = new HTTPClient().readFromGET(baseURL);
    JSONObject jsonObject = JSONObject.fromObject(projectInfo);
    JSONObject language = jsonObject.getJSONObject("language");
    String upperName = language.getString("name");

    // TODO : consider setting language here.
    String npLang = upperName.substring(0,1) + upperName.substring(1).toLowerCase();
    //  logger.info("got language " + npLang);
    boolean isLTR = language.get("direction").equals("LTR");
    serverProps.setRTL(!isLTR);

    String systemFontNames = language.getString("systemFontNames");
    String fontFaceURL = language.getString("fontFaceURL");
    serverProps.setFontNames(systemFontNames);
    serverProps.setFontFaceURL(fontFaceURL);
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

  private Exercise toExercise(JSONObject jsonObject) {
    JSONObject metadata = jsonObject.getJSONObject("metadata");
    JSONObject content = jsonObject.getJSONObject("content");
    String updateTime = jsonObject.getString("updateTime");
    String dominoID = ""+jsonObject.getInt("id");
    boolean isLegacy = metadata.containsKey("npDID");
    String npDID = isLegacy ? metadata.getString("npDID") : dominoID;

    long updateMillis = now;
    try {
      Date update = dateFmt.parse(updateTime);
      updateMillis = update.getTime();
    } catch (ParseException e) {
      logger.warn(e.getMessage() + " : can't parse date '" + updateTime + "' for " + npDID);
    }

    String fl = noMarkup(content.getString("pass"));
    String english = noMarkup(content.getString("trans"));
    String meaning = noMarkup(content.getString("meaning"));
    String transliteration = noMarkup(content.getString("translit"));

    String context = noMarkup(content.getString("context"));
    String contextTranslation = noMarkup(content.getString("context_trans"));

    Exercise exercise = new Exercise(
        npDID,
        english,
        fl,
        meaning,
        transliteration,
        context,
        contextTranslation, dominoID);
    exercise.setUpdateTime(updateMillis);
    if (!isLegacy) logger.info("NOT LEGACY " + exercise);

    for (String type : typeOrder) {
      try {
        exercise.addUnitToValue(type, noMarkup(content.getString(type.toLowerCase())));
      } catch (Exception e) {
        logger.error("couldn't find unit/chapter '" + type + "' in content - see typeOrder property");
      }
    }

    return exercise;
  }

  private String noMarkup(String source) { return source.replaceAll("\\<.*?>","");}
}
