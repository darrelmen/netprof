package mitll.langtest.client.monitoring;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.grading.GradingExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.Collections;
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
  private static final int MIN = (60 * 1000);
  private static final int HOUR = (60 * MIN);
  protected LangTestDatabaseAsync service;
  protected UserFeedback feedback;
  private final boolean useFile;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @param s
   * @param feedback
   * @param useFile
   */
  public MonitoringManager(LangTestDatabaseAsync s, UserFeedback feedback, boolean useFile) {
    this.service = s;
    this.feedback = feedback;
    this.useFile = useFile;
  }
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
    dialogBox.setHeight("80%");
    dialogVPanel.setHeight("80%");

    int left = (Window.getClientWidth()) / 20;
    int top  = (Window.getClientHeight()) / 20;
    dialogBox.setPopupPosition(left, top);

    ScrollPanel sp = new ScrollPanel();
    sp.setHeight((int)(Window.getClientHeight()*0.9f) + "px");
    sp.setWidth((int)(Window.getClientWidth()*0.9f) + "px");

    final VerticalPanel vp = new VerticalPanel();
    vp.setWidth((int)(Window.getClientWidth()*0.88f) + "px");
    sp.add(vp);
    dialogVPanel.add(sp);
    VerticalPanel toUse = new VerticalPanel();
    vp.add(toUse);
    doSessionQuery(vp);

    //showUserInfo(dialogBox, dialogVPanel, vp);
   // doResultLineQuery(vp, dialogVPanel, dialogBox);
    doResultQuery(vp, dialogVPanel, dialogBox);

    dialogBox.setWidget(dialogVPanel);

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  private void doSessionQuery(final Panel vp) {
    service.getSessions(new AsyncCallback<List<Session>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(List<Session> result) {
        long totalTime = 0;
        long total = 0;
        long valid = 0;
        Map<Long,Integer> rateToCount = new HashMap<Long, Integer>();
        for (Session s: result) {
          if (s.numAnswers > 1) {
            valid++;
            totalTime += s.duration;
            total += s.numAnswers;

            Integer count = rateToCount.get(s.getSecAverage());
            if (count == null) rateToCount.put(s.getSecAverage(), s.numAnswers);
            else rateToCount.put(s.getSecAverage(),count+s.numAnswers);
          }
        }
        vp.add(new HTML("<b>Num sessions " +valid + "</b>"));
        vp.add(new HTML("<b>Time spent " +totalTime/HOUR + " hours " + (totalTime - (totalTime/HOUR)*HOUR)/MIN + " mins"+ "</b>"));
        vp.add(new HTML("<b>Answers = " +total+ "</b>"));
        vp.add(new HTML("<b>Avg Answers/session = " +total/valid+ "</b>"));
        vp.add(new HTML("<b>Avg time spent/session = " +(totalTime/valid)/MIN + " mins"+ "</b>"));
        vp.add(new HTML("<b>Avg time spent/item = " +(totalTime/total)/1000 + " sec"+ "</b>"));

        getRateChart(rateToCount, vp);
      }
    });
  }

  private void getRateChart(Map<Long, Integer> rateToCount, Panel vp) {
    Options options = Options.create();
    options.setTitle("Answers by rate");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Seconds/Answer");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Seconds");

    data.addRows(rateToCount.size());

    int r = 0;
    List<Long> ages = new ArrayList<Long>(rateToCount.keySet());
    Collections.sort(ages);
    for (Long age : ages) {
      data.addRow();
      data.setValue(r, 0, age);
      data.setValue(r++, 1, rateToCount.get(age));
    }

    vp.add(new ColumnChart(data,options));
  }

  private void doResultLineQuery(final Panel vp, final Panel outer, final DialogBox dialogBox) {
    service.getResultPerExercise(useFile, new AsyncCallback<Map<String,List<Integer>>>() {
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Map<String, List<Integer>> result) {
        String title = "Answers per Exercise";
        int size = result.values().iterator().next().size();
        int chartSamples = 1000;
        for (int i = 0; i < size; i += chartSamples) {
          Map<String, List<Integer>> typeToList = new HashMap<String, List<Integer>>();
          for (Map.Entry<String, List<Integer>> pair : result.entrySet()) {
            typeToList.put(pair.getKey(), pair.getValue().subList(i, Math.min(size, i + chartSamples)));
          }
          LineChart lineChart = getLineChart(typeToList, title + " (" + i + "-" + (i + chartSamples) + ")", i == 0);
          vp.add(lineChart);
        }
        //}
        //LineChart lineChart = getLineChart(result, title);
/*        String width = (int) (Window.getClientWidth() * 0.78f) + "px";
        String height = (int) (Window.getClientHeight() * 0.78f) + "px";*/

/*
        AnnotatedTimeLine.Options options = AnnotatedTimeLine.Options.create();
        AnnotatedTimeLine lineChart = new AnnotatedTimeLine(data, options, width, height);
*/
        //  vp.add(lineChart);
        //vp.add(getResultCountChart(userToCount));

        //doResultQuery(vp, outer, dialogBox);
        doResultByDayQuery(vp, outer, dialogBox);
      }
    });
  }

  private LineChart getLineChart(Map<String,List<Integer>> typeToList, String title, boolean goBig) {
    Options options = Options.create();
    options.setTitle(title);

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Index");
    for (String key : typeToList.keySet()) {
      data.addColumn(AbstractDataTable.ColumnType.NUMBER, key);
    }
    int size = typeToList.values().iterator().next().size();
    data.addRows(size);
    for (int i = 0; i < size; i++) {
      data.setValue(i, 0, i);
    }
    int colCount = 1;
    for (String key : typeToList.keySet()) {
      List<Integer> result = typeToList.get(key);
      int r = 0;
      for (Integer n : result) {
        data.setValue(r++, colCount, n);
      }
      colCount++;
    }
    if (goBig) options.setHeight((int)(Window.getClientHeight()*0.3f));
    return new LineChart(data, options);
  }

  private void doResultQuery(final Panel vp, final Panel outer, final DialogBox dialogBox) {
    service.getResultCountToCount(useFile, new AsyncCallback<Map<Integer, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<Integer, Integer> userToCount) {
        int total = 0;
        for (int v : userToCount.values()) total += v;
        Integer unanswered = userToCount.get(0);
        float ratio = ((float) unanswered) / ((float) total);
        int percent = (int) (ratio*100f);
        vp.add(new HTML("<b><font color='red'>Number unanswered = " + unanswered +" or " + percent +
            "%</font></b>") );
        vp.add(new HTML("<b>Number answered = " +(total-unanswered)+" or " + (100-percent) +"%</b>") );
        vp.add(new HTML("<b>Number with one answer = " +userToCount.get(1)+"</b>"));
        vp.add(getResultCountChart(userToCount));
       // doResultByDayQuery(vp, outer, dialogBox);
        doResultLineQuery(vp, outer, dialogBox);
      }
    });
  }

  private void doResultByDayQuery(final Panel vp, final Panel outer, final DialogBox dialogBox) {
    service.getResultByDay(new AsyncCallback<Map<String, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<String, Integer> userToCount) {
        vp.add(getResultByDayChart(userToCount));
        doResultByHourOfDayQuery(vp, outer, dialogBox);
      }
    });
  }

  private void doResultByHourOfDayQuery(final Panel vp, final Panel outer, final DialogBox dialogBox) {
    service.getResultByHourOfDay(new AsyncCallback<Map<String, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<String, Integer> userToCount) {
        vp.add(getResultByHourOfDayChart(userToCount));
        showUserInfo(vp, outer, dialogBox);
  /*      outer.add(closeButton);
        dialogBox.show();*/
      }
    });
  }

  private void showUserInfo(final Panel vp, final Panel dialogVPanel, final DialogBox dialogBox) {
    service.getUserToResultCount(new AsyncCallback<Map<User, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<User, Integer> userToCount) {
        vp.add(getPerUserChart(userToCount));
        vp.add(getGenderChart(userToCount));
        vp.add(getAgeChart(userToCount));
        vp.add(getLangChart(userToCount));
        vp.add(getDialectChart(userToCount));
        vp.add(getExperienceChart(userToCount));
        vp.add(getBrowserChart(userToCount));
        dialogVPanel.add(closeButton);

        dialogBox.show();
      }
    });
  }

  private ColumnChart getGenderChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle("Answers by Gender");

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
      data.setValue(r, 0, (age -5)+"-"+ (age-1));
      data.setValue(r++, 1, ageToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

  private ColumnChart getPerUserChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle("Number of answers per user");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Num Answers");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Users with this many");

    Map<Integer,Integer> ageToCount = new HashMap<Integer,Integer>();
    for (Map.Entry<User,Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      int binned = count >= 100 ? (count / 100)*100 : count >= 10 ? (count / 10)*10 : count;
      Integer c = ageToCount.get(binned);
      if (c == null) ageToCount.put(binned,1); else ageToCount.put(binned,c+1);
    }

    int r = 0;
    List<Integer> ages = new ArrayList<Integer>(ageToCount.keySet());
    Collections.sort(ages);
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, age >= 100 ? age +"-"+(age+99): (age >= 10 ? age +"-" +(age+9) : ""+age));
      data.setValue(r++, 1, ageToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

  private ColumnChart getResultCountChart(Map<Integer, Integer> resultToCount) {
    Options options = Options.create();
    options.setTitle("Count of answers by item");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Num Answers");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Exercises with this many");

    Map<Integer, Integer> ageToCount = new HashMap<Integer, Integer>();
    for (Map.Entry<Integer, Integer> pair : resultToCount.entrySet()) {
      Integer count = pair.getKey();
      int binned =
          count >= 100 ? (count / 100) * 100 :
              //count >= 30 ? (count / 10) * 10 :
                  count >= 10 ? (count / 5) * 5 :
                      count;
      Integer c = ageToCount.get(binned);
      if (c == null) ageToCount.put(binned, pair.getValue());
      else ageToCount.put(binned, c + pair.getValue());
    }

    int r = 0;
    List<Integer> ages = new ArrayList<Integer>(ageToCount.keySet());
    Collections.sort(ages);
    for (Integer age : ages) {
      if (age > 1) {
        data.addRow();
        //     data.setValue(r, 0, age);
        data.setValue(r, 0,
            age >= 100 ? ""+ age+"-"+(age+99) :
                //(age >= 30 ? (age - 10) + "-" + age :
                    (age >= 10 ? age +"-"+(age+4) :
                        "" + age));
        data.setValue(r++, 1, ageToCount.get(age));
      }
    }

    return new ColumnChart(data, options);
  }

  private Widget getResultByDayChart(Map<String, Integer> dayToCount) {
    String slot = "Day";
    return getColumnChart(dayToCount, slot);
  }

  private Widget getColumnChart(Map<String, Integer> dayToCount, String slot) {
    Options options = getOptions(slot);
    DataTable data = getDataTable(slot, dayToCount);
    return new ColumnChart(data, options);
  }

  private Widget getAnnotatedResultByDayChart(Map<String, Integer> dayToCount) {
    String slot = "Day";
    //Options options = getOptions(slot);
    //DataTable data = getDataTable(slot, dayToCount);

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.DATE, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Items");
    int r = 0;

    for (Map.Entry<String, Integer> pair : dayToCount.entrySet()) {
      String key = pair.getKey();

      String month = key.substring(0,2);
      String day = key.substring(3,5);
      String year = key.substring(6,10);

      System.out.println("from " + key + " got '" + month+
          "' '" +  day+
          "' '" + year+
          "'");

      data.addRow();
      data.setValue(r, 0, new Date(Integer.parseInt(year)-1900,Integer.parseInt(month)-1,Integer.parseInt(day)));
      data.setValue(r++, 1, pair.getValue());
    }

    AnnotatedTimeLine.Options options = AnnotatedTimeLine.Options.create();
    return new AnnotatedTimeLine(data, options, (int)(Window.getClientWidth()*0.6f) + "px", (int)(Window.getClientHeight()*0.2f) + "px");
  }

  private Widget getResultByHourOfDayChart(Map<String, Integer> dayToCount) {
    String slot = "Hour of day (EST)";
    return getColumnChart(dayToCount, slot);
  }

  private ColumnChart getLangChart(Map<User, Integer> userToCount) {
    String slot = "Language";
    Options options = getOptions(slot);

    Map<String,Integer> langToCount = new HashMap<String,Integer>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      String nativeLang = user.nativeLang.toLowerCase();
      Integer c = langToCount.get(nativeLang);
      if (count > 0) {
        if (c == null) langToCount.put(nativeLang, count);
        else langToCount.put(nativeLang, c + count);
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
      String slotToUse = user.dialect.toLowerCase();
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
    return options;
  }

  private ColumnChart getExperienceChart(Map<User, Integer> userToCount) {
    String slot = "Experience";
    Options options = Options.create();
    options.setTitle("Answers by " + slot + " (months)");

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

    List<String> slotValues = new ArrayList<String>(langToCount.keySet());
    Collections.sort(slotValues);

    for (String key : slotValues) {
      //String key = pair.getKey();
      data.addRow();
      data.setValue(r, 0, key);
      data.setValue(r++, 1, langToCount.get(key));
    }
    return data;
  }
}
