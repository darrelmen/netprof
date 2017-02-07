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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.amas.ILRMapping;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Talk to domino-ws service to get content.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/16.
 */
public class AMASJSONURLExerciseDAO implements SimpleExerciseDAO<AmasExerciseImpl> {
  private static final Logger logger = LogManager.getLogger(AMASJSONURLExerciseDAO.class);

  public static final String ENGLISH = "english";
  private static final String ATT_LST = "att-lst";

  private final Map<String, AmasExerciseImpl> idToExercise = new HashMap<>();
  private final SectionHelper<AmasExerciseImpl> sectionHelper = new SectionHelper<>();
  private final ServerProperties serverProps;

  private List<AmasExerciseImpl> exercises = null;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public AMASJSONURLExerciseDAO(ServerProperties serverProps) {
    this.serverProps = serverProps;
    sectionHelper.setPredefinedTypeOrder(Arrays.asList(ILRMapping.TEST_TYPE, ILRMapping.ILR_LEVEL));
    readExercisesFromURL();

    new DominoReader().readProjectInfo(serverProps);
  }

  private void readExercisesFromURL() {
    populateIDToExercise(this.exercises = readExercises());
  }

  /**
   * Remember id->exercise for later fast lookup
   * @param exercises
   */
  private void populateIDToExercise(Collection<AmasExerciseImpl> exercises) {
    for (AmasExerciseImpl e : exercises) idToExercise.put(e.getOldID(), e);
  }

  /**
   * On startup, talk to the domino-ws to get the content for AMAS site.
   * @return
   */
  private List<AmasExerciseImpl> readExercises() {
    try {
      List<AmasExerciseImpl> exercises = getExercisesFromArray(getJSON());
      recordHierarchy(exercises);
      return exercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  /**
   * Talk to domino-ws service, e.g. http://domino-devel:9000/domino-ws/v1/projects/283/documents
   * For more <a href="https://gh.ll.mit.edu/Domino/domino-ws">domino-ws on github</a>
   * @return
   */
  private String getJSON() {
    try {
      return new HTTPClient().readFromGET(serverProps.getLessonPlan());
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
  }

  private List<AmasExerciseImpl> getExercisesFromArray(String json) {
    JSONArray content = JSONArray.fromObject(json);
    List<AmasExerciseImpl> exercises = new ArrayList<>();
    for (int i = 0; i < content.size(); i++) {
      JSONObject jsonObject = content.getJSONObject(i);
      exercises.add(toAMASExercise(jsonObject));
    }

    return exercises;
  }

  /**
   * Use the unit/chapter meta data on exercies to build a hierarchy
   * @param exercises
   */
  private void recordHierarchy(Collection<AmasExerciseImpl> exercises) {
    for (AmasExerciseImpl ex : exercises) {
      Collection<SectionHelper.Pair> pairs = new ArrayList<>();
      for (Map.Entry<String, String> pair : ex.getUnitToValue().entrySet()) {
        pairs.add(sectionHelper.addExerciseToLesson(ex, pair.getKey(), pair.getValue()));
      }
      sectionHelper.addAssociations(pairs);
    }
  }

  /**
   * Read JSON returned from domino-ws service for documents
   * E.g. the url might look like : http://domino-devel:9000/domino-ws/v1/projects/283/documents
   *
   * @param jsonObject
   * @return
   */
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
      if (!srcAudio.isEmpty()) {
        if (!srcAudio.startsWith("http")) {
         // srcAudio = URLEncoder.encode(srcAudio, "UTF-8");
          srcAudio = srcAudio.replaceAll("\\s++","%20");
          srcAudio = serverProps.getAudioAttachPrefix()+srcAudio;
        }
      }
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

  /**
   * @see DatabaseImpl#getAMASExercises()
   * @return
   */
  @Override
  public List<AmasExerciseImpl> getRawExercises() {
    return exercises;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getAMASExercise
   * @param id
   * @return
   */
  @Override
  public AmasExerciseImpl getExercise(int id) {
    if (idToExercise.isEmpty()) logger.warn("huh? couldn't find any exercises..?");
    if (!idToExercise.containsKey(id)) {
      logger.warn("couldn't find " + id + " in " + idToExercise.size() + " exercises...");
    }
    return idToExercise.get(id);
  }

  /**
   * @see DatabaseImpl#makeDAO
   * @return
   */
  @Override
  public int getNumExercises() {
    return exercises.size();
  }

  /**
   * @see DatabaseImpl#getAMASSectionHelper()
   * @return
   */
  @Override
  public SectionHelper<AmasExerciseImpl> getSectionHelper() {
    return sectionHelper;
  }

  @Override
  public void reload() {
    exercises = null;
    idToExercise.clear();
    sectionHelper.clear();
    readExercisesFromURL();
  }

  @Override
  public List<String> getTypeOrder() {
    return null;
  }

  @Override
  public Set<Integer> getIDs() {
    return null;
  }

  @Override
  public boolean isConfigured() {
    return false;
  }
}
