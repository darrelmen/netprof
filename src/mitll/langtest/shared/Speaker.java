package mitll.langtest.shared;


/**
 * A speaker in a dialog
 *
 * User: JE24276
 * Date: 2/10/15
 */
public class Speaker {

	private String name = "";
	private Dialog dialog = null;
	
	public Speaker(String name, Dialog dialog){
		this.name = name;
		this.dialog = dialog;
	}
	
	public String getName(){
		return name;
	}
	
	public Dialog getDialog(){
		return dialog;
	}
	
}
