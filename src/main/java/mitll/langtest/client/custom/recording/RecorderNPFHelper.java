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

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.*;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.exercise.*;

import java.util.Collection;
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
 * <T extends CommonShell & AudioRefExercise>
 */
public class RecorderNPFHelper extends SimpleChapterNPFHelper<CommonShell, CommonExercise> {
  //private final Logger logger = Logger.getLogger("RecorderNPFHelper");
  private static final String SHOW_ONLY_UNRECORDED = "Show Only Unrecorded";

  private final boolean doNormalRecording;
  private boolean added = false;
  private final RecordingProgressTable flex = new RecordingProgressTable();

  /**
   * @param controller
   * @param doNormalRecording
   * @see Navigation#Navigation
   */
  public RecorderNPFHelper(ExerciseController controller,
                    boolean doNormalRecording,
                    ReloadableContainer exerciseList) {
    super(controller, exerciseList);
    this.doNormalRecording = doNormalRecording;
  }

  @Override
  protected ExercisePanelFactory<CommonShell, CommonExercise> getFactory(final PagingExerciseList<CommonShell, CommonExercise> exerciseList) {
    final String oinstance = exerciseList.getInstance();
    return new ExercisePanelFactory<CommonShell, CommonExercise>(controller, exerciseList) {
      @Override
      public Panel getExercisePanel(final CommonExercise e) {
        return new MyWaveformExercisePanel(e, controller, exerciseList, oinstance);
      }
    };
  }

  @Override
  protected FlexListLayout<CommonShell, CommonExercise> getMyListLayout(SimpleChapterNPFHelper<CommonShell, CommonExercise> outer) {
    return new MyFlexListLayout<CommonShell, CommonExercise>(controller, outer) {

      final FlexListLayout outerLayout = this;

      @Override
      protected PagingExerciseList<CommonShell, CommonExercise> makeExerciseList(Panel topRow, Panel currentExercisePanel,
                                                                                 String instanceName) {
        return new NPFlexSectionExerciseList(outerLayout.getController(), topRow, currentExercisePanel,
            new ListOptions().setInstance(instanceName).setShowFirstNotCompleted(true)) {
          private final Logger logger = Logger.getLogger("NPFlexSectionExerciseList_" + instanceName);
          private CheckBox filterOnly;


          @Override
          protected void addTableWithPager(ClickablePagingContainer<CommonShell> pagingContainer) {
            // row 1
            Panel column = new FlowPanel();
            add(column);
            addTypeAhead(column);

            // row 2
            filterOnly = new CheckBox(SHOW_ONLY_UNRECORDED);
            filterOnly.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                pushNewSectionHistoryToken();
              }
            });
            filterOnly.addStyleName("leftFiveMargin");

            add(filterOnly);

            // row 3
            add(pagingContainer.getTableWithPager(true));
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

          /**
           * @see mitll.langtest.client.list.HistoryExerciseList#getHistoryToken
           * @param search
           * @param id
           * @return
           */
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
        };

      }
    };
  }

  private void addEventHandler(final String instanceName) {
    LangTest.EVENT_BUS.addHandler(AudioChangedEvent.TYPE, authenticationEvent -> {
      if (!authenticationEvent.getSource().equals(instanceName)) {
        // logger.info("this " + getClass() + " instance " + instanceName + " updating progress " + authenticationEvent.getSource());
        getProgressInfo(instanceName);
      }
    });
  }


  private Widget doMaleFemale() {
    flex.addStyleName("topMargin");
    getProgressInfo("RecordedNPFHelper");
    return flex;
  }

  /**
   * @see #doMaleFemale()
   * @see MyWaveformExercisePanel#onLoad
   */
  private void getProgressInfo(String instance) {
    //logger.info("Get progress info for " +getClass() + " instance " + instance);
    controller.getService().getMaleFemaleProgress(new AsyncCallback<Map<String, Float>>() {
      @Override
      public void onFailure(Throwable caught) {
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
  private class MyWaveformExercisePanel extends WaveformExercisePanel<CommonShell, CommonExercise> implements CommentAnnotator {
    //    private final Logger logger = Logger.getLogger("MyWaveformExercisePanel");

    /**
     * @param e
     * @param controller1
     * @param exerciseList1
     * @param instance
     * @see RecorderNPFHelper#getFactory
     */
    MyWaveformExercisePanel(CommonExercise e, ExerciseController controller1, ListInterface<CommonShell> exerciseList1, String instance) {
      super(e, controller1, exerciseList1, RecorderNPFHelper.this.doNormalRecording, instance);
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
        parent.removeStyleName("floatLeft");
        getParent().removeStyleName("floatLeft");
        ((Panel) parent).add(c);
        added = true;
      } else {
        getProgressInfo(instance);
      }
    }

    /**
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

      Widget contentWidget = (content.length() > 200) ?
          getContentScroller(maybeRTLContent)
          : maybeRTLContent;

      boolean exampleRecord = isExampleRecord();
      Widget entry = getEntry(e, exampleRecord ? QCNPFExercise.CONTEXT : QCNPFExercise.FOREIGN_LANGUAGE,
          contentWidget);
      entry.addStyleName("floatLeft");

      return entry;
    }

    /**
     * @param controller
     * @param completedExercise
     * @see RecorderNPFHelper.MyWaveformExercisePanel#postAnswers
     */
    @Override
    public void postAnswers(ExerciseController controller, HasID completedExercise) {
      super.postAnswers(controller, completedExercise);
      tellOtherListExerciseDirty(exercise);
    }

    /**
     * @param e
     * @param field
     * @return
     * @seex #getContext
     * @see GoodwaveExercisePanel#getQuestionContent
     */
    private Widget getEntry(AnnotationExercise e, final String field, Widget contentWidget) {
      return getEntry(field, e.getAnnotation(field), contentWidget);
    }

    /**
     * @param field
     * @param annotation
     * @return
     * @seex #makeFastAndSlowAudio(String)
     * @see #getEntry
     */
    private Widget getEntry(final String field, ExerciseAnnotation annotation, Widget contentWidget) {
      return getCommentBox(true).getEntry(field, contentWidget, annotation);
    }

    /**
     * @return
     * @seex #getEntry(String, String, String, ExerciseAnnotation)
     * @seex #makeFastAndSlowAudio(String)
     */
    private CommentBox getCommentBox(boolean tooltipOnRight) {
      return new CommentBox(this.exercise.getID(), controller, this, this.exercise.getMutableAnnotation(), tooltipOnRight);
    }

    /**
     * @param commentToPost
     * @param field
     * @see mitll.langtest.client.qc.QCNPFExercise#makeCommentEntry(String, ExerciseAnnotation)
     */
    @Override
    public void addIncorrectComment(final String commentToPost, final String field) {
      addAnnotation(field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost);
    }

    @Override
    public void addCorrectComment(final String field) {
      addAnnotation(field, ExerciseAnnotation.TYPICAL.CORRECT, "");
    }

    private void addAnnotation(final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost) {
      controller.getQCService().addAnnotation(exercise.getID(), field, status.toString(), commentToPost,
          new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Void result) {
            }
          });
    }
  }
}
