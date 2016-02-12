/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/29/13
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionNode implements IsSerializable, Comparable<SectionNode> {
  private String type;
  private String name;

  private List<SectionNode> children = new ArrayList<SectionNode>();

  public SectionNode() {
  }   // required for serialization

  /**
   * @param type
   * @param name
   * @see mitll.langtest.server.database.exercise.SectionHelper#addChildren(java.util.List, SectionNode, java.util.Map)
   */
  public SectionNode(String type, String name) {
    this.type = type;
    this.name = name;
  }

  public void addChild(SectionNode node) {
    children.add(node);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public boolean isLeaf() {
    return children.isEmpty();
  }

  public Collection<SectionNode> getChildren() {
    return children;
  }

  @Override
  public int compareTo(SectionNode o) {
    return name.compareTo(o.name);
  }

  public String toString() {
    String example = children.toString();//children.isEmpty() ? "" :children.get(0).toString();
    return getType() + "=" + name +
        (this.children.isEmpty() ? "" : (" : [(" + this.children.size() + "), e.g. " + example + "]"));
  }
}
