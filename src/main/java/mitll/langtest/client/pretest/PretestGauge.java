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

/**
 * 
 */
package mitll.langtest.client.pretest;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

/**
 * This is evil - easy to get it to draw a gauge that is squished 50% horizontally.
 * Only the way it works here allows it to do normal width.
 * Updating the library to gauge.js 4.6 breaks the coloring...
 * Sigh.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 *
 */
public class PretestGauge extends HTML{
  private final String id;                      // referenced in native js
  private final String label;                      // referenced in native js
  private JavaScriptObject canvasObject;  // referenced in native js
  private JavaScriptObject gaugeObject;   // referenced in native js

	private final PopupPanel tooltip;

  /**
   * @see mitll.langtest.client.gauge.ASRScorePanel#ASRScorePanel(String, mitll.langtest.client.exercise.ExerciseController, Exercise)
   * @paramx id
   */
	public PretestGauge(String id){
		this.id = id;
    this.label = "ASR";

		setHTML("<div id='" + id + "Container' style='align:center; text-align: center'/>");

		tooltip = new PopupPanel();
		tooltip.setWidth("350px");
    HTML tooltipLabel = new HTML("<div style='font-size: 10pt'>" + mitll.langtest.client.gauge.ASRScorePanel.INSTRUCTIONS + "</div>");

		tooltip.setStyleName("TooltipPopup");
		tooltipLabel.setStyleName("Tooltip");

		tooltip.setWidget(tooltipLabel);

    this.addMouseOverHandler(new PretestGaugeMouseOverHandler());
    this.addMouseOutHandler(new PretestGaugeMouseOutHandler());
	}

  /**
   * @see mitll.langtest.client.gauge.ASRScorePanel#onLoad()
   */
	public native void createCanvasElementOld() /*-{
    var wrapper = $doc.getElementById(this.@mitll.langtest.client.pretest.PretestGauge::id + "Container");
		this.@mitll.langtest.client.pretest.PretestGauge::canvasObject = $doc.createElement('canvas');
		this.@mitll.langtest.client.pretest.PretestGauge::canvasObject.setAttribute('width', 110);
		this.@mitll.langtest.client.pretest.PretestGauge::canvasObject.setAttribute('height', 110);
		wrapper.appendChild(this.@mitll.langtest.client.pretest.PretestGauge::canvasObject);

		if(typeof($wnd.G_vmlCanvasManager) != 'undefined'){		//we're in IE
			$wnd.G_vmlCanvasManager.initElement(this.@mitll.langtest.client.pretest.PretestGauge::canvasObject);
			//var ctx = this.@mitll.langtest.client.pretest.PretestGauge::canvasObject.getContext('2d');
		}
	}-*/;

  public native void createCanvasElement(String id) /*-{
      this.@mitll.langtest.client.pretest.PretestGauge::canvasObject = $doc.getElementById(id);
  }-*/;

  /**
   * Note the colors used here were generated from audioImage:
   * audio.image.TranscriptImage#generateColors
   */
	//call this after adding the widget to the page
	public native void initialize() /*-{
		var options;

		
		// Draw the gauge using custom settings (medium)
		options = {
			value: 0,
			label: this.@mitll.langtest.client.pretest.PretestGauge::label,
			unitsLabel: ' %',
			min: 0,
			max: 100,
			majorTicks: 5,
			minorTicks: 4, // small ticks inside each major tick
			sectOneFrom:0,
			sectOneTo:5,
			sectOneColor: '#FF0000',
			sectTwoFrom:5,
			sectTwoTo:10,
			sectTwoColor: '#FF1A00',
			sectThreeFrom:10,
			sectThreeTo:15,
			sectThreeColor: '#FF3500',
			sectFourFrom:15,
			sectFourTo: 20,
			sectFourColor: '#FF5000',
			sectFiveFrom: 20,
			sectFiveTo: 25,
			sectFiveColor: '#FF6B00',
			sectSixFrom: 25,
			sectSixTo: 30,
			sectSixColor: '#FF8600',
			sectSevenFrom: 30,
			sectSevenTo: 35,
			sectSevenColor: '#FFA100',
			sectEightFrom: 35,
			sectEightTo: 40,
			sectEightColor: '#FFBB00',
			sectNineFrom: 40,
			sectNineTo: 45,
			sectNineColor: '#FFD600',
			sectTenFrom: 45,
			sectTenTo: 50,
			sectTenColor: '#FFF100',
			sectElevenFrom: 50,
			sectElevenTo: 55,
			sectElevenColor: '#F1FF00',
			sectTwelveFrom: 55,
			sectTwelveTo: 60,
			sectTwelveColor: '#D6FF00',
			sectThirteenFrom: 60,
			sectThirteenTo: 65,
			sectThirteenColor: '#BBFF00',
			sectFourteenFrom: 65,
			sectFourteenTo: 70,
			sectFourteenColor: '#A1FF00',
			sectFifteenFrom: 70,
			sectFifteenTo: 75,
			sectFifteenColor: '#86FF00',
			sectSixteenFrom: 75,
			sectSixteenTo: 80,
			sectSixteenColor: '#6BFF00',
			sectSeventeenFrom: 80,
			sectSeventeenTo: 85,
			sectSeventeenColor: '#50FF00',
			sectEighteenFrom: 85,
			sectEighteenTo: 90,
			sectEighteenColor: '#35FF00',
			sectNineteenFrom: 90,
			sectNineteenTo: 95,
			sectNineteenColor: '#1AFF00',
			sectTwentyFrom: 95,
			sectTwentyTo: 100,
			sectTwentyColor: '#00FF00'
		};
		
		this.@mitll.langtest.client.pretest.PretestGauge::gaugeObject = new $wnd.Gauge(this.@mitll.langtest.client.pretest.PretestGauge::canvasObject, options);
	}-*/;

	public native void setValue(float value) /*-{
		this.@mitll.langtest.client.pretest.PretestGauge::gaugeObject.setValue(value);
	}-*/;

	private class PretestGaugeMouseOverHandler implements MouseOverHandler{
		/* (non-Javadoc)
		 * @see com.google.gwt.event.dom.client.MouseOverHandler#onMouseOver(com.google.gwt.event.dom.client.MouseOverEvent)
		 */
		public void onMouseOver(final MouseOverEvent event){
			tooltip.setPopupPositionAndShow(new PositionCallback(){
				public void setPosition(int offsetWidth, int offsetHeight){
					int x = event.getClientX();
					int y = event.getClientY() + 20;

					if((x + offsetWidth) > Window.getClientWidth()){
						x = Window.getClientWidth() - offsetWidth;
					}

					if((y + offsetHeight) > Window.getClientHeight()){
						y = Window.getClientHeight() - offsetHeight;
					}

					tooltip.setPopupPosition(x, y);	
				}		
			});
		}
	}

	private class PretestGaugeMouseOutHandler implements MouseOutHandler{
		/* (non-Javadoc)
		 * @see com.google.gwt.event.dom.client.MouseOutHandler#onMouseOut(com.google.gwt.event.dom.client.MouseOutEvent)
		 */
		//@Override
		public void onMouseOut(MouseOutEvent event){
			tooltip.hide();
		}
	}
}
