package mitll.langtest.client.analysis;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.SetCompleteDisplay;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.shared.analysis.WordAndScore;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class PhoneExampleContainer extends SimplePagingContainer<WordAndScore> {
  private final Logger logger = Logger.getLogger("WordContainer");
  private static final int COL_WIDTH = 55;

  /**
   * @param controller
   */
  public PhoneExampleContainer(ExerciseController controller) {
    super(controller);
//    sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());
//    this.plot = plot;
    //  this.learnTab = learnTab;
  }

  /**
   * Two rows each
   * @return
   */
  protected int getPageSize() {
    return 5;
  }
/*
  @Override
  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", 150 + "px");
  }
*/

/*  public Panel getTableWithPager(PhoneReport phoneReport) {
    List<WordAndScore> WordAndScores = new ArrayList<>();
    for (Map.Entry<String,Float> ps : phoneReport.getPhoneToAvgSorted().entrySet()) {
      WordAndScores.add(new WordAndScore(ps.getKey(),ps.getValue()));
    }
    return getTableWithPager(WordAndScores);
  }*/

  /**
   * @param
   * @return
   * @see SetCompleteDisplay#getScoreHistory(List, List, ExerciseController)
   */
  public Panel getTableWithPager() {
    Panel tableWithPager = super.getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    //   tableWithPager.setWidth(TABLE_HISTORY_WIDTH + "px");
    tableWithPager.addStyleName("floatLeft");
    return tableWithPager;
  }

  String phone;

  /**
   * @see PhoneContainer#showExamplesForSelectedSound()
   * @see PhoneContainer#gotClickOnItem(PhoneAndScore)
   * @param phone
   * @param sortedHistory
   */
  public void addItems(String phone, List<WordAndScore> sortedHistory) {
    this.phone = phone;
    if (sortedHistory != null) {
      logger.info("PhoneExampleContainer.addItems " + sortedHistory.size() + " items");
    }
    else {
      logger.warning("PhoneExampleContainer.addItems null items");
    }
    clear();
    for (WordAndScore WordAndScore : sortedHistory) {
      addItem(WordAndScore);
    }
    flush();
   // header.getValue()
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;

    o = GWT.create(LocalTableResources.class);

    return o;
  }

  TextHeader header = new TextHeader("Examples of sound");
  private void addReview() {
    Column<WordAndScore, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    table.setColumnWidth(itemCol, COL_WIDTH + "px");

//    String language = controller.getLanguage();

    //  String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, header);

    List<WordAndScore> dataList = getList();

    ColumnSortEvent.ListHandler<WordAndScore> columnSortHandler = getEnglishSorter(itemCol, dataList);
    table.addColumnSortHandler(columnSortHandler);
  }

  private ColumnSortEvent.ListHandler<WordAndScore> getEnglishSorter(Column<WordAndScore, SafeHtml> englishCol,
                                                                     List<WordAndScore> dataList) {
    ColumnSortEvent.ListHandler<WordAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<WordAndScore>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<WordAndScore>() {
          public int compare(WordAndScore o1, WordAndScore o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return o1.getWord().compareTo(o2.getWord());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

/*
  private ColumnSortEvent.ListHandler<WordAndScore> getScoreSorter(Column<WordAndScore, SafeHtml> scoreCol,
                                                                   List<WordAndScore> dataList) {
    ColumnSortEvent.ListHandler<WordAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<WordAndScore>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<WordAndScore>() {
          public int compare(WordAndScore o1, WordAndScore o2) {
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
*/

  @Override
  protected void addColumnsToTable() {
    addReview();

//    Column<WordAndScore, SafeHtml> scoreColumn = getScoreColumn();
//    table.addColumn(scoreColumn, "Score");
//
//    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
//    scoreColumn.setSortable(true);
//    // table.setColumnWidth(scoreColumn, "70" + "px");
//    table.setWidth("100%", true);
//
//    ColumnSortEvent.ListHandler<WordAndScore> columnSortHandler2 = getScoreSorter(scoreColumn, getList());
//    table.addColumnSortHandler(columnSortHandler2);

    new TooltipHelper().addTooltip(table, "Click on an item to review.");
  }

  private Column<WordAndScore, SafeHtml> getItemColumn() {
    return new Column<WordAndScore, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, WordAndScore object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(WordAndScore shell) {
        String columnText = new WordTable().toHTML(shell.getTranscript(), phone);
        if (columnText.isEmpty()) {
          //CommonShell exercise = plot.getIdToEx().get(shell.getId());
          String foreignLanguage = shell.getWord();//exercise == null ? "" : exercise.getForeignLanguage();
          if (controller.getLanguage().equalsIgnoreCase("Spanish")) foreignLanguage = foreignLanguage.toUpperCase();
          columnText = new WordTable().getColoredSpan(foreignLanguage, shell.getScore());
        }
        return getSafeHtml(columnText);
      }
    };
  }

  private void gotClickOnItem(final WordAndScore e) {
    //learnTab.showLearnAndItem(e.getId());
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  private Column<WordAndScore, SafeHtml> getScoreColumn() {
    return new Column<WordAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordAndScore shell) {
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
