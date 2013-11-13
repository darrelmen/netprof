package mitll.langtest.server.trie;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 11/13/13
 * Time: 1:17 AM
 * To change this template use File | Settings | File Templates.
 */


import java.io.Serializable;
import java.util.*;

/**
 * A tree-like data structure that enables quick lookup due to sharing common prefixes (using Aho Corasick algorithm).
 *
 */
public class Trie /*implements Serializable*/ {

  private static final boolean USE_SINGLE_TOKEN_MAP = false;
  private static final boolean SPLIT_ON_CHARACTERS = false;
  private TrieNode root;
  //private Pattern pattern; // put back if for some reason we have non-space whitespace separators in database (?)
  private Map<String,TextEntityValue> singleTokenMap;
  private Map<String,String> tempCache;
  private boolean convertToUpper = true;

  public Trie() {
    this(true);
  }

  public Trie(boolean convertToUpper) {
    this.convertToUpper = convertToUpper;
    this.root = new TrieNode();
    //this.pattern = Pattern.compile("\\s+");
    this.singleTokenMap = new HashMap<String,TextEntityValue>();
  }

  /**
   * Just for testing.
   * @param entryList
   */
  public Trie(List<TextEntityValue> entryList) {
    this();
    build(entryList);
  }

  public TrieNode getRoot() {
    return root;
  }

  /**
   * Builds trie from scratch. This method adds each entry and then computes failure function.
   *
   * @param entryList
   */
  public void build(List<TextEntityValue> entryList) {
    makeNodes(entryList);
    computeFailureFunction();
  }

  public void build2(List<String> entryList) {
    makeNodes2(entryList);
    computeFailureFunction();
  }

  /**
   * Check in HashMap for single tokens (less memory than putting everything in the trie).
   * @param transitionLabel
   * @return entity found with this label
   */
  public TextEntityValue getSingleTokenValue(String transitionLabel) {
    return singleTokenMap.get(transitionLabel);
  }

  /**
   * Creates a temp string cache so we can avoid making multiple copies of identical strings.
   * Depending on the dictionary this can be a memory savings of 10% - 15%.
   *
   * @param entryList to store in trie
   */
  private void makeNodes(List<TextEntityValue> entryList) {
    startMakingNodes();
    for (TextEntityValue entry : entryList) {
      addEntryToTrie(entry, tempCache);
    }
    endMakingNodes();
  }

  private void makeNodes2(List<String> entryList) {
    startMakingNodes();
    for (String entry : entryList) {
      addEntryToTrie(entry);
    }
    endMakingNodes();
  }

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

  public boolean addEntryToTrie(TextEntityValue textEntityDescription) {
    return addEntryToTrie(textEntityDescription, tempCache);
  }

  public boolean addEntryToTrie(final String entry) {
    return addEntryToTrie(new MyTextEntityValue(entry), tempCache);
  }
 /* public boolean addEntryToTrie2(final String entry) {
    StringBuilder builder = new StringBuilder();
    for (int i =0; i < entry.length(); i++) {
      builder.append(entry.charAt(i));
      return addEntryToTrie(new MyTextEntityValue(builder.toString()), tempCache);

    }
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
  private boolean addEntryToTrie(TextEntityValue textEntityDescription, Map<String,String> stringCache) {
    String normalizedValue = textEntityDescription.getNormalizedValue();
    List<String> split = SPLIT_ON_CHARACTERS ? getChars(normalizedValue) : getSpaceSeparatedTokens(normalizedValue);

    int n = split.size();
    if (n == 1 && USE_SINGLE_TOKEN_MAP) {
      String upperCase = convertToUpper ? normalizedValue.toUpperCase() : normalizedValue;
      singleTokenMap.put(upperCase, textEntityDescription);
      return false;
    }

    TrieNode currentState = root;
    for (int i = 0; i < n; i++) {
      String upperCaseToken = convertToUpper ? split.get(i).toUpperCase() : split.get(i);

      // avoid keeping references to duplicate strings
      String uniqueCopy = getUnique(stringCache, upperCaseToken);

      TrieNode nextState = currentState.getNextState(uniqueCopy);

      if (nextState == null) {
        nextState = new TrieNode();
        currentState.addTransition(uniqueCopy, nextState);
      }
      currentState = nextState;
    }

    currentState.getAndCreateEmitList().add(new EmitValue(textEntityDescription, n));
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

 /*   private List<String> getSpaceSeparatedTokensByPattern(String normalizedValue) {
       String[] tokenArray = pattern.split(normalizedValue);
       return Arrays.asList(tokenArray);
    }*/

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
    Queue<TrieNode> queue = new LinkedList<TrieNode>();
    for (TrieNode child : root.getTransitionValues()) {
      queue.add(child);
      child.setFailureNode(root);
    }
    while (!queue.isEmpty()) {
      TrieNode r = queue.remove();
      for (String a : r.getTransitionLabels()) {
        // a is transition label
        // r is current node
        TrieNode s = r.getKnownNextState(a);
        // s is the successor node/state when you follow along edge "a"
        queue.add(s);
        TrieNode state = r.getFailureNode();

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

  private static class MyTextEntityValue implements TextEntityValue {

    private final String entry;
    public MyTextEntityValue(String entry) {
      this.entry = entry;
    }

    @Override
    public String getNormalizedValue() {
      return entry;
    }
    public String toString() { return entry; }
  }

  public static void main(String [] arg) {
    Trie trie = new Trie(false);

    String [] str =  {"he went","he goes","he goes home","he goes to work","he returns","hello!","hi","hi there"};
    List<String> entryList =
        Arrays.asList(str);
    trie.build2(entryList);
    TrieNode root1 = trie.getRoot();
    System.out.println("got 1 " + root1);

    TrieNode he = root1.getNextState("he");
    System.out.println("got 2 " + he + " emits " + (he != null ? he.getEmitList() : "null"));
    TrieNode e = he != null ?he.getNextState("goes") : null;
    System.out.println("got 3 " + e + " emits " + (e != null ? e.getEmitList() : "null"));

    TrieNode ee = e != null ?e.getNextState("home") : null;
    System.out.println("got 4 " + ee + " emits " + (ee != null ? ee.getEmitList() : "null"));
 //   System.out.println("got 4 " + e.getNextState("l"));
  //  System.out.println("got " + trie.getSingleTokenValue("hel"));
  }
}
