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
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.recording.RecorderNPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ExerciseList;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.Reloadable;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.HashMap;
import java.util.Map;
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
public abstract class SimpleChapterNPFHelper<T extends CommonShell, U extends CommonShell>
    implements RequiresResize, ExerciseListContent {
  private final Logger logger = Logger.getLogger("SimpleChapterNPFHelper");

  public static final int RIGHT_SIDE_MARGIN = 155;
  private boolean madeNPFContent = false;

  protected final ExerciseController controller;
  private ExerciseList npfExerciseList;
  protected final FlexListLayout<T, U> flexListLayout;
//  private   IViewContaner viewContaner;
//  private INavigation.VIEWS myView;

  /**
   * @param controller
   * @param viewContaner
   * @param myView
   * @see RecorderNPFHelper#RecorderNPFHelper(ExerciseController, boolean, IViewContaner, INavigation.VIEWS)
   */
  public SimpleChapterNPFHelper(ExerciseController controller, IViewContaner viewContaner, INavigation.VIEWS myView) {
    this.controller = controller;
    final SimpleChapterNPFHelper<T, U> outer = this;
    this.flexListLayout = getMyListLayout(outer);
//    this.viewContaner = viewContaner;
//    this.myView = myView;
  }

  protected abstract FlexListLayout<T, U> getMyListLayout(SimpleChapterNPFHelper<T, U> outer);

  /**
   * Add npf widget to content of a tab - here marked tabAndContent
   *
   * @param instanceName flex, review, etc.
   * @see NewContentChooser#showView
   */
  public void showNPF(DivWidget content, String instanceName) {
  //   logger.info(getClass() + " : adding npf content instanceName = " + instanceName);//+ " loadExercises " + loadExercises);
    if (!madeNPFContent || content.getWidgetCount() == 0) {
      madeNPFContent = true;
    //  logger.info("\t: showNPF : adding npf content instanceName = " + instanceName);
      showContent(content, instanceName, true);
      npfExerciseList.reloadWithCurrent();
    } else {
      logger.warning("showNPF not doing anything for " + instanceName);
    }
  }

  /**
   * @param listContent
   * @param instanceName
   * @param fromClick
   * @see #showNPF
   */
  @Override
  public void showContent(Panel listContent, String instanceName, boolean fromClick) {
    //logger.info(getClass() + " : showContent instanceName = " + instanceName);//+ " loadExercises " + loadExercises);
    Panel child = doNPF(instanceName);

/*
    logger.info("showContent - (" + instanceName + " ) " +
        myView + " vs " + viewContaner.getCurrentView() +
        " adding " + child.getElement().getId() + " to " + listContent.getElement().getId() + " with " + listContent.getElement().getChildCount());
*/

    listContent.add(child);
 /*   if (fromClick) {
      logger.info(getClass() + " : END showContent instanceName = " + instanceName);//+ " loadExercises " + loadExercises);
    }*/
  }

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param instanceName
   * @return
   * @see ContentView#showContent
   */
  private Panel doNPF(String instanceName) {
   // logger.info(getClass() + " : doNPF instanceName = " + instanceName);
    Panel widgets = flexListLayout.doInternalLayout(-1, instanceName, false);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  /**
   * @return
   * @seez SimpleChapterNPFHelper#getCreatedPanel
   */
  public ListInterface<?, ?> getExerciseList() {
    return npfExerciseList;
  }

  @Override
  public Reloadable getReloadable() {
    return npfExerciseList;
  }

  /**
   * @see NewContentChooser#showPractice
   */
  @Override
  public void hideList() {
    npfExerciseList.hide();
  }

  public void loadExercise(int exid) {
    npfExerciseList.loadExercise(exid);
  }

  /**
   * This is important - this is where the actual content is chosen.
   *
   * @param exerciseList
   * @return
   * @see mitll.langtest.client.exercise.WaveformExercisePanel
   */
  protected abstract ExercisePanelFactory<T, U> getFactory(final PagingExerciseList<T, U> exerciseList);

  protected ExercisePanelFactory<CommonShell, ClientExercise> getFactoryInject(
      final PagingExerciseList<CommonShell, ClientExercise> exerciseList,
      PanelFactory panelFactory) {
    return new ExercisePanelFactory<CommonShell, ClientExercise>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(ClientExercise e) {
        return panelFactory.getExercisePanel(e);
      }
    };
  }

  public interface PanelFactory {
    Panel getExercisePanel(ClientExercise e);
  }


  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    }
  }

  protected abstract static class MyFlexListLayout<T extends CommonShell, U extends CommonShell> extends FlexListLayout<T, U> {
    private final SimpleChapterNPFHelper<T, U> outer;
    //private final Logger logger = Logger.getLogger("MyFlexListLayout");

    /**
     * @param controller
     * @param outer
     */
    protected MyFlexListLayout(ExerciseController controller, SimpleChapterNPFHelper<T, U> outer) {
      super(controller);
      this.outer = outer;
    }

    /**
     * @param exerciseList
     * @return
     * @see FlexListLayout#makeNPFExerciseList
     */
    @Override
    protected ExercisePanelFactory<T, U> getFactory(PagingExerciseList<T, U> exerciseList) {
      return outer.getFactory(exerciseList);
    }
  }
}
