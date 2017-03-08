package mitll.langtest.server.database.report;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.mail.MailSupport;

import java.util.Map;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ReportingServices {
  Map<String, Float> getMaleFemaleProgress(int projectid);

  void doReport(ServerProperties serverProps, String site, MailSupport mailSupport, PathHelper pathHelper);
}
