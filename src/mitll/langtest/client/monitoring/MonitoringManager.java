package mitll.langtest.client.monitoring;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
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
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
  private String item = "Item";
  private String items = item+"s";
  private String answer = "Answer";
  private String answers = answer +"s";
  private String user = "Recorder";
  private String users = user+"s";

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @param s
   * @param props
   */
  public MonitoringManager(LangTestDatabaseAsync s, PropertyHandler props) {
    this.service = s;
    this.useFile = props.isReadFromFile();
    this.item = props.getNameForItem();
    this.answer = props.getNameForAnswer();
    this.answers = props.getNameForAnswer() + "s";
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

    doDesiredQuery(getVPanel(vp));
    doSessionQuery(getVPanel(vp));
    doResultQuery(getVPanel(vp));
    doGenderQuery(getVPanel(vp));
   // doTimeUntilItems(getSPanel(vp));
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

  /**
   * @deprecated so confusing!
   * @param vp
   */
/*  private void doTimeUntilItems(final Panel vp) {
    service.getHoursToCompletion(useFile, new AsyncCallback<Map<Integer,Float>>() {
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<Integer, Float> result) {
       // String slot = "Projected hours until answers per item";
        vp.add(getNumToHoursChart(result));
      }
    });
  }*/

  private void doDesiredQuery(final Panel vp) {
    service.getDesiredCounts(useFile,new AsyncCallback<Map<String, Map<Integer, Map<Integer, Integer>>>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Map<Integer, Map<Integer, Integer>>> result) {
        vp.add(new HTML("<h2>Time to completion calculator</h2>"));
        for (String key : result.keySet()) {
          //System.out.println("got " + key);
          if (key.equals("desiredToMale")) {
            Panel hp = getItemCalculator(result, key, "Male");
            vp.add(hp);
            SimplePanel child = new SimplePanel();
            child.setHeight("5px");
            vp.add(child);
          } else if (key.equals("desiredToFemale")) {
            Panel hp = getItemCalculator(result, key, "Female");
            vp.add(hp);
          }
        }
      }

      private Panel getItemCalculator(Map<String, Map<Integer, Map<Integer, Integer>>> result, String key,String gender) {
        final Map<Integer, Map<Integer, Integer>> desiredToPeopleToItemsPerPerson = result.get(key);
        final Map<Integer, Map<Integer, Integer>> desiredToPeopleToMinutesPerPerson = result.get(key+"Hours");
        final ListBox desiredItemsBox = new ListBox();
        final ListBox numPeopleBox = new ListBox();
        final ListBox minutesBox = new ListBox();
        final HTML hoursBox = new HTML("");
      //  final HTML numPerPerson = new HTML();
        final Set<Integer> desiredSet = desiredToPeopleToItemsPerPerson.keySet();
        final List<Integer> desiredList = getBoxForSet(desiredItemsBox, desiredSet);
        final Grid g = new Grid(4,3);
       // g.getCellFormatter().getHeight(0, 1);
        desiredItemsBox.addChangeHandler(new ChangeHandler() {
          @Override
          public void onChange(ChangeEvent event) {
            Integer numDesired = getNumDesired(desiredItemsBox, desiredList);
            Map<Integer, Integer> peopleToNumPerForDesired = desiredToPeopleToItemsPerPerson.get(numDesired);
            //   System.out.println("desired " + numDesired + " people to num per " + peopleToNumPerForDesired);
            List<Integer> peopleList = getBoxForSet(numPeopleBox, peopleToNumPerForDesired.keySet());

            Integer firstPerson = peopleList.get(0);
            System.out.println("desired " + numDesired + " people list " + peopleList + " first " + firstPerson);

            setMinutesBox(numDesired, firstPerson, desiredToPeopleToMinutesPerPerson, minutesBox, hoursBox);
            setItemPerPerson(peopleToNumPerForDesired, firstPerson, g);
          }
        });

        vp.add(new HTML("<b>" +
            gender +
            " Recorders Needed</b>&nbsp;"));
        g.setText(0,0,"Desired " +answers + " per "+ item);
        g.setWidget(0, 1, desiredItemsBox);

        Map<Integer, Integer> peopleToNumPer = desiredToPeopleToItemsPerPerson.get(desiredList.get(0));
        g.setText(1,0,"Number of recorders");
        g.setWidget(1, 1, numPeopleBox);
        List<Integer> peopleList = getBoxForSet(numPeopleBox, peopleToNumPer.keySet());
        numPeopleBox.addChangeHandler(new ChangeHandler() {
          @Override
          public void onChange(ChangeEvent event) {
            Integer numDesired = getNumDesired(desiredItemsBox, desiredList);
            Map<Integer, Integer> peopleToNumPerForDesired = desiredToPeopleToItemsPerPerson.get(numDesired);

            int selectedIndex2 = numPeopleBox.getSelectedIndex();
            String value = numPeopleBox.getItemText(selectedIndex2);

            int numPeople = Integer.parseInt(value);
            //  System.out.println("desired " + numDesired + " people list " + peopleToNumPerForDesired.keySet() + " first " + numPeople);

            setMinutesBox(numDesired, numPeople, desiredToPeopleToMinutesPerPerson, minutesBox, hoursBox);
            setItemPerPerson(peopleToNumPerForDesired, numPeople, g);
          }
        });

        minutesBox.addChangeHandler(new ChangeHandler() {
          @Override
          public void onChange(ChangeEvent event) {
            int minSelected = Integer.parseInt(minutesBox.getItemText(minutesBox.getSelectedIndex()));
            int people = 0;
            Integer numDesired = getNumDesired(desiredItemsBox, desiredList);
            Map<Integer, Integer> peopleToMinutesPer = desiredToPeopleToMinutesPerPerson.get(numDesired);
            for (Map.Entry<Integer, Integer> pair : peopleToMinutesPer.entrySet()) {
                if (pair.getValue().equals(minSelected)) {
                  people = pair.getKey();
                  for (int i = 0; i < numPeopleBox.getItemCount(); i++) {
                    if (numPeopleBox.getItemText(i).equals(""+people)) {
                      numPeopleBox.setSelectedIndex(i);
                      break;
                    }
                  }
                  break;
                }
            }

            int hours = (int)Math.ceil((float)minSelected/60f);
          //  System.out.println("for " + minSelected + " minutes " + hours + " hours");
            hoursBox.setHTML(" minutes or " + hours + " hours.");

            Map<Integer, Integer> peopleToNumPerForDesired = desiredToPeopleToItemsPerPerson.get(numDesired);

            setItemPerPerson(peopleToNumPerForDesired, people, g);
          }
        });

        g.setText(3,0,items + " per recorder");
        Integer firstPerson = peopleList.get(0);
    //    Integer perPerson = peopleToNumPer.get(firstPerson);
        setItemPerPerson(peopleToNumPer, firstPerson, g);

        g.setText(2,0,"Time per recorder");
        setMinutesBox(desiredList.get(0), firstPerson, desiredToPeopleToMinutesPerPerson, minutesBox, hoursBox);
        g.setWidget(2, 1, minutesBox);
        g.setWidget(2,2, hoursBox);
        return g;
      }

/*      private void setItemsPerPerson(Grid g, Integer perPerson ) {
        g.setText(3,1,""+ perPerson);
      }*/

      private void setMinutesBox(Integer numDesired, int numPeople, Map<Integer, Map<Integer, Integer>> desiredToPeopleToMinutesPerPerson, ListBox minutesBox, HTML hoursBox) {
        Map<Integer, Integer> peopleToMinutesPer = desiredToPeopleToMinutesPerPerson.get(numDesired);
        setMinutesBox(numPeople, peopleToMinutesPer, minutesBox,hoursBox);
      }

      private void setItemPerPerson(Map<Integer, Integer> peopleToNumPerForDesired, int numPeople, Grid g) {
        Integer itemsPerPerson = peopleToNumPerForDesired.get(numPeople);
        g.setText(3, 1, "" + itemsPerPerson);
      }

      private Integer getNumDesired(ListBox desiredItemsBox, List<Integer> desiredList) {
        int selectedIndex = desiredItemsBox.getSelectedIndex();
        return desiredList.get(selectedIndex);
      }

      private void setMinutesBox(Integer firstPerson, Map<Integer, Integer> peopleToMinutesPer, ListBox minutesBox, HTML hoursBox) {
        Collection<Integer> values = peopleToMinutesPer.values();
        getBoxForSet(minutesBox, values);

        Integer minutesPerPerson = peopleToMinutesPer.get(firstPerson);
       // System.out.println("looking for "+ firstPerson + " in " + peopleToMinutesPer + " found " + minutesPerPerson);

        List<Integer> mins = new ArrayList<Integer>(values);
        Collections.sort(mins);
        int index = mins.indexOf(minutesPerPerson);
      //  System.out.println("index "+ index + " for " + minutesPerPerson + " in " + mins);
        int hours = (int)Math.ceil((float)minutesPerPerson/60f);
        System.out.println("for " + minutesPerPerson + " minutes " + hours + " hours");
        hoursBox.setHTML(" minutes or " + hours + " hours.");
        minutesBox.setSelectedIndex(index);
      }
    });
  }

  private List<Integer> getBoxForSet(ListBox box, Collection<Integer> desiredSet) {
    box.clear();
    List<Integer> desiredList = getSortedList(desiredSet);
    for (Integer desired : desiredList) box.addItem(""+desired);
    return desiredList;
  }

  private List<Integer> getSortedList(Collection<Integer> desiredSet) {
    List<Integer> desiredList =new ArrayList<Integer>(desiredSet);
    Collections.sort(desiredList);
    return desiredList;
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
        vp.add(new HTML("<h2>Session Info</h2>"));

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
            FlexTable flex=  new FlexTable();
            int row = 0;
            flex.setText(row,0,"Num sessions");
            flex.setText(row++, 1, "" + valid);

            long hoursSpent = totalTime / HOUR;
            double dhoursSpent = (double)hoursSpent;
            flex.setText(row,0,"Time spent");
            flex.setText(row++,1,""+hoursSpent + " hours " + (totalTime - hoursSpent *HOUR)/MIN + " mins");

            flex.setText(row,0,"Audio collected");
            flex.setText(row++,1,roundToHundredth(totalHours) + " hours");

            flex.setText(row,0,"# Bad audio recordings (zero length)");
            flex.setText(row++,1,""+badRecordings);

            if (dhoursSpent > 0) {
              flex.setText(row,0,"Audio yield (collected/spent)");
              flex.setText(row++,1,""+Math.round((totalHours/dhoursSpent)*100) + "%");
            }

            flex.setText(row,0,"Total "+ answers);
            flex.setText(row++,1,""+total);
            if (valid > 0) {
              flex.setText(row,0,"Avg " +
                  answers +
                  "/session");
              flex.setText(row++,1,""+(total / valid));
              flex.setText(row,0,"Avg time spent/session");
              flex.setText(row++,1,""+((totalTime / valid) / MIN) + " mins");
            }
            if (total > 0) {
              long rateInMillis = totalTime / total;
              flex.setText(row,0,"Avg time spent/" + item);
              flex.setText(row++,1,""+ (rateInMillis / 1000) + " sec" );
            }

            flex.setText(row,0,"Avg audio collected/" + item);
            flex.setText(row++,1,""+ roundToHundredth(avgSecs) + " sec");
            vp.add(flex);
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
    options.setTitle("Hours until " +
        answers +
        "/" +
        "item" +
        " (given the current rate).  How long until completion?");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Num " +
        answers);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Projected Hours");

    data.addRows(rateToCount.size());

    int r = 0;
    List<Integer> ages = getSortedList(rateToCount.keySet());
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, age);
      data.setValue(r++, 1, rateToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

  private void getRateChart(Map<Long, Integer> rateToCount, Panel vp) {
    Options options = Options.create();
    options.setTitle("Number " +answers+ " at rate");

    labelAxes(options,"Rate (seconds/" + item + ")","Count at rate");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Seconds/" +answers);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Number at Rate");

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

  private void labelAxes(Options options,String hAxisTitle ,String vAxisTitle) {
    AxisOptions options1 = AxisOptions.create();
    options1.setTitle(hAxisTitle);

    options.setHAxisOptions(options1);
    options1.set("maxTextLines",2d);
    AxisOptions options2 = AxisOptions.create();
    options2.setTitle(vAxisTitle);

    options.setVAxisOptions(options2);
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
        String title = answers + " per " + item + " index";

        int size = result.values().iterator().next().size();
        int chartSamples = 1000;

        List<Integer> overall = result.get("overall");
        int total = 0;
        for (Integer c : overall) total += c;
        float ratio = ((float) total)/((float)overall.size());
        vp.add(new HTML("<b>Avg " +
            answers +
            "/" +
            item +
            " = " + roundToHundredth(ratio) +"</b>") );

        for (int i = 0; i < size; i += chartSamples) {
          Map<String, List<Integer>> typeToList = new HashMap<String, List<Integer>>();
          for (Map.Entry<String, List<Integer>> pair : result.entrySet()) {
            int endIndex = Math.min(size, i + chartSamples);
            List<Integer> countsPerExercise = pair.getValue();
            endIndex = Math.min(countsPerExercise.size(),endIndex);
            typeToList.put(pair.getKey(), countsPerExercise.subList(i, endIndex));
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
    labelAxes(options,item +
        " index",
        "# " + answers);
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
        vp.add(new HTML("<h2>Collection progress</h2>"));
        vp.add(new HTML("<b><font color='red'>Number un" +
            answers +
            "ed = " + unanswered +" or " + percent +
            "%</font></b>") );
        int numAnswered = total - unanswered;
        vp.add(new HTML("<b>Number " +
            answer +
            "ed = " + numAnswered +" or " + (100-percent) +"%</b>") );
        vp.add(new HTML("<b>Number with one " +
            answer +
            " = " +userToCount.get(1)+"</b>"));
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
        int total =0;
        for (Integer count : userToCount.values()) total += count;
        vp.add(getPerUserChart(userToCount));
        vp.add(new HTML("Avg " + answers + " per " +
            user +
            " = " + total/userToCount.size()));
        vp.add(getUserChart(userToCount));
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
    options.setTitle(answers +
        " by Gender");
    AxisOptions options1 = AxisOptions.create();
    options1.setMinValue(0);
    options.setVAxisOptions(options1);

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Gender");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, items);

    data.addRows(2);
    data.setValue(0, 0, "Male");
    data.setValue(0, 1, m);
    data.setValue(1, 0, "Female");
    data.setValue(1, 1, f);

    return new ColumnChart(data, options);
  }

  private ColumnChart getAgeChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle(answers +
        " by Age");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Age");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, answers);

    Map<Integer,Integer> ageToCount = new HashMap<Integer,Integer>();
    for (Map.Entry<User,Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      int age = (user.age / 5)*5;
      Integer c = ageToCount.get(age);

      if (c == null) ageToCount.put(age,count); else ageToCount.put(age,c+count);
    }

    int r = 0;
    List<Integer> ages = getSortedList(ageToCount.keySet());
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, (age -5)+"-"+ (age-1));
      data.setValue(r++, 1, ageToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

  private ColumnChart getPerUserChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle("Number of " +
        answers +
        " per " +
        user);

    labelAxes(options, "", "# " + user);
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Num " + answers);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, users +
        " with this many");

    Map<Integer,Integer> ageToCount = new HashMap<Integer,Integer>();
    for (Map.Entry<User,Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      int binned = count >= 100 ? (count / 100)*100 : count >= 10 ? (count / 10)*10 : count;
      Integer c = ageToCount.get(binned);
      if (c == null) ageToCount.put(binned,1); else ageToCount.put(binned,c+1);
    }

    int r = 0;
    List<Integer> ages = getSortedList(ageToCount.keySet());
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, age >= 100 ? age +"-"+(age+99): (age >= 10 ? age +"-" +(age+9) : ""+age));
      data.setValue(r++, 1, ageToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

  private ColumnChart getResultCountChart(Map<Integer, Integer> resultToCount) {
    Options options = Options.create();
    options.setTitle("Count of " +
        answers +
        " by " +
        item);

    labelAxes(options,
        "",//answers,//answers+"/" + item + ")"
        "# " + items);


    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Num " +
        answers);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, items +
        " with this many");

    Map<Integer, Integer> ageToCount = new HashMap<Integer, Integer>();
    for (Map.Entry<Integer, Integer> pair : resultToCount.entrySet()) {
      Integer count = pair.getKey();
    /*  int binned =
          count >= 100 ? (count / 100) * 100 :
              //count >= 30 ? (count / 10) * 10 :
                  count >= 10 ? (count / 5) * 5 :
                      count;*/

      int binned =
          //count >= 100 ? (count / 100) * 100 :
              count >= 30 ? (count / 10) * 10 :
              count >= 10 ? (count / 5) * 5 :
                  count;
   //   int binned = count > 30 ? 31 : count;
      Integer c = ageToCount.get(binned);
      if (c == null) ageToCount.put(binned, pair.getValue());
      else ageToCount.put(binned, c + pair.getValue());
    }

    int r = 0;
    List<Integer> ages = getSortedList(ageToCount.keySet());
    for (Integer age : ages) {
      if (age > 1) {
        data.addRow();
        //     data.setValue(r, 0, age);
     //   data.setValue(r, 0, age > 30 ?  "> 30" : "" + age);
  /*          age >= 100 ? ""+ age+"-"+(age+99) :
                //(age >= 30 ? (age - 10) + "-" + age :
                    (age >= 10 ? age +"-"+(age+4) :
                        "" + age));*/
        //age >= 100 ? ""+ age+"-"+(age+99) :
        data.setValue(r, 0,
            (age >= 30 ? age + "-" + (age+9) :
            (age >= 10 ? age +"-"+(age+4) :
                "" + age)));
        data.setValue(r++, 1, ageToCount.get(age));
      }
    }

    return new ColumnChart(data, options);
  }

  private Widget getResultByDayChart(Map<String, Integer> dayToCount) {
    String slot = "Day";
    return getColumnChart(dayToCount, slot, true);
  }

  private Widget getColumnChart(Map<String, Integer> dayToCount, String slot, boolean isDate) {
    Options options = getOptions(slot);
    DataTable data = getDataTable(slot, dayToCount,isDate);
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
    return getColumnChart(dayToCount, slot,false);
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
      if (count > 5) {
        if (c == null) langToCount.put(nativeLang, count);
        else langToCount.put(nativeLang, c + count);
      }
    }

    DataTable data = getDataTable(slot, langToCount,false);

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
      if (count > 5) {
        if (c == null) langToCount.put(slotToUse, count);
        else langToCount.put(slotToUse, c + count);
      }
    }

    DataTable data = getDataTable(slot, langToCount,false);

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

    DataTable data = getDataTable(slot, langToCount,false);

    return new ColumnChart(data, options);
  }

  private ColumnChart getUserChart(Map<User, Integer> userToCount) {
    String slot = user;
    Options options = getOptions(slot);

    Map<String,Integer> nameToCount = new HashMap<String, Integer>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer doneByUser = pair.getValue();
      User user = pair.getKey();
      String name = user.firstName + " " +user.lastName;
      name = name.trim();
      Integer count = nameToCount.get(name);
      if (count == null) nameToCount.put(name,doneByUser);
      else nameToCount.put(name,doneByUser+count);
    }

    Map<Integer,List<String>> countToUser = new HashMap<Integer, List<String>>();
    for (Map.Entry<String, Integer> pair : nameToCount.entrySet()) {
      Integer doneByUser = pair.getValue();
      if (doneByUser > 5) {
        List<String> usersAtCount = countToUser.get(doneByUser);
        if (usersAtCount == null) countToUser.put(doneByUser, usersAtCount = new ArrayList<String>());
        String user = pair.getKey();
        usersAtCount.add(user);
      }
    }
    List<Integer> counts = new ArrayList<Integer>(countToUser.keySet());
    Collections.sort(counts, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return -1*o1.compareTo(o2);
      }
    });

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, answers);
    int r = 0;

    for (Integer c : counts) {
      List<String> users = countToUser.get(c);
      for (String u : users) {
        data.addRow();
        data.setValue(r, 0, u);//.userID + "(" + u.firstName + " " + u.lastName + ")");
        data.setValue(r++, 1, c);
      }
    }

    return new ColumnChart(data, options);
  }

  private Options getOptions(String slot) {
    Options options = Options.create();
    options.setTitle(answers +
        " by " + slot);
    return options;
  }

  private ColumnChart getExperienceChart(Map<User, Integer> userToCount) {
    String slot = "Experience";
    Options options = Options.create();
    options.setTitle(answers +
        " by " + slot + " (months)");
    labelAxes(options,"months experience","# "+answers);
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

    List<Integer> slotValues = getSortedList(slotToCount.keySet());

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, items);
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
    options.setTitle("Number of " +
        items +
        " with this many " +
        answers +
        ", by gender");

    labelAxes(options,
        "",//answers,//"",//"Number of " + answers,
        "Number of " +
            items);
   // AxisOptions options1 = AxisOptions.create();
   // options1.setTitle("# of items with this many answers");
  //  options.setHAxisOptions(options1);

    Set<Integer> slots = new HashSet<Integer>(maleNumAnswerToCount.keySet());
    slots.addAll(femaleNumAnswerToCount.keySet());
    List<Integer> slotValues = getSortedList(slots);

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, answers);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "" +
        items +
        " w/ # " +
        answers +
        ", Females");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "" +
        items +
        " w/ # " +
        answers +
        ", Males");
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
        data.setValue(r, 1, femaleCount);
        data.setValue(r, 2, maleCount);
        r++;
      }
    }

    data.addRow();
    data.setValue(r, 0, ">30");
    data.setValue(r, 1, femaleAbove);
    data.setValue(r, 2, maleAbove);

    return new ColumnChart(data, options);
  }

  private DataTable getDataTable(String slot, Map<String, Integer> langToCount, boolean isDate) {
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, answers);
    int r = 0;

    List<String> slotValues = new ArrayList<String>(langToCount.keySet());
    if (isDate) {
      Collections.sort(slotValues,new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          String[] split = o1.split("-");
          int m = Integer.parseInt(split[0]);
          int d = Integer.parseInt(split[1]);
          int y = Integer.parseInt(split[2]);

          String[] split2 = o2.split("-");
          int m2 = Integer.parseInt(split2[0]);
          int d2 = Integer.parseInt(split2[1]);
          int y2 = Integer.parseInt(split2[2]);
          return y < y2 ? -1 : y > y2 ? +1 : m < m2 ? -1 : m > m2 ? +1 : d < d2 ? -1 : d > d2 ? +1 : 0;
        }
      });
    }
    else {
      Collections.sort(slotValues);
    }

    for (String key : slotValues) {
      data.addRow();
      data.setValue(r, 0, key);
      data.setValue(r++, 1, langToCount.get(key));
    }
    return data;
  }
}
