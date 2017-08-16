package mitll.langtest.shared.user;

public interface ReportUser {
  int getID();
  String getUserID();
  long getTimestampMillis();
  String getIpaddr();
  Kind getUserKind();
  String getDevice();
}
