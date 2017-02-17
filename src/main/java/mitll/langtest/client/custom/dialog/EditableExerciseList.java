package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SuggestOracle;
import mitll.langtest.client.bootstrap.ButtonGroupSectionWidget;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListChangeListener;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.services.ExerciseService;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 2/14/17.
 */
class EditableExerciseList extends NPExerciseList<ButtonGroupSectionWidget> {
  private final Logger logger = Logger.getLogger("EditableExerciseList");

  /**
   * @see #makeDeleteButton
   */
  private static final String REMOVE_FROM_LIST = "Remove from list";

  private EditItem editItem;
  //  private final boolean includeAddItem;
  // private final ExerciseServiceAsync exerciseServiceAsync = GWT.create(ExerciseService.class);
  private UserList<CommonShell> list;
  private final ExerciseServiceAsync exerciseServiceAsync = GWT.create(ExerciseService.class);

  /**
   * @param controller
   * @param editItem
   * @param right
   * @param instanceName
   * @param list
   * @paramx includeAddItem
   * @see EditItem#makeExerciseList
   */
  public EditableExerciseList(ExerciseController controller,
                              EditItem editItem,
                              Panel right,
                              String instanceName,
                              //  boolean includeAddItem,
                              UserList<CommonShell> list) {
    super(right, GWT.create(ExerciseService.class),
        controller.getFeedback(), controller,
        false, instanceName, false, false, ActivityType.EDIT);
    this.editItem = editItem;
    this.list = list;

    if (list.isEmpty()) delete.setEnabled(false);

  }

  protected DivWidget getOptionalWidget() {
    DivWidget widgets = new DivWidget();

    DivWidget addW = getAddButtonContainer();
    widgets.add(addW);

    DivWidget delW = getRemoveButtonContainer();
    widgets.add(delW);

//    MaterialAutoComplete acList = new MaterialAutoComplete(new ExerciseOracle());
//    widgets.add(acList);

    addListChangedListener(new ListChangeListener<CommonShell>() {
      @Override
      public void listChanged(List<CommonShell> items, String selectionID) {
//        logger.warning("got list changed - list is " + isEmpty());
        enableRemove(!isEmpty());
      }
    });
    return widgets;
  }

  TextBox quickAddText;

  private Typeahead getTypeahead(final String whichField) {
    return getTypeaheadUsing(whichField, quickAddText = new TextBox());
  }

  int req = 0;
  CommonShell currentExercise = null;

  private <T extends CommonShell> Typeahead getTypeaheadUsing(final String whichField, TextBox w) {
    SuggestOracle oracle = new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        logger.info("make requesst for '" + request.getQuery() + "'");

        ExerciseListRequest exerciseListRequest = new ExerciseListRequest(req++, controller.getUser()).setPrefix(w.getText());
        exerciseServiceAsync.getExerciseIds(exerciseListRequest, new AsyncCallback<ExerciseListWrapper<T>>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(ExerciseListWrapper<T> result) {
                logger.info("got req back " + result.getReqID() + " vs " + req);
                makeSuggestionResponse(result, callback, request);
              }
            }
        );
      }
    };
    Typeahead typeahead = new Typeahead(oracle);
    typeahead.setDisplayItemCount(5);
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

        //  logger.info("Got " +exerciseSuggestion.getID());
        currentExercise = exerciseSuggestion.getShell();
        return selectedSuggestion.getReplacementString();
      }
    });

    w.getElement().setId("TextBox_" + whichField);
    typeahead.setWidget(w);
    configureTextBox(w);
    // addCallbacks(typeahead);
    return typeahead;
  }

  private void configureTextBox(final TextBox w) {
    w.addKeyUpHandler(getKeyUpHandler());
  }

  private KeyUpHandler getKeyUpHandler() {
    return event -> {
      logger.info("got key up");
    };
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
    if (size == 0) currentExercise = null;
    int limit = request.getLimit();

    if (size > limit) {
      logger.info("makeSuggestionResponse From " + size + " to " + limit);
      exercises = exercises.subList(0, limit);
    }

    int numberTruncated = Math.max(0, size - limit);

    logger.info("trunc " + numberTruncated);

    SuggestOracle.Response response = new SuggestOracle.Response(getSuggestions(request.getQuery(), exercises));
    response.setMoreSuggestionsCount(numberTruncated);

    try {
      callback.onSuggestionsReady(request, response);
    } catch (Exception e) {
      logger.warning("got " + e);
    }
  }

  @NotNull
  private Collection<SuggestOracle.Suggestion> getSuggestions(String query, List<? extends CommonShell> exercises) {
    Collection<SuggestOracle.Suggestion> suggestions = new ArrayList<>();
    logger.info("getSuggestions converting " + exercises.size());

    String before = query;
    query = normalizeSearch(query);

    logger.info("getSuggestions before '" + before + "'");
    logger.info("getSuggestions after  '" + query + "'");

    String[] searchWords = query.split(WHITESPACE_STRING);

    logger.info("getSuggestions searchWords length '" + searchWords.length + "'");

    for (CommonShell resp : exercises) {
      //suggestions.add(new ExerciseSuggestion(resp));
      ExerciseSuggestion suggestion = getSuggestion(searchWords, resp);

/*      ExerciseSuggestion suggestion = createSuggestion(resp.getForeignLanguage(),
          resp.getForeignLanguage() + " - " + resp.getEnglish(),
          resp.getID());
      */
      suggestions.add(suggestion);
    }

    logger.info("getSuggestions returning " + suggestions.size());

    return suggestions;
  }

  private ExerciseSuggestion getSuggestion(String[] searchWords, CommonShell resp) {
    int cursor = 0;
    int index = 0;
    String formattedSuggestion = resp.getForeignLanguage() + " - " + resp.getEnglish();
    String candidate = normalizeSuggestion(formattedSuggestion);
    // Create strong search string.
    SafeHtmlBuilder accum = new SafeHtmlBuilder();

    while (true) {
      WordBounds wordBounds = findNextWord(candidate, searchWords, index);
      if (wordBounds == null) {
        break;
      }
      if (wordBounds.startIndex == 0 ||
          WHITESPACE_CHAR == candidate.charAt(wordBounds.startIndex - 1)) {
        String part1 = formattedSuggestion.substring(cursor, wordBounds.startIndex);
        String part2 = formattedSuggestion.substring(wordBounds.startIndex,
            wordBounds.endIndex);
        cursor = wordBounds.endIndex;
        accum.appendEscaped(part1);
        accum.appendHtmlConstant("<strong>");
        accum.appendEscaped(part2);
        accum.appendHtmlConstant("</strong>");
      }
      index = wordBounds.endIndex;
    }

//    // Check to make sure the search was found in the string.
//    if (cursor == 0) {
//      continue;
//    }

    accum.appendEscaped(formattedSuggestion.substring(cursor));

    logger.info(resp.getID() + " formatted     " + formattedSuggestion);
    String displayString = accum.toSafeHtml().asString();
    logger.info(resp.getID() + " displayString " + displayString);
    ExerciseSuggestion suggestion = createSuggestion(resp.getForeignLanguage(),
        displayString,
        resp);

    return suggestion;
  }

  /**
   * Regular expression used to collapse all whitespace in a query string.
   */
  private static final String NORMALIZE_TO_SINGLE_WHITE_SPACE = "\\s+";

  /**
   * Normalize the search key by making it lower case, removing multiple spaces,
   * apply whitespace masks, and make it lower case.
   */
  private String normalizeSearch(String search) {
    // Use the same whitespace masks and case normalization for the search
    // string as was used with the candidate values.
    search = normalizeSuggestion(search);

    // Remove all excess whitespace from the search string.
    search = search.replaceAll(NORMALIZE_TO_SINGLE_WHITE_SPACE,
        WHITESPACE_STRING);

    return search.trim();
  }

  /**
   * @see #makeSuggestionResponse
   */
    /*    @Override
        public String getDisplayString() {
          return resp.getForeignLanguage() + " - " + resp.getEnglish();
        }

        @Override
        public String getReplacementString() {
          return resp.getForeignLanguage();
        }*/
  protected ExerciseSuggestion createSuggestion(
      String replacementString, @IsSafeHtml String displayString, CommonShell shell) {
    return new ExerciseSuggestion(replacementString, displayString, shell);
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

  private static class ExerciseSuggestion //implements SuggestOracle.Suggestion {
      extends MultiWordSuggestOracle.MultiWordSuggestion {
    //private int exid;
    CommonShell shell;

    public ExerciseSuggestion(String repl, String disp, CommonShell shell) {
      super(repl, disp);
      this.shell = shell;
    }

    public ExerciseSuggestion() {
    }

    public CommonShell getShell() {
      return shell;
    }

  }

  /**
   * Returns a {@link MultiWordSuggestOracle.WordBounds} representing the first word in {@code
   * searchWords} that is found in candidate starting at {@code indexToStartAt}
   * or {@code null} if no words could be found.
   */
  private WordBounds findNextWord(String candidate, String[] searchWords, int indexToStartAt) {
    WordBounds firstWord = null;
    for (String word : searchWords) {
      int index = candidate.indexOf(word, indexToStartAt);
      if (index != -1) {
        WordBounds newWord = new WordBounds(index, word.length());
        if (firstWord == null || newWord.compareTo(firstWord) < 0) {
          firstWord = newWord;
        }
      }
    }
    return firstWord;
  }


  /**
   * A class reresenting the bounds of a word within a string.
   * <p>
   * The bounds are represented by a {@code startIndex} (inclusive) and
   * an {@code endIndex} (exclusive).
   */
  private static class WordBounds implements Comparable<WordBounds> {

    final int startIndex;
    final int endIndex;

    public WordBounds(int startIndex, int length) {
      this.startIndex = startIndex;
      this.endIndex = startIndex + length;
    }

    public int compareTo(WordBounds that) {
      int comparison = this.startIndex - that.startIndex;
      if (comparison == 0) {
        comparison = that.endIndex - this.endIndex;
      }
      return comparison;
    }
  }

  private static final char WHITESPACE_CHAR = ' ';
  private static final String WHITESPACE_STRING = " ";

  @NotNull
  private DivWidget getRemoveButtonContainer() {
    DivWidget delW = new DivWidget();
    delW.addStyleName("floatLeftList");
    delW.addStyleName("leftFiveMargin");
    Button deleteButton = makeDeleteButton();
    delW.add(deleteButton);
    return delW;
  }

  @NotNull
  private DivWidget getAddButtonContainer() {
    DivWidget addW = new DivWidget();
    addW.addStyleName("floatLeftList");

    Typeahead exercise = getTypeahead("exercise");

    addW.add(exercise);
    Button add = getAddButton();

    addW.add(add);
    return addW;
  }

  //private CommonExercise currentExercise = null;

  @NotNull
  private Button getAddButton() {
    Button add = new Button("Add", IconType.PLUS);

    final ListInterface<CommonShell> outer = this;
    add.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onClickAdd(outer);
      }
    });
    add.setType(ButtonType.SUCCESS);
    return add;
  }

  NewUserExercise newExercise;
  protected final ListServiceAsync listService = GWT.create(ListService.class);


  private void onClickAdd(ListInterface<CommonShell> outer) {
    //CommonExercise newItem = currentExercise == null ? editItem.getNewItem() : currentExercise;

    if (currentExercise != null) {
      boolean found = false;
      List<CommonShell> exercises = list.getExercises();
      for (CommonShell shell:exercises) {
        if (shell.getID() == currentExercise.getID()) {
          found = true;
          break;
        }
      }
      if (!found) {
        listService.addItemToUserList(list.getID(), currentExercise.getID(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
            showNewItem(currentExercise);
          }
        });
      }
      else {
        // warn user already added.
      }
    } else {
      String safeText = getSafeText(quickAddText);
      controller.getScoringService().isValidForeignPhrase(safeText, "", new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Boolean result) {
/*        logger.info("\tisValidForeignPhrase : checking phrase " + foreignLang.getSafeText() +
            " before adding/changing " + newUserExercise + " -> " + result);*/
          if (result) {
            CommonExercise newItem = editItem.getNewItem();
            newItem.getMutable().setForeignLanguage(safeText);
            newItem.getMutable().setEnglish("");
            newItem.getMutable().setMeaning("");

            controller.getAudioService().reallyCreateNewItem(list.getID(), newItem, controller.getLanguage(), new AsyncCallback<CommonExercise>() {
              @Override
              public void onFailure(Throwable caught) {
              }

              @Override
              public void onSuccess(CommonExercise newExercise) {
                showNewItem(newExercise);
              }
            });
          } else {
//            markError(foreignLang, "The " + FOREIGN_LANGUAGE +
//                " text is not in our " + getLanguage() + " dictionary. Please edit.");
          }
        }
      });

//      //if (newExercise.isCompleted()) {
//      CommonExercise newItem = editItem.getNewItem();
//      newExercise = new NewUserExercise(controller, newItem, "newExercise", list);
//
//      DivWidget container = new DivWidget();
//      Panel widgets1 = newExercise.addNew(outer, container);
//      container.add(widgets1);
//
//      showModal(newExercise, container);
    }
    // }
  }

  private void showNewItem(CommonShell currentExercise) {
    list.addExercise(currentExercise);
    addExercise(currentExercise);

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        //logger.info("check init flash");
        markCurrentExercise(currentExercise.getID());
        gotClickOnItem(currentExercise);
      }
    });
  }

  public String getSafeText(TextBox box) {
    return sanitize(box.getText()).replaceAll("&#39;", "'");
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

//  private void showModal(NewUserExercise newExercise, DivWidget container) {
//    final Modal modal = new Modal(true);
//    modal.setWidth(750);
//    modal.setHeight(750 + "px");
//    modal.setMaxHeigth(750 + "px");
//    modal.setTitle("Create new item");
//    modal.add(container);
//    newExercise.setModal(modal);
//    modal.show();
//
//    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
//      public void execute() {
//        newExercise.setFocus();
//      }
//    });
//  }

  private Button delete;

  private Button makeDeleteButton() {

    EditableExerciseList widgets = this;

    delete = makeDeleteButtonItself();

    delete.addClickHandler(event -> {
      CommonShell currentSelection = pagingContainer.getCurrentSelection();
      if (currentSelection != null) {
//          logger.info(getClass() + " : makeDeleteButton npfHelperList (2) " + npfHelper);
        NewUserExercise newExercise = new NewUserExercise(controller, null, "newExercise", list);
        newExercise.deleteItem(currentSelection.getID(), widgets, null, widgets);
      }
    });
    // delete.addStyleName("topFiftyMargin");
    return delete;
  }

  /**
   * TODO : why is this here?
   *
   * @return
   * @see #makeDeleteButton
   */
  private Button makeDeleteButtonItself() {
    Button delete = new Button(REMOVE_FROM_LIST);
    delete.getElement().setId("Remove_from_list");
    // delete.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

    delete.setType(ButtonType.WARNING);
    //   delete.addStyleName("floatRight");
    // if (ul == null) logger.warning("no user list");
    // else
    if (controller == null) logger.warning("no controller??");
    else {
      controller.register(delete, "", "");//"Remove from list " + ul.getID() + "/" + ul.getName());
    }
    return delete;
  }

  @Override
  protected void onLastItem() {
    new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        reloadExercises();
      }
    });
  }

  public void enableRemove(boolean enabled) {
    delete.setEnabled(enabled);
  }

/*
  @Override
  protected void askServerForExercise(int itemID) {
    if (itemID == EditItem.NEW_EXERCISE_ID) {
      useExercise(editItem.getNewItem());
    } else {
      //     logger.info("EditItem.makeExerciseList - askServerForExercise = " + itemID);
      super.askServerForExercise(itemID);
    }
  }
*/

/*  @Override
  public List<CommonShell> rememberExercises(List<CommonShell> result) {
    clear();
    boolean addNewItem = includeAddItem;

    for (final CommonShell es : result) {
      logger.info("Adding " + es.getID() + " : " + es.getClass());
      addExercise(es);
      if (includeAddItem && es.getID() == EditItem.NEW_EXERCISE_ID) {
        addNewItem = false;
      }
    }

    if (addNewItem) {
      CommonExercise newItem = editItem.getNewItem();

      logger.info("Adding " + newItem.getID() + " : " + newItem.getClass());

      addExercise(newItem);  // TODO : fix this
    }
    flush();
    return result;
  }*/
}
