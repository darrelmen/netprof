package mitll.langtest.client.list;

import java.util.*;
import java.util.logging.Logger;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 4/18/13
* Time: 6:31 PM
* To change this template use File | Settings | File Templates.
*/
public class SelectionState {
  private Logger logger = Logger.getLogger("SelectionState");

  public static final String INSTANCE = "instance";
  private String item;
  private final Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
  private String instance = "";
  private String search = "";
  private final boolean debug = false;

  /**
   * @see ExerciseList#getIDFromToken(String)
   * @see HistoryExerciseList#getSelectionState(String)
   * @param token
   * @param removePlus
   */
  public SelectionState(String token, boolean removePlus) {
    String token1 = removePlus ? unencodeToken(token) : unencodeToken2(token);
    parseToken(token1);
  }

  /**
   * @seex mitll.langtest.client.flashcard.BootstrapFlashcardExerciseList#getExercises(long, boolean)
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
   // token = token.split(ResponseExerciseList.RESPONSE_TYPE_DIVIDER)[0]; // remove any other parameters

    String[] parts = token.split(";");

    for (String part : parts) {
      if (debug) logger.info("parseToken : part " + part + " : " + Arrays.asList(parts));

      if (part.contains("=")) {
        String[] segments = part.split("=");
        if (debug) logger.info("\tpart " + part + " : " + Arrays.asList(segments));
        if (segments.length >1) {
          String type    = segments[0].trim();
          String section = segments[1].trim();

          if (type.equals("#item") || type.equals("item")) {
            setItem(section);
          } else if (type.equals("#search") || type.equals("search")) {
            search = section;
          } else {
            String[] split = section.split(",");
            List<String> sections = Arrays.asList(split);

            if (sections.isEmpty()) {
              logger.warning("\t\tparseToken : part " + part + " is badly formed ");
            } else {
              if (debug) logger.info("\t\tparseToken : add " + type + " : " + sections);
              if (type.equals(INSTANCE)) instance = section;
              else add(type, sections);
            }
            if (debug) logger.info("\tparseToken : part " + part + " : " + type + "->" + section);
          }
        }
      } else if (part.length() > 0) {
        logger.warning("parseToken skipping part '" + part + "'");
      }
    }

  /*  if (token.contains("item")) {
      int item1 = token.indexOf("item=");
      String itemValue = token.substring(item1+"item=".length());
      itemValue = itemValue.split(";")[0];
      if (debug) logger.info("parseToken : got item = '" + itemValue +"'");
      setItem(itemValue);
    }*/

    if (debug) logger.info("parseToken : got " + this + " from token '" + token + "'");
  //  logger.info(getInfo());
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
  public String getSearch() {
    return search;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Collection<String> section : getTypeToSection().values()) {
      builder.append(section).append(", ");
    }
    String s = builder.toString();
    return s.substring(0, Math.max(0,s.length() - 2));
  }

  public String getInfo() {
    return "parseToken : instance " + instance + " : search " + search + " : item " + item + " : unit->chapter " + getTypeToSection();
  }
}