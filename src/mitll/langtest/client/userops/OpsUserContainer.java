/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.userops;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.BasicUserContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.UserPassDialog;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;

import java.util.Comparator;
import java.util.List;

public class OpsUserContainer extends BasicUserContainer<MiniUser> {
  DivWidget rightSide;

  public OpsUserContainer(ExerciseController controller, String header, DivWidget rightSide) {
    super(controller, header);
    this.rightSide = rightSide;
    rightSide.clear();
  }

  protected int getPageSize() {
    return 30;
  }

  @Override
  protected void addColumnsToTable() {
    super.addColumnsToTable();

    getNameCol("First", true);
    getNameCol("Last", false);
    table.getColumnSortList().push(dateCol);
    table.setWidth("100%", true);

    addTooltip();
  }

  private void getNameCol(String colHeader, boolean isFirst) {
    Column<MiniUser, SafeHtml> firstNameCol = getFirstCol(isFirst);
    firstNameCol.setSortable(true);
    table.setColumnWidth(firstNameCol, ID_WIDTH + "px");
    addColumn(firstNameCol, new TextHeader(colHeader));
    ColumnSortEvent.ListHandler<MiniUser> columnSortHandler = getNameSorter(firstNameCol, getList(), isFirst);
    table.addColumnSortHandler(columnSortHandler);
  }

  private Column<MiniUser, SafeHtml> getFirstCol(boolean useFirst) {
    return new Column<MiniUser, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, MiniUser object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(MiniUser shell) {
        return getSafeHtml(truncate(useFirst ?
            shell.getFirst() :
            shell.getLast()));
      }
    };
  }

  private ColumnSortEvent.ListHandler<MiniUser> getNameSorter(Column<MiniUser, SafeHtml> englishCol,
                                                              List<MiniUser> dataList,
                                                              boolean useFirst) {
    ColumnSortEvent.ListHandler<MiniUser> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<MiniUser>() {
          public int compare(MiniUser o1, MiniUser o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return useFirst ?
                    o1.getFirst().compareTo(o2.getFirst()) :
                    o1.getLast().compareTo(o2.getLast())
                    ;
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  @Override
  protected void gotClickOnItem(final MiniUser user) {
    super.gotClickOnItem(user);

    rightSide.clear();

    if (!user.isAdmin()) {
      UserServiceAsync userService = controller.getUserService();
      userService.getUser(user.getId(), new AsyncCallback<User>() {
        @Override
        public void onFailure(Throwable throwable) {
        }

        @Override
        public void onSuccess(User user) {
          populateUserEdit(rightSide, user);
        }
      });
    }
  }

  private void populateUserEdit(DivWidget userDetail,
                                User user
  ) {

    EditUserForm signUpForm = new EditUserForm(
        controller.getProps(),
        null,
        controller,
        new UserPassDialog() {
          @Override
          public void clearSignInHasFocus() {
          }

          @Override
          public void setSignInHasFocus() {
          }
        });
    signUpForm.setSignUpButtonTitle("Edit User");
    Panel signUpForm1 = signUpForm.getSignUpForm(user);
    signUpForm1.addStyleName("leftFiveMargin");
    userDetail.add(signUpForm1);
 //   getEnabledCheckBox(user, controller.getUserService(), rightSide);
  }

/*  private void getEnabledCheckBox(final User user, final UserServiceAsync userService,
                                  DivWidget toAddTo) {
    CheckBox enabled = new CheckBox("Enabled");
    enabled.setValue(user.isEnabled());
    enabled.addStyleName("leftTenMargin");
    enabled.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!enabled.getValue()) {
          userService.deactivate(user.getId(), new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable throwable) {
              // enabled.setValue(false);
            }

            @Override
            public void onSuccess(Boolean aBoolean) {
            }
          });
        } else {
          userService.activate(user.getId(), new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable throwable) {
              // enabled.setValue(true);
            }

            @Override
            public void onSuccess(Boolean aBoolean) {
            }
          });
        }
      }
    });

    addControlGroupEntry(toAddTo, "Enabled?", enabled, "Lock or unlock user");
  }*/

/*  protected ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget widget, String hint) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    ControlLabel labelWidget = new ControlLabel(label);
    labelWidget.getElement().setId("Label_" + label);
    userGroup.add(labelWidget);
    widget.addStyleName("leftFiveMargin");

    if (hint.isEmpty()) {
      userGroup.add(widget);
    } else {
      Panel vert = new VerticalPanel();

      HTML hint1 = new HTML(hint);
      hint1.getElement().getStyle().setProperty("fontSize", "smaller");
      hint1.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);

      vert.add(widget);
      vert.add(hint1);

      userGroup.add(vert);
    }
    dialogBox.add(userGroup);
    return userGroup;
  }*/
}
