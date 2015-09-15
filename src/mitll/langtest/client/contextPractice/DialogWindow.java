package mitll.langtest.client.contextPractice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.scoring.SimplePostAudioRecordButton;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAnswer;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class DialogWindow {
	
  private ExerciseController controller;
  private LangTestDatabaseAsync service;

  private final NumberFormat decF = NumberFormat.getFormat("#.####");	
	
  public DialogWindow(LangTestDatabaseAsync service, ExerciseController controller) {
	  this.controller = controller;
	  this.service    = service;
  }

  public void viewDialog(final Panel contentPanel) {
     contentPanel.clear();
     contentPanel.getElement().setId("contentPanel");
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
     final HashMap<String, String[]> dialogToParts = getDialogToPartsMap();
     final HashMap<Integer, String> dialogIndex = new HashMap<Integer, String>();
     final RadioButton yesDia = new RadioButton("showDia", "Show your part");
     final RadioButton noDia = new RadioButton("showDia", "Hide your part");
     final RadioButton regular = new RadioButton("audioSpeed", "Regular speed");
     final RadioButton slow = new RadioButton("audioSpeed", "Slow speed");
     
     availableDialogs.addItem(CHOOSE_DIALOG);
     availableDialogs.setVisibleItemCount(1);
     
     Integer i = 1;
     ArrayList<String> sortedDialogs = new ArrayList<String>(dialogToParts.keySet());
     java.util.Collections.sort(sortedDialogs);
     for( String dialog : sortedDialogs){
        dialogIndex.put(i, dialog);
        availableDialogs.addItem(dialog);
        i += 1;
     }
     
     availableSpeakers.addItem(CHOOSE_PART);
     availableSpeakers.getElement().setAttribute("disabled", "disabled");
     availableSpeakers.setVisibleItemCount(1);
     
     availableDialogs.addChangeHandler(new ChangeHandler() {
     public void onChange(ChangeEvent event){
     if(availableDialogs.getSelectedIndex() < 1){
        availableSpeakers.clear();
        availableSpeakers.addItem(CHOOSE_PART);
        availableSpeakers.getElement().setAttribute("disabled", "disabled");
     }
     else{
        availableSpeakers.clear();
        availableSpeakers.addItem(CHOOSE_PART);
        for(String part : dialogToParts.get(availableDialogs.getValue(availableDialogs.getSelectedIndex()))){
            availableSpeakers.addItem(part);
        }
        availableSpeakers.getElement().removeAttribute("disabled");
     }
     mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
     }
     });
     availableSpeakers.addChangeHandler(new ChangeHandler() {
        public void onChange(ChangeEvent event){
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
     yesDia.addClickHandler(new ClickHandler(){
        public void onClick(ClickEvent event){
            mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
        }
     });
     noDia.addClickHandler(new ClickHandler(){
        public void onClick(ClickEvent event){
            mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
        }
     });
     optionPanel.add(showDiaPanel);
     
     VerticalPanel audioSpeedPanel = new VerticalPanel();
     audioSpeedPanel.getElement().getStyle().setProperty("margin", "10px");
     regular.setValue(true);
     audioSpeedPanel.add(regular);
     audioSpeedPanel.add(slow);
     regular.addClickHandler(new ClickHandler(){
        public void onClick(ClickEvent event){
            mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
        }
     });
     slow.addClickHandler(new ClickHandler(){
        public void onClick(ClickEvent event){
            mkNewDialog(availableSpeakers, availableDialogs, forSents, forGoodPhoneScores, forBadPhoneScores, dialogIndex, yesDia, regular, false);
        }
     });
     optionPanel.add(audioSpeedPanel);
     
     Button startDialog = new Button("Start Recording!", new ClickHandler() {
    	 public void onClick(ClickEvent event) {
    		 if((availableSpeakers.getSelectedIndex() < 1) || (availableDialogs.getSelectedIndex() < 1)){
    			 Window.alert("Select a dialog and part first!");
    		 }
    		 else
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

  
  public void mkNewDialog(ListBox availableSpeakers, ListBox availableDialogs, FlowPanel forSents, FlowPanel forGoodPhoneScores, FlowPanel forBadPhoneScores, HashMap<Integer, String> dialogIndex, RadioButton yesDia, RadioButton regular, boolean fromButton){
	  if((availableSpeakers.getSelectedIndex() < 1) || (availableDialogs.getSelectedIndex() < 1)){
		  forSents.clear();
		  forGoodPhoneScores.clear();
		  forBadPhoneScores.clear();
		  return;
	  }
     else{
    	 forSents.clear();
    	 forGoodPhoneScores.clear();
    	 forBadPhoneScores.clear();
    	 Grid sentPanel = displayDialog(dialogIndex.get(availableDialogs.getSelectedIndex()), availableSpeakers.getValue(availableSpeakers.getSelectedIndex()), forSents, forGoodPhoneScores, forBadPhoneScores, yesDia.getValue(), regular.getValue());
    	 if(fromButton)
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
  
  private SimplePostAudioRecordButton getRecordButton(String sent, final HTML resultHolder, final Button continueButton, final Image check, final Image x, final Image somethingIsHappening){
	  SimplePostAudioRecordButton s = new SimplePostAudioRecordButton(controller, service, sent) {
		  		  
		  @Override
		  public void useResult(AudioAnswer result){
			  resultHolder.setHTML(decF.format(result.getScore()));
			  continueButton.setEnabled(true);
			  check.setVisible(true);
			  x.setVisible(false);
			  somethingIsHappening.setVisible(false);
			  this.lastResult = result;
		  }
		  
		  public void useInvalidResult(AudioAnswer result){
			  continueButton.setEnabled(false);
			  check.setVisible(false);
			  x.setVisible(true);
			  somethingIsHappening.setVisible(false);
			  this.lastResult = result;
		  }
		  
		  @Override
		  public void flip(boolean first){
			  //check.setVisible(first);
			  //x.setVisible(!first);
		  }
		  
	  };
	  s.addMouseDownHandler(new MouseDownHandler() {
		  @Override
		  public void onMouseDown(MouseDownEvent e){
			  check.setVisible(false);
			  x.setVisible(false);
			  somethingIsHappening.setVisible(false);
		  }
	  });
	  s.addMouseUpHandler(new MouseUpHandler() {
		  @Override
		  public void onMouseUp(MouseUpEvent e){
			  if(! somethingIsHappening.isVisible())
			     somethingIsHappening.setVisible(true);
		  }
	  });
	  s.setWidth("120px");
	  return s;
  }
  
  
  private SimplePostAudioRecordButton getFinalRecordButton(String sent, final Button continueButton, final Image check, final Image x, final Image somethingIsHappening, final ArrayList<HTML> scoreElements, final HTML score, final HTML avg){
	  SimplePostAudioRecordButton s = new SimplePostAudioRecordButton(controller, service, sent) {
		  
	      @Override
		  public void useResult(AudioAnswer result){
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
		  public void flip(boolean first){
			  //avg.setVisible(!first);
	      }
		  
		  @Override
		  protected void useInvalidResult(AudioAnswer result){
		     continueButton.setEnabled(false); 
		     check.setVisible(false);
		     x.setVisible(true);
		     this.lastResult = result;
		     somethingIsHappening.setVisible(false);
		  }
		  
      };
	  s.addMouseUpHandler(new MouseUpHandler() {
		  @Override
		  public void onMouseUp(MouseUpEvent e){
			  if(! somethingIsHappening.isVisible())
			     somethingIsHappening.setVisible(true);
		  }
	  });
	  s.setWidth("120px");
	  return s;
  }
  
  private Grid displayDialog(final String dialog, String part, FlowPanel cp, final FlowPanel goodPhonePanel, final FlowPanel badPhonePanel, boolean showPart, boolean regAudio){
	  
	  HashMap<String, String> sentToAudioPath = regAudio ? getSentToAudioPath() : getSentToSlowAudioPath();
	  HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSpeaker = getDialogToSentIndexToSpeaker();
	  final HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSent = getDialogToSentIndexToSent();
	  HashMap<String, HashMap<String, Integer>> dialogToSpeakerToLast = getDialogToSpeakerToLast();

	  int sentIndex = 0;
	  final Grid sentPanel = new Grid(dialogToSentIndexToSent.get(dialog).size(), 9);
	  final ArrayList<HTML> scoreElements = new ArrayList<HTML>();
      String otherPart = "";
      boolean youStart = false;
      int yourLast = dialogToSpeakerToLast.get(dialog).get(part);
	  final FlowPanel rp = new FlowPanel();
	  final HTML avg = new HTML("");
	  avg.setVisible(false);
	  rp.add(avg);
	  rp.setVisible(false);
	  final ArrayList<SimplePostAudioRecordButton> recoButtons = new ArrayList<SimplePostAudioRecordButton>();
	  final ArrayList<Integer> sentIndexes = new ArrayList<Integer>();
	  final ArrayList<Image> prevResponses = new ArrayList<Image>();
	  
	  while(dialogToSentIndexToSent.get(dialog).containsKey(sentIndex)){
		  String sentence = dialogToSentIndexToSent.get(dialog).get(sentIndex);
		  HTML sent = new HTML(sentence);
		  sent.getElement().getStyle().setProperty("color", "#B8B8B8");
		  sent.getElement().getStyle().setProperty("margin", "5px 10px");
		  sent.getElement().getStyle().setProperty("fontSize", "130%");
		  if(part.equals(dialogToSentIndexToSpeaker.get(dialog).get(sentIndex))){
			  boolean nextIsSame = dialogToSentIndexToSpeaker.get(dialog).containsKey(sentIndex+1) && (dialogToSentIndexToSpeaker.get(dialog).get(sentIndex).equals(dialogToSentIndexToSpeaker.get(dialog).get(sentIndex+1)));
			  sentIndexes.add(sentIndex);
			  if (sentIndex == 0)
				  youStart = true;
			  if(!showPart)
				  sent.setText("(Say your part)"); // be careful to not get the sentence for scoring from here!
			  PlayAudioPanel play = new PlayAudioPanel(controller, "config/mandarinClassroom/bestAudio/"+sentToAudioPath.get(sentence));
			  controller.register(play.getPlayButton(), "played reference audio for sentence "+sentence);
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
			  if(nextIsSame){
				  prevResponses.add(check);
				  prevResponses.add(x);
				  prevResponses.add(somethingIsHappening);
			  }
			  if(sentIndex != yourLast){
			     recordButton = getRecordButton(dialogToSentIndexToSent.get(dialog).get(sentIndex).replaceAll("-", " "), score, continueButton, check, x, somethingIsHappening);
			     continueButton.addClickHandler(new ClickHandler() {
			    	 @Override
			    	 public void onClick(ClickEvent e){
						 for(Image i : prevResponses)
					        i.setVisible(false);
			    		 prevResponses.clear();
			    	 }
			     });
			  }
			  else{
				 recordButton = getFinalRecordButton(dialogToSentIndexToSent.get(dialog).get(sentIndex).replaceAll("-", " "), continueButton, check, x, somethingIsHappening, scoreElements, score, avg);
				  
				  continueButton.addClickHandler(new ClickHandler() {
					  @Override
					  public void onClick(ClickEvent e){ 
						  if(continueButton.isEnabled()){
							  displayResults(dialog, scoreElements, sentIndexes, dialogToSentIndexToSent, recoButtons, sentPanel, rp, goodPhonePanel, badPhonePanel);
							  for(Image i : prevResponses)
								  i.setVisible(false);
							  prevResponses.clear();
						  }
					  }
				  });
			  }
			  
			  recordButton.addMouseUpHandler(new MouseUpHandler() {
				  @Override
				  public void onMouseUp(MouseUpEvent e){
					  //continueButton.setEnabled(true);
				  }
			  });
			  recordButton.addMouseDownHandler(new MouseDownHandler(){
				  @Override
				  public void onMouseDown(MouseDownEvent e){
					  check.setVisible(false);
					  x.setVisible(false);
				  }
			  });

			  controller.register(recordButton, "record button for sent: " + sentence + " in dialog " + dialog);

			  if(sentIndex == yourLast){
				  controller.register(continueButton, "recording stopped with sent " + sentence + " in dialog "+ dialog + " as speaker " + part + " with audio speed " + (regAudio ? "regular" : "slow") + " and part " + (showPart ? "visible" : "hidden"));
			  }
			  else{
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
			  if(nextIsSame)
			     continueButton.getElement().addClassName("nextIsSame");
			  play.setVisible(false);
			  sentPanel.setWidget(sentIndex, 5, check);
			  check.setVisible(false);
			  sentPanel.setWidget(sentIndex, 6, x);
			  x.setVisible(false);
			  sentPanel.setWidget(sentIndex, 7, somethingIsHappening);
			  somethingIsHappening.setVisible(false);
		  }
		  else{
			  PlayAudioPanel play = new PlayAudioPanel(controller, "config/mandarinClassroom/bestAudio/"+sentToAudioPath.get(sentence));
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
  
  private void displayResults(String dialog, ArrayList<HTML> scoreElements, ArrayList<Integer> sentIndexes, HashMap<String, HashMap<Integer, String>> dialogToSentIndexToSent, ArrayList<SimplePostAudioRecordButton> recoButtons, Grid sentPanel, FlowPanel rp, FlowPanel goodPhonePanel, FlowPanel badPhonePanel){
	     final HashMap<String, ArrayList<Float>> phonesToScores = new HashMap<String, ArrayList<Float>>();
	     HashMap<String, PlayAudioPanel> phoneToAudioExample = getPlayAudioWidget();
	     rp.setVisible(true);
	     for(HTML elt : scoreElements){
	    	 elt.setVisible(true);
	     }
	     for(int i = 0; i < sentIndexes.size(); i++){
	    	 sentPanel.setWidget(sentIndexes.get(i),  0, recoButtons.get(i).getSentColors(((dialogToSentIndexToSent.get(dialog).get(sentIndexes.get(i))))));
	    	 sentPanel.setWidget(sentIndexes.get(i), 8, recoButtons.get(i).getScoreBar(Float.parseFloat(scoreElements.get(i).getText())));
	     }
	     for(SimplePostAudioRecordButton recordButton: recoButtons){
	    	 Map<String, Float> pts = recordButton.getPhoneScores();
	    	 for(String phone : pts.keySet()){
	    		 if(! phonesToScores.containsKey(phone)){
	    			 phonesToScores.put(phone, new ArrayList<Float>());
	    		 }
	    		 phonesToScores.get(phone).add(pts.get(phone));
	    	 }
	     }
	     int numToShow = 5;
	     ArrayList<String> preFilteredPhones = new ArrayList<String>(phonesToScores.keySet());
	     ArrayList<String> phones = new ArrayList<String>();
	     for(String phone : preFilteredPhones){
	    	 if(phonesToScores.get(phone).size() > 1)
	    		 phones.add(phone);
	     }
	     if(phones.size() < 10){ //strategy 2, if there aren't enough phone repeats
	    	 phones.clear();
	    	 for(String phone : preFilteredPhones){
	    		 if(avg(phonesToScores.get(phone)) > 0.1)
	    			 phones.add(phone);
	    	 }
	    	 System.out.println("Falling back to cropping worst phones");
	     }
	     else
	    	 System.out.println("Filtering out phones with only one pronunication");
	     numToShow = phones.size() < 10 ? phones.size()/2 : 5; // the magic number 5!!!
	     Collections.sort(phones, new Comparator<String>(){
	    	 @Override
	    	 public int compare(String s1, String s2){
	    		 return (int) (1000*(avg(phonesToScores.get(s2)) - avg(phonesToScores.get(s1))));
	    	 }
	     });
	     Grid goodPhoneScores = new Grid(numToShow, 2); 
	     SimpleColumnChart chart = new SimpleColumnChart();
	     for(int pi = 0; pi < numToShow; pi++){
	    	 String currPhone = phones.get(pi);
	    	 PlayAudioPanel audiWid = phoneToAudioExample.get(currPhone);
	    	 audiWid.setMinWidth(60);
	    	 audiWid.getElement().getStyle().setWidth(60, Style.Unit.PX);
	    	 goodPhoneScores.setWidget(pi, 0, audiWid);
	    	 goodPhoneScores.setWidget(pi, 1, getScoreBar(phonesToScores.get(currPhone), chart));
	     }
	     Grid badPhoneScores = new Grid(numToShow, 2);
	     for(int pi = phones.size() -1; pi > phones.size()-numToShow-1; pi--){
	    	 String currPhone = phones.get(pi);
	    	 PlayAudioPanel audiWid = phoneToAudioExample.get(currPhone);
	    	 audiWid.setMinWidth(60);
	    	 audiWid.getElement().getStyle().setWidth(60, Style.Unit.PX);
	    	 badPhoneScores.setWidget(numToShow-(phones.size()-pi), 0, audiWid);
	    	 badPhoneScores.setWidget(numToShow-(phones.size()-pi), 1, getScoreBar(phonesToScores.get(currPhone), chart));
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
  
  private float avg(ArrayList<Float> scores){
	  float sum = 0;
	  for(float f: scores){
		  sum += f;
	  }
	  return sum/scores.size();
  }
  
  public DivWidget getScoreBar(ArrayList<Float> scores, SimpleColumnChart chart){
	  float score = avg(scores);
	  int iscore = (int) (100f * score);
	  final int HEIGHT = 18;
	  DivWidget bar = new DivWidget();
	  TooltipHelper tooltipHelper = new TooltipHelper();
	  bar.setWidth(iscore + "px");
	  bar.setHeight(HEIGHT + "px");
	  bar.getElement().getStyle().setBackgroundColor(chart.getColor(score));
	  bar.getElement().getStyle().setMarginTop(2, Style.Unit.PX);

	  tooltipHelper.createAddTooltip(bar, "Score " + score + "%", Placement.BOTTOM);
	  return bar;
  }
  
  private void setupPlayOrder(final Grid sentPanel, final int currIndex, final int stop){
	  setupPlayOrder(sentPanel, currIndex, stop, new ArrayList<Integer>());
  }
  
  private void setupPlayOrder(final Grid sentPanel, final int currIndex, final int stop, final ArrayList<Integer> reactingSents){
	  if(currIndex >= stop)
		  return;
	  sentPanel.getWidget(currIndex, 0).getElement().getStyle().setProperty("color", "#000000");
	  if(sentPanel.getWidget(currIndex, 2) != null){
		  sentPanel.getWidget(currIndex, 1).setVisible(true);
		  sentPanel.getWidget(currIndex,  2).setVisible(true);
		  final boolean sentsInARow = sentPanel.getWidget(currIndex, 3).getElement().hasClassName("nextIsSame");
		  if(sentsInARow){
			  reactingSents.add(currIndex);
		      setupPlayOrder(sentPanel, currIndex + 1, stop, reactingSents);
		      return;
		  }
		  sentPanel.getWidget(currIndex,  3).setVisible(true);
	  	  ((Button) sentPanel.getWidget(currIndex, 3)).addMouseUpHandler(new MouseUpHandler() {
			  @Override
			  public void onMouseUp(MouseUpEvent e){
				  if(((Button) sentPanel.getWidget(currIndex,  3)).isEnabled()){
					  reactingSents.add(currIndex);
					  for(int i : reactingSents){
						  sentPanel.getWidget(i, 0).getElement().getStyle().setProperty("color", "#B8B8B8");
						  sentPanel.getWidget(i, 2).setVisible(false);
						  sentPanel.getWidget(i, 1).setVisible(false);
						  sentPanel.getWidget(i, 3).setVisible(false);
						  sentPanel.getWidget(i, 5).setVisible(false);
						  sentPanel.getWidget(i, 6).setVisible(false);
					  }
					  if(currIndex+1 != stop){
					     sentPanel.getWidget(currIndex+1, 0).getElement().getStyle().setProperty("color", "#000000");
					  }
					  setupPlayOrder(sentPanel, currIndex + 1, stop, new ArrayList<Integer>());
				  }
			  }
	  	  });
	  }
	  else{
		  ((PlayAudioPanel) sentPanel.getWidget(currIndex, 1)).playCurrent();
		  ((PlayAudioPanel) sentPanel.getWidget(currIndex, 1)).addListener(new AudioControl(){

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
  
  private HTML getSetupText(String part, String otherPart, boolean youStart){
	  HTML setup = new HTML("You are "+part+" talking to "+otherPart+". "+(youStart ? "You" : otherPart) +" begin"+(youStart ? "" : "s")+" the conversation.");
	  setup.getElement().getStyle().setProperty("fontSize", "130%");
	  setup.getElement().getStyle().setProperty("margin", "10px");
	  return setup;
  }
  
  private String innerScoring(ArrayList<HTML> scoreElements){
	  double sum = 0.0;
	  for(HTML sco : scoreElements){
		//sco.setVisible(true);
		sum += Double.parseDouble(sco.getHTML());
	  }
	  return "Your average score was: "+decF.format(sum/scoreElements.size());
  }
  
  private HashMap<String, String[]> getDialogToPartsMap(){
	  HashMap<String, String[]> m = new HashMap<String, String[]>();
	  m.put("Unit 1: Part 1", new String[] {"Crane", "Wang"});
	  m.put("Unit 1: Part 2", new String[] {"Smith", "Zhao"});
	  m.put("Unit 1: Part 3", new String[] {"Kao", "He"});
	  m.put("Unit 1: Part 4", new String[] {"Mrs. Li", "Mrs. Smith"});
	  m.put("Unit 2: Part 1", new String[] {"Taiwanese Student", "Parsons"});
	  m.put("Unit 2: Part 2", new String[] {"First Chinese", "Second Chinese", "American"});
	  m.put("Unit 2: Part 3", new String[] {"American", "Chinese"});
	  m.put("Unit 2: Part 4", new String[] {"Rogers", "Taiwanese Guest", "Holbrooke"});
	  m.put("Unit 3: Part 1", new String[] {"Chinese", "American"});
	  m.put("Unit 3: Part 2", new String[] {"Teacher", "Thompson"});
	  m.put("Unit 3: Part 3", new String[] {"Little", "Salesman"});
	  m.put("Unit 3: Part 4", new String[] {"Haynes", "Ticket Agent"});
	  m.put("Unit 4: Part 1", new String[] {"American", "Language Lab Attendant"});
	  m.put("Unit 4: Part 2", new String[] {"Nurse", "Johns"});
	  m.put("Unit 4: Part 3", new String[] {"Ross", "Bellhop"});
	  m.put("Unit 4: Part 4", new String[] {"Miller", "Sun"});
     m.put("Unit 5: Part 1", new String[] {"Chinese", "American"});
     m.put("Unit 5: Part 2", new String[] {"American", "Chinese"});
     m.put("Unit 5: Part 3", new String[] {"Norris", "Cashier", "Li"});
     m.put("Unit 5: Part 4", new String[] {"Guo", "Walters"});
	  return m;
  }
  

  private HashMap<String, String> getSentToAudioPath() {
	  HashMap<String, String> m = new HashMap<String, String>();
	  m.put("Kē Léi'ēn, nǐ hăo!", "/4/regular_1403800547484_by_8.wav");
	  m.put("Nǐ dào năr qù a?", "/13/regular_1403801120710_by_8.wav");
	  m.put("Wŏ huí sùshè.", "/24/regular_1403800638873_by_8.wav");
	  m.put("Wáng Jīngshēng, nǐ hăo!", "/7/regular_1403800587874_by_8.wav");
	  m.put("Wŏ qù túshūguăn. Nĭ ne?", "/20/regular_1403800718502_by_8.wav");
	  
	  m.put("Zhào Guócái, nĭ hăo a!", "/40/regular_1403793765777_by_8.wav");
	  m.put("Hái xíng. Nĭ àirén, háizi dōu hăo ma?", "/57/regular_1403793281208_by_8.wav"); //hi
	  m.put("Wŏ yŏu yìdiănr shìr, xiān zŏule. Zàijiàn!", "/71/regular_1403792961229_by_8.wav");
	  m.put("Nĭ hăo! Hăo jiŭ bú jiànle.", "/44/regular_1403792777589_by_8.wav");
	  m.put("Zěmmeyàng a?", "/45/regular_1403792803630_by_8.wav");
	  m.put("Tāmen dōu hěn hăo, xièxie.", "/63/regular_1403793540903_by_8.wav");
	  m.put("Zàijiàn.", "/72/regular_1403792398921_by_8.wav");
	  
	  m.put("Èi, Lăo Hé!", "/90/regular_1403794727635_by_8.wav");
	  m.put("Xiăo Gāo!", "/93/regular_1403794869759_by_8.wav");
	  m.put("Zuìjìn zěmmeyàng a?", "/96/regular_1403794766781_by_8.wav");
	  m.put("Hái kéyi. Nĭ ne?", "/99/regular_1403795159594_by_8.wav");
	  m.put("Hái shi lăo yàngzi. Nĭ gōngzuò máng bu máng?", "/109/regular_1403795348397_by_8.wav");
	  m.put("Bú tài máng. Nĭ xuéxí zěmmeyàng?", "/115/regular_1403795085285_by_8.wav");
	  m.put("Tĭng jĭnzhāngde.", "/119/regular_1403795511433_by_8.wav");
	  
	  m.put("Xiè Tàitai, huānyíng, huānyíng! Qĭng jìn, qĭng jìn.", "/145/regular_1403797935594_by_8.wav");
	  m.put("Xièxie.", "/146/regular_1403798184425_by_8.wav");
	  m.put("Qĭng zuò, qĭng zuò.", "149/regular_1403798074844_by_8.wav");
	  m.put("Xièxie.", "/146/regular_1403798184425_by_8.wav");
	  m.put("Lĭ Tàitai, wŏ yŏu yìdiăn shì, děi zŏule.", "/154/regular_1403797865456_by_8.wav");
	  m.put("Lĭ Tàitai, xièxie nín le.", "/158/regular_1403797900178_by_8.wav");
	  m.put("Bú kèqi. Màn zŏu a!", "/161/regular_1403798275036_by_8.wav");
	  m.put("Zàijiàn, zàijiàn!", "/162/regular_1403797559758_by_8.wav");
	  
	  m.put("Qĭng wèn, nĭ shì něiguó rén?", "/188/regular_1409322875786_by_8.wav");
	  m.put("Wŏ shi Měiguo rén.", "/191/regular_1409322897089_by_8.wav");
	  m.put("Nĭ jiào shémme míngzi?", "/196/regular_1409323764263_by_8.wav");
	  m.put("Wŏ jiào Bái Jiéruì.", "/199/regular_1409323274311_by_8.wav");
	  m.put("Nĭmen dōu shi Měiguo rén ma?", "/200/regular_1409321590091_by_8.wav");
	  m.put("Wŏmen bù dōu shi Měiguo rén.", "/202/regular_1409323704609_by_8.wav");
	  m.put("Zhèiwèi tóngxué yě shi Měiguo rén, kěshi nèiwèi tóngxué shi Jiā'nádà rén.", "/215/regular_1409323661836_by_8.wav");
	  
	  m.put("Qĭng jìn!", "/242/regular_1409324867082_by_8.wav");
	  m.put("Èi, Xiăo Mă. Ò.", "/243/missing2_2.wav");
	  m.put("Ò, Xiăo Chén, wŏ gĕi nĭ jièshao yixiar.", "/261/regular_1409325498732_by_8.wav");
	  m.put("Zhè shi wŏde xīn tóngwū, tā jiào Wáng Àihuá.", "/262/regular_1409325719045_by_8.wav");
	  m.put("Wáng Àihuá, zhè shi wŏde lăo tóngxué, Xiăo Chén.", "/263/regular_1409325343747_by_8.wav");
	  m.put("Ò, huānyíng nĭ dào Zhōngguo lái!", "/268/regular_1409325523740_by_8.wav");
	  m.put("Hĕn gāoxìng rènshi nĭ, Chén Xiáojie!", "/272/regular_1409325037171_by_8.wav");
	  m.put("Ò, bié zhèmme chēnghu wŏ.", "/280/regular_1409325476239_by_8.wav");
	  m.put("Hái shi jiào wŏ Xiăo Chén hăole.", "/281/regular_1409325318317_by_8.wav");
	  m.put("Xíng. Nà nĭ yě jiào wŏ Xiăo Wáng hăole.", "/284/regular_1409325429191_by_8.wav");
	  m.put("Hăo.", "/285/regular_1409324474164_by_8.wav");
	  
	  m.put("Nín guìxìng?", "/296/regular_1409328095788_by_8.wav");
	  m.put("Wŏ xìng Gāo. Nín guìxìng?", "/299/regular_1409327839500_by_8.wav");
	  m.put("Wŏ xìng Wú, Wú Sùshān.", "/310/regular_1409327869935_by_8.wav");
	  m.put("Gāo Xiānsheng, nín zài nĕige dānwèi gōngzuò?", "/311/regular_1409327753895_by_8.wav");
	  m.put("Wŏ zài Wàijiāobù gōngzuò. Nín ne?", "/314/regular_1409327615970_by_8.wav");
	  m.put("Wŏ zài Měiguo Dàshĭguăn gōngzuò.", "/317/regular_1409327653722_by_8.wav");
	  m.put("Nèiwèi shi nínde xiānsheng ba?", "/323/regular_1409327593572_by_8.wav");
	  m.put("Bú shì, bú shì! Tā shì wŏde tóngshì.", "/328/regular_1409327910091_by_8.wav");
	  m.put("Ò, Wú nǚshì, duìbuqĭ.", "/332/regular_1409327942560_by_8.wav");
	  m.put("Wŏ yŏu yìdiănshìr, xiān zŏule. Zàijiàn!", "/333/regular_1409327507146_by_8.wav");
	  m.put("Zàijiàn!", "/334/regular_1409327365810_by_8.wav");
	  
	  m.put("Nĭ hăo! Wŏ jiào Luó Jiésī.", "/355/regular_1409329603821_by_8.wav");
	  m.put("Qĭng duō zhĭ jiào.", "/357/regular_1409330000487_by_8.wav");
	  m.put("Wŏ xìng Shī. Duìbuqĭ, wŏ méi dài míngpiàn.", "/368/regular_1409329841202_by_8.wav");
	  m.put("Wŏ zài Zhōng-Měi Màoyì Gōngsī gōngzuò.", "/369/regular_1409329682905_by_8.wav");
	  m.put("Zŏngjīnglĭ, zhè shì Zhōng-Měi Màoyì Gōngsī de Shī Xiáojie.", "/374/regular_1409329793024_by_8.wav");
	  m.put("À, huānyíng, huānyíng! Wŏ xìng Hóu.", "/378/regular_1409329975201_by_8.wav");
	  m.put("Xièxie. Zŏngjīnglĭ yě shi Yīngguo rén ba?", "/382/regular_1409330116515_by_8.wav");
	  m.put("Bù, wŏ gēn Luó Xiáojie dōu bú shi Yīngguo rén.", "/387/regular_1409329916452_by_8.wav");
	  m.put("Wŏmen shi Měiguo rén.", "/388/regular_1409330281249_by_8.wav");
	  m.put("Ò, duìbuqĭ, wŏ găocuòle.", "/395/regular_1409329951250_by_8.wav");
	  m.put("Méi guānxi.", "/397/regular_1409330196368_by_8.wav");
	  
	  m.put("Nĭmen bānshang yŏu jĭwèi tóngxué?", "/415/regular_1411181687843_by_6.wav");
	  m.put("Yŏu shíwèi.", "/418/regular_1411182172767_by_6.wav");
	  m.put("Dōu shi Mĕiguo rén ma?", "/419/regular_1411181176755_by_6.wav");
	  m.put("Bù dōu shi Mĕiguó rén. Yŏu qíge Mĕiguo rén, liăngge Déguo rén gēn yíge Făguo rén.", "/432/regular_1411182280591_by_6.wav");
	  m.put("Jĭge nánshēng, jĭge nǚshēng?", "/436/regular_1411181675630_by_6.wav");
	  m.put("Yíbànr yíbànr. Wŭge nánde, wŭge nǚde.", "/446/regular_1411181498097_by_6.wav");
	  m.put("Nà, nĭmen yŏu jĭwèi lăoshī ne?", "/448/regular_1411182074862_by_6.wav");
	  m.put("Yígòng yŏu sānwèi. Liăngwèi shi nǚlăoshī, yíwèi shi nánlăoshī.", "/459/regular_1411182204835_by_6.wav");
	  
	  m.put("Zhè shi nĭ fùqin ma? Tā duō dà niánji le?", "/478/regular_1411225958788_by_6.wav");
	  m.put("Wŏ xiángxiang kàn. Tā jīnnián wŭshisānsuì le--bù, wŭshisìsuì le.", "/488/regular_1411226099431_by_6.wav");
	  m.put("Ò. Nà, zhèiwèi shi nĭ mŭqin ba?", "/490/regular_1411226382106_by_6.wav");
	  m.put("Duì, tā jīnnián sìshibāsuì le.", "/493/regular_1411226423539_by_6.wav");
	  m.put("Zhè shi nĭ mèimei, duì bu dui? Tā hĕn kĕ'ài! Tā jĭsuì le?", "/502/regular_1411226674835_by_6.wav");
	  m.put("Tā bāsuì. Xiàge yuè jiù jiŭsuì le.", "/510/regular_1411226507164_by_6.wav");
	  
	  m.put("Qĭng wèn, zhèige duōshăo qián?", "/529/regular_1411227214576_by_6.wav");
	  m.put("Yìbăi jiŭshibākuài.", "/534/regular_1411227584635_by_6.wav");
	  m.put("Yò, tài guìle!", "/538/regular_1411227290562_by_6.wav");
	  m.put("Zhèige bēizi duōshăo qián?", "/541/regular_1411227305048_by_6.wav");
	  m.put("Nèige zhĭ yào sānkuài wŭ.", "/547/regular_1411227850021_by_6.wav");
	  m.put("Wŏ kànkan, xíng bu xíng?", "/551/regular_1411228091152_by_6.wav");
	  m.put("Xíng, nín kàn ba.", "/554/regular_1411227776540_by_6.wav");
	  m.put("Hăo, wŏ măi liăngge.", "/557/regular_1411227551924_by_6.wav");
	  
	  m.put("Qĭng wèn, xiàyítàng dào Tiānjīnde huŏchē jĭdiăn kāi?", "/597/regular_1411228709840_by_6.wav");
	  m.put("Jiŭdiăn èrshí. Kĕshi xiànzài yĭjīng jiŭdiăn yíkè le, kŏngpà nín láibujíle.", "/610/regular_1411229177452_by_6.wav");
	  m.put("Nèmme, zài xiàyítàng ne?", "/614/regular_1411229636869_by_6.wav");
	  m.put("Wŏ kànkàn. Zài xiàyítàng shi shídiăn bàn.", "/616/regular_1411229078926_by_6.wav");
	  m.put("Hăo. Nà, wŏ jiù zuò shídiăn bànde.", "/620/regular_1411229292886_by_6.wav");
	  m.put("Duōshăo qián?", "/621/regular_1411228846639_by_6.wav");
	  m.put("Shíyīkuài wŭ.", "/622/regular_1411228674625_by_6.wav");
	  m.put("Dào Tiānjīn yào duō cháng shíjiān?", "/627/regular_1411228826868_by_6.wav");
	  m.put("Chàbuduō yào liăngge bàn zhōngtóu.", "/631/regular_1411228902183_by_6.wav");
	  m.put("Hăo, xièxie nín.", "/632/regular_1411229312766_by_6.wav");
	  
	  m.put("Qĭng wèn, yŭyán shíyànshì mĕitiān jĭdiăn zhōng kāimén, jĭdiăn zhōng guānmén?", "/662/regular_1411262880448_by_6.wav");
	  m.put("Zăoshang bādiăn kāimén, wănshang jiŭdiăn bàn guānmén.", "/667/regular_1411263236792_by_6.wav");
	  m.put("Xīngqīliù kāi bu kāi?", "/670/regular_1411263212800_by_6.wav");
	  m.put("Xīngqīliù kāi bàntiān. Shàngwŭ kāi, xiàwŭ bù kāi.", "/4-1.wav");
	  m.put("Xīngqītiān ne?", "/679/regular_1411262609818_by_6.wav");
	  m.put("Xīngqītiān xiūxi.", "/681/regular_1411263300998_by_6.wav");
	  m.put("Xièxie nĭ.", "/682/regular_1411263813283_by_6.wav");
	  m.put("Náli.", "/684/regular_1411263799454_by_6.wav");
	  
	  m.put("Nǐ jiào shémme míngzi?", "/721/regular_1411265548808_by_6.wav");
	  m.put("Wŏ jiào Zhāng Wényīng.", "/724/regular_1411265081710_by_6.wav");
	  m.put("Zhāng shi gōng cháng Zhāng, wén shi wénhuàde wén, yīng shi Yīngguode yīng.", "/729/regular_1411265406432_by_6.wav");
	  m.put("Nĭ shi nĕinián chūshēngde?", "/734/regular_1411265633760_by_6.wav");
	  m.put("Yī-jiŭ-bā-líng-nián, jiù shi Mínguó liùshijiŭnián.", "/740/regular_1411264916882_by_6.wav");
	  m.put("Jĭyuè jĭhào?", "/744/regular_1411265584705_by_6.wav");
	  m.put("Sìyuè shísānhào.", "/747/regular_1411264461878_by_6.wav");
	  m.put("Nĭde dìzhĭ shi...", "/749/regular_1411265691435_by_6.wav");
	  m.put("Hépíng Dōng Lù yīduàn, èrshiqīxiàng, sānnòng, yībăi wŭshisìhào, bālóu.", "/764/regular_1411264851371_by_6.wav");
	  m.put("Hăo, qĭng dĕng yíxià.", "/767/regular_1411264409681_by_6.wav");
	  
	  m.put("Nĭ hăo!", "/788/regular_1411268347706_by_6.wav");
	  m.put("Nĭ hăo! Nĭ shi Mĕiguo rén ma?", "/789/regular_1411268363857_by_6.wav");
	  m.put("Duì, wŏ shi Mĕiguo rén.", "/790/regular_1411269067335_by_6.wav");
	  m.put("Zhè shi nĭ dìyīcì dào Zhōngguo lái ma?", "/796/regular_1411268475838_by_6.wav");
	  m.put("Bù, zhè shi dì'èrcì. Wŏ qùnián láiguo yícì.", "/802/regular_1411268599745_by_6.wav");
	  m.put("M, nĭ zhèicì yào zhù duō jiŭ?", "/810/regular_1411268921781_by_6.wav");
	  m.put("Dàyuē bàn'ge yuè. Wŏ shí'èrhào huíguó.", "/816/regular_1411266211535_by_6.wav");
	  m.put("Nĭ zhù nĕige fángjiān?", "/819/regular_1411268985661_by_6.wav");
	  m.put("Wŏ zhù sān líng liù.", "/820/regular_1411268449048_by_6.wav");
	  m.put("Ò, duìbuqĭ, wŏ dĕi zŏule. Zàijiàn!", "/821/regular_1411268640899_by_6.wav");
	  m.put("Zàijiàn!", "/70/regular_1403654964412_by_4.wav");
	  
	  m.put("Sūn Lăoshī, qĭng wèn, Zhōngguo yŏu duōshăo rén?", "/839/regular_1411269368133_by_6.wav");
	  m.put("Zhōngguo chàbuduō yŏu shísānyì rén.", "/843/regular_1411269312568_by_6.wav");
	  m.put("Bĕijīng yŏu duōshăo rén?", "/845/regular_1411269494518_by_6.wav");
	  m.put("Bĕijīng yŏu yìqiānduōwàn rén.", "/852/regular_1411269286037_by_6.wav");
	  m.put("Nèmme, Nánjīng ne?", "/854/regular_1411269188503_by_6.wav");
	  m.put("Nánjīng de rénkŏu bĭjiào shăo.", "/865/regular_1411270065286_by_6.wav");
	  m.put("Hǎoxiàng zhĭ yŏu wŭbăiwàn.", "/866/regular_1411269694394_by_6.wav");

     m.put("Qĭng wèn.", "/182/regular_1409235429596_by_6.wav");
     m.put("Duìbuqĭ, wŏ xiăng zhăo yixiar Wáng Xiáojie.", "/892/regular_1414254585383_by_4.wav");
     m.put("Qĭng wèn, tā zài bu zai?", "/893/regular_1414255035009_by_4.wav");
     m.put("Nĕiwèi Wáng Xiáojie?", "/894/regular_1414255431428_by_4.wav");
     m.put("Shízài bàoqiàn.", "/897/regular_1414255228386_by_4.wav");
     m.put("Wŏ shi Mĕiguo Huáqiáo, Zhōngwén bú tài hăo.", "/910/regular_1414254783456_by_4.wav");
     m.put("Wŏ bù zhīdào tāde Zhōngwén míngzi.", "/911/regular_1414254726324_by_4.wav");
     m.put("Búguò, tāde Yīngwén míngzi jiào Mary Wang.", "/912/regular_1414254444764_by_4.wav");
     m.put("Ò, wŏ zhīdaole. Shi Wáng Guólì.", "/919/regular_1414255166050_by_4.wav");
     m.put("Búguò, tā xiànzài bú zài zhèr.", "/920/regular_1414254481166_by_4.wav");
     m.put("Qĭng wèn, nĭ zhīdao tā zài năr ma?", "/923/regular_1414254632039_by_4.wav");
     m.put("Tā zài lăobănde bàngōngshì.", "/927/regular_1414255257570_by_4.wav");
     m.put("Nà, wŏ kĕ bu kéyi gĕi tā liú yíge tiáozi?", "/934/regular_1414254895274_by_4.wav");
     m.put("Dāngrán kéyi.", "/936/regular_1414255136505_by_4.wav");
     m.put("Xièxie", "/62/regular_1403655382533_by_4.wav");

     m.put("Qĭng wèn, zhèige wèizi yŏu rén ma?", "/951/regular_1415630059034_by_4.wav");
     m.put("Méiyou, méiyou. Nín zùo ba.", "/952/regular_1415630501759_by_4.wav");
     m.put("Nín cháng lái zhèr chī wŭfàn ma?", "/958/regular_1415629892709_by_4.wav");
     m.put("Wŏ cháng lái. Nín ne?", "/959/regular_1415630135839_by_4.wav");
     m.put("Wŏ yĕ cháng zài zhèr chī. Nín shi xuésheng ma?", "/963/regular_1415630227493_by_4.wav");
     m.put("Bù, wŏ shi gōngrén, zài Bĕijīng Dìyī Píxié Chăng gōngzuò. Nín ne?", "/971/regular_1415630465812_by_4.wav");
     m.put("Wŏ zài Mĕiguo shi dàxuéshēng, xiànzài zài zhèr xué Zhōngwén.", "/977/regular_1415630298570_by_4.wav");
     m.put("Nín zài năr xuéxí?", "/978/regular_1415630764757_by_4.wav");
     m.put("Zài Bĕidàde Hànyŭ péixùn zhōngxīn.", "/984/regular_1415629699492_by_4.wav");
     m.put("Yò! Kuài yīdiăn le. Wŏ dĕi zŏule. Zàijiàn!", "/987/regular_1415630115280_by_4.wav");
     m.put("Zàijiàn!", "/70/regular_1403654964412_by_4.wav");

     m.put("Qĭng wèn, cèsuŏ zài năr?", "/1002/regular_1415631214967_by_4.wav");
     m.put("Zài nèibianr.", "/1005/regular_1415631477020_by_4.wav");
     m.put("Xiăo Lĭ, dùibuqĭ, ràng nĭ jiŭ dĕngle.", "/1009/regular_1415631573217_by_4.wav");
     m.put("Méi shìr, méi shìr. Xiăo Luó, hăo jiŭ bú jiànle!", "/1022/regular_1415632027900_by_4.wav");
     m.put("Nĭ zhèihuí lái Bĕijīng zhùzai năr?", "/1024/regular_1415632336371_by_4.wav");
     m.put("Zhùzài Cháng Chéng Fàndiàn, qī-yāo-wŭ-hào fángjiān.", "/1034/regular_1415631416029_by_4.wav");
     m.put("Nĭ hái shi zhùzai lăo dìfang ma?", "/1035/regular_1415631060610_by_4.wav");
     m.put("Bù, wŏmen qùnián jiù bāndao Xiāng Shān le.", "/1043/regular_1415631713088_by_4.wav");
     m.put("Xiāng Shān zài năr?", "/1044/regular_1415632376147_by_4.wav");
     m.put("Zài Bĕijīng chéngde bĕibiānr.", "/1050/regular_1415632183401_by_4.wav");

     m.put("Peter, wŏ măile yìtái diànnăo, zài shūfángli.", "/1082/regular_1415634859810_by_4.wav");
     m.put("Nĭ yào bu yao kànkan?", "/1083/regular_1415633449332_by_4.wav");
     m.put("Hăo a!", "/1084/regular_1415634968820_by_4.wav");
     m.put("E, shi yìtái wŭ-bā-liù ma?", "/1090/regular_1415635058807_by_4.wav");
     m.put("Lĭmiàn yŏu duōshăo RAM?", "/1091/regular_1415633568377_by_4.wav");
     m.put("Sānshi'èrge.", "/1092/regular_1415633261684_by_4.wav");
     m.put("Kāiguān yīnggāi zài pángbiān ba?", "/1097/regular_1415635001137_by_4.wav");
     m.put("Bú duì, zài qiánmian.", "/1101/regular_1415634309561_by_4.wav");
     m.put("Ò. E, wŏ kĕ bu kéyi kànkan shĭyòng shŏucè?", "/1106/regular_1415634453251_by_4.wav");
     m.put("Dāngrán kéyĭ.", "/1115/regular_1415634338874_by_4.wav");
     m.put("Jiù zài nĭ zuŏbiānde nèige shūjiàshang", "/1116/regular_1415634091286_by_4.wav");
     m.put("ò, bú duì, bú duì, shi zài nĭ hòumiande nèibă yĭzishang.", "/1117/regular_1415634396890_by_4.wav");
     m.put("Āiyò, zhuōzi dĭxia shi shémme dōngxi a?", "/1125/regular_1415633544254_by_4.wav");
     m.put("Ò, shi Láifú, wŏmen jiāde gŏu. Nĭ búyào guăn ta.", "/1133/regular_1415634363365_by_4.wav");
	  
	  return m;
  }
  
  private HashMap<String, String> getSentToSlowAudioPath() {
	  HashMap<String, String> m = new HashMap<String, String>();
	  m.put("Kē Léi'ēn, nǐ hăo!", "/4/slow_1403800571291_by_8.wav");
	  m.put("Nǐ dào năr qù a?", "/13/slow_1403801128819_by_8.wav");
	  m.put("Wŏ huí sùshè.", "/24/slow_1403800649832_by_8.wav");
	  m.put("Wáng Jīngshēng, nǐ hăo!", "/7/slow_1403800597192_by_8.wav");
	  m.put("Wŏ qù túshūguăn. Nĭ ne?", "/20/slow_1403800730216_by_8.wav");
	  
	  m.put("Zhào Guócái, nĭ hăo a!", "/40/slow_1403793805369_by_8.wav");
	  m.put("Hái xíng. Nĭ àirén, háizi dōu hăo ma?", "/57/slow_1403793264402_by_8.wav"); //hi
	  m.put("Wŏ yŏu yìdiănr shìr, xiān zŏule. Zàijiàn!", "/71/slow_1403792972693_by_8.wav");
	  m.put("Nĭ hăo! Hăo jiŭ bú jiànle.", "/44/slow_1403792786355_by_8.wav");
	  m.put("Zěmmeyàng a?", "/45/slow_1403792847063_by_8.wav");
	  m.put("Tāmen dōu hěn hăo, xièxie.", "/63/slow_1403793604382_by_8.wav");
	  m.put("Zàijiàn.", "/72/slow_1403792425728_by_8.wav");
	  
	  m.put("Èi, Lăo Hé!", "/90/slow_1403794714026_by_8.wav");
	  m.put("Xiăo Gāo!", "/93/slow_1403794875313_by_8.wav");
	  m.put("Zuìjìn zěmmeyàng a?", "/96/slow_1403794750850_by_8.wav");
	  m.put("Hái kéyi. Nĭ ne?", "/99/slow_1403795185981_by_8.wav");
	  m.put("Hái shi lăo yàngzi. Nĭ gōngzuò máng bu máng?", "/109/slow_1403795359109_by_8.wav");
	  m.put("Bú tài máng. Nĭ xuéxí zěmmeyàng?", "/115/slow_1403795093515_by_8.wav");
	  m.put("Tĭng jĭnzhāngde.", "/119/slow_1403795517517_by_8.wav");
	  
	  m.put("Xiè Tàitai, huānyíng, huānyíng! Qĭng jìn, qĭng jìn.", "/145/slow_1403797946854_by_8.wav");
	  m.put("Xièxie.", "/146/slow_1403798188972_by_8.wav");
	  m.put("Qĭng zuò, qĭng zuò.", "/149/slow_1403798082119_by_8.wav");
	  m.put("Xièxie.", "/146/slow_1403798188972_by_8.wav");
	  m.put("Lĭ Tàitai, wŏ yŏu yìdiăn shì, děi zŏule.", "/154/slow_1403797874655_by_8.wav");
	  m.put("Lĭ Tàitai, xièxie nín le.", "/158/slow_1403797916593_by_8.wav");
	  m.put("Bú kèqi. Màn zŏu a!", "/161/slow_1403798282629_by_8.wav");
	  m.put("Zàijiàn, zàijiàn!", "/162/slow_1403797569320_by_8.wav");
	  
	  m.put("Qĭng wèn, nĭ shì něiguó rén?", "/188/slow_1409322882550_by_8.wav");
	  m.put("Wŏ shi Měiguo rén.", "/191/slow_1409322902819_by_8.wav");
	  m.put("Nĭ jiào shémme míngzi?", "/196/slow_1409323768824_by_8.wav");
	  m.put("Wŏ jiào Bái Jiéruì.", "/199/slow_1409323280017_by_8.wav");
	  m.put("Nĭmen dōu shi Měiguo rén ma?", "/200/slow_1409321642660_by_8.wav");
	  m.put("Wŏmen bù dōu shi Měiguo rén.", "/202/slow_1409323710820_by_8.wav");
	  m.put("Zhèiwèi tóngxué yě shi Měiguo rén, kěshi nèiwèi tóngxué shi Jiā'nádà rén.", "/215/slow_1409323675900_by_8.wav");
	  
	  m.put("Qĭng jìn!", "/242/slow_1409324871090_by_8.wav");
	  m.put("Èi, Xiăo Mă. Ò.", "/243/missing2_2.wav");
	  m.put("Ò, Xiăo Chén, wŏ gĕi nĭ jièshao yixiar.", "/261/slow_1409325506235_by_8.wav");
	  m.put("Zhè shi wŏde xīn tóngwū, tā jiào Wáng Àihuá.", "/262/slow_1409325727429_by_8.wav");
	  m.put("Wáng Àihuá, zhè shi wŏde lăo tóngxué, Xiăo Chén.", "/263/slow_1409325351489_by_8.wav");
	  m.put("Ò, huānyíng nĭ dào Zhōngguo lái!", "/268/slow_1409325530229_by_8.wav");
	  m.put("Hĕn gāoxìng rènshi nĭ, Chén Xiáojie!", "/272/slow_1409325044588_by_8.wav");
	  m.put("Ò, bié zhèmme chēnghu wŏ.", "/280/slow_1409325482468_by_8.wav");
	  m.put("Hái shi jiào wŏ Xiăo Chén hăole.", "/281/slow_1409325324176_by_8.wav");
	  m.put("Xíng. Nà nĭ yě jiào wŏ Xiăo Wáng hăole.", "/284/slow_1409325437152_by_8.wav");
	  m.put("Hăo.", "/285/slow_1409324477262_by_8.wav");
	  
	  m.put("Nín guìxìng?", "/296/slow_1409328100134_by_8.wav");
	  m.put("Wŏ xìng Gāo. Nín guìxìng?", "/299/slow_1409327855443_by_8.wav");
	  m.put("Wŏ xìng Wú, Wú Sùshān.", "/310/slow_1409327876439_by_8.wav");
	  m.put("Gāo Xiānsheng, nín zài nĕige dānwèi gōngzuò?", "/311/slow_1409327769824_by_8.wav");
	  m.put("Wŏ zài Wàijiāobù gōngzuò. Nín ne?", "/314/slow_1409327623622_by_8.wav");
	  m.put("Wŏ zài Měiguo Dàshĭguăn gōngzuò.", "/317/slow_1409327660792_by_8.wav");
	  m.put("Nèiwèi shi nínde xiānsheng ba?", "/323/slow_1409327599327_by_8.wav");
	  m.put("Bú shì, bú shì! Tā shì wŏde tóngshì.", "/328/slow_1409327926507_by_8.wav");
	  m.put("Ò, Wú nǚshì, duìbuqĭ.", "/332/slow_1409327948820_by_8.wav");
	  m.put("Wŏ yŏu yìdiănshìr, xiān zŏule. Zàijiàn!", "/333/slow_1409327522521_by_8.wav");
	  m.put("Zàijiàn!", "/334/slow_1409327369250_by_8.wav");
	  
	  m.put("Nĭ hăo! Wŏ jiào Luó Jiésī.", "/355/slow_1409329610608_by_8.wav");
	  m.put("Qĭng duō zhĭ jiào.", "/357/slow_1409330005743_by_8.wav");
	  m.put("Wŏ xìng Shī. Duìbuqĭ, wŏ méi dài míngpiàn.", "/368/slow_1409329849454_by_8.wav");
	  m.put("Wŏ zài Zhōng-Měi Màoyì Gōngsī gōngzuò.", "/369/slow_1409329689735_by_8.wav");
	  m.put("Zŏngjīnglĭ, zhè shì Zhōng-Měi Màoyì Gōngsī de Shī Xiáojie.", "/374/slow_1409329769057_by_8.wav");
	  m.put("À, huānyíng, huānyíng! Wŏ xìng Hóu.", "/378/slow_1409329982278_by_8.wav");
	  m.put("Xièxie. Zŏngjīnglĭ yě shi Yīngguo rén ba?", "/382/slow_1409330123914_by_8.wav");
	  m.put("Bù, wŏ gēn Luó Xiáojie dōu bú shi Yīngguo rén.", "/387/slow_1409329924825_by_8.wav");
	  m.put("Wŏmen shi Měiguo rén.", "/388/slow_1409330286656_by_8.wav");
	  m.put("Ò, duìbuqĭ, wŏ găocuòle.", "/395/slow_1409329957821_by_8.wav");
	  m.put("Méi guānxi.", "/397/slow_1409330185688_by_8.wav");
	  
	  m.put("Nĭmen bānshang yŏu jĭwèi tóngxué?", "/415/slow_1411181695953_by_6.wav");
	  m.put("Yŏu shíwèi.", "/418/slow_1411182177877_by_6.wav");
	  m.put("Dōu shi Mĕiguo rén ma?", "/419/slow_1411181183573_by_6.wav");
	  m.put("Bù dōu shi Mĕiguó rén. Yŏu qíge Mĕiguo rén, liăngge Déguo rén gēn yíge Făguo rén.", "/432/slow_1411182270715_by_6.wav");
	  m.put("Jĭge nánshēng, jĭge nǚshēng?", "/436/slow_1411181659446_by_6.wav");
	  m.put("Yíbànr yíbànr. Wŭge nánde, wŭge nǚde.", "/446/slow_1411181508275_by_6.wav");
	  m.put("Nà, nĭmen yŏu jĭwèi lăoshī ne?", "/448/slow_1411182082658_by_6.wav");
	  m.put("Yígòng yŏu sānwèi. Liăngwèi shi nǚlăoshī, yíwèi shi nánlăoshī.", "/459/slow_1411182219021_by_6.wav");
	  
	  m.put("Zhè shi nĭ fùqin ma? Tā duō dà niánji le?", "/478/slow_1411225940951_by_6.wav");
	  m.put("Wŏ xiángxiang kàn. Tā jīnnián wŭshisānsuì le--bù, wŭshisìsuì le.", "/488/slow_1411226089961_by_6.wav");
	  m.put("Ò. Nà, zhèiwèi shi nĭ mŭqin ba?", "/490/slow_1411226358196_by_6.wav");
	  m.put("Duì, tā jīnnián sìshibāsuì le.", "/493/slow_1411226431876_by_6.wav");
	  m.put("Zhè shi nĭ mèimei, duì bu dui? Tā hĕn kĕ'ài! Tā jĭsuì le?", "/502/slow_1411226650376_by_6.wav");
	  m.put("Tā bāsuì. Xiàge yuè jiù jiŭsuì le.", "/510/slow_1411226515233_by_6.wav");
	  
	  m.put("Qĭng wèn, zhèige duōshăo qián?", "/529/slow_1411227238271_by_6.wav");
	  m.put("Yìbăi jiŭshibākuài.", "/534/slow_1411227591689_by_6.wav");
	  m.put("Yò, tài guìle!", "/538/slow_1411227285892_by_6.wav");
	  m.put("Zhèige bēizi duōshăo qián?", "/541/slow_1411227338293_by_6.wav");
	  m.put("Nèige zhĭ yào sānkuài wŭ.", "/547/slow_1411227857209_by_6.wav");
	  m.put("Wŏ kànkan, xíng bu xíng?", "/551/slow_1411228077277_by_6.wav");
	  m.put("Xíng, nín kàn ba.", "/554/slow_1411227762805_by_6.wav");
	  m.put("Hăo, wŏ măi liăngge.", "/557/slow_1411227557881_by_6.wav");
	  
	  m.put("Qĭng wèn, xiàyítàng dào Tiānjīnde huŏchē jĭdiăn kāi?", "/597/slow_1411228702408_by_6.wav");
	  m.put("Jiŭdiăn èrshí. Kĕshi xiànzài yĭjīng jiŭdiăn yíkè le, kŏngpà nín láibujíle.", "/610/slow_1411229159081_by_6.wav");
	  m.put("Nèmme, zài xiàyítàng ne?", "/614/slow_1411229643458_by_6.wav");
	  m.put("Wŏ kànkàn. Zài xiàyítàng shi shídiăn bàn.", "/616/slow_1411229071236_by_6.wav");
	  m.put("Hăo. Nà, wŏ jiù zuò shídiăn bànde.", "/620/slow_1411229285530_by_6.wav");
	  m.put("Duōshăo qián?", "/621/slow_1411228851261_by_6.wav");
	  m.put("Shíyīkuài wŭ.", "/622/slow_1411228679964_by_6.wav");
	  m.put("Dào Tiānjīn yào duō cháng shíjiān?", "/627/slow_1411228820212_by_6.wav");
	  m.put("Chàbuduō yào liăngge bàn zhōngtóu.", "/631/slow_1411228890218_by_6.wav");
	  m.put("Hăo, xièxie nín.", "/632/slow_1411229318465_by_6.wav");
	  
	  m.put("Qĭng wèn, yŭyán shíyànshì mĕitiān jĭdiăn zhōng kāimén, jĭdiăn zhōng guānmén?", "/662/slow_1411262844923_by_6.wav");
	  m.put("Zăoshang bādiăn kāimén, wănshang jiŭdiăn bàn guānmén.", "/667/slow_1411263228916_by_6.wav");
	  m.put("Xīngqīliù kāi bu kāi?", "/670/slow_1411263190845_by_6.wav");
	  m.put("Xīngqīliù kāi bàntiān. Shàngwŭ kāi, xiàwŭ bù kāi.", "/4-1.wav");
	  m.put("Xīngqītiān ne?", "/679/slow_1411262599867_by_6.wav");
	  m.put("Xīngqītiān xiūxi.", "/681/slow_1411263289663_by_6.wav");
	  m.put("Xièxie nĭ.", "/682/slow_1411263818028_by_6.wav");
	  m.put("Náli.", "/684/slow_1411263804056_by_6.wav");
	  
	  m.put("Nǐ jiào shémme míngzi?", "/721/slow_1411265556217_by_6.wav");
	  m.put("Wŏ jiào Zhāng Wényīng.", "/724/slow_1411265087634_by_6.wav");
	  m.put("Zhāng shi gōng cháng Zhāng, wén shi wénhuàde wén, yīng shi Yīngguode yīng.", "/729/slow_1411265395882_by_6.wav");
	  m.put("Nĭ shi nĕinián chūshēngde?", "/734/slow_1411265640694_by_6.wav");
	  m.put("Yī-jiŭ-bā-líng-nián, jiù shi Mínguó liùshijiŭnián.", "/740/slow_1411264906908_by_6.wav");
	  m.put("Jĭyuè jĭhào?", "/744/slow_1411265591145_by_6.wav");
	  m.put("Sìyuè shísānhào.", "/747/slow_1411264467785_by_6.wav");
	  m.put("Nĭde dìzhĭ shi...", "/749/slow_1411265697886_by_6.wav");
	  m.put("Hépíng Dōng Lù yīduàn, èrshiqīxiàng, sānnòng, yībăi wŭshisìhào, bālóu.", "/764/slow_1411264833266_by_6.wav");
	  m.put("Hăo, qĭng dĕng yíxià.", "/767/slow_1411264415948_by_6.wav");
	  
	  m.put("Nĭ hăo!", "/788/slow_1411268352448_by_6.wav");
	  m.put("Nĭ hăo! Nĭ shi Mĕiguo rén ma?", "/789/slow_1411268371731_by_6.wav");
	  m.put("Duì, wŏ shi Mĕiguo rén.", "/790/slow_1411269074227_by_6.wav");
	  m.put("Zhè shi nĭ dìyīcì dào Zhōngguo lái ma?", "/796/slow_1411268484407_by_6.wav");
	  m.put("Bù, zhè shi dì'èrcì. Wŏ qùnián láiguo yícì.", "/802/slow_1411268580827_by_6.wav");
	  m.put("M, nĭ zhèicì yào zhù duō jiŭ?", "/810/slow_1411268929660_by_6.wav");
	  m.put("Dàyuē bàn'ge yuè. Wŏ shí'èrhào huíguó.", "/816/slow_1411266203537_by_6.wav");
	  m.put("Nĭ zhù nĕige fángjiān?", "/819/slow_1411268992528_by_6.wav");
	  m.put("Wŏ zhù sān líng liù.", "/820/slow_1411268438086_by_6.wav");
	  m.put("Ò, duìbuqĭ, wŏ dĕi zŏule. Zàijiàn!", "/821/slow_1411268649403_by_6.wav");
	  m.put("Zàijiàn!", "/70/slow_1403654938909_by_4.wav");
	  
	  m.put("Sūn Lăoshī, qĭng wèn, Zhōngguo yŏu duōshăo rén?", "/839/slow_1411269378348_by_6.wav");
	  m.put("Zhōngguo chàbuduō yŏu shísānyì rén.", "/843/slow_1411269320632_by_6.wav");
	  m.put("Bĕijīng yŏu duōshăo rén?", "/845/slow_1411269501131_by_6.wav");
	  m.put("Bĕijīng yŏu yìqiānduōwàn rén.", "/852/slow_1411269294272_by_6.wav");
	  m.put("Nèmme, Nánjīng ne?", "/854/slow_1411269194605_by_6.wav");
	  m.put("Nánjīng de rénkŏu bĭjiào shăo.", "/865/slow_1411270052130_by_6.wav");
	  m.put("Hǎoxiàng zhĭ yŏu wŭbăiwàn.", "/866/slow_1411269701172_by_6.wav");

     m.put("Qĭng wèn.", "/182/slow_1409235448553_by_6.wav");
     m.put("Duìbuqĭ, wŏ xiăng zhăo yixiar Wáng Xiáojie.", "/892/slow_1414254592735_by_4.wav");
     m.put("Qĭng wèn, tā zài bu zai?", "/893/slow_1414255040713_by_4.wav");
     m.put("Nĕiwèi Wáng Xiáojie?", "/894/slow_1414255436557_by_4.wav");
     m.put("Shízài bàoqiàn.", "/897/slow_1414255233109_by_4.wav");
     m.put("Wŏ shi Mĕiguo Huáqiáo, Zhōngwén bú tài hăo.", "/910/slow_1414254791674_by_4.wav");
     m.put("Wŏ bù zhīdào tāde Zhōngwén míngzi.", "/911/slow_1414254733440_by_4.wav");
     m.put("Búguò, tāde Yīngwén míngzi jiào Mary Wang.", "/912/slow_1414254453161_by_4.wav");
     m.put("Ò, wŏ zhīdaole. Shi Wáng Guólì.", "/919/slow_1414255188188_by_4.wav");
     m.put("Búguò, tā xiànzài bú zài zhèr.", "/920/slow_1414254487909_by_4.wav");
     m.put("Qĭng wèn, nĭ zhīdao tā zài năr ma?", "/923/slow_1414254617292_by_4.wav");
     m.put("Tā zài lăobănde bàngōngshì.", "/927/slow_1414255264119_by_4.wav");
     m.put("Nà, wŏ kĕ bu kéyi gĕi tā liú yíge tiáozi?", "/934/slow_1414254904193_by_4.wav");
     m.put("Dāngrán kéyi.", "/936/slow_1414255141041_by_4.wav");
     m.put("Xièxie", "/62/slow_1403655389478_by_4.wav");

     m.put("Qĭng wèn, zhèige wèizi yŏu rén ma?", "/951/slow_1415630041850_by_4.wav");
     m.put("Méiyou, méiyou. Nín zùo ba.", "/952/slow_1415630494265_by_4.wav");
     m.put("Nín cháng lái zhèr chī wŭfàn ma?", "/958/slow_1415629881875_by_4.wav");
     m.put("Wŏ cháng lái. Nín ne?", "/959/slow_1415630131207_by_4.wav");
     m.put("Wŏ yĕ cháng zài zhèr chī. Nín shi xuésheng ma?", "/963/slow_1415630220413_by_4.wav");
     m.put("Bù, wŏ shi gōngrén, zài Bĕijīng Dìyī Píxié Chăng gōngzuò. Nín ne?", "/971/slow_1415630457858_by_4.wav");
     m.put("Wŏ zài Mĕiguo shi dàxuéshēng, xiànzài zài zhèr xué Zhōngwén.", "/977/slow_1415630291392_by_4.wav");
     m.put("Nín zài năr xuéxí?", "/978/slow_1415630758816_by_4.wav");
     m.put("Zài Bĕidàde Hànyŭ péixùn zhōngxīn.", "/984/slow_1415629682549_by_4.wav");
     m.put("Yò! Kuài yīdiăn le. Wŏ dĕi zŏule. Zàijiàn!", "/987/slow_1415630109395_by_4.wav");
     m.put("Zàijiàn!", "/70/slow_1403654938909_by_4.wav");

     m.put("Qĭng wèn, cèsuŏ zài năr?", "/1002/slow_1415631209901_by_4.wav");
     m.put("Zài nèibianr.", "/1005/slow_1415631468521_by_4.wav");
     m.put("Xiăo Lĭ, dùibuqĭ, ràng nĭ jiŭ dĕngle.", "/1009/slow_1415631566554_by_4.wav");
     m.put("Méi shìr, méi shìr. Xiăo Luó, hăo jiŭ bú jiànle!", "/1022/slow_1415632020964_by_4.wav");
     m.put("Nĭ zhèihuí lái Bĕijīng zhùzai năr?", "/1024/slow_1415632329433_by_4.wav");
     m.put("Zhùzài Cháng Chéng Fàndiàn, qī-yāo-wŭ-hào fángjiān.", "/1034/slow_1415631409023_by_4.wav");
     m.put("Nĭ hái shi zhùzai lăo dìfang ma?", "/1035/slow_1415631055266_by_4.wav");
     m.put("Bù, wŏmen qùnián jiù bāndao Xiāng Shān le.", "/1043/slow_1415631706100_by_4.wav");
     m.put("Xiāng Shān zài năr?", "/1044/slow_1415632371759_by_4.wav");
     m.put("Zài Bĕijīng chéngde bĕibiānr.", "/1050/slow_1415632161980_by_4.wav");

     m.put("Peter, wŏ măile yìtái diànnăo, zài shūfángli.", "/1082/slow_1415634851800_by_4.wav");
     m.put("Nĭ yào bu yao kànkan?", "/1083/slow_1415633442995_by_4.wav");
     m.put("Hăo a!", "/1084/slow_1415634965753_by_4.wav");
     m.put("E, shi yìtái wŭ-bā-liù ma?", "/1090/slow_1415635053418_by_4.wav");
     m.put("Lĭmiàn yŏu duōshăo RAM?", "/1091/slow_1415633562473_by_4.wav");
     m.put("Sānshi'èrge.", "/1092/slow_1415633271455_by_4.wav");
     m.put("Kāiguān yīnggāi zài pángbiān ba?", "/1097/slow_1415634995609_by_4.wav");
     m.put("Bú duì, zài qiánmian.", "/1101/slow_1415634300555_by_4.wav");
     m.put("Ò. E, wŏ kĕ bu kéyi kànkan shĭyòng shŏucè?", "/1106/slow_1415634424140_by_4.wav");
     m.put("Dāngrán kéyĭ.", "/1115/slow_1415634333448_by_4.wav");
     m.put("Jiù zài nĭ zuŏbiānde nèige shūjiàshang", "/1116/slow_1415634084799_by_4.wav");
     m.put("ò, bú duì, bú duì, shi zài nĭ hòumiande nèibă yĭzishang.", "/1117/slow_1415634387961_by_4.wav");
     m.put("Āiyò, zhuōzi dĭxia shi shémme dōngxi a?", "/1125/slow_1415633535971_by_4.wav");
     m.put("Ò, shi Láifú, wŏmen jiāde gŏu. Nĭ búyào guăn ta.", "/1133/slow_1415634356668_by_4.wav");
	  
	  return m;
  }
  
  private HashMap<String, PlayAudioPanel> getPlayAudioWidget(){
	  HashMap<String, PlayAudioPanel> pw = new HashMap<String, PlayAudioPanel>();
	  //at the moment, this list seems complete. wu3 is the only phone recorded by Haohsiang.
	  pw.put("a1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma1.mp3").setPlayLabel("a1"));
	  pw.put("a2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma2.mp3").setPlayLabel("a2"));
	  pw.put("a3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ha3o3.mp3").setPlayLabel("a3"));
	  pw.put("a4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ba4.mp3").setPlayLabel("a4"));
	  pw.put("b",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu4.mp3").setPlayLabel("b"));
	  pw.put("c",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/cai2.mp3").setPlayLabel("c"));
	  pw.put("ch",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/che2.mp3").setPlayLabel("ch"));
	  pw.put("d",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/dao2.mp3").setPlayLabel("d"));
	  pw.put("e1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke1.mp3").setPlayLabel("e1"));
	  pw.put("e2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/he2.mp3").setPlayLabel("e2"));
	  pw.put("e3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ye3.mp3").setPlayLabel("e3"));
	  pw.put("e4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke4.mp3").setPlayLabel("e4"));
	  pw.put("f",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/fa3.mp3").setPlayLabel("f"));
	  pw.put("g",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/go1.mp3").setPlayLabel("g"));
	  pw.put("h",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/he2.mp3").setPlayLabel("h"));
	  pw.put("i1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shi1.mp3").setPlayLabel("i1"));
	  pw.put("i2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/yi2.mp3").setPlayLabel("i2"));
	  pw.put("i3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ni3.mp3").setPlayLabel("i3"));
	  pw.put("i4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shi4.mp3").setPlayLabel("i4"));
	  pw.put("j",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ji1ng.mp3").setPlayLabel("j"));
	  pw.put("k",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ke4.mp3").setPlayLabel("k"));
	  pw.put("l",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/lao3.mp3").setPlayLabel("l"));
	  pw.put("m",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ma2.mp3").setPlayLabel("m"));
	  pw.put("n",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ni3.mp3").setPlayLabel("n"));
	  pw.put("ng",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/a2ng.mp3").setPlayLabel("ng"));
	  pw.put("o1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zho1.mp3").setPlayLabel("o1"));
	  pw.put("o2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ro2.mp3").setPlayLabel("o2"));
	  pw.put("o3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wo3.mp3").setPlayLabel("o3"));
	  pw.put("o4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zuo4.mp3").setPlayLabel("o4"));
	  pw.put("p",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/pi4a4n.mp3").setPlayLabel("p"));
	  pw.put("q",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/quu4.mp3").setPlayLabel("q"));
	  pw.put("r",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/ro2.mp3").setPlayLabel("r"));
	  pw.put("s",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/su4.mp3").setPlayLabel("s"));
	  pw.put("sh",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shu1.mp3").setPlayLabel("sh"));
	  pw.put("t",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/tu2.mp3").setPlayLabel("t"));
	  pw.put("u1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/shu1.mp3").setPlayLabel("u1"));
	  pw.put("u2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu2.mp3").setPlayLabel("u2"));
	  pw.put("u3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wu3.mp3").setPlayLabel("u3"));
	  pw.put("u4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/bu4.mp3").setPlayLabel("u4"));
	  pw.put("uu1",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("uu1"));
	  pw.put("uu2",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/xuue2.mp3").setPlayLabel("uu2"));
	  pw.put("uu3",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/nuu3.mp3").setPlayLabel("uu3"));
	  pw.put("uu4",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("uu4"));
	  pw.put("w",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/wo3.mp3").setPlayLabel("w"));
	  pw.put("x",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/xuue2.mp3").setPlayLabel("x"));
	  pw.put("y",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/yi2.mp3").setPlayLabel("y"));
	  pw.put("z",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zou3.mp3").setPlayLabel("z"));
	  pw.put("zh",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/zho1.mp3").setPlayLabel("zh"));
	  pw.put("sil",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("sil"));
	  pw.put("unk",new PlayAudioPanel(controller, "config/mandarinClassroom/phones/x.mp3").setPlayLabel("unk"));
	  for(String k: pw.keySet()){
		  controller.register(pw.get(k).getPlayButton(), "playing example phone for "+k);
	  }
	  return pw;
  }
  
  private HashMap<String, HashMap<String, Integer>> getDialogToSpeakerToLast(){
	  HashMap<String, HashMap<String, Integer>> m = new HashMap<String, HashMap<String, Integer>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  String up3 = "Unit 1: Part 3";
	  String up4 = "Unit 1: Part 4";
	  String u2p1 = "Unit 2: Part 1";
	  String u2p2 = "Unit 2: Part 2";
	  String u2p3 = "Unit 2: Part 3";
	  String u2p4 = "Unit 2: Part 4";
	  String u3p1 = "Unit 3: Part 1";
	  String u3p2 = "Unit 3: Part 2";
	  String u3p3 = "Unit 3: Part 3";
	  String u3p4 = "Unit 3: Part 4";
	  String u4p1 = "Unit 4: Part 1";
	  String u4p2 = "Unit 4: Part 2";
	  String u4p3 = "Unit 4: Part 3";
	  String u4p4 = "Unit 4: Part 4";
     String u5p1 = "Unit 5: Part 1";
     String u5p2 = "Unit 5: Part 2";
     String u5p3 = "Unit 5: Part 3";
     String u5p4 = "Unit 5: Part 4";

	  m.put(up1, new HashMap<String, Integer>());
	  m.put(up2, new HashMap<String, Integer>());
	  m.put(up3, new HashMap<String, Integer>());
	  m.put(up4, new HashMap<String, Integer>());
	  m.put(u2p1, new HashMap<String, Integer>());
	  m.put(u2p2, new HashMap<String, Integer>());
	  m.put(u2p3, new HashMap<String, Integer>());
	  m.put(u2p4, new HashMap<String, Integer>());
	  m.put(u3p1, new HashMap<String, Integer>());
	  m.put(u3p2, new HashMap<String, Integer>());
	  m.put(u3p3, new HashMap<String, Integer>());
	  m.put(u3p4, new HashMap<String, Integer>());
	  m.put(u4p1, new HashMap<String, Integer>());
	  m.put(u4p2, new HashMap<String, Integer>());
	  m.put(u4p3, new HashMap<String, Integer>());
	  m.put(u4p4, new HashMap<String, Integer>());
     m.put(u5p1, new HashMap<String, Integer>());
     m.put(u5p2, new HashMap<String, Integer>());
     m.put(u5p3, new HashMap<String, Integer>());
     m.put(u5p4, new HashMap<String, Integer>());
	  m.get(up1).put("Wang", 4);
	  m.get(up1).put("Crane", 3);
	  m.get(up2).put("Smith", 5);
	  m.get(up2).put("Zhao", 6);
	  m.get(up3).put("He", 5);
	  m.get(up3).put("Kao", 6);
	  m.get(up4).put("Mrs. Smith", 7);
	  m.get(up4).put("Mrs. Li", 6);
	  m.get(u2p1).put("Taiwanese Student", 4);
	  m.get(u2p1).put("Parsons", 6);
	  m.get(u2p2).put("First Chinese", 4);
	  m.get(u2p2).put("American", 9);
	  m.get(u2p2).put("Second Chinese", 10);
	  m.get(u2p3).put("Chinese", 9);
	  m.get(u2p3).put("American", 10);
	  m.get(u2p4).put("Rogers", 4);
	  m.get(u2p4).put("Taiwanese Guest", 9);
	  m.get(u2p4).put("Holbrooke", 10);
	  m.get(u3p1).put("Chinese", 6);
	  m.get(u3p1).put("American", 7);
	  m.get(u3p2).put("Teacher", 4);
	  m.get(u3p2).put("Thompson", 5);
	  m.get(u3p3).put("Salesman", 6);
	  m.get(u3p3).put("Little", 7);
	  m.get(u3p4).put("Ticket Agent", 8);
	  m.get(u3p4).put("Haynes", 9);
	  m.get(u4p1).put("American", 6);
	  m.get(u4p1).put("Language Lab Attendant", 7);
     m.get(u4p2).put("Johns", 8);
     m.get(u4p2).put("Nurse", 9);
     m.get(u4p3).put("Bellhop", 9);
     m.get(u4p3).put("Ross", 10);
     m.get(u4p4).put("Miller", 4);
     m.get(u4p4).put("Sun", 6);
     m.get(u5p1).put("Chinese", 0);
     m.get(u5p1).put("American", 1);
     m.get(u5p1).put("American", 2);
     m.get(u5p1).put("Chinese", 3);
     m.get(u5p1).put("American", 4);
     m.get(u5p1).put("American", 5);
     m.get(u5p1).put("American", 6);
     m.get(u5p1).put("American", 7);
     m.get(u5p1).put("Chinese", 8);
     m.get(u5p1).put("Chinese", 9);
     m.get(u5p1).put("American", 10);
     m.get(u5p1).put("Chinese", 11);
     m.get(u5p1).put("American", 12);
     m.get(u5p1).put("Chinese", 13);
     m.get(u5p1).put("American", 14);

     m.get(u5p2).put("American", 0);
     m.get(u5p2).put("Chinese", 1);
     m.get(u5p2).put("American", 2);
     m.get(u5p2).put("Chinese", 3);
     m.get(u5p2).put("American", 4);
     m.get(u5p2).put("Chinese", 5);
     m.get(u5p2).put("American", 6);
     m.get(u5p2).put("Chinese", 7);
     m.get(u5p2).put("American", 8);
     m.get(u5p2).put("Chinese", 9);
     m.get(u5p2).put("American", 10);

     m.get(u5p3).put("Norris", 0);
     m.get(u5p3).put("Cashier", 1);
     m.get(u5p3).put("Norris", 2);
     m.get(u5p3).put("Li", 3);
     m.get(u5p3).put("Li", 4);
     m.get(u5p3).put("Norris", 5);
     m.get(u5p3).put("Norris", 6);
     m.get(u5p3).put("Li", 7);
     m.get(u5p3).put("Norris", 8);
     m.get(u5p3).put("Li", 9);

     m.get(u5p4).put("Guo", 0);
     m.get(u5p4).put("Guo", 1);
     m.get(u5p4).put("Walters", 2);
     m.get(u5p4).put("Walters", 3);
     m.get(u5p4).put("Walters", 4);
     m.get(u5p4).put("Guo", 5);
     m.get(u5p4).put("Walters", 6);
     m.get(u5p4).put("Guo", 7);
     m.get(u5p4).put("Walters", 8);
     m.get(u5p4).put("Guo", 9);
     m.get(u5p4).put("Guo", 10);
     m.get(u5p4).put("Guo", 11);
     m.get(u5p4).put("Walters", 12);
     m.get(u5p4).put("Guo", 13);
	  
	  return m;
  }
  
  private HashMap<String, HashMap<Integer, String>> getDialogToSentIndexToSpeaker() {
	  HashMap<String, HashMap<Integer, String>> m = new HashMap<String, HashMap<Integer, String>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  String up3 = "Unit 1: Part 3";
	  String up4 = "Unit 1: Part 4";
	  String u2p1 = "Unit 2: Part 1";
	  String u2p2 = "Unit 2: Part 2";
	  String u2p3 = "Unit 2: Part 3";
	  String u2p4 = "Unit 2: Part 4";
	  String u3p1 = "Unit 3: Part 1";
	  String u3p2 = "Unit 3: Part 2";
	  String u3p3 = "Unit 3: Part 3";
	  String u3p4 = "Unit 3: Part 4";
	  String u4p1 = "Unit 4: Part 1";
	  String u4p2 = "Unit 4: Part 2";
	  String u4p3 = "Unit 4: Part 3";
	  String u4p4 = "Unit 4: Part 4";
     String u5p1 = "Unit 5: Part 1";
     String u5p2 = "Unit 5: Part 2";
     String u5p3 = "Unit 5: Part 3";
     String u5p4 = "Unit 5: Part 4";

	  m.put(up1, new HashMap<Integer, String>());
	  m.put(up2, new HashMap<Integer, String>());
	  m.put(up3, new HashMap<Integer, String>());
	  m.put(up4, new HashMap<Integer, String>());
	  m.put(u2p1, new HashMap<Integer, String>());
	  m.put(u2p2, new HashMap<Integer, String>());
	  m.put(u2p3, new HashMap<Integer, String>());
	  m.put(u2p4, new HashMap<Integer, String>());
	  m.put(u3p1, new HashMap<Integer, String>());
	  m.put(u3p2, new HashMap<Integer, String>());
	  m.put(u3p3, new HashMap<Integer, String>());
	  m.put(u3p4, new HashMap<Integer, String>());
	  m.put(u4p1, new HashMap<Integer, String>());
	  m.put(u4p2, new HashMap<Integer, String>());
	  m.put(u4p3, new HashMap<Integer, String>());
	  m.put(u4p4, new HashMap<Integer, String>());
     m.put(u5p1, new HashMap<Integer, String>());
     m.put(u5p2, new HashMap<Integer, String>());
     m.put(u5p3, new HashMap<Integer, String>());
     m.put(u5p4, new HashMap<Integer, String>());
	  
	  m.get(up1).put(0, "Wang");
	  m.get(up1).put(1, "Crane");
	  m.get(up1).put(2, "Wang");
	  m.get(up1).put(3, "Crane");
	  m.get(up1).put(4, "Wang");
	  
	  m.get(up2).put(0, "Smith");
	  m.get(up2).put(1, "Zhao");
	  m.get(up2).put(2, "Zhao");
	  m.get(up2).put(3, "Smith");
	  m.get(up2).put(4, "Zhao");
	  m.get(up2).put(5, "Smith");
	  m.get(up2).put(6, "Zhao");
	  
	  m.get(up3).put(0, "Kao");
	  m.get(up3).put(1, "He");
	  m.get(up3).put(2, "Kao");
	  m.get(up3).put(3, "He");
	  m.get(up3).put(4, "Kao");
	  m.get(up3).put(5, "He");
	  m.get(up3).put(6, "Kao");
	  
	  m.get(up4).put(0, "Mrs. Li");
	  m.get(up4).put(1, "Mrs. Smith");
	  m.get(up4).put(2, "Mrs. Li");
	  m.get(up4).put(3, "Mrs. Smith");
	  m.get(up4).put(4, "Mrs. Smith");
	  m.get(up4).put(5, "Mrs. Smith");
	  m.get(up4).put(6, "Mrs. Li");
	  m.get(up4).put(7, "Mrs. Smith");
	  
	  m.get(u2p1).put(0, "Taiwanese Student");
	  m.get(u2p1).put(1, "Parsons");
	  m.get(u2p1).put(2, "Taiwanese Student");
	  m.get(u2p1).put(3, "Parsons");
	  m.get(u2p1).put(4, "Taiwanese Student");
	  m.get(u2p1).put(5, "Parsons");
	  m.get(u2p1).put(6, "Parsons");
	  
	  m.get(u2p2).put(0, "First Chinese");
	  m.get(u2p2).put(1, "Second Chinese");
	  m.get(u2p2).put(2, "First Chinese");
	  m.get(u2p2).put(3, "First Chinese");
	  m.get(u2p2).put(4, "First Chinese");
	  m.get(u2p2).put(5, "Second Chinese");
	  m.get(u2p2).put(6, "American");
	  m.get(u2p2).put(7, "Second Chinese");
	  m.get(u2p2).put(8, "Second Chinese");
	  m.get(u2p2).put(9, "American");
	  m.get(u2p2).put(10, "Second Chinese");
	  
	  m.get(u2p3).put(0, "American");
	  m.get(u2p3).put(1, "Chinese");
	  m.get(u2p3).put(2, "American");
	  m.get(u2p3).put(3, "American");
	  m.get(u2p3).put(4, "Chinese");
	  m.get(u2p3).put(5, "American");
	  m.get(u2p3).put(6, "Chinese");
	  m.get(u2p3).put(7, "American");
	  m.get(u2p3).put(8, "Chinese");
	  m.get(u2p3).put(9, "Chinese");
	  m.get(u2p3).put(10, "American");
	  
	  m.get(u2p4).put(0, "Rogers");
	  m.get(u2p4).put(1, "Rogers");
	  m.get(u2p4).put(2, "Taiwanese Guest");
	  m.get(u2p4).put(3, "Taiwanese Guest");
	  m.get(u2p4).put(4, "Rogers");
	  m.get(u2p4).put(5, "Holbrooke");
	  m.get(u2p4).put(6, "Taiwanese Guest");
	  m.get(u2p4).put(7, "Holbrooke");
	  m.get(u2p4).put(8, "Holbrooke");
	  m.get(u2p4).put(9, "Taiwanese Guest");
	  m.get(u2p4).put(10, "Holbrooke");
	  
	  m.get(u3p1).put(0, "Chinese");
	  m.get(u3p1).put(1, "American");
	  m.get(u3p1).put(2, "Chinese");
	  m.get(u3p1).put(3, "American");
	  m.get(u3p1).put(4, "Chinese");
	  m.get(u3p1).put(5, "American");
	  m.get(u3p1).put(6, "Chinese");
	  m.get(u3p1).put(7, "American");
	  
	  m.get(u3p2).put(0, "Teacher");
	  m.get(u3p2).put(1, "Thompson");
	  m.get(u3p2).put(2, "Teacher");
	  m.get(u3p2).put(3, "Thompson");
	  m.get(u3p2).put(4, "Teacher");
	  m.get(u3p2).put(5, "Thompson");
	  
	  m.get(u3p3).put(0, "Little");
	  m.get(u3p3).put(1, "Salesman");
	  m.get(u3p3).put(2, "Little");
	  m.get(u3p3).put(3, "Little");
	  m.get(u3p3).put(4, "Salesman");
	  m.get(u3p3).put(5, "Little");
	  m.get(u3p3).put(6, "Salesman");
	  m.get(u3p3).put(7, "Little");
	  
	  m.get(u3p4).put(0, "Haynes");
	  m.get(u3p4).put(1, "Ticket Agent");
	  m.get(u3p4).put(2, "Haynes");
	  m.get(u3p4).put(3, "Ticket Agent");
	  m.get(u3p4).put(4, "Haynes");
	  m.get(u3p4).put(5, "Haynes");
	  m.get(u3p4).put(6, "Ticket Agent");
	  m.get(u3p4).put(7, "Haynes");
	  m.get(u3p4).put(8, "Ticket Agent");
	  m.get(u3p4).put(9, "Haynes");
	  
	  m.get(u4p1).put(0, "American");
	  m.get(u4p1).put(1, "Language Lab Attendant");
	  m.get(u4p1).put(2, "American");
	  m.get(u4p1).put(3, "Language Lab Attendant");
	  m.get(u4p1).put(4, "American");
	  m.get(u4p1).put(5, "Language Lab Attendant");
	  m.get(u4p1).put(6, "American");
	  m.get(u4p1).put(7, "Language Lab Attendant");
	  
	  m.get(u4p2).put(0, "Nurse");
	  m.get(u4p2).put(1, "Johns");
	  m.get(u4p2).put(2, "Johns");
	  m.get(u4p2).put(3, "Nurse");
	  m.get(u4p2).put(4, "Johns");
	  m.get(u4p2).put(5, "Nurse");
	  m.get(u4p2).put(6, "Johns");
	  m.get(u4p2).put(7, "Nurse");
	  m.get(u4p2).put(8, "Johns");
	  m.get(u4p2).put(9, "Nurse");
	  
	  m.get(u4p3).put(0, "Ross");
	  m.get(u4p3).put(1, "Bellhop");
	  m.get(u4p3).put(2, "Ross");
	  m.get(u4p3).put(3, "Bellhop");
	  m.get(u4p3).put(4, "Ross");
	  m.get(u4p3).put(5, "Bellhop");
	  m.get(u4p3).put(6, "Ross");
	  m.get(u4p3).put(7, "Bellhop");
	  m.get(u4p3).put(8, "Ross");
	  m.get(u4p3).put(9, "Bellhop");
	  m.get(u4p3).put(10, "Ross");
	  
	  m.get(u4p4).put(0, "Miller");
	  m.get(u4p4).put(1, "Sun");
	  m.get(u4p4).put(2, "Miller");
	  m.get(u4p4).put(3, "Sun");
	  m.get(u4p4).put(4, "Miller");
	  m.get(u4p4).put(5, "Sun");
	  m.get(u4p4).put(6, "Sun");

     m.get(u5p1).put(0, "Chinese");
     m.get(u5p1).put(1, "American");
     m.get(u5p1).put(2, "American");
     m.get(u5p1).put(3, "Chinese");
     m.get(u5p1).put(4, "American");
     m.get(u5p1).put(5, "American");
     m.get(u5p1).put(6, "American");
     m.get(u5p1).put(7, "American");
     m.get(u5p1).put(8, "Chinese");
     m.get(u5p1).put(9, "Chinese");
     m.get(u5p1).put(10, "American");
     m.get(u5p1).put(11, "Chinese");
     m.get(u5p1).put(12, "American");
     m.get(u5p1).put(13, "Chinese");
     m.get(u5p1).put(14, "American");

     m.get(u5p2).put(0, "American");
     m.get(u5p2).put(1, "Chinese");
     m.get(u5p2).put(2, "American");
     m.get(u5p2).put(3, "Chinese");
     m.get(u5p2).put(4, "American");
     m.get(u5p2).put(5, "Chinese");
     m.get(u5p2).put(6, "American");
     m.get(u5p2).put(7, "Chinese");
     m.get(u5p2).put(8, "American");
     m.get(u5p2).put(9, "Chinese");
     m.get(u5p2).put(10, "American");

     m.get(u5p3).put(0, "Norris");
     m.get(u5p3).put(1, "Cashier");
     m.get(u5p3).put(2, "Norris");
     m.get(u5p3).put(3, "Li");
     m.get(u5p3).put(4, "Li");
     m.get(u5p3).put(5, "Norris");
     m.get(u5p3).put(6, "Norris");
     m.get(u5p3).put(7, "Li");
     m.get(u5p3).put(8, "Norris");
     m.get(u5p3).put(9, "Li");

     m.get(u5p4).put(0, "Guo");
     m.get(u5p4).put(1, "Guo");
     m.get(u5p4).put(2, "Walters");
     m.get(u5p4).put(3, "Walters");
     m.get(u5p4).put(4, "Walters");
     m.get(u5p4).put(5, "Guo");
     m.get(u5p4).put(6, "Walters");
     m.get(u5p4).put(7, "Guo");
     m.get(u5p4).put(8, "Walters");
     m.get(u5p4).put(9, "Guo");
     m.get(u5p4).put(10, "Guo");
     m.get(u5p4).put(11, "Guo");
     m.get(u5p4).put(12, "Walters");
     m.get(u5p4).put(13, "Guo");

	  
	  return m;
  }
  
  private HashMap<String, HashMap<Integer, String>> getDialogToSentIndexToSent() {
	  HashMap<String, HashMap<Integer, String>> m = new HashMap<String, HashMap<Integer, String>>();
	  String up1 = "Unit 1: Part 1";
	  String up2 = "Unit 1: Part 2";
	  String up3 = "Unit 1: Part 3";
	  String up4 = "Unit 1: Part 4";
	  String u2p1 = "Unit 2: Part 1";
	  String u2p2 = "Unit 2: Part 2";
	  String u2p3 = "Unit 2: Part 3";
	  String u2p4 = "Unit 2: Part 4";
	  String u3p1 = "Unit 3: Part 1";
	  String u3p2 = "Unit 3: Part 2";
	  String u3p3 = "Unit 3: Part 3";
	  String u3p4 = "Unit 3: Part 4";
	  String u4p1 = "Unit 4: Part 1";
	  String u4p2 = "Unit 4: Part 2";
	  String u4p3 = "Unit 4: Part 3";
	  String u4p4 = "Unit 4: Part 4";
     String u5p1 = "Unit 5: Part 1";
     String u5p2 = "Unit 5: Part 2";
     String u5p3 = "Unit 5: Part 3";
     String u5p4 = "Unit 5: Part 4";
	  
	  m.put(up1, new HashMap<Integer, String>());
	  m.put(up2, new HashMap<Integer, String>());
	  m.put(up3, new HashMap<Integer, String>());
	  m.put(up4, new HashMap<Integer, String>());
	  m.put(u2p1, new HashMap<Integer, String>());
	  m.put(u2p2, new HashMap<Integer, String>());
	  m.put(u2p3, new HashMap<Integer, String>());
	  m.put(u2p4, new HashMap<Integer, String>());
	  m.put(u3p1, new HashMap<Integer, String>());
	  m.put(u3p2, new HashMap<Integer, String>());
	  m.put(u3p3, new HashMap<Integer, String>());
	  m.put(u3p4, new HashMap<Integer, String>());
	  m.put(u4p1, new HashMap<Integer, String>());
	  m.put(u4p2, new HashMap<Integer, String>());
	  m.put(u4p3, new HashMap<Integer, String>());
	  m.put(u4p4, new HashMap<Integer, String>());
     m.put(u5p1, new HashMap<Integer, String>());
     m.put(u5p2, new HashMap<Integer, String>());
     m.put(u5p3, new HashMap<Integer, String>());
     m.put(u5p4, new HashMap<Integer, String>());
	  
	  m.get(up1).put(0, "Kē Léi'ēn, nǐ hăo!");
	  m.get(up1).put(1, "Wáng Jīngshēng, nǐ hăo!");
	  m.get(up1).put(2, "Nǐ dào năr qù a?");
	  m.get(up1).put(3, "Wŏ qù túshūguăn. Nĭ ne?");
	  m.get(up1).put(4, "Wŏ huí sùshè.");
	  
	  m.get(up2).put(0, "Zhào Guócái, nĭ hăo a!");
	  m.get(up2).put(1, "Nĭ hăo! Hăo jiŭ bú jiànle.");
	  m.get(up2).put(2, "Zěmmeyàng a?");
	  m.get(up2).put(3, "Hái xíng. Nĭ àirén, háizi dōu hăo ma?");
	  m.get(up2).put(4, "Tāmen dōu hěn hăo, xièxie.");
	  m.get(up2).put(5, "Wŏ yŏu yìdiănr shìr, xiān zŏule. Zàijiàn!");
	  m.get(up2).put(6, "Zàijiàn.");
	  
	  m.get(up3).put(0, "Èi, Lăo Hé!");
	  m.get(up3).put(1, "Xiăo Gāo!");
	  m.get(up3).put(2, "Zuìjìn zěmmeyàng a?");
	  m.get(up3).put(3, "Hái kéyi. Nĭ ne?");
	  m.get(up3).put(4, "Hái shi lăo yàngzi. Nĭ gōngzuò máng bu máng?");
	  m.get(up3).put(5, "Bú tài máng. Nĭ xuéxí zěmmeyàng?");
	  m.get(up3).put(6, "Tĭng jĭnzhāngde.");
	  
	  m.get(up4).put(0, "Xiè Tàitai, huānyíng, huānyíng! Qĭng jìn, qĭng jìn.");
	  m.get(up4).put(1, "Xièxie.");
	  m.get(up4).put(2, "Qĭng zuò, qĭng zuò.");
	  m.get(up4).put(3, "Xièxie.");
	  m.get(up4).put(4, "Lĭ Tàitai, wŏ yŏu yìdiăn shì, děi zŏule.");
	  m.get(up4).put(5, "Lĭ Tàitai, xièxie nín le.");
	  m.get(up4).put(6, "Bú kèqi. Màn zŏu a!");
	  m.get(up4).put(7, "Zàijiàn, zàijiàn!");
	  
	  m.get(u2p1).put(0, "Qĭng wèn, nĭ shì něiguó rén?");
	  m.get(u2p1).put(1, "Wŏ shi Měiguo rén.");
	  m.get(u2p1).put(2, "Nĭ jiào shémme míngzi?");
	  m.get(u2p1).put(3, "Wŏ jiào Bái Jiéruì.");
	  m.get(u2p1).put(4, "Nĭmen dōu shi Měiguo rén ma?");
	  m.get(u2p1).put(5, "Wŏmen bù dōu shi Měiguo rén.");
	  m.get(u2p1).put(6, "Zhèiwèi tóngxué yě shi Měiguo rén, kěshi nèiwèi tóngxué shi Jiā'nádà rén.");
	  
	  m.get(u2p2).put(0, "Qĭng jìn!");
	  m.get(u2p2).put(1, "Èi, Xiăo Mă. Ò.");
	  m.get(u2p2).put(2, "Ò, Xiăo Chén, wŏ gĕi nĭ jièshao yixiar.");
	  m.get(u2p2).put(3, "Zhè shi wŏde xīn tóngwū, tā jiào Wáng Àihuá.");
	  m.get(u2p2).put(4, "Wáng Àihuá, zhè shi wŏde lăo tóngxué, Xiăo Chén.");
	  m.get(u2p2).put(5, "Ò, huānyíng nĭ dào Zhōngguo lái!");
	  m.get(u2p2).put(6, "Hĕn gāoxìng rènshi nĭ, Chén Xiáojie!");
	  m.get(u2p2).put(7, "Ò, bié zhèmme chēnghu wŏ.");
	  m.get(u2p2).put(8, "Hái shi jiào wŏ Xiăo Chén hăole.");
	  m.get(u2p2).put(9, "Xíng. Nà nĭ yě jiào wŏ Xiăo Wáng hăole.");
	  m.get(u2p2).put(10, "Hăo.");
	  
	  m.get(u2p3).put(0, "Nín guìxìng?");
	  m.get(u2p3).put(1, "Wŏ xìng Gāo. Nín guìxìng?");
	  m.get(u2p3).put(2, "Wŏ xìng Wú, Wú Sùshān.");
	  m.get(u2p3).put(3, "Gāo Xiānsheng, nín zài nĕige dānwèi gōngzuò?");
	  m.get(u2p3).put(4, "Wŏ zài Wàijiāobù gōngzuò. Nín ne?");
	  m.get(u2p3).put(5, "Wŏ zài Měiguo Dàshĭguăn gōngzuò.");
	  m.get(u2p3).put(6, "Nèiwèi shi nínde xiānsheng ba?");
	  m.get(u2p3).put(7, "Bú shì, bú shì! Tā shì wŏde tóngshì.");
	  m.get(u2p3).put(8, "Ò, Wú nǚshì, duìbuqĭ.");
	  m.get(u2p3).put(9, "Wŏ yŏu yìdiănshìr, xiān zŏule. Zàijiàn!");
	  m.get(u2p3).put(10, "Zàijiàn!");
	  
	  m.get(u2p4).put(0, "Nĭ hăo! Wŏ jiào Luó Jiésī.");
	  m.get(u2p4).put(1, "Qĭng duō zhĭ jiào.");
	  m.get(u2p4).put(2, "Wŏ xìng Shī. Duìbuqĭ, wŏ méi dài míngpiàn.");
	  m.get(u2p4).put(3, "Wŏ zài Zhōng-Měi Màoyì Gōngsī gōngzuò.");
	  m.get(u2p4).put(4, "Zŏngjīnglĭ, zhè shì Zhōng-Měi Màoyì Gōngsī de Shī Xiáojie.");
	  m.get(u2p4).put(5, "À, huānyíng, huānyíng! Wŏ xìng Hóu.");
	  m.get(u2p4).put(6, "Xièxie. Zŏngjīnglĭ yě shi Yīngguo rén ba?");
	  m.get(u2p4).put(7, "Bù, wŏ gēn Luó Xiáojie dōu bú shi Yīngguo rén.");
	  m.get(u2p4).put(8, "Wŏmen shi Měiguo rén.");
	  m.get(u2p4).put(9, "Ò, duìbuqĭ, wŏ găocuòle.");
	  m.get(u2p4).put(10, "Méi guānxi.");
	  
	  m.get(u3p1).put(0, "Nĭmen bānshang yŏu jĭwèi tóngxué?");
	  m.get(u3p1).put(1, "Yŏu shíwèi.");
	  m.get(u3p1).put(2, "Dōu shi Mĕiguo rén ma?");
	  m.get(u3p1).put(3, "Bù dōu shi Mĕiguó rén. Yŏu qíge Mĕiguo rén, liăngge Déguo rén gēn yíge Făguo rén.");
	  m.get(u3p1).put(4, "Jĭge nánshēng, jĭge nǚshēng?");
	  m.get(u3p1).put(5, "Yíbànr yíbànr. Wŭge nánde, wŭge nǚde.");
	  m.get(u3p1).put(6, "Nà, nĭmen yŏu jĭwèi lăoshī ne?");
	  m.get(u3p1).put(7, "Yígòng yŏu sānwèi. Liăngwèi shi nǚlăoshī, yíwèi shi nánlăoshī.");
	  
	  m.get(u3p2).put(0, "Zhè shi nĭ fùqin ma? Tā duō dà niánji le?");
	  m.get(u3p2).put(1, "Wŏ xiángxiang kàn. Tā jīnnián wŭshisānsuì le--bù, wŭshisìsuì le.");
	  m.get(u3p2).put(2, "Ò. Nà, zhèiwèi shi nĭ mŭqin ba?");
	  m.get(u3p2).put(3, "Duì, tā jīnnián sìshibāsuì le.");
	  m.get(u3p2).put(4, "Zhè shi nĭ mèimei, duì bu dui? Tā hĕn kĕ'ài! Tā jĭsuì le?");
	  m.get(u3p2).put(5, "Tā bāsuì. Xiàge yuè jiù jiŭsuì le.");
	  
	  m.get(u3p3).put(0, "Qĭng wèn, zhèige duōshăo qián?");
	  m.get(u3p3).put(1, "Yìbăi jiŭshibākuài.");
	  m.get(u3p3).put(2, "Yò, tài guìle!");
	  m.get(u3p3).put(3, "Zhèige bēizi duōshăo qián?");
	  m.get(u3p3).put(4, "Nèige zhĭ yào sānkuài wŭ.");
	  m.get(u3p3).put(5, "Wŏ kànkan, xíng bu xíng?");
	  m.get(u3p3).put(6, "Xíng, nín kàn ba.");
	  m.get(u3p3).put(7, "Hăo, wŏ măi liăngge.");
	  
	  m.get(u3p4).put(0, "Qĭng wèn, xiàyítàng dào Tiānjīnde huŏchē jĭdiăn kāi?");
	  m.get(u3p4).put(1, "Jiŭdiăn èrshí. Kĕshi xiànzài yĭjīng jiŭdiăn yíkè le, kŏngpà nín láibujíle.");
	  m.get(u3p4).put(2, "Nèmme, zài xiàyítàng ne?");
	  m.get(u3p4).put(3, "Wŏ kànkàn. Zài xiàyítàng shi shídiăn bàn.");
	  m.get(u3p4).put(4, "Hăo. Nà, wŏ jiù zuò shídiăn bànde.");
	  m.get(u3p4).put(5, "Duōshăo qián?");
	  m.get(u3p4).put(6, "Shíyīkuài wŭ.");
	  m.get(u3p4).put(7, "Dào Tiānjīn yào duō cháng shíjiān?");
	  m.get(u3p4).put(8, "Chàbuduō yào liăngge bàn zhōngtóu.");
	  m.get(u3p4).put(9, "Hăo, xièxie nín.");
	  
	  m.get(u4p1).put(0, "Qĭng wèn, yŭyán shíyànshì mĕitiān jĭdiăn zhōng kāimén, jĭdiăn zhōng guānmén?");
	  m.get(u4p1).put(1, "Zăoshang bādiăn kāimén, wănshang jiŭdiăn bàn guānmén.");
	  m.get(u4p1).put(2, "Xīngqīliù kāi bu kāi?");
	  m.get(u4p1).put(3, "Xīngqīliù kāi bàntiān. Shàngwŭ kāi, xiàwŭ bù kāi.");
	  m.get(u4p1).put(4, "Xīngqītiān ne?");
	  m.get(u4p1).put(5, "Xīngqītiān xiūxi.");
	  m.get(u4p1).put(6, "Xièxie nĭ.");
	  m.get(u4p1).put(7, "Náli.");
	  
	  m.get(u4p2).put(0, "Nǐ jiào shémme míngzi?");
	  m.get(u4p2).put(1, "Wŏ jiào Zhāng Wényīng.");
	  m.get(u4p2).put(2, "Zhāng shi gōng cháng Zhāng, wén shi wénhuàde wén, yīng shi Yīngguode yīng.");
	  m.get(u4p2).put(3, "Nĭ shi nĕinián chūshēngde?");
	  m.get(u4p2).put(4, "Yī-jiŭ-bā-líng-nián, jiù shi Mínguó liùshijiŭnián.");
	  m.get(u4p2).put(5, "Jĭyuè jĭhào?");
	  m.get(u4p2).put(6, "Sìyuè shísānhào.");
	  m.get(u4p2).put(7, "Nĭde dìzhĭ shi...");
	  m.get(u4p2).put(8, "Hépíng Dōng Lù yīduàn, èrshiqīxiàng, sānnòng, yībăi wŭshisìhào, bālóu.");
	  m.get(u4p2).put(9, "Hăo, qĭng dĕng yíxià.");
	  
	  m.get(u4p3).put(0, "Nĭ hăo!");
	  m.get(u4p3).put(1, "Nĭ hăo! Nĭ shi Mĕiguo rén ma?");
	  m.get(u4p3).put(2, "Duì, wŏ shi Mĕiguo rén.");
	  m.get(u4p3).put(3, "Zhè shi nĭ dìyīcì dào Zhōngguo lái ma?");
	  m.get(u4p3).put(4, "Bù, zhè shi dì'èrcì. Wŏ qùnián láiguo yícì.");
	  m.get(u4p3).put(5, "M, nĭ zhèicì yào zhù duō jiŭ?");
	  m.get(u4p3).put(6, "Dàyuē bàn'ge yuè. Wŏ shí'èrhào huíguó.");
	  m.get(u4p3).put(7, "Nĭ zhù nĕige fángjiān?");
	  m.get(u4p3).put(8, "Wŏ zhù sān líng liù.");
	  m.get(u4p3).put(9, "Ò, duìbuqĭ, wŏ dĕi zŏule. Zàijiàn!");
	  m.get(u4p3).put(10, "Zàijiàn!");
	  
	  m.get(u4p4).put(0, "Sūn Lăoshī, qĭng wèn, Zhōngguo yŏu duōshăo rén?");
	  m.get(u4p4).put(1, "Zhōngguo chàbuduō yŏu shísānyì rén.");
	  m.get(u4p4).put(2, "Bĕijīng yŏu duōshăo rén?");
	  m.get(u4p4).put(3, "Bĕijīng yŏu yìqiānduōwàn rén.");
	  m.get(u4p4).put(4, "Nèmme, Nánjīng ne?");
	  m.get(u4p4).put(5, "Nánjīng de rénkŏu bĭjiào shăo.");
	  m.get(u4p4).put(6, "Hǎoxiàng zhĭ yŏu wŭbăiwàn.");

     m.get(u5p1).put(0, "Qĭng wèn.");
     m.get(u5p1).put(1, "Duìbuqĭ, wŏ xiăng zhăo yixiar Wáng Xiáojie.");
     m.get(u5p1).put(2, "Qĭng wèn, tā zài bu zai?");
     m.get(u5p1).put(3, "Nĕiwèi Wáng Xiáojie?");
     m.get(u5p1).put(4, "Shízài bàoqiàn.");
     m.get(u5p1).put(5, "Wŏ shi Mĕiguo Huáqiáo, Zhōngwén bú tài hăo.");
     m.get(u5p1).put(6, "Wŏ bù zhīdào tāde Zhōngwén míngzi.");
     m.get(u5p1).put(7, "Búguò, tāde Yīngwén míngzi jiào Mary Wang.");
     m.get(u5p1).put(8, "Ò, wŏ zhīdaole. Shi Wáng Guólì.");
     m.get(u5p1).put(9, "Búguò, tā xiànzài bú zài zhèr.");
     m.get(u5p1).put(10, "Qĭng wèn, nĭ zhīdao tā zài năr ma?");
     m.get(u5p1).put(11, "Tā zài lăobănde bàngōngshì.");
     m.get(u5p1).put(12, "Nà, wŏ kĕ bu kéyi gĕi tā liú yíge tiáozi?");
     m.get(u5p1).put(13, "Dāngrán kéyi.");
     m.get(u5p1).put(14, "Xièxie");

     m.get(u5p2).put(0, "Qĭng wèn, zhèige wèizi yŏu rén ma?");
     m.get(u5p2).put(1, "Méiyou, méiyou. Nín zùo ba.");
     m.get(u5p2).put(2, "Nín cháng lái zhèr chī wŭfàn ma?");
     m.get(u5p2).put(3, "Wŏ cháng lái. Nín ne?");
     m.get(u5p2).put(4, "Wŏ yĕ cháng zài zhèr chī. Nín shi xuésheng ma?");
     m.get(u5p2).put(5, "Bù, wŏ shi gōngrén, zài Bĕijīng Dìyī Píxié Chăng gōngzuò. Nín ne?");
     m.get(u5p2).put(6, "Wŏ zài Mĕiguo shi dàxuéshēng, xiànzài zài zhèr xué Zhōngwén.");
     m.get(u5p2).put(7, "Nín zài năr xuéxí?");
     m.get(u5p2).put(8, "Zài Bĕidàde Hànyŭ péixùn zhōngxīn.");
     m.get(u5p2).put(9, "Yò! Kuài yīdiăn le. Wŏ dĕi zŏule. Zàijiàn!");
     m.get(u5p2).put(10, "Zàijiàn!");

     m.get(u5p3).put(0, "Qĭng wèn, cèsuŏ zài năr?");
     m.get(u5p3).put(1, "Zài nèibianr.");
     m.get(u5p3).put(2, "Xiăo Lĭ, dùibuqĭ, ràng nĭ jiŭ dĕngle.");
     m.get(u5p3).put(3, "Méi shìr, méi shìr. Xiăo Luó, hăo jiŭ bú jiànle!");
     m.get(u5p3).put(4, "Nĭ zhèihuí lái Bĕijīng zhùzai năr?");
     m.get(u5p3).put(5, "Zhùzài Cháng Chéng Fàndiàn, qī-yāo-wŭ-hào fángjiān.");
     m.get(u5p3).put(6, "Nĭ hái shi zhùzai lăo dìfang ma?");
     m.get(u5p3).put(7, "Bù, wŏmen qùnián jiù bāndao Xiāng Shān le.");
     m.get(u5p3).put(8, "Xiāng Shān zài năr?");
     m.get(u5p3).put(9, "Zài Bĕijīng chéngde bĕibiānr.");

     m.get(u5p4).put(0, "Peter, wŏ măile yìtái diànnăo, zài shūfángli.");
     m.get(u5p4).put(1, "Nĭ yào bu yao kànkan?");
     m.get(u5p4).put(2, "Hăo a!");
     m.get(u5p4).put(3, "E, shi yìtái wŭ-bā-liù ma?");
     m.get(u5p4).put(4, "Lĭmiàn yŏu duōshăo RAM?");
     m.get(u5p4).put(5, "Sānshi'èrge.");
     m.get(u5p4).put(6, "Kāiguān yīnggāi zài pángbiān ba?");
     m.get(u5p4).put(7, "Bú duì, zài qiánmian.");
     m.get(u5p4).put(8, "Ò. E, wŏ kĕ bu kéyi kànkan shĭyòng shŏucè?");
     m.get(u5p4).put(9, "Dāngrán kéyĭ.");
     m.get(u5p4).put(10, "Jiù zài nĭ zuŏbiānde nèige shūjiàshang");
     m.get(u5p4).put(11, "ò, bú duì, bú duì, shi zài nĭ hòumiande nèibă yĭzishang.");
     m.get(u5p4).put(12, "Āiyò, zhuōzi dĭxia shi shémme dōngxi a?");
     m.get(u5p4).put(13, "Ò, shi Láifú, wŏmen jiāde gŏu. Nĭ búyào guăn ta.");
	  
	  return m;
  }
}
