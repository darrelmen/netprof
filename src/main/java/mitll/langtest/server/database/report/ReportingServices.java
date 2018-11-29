package mitll.langtest.server.database.report;

import com.google.gson.JsonObject;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.LogAndNotify;


import java.util.Map;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ReportingServices {
  /**
   * @param userID
   * @see LangTestDatabaseImpl#sendReport
   */
  void sendReport(int userID);

  String getReport(int year, JsonObject jsonObject);

  Map<String, Float> getMaleFemaleProgress(int projectid);

  void doReport();

  void sendReports();

  LogAndNotify getLogAndNotify();
}
