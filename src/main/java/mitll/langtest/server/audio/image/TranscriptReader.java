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

package mitll.langtest.server.audio.image;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Creates transcript events from a MLF file.
 *
 * @see <a href="http://www.icsi.berkeley.edu/speech/docs/HTKBook/node99_ct.html">MLF file format examples</a>
 *      <p>
 *      <p>
 *      And specifically we can handle files that look like the following,
 *      skipping phonemes, unless <code>showPhones</code> is true.
 *      <p>
 *      <pre>
 *           "'star'/sll-in_10_2.lab" - label reference
 *           1800000 3400000 THE  - word with start/end offset
 *           1800000 2400000 [dh] - phoneme with start/end offset
 *           ... more lines ...
 *           .                    - end of segment marker
 *           </pre>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 6/24/11
 *      Time: 6:35 PM
 */
public class TranscriptReader {
//  private static Logger logger = LogManager.getLogger(TranscriptReader.class);

  /**
   * Converts MLF units to seconds
   */
 // private static final int MLF_OFFSET_UNITS = 10000000;
 // private static final boolean showPhones = false; // don't show phonemes if also have words
  private final Pattern p = Pattern.compile("\\s+");    /// Splitter for fields in transcript
  private final Pattern semi = Pattern.compile(";");

  /**
   * @see mitll.langtest.server.audio.imagewriter.TranscriptWriter#getImageTypeMapMap(Map, boolean, Map)
   * @param labString
   * @param usePhone
   * @param phoneToDisplay
   * @return
   */
  public SortedMap<Float, TranscriptEvent> readEventsFromString(String labString, boolean usePhone, Map<String,String> phoneToDisplay) {
    // format: start end event;start end event;...
    String[] split = semi.split(labString);
    SortedMap<Float, TranscriptEvent> events = new TreeMap<Float, TranscriptEvent>();
    for (String lab : split) {
      String[] fields = p.split(lab);
      assert (fields.length == 3 || fields.length == 4);
      String startTime = fields[0];
      String endTime = fields[1];
      float start = Float.valueOf(startTime), end = Float.valueOf(endTime);
      // TODO jess for alignment this will probably need to have phone scores also
      String event = fields[2];
      if (usePhone) {
        //    String before = event;
        event = getDisplayPhoneme(phoneToDisplay, event);
        //    logger.debug("before " + before + " after " + event);
      }

      events.put(start, new TranscriptEvent(start, end, event, fields.length == 4 ? Float.valueOf(fields[3]) : 0.0f));
    }
    return events;
  }

  private String getDisplayPhoneme(Map<String,String> phoneToDisplay,String phone) {
    String s = phoneToDisplay.get(phone);
    if (s == null) return phone;
    else return s;
  }

/*  public SortedMap<Float, TranscriptEvent> readEventsFromFile(String tfn, boolean usePhone, Map<String,String> phoneToDisplay) throws IOException {
    BufferedReader f = getReader(tfn);
    String ev;

    SortedMap<Float, TranscriptEvent> events = new TreeMap<Float, TranscriptEvent>();
    while ((ev = f.readLine()) != null) {
      String[] fields = p.split(ev);
      assert (fields.length == 3 || fields.length == 4);
      String startTime = fields[0];
      if (startTime.length() == 0) {
//        logger.error("huh? reading " + tfn + " got a line that has a blank start time " + ev);
      } else {
        String endTime = fields[1];
        if (endTime.length() == 0) {
  //        logger.error("huh? reading " + tfn + " got a line that has a blank end time " + ev);
        } else {
          float start = Float.valueOf(startTime), end = Float.valueOf(endTime);
          String event = fields[2];
          if (usePhone) event = getDisplayPhoneme(phoneToDisplay, event);
          events.put(start, new TranscriptEvent(start, end, event, fields.length == 4 ? Float.valueOf(fields[3]) : 0.0f));
        }
      }
    }
    f.close();
    return events;
  }
  */

  /**
   * @deprecatedx
   * @param tfn
   * @param filteredLabel
   * @return
   * @throws java.io.IOException
   */
/*  private Map<Float, TranscriptEvent> readEventsFromAlignFile(String tfn, String filteredLabel) throws IOException {
    return readEventsFromAlignFile(tfn, filteredLabel, false);
  }*/

/*  public Map<Float, TranscriptEvent> readEventsFromAlignFile(String tfn, String filteredLabel, boolean phonesP) throws IOException {
    BufferedReader f = getReader(tfn);
    String ev;
    Pattern utteranceMatcher = Pattern.compile("^(.*)\\s*\\(name: ([^,]+), start: \\d+\\.\\d+, end: \\d+\\.\\d+\\)$");
    Map<Float, TranscriptEvent> events = new TreeMap<Float, TranscriptEvent>();
    while ((ev = f.readLine()) != null) {
      ev = ev.trim();
      if (ev.startsWith("File: ") || ev.equals("END")) continue;
      //System.out.println("line " + ev);
      Matcher matcher = utteranceMatcher.matcher(ev);
      boolean b = matcher.find();
      if (!b) {
        System.err.println("Trouble parsing " +tfn + ".");
        continue;
      }
      String eventsGroup = matcher.group(1);
      String labelInfo   = matcher.group(2);
      //System.out.println("g1 " + eventsGroup);
      //System.out.println("g2 " + labelInfo);

      if (labelInfo.equals(filteredLabel)) {
        String[] split = p.split(eventsGroup);
        for (String event : split) {
          String[] parts = event.split(":");

          float start = Float.parseFloat(parts[1]);
          float end = Float.parseFloat(parts[2]);
          String token = parts[0];
          boolean include = (phonesP && token.startsWith("[") && token.endsWith("]")) ||
            (!phonesP && !token.startsWith("[") && !token.endsWith("]"));
          if (include) {
            String s = phonesP ? token.replaceFirst("^\\[", "").replaceFirst("\\]$", "") : token;
            TranscriptEvent transcriptEvent =
              parts.length == 4 ?
              new TranscriptEvent(start, end, s, Float.parseFloat(parts[3])) :
              new TranscriptEvent(start, end, s);
            events.put(transcriptEvent.start, transcriptEvent);
          }
        }
      }
    }
    f.close();
    return events;
  }*/

  /**
   * Smart enough to use UTF-8 character set.  Should work with Arabic.
   *
   * @param tfn file to read from
   * @return a reader we can use to read each line of the tile
   * @throws UnsupportedEncodingException
   * @throws FileNotFoundException
   */
 /* private BufferedReader getReader(String tfn) throws UnsupportedEncodingException, FileNotFoundException {
    return new BufferedReader(new InputStreamReader(new FileInputStream(tfn),"UTF-8"));
  }*/

  /**
   * @deprecated
   * @param tfn
   * @param sampleRate
   * @param label
   * @return
   * @throws java.io.IOException
   */
/*  public Map<Float, TranscriptEvent> readEventsFromMLFForOneLabel(String tfn, float sampleRate, String label)
    throws IOException {
    List<Segment> segments = new ArrayList<Segment>(1);
    segments.add(new Segment(label));
    return readEventsFromMLFFile(tfn, sampleRate, segments);
  }*/

  /**
   * Filters down to just the current audio file.
   * <p/>
   * Parses mlf file that looks like:
   * <pre>
   * "'star'/sll-in_10_2.lab" - label reference
   * 1800000 3400000 THE  - word with start/end offset
   * 1800000 2400000 [dh] - phoneme with start/end offset
   * ... more lines ...
   * .                    - end of segment marker
   * </pre>
   *
   * All we need from the segment is the label and the start offset.
   *
   * @deprecated we don't use mlf files anymore
   * @param tfn        file name of mlf file
   * @param sampleRate need this since the offsets in the mlf files are in terms of samples (not seconds)
   * @param segments   segments in the analist file
   * @return Map of start of event to a {@link TranscriptEvent}
   * @throws java.io.IOException if we can't read from the file
   * @deprecated
   */

/*  private Map<Float, TranscriptEvent> readEventsFromMLFFile(String tfn, float sampleRate, Collection<Segment> segments)
    throws IOException {
    BufferedReader f = getReader(tfn);
    String ev = f.readLine();
    if (!ev.equals("#!MLF!#")) {
      throw new IllegalArgumentException("Expecting MLF file, but " + tfn + " didn't have expected header");
    }

    Map<String, Segment> labelToSegment = new HashMap<String, Segment>();
    for (Segment s : segments) {
      labelToSegment.put(s.label, s);
    }

    Map<Float, TranscriptEvent> events = new TreeMap<Float, TranscriptEvent>();

    float secPerSample = 1.0f / sampleRate;

    boolean inIncludedLabel = false;

    float labelStartOffset = 0;
    while ((ev = f.readLine()) != null) {
      if (ev.endsWith(".lab\"")) {
        String label = ev.substring(ev.indexOf("/") + 1, ev.indexOf(".")); // match "*   /sw_40016_120.lab"
        inIncludedLabel = labelToSegment.keySet().contains(label);
        if (inIncludedLabel) {
          Segment segment = labelToSegment.get(label);
          if (segment == null) {
            System.err.println("huh? mlf label " + label + " not in segments : keys " + labelToSegment.keySet());
          } else {
            int labelStart = segment.start;
            labelStartOffset = ((float) labelStart) * secPerSample;
          }
        }
      } else if (inIncludedLabel) {
        if (ev.equals(".")) {
          inIncludedLabel = false;
        } else {
          addMLFEvent(ev, events, labelStartOffset);
        }
      }
    }
    f.close();
    return events;
  }*/

/*
  private void addMLFEvent(String ev, Map<Float, TranscriptEvent> events, float labelStartOffset) {
    String[] fields = p.split(ev);
    assert (fields.length == 3);
    float start = Float.valueOf(fields[0]), end = Float.valueOf(fields[1]);
    start /= MLF_OFFSET_UNITS;
    end /= MLF_OFFSET_UNITS;
    start += labelStartOffset;
    end += labelStartOffset;
    String phone = fields[2];
    if (phone.equals("sil")) {
      phone = "#";
    }

    boolean include = showPhones || !phone.startsWith("[");

    if (include) {
      TranscriptEvent transcriptEvent = new TranscriptEvent(start, end, phone);
      events.put(start, transcriptEvent);
    }
  }
*/

}
