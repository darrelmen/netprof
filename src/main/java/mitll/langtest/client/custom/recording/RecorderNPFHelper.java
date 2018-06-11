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

package mitll.langtest.client.custom.recording;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.WaveformExercisePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.exercise.AnnotationExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseAnnotation;

import java.util.Map;

/**
 * Sets up recording both ref recordings and context ref recordings.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/15.
 * <p>
 * <T extends CommonShell & AudioRefExercise>
 */
public class RecorderNPFHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  // private final Logger logger = Logger.getLogger("RecorderNPFHelper");
  // private static final String SHOW_ONLY_UNRECORDED = "Show Only Unrecorded";
  /**
   *
   */
  private final boolean doNormalRecording;
  private boolean added = false;
  private final RecordingProgressTable flex = new RecordingProgressTable();
  private INavigation.VIEWS myView;

  /**
   * @param controller
   * @param doNormalRecording
   * @param viewContaner
   * @param myView
   * @see mitll.langtest.client.banner.NewContentChooser#showView
   */
  public RecorderNPFHelper(ExerciseController controller,
                           boolean doNormalRecording,
                           IViewContaner viewContaner,
                           INavigation.VIEWS myView) {
    super(controller, viewContaner, myView);
    this.myView = myView;
    this.doNormalRecording = doNormalRecording;
  }

  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    return new RecordFactory(exerciseList);
  }

  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(controller, outer) {
      final FlexListLayout outerLayout = this;

      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
      }

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow,
                                                                                 Panel currentExercisePanel,
                                                                                 String instanceName,
                                                                                 DivWidget listHeader,
                                                                                 DivWidget footer) {
        return new RecordingFacetExerciseList(controller,
            topRow, currentExercisePanel, instanceName, listHeader, myView == INavigation.VIEWS.CONTEXT);

        /*        return new NPExerciseList(currentExercisePanel,
            outerLayout.getController(),
            new ListOptions()
                .setInstance(instanceName)
                .setShowFirstNotCompleted(true)
                .setActivityType(ActivityType.RECORDER),
            //    .setContextOnly(!doNormalRecording)
            -1) {
          private CheckBox filterOnly;

          @Override
          protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
            // row 1
            Panel column = new FlowPanel();
            add(column);
            addTypeAhead(column);

            // row 2
            filterOnly = new CheckBox(SHOW_ONLY_UNRECORDED);
            filterOnly.addClickHandler(event -> pushNewSectionHistoryToken());
            filterOnly.addStyleName("leftFiveMargin");
            add(filterOnly);

            // row 3
            add(pagingContainer.getTableWithPager(new ListOptions()));
            setOnlyExamples(!doNormalRecording);

            addEventHandler(instanceName);
          }

          @Override
          protected void loadExercisesUsingPrefix(Map<String, Collection<String>> typeToSection,
                                                  String prefix,
                                                  int exerciseID,
                                                  boolean onlyWithAudioAnno,
                                                  boolean onlyUnrecorded,
                                                  boolean onlyDefaultUser,
                                                  boolean onlyUninspected) {
            super.loadExercisesUsingPrefix(typeToSection, prefix, exerciseID, onlyWithAudioAnno, onlyUnrecorded, onlyDefaultUser, onlyUninspected);
            filterOnly.setText(setCheckboxTitle());
          }

          private String setCheckboxTitle() {
            return SHOW_ONLY_UNRECORDED + (controller.getUserManager().isMale() ? " by Males" : " by Females");
          }

          *//**
         * @see mitll.langtest.client.list.HistoryExerciseList#getHistoryToken
         * @param search
         * @param id
         * @return
         *//*
          protected String getHistoryTokenFromUIState(String search, int id) {
            String s = super.getHistoryTokenFromUIState(search, id) +
                SelectionState.SECTION_SEPARATOR +
                SelectionState.ONLY_UNRECORDED +
                "=" + filterOnly.getValue();
            //           logger.info("RecorderNPFHelper : history token now  " + s);
            return s;
          }

          @Override
          protected void restoreUIState(SelectionState selectionState) {
            super.restoreUIState(selectionState);
            filterOnly.setValue(selectionState.isOnlyUnrecorded());
          }
        };*/
      }
    };
  }

/*  private void addEventHandler(final String instanceName) {
    LangTest.EVENT_BUS.addHandler(AudioChangedEvent.TYPE, authenticationEvent -> {
      if (!authenticationEvent.getSource().equals(instanceName)) {
        // logger.info("this " + getClass() + " instance " + instanceName + " updating progress " + authenticationEvent.getSource());
        getProgressInfo();
      }
    });
  }*/

  private Widget doMaleFemale() {
    flex.addStyleName("topMargin");
   // getProgressInfo();
    return flex;
  }

  /**
   * @see #doMaleFemale()
   * @see RecordRefAudioPanel#onLoad
   */
  private void getProgressInfo() {
    //logger.info("Get progress info for " +getClass() + " instance " + instance);
    controller.getService().getMaleFemaleProgress(new AsyncCallback<Map<String, Float>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting recording progress", caught);
      }

      @Override
      public void onSuccess(Map<String, Float> result) {
        flex.populate(result);
      }
    });
  }

  /**
   * @see #getFactory(PagingExerciseList)
   */
  private class RecordRefAudioPanel extends WaveformExercisePanel<CommonShell, CommonExercise> implements CommentAnnotator {
    //    private final Logger logger = Logger.getLogger("RecordRefAudioPanel");

    /**
     * @param e
     * @param controller1
     * @param exerciseList1
     * @param instance
     * @see RecorderNPFHelper#getFactory
     */
    RecordRefAudioPanel(CommonExercise e, ExerciseController controller1,
                        ListInterface<CommonShell, CommonExercise> exerciseList1, String instance) {
      super(e, controller1, exerciseList1, RecorderNPFHelper.this.doNormalRecording, instance);
    }

    @Override
    protected boolean showPrevButton() {
      return true;
    }

    @Override
    protected void enableNext() {
      super.enableNext();
      if (isCompleted()) {
        showRecordedState(exercise);
      }
    }

    @Override
    protected void onLoad() {
      super.onLoad();
      if (!added) {
        DivWidget c = new DivWidget();
        c.add(doMaleFemale());
        c.setWidth("100%");
        Widget parent = getParent().getParent();
        parent.removeStyleName("floatLeftAndClear");
        getParent().removeStyleName("floatLeftAndClear");
        ((Panel) parent).add(c);
        added = true;
      } else {
        getProgressInfo();
      }
    }

    /**
     * TODO : confirm we haven't broken this...
     *
     * @param e
     * @return
     * @seex #ExercisePanel(T, LangTestDatabaseAsync, ExerciseController, ListInterface, String, String)
     */
    @Override
    protected Widget getQuestionContent(CommonExercise e) {
      String content = getExerciseContent(e);

      HTML maybeRTLContent = getMaybeRTLContent(content);
      maybeRTLContent.addStyleName("rightTenMargin");
      maybeRTLContent.addStyleName("topMargin");

      Widget contentWidget = (content.length() > 200) ? getContentScroller(maybeRTLContent) : maybeRTLContent;

      boolean rtlContent = isRTLContent(e.getFLToShow());
      Widget entry = getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, contentWidget, rtlContent);

      //   logger.info("rtl " + rtlContent + " for " + content);
      entry.addStyleName(rtlContent ? "floatRight" : "floatLeftAndClear");

      return entry;
    }

    private boolean isRTLContent(String content) {
      return WordCountDirectionEstimator.get().estimateDirection(content) == HasDirection.Direction.RTL;
    }


    /**
     * @param e
     * @param field
     * @param isRTL
     * @return
     * @seex #getContext
     * @see GoodwaveExercisePanel#getQuestionContent
     */
    private Widget getEntry(AnnotationExercise e, final String field, Widget contentWidget, boolean isRTL) {
      return getEntry(field, e.getAnnotation(field), contentWidget, isRTL);
    }

    /**
     * @param field
     * @param annotation
     * @param isRTL
     * @return
     * @seex #makeFastAndSlowAudio(String)
     * @see #getEntry
     */
    private Widget getEntry(final String field, ExerciseAnnotation annotation, Widget contentWidget, boolean isRTL) {
      return getCommentBox(true).getEntry(field, contentWidget, annotation, true, isRTL);
    }

    /**
     * @return
     */
    private CommentBox getCommentBox(boolean tooltipOnRight) {
      return new CommentBox(this.exercise.getID(), controller, this, this.exercise.getMutableAnnotation(), tooltipOnRight);
    }

    /**
     * @param exid
     * @param field         @see mitll.langtest.client.qc.QCNPFExercise#makeCommentEntry(String, ExerciseAnnotation)
     * @param commentToPost
     */
    @Override
    public void addIncorrectComment(int exid, final String field, final String commentToPost) {
      addAnnotation(field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost);
    }

    @Override
    public void addCorrectComment(int exid, final String field) {
      addAnnotation(field, ExerciseAnnotation.TYPICAL.CORRECT, "");
    }

    private void addAnnotation(final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost) {
      controller.getQCService().addAnnotation(exercise.getID(), field, status.toString(), commentToPost,
          new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              controller.handleNonFatalError("adding annotation", caught);
            }

            @Override
            public void onSuccess(Void result) {
            }
          });
    }
  }

  private class RecordFactory extends ExercisePanelFactory<CommonShell, CommonExercise> {
    RecordFactory(PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
      super(RecorderNPFHelper.this.controller, exerciseList);
    }

    @Override
    public Panel getExercisePanel(CommonExercise e) {
      Scheduler.get().scheduleDeferred(RecorderNPFHelper.this::getProgressInfo);
      return new RecordRefAudioPanel(e, controller, exerciseList, myView.toString());
    }
  }
}
