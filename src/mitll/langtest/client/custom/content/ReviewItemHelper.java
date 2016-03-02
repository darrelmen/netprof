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
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.logging.Logger;

/**
 * Created by GO22670 on 3/26/2014.
 */
public class ReviewItemHelper extends NPFHelper {
  private final Logger logger = Logger.getLogger("ReviewItemHelper");

  private static final String ONLY_WITH_AUDIO_DEFECTS = "Only with audio defects";

  private FlexListLayout<CommonShell, CommonExercise> flexListLayout;
  private final HasText itemMarker;
  private final ReloadableContainer predefinedContent;
//  private final NPFHelper npfHelper;

  /**
   * @param service
   * @param feedback
   * @param controller
   * @param predefinedContent
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @see mitll.langtest.client.custom.ListManager#ListManager
   */
  public ReviewItemHelper(final LangTestDatabaseAsync service, final UserFeedback feedback,
                          final ExerciseController controller,
                          final ReloadableContainer predefinedContent
  ) {
    super(service, feedback, controller, true);
    this.itemMarker = null;
    this.predefinedContent = predefinedContent;
    if (predefinedContent == null) logger.warning("huh? predefinedContent is null");
  }

  /**
   * Left and right components
   *
   * @param ul
   * @param instanceName
   * @return
   * @see #doNPF
   */
  @Override
  protected Panel doInternalLayout(final UserList<CommonShell> ul, String instanceName) {
//    logger.info(getClass() + " : doInternalLayout instanceName = " + instanceName + " for list " + ul);
    this.flexListLayout = new ReviewFlexListLayout(ul);
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

  // private class ReviewFlexListLayout<CommonExercise extends CommonShell & AnnotationExercise & AudioRefExercise> extends FlexListLayout<CommonExercise> {
  private class ReviewFlexListLayout extends FlexListLayout<CommonShell, CommonExercise> {
    private final UserList<CommonShell> ul;

    public ReviewFlexListLayout(UserList<CommonShell> ul) {
      super(ReviewItemHelper.this.service, ReviewItemHelper.this.feedback, ReviewItemHelper.this.controller);
      this.ul = ul;
    }

    @Override
    protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> pagingExerciseList) {
      return new ExercisePanelFactory<CommonShell, CommonExercise>(service, feedback, controller, pagingExerciseList) {
        @Override
        public Panel getExercisePanel(CommonExercise exercise) {
          CommonExercise userExercise = new UserExercise(exercise, exercise.getCreator());

          ReviewEditableExercise reviewEditableExercise =
              new ReviewEditableExercise(service, controller, itemMarker,
                  userExercise, ul,
                  pagingExerciseList, predefinedContent,
                  "ReviewEditableExercise"
              );

          SimplePanel ignoredContainer = new SimplePanel();

          Panel widgets = reviewEditableExercise.addNew(
              ul,
              ul,
              npfExerciseList,
              ignoredContainer);
          reviewEditableExercise.setFields(exercise);

          return widgets;
        }
      };
    }

    @Override
    protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow, Panel currentExercisePanel,
                                                                               String instanceName, boolean incorrectFirst) {
      FlexListLayout outer = this;
      return new NPFlexSectionExerciseList(outer, topRow, currentExercisePanel, instanceName, incorrectFirst) {
        //com.github.gwtbootstrap.client.ui.CheckBox onlyAudio;

        @Override
        protected void addTableWithPager(ClickablePagingContainer pagingContainer) {
          // row 1
          Panel column = new FlowPanel();
          add(column);
          addTypeAhead(column);

          // row 2
          final com.github.gwtbootstrap.client.ui.CheckBox w = new com.github.gwtbootstrap.client.ui.CheckBox(ONLY_WITH_AUDIO_DEFECTS);
          //onlyAudio = w;
          w.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              loadExercises(getHistoryToken(""), getTypeAheadText(), w.getValue());
            }
          });
          w.addStyleName("leftFiveMargin");
          add(w);

          // row 3
          add(pagingContainer.getTableWithPager());
        }
      };

    }
  }
}
