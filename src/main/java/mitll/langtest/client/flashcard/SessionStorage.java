package mitll.langtest.client.flashcard;

import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.dialog.RehearseViewHelper;

public class SessionStorage {
  private final String key;
  protected final KeyStorage storage;

  public SessionStorage(KeyStorage storage, String key) {
    this.storage = storage;
    this.key = key;
  }

  /**
   * The idea is that we remember the current session id across page reloads...
   */
  void clearSession() {
    storage.removeValue(key);
    //  logger.info("clearSession " );
  }

  /**
   * @see RehearseViewHelper#gotPlay
   */
  public void storeSession() {
    storeSession(System.currentTimeMillis());
  }

  /**
   * @see PolyglotFlashcardFactory#startTimedRun()
   * @param millis
   */
  private void storeSession(long millis) {
    storage.storeValue(key, "" + millis);
    // logger.info("storeSession " + millis);
  }

  public long getSession() {
    return getLongValue(key);
  }

  long getLongValue(String session) {
    String value = storage.getValue(session);
    if (value == null) return 0L;
    long i = 0L;
    try {
      i = Long.parseLong(value);
    } catch (NumberFormatException e) {

    }
    //   logger.info("getSession " + i);

    return i;
  }
}
