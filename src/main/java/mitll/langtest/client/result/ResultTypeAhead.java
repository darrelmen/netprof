package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Typeahead;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.view.client.RangeChangeEvent;
import mitll.langtest.client.list.TypeAhead;
import mitll.langtest.client.services.ResultServiceAsync;
import mitll.langtest.shared.result.MonitorResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 2/16/17.
 */
public class ResultTypeAhead {
  private static final String USER_ID = "User";

  private final Map<String, Typeahead> typeToSuggest = new HashMap<String, Typeahead>();
  private Typeahead textSuggest;
  private final Collection<String> typeOrder;
  private final CellTable<MonitorResult> cellTable;
  private final ResultServiceAsync resultServiceAsync;

  ResultTypeAhead(Collection<String> typeOrder,
                  CellTable<MonitorResult> cellTable, ResultServiceAsync resultServiceAsync) {
    this.typeOrder = typeOrder;
    this.cellTable = cellTable;
    this.resultServiceAsync = resultServiceAsync;
  }

  Panel getSearchBoxes() {
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("search_container");

    for (final String type : typeOrder) {
      Typeahead typeahead = getTypeahead(type);
      typeToSuggest.put(type, typeahead);
      hp.add(TypeAhead.getControlGroup(type, typeahead.asWidget()));
    }

    hp.add(getUserIDSuggestWidget());
    hp.add(getTextSuggestWidget());

    return hp;
  }

  private ControlGroup getUserIDSuggestWidget() {
    Typeahead userIDSuggest = getTypeahead(MonitorResult.USERID);
    return TypeAhead.getControlGroup(USER_ID, userIDSuggest.asWidget());
  }

  private ControlGroup getTextSuggestWidget() {
    TextBox w1 = new TextBox();
    w1.setPlaceholder("Word (type or paste) or Item ID");
    w1.setDirectionEstimator(true);
    textSuggest = getTypeaheadUsing(MonitorResult.TEXT, w1);

    return TypeAhead.getControlGroup("Text", textSuggest.asWidget());
  }

  /**
   * @param whichField
   * @return
   * @see #getSearchBoxes
   * @see #getUserIDSuggestWidget
   */
  private Typeahead getTypeahead(final String whichField) {
    return getTypeaheadUsing(whichField, new TextBox());
  }

  private Typeahead getTypeaheadUsing(final String whichField, TextBox w) {
    Typeahead typeahead = new Typeahead(new SuggestOracle() {
      @Override
      public void requestSuggestions(final Request request, final Callback callback) {
        //logger.info(" requestSuggestions got request for " + type + " : " + unitToValue);
        resultServiceAsync.getResultAlternatives(getUnitToValue(), getText(), whichField, new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Collection<String> result) {
            // logger.info(" request for " + type + " : " + unitToValue + " yielded " + result.size());
            makeSuggestionResponse(result, callback, request);
          }
        });
      }
    });
    w.getElement().setId("TextBox_" + whichField);
    typeahead.setWidget(w);
    configureTextBox(w);
    addCallbacks(typeahead);
    return typeahead;
  }

  public Map<String, String> getUnitToValue() {
    Map<String, String> unitToValue = new HashMap<String, String>();
    for (String type : typeOrder) {
      Typeahead suggestBox = typeToSuggest.get(type);
      if (suggestBox != null) {
        String text = getTextFromTypeahead(suggestBox);
        if (!text.isEmpty()) {
          unitToValue.put(type, text);
        }
      }
    }
    return unitToValue;
  }

  /**
   * @param result
   * @param callback
   * @param request
   * @see #getTypeaheadUsing
   */
  private void makeSuggestionResponse(Collection<String> result, SuggestOracle.Callback callback, SuggestOracle.Request request) {
    callback.onSuggestionsReady(request, new SuggestOracle.Response(getSuggestions(result)));
  }

  @NotNull
  private Collection<SuggestOracle.Suggestion> getSuggestions(Collection<String> result) {
    Collection<SuggestOracle.Suggestion> suggestions = new ArrayList<>();
    for (String resp : result) suggestions.add(new MySuggestion(resp));
    return suggestions;
  }

  private void configureTextBox(final TextBox w) {  w.addKeyUpHandler(getKeyUpHandler()); }

  /**
   * NOTE : we need both a redraw on key up and one on selection!
   *
   * @return
   * @paramx w
   */
  private KeyUpHandler getKeyUpHandler() { return event -> { redraw(); };  }

  private void redraw() { RangeChangeEvent.fire(cellTable, cellTable.getVisibleRange());  }

  private void addCallbacks(final Typeahead user) { user.setUpdaterCallback(getUpdaterCallback());  }

  private Typeahead.UpdaterCallback getUpdaterCallback() {
    return selectedSuggestion -> {
      String replacementString = selectedSuggestion.getReplacementString();
      //  logger.info("UpdaterCallback " + " got update " +" " + " ---> '" + replacementString +"'");

      // NOTE : we need both a redraw on key up and one on selection!
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          //       logger.info("--> getUpdaterCallback onSelection REDRAW ");
          redraw();
        }
      });

      return replacementString;
    };
  }

  public String getText() {  return textSuggest == null ? "" : getTextFromTypeahead(textSuggest);  }

  private String getTextFromTypeahead(Typeahead textSuggest) {
    if (textSuggest == null) {
      return "";
    } else {
      TextBox widget = (TextBox) textSuggest.getWidget();
      return widget.getValue();
    }
  }

  /**
   * @see #makeSuggestionResponse
   */
  private static class MySuggestion implements SuggestOracle.Suggestion {
    private final String resp;

    MySuggestion(String resp) {
      this.resp = resp;
    }

    @Override
    public String getDisplayString() {
      return resp;
    }

    @Override
    public String getReplacementString() {
      return resp;
    }
  }
}
