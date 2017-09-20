package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.mail.MailSupport;
import mitll.npdata.dao.SlickProject;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface IReport {
  boolean doReport(int projid,
                   String language,
                   String site,
                   ServerProperties serverProps,
                   MailSupport mailSupport,
                   PathHelper pathHelper,
                   boolean forceSend);
  JSONObject writeReportToFile(ReportStats stats, PathHelper pathHelper, List<ReportStats> allReports) throws IOException;

  /**
   * @param projects
   * @param jsonObject
   * @param year
   * @param allReports
   * @return
   */
  String getAllReports(Collection<SlickProject> projects, JSONObject jsonObject, int year, List<ReportStats> allReports);

  File getReportPathDLI(PathHelper pathHelper, String suffix);
}
