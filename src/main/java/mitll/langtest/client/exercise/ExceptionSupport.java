package mitll.langtest.client.exercise;

public interface ExceptionSupport {
  String logException(Throwable throwable);

  void logMessageOnServer(String message, String prefix, boolean sendEmail);
}
