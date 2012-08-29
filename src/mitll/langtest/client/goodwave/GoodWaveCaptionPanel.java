package mitll.langtest.client.goodwave;

import com.goodwave.client.GoodWavePanel;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;

public class GoodWaveCaptionPanel extends CaptionPanel implements ProvidesResize,RequiresResize{

	public GoodWavePanel getChild() {
		return child;
	}

	public void setChild(GoodWavePanel child) {
		this.child = child;
	}

	private GoodWavePanel child;
	
	public GoodWaveCaptionPanel(String name, GoodWavePanel child){
		super(name);
		this.child = child;
		child.setHeight("100%");
	}

	public void onResize(){
		child.setHeight("100%");
		this.child.onResize();
	}

}
