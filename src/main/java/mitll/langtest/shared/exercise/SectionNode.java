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

import java.util.ArrayList;
import java.util.Collection;
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

  private List<SectionNode> children = null;//Collections.emptyList();// ArrayList<SectionNode>();

  private String childType;

  public SectionNode() {
  }   // required for serialization

  /**
   * @param name
   * @see mitll.langtest.server.database.exercise.SectionHelper#addChildren(java.util.List, SectionNode, java.util.Map)
   */
  public SectionNode(String type, String name) {
    this.type = type;
    this.name = name;
  }

  /**
   * @paramx node
   * @see mitll.langtest.server.database.exercise.SectionHelper#addChildren
   */
/*  public void addChild(SectionNode node) {
    children.add(node);
  }*/
  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }


  //@Deprecated
  public boolean isLeaf() {
    return children == null || children.isEmpty();
  }

  //  @Deprecated
  public Collection<SectionNode> getChildren() {
    return isLeaf() ? Collections.emptyList() : children;
  }
/*

  public Collection<SectionNode> getChildren() {
    if (typeToChildren.isEmpty()) return Collections.emptyList();
    else return typeToChildren.values().iterator().next();
  }
*/

  public int count() {
    if (children.isEmpty()) return 1;
    else {
      int total = 0;
      for (SectionNode child : children) {
        total += child.count();
      }
      return total;
    }
  }

  @Override
  public int compareTo(SectionNode o) {
    return name.compareTo(o.name);
  }

  public boolean equals(Object other) {
    if (other instanceof SectionNode) return compareTo((SectionNode) other) == 0;
    else return false;
  }

  public String toComplete(int level) {
    StringBuilder builder = new StringBuilder();
    for (SectionNode sectionNode : children) {
      for (int i = 0; i < level; i++) builder.append("\t");
      builder.append(sectionNode.toComplete(level + 1));
      builder.append("\n");
    }
    return
        //getType() + "=" +
        name + " children are " + childType +
            (this.children.isEmpty() ? "" : (" : [(" + this.children.size() + "), " + builder.toString() + "]"));
  }

  public String toString() {
    String example = isLeaf() ? "" : children.toString();//children.get(0).toString();
    return
        //  getType() + "="       +
        name + "" + //(typeToChildren.isEmpty() ? "" : " " + typeToChildren) +
            (isLeaf() ? "" : (" " + childType +
                " : [" +
                //"(" + this.children.size() + ")" +
                //" : " +
                example + "]"));
  }

  public String easy() {
    return name + " " + childType + " " + children.size();
  }

  public SectionNode getChild(String type, String value) {
    //Set<SectionNode> sectionNodes = typeToChildren.computeIfAbsent(type, p -> new TreeSet<>());

    if (childType == null) childType = type;
    //Set<SectionNode> sectionNodes = typeToChildren.get(type);
    if (children == null) children = new ArrayList<>();
//      typeToChildren.put(type, sectionNodes = new TreeSet<SectionNode>());

    // System.out.println("for " + this + " now " + typeToChildren);

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

//  public void setChildType(String childType) {
//    this.childType = childType;
//  }
}
