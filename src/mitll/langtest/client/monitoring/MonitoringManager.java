package mitll.langtest.client.monitoring;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.grading.GradingExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class MonitoringManager {
  private static final int PAGE_SIZE = 12;
  protected static final String UNGRADED = "Ungraded";
  protected static final String SKIP = "Skip";
  private int pageSize = PAGE_SIZE;
  protected LangTestDatabaseAsync service;
  protected UserFeedback feedback;

  //private Set<Integer> remainingResults = new HashSet<Integer>();
  private final AudioTag audioTag = new AudioTag();

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @param s
   * @param feedback
   */
  public MonitoringManager(LangTestDatabaseAsync s, UserFeedback feedback) {
    this.service = s;
    this.feedback = feedback;
  }

  public void setFeedback(GradingExercisePanel panel) {
    //this.panel = panel;
  }

  public void setPageSize(int s) { this.pageSize = s; }

  private Widget lastTable = null;
  private Button closeButton;

  public void showResults() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Monitoring");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();
/*    dialogVPanel.setWidth(1200 + "px");
    dialogBox.setWidth(1200 + "px");*/

    dialogBox.setHeight("80%");
    dialogVPanel.setHeight("80%");

    int left = (Window.getClientWidth()) / 20;
    int top  = (Window.getClientHeight()) / 20;
    dialogBox.setPopupPosition(left, top);

    service.getUserToResultCount(new AsyncCallback<Map<User, Integer>>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(Map<User, Integer> userToCount) {
        ScrollPanel sp = new ScrollPanel();
        sp.setHeight((int)(Window.getClientHeight()*0.8f) + "px");
        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("90%");
        sp.add(vp);
        dialogVPanel.add(sp);
        vp.add(getGenderChart(userToCount));
        vp.add(getAgeChart(userToCount));
        vp.add(getLangChart(userToCount));
        vp.add(getDialectChart(userToCount));
        vp.add(getExperienceChart(userToCount));
        vp.add(getBrowserChart(userToCount));
        dialogVPanel.add(closeButton);

       // lastTable = table;
        dialogBox.show();
      }
    });

    dialogBox.setWidget(dialogVPanel);

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  private ColumnChart getGenderChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle("Answers by Gender");
    options.setWidth((int)(Window.getClientWidth()*0.7f));
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Gender");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Items");

    int m = 0, f = 0;
    for (Map.Entry<User,Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      if (pair.getKey().gender == 0) m += count; else f += count;
    }

    data.addRows(2);
    data.setValue(0, 0, "Male");
    data.setValue(0, 1, m);
    data.setValue(1, 0, "Female");
    data.setValue(1, 1, f);

    return new ColumnChart(data, options);
  }

  private ColumnChart getAgeChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle("Answers by Age");
    options.setWidth((int) (Window.getClientWidth() * 0.7f));

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Age");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Items");

    Map<Integer,Integer> ageToCount = new HashMap<Integer,Integer>();
    for (Map.Entry<User,Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      int age = (user.age / 5)*5;
      Integer c = ageToCount.get(age);
      if (c == null) ageToCount.put(age,count); else ageToCount.put(age,c+count);
    }

    int r = 0;
    List<Integer> ages = new ArrayList<Integer>(ageToCount.keySet());
    Collections.sort(ages);
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, (age -5)+"-"+ age);
      data.setValue(r++, 1, ageToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

  private ColumnChart getLangChart(Map<User, Integer> userToCount) {
    String slot = "Language";
    Options options = getOptions(slot);

    Map<String,Integer> langToCount = new HashMap<String,Integer>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      Integer c = langToCount.get(user.nativeLang);
      if (count > 0) {
        if (c == null) langToCount.put(user.nativeLang, count);
        else langToCount.put(user.nativeLang, c + count);
      }
    }

    DataTable data = getDataTable(slot, langToCount);

    return new ColumnChart(data, options);
  }

  private ColumnChart getDialectChart(Map<User, Integer> userToCount) {
    String slot = "Dialect";
    Options options = getOptions(slot);

    Map<String,Integer> langToCount = new HashMap<String,Integer>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      String slotToUse = user.dialect;
      Integer c = langToCount.get(slotToUse);
      if (count > 0) {
        if (c == null) langToCount.put(slotToUse, count);
        else langToCount.put(slotToUse, c + count);
      }
    }

    DataTable data = getDataTable(slot, langToCount);

    return new ColumnChart(data, options);
  }

  private ColumnChart getBrowserChart(Map<User, Integer> userToCount) {
    BrowserCheck checker = new BrowserCheck();
    String slot = "Browser";
    Options options = getOptions(slot);

    Map<String,Integer> langToCount = new HashMap<String,Integer>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      String slotToUse = checker.getBrowser(user.ipaddr);
      Integer c = langToCount.get(slotToUse);
      if (count > 0) {
        if (c == null) langToCount.put(slotToUse, count);
        else langToCount.put(slotToUse, c + count);
      }
    }

    DataTable data = getDataTable(slot, langToCount);

    return new ColumnChart(data, options);
  }

  private Options getOptions(String slot) {
    Options options = Options.create();
    options.setTitle("Answers by " + slot);
    options.setWidth((int) (Window.getClientWidth() * 0.7f));
    return options;
  }

  private ColumnChart getExperienceChart(Map<User, Integer> userToCount) {
    String slot = "Experience";
    Options options = Options.create();
    options.setTitle("Answers by " + slot + " (months)");
    options.setWidth((int) (Window.getClientWidth() * 0.7f));

    Map<Integer,Integer> slotToCount = new HashMap<Integer,Integer>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      Integer slotToUse = user.experience;
      Integer c = slotToCount.get(slotToUse);
      if (count > 0) {
        if (c == null) slotToCount.put(slotToUse, count);
        else slotToCount.put(slotToUse, c + count);
      }
    }

    List<Integer> slotValues = new ArrayList<Integer>(slotToCount.keySet());
    Collections.sort(slotValues);

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Items");
    int r = 0;
    for (Integer age : slotValues) {
      data.addRow();
      data.setValue(r, 0, age);
      data.setValue(r++, 1, slotToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

  private DataTable getDataTable(String slot, Map<String, Integer> langToCount) {
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Items");
    int r = 0;
    for (Map.Entry<String,Integer> pair : langToCount.entrySet()) {
      String key = pair.getKey();
      data.addRow();
      data.setValue(r, 0, key);
      data.setValue(r++, 1, pair.getValue());
    }
    return data;
  }
}
