/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.audio.image;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class AudioImage extends BufferedImage {
  static final float[][] RYB_COLOR_MAP = {
      {255f, 0f, 0f}, // red
      {255f, 32f, 0f},
      {255f, 64f, 0f},
      {255f, 128f, 0f},
      {255f, 192f, 0f},
      {255f, 255f, 0f}, // yellow
      {192f, 255f, 0f},
      {128f, 255f, 0f},
      {64f, 255f, 0f},
      {32f, 255f, 0f},
      {0f, 255f, 0f}};  // green
  static final float[][] JET_COLOR_MAP = new float[][]{{0f, 0f, 0f},
      {0f, 0f, 16f},
      {0f, 0f, 64f},
      {0f, 0f, 128f},
      {0f, 0f, 192f},
      {0f, 0f, 255f}, // blue

      {0f, 64f, 255f},
      {0f, 128f, 255f},
      {0f, 192f, 255f},
      {0f, 255f, 255f}, // cyan

      {64f, 255f, 192f},
      {128f, 255f, 128f},
      {192f, 255f, 64f},
      {255f, 255f, 0f}, // yellow

      {255f, 192f, 0f},
      {255f, 128f, 0f},
      {255f, 64f, 0f},
      {255f, 0f, 0f}};

  /*
  public static final float[][] FIRE_COLOR_MAP = new float[][]{{0f, 0f, 0f},
      {32f, 0f, 0f},
      {64f, 0f, 0f},
      {128f, 0f, 0f},
      {192f, 0f, 0f},
      {255f, 0f, 0f}, // red
      {255f, 32f, 0f},
      {255f, 64f, 0f},
      {255f, 128f, 0f},
      {255f, 192f, 0f},
      {255f, 255f, 0f}};
*/

  private final float fps;                              /// Samples (frames) per second for current audio

  AudioImage(float framesPerSecond, int xm, int ym) {
    super(xm, ym, BufferedImage.TYPE_INT_RGB);
    fps = framesPerSecond;
  }

  float toSeconds(int idx) {
    return (float) idx / fps;
  }

  float getLen(int displayStart, int displayEnd) {
    return toSeconds(displayEnd - displayStart);
  }

  public enum IMAGE_FORMAT {
    JPG, PNG;

    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  /**
   * Get a subimage of the larger image starting at x and extending w pixels wide.
   * <p>
   * If x+w would extend beyond the right edge of the image, pad the right side with the background color.
   * <p>
   * This means all subimages will all be the same size.
   *
   * @param x starting here horizontally in pixel space
   * @param w this pixels wide
   * @return image that is w x image height
   */
  public BufferedImage getSubimage(int x, int w) {
    int imageWidth = getWidth();
    int h = getHeight();
    boolean onLastTile = x + w > imageWidth;
    int sliceWidth = onLastTile ? imageWidth - x : w;
    BufferedImage subimage = super.getSubimage(x, 0, sliceWidth, h);

    if (onLastTile) {
      BufferedImage paddedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = paddedImage.createGraphics();
      graphics.setColor(getBackgroundColor(graphics));
      graphics.fillRect(0, 0, w, h);
      paddedImage.setData(subimage.getData());
      return paddedImage;
    }
    return subimage;
  }

  /**
   * Use this color to fill in tiles that need to be padded.
   *
   * @param graphics
   * @return Color
   */
  Color getBackgroundColor(Graphics2D graphics) {
    return graphics.getBackground();
  }

  public abstract IMAGE_FORMAT getFormat();

  protected abstract ImageType getName();

  public String toString() {
    return getName().toString();
  }
}
