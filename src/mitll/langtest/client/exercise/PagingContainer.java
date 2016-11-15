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

package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.custom.dialog.EditItem;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/13
 * Time: 7:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingContainer<T extends CommonShell> extends ClickablePagingContainer<T> {
  private final Logger logger = Logger.getLogger("PagingContainer");

  private static final int MAX_LENGTH_ID = 17;
  private static final int JAPANESE_LENGTH = 9;
  private static final String TRUNCATED = "...";

  private final boolean isRecorder;
  private final ExerciseComparator sorter;
  private static final String ENGLISH = "English";
  private final boolean english;
  private final boolean showExerciseState;
  private final String instance;
  private int FLLength = MAX_LENGTH_ID;

  /**
   * @param controller
   * @param verticalUnaccountedFor
   * @param isRecorder
   * @param showExerciseState
   * @param instance
   * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer
   */
  public PagingContainer(ExerciseController controller, int verticalUnaccountedFor, boolean isRecorder,
                         boolean showExerciseState, String instance) {
    super(controller);
    sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());
    this.verticalUnaccountedFor = verticalUnaccountedFor;
    this.isRecorder = isRecorder;
    english = controller.getLanguage().equals(ENGLISH);
    boolean japanese = controller.getLanguage().equalsIgnoreCase("Japanese");
    if (japanese) FLLength = JAPANESE_LENGTH;
    this.showExerciseState = showExerciseState;
    this.instance = instance;
 //   logger.info("for " + instance + " show " + showExerciseState + " for recorder " + isRecorder);
  }

  protected void addColumnsToTable() {
    Column<T, SafeHtml> englishCol = getEnglishColumn();
    englishCol.setSortable(true);

    addColumn(englishCol, new TextHeader(ENGLISH));

    Column<T, SafeHtml> flColumn = getFLColumn();
    flColumn.setSortable(true);

    String language = controller.getLanguage();
    String headerForFL = language.equals(ENGLISH) ? "Meaning" : language;
    addColumn(flColumn, new TextHeader(headerForFL));

    List<T> dataList = getList();

    ColumnSortEvent.ListHandler<T> columnSortHandler = getEnglishSorter(englishCol, dataList);
    table.addColumnSortHandler(columnSortHandler);

    ColumnSortEvent.ListHandler<T> columnSortHandler2 = getFLSorter(flColumn, dataList);
    table.addColumnSortHandler(columnSortHandler2);

    // We know that the data is sorted alphabetically by default.
    table.getColumnSortList().push(englishCol);

    table.setWidth("100%", true);

    // Set the width of each column.
    table.setColumnWidth(englishCol, 50.0, Style.Unit.PCT);
    table.setColumnWidth(flColumn, 50.0, Style.Unit.PCT);
  }

  private ColumnSortEvent.ListHandler<T> getFLSorter(Column<T, SafeHtml> flColumn, List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler2 = new ColumnSortEvent.ListHandler<T>(dataList);

    columnSortHandler2.setComparator(flColumn,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                String id1 = o1.getForeignLanguage();
                String id2 = o2.getForeignLanguage();
                return id1.toLowerCase().compareTo(id2.toLowerCase());
              }
            }
            return -1;
          }
        });
    return columnSortHandler2;
  }

  private ColumnSortEvent.ListHandler<T> getEnglishSorter(Column<T, SafeHtml> englishCol, List<T> dataList) {
    ColumnSortEvent.ListHandler<T> columnSortHandler = new ColumnSortEvent.ListHandler<T>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<T>() {
          public int compare(T o1, T o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return sorter.simpleCompare(o1, o2, isRecorder);
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }


  /**
   * @return
   * @see #addColumnsToTable()
   */
  private Column<T, SafeHtml> getEnglishColumn() {
    return new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(T shell) {
        String columnText = getEnglishText(shell);

        if (!showExerciseState) {
          return getColumnToolTip(columnText);
        } else {
          String html = shell.getID();
          if (columnText != null) {
            columnText = truncate(columnText);
            STATE state = shell.getState();

            boolean isDefect = state == STATE.DEFECT;
            boolean isFixed = state == STATE.FIXED;
            boolean isLL = shell.getSecondState() == STATE.ATTN_LL;
            boolean isRerecord = shell.getSecondState() == STATE.RECORDED;

            boolean hasSecondState = isLL || isRerecord;
            boolean recorded = state == STATE.RECORDED;
            boolean approved = state == STATE.APPROVED || recorded;

            boolean isSet = isDefect || isFixed || approved;
            String icon =
                approved ? "icon-check" :
                    isDefect ? "icon-bug" :
                        isFixed ? "icon-thumbs-up" :
                            "";

            html = (isSet ?
                "<i " +
                    (isDefect ? "style='color:red'" :
                        isFixed ? "style='color:green'" :
                            "") +
                    " class='" +
                    icon +
                    "'></i>" +

                    "&nbsp;" : "") + columnText + (hasSecondState ?
                "&nbsp;<i " +
                    (isLL ? "style='color:gold'" : "") +
                    " class='" +
                    (isLL ? "icon-warning-sign" : "icon-microphone") +
                    "'></i>" : "");

          }
          return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
        }
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
   * @see #addColumnsToTable()
   */
  private Column<T, SafeHtml> getFLColumn() {
    return new Column<T, SafeHtml>(new ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, T object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
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


  /**
   * Confusing for english - english col should be foreign language for english,
   *
   * @param shell
   * @return
   */
  private String getEnglishText(CommonShell shell) {
//    logger.info("getEnglishText " + shell.getID() + " en " + shell.getEnglish() + " fl " + shell.getForeignLanguage() + " mn " + shell.getMeaning());
    String s = english && !shell.getEnglish().equals(EditItem.NEW_ITEM) ? shell.getForeignLanguage() : shell.getEnglish();
    if (s.isEmpty()) s = shell.getID();
    return s;
  }

  /**
   * Confusing for english - fl text should be english or meaning if there is meaning
   *
   * @param shell
   * @return
   */
  private String getFLText(CommonShell shell) {
    String toShow = shell.getForeignLanguage();
    if (english && !shell.getEnglish().equals(EditItem.NEW_ITEM)) {
      String meaning = shell.getMeaning();
      toShow = meaning.isEmpty() ? shell.getEnglish() : meaning;
    }
    if (toShow.isEmpty()) toShow = shell.getID();
    return toShow;
  }
}
