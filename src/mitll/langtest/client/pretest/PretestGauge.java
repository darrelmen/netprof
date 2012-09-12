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
	private HTML tooltipLabel;

	float[][] colormap = {{255f, 0f, 0f}, {255f, 32f, 0f}, {255f, 64f, 0f}, {255f, 128f, 0f}, {255f, 192f, 0f}, {255f, 255f, 0f},             
		{192f, 255f, 0f}, {128f, 255f, 0f}, {64f, 255f, 0f}, {32f, 255f, 0f}, {32f, 255f, 0f}, {32f, 255f, 0f}, {32f, 255f, 0f},
		{0f, 255f, 0f}, {0f, 255f, 0f}, {0f, 255f, 0f}};

	public PretestGauge(String id, String instructions){
		this.id = id;

		setHTML("<div id='" + id + "Container' style='align:center; text-align: center'/>");

		tooltip = new PopupPanel();
		tooltip.setWidth("350px");
		tooltipLabel = new HTML("<div style='font-size: 10pt'>" + instructions + "</div>");

		tooltip.setStyleName("TooltipPopup");
		tooltipLabel.setStyleName("Tooltip");

		tooltip.setWidget(tooltipLabel);
		

		addMouseOverHandler(new PretestGaugeMouseOverHandler());
		addMouseOutHandler(new PretestGaugeMouseOutHandler());
	}

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
			sectOneColor: '#FF1600',
			sectTwoFrom:5,
			sectTwoTo:10,
			sectTwoColor: '#FF2D00',
			sectThreeFrom:10,
			sectThreeTo:15,
			sectThreeColor: '#FF4600',
			sectFourFrom:15,
			sectFourTo: 20,
			sectFourColor: '#FF7300',
			sectFiveFrom: 20,
			sectFiveTo: 25,
			sectFiveColor: '#FFA000',
			sectSixFrom: 25,
			sectSixTo: 30,
			sectSixColor: '#FFCD00',
			sectSevenFrom: 30,
			sectSevenTo: 35,
			sectSevenColor: '#FFF900',
			sectEightFrom: 35,
			sectEightTo: 40,
			sectEightColor: '#D9FF00',
			sectNineFrom: 40,
			sectNineTo: 45,
			sectNineColor: '#ADFF00',
			sectTenFrom: 45,
			sectTenTo: 50,
			sectTenColor: '#80FF00',
			sectElevenFrom: 50,
			sectElevenTo: 55,
			sectElevenColor: '#53FF00',
			sectTwelveFrom: 55,
			sectTwelveTo: 60,
			sectTwelveColor: '#33FF00',
			sectThirteenFrom: 60,
			sectThirteenTo: 65,
			sectThirteenColor: '#20FF00',
			sectFourteenFrom: 65,
			sectFourteenTo: 70,
			sectFourteenColor: '#20FF00',
			sectFifteenFrom: 70,
			sectFifteenTo: 75,
			sectFifteenColor: '#20FF00',
			sectSixteenFrom: 75,
			sectSixteenTo: 80,
			sectSixteenColor: '#20FF00',
			sectSeventeenFrom: 80,
			sectSeventeenTo: 85,
			sectSeventeenColor: '#20FF00',
			sectEighteenFrom: 85,
			sectEighteenTo: 90,
			sectEighteenColor: '#0DFF00',
			sectNineteenFrom: 90,
			sectNineteenTo: 95,
			sectNineteenColor: '#00FF00',
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

	public String getColor(float score){ 
		float nf = Math.max(score, 0.0f) * (float) (colormap.length - 2);
		int idx = (int) Math.floor(nf);
		int[] color = {0, 0, 0};
		for (int cc = 0; cc < 3; cc++){
			color[cc] = Math.round((colormap[idx + 1][cc] - colormap[idx][cc]) * (nf - (float) idx) + colormap[idx][cc]);
		}

		return "#" + getHexNumber(color[0]) + getHexNumber(color[1]) + getHexNumber(color[2]);
	}

	public String getHexNumber(int number){
		String hexString = Integer.toHexString(number).toUpperCase();

		if(hexString.length() == 0){
			return "00";
		}
		else if(hexString.length() == 1){
			return "0" + hexString;
		}
		else{
			return hexString;
		}
	}
}
