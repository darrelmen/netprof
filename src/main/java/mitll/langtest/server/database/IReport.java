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

package mitll.langtest.server.database;

import com.google.gson.JsonObject;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.npdata.dao.SlickProject;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface IReport {
  /**
   * @see mitll.langtest.server.database.report.ReportHelper#getReportStats(IReport, boolean, List)
   * @param projid
   * @param language
   * @param site
   * @param pathHelper
   * @param forceSend
   * @param getAllYears
   * @return
   */
  List<ReportStats> doReport(int projid,
                             String language,
                             String site,
                             PathHelper pathHelper,
                             boolean forceSend,
                             boolean getAllYears);

  /**
   * @see mitll.langtest.server.database.report.ReportHelper#doReportForYear(IReport, int)
   * @param stats
   * @param pathHelper
   * @param allReports
   * @return
   * @throws IOException
   */
  JsonObject writeReportToFile(ReportStats stats, PathHelper pathHelper, List<ReportStats> allReports) throws IOException;

  /**
   * @param projects
   * @param jsonObject
   * @param year
   * @param allReports
   * @return
   * @see DatabaseImpl#getReport(int, JsonObject)
   */
  String getAllReports(Collection<SlickProject> projects, JsonObject jsonObject, int year, List<ReportStats> allReports);

  /**
   * @see mitll.langtest.server.database.report.ReportHelper#sendReports(IReport, boolean, int)
   * @param mailSupport
   * @param reportEmails
   * @param receiverNames
   * @param reportStats
   * @param pathHelper
   */
  void sendExcelViaEmail(MailSupport mailSupport,
                         List<String> reportEmails,
                         List<String> receiverNames,
                         List<ReportStats> reportStats,
                         PathHelper pathHelper);

  File getSummaryReport(List<ReportStats> allReports, PathHelper pathHelper);

  /**
   * @see Report#getSummaryReport(List, PathHelper)
   * @param pathHelper
   * @param suffix
   * @return
   */
//  File getReportPathDLI(PathHelper pathHelper, String suffix);
}
