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

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickAnnotation;
import mitll.npdata.dao.annotation.AnnotationDAOWrapper;
import org.apache.log4j.Logger;
import scala.Tuple4;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class SlickAnnotationDAO
    extends BaseAnnotationDAO implements IAnnotationDAO, ISchema<UserAnnotation, SlickAnnotation> {
  private static final Logger logger = Logger.getLogger(SlickAnnotationDAO.class);

  private final AnnotationDAOWrapper dao;

  public SlickAnnotationDAO(Database database, DBConnection dbConnection, int defectDetector) {
    super(database);
    dao = new AnnotationDAOWrapper(dbConnection);
    populate(defectDetector);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public SlickAnnotation toSlick(UserAnnotation shared, String language) {
    return new SlickAnnotation(-1,
        (int) shared.getCreatorID(),
        shared.getExerciseID(),
        shared.getField(),
        shared.getStatus(),
        shared.getComment(),
        new Timestamp(shared.getTimestamp()));
  }

  @Override
  public UserAnnotation fromSlick(SlickAnnotation slick) {
    return new UserAnnotation(
        slick.exid(),
        slick.field(),
        slick.status(),
        slick.comment(), slick.userid(), slick.modified().getTime()
    );
  }

  public void insert(SlickAnnotation word) {
    dao.insert(word);
  }

  public void addBulk(List<SlickAnnotation> bulk) {
    dao.addBulk(bulk);
  }

  //  @Override
  public void add(UserAnnotation word) {
    dao.insert(toSlick(word, ""));
  }


  @Override
  List<UserAnnotation> getAll(int userid) {
    Collection<SlickAnnotation> slickAnnotations = dao.byUser(userid);

    List<UserAnnotation> copy = new ArrayList<>();
    for (SlickAnnotation annotation : slickAnnotations) copy.add(fromSlick(annotation));
    return copy;
  }

  @Override
  public Collection<String> getAudioAnnos() {
    return dao.getOnlyAudioAnnos();
  }

  @Override
  public Map<String, ExerciseAnnotation> getLatestByExerciseID(String exerciseID) {
    return getFieldToAnnotationMapSlick(dao.getLatestByExerciseID(exerciseID));
  }

  @Override
  public Set<String> getExercisesWithIncorrectAnnotations() {
    Collection<Tuple4<String, String, String, Timestamp>> annoToCreator = dao.getAnnoToCreator();

    String prevExid = "";

    Set<String> incorrect = new HashSet<>();

    Map<String, String> fieldToStatus = new HashMap<String, String>();
    for (Tuple4<String, String, String, Timestamp> tuple4 : annoToCreator) {
      String exid = tuple4._1();
      String field = tuple4._2();
      String status = tuple4._3();

      if (prevExid.isEmpty()) {
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

    return incorrect;
  }

  protected Map<String, ExerciseAnnotation> getFieldToAnnotationMapSlick(Collection<SlickAnnotation> lists) {
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

}
