package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.cell.client.Cell;
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
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.SetCompleteDisplay;
import mitll.langtest.shared.User;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/20/15.
 */
class UserContainer extends SimplePagingContainer<User> {
  private final Logger logger = Logger.getLogger("UserContainer");

  private static final int TABLE_HISTORY_WIDTH = 420;
 // private ExerciseComparator sorter;
  //private AnalysisPlot plot;
  private ShowTab learnTab;
  DivWidget rightSide;
  LangTestDatabaseAsync service;
  /**
   * @param controller
   * @param rightSide
   */
  public UserContainer(LangTestDatabaseAsync service, ExerciseController controller, DivWidget rightSide
  //    , AnalysisPlot plot,
                       ,ShowTab learnTab
  ) {
    super(controller);
   // sorter = new ExerciseComparator(controller.getStartupInfo().getTypeOrder());
    this.rightSide = rightSide;
    //this.plot = plot;
    this.learnTab = learnTab;
    this.service = service;
  }

  /**
   * @param users
   * @return
   * @see SetCompleteDisplay#getScoreHistory(List, List, ExerciseController)
   */
  public Panel getTableWithPager(final List<User> users) {
    Panel tableWithPager = getTableWithPager();
    tableWithPager.getElement().setId("TableScoreHistory");
    //tableWithPager.setWidth(TABLE_HISTORY_WIDTH + "px");
    tableWithPager.addStyleName("floatLeft");

    for (User User : users) {
      addItem(User);
    }
    flush();


    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
//        PlayAudioWidget.addPlayer();
        User selectedObject = selectionModel.getSelectedObject();
        logger.info("seleceted is " +selectedObject);

        if (!users.isEmpty()) {
          table.getSelectionModel().setSelected(users.get(0), true);
          gotClickOnItem(users.get(0));
        }
      }
    });

    return tableWithPager;
  }

  @Override
  protected void setMaxWidth() {
    table.getElement().getStyle().setProperty("maxWidth", 150 + "px");
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<User>();
    table.setSelectionModel(selectionModel);
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
  }

  private void addReview() {
    Column<User, SafeHtml> itemCol = getUserColumn();
    itemCol.setSortable(true);
   // table.setColumnWidth(itemCol, 300 + "px");

   // String language = controller.getLanguage();

  //  String headerForFL = language.equals("English") ? "Meaning" : language;
    addColumn(itemCol, new TextHeader("Students"));
    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<User> columnSortHandler = getUserSorter(itemCol,  getList());
    table.addColumnSortHandler(columnSortHandler);
  }

  private ColumnSortEvent.ListHandler<User> getUserSorter(Column<User, SafeHtml> englishCol,
                                                                  List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(englishCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }

            // Compare the name columns.
            if (o1 != null) {
              if (o2 == null) return 1;
              else {
                return o1.getUserID().compareTo(o2.getUserID());
              }
            }
            return -1;
          }
        });
    return columnSortHandler;
  }

//  private CommonShell getShell(String id) {
//    return plot.getIdToEx().get(id);
//  }

/*  private ColumnSortEvent.ListHandler<User> getScoreSorter(Column<User, SafeHtml> scoreCol,
                                                                List<User> dataList) {
    ColumnSortEvent.ListHandler<User> columnSortHandler = new ColumnSortEvent.ListHandler<User>(dataList);
    columnSortHandler.setComparator(scoreCol,
        new Comparator<User>() {
          public int compare(User o1, User o2) {
            if (o1 == o2) {
              return 0;
            }

            if (o1 != null) {
              if (o2 == null) {
                logger.warning("------- o2 is null?");
                return -1;
              } else {
                float a1 = o1.getPronScore();
                float a2 = o2.getPronScore();
                int i = Float.valueOf(a1).compareTo(a2);
                // logger.info("a1 " + a1 + " vs " + a2 + " i " + i);
                if (i == 0) {
                  return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
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
  }*/

  @Override
  protected void addColumnsToTable() {
    addReview();

/*
    Column<User, SafeHtml> scoreColumn = getScoreColumn();
    table.addColumn(scoreColumn, "Score");

    scoreColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    scoreColumn.setSortable(true);
    table.setColumnWidth(scoreColumn, "70" + "px");

    Column<User, SafeHtml> column = getPlayAudio();
    table.addColumn(column, "Play");
    table.setColumnWidth(column, 50 + "px");

    table.setWidth("100%", true);

    ColumnSortEvent.ListHandler<User> columnSortHandler2 = getScoreSorter(scoreColumn, getList());
    table.addColumnSortHandler(columnSortHandler2);
*/

    new TooltipHelper().addTooltip(table, "Click on a student.");
  }

  private Column<User, SafeHtml> getUserColumn() {
    return new Column<User, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, User object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(User shell) {
        return getSafeHtml(shell.getUserID());
      }
    };
  }

  private void gotClickOnItem(final User user) {
    AnalysisTab widgets = new AnalysisTab(service, controller, (int)user.getId(), learnTab);
    rightSide.clear();
    rightSide.add(widgets);
  }

  private SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

/*  private Column<User, SafeHtml> getScoreColumn() {
    return new Column<User, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(User shell) {
        float v = shell.getPronScore() * 100;
        String s = "<span " +
            "style='" +
            "margin-left:10px;" +
            "'" +
            ">" + ((int) v) +
            "</span>";

        return new SafeHtmlBuilder().appendHtmlConstant(s).toSafeHtml();
      }
    };
  }*/

  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ScoresCellTableStyleSheet.css"})
    TableResources.TableStyle cellTableStyle();
  }
}
