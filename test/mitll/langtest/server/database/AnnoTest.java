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

package mitll.langtest.server.database;

import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AnnoTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(AnnoTest.class);

  @Test
  public void testAnno() {
    DatabaseImpl spanish = getDatabase("spanish");

    IAnnotationDAO annotationDAO = spanish.getAnnotationDAO();

    Collection<Integer> exids = annotationDAO.getAudioAnnos();
    int size = exids.size();
    Integer first = exids.iterator().next();
    logger.info("got " + size + " first " + first);
    for (Integer exid : exids) logger.info("got " + exid);

    Set<Integer> incorrectAnnotations = annotationDAO.getExercisesWithIncorrectAnnotations();
    logger.info("incorrect : got " + incorrectAnnotations.size() + " first " + incorrectAnnotations.iterator().next());

    AnnotationDAO dao = new AnnotationDAO(spanish, new UserDAO(spanish));

    Collection<Integer> audioAnnos = dao.getAudioAnnos();
    logger.info("got " + audioAnnos.size() + " first " + audioAnnos.iterator().next());
    for (Integer exid : audioAnnos) logger.info("truth " + exid);

    Set<Integer> incorrectAnnotations2 = dao.getExercisesWithIncorrectAnnotations();
    logger.info("incorrect truth: got " + incorrectAnnotations2.size() + " first " + incorrectAnnotations2.iterator().next());
  }

  @Test
  public void testAnno2() {
    DatabaseImpl spanish = getDatabase("spanish");

    IAnnotationDAO annotationDAO = spanish.getAnnotationDAO();
    Set<Integer> incorrectAnnotations = annotationDAO.getExercisesWithIncorrectAnnotations();
    logger.info("incorrect : got " + incorrectAnnotations.size() + " first " + incorrectAnnotations.iterator().next());

    AnnotationDAO dao = new AnnotationDAO(spanish, new UserDAO(spanish));

    Set<Integer> incorrectAnnotations2 = dao.getExercisesWithIncorrectAnnotations();
    logger.info("incorrect truth: got " + incorrectAnnotations2.size() + " first " + incorrectAnnotations2.iterator().next());
  }

  @Test
  public void testAnno3() {
    DatabaseImpl spanish = getDatabase("spanish");

    IAnnotationDAO annotationDAO = spanish.getAnnotationDAO();
    Map<String, ExerciseAnnotation> latestByExerciseID = annotationDAO.getLatestByExerciseID(1);
    logger.info("incorrect : got " + latestByExerciseID.size() + " first " + latestByExerciseID);

    AnnotationDAO dao = new AnnotationDAO(spanish, new UserDAO(spanish));

    Map<String, ExerciseAnnotation> latestByExerciseID2 =  dao.getLatestByExerciseID(1);
    logger.info("incorrect truth: got " + latestByExerciseID2.size() + " first " + latestByExerciseID2);
  }

  @Test
  public void testAnswerDAO() {
    DatabaseImpl spanish = getDatabase("spanish");
  }
}
