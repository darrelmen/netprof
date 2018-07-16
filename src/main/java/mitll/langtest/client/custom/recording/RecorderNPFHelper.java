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
import mitll.langtest.shared.exercise.*;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Sets up recording both ref recordings and context ref recordings.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/15.
 * <p>
 */
public class RecorderNPFHelper<T extends CommonShell & ScoredExercise> extends SimpleChapterNPFHelper<T, ClientExercise> {
  // private final Logger logger = Logger.getLogger("RecorderNPFHelper");
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
  protected ExercisePanelFactory<T, ClientExercise> getFactory(PagingExerciseList<T, ClientExercise> exerciseList) {
    return new RecordFactory(exerciseList);
  }

  @Override
  protected FlexListLayout<T, ClientExercise> getMyListLayout(SimpleChapterNPFHelper<T, ClientExercise> outer) {
    return new MyFlexListLayout<T, ClientExercise>(controller, outer) {
      final FlexListLayout outerLayout = this;

      protected void styleBottomRow(Panel bottomRow) {
        bottomRow.addStyleName("centerPractice");
      }

      @Override
      protected PagingExerciseList<T, ClientExercise> makeExerciseList(Panel topRow,
                                                                       Panel currentExercisePanel,
                                                                       String instanceName,
                                                                       DivWidget listHeader,
                                                                       DivWidget footer) {
        return new RecordingFacetExerciseList<T>(controller,
            topRow, currentExercisePanel, instanceName, listHeader, myView == INavigation.VIEWS.CONTEXT) {

        };
      }
    };
  }

  private Widget doMaleFemale() {
    flex.addStyleName("topMargin");
    return flex;
  }

  /**
   * @see #doMaleFemale()
   * @see RecordRefAudioPanel#onLoad
   */
  private void getProgressInfo() {
    logger.info("getProgressInfo Get progress info for " + getClass());
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
  private class RecordRefAudioPanel extends WaveformExercisePanel<T, ClientExercise> implements CommentAnnotator {
    //    private final Logger logger = Logger.getLogger("RecordRefAudioPanel");

    /**
     * @param e
     * @param controller1
     * @param exerciseList1
     * @param instance
     * @see RecorderNPFHelper#getFactory
     */
    RecordRefAudioPanel(ClientExercise e, ExerciseController controller1, ListInterface<T, ClientExercise> exerciseList1, String instance) {
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
        //getProgressInfo();
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
    protected Widget getQuestionContent(ClientExercise e) {
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

  private class RecordFactory extends ExercisePanelFactory<T, ClientExercise> {
    RecordFactory(PagingExerciseList<T, ClientExercise> exerciseList) {
      super(RecorderNPFHelper.this.controller, exerciseList);
    }

    @Override
    public Panel getExercisePanel(ClientExercise e) {
      Scheduler.get().scheduleDeferred(RecorderNPFHelper.this::getProgressInfo);
      return new RecordRefAudioPanel(e, controller, exerciseList, myView.toString());
    }
  }
}
