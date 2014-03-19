package mitll.langtest.client.list;

import com.google.gwt.user.client.History;
import mitll.langtest.client.bootstrap.ResponseExerciseList;

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
  private final Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
  private String instance = "";
  private final boolean debug = false;

  /**
   * Populated from history token!
   * @see mitll.langtest.client.flashcard.BootstrapFlashcardExerciseList#getExercises(long, boolean)
   */
  public SelectionState( boolean removePlus) {
    this(History.getToken(), removePlus);
  }

  /**
   * @see ExerciseList#getIDFromToken(String)
   * @see HistoryExerciseList#getSelectionState(String)
   * @param token
   * @param removePlus
   */
  public SelectionState(String token, boolean removePlus) {
    String token1 = removePlus ? unencodeToken(token) : unencodeToken2(token);
    //System.out.println("SelectionState : selection state token " + token1);
    parseToken(token1);
  }

  /**
   * @see mitll.langtest.client.flashcard.BootstrapFlashcardExerciseList#getExercises(long, boolean)
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


  /**
   * Deals with responseType being on the URL.
   * @param token
   */
  private void parseToken(String token) {
    //token = token.contains("###") ? token.split("###")[0] : token;
    token = token.split(ResponseExerciseList.RESPONSE_TYPE_DIVIDER)[0]; // remove any other parameters

    String[] parts = token.split(";");

    for (String part : parts) {
      if (debug) System.out.println("parseToken : part " + part + " : " + Arrays.asList(parts));

      if (part.contains("=")) {
        String[] segments = part.split("=");
        if (debug) System.out.println("\tpart " + part + " : " + Arrays.asList(segments));

        String type = segments[0].trim();
        String section = segments[1].trim();

        if (type.equals("#item") || type.equals("item")) {
          setItem(section);
        } else {
          String[] split = section.split(",");
          List<String> sections = Arrays.asList(split);

          if (sections.isEmpty()) {
            System.err.println("\t\tparseToken : part " + part + " is badly formed ");
          } else {
            if (debug) System.out.println("\t\tparseToken : add " + type + " : " + sections);
            if (type.equals("instance")) instance = section;
            else add(type, sections);
          }
          if (debug) System.out.println("\tparseToken : part " + part + " : " + type + "->" + section);
        }
      } else if (part.length() > 0) {
        System.err.println("parseToken skipping part '" + part + "'");
      }
    }

  /*  if (token.contains("item")) {
      int item1 = token.indexOf("item=");
      String itemValue = token.substring(item1+"item=".length());
      itemValue = itemValue.split(";")[0];
      if (debug) System.out.println("parseToken : got item = '" + itemValue +"'");
      setItem(itemValue);
    }*/

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

  public String getInstance() {
    return instance;
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
