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

package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.HasID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;

public class BaseUserExerciseDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BaseUserExerciseDAO.class);

  /**
   * @see #getPredefExercise
   * @see #setExerciseDAO
   */
  ExerciseDAO<CommonExercise> exerciseDAO;

  protected BaseUserExerciseDAO(Database database) { super(database);  }

  /**
   * TODO : Do we need to set the english field to meaning for english items???
   *
   * @param userExercises2
   * @param userExercises
   * @deprecated not needed with postgres
   */
  void enrichWithPredefInfo(List<CommonShell> userExercises2, Collection<CommonExercise> userExercises) {
    int c = 0;
    for (CommonExercise ue : userExercises) {
      // if (DEBUG) logger.debug("\ton list " + listID + " " + ue.getOldID() + " / " + ue.getUniqueID() + " : " + ue);
      if (ue.isPredefined()) {
        CommonExercise byID = getExercise(ue);

        if (byID != null) {
          userExercises2.add(new Exercise(byID)); // all predefined references
          /// TODO : put this back???
          // if (isEnglish) {
          //    e.setEnglish(exercise.getMeaning());
          //  }

        } else {
          if (c++ < 10)
            logger.error("getOnList: huh can't find user exercise '" + ue.getOldID() + "'");
        }
      } else {
        userExercises2.add(ue);
      }
    }
    if (c > 0) logger.warn("huh? can't find " + c + "/" + userExercises.size() + " items???");
  }

  /**
   * @param ue
   * @return
   * @see #enrichWithPredefInfo
   */
  private CommonExercise getExercise(HasID ue) {
    return getPredefExercise(ue.getID());
  }

  /**
   * @param exid
   * @return
   * @see IUserListManager#getReviewedUserExercises
   */
  public CommonExercise getPredefExercise(int exid) {  return exerciseDAO.getExercise(exid);  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#makeDAO
   * @param exerciseDAO
   */
  public void setExerciseDAO(ExerciseDAO<CommonExercise> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }
}
