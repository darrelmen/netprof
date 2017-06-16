package mitll.langtest.server.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.database.exercise.Project;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

/**
 * Created by go22670 on 1/20/17.
 * For the new iOS.
 */
public class ProjectExport {
  //private static final Logger logger = LogManager.getLogger(ProjectExport.class);

  /**
   * @param productionProjects
   * @return
   * @see mitll.langtest.server.ScoreServlet#getProjects
   */
  public String toJSON(Collection<Project> productionProjects) {
    JsonObject jsonObject = new JsonObject();
    JsonArray value = new JsonArray();
    jsonObject.add("sites", value);

    //logger.info("toJSON converting " + productionProjects.size() + " projects");

    for (Project project : productionProjects) {
      JsonObject proj = new JsonObject();
      value.add(proj);
      proj.addProperty("id", project.getProject().id());
      proj.addProperty("name", project.getProject().name());
      proj.addProperty("type", "npf2");
//      proj.addProperty("appVersion", "npf");
      proj.addProperty("language", project.getProject().language());
      proj.addProperty("course", project.getProject().course());
      proj.addProperty("kind", project.getProject().kind());
      proj.addProperty("status", project.getProject().status());
      proj.addProperty("countrycode", project.getProject().countrycode());
      proj.addProperty("displayorder", project.getProject().displayorder());
      proj.addProperty("showOnIOS", Boolean.TRUE);
      proj.addProperty("rtl", project.isRTL());
      String host = project.getWebserviceHost();
      proj.addProperty("host", host.equals("127.0.0.1") ? "" : host);
    }
    return jsonObject.toString();
  }
}
