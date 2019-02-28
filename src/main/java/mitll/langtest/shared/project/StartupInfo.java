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

package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.LangTest;
import mitll.langtest.shared.user.Affiliation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StartupInfo implements IsSerializable {
  private List<Affiliation> affiliations;
  private Map<String, String> properties;
  private List<SlimProject> projects;
  private String message = "";
  private String implementationVersion;
  private static final String WEBSERVICE_HOST_DEFAULT = "127.0.0.1";
  /**
   * Name.IMPLEMENTATION_VERSION
   */
  private static final String IMPL_VERSION = "Implementation-Version";

  public StartupInfo() {
  } // for serialization

  /**
   * @param properties - ui properties only!
   * @see mitll.langtest.server.LangTestDatabaseImpl#getStartupInfo
   */
  public StartupInfo(Map<String, String> properties,
                     List<SlimProject> projects,
                     String message,
                     List<Affiliation> affiliations) {
    this.properties = properties;
    this.projects = projects;
    this.message = message;
    this.affiliations = affiliations;
    this.implementationVersion = properties.get(IMPL_VERSION);
  }

  /**
   * @return
   * @see mitll.langtest.client.LangTest#rememberStartup(StartupInfo, boolean)
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  public List<SlimProject> getProjects() {
    return projects;
  }

  /**
   * Not sure when these could be out of sync but...
   *
   * @param id
   * @return
   * @see LangTest#getHost
   */
  public String getHost(int id) {
    List<SlimProject> withThisID =
        getAllProjects()
            .stream()
            .filter(slimProject -> slimProject.getID() == id)
            .collect(Collectors.toList());

    if (withThisID.isEmpty()) {
      return "";
    } else {
      String host = withThisID.iterator().next().getHost();
      return host.equals(WEBSERVICE_HOST_DEFAULT) ? "" : host;
    }
  }

  /**
   * @return mutable copy of the current project list
   * @see LangTest#getAllProjects
   * OK to modify the returned list.
   */
  public List<SlimProject> getAllProjects() {
    List<SlimProject> all = new ArrayList<>();
    projects.forEach(slimProject -> {
      if (slimProject.hasChildren()) {
        all.addAll(slimProject.getChildren());
      } else {
        all.add(slimProject);
      }
    });
    return all;
  }

  /**
   * If the app doesn't start properly, what to show
   *
   * @return
   */
  public String getMessage() {
    return message;
  }
  public List<Affiliation> getAffiliations() {    return affiliations;  }

  public String getImplementationVersion() {   return implementationVersion;  }

  public String toString() {
    return "Message = '" + message + "', " + getProjects().size() + " projects.";
  }
}
