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

    {
      UserListSupport userListSupport = new UserListSupport(controller);
      userListSupport.addListOptions(dropdownContainer, exercise.getID());

      {
        NavLink share = new NavLink(SHARE_ITEM);
        dropdownContainer.add(share);
        share.setHref(userListSupport.getMailToExercise(exercise));
      }
      userListSupport.addSendLinkWhatYouSee(dropdownContainer);
    }

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
