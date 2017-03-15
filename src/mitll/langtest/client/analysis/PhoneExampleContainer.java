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
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.SetCompleteDisplay;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.sound.PlayAudioWidget;
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
class PhoneExampleContainer extends SimplePagingContainer<WordAndScore> {
  private static final String WORDS_USING = "Words with ";
  private final Logger logger = Logger.getLogger("PhoneExampleContainer");

  private static final int PLAY_WIDTH = WordContainer.PLAY_WIDTH;
  private static final int ITEM_WIDTH = 200;
  private final ShowTab learnTab;
  private String phone;
  private final boolean isSpanish;
  private final TextHeader header = new TextHeader("Examples of sound");
  private final Heading heading;

  /**
   * @param controller
   * @see AnalysisTab#getPhoneReport
   */
  PhoneExampleContainer(ExerciseController controller, ShowTab learnTab, Heading heading) {
    super(controller, true);
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

  /**
   * @return
   * @see SetCompleteDisplay#getScoreHistory
   */
  public Panel getTableWithPager() {
    Panel tableWithPager = super.getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    tableWithPager.addStyleName("floatLeft");
    return tableWithPager;
  }

  /**
   * @param phone
   * @param sortedHistory
   * @see PhoneContainer#clickOnPhone(String)
   */
  public void addItems(String phone, Collection<WordAndScore> sortedHistory) {
    this.phone = phone;
    heading.setText(WORDS_USING + phone);
    heading.setSubtext("first ten");
    clear();
    if (sortedHistory != null) {
     // logger.info("PhoneExampleContainer.addItems " + sortedHistory.size() + " items");
      for (WordAndScore WordAndScore : sortedHistory) {
        addItem(WordAndScore);
      }
    } else {
      logger.warning("PhoneExampleContainer.addItems null items");
    }
    flush();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        PlayAudioWidget.addPlayer();
      }
    });
  }

/*  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
  }*/

  private Column<WordAndScore, SafeHtml> getPlayAudio() {
    return new Column<WordAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordAndScore shell) {
        // CommonShell exercise = null;//getShell(shell.getExID());
        // logger.info("Got " + shell + "  : " + exercise);
        // String title = exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
        return PlayAudioWidget.getAudioTagHTML(shell.getAnswerAudio(), "play");
      }
    };
  }

  private Column<WordAndScore, SafeHtml> getPlayNativeAudio() {
    return new Column<WordAndScore, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(WordAndScore shell) {
        //  CommonShell exercise = getShell(shell.getExID());
        // logger.info("getPlayAudio : Got " + shell.getExID() + "  : " + exercise);
        //String title = exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
        if (shell.getRefAudio() != null) {
          return PlayAudioWidget.getAudioTagHTML(shell.getRefAudio(), "play");
        } else {
          return new SafeHtmlBuilder().toSafeHtml();
        }
      }
    };
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

  @Override
  protected void addColumnsToTable() {
    Column<WordAndScore, SafeHtml> itemCol = getItemColumn();
    itemCol.setSortable(true);
    table.setColumnWidth(itemCol, ITEM_WIDTH + "px");
    addColumn(itemCol, header);
    ColumnSortEvent.ListHandler<WordAndScore> columnSortHandler = getEnglishSorter(itemCol, getList());
    table.addColumnSortHandler(columnSortHandler);

    try {
      Column<WordAndScore, SafeHtml> column = getPlayAudio();
      table.addColumn(column, "Play");
      table.setColumnWidth(column, PLAY_WIDTH + "px");

      column = getPlayNativeAudio();
      table.addColumn(column, "Ref");
      table.setColumnWidth(column, PLAY_WIDTH + "px");

      table.setWidth("100%", true);
    } catch (Exception e) {
      logger.warning("Got " + e);
    }

    new TooltipHelper().addTooltip(table, "Click on an item to review.");
  }

  /**
   * @see #addColumnsToTable()
   * @return
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
        String columnText = new WordTable().toHTML(shell.getTranscript(), phone);
        if (columnText.isEmpty()) {
          //CommonShell exercise = plot.getIdToEx().get(shell.getExID());
          String foreignLanguage = shell.getWord();//exercise == null ? "" : exercise.getForeignLanguage();
          if (isSpanish) foreignLanguage = foreignLanguage.toUpperCase();
          columnText = new WordTable().getColoredSpan(foreignLanguage, shell.getScore());
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
/*  public interface LocalTableResources extends CellTable.Resources {
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "PhoneScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }*/
}
