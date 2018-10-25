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

package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.audio.NativeAudioResult;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.security.IUserSecurityManager;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 6/26/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Database extends AutoCloseable {
  String TIME = "time";
  String EXID = "exid";

  Connection getConnection(String who);

  void closeConnection(Connection connection);

  void logEvent(String exid, String context, int userid, String device, int projID);

  ServerProperties getServerProps();

  @Deprecated
  String getLanguage();

  Project getProject(int projectid);

  void dropProject(int projID);

  String getWebPageAudioRef(String language, String path);

  String getRelPrefix(String language);

  String getWebPageAudioRefWithPrefix(String relPrefix, String path);

  LogAndNotify getLogAndNotify();

  Collection<String> getTypeOrder(int projectid);

  /**
   * @see IUserSecurityManager#setSessionUser
   * @param userWhere
   */
  void setStartupInfo(User userWhere);

  CommonExercise getCustomOrPredefExercise(int projid, int id);

  /**
   *
   * TODO : Why called from phone report too?
   *
   * @param userToGender
   * @param userid
   * @param exid
   * @param project
   * @param idToMini
   * @return
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getUserToResults
   * @see mitll.langtest.server.database.phone.SlickPhoneDAO#getPhoneReport
   */
  NativeAudioResult getNativeAudio(Map<Integer, MiniUser.Gender> userToGender, int userid, int exid, Project project, Map<Integer, MiniUser> idToMini);
}
