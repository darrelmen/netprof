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

package mitll.langtest.client.table;

/**
 * ColorFactory
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since Apr 4, 2013 2:00:27 PM
 *
 * TODO : remove me
 */
public class ColorFactory {
	
	
	/*public static final String[] FG_BW_COLOR_CODES = {
		"#000000", "#444444", "#5D5D5D", "#999999", "#CCCCCC", "#EEEEEE", "#F3F3F3", "#FFFFFF" };
	
	public static final String[] BG_BW_COLOR_CODES = {
		"#000000", "#444444", "#5D5D5D", "#CCCCCC", "#EEEEEE", "#F3F3F3", "#FFFFFF", null };

	public static final String[] PRIMARY_COLOR_CODES = {
		"#FF0000", "#FF9900", "#FFFF00", "#00FF00", "#00FFFF", "#0000FF", "#9900FF", "#FF00FF" };
	
	public static final String[] GRADED_COLOR_CODES = { 
		"#F4CCCC", "#FCE5CD", "#FFF2CC", "#D9EAD3", "#D0E0E3", "#CFE2F3", "#D9D2E9", "#EAD1DC",
		"#EA9999", "#F9CB9C", "#FFE599", "#B6D7A8", "#A2C4C9", "#9FC5E8", "#B4A7D6", "#D5A6BD",
		"#E06666", "#F6B26B", "#FFD966", "#93C47D", "#76A5AF", "#6FA8DC", "#8E7CC3", "#C27BA0",
		"#CC0000", "#E69138", "#F1C232", "#6AA84F", "#45818E", "#3D85C6", "#674EA7", "#A64D79",
		"#990000", "#B45F06", "#BF9000", "#38761D", "#134F5C", "#0B5394", "#351C75", "#741B47",
		"#660000", "#783F04", "#7F6000", "#274E13", "#0C343D", "#073763", "#20124D", "#4C1130"
	};
	protected static final Logger log = Logger.getLogger(ColorFactory.class.getName());

	private int startingUserColorIndex = 10; // starting the middle and work backwards to use lighter colors.
	private int nextUserColorIndex = startingUserColorIndex;
	private int startingTaskColorIndex = 23;
	private int nextTaskColorIndex = startingTaskColorIndex;
	private Map<Integer, String> userColorCache = new HashMap<Integer, String>();
	private Map<String, String> taskColorCache = new HashMap<String, String>();
	
	public ColorFactory() {}
	
	public String getUserColor(int userDBId) {
		String mappedColor = userColorCache.get(userDBId);
		if (mappedColor == null) {
			mappedColor = GRADED_COLOR_CODES[nextUserColorIndex];
			userColorCache.put(userDBId, mappedColor);
			if (nextUserColorIndex <= 0) {
				nextUserColorIndex = startingUserColorIndex+1; // when we hit the bottom, start again above the start index.
			} else if (nextUserColorIndex >= GRADED_COLOR_CODES.length-1) {
				nextUserColorIndex = startingUserColorIndex; // start again at the first after running through the full list.
			} else if (nextUserColorIndex > startingUserColorIndex) {
			 	nextUserColorIndex++; // increase when above the starting index.
			} else {
				nextUserColorIndex--; // decrease when below the starting index.
			}
		}
		return mappedColor;
	}
	
	public String getTaskColor(String taskName) {
		if (taskName == null || taskName.equals("")) {
			return "#FFFFCC"; // default yellow
		}
		
		String mappedColor = taskColorCache.get(taskName);
		if (mappedColor == null) {
			mappedColor = GRADED_COLOR_CODES[nextTaskColorIndex];
			taskColorCache.put(taskName, mappedColor);
			if (nextTaskColorIndex <= 0) {
				nextTaskColorIndex = startingTaskColorIndex+1; // when we hit the bottom, start again above the start index.
			} else if (nextTaskColorIndex >= GRADED_COLOR_CODES.length-1) {
				nextTaskColorIndex = startingTaskColorIndex; // start again at the first after running through the full list.
			} else if (nextTaskColorIndex > startingTaskColorIndex) {
			 	nextTaskColorIndex++; // increase when above the starting index.
			} else {
				nextTaskColorIndex--; // decrease when below the starting index.
			}
		}
		return mappedColor;
	}
	
	*//**
	 * Convert an RGB formatted color string into a Hexadecimal.
	 * @param rgbString The rgb string -- i.e., rgb(255, 255, 255)
	 * @return The Hex string -- i.e., #ffffff 
	 *//*
	public String rgbToHex(String rgbString) {
		int lIdx = rgbString.indexOf("(");
		int rIdx = rgbString.indexOf(")");
		if (rgbString.startsWith("rgb") && lIdx >= 0 && rIdx >= 0) {
			try {
				String numString = rgbString.substring(lIdx+1, rIdx);
				//rgbString.subSequence(arg0, arg1)
				String[] splits = numString.split(",");
				String result = "#";
				
				if (splits.length == 3) {
					for (int i = 0; i < splits.length; i++) {
						int intVal = Integer.parseInt(splits[i].trim());
						String hexStr = Integer.toHexString(intVal);
						if (hexStr.length() == 1) {
							hexStr = "0" + hexStr;
						}
						result += hexStr;
					}
					return result;
				}
			} catch (Exception ex) {
				log.warning("Can not convert string " + rgbString);
				return rgbString;
			}
		}
		log.warning("Can not convert string " + rgbString);
		return rgbString;
	}*/

}
