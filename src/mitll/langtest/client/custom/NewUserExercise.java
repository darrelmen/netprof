package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewUserExercise extends BasicDialog {
  public static final String FOREIGN_LANGUAGE = "Foreign Language";
  protected UserExercise newUserExercise = null;
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  private HTML itemMarker;
  protected BasicDialog.FormField english;
  protected BasicDialog.FormField foreignLang;
  protected BasicDialog.FormField translit;
  protected CreateFirstRecordAudioPanel rap;
  protected CreateFirstRecordAudioPanel rapSlow;
  protected Button submit;

  /**
   * @see Navigation#addItem(mitll.langtest.shared.custom.UserList)
   * @param service
   * @param userManager
   * @param controller
   * @param itemMarker
   */
  public NewUserExercise(final LangTestDatabaseAsync service, final UserManager userManager,
                         ExerciseController controller,HTML itemMarker) {
    this.controller = controller;
    this.service = service;
    this.itemMarker = itemMarker;
    this.userManager = userManager;
  }

  public <T extends ExerciseShell> Panel addNew(final UserList ul, final PagingContainer<T> pagingContainer, final Panel toAddTo) {
    final FluidContainer container = new FluidContainer();
    container.addStyleName("greenBackground");

    makeEnglishRow(container);
    makeForeignLangRow(container);
    makeTranslitRow(container);

    // make audio row
    FluidRow row = new FluidRow();
    container.add(row);

    final ControlGroup normalSpeedRecording = makeRegularAudioPanel(row);

    makeSlowAudioPanel(row);
    rap.setOtherRAP(rapSlow.getPostAudioButton());
    rapSlow.setOtherRAP(rap.getPostAudioButton());

    Column column = getCreateButton(ul, pagingContainer, toAddTo, normalSpeedRecording);
    row.add(column);

    return container;
  }

  protected void makeSlowAudioPanel(FluidRow row) {
    rapSlow = makeRecordAudioPanel(row, english, foreignLang, false);
    addControlGroupEntry(row, "Slow speed reference recording (optional)", rapSlow);
  }

  protected ControlGroup makeRegularAudioPanel(FluidRow row) {
    rap = makeRecordAudioPanel(row, english, foreignLang, true);
    return addControlGroupEntry(row, "Normal speed reference recording", rap);
  }

  protected Panel makeEnglishRow(Panel container) {
    FluidRow row = new FluidRow();
    container.add(row);
    english = addControlFormField(row, "English", false,1);

    focusOnEnglish(english);
    return row;
  }

  protected void makeForeignLangRow(Panel container) {
    FluidRow row = new FluidRow();
    container.add(row);
    foreignLang = addControlFormField(row, controller.getLanguage(),false,1);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
  }

  protected void makeTranslitRow(Panel container) {
    FluidRow row = new FluidRow();
    container.add(row);
    translit = addControlFormField(row, "Transliteration (optional)",false,0);
 //   translit.box.setDirectionEstimator(false);   // automatically detect whether text is RTL
  }

  protected void focusOnEnglish(final FormField english) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        english.box.setFocus(true);
      }
    });
  }

  private Column getCreateButton(UserList ul, PagingContainer<?> pagingContainer, Panel toAddTo, ControlGroup normalSpeedRecording) {
    Button submit = makeCreateButton(ul, pagingContainer, toAddTo, english, foreignLang, rap, normalSpeedRecording);
    DOM.setStyleAttribute(submit.getElement(), "marginBottom", "5px");
    DOM.setStyleAttribute(submit.getElement(), "marginRight", "15px");

    Column column = new Column(2, 9, submit);
    column.addStyleName("topMargin");
    return column;
  }

  private Button makeCreateButton(final UserList ul, final PagingContainer<?> pagingContainer, final Panel toAddTo,
                                  final FormField english, final FormField foreignLang,
                                  final RecordAudioPanel rap, final ControlGroup normalSpeedRecording) {
    submit = new Button("Create");
    submit.setType(ButtonType.SUCCESS);
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("makeCreateButton : creating new item for " + english + " " + foreignLang);

        if (validateForm(english, foreignLang, rap, normalSpeedRecording)) {
          createButtonClicked(english, foreignLang, ul, pagingContainer, toAddTo);
        }
      }
    });
    submit.addStyleName("rightFiveMargin");
    return submit;
  }

  private void createButtonClicked(FormField english, FormField foreignLang, UserList ul,
                                   PagingContainer<?> pagingContainer, Panel toAddTo) {
    newUserExercise.setEnglish(english.getText());
    newUserExercise.setForeignLanguage(foreignLang.getText());
    newUserExercise.setTransliteration(translit.getText());

    System.out.println("createButtonClicked : now " + newUserExercise);

    onClick(ul, pagingContainer, toAddTo);
  }

  protected void onClick(final UserList ul, final PagingContainer<?> pagingContainer, final Panel toAddTo) {
    System.out.println("onClick : adding " + newUserExercise + " to " +ul);

    service.reallyCreateNewItem(ul.getUniqueID(), newUserExercise, new AsyncCallback<UserExercise>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(UserExercise newExercise) {
        ul.addExercise(newExercise);
        itemMarker.setText(ul.getExercises().size() + " items");
        pagingContainer.addAndFlush(newExercise);
        toAddTo.clear();
        toAddTo.add(addNew(ul, pagingContainer, toAddTo));
        newUserExercise = null;
      }
    });
  }

  protected CreateFirstRecordAudioPanel makeRecordAudioPanel(final FluidRow row, final FormField english,
                                                           final FormField foreignLang,
                                                           boolean recordRegularSpeed) {
    return new CreateFirstRecordAudioPanel(row, english, foreignLang, recordRegularSpeed);
  }

  protected class CreateFirstRecordAudioPanel extends RecordAudioPanel {
    private final FormField english;
    private final FormField foreignLang;
    boolean recordRegularSpeed = true;
    private PostAudioRecordButton otherRAP;
    private WaveformPostAudioRecordButton postAudioButton;

    public CreateFirstRecordAudioPanel(Panel row, FormField english, FormField foreignLang, boolean recordRegularSpeed) {
      super(null, NewUserExercise.this.controller, row, NewUserExercise.this.service, 0, false);
      this.english = english;
      this.foreignLang = foreignLang;
      this.recordRegularSpeed = recordRegularSpeed;
    }

    @Override
    protected void getEachImage(int width) {
      float newWidth = Window.getClientWidth() * 0.65f;
      super.getEachImage((int) newWidth);    //To change body of overridden methods use File | Settings | File Templates.
    }

    /**
     * Note that we want to post the audio the server, but not record in the results table (since it's not an answer
     * to an exercise...)
     * That's the final "false" on the end of the WaveformPostAudioRecordButton
     * @see mitll.langtest.client.exercise.RecordAudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget)
     * @return
     */
    @Override
    protected WaveformPostAudioRecordButton makePostAudioRecordButton() {
      final ExerciseController outer = controller;
      postAudioButton =
        new WaveformPostAudioRecordButton(exercise, controller, exercisePanel, this, service, 0, false // don't record in results table
        ) {
          @Override
          public void stopRecording() {
            showStop();
            System.out.println("WaveformPostAudioRecordButton.stopRecording with newUserExercise " + newUserExercise + " and exercise " + exercise);
            if (newUserExercise == null) {
              // first we need to create an item to attach audio to it
              NewUserExercise.this.service.createNewItem(userManager.getUser(), english.getText(), foreignLang.getText(),
                "", new AsyncCallback<UserExercise>() {
                @Override
                public void onFailure(Throwable caught) {
                  System.out.println("onFailure : stopRecording  " + caught);
                }

                  @Override
                  public void onSuccess(UserExercise newExercise) {
                    if (newExercise == null) {
                      showPopup("The " + FOREIGN_LANGUAGE +
                        " text is not in our " + outer.getLanguage() + " dictionary. Please edit.", foreignLang.box);
                    } else {
                      newUserExercise = newExercise;
                      System.out.println("\tonSuccess : stopRecording with newUserExercise " + newUserExercise);

                      exercise = newExercise.toExercise();
                      otherRAP.setExercise(exercise);
                      setExercise(exercise);
                      stopRecording();
                    }
                  }
                });
            } else {
              System.out.println("\t\tonSuccess : stopRecording with newUserExercise " + newUserExercise + " and exercise " + exercise);

              super.stopRecording();
            }
          }

          @Override
          public void startRecording() {
            super.startRecording();
            showStart();
          }

          @Override
          public void flip(boolean first) {
            super.flip(first);
            flipRecordImages(first);
          }

          @Override
          public void useResult(AudioAnswer result) {
            super.useResult(result);
            System.out.println("useResult : path to audio is " + result.path);
            //  fastPath = result.path;
            if (recordRegularSpeed) {
              newUserExercise.setRefAudio(result.path);
            } else {
              newUserExercise.setSlowRefAudio(result.path);
            }
            System.out.println("newUserExercise " + newUserExercise);
          }
      };
      postAudioButton.getElement().setId("NewUserExercise_WaveformPostAudioRecordButton");
      return postAudioButton;
    }

    public void setOtherRAP(PostAudioRecordButton otherRAP) {
      this.otherRAP = otherRAP;
    }

    public WaveformPostAudioRecordButton getPostAudioButton() {
      return postAudioButton;
    }
  }

  private void showPopup(String html, Widget target) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.showRelativeTo(target);
    // pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(5000);
  }

  private boolean validateForm(final FormField english, final FormField foreignLang, final RecordAudioPanel rap,
                               final ControlGroup normalSpeedRecording) {
    if (english.getText().isEmpty()) {
      markError(english, "Please enter an english word or phrase.");
      return false;
    } else if (foreignLang.getText().isEmpty()) {
      markError(foreignLang, "Please enter the foreign language phrase.");
      return false;
    } else if (newUserExercise == null || newUserExercise.getRefAudio() == null) {
      System.out.println("validateForm : new user ex " + newUserExercise);

      markError(normalSpeedRecording, rap.getButton(), rap.getButton(), "",
        "Please record reference audio for the foreign language phrase.");
      rap.getButton().addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          normalSpeedRecording.setType(ControlGroupType.NONE);
        }
      });
      return false;
    }
    return true;
  }
}
