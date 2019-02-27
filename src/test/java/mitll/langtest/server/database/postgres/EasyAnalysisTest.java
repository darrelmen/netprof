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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.shared.analysis.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

public class EasyAnalysisTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(EasyAnalysisTest.class);
  public static final int MAX = 200;

  @Test
  public void testFile() {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectManagement projectManagement = andPopulate.getProjectManagement();
    Project project = projectManagement.getProject(3, true);

    int userForFile3 = projectManagement.getUserForFile("7854/1/subject-659/answer_1542410949930.wav");
    logger.info("user " +userForFile3);

    int userForFile2 = projectManagement.getUserForFile("/answers/spanish/7854/1/subject-659/answer_1542410949930.wav");
    logger.info("user " +userForFile2);

    int userForFile = projectManagement.getUserForFile("/opt/netprof/answers/spanish/7854/1/subject-659/answer_1542410949930.wav");
    logger.info("user " +userForFile);


  }

  @Test
  public void testAnalysis() {
    DatabaseImpl andPopulate = getAndPopulate();
    AnalysisReport performanceReportForUser = null;//getPerformanceReportForUser(andPopulate, 2, 295, 1, -1);

    logger.info("Got " + performanceReportForUser);
    //  logger.info("Got word score " + performanceReportForUser.getWordScores().size() + " num");

    PhoneSummary phoneReport = performanceReportForUser.getPhoneSummary();
    logger.info("phone Report " + phoneReport);

    Map<String, PhoneStats> phoneToAvgSorted = phoneReport.getPhoneToAvgSorted();

    phoneToAvgSorted.forEach((k, v) -> {
      logger.info(k + " = " + v.getSessions().size() + " sessions");

/*
      v.getSessions()
          .forEach(phoneSession -> logger.info("\t" + k + " = " +
              debugFormat(phoneSession.getStart()) + "-" + debugFormat(phoneSession.getEnd())));
*/
    });
    getPerformanceReportForUser2(andPopulate, 7, 729, -1, "b", "b-jj");
/*
    Map<String, Map<String, List<WordAndScore>>> phoneToWordAndScoreSorted = phoneReport.getPhoneToWordAndScoreSorted();

    phoneToWordAndScoreSorted.forEach((phone, v) -> {
      // v.sort(Comparator.comparingLong(WordScore::getTimestamp));

      logger.info(phone + " = " + v.size() + " words");
*//*      v
          .forEach(phoneSession -> logger.info("\t" + phone + " = " +
              phoneSession.getWord() + " = " +
              debugFormat(phoneSession.getTimestamp())));*//*
    });*/

    String x = "nj";
    List<WordAndScore> performanceReportForUser2 = getPerformanceReportForUser2(andPopulate, 2, 295, x);
    logger.info("for " + x +
        " " + performanceReportForUser2.size());

  /*  List<WordScore> wordScores = performanceReportForUser.getWordScores();
    wordScores.sort(new Comparator<WordScore>() {
      @Override
      public int compare(WordScore o1, WordScore o2) {
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
      }
    });
    for (WordScore wordScore : wordScores) {
      logger.info("got " + wordScore.getExid() + " " + wordScore.getResultID() + " " + new Date(wordScore.getTimestamp()));
    }*/

    andPopulate.close();
  }


  private String debugFormat(long first) {
    return new Date(first).toString();
//    return debugShortFormat.format(new Date(first));
  }
/*
  private AnalysisReport getPerformanceReportForUser(DatabaseImpl db, int projectID, int id, int minRecordings, int listid) {
    // logger.info("getPerformanceForUser " +id+ " list " + listid + " min " + minRecordings);
    if (projectID == -1) {
      return new AnalysisReport();
    } else {
      return getSlickAnalysis(db, projectID).getPerformanceReportForUser(analysisRequest);
    }
  }*/

  private List<WordAndScore> getPerformanceReportForUser2(DatabaseImpl db,
                                                          int projectID, int id, int listid, String phone, String bigram) {
    // logger.info("getPerformanceForUser " +id+ " list " + listid + " min " + minRecordings);
    if (projectID == -1) {
      return new ArrayList<>();
    } else {
      return getSlickAnalysis(db, projectID).getWordAndScoreForPhoneAndBigram(new AnalysisRequest());
    }
  }

  private List<WordAndScore> getPerformanceReportForUser2(DatabaseImpl db, int projectID, int id, String phone) {
    // logger.info("getPerformanceForUser " +id+ " list " + listid + " min " + minRecordings);
    if (projectID == -1) {
      return new ArrayList<>();
    } else {
      SlickAnalysis slickAnalysis = getSlickAnalysis(db, projectID);

      long from = new Date().getTime();
      long ten = 10l * 365l * 24l * 60l * 60l * 1000l;
      from -= ten;
      long to = new Date().getTime();
      to += ten;

      logger.info("from " + new Date(from));
      logger.info("to   " + new Date(to));

      to = 1506463703650l;
      // logger.info("from " + new Date(from));

      from = 1457051837030l;
      from--;
      to = 1485273391652l;
      to++;
      //  return slickAnalysis.getPhoneReportFor(id, -1, phone, from, to);
      return slickAnalysis.getWordAndScoreForPhoneAndBigram(new AnalysisRequest());
    }
  }

  @NotNull
  private SlickAnalysis getSlickAnalysis(DatabaseImpl db, int projectID) {
    return new SlickAnalysis(
        db.getDatabase(),
        db.getPhoneDAO(),
        db.getAudioDAO(),
        (SlickResultDAO) db.getResultDAO(),
        db.getProject(projectID).getLanguageEnum(),
        projectID,
        false);
  }
}
