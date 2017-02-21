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

package mitll.langtest.client.services;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.List;

@RemoteServiceRelativePath("list-manager")
public interface ListService extends RemoteService {
  /**
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#addUserList(BasicDialog.FormField, TextArea, BasicDialog.FormField, boolean)
   */
  long addUserList(String name, String description, String dliClass, boolean isPublic);

  /**
   * @param userListID
   * @param isPublic
   * @see mitll.langtest.client.custom.ListManager#setPublic(long, boolean)
   */
  void setPublicOnList(long userListID, boolean isPublic);

  // Deleting lists and exercises from lists

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.ListManager#deleteList(Button, UserList, boolean)
   */
  boolean deleteList(long id);

  /**
   * @param listid
   * @param exid
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#deleteItem(String, long, UserList, PagingExerciseList, ReloadableContainer)
   */
  boolean deleteItemFromList(long listid, int exid);
  /**
   * @see ReviewEditableExercise#confirmThenDeleteItem()
   * @param exid
   * @return
   */
  // boolean deleteItem(String exid);

  /**
   * @param userListID
   * @param user
   * @see mitll.langtest.client.custom.ListManager#addVisitor(UserList)
   */
  void addVisitor(long userListID, int user);

  /**
   * @param onlyCreated
   * @param visited
   * @return
   * @see mitll.langtest.client.custom.ListManager#viewLessons
   */
  Collection<UserList<CommonShell>> getListsForUser(boolean onlyCreated, boolean visited);

  /**
   * @param search
   * @return
   * @see mitll.langtest.client.custom.ListManager#viewLessons
   */
  Collection<UserList<CommonShell>> getUserListsForText(String search);

  /**
   * @return
   * @see mitll.langtest.client.custom.ListManager#viewReview(Panel)
   */
  List<UserList<CommonShell>> getReviewLists();

  /**
   * @param userListID
   * @param exID
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  void addItemToUserList(long userListID, int exID);

  /**
   * @param userListID
   * @param userExerciseText
   * @return
   * @see mitll.langtest.client.custom.ListManager#showImportItem(UserList, TabAndContent, TabAndContent, String, TabPanel)
   */
  Collection<CommonExercise> reallyCreateNewItems(long userListID, String userExerciseText);

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise(Button)
   */
  CommonExercise duplicateExercise(CommonExercise id);

  /**
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  void editItem(CommonExercise userExercise, boolean keepAudio);
}
