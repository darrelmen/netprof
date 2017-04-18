package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
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

  SearchTypeahead(ExerciseController controller, FeedbackExerciseList feedbackExerciseList) {
    this.controller = controller;
    this.feedbackExerciseList = feedbackExerciseList;
  }

  /**
   * @param whichField
   * @param w
   * @param <T>
   * @return
   * @see EditableExerciseList#getTypeahead
   */
   <T extends CommonShell> Typeahead getTypeaheadUsing(final String whichField, TextBox w) {
    SuggestOracle oracle = new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        logger.info("make request for '" + request.getQuery() + "'");

        ExerciseListRequest exerciseListRequest = new ExerciseListRequest(req++, controller.getUser())
            .setPrefix(w.getText())
            .setLimit(DISPLAY_ITEMS)
            .setAddFirst(false);

        controller.getExerciseService().getExerciseIds(exerciseListRequest, new AsyncCallback<ExerciseListWrapper<T>>() {
              @Override
              public void onFailure(Throwable caught) {
              }

              @Override
              public void onSuccess(ExerciseListWrapper<T> result) {
                logger.info("getTypeaheadUsing got req back " + result.getReqID() + " vs " + req);
                makeSuggestionResponse(result, callback, request);
              }
            }
        );
      }
    };

    Typeahead typeahead = new Typeahead(oracle);
    typeahead.setDisplayItemCount(DISPLAY_ITEMS);
    typeahead.setMatcherCallback(new Typeahead.MatcherCallback() {
      @Override
      public boolean compareQueryToItem(String query, String item) {
        return true;
      }
    });
    typeahead.setUpdaterCallback(new Typeahead.UpdaterCallback() {
      @Override
      public String onSelection(SuggestOracle.Suggestion selectedSuggestion) {
        ExerciseSuggestion exerciseSuggestion = (ExerciseSuggestion) selectedSuggestion;
        currentExercise = exerciseSuggestion.getShell();
        return selectedSuggestion.getReplacementString();
      }
    });

    w.getElement().setId("TextBox_" + whichField);
    typeahead.setWidget(w);
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
    exercises.sort(new Comparator<CommonShell>() {
      @Override
      public int compare(CommonShell o1, CommonShell o2) {
        return o1.getForeignLanguage().compareTo(o2.getForeignLanguage());
      }
    });

    int size = exercises.size();
    if (size == 0) clearCurrentExercise();
    int limit = request.getLimit();

    if (size > limit) {
      logger.info("makeSuggestionResponse From " + size + " to " + limit);
      exercises = exercises.subList(0, limit);
    }

    int numberTruncated = Math.max(0, size - limit);
    //  logger.info("trunc " + numberTruncated);

    SuggestOracle.Response response = new SuggestOracle.Response(getSuggestions(request.getQuery(), exercises));
    response.setMoreSuggestionsCount(numberTruncated);

    try {
      callback.onSuggestionsReady(request, response);
    } catch (Exception e) {
      logger.warning("got " + e);
    }
  }

   void clearCurrentExercise() {
    currentExercise = null;
  }

  @NotNull
  private Collection<SuggestOracle.Suggestion> getSuggestions(String query, List<? extends CommonShell> exercises) {
    Collection<SuggestOracle.Suggestion> suggestions = new ArrayList<>();
    feedbackExerciseList.clearMessage();
    String[] searchWords = normalizeSearch(query).split(WHITESPACE_STRING);

    for (CommonShell resp : exercises) {
      suggestions.add(getSuggestion(searchWords, resp));
    }
    return suggestions;
  }

  private ExerciseSuggestion getSuggestion(String[] searchWords, CommonShell resp) {
    String formattedSuggestion = resp.getForeignLanguage() + " - " + resp.getEnglish();
    String lowerCaseSuggestion = normalizeSuggestion(formattedSuggestion);
    String displayString = highlighter.getHighlightedString(searchWords, formattedSuggestion, lowerCaseSuggestion);

    // logger.info(resp.getID() + " displayString " + displayString);
    ExerciseSuggestion suggestion = createSuggestion(resp.getForeignLanguage(),
        displayString,
        resp);

    return suggestion;
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
   * @see EditableExerciseList#isOnList
   * @return
   */
  CommonShell getCurrentExercise() {
    return currentExercise;
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
