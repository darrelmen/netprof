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

package mitll.langtest.server.database;

import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.dialog.IDialogDAO;
import mitll.langtest.server.database.dialog.IDialogSessionDAO;
import mitll.langtest.server.database.dialog.IRelatedResultDAO;
import mitll.langtest.server.database.dliclass.IDLIClassJoinDAO;
import mitll.langtest.server.database.exercise.IOOVDAO;
import mitll.langtest.server.database.image.IImageDAO;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.phone.IPhoneDAO;
import mitll.langtest.server.database.phone.Phone;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.result.IAnswerDAO;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.user.IPendingUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.IUserProjectDAO;
import mitll.langtest.server.database.user.IUserSessionDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.word.IWordDAO;

public interface DAOContainer {
  IResultDAO getResultDAO();

  IRefResultDAO getRefResultDAO();

  IUserDAO getUserDAO();

  IUserExerciseDAO getUserExerciseDAO();

  IProjectDAO getProjectDAO();

  IUserProjectDAO getUserProjectDAO();

  IUserSessionDAO getUserSessionDAO();

  IImageDAO getImageDAO();

  IWordDAO getWordDAO();

  IPhoneDAO<Phone> getPhoneDAO();

  IAnswerDAO getAnswerDAO();

  IEventDAO getEventDAO();

  IAudioDAO getAudioDAO();

  IDialogDAO getDialogDAO();

  IDialogSessionDAO getDialogSessionDAO();

  IRelatedResultDAO getRelatedResultDAO();

  IDLIClassJoinDAO getDliClassJoinDAO();

  IPendingUserDAO getPendingUserDAO();

  IOOVDAO getOOVDAO();
}
