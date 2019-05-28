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

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.SetCompleteDisplay;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.project.Language;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class PhoneExampleContainer extends AudioExampleContainer<WordAndScore> {
  private final Logger logger = Logger.getLogger("PhoneExampleContainer");

  static final String CLICK_ON = "Click on an item to review.";
  /**
   * @see #header
   */
  private static final String EXAMPLES_OF_SOUND = "Examples of sound";

  /**
   * @see #addItems
   */
  private static final String WORDS_USING = "Vocabulary with ";

  private static final int ITEM_WIDTH = 400;
  /**
   * @see #getItemColumn()
   * @see #addItems(String, String, Collection, int)
   */
  private String phone, bigram;
  private boolean first;
  private final boolean isSpanish;
  private final TextHeader header = new TextHeader(EXAMPLES_OF_SOUND);
  private final Heading heading;

  /**
   * @param controller
   * @param jumpView
   * @see AnalysisTab#getPhoneReport
   */
  PhoneExampleContainer(ExerciseController controller, Heading heading, INavigation.VIEWS jumpView) {
    super(controller, jumpView);
    isSpanish = controller.getLanguageInfo() == Language.SPANISH;
    this.heading = heading;
  }

  @Override
  protected void setMaxWidth() {
  }
  @Override
  protected int getNumTableRowsGivenScreenHeight() {
    return 8;
  }
  /**
   * Two rows each
   *
   * @return
   */
  protected int getPageSize() {
    return 4;
  }

  Panel getTableWithPager() {
    return getTableWithPager(new ListOptions().setCompact(true));
  }

  /**
   * @param listOptions
   * @return
   * @see SetCompleteDisplay#getScoreHistory
   */
  public Panel getTableWithPager(ListOptions listOptions) {
    Panel tableWithPager = super.getTableWithPager(listOptions);
    tableWithPager.getElement().setId("PhoneExampleContainer");
    tableWithPager.addStyleName("floatLeftAndClear");
    tableWithPager.getElement().getStyle().setProperty("minWidth", "334px");  // helps safari in layout

    return tableWithPager;
  }

  /**
   * @param phone
   * @param bigram
   * @param sortedHistory
   * @param maxExamples
   * @see BigramContainer#clickOnPhone2
   */
  void addItems(String phone, String bigram, Collection<WordAndScore> sortedHistory, int maxExamples) {
    stopAll();

    this.phone = phone;

    heading.setText(WORDS_USING + bigram);

    {
      String[] split = bigram.split("-");
      if (split[0].equalsIgnoreCase(phone)) {
        first = true;
        this.bigram = split[1];
      } else {
        first = false;
        this.bigram = split[0];
      }
    }

    {
      boolean onlyFirstFew = sortedHistory != null && sortedHistory.size() > maxExamples;
      if (onlyFirstFew) {
        String subtext = sortedHistory == null ? "" : sortedHistory.size() > maxExamples ? "first " + maxExamples : "" + sortedHistory.size();
        heading.setSubtext(subtext);
      }
    }
    clear();

    addItemsToTable(sortedHistory);

    flush();
  }

  private void addItemsToTable(Collection<WordAndScore> sortedHistory) {
    if (sortedHistory != null) {
      // StringBuffer buffer = new StringBuffer();
      // sortedHistory.forEach(wordAndScore -> buffer.append(wordAndScore.getPronScore()).append(", "));
      //    logger.info("PhoneExampleContainer Scores " + buffer);
      sortedHistory.forEach(this::addItem);
      if (!sortedHistory.isEmpty()) {
        setSelectedAndShowReco(sortedHistory.iterator().next(), false);
      }
    } else {
      logger.warning("PhoneExampleContainer.addItems null items");
    }
  }


  @Override
  protected void addColumnsToTable() {
    {
      Column<WordAndScore, SafeHtml> itemCol = getItemColumn();
      itemCol.setSortable(true);
      table.setColumnWidth(itemCol, ITEM_WIDTH + "px");
      addColumn(itemCol, header);
      table.addColumnSortHandler(getEnglishSorter(itemCol, getList()));
    }
    try {
      table.setWidth("100%", true);
    } catch (Exception e) {
      logger.warning("Got " + e);
    }
  }

  private ColumnSortEvent.ListHandler<WordAndScore> getEnglishSorter(Column<WordAndScore, SafeHtml> englishCol,
                                                                     List<WordAndScore> dataList) {
    ColumnSortEvent.ListHandler<WordAndScore> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0; // how?
          }

          // Compare the name columns.
          return (o1 == null) ? -1 : (o2 == null) ? 1 : o1.getWord().compareTo(o2.getWord());
        });
    return columnSortHandler;
  }

  /**
   * @return
   * @see SimplePagingContainer#addColumnsToTable
   */
  private Column<WordAndScore, SafeHtml> getItemColumn() {
    return new Column<WordAndScore, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, WordAndScore object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        gotClick(object, event);
      }

      @Override
      public SafeHtml getValue(WordAndScore shell) {
//        logger.info("for " + phone + " bigram " + bigram + " first " + first +
//            " full " + shell.getFullTranscript());
        String columnText = new WordTable().toHTML(shell.getFullTranscript(), phone, bigram, first);
//        logger.info("textx " + columnText);
        if (columnText.isEmpty()) {
          String foreignLanguage = shell.getWord();
          if (isSpanish) foreignLanguage = foreignLanguage.toUpperCase();

          columnText = new WordTable().getColoredSpan(foreignLanguage, shell.getPronScore());
        }
        return getSafeHtml(columnText);
      }
    };
  }

  /**
   * Must be public
   */
  public interface LocalTableResources extends CellTable.Resources {
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "PhoneScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
