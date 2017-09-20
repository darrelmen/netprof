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
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.shared.analysis.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class EasyAnalysisTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(EasyAnalysisTest.class);
  public static final int MAX = 200;

  //private final DateFormat debugShortFormat = DateFormat.getInstance().get("MMM d yyyy");

  @Test
  public void testAnalysis() {
    DatabaseImpl andPopulate = getAndPopulate();
    AnalysisReport performanceReportForUser = getPerformanceReportForUser(andPopulate, 2, 295, 1, -1);

    logger.info("Got " + performanceReportForUser);
    PhoneReport phoneReport = performanceReportForUser.getPhoneReport();
    logger.info("phone Report " + phoneReport);

    Map<String, PhoneStats> phoneToAvgSorted = phoneReport.getPhoneToAvgSorted();

    phoneToAvgSorted.forEach((k, v) -> {
      logger.info(k + " = " + v.getSessions().size() + " sessions");
      v.getSessions()
          .forEach(phoneSession -> logger.info("\t" + k + " = " +
              debugFormat(phoneSession.getStart()) + "-" + debugFormat(phoneSession.getEnd())));
    });

    Map<String, List<WordAndScore>> phoneToWordAndScoreSorted = phoneReport.getPhoneToWordAndScoreSorted();

    phoneToWordAndScoreSorted.forEach((k, v) -> {
      v.sort(new Comparator<WordAndScore>() {
        @Override
        public int compare(WordAndScore o1, WordAndScore o2) {
          return Long.compare(o1.getTimestamp(), o2.getTimestamp());
        }
      });

      logger.info(k + " = " + v.size() + " words");
      v
          .forEach(phoneSession -> logger.info("\t" + k + " = " +
              phoneSession.getWord() + " = " +
              debugFormat(phoneSession.getTimestamp())));
    });

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

  public AnalysisReport getPerformanceReportForUser(DatabaseImpl db, int projectID, int id, int minRecordings, int listid) {
    // logger.info("getPerformanceForUser " +id+ " list " + listid + " min " + minRecordings);
    if (projectID == -1) {
      return new AnalysisReport();
    } else {
      SlickAnalysis slickAnalysis =
          new SlickAnalysis(
              db.getDatabase(),
              db.getPhoneDAO(),
              db.getAudioDAO(),
              (SlickResultDAO) db.getResultDAO(),
              db.getProject(projectID).getLanguage(),
              projectID
          );

      return slickAnalysis.getPerformanceReportForUser(id, minRecordings, listid);
    }
  }
}
