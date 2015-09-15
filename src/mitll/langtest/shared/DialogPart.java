package mitll.langtest.shared;


/**
 * A part in a dialog
 *
 * User: JE24276
 * Date: 2/10/15
 */
public class DialogPart {

	private Speaker speaker = null;
	private Exercise ex = null;
	private String regularAudioPath = null;
	private String slowAudioPath = null;
	private String text = null;
	
	public DialogPart(Speaker speaker, Exercise ex){
		this.speaker = speaker;
		this.ex = ex;
		this.regularAudioPath = ex.getRegularSpeed().getAudioRef();
		this.slowAudioPath = ex.getSlowSpeed().getAudioRef();
		this.text = ex.getContent();
	}
	
	public Speaker getSpeaker(){
		return speaker;
	}
	
	public String getRegularAudioPath(){
		return regularAudioPath;
	}
	
	public String getSlowAudioPath(){
		return slowAudioPath;
	}
	
	public String getText(){
		return text;
	}
}
