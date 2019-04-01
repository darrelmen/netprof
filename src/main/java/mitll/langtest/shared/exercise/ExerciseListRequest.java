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

package mitll.langtest.shared.exercise;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.qc.MarkDefectsChapterNPFHelper;
import mitll.langtest.client.custom.dialog.SearchTypeahead;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.server.services.ExerciseServiceImpl;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ExerciseListRequest implements IsSerializable, IRequest {
  private int reqID = 1;
  private Map<String, Collection<String>> typeToSelection = new HashMap<>();
  private String prefix = "";
  private int userListID = -1;

  private int userID = -5;

  private ActivityType activityType = ActivityType.UNSET;

  // TODO : which of these are mutually exclusive???
  private boolean onlyUnrecordedByMe = false;
  private boolean onlyExamples = false;
  private boolean onlyWithAnno = false;
  private boolean onlyUninspected = false;
  private boolean onlyForUser = false;
  private boolean addFirst = true;
  private int limit = -1;
  private boolean QC = false;
  private boolean addContext = false;
  private boolean plainVocab = false;
  private boolean isOnlyFL = false;
  private boolean exactMatch = false;

  private int dialogID = -1;
  private ProjectMode mode = ProjectMode.VOCABULARY;

  public ExerciseListRequest() {
  }

  public ExerciseListRequest(int reqID, int userID) {
    this.reqID = reqID;
    this.userID = userID;
  }

  public boolean isNoFilter() {
    return userListID == -1 &&
        limit == -1 &&
        typeToSelection.isEmpty() &&
        prefix.isEmpty() &&
        !isFilterActivity(activityType) &&
        !onlyUnrecordedByMe &&
        !onlyExamples &&
        !onlyWithAnno &&
        !onlyUninspected &&
        !onlyForUser;
  }

  private boolean isFilterActivity(ActivityType activityType) {
    return activityType == ActivityType.RECORDER || activityType == ActivityType.MARK_DEFECTS;
  }

  /**
   * TODO something less error prone
   *
   * @param other
   * @return
   */
  public boolean sameAs(ExerciseListRequest other) {
    return prefix.equals(other.getPrefix()) &&
        typeToSelection.equals(other.getTypeToSelection()) &&
        onlyUnrecordedByMe == other.onlyUnrecordedByMe &&
        onlyExamples == other.onlyExamples &&
        onlyWithAnno == other.onlyWithAnno &&
        onlyUninspected == other.onlyUninspected &&
        userListID == other.userListID;
  }

  public int getReqID() {
    return reqID;
  }

  public Map<String, Collection<String>> getTypeToSelection() {
    return typeToSelection;
  }

  /**
   * @param typeToSelection
   * @return
   * @see HistoryExerciseList#loadExercisesUsingPrefix
   */
  public ExerciseListRequest setTypeToSelection(Map<String, Collection<String>> typeToSelection) {
    this.typeToSelection = typeToSelection;
    return this;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  public ExerciseListRequest setPrefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  public int getUserListID() {
    return userListID;
  }

  public ExerciseListRequest setUserListID(int userListID) {
    this.userListID = userListID;
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.FilterResponseHelper#filterExercises
   */
  public int getUserID() {
    return userID;
  }

  /**
   * @return
   * @see ExerciseServiceImpl#getExerciseIds
   */
  public ActivityType getActivityType() {
    return activityType;
  }

  /**
   * @param activityType
   * @return
   * @see PagingExerciseList#getExerciseListRequest
   */
  public ExerciseListRequest setActivityType(ActivityType activityType) {
    this.activityType = activityType;
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.FilterResponseHelper#filterExercises
   */
  public boolean isOnlyUnrecordedByMe() {
    return onlyUnrecordedByMe;
  }

  public ExerciseListRequest setOnlyUnrecordedByMe(boolean onlyUnrecordedByMe) {
    this.onlyUnrecordedByMe = onlyUnrecordedByMe;
    return this;
  }

  /**
   * @return
   */
  public boolean isOnlyExamples() {
    return onlyExamples;
  }

  /**
   * @param onlyExamples
   * @return
   * @see HistoryExerciseList#getExerciseListRequest
   */
  public ExerciseListRequest setOnlyExamples(boolean onlyExamples) {
    this.onlyExamples = onlyExamples;
    return this;
  }

  public boolean isOnlyWithAnno() {
    return onlyWithAnno;
  }

  public ExerciseListRequest setOnlyWithAnno(boolean onlyWithAnno) {
    this.onlyWithAnno = onlyWithAnno;
    return this;
  }

  public boolean isOnlyUninspected() {
    return onlyUninspected;
  }

  /**
   * @param onlyDefaultAudio
   * @return
   * @see mitll.langtest.client.custom.DefectsExerciseList#getExerciseListRequest(String)
   */
  public ExerciseListRequest setOnlyUninspected(boolean onlyDefaultAudio) {
    this.onlyUninspected = onlyDefaultAudio;
    return this;
  }

  /**
   * @return
   * @see ExerciseServiceImpl#getFirstFew
   */
  @Override
  public int getLimit() {
    return limit;
  }

  /**
   * @param limit
   * @return
   * @see SearchTypeahead#getTypeaheadUsing
   */
  public ExerciseListRequest setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public boolean isAddFirst() {
    return addFirst;
  }

  public ExerciseListRequest setAddFirst(boolean addFirst) {
    this.addFirst = addFirst;
    return this;
  }

  /**
   * @return
   * @see ExerciseServiceImpl#makeExerciseListWrapper
   */
  public boolean isQC() {
    return QC;
  }

  /**
   * @param QC
   * @return
   * @see MarkDefectsChapterNPFHelper#getMyListLayout
   */
  public ExerciseListRequest setQC(boolean QC) {
    this.QC = QC;
    return this;
  }

  public boolean shouldAddContext() {
    return addContext;
  }

  /**
   * @param QC
   * @return
   * @see HistoryExerciseList#getExerciseListRequest
   * @see mitll.langtest.client.custom.content.ReviewItemHelper.ReviewFlexListLayout#makeExerciseList(Panel, Panel, INavigation.VIEWS, DivWidget, DivWidget)
   */
  public ExerciseListRequest setAddContext(boolean QC) {
    this.addContext = QC;
    return this;
  }

  public ExerciseListRequest setOnlyPlainVocab(boolean plainVocab) {
    this.plainVocab = plainVocab;
    return this;
  }

  /**
   * @return
   * @see ExerciseServiceImpl#getExerciseWhenNoUnitChapter
   */
  public boolean isPlainVocab() {
    return plainVocab;
  }


  /**
   * @return
   */
  public boolean isOnlyFL() {
    return isOnlyFL;
  }

  /**
   * @param onlyExamples
   * @return
   * @see HistoryExerciseList#getExerciseListRequest
   */
  public ExerciseListRequest setOnlyFL(boolean isOnlyFL) {
    this.isOnlyFL = isOnlyFL;
    return this;
  }

  public ExerciseListRequest setUserID(int userID) {
    this.userID = userID;
    return this;
  }

  public ExerciseListRequest setDialogID(int dialogID) {
    this.dialogID = dialogID;
    return this;
  }

  public int getDialogID() {
    return dialogID;
  }

  public ExerciseListRequest setReqID(int currentReq) {
    this.reqID = currentReq;
    return this;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.exercise.FilterResponseHelper#getSectionHelperFromFiltered
   */
//  public ProjectType getProjectType() {
//    return projectType;
//  }

//  public ExerciseListRequest setProjectType(ProjectType projectType) {
//    this.projectType = projectType;
//    return this;
//  }
  @Override
  public ProjectMode getMode() {
    return mode;
  }

  public ExerciseListRequest setMode(ProjectMode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * @return
   */
  public String toString() {
    return
        "req #" + getReqID() + " " +
            (limit == -1 ? "" : "\n\tlimit                  '" + limit + "'") +
            (prefix.isEmpty() ? "" : "\n\tprefix                  '" + prefix + "'") +
            (getTypeToSelection().isEmpty() ? "" : "\n\tselection           " + getTypeToSelection()) +
            (userListID != -1 ? "\n\tuser list id        " + userListID : "") +
            "\n\tuser                " + userID +
            (dialogID != -1 ? "\n\tdialog              " + dialogID : "") +
            (activityType == ActivityType.UNSET ? "" :
                "\n\tactivity             " + activityType) +
            (onlyUnrecordedByMe ? "\n\tonly recorded by me" : "") +
            (onlyExamples ? "\n\tonly examples       " : "") +
            (onlyWithAnno ? "\n\tonly with anno " : "") +
            (onlyForUser ? "\n\tonlyForUser     " : "") +
            (exactMatch ? "\n\texactMatch     " : "") +
            //   (incorrectFirstOrder ? "\n\tincorrectFirstOrder     " : "") +
            (onlyUninspected ? "\n\tonly uninspected    " : "") +
            (addContext ? "\n\tadd context    " : "") +
            "\n\tmode    " + mode +
            (addFirst ? "\n\tadd first ex    " : "\n\tdon't add first") +
            (QC ? "\n\tqc request    " : "")
        ;
  }

  public boolean isExactMatch() {
    return exactMatch;
  }

  public void setExactMatch(boolean exactMatch) {
    this.exactMatch = exactMatch;
  }
}
