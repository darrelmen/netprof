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

package mitll.langtest.client.list;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.analysis.AudioExampleContainer;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

class NPExerciseListContainer<T extends CommonShell, U extends HasID> extends ClickablePagingContainer<T> {
  private final Logger logger = Logger.getLogger("NPExerciseListContainer");

  private final NPExerciseList<T, U> exerciseList;
  private static final double FIFTY = 50.0;
  private static final int MAX_LENGTH_ID = 17;
  private static final int JAPANESE_LENGTH = 9;
  private static final String TRUNCATED = "...";

//  private final IExerciseComparator sorter;
  private static final String ENGLISH = "English";
  private final boolean english;
  private int FLLength = MAX_LENGTH_ID;
  private Language language;

  /**
   * @param exerciseList
   * @see NPExerciseList#makePagingContainer
   */
  NPExerciseListContainer(NPExerciseList<T, U> exerciseList) {
    super(exerciseList.controller);
    this.exerciseList = exerciseList;

    ProjectStartupInfo startupInfo = controller.getProjectStartupInfo();
    if (startupInfo == null) {
      logger.warning("PagingContainer huh? no startup info?");
    }

    language = startupInfo == null ? Language.UNKNOWN : startupInfo.getLanguageInfo();

    if (language == Language.JAPANESE) FLLength = JAPANESE_LENGTH;
    english = language == Language.ENGLISH;
  }

  @Override
  protected int getNumTableRowsGivenScreenHeight() {
    int pageSize = exerciseList.getPageSize();
    return (pageSize == -1) ? super.getNumTableRowsGivenScreenHeight() : pageSize;
  }
//
//  @NotNull
//  private IExerciseComparator getSorter() {
//    return new SimpleExerciseComparator();
//  }

  /**
   * i guess we don't want english to be sortable?
   * in the edit list dialog?
   * @see #configureTable
   */
  protected void addColumnsToTable() {
    Column<T, SafeHtml> flColumn = addFLColumn();

    Column<T, SafeHtml> englishCol = getEnglishColumn();
//    if (sortEnglish) {
//      //   logger.warning("addColumnsToTable sorting " + this);
//      englishCol.setSortable(true);
//    }
    addColumn(englishCol, new TextHeader(ENGLISH));

    List<T> dataList = getList();

    //   table.addColumnSortHandler(getEnglishSorter(englishCol, dataList));
    table.addColumnSortHandler(getFLSorter(flColumn, dataList));

    // We know that the data is sorted alphabetically by default.
//    if (sortEnglish) {
//      table.getColumnSortList().push(englishCol);
//    }

    table.setWidth("100%", true);

    // Set the width of each column.
    // table.setColumnWidth(englishCol, FIFTY, Style.Unit.PCT);
    table.setColumnWidth(flColumn, FIFTY, Style.Unit.PCT);
  }

  /**
   * @return
   * @see #addColumnsToTable
   */
  @NotNull
  private Column<T, SafeHtml> addFLColumn() {
    Column<T, SafeHtml> flColumn = getFLColumn();
    flColumn.setSortable(true);

    String headerForFL = language == Language.ENGLISH ? "Meaning" : language.toDisplay();

    addColumn(flColumn, new TextHeader(headerForFL));
    return flColumn;
  }

  private ColumnSortEvent.ListHandler<T> getFLSorter(Column<T, SafeHtml> flColumn, List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler2 = new ColumnSortEvent.ListHandler<>(dataList);

    columnSortHandler2.setComparator(flColumn,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            return (o2 == null) ? 1 : o1.getFLToShow().toLowerCase().compareTo(o2.getFLToShow().toLowerCase());
          }
          return -1;
        });
    return columnSortHandler2;
  }

/*  private ColumnSortEvent.ListHandler<T> getEnglishSorter(Column<T, SafeHtml> englishCol, List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    final boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            return (o2 == null) ? 1 : sorter.simpleCompare(o1, o2, isEnglish, "");
          }

          return -1;
        });
    return columnSortHandler;
  }*/


  /**
   * TODO: remove me
   *
   * @return
   * @see SimplePagingContainer#addColumnsToTable
   */
  private Column<T, SafeHtml> getEnglishColumn() {
    return new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (isClick(event)) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(T shell) {
        String columnText = getEnglishText(shell);
        return getColumnToolTip(columnText);
      }

      private SafeHtml getColumnToolTip(String columnText) {
        columnText = truncate(columnText);
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  private String truncate(String columnText) {
    int lengthToUse = MAX_LENGTH_ID;
    if (columnText.length() > lengthToUse) columnText = columnText.substring(0, lengthToUse - 3) + TRUNCATED;
    return columnText;
  }

  private String truncateFL(String columnText) {
    if (columnText.length() > FLLength) columnText = columnText.substring(0, FLLength - 3) + TRUNCATED;
    return columnText;
  }

  /**
   * @return
   * @see #addFLColumn
   */
  private Column<T, SafeHtml> getFLColumn() {
    return new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (isClick(event)) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(T shell) {
        String columnText = truncateFL(getFLText(shell));
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  private boolean isClick(NativeEvent event) {
    return BrowserEvents.CLICK.equals(event.getType());
  }

  /**
   * Confusing for english - english col should be foreign language for english,
   *
   * @param shell
   * @return
   */
  private String getEnglishText(CommonShell shell) {
    String s = english ? shell.getFLToShow() : shell.getEnglish();
    // if (s.isEmpty()) s = ""+shell.getID();
    return s;
  }

  /**
   * Confusing for english - fl text should be english or meaning if there is meaning
   *
   * @param shell
   * @return
   * @see #getFLColumn
   */
  private String getFLText(CommonShell shell) {
//    logger.info("getFLText on " +shell);
    String toShow = shell.getFLToShow();
    if (english) {
      String meaning = shell.getMeaning();
      toShow = meaning.isEmpty() ? shell.getEnglish() : meaning;
    }
    return toShow;
  }

  @Override
  public void gotClickOnItem(T e) {
    exerciseList.gotClickOnItem(e);
  }

  @Override
  protected CellTable.Resources chooseResources() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    boolean isRTL = projectStartupInfo != null && projectStartupInfo.getLanguageInfo().isRTL();

    CellTable.Resources o;
    if (isRTL) {   // so when we truncate long entries, the ... appears on the correct end
      // logger.info("simplePaging : chooseResources RTL - content");
      if (controller.getLanguage().equalsIgnoreCase("urdu")) {
        o = GWT.create(AudioExampleContainer.UrduTableResources.class);
      } else {
        o = GWT.create(NPExerciseListContainer.RTLTableResources.class);
      }
    } else {
      // logger.info("simplePaging : chooseResources LTR - content");
      o = GWT.create(NPExerciseListContainer.TableResources.class);
    }
    return o;
  }

  public interface TableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css"})
    NPExerciseListContainer.TableResources.TableStyle cellTableStyle();
  }

  public interface RTLTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLExerciseCellTableStyleSheet.css"})
    NPExerciseListContainer.RTLTableResources.TableStyle cellTableStyle();
  }

  public interface UrduTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "UrduExerciseCellTableStyleSheet.css"})
    NPExerciseListContainer.RTLTableResources.TableStyle cellTableStyle();
  }
}
