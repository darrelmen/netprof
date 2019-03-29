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

package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.banner.NewBanner;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.user.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static mitll.langtest.shared.project.ProjectMode.EITHER;
import static mitll.langtest.shared.project.ProjectMode.VOCABULARY;
import static mitll.langtest.shared.user.User.Permission.*;

/**
 * Closely related to {@link mitll.langtest.shared.user.User.Permission}
 * Created by go22670 on 4/10/17.
 */
public interface INavigation extends IViewContaner {
  enum VIEWS {
    NONE("", EITHER),

    LISTS("Lists", VOCABULARY),
    /**
     *
     */
    PROGRESS("Progress", VOCABULARY),

    LEARN("Learn", VOCABULARY),
    PRACTICE("Practice", VOCABULARY),
    QUIZ("Quiz", VOCABULARY),


    DIALOG("Dialogs", ProjectMode.DIALOG),
    /**
     * @see mitll.langtest.client.banner.DialogExerciseList#gotClickOnDialog
     * @see mitll.langtest.client.banner.NewContentChooser#showView
     */
    STUDY("Study", ProjectMode.DIALOG),
    LISTEN("Listen", ProjectMode.DIALOG),

    REHEARSE("Rehearse", ProjectMode.DIALOG, true),
    CORE_REHEARSE("Rehearse (auto play)", ProjectMode.DIALOG, false),

    PERFORM_PRESS_AND_HOLD("Perform", ProjectMode.DIALOG, true),
    PERFORM("Perform (auto play)", ProjectMode.DIALOG, false),

    /**
     * @see mitll.langtest.client.banner.NewContentChooser#showScores
     */
    SCORES("Scores", ProjectMode.DIALOG),

    /**
     * @see NewBanner#getRecNav
     */
    RECORD_ENTRIES("Record Entries", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    RECORD_SENTENCES("Record Sentences", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),


    QC_ENTRIES("QC Entries", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), true, false, false),
    FIX_ENTRIES("Fix Entries", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), false, true, false),
    QC_SENTENCES("QC Sentences", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), true, false, true),
    FIX_SENTENCES("Fix Sentences", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), false, true, true),

    OOV_EDITOR("Missing In Dictionary", VOCABULARY);

    private final List<User.Permission> perms;
    private final ProjectMode mode;
    private boolean isQC;
    private boolean isFix;
    private boolean isContext;
    private boolean isPressAndHold;

    final String display;

    VIEWS(String display, List<User.Permission> perms) {
      this.display = display;
      this.perms = perms;
      this.mode = EITHER;
    }

    VIEWS(String display, List<User.Permission> perms, boolean isQC, boolean isFix, boolean isContext) {
      this.display = display;
      this.perms = perms;
      this.mode = EITHER;
      this.isQC = isQC;
      this.isFix = isFix;
      this.isContext = isContext;
    }

    VIEWS(String display, ProjectMode mode) {
      this.display = display;
      this.perms = Collections.emptyList();
      this.mode = mode;
    }

    VIEWS(String display, ProjectMode mode, boolean isPressAndHold) {
      this.display = display;
      this.perms = Collections.emptyList();
      this.mode = mode;
      this.isPressAndHold = isPressAndHold;
    }

    public VIEWS getPrev() {
      VIEWS[] vals = values();
      return vals[(this.ordinal() - 1) % vals.length];
    }

    public VIEWS getNext() {
      VIEWS[] vals = values();
      return vals[(this.ordinal() + 1) % vals.length];
    }

    public List<User.Permission> getPerms() {
      return perms;
    }

    public ProjectMode getMode() {
      return mode;
    }

    public String toString() {
      return display;
    }

    public boolean isQC() {
      return isQC;
    }

    public boolean isFix() {
      return isFix;
    }

    public boolean isContext() {
      return isContext;
    }

    public boolean isPressAndHold() {
      return isPressAndHold;
    }

  }

  void storeViewForMode(ProjectMode mode);

  void show(VIEWS views);

  void showView(VIEWS view);

  void showView(VIEWS view, boolean isFirstTime, boolean fromClick);

  void showInitialState();

  void showListIn(int listid, VIEWS view);

  void showDialogIn(int dialogid, VIEWS view);

  /**
   * @param views
   * @return
   */
  ShowTab getShowTab(VIEWS views);

  Widget getNavigation();

  void setBannerVisible(boolean visible);

  void onResize();

  void showPreviousState();

  void clearCurrent();

  VIEWS getCurrent();
}
