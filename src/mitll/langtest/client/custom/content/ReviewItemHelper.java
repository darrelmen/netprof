/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.content;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ReviewItemHelper extends NPFHelper {
  //private Logger logger = Logger.getLogger("ReviewItemHelper");

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
//    logger.info(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);

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
        return new MyFlexSectionExerciseList(topRow, currentExercisePanel, instanceName, incorrectFirst) {
          com.github.gwtbootstrap.client.ui.CheckBox onlyAudio;
          @Override
          protected void addTableWithPager(PagingContainer pagingContainer) {
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
                  loadExercises(getHistoryToken(""), getTypeAheadText(),w.getValue());
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
