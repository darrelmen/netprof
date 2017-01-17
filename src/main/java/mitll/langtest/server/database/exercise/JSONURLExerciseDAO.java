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
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Read from a domino url - either documents or exam
 * <p>
 * For exam, maybe we need a "latestExam" link?
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/16.
 * @deprecated not sure if this will ever be used --
 */
public class JSONURLExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = LogManager.getLogger(JSONURLExerciseDAO.class);
  private final Collection<String> typeOrder;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public JSONURLExerciseDAO(
      ServerProperties serverProps,
      IUserListManager userListManager,
      boolean addDefects) {
    super(serverProps, userListManager, addDefects, serverProps.getLanguage(),-1);
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
   * TODO : make this not use the server props lesson plan
   * E.g read from http://domino-devel:9000/domino-ws/v1/projects/354/documents
   * @return
   */
  private String getJSON() {
    String lessonPlan = serverProps.getLessonPlan();
    logger.info(" Reading from " + lessonPlan);
    this.now = System.currentTimeMillis();
    return new HTTPClient().readFromGET(lessonPlan);
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
    int dominoID     = jsonObject.getInt("id");
    String npDID = isLegacyExercise(metadata) ? metadata.getString("npDID") : ""+dominoID;

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
        dominoID,
        -1);

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

  /**
   * TODO : include alt fl field - e.g mandarin simpified/traditional, serbo-croatian
   * @param content
   * @param exercise
   */
  private void addContextSentences(JSONObject content, Exercise exercise) {
    JSONObject contextSentences = content.getJSONObject("q-lst");
    for (Object key : contextSentences.keySet()) {
      JSONObject pair = contextSentences.getJSONObject(key.toString());
      String stem = pair.getString("stem");
      String trans = pair.getString("trans");
      exercise.addContext(stem, "", trans);
    }
  }

  private String noMarkup(String source) {
    return source.replaceAll("\\<.*?>", "");
  }

  @Override
  public Map<Integer, String> getIDToFL(int projid) {
    return null;
  }
}
