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
