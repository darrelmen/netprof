/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
