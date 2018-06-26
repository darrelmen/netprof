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

package mitll.langtest.server.database.annotation;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class BaseAnnotationDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BaseAnnotationDAO.class);
  public static final String CORRECT = "correct";
  int defectDetector;

  private Map<Integer, List<UserAnnotation>> exerciseToAnnos = null;

  BaseAnnotationDAO(Database database, int defectDetector) {
    super(database);
    this.defectDetector = defectDetector;
  }

  abstract List<UserAnnotation> getAll(int userid);

  /**
   * TODO : this seems like a bad idea...
   *
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @return
   * @see IUserListManager#addDefect
   */
  public boolean hasDefect(int exerciseID, String field, String status, String comment) {
    List<UserAnnotation> userAnnotations = getDefectsForExercise(exerciseID);
    if (userAnnotations == null) {
      return false;
    }
    Map<String, ExerciseAnnotation> latestByExerciseID = getFieldToAnnotationMap(userAnnotations);
    ExerciseAnnotation annotation = latestByExerciseID.get(field);
    return (annotation != null) && (annotation.getStatus().equals(status) && annotation.getComment().equals(comment));
  }

  /**
   * TODO : this seems like a bad idea...
   *
   * @param userid
   * @seez #AnnotationDAO
   */
  protected void populate(int userid) {
    exerciseToAnnos = new HashMap<>();
    List<UserAnnotation> all = getAll(userid);
    for (UserAnnotation userAnnotation : all) {
      List<UserAnnotation> userAnnotations = getDefectsForExercise(userAnnotation.getExerciseID());
      if (userAnnotations == null) {
        exerciseToAnnos.put(userAnnotation.getExerciseID(), userAnnotations = new ArrayList<>());
      }
      userAnnotations.add(userAnnotation);
    }
  }

  private List<UserAnnotation> getDefectsForExercise(int exerciseID) {
    if (exerciseToAnnos == null) populate(defectDetector);
    return exerciseToAnnos.get(exerciseID);
  }

  /**
   * Always return the latest annotation.
   *
   * @param lists
   * @return
   */
  protected Map<String, ExerciseAnnotation> getFieldToAnnotationMap(Collection<UserAnnotation> lists) {
    Map<String, UserAnnotation> fieldToAnno = new HashMap<>();

    for (UserAnnotation annotation : lists) {
      UserAnnotation prevAnnotation = fieldToAnno.get(annotation.getField());
      if (prevAnnotation == null) fieldToAnno.put(annotation.getField(), annotation);
      else if (prevAnnotation.getTimestamp() < annotation.getTimestamp()) {
        fieldToAnno.put(annotation.getField(), annotation);
      }
    }
    if (lists.isEmpty()) {
      //logger.error("huh? no annotation with id " + unique);
      return Collections.emptyMap();
    } else {
      Map<String, ExerciseAnnotation> fieldToAnnotation = new HashMap<>();
      for (Map.Entry<String, UserAnnotation> pair : fieldToAnno.entrySet()) {
        fieldToAnnotation.put(pair.getKey(), new ExerciseAnnotation(pair.getValue().getStatus(), pair.getValue().getComment()));
      }
      //logger.debug("field->anno " + fieldToAnno);
      return fieldToAnnotation;
    }
  }

  boolean examineFields(boolean forDefects, Map<String, String> fieldToStatus) {
    boolean foundIncorrect = false;
    for (String latest : fieldToStatus.values()) {
      if (!latest.equals(CORRECT)) {
        //lists.add(prevExid);
        foundIncorrect = true;
        break;
      }
    }
    if (forDefects) {
      if (foundIncorrect) {
        // lists.add(prevExid);
        return true;
      }
    } else {
      if (!foundIncorrect) {
        //lists.add(prevExid);
        return true;
      }
    }
    return false;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#getCommentedList(Collection)
   * @seex mitll.langtest.server.database.custom.UserListManager#getAmmendedStateMap
   */
/*  public Map<String, Long> getAnnotatedExerciseToCreator() {
    long then = System.currentTimeMillis();
    Map<String, Long> stateIds = getAnnotationExToCreator(true);
    long now = System.currentTimeMillis();
    if (now - then > 200)
      logger.debug("getAnnotatedExerciseToCreator took " + (now - then) + " millis to find " + stateIds.size());
    return stateIds;
  }*/

  //abstract Map<String, Long> getAnnotationExToCreator(boolean forDefects);
}
