package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/29/13
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionNode implements IsSerializable {
  private String type;
  private String name;
  private List<SectionNode> children = new ArrayList<SectionNode>();

  public SectionNode() {}   // required for serialization

  /**
   * @see mitll.langtest.server.database.SectionHelper#addChildren(java.util.List, SectionNode, java.util.Map)
   * @param type
   * @param name
   */
  public SectionNode(String type, String name) { this.type = type; this.name = name; }
  public void addChild(SectionNode node) { children.add(node);}

  public String getName() { return name; }
  public String getType() { return type; }
  public boolean isLeaf() { return children.isEmpty(); }

  public List<SectionNode> getChildren() { return children; }

  public String toString() {
    return getType() +"="+name + (children.isEmpty() ? "" : (" : [(" + children.size() + ") " + children +"]"));
  }
}
