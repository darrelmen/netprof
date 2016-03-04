package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Created by go22670 on 2/10/16.
 */
public class JSONURLExerciseDAO extends JSONExerciseDAO {
  private static final Logger logger = Logger.getLogger(JSONURLExerciseDAO.class);

//  public static final String ENGLISH = "english";
//
//  private final Map<String, CommonExercise> idToExercise = new HashMap<>();
//  protected final SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();
//  protected final String language;
//  protected final ServerProperties serverProps;

//  private List<CommonExercise> exercises = null;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public JSONURLExerciseDAO(
                            ServerProperties serverProps,
                            UserListManager userListManager,
                            boolean addDefects) {
    super("", serverProps, userListManager, addDefects);
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

  private String getJSON() throws IOException {
    logger.info(serverProps.getLanguage() + " Reading from " + serverProps.getLessonPlan());
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

  private Exercise toExercise(JSONObject jsonObject) {
    JSONObject metadata = jsonObject.getJSONObject("metadata");
    JSONObject content  = jsonObject.getJSONObject("content");

    // logger.info("got " + attlist);
    String npDID = metadata.getString("npDID");
    // logger.info("got " + attlist);

    String fl = content.getString("pass");
    String english = content.getString("trans");
    String meaning = "";
    String transliteration = "";

    String context = "";
    String contextTranslation = "";
    Exercise exercise = new Exercise(
        npDID,
        english,
        meaning,
        fl,
         transliteration,
        context,
        contextTranslation);

//    exercise.addUnitToValue(ILRMapping.ILR_LEVEL, ilr);
//    exercise.addUnitToValue(ILRMapping.TEST_TYPE, lc ? ILRMapping.LISTENING : ILRMapping.READING);
    return exercise;
  }
}
