package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;

import java.util.logging.Logger;

public class UserTypeahead {
  private final Logger logger = Logger.getLogger("UserTypeahead");

  //private static final int DISPLAY_ITEMS = 15;
  private static final int BOX_WIDTH = 150;
  private static final String HINT = "user id or name or date";
  private static final int SEARCH_FONT = 14;
  private final TextBox box;
  //private Collection<UserInfo> choices;
  TypeaheadListener listener;

  UserTypeahead(TypeaheadListener listener) {
    TextBox box = getTextBox();
    box.setPlaceholder(HINT);
    box.addKeyUpHandler(event -> listener.gotKey(box.getText()));
/*
    Typeahead typeahead = new Typeahead(new MultiWordSuggestOracle() {
      @Override
      public void requestSuggestions(Request request, Callback callback) {
        List<UserInfo> matches = getMatches(box);

        callback.onSuggestionsReady(request, getResponse(request, matches, matches.size(), 10));
      }
    });

    addCallbacks(typeahead);
*/
//    getTypeahead(box, typeahead);
    this.box = box;
  }

/*  public void setChoices(Collection<UserInfo> choices) {
    this.choices = choices;
  }*/

  Widget getSearch() {
    Panel flow = new DivWidget();
    flow.addStyleName("floatLeft");
    flow.addStyleName("inlineFlex");
    Icon child = new Icon(IconType.SEARCH);
    child.setIconSize(IconSize.LARGE);
    //  child.getElement().getStyle().setMarginTop(14, Style.Unit.PX);

    Style style = child.getElement().getStyle();
    style.setMarginRight(10, Style.Unit.PX);
    style.setColor("gray");
    flow.add(child);
    flow.add(box);
    //flow.add(waitCursor);
    return flow;
  }

 /* @NotNull
  private void getTypeahead(TextBox textBox, Typeahead typeahead) {
    typeahead.setDisplayItemCount(DISPLAY_ITEMS);
    typeahead.setMatcherCallback((query, item) -> true);
    typeahead.setUpdaterCallback(selectedSuggestion -> {
//      currentExercise = ((SearchTypeahead.ExerciseSuggestion) selectedSuggestion).getShell();
//      add.setEnabled(currentExercise != null);

      return selectedSuggestion.getReplacementString();
    });

    textBox.getElement().setId("TextBox_user");
    typeahead.setWidget(textBox);
    //return typeahead;
  }*/
/*
  @NotNull
  private List<UserInfo> getMatches(TextBox box) {
    String text = box.getText();

    logger.info("getMatches for " + text);
    List<UserInfo> matches = new ArrayList<>();

    for (UserInfo user : choices) {
      if (user.getUserID().toLowerCase().startsWith(text.toLowerCase())) {
        matches.add(user);
      }
    }
    return matches;
  }*/

  /*@NotNull
  private SuggestOracle.Response getResponse(SuggestOracle.Request request,
                                             List<UserInfo> users,
                                             int size,
                                             int limit) {
    int numberTruncated = Math.max(0, size - limit);
    //  logger.info("trunc " + numberTruncated);
    SuggestOracle.Response response = new SuggestOracle.Response(getSuggestions(request.getQuery(), users));
    response.setMoreSuggestionsCount(numberTruncated);
    return response;
  }

  @NotNull
  private Collection<SuggestOracle.Suggestion> getSuggestions(String query, List<UserInfo> users) {
    Collection<SuggestOracle.Suggestion> suggestions = new ArrayList<>();
    users.forEach(resp -> suggestions.add(new UserSuggestion(query, resp)));
    return suggestions;
  }*/
/*  public TextBox getBox() {
    return box;
  }*/

/*  private static class UserSuggestion extends MultiWordSuggestOracle.MultiWordSuggestion {
    private UserInfo userInfo;
    String repl;

    @Override
    public String getDisplayString() {
      return userInfo.getFirst() + " " + userInfo.getLast();
    }

    @Override
    public String getReplacementString() {
      return repl;
    }

    */

  /**
   * @paramx repl
   * @paramx userInfo
   * @see
   *//*
    UserSuggestion(String repl, UserInfo userInfo) {
      super(repl, userInfo.getFirst() + " " + userInfo.getLast());
      this.repl = repl;
      this.userInfo = userInfo;
    }

    public UserSuggestion() {
    }

    public UserInfo getUserInfo() {
      return userInfo;
    }
  }*/
  private TextBox getTextBox() {
    TextBox quickAddText = new TextBox();
    quickAddText.setMaxLength(100);
    quickAddText.setVisibleLength(40);
//    quickAddText.getElement().getStyle().setMarginBottom(););
    // quickAddText.addStyleName("topMargin");
    quickAddText.setWidth(BOX_WIDTH + "px");
    quickAddText.getElement().getStyle().setProperty("fontFamily", "sans-serif");
    quickAddText.getElement().getStyle().setFontSize(SEARCH_FONT, Style.Unit.PX);
    //quickAddText.addStyleName("bigflfont");

    return quickAddText;
  }


  /*private void addCallbacks(final Typeahead user) {
    user.setUpdaterCallback(getUpdaterCallback());
  }

  private Typeahead.UpdaterCallback getUpdaterCallback() {
    return selectedSuggestion -> {
      String replacementString = selectedSuggestion.getReplacementString();
        logger.info("UpdaterCallback " + " got update " +" " + " ---> '" + replacementString +"'");

      // NOTE : we need both a redraw on key up and one on selection!
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          //       logger.info("--> getUpdaterCallback onSelection REDRAW ");
          //      redraw();
        }
      });

      return replacementString;
    };
  }*/

}
