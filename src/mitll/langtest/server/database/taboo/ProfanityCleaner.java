package mitll.langtest.server.database.taboo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/5/13
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProfanityCleaner {
  private final List<String> nastyWords = Arrays.asList("shit", "piss", "fuck", "fucker", "fucking", "cunt", "cocksucker", "motherfucker", "tits", "asshole");
  private final Set<String> sevenWords = new HashSet<String>(nastyWords);

  /**
   * This can be defeated relatively easily...
   *
   * @param answer
   * @return
   */
  public String replaceProfanity(String answer) {
    String[] words = answer.split("\\p{Z}+"); // fix for unicode spaces! Thanks Jessica!
    StringBuilder builder = new StringBuilder();
    for (String word : words) {
      if (sevenWords.contains(word)) builder.append(word.replaceAll(".","_"));
      else builder.append(word);
      builder.append(" ");
    }
    return builder.toString().trim();
  }
}
