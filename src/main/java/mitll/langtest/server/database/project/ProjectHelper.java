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
   * @see ProjectManagement#getNestedProjectInfo
   * @param db
   * @param securityManager
   * @return
   */
  public List<SlimProject> getProjectInfos(DatabaseServices db, IUserSecurityManager securityManager) {
    List<SlimProject> projectInfos = new ArrayList<>();
    if (db == null) {
      logger.info("getProjectInfos no db yet...");
    } else {
      IProjectManagement projectManagement = db.getProjectManagement();
      ((NPUserSecurityManager) securityManager).setProjectManagement(projectManagement);
      if (projectManagement == null) {
        logger.error("getProjectInfos : config error - didn't make project management");
      } else {
        long then = System.currentTimeMillis();
        projectInfos = projectManagement.getNestedProjectInfo();
        long now = System.currentTimeMillis();
        if (now - then > 10L)
          logger.info("getProjectInfos took " + (now - then) + " millis to get nested projects.");
      }
    }
    return projectInfos;
  }
}
