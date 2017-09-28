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

import java.awt.*;

/**
 * Draws a waveform for an audio file.  Marks with gridlines and time labels.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/26/11
 * Time: 5:18 PM
 */
public class WaveformImage extends AudioImage {
  private static final Color DEFAULT_PAINT = new Color(255, 255, 10);
  private static final Color DEFAULT_RMS_PAINT = new Color(210, 210, 30);
  private static final boolean DEFAULT_DRAW_GRID_LINES = false;
  private static final boolean DEFAULT_DRAW_TICKS = false;
//  private static final float DEFAULT_TIME_GRID_LINE_FRACTION = 0.1f;

  private static final String TWO_DECIMAL_LABEL_FORMAT = "%.2f";
  private static final String ONE_DECIMAL_LABEL_FORMAT = "%.1f";
  private static final String NO_DECIMAL_LABEL_FORMAT = "%.0f";
  private static final int TIC_FONT_SIZE = 9;
  private static final float ROUNDING_FACTOR = 10f;
  private static final float VERTICAL_OFFSET_WITH_TEXT = 15.0f;
  private static final float VERTICAL_OFFSET = 0.0f;

  public WaveformImage(short[] audio, float framesPerSecond, int x, int y,
                       int displayStart, int displayEnd, float timeGridLineFraction
  ) {
    this(audio, framesPerSecond, x, y, displayStart, displayEnd,
        timeGridLineFraction,
        DEFAULT_PAINT, DEFAULT_RMS_PAINT,
        DEFAULT_DRAW_GRID_LINES,
        DEFAULT_DRAW_TICKS);
  }

  private WaveformImage(short[] audio, float framesPerSecond, int x, int y,
                        int displayStart, int displayEnd,
                        float timeGridLineFraction,
                        Color paint,
                        Color rmsPaint,
                        boolean gridLinesP,
                        boolean drawTicksAndLabels
  ) {
    super(framesPerSecond, x, y);
    if (audio.length > 0) {
      drawImage(audio, x, y, displayStart, displayEnd,
          gridLinesP, timeGridLineFraction, drawTicksAndLabels,
          paint, rmsPaint);
    }
  }

  /**
   * Draw the waveform, with labeling and grid lines (both optional).
   *
   * @param buffer               audio data
   * @param xm
   * @param ym
   * @param displayStart
   * @param displayEnd
   * @param gridLinesP           true if want grid lines
   * @param timeGridLineFraction how often to draw grid lines (0.1 -> ten lines, 0.05 -> twenty lines, etc.)
   * @param drawTicksAndLabels   true if want time ticks and second labels
   * @param paint                Color of waveform
   * @param rmsPaint             Color of RMS plots
   * @see #WaveformImage(short[], float, int, int, int, int, float, Color, Color, boolean, boolean)
   */
  private void drawImage(short[] buffer, int xm, int ym, int displayStart, int displayEnd,
                         boolean gridLinesP,
                         float timeGridLineFraction,
                         boolean drawTicksAndLabels, Color paint,
                         Color rmsPaint) {
    Graphics2D gri = this.createGraphics();

    // Coords and Grid
    drawGridLinesAndLabels(xm, ym, displayStart, displayEnd, gridLinesP, timeGridLineFraction, drawTicksAndLabels, gri);

    // Compute stats for display
    drawWaveform(buffer, xm, ym, displayStart, displayEnd, paint, rmsPaint, gri, drawTicksAndLabels);

    // draw over status line
    if (drawTicksAndLabels) {
      drawStatusLine(displayStart, displayEnd, gri);
    }
  }

  private void drawStatusLine(int displayStart, int displayEnd, Graphics2D gri) {
    gri.setColor(new Color(100, 80, 90));
    boolean drawBeginTime = displayStart > 0;
    if (drawBeginTime) {
      String str = String.format("Begin time = %.2f Seconds", toSeconds(displayStart));
      gri.drawString(str, 5, 12);
    }
    float len = getLen(displayStart, displayEnd);
    String str = "Time span = " + String.format("%.2f", len) + " Seconds";
    gri.drawString(str, drawBeginTime ? 180 : 5, 12);
    gri.setColor(new Color(255, 0, 0));
  }

  /**
   * @param xm
   * @param ym
   * @param displayStart
   * @param displayEnd
   * @param gridLinesP
   * @param timeGridLineFraction
   * @param drawTicksAndLabels
   * @param gri
   * @return
   * @see #drawImage(short[], int, int, int, int, boolean, float, boolean, Color, Color)
   */
  private float drawGridLinesAndLabels(int xm, int ym, int displayStart, int displayEnd,
                                       boolean gridLinesP, float timeGridLineFraction, boolean drawTicksAndLabels,
                                       Graphics2D gri) {
    int y;
    int x;
    String str;
    float len = getLen(displayStart, displayEnd);
    gri.setColor(getBackgroundColor(gri));
    gri.fillRect(0, 0, xm, ym);

    // Line color
    gri.setColor(new Color(120, 100, 110));

    // Grid over amplitude
    if (gridLinesP) {
      for (float a = 0.0f; a <= 1.1f; a += 0.1f) {
        y = (int) (a * ((float) ym - 30.0) + 15.0);
        gri.drawLine(0, y, xm, y);
      }
    }
    if (gridLinesP || drawTicksAndLabels) {
      gri.setFont(new Font("Dialog", 0, TIC_FONT_SIZE));
      FontMetrics metrics = gri.getFontMetrics(gri.getFont());
      // Grid over time
      float maxGridLine = 0.995f;//1 - timeGridLineFraction;

      String format = len > 10 ? NO_DECIMAL_LABEL_FORMAT : len > 1 ? ONE_DECIMAL_LABEL_FORMAT : TWO_DECIMAL_LABEL_FORMAT;
      float step = getTickInterval(len);
      float roundingFactor = len > 1 ? ROUNDING_FACTOR : 100f;

      for (float a = step; a < maxGridLine; a += step) {
        x = (int) (a * (float) xm);
        if (gridLinesP) {
          gri.drawLine(x, 15, x, ym - 16);
        } else if (drawTicksAndLabels) {
          gri.drawLine(x, ym - 19, x, ym - 17); // tick
        }

        if (drawTicksAndLabels) {
          float ticTime = (float) Math.round((a * len + toSeconds(displayStart)) * roundingFactor) / roundingFactor;
          str = String.format(format, ticTime);
          int stringWidthInFont = metrics.stringWidth(str);
          gri.drawString(str, x - stringWidthInFont / 2, ym - 5); // center text under tic
        }
      }
    }

    // middle line
    gri.setColor(new Color(255, 0, 0));
    int zero = ym / 2;

    gri.drawLine(0, zero, xm, zero);
    return len;
  }

  /**
   * Try to choose tic marks at uniform intervals. (e.g. .1 for a 1 sec audio file).
   * Goes up to 20 tics before increasing interval by a power of two.
   *
   * @param len
   * @return
   */
  private float getTickInterval(float len) {
    double log = Math.log10(len);
    double tickLog = Math.floor(log) - 1;
    double tickInterval = Math.pow(10, tickLog);
    double numTicks = len / tickInterval;
    float step = (float) tickInterval / len;
    if (numTicks > 20) {
      double floor = Math.floor(Math.log(numTicks / 10) / Math.log(2));
      step *= floor;
    }
    return step;
  }

  private void drawWaveform(short[] buffer, int xm, int ym, int displayStart, int displayEnd,
                            Color paint, Color rmsPaint, Graphics2D gri, boolean drawTicks) {
    int x;
    int y;
    short[] dMin, dMax;                /// Buffers for displayed waveforms (indexed by pixel)
    float[] dx, dx2;                   /// Display mean and covariance stats

    float dX = ((float) (displayEnd - displayStart) / (float) xm);

    dMin = new short[xm];
    dMax = new short[xm];
    dx = new float[xm];
    dx2 = new float[xm];

    int zero = ym / 2;

    int lasty = zero;
    float verticalInset = drawTicks ? VERTICAL_OFFSET_WITH_TEXT : VERTICAL_OFFSET;  // leave room for text if it's drawn

    float verticalSpaceToUse = ym / 2.0f - verticalInset;

    if ((float) (displayEnd - displayStart) < 1.3f * xm) {
      for (x = 0; x < xm; x++) {
        int offset = Math.min(Math.round((float) displayStart + (float) x * dX), buffer.length - 1);
        dMin[x] = dMax[x] = buffer[offset];
      }

      // Paint Amplitude on Screen
      gri.setColor(paint);
      for (x = 0; x < xm; x++) {
        y = (int) ((float) dMax[x] / 32767.0f * verticalSpaceToUse);
        gri.drawLine(x - 1, lasty, x, y + zero);
        lasty = y + zero;
      }
    } else {
      for (x = 0; x < xm; x++) {
        short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
        dx[x] = dx2[x] = 0.0f;
        for (int c = 0; c < dX; c++) {
          int offset = Math.min(Math.round((float) displayStart + (float) x * dX) + c, buffer.length - 1);
          if (buffer[offset] > max) max = buffer[offset];
          if (buffer[offset] < min) min = buffer[offset];
          dx[x] += (float) buffer[offset];
          dx2[x] += (float) buffer[offset] * buffer[offset];
        }
        dMin[x] = min;
        dMax[x] = max;
      }

      // Paint Waveform on Screen
      gri.setColor(paint);
      for (x = 0; x < xm; x++) {
        y = (int) ((float) dMax[x] / 32767.0f * verticalSpaceToUse);
        int dy = (int) ((float) dMin[x] / 32767.0f * verticalSpaceToUse);
        gri.drawLine(x, dy + zero, x, y + zero);
      }

      // Paint RMS power bars
      gri.setColor(rmsPaint);
      for (x = 0; x < xm; x++) {
        int sigma = (int) (Math.sqrt(dx2[x] / dX) / 32767.0 * verticalSpaceToUse);
        //System.err.println("mu = " + mu + ", sigma = " + sigma + ", dx = " + dx[x] + ", dx2 = " + dx2[x] + ", dX = " + dX);
        gri.drawLine(x, zero - sigma, x, zero + sigma);
      }
    }
  }

  @Override
  public IMAGE_FORMAT getFormat() {
    return IMAGE_FORMAT.PNG;
  }

  @Override
  public ImageType getName() {
    return ImageType.WAVEFORM;
  }

  @Override
  protected Color getBackgroundColor(Graphics2D graphics) {
    return new Color(0, 0, 0);
  }
}
