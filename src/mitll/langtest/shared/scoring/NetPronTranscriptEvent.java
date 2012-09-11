/**
 * 
 */
package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * @author gregbramble
 * @deprecated
 */
public class NetPronTranscriptEvent implements IsSerializable {
//	private static final long serialVersionUID = 8280463551519144287L;

	private float start, end;                    // Start / End time in seconds
//	private String event;                 		 // Text to be displayed per event
//	private float score;                  		 // score

	public NetPronTranscriptEvent(){
		
	}
	
/*	public NetPronTranscriptEvent(float start, float end, String event, float score){
		this.start = start;
		this.end = end;
		this.event = event;
		this.score = score;
	}*/
	
	public boolean fallsWithin(double time){
    return (time >= start) && (time <= end);
	}
	
	public float getStart(){
		return start;
	}
	
	public float getEnd(){
		return end;
	}
}
