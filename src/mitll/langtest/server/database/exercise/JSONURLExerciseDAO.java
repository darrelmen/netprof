package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.amas.ILRMapping;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Created by go22670 on 2/10/16.
 */
public class JSONURLExerciseDAO implements SimpleExerciseDAO<AmasExerciseImpl> {
  private static final Logger logger = Logger.getLogger(JSONURLExerciseDAO.class);

  public static final String ENGLISH = "english";

  private final Map<String, AmasExerciseImpl> idToExercise = new HashMap<>();
  protected final SectionHelper<AmasExerciseImpl> sectionHelper = new SectionHelper<>();
  protected final String language;
  protected final ServerProperties serverProps;

  private List<AmasExerciseImpl> exercises = null;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public JSONURLExerciseDAO(
      ServerProperties serverProps) {
    this.serverProps = serverProps;
    this.language = serverProps.getLanguage();
    sectionHelper.setPredefinedTypeOrder(Arrays.asList(ILRMapping.TEST_TYPE,ILRMapping.ILR_LEVEL));
   //sectionHelper.report();
    this.exercises = readExercises();
   // sectionHelper.report();
    populateIDToExercise(exercises);
  }

  private void populateIDToExercise(Collection<AmasExerciseImpl> exercises) {
    for (AmasExerciseImpl e : exercises) idToExercise.put(e.getID(), e);
  }

  List<AmasExerciseImpl> readExercises() {
    try {
      String json = getJSON();

      List<AmasExerciseImpl> exercises = getExercisesFromArray(json);

      for (AmasExerciseImpl ex : exercises) {
        Collection<SectionHelper.Pair> pairs = new ArrayList<>();
//        logger.info("For " + ex.getID() + " " + ex.getUnitToValue());
        for (Map.Entry<String, String> pair : ex.getUnitToValue().entrySet()) {
          pairs.add(sectionHelper.addExerciseToLesson(ex, pair.getKey(), pair.getValue()));
        }
        sectionHelper.addAssociations(pairs);
      }

   //   logger.info("read " + exercises.size());
     // sectionHelper.report();
      return exercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  private String getJSON() throws IOException {
    return new HTTPClient().readFromGET(serverProps.getLessonPlan());
  }

  public List<AmasExerciseImpl> getExercisesFromArray(String json) {
    JSONArray content = JSONArray.fromObject(json);
    List<AmasExerciseImpl> exercises = new ArrayList<>();
    for (int i = 0; i < content.size(); i++) {
      JSONObject jsonObject = content.getJSONObject(i);
      exercises.add(toAMASExercise(jsonObject));
    }

//    for (AmasExerciseImpl ex : exercises.subList(0, 10)) {
//      logger.info("got " + ex);
//    }

    return exercises;
  }

  private AmasExerciseImpl toAMASExercise(JSONObject jsonObject/*, Collection<String> types*/) {
    JSONObject metadata = jsonObject.getJSONObject("metadata");
    JSONObject content = jsonObject.getJSONObject("content");
    JSONObject qlist = content.getJSONObject("q-lst");
    JSONObject attlist = jsonObject.has("att-list") ? jsonObject.getJSONObject("att-lst") : new JSONObject();

    String hubDID = metadata.getString("hubDID");
    String srcAudio = null;
    try {
      srcAudio = attlist.has("src-att") ? attlist.getString("src-att") : "";
    } catch (Exception e) {
      logger.error("reading hub :" + hubDID + " and " + jsonObject +
          " Got " + e, e);
    }

    String ilr = metadata.getString("ilr");
    if (ilr.endsWith(".0")) ilr = ilr.substring(0,ilr.length()-2);
    else if (ilr.endsWith(".5")) ilr = ilr.substring(0,ilr.length()-2) + "+";

    boolean lc = metadata.getString("Skill").equals("LC");
    AmasExerciseImpl exercise = new AmasExerciseImpl(
        hubDID,
        content.getString("pass"),
        content.getString("trans"),
        content.getString("orient"),
        content.getString("orient-trans"),
        lc,
        ilr,
        srcAudio);

    logger.error(qlist.keySet());

    for (Object key : qlist.keySet()) {
      JSONObject jsonObject1 = qlist.getJSONObject((String) key);
      String flq = jsonObject1.getString("stem");
      String enq = jsonObject1.getString("stem-trans");
      String fla = jsonObject1.getString("key-idea");
      String ena = jsonObject1.getString("key-idea-trans");
      try {
        exercise.addQuestion(true,  flq, fla);
        exercise.addQuestion(false, enq, ena);
      } catch (Exception e) {
        logger.error("Got " + e,e);
        return exercise;
      }
    }

    logger.info("After " +exercise.getQuestions());

    exercise.addUnitToValue(ILRMapping.ILR_LEVEL, ilr);
    exercise.addUnitToValue(ILRMapping.TEST_TYPE, lc ? ILRMapping.LISTENING : ILRMapping.READING);

    /*try {
      for (String type : types) {
        if (jsonObject.has(type)) {
          String value = jsonObject.getString(type);
          if (value == null) logger.error("toExercise : missing " + type + " on " + exercise.getID());
          else exercise.addUnitToValue(type, value);
        } else {
          exercise.addUnitToValue(type, "");
        }
      }
    } catch (Exception e) {
      logger.warn("toExercise : got " + e + " for " + exercise.getID());
    }*/
    return exercise;
  }


  @Override
  public List<AmasExerciseImpl> getRawExercises() {
    return exercises;
  }

  @Override
  public AmasExerciseImpl getExercise(String id) {
    if (idToExercise.isEmpty()) logger.warn("huh? couldn't find any exercises..?");
    if (!idToExercise.containsKey(id)) {
      logger.warn("couldn't find " + id + " in " + idToExercise.size() + " exercises...");
    }
    return idToExercise.get(id);
  }

  @Override
  public int getNumExercises() {
    return exercises.size();
  }

  @Override
  public SectionHelper<AmasExerciseImpl> getSectionHelper() {
    return sectionHelper;
  }
}
