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

package mitll.langtest.server.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.database.project.Project;
import mitll.npdata.dao.SlickProject;

import java.util.Collection;

/**
 * Created by go22670 on 1/20/17.
 * For the new iOS.
 */
public class ProjectExport {
  //private static final Logger logger = LogManager.getLogger(ProjectExport.class);
  private static final String SHOW_ON_IOS = "showOnIOS";
  private static final String LOCALHOST = "127.0.0.1";

  /**
   * Also sends current iOSVersion.
   *
   * @param productionProjects
   * @return
   * @see mitll.langtest.server.ScoreServlet#getProjects
   */
  public String toJSON(Collection<Project> productionProjects, String iOSVersion) {
    JsonObject jsonObject = new JsonObject();
    JsonArray value = new JsonArray();
    jsonObject.addProperty("iOSVersion",  iOSVersion);
    jsonObject.add("sites", value);
    //logger.info("toJSON converting " + productionProjects.size() + " projects");

    for (Project project : productionProjects) {
      project.clearPropCache();

      JsonObject proj = new JsonObject();
      value.add(proj);

      SlickProject project1 = project.getProject();

      proj.addProperty("id", project1.id());
      proj.addProperty("name", project1.name());
      proj.addProperty("type", "npf2");
//      proj.addProperty("appVersion", "npf");
      proj.addProperty("language", project1.language());
      proj.addProperty("course", project1.course());
      proj.addProperty("kind", project1.kind());
      proj.addProperty("status", project1.status());
      proj.addProperty("countrycode", project1.countrycode());
      proj.addProperty("displayorder", project1.displayorder());
      proj.addProperty(SHOW_ON_IOS, project.isOnIOS());
      proj.addProperty("rtl", project.isRTL());

      String host = project.getWebserviceHost();
      proj.addProperty("host", host.equals(LOCALHOST) ? "" : host);
    }
    return jsonObject.toString();
  }
}
