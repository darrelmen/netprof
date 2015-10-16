package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;

import java.util.logging.Logger;

/**
 * Lets you show a user list with a paging container...
 * User: GO22670
 * Date: 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
class SimpleChapterNPFHelper implements RequiresResize {
  private Logger logger = Logger.getLogger("SimpleChapterNPFHelper");

  private boolean madeNPFContent = false;

  protected final LangTestDatabaseAsync service;
  protected final ExerciseController controller;
  protected final UserManager userManager;

  protected final UserFeedback feedback;
  protected PagingExerciseList npfExerciseList;
  private final ListInterface predefinedContentList;

  /**
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @see Navigation#Navigation
   * @see Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
   */
  public SimpleChapterNPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                                ExerciseController controller,
                                ListInterface predefinedContentList) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.userManager = userManager;
    this.predefinedContentList = predefinedContentList;

    final SimpleChapterNPFHelper outer = this;
    this.flexListLayout = getMyListLayout(service, feedback, userManager, controller, outer);
  }

  protected NPFHelper.FlexListLayout getMyListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                                                     UserManager userManager, ExerciseController controller, SimpleChapterNPFHelper outer) {
    return new MyFlexListLayout(service, feedback, userManager, controller, outer);
  }

  /**
   * Add npf widget to content of a tab - here marked tabAndContent
   *
   * @param tabAndContent in this tab
   * @param instanceName  flex, review, etc.
   * @see Navigation#addPracticeTab()
   * @see Navigation#addTabs(com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.client.custom.Navigation#selectPreviousTab(String)
   */
  public void showNPF(TabAndContent tabAndContent, String instanceName) {
   // logger.info(getClass() + " : adding npf content instanceName = " + instanceName);//+ " loadExercises " + loadExercises);
    DivWidget content = tabAndContent.getContent();
    int widgetCount = content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      madeNPFContent = true;
      //logger.info("\t: adding npf content instanceName = " + instanceName + " loadExercises " + loadExercises);
      addNPFToContent(content, instanceName);
    }
  }

  private void addNPFToContent(Panel listContent, String instanceName) {
    listContent.add(doNPF(instanceName));
    listContent.addStyleName("userListBackground");
  }

  private final NPFHelper.FlexListLayout flexListLayout;

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param instanceName
   * @return
   */
  private Panel doNPF(String instanceName) {
    //logger.info(getClass() + " : doNPF instanceName = " + instanceName + " for list loadExercises " + loadExercises);
    Panel widgets = flexListLayout.doInternalLayout(null, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  public ListInterface getExerciseList() {
    return npfExerciseList;
  }

  /**
   * @see Navigation#addPracticeTab()
   * @see mitll.langtest.client.custom.Navigation#selectPreviousTab(String)
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
  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
    final String instance = exerciseList.getInstance();
    return new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final CommonExercise e) {
        return new WaveformExercisePanel(e, service, controller, exerciseList, true, instance) {
          @Override
          public void postAnswers(ExerciseController controller, CommonExercise completedExercise) {
            super.postAnswers(controller, completedExercise);
            tellOtherListExerciseDirty(e);
          }
        };
      }
    };
  }

  protected void tellOtherListExerciseDirty(CommonExercise e) {
    if (predefinedContentList != null && e.getID().equals(predefinedContentList.getCurrentExerciseID())) {
      logger.info("WaveformExercisePanel.reloading " + e.getID());

      predefinedContentList.loadExercise(e.getID());
    } else {
      logger.info("WaveformExercisePanel.not reloading " + e.getID());
    }
  }

  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    } else {
      logger.info("SimpleChapterNPFHelper.onResize : not sending resize event - flexListLayout is null?");
    }
  }

  public void setContentPanel(DivWidget contentPanel) {
  //  logger.info("SimpleChapterNPFHelper.setContentPanel : got " + contentPanel);
  //  DivWidget contentPanel1 = contentPanel;
  }

/*
  public DivWidget getContentPanel() {
    return contentPanel;
  }
*/

  protected static class MyFlexListLayout extends NPFHelper.FlexListLayout {
    private final SimpleChapterNPFHelper outer;

    public MyFlexListLayout(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                            ExerciseController controller, SimpleChapterNPFHelper outer) {
      super(service, feedback, userManager, controller);
      this.outer = outer;
    }

    @Override
    protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList, String instanceName) {
      return outer.getFactory(exerciseList);
    }
  }
}
