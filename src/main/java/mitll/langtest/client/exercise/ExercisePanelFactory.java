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

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.NewBanner;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.ShowChoices;
import mitll.langtest.shared.exercise.Shell;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 7/9/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ExercisePanelFactory<T extends Shell, U extends Shell> {
  private final Logger logger = Logger.getLogger("ExercisePanelFactory");
  private static final String SHOW_PHONES = "showPhones";

  protected final ExerciseController controller;
  protected ListInterface<T, U> exerciseList;

  /**
   * @param controller
   * @param exerciseList
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   */
  public ExercisePanelFactory(final ExerciseController controller,
                              ListInterface<T, U> exerciseList) {
    this.controller = controller;
    this.exerciseList = exerciseList;
  }

  public void setExerciseList(ListInterface<T, U> exerciseList) {
    this.exerciseList = exerciseList;
  }

  /**
   * @param e
   * @return
   * @seex mitll.langtest.client.list.ExerciseList#makeExercisePanel
   */
  public abstract Panel getExercisePanel(U e);

  @NotNull
  public ShowChoices getChoices() {
    ShowChoices choices = ShowChoices.FL;
    String show = controller.getStorage().getValue(NewBanner.SHOW);
    if (show != null) {
      // logger.warning("value for show " + controller.getStorage().getValue("show"));
      try {
        choices = ShowChoices.valueOf(show);
        // logger.info("ExercisePanelFactory got " + choices);
      } catch (IllegalArgumentException ee) {
        // logger.warning("got " + ee);
      }
    }
    return choices;
  }

  @NotNull
  public PhonesChoices getPhoneChoices() {
    PhonesChoices choices = PhonesChoices.SHOW;
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
}
