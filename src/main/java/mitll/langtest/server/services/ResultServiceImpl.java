/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.services;

import mitll.langtest.client.services.ResultService;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.ResultAndTotal;
import mitll.langtest.shared.result.MonitorResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("serial")
public class ResultServiceImpl extends MyRemoteServiceServlet implements ResultService {
  private static final Logger logger = LogManager.getLogger(ResultServiceImpl.class);

  private static final int MAX = 30;

  /**
   * NOTE NOTE NOTE - we skip doing ensure ogg/mp3 on files for now - since this service will likely not be
   * on the server that has the audio.  And ideally this will already have been done.
   * <p>
   * TODO : consider doing offset/limit on database query.
   * TODO : super expensive on long lists
   * <p>
   * Sometimes we type faster than we can respond, so we can throw away stale requests.
   * <p>
   * Filter results by search criteria -- unit->value map (e.g. chapter=5), userid, and foreign language text
   *
   * @param sortInfo - encoding which fields we want to sort, and ASC/DESC choice
   * @param req      - to echo back -- so that if we get an old request we can discard it
   * @return
   * @see mitll.langtest.client.result.ResultManager#createProvider
   */
  @Override
  public ResultAndTotal getResults(int start,
                                   int end,
                                   String sortInfo,
                                   Map<String, String> unitToValue,
                                   String flText,
                                   int req) {
    int userIDFromSession = getUserIDFromSession();
    List<MonitorResult> results = getResults(unitToValue, userIDFromSession, flText);
    if (!results.isEmpty()) {
      Comparator<MonitorResult> comparator = results.get(0).getComparator(Arrays.asList(sortInfo.split(",")));
      try {
        Collections.sort(results, comparator);
      } catch (Exception e) {
        logger.error("Doing " + sortInfo + " " + unitToValue + " " + userIDFromSession + " " + flText + " " + start + "-" + end +
            " Got " + e, e);
      }
    }
    int n = results.size();
    int min = Math.min(end, n);
    if (start > min) {
      logger.debug("original req from " + start + " to " + end);
      start = 0;
    }
    List<MonitorResult> resultList = results.subList(start, min);
    logger.info("getResults ensure compressed audio for " + resultList.size() + " items.");
//    int projectID = getProjectID();

// TODO : not doing this - ideally not needed

/*
    for (MonitorResult result : resultList) {
      ensureCompressedAudio(result.getUserid(), db.getCustomOrPredefExercise(projectID, result.getExID()), result.getAnswer());
    }
*/

    return new ResultAndTotal(new ArrayList<>(resultList), n, req);
  }

  @Override
  public int getNumResults() {
    return db.getResultDAO().getNumResults(getProjectID());
  }

  /**
   * TODO : don't fetch everything from the database if you don't have to.
   * Use offset and limit to restrict?
   *
   * @param unitToValue
   * @param userid
   * @param flText
   * @return
   * @see #getResults
   */
  private List<MonitorResult> getResults(Map<String, String> unitToValue, int userid, String flText) {
    logger.debug("getResults : request unit to value " + unitToValue + " user " + userid + " text '" + flText + "'");
    int projectID = getProjectID();

    if (isNumber(flText)) {
      int i = Integer.parseInt(flText);
      List<MonitorResult> monitorResultsByID =
          db.getMonitorResultsWithText(db.getResultDAO().getMonitorResultsByID(i), projectID);
      logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText +
          " returning " + monitorResultsByID.size() + " results...");
      return monitorResultsByID;
    }


    Collection<MonitorResult> results = getMonitorResults();

    Trie<MonitorResult> trie;

    // filter on unit->value
    if (!unitToValue.isEmpty()) {
      for (String type : db.getTypeOrder(projectID)) {
        if (unitToValue.containsKey(type)) {

          // logger.debug("getResults making trie for " + type);
          // make trie from results
          trie = new Trie<>();

          trie.startMakingNodes();
          for (MonitorResult result : results) {
            String s = result.getUnitToValue().get(type);
            if (s != null) {
              trie.addEntryToTrie(new ResultWrapper(s, result));
            }
          }
          trie.endMakingNodes();

          results = trie.getMatchesLC(unitToValue.get(type));
        }
      }
    }

    boolean filterByUser = userid > -1;
    if (filterByUser) { // asking for userid
      // make trie from results
      results = filterByUser(userid, results);
    }

    // must be asking for text
    boolean filterByText = flText != null && !flText.isEmpty();
    if (filterByText) { // asking for text
      trie = new Trie<>();
      trie.startMakingNodes();
      logger.debug("filter text searching over " + results.size() + " for " + filterByText);
      for (MonitorResult result : results) {
        String foreignText = result.getForeignText();
        if (foreignText != null) {
          trie.addEntryToTrie(new ResultWrapper(foreignText.trim(), result));
        }
      }
      trie.endMakingNodes();

      results = trie.getMatchesLC(flText);
    }
    logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText + " returning " + results.size() + " results...");
    return new ArrayList<>(results);
  }

  private Collection<MonitorResult> filterByUser(int userid, Collection<MonitorResult> results) {
    Trie<MonitorResult> trie;
    logger.debug("making trie for userid " + userid);

    trie = new Trie<>();
    trie.startMakingNodes();
    for (MonitorResult result : results) {
      trie.addEntryToTrie(new ResultWrapper(Long.toString(result.getUserid()), result));
    }
    trie.endMakingNodes();

    results = trie.getMatchesLC(Long.toString(userid));
    return results;
  }

  private boolean isNumber(String flText) {
    if (flText.isEmpty()) {
      return false;
    } else {
      boolean isNumber = false;
      try {
        int i = Integer.parseInt(flText);
        isNumber = true;
      } catch (NumberFormatException e) {
      }
      return isNumber;
    }
  }

  private Collection<MonitorResult> getMonitorResults() {
    return db.getMonitorResults(getProjectID());
  }

  /**
   * Respond to type ahead.
   *
   * @param unitToValue
   * @param flText
   * @param which
   * @return
   * @see mitll.langtest.client.result.ResultTypeAhead#getTypeaheadUsing
   */
  @Override
  public Collection<String> getResultAlternatives(Map<String, String> unitToValue,
                                                  String flText,
                                                  String which) {
    Collection<MonitorResult> results = getMonitorResults();
    int userid = getUserIDFromSession();

    logger.debug("getResultAlternatives request " + unitToValue + " userid=" + userid + " fl '" + flText + "' :'" + which + "'");

    Collection<String> matches = new TreeSet<>();
    Trie<MonitorResult> trie;

    for (String type : db.getTypeOrder(getProjectID())) {
      if (unitToValue.containsKey(type)) {
        logger.debug("getResultAlternatives making trie for " + type);
        // make trie from results
        trie = new Trie<>();

        trie.startMakingNodes();
        for (MonitorResult result : results) {
          String s = result.getUnitToValue().get(type);
          if (s != null) {
            trie.addEntryToTrie(new ResultWrapper(s, result));
          }
        }
        trie.endMakingNodes();

        String valueForType = unitToValue.get(type);
        Collection<MonitorResult> matchesLC = trie.getMatchesLC(valueForType);

        // stop!
        if (which.equals(type)) {
          //        logger.debug("\tmatch for " + type);

          boolean allInt = true;
          for (MonitorResult result : matchesLC) {
            String e = result.getUnitToValue().get(type);
            if (allInt) {
              try {
                Integer.parseInt(e);
              } catch (NumberFormatException e1) {
                allInt = false;
              }
            }
            matches.add(e);
          }

          return allInt ? getIntSorted(matches) : matches;
        } else {
          results = matchesLC;
        }
      }
    }

    if (userid > -1) { // asking for userid
      // make trie from results

      logger.debug("making trie for userid " + userid);

      trie = new Trie<>();
      trie.startMakingNodes();
      for (MonitorResult result : results) {
        trie.addEntryToTrie(new ResultWrapper(Long.toString(result.getUserid()), result));
      }
      trie.endMakingNodes();

      Set<Integer> imatches = new TreeSet<>();
      Collection<MonitorResult> matchesLC = trie.getMatchesLC(Long.toString(userid));

      // stop!
      if (which.equals(MonitorResult.USERID)) {
        for (MonitorResult result : matchesLC) {
          imatches.add(result.getUserid());
        }
        //logger.debug("returning " + imatches);

        for (Integer m : imatches) matches.add(Long.toString(m));
        matches = getLimitedSizeList(matches);
        return matches;
      } else {
        results = matchesLC;
      }
    }

    // must be asking for text
    if (!flText.isEmpty()) {
      trie = new Trie<MonitorResult>();
      trie.startMakingNodes();
      logger.debug("text searching over " + results.size());
      for (MonitorResult result : results) {
        trie.addEntryToTrie(new ResultWrapper(result.getForeignText(), result));
        trie.addEntryToTrie(new ResultWrapper("" + result.getExID(), result));
      }
      trie.endMakingNodes();

      results = trie.getMatchesLC(flText);
      logger.debug("matchesLC for '" + flText + "' " + results);
    }

    boolean isNumber = isNumber(flText);

    if (isNumber) {
      for (MonitorResult result : results) {
        matches.add("" + result.getExID());
      }
    } else {
      for (MonitorResult result : results) {
        matches.add(result.getForeignText().trim());
      }
    }
    logger.debug("returning text " + matches);

    return getLimitedSizeList(matches);
  }

//  private boolean isNumber(String flText) {
//    boolean isNumber = false;
//    try {
//      Integer.parseInt(flText);
//      isNumber = true;
//    } catch (NumberFormatException e) {
//    }
//    return isNumber;
//  }

  @NotNull
  private Collection<String> getIntSorted(Collection<String> matches) {
    List<String> sorted = new ArrayList<String>(matches);
    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return compareTwoMaybeInts(o1, o2);
      }
    });
    return sorted;
  }

  private Collection<String> getLimitedSizeList(Collection<String> matches) {
    if (matches.size() > MAX) {
      List<String> matches2 = new ArrayList<String>();
      int nn = 0;
      for (String match : matches) {
        if (nn++ < MAX) {
          matches2.add(match);
        }
      }
      matches = matches2;
    }
    return matches;
  }

  private int compareTwoMaybeInts(String id1, String id2) {
    int comp;
    try {   // this could be slow
      int i = Integer.parseInt(id1);
      int j = Integer.parseInt(id2);
      comp = i - j;
    } catch (NumberFormatException e) {
      comp = id1.compareTo(id2);
    }
    return comp;
  }

  private static class ResultWrapper implements TextEntityValue<MonitorResult> {
    private final String value;
    private final MonitorResult e;

    ResultWrapper(String value, MonitorResult e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public MonitorResult getValue() {
      return e;
    }

    @Override
    public String getNormalizedValue() {
      return value;
    }

    public String toString() {
      return "result " + e.getExID() + " : " + value;
    }
  }
}