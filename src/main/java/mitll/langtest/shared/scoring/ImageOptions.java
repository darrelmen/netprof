package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 11/16/16.
 */
public class ImageOptions implements IsSerializable {
  private int width = 120;
  private int height = 120;
  private boolean useScoreToColorBkg;
  private boolean writeImages = false;

  public ImageOptions() {
  }

  public static ImageOptions getDefault() {
    return new ImageOptions(128, 128, false, false);
  }

  public ImageOptions(int width, int height, boolean useScoreToColorBkg, boolean writeImages) {
    this.width = width;
    this.height = height;
    this.useScoreToColorBkg = useScoreToColorBkg;
    this.writeImages=writeImages;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isUseScoreToColorBkg() {
    return useScoreToColorBkg;
  }

  public boolean isWriteImages() {
    return writeImages;
  }

  public void setWriteImages(boolean writeImages) {
    this.writeImages = writeImages;
  }

  public String toString() {
    return "opt :" + (writeImages ? " write " : " skip ") +
        "  w " + width + " x h " + height + (useScoreToColorBkg ? " scoreToColor" : "");
  }
}
