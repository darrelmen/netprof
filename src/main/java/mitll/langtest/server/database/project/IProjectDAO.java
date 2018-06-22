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

import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.ProjectType;
import mitll.npdata.dao.SlickProject;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IProjectDAO extends IDAO {
  /**
   * TODO : why two calls?
   * @see mitll.langtest.server.database.copy.CreateProject#addProject
   * @param userid
   * @param name
   * @param language
   * @param course
   * @param firstType
   * @param secondType
   * @param countryCode
   * @param displayOrder
   * @param projectType
   * @param status
   * @param dominoID
   * @return
   */
  int add(int userid, String name, String language, String course,
          String firstType, String secondType, String countryCode, int displayOrder,
          ProjectType projectType, ProjectStatus status, int dominoID);

  /**
   * @see ProjectDAO#add(int, long, String, String, String, ProjectType, ProjectStatus, String, String, String, int, int)
   * @param userid
   * @param modified
   * @param name
   * @param language
   * @param course
   * @param type
   * @param status
   * @param firstType
   * @param secondType
   * @param countryCode
   * @param displayOrder
   * @param dominoID
   * @return
   */
  int add(int userid, long modified, String name, String language, String course,
          ProjectType type, ProjectStatus status, String firstType, String secondType, String countryCode, int displayOrder, int dominoID);

  /**
   * Deprecated - this doesn't really work in practice - takes forever, locks database while it's running.
   * It's like a suicide pill.
   * @param id
   * @return
   */
  boolean delete(int id);
  boolean deleteAllBut(int id);

  /**
   * @return
   * @see ProjectManagement#populateProjects
   */
  Collection<SlickProject> getAll();

  /**
   * @see ProjectManagement#getNestedProjectInfo
   * @return
   */
  int getNumProjects();

  int getByName(String name);
  int getByLanguageAndName(String language, String name);
  int getByLanguage(String language);

  int ensureDefaultProject(int defaultUser);

  int getDefault();

  /**
   * @see mitll.langtest.server.services.ProjectServiceImpl#exists(int)
   * @param projid
   * @return
   */
  boolean exists(int projid);
  SlickProject getByID(int projid);

  /**
   * @see ProjectDAO#update
   * @param changed
   * @return
   */
  boolean easyUpdate(SlickProject changed);
  boolean easyUpdateNetprof(SlickProject changed, long sinceWhen);

  /**
   * @see mitll.langtest.server.services.ProjectServiceImpl#update
   * @see mitll.langtest.client.project.ProjectEditForm#updateProject
   * @param userid
   * @param projectInfo
   * @return
   */
  boolean update(int userid, ProjectInfo projectInfo);

  /**
   * @see mitll.langtest.server.database.copy.CreateProject#addModelProp
   * @param projid
   * @param projectProperty
   * @param value
   * @param propertyType
   * @param parent
   */
  void addProperty(int projid, ProjectProperty projectProperty, String value, String propertyType, String parent);

  /**
   * @see mitll.langtest.server.database.copy.CreateProject#createProject(DAOContainer, ProjectServices, ProjectInfo, int)
   * @param projid
   * @param key
   * @param value
   * @param propertyType
   * @param parent
   */
  void addProperty(int projid, String key, String value, String propertyType, String parent);

  /**
   * @see
   * @param projid
   * @param projectProperty
   * @param newValue
   * @return true if changed
   */
  boolean addOrUpdateProperty(int projid, ProjectProperty projectProperty, String newValue);

  List<String> getListProp(int projid, ProjectProperty projectProperty);

  /**
   * @see Project#getProp
   * @param projid
   * @param key
   * @return
   */
  String getPropValue(int projid, String key);
  String getDefPropValue(int projid, ProjectProperty projectProperty);
  boolean getShouldSwap(int projid);

  /**
   * @see Project#putAllProps
   * @param projid
   * @return
   */
  Map<String, String> getProps(int projid);

  boolean maybeSetDominoIDs(Project project);
}
