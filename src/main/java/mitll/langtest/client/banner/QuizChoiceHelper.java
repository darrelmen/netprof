package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.QuizIntro;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.custom.IUserList;
import mitll.langtest.shared.custom.UserList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * So what if we reload the page with an ongoing quiz?
 * What if we jump to the quiz from the ListView?
 */
public class QuizChoiceHelper implements ContentView {
  private final Logger logger = Logger.getLogger("QuizChoiceHelper");
  private static final String GETTING_LISTS_FOR_USER = "getting simple lists for user";

  private static final boolean DEBUG = true;
  ExerciseController controller;
//  private final INavigation navigation;

  NewQuizHelper newQuizHelper;

  QuizChoiceHelper(ExerciseController controller, INavigation navigation) {
    this.controller = controller;
    //  this.navigation = navigation;
    this.newQuizHelper = new NewQuizHelper(controller, this);
  }

  private Panel listContent;

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS ignored) {
    this.listContent = listContent;
    showQuizIntro();
//    getQuizIntro(controller, listContent);
  }

  /**
   * From start over...
   */
  void showQuizIntro() {
    listContent.clear();
    newQuizHelper.clearListSelection();
    getQuizIntro(controller, listContent);
  }

  @NotNull
  private void getQuizIntro(ExerciseController controller, Panel toAddTo) {
    if (DEBUG) logger.info("getQuizIntro ");

    controller.getListService().getSimpleListsForUser(true, true, UserList.LIST_TYPE.QUIZ,
        new AsyncCallback<Collection<IUserList>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError(GETTING_LISTS_FOR_USER, caught);
          }

          @Override
          public void onSuccess(Collection<IUserList> result) {
            //long l = System.currentTimeMillis();
            final Map<Integer, IUserList> idToList = new LinkedHashMap<>();
            result.forEach(ul -> idToList.put(ul.getID(), ul));

            QuizIntro widgets = new QuizIntro(
                idToList,
                QuizChoiceHelper.this::gotQuizChoice,
                controller.getUserManager().getUserID());

            toAddTo.add(widgets);
          }
        });

//    return new QuizIntro(
//        getIdToList(),
//        QuizHelper.this::gotQuizChoice,
//        controller.getUserManager().getUserID());
  }

  public void showChosenQuiz(DivWidget listContent) {
    this.listContent = listContent;
    listContent.clear();
    gotQuizChoice(new SelectionState().getList());
  }


  void gotQuizChoice(int listid) {
    if (DEBUG) logger.info("getQuizIntro : got choice " + listid);
    listContent.clear();
    newQuizHelper.showContent(listContent, INavigation.VIEWS.QUIZ);
    newQuizHelper.gotQuizChoice(listid);

//    polyglotFlashcardFactory.cancelRoundTimer();
//    chosenList = listid;
//    polyglotFlashcardFactory.removeItemFromHistory(chosenList);
//    showQuizForReal();
//    polyglotFlashcardFactory.startQuiz();
  }

}
