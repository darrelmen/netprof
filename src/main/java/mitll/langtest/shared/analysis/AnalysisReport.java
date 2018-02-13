package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.analysis.IAnalysis;

import java.io.Serializable;

/**
 * @see mitll.langtest.client.analysis.AnalysisTab#useReport
 */
public class AnalysisReport implements Serializable {
  private int req;
  private UserPerformance userPerformance;
  private PhoneReport phoneReport;
  private int numScores;
  public AnalysisReport() {
  }

  /**
   * @see IAnalysis#getPerformanceReportForUser(int, int, int, int)
   * @param userPerformance
   * @paramx wordScores
   * @param phoneReport
   */
  public AnalysisReport(UserPerformance userPerformance,
                        PhoneReport phoneReport,
                        int numScores,
                        int req) {
    this.userPerformance = userPerformance;
    this.phoneReport = phoneReport;
    this.numScores = numScores;
    this.req=req;
  }

  public UserPerformance getUserPerformance() {
    return userPerformance;
  }

  public PhoneReport getPhoneReport() {
    return phoneReport;
  }

  public int getNumScores() {
    return numScores;
  }

  public int getReq() {
    return req;
  }

  public String toString() {
    return "UserPerf:" +
        "\n\tperf :       " + userPerformance +
        "\n\tword scores  " +numScores+ " scores " +
        "\n\tphone scores " + phoneReport;
    //+
    //   "\n\tphone to word " + phoneReport.getPhoneToWordAndScoreSorted().size();
  }
}
