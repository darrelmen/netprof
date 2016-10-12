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

package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.project.ProjectInfo;

public class ProjectContainer<T extends ProjectInfo> extends MemoryItemContainer<T> implements RequiresResize {
  private static final String FIRST = "First";
  private static final String LAST = "Last";
  private final DivWidget rightSide;
  private final ProjectOps projectOps;

  /**
   * @param controller
   * @param header
   * @param rightSide
   * @seex UserOps#getOpsUserContainer(User.Kind, DivWidget)
   */
  ProjectContainer(ExerciseController controller, String header, DivWidget rightSide,
                   ProjectOps projectOps) {
    super(controller, header);
    this.rightSide = rightSide;
    rightSide.clear();
    this.projectOps = projectOps;
  }

  protected int getPageSize() {
    return 31;
  }

  protected int getNameCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        return o1.getName().compareTo(o2.getName());
      }
    }
    return -1;
  }

  protected String getItemLabel(T shell) {
    return shell.getName();
  }

  protected int getDateCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }

    // Compare the name columns.
    if (o1 != null) {
      if (o2 == null) return 1;
      else {
        return Long.valueOf(o1.getCreated()).compareTo(o2.getCreated());
      }
    }
    return -1;
  }

  public Long getItemDate(T shell) {
    return shell.getCreated();
  }

  @Override
  protected void addColumnsToTable() {
    super.addColumnsToTable();

/*    getNameCol(FIRST, true);
    getNameCol(LAST,  false);*/

    table.getColumnSortList().push(dateCol);
    table.setWidth("100%", true);

    addTooltip();
  }

/*  private void getNameCol(String colHeader, boolean isFirst) {
    Column<ProjectInfo, SafeHtml> firstNameCol = getFirstCol(isFirst);
    firstNameCol.setSortable(true);
    table.setColumnWidth(firstNameCol, ID_WIDTH + "px");
    addColumn(firstNameCol, new TextHeader(colHeader));
    ColumnSortEvent.ListHandler<ProjectInfo> columnSortHandler = getNameSorter(firstNameCol, getList(), isFirst);
    table.addColumnSortHandler(columnSortHandler);
  }*/

/*
  private Column<ProjectInfo, SafeHtml> getFirstCol(boolean useFirst) {
    return new Column<ProjectInfo, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, ProjectInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(ProjectInfo shell) {
        return getSafeHtml(truncate(useFirst ?
            shell.getFirst() :
            shell.getLast()));
      }
    };
  }
*/

/*  private ColumnSortEvent.ListHandler<ProjectInfo> getNameSorter(Column<ProjectInfo, SafeHtml> englishCol,
                                                              List<ProjectInfo> dataList,
                                                              boolean useFirst) {
    ColumnSortEvent.ListHandler<ProjectInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<ProjectInfo>() {
          public int compare(ProjectInfo o1, ProjectInfo o2) {
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
  }*/

  @Override
  protected void gotClickOnItem(final T user) {
    super.gotClickOnItem(user);
    rightSide.clear();

/*    if (!user.isAdmin()) {
      UserServiceAsync userService = controller.getUserService();
      userService.getUser(user.getID(), new AsyncCallback<User>() {
        @Override
        public void onFailure(Throwable throwable) {
        }

        @Override
        public void onSuccess(User user) {

          //populateUserEdit(rightSide, user);
        }
      });
    }*/
  }
/*  private void populateUserEdit(DivWidget userDetail, User user) {
    EditUserForm signUpForm = new EditUserForm(
        controller.getProps(),
        controller.getUserManager(),
        controller,
        new UserPassDialog() {
          @Override
          public void clearSignInHasFocus() {
          }

          @Override
          public void setSignInHasFocus() {
          }
        }, user, projectOps);
    signUpForm.setSignUpButtonTitle("Edit User");
    Panel signUpForm1 = signUpForm.getSignUpForm(user);
    signUpForm1.addStyleName("leftFiveMargin");
    userDetail.add(signUpForm1);
  }*/
}