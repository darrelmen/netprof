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

package mitll.langtest.client.flashcard;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonAnnotatable;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/14.
 * stores state -- how many items have been done, where are we in the list
 * so we can skip around in the list...? if get to end - get score of all the ones we've answered.
 * TODOx : concept of rounds explicit?
 * TODO : review table...?
 */
public class StatsFlashcardFactory<L extends CommonShell, T extends CommonExercise>
    extends ExercisePanelFactory<L, T>
    implements FlashcardContainer {
  private final Logger logger = Logger.getLogger("StatsFlashcardFactory");

  private static final boolean ADD_KEY_BINDING = true;

  final ControlState controlState;
  private List<L> allExercises;


  private String selectionID = "";
  private final String instance;
  final StickyState sticky;
  private Map<String, Collection<String>> selection = new HashMap<>();

  private Widget contentPanel;

  //StatsPracticePanel currentFlashcard = null;
  FlashcardPanel<?> currentFlashcard = null;
  private NewContentChooser navigation;
  final KeyStorage storage;

  final MySoundFeedback soundFeedback = new MySoundFeedback(this.controller.getSoundManager());

  //  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param exerciseList
   * @param instance
   * @see mitll.langtest.client.banner.PracticeHelper#getFactory
   */
  public StatsFlashcardFactory(ExerciseController controller,
                               ListInterface<L, T> exerciseList,
                               String instance) {
    super(controller, exerciseList);
    controlState = new ControlState();
    this.instance = instance;

    if (exerciseList != null) { // TODO ? can this ever happen?
      exerciseList.addListChangedListener(new ListChangeListener<L>() {
        /**
         * @param items
         * @param selectionID
         * @see mitll.langtest.client.list.ExerciseList#rememberAndLoadFirst
         */
        @Override
        public void listChanged(List<L> items, String selectionID) {
          StatsFlashcardFactory.this.selectionID = selectionID;
          allExercises = items;
          //     logger.info("StatsFlashcardFactory : " + selectionID + " got new set of items from list. " + items.size());
          reset();
        }
      });
    }

    storage = new KeyStorage(controller) {
      @Override
      protected String getKey(String name) {
        return (selectionID.isEmpty() ? "" : selectionID + "_") + super.getKey(name); // in the context of this selection
      }
    };

    sticky = getSticky();
    controlState.setStorage(storage);
    // logger.info("setting shuffle --------------------- " +controlState.isShuffle()+ "\n");
    if (exerciseList != null) {
      exerciseList.simpleSetShuffle(controlState.isShuffle());
    }
  }

  @NotNull
  StickyState getSticky() {
    return new StickyState(storage);
  }

  /**
   * @param e
   * @return
   * @see mitll.langtest.client.list.ExerciseList#addExerciseWidget
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels
   */
  @Override
  public Panel getExercisePanel(T e) {
    sticky.storeCurrent(e);

   // boolean showRecordingFlashcard = shouldShowRecordingFlashcard();

    currentFlashcard = controller.shouldRecord() || true ?
        currentFlashcard = getFlashcard(e) :
        getNoRecordFlashcardPanel(e.getCommonAnnotatable());
/*    if (!showRecordingFlashcard) {
//      logger.info("getExercisePanel no recording ");
      currentFlashcard = null;
    }*/

    //logger.info("getExercisePanel return flashcard ");
    return currentFlashcard;
  }

  @NotNull
  protected StatsPracticePanel<L, T> getFlashcard(T e) {
    return new StatsPracticePanel<L, T>(this,
        controlState,
        controller,
        soundFeedback,
        PolyglotDialog.PROMPT_CHOICE.NOT_YET,
        e.getCommonAnnotatable(),
        sticky,
        exerciseList);
  }

/*  boolean shouldShowRecordingFlashcard() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    boolean hasModel = (projectStartupInfo != null) && projectStartupInfo.isHasModel();
    return hasModel && controller.isRecordingEnabled();
  }*/

  private FlashcardPanel<CommonAnnotatable> getNoRecordFlashcardPanel(final CommonAnnotatable e) {
    FlashcardPanel<CommonAnnotatable> noRecord = new FlashcardPanel<CommonAnnotatable>(e,
        StatsFlashcardFactory.this.controller,
        ADD_KEY_BINDING,
        StatsFlashcardFactory.this.controlState,
        soundFeedback,
        soundFeedback.getEndListener(),
        StatsFlashcardFactory.this.instance,
        exerciseList
    ) {

      @Override
      protected void gotShuffleClick(boolean b) {
        sticky.resetStorage();
        super.gotShuffleClick(b);
      }
    };
    noRecord.addWidgets(e, controller, controlState);
    return noRecord;
  }

  void reset() {
    sticky.reset();
  }

  /**
   * Pull state out of cache and re-populate correct, incorrect, and score history.
   *
   * @see mitll.langtest.client.banner.PracticeHelper#getMyListLayout
   */
  public void populateCorrectMap() {
    sticky.populateCorrectMap();
  }

  public void resetStorage() {
    sticky.resetStorage();
  }

  public void setSelection(Map<String, Collection<String>> selection) {
    this.selection = selection;
  }

  public void setContentPanel(Widget contentPanel) {
    this.contentPanel = contentPanel;
  }

//  private int counter = 0;

  public void setNavigation(NewContentChooser navigation) {
    this.navigation = navigation;
  }

  private int getFirstExercise() {
    return allExercises.iterator().next().getID();
  }

  public void reload() {
    exerciseList.reload(selection);
  }

  @Override
  public void startOver() {
    int lastID = allExercises.isEmpty() ? -1 : allExercises.get(allExercises.size() - 1).getID();
    int currentExerciseID = sticky.getCurrentExerciseID();
//logger.info("startOver : current " + currentExerciseID + " = " + statsFlashcardFactory.mode);
    if (currentExerciseID != -1 && currentExerciseID != lastID) {
      exerciseList.loadExercise(currentExerciseID);
    } else {
      reallyStartOver();
    }
  }

  private void reallyStartOver() {
    sticky.reset();
    sticky.resetStorage();
    loadFirstExercise();
  }

  public void loadFirstExercise() {
    if (!allExercises.isEmpty()) {
      exerciseList.loadExercise(getFirstExercise());
    }
  }

  /**
   * @see PolyglotPracticePanel#getDeviceTypeValue
   * @return
   */
  @Override
  public int getNumExercises() {
    return allExercises.size();
  }

  public void showDrill() {
    navigation.showDrill();
  }  public void showQuiz() {
    navigation.showQuiz();
  }

  public void styleContent(boolean showCard) {
    if (showCard) {
      contentPanel.removeStyleName("noWidthCenterPractice");
      contentPanel.addStyleName("centerPractice");
    } else {
      contentPanel.removeStyleName("centerPractice");
      contentPanel.addStyleName("noWidthCenterPractice");
    }
  }
}