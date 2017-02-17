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

package mitll.langtest.server.trie;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/13/13
 * Time: 1:17 AM
 * To change this template use File | Settings | File Templates.
 */

import mitll.langtest.server.scoring.SmallVocabDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * A tree-like data structure that enables quick lookup due to sharing common prefixes (using Aho Corasick algorithm).
 */
public class Trie<T> {
  private static final Logger logger = LogManager.getLogger(Trie.class);

  private static final boolean SPLIT_ON_CHARACTERS = true;
  private final TrieNode<T> root;
  private Map<String, String> tempCache;
  private boolean convertToUpper = true;

  public Trie() {
    this(false);
  }

  private Trie(boolean convertToUpper) {
    this.convertToUpper = convertToUpper;
    this.root = new TrieNode<>();
  }

  /**
   * @param toMatch
   * @return
   * @see #getMatches(String)
   */
  private List<EmitValue<T>> getEmits(String toMatch) {
    TrieNode<T> start = root;
    for (String c : getChars(toMatch)) {
      start = start.getNextState(c);
      if (start == null) break;
    }
    if (start == null) {
      return Collections.emptyList();
    } else {
      return start.getEmitsBelow();
    }
  }

  /**
   * Start building
   * @see ExerciseTrie#ExerciseTrie(Collection, String, SmallVocabDecoder)
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

  public boolean addEntryToTrie(TextEntityValue<T> textEntityDescription) {
    return addEntryToTrie(textEntityDescription, tempCache);
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
    List<String> split = SPLIT_ON_CHARACTERS ? getChars(normalizedValue) : getSpaceSeparatedTokens(normalizedValue);

    int n = split.size();
/*    if (n == 1 && USE_SINGLE_TOKEN_MAP) {
      String upperCase = convertToUpper ? normalizedValue.toUpperCase() : normalizedValue;
      singleTokenMap.put(upperCase, textEntityDescription);
      return false;
    }*/

    TrieNode<T> currentState = root;
    for (String aSplit : split) {
      String upperCaseToken = convertToUpper ? aSplit.toUpperCase() : aSplit;

      // avoid keeping references to duplicate strings
      String uniqueCopy = getUnique(stringCache, upperCaseToken);

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

  private List<String> getChars(String entry) {
    List<String> toAdd = new ArrayList<>();
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < entry.length(); i++) {
      builder.append(entry.charAt(i));
      toAdd.add(builder.toString());
    }
    return toAdd;
  }

  /**
   * Faster to use this than a pattern by approximately 12%.
   *
   * @param normalizedValue
   * @return list of tokens
   */
  private List<String> getSpaceSeparatedTokens(String normalizedValue) {
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
  }

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
      for (String a : r.getTransitionLabels()) {
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultAlternatives(Map, long, String, String)
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults(Map, long, String)
   * @param toMatch
   * @return
   */
  public Collection<T> getMatchesLC(String toMatch) { return getMatches(toMatch.toLowerCase());  }

  /**
   *
   * @param lc
   * @return
   * @see ExerciseTrie#getExercises(String, SmallVocabDecoder)
   * @see Trie#getMatchesLC(String)
   */
  public List<T> getMatches(String lc) {
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
}
