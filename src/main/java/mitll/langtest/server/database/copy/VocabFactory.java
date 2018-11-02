package mitll.langtest.server.database.copy;

import mitll.langtest.shared.exercise.VocabToken;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by go22670 on 6/5/17.
 *
 */
public class VocabFactory {
  public static final String HTML_TAG_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";

  private final Pattern pattern = Pattern.compile(HTML_TAG_PATTERN);

  public List<VocabToken> getTokens(String t) {
    Matcher matcher;
    matcher = pattern.matcher(t);

    List<VocabToken> tokens = new ArrayList<>();
    int start = -1;
    while (matcher.find()) {
      if (start == -1) start = 0;
      int start1 = matcher.start();
      int end    = matcher.end();

      if (start1 > start) {
        tokens.add(new VocabToken(t.substring(start, start1)));
      }
      tokens.add(new VocabToken(true, t.substring(start1, end)));
      start = end;
    }

    if (start == -1) {
      tokens.add(new VocabToken(t));
    } else if (start < t.length()) {
      tokens.add(new VocabToken(false, t.substring(start)));
    }
    return tokens;
  }
}
