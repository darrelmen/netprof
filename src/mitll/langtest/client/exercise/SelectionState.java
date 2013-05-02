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

  public SelectionState(ValueChangeEvent<String> event) {
    parseToken(getTokenFromEvent(event));
  }

  public SelectionState() {
    this(History.getToken());
  }

  public SelectionState(String token) {
    parseToken(unencodeToken(token));
  }

  public boolean isEmpty() { return typeToSection.isEmpty(); }

  protected String getTokenFromEvent(ValueChangeEvent<String> event) {
    String token = event.getValue();
    token = unencodeToken(token);
    return token;
  }


  protected String unencodeToken(String token) {
    token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ").replaceAll("\\+", " ");
    return token;
  }

  private void parseToken(String token) {
    String[] parts = token.split(";");

    for (String part : parts) {
      //System.out.println("getSelectionState : part " + part + " : " + Arrays.asList(parts));

      if (part.contains("=")) {
        String[] segments = part.split("=");
        //System.out.println("\tpart " + part + " : " + Arrays.asList(segments));

        String type = segments[0].trim();
        String section = segments[1].trim();
        String[] split = section.split(",");
        List<String> sections = Arrays.asList(split);

        if (sections.isEmpty()) {
          System.err.println("\tpart " + part + " is badly formed ");
        }
        else {
          add(type, sections);
        }
        //System.out.println("getSelectionState : part " + part + " : " + type + "->" +section + " : " + selectionState);
      }
      else if (part.length() > 0) {
        System.err.println("getSelectionState skipping part '" + part+ "'");
      }
    }

    if (token.contains("item")) {
      int item1 = token.indexOf("item=");
      String itemValue = token.substring(item1+"item=".length());
      //System.out.println("getSelectionState : got item = '" + itemValue +"'");
      setItem(itemValue);
    }

    System.out.println("getSelectionState : got " +
      this +
      " from token '" +token + "'");
  }

  public void add(String type, Collection<String> section) {
    typeToSection.put(type, section);
  }

  public void setItem(String item) {
    this.item = item;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Collection<String> section : typeToSection.values()) {
      builder.append(section).append(", ");
    }
    String s = builder.toString();
    return s.substring(0, Math.max(0,s.length() - 2));
  }
}
