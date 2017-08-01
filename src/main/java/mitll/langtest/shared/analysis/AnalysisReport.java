package mitll.langtest.shared.analysis;

import java.io.Serializable;
import java.util.List;

public class AnalysisReport implements Serializable {
  private  List<WordScore> wordScores;
  private UserPerformance userPerformance;
  private  PhoneReport phoneReport;

  public AnalysisReport(){}

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
    return "UserPerf:"+userPerformance+
        "\n\tword scores"  + wordScores +
        "\n\tphone scores "+phoneReport;
    //+
     //   "\n\tphone to word " + phoneReport.getPhoneToWordAndScoreSorted().size();

  }
}
