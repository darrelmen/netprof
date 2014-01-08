package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem {
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  private PagingContainer<UserExercise> pagingContainer;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @param service
   * @param userManager
   * @param controller
   */
  public EditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller) {
    this.controller = controller;
    this.service = service;
    this.userManager = userManager;
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#showEditItem(mitll.langtest.shared.custom.UserList, mitll.langtest.client.custom.Navigation.TabAndContent)
   * @param ul
   * @param itemMarker
   * @return
   */
  public Panel editItem(final UserList ul, final HTML itemMarker) {
    Panel hp = new HorizontalPanel();
    SimplePanel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final SimplePanel contentOnRight = new SimplePanel();
    hp.add(contentOnRight);

    pagingContainer = new PagingContainer<UserExercise>(controller, 100) {
      @Override
      protected void gotClickOnItem(UserExercise exerciseShell) {
        showItem(exerciseShell, contentOnRight, ul, itemMarker);
      }
    };

    pagerOnLeft.add(getPager(ul));
    showInitialItem(ul, itemMarker, contentOnRight);
    return hp;
  }

  private Panel getPager(UserList ul) {
    Panel container = pagingContainer.getTableWithPager();
    for (UserExercise es : ul.getExercises()) {
       pagingContainer.addExerciseToList2(es);
    }
    pagingContainer.flush();
    return container;
  }

  private void showInitialItem(UserList ul, HTML itemMarker, SimplePanel contentOnRight) {
    UserExercise exerciseShell = pagingContainer.selectFirst();
    if (exerciseShell == null) {
      System.err.println("huh? nothing first?");
      showPopup("No editable items yet.",contentOnRight);
    }
    else {
//      editItem(exerciseShell, contentOnRight, ul, itemMarker);
      showItem(exerciseShell, contentOnRight, ul, itemMarker);
    }
  }

  private void showItem(final UserExercise exerciseShell, final SimplePanel contentOnRight, final UserList ul, final HTML itemMarker) {
    service.getExercise(exerciseShell.getID(), new AsyncCallback<Exercise>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Exercise result) {
        editItem(new UserExercise(result), contentOnRight, ul, itemMarker, exerciseShell);
      }
    });
  }

  private void editItem(final UserExercise exercise, SimplePanel right, UserList ul, HTML itemMarker,UserExercise exerciseShell) {
    right.clear();

    EditableExercise newUserExercise = new EditableExercise(itemMarker, exercise,exerciseShell);
    right.add(newUserExercise.addNew(ul, pagingContainer, right));
    newUserExercise.setFields(exercise);
  }

  private void showPopup(String toShow, Widget over) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.showRelativeTo(over);
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }

  private class EditableExercise extends NewUserExercise {
    private final UserExercise changedUserExercise;
    UserExercise exerciseShell;
    HTML englishAnno = new HTML();
    HTML translitAnno = new HTML();
    HTML foreignAnno = new HTML();
    HTML fastAnno = new HTML();  // TODO add these
    HTML slowAnno = new HTML();

    /**
     * @see EditItem#editItem(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.SimplePanel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.shared.custom.UserExercise)
     * @param itemMarker
     * @param changedUserExercise
     */
    public EditableExercise(HTML itemMarker, UserExercise changedUserExercise,UserExercise exerciseShell) {
      super(EditItem.this.service, EditItem.this.userManager, EditItem.this.controller, itemMarker);
      this.changedUserExercise = changedUserExercise;
      this.exerciseShell = exerciseShell;
    }

    /**
     *
     * @param container
     * @return
     */
    @Override
    protected Panel makeEnglishRow(Panel container) {
      FluidRow row = new FluidRow();
      container.add(row);
      english = makeBoxAndAnno(row, "English",englishAnno);
      focusOnEnglish(english);
      return row;
    }

    /**
     * TODO : on blur - check if we need to re-record the audio!
     * @param container
     */
    @Override
    protected void makeForeignLangRow(Panel container) {
      FluidRow row = new FluidRow();
      container.add(row);
      foreignLang = makeBoxAndAnno(row, controller.getLanguage(), foreignAnno);
      foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL 
    }

    @Override
    protected void makeTranslitRow(Panel container) {
      FluidRow row = new FluidRow();
      container.add(row);
      translit = makeBoxAndAnno(row, "Transliteration (optional)", translitAnno);
    } 
    

    private BasicDialog.FormField makeBoxAndAnno(FluidRow row,  String label, HTML englishAnno) {
      BasicDialog.FormField formField = addControlFormField(row, label, false, 1, englishAnno);
      englishAnno.addStyleName("leftFiveMargin");
      englishAnno.addStyleName("editComment");
      return formField;
    }

    /**
     * TODO : update tool tip as well
     * @param ul
     * @param pagingContainer
     * @param toAddTo
     */
    @Override
    protected void onClick(final UserList ul, final PagingContainer<?> pagingContainer, Panel toAddTo) {
      changedUserExercise.setCreator(controller.getUser());
      service.editItem(changedUserExercise, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void newExercise) {
          exerciseShell.setEnglish(changedUserExercise.getEnglish());
          pagingContainer.refresh();
          showPopup(changedUserExercise.getTooltip() + " has been updated.", submit);
        }
      });
    }

    /**
     * @see mitll.langtest.client.custom.EditItem#editItem(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.SimplePanel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.shared.custom.UserExercise)
     * @param userExercise
     */
    public void setFields(UserExercise userExercise) {
      newUserExercise = userExercise;
      System.out.println("NewUserExercise : setting fields with " + userExercise);
      TextBoxBase box = english.box;
      box.setText(userExercise.getEnglish());

      // add annotation if it's there
      useAnnotation(userExercise,"english",englishAnno);

      translit.box.setText(userExercise.getTransliteration());
      useAnnotation(userExercise,"transliteration",translitAnno);

      foreignLang.box.setText(userExercise.getForeignLanguage());
      useAnnotation(userExercise, "foreignLanguage", foreignAnno);

      Exercise exercise = newUserExercise.toExercise();
      rap.getPostAudioButton().setExercise(exercise);

      if (exercise.getRefAudio() != null) {
        rap.getImagesForPath(exercise.getRefAudio());
      } else {
        System.err.println("no regular audio ref on " + userExercise);
      }

      rapSlow.getPostAudioButton().setExercise(exercise);

      if (exercise.getSlowAudioRef() != null) {
        rapSlow.getImagesForPath(exercise.getSlowAudioRef());
      }
      else {
        System.err.println("no slow audio ref on " + userExercise);
      }
      submit.setText("Change");
    }

    private void useAnnotation(UserExercise userExercise,String field, HTML annoField) {
      ExerciseAnnotation englishAnnotation = userExercise.getAnnotation(field);
      boolean isIncorrect = englishAnnotation != null && !englishAnnotation.isCorrect();
      if (isIncorrect) {
        annoField.setText(englishAnnotation.comment);
      }
      annoField.setVisible(isIncorrect);
    }
  }
/*
  protected Label getCommentLabel() {
    final Label commentLabel = new Label("comment : ");
   // DOM.setStyleAttribute(commentLabel.getElement(), "backgroundColor", "#ff0000");
    commentLabel.setVisible(true);
   // commentLabel.addStyleName("ImageOverlay");
    return commentLabel;
  }*/

}
