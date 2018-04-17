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

package mitll.langtest.server.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/30/15.
 * <p>
 * function minmax(audio_name)
 * [audio_data, fs] = audioread(audio_name);
 * % determine how many samples are in a 50ms window
 * seconds_per_sample = 1/fs;
 * window = floor(.05/seconds_per_sample);
 * slide = floor(window/10);   % number of samples to slide the window
 * a = 1;
 * o = a + window;
 * minrms = 1;
 * maxrms = 0;
 * datasize = size(audio_data);
 * while o < datasize(1)
 * res = rms(audio_data(a:a+slide));
 * if res < .000032    % less than -89.9 dB
 * a = a + slide;
 * o = o + slide;
 * continue;
 * end
 * res = rms(audio_data(o-slide:o));
 * if res < .000032    % less than -89.9 dB
 * a = a + slide;
 * o = o + slide;
 * continue;
 * end
 * res = rms(audio_data(a:o));
 * if res > maxrms
 * maxrms = res;
 * elseif res < minrms
 * minrms = res;
 * end
 * a = a + slide;
 * o = o + slide;
 * end
 * name_size = size(audio_name);
 * stat_name = audio_name(1:name_size(2)-4);
 * stat_name = strcat(stat_name, '-stats.txt');
 * STATID = fopen(stat_name, 'w');
 * <p>
 * fprintf(STATID,'Max-Min Range:\t%.2fdB\n', 20*log10(maxrms)-20*log10(minrms));
 * fprintf(STATID,'Maximum Sample Value:\t%d\n', max(audio_data)*32768);
 * fprintf(STATID,'Minimum Sample Value:\t%d\n', min(audio_data)*32768);
 * fprintf(STATID,'Total RMS Value:\t%.2fdBFS\n', 20*log10(rms(audio_data)));
 * fprintf(STATID,'Minimum RMS Value:\t%.2fdBFS\n', 20*log10(minrms));
 * fprintf(STATID,'Maximum RMS Value:\t%.2fdBFS\n', 20*log10(maxrms));
 */
public class DynamicRange {
  private static final Logger logger = LogManager.getLogger(DynamicRange.class);
  private static final double MAX_VALUE = 32768.0f;
  private static final double SILENCE_THRESHOLD = 0.000032;

  private static final boolean DEBUG = false;

  /**
   * Verify audio messages
   *
   * @param file audio byte array with header
   * @return true if well formed
   * @see AudioCheck#addDynamicRange
   * @see AudioConversion#isValid(File, boolean, boolean)
   */
  public RMSInfo getDynamicRange(File file) {
    AudioInputStream ais = null;
    try {
      ais = AudioSystem.getAudioInputStream(file);
      AudioFormat format = ais.getFormat();
      //    logger.info("file " + file.getName() + " sample rate " + format.getSampleRate());

      boolean bigEndian = format.isBigEndian();
      if (bigEndian) {
        logger.warn("getDynamicRange huh? file " + file.getAbsoluteFile() + " is in big endian format?");
      }
      int fsize = format.getFrameSize();
      assert (fsize == 2);
      assert (format.getChannels() == 1);

      // frames per second
      double frameRate = format.getFrameRate();
      double secPerFrame = 1d / frameRate;

//      logger.info(" frameRate" + frameRate);
//      logger.info(" secPerFrame" + secPerFrame);
      //   logger.info(" fsize" + fsize);

      double floor = Math.floor(.05f / secPerFrame);
      double actualFloor = Math.ceil(floor / 10) * 10;
      int window = Double.valueOf(actualFloor).shortValue();

      int numBufs = 10;
      int slide = window / numBufs;

      double[] bufs = new double[numBufs];
      int currentBuf = 0;

      double minrms = 1;
      double maxrms = 0;

      int bufSize = 4096;//WinSize * fsize;
      byte[] buf = new byte[bufSize];

      short minSample = Short.MAX_VALUE;
      short maxSample = Short.MIN_VALUE;

      int windowCount = 0;

      double lastTotal = 0;
      double windowTotal = 0;
      int lastStart = window - slide;
      double allTotal = 0;
      int allCount = 0;
      int sIndex = 0;

//      logger.info("bufSize " + bufSize);
//      logger.info("fsize " + fsize);
//      logger.info("slide " + slide);
//      logger.info("window " + window);
//      logger.info("lastStart " + lastStart);
      int c = 0;
      int charsRead;
      while ((charsRead = ais.read(buf)) != -1) {
        c++;
        for (int i = 0; i < charsRead; i += fsize)
          for (int s = 0; s < fsize; s += 2) {
            // short tmp = (short) ((buf[i + s] << 8) | buf[i + s + 1]); // BIG ENDIAN
            byte firstByte = buf[i + s];
            byte secondByte = buf[i + s + 1];
            short tmp = (short) ((firstByte & 0xFF) | (secondByte << 8)); // LITTLE ENDIAN

            if (tmp < minSample) {
              minSample = tmp;
              //logger.info("c : at " + sIndex + "\tnow\t" + minSample + " at i " + i + " s " + s + " i + s " + (i+s) + " f\t" + firstByte + " s\t" + secondByte + " second shifted\t" + (secondByte << 8) );
            }
            if (tmp > maxSample) maxSample = tmp;

            double r = ((double) tmp) / MAX_VALUE;

            int bufIndex = sIndex / slide;
            bufIndex = bufIndex % numBufs;

            double squared = r * r;

            allTotal += squared;
            allCount++;

            bufs[bufIndex] += squared;

            if (sIndex > lastStart) {
              lastTotal += squared;
            }

            windowTotal += squared;
            windowCount++;

            sIndex++;

            if (sIndex >= window) {
              if (sIndex % slide == 0) {
                double currTotal = bufs[currentBuf];
                double fres = srms(currTotal, slide);
//
//                logger.info("c " + c + " " + sIndex + " fres " + fres + " currTotal " + currTotal +
//                    " currentBuf " + currentBuf);

                if (fres < SILENCE_THRESHOLD) { // start over
                  if (DEBUG) logger.info("start over first ----------> ");
                } else {
                  double lres = srms(lastTotal, slide);
                  lastTotal = 0;
                  if (lres < SILENCE_THRESHOLD) { // start over
                    if (DEBUG) logger.info("start over last -------> ");
                  } else {
                    double res = srms(windowTotal, window);
  /*                  if (false) {
                      logger.info("c " + c + " " + sIndex +
                          " fres " + fres +
                          " lres " + lres +
                          " res " + res + " currTotal " + currTotal +
                          " currentBuf " + currentBuf + " total " + windowTotal + " " + windowCount + " vs " + window);
                    }*/
                    if (res > maxrms) maxrms = res;
                    else if (res < minrms) minrms = res;
                  }
                }

                windowTotal -= currTotal;
                bufs[currentBuf] = 0; // we've checked this total, now get ready to use it again
                currentBuf = (currentBuf + 1) % numBufs;
              } else {
                //logger.info("skip " +sIndex);
              }
            }
          }
      }

      // logger.info("did " + c);
      //   DecimalFormat decimalFormat = new DecimalFormat("##.##");

//      logger.info("maxrms:\t" + maxrms);
//      logger.info("minrms:\t" + minrms);
//      logger.info("allTotal:\t" + allTotal);
//      logger.info("allCount:\t" + allCount);

      double range = 20 * Math.log10(maxrms) - 20 * Math.log10(minrms);
      double totalRMS = 20 * Math.log10(srms(allTotal, allCount));
      double minRMS = 20 * Math.log10(minrms);
      double maxRMS = 20 * Math.log10(maxrms);

//      logger.info("Max-Min Range:\t" + decimalFormat.format(range) + "dB");
//      logger.info("Maximum Sample Value:\t" + maxSample);
//      logger.info("Minimum Sample Value:\t" + minSample);
//      logger.info("Total RMS Value:\t" + decimalFormat.format(totalRMS) + "dBFS");
//      logger.info("Minimum RMS Value:\t" + decimalFormat.format(minRMS) + "dBFS");
//      logger.info("Maximum RMS Value:\t" + decimalFormat.format(maxRMS) + "dBFS");

      RMSInfo rmsInfo = new RMSInfo(range, maxSample, minSample, totalRMS, minRMS, maxRMS);

//      logger.info("got\n" + rmsInfo);
//      logger.info("dymanic range : " + rmsInfo.getRange());
      return rmsInfo;
    } catch (Exception e) {
      logger.error("Got " + e, e);
    } finally {
      try {
        if (ais != null) ais.close();
      } catch (IOException e) {
        logger.error("Got " + e, e);
      }
    }
    return null;
  }

  private double srms(double rms, double len) {
    return Math.sqrt(rms / len);
  }

  public static class RMSInfo {
    final double maxMin;
    final int max;
    final int min;
    final double totalRMS;
    final double minRMS;
    final double maxRMS;
    final DecimalFormat decimalFormat = new DecimalFormat("##.##");

    public RMSInfo() {
      this(0, 0, 0, 0, 0, 0);
    }

    RMSInfo(double maxMin, int max, int min, double totalRMS, double minRMS, double maxRMS) {
      this.maxMin = maxMin;
      this.max = max;
      this.min = min;
      this.totalRMS = totalRMS;
      this.minRMS = minRMS;
      this.maxRMS = maxRMS;
    }

    public String toString() {
      return "Max-Min Range:\t" +
          getRange() + "\n" +
          "Maximum Sample Value:\t" + max + "\n" +
          "Minimum Sample Value:\t" + min + "\n" +
          "Total RMS Value:\t" + format(totalRMS) + "dBFS\n" +
          "Minimum RMS Value:\t" + format(minRMS) + "dBFS" + "\n" +
          "Maximum RMS Value:\t" + format(maxRMS) + "dBFS";
    }

    public String getRange() {
      return format(maxMin) + "dB";
    }

    private String format(double maxMin) {
      return decimalFormat.format(maxMin);
    }
  }
}
