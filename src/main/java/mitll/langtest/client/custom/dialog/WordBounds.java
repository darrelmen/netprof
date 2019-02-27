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

package mitll.langtest.client.custom.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * A class representing the bounds of a word within a string.
 * <p>
 * The bounds are represented by a {@code startIndex} (inclusive) and
 * an {@code endIndex} (exclusive).
 */
public class WordBounds implements Comparable<WordBounds> {
  private final int startIndex;
  private final int endIndex;

  WordBounds(int startIndex, int length) {
    this.startIndex = startIndex;
    this.endIndex = startIndex + length;
  }

  public int compareTo(WordBounds that) {
    int comparison = this.startIndex - that.startIndex;
    if (comparison == 0) {
      comparison = that.endIndex - this.endIndex;
    }
    return comparison;
  }

   String [] getParts(String suggestion, int cursor) {
    String part1 = suggestion.substring(cursor, startIndex);
    String part2 = suggestion.substring(startIndex, endIndex);
    String[] strings = new String[2];
    strings[0] = part1;
    strings[1] = part2;
    return strings;
  }

  public List<String> getTriple(String suggestion) {
    List<String> parts = new ArrayList<>();
    parts.add(suggestion.substring(0, startIndex));
    parts.add(suggestion.substring(startIndex, endIndex));
    parts.add(suggestion.substring(endIndex));
    return parts;
  }

  public int getStartIndex() {
    return startIndex;
  }
  int getEndIndex() {
    return endIndex;
  }

  public String toString() {
    return "[" + startIndex + "-" + endIndex + "]";
  }
}
