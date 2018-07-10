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

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.exercise.SectionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/29/13
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionNode implements IsSerializable, Comparable<SectionNode> {
  private String type; // redundant
  private String name;
  private int count;

  private List<SectionNode> children = null;

  private String childType;

  public SectionNode() {
  }   // required for serialization

  /**
   * @param name
   * @see SectionHelper#makeRoot
   * @see #getChild
   */
  public SectionNode(String type, String name) {
    this.type = type;
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public boolean isLeaf() {
    return children == null || children.isEmpty();
  }

  public List<SectionNode> getChildren() {
    return isLeaf() ? Collections.emptyList() : children;
  }

  /**
   * JUST FOR TESTING
   *
   * @return
   */
  public int childCount() {
    if (isLeaf()) return 1;
    else {
      int total = 0;
      for (SectionNode child : children) {
        total += child.childCount();
      }
      return total;
    }
  }

  @Override
  public int compareTo(SectionNode o) {
    return name.compareTo(o.name);
  }

  public boolean equals(Object other) {
    return other instanceof SectionNode && compareTo((SectionNode) other) == 0;
  }

  public SectionNode getChild(String type, String value) {
    if (childType == null) childType = type;
    if (children == null) children = new ArrayList<>();
    SectionNode e;
    List<SectionNode> collect = children
        .stream()
        .filter(p -> p.getName().equals(value))
        .collect(Collectors.toList());

    if (collect.isEmpty()) {
      e = new SectionNode(type, value);
      children.add(e);
//      System.out.println("1 now " + typeToChildren);
      //    System.out.println("1.5 now " + this);
    } else {
      if (collect.size() > 1) {
        System.err.println("getChild found " + collect.size());
        e = null;
      } else {
        e = collect.get(0);
        //  System.out.println("2 now " + typeToChildren + " " + e);
      }
    }
    e.count++;
    return e;
  }

  public SectionNode getChildWithName(String name) {
    for (SectionNode node : children) {
      if (node.getName().equals(name)) return node;
    }
    return null;
  }

  public String getChildType() {
    return childType;
  }

  public String toComplete(int level) {
    StringBuilder builder = new StringBuilder();
    if (children != null) {
      for (SectionNode sectionNode : children) {
        for (int i = 0; i < level; i++) builder.append("\t");
        builder.append(sectionNode.toComplete(level + 1));
        builder.append("\n");
      }
    }
    boolean leaf = isLeaf();
    return
        //getProperty() + "=" +
        name +
            (!leaf ? " children are " + childType : "") +
            (leaf ? "" :  (" : [(" + this.children.size() + "), " + builder.toString() + "]"));
  }

  public int getCount() {
    return count;
  }

  public String easy() {
    return name + " " + childType + " " + children.size();
  }

  public String toString() {
    String example = isLeaf() ? "" : children.toString();
    return
        name + "/" + count +
            "" +
            (isLeaf() ? "" : (" " + childType +
                " : [" +
                example + "]"));
  }
}
