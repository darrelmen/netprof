package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Lets you show a user list with a paging container...
 * List on the left, content on the right, made by a factory.
 * User: GO22670
 * Date: 10/8/13
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPFHelper implements RequiresResize {
  protected static final String LIST_COMPLETE = "List complete!";
  protected static final String COMPLETE = "Complete";
  private boolean madeNPFContent = false;

  protected final LangTestDatabaseAsync service;
  protected final ExerciseController controller;
  protected final UserManager userManager;

  protected final UserFeedback feedback;
  public PagingExerciseList npfExerciseList;
  private Panel npfContentPanel;
  private final boolean showQC;
  DivWidget contentPanel;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @param service
   * @param feedback
   * @param userManager
   * @param controller
   * @param showQC
   */
  public NPFHelper(LangTestDatabaseAsync service, UserFeedback feedback, UserManager userManager,
                   ExerciseController controller, boolean showQC) {
    this.service = service;
    this.feedback = feedback;
    this.controller = controller;
    this.userManager = userManager;
    this.showQC = showQC;
  }

  /**
   * Add npf widget to content of a tab - here marked tabAndContent
   * @see mitll.langtest.client.custom.Navigation#getListOperations
   * @param ul show this user list
   * @param tabAndContent in this tab
   * @param instanceName flex, review, etc.
   * @param loadExercises should we load exercises initially
   */
  public void showNPF(UserList ul, TabAndContent tabAndContent, String instanceName, boolean loadExercises) {
    //System.out.println(getClass() + " : adding npf content instanceName = " + instanceName + " for list " + ul);
    DivWidget content = tabAndContent.getContent();
    int widgetCount = content.getWidgetCount();
    if (!madeNPFContent || widgetCount == 0) {
      madeNPFContent = true;
     // System.out.println("\t: adding npf content instanceName = " + instanceName + " for list " + ul);
     // System.out.println("\t: first is = " + instanceName + "  " + ul.getExercises().iterator().next().getID());
      addNPFToContent(ul, content, instanceName, loadExercises);
    } else {
    //  System.out.println("\t: rememberAndLoadFirst instanceName = " + instanceName + " for list " + ul);
   //   System.out.println("\t: first is = " + instanceName + "  " + ul.getExercises().iterator().next().getID());
      rememberAndLoadFirst(ul);
    }
  }

  public ListInterface getExerciseList() { return npfExerciseList; }

  private void addNPFToContent(UserList ul, Panel listContent, String instanceName, boolean loadExercises) {
    Panel npfContent = doNPF(ul, instanceName, loadExercises);
    listContent.add(npfContent);
    listContent.addStyleName("userListBackground");
  }

  /**
   * Make the instance name uses the unique id for the list.
   *
   * @param ul
   * @param instanceName
   * @param loadExercises
   * @return
   * @see #addNPFToContent(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String, boolean)
   */
  private Panel doNPF(UserList ul, String instanceName, boolean loadExercises) {
   // System.out.println(getClass() + " : doNPF instanceName = " + instanceName + " for list " + ul);
    Panel hp = doInternalLayout(ul, instanceName);

    if (loadExercises) {
      rememberAndLoadFirst(ul);
    }
    setupContent(hp);
    return hp;
  }

  /**
   * Left and right components
   * @param ul
   * @param instanceName
   * @return
   * @see #doNPF(mitll.langtest.shared.custom.UserList, String, boolean)
   */
  protected Panel doInternalLayout(UserList ul, String instanceName) {
    //System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

    Panel hp = new HorizontalPanel();
    hp.getElement().setId("internalLayout_Row");

    Panel left = new SimplePanel();
    left.getElement().setId("internalLayout_LeftCol");
    left.addStyleName("floatLeft");

    hp.add(left);

    npfContentPanel = getRightSideContent(ul, instanceName);

    left.add(npfExerciseList.getExerciseListOnLeftSide(controller.getProps()));

    hp.add(npfContentPanel);

    return hp;
  }

  protected Panel getRightSideContent(UserList ul, String instanceName) {
    Panel npfContentPanel = new SimplePanel();
    npfContentPanel.addStyleName("floatRight");
    npfContentPanel.getElement().setId("internalLayout_RightContent");

    npfExerciseList = makeNPFExerciseList(npfContentPanel, instanceName + "_"+ul.getUniqueID());
    return npfContentPanel;
  }

  /**
   * @see #doNPF
   * @param right
   * @param instanceName
   * @return
   */
  PagingExerciseList makeNPFExerciseList(Panel right, String instanceName) {
    final PagingExerciseList exerciseList = makeExerciseList(right, instanceName);
    setFactory(exerciseList, instanceName, showQC);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  /**
   * @see #doNPF(mitll.langtest.shared.custom.UserList, String, boolean)
   * @see #showNPF(mitll.langtest.shared.custom.UserList, TabAndContent, String, boolean)
   * @param ul
   */
  private void rememberAndLoadFirst(final UserList ul) {
    System.out.println(getClass() + ".rememberAndLoadFirst : for " +ul);
    npfExerciseList.setUserListID(ul.getUniqueID());
    npfExerciseList.rememberAndLoadFirst(new ArrayList<CommonShell>(ul.getExercises()));
  }

  Panel setupContent(Panel hp) {  return npfContentPanel;  }

  PagingExerciseList makeExerciseList(final Panel right, final String instanceName) {
    //System.out.println(getClass() + ".makeExerciseList : instanceName " + instanceName);
    return new PagingExerciseList(right, service, feedback, null, controller,
      true, instanceName, false) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog(COMPLETE, LIST_COMPLETE, new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }
    };
  }

  /**
   * @see #makeNPFExerciseList(com.google.gwt.user.client.ui.Panel, String)
   * @param exerciseList
   * @param instanceName
   * @param showQC
   */
  void setFactory(final PagingExerciseList exerciseList, final String instanceName, boolean showQC) {
    exerciseList.setFactory(getFactory(exerciseList, instanceName, showQC), userManager);
  }

  protected ExercisePanelFactory getFactory(final PagingExerciseList exerciseList, final String instanceName, final boolean showQC) {
    return new GoodwaveExercisePanelFactory(service, feedback, controller, exerciseList, 1.0f) {
      @Override
      public Panel getExercisePanel(CommonExercise e) {
        if (showQC) {
         // System.out.println("\nNPFHelper : making new QCNPFExercise for " +e + " instance " + instanceName);
          return new QCNPFExercise(e, controller, exerciseList, instanceName);
        }
        else {
         // System.out.println("\nmaking new CommentNPFExercise for " +e + " instance " + instanceName);
          return new CommentNPFExercise(e, controller, exerciseList, false, instanceName);
        }
      }
    };
  }

  /**
   * @see #doNPF(mitll.langtest.shared.custom.UserList, String, boolean)
   * @return
   */
 // Panel getNpfContentPanel() { return npfContentPanel; }

  @Override
  public void onResize() { if (npfContentPanel != null) {  npfExerciseList.onResize(); } }

  public void setContentPanel(DivWidget content) {
    this.contentPanel = content;
  }

  /**
   * Created by GO22670 on 3/26/2014.
   */
  public static class ReviewItemHelper extends NPFHelper {
    private FlexListLayout flexListLayout;
    private final HasText itemMarker;
    private final ListInterface predefinedContent;
    private final NPFHelper npfHelper;

    /**
     * @see mitll.langtest.client.custom.Navigation#Navigation
     * @param service
     * @param feedback
     * @param userManager
     * @param controller
     * @param predefinedContent
     */
    public ReviewItemHelper(final LangTestDatabaseAsync service, final UserFeedback feedback,
                            final UserManager userManager, final ExerciseController controller,
                            final ListInterface predefinedContent,
                            NPFHelper npfHelper) {
      super(service, feedback, userManager, controller, true);
      this.itemMarker = null;
      this.predefinedContent = predefinedContent;
      this.npfHelper = npfHelper;
    }

    /**
     * Left and right components
     *
     *
     * @param ul
     * @param instanceName
     * @return
     * @see #doNPF(mitll.langtest.shared.custom.UserList, String, boolean)
     */
    protected Panel doInternalLayout(final UserList ul, String instanceName) {
      this.flexListLayout = new FlexListLayout(service,feedback,userManager,controller) {
        @Override
        protected ExercisePanelFactory getFactory(final PagingExerciseList pagingExerciseList, String instanceName) {
          return new ExercisePanelFactory(service,feedback,controller,predefinedContent) {
            @Override
            public Panel getExercisePanel(CommonExercise exercise) {
              ReviewEditableExercise reviewEditableExercise =
                new ReviewEditableExercise(service, controller, itemMarker, new UserExercise(exercise), ul,
                  pagingExerciseList, predefinedContent, npfHelper);
              SimplePanel ignoredContainer = new SimplePanel();

              Panel widgets = reviewEditableExercise.addNew(ul, ul,
                npfExerciseList,
                ignoredContainer);
              reviewEditableExercise.setFields(exercise);

              return widgets;
            }
          };
        }
      };

      Panel widgets = flexListLayout.doInternalLayout(ul, instanceName);
      npfExerciseList = flexListLayout.npfExerciseList;
      return widgets;
    }

    @Override
    public void onResize() {
      if (flexListLayout != null) {
        flexListLayout.onResize();
      } else if (npfExerciseList != null) {
        npfExerciseList.onResize();
      } else {
        //System.out.println("ReviewItemHelper.onResize : not sending resize event - flexListLayout is null?");
      }
    }
  }

  /**
   * Created by GO22670 on 3/28/2014.
   */
  public abstract static class FlexListLayout implements RequiresResize {
    public PagingExerciseList npfExerciseList;
    private final ExerciseController controller;
    private final LangTestDatabaseAsync service;
    private final UserFeedback feedback;
    private final UserManager userManager;
    final boolean incorrectFirst;

    /**
     * @see ChapterNPFHelper#ChapterNPFHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, boolean)
     * @see mitll.langtest.client.custom.content.NPFHelper.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     * @param service
     * @param feedback
     * @param userManager
     * @param controller
     */
    public FlexListLayout(LangTestDatabaseAsync service, UserFeedback feedback,
                          UserManager userManager, ExerciseController controller) {
      this.controller = controller;
      this.service = service;
      this.feedback = feedback;
      this.userManager = userManager;
      this.incorrectFirst = false;
    }

    /**
     * @see ChapterNPFHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     * @see mitll.langtest.client.custom.content.NPFHelper.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     * @param ul
     * @param instanceName
     * @return
     */
    public Panel doInternalLayout(UserList ul, String instanceName) {
      //System.out.println(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

      Panel twoRows = new FlowPanel();
      twoRows.getElement().setId("NPFHelper_twoRows");

      Panel exerciseListContainer = new SimplePanel();
      exerciseListContainer.addStyleName("floatLeft");
      exerciseListContainer.getElement().setId("NPFHelper_exerciseListContainer");

      // second row ---------------
      FluidRow topRow = new FluidRow();
      topRow.getElement().setId("NPFHelper_topRow");

      twoRows.add(topRow);

      Panel bottomRow = new HorizontalPanel();
      bottomRow.add(exerciseListContainer);
      bottomRow.getElement().setId("NPFHelper_bottomRow");
      styleBottomRow(bottomRow);

      twoRows.add(bottomRow);

      FlowPanel currentExerciseVPanel = new FlowPanel();
      currentExerciseVPanel.getElement().setId("NPFHelper_defect_currentExercisePanel");

      currentExerciseVPanel.addStyleName("floatLeftList");
      bottomRow.add(currentExerciseVPanel);

      long uniqueID = ul == null ? -1 : ul.getUniqueID();
      FlexSectionExerciseList widgets = makeNPFExerciseList(topRow, currentExerciseVPanel, instanceName, uniqueID, incorrectFirst);
      npfExerciseList = widgets;

      Widget exerciseListOnLeftSide = npfExerciseList.getExerciseListOnLeftSide(controller.getProps());
      exerciseListContainer.add(exerciseListOnLeftSide);

      widgets.addWidgets();
      return twoRows;
    }

    protected void styleBottomRow(Panel bottomRow) {
      bottomRow.setWidth("100%");
      bottomRow.addStyleName("trueInlineStyle");
    }

    /**
     * @see #doInternalLayout(mitll.langtest.shared.custom.UserList, String)
     * @param topRow
     * @param currentExercisePanel
     * @param instanceName
     * @param userListID
     * @param incorrectFirst
     * @return
     */
    private FlexSectionExerciseList makeNPFExerciseList(final Panel topRow, Panel currentExercisePanel, String instanceName,
                                                        long userListID, boolean incorrectFirst) {
      final FlexSectionExerciseList exerciseList = makeExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst);
      exerciseList.setUserListID(userListID);

      exerciseList.setFactory(getFactory(exerciseList, instanceName), userManager);
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          exerciseList.onResize();
        }
      });
      return exerciseList;
    }

    protected abstract ExercisePanelFactory getFactory(final PagingExerciseList exerciseList, final String instanceName);

    protected FlexSectionExerciseList makeExerciseList(final Panel topRow, Panel currentExercisePanel,
                                                       final String instanceName, boolean incorrectFirst) {
      return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst);
    }

    @Override
    public void onResize() {
      if (npfExerciseList != null) {
        npfExerciseList.onResize();
      }
    }

    protected class MyFlexSectionExerciseList extends FlexSectionExerciseList {
      /**
       * @see mitll.langtest.client.custom.Navigation#makePracticeHelper(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.user.UserFeedback)
       * @see mitll.langtest.client.custom.Navigation.RecorderNPFHelper#getMyListLayout(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.user.UserManager, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.custom.SimpleChapterNPFHelper)
       * @see mitll.langtest.client.custom.content.NPFHelper.FlexListLayout#makeExerciseList(com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.Panel, String, boolean)
       * @param topRow
       * @param currentExercisePanel
       * @param instanceName
       * @param incorrectFirst
       */
      public MyFlexSectionExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, boolean incorrectFirst) {
        super(topRow, currentExercisePanel, FlexListLayout.this.service, FlexListLayout.this.feedback, FlexListLayout.this.controller, instanceName, incorrectFirst);
      }

      @Override
      protected void onLastItem() {
        new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
          @Override
          public void onHidden(HiddenEvent hiddenEvent) {
            reloadExercises();
          }
        });
      }

      @Override
      protected void noSectionsGetExercises(long userID) {
        loadExercises(getHistoryToken(""), getPrefix());
      }

      @Override
      protected void loadExercises(final Map<String, Collection<String>> typeToSection, final String item) {
        System.out.println(getClass() + ".loadExercises : instance " + getInstance() + " " + typeToSection +
            " and item '" + item + "'" + " for list " + userListID);
        loadExercisesUsingPrefix(typeToSection, getPrefix());
      }
    }
  }
}
