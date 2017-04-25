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

package mitll.langtest.client.flashcard;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.langtest.shared.sorter.ExerciseComparator;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class ScoreHistoryContainer extends SimplePagingContainer<ExerciseCorrectAndScore> {
  private final Logger logger = Logger.getLogger("ScoreHistoryContainer");

  private static final int HISTORY_COL_WIDTH = 105;

  private static final String ENGLISH = "English";
  private final boolean english;

  private static final int MAX_LENGTH_ID = 15;
  static final int TABLE_HISTORY_WIDTH = 420;
  private static final int COL_WIDTH = 130;
  private static final String CORRECT_INCORRECT_HISTORY_AND_AVERAGE_PRONUNCIATION_SCORE = "Correct/Incorrect history and average pronunciation score";

  private final Map<Integer, CommonShell> idToExercise = new HashMap<>();
  private final ExerciseComparator sorter;

  /**
   * @param controller
   * @param allExercises
   */
  ScoreHistoryContainer(ExerciseController controller, Collection<? extends CommonShell> allExercises) {
    super(controller);
    english = controller.getLanguage().equals(ENGLISH);
    sorter = new ExerciseComparator();//controller.getProjectStartupInfo().getTypeOrder());

    for (CommonShell commonShell : allExercises) {
      idToExercise.put(commonShell.getID(), commonShell);
    }
  }

  /**
   * Scroll to first score.
   * @param sortedHistory
   * @return
   * @see SetCompleteDisplay#getScoreHistory
   */
  public Panel getTableWithPager(List<ExerciseCorrectAndScore> sortedHistory) {
    Panel tableWithPager = getTableWithPager(new ListOptions());
    tableWithPager.getElement().setId("TableScoreHistory");

    int last = -1;
    int i = 0;
    for (ExerciseCorrectAndScore exerciseCorrectAndScore : sortedHistory) {
      addItem(exerciseCorrectAndScore);
      if (last == -1 && !exerciseCorrectAndScore.isEmpty()) {
  //      logger.info("found non empty " + exerciseCorrectAndScore.getId());
        last = i;
      }
      i++;
    }

    final int flast = last;
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
       // logger.info("scroll to visible " + flast);
        scrollToVisible(flast);
      }
    });

    flush();
    return tableWithPager;
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    if (controller.isRightAlignContent()) {   // so when we truncate long entries, the ... appears on the correct end
      o = GWT.create(RTLLocalTableResources.class);
    } else {
      o = GWT.create(LocalTableResources.class);
    }
    return o;
  }

  private void addEnglishAndFL() {
    Column<ExerciseCorrectAndScore, SafeHtml> englishCol = getEnglishColumn();
    englishCol.setSortable(true);
    table.setColumnWidth(englishCol, COL_WIDTH + "px");

    Column<ExerciseCorrectAndScore, SafeHtml> flColumn = getFLColumn();
    flColumn.setSortable(true);
    table.setColumnWidth(flColumn, COL_WIDTH + "px");

    String language = controller.getLanguage();
    String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(flColumn, new TextHeader(headerForFL));
    addColumn(englishCol, new TextHeader("English"));

    List<ExerciseCorrectAndScore> dataList = getList();

    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler = getEnglishSorter(englishCol, dataList);
    table.addColumnSortHandler(columnSortHandler);

    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler2 = getFLSorter(flColumn, dataList);
    table.addColumnSortHandler(columnSortHandler2);

    table.setWidth("100%", true);
    // We know that the data is sorted alphabetically by default.
//    table.getColumnSortList().push(englishCol);
  }

  private String truncate(String columnText) {
    if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
    return columnText;
  }

  private ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> getEnglishSorter(Column<ExerciseCorrectAndScore, SafeHtml> englishCol,
                                                                                List<ExerciseCorrectAndScore> dataList) {
    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<ExerciseCorrectAndScore>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<ExerciseCorrectAndScore>() {
          public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                CommonShell shell1 = idToExercise.get(o1.getId());
                CommonShell shell2 = idToExercise.get(o2.getId());
                return sorter.compareStrings(getEnglishText(shell1), getEnglishText(shell2));
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> getFLSorter(Column<ExerciseCorrectAndScore, SafeHtml> flColumn,
                                                                           List<ExerciseCorrectAndScore> dataList) {
    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler2 = new ColumnSortEvent.ListHandler<ExerciseCorrectAndScore>(dataList);

    columnSortHandler2.setComparator(flColumn,
        new Comparator<ExerciseCorrectAndScore>() {
          public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                CommonShell shell1 = idToExercise.get(o1.getId());
                CommonShell shell2 = idToExercise.get(o2.getId());

                String id1 = getFLText(shell1);
                String id2 = getFLText(shell2);
                return id1.toLowerCase().compareTo(id2.toLowerCase());
              }
            }
            return -1;
          }
        });
    return columnSortHandler2;
  }


  private ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> getScoreSorter(Column<ExerciseCorrectAndScore, SafeHtml> scoreCol,
                                                                              List<ExerciseCorrectAndScore> dataList) {
    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<ExerciseCorrectAndScore>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<ExerciseCorrectAndScore>() {
          public int compare(ExerciseCorrectAndScore o1, ExerciseCorrectAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                int a1 = o1.getAvgScorePercent();
                int a2 = o2.getAvgScorePercent();
                int i = Integer.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  CommonShell shell1 = idToExercise.get(o1.getId());
                  CommonShell shell2 = idToExercise.get(o2.getId());
                  if (o1.getId() == o2.getId()) logger.warning("same id " + o1.getId());
                  return shell1.getEnglish().compareTo(shell2.getEnglish());
                } else {
                  return i;
                }
              }
            } else {
              logger.warning("------- o1 is null?");

              return -1;
            }
          }
        });
    return columnSortHandler;
  }

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    addEnglishAndFL();

    Column<ExerciseCorrectAndScore, SafeHtml> column2 = getHistoryColumn();
    table.addColumn(column2, "History");
    table.setColumnWidth(column2, HISTORY_COL_WIDTH + "px");

    Column<ExerciseCorrectAndScore, SafeHtml> scoreColumn = getScoreColumn();
    table.addColumn(scoreColumn, "Score");

    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    scoreColumn.setSortable(true);
    table.setColumnWidth(scoreColumn, 70 + "px");
    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<ExerciseCorrectAndScore> columnSortHandler2 = getScoreSorter(scoreColumn, getList());
    table.addColumnSortHandler(columnSortHandler2);

    new TooltipHelper().addTooltip(table, CORRECT_INCORRECT_HISTORY_AND_AVERAGE_PRONUNCIATION_SCORE);
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getEnglishColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        CommonShell shell1 = idToExercise.get(shell.getId());
        String columnText = getEnglishText(shell1);
        return getSafeHtml(shell, truncate(columnText));
      }
    };
  }

  /**
   * TODO : what if we can't find the exercise id here?
   *
   * @return
   */
  private Column<ExerciseCorrectAndScore, SafeHtml> getFLColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        CommonShell shell1 = idToExercise.get(shell.getId());
        String toShow = shell1 == null ? "" : getFLText(shell1);
        String columnText = truncate(toShow);
        return getSafeHtml(shell, columnText);
      }
    };
  }

  private SafeHtml getSafeHtml(ExerciseCorrectAndScore shell, String columnText) {
    String html = ""+shell.getId();
    if (columnText != null) {
      if (columnText.length() > MAX_LENGTH_ID)
        columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
      html = "<span " +
          //"style='float:left;'" +
          ">" + columnText + "</span>";
    }
    return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getHistoryColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        String history = SetCompleteDisplay.getScoreHistory(shell.getCorrectAndScores());
        String s = shell.getCorrectAndScores().isEmpty() ? "" : "<span style='float:right;" +
            "'>" + history +
            "</span>";

        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
      }
    };
  }

  private Column<ExerciseCorrectAndScore, SafeHtml> getScoreColumn() {
    return new Column<ExerciseCorrectAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(ExerciseCorrectAndScore shell) {
        String s = shell.getCorrectAndScores().isEmpty() ? "" : "<span " +
            "style='" +
            "margin-left:10px;" +
            "'" +
            ">" + shell.getAvgScorePercent() +
            "</span>";

        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
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
//    logger.info("getEnglishText " + shell.getOldID() + " en " + shell.getEnglish() + " fl " + shell.getForeignLanguage() + " mn " + shell.getMeaning());
    return english ? shell.getForeignLanguage() : shell.getEnglish();
  }

  /**
   * Confusing for english - fl text should be english or meaning if there is meaning
   *
   * @param shell
   * @return
   */
  private String getFLText(CommonShell shell) {
    String toShow = shell.getForeignLanguage();
    if (english) {
      String meaning = shell.getMeaning();
      toShow = meaning.isEmpty() ? shell.getEnglish() : meaning;
    }
    return toShow;
  }

  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ScoresCellTableStyleSheet.css"})
    PagingContainer.TableResources.TableStyle cellTableStyle();
  }

  public interface RTLLocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLScoresCellTableStyleSheet.css"})
    PagingContainer.TableResources.TableStyle cellTableStyle();
  }
}
