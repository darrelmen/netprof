package mitll.langtest.client.exercise;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;

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
  public String item;
  public Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#showSelectionState(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param event
   */
  public SelectionState(ValueChangeEvent<String> event) {
    parseToken(getTokenFromEvent(event));
  }

  /**
   * Populated from history token!
   * @see mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList#getExercises(long)
   */
  public SelectionState() {
    this(History.getToken());
  }

  /**
   * @see SectionExerciseList#getSelectionState(String)
   * @param token
   */
  public SelectionState(String token) {
    parseToken(unencodeToken(token));
  }

  /**
   * @see mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList#getExercises(long)
   */
  public boolean isEmpty() { return typeToSection.isEmpty(); }

  private String getTokenFromEvent(ValueChangeEvent<String> event) {
    String token = event.getValue();
    token = unencodeToken(token);
    return token;
  }


  private String unencodeToken(String token) {
    token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ").replaceAll("\\+", " ");
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
    typeToSection.put(type, section);
  }

  private void setItem(String item) {
    this.item = item;
  }

 // public Map<String,Collection<String>> getSelection() { return typeToSection; }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Collection<String> section : typeToSection.values()) {
      builder.append(section).append(", ");
    }
    String s = builder.toString();
    return s.substring(0, Math.max(0,s.length() - 2));
  }
}
