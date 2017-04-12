package mitll.langtest.client.custom.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * A class reresenting the bounds of a word within a string.
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

  public String [] getParts(String suggestion, int cursor) {
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

  public int getEndIndex() {
    return endIndex;
  }

  public String toString() {
    return "[" + startIndex + "-" + endIndex + "]";
  }
}
