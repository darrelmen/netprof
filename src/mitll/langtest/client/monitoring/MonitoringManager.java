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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.corechart.AxisOptions;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private final boolean useFile;

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @param s
   * @param feedback
   * @param useFile
   */
  public MonitoringManager(LangTestDatabaseAsync s, UserFeedback feedback, boolean useFile) {
    this.service = s;
    this.useFile = useFile;
  }

  public void showResults() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Monitoring");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    Button closeButton = new Button("Close");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");

    final VerticalPanel dialogVPanel = new VerticalPanel();
    dialogBox.setHeight("80%");
    dialogVPanel.setHeight("80%");

    int left = (Window.getClientWidth()) / 20;
    int top  = (Window.getClientHeight()) / 20;
    dialogBox.setPopupPosition(left, top);

    ScrollPanel sp = new ScrollPanel();
    sp.setHeight((int)(Window.getClientHeight()*0.8f) + "px");
    sp.setWidth((int)(Window.getClientWidth()*0.9f) + "px");

    final VerticalPanel vp = new VerticalPanel();
    vp.setWidth((int)(Window.getClientWidth()*0.88f) + "px");
    sp.add(vp);
    dialogVPanel.add(sp);

    doSessionQuery(getVPanel(vp));
    doResultQuery(getVPanel(vp));
    doGenderQuery(getVPanel(vp));
    doTimeUntilItems(getSPanel(vp));
    doResultLineQuery(getVPanel(vp));
    doResultByDayQuery(getSPanel(vp));
    doResultByHourOfDayQuery(getSPanel(vp));

    showUserInfo(getVPanel(vp));
    dialogVPanel.add(closeButton);

    dialogBox.setWidget(dialogVPanel);
    dialogBox.show();

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  private Panel getSPanel(VerticalPanel vp) {
    Panel toUse4 = new SimplePanel();
    vp.add(toUse4);
    toUse4.setWidth("100%");
    return toUse4;
  }

  private Panel getVPanel(VerticalPanel vp) {
    Panel toUse2 = new VerticalPanel();
    vp.add(toUse2);
    toUse2.setWidth("100%");
    return toUse2;
  }

  private void doTimeUntilItems(final Panel vp) {
    service.getHoursToCompletion(useFile, new AsyncCallback<Map<Integer,Float>>() {
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<Integer, Float> result) {
       // String slot = "Projected hours until answers per item";
        vp.add(getNumToHoursChart(result));
      }
    });
  }


  private void doSessionQuery(final Panel vp) {
    service.getResultStats(new AsyncCallback<Map<String, Number>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Map<String, Number> result) {
        Number total = result.get("totalHrs");
        final double totalHours = total.doubleValue();
        final double avgSecs = result.get("avgSecs").doubleValue();
        final int badRecordings = result.get("badRecordings").intValue();

        service.getSessions(new AsyncCallback<List<Session>>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(List<Session> result) {
            long totalTime = 0;
            long total = 0;
            long valid = result.size();
            Map<Long,Integer> rateToCount = new HashMap<Long, Integer>();
            for (Session s: result) {
              totalTime += s.duration;
              total += s.numAnswers;

              Integer count = rateToCount.get(s.getSecAverage());
              if (count == null) rateToCount.put(s.getSecAverage(), s.numAnswers);
              else rateToCount.put(s.getSecAverage(),count+s.numAnswers);
            }
            vp.add(new HTML("<b>Num sessions " +valid + "</b>"));
            long hoursSpent = totalTime / HOUR;
            double dhoursSpent = (double)hoursSpent;
            vp.add(new HTML("<b>Time spent " + hoursSpent + " hours " + (totalTime - hoursSpent *HOUR)/MIN + " mins"+ "</b>"));
            vp.add(new HTML("<b>Audio collected = " + roundToHundredth(totalHours) + " hours</b>"));
            vp.add(new HTML("<b># Bad audio recordings (zero length) = " + badRecordings + "</b>"));
            if (dhoursSpent > 0) {
             // vp.add(new HTML("<b>Audio yield collected/spent = " +(totalHours/dhoursSpent) + " ratio</b>"));
              vp.add(new HTML("<b>Audio yield (collected/spent) = " +Math.round((totalHours/dhoursSpent)*100) + "%</b>"));
            }
            vp.add(new HTML("<b>Answers = " + total + "</b>"));
            if (valid > 0) {
              vp.add(new HTML("<b>Avg Answers/session = " + total / valid + "</b>"));
              vp.add(new HTML("<b>Avg time spent/session = " + (totalTime / valid) / MIN + " mins" + "</b>"));
            }
            if (total > 0) {
              long rateInMillis = totalTime / total;
              vp.add(new HTML("<b>Avg time spent/item = " + (rateInMillis / 1000) + " sec" + "</b>"));
            }
            vp.add(new HTML("<b>Avg audio collected/item = " + roundToHundredth(avgSecs) + " sec"+ "</b>"));

            getRateChart(rateToCount, vp);
          }
        });
      }
    });

  }

  private float roundToHundredth(double totalHours) {
    return ((float)((Math.round(totalHours*100))))/100f;
  }

 /* private float roundToHundredth(double totalHours) {
    return ((float)((int)totalHours*100))/100f;
  }*/

  private ColumnChart getNumToHoursChart(Map<Integer, Float> rateToCount) {
    Options options = Options.create();
    options.setTitle("Hours until responses/item (given the current rate).  How long until completion?");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Num Answers");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Projected Hours");

    data.addRows(rateToCount.size());

    int r = 0;
    List<Integer> ages = new ArrayList<Integer>(rateToCount.keySet());
    Collections.sort(ages);
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, age);
      data.setValue(r++, 1, rateToCount.get(age));
    }

    return new ColumnChart(data, options);
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
      Integer value = rateToCount.get(age);
      data.setValue(r++, 1, value);
    }

    vp.add(new ColumnChart(data,options));
  }

  private void doResultLineQuery(final Panel vp) {
    service.getResultPerExercise(useFile, new AsyncCallback<Map<String,List<Integer>>>() {
      public void onFailure(Throwable caught) {}

      /**
       *
       * @param result map or overall,male,female to counts
       */
      @Override
      public void onSuccess(Map<String, List<Integer>> result) {
        String title = "Answers per Exercise";
        int size = result.values().iterator().next().size();
        int chartSamples = 1000;

        List<Integer> overall = result.get("overall");
        int total = 0;
        for (Integer c : overall) total += c;
        float ratio = ((float) total)/((float)overall.size());
        vp.add(new HTML("<b>Avg answers/item = " + roundToHundredth(ratio) +"</b>") );

        for (int i = 0; i < size; i += chartSamples) {
          Map<String, List<Integer>> typeToList = new HashMap<String, List<Integer>>();
          for (Map.Entry<String, List<Integer>> pair : result.entrySet()) {
            typeToList.put(pair.getKey(), pair.getValue().subList(i, Math.min(size, i + chartSamples)));
          }
          LineChart lineChart = getLineChart(typeToList, title + " (" + i + "-" + (i + chartSamples) + ")", i == 0, i);
          vp.add(lineChart);
        }

        int maleTotal = 0;
        int femaleTotal = 0;

        for (Integer c: result.get("male")) {
          maleTotal += c;
        }
        for (Integer c: result.get("female")) {
          femaleTotal += c;
        }
        vp.add(getGenderChart(maleTotal,femaleTotal));
      }
    });
  }

  private LineChart getLineChart(Map<String, List<Integer>> typeToList, String title, boolean goBig, int offset) {
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
      data.setValue(i, 0, offset+i);
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

  private void doResultQuery(final Panel vp) {
    service.getResultCountToCount(useFile, new AsyncCallback<Map<Integer, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<Integer, Integer> userToCount) {
        int total = 0;
        for (int v : userToCount.values()) total += v;
        Integer unanswered = userToCount.get(0);
        float ratio = total > 0 ? ((float) unanswered) / ((float) total) : 0;
        int percent = (int) (ratio*100f);
        vp.add(new HTML("<b><font color='red'>Number unanswered = " + unanswered +" or " + percent +
            "%</font></b>") );
        int numAnswered = total - unanswered;
        vp.add(new HTML("<b>Number answered = " + numAnswered +" or " + (100-percent) +"%</b>") );
        vp.add(new HTML("<b>Number with one answer = " +userToCount.get(1)+"</b>"));
        vp.add(getResultCountChart(userToCount));
      }
    });
  }


  private void doGenderQuery(final Panel vp) {
    service.getResultCountsByGender(useFile, new AsyncCallback<Map<String, Map<Integer, Integer>>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Map<Integer, Integer>> result) {
        vp.add(getGenderCounts(result.get("maleCount"), result.get("femaleCount")));
      }
    });
  }

  private void doResultByDayQuery(final Panel vp) {
    service.getResultByDay(new AsyncCallback<Map<String, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<String, Integer> userToCount) {
        vp.add(getResultByDayChart(userToCount));
      }
    });
  }

  private void doResultByHourOfDayQuery(final Panel vp) {
    service.getResultByHourOfDay(new AsyncCallback<Map<String, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<String, Integer> userToCount) {
        vp.add(getResultByHourOfDayChart(userToCount));
      }
    });
  }

  private void showUserInfo(final Panel vp) {
    service.getUserToResultCount(new AsyncCallback<Map<User, Integer>>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(Map<User, Integer> userToCount) {
        vp.add(getPerUserChart(userToCount));
        vp.add(getAgeChart(userToCount));
        vp.add(getLangChart(userToCount));
        vp.add(getDialectChart(userToCount));
        vp.add(getExperienceChart(userToCount));
        vp.add(getBrowserChart(userToCount));
      }
    });
  }

  private ColumnChart getGenderChart(int m, int f) {
    Options options = Options.create();
    options.setTitle("Answers by Gender");
    AxisOptions options1 = AxisOptions.create();
    options1.setMinValue(0);
    options.setVAxisOptions(options1);

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Gender");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Items");

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

/*  private Widget getAnnotatedResultByDayChart(Map<String, Integer> dayToCount) {
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
  }*/

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
      if (count > 1) {
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

  private ColumnChart getGenderCounts(Map<Integer, Integer> maleNumAnswerToCount,
                                      Map<Integer, Integer> femaleNumAnswerToCount) {
    Options options = Options.create();
    options.setTitle("Count of answers by gender");

    Set<Integer> slots = new HashSet<Integer>(maleNumAnswerToCount.keySet());
    slots.addAll(femaleNumAnswerToCount.keySet());
    List<Integer> slotValues = new ArrayList<Integer>(slots);
    Collections.sort(slotValues);

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Answers");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Male");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Female");
    int r = 0;
    int maleAbove = 0;
    int femaleAbove = 0;
    for (Integer age : slotValues) {
      Integer male = maleNumAnswerToCount.get(age);
      int maleCount = male != null ? male : 0;
      Integer female = femaleNumAnswerToCount.get(age);
      int femaleCount = female != null ? female : 0;
      if (age > 30) {
        maleAbove += maleCount;
        femaleAbove += femaleCount;
      } else {
        data.addRow();
        data.setValue(r, 0, "" + age);
        data.setValue(r, 1, maleCount);
        data.setValue(r, 2, femaleCount);
        r++;
      }
    }

    data.addRow();
    data.setValue(r, 0, ">30");
    data.setValue(r, 1, maleAbove);
    data.setValue(r, 2, femaleAbove);

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
      data.addRow();
      data.setValue(r, 0, key);
      data.setValue(r++, 1, langToCount.get(key));
    }
    return data;
  }
}
