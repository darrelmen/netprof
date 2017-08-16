package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.mail.MailSupport;
import mitll.npdata.dao.SlickProject;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.Collection;

public interface IReport {
  boolean doReport(int projid,
                   String language,
                   String site,
                   ServerProperties serverProps,
                   MailSupport mailSupport,
                   PathHelper pathHelper);

  boolean isTodayAGoodDay();

  JSONObject writeReportToFile(int projid, PathHelper pathHelper, String language, int year, String name) throws IOException;

  String getAllReports(Collection<SlickProject> projects, JSONObject jsonObject, int year);
}
