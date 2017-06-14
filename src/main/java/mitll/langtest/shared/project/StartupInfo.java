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

package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.user.Affiliation;
import mitll.langtest.shared.project.SlimProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/19/13
 * Time: 7:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class StartupInfo implements IsSerializable {
  private List<Affiliation> affiliations;
  private Map<String, String> properties;
  private List<SlimProject> projects;
  private String message = "";

  public StartupInfo() {
  } // for serialization

  public Set<String> getKnownHydraHosts() {
    return projects.stream().map(ProjectInfo::getHost).collect(Collectors.toSet());
  }

  /**
   * @param properties
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
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public List<SlimProject> getProjects() {
    return projects;
  }

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

  public List<Affiliation> getAffiliations() {
    return affiliations;
  }
}
