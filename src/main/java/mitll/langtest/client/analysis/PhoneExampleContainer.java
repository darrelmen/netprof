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

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.cell.client.Cell;
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
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.shared.analysis.WordAndScore;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
public class PhoneExampleContainer extends AudioExampleContainer<WordAndScore> {
  private final Logger logger = Logger.getLogger("PhoneExampleContainer");

  private static final String CLICK_ON = "Click on an item to review.";
  private static final String EXAMPLES_OF_SOUND = "Examples of sound";

  private static final String WORDS_USING = "Vocabulary with ";

  private static final int ITEM_WIDTH = 200;
  private final ShowTab learnTab;
  private String phone;
  private final boolean isSpanish;
  private final TextHeader header = new TextHeader(EXAMPLES_OF_SOUND);
  private final Heading heading;

  /**
   * @param controller
   * @see AnalysisTab#getPhoneReport
   */
  PhoneExampleContainer(ExerciseController controller, AnalysisPlot plot, ShowTab learnTab, Heading heading) {
    super(controller, plot);
    this.learnTab = learnTab;
    isSpanish = controller.getLanguage().equalsIgnoreCase("Spanish");
    this.heading = heading;
  }

  /**
   * Two rows each
   *
   * @return
   */
  protected int getPageSize() {
    return 4;
  }

  public Panel getTableWithPager() {
    return getTableWithPager(new ListOptions());
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
    return tableWithPager;
  }

  /**
   * @param phone
   * @param sortedHistory
   * @param maxExamples
   * @see PhoneContainer#clickOnPhone
   */
  void addItems(String phone, Collection<WordAndScore> sortedHistory, int maxExamples) {
    this.phone = phone;
    heading.setText(WORDS_USING + phone);
    {
      boolean onlyFirstFew = sortedHistory != null && sortedHistory.size() > maxExamples;
      String subtext = sortedHistory == null ? "" : sortedHistory.size() > maxExamples ? "first " + maxExamples : "" + sortedHistory.size();
      if (onlyFirstFew) heading.setSubtext(subtext);
    }
    clear();

    if (sortedHistory != null) {
      sortedHistory.forEach(this::addItem);
    } else {
      logger.warning("PhoneExampleContainer.addItems null items");
    }

    flush();
    addPlayer();
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

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    {
      Column<WordAndScore, SafeHtml> itemCol = getItemColumn();
      itemCol.setSortable(true);
      table.setColumnWidth(itemCol, ITEM_WIDTH + "px");
      addColumn(itemCol, header);
      table.addColumnSortHandler(getEnglishSorter(itemCol, getList()));
    }

    try {
      addAudioColumns();
      table.setWidth("100%", true);
    } catch (Exception e) {
      logger.warning("Got " + e);
    }

    new TooltipHelper().addTooltip(table, CLICK_ON);
  }

  /**
   * @return
   * @see  #addColumnsToTable
   */
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
        String columnText = new WordTable().toHTML(shell.getFullTranscript(), phone);
        if (columnText.isEmpty()) {
          String foreignLanguage = shell.getWord();
          if (isSpanish) foreignLanguage = foreignLanguage.toUpperCase();
          columnText = new WordTable().getColoredSpan(foreignLanguage, shell.getPronScore());
        }
        return getSafeHtml(columnText);
      }
    };
  }

  private void gotClickOnItem(final WordAndScore e) {
    learnTab.showLearnAndItem(e.getExid());
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
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
