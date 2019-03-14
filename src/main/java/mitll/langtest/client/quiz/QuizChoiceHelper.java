/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.quiz;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.NewContentChooser;
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

  private static final boolean DEBUG = false;
  private ExerciseController controller;

  private NewQuizHelper newQuizHelper;
  private Panel listContent;

  public QuizChoiceHelper(ExerciseController controller, INavigation navigation) {
    this.controller = controller;
    this.newQuizHelper = new NewQuizHelper(controller, this);
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS ignored) {
    this.listContent = listContent;
    showQuizIntro();
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
            QuizIntro widgets = new QuizIntro(
                getIdToUserListMap(result),
                QuizChoiceHelper.this::gotQuizChoice,
                controller.getUserManager().getUserID());
            toAddTo.add(widgets);
          }
        });
  }

  /**
   * Only show public lists or ones that are mine!
   *
   * @param result
   * @return
   */
  @NotNull
  private Map<Integer, IUserList> getIdToUserListMap(Collection<IUserList> result) {
    final Map<Integer, IUserList> idToList = new LinkedHashMap<>();
    int me = controller.getUser();
    result.forEach(ul -> {
      if (!ul.isPrivate() || ul.getUserID() == me) {
        idToList.put(ul.getID(), ul);
      }
    });
    return idToList;
  }

  /**
   * @param listContent
   * @see NewContentChooser#showQuizForReal
   */
  public void showChosenQuiz(DivWidget listContent) {
    int list = new SelectionState().getList();

    if (DEBUG) {
      logger.info("showChosenQuiz - #" + list);
    }

    this.listContent = listContent;
    listContent.clear();
    gotQuizChoice(list, false);
  }

  private void gotQuizChoice(int listid) {
    gotQuizChoice(listid, true);
  }

  private void gotQuizChoice(int listid, boolean removeCurrent) {
    if (DEBUG) {
      logger.info("gotQuizChoice : got choice " + listid + " remove current " + removeCurrent);
    }
    listContent.clear();
    newQuizHelper.showContent(listContent, INavigation.VIEWS.QUIZ);
    newQuizHelper.gotQuizChoice(listid, removeCurrent);
  }
}
