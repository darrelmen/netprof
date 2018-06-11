package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/5/17.
 */
public class SearchTypeahead {
  private final Logger logger = Logger.getLogger("SearchTypeahead");

  /**
   * Regular expression used to collapse all whitespace in a query string.
   */
  private static final String NORMALIZE_TO_SINGLE_WHITE_SPACE = "\\s+";
  private static final int DISPLAY_ITEMS = 15;
  private static final String WHITESPACE_STRING = " ";

  private final ExerciseController controller;
  private int req = 0;
  private final FeedbackExerciseList feedbackExerciseList;
  private CommonShell currentExercise = null;
  private final SearchHighlighter highlighter = new SearchHighlighter();
  private Button add;

  /**
   * @param controller
   * @param feedbackExerciseList
   * @param add
   * @see EditableExerciseList#getTypeahead
   */
  SearchTypeahead(ExerciseController controller, FeedbackExerciseList feedbackExerciseList, Button add) {
    this.controller = controller;
    this.feedbackExerciseList = feedbackExerciseList;
    this.add = add;
  }

  /**
   * @param textBox
   * @param <T>
   * @return
   * @see EditableExerciseList#getTypeahead
   */
  <T extends CommonShell> Typeahead getTypeaheadUsing(TextBox textBox) {
    SuggestOracle oracle = new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        //logger.info("make request for '" + request.getQuery() + "'");
        ExerciseListRequest exerciseListRequest = new ExerciseListRequest(req++, controller.getUser())
            .setPrefix(textBox.getText())
            .setLimit(DISPLAY_ITEMS)
            .setAddFirst(false)
            .setOnlyPlainVocab(true);

        controller.getExerciseService().getExerciseIds(exerciseListRequest, new AsyncCallback<ExerciseListWrapper<T>>() {
              @Override
              public void onFailure(Throwable caught) {
                controller.handleNonFatalError("searching for exercises", caught);
              }

              @Override
              public void onSuccess(ExerciseListWrapper<T> result) {
//                logger.info("getTypeaheadUsing got req back " + result.getReqID() + " vs " + req);
                makeSuggestionResponse(result, callback, request);
              }
            }
        );
      }
    };

    return getTypeahead(textBox, new Typeahead(oracle));
  }

  @NotNull
  private Typeahead getTypeahead(TextBox textBox, Typeahead typeahead) {
    typeahead.setDisplayItemCount(DISPLAY_ITEMS);
    typeahead.setMatcherCallback((query, item) -> true);
    typeahead.setUpdaterCallback(selectedSuggestion -> {
      currentExercise = ((ExerciseSuggestion) selectedSuggestion).getShell();
      add.setEnabled(currentExercise != null);
      return selectedSuggestion.getReplacementString();
    });

    textBox.getElement().setId("TextBox_exercise");
    typeahead.setWidget(textBox);

    Scheduler.get().scheduleDeferred((Command) () -> textBox.setFocus(true));
    textBox.setDirectionEstimator(true);   // automatically detect whether text is RTL

    return typeahead;
  }

  /**
   * @param result
   * @param callback
   * @param request
   * @see #getTypeaheadUsing
   */
  private void makeSuggestionResponse(ExerciseListWrapper<? extends CommonShell> result,
                                      SuggestOracle.Callback callback,
                                      SuggestOracle.Request request) {
    List<? extends CommonShell> exercises = result.getExercises();
    int size = exercises.size();
    if (size == 0) {
      clearCurrentExercise();
    }
    int limit = request.getLimit();

    if (size > limit) {
//      logger.info("makeSuggestionResponse From " + size + " to " + limit);
      exercises = exercises.subList(0, limit);
    }

    try {
      callback.onSuggestionsReady(request, getResponse(request, exercises, size, limit));
    } catch (Exception e) {
      logger.warning("got " + e);
    }
  }

  void clearCurrentExercise() {
    currentExercise = null;
    add.setEnabled(false);
  }

  @NotNull
  private SuggestOracle.Response getResponse(SuggestOracle.Request request, List<? extends CommonShell> exercises,
                                             int size, int limit) {
    int numberTruncated = Math.max(0, size - limit);
    //  logger.info("trunc " + numberTruncated);
    SuggestOracle.Response response = new SuggestOracle.Response(getSuggestions(request.getQuery(), exercises));
    response.setMoreSuggestionsCount(numberTruncated);
    return response;
  }

  @NotNull
  private Collection<SuggestOracle.Suggestion> getSuggestions(String query, List<? extends CommonShell> exercises) {
    Collection<SuggestOracle.Suggestion> suggestions = new ArrayList<>();
    feedbackExerciseList.clearMessage();
    String[] searchWords = normalizeSearch(query).split(WHITESPACE_STRING);

    exercises.forEach(resp -> suggestions.add(getSuggestion(searchWords, resp)));

    return suggestions;
  }

  /**
   * What to show for each trie search result - if match in vocab item, show it, if in context sentence, show that.
   *
   * @param searchWords
   * @param resp
   * @return
   */
  private ExerciseSuggestion getSuggestion(String[] searchWords, CommonShell resp) {
    //String lcQ = query.toLowerCase();

    String foreignLanguage = resp.getFLToShow();
    //boolean found = foreignLanguage.toLowerCase().contains(lcQ) || resp.getEnglish().toLowerCase().contains(lcQ);

    String formattedSuggestion =// found ?
        (foreignLanguage + " - " + resp.getEnglish());
    //:
    //    (resp.getCforeignLanguage() + (resp.getCenglish().isEmpty() ? "" : (" - " + resp.getCenglish())) + " (Context)");

    String lowerCaseSuggestion = normalizeSuggestion(formattedSuggestion);
    String displayString = highlighter.getHighlightedString(searchWords, formattedSuggestion, lowerCaseSuggestion);
    // logger.info(resp.getID() + " displayString " + displayString);
    return createSuggestion(foreignLanguage, displayString, resp);
  }

  /**
   * @see #makeSuggestionResponse
   */
  private ExerciseSuggestion createSuggestion(String replacementString,
                                              @IsSafeHtml String displayString,
                                              CommonShell shell) {
    return new ExerciseSuggestion(replacementString, displayString, shell);
  }

  /**
   * Normalize the search key by making it lower case, removing multiple spaces,
   * apply whitespace masks, and make it lower case.
   */
  private String normalizeSearch(String search) {
    // Use the same whitespace masks and case normalization for the search
    // string as was used with the candidate values.
    search = normalizeSuggestion(search);

    // Remove all excess whitespace from the search string.
    search = search.replaceAll(NORMALIZE_TO_SINGLE_WHITE_SPACE, WHITESPACE_STRING);

    return search.trim();
  }

  /**
   * Takes the formatted suggestion, makes it lower case and blanks out any
   * existing whitespace for searching.
   */
  private String normalizeSuggestion(String formattedSuggestion) {
    // Formatted suggestions should already have normalized whitespace. So we
    // can skip that step.

    // Lower case suggestion.
    formattedSuggestion = formattedSuggestion.toLowerCase(Locale.ROOT);

    // Apply whitespace.
//    if (whitespaceChars != null) {
//      for (int i = 0; i < whitespaceChars.length; i++) {
//        char ignore = whitespaceChars[i];
//        formattedSuggestion = formattedSuggestion.replace(ignore,
//            WHITESPACE_CHAR);
//      }
//    }
    return formattedSuggestion;
  }

  /**
   * @return
   * @see EditableExerciseList#isOnList
   * @see EditableExerciseList#onClickAdd
   */
  CommonShell getCurrentExercise() {
    return currentExercise;
  }

  public void grabFocus() {
    //getTypeahead()
  }

  private static class ExerciseSuggestion extends MultiWordSuggestOracle.MultiWordSuggestion {
    private CommonShell shell;

    /**
     * @param repl
     * @param disp
     * @param shell
     * @see #createSuggestion(String, String, CommonShell)
     */
    ExerciseSuggestion(String repl, String disp, CommonShell shell) {
      super(repl, disp);
      this.shell = shell;
    }

    public ExerciseSuggestion() {
    }

    public CommonShell getShell() {
      return shell;
    }
  }
}
