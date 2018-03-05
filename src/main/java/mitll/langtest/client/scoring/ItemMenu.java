package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ItemMenu {
  /**
   * @see #getDropdown
   */
  /**
   * @see #getDropdown
   */
  private static final String EMAIL = "Email Item";

  private static final String SHOW_COMMENTS = "Leave Comments";
  private static final String HIDE_COMMENTS = "Hide Comments";

  private final ExerciseController controller;
  private final CommonShell exercise;
  private boolean showingComments = false;
  private final List<CommentBox> comments = new ArrayList<>();

  ItemMenu(ExerciseController controller, CommonShell exercise) {
    this.controller = controller;
    this.exercise = exercise;
  }

  @NotNull
  Dropdown getDropdown() {
    Dropdown dropdownContainer = new Dropdown("");
    dropdownContainer.setIcon(IconType.REORDER);
    dropdownContainer.setRightDropdown(true);
    dropdownContainer.getMenuWiget().getElement().getStyle().setTop(10, Style.Unit.PCT);

    dropdownContainer.addStyleName("leftThirtyMargin");
    dropdownContainer.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);
    dropdownContainer.getTriggerWidget().setCaret(false);

    UserListSupport userListSupport = new UserListSupport(controller);
    userListSupport.addListOptions(dropdownContainer, exercise.getID());

    {
      NavLink share = new NavLink(EMAIL);
      dropdownContainer.add(share);
      share.setHref(userListSupport.getMailToExercise(exercise));
    }
    userListSupport.addSendLinkWhatYouSee(dropdownContainer);

    dropdownContainer.add(getShowComments());

    return dropdownContainer;
  }

  @NotNull
  private NavLink getShowComments() {
    NavLink widget = new NavLink(SHOW_COMMENTS);
    widget.addClickHandler(event -> {
      for (CommentBox box : comments) {
        if (showingComments) {
          box.hideButtons();
        } else {
          box.showButtons();
        }
      }
      showingComments = !showingComments;
      if (showingComments) {
        widget.setText(HIDE_COMMENTS);
      } else {
        widget.setText(SHOW_COMMENTS);
      }
    });
    return widget;
  }

  public void addCommentBox(CommentBox box) {
    this.comments.add(box);
  }
}
