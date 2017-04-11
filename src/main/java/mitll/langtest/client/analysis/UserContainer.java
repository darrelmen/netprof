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

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.shared.analysis.UserInfo;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
public class UserContainer extends BasicUserContainer<UserInfo> {
  private final Logger logger = Logger.getLogger("UserContainer");

  private static final String CURRENT = "Avg";//"Curr.";
  private static final int CURRENT_WIDTH = 60;
  private static final int DIFF_WIDTH = 55;
  private static final int INITIAL_SCORE_WIDTH = 75;
  private static final String DIFF_COL_HEADER = "+/-";
  private static final int MIN_RECORDINGS = 5;
  public static final String STUDENT = "Student";

  private final ShowTab learnTab;
  private final DivWidget rightSide;
  private final DivWidget overallBottom;
 // private final ExerciseServiceAsync exerciseServiceAsync;

  /**
   * @param controller
   * @param rightSide
   * @see StudentAnalysis#StudentAnalysis
   */
  UserContainer(
                ExerciseController controller,
                DivWidget rightSide,
                DivWidget overallBottom,
                ShowTab learnTab,
                String selectedUserKey
  ) {
    super(controller, selectedUserKey, STUDENT);
    this.rightSide = rightSide;
    this.learnTab = learnTab;
    logger.info("overall bottom is " + overallBottom.getElement().getId() + " selected " + selectedUserKey);
   // this.exerciseServiceAsync = controller.getExerciseService();
    this.overallBottom = overallBottom;
  }

  /**
   * @see SimplePagingContainer#configureTable
   * @param sortEnglish
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    super.addColumnsToTable(sortEnglish);

    addNumber();
  //  addInitialScore();
    addCurrent();
    addFinalScore();
    addDate();

    table.getColumnSortList().push(dateCol);
    table.setWidth("100%", true);

    addTooltip();
  }

  private void addDate() {
    Column<UserInfo, SafeHtml> diff = getDiff();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(DIFF_COL_HEADER));
    table.addColumnSortHandler(getDiffSorter(diff, getList()));
    table.setColumnWidth(diff, DIFF_WIDTH + "px");
  }

  private void addCurrent() {
    Column<UserInfo, SafeHtml> current = getCurrent();
    current.setSortable(true);
    addColumn(current, new TextHeader(CURRENT));
    table.setColumnWidth(current, CURRENT_WIDTH + "px");

    table.addColumnSortHandler(getCurrentSorter(current, getList()));
  }

  private void addNumber() {
    Column<UserInfo, SafeHtml> num = getNum();
    num.setSortable(true);
    addColumn(num, new TextHeader("#"));
    table.addColumnSortHandler(getNumSorter(num, getList()));
    table.setColumnWidth(num, 50 + "px");
  }

/*
 private void addInitialScore() {
    Column<UserInfo, SafeHtml> start = getStart();
    start.setSortable(true);
    addColumn(start, new TextHeader("Initial Score"));
    table.setColumnWidth(start, INITIAL_SCORE_WIDTH + "px");
    table.addColumnSortHandler(getStartSorter(start, getList()));
  }
  */

  private void addFinalScore() {
    Column<UserInfo, SafeHtml> start = getFinal();
    start.setSortable(true);
    addColumn(start, new TextHeader("Latest"));
    table.setColumnWidth(start, INITIAL_SCORE_WIDTH + "px");
    table.addColumnSortHandler(getFinalSorter(start, getList()));
  }

  /**
   * @param englishCol
   * @param dataList
   * @return
   */
  private ColumnSortEvent.ListHandler<UserInfo> getNumSorter(Column<UserInfo, SafeHtml> englishCol,
                                                             List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getNum()).compareTo(o2.getNum());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  /*
  private ColumnSortEvent.ListHandler<UserInfo> getStartSorter(Column<UserInfo, SafeHtml> englishCol,
                                                               List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getStart()).compareTo(o2.getStart());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }
  */

  private ColumnSortEvent.ListHandler<UserInfo> getFinalSorter(Column<UserInfo, SafeHtml> englishCol,
                                                               List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getFinalScores()).compareTo(o2.getFinalScores());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserInfo> getCurrentSorter(Column<UserInfo, SafeHtml> englishCol,
                                                                 List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getCurrent()).compareTo(o2.getCurrent());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }


  private ColumnSortEvent.ListHandler<UserInfo> getDiffSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<UserInfo>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<UserInfo>() {
          public int compare(UserInfo o1, UserInfo o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return Integer.valueOf(o1.getDiff()).compareTo(o2.getDiff());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

/*
  private Column<UserInfo, SafeHtml> getStart() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getProperty())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getStart());
      }
    };
  }*/

  private Column<UserInfo, SafeHtml> getCurrent() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getCurrent());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getFinal() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getFinalScores());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getDiff() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getDiff());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getNum() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getNum());
      }
    };
  }

  public void gotClickOnItem(final UserInfo user) {
    super.gotClickOnItem(user);
    //MiniUser user1 = user.getUser();
   // int id = user.getID();
    logger.warning("gotClickOnItem " +overallBottom.getElement().getId());

/*    if (overallBottom != null) {
      overallBottom.clear();
    }
    else {
      logger.warning("\n\n\n no bottom div for " );
    }*/

    AnalysisTab widgets = new AnalysisTab(controller, learnTab, MIN_RECORDINGS, overallBottom);
    rightSide.clear();
    rightSide.add(widgets);
  }
}
