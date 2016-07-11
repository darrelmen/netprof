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
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.log4j.Logger;

import java.util.*;

public class DBExerciseDAO extends BaseExerciseDAO implements ExerciseDAO<CommonExercise> {
  private static final Logger logger = Logger.getLogger(DBExerciseDAO.class);
  // private final Collection<String> typeOrder;
  SlickUserExerciseDAO userExerciseDAO;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   */
  public DBExerciseDAO(
      ServerProperties serverProps,
      UserListManager userListManager,
      boolean addDefects,
      SlickUserExerciseDAO userExerciseDAO) {
    super(serverProps, userListManager, addDefects);
    // this.typeOrder = serverProps.getTypes();
    this.userExerciseDAO = userExerciseDAO;
    //  new DominoReader().readProjectInfo(serverProps);
  }

  /**
   * Does join with related exercise table - maybe better way to do this in scala side?
   * TODO : type order is from the project
   *
   * @return
   * @see #getRawExercises()
   */
  @Override
  List<CommonExercise> readExercises() {
    try {
      List<String> typeOrder = Arrays.asList("Unit", "Chapter");
      List<CommonExercise> allExercises = userExerciseDAO.getAllExercises(typeOrder, getSectionHelper());

      Collection<SlickRelatedExercise> related = userExerciseDAO.getAllRelated();

      Map<Integer, CommonExercise> idToEx = new HashMap<>();
      for (CommonExercise ex : allExercises) {
        idToEx.put(ex.getRealID(), ex);
      }

      for (SlickRelatedExercise relatedExercise : related) {
        CommonExercise root = idToEx.get(relatedExercise.id());
        CommonExercise context = idToEx.get(relatedExercise.contextexid());
        if (root != null && context != null) root.getMutable().addContextExercise(context);
      }
      logger.info("Read " + allExercises.size() + " exercises from database");
      //  List<CommonExercise> exercises = getExercisesFromArray(getJSON());
      return allExercises;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }
}
