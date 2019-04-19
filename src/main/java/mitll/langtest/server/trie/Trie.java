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

package mitll.langtest.server.trie;

import mitll.langtest.server.scoring.SmallVocabDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * A tree-like data structure that enables quick lookup due to sharing common prefixes (using Aho Corasick algorithm).
 */
public class Trie<T> {
  private static final Logger logger = LogManager.getLogger(Trie.class);

  //  private static final boolean SPLIT_ON_CHARACTERS = true;
  // private static final boolean SPACES_ARE_OK = true;
  private static final int WINDOW_SIZE = 20;
  private static final int MAX_WINDOW = WINDOW_SIZE;// * 2;
  private static final int MIN_WINDOW = WINDOW_SIZE / 2;
  private static final int MAX_LEN = 1000;

  private final TrieNode<T> root;
  private Map<String, String> tempCache = null;
  private boolean convertToUpper = true;

  private static final boolean DEBUG = false;

  public Trie() {
    this(false);
  }

  private Trie(boolean convertToUpper) {
    this.convertToUpper = convertToUpper;
    this.root = new TrieNode<>();
  }

  /**
   * Start building
   *
   * @see ExerciseTrie#ExerciseTrie(Collection, String, SmallVocabDecoder, boolean, boolean)
   */
  public void startMakingNodes() {
    this.tempCache = new HashMap<>();
  }

  /**
   * Stop building - calls {@link #computeFailureFunction}
   */
  public void endMakingNodes() {
    tempCache = null;
    computeFailureFunction();
  }

  public void addEntryToTrie(TextEntityValue<T> textEntityDescription) {
    addEntryToTrie(textEntityDescription, tempCache);
  }

  /**
   * @param toMatch
   * @return
   * @see #getMatches(String)
   */
  private List<EmitValue<T>> getEmits(String toMatch) {
    TrieNode<T> start = root;

    for (String c : getChars(toMatch)) {
//      logger.info("char " + toMatch+ " = '" +c +"'");
      //TrieNode<T> nextState = start.getNextState(c);
      //    logger.info(" explain "+ start.explain(c));
      start = start.getNextState(c);
      if (start == null) break;
    }

    return (start == null) ? Collections.emptyList() : start.getEmitsBelow();
  }

  /**
   * addEntryToTrie is a method to implement algorithm 2 in the AC paper
   * <p/>
   * NOTE : All variant forms (normalized_value) of entries in the dictionary in database are whitespace separated
   * with possessive apostrophe as only punctuation.
   * <p/>
   * Example: JASON 'S HOUSE OF PANCAKES
   *
   * @param textEntityDescription
   * @param stringCache           - don't keep around multiple copies of identical strings (10-15% memory savings)
   * @return true if entry went into trie, false if into singleTokenMap
   */
  private boolean addEntryToTrie(TextEntityValue<T> textEntityDescription, Map<String, String> stringCache) {
    String normalizedValue = textEntityDescription.getNormalizedValue();
//    logger.info("addEntryToTrie adding '" + normalizedValue + "'");
    //List<String> split = SPLIT_ON_CHARACTERS ? getChars(normalizedValue) : getSpaceSeparatedTokens(normalizedValue);
    List<String> split = getChars(normalizedValue);

    int n = split.size();
/*    if (n == 1 && USE_SINGLE_TOKEN_MAP) {
      String upperCase = convertToUpper ? normalizedValue.toUpperCase() : normalizedValue;
      singleTokenMap.put(upperCase, textEntityDescription);
      return false;
    }*/

    TrieNode<T> currentState = root;
    for (String aSplit : split) {
      String upperCaseToken = convertToUpper ? aSplit.toUpperCase() : aSplit;
      //   logger.info("addEntryToTrie upperCaseToken '" + upperCaseToken + "'");

      // avoid keeping references to duplicate strings
      String uniqueCopy = getUnique(stringCache, upperCaseToken);
      //   logger.info("addEntryToTrie uniqueCopy '" + uniqueCopy + "'");

      TrieNode<T> nextState = currentState.getNextState(uniqueCopy);

      if (nextState == null) {
        nextState = new TrieNode<>();
        currentState.addTransition(uniqueCopy, nextState);
      }
      currentState = nextState;
    }

    currentState.getAndCreateEmitList().add(new EmitValue<>(textEntityDescription, n));
    return true;
  }

  private String getUnique(Map<String, String> stringCache, String upperCaseToken) {
    String uniqueCopy = stringCache.get(upperCaseToken);
    if (uniqueCopy == null) {
      stringCache.put(upperCaseToken, upperCaseToken);
      uniqueCopy = upperCaseToken;
    }
    return uniqueCopy;
  }

  /**
   * This is nutty - need sliding window.
   *
   * @param entry
   * @return
   * @see #addEntryToTrie(TextEntityValue, Map)
   * @see #getEmits
   */
  private List<String> getChars(String entry) {
    List<String> toAdd = new ArrayList<>();

    int length = entry.length();
    if (length > MAX_LEN) {
      int orig = entry.length();
      entry = entry.substring(0, MAX_LEN);
      logger.warn("getChars adding entry " + entry + " length " + orig);
    }
    if (DEBUG && length > WINDOW_SIZE) logger.info("getChars : " + entry);

    Deque<Character> slidingWindow = new LinkedList<>();
    int n = Math.min(length, entry.length());

    for (int i = 0; i < n; i++) {
      char c = entry.charAt(i);

      slidingWindow.addLast(c);
      int size = slidingWindow.size();
      boolean moreThanMax = size > MAX_WINDOW;
      if (moreThanMax || (size > WINDOW_SIZE && Character.isWhitespace(c))) {

        if (moreThanMax) {
          if (DEBUG) logger.warn("getChars max vs " + length + " : '" + getWindow(slidingWindow) + "'");
        } else if (DEBUG && (size > WINDOW_SIZE && Character.isWhitespace(c)))
          logger.warn("getChars break '" + getWindow(slidingWindow) + "'");

        Character first = slidingWindow.peekFirst();
        while (
            slidingWindow.size() > MAX_WINDOW ||
                (slidingWindow.size() > MIN_WINDOW && !Character.isWhitespace(first))) {
          first = slidingWindow.removeFirst();
        }
      }
      String window = getWindow(slidingWindow);

      //
//      if (DEBUG && length > WINDOW_SIZE) {
//        logger.info("adding : " + window);
//      }

      toAdd.add(window);
      // }
    }
    return toAdd;
  }

  private String getWindow(Deque<?> window) {
    StringBuilder sb = new StringBuilder();
    window.forEach(sb::append);
    return sb.toString();
  }

  /**
   * Faster to use this than a pattern by approximately 12%.
   *
   * @return list of tokens
   * @paramx normalizedValue
   */
/*  private List<String> getSpaceSeparatedTokens(String normalizedValue) {
    int findex = 0;
    int nindex = 0;
    List<String> split = new ArrayList<>();
    while ((nindex = normalizedValue.indexOf(' ', findex)) != -1) {
      if (nindex > findex) {
        String token = normalizedValue.substring(findex, nindex);
        split.add(token);
      }
      findex = nindex + 1;
    }
    nindex = normalizedValue.length();

    if (nindex > findex) {
      split.add(normalizedValue.substring(findex, nindex));
    }
    return split;
  }*/
  private void computeFailureFunction() {
    // computeFailureFunction is a method to implement algorithm 3 here from
    // AC paper after the tree is built
    Queue<TrieNode<T>> queue = new LinkedList<>();
    for (TrieNode<T> child : root.getTransitionValues()) {
      queue.add(child);
      child.setFailureNode(root);
    }
    while (!queue.isEmpty()) {
      TrieNode<T> r = queue.remove();
      Collection<String> transitionLabels = r.getTransitionLabels();
      // logger.info("computeFailureFunction transition "+ transitionLabels.size());
      for (String a : transitionLabels) {
        // a is transition label
        // r is current node
        TrieNode<T> s = r.getKnownNextState(a);
        // s is the successor node/state when you follow along edge "a"
        queue.add(s);
        TrieNode<T> state = r.getFailureNode();

        while (!state.hasTransitionLabel(a)) {
          if (state.getFailureNode() == null) {
            // this is checking if state = root node
            break;
          }
          state = state.getFailureNode();
        }

        s.setFailureNode(state.getKnownNextState(a));

        if (s.getFailureNode() == null) {
          // state is actually root and there's no valid transition
          // label "a"
          s.setFailureNode(state);
        }

        if (s.getFailureNode().getEmitList() != null) {
          s.getAndCreateEmitList().addAll(s.getFailureNode().getEmitList());
        }
      }
    }
  }

  /**
   * @param toMatch
   * @return
   * @see mitll.langtest.server.services.ResultServiceImpl#getResultAlternatives
   * @see mitll.langtest.server.services.ResultServiceImpl#getResults
   */
  public Collection<T> getMatchesLC(String toMatch) {
    return getMatches(toMatch.toLowerCase());
  }

  /**
   * @param lc
   * @return
   * @see ExerciseTrie#getExercises(String)
   * @see Trie#getMatchesLC(String)
   */
  List<T> getMatches(String lc) {
    List<EmitValue<T>> emits = getEmits(lc);
    Set<T> unique = new HashSet<>();
    List<T> ids = new ArrayList<>();
    for (EmitValue<T> ev : emits) {
      T exercise = ev.getValue();
      if (!unique.contains(exercise)) {
        ids.add(exercise);
        unique.add(exercise);
      }
    }

    //  logger.debug("getExercises : for '" +lc + "' (" +lc+ ") got " + ids.size() + " matches");
    return ids;
  }

  public boolean isEmpty() {
    return tempCache == null;
  }
}
