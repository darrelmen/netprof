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

package mitll.langtest.client.exercise;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.initial.WavCallback;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.services.OpenUserServiceAsync;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.User;

import java.util.Collection;

/**
 * Common services for UI components.
 * <p>
 * TODO :  This could be made less of a grab bag of stuff - break into interfaces, etc.
 * <p>
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/9/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseController extends Services, ExceptionSupport {
  UserManager getUserManager();

  UserFeedback getFeedback();

  String getBrowserInfo();

  UserState getUserState();

  int getUser();

  void startRecording();

  void stopRecording(WavCallback wavCallback);

  int getRecordTimeout();

  SoundManagerAPI getSoundManager();

  boolean useBkgColorForRef();

  boolean isLogClientMessages();

  void reallySetTheProject(int projectid);

  Collection<User.Permission> getPermissions();

  void getImage(int reqid, String path, String type, int toUse, int height, int exerciseID, AsyncCallback<ImageResponse> client);

  String getLanguage();

  boolean isRightAlignContent();

  int getHeightOfTopRows();

  PropertyHandler getProps();


  ProjectStartupInfo getProjectStartupInfo();

  boolean hasModel();

  Collection<String> getTypeOrder();

  void addKeyListener(KeyPressHelper.KeyListener listener);

  boolean isRecordingEnabled();

  boolean usingFlashRecorder();

  boolean isMicAvailable();

  KeyStorage getStorage();

  void showLearnList(int id);
  void showDrillList(int id);

  CommentAnnotator getCommentAnnotator();

  MessageHelper getMessageHelper();
}
