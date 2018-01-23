package mitll.langtest.server.database.report;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.LogAndNotify;
import net.sf.json.JSONObject;

import java.util.Map;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ReportingServices {
  /**
   * @see LangTestDatabaseImpl#sendReport
   * @param userID
   */
  void sendReport(int userID);

  String getReport(int year, JSONObject jsonObject);

  Map<String, Float> getMaleFemaleProgress(int projectid);
  void doReport();

  LogAndNotify getLogAndNotify();
}
