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

/**
 * Lets you show a user list with a paging container...
 * User: GO22670
 * Date: 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
class SimpleChapterNPFHelper implements RequiresResize {
  private boolean madeNPFContent = false;

  protected final LangTestDatabaseAsync service;
  protected final ExerciseController controller;
  protected final UserManager userManager;

  protected final UserFeedback feedback;
  protected PagingExerciseList npfExerciseList;
  ListInterface predefinedContentList;

  /**
   * @see Navigation#Navigation
   * @see Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
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
   * @see Navigation#addPracticeTab()
   * @see Navigation#addTabs(com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.client.custom.Navigation#selectPreviousTab(String)
   * @param tabAndContent in this tab
   * @param instanceName flex, review, etc.
   */
  public void showNPF(TabAndContent tabAndContent, String instanceName) {
    //System.out.println(getClass() + " : adding npf content instanceName = " + instanceName + " loadExercises " + loadExercises);
    DivWidget content = tabAndContent.getContent();
    int widgetCount = content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      madeNPFContent = true;
      //System.out.println("\t: adding npf content instanceName = " + instanceName + " loadExercises " + loadExercises);
      addNPFToContent(content, instanceName);
    }
  }

  private void addNPFToContent(Panel listContent, String instanceName) {
    listContent.add(doNPF(instanceName));
    listContent.addStyleName("userListBackground");
  }

  private NPFHelper.FlexListLayout flexListLayout;
  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param instanceName
   * @return
   */
  private Panel doNPF(String instanceName) {
    //System.out.println(getClass() + " : doNPF instanceName = " + instanceName + " for list loadExercises " + loadExercises);
    Panel widgets = flexListLayout.doInternalLayout(null, instanceName);
    npfExerciseList = flexListLayout.npfExerciseList;
    return widgets;
  }

  public void hideList() { npfExerciseList.hide(); }

  /**
   * This is important - this is where the actual content is chosen.
   * @see mitll.langtest.client.exercise.WaveformExercisePanel
   * @param exerciseList
   * @return
   */
  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList) {
    return new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final CommonExercise e) {
        return new WaveformExercisePanel(e, service, controller, exerciseList, true) {
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
    if (e.getID().equals(predefinedContentList.getCurrentExerciseID())) {
      System.out.println( "WaveformExercisePanel.reloading "+ e.getID());

      predefinedContentList.loadExercise(e.getID());
    }
    else {
      System.out.println( "WaveformExercisePanel.not reloading "+ e.getID());

    }
  }

  @Override
  public void onResize() {
    if (flexListLayout != null) {
      flexListLayout.onResize();
    } else if (npfExerciseList != null) {
      npfExerciseList.onResize();
    } else {
      System.out.println("SimpleChapterNPFHelper.onResize : not sending resize event - flexListLayout is null?");
    }
  }

  protected static class MyFlexListLayout extends NPFHelper.FlexListLayout {
    private final SimpleChapterNPFHelper outer;

    public MyFlexListLayout(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager, ExerciseController controller, SimpleChapterNPFHelper outer) {
      super(service, feedback, userManager, controller);
      this.outer = outer;
    }

    @Override
    protected ExercisePanelFactory getFactory(PagingExerciseList exerciseList, String instanceName) {
      return outer.getFactory(exerciseList);
    }
  }
}
