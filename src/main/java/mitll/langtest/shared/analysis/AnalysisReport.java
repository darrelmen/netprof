package mitll.langtest.shared.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import mitll.langtest.client.analysis.AnalysisTab;
import mitll.langtest.server.database.analysis.IAnalysis;

import java.io.Serializable;

/**
 * @see mitll.langtest.client.analysis.AnalysisTab#useReport
 */
public class AnalysisReport implements Serializable {
  private int req;
  private UserPerformance userPerformance;
  private PhoneSummary phoneReport;
  private int numScores;
  private long serverTime;

  public AnalysisReport() {
  }

  /**
   * @param userPerformance
   * @param phoneReport
   * @paramx wordScores
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getPerformanceReportForUser(int, int, int, int)
   */
  public AnalysisReport(UserPerformance userPerformance,
                        PhoneSummary phoneReport,
                        int numScores,
                        int req) {
    this.userPerformance = userPerformance;
    this.phoneReport = phoneReport;
    this.numScores = numScores;
    this.req = req;
  }

  /**
   * @see mitll.langtest.client.analysis.AnalysisTab#useReport
   * @return
   */
  public UserPerformance getUserPerformance() {
    return userPerformance;
  }

  /**
   *
   * @return
   */
  public PhoneSummary getPhoneSummary() {
    return phoneReport;
  }

  public int getNumScores() {
    return numScores;
  }

  public int getReq() {
    return req;
  }

  public long getServerTime() {
    return serverTime;
  }

  public void setServerTime(long serverTime) {
    this.serverTime = serverTime;
  }

  public String toString() {
    return "UserPerf:" +
        "\n\tperf         " + userPerformance +
        "\n\ttook         " + serverTime +
        "\n\tword scores  " + numScores + " scores " +
        "\n\tphone scores " + phoneReport;
    //+
    //   "\n\tphone to word " + phoneReport.getPhoneToWordAndScoreSorted().size();
  }
}
