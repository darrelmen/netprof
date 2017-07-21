package mitll.langtest.server.scoring;

import java.util.List;
import java.util.Set;

/**
 * @see mitll.langtest.server.audio.AudioFileHelper#countPhones
 */
public class PhoneInfo {
  private final List<String> firstPron;
  private final Set<String> phoneSet;

  public PhoneInfo(List<String> firstPron, Set<String> phoneSet) {
    this.firstPron = firstPron;
    this.phoneSet = phoneSet;
  }

  public List<String> getFirstPron() {
    return firstPron;
  }

/*
  public Set<String> getPhoneSet() {
    return phoneSet;
  }
*/

  public String toString() {
    return "Phones " + phoneSet + " " + getFirstPron();
  }
}
