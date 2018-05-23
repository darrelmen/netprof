package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.TabPane;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.DominoUpdateItem;
import mitll.langtest.shared.exercise.DominoUpdateResponse;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class ResponseModal {
  private static final Logger log = Logger.getLogger(ResponseModal.class.getName());
  /*private static final String IMPORT_ARC = "Import Excel";
  private static final String FILE_COUNT = "Total Import Rows";
  private static final String FILE_MATCH_COUNT = "Matching Import Rows";
  private static final String FILE_UNMATCHED_COUNT = "Unmatched Import Rows";

  private static final String TOT_DOCS = "Total Documents";
  private static final String MATCHED_DOCS_SUCCESS = "Updated Documents";
  private static final String MATCHED_DOCS_FAIL = "Matching Documents";

  private static final String UNMATCHED_ROW = "Unmatched Row";
  private static final String SUMMARY = "Summary";
  private static final String FILE_SUMMARY = "Excel Summary";*/
  // private static List<String> COLS = Arrays.asList(INDEX, COL, TASK, FIELD, KEY, MESSAGE);

  DominoUpdateResponse result;
  ExerciseController controller;

  /**
   * @param title
   * @param message
   * @param result
   */
  ResponseModal(String title,
                String message,
                String importFilename,
                String cmt,
                String fileFormat,
                DominoUpdateResponse result,
                DialogHelper.CloseListener closeListener,
                ExerciseController controller) {
//    super(title, message,
//        cmt, importFilename, fileFormat, null);
    this.result = result;
    this.closeListener = closeListener;
    this.controller = controller;
  }

  DialogHelper.CloseListener closeListener;


//  @Override
//  protected void prepareContentWidget() {
//    DivWidget cDivWidget = new DivWidget();
//    cDivWidget.addStyleName("bulk-update-modal-content");
//
//    Label label = getLabel(messageStr);
//    if (!messageStr.equals(IMPORT_SUCCESS_MSG)) {
//      markRed(label);
//    }
//    cDivWidget.add(label);
//
//    TabPanel tp = new TabPanel();
//    cDivWidget.add(tp);
//
//    {
//      TabPane dwPane = new TabPane(SUMMARY);
//      dwPane.add(getSummary());
//      tp.add(dwPane);
//    }
//
//    tp.add(getResultDetails());
//    tp.add(getUnmatchedRows());
//    tp.add(getExcelColReport());
//    tp.add(getRowReport());
//    tp.selectTab(0);
//    contentWidget = cDivWidget;
//  }


  //@Override
  public void prepareContentWidget() {

//    TabPane dwPane = new TabPane("Sync with domino complete!");
    DivWidget cDivWidget = new DivWidget();
    //cDivWidget.addStyleName("bulk-update-modal-content");

    //com.google.gwt.user.client.ui.Label label = getLabel(result.getMessage());

    //   int add = 0, change = 0, delete = 0;
    List<DominoUpdateItem> added = new ArrayList<>();
    List<DominoUpdateItem> changed = new ArrayList<>();
    List<DominoUpdateItem> deleted = new ArrayList<>();
    for (DominoUpdateItem item : result.getUpdates()) {
      if (item.getStatus() == DominoUpdateItem.ITEM_STATUS.ADD) {
        added.add(item);
      } else if (item.getStatus() == DominoUpdateItem.ITEM_STATUS.CHANGE) {
        changed.add(item);
      } else if (item.getStatus() == DominoUpdateItem.ITEM_STATUS.DELETE) {
        deleted.add(item);
      }
    }

    DivWidget upper = new DivWidget();
    DivWidget row = new DivWidget();
    row.setWidth("100%");
    upper.add(row);

    {
      Label label1 = getLabel("Changes since last sync on");// <b>" + result.getTimestamp() + "</b>");
      row.add(label1);
      label1.addStyleName("floatLeft");

      com.github.gwtbootstrap.client.ui.Label modified = new com.github.gwtbootstrap.client.ui.Label(LabelType.IMPORTANT, result.getTimestamp());
      modified.addStyleName("leftFiveMargin");
      modified.addStyleName("floatLeft");
      row.add(modified);
      row.addStyleName("floatLeftAndClear");
      row.addStyleName("bottomFiveMargin");
    }


    Label w = getLabel(" ");
    w.addStyleName("floatLeftAndClear");
    upper.add(w);

    Label label = getLabel("This update would make the following changes.");
    label.addStyleName("bottomFiveMargin");
    upper.add(label);
    upper.add(getLabel(" "));
    upper.add(getLabel(added.size() + " items would be added."));
    upper.add(getLabel(changed.size() + " items would be changed."));
    upper.add(getLabel(deleted.size() + " items would be deleted."));

    upper.setHeight("110px");
    cDivWidget.add(upper);
    TabPanel tp = new TabPanel();
    tp.addStyleName("bottomFiveMargin");
    tp.setHeight("400px");

    cDivWidget.add(tp);
    tp.add(getUnmatchedRows2("Added", added));
    tp.add(getUnmatchedRows2("Changed", changed));
    tp.add(getUnmatchedRows2("Deleted", deleted));
//    {
//      TabPane dwPane = new TabPane(SUMMARY);
//      dwPane.add(getSummary());
//      tp.add(dwPane);
//    }
//
//    tp.add(getResultDetails());
//    tp.add(getUnmatchedRows());
//    tp.add(getExcelColReport());
//    tp.add(getRowReport());
    tp.selectTab(0);
    tp.addStyleName("cardBorderShadow");

    //contentWidget = cDivWidget;

    new DialogHelper(true).show(
        "Do you want to continue?", cDivWidget, closeListener, 600, 800);
    // new ModalInfoDialog("Success", "Sync with domino complete!");
  }


  @NotNull
  protected Label getLabel(String messageStr) {
    Label msgLabel = new Label(messageStr);
    msgLabel.addStyleName("bulk-update-modal-msg-label");
    //msgLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
    return msgLabel;
  }

  private int addLabel(Grid g, int row, int col, String string) {
    return addLabel(g, row, col, string, false);
  }

  protected int addLabel(Grid g, int row, int col, String string, boolean isCentered) {
    SafeHtmlBuilder shb = new SafeHtmlBuilder();
    shb.appendEscapedLines(string);
    HTML idlblVal = new HTML(shb.toSafeHtml());
    g.setWidget(row, col, idlblVal);
    if (isCentered) {
      g.getCellFormatter().addStyleName(row, col, "centered-col");
    }
    return ++col;
  }

  //@Override
  protected void handleSave() {
    closeListener.gotYes();
  }

  /*	private TabPane getUnmatchedDocuments() {
		JSONValue matchResults = results.get(MetadataImportResultContainer.UNMATCHED_DOCUMENTS);

			JSONArray matches = matchResults.isArray();
		int numMatches = matches.size();

		String suffix = numMatches == 0 ? "" : " (" + numMatches + ")";
		TabPane dwPane = new TabPane((isSuccess ? UNMATCHED_DOCUMENTS_SUCCESS : UNMATCHED_DOCUMENTS_FAIL) + suffix);

		if (numMatches > 0) {
			DivWidget d = new DivWidget("bulk-update-result-pdiv");
			int cols =   4;
			Grid g = new Grid(numMatches + 1, cols);
			d.add(g);
			g.addStyleName("bulk-update-result-detail");

			int row = 0;
			addHeader(g);
			row++;

			for (int i = 0; i < numMatches; i++) {
				JSONObject oneResult = matches.get(i).isObject();

				int col = 0;

				col = addStringCol(g, row, col, oneResult, SimpleUnmatchedDocument.DOC_ID, true);
				col = addStringCol(g, row, col, oneResult, SimpleUnmatchedDocument.NAME);
				col = addCheckOrBlank(g, row, col, false);

				row++;
			}
			dwPane.add(d);
		} else {
			dwPane.add(new Label("All documents were assigned."));
		}

		return dwPane;
	}*/

/*	private void addHeader(Grid g) {
		// add header
		int col = 0;
		int row = 0;
		addHeader(g, row, col++, DOC_ID);
		addHeader(g, row, col++, NAME);

		addHeader(g, row, col++, "Attempted Match");
	}*/

  private void addUnmatchedRowHeader(Grid g) {
    // add header
    int col = 0;
    int row = 0;
    addHeader(g, row, col++, "Domino ID");
    addHeader(g, row, col++, "Netprof ID");
    addHeader(g, row, col++, "Exercise ID");
    addHeader(g, row, col++, "English");
    addHeader(g, row, col++, "Vocabulary");
    addHeader(g, row, col++, "Message");
  }

  protected int addHeader(Grid g, int row, int col, String string) {
    return addLabel(g, row, col, string, true);
  }

/*
  private void addMatchedHeader(Grid g) {
    int col = 0;
    int row = 0;
    addHeader(g, row, col++, "Success?");
    addHeader(g, row, col++, DOC_ID);
    addHeader(g, row, col++, NAME);
    addHeader(g, row, col++, "Row");
    addHeader(g, row, col++, "Message");
  }

  private void addColReportHeader(Grid g) {
    int col = 0;
    int row = 0;
    addHeader(g, row, col++, "Col #");
    addHeader(g, row, col++, "Column");
    addHeader(g, row, col++, "Task");
    addHeader(g, row, col++, "Field");
    addHeader(g, row, col++, "Key");
    addHeader(g, row, col++, "Usage");
  }

  private void addHeader(Grid g, List<String> cols) {
    int col = 0;
    int row = 0;
    for (String col1 : cols) {
      addHeader(g, row, col++, col1);
    }
  }
*/

  private TabPane getUnmatchedRows(String prefix, List<DominoUpdateItem> items) {
    // JSONValue matchResults = results.get(UNMATCHED_ROWS);
    // JSONArray matches = matchResults.isArray();
    int numMatches = items.size();
    String suffix = numMatches == 0 ? "" : " (" + numMatches + ")";
    TabPane dwPane = new TabPane(prefix + suffix);

    if (numMatches == 0) {
      com.github.gwtbootstrap.client.ui.Label w = new com.github.gwtbootstrap.client.ui.Label("Nothing " + prefix.toLowerCase() + ".");
      w.addStyleName("topFiveMargin");

      w.setType(LabelType.INFO);

      dwPane.add(w);
    } else {

      log.info("getUnmatchedRows Num unmatched : " + numMatches);

      DivWidget d = new DivWidget("bulk-update-result-pdiv");
      d.addStyleName("narrow");
      Grid g = new Grid(numMatches + 1, 6);
      d.add(g);
      g.addStyleName("bulk-update-result-detail");

      int row = 0;

      addUnmatchedRowHeader(g);
      row++;

      for (int i = 0; i < numMatches; i++) {
        DominoUpdateItem dominoUpdateItem = items.get(i);

        int col = 0;
        addLabel(g, row, col++, "" + dominoUpdateItem.getDominoID(), false);
        addLabel(g, row, col++, "" + dominoUpdateItem.getNetprofID(), false);
        addLabel(g, row, col++, "" + dominoUpdateItem.getId(), false);
        addLabel(g, row, col++, "" + dominoUpdateItem.getEnglish(), false);
        addLabel(g, row, col++, "" + dominoUpdateItem.getForeignLanguage(), false);
        String string = dominoUpdateItem.getChangedFields().isEmpty() ? "" : "" + dominoUpdateItem.getChangedFields();
        addLabel(g, row, col++, string, false);

      }
      dwPane.add(d);
    }
    return dwPane;
  }


  private TabPane getUnmatchedRows2(String prefix, Collection<DominoUpdateItem> items) {
    // JSONValue matchResults = results.get(UNMATCHED_ROWS);
    // JSONArray matches = matchResults.isArray();
    int numMatches = items.size();
    String suffix = numMatches == 0 ? "" : " (" + numMatches + ")";
    TabPane dwPane = new TabPane(prefix + suffix);

    if (numMatches == 0) {
      com.github.gwtbootstrap.client.ui.Label w = new com.github.gwtbootstrap.client.ui.Label("Nothing " + prefix.toLowerCase() + ".");
      w.addStyleName("topFiveMargin");

      w.setType(LabelType.INFO);

      dwPane.add(w);
    } else {

      log.info("getUnmatchedRows Num unmatched : " + numMatches);


      Panel tableWithPager = new DominoUpdateResponseSimplePagingContainer(controller, prefix).getTableWithPager(items);

      dwPane.add(tableWithPager);


    }
    return dwPane;
  }

  /*

  private TabPane getExcelColReport() {
    JSONValue matchResults = results.get(COLUMN_REPORT);
    JSONArray matches = matchResults.isArray();
    int numMatches = matches.size();
    String suffix = numMatches == 0 ? "" : " (" + numMatches + ")";
    TabPane dwPane = new TabPane("Excel Columns" + suffix);

    log.info("getUnmatchedRows Num unmatched : " + numMatches);

    DivWidget d = new DivWidget("bulk-update-result-pdiv");
    d.addStyleName("narrow");
    Grid g = new Grid(numMatches + 1, COLS.size());
    d.add(g);
    g.addStyleName("bulk-update-result-detail");

    int row = 0;

    addColReportHeader(g);
    row++;

    List<String> cols = COLS;
    for (int i = 0; i < numMatches; i++) {
      JSONObject oneResult = matches.get(i).isObject();
      int col = 0;
      col = addIntCol(g, row, col, oneResult, cols.get(col), false);

      for (int j = 1; j < cols.size(); j++) {
        col = addStringCol(g, row, col, oneResult, cols.get(col));
      }
      row++;
      dwPane.add(d);
    }
    return dwPane;
  }

  private TabPane getRowReport() {

    JSONValue colsInOrder = results.get(COLS_IN_ORDER);
    JSONArray colsInOrderArray = colsInOrder.isArray();

    List<String> colLabels = new ArrayList<>();
    colLabels.add("Row");
    for (int i = 0; i < colsInOrderArray.size(); i++) colLabels.add(colsInOrderArray.get(i).isString().stringValue());


    JSONValue rowMatchResults = results.get(ROW_MATCH_RESULTS);
    JSONArray rowMatchResultsArray = rowMatchResults.isArray();


    int numMatches = rowMatchResultsArray.size();
    String suffix = numMatches == 0 ? "" : " (" + numMatches + ")";
    TabPane dwPane = new TabPane("Row Report " + suffix);

    //log.info("getUnmatchedRows Num unmatched : " + numMatches);

    DivWidget d = new DivWidget("bulk-update-result-pdiv");
    d.addStyleName("narrow");
    Grid g = new Grid(numMatches + 1, colLabels.size());
    d.add(g);
    g.addStyleName("bulk-update-result-detail");

    int row = 0;

    addHeader(g, colLabels);
    row++;

    for (int i = 0; i < numMatches; i++) {
      JSONObject oneResult = rowMatchResultsArray.get(i).isObject();
      double row1 = oneResult.get("row").isNumber().doubleValue();

      log.info("row #" + row1);
      JSONValue cells = oneResult.get("cells");
      JSONArray cellsArray = cells.isArray();

      int col = 0;
      col = addLabel(g, row, col, "" + Double.valueOf(row1).intValue(), false);

      for (int j = 0; j < cellsArray.size(); j++) {
        JSONObject cellResult = cellsArray.get(j).isObject();
        String action = cellResult.get("action").isString().stringValue();
        log.info("row " + row1 + " col " + j + " = " + action);

        if (action.equals("I")) {
          col = addLabel(g, row, col, "", false);
        } else {
          String value = cellResult.get("value").isString().stringValue();
          String target = cellResult.get("target").isString().stringValue();
          log.info("row " + row1 + " col " + j + " = " + action + " set " + value + " on " + target);
          col = addLabel(g, row, col, action + " '" + value + "' on " + target, false);
        }

      }
      row++;
    }

    dwPane.add(d);
    return dwPane;
  }

  @NotNull
  private TabPane getResultDetails() {
    JSONValue matchResults = results.get(MATCH_RESULTS);

    JSONArray matches = (matchResults == null) ? null : matchResults.isArray();
    log.info("Num Matches: " + matches.toString());
    int numMatches = matches == null ? 0 : matches.size();
    String suffix = numMatches == 0 ? "" : " (" + numMatches + ")";
    TabPane resultTP = new TabPane((isSuccess ? MATCHED_DOCS_SUCCESS : MATCHED_DOCS_FAIL) + suffix);
    if (numMatches == 0) {
      resultTP.add(new Label("No documents with matching Row found to update."));
    } else {
      DivWidget d = new DivWidget("bulk-update-result-pdiv");
      int numCols = 5;
      if (!isSuccess) {
        numCols -= 2;
      }
      Grid g = new Grid(numMatches + 1, numCols);
      d.add(g);
      g.addStyleName("bulk-update-result-detail");
      int row = 0;

      addMatchedHeader(g);
      row++;
      //		tp.selectTab(0);
      for (int i = 0; i < numMatches; i++) {
        JSONObject oneResult = matches.get(i).isObject();

        int col = 0;

        g.setWidget(row, col, new Icon(getIconType(oneResult)));
        g.getCellFormatter().addStyleName(row, col++, "centered-col");
        col = addStringCol(g, row, col, oneResult, SimpleUnmatchedDocument.DOC_ID, true);
        col = addStringCol(g, row, col, oneResult, SimpleUnmatchedDocument.NAME);
        col = addStringCol(g, row, col, oneResult, "rows");
        col = addStringColMaybeRed(g, row, col, oneResult, "message");
        row++;
      }
      resultTP.add(d);
    }
    return resultTP;
  }

  @NotNull
  private Widget getSummary() {
    DivWidget d = new DivWidget("bulk-update-result-pdiv");
    d.addStyleName("right-first-col");
    d.addStyleName("narrow");
    FlexTable resp = new FlexTable();
    d.add(resp);
    resp.addStyleName("bulk-update-result-detail");

    //	int docCount = getInt(results, NUM_DOCS_DUPES);
    int uniqueDocCount = getInt(results, NUM_DOCS_UNIQUE);
    //	int unmatchedDocCount =  results.get(UNMATCHED_DOCUMENTS).isArray().size();
    int matchedDocCount = getInt(results, NUM_MATCH_FILES);

    int row = 0;


    int matchRowCount = getInt(results, NUM_MATCHED_ROWS);
    int unmatchedFileCount = results.get(UNMATCHED_ROWS).isArray().size();
    int totFileCount = matchRowCount + unmatchedFileCount;

    resp.setWidget(row, 0, getLabel("Import Summary"));
    resp.getCellFormatter().addStyleName(row, 0, "bulk-update-header-cell");
    resp.getFlexCellFormatter().setColSpan(row++, 0, 2);

    resp.setWidget(row, 0, getLabel(IMPORT_ARC));
    resp.setWidget(row++, 1, getLabel(importFilename));

    resp.setWidget(row, 0, getLabel("Match Format"));
    resp.setWidget(row++, 1, getLabel(fileFormat));


    resp.setWidget(row, 0, getLabel("Change Comment"));
    resp.setWidget(row++, 1, getLabel(cmt));

    resp.setWidget(row, 0, getLabel(FILE_SUMMARY));
    resp.getCellFormatter().addStyleName(row, 0, "bulk-update-header-cell");
    resp.getFlexCellFormatter().setColSpan(row++, 0, 2);

    resp.setWidget(row, 0, getLabel(FILE_COUNT));
    resp.setWidget(row++, 1, getLabel(Integer.toString(totFileCount)));

    resp.setWidget(row, 0, getLabel(FILE_MATCH_COUNT));
    resp.setWidget(row++, 1, getLabel(Integer.toString(matchRowCount)));

    resp.setWidget(row, 0, getLabel(FILE_UNMATCHED_COUNT));
    resp.setWidget(row++, 1, getLabel(Integer.toString(unmatchedFileCount)));

    resp.setWidget(row, 0, getLabel("Project Summary"));
    resp.getCellFormatter().addStyleName(row, 0, "bulk-update-header-cell");
    resp.getFlexCellFormatter().setColSpan(row++, 0, 2);

    resp.setWidget(row, 0, getLabel(TOT_DOCS));
    resp.setWidget(row++, 1, getLabel(Integer.toString(uniqueDocCount)));

    resp.setWidget(row, 0, getLabel((isSuccess ? MATCHED_DOCS_SUCCESS : MATCHED_DOCS_FAIL)));
    resp.setWidget(row++, 1, getLabel(Integer.toString(matchedDocCount)));

*//*		resp.setWidget(row, 0, getLabel((isSuccess ? UNMATCHED_DOCUMENTS_SUCCESS : UNMATCHED_DOCUMENTS_FAIL)));
		resp.setWidget(row++, 1, getLabel(Integer.toString(unmatchedDocCount)));*//*

   *//*		resp.setWidget(row, 0, getLabel(TOT_DOCS_UNIQUE));
		resp.setWidget(row++, 1, getLabel(Integer.toString(uniqueDocCount)));*//*


    // TODO show the total with formatted Row.
    //resp.setWidget(row, 0, getLabel(TOT_WITH_FORMATTED_Row));
    //int numWithFormattedRow = getInt(results, NUM_WITH_FORMATTED_Row);
    //
    //		float pct = (100.0f) * ((float) numWithFormattedRow) / ((float) numDocs);
    //		float fpct = roundToTenthOfPercent(pct);
    //		resp.setWidget(row++, 1, getLabel(numWithFormattedRow + " (" +
    //				fpct + "%" +
    //				")"));
    return d;
  }

//	private float roundToTenthOfPercent(float pct) {
//		float pct1000 = pct * 10f;
//		int ipct = (int) pct1000;
//		float fpct = (float) ipct;
//		fpct /= 10;
//		return fpct;
//	}*/

}