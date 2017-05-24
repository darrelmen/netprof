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

package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.list.ExerciseList;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.Reloadable;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;

import java.util.logging.Logger;

/**
 * Lets you show a user list with a paging container...
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SimpleChapterNPFHelper<T extends CommonShell, U extends CommonExercise>
    implements ReloadableContainer, RequiresResize, ExerciseListContent {
  public static final int RIGHT_SIDE_MARGIN = 155;//120;
  private final Logger logger = Logger.getLogger("SimpleChapterNPFHelper");
  private boolean madeNPFContent = false;

  protected final ExerciseController controller;
  private ExerciseList npfExerciseList;
  private final ReloadableContainer predefinedContentList;

  /**
   * @param controller
   * @see Navigation#Navigation
   */
  public SimpleChapterNPFHelper(ExerciseController controller,
                                ReloadableContainer predefinedContentList) {
    this.controller = controller;
    this.predefinedContentList = predefinedContentList;

    final SimpleChapterNPFHelper<T, U> outer = this;
    this.flexListLayout = getMyListLayout(outer);
  }

  Panel getCreatedPanel() {
    return getExerciseList() != null ? getExerciseList().getCreatedPanel() : null;
  }

  protected abstract FlexListLayout<T, U> getMyListLayout(SimpleChapterNPFHelper<T, U> outer);

  /**
   * Add npf widget to content of a tab - here marked tabAndContent
   *
   * @param instanceName flex, review, etc.
   * @paramx tabAndContent in this tab
   * @paramx activityType
   * @see Navigation#addPracticeTab()
   * @see Navigation#addTabs
   * @see mitll.langtest.client.custom.Navigation#selectPreviousTab
   */
  public void showNPF(DivWidget content, String instanceName) {
    // logger.info(getClass() + " : adding npf content instanceName = " + instanceName);//+ " loadExercises " + loadExercises);
    // DivWidget content = tabAndContent.getContent();
    if (!madeNPFContent || content.getWidgetCount() == 0) {
      madeNPFContent = true;
      logger.info("\t: adding npf content instanceName = " + instanceName);
      showContent(content, instanceName);
      npfExerciseList.reloadWithCurrent();
    } else {
      logger.warning("showNPF not doing anything for " + instanceName);
    }
  }

  /**
   * @param listContent
   * @param instanceName
   * @see #showNPF
   */
  @Override
  public void showContent(Panel listContent, String instanceName) {
    listContent.add(doNPF(instanceName));
   // listContent.addStyleName("userListBackground");
   // listContent.getElement().getStyle().setMarginRight(RIGHT_SIDE_MARGIN, Style.Unit.PX);
  }

  private final FlexListLayout<T, U> flexListLayout;

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param instanceName
   * @return
   * @see #showContent(Panel, String)
   */
  private Panel doNPF(String instanceName) {
   // logger.info(getClass() + " : doNPF instanceName = " + instanceName);
    Panel widgets = flexListLayout.doInternalLayout(-1, instanceName, false);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  /**
   * @return
   * @see SimpleChapterNPFHelper#getCreatedPanel
   */
  public ListInterface<?,?> getExerciseList() {
    return npfExerciseList;
  }

  @Override
  public Reloadable getReloadable() {
    return npfExerciseList;
  }

  /**
   * @see mitll.langtest.client.banner.NewContentChooser#showView(INavigation.VIEWS)
   */
  @Override
  public void hideList() {
    npfExerciseList.hide();
  }

  public void loadExercise(int exid) { npfExerciseList.loadExercise(exid);  }

  public void showList(int userlistID) {

  }

  /**
   * This is important - this is where the actual content is chosen.
   *
   * @param exerciseList
   * @return
   * @see mitll.langtest.client.exercise.WaveformExercisePanel
   */
  protected ExercisePanelFactory<T, U> getFactory(final PagingExerciseList<T, U> exerciseList) {
    final String instance = exerciseList.getInstance();
    return new ExercisePanelFactory<T, U>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final U e) {
        return new WaveformExercisePanel<T, U>(e, controller, exerciseList, true, instance) {
          @Override
          public void postAnswers(ExerciseController controller, HasID completedExercise) {
            super.postAnswers(controller, completedExercise);
            tellOtherListExerciseDirty(e);
          }
        };
      }
    };
  }

  /**
   * TODO : make sure this is doing something helpful
   *
   * @param e
   * @see RecorderNPFHelper.MyWaveformExercisePanel#postAnswers(ExerciseController, HasID)
   */
  protected void tellOtherListExerciseDirty(HasID e) {
    if (predefinedContentList != null &&
        predefinedContentList.getReloadable() != null &&
        predefinedContentList.getReloadable().getCurrentExerciseID() != -1 &&
        e.getID() == predefinedContentList.getReloadable().getCurrentExerciseID()) {
      // logger.info("SimpleChapterNPFHelper.reloading " + e.getOldID());
      predefinedContentList.getReloadable().loadExercise(e.getID());
    } else {
      //    logger.info("\n\n\n--> SimpleChapterNPFHelper.not reloading " + e.getOldID());
    }
  }

  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    } else {
//      logger.info("SimpleChapterNPFHelper.onResize : not sending resize event - flexListLayout is null?");
    }
  }

  protected abstract static class MyFlexListLayout<T extends CommonShell, U extends CommonExercise> extends FlexListLayout<T, U> {
    private final SimpleChapterNPFHelper<T, U> outer;

    protected MyFlexListLayout(ExerciseController controller, SimpleChapterNPFHelper<T, U> outer) {
      super(controller);
      this.outer = outer;
    }

    @Override
    protected ExercisePanelFactory<T, U> getFactory(PagingExerciseList<T, U> exerciseList) {
      return outer.getFactory(exerciseList);
    }
  }
}
