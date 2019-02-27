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

package mitll.langtest.server.services;

import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.services.QCService;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.STATE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("serial")
public class QCServiceImpl extends MyRemoteServiceServlet implements QCService {
  private static final Logger logger = LogManager.getLogger(LangTestDatabaseImpl.class);
  public static final String MARK_STATE = "mark state";

  /**
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation
   */
  @Override
  public void addAnnotation(int exerciseID, String field, String status, String comment) throws DominoSessionException {
    getUserListManager().addAnnotation(exerciseID, field, status, comment, getUserIDFromSessionOrDB());
  }

  /**
   * @param exid
   * @param isCorrect
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  public void markReviewed(int exid, boolean isCorrect) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasQCPerm(userIDFromSessionOrDB)) {
      int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);
      CommonExercise customOrPredefExercise = db.getExercise(projectIDFromUser,exid);//db.getCustomOrPredefExercise(projectIDFromUser, exid);
      getUserListManager().markCorrectness(customOrPredefExercise, isCorrect, userIDFromSessionOrDB);
    } else {
      throw getRestricted("mark reviewed");
    }
  }

  /**
   * @param exid
   * @param state
   * @see ReviewEditableExercise#userSaidExerciseIsFixed
   */
  public void markState(int exid, STATE state) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasQCPerm(userIDFromSessionOrDB)) {
      int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);
      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(projectIDFromUser, exid);
      getUserListManager().markState(customOrPredefExercise, state, getUserIDFromSessionOrDB());
    } else {
      throw getRestricted(MARK_STATE);
    }
  }

  /**
   * @param audioAttribute
   * @param exid
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  @Override
  public void markAudioDefect(AudioAttribute audioAttribute, HasID exid) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasQCPerm(userIDFromSessionOrDB)) {
      logger.debug("markAudioDefect mark audio defect for " + exid + " on " + audioAttribute);
      //CommonExercise before = db.getCustomOrPredefExercise(exid);  // allow custom items to mask out non-custom items
      //int beforeNumAudio = before.getAudioAttributes().size();
      db.markAudioDefect(audioAttribute);

      CommonExercise byID = db.getCustomOrPredefExercise(getProjectIDFromUser(userIDFromSessionOrDB), exid.getID());  // allow custom items to mask out non-custom items

      if (!byID.getMutableAudio().removeAudio(audioAttribute)) {
        String key = audioAttribute.getKey();
        logger.warn("markAudioDefect huh? couldn't remove key '" + key +
                "' : " + audioAttribute + " from ex #" + exid +
                "\n\tkeys were " + byID.getAudioRefToAttr().keySet()
            //    + " contains " + byID.getAudioRefToAttr().containsKey(key)
        );
      }
    /*   int afterNumAudio = byID.getAudioAttributes().size();
    if (afterNumAudio != beforeNumAudio - 1) {
      logger.error("\thuh? before there were " + beforeNumAudio + " but after there were " + afterNumAudio);
    }*/
    } else {
      throw getRestricted("marking audio defect");
    }
  }

  /**
   * This supports labeling really old audio for gender.
   * <p>
   * TODO : why think about attach audio???
   *
   * @param attr
   * @param isMale
   * @see mitll.langtest.client.qc.QCNPFExercise#getGenderGroup
   */
  @Override
  public void markGender(AudioAttribute attr, boolean isMale) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasQCPerm(userIDFromSessionOrDB)) {

      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(getProjectIDFromUser(), attr.getExid());
      int projid = -1;
      if (customOrPredefExercise == null) {
        logger.error("markGender can't find exercise id " + attr.getExid() + "?");
      } else {
        projid = customOrPredefExercise.getProjectID();
      }
      db.getAudioDAO().addOrUpdateUser(isMale ? BaseUserDAO.DEFAULT_MALE_ID : BaseUserDAO.DEFAULT_FEMALE_ID, projid, attr);

      int exid = attr.getExid();
      CommonExercise byID = db.getCustomOrPredefExercise(projid, exid);
      if (byID == null) {
        logger.error(getLanguage() + " : couldn't find exercise " + exid);
        logAndNotifyServerException(new Exception("couldn't find exercise " + exid));
      } else {
        // TODO : consider putting this back???
        //   byID.getAudioAttributes().clear();
//      logger.debug("re-attach " + attr + " given isMale " + isMale);

        // TODO : consider putting this back???
        //   attachAudio(byID);
/*
      String addr = Integer.toHexString(byID.hashCode());
      for (AudioAttribute audioAttribute : byID.getAudioAttributes()) {
        logger.debug("markGender 1 after gender change, now " + audioAttribute + " : " +audioAttribute.getUserid() + " on " + addr);
      }
*/
    //    db.getExerciseDAO(getProjectIDFromUser()).addOverlay(byID);

/*      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(exid);
      String adrr3 = Integer.toHexString(customOrPredefExercise.hashCode());
      logger.info("markGender getting " + adrr3 + " : " + customOrPredefExercise);
      for (AudioAttribute audioAttribute : customOrPredefExercise.getAudioAttributes()) {
        logger.debug("markGender 2 after gender change, now " + audioAttribute + " : " +audioAttribute.getUserid() + " on "+ adrr3);
      }*/

      }
      getSectionHelper().refreshExercise(byID);
    } else {
      throw getRestricted("marking gender");
    }
  }
}
