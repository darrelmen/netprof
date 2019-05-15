/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.annotation;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickAnnotation;
import mitll.npdata.dao.annotation.AnnotationDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Tuple4;

import java.sql.Timestamp;
import java.util.*;

public class SlickAnnotationDAO extends BaseAnnotationDAO implements IAnnotationDAO {
  private static final Logger logger = LogManager.getLogger(SlickAnnotationDAO.class);

  private final AnnotationDAOWrapper dao;

  public SlickAnnotationDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database, userDAO);
    dao = new AnnotationDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  public SlickAnnotation toSlick(UserAnnotation shared) {
    return new SlickAnnotation(-1,
        (int) shared.getCreatorID(),
        shared.getExerciseID(),
        new Timestamp(shared.getTimestamp()),
        shared.getField(),
        shared.getStatus(),
        shared.getComment()
    );
  }

  private UserAnnotation fromSlick(SlickAnnotation slick) {
    return new UserAnnotation(
        slick.exid(),
        slick.field(),
        slick.status(),
        slick.comment(),
        slick.userid(),
        slick.modified().getTime(),
        "" + slick.exid()
    );
  }

  public void addBulk(List<SlickAnnotation> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public void add(UserAnnotation word) {
    dao.insert(toSlick(word));
  }

  @Override
  List<UserAnnotation> getAll(int userid) {
    //   logger.info("getAllPredef - " + userid);
    Collection<SlickAnnotation> slickAnnotations = dao.byUser(userid);
    List<UserAnnotation> copy = new ArrayList<>();
    for (SlickAnnotation annotation : slickAnnotations) copy.add(fromSlick(annotation));
    return copy;
  }

/*  @Override
  public Collection<Integer> getAudioAnnos() {
    return dao.getOnlyAudioAnnos();
  }*/

  @Override
  public Map<String, ExerciseAnnotation> getLatestByExerciseID(int exerciseID) {
    long then = System.currentTimeMillis();

    Collection<SlickAnnotation> latestByExerciseID = dao.getLatestByExerciseID(exerciseID);

/*    if (!latestByExerciseID.isEmpty()) {
      logger.info("found " + latestByExerciseID.size() + " for "+exerciseID);
    }*/
    long now = System.currentTimeMillis();

    if (now - then > 20) {
      logger.info("getLatestByExerciseID took " + (now - then) + " millis to get ex->anno map for " + exerciseID);
    }

    return getFieldToAnnotationMapSlick(latestByExerciseID);
  }

  @Override
  public Map<Integer, Map<String, ExerciseAnnotation>> getLatestByExerciseIDs(Set<Integer> exerciseIDs) {
    Map<Integer, Collection<SlickAnnotation>> latestByExerciseIDs = dao.getLatestByExerciseIDs(exerciseIDs);
    Map<Integer, Map<String, ExerciseAnnotation>> exToAnnos = new HashMap<>(latestByExerciseIDs.size());
    latestByExerciseIDs.forEach((k, v) -> exToAnnos.put(k, getFieldToAnnotationMapSlick(v)));
    return exToAnnos;
  }

  /**
   * @param projID
   * @param isContext
   * @return
   * @see IUserListManager#getCommentedList
   */
  @Override
  public Set<Integer> getExercisesWithIncorrectAnnotations(int projID, boolean isContext) {
    Collection<Tuple4<Integer, String, String, Timestamp>> annoToCreator = dao.getAnnosGrouped(projID, isContext);

    logger.info("getExercisesWithIncorrectAnnotations " + annoToCreator.size() + " projID " + projID + " is context " + isContext);

    boolean debug = annoToCreator.size() < 20 || isContext;
    Integer prevExid = -1;

    Set<Integer> incorrect = new HashSet<>();

    Map<String, String> fieldToStatus = new HashMap<>();
    for (Tuple4<Integer, String, String, Timestamp> tuple4 : annoToCreator) {
      Integer exid = tuple4._1();
      String field = tuple4._2();
      String status = tuple4._3();
//      logger.info("getExercisesWithIncorrectAnnotations Got " + tuple4);
//      if (debug) logger.info("getExercisesWithIncorrectAnnotations ex " + exid + " : " + field + " : " + status);

      if (prevExid == -1) {
        prevExid = exid;
      } else if (!prevExid.equals(exid)) {
        // go through all the fields -- if the latest is "incorrect" on any field, it's a defect
        //examineFields(forDefects, lists, prevExid, fieldToStatus);
        if (examineFields(true, fieldToStatus)) {
          incorrect.add(prevExid);
        }
        fieldToStatus.clear();
        prevExid = exid;
      }

      fieldToStatus.put(field, status);
    }

    if (examineFields(true, fieldToStatus)) {
      incorrect.add(prevExid);
    }

    //logger.debug("getUserAnnotations forDefects " +forDefects+ " sql " + sql2 + " yielded " + exToCreator.size());
   /*   if (lists.size() > 20) {
        Iterator<String> iterator = lists.iterator();
        //for (int i = 0; i < 20;i++) logger.debug("\tgetUserAnnotations e.g. " + iterator.next() );
      }*/

//      finish(connection, statement, rs);


    logger.info("getExercisesWithIncorrectAnnotations from " + annoToCreator.size() + " returning " + incorrect.size());

//    if (incorrect.size()<20) incorrect.forEach(ex->logger.info("getExercisesWithIncorrectAnnotations return " + ex));

    return incorrect;
  }

  private Map<String, ExerciseAnnotation> getFieldToAnnotationMapSlick(Collection<SlickAnnotation> lists) {
    Map<String, SlickAnnotation> fieldToAnno = new HashMap<>();

    for (SlickAnnotation annotation : lists) {
      String field = annotation.field();
      SlickAnnotation prevAnnotation = fieldToAnno.get(field);
      if (prevAnnotation == null) fieldToAnno.put(field, annotation);
      else if (prevAnnotation.modified().getTime() < annotation.modified().getTime()) {
        fieldToAnno.put(field, annotation);
      }
    }
    if (lists.isEmpty()) {
      //logger.error("huh? no annotation with id " + unique);
      return Collections.emptyMap();
    } else {
      Map<String, ExerciseAnnotation> fieldToAnnotation = new HashMap<>();
      for (Map.Entry<String, SlickAnnotation> pair : fieldToAnno.entrySet()) {
        fieldToAnnotation.put(pair.getKey(), new ExerciseAnnotation(pair.getValue().status(), pair.getValue().comment()));
      }
      //logger.debug("field->anno " + fieldToAnno);
      return fieldToAnnotation;
    }
  }

/*
  @Override
  public Map<String, Long> getAnnotationExToCreator(boolean forDefects) {
    Collection<Tuple4<String, String, String, Timestamp>> annoToCreator = dao.getAnnoToCreator();

    Map<String, Long> exToCreator = Collections.emptyMap();
    try {
      exToCreator = new HashMap<String, Long>();
      String prevExid = "";
      long prevCreatorid = -1;

      Map<String, String> fieldToStatus = new HashMap<String, String>();
      for (Tuple4<String, String, String, Integer> tuple4 : annoToCreator) {
        String exid = tuple4._1();
        String field = tuple4._2();
        String status = tuple4._3();
        //long modified = rs.getTimestamp(4).getStart();
        int creatorid = tuple4._4();

        if (prevExid.isEmpty()) {
          prevExid = exid;
        } else if (!prevExid.equals(exid)) {
          // go through all the fields -- if the latest is "incorrect" on any field, it's a defect
          //examineFields(forDefects, lists, prevExid, fieldToStatus);
          if (examineFields(forDefects, fieldToStatus)) {
            exToCreator.put(prevExid, creatorid);
          }
          fieldToStatus.clear();
          prevExid = exid;
          prevCreatorid = creatorid;
        }

        fieldToStatus.put(field, status);
      }

      if (examineFields(forDefects, fieldToStatus)) {
        exToCreator.put(prevExid, prevCreatorid);
      }

      //logger.debug("getUserAnnotations forDefects " +forDefects+ " sql " + sql2 + " yielded " + exToCreator.size());
   */
/*   if (lists.size() > 20) {
        Iterator<String> iterator = lists.iterator();
        //for (int i = 0; i < 20;i++) logger.debug("\tgetUserAnnotations e.g. " + iterator.next() );
      }*//*


//      finish(connection, statement, rs);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return exToCreator;
  }

*/

/*  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }*/
}
