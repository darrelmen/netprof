package mitll.langtest.server.database.project;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.server.database.security.NPUserSecurityManager;
import mitll.langtest.server.services.AudioServiceImpl;
import mitll.langtest.shared.project.SlimProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ProjectHelper {
  private static final Logger logger = LogManager.getLogger(ProjectHelper.class);

  /**
   * @see LangTestDatabaseImpl#getStartupInfo
   * @see AudioServiceImpl#getStartupInfo
   * @param db
   * @param securityManager
   * @return
   */
  public List<SlimProject> getProjectInfos(DatabaseServices db, IUserSecurityManager securityManager) {
    List<SlimProject> projectInfos = new ArrayList<>();
    if (db == null) {
      logger.info("getStartupInfo no db yet...");
    } else {
      IProjectManagement projectManagement = db.getProjectManagement();
      ((NPUserSecurityManager) securityManager).setProjectManagement(projectManagement);
      if (projectManagement == null) {
        logger.error("getStartupInfo : config error - didn't make project management");
      } else {
        long then = System.currentTimeMillis();
        projectInfos = projectManagement.getNestedProjectInfo();
        long now = System.currentTimeMillis();
        if (now - then > 100L)
          logger.info("getStartupInfo took " + (now - then) + " millis to get nested projects.");
      }
    }
    return projectInfos;
  }
}
