package mitll.langtest.server.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.database.exercise.Project;
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
