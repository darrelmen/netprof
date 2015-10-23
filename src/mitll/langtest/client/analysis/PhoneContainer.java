package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.SetCompleteDisplay;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class PhoneContainer extends SimplePagingContainer<PhoneAndScore> {
  private final Logger logger = Logger.getLogger("PhoneContainer");
  private static final int COL_WIDTH = 55;
  PhoneExampleContainer exampleContainer;

  /**
   * @param controller
   */
  public PhoneContainer(ExerciseController controller, PhoneExampleContainer exampleContainer) {
    super(controller);
//    sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());
//    this.plot = plot;
    //  this.learnTab = learnTab;
    this.exampleContainer = exampleContainer;
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<PhoneAndScore>();
    table.setSelectionModel(selectionModel);
  }

  @Override
  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", 150 + "px");
  }

  PhoneReport phoneReport;
  /**
   * @see AnalysisTab#getPhoneReport(LangTestDatabaseAsync, ExerciseController, int, Panel)
   * @param phoneReport
   * @return
   */
  public Panel getTableWithPager(PhoneReport phoneReport) {
    List<PhoneAndScore> phoneAndScores = new ArrayList<>();
    for (Map.Entry<String, Float> ps : phoneReport.getPhoneToAvgSorted().entrySet()) {
      phoneAndScores.add(new PhoneAndScore(ps.getKey(), ps.getValue()));
    }
    this.phoneReport = phoneReport;
  //  logger.info("examples " +phoneReport.getPhoneToWordAndScoreSorted());
    return getTableWithPager(phoneAndScores);
  }

  /**
   * @param sortedHistory
   * @return
   * @see SetCompleteDisplay#getScoreHistory(List, List, ExerciseController)
   */
  public Panel getTableWithPager(List<PhoneAndScore> sortedHistory) {
  //  logger.info("PhoneContainer.getTableWithPager " + sortedHistory.size());

    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    //   tableWithPager.setWidth(TABLE_HISTORY_WIDTH + "px");
    tableWithPager.addStyleName("floatLeft");

    addItems(sortedHistory);
    return tableWithPager;
  }

  private void addItems(List<PhoneAndScore> sortedHistory) {
   // logger.info("PhoneContainer.addItems " + sortedHistory.size());

    for (PhoneAndScore ps : sortedHistory) {
      addItem(ps);
    }
    flush();

    try {
      if (!sortedHistory.isEmpty()) {
        table.getSelectionModel().setSelected(sortedHistory.get(0), true);
      }
    } catch (Exception e) {
      logger.warning("Got " +e);
    }
    // showExamplesForSelectedSound();
  }

  public void showExamplesForSelectedSound() {
  // logger.info("PhoneContainer.showExamplesForSelectedSound ");
    List<PhoneAndScore> list = getList();
    if (list.isEmpty()) {
      logger.info("list empty?");
    }
    else {
   //   String phone = selectionModel.getSelectedObject().getPhone();
      String phone = list.get(0).getPhone();
  //    logger.info("first phone " + phone);

      List<WordAndScore> wordExamples = phoneReport.getWordExamples(phone);
    //  logger.info("showExamplesForSelectedSound adding " + phone + " num examples " + wordExamples.size());

      exampleContainer.addItems(phone, wordExamples);
    }
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;

    o = GWT.create(LocalTableResources.class);

    return o;
  }

  private void addReview() {
    Column<PhoneAndScore, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    table.setColumnWidth(itemCol, COL_WIDTH + "px");
//    String language = controller.getLanguage();
    //  String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader("Sound"));

    List<PhoneAndScore> dataList = getList();

    ColumnSortEvent.ListHandler<PhoneAndScore> columnSortHandler = getEnglishSorter(itemCol, dataList);
    table.addColumnSortHandler(columnSortHandler);
  }

  private ColumnSortEvent.ListHandler<PhoneAndScore> getEnglishSorter(Column<PhoneAndScore, SafeHtml> englishCol,
                                                                      List<PhoneAndScore> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<PhoneAndScore>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<PhoneAndScore>() {
          public int compare(PhoneAndScore o1, PhoneAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return o1.getPhone().compareTo(o2.getPhone());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<PhoneAndScore> getScoreSorter(Column<PhoneAndScore, SafeHtml> scoreCol,
                                                                    List<PhoneAndScore> dataList) {
    ColumnSortEvent.ListHandler<PhoneAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<PhoneAndScore>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<PhoneAndScore>() {
          public int compare(PhoneAndScore o1, PhoneAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                float a1 = o1.getScore();
                float a2 = o2.getScore();
                int i = Float.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return o1.getPhone().compareTo(o2.getPhone());
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
  protected void addColumnsToTable() {
    addReview();

    Column<PhoneAndScore, SafeHtml> scoreColumn = getScoreColumn();
    table.addColumn(scoreColumn, "Score");

    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    scoreColumn.setSortable(true);
    // table.setColumnWidth(scoreColumn, "70" + "px");
    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<PhoneAndScore> columnSortHandler2 = getScoreSorter(scoreColumn, getList());
    table.addColumnSortHandler(columnSortHandler2);

    new TooltipHelper().createAddTooltip(table, "Click on an item to review.", Placement.BOTTOM);
  }

  private Column<PhoneAndScore, SafeHtml> getItemColumn() {
    return new Column<PhoneAndScore, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, PhoneAndScore object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(PhoneAndScore shell) {
        String columnText = new WordTable().getColoredSpan(shell.getPhone(), shell.getScore());
        return getSafeHtml(columnText);
      }
    };
  }

  private void gotClickOnItem(final PhoneAndScore e) {
    exampleContainer.addItems(e.getPhone(), phoneReport.getWordExamples(e.getPhone()));
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  private Column<PhoneAndScore, SafeHtml> getScoreColumn() {
    return new Column<PhoneAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(PhoneAndScore shell) {
        float v = shell.getScore() * 100;
        String s = "<span " +
            "style='" +
            "margin-left:10px;" +
            "'" +
            ">" + ((int) v) +
            "</span>";

        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
      }
    };
  }

  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "PhoneScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
