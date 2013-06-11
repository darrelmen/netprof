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
  public SectionNode() {}   // required for serialization
  private String type;
  private String name;
  private List<SectionNode> children = new ArrayList<SectionNode>();

  public SectionNode(String type, String name) { this.type = type; this.name = name; }
  public String getName() {
    return name;
  }

  public void addChild(SectionNode node) { children.add(node);}
  public List<SectionNode> getChildren() { return children; }

  public String toString() {
    return type+"="+name + (children.isEmpty() ? "" : (" : [(" + children.size() + ") " + children +"]"));
  }
}
