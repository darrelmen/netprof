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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.log4j.Logger;

import java.util.*;

public class DBExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = Logger.getLogger(DBExerciseDAO.class);
  private SlickUserExerciseDAO userExerciseDAO;
  private SlickProject project;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeExerciseDAO(String, boolean)
   */
  public DBExerciseDAO(
      ServerProperties serverProps,
      IUserListManager userListManager,
      boolean addDefects,
      SlickUserExerciseDAO userExerciseDAO,
      SlickProject project
  ) {
    super(serverProps, userListManager, addDefects, project.language());
    logger.info("reading from database--------- ");
    this.userExerciseDAO = userExerciseDAO;
    this.project = project;
  }

  /**
   * I don't think we're doing user exercise mask out overrides anymore...
   * @param removes
   */
  protected void addOverlays(Collection<Integer> removes) {}

  public Map<Integer,String> getIDToFL(int projid) {
    return userExerciseDAO.getIDToFL(projid);
  }
  /**
   * Does join with related exercise table - maybe better way to do this in scala side?
   *
   * @return
   * @see #getRawExercises()
   */
  @Override
  List<CommonExercise> readExercises() {
    try {
      List<String> typeOrder = Arrays.asList(project.first(), project.second());
      String prefix = "Project " + project.name();

      int projid = project.id();
      List<CommonExercise> allNonContextExercises = userExerciseDAO.getByProject(projid, typeOrder, getSectionHelper());
      logger.info("project " + project +" readExercises got " + allNonContextExercises.size() + " predef exercises;");

      Collection<SlickRelatedExercise> related = userExerciseDAO.getAllRelated(projid);

//      logger.info(prefix + " readExercises got " + related.size() + " related exercises;");

      Map<Integer, CommonExercise> idToEx = getIDToExercise(allNonContextExercises);
      Map<Integer, CommonExercise> idToContext =
          getIDToExercise(userExerciseDAO.getContextByProject(projid, typeOrder, getSectionHelper()));

      logger.info(prefix + " idToContext " + idToContext.size());

      int attached = 0;
      int c = 0;
      for (SlickRelatedExercise relatedExercise : related) {
        CommonExercise root = idToEx.get(relatedExercise.exid());
        if (root != null) {
          CommonExercise context = idToContext.get(relatedExercise.contextexid());
          if (context != null) {
            root.getMutable().addContextExercise(context);
            attached++;
          } else if (c++ < 10) {
            logger.warn("1 " +prefix + " didn't attach " + relatedExercise + "" +
                " for\n" + root + "\n and " + context);
          }
        } else if (c++ < 10) {
          logger.warn("2 " + prefix + " didn't attach " + relatedExercise + "" +
              " for, e.g. " + allNonContextExercises.iterator().next());
        }
      }
      logger.info(prefix +
          " Read " + allNonContextExercises.size() + " exercises from database, attached " + attached);
      return allNonContextExercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }

  private Map<Integer, CommonExercise> getIDToExercise(Collection<CommonExercise> allExercises) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise ex : allExercises) {
      idToEx.put(ex.getID(), ex);
    }
    return idToEx;
  }
}
