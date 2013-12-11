package mitll.langtest.server.trie;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 11/13/13
 * Time: 1:17 AM
 * To change this template use File | Settings | File Templates.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * A tree-like data structure that enables quick lookup due to sharing common prefixes (using Aho Corasick algorithm).
 *
 */
public class Trie<T> {
  private static final boolean USE_SINGLE_TOKEN_MAP = false;
  private static final boolean SPLIT_ON_CHARACTERS = true;
  private TrieNode<T> root;
  private Map<String,TextEntityValue<T>> singleTokenMap;
  private Map<String,String> tempCache;
  private boolean convertToUpper = true;

  public Trie() {
    this(false);
  }

  public Trie(boolean convertToUpper) {
    this.convertToUpper = convertToUpper;
    this.root = new TrieNode<T>();
    this.singleTokenMap = new HashMap<String,TextEntityValue<T>>();
  }

  /**
   * Just for testing.
   * @paramx entryList
   */
/*  public Trie(List<TextEntityValue> entryList) {
    this();
    build(entryList);
  }*/

  private TrieNode<T> getRoot() {
    return root;
  }

  /**
   * @see mitll.langtest.server.ExerciseTrie#getExercises(String)
   * @param toMatch
   * @return
   */
  protected List<EmitValue<T>> getEmits(String toMatch) {
    TrieNode<T> start = getRoot();
    for (String c : getChars(toMatch)) {
      start = start.getNextState(c);
      if (start == null) break;
    }
    if (start == null) {
      return Collections.emptyList();
    }
    else {
      return start.getEmitsBelow();
    }
  }

  /**
   * Builds trie from scratch. This method adds each entry and then computes failure function.
   *
   * @param entryList
   */
/*  private void build(List<TextEntityValue> entryList) {
    makeNodes(entryList);
    computeFailureFunction();
  }*/

/*  private void build2(List<String> entryList) {
    makeNodes2(entryList);
    computeFailureFunction();
  }*/

  /**
   * Check in HashMap for single tokens (less memory than putting everything in the trie).
   * @param transitionLabel
   * @return entity found with this label
   */
  public TextEntityValue<T> getSingleTokenValue(String transitionLabel) {
    return singleTokenMap.get(transitionLabel);
  }

  /**
   * Creates a temp string cache so we can avoid making multiple copies of identical strings.
   * Depending on the dictionary this can be a memory savings of 10% - 15%.
   *
   * @param entryList to store in trie
   */
/*  private void makeNodes(List<TextEntityValue> entryList) {
    startMakingNodes();
    for (TextEntityValue entry : entryList) {
      addEntryToTrie(entry, tempCache);
    }
    endMakingNodes();
  }*/

/*  private void makeNodes2(List<String> entryList) {
    startMakingNodes();
    for (String entry : entryList) {
      addEntryToTrie(entry);
    }
    endMakingNodes();
  }*/

  /**
   * Start building
   */
  public void startMakingNodes() {
    this.tempCache = new HashMap<String,String>();
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

/*  public boolean addEntryToTrie(final String entry) {
    return addEntryToTrie(new MyTextEntityValue(entry), tempCache);
  }*/

  /**
   * addEntryToTrie is a method to implement algorithm 2 in the AC paper
   *
   * NOTE : All variant forms (normalized_value) of entries in the dictionary in database are whitespace separated
   * with possessive apostrophe as only punctuation.
   *
   * Example: JASON 'S HOUSE OF PANCAKES
   *
   * @param textEntityDescription
   * @param stringCache - don't keep around multiple copies of identical strings (10-15% memory savings)
   * @return true if entry went into trie, false if into singleTokenMap
   */
  private boolean addEntryToTrie(TextEntityValue<T> textEntityDescription, Map<String,String> stringCache) {
    String normalizedValue = textEntityDescription.getNormalizedValue();
    List<String> split = SPLIT_ON_CHARACTERS ? getChars(normalizedValue) : getSpaceSeparatedTokens(normalizedValue);

    int n = split.size();
    if (n == 1 && USE_SINGLE_TOKEN_MAP) {
      String upperCase = convertToUpper ? normalizedValue.toUpperCase() : normalizedValue;
      singleTokenMap.put(upperCase, textEntityDescription);
      return false;
    }

    TrieNode<T> currentState = root;
    for (String aSplit : split) {
      String upperCaseToken = convertToUpper ? aSplit.toUpperCase() : aSplit;

      // avoid keeping references to duplicate strings
      String uniqueCopy = getUnique(stringCache, upperCaseToken);

      TrieNode<T> nextState = currentState.getNextState(uniqueCopy);

      if (nextState == null) {
        nextState = new TrieNode<T>();
        currentState.addTransition(uniqueCopy, nextState);
      }
      currentState = nextState;
    }

    currentState.getAndCreateEmitList().add(new EmitValue<T>(textEntityDescription, n));
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
    List<String> toAdd = new ArrayList<String>();
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
    List<String> split = new ArrayList<String>();
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
    Queue<TrieNode<T>> queue = new LinkedList<TrieNode<T>>();
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

  private static class MyTextEntityValue implements TextEntityValue<String> {
    private final String entry;
    public MyTextEntityValue(String entry) {
      this.entry = entry;
    }

    @Override
    public String getValue() {
      return null;
    }

    @Override
    public String getNormalizedValue() {
      return entry;
    }
    public String toString() { return entry; }
  }

/*  public static void main(String [] arg) {
    Trie trie = new Trie(false);

    String [] str =  {"he went","he goes","he goes home","he goes to work","he returns","hello!","hi","hi there"};
    List<String> entryList = Arrays.asList(str);
    trie.build2(entryList);
    TrieNode root1 = trie.getRoot();
    System.out.println("got 1 " + root1);

    if (false) {
    TrieNode he = root1.getNextState("he");
    System.out.println("got 2 " + he + " emits " + (he != null ? he.getEmitList() : "null"));
    TrieNode e = he != null ?he.getNextState("goes") : null;
    System.out.println("got 3 " + e + " emits " + (e != null ? e.getEmitList() : "null"));

    TrieNode ee = e != null ?e.getNextState("home") : null;
    System.out.println("got 4 " + ee + " emits " + (ee != null ? ee.getEmitList() : "null"));
    }
    else {
      TrieNode he = root1.getNextState("h");
      System.out.println("got 2 " + he + " emits " + (he != null ? he.getEmitList() : "null"));
      System.out.println("got 2 " + he + " emits " + (he != null ? he.getEmitsBelow() : "null"));
      TrieNode e = he != null ?he.getNextState("he") : null;
      System.out.println("got 3 " + e + " emits " + (e != null ? e.getEmitsBelow() : "null"));

      System.out.println("h = " +trie.getEmits("h"));
      System.out.println("he = " +trie.getEmits("he"));
      System.out.println("hel = " +trie.getEmits("hel"));
      System.out.println("hi = " +trie.getEmits("hi"));
      System.out.println("xx = " +trie.getEmits("xx"));
    }
 //   System.out.println("got 4 " + e.getNextState("l"));
  //  System.out.println("got " + trie.getSingleTokenValue("hel"));
  }*/
}
