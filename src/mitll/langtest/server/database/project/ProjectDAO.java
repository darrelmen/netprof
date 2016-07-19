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

package mitll.langtest.server.database.project;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.project.ProjectDAOWrapper;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

public class ProjectDAO extends DAO implements IProjectDAO {
  private static final Logger logger = Logger.getLogger(ProjectDAO.class);

  private ProjectDAOWrapper dao;
  private ProjectPropertyDAO propertyDAO;

  public ProjectDAO(Database database, DBConnection dbConnection) {
    super(database);
    propertyDAO = new ProjectPropertyDAO(database, dbConnection);
    dao = new ProjectDAOWrapper(dbConnection);
  }

  public ProjectPropertyDAO getProjectPropertyDAO() {
    return propertyDAO;
  }

  public void createTable() {  dao.createTable(); }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  public int add(int userid, String name, String language, String firstType, String secondType) {
    return add(userid, System.currentTimeMillis(), name, language, "", ProjectType.NP, ProjectStatus.PRODUCTION, firstType, secondType);
  }

  @Override
  public int add(int userid, long modified, String name, String language, String course,
                 ProjectType type, ProjectStatus status, String firstType, String secondType) {
    return dao.insert(new SlickProject(
        -1,
        userid,
        new Timestamp(modified),
        name,
        language,
        course,
        type.toString(),
        status.toString(),
        firstType,
        secondType));
  }

  @Override
  public Collection<SlickProject> getAll() {  return dao.getAll();  }

  public void addProperty(int project, String key, String value) {
    propertyDAO.add(project, System.currentTimeMillis(), key, value);
  }

  @Override
  public int getByName(String name) {
    return dao.byName(name);
  }
}
