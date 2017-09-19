package mitll.langtest.shared.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import mitll.langtest.client.analysis.ShowTab;

import java.io.Serializable;
import java.util.List;

/**
 * @see mitll.langtest.client.analysis.AnalysisTab#useReport
 */
public class AnalysisReport implements Serializable {
  private List<WordScore> wordScores;
  private UserPerformance userPerformance;
  private PhoneReport phoneReport;

  public AnalysisReport() {
  }

  /**
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getPerformanceReportForUser(int, int, int)
   * @param userPerformance
   * @param wordScores
   * @param phoneReport
   */
  public AnalysisReport(UserPerformance userPerformance, List<WordScore> wordScores, PhoneReport phoneReport) {
    this.userPerformance = userPerformance;
    this.wordScores = wordScores;
    this.phoneReport = phoneReport;
  }

  public List<WordScore> getWordScores() {
    return wordScores;
  }

  public UserPerformance getUserPerformance() {
    return userPerformance;
  }

  public PhoneReport getPhoneReport() {
    return phoneReport;
  }

  public String toString() {
    return "UserPerf:" + userPerformance +
        "\n\tword scores" + wordScores +
        "\n\tphone scores " + phoneReport;
    //+
    //   "\n\tphone to word " + phoneReport.getPhoneToWordAndScoreSorted().size();

  }
}
