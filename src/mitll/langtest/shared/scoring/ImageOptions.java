package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 11/16/16.
 */
public class ImageOptions implements IsSerializable {
  private int width;
  private int height;
  private boolean useScoreToColorBkg;

  public ImageOptions() {
  }

  public static ImageOptions getDefault() {
    return new ImageOptions(128, 128, false);
  }

  public ImageOptions(int width, int height, boolean useScoreToColorBkg) {
    this.width = width;
    this.height = height;
    this.useScoreToColorBkg = useScoreToColorBkg;
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

  public String toString() {
    return "w " + width + " x h " + height + (useScoreToColorBkg ? " scoreToColor" : "");
  }
}
