package mitll.langtest.client.grading;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.result.ResultManager;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.grade.ResultsAndGrades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Allows a grader to grade answers entered in the default mode.
 * <p/>
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GradingExercisePanel extends ExercisePanel {
  private static final int ONE_QUESTION_PAGE_SIZE = 3;
  private static final int TWO_QUESTION_PAGE_SIZE = ONE_QUESTION_PAGE_SIZE;

  private static final int BIG_ONE_QUESTION_PAGE_SIZE = 8;
  private static final int BIG_TWO_QUESTION_PAGE_SIZE = BIG_ONE_QUESTION_PAGE_SIZE / 2;
  private static final List<Boolean> BOOLEANS = Arrays.asList(true, false);
  private final UserFeedback userFeedback;

  /**
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   * @param listContainer
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   */
  public GradingExercisePanel(final CommonExercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                              final ExerciseController controller, ListInterface listContainer) {
    super(e,service,userFeedback,controller, listContainer);
    this.userFeedback = userFeedback;
    enableNextButton(true);
  }

  @Override
  protected void addItemHeader(CommonExercise e) {}

  /**
   * If controller is english only, then show the answer too.
   *
   * @see #getQuestionPanel
   * @param i
   * @param total
   * @param engQAPair
   * @param englishPair
   * @param flQAPair
   * @param showAnswer
   * @param toAddTo
   */
/*
  @Override
  protected void getQuestionHeader(int i, int total, CommonExercise.QAPair engQAPair, Exercise.QAPair englishPair,
                                   Exercise.QAPair flQAPair, boolean showAnswer, HasWidgets toAddTo) {
    String english = engQAPair.getQuestion();
    String prefix = "Question" + (total > 1 ? " #" + i : "") + " : ";

    if (showAnswer) {
      String answer = engQAPair.getAnswer();
      toAddTo.add(new HTML("<br></br><b>" + prefix + "</b>" + english));
      toAddTo.add(new HTML("<b>Answer : &nbsp;&nbsp;</b>" + answer + "<br></br>"));
    } else {
      String questionHeader = prefix + flQAPair.getQuestion();
      toAddTo.add(new HTML("<h4>" + questionHeader + " / " + english + "</h4>"));
    }
  }
*/

/*
  @Override
  protected void addQuestionPrompt(Panel vp, Exercise e) {

    vp.add(new Heading(5, "Reference Audio:"));
    String refAudio = e.getRefAudio();
    AudioTag audioTag = new AudioTag();
    if (refAudio == null || refAudio.isEmpty()) refAudio = e.getSlowAudioRef();
    if (refAudio.endsWith(".wav")) {
      vp.add(new HTML(audioTag.getAudioTag(refAudio)));
    }
    else {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      sb.appendHtmlConstant(refAudio);
      vp.add(new HTML(sb.toSafeHtml()));
    }
    super.addQuestionPrompt(vp,e);
  }
*/

    /**
     * If controller is english only, then show the answer too.
     * @return true if in english only mode
     */
  @Override
  protected boolean shouldShowAnswer() {
    return super.shouldShowAnswer() || controller.getEnglishOnly();
  }

  /**
   * Partitions results into 3 (or fewer) separate tables for each of the
   * possible spoken/written & english/f.l. combinations.
   * <br></br>
   * Uses a result manager table (simple pager).  {@link mitll.langtest.client.result.ResultManager#getTable}<br></br>
   * If the controller says this is an English only grading mode, then only show english answers.
   * @see ExercisePanel#ExercisePanel
   * @param exercise
   * @param service
   * @param controller
   * @param index      of the question (0 for first, 1 for second, etc.)
   * @return
   * @seex ExercisePanel#getQuestionPanel
   */
/*  @Override
  protected Widget getAnswerWidget(final CommonExercise exercise, final LangTestDatabaseAsync service,
                                   final ExerciseController controller, final int index) {
    final TabPanel vp = new TabPanel();

    // TODO fix this eventually
    final int n = 1;//exercise.getNumQuestions();
    final GradingExercisePanel outer = this;
    final boolean englishOnly = controller.getEnglishOnly();
    service.getResultsForExercise(exercise.getID(), controller.isArabicTextDataCollect(), new AsyncCallback<ResultsAndGrades>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(ResultsAndGrades resultsAndGrades) {
        //System.out.println("getResultsForExercise (success) : " + exercise.getID() + " : " + resultsAndGrades);

        boolean anyAnswers = false;
        int count = countDistinctTypes(resultsAndGrades);
        boolean bigPage = count == 1 || englishOnly || controller.getNumGradesToCollect() > 1;
        List<Boolean> graded = new ArrayList<Boolean>();
        for (boolean isSpoken : BOOLEANS) {
          Map<Boolean, List<Result>> langToResult = resultsAndGrades.spokenToLangToResult.get(isSpoken);
          if (langToResult != null) { // there might not be any written types
            anyAnswers = addResultSet(resultsAndGrades, anyAnswers, bigPage, isSpoken, langToResult, vp, graded);
          }
        }
        if (!anyAnswers) {
          Tab tab = new Tab();
          tab.setHeading("No answers yet.");
          vp.add(tab);
        }
        selectTab(graded);
      }

      private void selectTab(List<Boolean> graded) {
        boolean anySelected = false;
        for (int i = 0; i < graded.size(); i++) {
          if (!graded.get(i)) {
            vp.selectTab(i);
            anySelected = true;
            break;
          }
        }
        if (!anySelected) {
          vp.selectTab(0);
        }
      }

      public boolean addResultSet(ResultsAndGrades resultsAndGrades, boolean anyAnswers, boolean bigPage,
                                  boolean isSpoken, Map<Boolean, List<Result>> langToResult, Panel toAddTo,
                                  List<Boolean> graded) {
        for (boolean isForeign : BOOLEANS) {
          if (englishOnly && isForeign) {
            //System.out.println("\tskipping! spoken : " + isSpoken + " isFLQ " + isForeign + " has " + langToResult.size() + " results");

            continue; // skip non-english
          }
          anyAnswers = addResults(resultsAndGrades, anyAnswers, bigPage, isSpoken, langToResult, isForeign, toAddTo, graded);
        }
        return anyAnswers;
      }

      public boolean addResults(ResultsAndGrades resultsAndGrades, boolean anyAnswers, boolean bigPage,
                                boolean isSpoken, Map<Boolean, List<Result>> langToResult, boolean isForeign, Panel toAddTo,
                                List<Boolean> graded) {
        List<Result> results = langToResult.get(isForeign);
        if (results != null) {
          SortedSet<String> labels = new TreeSet<String>();
          Map<String, List<Result>> labelToResults = new HashMap<String, List<Result>>();
          for (Result r : results) {
            String audioType = r.getAudioType().trim();
            labels.add(audioType);
            List<Result> results1 = labelToResults.get(audioType);
            if (results1 == null) labelToResults.put(audioType, results1 = new ArrayList<Result>());
            results1.add(r);
          }

          for (String label : labels) {
            List<Result> toUse = labelToResults.get(label);
            System.out.println("\tlabel " + label + " spoken : " + isSpoken + " isFLQ " + isForeign + " has " + toUse.size() + " results");
            anyAnswers = true;
            boolean englishOrForeign = isForeign;
            if (label.contains("english")) englishOrForeign = false;
            String prompt = getPrompt(isSpoken, englishOrForeign, outer) + " (" + label + " response)";
            //System.out.println("\tgetResultsForExercise add answer group for results (index = " + index + ") size = " + toUse.size());

            Tab child = addAnswerGroup(resultsAndGrades.grades, toUse, bigPage, prompt, service, controller.getProps(), n, index, label);
            toAddTo.add(child);
            graded.add(allGraded(toUse, resultsAndGrades.grades, index, controller.getNumGradesToCollect()));
          }
        } else {
          System.out.println("\tspoken : " + isSpoken + " isFLQ " + isForeign);
        }
        return anyAnswers;
      }
    });
    addAnswerWidget(index, vp);

    return vp;
  }*/

  /**
   * Are all results graded?
   * @param results
   * @param grades
   * @param qIndex
   * @param numGrades
   * @return
   */
  private boolean allGraded(Collection<Result> results, Collection<Grade> grades, int qIndex, int numGrades) {
    List<Result> filtered = getResultsForThisQuestion(results, qIndex);
    Set<Integer> expected = new HashSet<Integer>();
    for (Result r : filtered) expected.add(r.uniqueID);
    for (Grade g : grades) {
      if (g.gradeIndex == numGrades - 1) {
        expected.remove(g.resultID);
      }
    }

    return expected.isEmpty();
  }

  private String getPrompt(boolean isSpoken, boolean isForeign, GradingExercisePanel outer) {
    boolean isEnglish = !isForeign;
    return isSpoken ? outer.getSpokenPrompt(isEnglish) : outer.getWrittenPrompt(isEnglish);
  }

  /**
   * TODO : really hacky trying to determine how big the table should be
   *
   * @param grades
   * @param results
   * @param bigPage
   * @param prompt
   * @param service
   * @param propertyHandler
   * @param numQuestions
   * @param index
   * @param tabHeading
   * @return
   * @seex #getAnswerWidget
   */
  private Tab addAnswerGroup(Collection<Grade> grades,
                             List<Result> results, boolean bigPage, String prompt,
                             LangTestDatabaseAsync service, PropertyHandler propertyHandler, int numQuestions, int index,
                             String tabHeading) {
    Tab tab = new Tab();
    tab.setHeading(tabHeading);
    Panel vp = new VerticalPanel();
    tab.add(vp);
    boolean isText = tabHeading.contains("text");
    int oneQuestionPageSize = bigPage ? BIG_ONE_QUESTION_PAGE_SIZE : (isText ? 4 : ONE_QUESTION_PAGE_SIZE);
    int twoQuestionPageSize = bigPage ? BIG_TWO_QUESTION_PAGE_SIZE : TWO_QUESTION_PAGE_SIZE;
    HTML child = new HTML(prompt);
    vp.add(child);

    String responseType = "either";
    if (isText) responseType = "text";
    else if (tabHeading.contains("audio")) responseType = "audio";
    Widget child1 = showResults(results, grades, service, propertyHandler, numQuestions > 1, index,
      oneQuestionPageSize, twoQuestionPageSize, controller.getUser(), responseType);

    vp.add(child1);
    return tab;
  }

  /**
   * How many distinct types there are - combinations of spoken/written & flq/english q.
   *
   * @param resultsAndGrades
   * @return 1-3
   * @seex #getAnswerWidget(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  private int countDistinctTypes(ResultsAndGrades resultsAndGrades) {
    int count = 0;
    for (boolean s : BOOLEANS) {
      Map<Boolean, List<Result>> langToResult = resultsAndGrades.spokenToLangToResult.get(s);
      if (langToResult != null) { // there might not be any written types
        for (boolean l : BOOLEANS) {
          List<Result> results = langToResult.get(l);
          if (results != null) {
            count++;
          }
        }
      }
    }
    return count;
  }

  /**
   * @param results
   * @param grades
   * @param service
   * @param propertyHandler
   * @param moreThanOneQuestion
   * @param index
   * @param pageSize
   * @param twoQPageSize
   * @param grader
   * @return
   * @seex #getAnswerWidget(mitll.langtest.shared.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  private Widget showResults(Collection<Result> results, Collection<Grade> grades,
                             LangTestDatabaseAsync service, PropertyHandler propertyHandler,
                             boolean moreThanOneQuestion, int index, int pageSize, int twoQPageSize, int grader,
                             String responseType) {
    boolean isText = responseType.equals("text");
    ResultManager rm = new GradingResultManager(service, userFeedback, false, propertyHandler, isText);
    rm.setPageSize(pageSize);
    if (moreThanOneQuestion) {
      results = getResultsForThisQuestion(results, index);
      rm.setPageSize(twoQPageSize);
    }

    Widget table = rm.getTable(results, false, grades, grader, controller.getNumGradesToCollect());

    String tip;
    boolean isAudio = responseType.equals("audio");

    if (isText) {
      tip = "(1) Indicates a wrong answer.<br/>" +
        "(2) Indicates a partial answer with one or more spelling or grammatical errors.<br/>" +
        "(3) Indicates a partial answer.<br/>" +
        "(4) Indicates a correct answer with one or more spelling or grammatical errors.<br/>" +
        "(5) Indicates a correct yet (grammatically or structurally) incomplete answer: <br/>" +
        "The meaning expressed by the answer provided is correct but the way it was phrased is not grammatically or structurally perfect (the phrase or sentence structure is not complete; it is not how a native speaker would normally answer it).<br/>" +
        "(6) Indicates a correct and complete answer. (The answer is perfect: it is error-free and structurally/ grammatically complete.)";
    } else {
      if (isAudio) {
        tip = "(1) Indicates a wrong answer.<br/>" +
          "(2) Indicates a partial answer with one or more pronunciation errors (one or more sounds that are mispronounced).<br/>" +
          "(3) Indicates a partial answer (no pronunciation errors).<br/>" +
          "(4) Indicates a correct answer with one or more pronunciation errors (one or more sounds that are mispronounced).<br/>" +
          "(5) Indicates a correct answer (no pronunciation errors).";
      } else {
        tip = "Text responses :" + "(1) Indicates a wrong answer.<br/>" +
          "(2) Indicates a partial answer with one or more spelling or grammatical errors.<br/>" +
          "(3) Indicates a partial answer.<br/>" +
          "(4) Indicates a correct answer with one or more spelling or grammatical errors.<br/>" +
          "(5) Indicates a correct yet (grammatically or structurally) incomplete answer: <br/>" +
          "The meaning expressed by the answer provided is correct but the way it was phrased is not grammatically or structurally perfect (the phrase or sentence structure is not complete; it is not how a native speaker would normally answer it).<br/>" +
          "(6) Indicates a correct and complete answer. (The answer is perfect: it is error-free and structurally/ grammatically complete.)<br/>" +
          "Audio responses:<br/>" +
          "(1) Indicates a wrong answer.<br/>" +
          "(2) Indicates a partial answer with one or more pronunciation errors (one or more sounds that are mispronounced).<br/>" +
          "(3) Indicates a partial answer (no pronunciation errors).<br/>" +
          "(4) Indicates a correct answer with one or more pronunciation errors (one or more sounds that are mispronounced).<br/>" +
          "(5) Indicates a correct answer (no pronunciation errors).<br/>";
      }
    }

    Popover popover = new Popover();
    showPopover(popover,table,"Grade Guidance",tip, Placement.RIGHT);

    return table;
  }

  private void showPopover(Popover popover, Widget w, String heading, String message, Placement placement) {
    popover.setWidget(w);
    popover.setText(message);
    popover.setHtml(true);
    popover.setHeading(heading);
    popover.setPlacement(placement);
    popover.reconfigure();
    popover.show();
  }

  private List<Result> getResultsForThisQuestion(Collection<Result> results, int index) {
    List<Result> filtered = new ArrayList<Result>();
    for (Result r : results) if (r.qid == index) filtered.add(r);
    return filtered;
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) { return ""; }

  /**
   * Consider : on the server, notice which audio posts have arrived, and take the latest ones...
   *
   * @param controller
   * @paramx completedExercise
   */
/*  @Override
  public void postAnswers(ExerciseController controller, ExerciseShell completedExercise) {
    exerciseList.loadNextExercise(completedExercise);
  }*/

  @Override
  protected NavigationHelper getNavigationHelper(ExerciseController controller) {
    return new NavigationHelper(exercise,controller, this, exerciseList, true, true) {
      @Override
      protected String getNextButtonText() {
        return "Next Ungraded";
      }
    };
  }
}
