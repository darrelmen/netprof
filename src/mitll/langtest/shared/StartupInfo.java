package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/19/13
 * Time: 7:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class StartupInfo implements IsSerializable {
  private Map<String, String> properties;
  private Collection<String> typeOrder;
  private List<SectionNode> sectionNodes;

  public StartupInfo() {} // for serialization

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getStartupInfo()
   * @param properties
   * @param typeOrder
   * @param sectionNodes
   */
  public StartupInfo(Map<String, String> properties, Collection<String> typeOrder, List<SectionNode> sectionNodes) {
    this.properties = properties;
    this.typeOrder = typeOrder;
    this.sectionNodes = sectionNodes;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Collection<String> getTypeOrder() {
    return typeOrder;
  }

  public List<SectionNode> getSectionNodes() {
    return sectionNodes;
  }

  public String toString() { return "Order " + getTypeOrder() + " nodes " + getSectionNodes(); }
}
