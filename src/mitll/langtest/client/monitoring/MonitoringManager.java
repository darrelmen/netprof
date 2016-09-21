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

package mitll.langtest.client.monitoring;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.corechart.AxisOptions;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import mitll.langtest.client.BrowserCheck;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.custom.RecordingProgressTable;
import mitll.langtest.client.services.MonitoringService;
import mitll.langtest.client.services.MonitoringServiceAsync;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.user.User;

import java.util.*;
import java.util.logging.Logger;

/**
 * Show lots of charts and graphs to allow us to follow progress of data collection.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/18/12
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class MonitoringManager {
  private static final Logger logger = Logger.getLogger("MonitoringManager");

  private static final int MIN = (60 * 1000);
  private static final int HOUR = (60 * MIN);
  private static final int MIN_COUNT_FOR_BROWSER = 10;
  private static final int MIN_COUNT_FOR_DIALECT = 10;
  private static final int MIN_SIZE_TO_TRIGGER_FILTER = 15;
  private static final int ITEM_CHART_ITEM_WIDTH = 1000;
  private static final String NUMBER_AT_RATE = "Number at Rate";
  // private static final int MAX_GRADE_ROUNDS = 3;

  private final MonitoringServiceAsync service;
  private String item = "Item";
  private final String items = item + "s";
  private String answer = "Answer";
  private String answers = answer + "s";
  private String user = "Recorder";
  private String users = user + "s";

  /**
   * @paramx s
   * @param props
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public MonitoringManager( PropertyHandler props) {
    this.service = GWT.create(MonitoringService.class);
    this.item = props.getNameForItem();
    this.answer = props.getNameForAnswer();
    this.answers = props.getNameForAnswer() + "s";
    this.user = props.getNameForRecorder();
    this.users = props.getNameForRecorder() + "s";
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

    int left = (Window.getClientWidth()) / 40;
    int top = (Window.getClientHeight()) / 160;
    dialogBox.setPopupPosition(left, top);

    ScrollPanel sp = new ScrollPanel();
    sp.setHeight((int) (Window.getClientHeight() * 0.88f) + "px");
    sp.setWidth((int) (Window.getClientWidth() * 0.90f) + "px");

    final VerticalPanel vp = new VerticalPanel();
    vp.setWidth((int) (Window.getClientWidth() * 0.88f) + "px");
    sp.add(vp);
    dialogVPanel.add(sp);

    doDesiredQuery(getVPanel(vp));
    doSessionQuery(getVPanel(vp));
    doMaleFemale(getVPanel(vp));
    doResultQuery(getVPanel(vp));
    // doGradeQuery(getVPanel(vp));
    doGenderQuery(getVPanel(vp));
    // doTimeUntilItems(getSPanel(vp));
    if (false) {
      doResultLineQuery(getVPanel(vp));
    }
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

  private interface DoIt {
    void go();
  }

  /**
   * @param vp
   */
  private void doDesiredQuery(final Panel vp) {
    service.getDesiredCounts(new AsyncCallback<Map<String, Map<Integer, Map<Integer, Integer>>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Map<String, Map<Integer, Map<Integer, Integer>>> result) {
        vp.add(new HTML("<h2>Time to completion calculator</h2>"));
        vp.add(new HTML("The options below let you work in either of two typical situations :"));
        vp.add(new HTML("1) If you have a fixed size team, choose a number of " + users.toLowerCase() +
            ", and you will see the minutes or hours of each person's time to request."));
        vp.add(new HTML("2) If you have a limit on how much time you can request of a " + user.toLowerCase() +
            ", set the minutes below to that number and you will see the required team size."));
        vp.add(new HTML("Depending on the type of collection, you may want a varying number of " + answers + " per " + item + ", which can be set with the first choice."));
        SimplePanel child1 = new SimplePanel();
        child1.setHeight("5px");
        child1.setWidth("5px");
        vp.add(child1);

        Panel hp = getItemCalculator(result, "desiredToMale", "Male");
        vp.add(hp);

        SimplePanel child = new SimplePanel();
        child.setHeight("5px");
        child.setWidth("5px");

        vp.add(child);

        Panel hp2 = getItemCalculator(result, "desiredToFemale", "Female");
        vp.add(hp2);
        if (null != null) ((DoIt) null).go();
      }

      private Panel getItemCalculator(Map<String, Map<Integer, Map<Integer, Integer>>> result, String key, String gender) {
        final Map<Integer, Map<Integer, Integer>> desiredToPeopleToItemsPerPerson = result.get(key);
        final Map<Integer, Map<Integer, Integer>> desiredToPeopleToMinutesPerPerson = result.get(key + "Hours");
        final ListBox desiredItemsBox = new ListBox();
        final ListBox numPeopleBox = new ListBox();
        final ListBox minutesBox = new ListBox();
        final HTML hoursBox = new HTML("");
        final Set<Integer> desiredSet = desiredToPeopleToItemsPerPerson.keySet();
        final List<Integer> desiredList = getBoxForSet(desiredItemsBox, desiredSet);
        final Grid g = new Grid(4, 3);
        desiredItemsBox.addChangeHandler(new ChangeHandler() {
          @Override
          public void onChange(ChangeEvent event) {
            Integer numDesired = getNumDesired(desiredItemsBox, desiredList);
            Map<Integer, Integer> peopleToNumPerForDesired = desiredToPeopleToItemsPerPerson.get(numDesired);
            List<Integer> peopleList = getBoxForSet(numPeopleBox, peopleToNumPerForDesired.keySet());

            Integer firstPerson = peopleList.get(0);
            setMinutesBox(numDesired, firstPerson, desiredToPeopleToMinutesPerPerson, minutesBox, hoursBox);
            setItemPerPerson(peopleToNumPerForDesired, firstPerson, g);
          }
        });

        vp.add(new HTML("<b>" +
            gender +
            " " + users +
            " Needed</b>&nbsp;"));
        g.setText(0, 0, "Desired " + answers + " per " + item);
        g.setWidget(0, 1, desiredItemsBox);

        Map<Integer, Integer> peopleToNumPer = desiredToPeopleToItemsPerPerson.get(desiredList.get(0));
        g.setText(1, 0, "Number of " + users);
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
            //  logger.info("desired " + numDesired + " people list " + peopleToNumPerForDesired.keySet() + " first " + numPeople);

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
                  if (numPeopleBox.getItemText(i).equals("" + people)) {
                    numPeopleBox.setSelectedIndex(i);
                    break;
                  }
                }
                break;
              }
            }

            int hours = (int) Math.ceil((float) minSelected / 60f);
            hoursBox.setHTML(" minutes or " + hours + " hours.");
            Map<Integer, Integer> peopleToNumPerForDesired = desiredToPeopleToItemsPerPerson.get(numDesired);
            setItemPerPerson(peopleToNumPerForDesired, people, g);
          }
        });

        g.setText(3, 0, items + " per " + user);
        Integer firstPerson = peopleList.get(0);
        setItemPerPerson(peopleToNumPer, firstPerson, g);

        g.setText(2, 0, "Time per " + user);
        setMinutesBox(desiredList.get(0), firstPerson, desiredToPeopleToMinutesPerPerson, minutesBox, hoursBox);
        g.setWidget(2, 1, minutesBox);
        g.setWidget(2, 2, hoursBox);
        return g;
      }

      private void setMinutesBox(Integer numDesired, int numPeople, Map<Integer, Map<Integer, Integer>> desiredToPeopleToMinutesPerPerson, ListBox minutesBox, HTML hoursBox) {
        Map<Integer, Integer> peopleToMinutesPer = desiredToPeopleToMinutesPerPerson.get(numDesired);
        setMinutesBox(numPeople, peopleToMinutesPer, minutesBox, hoursBox);
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
        // logger.info("looking for "+ firstPerson + " in " + peopleToMinutesPer + " found " + minutesPerPerson);

        List<Integer> mins = new ArrayList<>(values);
        Collections.sort(mins);
        int index = mins.indexOf(minutesPerPerson);
        //  logger.info("index "+ index + " for " + minutesPerPerson + " in " + mins);
        int hours = (int) Math.ceil((float) minutesPerPerson / 60f);
        logger.info("for " + minutesPerPerson + " minutes " + hours + " hours");
        hoursBox.setHTML(" minutes or " + hours + " hours.");
        minutesBox.setSelectedIndex(index);
      }
    });
  }

  private List<Integer> getBoxForSet(ListBox box, Collection<Integer> desiredSet) {
    box.clear();
    List<Integer> desiredList = getSortedList(desiredSet);
    for (Integer desired : desiredList) box.addItem("" + desired);
    return desiredList;
  }

  private List<Integer> getSortedList(Collection<Integer> desiredSet) {
    List<Integer> desiredList = new ArrayList<>(desiredSet);
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

        //vp.add(addGradeInfo(result));
        vp.add(new HTML("<h2>Session Info</h2>"));

        service.getSessions(new AsyncCallback<List<Session>>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(List<Session> sessions) {
            long totalTime = 0;
            long total = 0;
            long valid = sessions.size();
            Map<Long, Integer> rateToCount = new HashMap<>();
            for (Session s : sessions) {
              totalTime += s.duration;
              total += s.getNumAnswers();

              Integer count = rateToCount.get(s.getSecAverage());
              if (count == null) rateToCount.put(s.getSecAverage(), s.getNumAnswers());
              else rateToCount.put(s.getSecAverage(), count + s.getNumAnswers());
            }
            FlexTable flex = new FlexTable();
            int row = 0;
            flex.setText(row, 0, "Num sessions");
            flex.setText(row++, 1, "" + valid);

            long hoursSpent = totalTime / HOUR;
            double dhoursSpent = (double) hoursSpent;
            flex.setText(row, 0, "Time spent");
            flex.setText(row++, 1, "" + hoursSpent + " hours " + (totalTime - hoursSpent * HOUR) / MIN + " mins");

            flex.setText(row, 0, "Audio collected");
            flex.setText(row++, 1, roundToHundredth(totalHours) + " hours");

            flex.setText(row, 0, "# Bad audio recordings (zero length)");
            flex.setText(row++, 1, "" + badRecordings);

            if (dhoursSpent > 0) {
              flex.setText(row, 0, "Audio yield (collected/spent)");
              flex.setText(row++, 1, "" + Math.round((totalHours / dhoursSpent) * 100) + "%");
            }

            flex.setText(row, 0, "Total " + answers);
            flex.setText(row++, 1, "" + total);
            if (valid > 0) {
              flex.setText(row, 0, "Avg " +
                  answers +
                  "/session");
              flex.setText(row++, 1, "" + (total / valid));
              flex.setText(row, 0, "Avg time spent/session");
              flex.setText(row++, 1, "" + ((totalTime / valid) / MIN) + " mins");
            }
            if (total > 0) {
              long rateInMillis = totalTime / total;
              flex.setText(row, 0, "Avg time spent/" + item);
              flex.setText(row++, 1, "" + (rateInMillis / 1000) + " sec");
            }

            flex.setText(row, 0, "Avg audio collected/" + item);
            flex.setText(row++, 1, "" + roundToHundredth(avgSecs) + " sec");


            vp.add(flex);
            getRateChart(rateToCount, vp);

            if (null != null) ((DoIt) null).go();
          }
        });
      }
    });

  }

/*  private Panel addGradeInfo(Map<String, Number> result) {
    VerticalPanel vp = new VerticalPanel();
    HTML html = new HTML("<h2>Grade Stats</h2>");
    vp.add(html);

    for (int i = 0; i < 3; i++) {
      if (result.containsKey("totalGraded_" + i) && result.get("totalGraded_" + i).intValue() > 5) {
        HTML html2 = new HTML("<h3>Grade Round #" + (i + 1) +
          "</h2>");
        vp.add(html2);
        FlexTable flex = new FlexTable();

        final int graded = result.get("totalGraded_" + i).intValue();
        final int validGrades = result.get("validGraded_" + i).intValue();
        final float avgNumGraded = result.get("averageNumGraded_" + i).floatValue();
        final float avgGrade = result.get("avgGrade_" + i).floatValue();
        final int percentGraded = result.get("percentGraded_" + i).intValue();
        final int incorrect = result.get("incorrectGraded_" + i).intValue();
        final float avgIncorrect = result.get("averageNumIncorrect_" + i).floatValue();
        final int correct = result.get("correctGraded_" + i).intValue();
        final float avgCorrect = result.get("averageNumCorrect_" + i).floatValue();

        int row = 0;

        flex.setText(row, 0, "Total Graded");
        flex.setText(row++, 1, "" + graded + " (" + percentGraded + "% of " + answers +
          ")");

        flex.setText(row, 0, "Total Valid Graded (1-5)");
        flex.setText(row++, 1, "" + validGrades + " (" + getPercent(validGrades, graded) + ")");


        flex.setText(row, 0, "Average Number Graded");
        flex.setText(row++, 1, "" + roundToHundredth(avgNumGraded));

        flex.setText(row, 0, "Average Grade");
        flex.setText(row++, 1, "" +  roundToHundredth(avgGrade));

        flex.setText(row, 0, "Incorrect Grades (1-3)");
        flex.setText(row++, 1, "" + incorrect + " (" + getPercent(incorrect, validGrades) + " of valid)");

        flex.setText(row, 0, "Average Number Incorrect Grades");
        flex.setText(row++, 1, "" +  roundToHundredth(avgIncorrect));

        flex.setText(row, 0, "Correct Grades (4-5)");
        flex.setText(row++, 1, "" + correct + " (" + getPercent(correct, validGrades) + " of valid)");

        flex.setText(row, 0, "Average Number Correct Grades");
        flex.setText(row++, 1, "" +  roundToHundredth(avgCorrect));
        vp.add(flex);

        addGraphOfExperienceToIncorrectAndCorrect(result, vp, i);
      }
    }
    vp.setWidth("100%");
    return vp;
  }*/

/*  private void addGraphOfExperienceToIncorrectAndCorrect(Map<String, Number> result, VerticalPanel vp, int i) {
    List<String> incorrectKeys = new ArrayList<String>();
    List<String> correctKeys = new ArrayList<String>();
    for (String key : result.keySet()) {
      if (key.contains("at") && key.contains("_" + i)) {
        if (key.startsWith("incorrect")) {
          incorrectKeys.add(key);
        } else if (key.startsWith("correct")) {
          correctKeys.add(key);
        }
      }
    }
    Map<Integer,Integer> expToIncorrect = new HashMap<Integer, Integer>();
    Map<Integer,Integer> expToCorrect = new HashMap<Integer, Integer>();
    for (String key : incorrectKeys) {
      final int incorrectAtExp = result.get(key).intValue();
      expToIncorrect.put(Integer.parseInt(key.split("_at_")[1]), incorrectAtExp);
    }

    for (String key : correctKeys) {
      final int correctAtExp = result.get(key).intValue();
      expToCorrect.put(Integer.parseInt(key.split("_at_")[1]),correctAtExp);
    }

    vp.add(getExperienceToBoth(expToIncorrect, expToCorrect, "Incorrect"));
  }

  private String getPercent(int numer, int denom) {
    return (int)(100f*(float)numer/(float)denom) + "%";
  }*/

  private float roundToHundredth(double totalHours) {
    return ((float) ((Math.round(totalHours * 100)))) / 100f;
  }

/*  private ColumnChart getNumToHoursChart(Map<Integer, Float> rateToCount) {
    Options options = Options.create();
    options.setTitle("Hours until " +
        answers +
        "/" +
        "item" +
        " (given the current rate).  How long until completion?");

    DataTable data = DataTable.create();
    data.addVarchar(AbstractDataTable.ColumnType.NUMBER, "Num " +
        answers);
    data.addVarchar(AbstractDataTable.ColumnType.NUMBER, "Projected Hours");

    data.addRows(rateToCount.size());

    int r = 0;
    List<Integer> ages = getSortedList(rateToCount.keySet());
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, age);
      data.setValue(r++, 1, rateToCount.get(age));
    }

    return new ColumnChart(data, options);
  }*/

  /**
   * @param rateToCount
   * @param vp
   * @see #doSessionQuery(Panel)
   */
  private void getRateChart(Map<Long, Integer> rateToCount, Panel vp) {
    Options options = Options.create();
    options.setTitle("Number " + answers + " at rate");

    labelAxes(options, "Rate (seconds/" + item + ")", "Count at rate");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Seconds/" + answers);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, NUMBER_AT_RATE);

    data.addRows(rateToCount.size());

    int r = 0;
    List<Long> ages = new ArrayList<>(rateToCount.keySet());
    Collections.sort(ages);
    if (ages.size() > 120) ages = ages.subList(0, 120);
    logger.info("found " + ages.size() + " samples");
    for (Long age : ages) {
      Integer value = rateToCount.get(age);
      if (value < 120) {
        data.addRow();
        data.setValue(r, 0, age);
        data.setValue(r++, 1, value);
      }
    }

    vp.add(new ColumnChart(data, options));
  }

  private void labelAxes(Options options, String hAxisTitle, String vAxisTitle) {
    AxisOptions options1 = AxisOptions.create();
    options1.setTitle(hAxisTitle);

    options.setHAxisOptions(options1);
    options1.set("maxTextLines", 2d);
    AxisOptions options2 = AxisOptions.create();
    options2.setTitle(vAxisTitle);

    options.setVAxisOptions(options2);
  }


  private void labelAxes2(Options options, String hAxisTitle, String vAxisTitle) {
    AxisOptions options1 = AxisOptions.create();
    options1.setTitle(hAxisTitle);
    options.setHAxisOptions(options1);

    AxisOptions options2 = AxisOptions.create();
    options2.setTitle(vAxisTitle);
    options.setVAxisOptions(options2);
  }


  private void doResultLineQuery(final Panel vp) {
    service.getResultPerExercise(new AsyncCallback<Map<String, Map<String, Integer>>>() {
      public void onFailure(Throwable caught) {
      }

      /**
       *
       * @param result map or overall,male,female to counts
       */
      @Override
      public void onSuccess(Map<String, Map<String, Integer>> result) {
        Map<String, Integer> overall = result.get("overall");
        int total = 0;
        for (Integer c : overall.values()) total += c;
        float ratio = ((float) total) / ((float) overall.size());
        vp.add(new HTML("<b>Avg " +
            answers +
            "/" +
            item +
            " = " + roundToHundredth(ratio) + "</b>"));

        List<String> keys = getSortedKeys(overall);
        int size = keys.size();
        boolean showItemIDs = size > 500;
        String title = answers + " per " + item + (showItemIDs ? "index" : "");

        int chartSamples = Math.min(ITEM_CHART_ITEM_WIDTH, size);

        for (int i = 0; i < size; i += chartSamples) {
          Map<String, Map<String, Integer>> typeToList = new HashMap<>();
          int endIndex = Math.min(size, i + chartSamples);
          endIndex = Math.min(keys.size(), endIndex);
          List<String> sublist = keys.subList(i, endIndex);
          for (Map.Entry<String, Map<String, Integer>> pair : result.entrySet()) {
            Map<String, Integer> exidToCount = pair.getValue();
            Map<String, Integer> submap = new HashMap<>();
            for (String key : sublist) {
              Integer value = exidToCount.get(key);
              submap.put(key, value == null ? 0 : value);
            }
            typeToList.put(pair.getKey(), submap);
          }
          String title1 = title + " (" + i + "-" + (i + chartSamples) + ")";
          Widget lineChart = showItemIDs ?
              getLineChartBigSet(sublist, typeToList, title1, size < 300, i) :
              getLineChart(sublist, typeToList, title1, size < 300, i);

          vp.add(lineChart);
        }

        int maleTotal = 0;
        int femaleTotal = 0;

        for (Integer c : result.get("male").values()) {
          maleTotal += c;
        }
        for (Integer c : result.get("female").values()) {
          femaleTotal += c;
        }
        vp.add(getGenderChart(maleTotal, femaleTotal));
        if (null != null) ((DoIt) null).go();
      }
    });
  }

  private List<String> getSortedKeys(Map<String, Integer> overall) {
    List<String> keys = new ArrayList<>(overall.keySet());
    List<String> strings = keys.subList(0, Math.min(200, keys.size()));
    logger.info("keys " + strings);
    sortKeysIntelligently(keys);
    return keys;
  }

  private LineChart getLineChart(List<String> keys, Map<String, Map<String, Integer>> overallToExIDToCount, String title, boolean goBig, int offset) {
    Options options = Options.create();
    options.setTitle(title);
    labelAxes2(options, item +
            " index",
        "# " + answers);
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "ID");
    for (String key : overallToExIDToCount.keySet()) {
      //     logger.info("type " + key);
      data.addColumn(AbstractDataTable.ColumnType.NUMBER, key);
    }
    int size = keys.size();
    //  logger.info(" keys " + size + " table " + data);

    data.addRows(size);
    int colCount = 1;

    int r = 0;
    for (String exid : keys) {
      data.setValue(r++, 0, exid);
    }

    for (String key : overallToExIDToCount.keySet()) {
      //   logger.info("type " + key + " keys " + keys);
      Map<String, Integer> exidToCount = overallToExIDToCount.get(key);
      r = 0;
      for (String exid : keys) {
        Integer value = exidToCount.get(exid);
        data.setValue(r++, colCount, value == null ? 0 : value);
      }
      colCount++;
    }
    if (goBig) options.setHeight((int) (Window.getClientHeight() * 0.3f));

    LineChart lineChart = new LineChart(data, options);
    lineChart.setWidth("96%");
    return lineChart;
  }

  private LineChart getLineChartBigSet(List<String> keys, Map<String, Map<String, Integer>> overallToExIDToCount, String title, boolean goBig, int offset) {
    Options options = Options.create();
    options.setTitle(title);
    labelAxes2(options, item +
            " index",
        "# " + answers);
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Index");
    for (String key : overallToExIDToCount.keySet()) {
      //     logger.info("type " + key);
      data.addColumn(AbstractDataTable.ColumnType.NUMBER, key);
    }
    int size = keys.size();
    //  logger.info(" keys " + size + " table " + data);

    data.addRows(size);
    int colCount = 1;

    int r = 0;

    for (String exid : keys) {
      data.setValue(r, 0, r);
      r++;
    }

    for (String key : overallToExIDToCount.keySet()) {
      //   logger.info("type " + key + " keys " + keys);
      Map<String, Integer> exidToCount = overallToExIDToCount.get(key);
      r = 0;
      for (String exid : keys) {
        Integer value = exidToCount.get(exid);
        data.setValue(r++, colCount, value == null ? 0 : value);
      }
      colCount++;
    }
    if (goBig) options.setHeight((int) (Window.getClientHeight() * 0.3f));

    LineChart lineChart = new LineChart(data, options);
    lineChart.setWidth("96%");
    return lineChart;
  }

  private void doResultQuery(final Panel vp) {
    service.getResultCountToCount(new AsyncCallback<Map<Integer, Integer>>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(Map<Integer, Integer> userToCount) {
        int total = 0;
        for (int v : userToCount.values()) total += v;
        Integer unanswered = userToCount.get(0);
        float ratio = total > 0 ? ((float) unanswered) / ((float) total) : 0;
        int percent = (int) (ratio * 100f);

        vp.add(new HTML("<h2>Collection progress</h2>"));
        FlexTable flex = new FlexTable();
        flex.setHTML(0, 0, "<font color='red'>Number without a " +
            answer +
            "</font>");
        flex.setHTML(0, 1, unanswered + " or " + percent + "%");

        int numAnswered = total - unanswered;
        flex.setHTML(1, 0, "Number with a male or female " +
            answer);
        flex.setHTML(1, 1, numAnswered + " or " + (100 - percent) + "%");
        vp.add(flex);
        vp.add(getResultCountChart(userToCount));
        if (null != null) ((DoIt) null).go();
      }
    });
  }

  private void doMaleFemale(final Panel vp) {
    service.getMaleFemaleProgress(new AsyncCallback<Map<String, Float>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Map<String, Float> result) {
        vp.add(new HTML("<h2>Male/Female Reference Audio Coverage</h2>"));
        RecordingProgressTable flex = new RecordingProgressTable();
        vp.add(flex);
        flex.populate(result);
      }
    });
  }

  private void doGenderQuery(final Panel vp) {
    service.getResultCountsByGender(new AsyncCallback<Map<String, Map<Integer, Integer>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Map<String, Map<Integer, Integer>> result) {
        vp.add(getGenderCounts(result.get("maleCount"), result.get("femaleCount")));
      }
    });
  }
/*
  private void doGradeQuery(final Panel vp) {
    service.getGradeCountPerExercise(new AsyncCallback<Map<Integer, Map<String, Map<String, Integer>>>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("couldn't contact server.");
      }

      @Override
      public void onSuccess(Map<Integer, Map<String, Map<String, Integer>>> result) {
        for (int gradeRound = 0; gradeRound < MAX_GRADE_ROUNDS; gradeRound++) {
          Map<String, Map<String, Integer>> gradeSet = result.get(gradeRound);
          if (gradeSet != null) {
            Map<String, Integer> correct = gradeSet.get("correct");
            Map<String, Integer> incorrect = gradeSet.get("incorrect");
            if (correct != null && !correct.isEmpty() && incorrect != null && !incorrect.isEmpty()) {
              List<String> keys = getSortedKeys(correct);
              doChartSequence(gradeRound,keys,correct,incorrect,vp);
            }
          }
        }
      }
    });
  }*/

  /**
   * Handles a large exercise list by making a sequence of charts.
   *
   * @param round
   * @param keys
   * @param correct
   * @param incorrect
   * @param vp
   */
/*  private void doChartSequence(int round, List<String> keys,
                               Map<String, Integer> correct, Map<String, Integer> incorrect, final Panel vp) {
    int size = keys.size();
    //boolean showItemIDs = size > 500;
    int chartSamples = Math.min(ITEM_CHART_ITEM_WIDTH, size);

    for (int i = 0; i < size; i += chartSamples) {
      int endIndex = Math.min(size, i + chartSamples);
      endIndex = Math.min(keys.size(),endIndex);
      List<String> keySublist = keys.subList(i, endIndex);    // which items we show for this chart

      Map<String, Integer> csubmap = new HashMap<String, Integer>();
      for (String key : keySublist) {
        Integer value = correct.get(key);
        csubmap.put(key, value == null ? 0 : value);
      }

      Map<String, Integer> icsubmap = new HashMap<String, Integer>();
      for (String key : keySublist) {
        Integer value = incorrect.get(key);
        icsubmap.put(key, value == null ? 0 : value);
      }

      LineChart gradeCounts = getGradeCounts(round,
        i, endIndex,
        keySublist, csubmap, icsubmap);

      vp.add(gradeCounts);
    }
  }*/
  private void sortKeysIntelligently(List<String> keys) {
    String sample = keys.isEmpty() ? "" : keys.iterator().next();
    final boolean firstInt = Character.isDigit(sample.charAt(0));
    Collections.sort(keys, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        String[] split = o1.split("/");
        String r1 = split[0];
        String q1 = split[1];

        String[] split2 = o2.split("/");
        String r2 = split2[0];
        String q2 = split2[1];

        String suffix = "";
        if (r1.contains("-")) {
          String[] split1 = r1.split("-");
          String s = split1[0];
          suffix = split1[1];

          r1 = s;
        }

        String suffix2 = "";
        if (r2.contains("-")) {
          String[] split1 = r2.split("-");
          String s = split1[0];
          suffix2 = split1[1];
          r2 = s;
        }
        int c1 = safeCompare(r1, r2);
        if (c1 != 0) return c1;
        else if ((!suffix.isEmpty() || !suffix2.isEmpty())) {
          int i = safeCompare(suffix, suffix2);
          return i == 0 ? safeCompare(q1, q2) : i;
        }
        //     int comp = safeCompare(r1, r2);
        return c1 == 0 ? safeCompare(q1, q2) : c1;
      }
    });
  }

  private int safeCompare(String r1, String r2) {
    try {
      int i1 = Integer.parseInt(r1);
      int anotherInteger = Integer.parseInt(r2);
      return new Integer(i1).compareTo(anotherInteger);
    } catch (NumberFormatException e) {
      return r1.compareTo(r2);
    }
  }

  private void doResultByDayQuery(final Panel vp) {
    service.getResultByDay(new AsyncCallback<Map<String, Integer>>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(Map<String, Integer> userToCount) {
        vp.add(getResultByDayChart(userToCount));
      }
    });
  }

  private void doResultByHourOfDayQuery(final Panel vp) {
    service.getResultByHourOfDay(new AsyncCallback<Map<String, Integer>>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(Map<String, Integer> userToCount) {
        vp.add(getResultByHourOfDayChart(userToCount));
      }
    });
  }

  /**
   * Tamas didn't want an age chart.
   *
   * @param vp
   * @see #showResults
   */
  private void showUserInfo(final Panel vp) {
    service.getUserToResultCount(new AsyncCallback<Map<User, Integer>>() {
      public void onFailure(Throwable caught) {
      }

      public void onSuccess(Map<User, Integer> userToCount) {
        int total = 0;
        for (Integer count : userToCount.values()) total += count;
        vp.add(getPerUserChart(userToCount));
        //      vp.add(getPerUserLineChart(userToCount));

        vp.add(new HTML("Avg " + answers + " per " +
            user +
            " = " + total / userToCount.size()));
        vp.add(getUserChart(userToCount));

        // vp.add(getAgeChart(userToCount));
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
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, answers);

    data.addRows(2);
    data.setValue(0, 0, "Male");
    data.setValue(0, 1, m);
    data.setValue(1, 0, "Female");
    data.setValue(1, 1, f);

    return new ColumnChart(data, options);
  }

/*
  private ColumnChart getAgeChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle(answers +
        " by Age");

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, "Age");
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, answers);

    Map<Integer, Integer> ageToCount = new HashMap<Integer, Integer>();
    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      int age = (user.getAge() / 5) * 5;
      Integer c = ageToCount.get(age);

      if (c == null) ageToCount.put(age, count);
      else ageToCount.put(age, c + count);
    }

    int r = 0;
    List<Integer> ages = getSortedList(ageToCount.keySet());
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, (age - 5) + "-" + (age - 1));
      data.setValue(r++, 1, ageToCount.get(age));
    }

    return new ColumnChart(data, options);
  }
*/

  /**
   * Number of answers per speaker chart
   *
   * @param userToCount
   * @return
   * @see #showUserInfo(com.google.gwt.user.client.ui.Panel)
   */
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

    Map<Integer, Integer> ageToCount = new HashMap<>();
    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      int binned = count >= 100 ? (count / 100) * 100 : count >= 10 ? (count / 10) * 10 : count;
      Integer c = ageToCount.get(binned);
      if (c == null) ageToCount.put(binned, 1);
      else ageToCount.put(binned, c + 1);
    }

    int r = 0;
    List<Integer> ages = getSortedList(ageToCount.keySet());
    for (Integer age : ages) {
      data.addRow();
      data.setValue(r, 0, age >= 100 ? age + "-" + (age + 99) : (age >= 10 ? age + "-" + (age + 9) : "" + age));
      data.setValue(r++, 1, ageToCount.get(age));
    }

    return new ColumnChart(data, options);
  }

// --Commented out by Inspection START (4/5/16, 6:09 PM):
//  private ColumnChart getExperienceToBoth(Map<Integer, Integer> expToIncorrect, Map<Integer, Integer> expToCorrect, String incorrect) {
//    Options options = Options.create();
//    //String incorrect = "Incorrect";
//    options.setTitle("Number of " + " " + incorrect + "/Correct by User Experience");
//
//    AxisOptions options1 = AxisOptions.create();
//    options1.setMinValue(-1);
//    options1.setMaxValue(18);
//    options.setHAxisOptions(options1);
//    options1.setTitle("Months experience");
//
//    //options.setHAxisOptions(options1);
//    AxisOptions options2 = AxisOptions.create();
//    options2.setTitle("# " + incorrect + "/Correct responses");
//    options.setVAxisOptions(options2);
//
//    DataTable data = DataTable.create();
//    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Experience");
//    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Num " + "Correct");
//    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Num " + incorrect);
//
//    int r = 0;
//    List<Integer> countsForUsers = getSortedList(expToIncorrect.keySet());
//    logger.info("exp " + countsForUsers);
//    for (Integer count : countsForUsers) {
//      if (count > 36) logger.info("Skipping " + count + "->" + expToIncorrect.get(count));
//      else {
//        data.addRow();
//        data.setValue(r, 0, count);
//        data.setValue(r, 1, expToCorrect.get(count));
//        data.setValue(r++, 2, expToIncorrect.get(count));
//      }
//    }
//
//    ColumnChart columnChart = new ColumnChart(data, options);
//    columnChart.setWidth("90%");
//    return columnChart;
//  }
// --Commented out by Inspection STOP (4/5/16, 6:09 PM)

 /* private ColumnChart getExperienceToIncorrect(Map<Integer, Integer> expToIncorrect, String incorrect) {
    Options options = Options.create();
    //String incorrect = "Incorrect";
    options.setTitle("Number of " + " " + incorrect + " by User Experience");

    labelAxes(options, "Months experience", "# " + incorrect + " responses");
    DataTable data = DataTable.create();
    data.addVarchar(AbstractDataTable.ColumnType.NUMBER, "Experience");
    data.addVarchar(AbstractDataTable.ColumnType.NUMBER, "Num " + incorrect);

    int r = 0;
    List<Integer> countsForUsers = getSortedList(expToIncorrect.keySet());
    logger.info("exp " + countsForUsers);
    for (Integer count : countsForUsers) {
      if (count > 36) logger.info("Skipping " + count + "->" + expToIncorrect.get(count));
      else {
        data.addRow();
        data.setValue(r, 0, count);// count >= 100 ? count +"-"+(count+99): (count >= 10 ? count +"-" +(count+9) : ""+count));
        data.setValue(r++, 1, expToIncorrect.get(count));
      }
    }

    return new ColumnChart(data, options);
  }
*/
/*  private ColumnChart getPerUserLineChart(Map<User, Integer> userToCount) {
    Options options = Options.create();
    options.setTitle("Number of " +
      answers +
      " per " +
      user);

    labelAxes(options, "", "# " + user);
    DataTable data = DataTable.create();
    data.addVarchar(AbstractDataTable.ColumnType.NUMBER, "Num " + answers);
    data.addVarchar(AbstractDataTable.ColumnType.NUMBER, users +
      " with this many");

    Map<Integer, Integer> usersAtCount = new HashMap<Integer, Integer>();
    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      // int binned = count >= 100 ? (count / 100)*100 : count >= 10 ? (count / 10)*10 : count;
      Integer c = usersAtCount.get(count);
      usersAtCount.put(count, (c == null) ? 1 : c + 1);
    }

    long total = 0;
    for (Integer v : usersAtCount.values()) total += v;
    long avg = total/usersAtCount.size();
    logger.info("avg " + avg);
    AxisOptions options1 = AxisOptions.create();
    options1.setMaxValue(4*avg);
    options.setVAxisOptions(options1);

    int r = 0;
    List<Integer> countsForUsers = getSortedList(usersAtCount.keySet());
    logger.info("counts for users " + countsForUsers);
    for (Integer count : countsForUsers) {
      data.addRow();
      data.setValue(r, 0, count);// count >= 100 ? count +"-"+(count+99): (count >= 10 ? count +"-" +(count+9) : ""+count));
      data.setValue(r++, 1, usersAtCount.get(count));
    }


    return new ColumnChart(data, options);
  }*/

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

    Map<Integer, Integer> ageToCount = new HashMap<>();
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
      // if (age > 1) {
      data.addRow();
      //     data.setValue(r, 0, age);
      //   data.setValue(r, 0, age > 30 ?  "> 30" : "" + age);
  /*          age >= 100 ? ""+ age+"-"+(age+99) :
                //(age >= 30 ? (age - 10) + "-" + age :
                    (age >= 10 ? age +"-"+(age+4) :
                        "" + age));*/
      //age >= 100 ? ""+ age+"-"+(age+99) :
      data.setValue(r, 0,
          (age >= 30 ? age + "-" + (age + 9) :
              (age >= 10 ? age + "-" + (age + 4) :
                  "" + age)));
      data.setValue(r++, 1, ageToCount.get(age));
      // }
    }

    return new ColumnChart(data, options);
  }

  private Widget getResultByDayChart(Map<String, Integer> dayToCount) {
    String slot = "Day";
    return getColumnChart(dayToCount, slot, true);
  }

  private Widget getColumnChart(Map<String, Integer> dayToCount, String slot, boolean isDate) {
    Options options = getOptions(slot);
    DataTable data = getDataTable(slot, dayToCount, isDate);
    return new ColumnChart(data, options);
  }

/*  private Widget getAnnotatedResultByDayChart(Map<String, Integer> dayToCount) {
    String slot = "Day";
    //Options options = getOptions(slot);
    //DataTable data = getDataTable(slot, dayToCount);

    DataTable data = DataTable.create();
    data.addVarchar(AbstractDataTable.ColumnType.DATE, slot);
    data.addVarchar(AbstractDataTable.ColumnType.NUMBER, "Items");
    int r = 0;

    for (Map.Entry<String, Integer> pair : dayToCount.entrySet()) {
      String key = pair.getKey();

      String month = key.substring(0,2);
      String day = key.substring(3,5);
      String year = key.substring(6,10);

      logger.info("from " + key + " got '" + month+
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
    return getColumnChart(dayToCount, slot, false);
  }

  private ColumnChart getLangChart(Map<User, Integer> userToCount) {
    String slot = "Language";
    Options options = getOptions(slot);

    Map<String, Integer> langToCount = new HashMap<>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      String nativeLang1 = user.getNativeLang();
      if (nativeLang1 != null) {
        String nativeLang = nativeLang1.toLowerCase();
        Integer c = langToCount.get(nativeLang);
        if (count > 5) {
          if (c == null) langToCount.put(nativeLang, count);
          else langToCount.put(nativeLang, c + count);
        }
      }
    }

    DataTable data = getDataTable(slot, langToCount, false);

    return new ColumnChart(data, options);
  }

  private ColumnChart getDialectChart(Map<User, Integer> userToCount) {
    String slot = "Dialect";
    Options options = getOptions(slot);

    Map<String, Integer> dialectToCount = new HashMap<>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      String dialect = user.getDialect();
      if (dialect != null) {
        if (dialect.length() == 0) dialect = "Unknown";
        String slotToUse = dialect.toLowerCase();
        Integer c = dialectToCount.get(slotToUse);
        if (count > 5) {
          if (c == null) dialectToCount.put(slotToUse, count);
          else dialectToCount.put(slotToUse, c + count);
        }
      }
    }

    Map<String, Integer> dialectToCount2 = filterCount(dialectToCount, MIN_COUNT_FOR_DIALECT, 15);

    DataTable data = getDataTable(slot, dialectToCount2, false);

    return new ColumnChart(data, options);
  }

  private ColumnChart getBrowserChart(Map<User, Integer> userToCount) {
    BrowserCheck checker = new BrowserCheck();
    String slot = "Browser";
    Options options = getOptions(slot);

    Map<String, Integer> browserToCount = new HashMap<>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      String ipaddr = user.getIpaddr();
      boolean badIP = ipaddr == null || ipaddr.length() == 0;
//      if (badIP) {
//        /System.err.println("huh? no ipaddr for " + user);
      //     }
      String slotToUse = badIP ? "Unknown" : checker.getBrowser(ipaddr);
      Integer c = browserToCount.get(slotToUse);
      if (count > 0) {
        browserToCount.put(slotToUse, (c == null) ? count : c + count);
      }
    }
    Map<String, Integer> browserToCount2 = filterCount(browserToCount, MIN_COUNT_FOR_BROWSER, MIN_SIZE_TO_TRIGGER_FILTER);

    DataTable data = getDataTable(slot, browserToCount2, false);

    return new ColumnChart(data, options);
  }

  private Map<String, Integer> filterCount(Map<String, Integer> browserToCount, int minCount, int minSize) {
    if (browserToCount.size() < minSize) return browserToCount;
    Map<String, Integer> browserToCount2 = new HashMap<>();

    for (Map.Entry<String, Integer> pair : browserToCount.entrySet()) {
      if (pair.getValue() > minCount)
        browserToCount2.put(pair.getKey(), pair.getValue());
    }
    return browserToCount2;
  }

  /**
   * @param userToCount
   * @return
   * @see #showUserInfo
   */
  private ColumnChart getUserChart(Map<User, Integer> userToCount) {
    String slot = user;
    Options options = getOptions(slot);

    Map<String, Integer> nameToCount = new HashMap<>();
    Set<String> males = new HashSet<>();
    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer doneByUser = pair.getValue();
      User user = pair.getKey();
      String name = user.getUserID();
      if (name == null || name.length() == 0) {
        name = "" + user.getID();
      }
      name = name.trim();
      Integer count = nameToCount.get(name);
      if (count == null) nameToCount.put(name, doneByUser);
      else nameToCount.put(name, doneByUser + count);
      if (user.isMale()) males.add(name);
    }

    Map<Integer, List<String>> countToUser = new HashMap<>();
    for (Map.Entry<String, Integer> pair : nameToCount.entrySet()) {
      Integer doneByUser = pair.getValue();
      if (doneByUser > 5) {
        List<String> usersAtCount = countToUser.get(doneByUser);
        if (usersAtCount == null) countToUser.put(doneByUser, usersAtCount = new ArrayList<>());
        String user = pair.getKey();
        usersAtCount.add(user);
      }
    }
    List<Integer> counts = new ArrayList<>(countToUser.keySet());
    Collections.sort(counts, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return -1 * o1.compareTo(o2);
      }
    });

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Female " + answers);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "Male " + answers);
    int r = 0;

    //logger.info("counts " + counts);
    for (Integer c : counts) {
      List<String> users = countToUser.get(c);
      //  logger.info("\tcount " + c + " users " + users);
      for (String u : users) {
        data.addRow();
        data.setValue(r, 0, u);
        if (males.contains(u)) {
          data.setValue(r, 1, 0);
          data.setValue(r++, 2, c);
        } else {
          data.setValue(r, 1, c);
          data.setValue(r++, 2, 0);
        }
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
    labelAxes(options, "months experience", "# " + answers);
    Map<Integer, Integer> slotToCount = new HashMap<>();

    for (Map.Entry<User, Integer> pair : userToCount.entrySet()) {
      Integer count = pair.getValue();
      User user = pair.getKey();
      Integer slotToUse = user.getExperience();
      Integer c = slotToCount.get(slotToUse);
      if (count > 0) {
        if (c == null) slotToCount.put(slotToUse, count);
        else slotToCount.put(slotToUse, c + count);
      }
    }

    List<Integer> slotValues = getSortedList(slotToCount.keySet());

    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, answers);
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

    Set<Integer> slots = new HashSet<>(maleNumAnswerToCount.keySet());
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

// --Commented out by Inspection START (4/5/16, 6:09 PM):
//  private LineChart getGradeCounts(int round,
//                                   int start, int end,
//                                   List<String> keysToUse,
//                                   Map<String, Integer> correctToCount,
//                                   Map<String, Integer> incorrectToCount) {
//    Options options = Options.create();
//    options.setTitle("Number of " +
//        items +
//        " with this many " +
//        "grades" +
//        " correct/incorrect " + "Round #" + (round + 1) +
//        "(" + start + " - " + end +
//        ")"
//    );
//
//    labelAxes(options,
//        "",
//        "Number of " +
//            items);
//
//    DataTable data = DataTable.create();
//    data.addColumn(AbstractDataTable.ColumnType.STRING, answers);
//    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "" +
//        items +
//        " w/ # " +
//        answers +
//        ", Correct");
//    data.addColumn(AbstractDataTable.ColumnType.NUMBER, "" +
//        items +
//        " w/ # " +
//        answers +
//        ", Incorrect");
//
//    int r = 0;
//    for (String key : keysToUse) {
//      Integer correct = correctToCount.get(key);
//      Integer incorrect = incorrectToCount.get(key);
//
//      data.addRow();
//      data.setValue(r, 0, key);
//      data.setValue(r, 1, correct == null ? 0 : correct);
//      data.setValue(r, 2, incorrect == null ? 0 : incorrect);
//      r++;
//    }
//
//    //return new ColumnChart(data, options);
//    return new LineChart(data, options);
//  }
// --Commented out by Inspection STOP (4/5/16, 6:09 PM)


  private DataTable getDataTable(String slot, Map<String, Integer> langToCount, boolean isDate) {
    DataTable data = DataTable.create();
    data.addColumn(AbstractDataTable.ColumnType.STRING, slot);
    data.addColumn(AbstractDataTable.ColumnType.NUMBER, answers);
    int r = 0;

    List<String> slotValues = new ArrayList<>(langToCount.keySet());
    if (isDate) {
      Collections.sort(slotValues, new Comparator<String>() {
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
    } else {
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
