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
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.list.ExerciseList;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.Reloadable;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.AudioRefExercise;
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
public abstract class SimpleChapterNPFHelper<T extends CommonShell, U extends CommonShell & AudioRefExercise>
    implements ReloadableContainer, RequiresResize {
  private final Logger logger = Logger.getLogger("SimpleChapterNPFHelper");
  private boolean madeNPFContent = false;

  protected final LangTestDatabaseAsync service;
  protected final ExerciseController controller;

  final UserFeedback feedback;
  private ExerciseList npfExerciseList;
  private final ReloadableContainer predefinedContentList;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @see Navigation#Navigation
   */
  public SimpleChapterNPFHelper(LangTestDatabaseAsync service,
                                UserFeedback feedback,
                                UserManager userManager,
                                ExerciseController controller,
                                ReloadableContainer predefinedContentList) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.predefinedContentList = predefinedContentList;

    final SimpleChapterNPFHelper<T, U> outer = this;
    this.flexListLayout = getMyListLayout(service, feedback, userManager, controller, outer);
  }

  public Panel getCreatedPanel() {
    return getExerciseList() != null ? getExerciseList().getCreatedPanel() : null;
  }

  protected abstract FlexListLayout<T, U> getMyListLayout(LangTestDatabaseAsync service,
                                                          UserFeedback feedback,
                                                          UserManager userManager, ExerciseController controller,
                                                          SimpleChapterNPFHelper<T, U> outer);

  /**
   * Add npf widget to content of a tab - here marked tabAndContent
   *
   * @param tabAndContent in this tab
   * @param instanceName  flex, review, etc.
   * @see Navigation#addPracticeTab()
   * @see Navigation#addTabs
   * @see mitll.langtest.client.custom.Navigation#selectPreviousTab
   */
  void showNPF(TabAndContent tabAndContent, String instanceName) {
    // logger.info(getClass() + " : adding npf content instanceName = " + instanceName);//+ " loadExercises " + loadExercises);
    DivWidget content = tabAndContent.getContent();
    if (!madeNPFContent || content.getWidgetCount() == 0) {
      madeNPFContent = true;
  //    logger.info("\t: adding npf content instanceName = " + instanceName);
      addNPFToContent(content, instanceName);
    }
  }

  /**
   * @param listContent
   * @param instanceName
   * @see #showNPF(TabAndContent, String)
   */
  protected void addNPFToContent(Panel listContent, String instanceName) {
    listContent.add(doNPF(instanceName));
    listContent.addStyleName("userListBackground");
  }

  private final FlexListLayout<T, U> flexListLayout;

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param instanceName
   * @return
   * @see #addNPFToContent(Panel, String)
   */
  private Panel doNPF(String instanceName) {
//    logger.info(getClass() + " : doNPF instanceName = " + instanceName);
    Panel widgets = flexListLayout.doInternalLayout(null, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  /**
   * @return
   * @see Navigation#getTabPanel()
   */
  public ListInterface<?> getExerciseList() {
    return npfExerciseList;
  }

  @Override
  public Reloadable getReloadable() {
    return npfExerciseList;
  }

  /**
   * @see Navigation#addPracticeTab()
   * @see mitll.langtest.client.custom.Navigation#selectPreviousTab
   */
  public void hideList() {
    npfExerciseList.hide();
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
    return new ExercisePanelFactory<T, U>(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final U e) {
        return new WaveformExercisePanel<T, U>(e, service, controller, exerciseList, true, instance) {
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
   * @param e
   * @see mitll.langtest.client.custom.RecorderNPFHelper.MyWaveformExercisePanel#postAnswers(ExerciseController, HasID)
   */
  void tellOtherListExerciseDirty(HasID e) {
    if (predefinedContentList != null &&
        predefinedContentList.getReloadable() != null &&
        predefinedContentList.getReloadable().getCurrentExerciseID() != null &&
        e.getID().equals(predefinedContentList.getReloadable().getCurrentExerciseID())) {
      // logger.info("SimpleChapterNPFHelper.reloading " + e.getID());
      predefinedContentList.getReloadable().loadExercise(e.getID());
    } else {
  //    logger.info("\n\n\n--> SimpleChapterNPFHelper.not reloading " + e.getID());
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

  protected abstract static class MyFlexListLayout<T extends CommonShell, U extends CommonShell & AudioRefExercise> extends FlexListLayout<T, U> {
    private final SimpleChapterNPFHelper<T, U> outer;

    public MyFlexListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                            ExerciseController controller, SimpleChapterNPFHelper<T, U> outer) {
      super(service, feedback, controller);
      this.outer = outer;
    }

    @Override
    protected ExercisePanelFactory<T, U> getFactory(PagingExerciseList<T, U> exerciseList) {
      return outer.getFactory(exerciseList);
    }
  }
}
