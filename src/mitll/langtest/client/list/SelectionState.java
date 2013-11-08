package mitll.langtest.client.list;

import com.google.gwt.user.client.History;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 4/18/13
* Time: 6:31 PM
* To change this template use File | Settings | File Templates.
*/
public class SelectionState {
  private String item;
  private Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();

  /**
   * Populated from history token!
   * @see mitll.langtest.client.list.ListInterface#getExercises(long, boolean)
   */
  public SelectionState() {
    this(History.getToken(), true);
  }

  /**
   * @see mitll.langtest.client.list.section.SectionExerciseList#getSelectionState(String)
   * @param token
   * @param removePlus
   */
  public SelectionState(String token, boolean removePlus) {
    parseToken(removePlus ? unencodeToken(token) : unencodeToken2(token));
  }

  /**
   * @see mitll.langtest.client.list.ListInterface#getExercises(long, boolean)
   */
  public boolean isEmpty() { return getTypeToSection().isEmpty(); }

  private String unencodeToken(String token) {
    token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ").replaceAll("\\+", " ");
    return token;
  }

  private String unencodeToken2(String token) {
    token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ");
    return token;
  }

  boolean debug = false;
  private void parseToken(String token) {
    String[] parts = token.split(";");

    for (String part : parts) {
      if (debug) System.out.println("parseToken : part " + part + " : " + Arrays.asList(parts));

      if (part.contains("=")) {
        String[] segments = part.split("=");
        if (debug) System.out.println("\tpart " + part + " : " + Arrays.asList(segments));

        String type = segments[0].trim();
        String section = segments[1].trim();
        String[] split = section.split(",");
        List<String> sections = Arrays.asList(split);

        if (sections.isEmpty()) {
          System.err.println("\t\tparseToken : part " + part + " is badly formed ");
        }
        else {
          if (debug) System.out.println("\t\tparseToken : add " + type + " : " +sections);

          add(type, sections);
        }
        if (debug) System.out.println("\tparseToken : part " + part + " : " + type + "->" +section);
      }
      else if (part.length() > 0) {
        System.err.println("parseToken skipping part '" + part+ "'");
      }
    }

    if (token.contains("item")) {
      int item1 = token.indexOf("item=");
      String itemValue = token.substring(item1+"item=".length());
      if (debug) System.out.println("parseToken : got item = '" + itemValue +"'");
      setItem(itemValue);
    }

    if (debug) System.out.println("parseToken : got " + this + " from token '" +token + "'");
  }

  private void add(String type, Collection<String> section) {
    List<String> copy = new ArrayList<String>();
    for (String s : section) copy.add(s.trim());
    getTypeToSection().put(type, copy);
  }

  public String getItem() {
    return item;
  }

  private void setItem(String item) {
    this.item = item;
  }

  public Map<String, Collection<String>> getTypeToSection() {
    return typeToSection;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Collection<String> section : getTypeToSection().values()) {
      builder.append(section).append(", ");
    }
    String s = builder.toString();
    return s.substring(0, Math.max(0,s.length() - 2));
  }
}
