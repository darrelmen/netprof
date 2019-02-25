package mitll.langtest.shared.analysis;

import java.util.Collection;
import java.util.Map;

public class PhoneReportRequest {

  private int userid;
  private int projid;
  private Map<String, Collection<String>> typeToValues;
  private long sessionID;
  private boolean sentencesOnly;

  public PhoneReportRequest(int userid, int projid, Map<String, Collection<String>> typeToValues,
                            long sessionID, boolean sentencesOnly) {
    this.userid = userid;
    this.projid = projid;
    this.typeToValues = typeToValues;
    this.sessionID = sessionID;
    this.sentencesOnly = sentencesOnly;
  }

  public int getUserid() {
    return userid;
  }

  public int getProjid() {
    return projid;
  }

  public Map<String, Collection<String>> getTypeToValues() {
    return typeToValues;
  }

  public long getSessionID() {
    return sessionID;
  }

  public boolean isSentencesOnly() {
    return sentencesOnly;
  }
}
