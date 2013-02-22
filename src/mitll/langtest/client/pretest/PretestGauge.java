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
 * @author gregbramble, sharontam
 *
 */
public class PretestGauge extends HTML{
	private String id;
	private JavaScriptObject canvasObject, gaugeObject;

	private PopupPanel tooltip;

/*	float[][] colormap = {
      {255f, 0f, 0f},
      {255f, 32f, 0f},
      {255f, 64f, 0f},
      {255f, 128f, 0f},
      {255f, 192f, 0f},
      {255f, 255f, 0f},
      {192f, 255f, 0f},
      {128f, 255f, 0f},
      {64f, 255f, 0f},
      {32f, 255f, 0f},
      {32f, 255f, 0f},
      {32f, 255f, 0f},
      {32f, 255f, 0f},
      {0f, 255f, 0f},
      {0f, 255f, 0f},
      {0f, 255f, 0f}};*/

  /**
   * @see mitll.langtest.client.gauge.ASRScorePanel
   * @param id
   * @param instructions
   */
	public PretestGauge(String id, String instructions){
		this.id = id;

		setHTML("<div id='" + id + "Container' style='align:center; text-align: center'/>");

		tooltip = new PopupPanel();
		tooltip.setWidth("350px");
    HTML tooltipLabel = new HTML("<div style='font-size: 10pt'>" + instructions + "</div>");

		tooltip.setStyleName("TooltipPopup");
		tooltipLabel.setStyleName("Tooltip");

		tooltip.setWidget(tooltipLabel);

		addMouseOverHandler(new PretestGaugeMouseOverHandler());
		addMouseOutHandler(new PretestGaugeMouseOutHandler());
	}

/*  public String getColor(float score){
    if (score > 1.0f) {
      Window.alert("ERROR: getColor: score > 1");
      return "#000000";
    }
    float nf = Math.max(score, 0.0f) * (float) (colors.length-1);
    int idx = (int) Math.floor(nf);
   // System.out.println("score " + score + " colors " + colors.length + " " + idx);
    if (idx > colors.length-1) {
      System.out.println("huh? score " + score + " colors " + colors.length + " " + idx + " had to make sure it didn't go over");

      idx = colors.length-1;
    }
    return colors[idx];
  }*/

	public native void createCanvasElement() /*-{
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
			label: this.@mitll.langtest.client.pretest.PretestGauge::id,
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

	public native float getValue() /*-{
		return this.@mitll.langtest.client.pretest.PretestGauge::gaugeObject.getValue()
	}-*/;

	private class PretestGaugeMouseOverHandler implements MouseOverHandler{
		/* (non-Javadoc)
		 * @see com.google.gwt.event.dom.client.MouseOverHandler#onMouseOver(com.google.gwt.event.dom.client.MouseOverEvent)
		 */
		//@Override
		public void onMouseOver(final MouseOverEvent event){
			tooltip.setPopupPositionAndShow(new PositionCallback(){
				//@Override
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
