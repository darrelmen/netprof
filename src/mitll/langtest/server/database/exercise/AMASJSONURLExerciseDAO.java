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
public class AMASJSONURLExerciseDAO implements SimpleExerciseDAO<AmasExerciseImpl> {
  private static final Logger logger = Logger.getLogger(AMASJSONURLExerciseDAO.class);

  public static final String ENGLISH = "english";
  public static final String ATT_LST = "att-lst";

  private final Map<String, AmasExerciseImpl> idToExercise = new HashMap<>();
  protected final SectionHelper<AmasExerciseImpl> sectionHelper = new SectionHelper<>();
  protected final String language;
  protected final ServerProperties serverProps;

  private List<AmasExerciseImpl> exercises = null;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public AMASJSONURLExerciseDAO(ServerProperties serverProps) {
    this.serverProps = serverProps;
    this.language = serverProps.getLanguage();
    sectionHelper.setPredefinedTypeOrder(Arrays.asList(ILRMapping.TEST_TYPE, ILRMapping.ILR_LEVEL));
    this.exercises = readExercises();
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
    logger.info(serverProps.getLanguage() + " Reading from " + serverProps.getLessonPlan());
    return new HTTPClient().readFromGET(serverProps.getLessonPlan());
  }

  public List<AmasExerciseImpl> getExercisesFromArray(String json) {
    JSONArray content = JSONArray.fromObject(json);
    List<AmasExerciseImpl> exercises = new ArrayList<>();
    for (int i = 0; i < content.size(); i++) {
      JSONObject jsonObject = content.getJSONObject(i);
      exercises.add(toAMASExercise(jsonObject));
    }

    return exercises;
  }

  private AmasExerciseImpl toAMASExercise(JSONObject jsonObject) {
    JSONObject metadata = jsonObject.getJSONObject("metadata");
    JSONObject content = jsonObject.getJSONObject("content");
    JSONObject qlist   = content.getJSONObject("q-lst");
    JSONObject attlist = content.has(ATT_LST) ? content.getJSONObject(ATT_LST) : new JSONObject();

    // logger.info("got " + attlist);
    String hubDID = metadata.getString("hubDID");
    String srcAudio = null;
    try {
      srcAudio = attlist.has("src-att") ? attlist.getString("src-att") : "";
    } catch (Exception e) {
      logger.error("reading hub :" + hubDID + " and " + jsonObject +
          " Got " + e, e);
    }

    String ilr = metadata.getString("ilr");
    if (ilr.endsWith(".0")) ilr = ilr.substring(0, ilr.length() - 2);
    else if (ilr.endsWith(".5")) ilr = ilr.substring(0, ilr.length() - 2) + "+";

    boolean lc = metadata.getString("Skill").equals("LC");

    if (lc && (srcAudio == null || srcAudio.isEmpty()))
      logger.error("no audio for " + hubDID + " but it's listening comp");

    AmasExerciseImpl exercise = new AmasExerciseImpl(
        hubDID,
        content.getString("pass"),
        content.getString("trans"),
        content.getString("orient"),
        content.getString("orient-trans"),
        lc,
        ilr,
        srcAudio);

    for (Object key : qlist.keySet()) {
      JSONObject jsonObject1 = qlist.getJSONObject((String) key);

      try {
        String flq = jsonObject1.getString("stem");
        String fla = jsonObject1.getString("key-idea");
        String question = removeMarkup(flq);
        if (question.isEmpty()) logger.error("huh? question empty for " + hubDID + " originally '" + flq +"'");
        exercise.addQuestion(true, question, getAnswerKey(fla));

        String enq = jsonObject1.getString("stem-trans");
        String ena = jsonObject1.getString("key-idea-trans");
        exercise.addQuestion(false, removeMarkup(enq), getAnswerKey(ena));

      } catch (Exception e) {
        logger.error("Got " + e, e);
        return exercise;
      }
    }

    exercise.addUnitToValue(ILRMapping.ILR_LEVEL, ilr);
    exercise.addUnitToValue(ILRMapping.TEST_TYPE, lc ? ILRMapping.LISTENING : ILRMapping.READING);

    return exercise;
  }

  private List<String> getAnswerKey(String fla) {
    String[] split = fla.split("\\[.\\]");
    List<String> cleaned = new ArrayList<>();
    for (String answer : split) {
      String s = removeMarkup(answer);
      if (!s.isEmpty()) {
        cleaned.add(s);
        if (s.length() < 3) {
          logger.info("Adding '" + s + "'");
        }
      }
    }
  //  logger.info("got " + cleaned.size() + " :\n" +cleaned);
    return cleaned;
  }

  private String removeMarkup(String answer) {
    return answer.replaceAll("<p>", "").replaceAll("</p>", "").replaceAll("<br />", "").trim().replaceAll("(^\\h*)|(\\h*$)","");
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
