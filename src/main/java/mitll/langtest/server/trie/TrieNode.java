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

import java.util.*;

public class TrieNode<T> {
  private Map<String, TrieNode<T>> gotoMap; // gotoMap is a property of each node
  private String singleTransition;
  private TrieNode<T> singleNode;
  private TrieNode<T> failureNode; // define the node to go to if the transition node is not found in the goto map

  private List<EmitValue<T>> emitList; // define emission values for each node

  TrieNode() {
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
   * @return
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  List<EmitValue<T>> getEmitList() {
    return emitList;
  }

  /**
   * @return
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  TrieNode<T> getFailureNode() {
    return failureNode;
  }

  /**
   * @return
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  boolean hasTransitionLabel(String transitionLabel) {
    return getNextState(transitionLabel) != null;
  }

  /**
   * Doesn't assume label is already in map.
   *
   * @param transitionLabel
   * @return node for label or null if not known
   */
  TrieNode<T> getNextState(String transitionLabel) {
    if (singleTransition != null) {
      return (singleTransition.equals(transitionLabel)) ? singleNode : null;
    }
    return gotoMap != null ? gotoMap.get(transitionLabel) : null;
  }

  public String explain(String transitionLabel) {
    if (singleTransition != null) {
      return (singleTransition.equals(transitionLabel)) ? "match single transition (" + singleTransition +
          ") = " + singleNode : " no match to single transition (" + singleTransition +
          ")";
    }
    return gotoMap != null ? "got goto match " + gotoMap.get(transitionLabel) : " no goto match in " + gotoMap.keySet();
  }

  List<EmitValue<T>> getEmitsBelow() {
    List<EmitValue<T>> emits = new ArrayList<EmitValue<T>>();
    if (emitList != null) {
      emits.addAll(emitList);
    }
    if (singleNode != null) {
      emits.addAll(singleNode.getEmitsBelow());
    } else if (gotoMap != null) {
      for (TrieNode<T> child : gotoMap.values()) {
        emits.addAll(child.getEmitsBelow());
      }
    }
    return emits;
  }

  // everything below is used just during trie construction ---------------------------------------------------------

  /**
   * @return
   * @see Trie#addEntryToTrie(TextEntityValue, java.util.Map)
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  List<EmitValue<T>> getAndCreateEmitList() {
    if (emitList == null) {
      emitList = new ArrayList<EmitValue<T>>(1);
    }
    return emitList;
  }

  /**
   * @param failureNode
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  void setFailureNode(TrieNode<T> failureNode) {
    this.failureNode = failureNode;
  }

/*  boolean hasGotoMap() {
    return singleTransition != null || gotoMap != null;
  }*/

  /**
   * @param label
   * @param node
   * @see Trie#addEntryToTrie(TextEntityValue, java.util.Map)
   */
  void addTransition(String label, TrieNode<T> node) {
    if (singleTransition == null && gotoMap == null) {
      singleTransition = label;
      singleNode = node;

    } else { // either we have a single entry or multiple
      if (gotoMap == null) { // if single, make multiple storage
        gotoMap = new HashMap<String, TrieNode<T>>(2);
        gotoMap.put(singleTransition, singleNode);
        singleTransition = null;
        singleNode = null;
      }
      gotoMap.put(label, node);
    }
  }

  /**
   * @return
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  Collection<String> getTransitionLabels() {
    if (singleTransition != null) {
      return Collections.singleton(singleTransition);
    } else if (gotoMap != null) {
      return gotoMap.keySet();
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * @return
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  Collection<TrieNode<T>> getTransitionValues() {
    if (singleNode != null) {
      return Collections.singleton(singleNode);
    } else if (gotoMap != null) {
      return gotoMap.values();
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * Assumes label is a known label (one already added).
   * <p>
   * ONLY use this during building the tree!
   *
   * @param transitionLabel
   * @return node for label
   * @see mitll.langtest.server.trie.Trie#computeFailureFunction()
   */
  TrieNode<T> getKnownNextState(String transitionLabel) {
    if (singleNode != null) {
      return singleNode;
    }
    return gotoMap.get(transitionLabel);
  }

  public String toString() {
    return singleTransition != null ? (singleTransition + "=[" + singleNode + "]") : gotoMap != null ? gotoMap.toString() : "" + "" +
        (hasEmitValues() ? emitList.size() == 1 ? emitList.iterator().next() : emitList : "");
  }
}
