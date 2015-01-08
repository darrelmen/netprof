package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ReviewItemHelper extends NPFHelper {
  private Logger logger = Logger.getLogger("ReviewItemHelper");

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
    logger.info("making ReviewItemHelper --->");
  }

/*  @Override
  PagingExerciseList makeExerciseList(Panel right, String instanceName) {
    // return super.makeExerciseList(right, instanceName);
    logger.info("making ReviewItemHelper.makeExerciseList --->");

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

      @Override
      protected void addTableWithPager(PagingContainer pagingContainer) {

        logger.info("doing new makeExerciseList.addTableWithPager --->");
        // row 1
        Panel column = new FlowPanel();
        add(column);
        addTypeAhead(column);

        // row 2
        final com.github.gwtbootstrap.client.ui.CheckBox w = new com.github.gwtbootstrap.client.ui.CheckBox("Only with audio defects");
        w.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            logger.info("got click " + w.getValue());
            //  setUnrecorded(w.getValue());
            //  scheduleWaitTimer();
            //  loadExercises(getHistoryToken(""), getTypeAheadText());
          }
        });
        w.addStyleName("leftFiveMargin");
        add(w);

        // row 3
        add(pagingContainer.getTableWithPager());
        //setOnlyExamples(!doNormalRecording);
      }
    };
  }*/

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

    logger.info(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

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

      @Override
      protected FlexSectionExerciseList makeExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, boolean incorrectFirst) {
        //return super.makeExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst);

        return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst) {
          com.github.gwtbootstrap.client.ui.CheckBox onlyAudio;
          @Override
          protected void addTableWithPager(PagingContainer pagingContainer) {
            //super.addTableWithPager(pagingContainer);

            logger.info("doing new makeExerciseList.addTableWithPager --->");
            // row 1
            Panel column = new FlowPanel();
            add(column);
            addTypeAhead(column);

            // row 2
            final com.github.gwtbootstrap.client.ui.CheckBox w = new com.github.gwtbootstrap.client.ui.CheckBox("Only with audio defects");
            onlyAudio = w;
            w.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                logger.info("got click " + w.getValue());
                //  setUnrecorded(w.getValue());
                //  scheduleWaitTimer();
                  loadExercises(getHistoryToken(""), getTypeAheadText(),w.getValue());
                //reload();
              }
            });
            w.addStyleName("leftFiveMargin");
            add(w);

            // row 3
            add(pagingContainer.getTableWithPager());
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
