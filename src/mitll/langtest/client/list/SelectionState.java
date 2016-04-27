/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

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
  private final Logger logger = Logger.getLogger("SelectionState");

  static final String INSTANCE = "instance";
  private String item = "";
  private final Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
  private String instance = "";
  private String search = "";
  private static final boolean DEBUG = false;

  /**
   * @param token
   * @param removePlus
   * @see HistoryExerciseList#getIDFromToken
   * @see HistoryExerciseList#getSelectionState(String)
   */
  SelectionState(String token, boolean removePlus) {
    parseToken(removePlus ? unencodeToken(token) : unencodeToken2(token));
  }

  private String unencodeToken(String token) {
    return unencodeToken2(token).replaceAll("\\+", " ");
  }

  private String unencodeToken2(String token) {
    return token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ");
  }

  /**
   * @seex mitll.langtest.client.flashcard.BootstrapFlashcardExerciseList#getExercises(long, boolean)
   */
  public boolean isEmpty() {
    return getTypeToSection().isEmpty();
  }


  /**
   * Deals with responseType being on the URL.
   *
   * @param token
   */
  private void parseToken(String token) {
    //token = token.contains("###") ? token.split("###")[0] : token;
    // token = token.split(ResponseExerciseList.RESPONSE_TYPE_DIVIDER)[0]; // remove any other parameters
    String[] parts = token.split(";");

    for (String part : parts) {
      if (DEBUG) logger.info("parseToken : part " + part + " : " + Arrays.asList(parts));

      if (part.contains("=")) {
        String[] segments = part.split("=");
        if (DEBUG) logger.info("\tpart " + part + " : " + Arrays.asList(segments));
        if (segments.length > 1) {
          String type = segments[0].trim();
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
              if (DEBUG) logger.info("\t\tparseToken : add " + type + " : " + sections);
              if (type.equals(INSTANCE)) instance = section;
              else add(type, sections);
            }
            if (DEBUG) logger.info("\tparseToken : part " + part + " : " + type + "->" + section);
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

    if (DEBUG) logger.info("parseToken : got " + this + " from token '" + token + "'");
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
    return s.substring(0, Math.max(0, s.length() - 2));
  }

  public String getInfo() {
    return "parseToken : instance " + instance + " : search " + search + " : item " + item + " : unit->chapter " + getTypeToSection();
  }
}