package mitll.langtest.shared;

import java.util.List;


/**
 * A dialog
 *
 * User: JE24276
 * Date: 2/10/15
 */
public class Dialog {

	private String tag = "";
	private List<Speaker> speakers = null;
	private List<DialogPart> parts = null;
	
	public Dialog(String tag, List<Speaker> speakers, List<DialogPart> parts){
		this.tag = tag;
	}
	
	public String getTag(){
		return tag;
	}
	
	public List<Speaker> getSpeakers(){
		return speakers;
	}
	
	public List<DialogPart> getParts(){
		return parts;
	}

}
