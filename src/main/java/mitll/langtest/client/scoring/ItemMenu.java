package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Divider;
import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class ItemMenu {
  /**
   * @see #getDropdown
   */
  /**
   * @see #getDropdown
   */
  private static final String SHARE_ITEM = "Share Item";

  private static final String SHOW_COMMENTS = "Leave Comments";
  private static final String HIDE_COMMENTS = "Hide Comments";
  private static final String SHARE = "Share";

  private final ExerciseController controller;
  private final CommonShell exercise;
  private boolean showingComments = false;
  private final List<CommentBox> comments = new ArrayList<>();

  ItemMenu(ExerciseController controller, CommonShell exercise) {
    this.controller = controller;
    this.exercise = exercise;
  }

  /**
   *
   * @return
   */
  @NotNull
  Dropdown getDropdown() {
    Dropdown dropdownContainer = new Dropdown(SHARE);
    {
      dropdownContainer.setIcon(IconType.SHARE_ALT);
      dropdownContainer.getTriggerWidget().addStyleName("inlineFlex");

      dropdownContainer.setRightDropdown(true);
      dropdownContainer.getMenuWiget().getElement().getStyle().setTop(10, Style.Unit.PCT);

      dropdownContainer.addStyleName("leftTenMargin");
      dropdownContainer.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);
      dropdownContainer.getTriggerWidget().setCaret(false);

      // the icon link
      Widget widget = dropdownContainer.getWidget(0);

      Element element = DOM.getChild(widget.getElement(), 0);
      element.getStyle().setMarginRight(5, Style.Unit.PX);
      element.getStyle().setMarginTop(3, Style.Unit.PX);
    }


    UserListSupport userListSupport = new UserListSupport(controller);
    userListSupport.addListOptions(dropdownContainer, exercise.getID());


    {
      NavLink share = new NavLink(SHARE_ITEM);
      dropdownContainer.add(share);
      share.setHref(userListSupport.getMailToExercise(exercise));
    }
    userListSupport.addSendLinkWhatYouSee(dropdownContainer);

    dropdownContainer.add(new Divider());

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
