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

package mitll.langtest.client.contextPractice;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.scoring.SimplePostAudioRecordButton;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.ContextPractice;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class DialogWindow implements  DialogViewer {
  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final NumberFormat decF = NumberFormat.getFormat("#.####");
  private final ContextPractice cpw;

  /**
   * @see Navigation#Navigation
   * @param service
   * @param controller
   * @param cpw
   */
  public DialogWindow(LangTestDatabaseAsync service, ExerciseController controller, ContextPractice cpw) {
    this.controller = controller;
    this.service = service;
    this.cpw = cpw;
  }

  /**
   *
   * @param contentPanel
   */
  public void viewDialog(final HasWidgets contentPanel) {
    contentPanel.clear();
//    contentPanel.getElement().setId("contentPanel");
    final HorizontalPanel optionPanel = new HorizontalPanel();
    final FlowPanel forSents = new FlowPanel();
    final FlowPanel forGoodPhoneScores = new FlowPanel();
    forGoodPhoneScores.getElement().getStyle().setProperty("borderLeftStyle", "dashed");
    forGoodPhoneScores.getElement().getStyle().setProperty("borderLeftWidth", "2px");
    forGoodPhoneScores.getElement().getStyle().setProperty("paddingLeft", "18px");
    final FlowPanel forBadPhoneScores = new FlowPanel();
    forSents.getElement().getStyle().setProperty("marginBottom", "10px");

    final ListBox availableDialogs = new ListBox();
    final ListBox availableSpeakers = new ListBox();
    availableDialogs.getElement().getStyle().setProperty("fontSize", "150%");
    availableDialogs.getElement().getStyle().setProperty("width", "auto");
    availableDialogs.getElement().getStyle().setProperty("margin", "10px");
    availableSpeakers.getElement().getStyle().setProperty("fontSize", "150%");
    availableSpeakers.getElement().getStyle().setProperty("width", "auto");
    availableSpeakers.getElement().getStyle().setProperty("margin", "10px");
    final String CHOOSE_PART = "Choose a part to read";
    final String CHOOSE_DIALOG = "Choose a dialog to practice";
    final Map<String, String[]> dialogToParts = cpw.getDialogToPartsMap();
    final Map<Integer, String> dialogIndex = new HashMap<>();
    final RadioButton yesDia = new RadioButton("showDia", "Show your part");
    final RadioButton noDia = new RadioButton("showDia", "Hide your part");
    final RadioButton regular = new RadioButton("audioSpeed", "Regular speed");
    final RadioButton slow = new RadioButton("audioSpeed", "Slow speed");

    availableDialogs.addItem(CHOOSE_DIALOG);
    availableDialogs.setVisibleItemCount(1);

    Integer i = 1;
    ArrayList<String> sortedDialogs = new ArrayList<>(dialogToParts.keySet());
    java.util.Collections.sort(sortedDialogs);
    for (String dialog : sortedDialogs) {
      dialogIndex.put(i, dialog);
      availableDialogs.addItem(dialog);
      i += 1;
    }

    availableSpeakers.addItem(CHOOSE_PART);
    availableSpeakers.getElement().setAttribute("disabled", "disabled");
    availableSpeakers.setVisibleItemCount(1);

    availableDialogs.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        if (availableDialogs.getSelectedIndex() < 1) {
          availableSpeakers.clear();
          availableSpeakers.addItem(CHOOSE_PART);
          availableSpeakers.getElement().setAttribute("disabled", "disabled");
        } else {
          availableSpeakers.clear();
          availableSpeakers.addItem(CHOOSE_PART);
          for (String part : dialogToParts.get(availableDialogs.getValue(availableDialogs.getSelectedIndex()))) {
            availableSpeakers.addItem(part);
          }
          availableSpeakers.getElement().removeAttribute("disabled");
        }
        mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
      }
    });
    availableSpeakers.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
      }
    });
    optionPanel.add(availableDialogs);
    optionPanel.add(availableSpeakers);

    VerticalPanel showDiaPanel = new VerticalPanel();
    showDiaPanel.getElement().getStyle().setProperty("margin", "10px");
    yesDia.setValue(true);
    showDiaPanel.add(yesDia);
    showDiaPanel.add(noDia);
    yesDia.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
      }
    });
    noDia.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
      }
    });
    optionPanel.add(showDiaPanel);

    VerticalPanel audioSpeedPanel = new VerticalPanel();
    audioSpeedPanel.getElement().getStyle().setProperty("margin", "10px");
    regular.setValue(true);
    audioSpeedPanel.add(regular);
    audioSpeedPanel.add(slow);
    regular.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
      }
    });
    slow.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
      }
    });
    optionPanel.add(audioSpeedPanel);

    Button startDialog = new Button("Start Recording!", new ClickHandler() {
      public void onClick(ClickEvent event) {
        if ((availableSpeakers.getSelectedIndex() < 1) || (availableDialogs.getSelectedIndex() < 1)) {
          Window.alert("Select a dialog and part first!");
        } else
          mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, true);
      }
    });

    //make startDialog green or something? also change on radio select

    startDialog.getElement().getStyle().setProperty("fontSize", "150%");
    availableDialogs.getElement().getStyle().setProperty("margin", "10px");
    forGoodPhoneScores.getElement().getStyle().setProperty("margin", "10px");
    forBadPhoneScores.getElement().getStyle().setProperty("margin", "10px");
    HorizontalPanel sentsAndPhones = new HorizontalPanel();
    VerticalPanel diaAndStart = new VerticalPanel();
    diaAndStart.add(forSents);
    diaAndStart.add(startDialog);
    contentPanel.add(optionPanel);
    sentsAndPhones.add(diaAndStart);
    sentsAndPhones.add(forGoodPhoneScores);
    sentsAndPhones.add(forBadPhoneScores);
    contentPanel.add(sentsAndPhones);
    //contentPanel.add(startDialog);
  }


  private void mkNewDialog(ListBox availableSpeakers, ListBox availableDialogs, FlowPanel forSents, FlowPanel forGoodPhoneScores, FlowPanel forBadPhoneScores, Map<Integer, String> dialogIndex, RadioButton yesDia, RadioButton regular, boolean fromButton) {
    if ((availableSpeakers.getSelectedIndex() < 1) || (availableDialogs.getSelectedIndex() < 1)) {
      forSents.clear();
      forGoodPhoneScores.clear();
      forBadPhoneScores.clear();
      return;
    } else {
      forSents.clear();
      forGoodPhoneScores.clear();
      forBadPhoneScores.clear();
      Grid sentPanel = displayDialog(dialogIndex.get(availableDialogs.getSelectedIndex()), availableSpeakers.getValue(availableSpeakers.getSelectedIndex()), forSents, forGoodPhoneScores, forBadPhoneScores, yesDia.getValue(), regular.getValue());
      if (fromButton)
        setupPlayOrder(sentPanel, 0, sentPanel.getRowCount());
    }
  }

  private native void addPlayer() /*-{
      $wnd.basicMP3Player.init();
  }-*/;

  private native void resetPlayer() /*-{
      $wnd.soundManager.reset;
      $wnd.soundManager.init;
  }-*/;

  private SimplePostAudioRecordButton getRecordButton(String sent, final HTML resultHolder, final Button continueButton, final Image check, final Image x, final Image somethingIsHappening) {
    SimplePostAudioRecordButton s = new SimplePostAudioRecordButton(controller, service, sent) {

      @Override
      public void useResult(AudioAnswer result) {
        resultHolder.setHTML(decF.format(result.getScore()));
        continueButton.setEnabled(true);
        check.setVisible(true);
        x.setVisible(false);
        somethingIsHappening.setVisible(false);
        this.lastResult = result;
      }

      public void useInvalidResult(AudioAnswer result) {
        continueButton.setEnabled(false);
        check.setVisible(false);
        x.setVisible(true);
        somethingIsHappening.setVisible(false);
        this.lastResult = result;
      }

      @Override
      public void flip(boolean first) {
        //check.setVisible(first);
        //x.setVisible(!first);
      }

    };
    s.addMouseDownHandler(new MouseDownHandler() {
      @Override
      public void onMouseDown(MouseDownEvent e) {
        check.setVisible(false);
        x.setVisible(false);
        somethingIsHappening.setVisible(false);
      }
    });
    s.addMouseUpHandler(new MouseUpHandler() {
      @Override
      public void onMouseUp(MouseUpEvent e) {
        if (!somethingIsHappening.isVisible())
          somethingIsHappening.setVisible(true);
      }
    });
    s.setWidth("120px");
    return s;
  }


  private SimplePostAudioRecordButton getFinalRecordButton(String sent, final Button continueButton, final Image check, final Image x, final Image somethingIsHappening, final ArrayList<HTML> scoreElements, final HTML score, final HTML avg) {
    SimplePostAudioRecordButton s = new SimplePostAudioRecordButton(controller, service, sent) {

      @Override
      public void useResult(AudioAnswer result) {
        avg.setVisible(true);
        score.setHTML(decF.format(result.getScore()));
        avg.setHTML(innerScoring(scoreElements));
        avg.getElement().getStyle().setProperty("fontSize", "130%");
        avg.getElement().getStyle().setProperty("margin", "10px");
        continueButton.setEnabled(true);
        check.setVisible(true);
        x.setVisible(false);
        somethingIsHappening.setVisible(false);
        this.lastResult = result;
      }

      @Override
      public void flip(boolean first) {
        //avg.setVisible(!first);
      }

      @Override
      protected void useInvalidResult(AudioAnswer result) {
        continueButton.setEnabled(false);
        check.setVisible(false);
        x.setVisible(true);
        this.lastResult = result;
        somethingIsHappening.setVisible(false);
      }

    };
    s.addMouseUpHandler(new MouseUpHandler() {
      @Override
      public void onMouseUp(MouseUpEvent e) {
        if (!somethingIsHappening.isVisible())
          somethingIsHappening.setVisible(true);
      }
    });
    s.setWidth("120px");
    return s;
  }

  private Grid displayDialog(final String dialog, String part, FlowPanel cp, final FlowPanel goodPhonePanel, final FlowPanel badPhonePanel, boolean showPart, boolean regAudio) {

    Map<String, String> sentToAudioPath = regAudio ? cpw.getSentToAudioPath() : cpw.getSentToSlowAudioPath();
    Map<String, Map<Integer, String>> dialogToSentIndexToSpeaker = cpw.getDialogToSentIndexToSpeaker();
    final Map<String, Map<Integer, String>> dialogToSentIndexToSent = cpw.getDialogToSentIndexToSent();
    Map<String, Map<String, Integer>> dialogToSpeakerToLast = cpw.getDialogToSpeakerToLast();

    int sentIndex = 0;
    final Grid sentPanel = new Grid(dialogToSentIndexToSent.get(dialog).size(), 9);
    final ArrayList<HTML> scoreElements = new ArrayList<>();
    String otherPart = "";
    boolean youStart = false;
    int yourLast = dialogToSpeakerToLast.get(dialog).get(part);
    final FlowPanel rp = new FlowPanel();
    final HTML avg = new HTML("");
    avg.setVisible(false);
    rp.add(avg);
    rp.setVisible(false);
    final ArrayList<SimplePostAudioRecordButton> recoButtons = new ArrayList<>();
    final ArrayList<Integer> sentIndexes = new ArrayList<>();
    final ArrayList<Image> prevResponses = new ArrayList<>();

    while (dialogToSentIndexToSent.get(dialog).containsKey(sentIndex)) {
      String sentence = dialogToSentIndexToSent.get(dialog).get(sentIndex);
      HTML sent = new HTML(sentence);
      sent.getElement().getStyle().setProperty("color", "#B8B8B8");
      sent.getElement().getStyle().setProperty("margin", "5px 10px");
      sent.getElement().getStyle().setProperty("fontSize", "130%");
      if (part.equals(dialogToSentIndexToSpeaker.get(dialog).get(sentIndex))) {
        boolean nextIsSame = dialogToSentIndexToSpeaker.get(dialog).containsKey(sentIndex + 1) && (dialogToSentIndexToSpeaker.get(dialog).get(sentIndex).equals(dialogToSentIndexToSpeaker.get(dialog).get(sentIndex + 1)));
        sentIndexes.add(sentIndex);
        if (sentIndex == 0)
          youStart = true;
        if (!showPart)
          sent.setText("(Say your part)"); // be careful to not get the sentence for scoring from here!
        PlayAudioPanel play = new PlayAudioPanel(controller, "config/mandarinClassroom/bestAudio/" + sentToAudioPath.get(sentence));
        controller.register(play.getPlayButton(), "played reference audio for sentence " + sentence);
        play.setMinWidth(82);
        play.setPlayLabel("Play");
        final HTML score = new HTML("0.0");
        scoreElements.add(score);
        SimplePostAudioRecordButton recordButton = null;
        final Button continueButton = new Button("Continue");
        continueButton.setEnabled(false);
        final Image check = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "checkmark32.png"));
        final Image x = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "redx32.png"));
        final Image somethingIsHappening = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif"));
        if (nextIsSame) {
          prevResponses.add(check);
          prevResponses.add(x);
          prevResponses.add(somethingIsHappening);
        }
        if (sentIndex != yourLast) {
          recordButton = getRecordButton(dialogToSentIndexToSent.get(dialog).get(sentIndex).replaceAll("-", " "), score, continueButton, check, x, somethingIsHappening);
          continueButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
              for (Image i : prevResponses)
                i.setVisible(false);
              prevResponses.clear();
            }
          });
        } else {
          recordButton = getFinalRecordButton(dialogToSentIndexToSent.get(dialog).get(sentIndex).replaceAll("-", " "), continueButton, check, x, somethingIsHappening, scoreElements, score, avg);

          continueButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent e) {
              if (continueButton.isEnabled()) {
                displayResults(dialog, scoreElements, sentIndexes, dialogToSentIndexToSent, recoButtons, sentPanel, rp, goodPhonePanel, badPhonePanel);
                for (Image i : prevResponses)
                  i.setVisible(false);
                prevResponses.clear();
              }
            }
          });
        }

        recordButton.addMouseUpHandler(new MouseUpHandler() {
          @Override
          public void onMouseUp(MouseUpEvent e) {
            //continueButton.setEnabled(true);
          }
        });
        recordButton.addMouseDownHandler(new MouseDownHandler() {
          @Override
          public void onMouseDown(MouseDownEvent e) {
            check.setVisible(false);
            x.setVisible(false);
          }
        });

        controller.register(recordButton, "record button for sent: " + sentence + " in dialog " + dialog);

        if (sentIndex == yourLast) {
          controller.register(continueButton, "recording stopped with sent " + sentence + " in dialog " + dialog + " as speaker " + part + " with audio speed " + (regAudio ? "regular" : "slow") + " and part " + (showPart ? "visible" : "hidden"));
        } else {
          controller.register(continueButton, "continue button for sent: " + sentence + " in dialog " + dialog);
        }
        recoButtons.add((SimplePostAudioRecordButton) recordButton);

        sentPanel.setWidget(sentIndex, 2, recordButton);
        sent.getElement().getStyle().setProperty("fontWeight", "900");
        sentPanel.setWidget(sentIndex, 1, play);
        score.setVisible(false);
        sentPanel.setWidget(sentIndex, 3, continueButton);
        continueButton.setVisible(false);
        sentPanel.setWidget(sentIndex, 4, score);
        recordButton.setVisible(false);
        if (nextIsSame)
          continueButton.getElement().addClassName("nextIsSame");
        play.setVisible(false);
        sentPanel.setWidget(sentIndex, 5, check);
        check.setVisible(false);
        sentPanel.setWidget(sentIndex, 6, x);
        x.setVisible(false);
        sentPanel.setWidget(sentIndex, 7, somethingIsHappening);
        somethingIsHappening.setVisible(false);
      } else {
        PlayAudioPanel play = new PlayAudioPanel(controller, "config/mandarinClassroom/bestAudio/" + sentToAudioPath.get(sentence));
        sentPanel.setWidget(sentIndex, 1, play);
        sent.getElement().getStyle().setProperty("fontStyle", "italic");
        play.setVisible(false);
        otherPart = dialogToSentIndexToSpeaker.get(dialog).get(sentIndex);
      }
      sentPanel.setWidget(sentIndex, 0, sent);
      sentIndex += 1;
    }

    cp.add(getSetupText(part, otherPart, youStart));
    cp.add(sentPanel);

    cp.add(rp);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        addPlayer();
      }
    });
    return sentPanel;
  }

  private void displayResults(String dialog,
                              List<HTML> scoreElements,
                              List<Integer> sentIndexes,
                              Map<String, Map<Integer, String>> dialogToSentIndexToSent,
                              List<SimplePostAudioRecordButton> recoButtons, Grid sentPanel, FlowPanel rp, FlowPanel goodPhonePanel, FlowPanel badPhonePanel) {
    final Map<String, List<Float>> phonesToScores = new HashMap<>();
    Map<String, PlayAudioPanel> phoneToAudioExample = getPlayAudioWidget();
    rp.setVisible(true);
    for (HTML elt : scoreElements) {
      elt.setVisible(true);
    }
    for (int i = 0; i < sentIndexes.size(); i++) {
      sentPanel.setWidget(sentIndexes.get(i), 0, recoButtons.get(i).getSentColors(((dialogToSentIndexToSent.get(dialog).get(sentIndexes.get(i))))));
      sentPanel.setWidget(sentIndexes.get(i), 8, recoButtons.get(i).getScoreBar(Float.parseFloat(scoreElements.get(i).getText())));
    }
    for (SimplePostAudioRecordButton recordButton : recoButtons) {
      Map<String, Float> pts = recordButton.getPhoneScores();
      for (String phone : pts.keySet()) {
        if (!phonesToScores.containsKey(phone)) {
          List<Float> value = new ArrayList<>();
          phonesToScores.put(phone, value);
        }
        phonesToScores.get(phone).add(pts.get(phone));
      }
    }
    int numToShow = 5;
    List<String> preFilteredPhones = new ArrayList<>(phonesToScores.keySet());
    List<String> phones = new ArrayList<>();
    for (String phone : preFilteredPhones) {
      if (phonesToScores.get(phone).size() > 1)
        phones.add(phone);
    }
    if (phones.size() < 10) { //strategy 2, if there aren't enough phone repeats
      phones.clear();
      for (String phone : preFilteredPhones) {
        if (avg(phonesToScores.get(phone)) > 0.1)
          phones.add(phone);
      }
      System.out.println("Falling back to cropping worst phones");
    } else
      System.out.println("Filtering out phones with only one pronunication");
    numToShow = phones.size() < 10 ? phones.size() / 2 : 5; // the magic number 5!!!
    Collections.sort(phones, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        return (int) (1000 * (avg(phonesToScores.get(s2)) - avg(phonesToScores.get(s1))));
      }
    });
    Grid goodPhoneScores = new Grid(numToShow, 2);
    SimpleColumnChart chart = new SimpleColumnChart();
    for (int pi = 0; pi < numToShow; pi++) {
      String currPhone = phones.get(pi);
      PlayAudioPanel audiWid = phoneToAudioExample.get(currPhone);
      audiWid.setMinWidth(60);
      audiWid.getElement().getStyle().setWidth(60, Style.Unit.PX);
      goodPhoneScores.setWidget(pi, 0, audiWid);
      goodPhoneScores.setWidget(pi, 1, getScoreBar(phonesToScores.get(currPhone), chart));
    }
    Grid badPhoneScores = new Grid(numToShow, 2);
    for (int pi = phones.size() - 1; pi > phones.size() - numToShow - 1; pi--) {
      String currPhone = phones.get(pi);
      PlayAudioPanel audiWid = phoneToAudioExample.get(currPhone);
      audiWid.setMinWidth(60);
      audiWid.getElement().getStyle().setWidth(60, Style.Unit.PX);
      badPhoneScores.setWidget(numToShow - (phones.size() - pi), 0, audiWid);
      badPhoneScores.setWidget(numToShow - (phones.size() - pi), 1, getScoreBar(phonesToScores.get(currPhone), chart));
    }
    HTML goodPhonesTitle = new HTML("Some Sounds You Pronounced Well");
    goodPhonesTitle.getElement().getStyle().setProperty("fontSize", "130%");
    goodPhonesTitle.getElement().getStyle().setProperty("color", "#048500");
    goodPhonesTitle.getElement().getStyle().setProperty("marginBottom", "10px");
    goodPhonePanel.add(goodPhonesTitle);
    goodPhonePanel.add(goodPhoneScores);
    HTML badPhonesTitle = new HTML("Some Sounds You May Need To Improve");
    badPhonesTitle.getElement().getStyle().setProperty("fontSize", "130%");
    badPhonesTitle.getElement().getStyle().setProperty("color", "#AD0000");
    badPhonesTitle.getElement().getStyle().setProperty("marginBottom", "10px");
    badPhonePanel.add(badPhonesTitle);
    badPhonePanel.add(badPhoneScores);
  }

  private float avg(Collection<Float> scores) {
    float sum = 0;
    for (float f : scores) {
      sum += f;
    }
    return sum / scores.size();
  }

  private DivWidget getScoreBar(Collection<Float> scores, SimpleColumnChart chart) {
    float score = avg(scores);
    int iscore = (int) (100f * score);
    final int HEIGHT = 18;
    DivWidget bar = new DivWidget();
    TooltipHelper tooltipHelper = new TooltipHelper();
    bar.setWidth(iscore + "px");
    bar.setHeight(HEIGHT + "px");
    bar.getElement().getStyle().setBackgroundColor(SimpleColumnChart.getColor(score));
    bar.getElement().getStyle().setMarginTop(2, Style.Unit.PX);

    tooltipHelper.createAddTooltip(bar, "Score " + score + "%", Placement.BOTTOM);
    return bar;
  }

  private void setupPlayOrder(final Grid sentPanel, final int currIndex, final int stop) {
    List<Integer> reactingSents = new ArrayList<>();
    setupPlayOrder(sentPanel, currIndex, stop, reactingSents);
  }

  private void setupPlayOrder(final Grid sentPanel, final int currIndex, final int stop, final List<Integer> reactingSents) {
    if (currIndex >= stop)
      return;
    sentPanel.getWidget(currIndex, 0).getElement().getStyle().setProperty("color", "#000000");
    if (sentPanel.getWidget(currIndex, 2) != null) {
      sentPanel.getWidget(currIndex, 1).setVisible(true);
      sentPanel.getWidget(currIndex, 2).setVisible(true);
      final boolean sentsInARow = sentPanel.getWidget(currIndex, 3).getElement().hasClassName("nextIsSame");
      if (sentsInARow) {
        reactingSents.add(currIndex);
        setupPlayOrder(sentPanel, currIndex + 1, stop, reactingSents);
        return;
      }
      sentPanel.getWidget(currIndex, 3).setVisible(true);
      ((Button) sentPanel.getWidget(currIndex, 3)).addMouseUpHandler(new MouseUpHandler() {
        @Override
        public void onMouseUp(MouseUpEvent e) {
          if (((Button) sentPanel.getWidget(currIndex, 3)).isEnabled()) {
            reactingSents.add(currIndex);
            for (int i : reactingSents) {
              sentPanel.getWidget(i, 0).getElement().getStyle().setProperty("color", "#B8B8B8");
              sentPanel.getWidget(i, 2).setVisible(false);
              sentPanel.getWidget(i, 1).setVisible(false);
              sentPanel.getWidget(i, 3).setVisible(false);
              sentPanel.getWidget(i, 5).setVisible(false);
              sentPanel.getWidget(i, 6).setVisible(false);
            }
            if (currIndex + 1 != stop) {
              sentPanel.getWidget(currIndex + 1, 0).getElement().getStyle().setProperty("color", "#000000");
            }
            List<Integer> reactingSents1 = new ArrayList<>();
            setupPlayOrder(sentPanel, currIndex + 1, stop, reactingSents1);
          }
        }
      });
    } else {
      ((PlayAudioPanel) sentPanel.getWidget(currIndex, 1)).playCurrent();
      ((PlayAudioPanel) sentPanel.getWidget(currIndex, 1)).addListener(new AudioControl() {

        @Override
        public void reinitialize() {
          // TODO Auto-generated method stub

        }

        @Override
        public void songFirstLoaded(double durationEstimate) {
          // TODO Auto-generated method stub

        }

        @Override
        public void songLoaded(double duration) {
          // TODO Auto-generated method stub

        }

        @Override
        public void songFinished() {
          setupPlayOrder(sentPanel, currIndex + 1, stop, reactingSents);
          sentPanel.getWidget(currIndex, 0).getElement().getStyle().setProperty("color", "#B8B8B8");
        }

        @Override
        public void update(double position) {
          // TODO Auto-generated method stub

        }
      });
    }
  }

  private HTML getSetupText(String part, String otherPart, boolean youStart) {
    HTML setup = new HTML("You are " + part + " talking to " + otherPart + ". " + (youStart ? "You" : otherPart) + " begin" + (youStart ? "" : "s") + " the conversation.");
    setup.getElement().getStyle().setProperty("fontSize", "130%");
    setup.getElement().getStyle().setProperty("margin", "10px");
    return setup;
  }

  private String innerScoring(ArrayList<HTML> scoreElements) {
    double sum = 0.0;
    for (HTML sco : scoreElements) {
      //sco.setVisible(true);
      sum += Double.parseDouble(sco.getHTML());
    }
    return "Your average score was: " + decF.format(sum / scoreElements.size());
  }

  private HashMap<String, PlayAudioPanel> getPlayAudioWidget() {
    HashMap<String, PlayAudioPanel> pw = new HashMap<>();
    //at the moment, this list seems complete. wu3 is the only phone recorded by Haohsiang.
    pw.put("a1", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma1.mp3").setPlayLabel("a1"));
    pw.put("a2", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma2.mp3").setPlayLabel("a2"));
    pw.put("a3", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ha3o3.mp3").setPlayLabel("a3"));
    pw.put("a4", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ba4.mp3").setPlayLabel("a4"));
    pw.put("b", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu4.mp3").setPlayLabel("b"));
    pw.put("c", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/cai2.mp3").setPlayLabel("c"));
    pw.put("ch", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/che2.mp3").setPlayLabel("ch"));
    pw.put("d", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/dao2.mp3").setPlayLabel("d"));
    pw.put("e1", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke1.mp3").setPlayLabel("e1"));
    pw.put("e2", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/he2.mp3").setPlayLabel("e2"));
    pw.put("e3", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ye3.mp3").setPlayLabel("e3"));
    pw.put("e4", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke4.mp3").setPlayLabel("e4"));
    pw.put("f", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/fa3.mp3").setPlayLabel("f"));
    pw.put("g", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/go1.mp3").setPlayLabel("g"));
    pw.put("h", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/he2.mp3").setPlayLabel("h"));
    pw.put("i1", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shi1.mp3").setPlayLabel("i1"));
    pw.put("i2", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/yi2.mp3").setPlayLabel("i2"));
    pw.put("i3", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ni3.mp3").setPlayLabel("i3"));
    pw.put("i4", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shi4.mp3").setPlayLabel("i4"));
    pw.put("j", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ji1ng.mp3").setPlayLabel("j"));
    pw.put("k", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke4.mp3").setPlayLabel("k"));
    pw.put("l", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/lao3.mp3").setPlayLabel("l"));
    pw.put("m", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma2.mp3").setPlayLabel("m"));
    pw.put("n", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ni3.mp3").setPlayLabel("n"));
    pw.put("ng", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/a2ng.mp3").setPlayLabel("ng"));
    pw.put("o1", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zho1.mp3").setPlayLabel("o1"));
    pw.put("o2", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ro2.mp3").setPlayLabel("o2"));
    pw.put("o3", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wo3.mp3").setPlayLabel("o3"));
    pw.put("o4", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zuo4.mp3").setPlayLabel("o4"));
    pw.put("p", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/pi4a4n.mp3").setPlayLabel("p"));
    pw.put("q", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/quu4.mp3").setPlayLabel("q"));
    pw.put("r", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ro2.mp3").setPlayLabel("r"));
    pw.put("s", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/su4.mp3").setPlayLabel("s"));
    pw.put("sh", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shu1.mp3").setPlayLabel("sh"));
    pw.put("t", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/tu2.mp3").setPlayLabel("t"));
    pw.put("u1", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shu1.mp3").setPlayLabel("u1"));
    pw.put("u2", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu2.mp3").setPlayLabel("u2"));
    pw.put("u3", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wu3.mp3").setPlayLabel("u3"));
    pw.put("u4", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu4.mp3").setPlayLabel("u4"));
    pw.put("uu1", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("uu1"));
    pw.put("uu2", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/xuue2.mp3").setPlayLabel("uu2"));
    pw.put("uu3", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/nuu3.mp3").setPlayLabel("uu3"));
    pw.put("uu4", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("uu4"));
    pw.put("w", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wo3.mp3").setPlayLabel("w"));
    pw.put("x", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/xuue2.mp3").setPlayLabel("x"));
    pw.put("y", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/yi2.mp3").setPlayLabel("y"));
    pw.put("z", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zou3.mp3").setPlayLabel("z"));
    pw.put("zh", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zho1.mp3").setPlayLabel("zh"));
    pw.put("sil", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("sil"));
    pw.put("unk", new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("unk"));
    for (String k : pw.keySet()) {
      controller.register(pw.get(k).getPlayButton(), "playing example phone for " + k);
    }
    return pw;
  }
}
