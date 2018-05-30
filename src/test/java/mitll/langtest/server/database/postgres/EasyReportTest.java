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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ProjectSync;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.DominoUpdateResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class EasyReportTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(EasyReportTest.class);
  public static final int MAX = 200;


/*  @Test
  public void testReport() {
    DatabaseImpl andPopulate = getAndPopulate();
    andPopulate.getReport();
    andPopulate.close();
  }*/

  @Test
  public void test() {
    DatabaseImpl andPopulate = getAndPopulate();
    //  andPopulate.sendReport(-1);
    andPopulate.close();
  }

  @Test
  public void testSegment() {
    DatabaseImpl andPopulate = getAndPopulate();

    Project project = andPopulate.getProject(21);

    String temp = "１";
    String sentence = "きのうの午後５時から７時まで黒い車が止まっていました";
    logger.info("\n\ntestSegment for " + sentence);

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    String res = project.getAudioFileHelper().getSegmented(sentence);

    logger.info("\n\ntestSegment res " + res);

    andPopulate.close();

  }

  @Test
  public void testSync() {
    DatabaseImpl andPopulate = getAndPopulate();
    int projectid = 16;

    ProjectSync projectSync = andPopulate.getProjectSync();

    ImportInfo importFromDomino = andPopulate.getProjectManagement().getImportFromDomino(projectid);
    DominoUpdateResponse dominoUpdateResponse = projectSync.getDominoUpdateResponse(projectid, 2, false, importFromDomino);


    logger.info("Got " + dominoUpdateResponse);

    andPopulate.close();
  }

  @Test
  public void testSendReport() {
    DatabaseImpl andPopulate = getAndPopulate();
    andPopulate.sendReport(-1);
    andPopulate.close();
  }

  @Test
  public void testQuiz() {
    DatabaseImpl andPopulate = getAndPopulate();

    int projectid = 2;
    showQuizInfo(andPopulate, projectid);
    showQuizInfo(andPopulate, 5);
    //  andPopulate.sendReport(-1);
    andPopulate.close();
  }

  private void showQuizInfo(DatabaseImpl andPopulate, int projectid) {
    Project project = andPopulate.getProject(projectid);

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();
    //String next = sectionHelper.getTypeOrder().iterator().next();
    sectionHelper.report();

    Collection<CommonExercise> first = sectionHelper.getFirst();
    logger.info("first " + first.size() + " e.g. " + first.iterator().next());

    /*ISection<CommonExercise> quizSectionHelper = andPopulate.getQuizSectionHelper(projectid);//, project.getSectionHelper().getFirst());
    HashMap<String, Collection<String>> typeToSection = new HashMap<>();
    typeToSection.put("QUIZ", Arrays.asList("quiz"));
    typeToSection.put("Unit", Arrays.asList("Dry Run"));
*/
   /* Collection<CommonExercise> exercisesForSelectionState = quizSectionHelper.getExercisesForSelectionState(typeToSection);
    logger.info("1 for  " + typeToSection + " got " +exercisesForSelectionState.size());


    typeToSection.put("Unit", Arrays.asList("Quiz"));
    Collection<CommonExercise> exercisesForSelectionState2 = quizSectionHelper.getExercisesForSelectionState(typeToSection);

    logger.info("2 for  " + typeToSection + " got " +exercisesForSelectionState2.size());*/
  }

  @Test
  public void testReportWrite() {
    DatabaseImpl andPopulate = getAndPopulate();
    andPopulate.doReportForYear(-1);
    andPopulate.close();
  }

  @Test
  public void testFind() {
    DatabaseImpl andPopulate = getAndPopulate();
    Project project = andPopulate.getProject(3);
    CommonExercise vit = project.getExerciseBySearch("vit");
    logger.info(vit);
    logger.info(project.getExerciseBySearch("Vit"));
    logger.info(project.getExerciseBySearch("gourd"));
    //andPopulate.doReportForYear(-1);
    andPopulate.close();
  }

  @Test
  public void testJson() {
    DatabaseImpl andPopulate = getAndPopulate();
    //   Project project = andPopulate.getProject(3);
    JsonExport jsonExport = andPopulate.getJSONExport(3);
    // long now = System.currentTimeMillis();

    JSONArray contentAsJson = jsonExport.getContentAsJson(false);
    logger.info("Got\n\t" + contentAsJson);
  }

  @Test
  public void testJson2() {
    DatabaseImpl andPopulate = getAndPopulate();
    //   Project project = andPopulate.getProject(3);
    HashMap<String, Collection<String>> typeToValues = new HashMap<>();
    typeToValues.put("Unit", Collections.singleton("21"));
    JSONObject jsonPhoneReport = andPopulate.getJsonPhoneReport(295, 2, typeToValues);
    // long now = System.currentTimeMillis();

    logger.info("Got\n\t" + jsonPhoneReport);
  }


  @Test
  public void testProp() {
    DatabaseImpl andPopulate = getAndPopulate();

    Project project = andPopulate.getProject(2);
    int webservicePort = project.getWebservicePort();
    logger.info("port " + webservicePort);
    logger.info("host " + project.getWebserviceHost());
  }
}
