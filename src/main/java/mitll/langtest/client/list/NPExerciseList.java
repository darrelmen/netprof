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

package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/5/16.
 */
public class NPExerciseList<T extends CommonShell, U extends Shell> extends HistoryExerciseList<T, U> {
  private Logger logger = Logger.getLogger("NPExerciseList");
  private final int pageSize;

  public NPExerciseList(Panel currentExerciseVPanel, ExerciseController controller,
                        ListOptions listOptions, int pageSize) {
    super(currentExerciseVPanel, controller, listOptions);
    this.pageSize = pageSize;
  }

  @Override
  protected FacetContainer getSectionWidgetContainer() {
    return new FacetContainer() {
      @Override
      public void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder) {
      }

      @Override
      public String getHistoryToken() {
        return null;
      }

      @Override
      public int getNumSelections() {
        return 0;
      }
    };
  }

  /**
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#addComponents
   */
  protected ClickablePagingContainer<T> makePagingContainer() {
    final PagingExerciseList<?, ?> outer = this;
    if (logger == null) {
      logger = Logger.getLogger("NPExerciseList");
    }

/*
    logger.info("makePagingContainer : for" +
        "\n\tinstance " + getInstance() +
        "\n\tshow first not complete " + showFirstNotCompleted +
        "\n\tactivityType " + getActivityType());
*/
    boolean isRecorder = getActivityType() == ActivityType.RECORDER;
    final boolean showFirstNotCompleted = listOptions.isShowFirstNotCompleted();

    pagingContainer = new NPExerciseListContainer(this, isRecorder, showFirstNotCompleted);
    return pagingContainer;
  }

  int getPageSize() {
    return pageSize;
  }

  @Override
  public boolean isCurrentReq(int req) {
    return true;
  }
}
