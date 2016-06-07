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
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.WavCallback;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioType;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.User;

import java.util.Collection;

/**
 * Common services for UI components.
 *
 * TODO :  This could be made less of a grab bag of stuff - break into interfaces, etc.
 *
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/9/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseController extends EventRegistration {
  LangTestDatabaseAsync getService();
  UserFeedback getFeedback();

  String getBrowserInfo();

  int getUser();
  boolean isTeacher();

  void startRecording();
  void stopRecording(WavCallback wavCallback);

  SoundManagerAPI getSoundManager();

  boolean showOnlyOneExercise();

  boolean useBkgColorForRef();

  int getRecordTimeout();

  boolean isLogClientMessages();
  AudioType getAudioType();
  Collection<User.Permission> getPermissions();

  boolean showCompleted();

  void getImage(int reqid, String path, String type, int toUse, int height, String exerciseID, AsyncCallback<ImageResponse> client);

  String getLanguage();
  boolean isRightAlignContent();
  int getHeightOfTopRows();

  PropertyHandler getProps();

  String logException(Throwable throwable);
  void logMessageOnServer(String message, String prefix);

  StartupInfo getStartupInfo();
  Collection<String> getTypeOrder();

  void addKeyListener(KeyPressHelper.KeyListener listener);

  boolean isRecordingEnabled();

  boolean usingFlashRecorder();

  void checkUser();

  boolean isMicAvailable();
}
