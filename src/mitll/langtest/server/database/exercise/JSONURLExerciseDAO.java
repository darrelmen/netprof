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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/16.
 */
public class JSONURLExerciseDAO extends JSONExerciseDAO {
  private static final Logger logger = Logger.getLogger(JSONURLExerciseDAO.class);
  public static final String DOCUMENTS = "documents";
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
   * @param serverProps
   */
  void readProjectInfo(ServerProperties serverProps) {
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
