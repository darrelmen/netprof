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

package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.list.DisplayMenu;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.scoring.AlignmentAndScore;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.logging.Logger;

import static mitll.langtest.client.list.DisplayMenu.SHOW_PHONES;

public abstract class ExercisePanelFactory<T extends HasID, U extends HasID> {
  private final Logger logger = Logger.getLogger("ExercisePanelFactory");
  /**
   * Fix for bug #115 : <a href='https://gh.ll.mit.edu/DLI-LTEA/netprof2/issues/115'>Bug #115 : Remove the transliteration </a>
   */
  private final PhonesChoices SHOW_CHOICE_DEFAULT = PhonesChoices.HIDE;

  protected final ExerciseController controller;
  protected ListInterface<T, U> exerciseList;

  /**
   * @param controller
   * @param exerciseList
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   */
  public ExercisePanelFactory(final ExerciseController controller, ListInterface<T, U> exerciseList) {
    this.controller = controller;
    this.exerciseList = exerciseList;
  }

  public void addToCache(Map<Integer, AlignmentAndScore> toAdd){}

  /**
   * @param e
   * @return
   * @see mitll.langtest.client.list.ExerciseList#addExerciseWidget
   */
  public abstract Panel getExercisePanel(U e);

  @NotNull
  public boolean getFLChoice() {
    String show = controller.getStorage().getValue(DisplayMenu.SHOWFL);
    Boolean showFL = true;
    if (show != null) {
      showFL = Boolean.valueOf(show);
    }
    return showFL;
  }

  @NotNull
  public boolean getALTFLChoice() {
    String show = controller.getStorage().getValue(DisplayMenu.SHOWALT);
    Boolean showFL = false;
    if (show != null) {
      showFL = Boolean.valueOf(show);
    }
    return showFL;
  }

  @NotNull
  public PhonesChoices getPhoneChoices() {
    PhonesChoices choices = SHOW_CHOICE_DEFAULT;

    String show = controller.getStorage().getValue(SHOW_PHONES);
    if (show != null && !show.isEmpty()) {
      try {
        choices = PhonesChoices.valueOf(show);
        //    logger.info("ExercisePanelFactory got " + choices);
      } catch (IllegalArgumentException ee) {
        logger.warning("getPhoneChoices for '" + show + "' got " + ee);
      }
    }
    return choices;
  }

  public ListInterface<T, U> getExerciseList() {
    return exerciseList;
  }
}
