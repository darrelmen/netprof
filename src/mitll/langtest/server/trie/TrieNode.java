package mitll.langtest.server.trie;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 11/13/13
 * Time: 1:19 AM
 * To change this template use File | Settings | File Templates.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A node in a trie.
 *
 * If there is just one transition from this node to the next, doesn't make a gotoMap.
 * If there are no emit values, doesn't make an emit values array.
 *
 * Nodes represent states in a state machine, e.g., (root)--"joe"-->(node1)--"blow"-->(node2)
 */
public class TrieNode<T> {
  private Map<String, TrieNode<T>> gotoMap; // gotoMap is a property of each node
  private String singleTransition;
  private TrieNode<T> singleNode;
  private TrieNode<T> failureNode; // define the node to go to if the transition node is not found in the goto map

  private List<EmitValue<T>> emitList; // define emission values for each node

  public TrieNode() {
    this.gotoMap = null;
    this.failureNode = null;
    this.emitList = null;
    this.singleTransition = null;
    this.singleNode = null;
  }

  private boolean hasEmitValues() {
    return emitList != null && !emitList.isEmpty();
  }

  /**
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @return
   */
  public List<EmitValue<T>> getEmitList() {
    return emitList;
  }

  /**
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @return
   */
  public TrieNode<T> getFailureNode() {
    return failureNode;
  }

  /**
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @return
   */
  public boolean hasTransitionLabel(String transitionLabel) {
    return getNextState(transitionLabel) != null;
  }

  /**
   * Doesn't assume label is already in map.
   * @param transitionLabel
   * @return node for label or null if not known
   */
  public TrieNode<T> getNextState(String transitionLabel) {
    if (singleTransition != null) {
      return (singleTransition.equals(transitionLabel)) ? singleNode : null;
    }
    return gotoMap != null ? gotoMap.get(transitionLabel) : null;
  }

  public List<EmitValue<T>> getEmitsBelow() {
    List<EmitValue<T>> emits = new ArrayList<EmitValue<T>>();
    if (emitList != null) {
      emits.addAll(emitList);
    }
    if (singleNode != null) {
      emits.addAll(singleNode.getEmitsBelow());
    }
    else if (gotoMap != null) {
      for (TrieNode<T> child : gotoMap.values()) {
        emits.addAll(child.getEmitsBelow());
      }
    }
    return emits;
  }

  // everything below is used just during trie construction ---------------------------------------------------------

  /**
   * @see Trie#addEntryToTrie(TextEntityValue, java.util.Map)
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @return
   */
  List<EmitValue<T>> getAndCreateEmitList() {
    if (emitList == null)  {
      emitList = new ArrayList<EmitValue<T>>(1);
    }
    return emitList;
  }

  /**
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @param failureNode
   */
  void setFailureNode(TrieNode<T> failureNode) {
    this.failureNode = failureNode;
  }

/*  boolean hasGotoMap() {
    return singleTransition != null || gotoMap != null;
  }*/

  /**
   * @see Trie#addEntryToTrie(TextEntityValue, java.util.Map)
   * @param label
   * @param node
   */
  void addTransition(String label, TrieNode<T> node) {
    if (singleTransition == null && gotoMap == null) {
      singleTransition = label;
      singleNode = node;
    }
    else { // either we have a single entry or multiple
      if (gotoMap == null)  { // if single, make multiple storage
        gotoMap = new HashMap<String, TrieNode<T>>(2);
        gotoMap.put(singleTransition, singleNode);
        singleTransition = null;
        singleNode = null;
      }
      gotoMap.put(label,node);
    }
  }

  /**
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @return
   */
  Collection<String> getTransitionLabels() {
    if (singleTransition != null) {
      return Collections.singleton(singleTransition);
    }
    else if (gotoMap != null) {
      return gotoMap.keySet();
    }
    else {
      return Collections.emptySet();
    }
  }

  /**
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @return
   */
  Collection<TrieNode<T>> getTransitionValues() {
    if (singleNode != null) {
      return Collections.singleton(singleNode);
    }
    else if (gotoMap != null) {
      return gotoMap.values();
    }
    else {
      return Collections.emptySet();
    }
  }

  /**
   * Assumes label is a known label (one already added).
   *
   * ONLY use this during building the tree!
   *
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   * @param transitionLabel
   * @return node for label
   */
  TrieNode<T> getKnownNextState(String transitionLabel) {
    if (singleNode != null) {
      return singleNode;
    }
    return gotoMap.get(transitionLabel);
  }

  public String toString() {
    return singleTransition != null ? (singleTransition + "=[" + singleNode +"]") : gotoMap != null ? gotoMap.toString() : "" + "" +
        (hasEmitValues() ? emitList.size() == 1 ? emitList.iterator().next() : emitList : "");
  }
}
