/**
 * 
 */
package mitll.langtest.client.goodwave;

import com.goodwave.client.GoodWavePanel;
import com.goodwave.client.songimage.SongImageManagerPanel;
import com.goodwave.client.songimage.SongImageManagerPanel.GoodWaveImageType;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gregbramble
 *
 */
public class HidePanelsControlPanel extends HorizontalPanel{
  private GoodWavePanel parent;   // TODO remove
  private SongImageManagerPanel managerPanel;
  private Map<GoodWaveImageType, Boolean> checked = new HashMap<GoodWaveImageType, Boolean>();
	private Collection<GoodWaveImageType> hideableImageTypes;

  public HidePanelsControlPanel(GoodWavePanel parent){   // TODO remove
    this.parent = parent;
  }

  public HidePanelsControlPanel(SongImageManagerPanel parent){
    this.managerPanel = parent;
  }

  /**
   * @see com.goodwave.client.GoodWavePanel#setGoodWaveAudioInfo(com.goodwave.shared.GoodWaveAudioInfo)
   * @param imageTypes
   */
	public void init(Collection<GoodWaveImageType> imageTypes){
		clear();
		
		hideableImageTypes = imageTypes;
		
		for(GoodWaveImageType type: hideableImageTypes){
			CheckBox checkbox = new CheckBox();
			checkbox.setValue(true);			//default to all checks because we add all elsewhere  TODO: save this in cookies and tie it in to the code that adds the panels
			checked.put(type, true);
			checkbox.addValueChangeHandler(new CheckBoxValueChangeHandler(type));
			add(checkbox);
			
			switch(type){
			case WAVEFORM: 
				add(new Label("Waveform"));
				continue;
			case SPECTROGRAM: 
				add(new Label("Spectrogram"));
				continue;
			case WORD_TRANSCRIPT: 
				add(new Label("Word Transcript"));
				continue;
			case SPEECH_TRANSCRIPT: 
				add(new Label("Speech Transcript"));
				continue;
			case PHONE_TRANSCRIPT: 
				add(new Label("Phone Transcript"));
				continue;
			default: add(new Label(""));
		}
			//add(new Label(type.toString()));
		}
	}
	
	public void enforceCheckedBoxes(){
		for(GoodWaveImageType type: hideableImageTypes){
			if (parent == null) {
        managerPanel.setSongImagePanelVisible(type, checked.get(type));
      }
      else {
        parent.setSongImagePanelVisible(type, checked.get(type));

      }
		}
	}
	
	private class CheckBoxValueChangeHandler implements ValueChangeHandler<Boolean>{
		private GoodWaveImageType type;
		
		public CheckBoxValueChangeHandler(GoodWaveImageType type){
			this.type = type;
		}
		
		public void onValueChange(ValueChangeEvent<Boolean> event){
      if (parent == null) {
        managerPanel.setSongImagePanelVisible(type, event.getValue());
        managerPanel.placeImageOverlays();
      }
      else {
        parent.setSongImagePanelVisible(type, event.getValue());
        parent.placeImageOverlays();
      }

		}
	}
}
