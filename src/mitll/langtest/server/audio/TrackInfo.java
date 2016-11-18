package mitll.langtest.server.audio;

/**
 * Created by go22670 on 11/16/16.
 */
public class TrackInfo {
  private String title;
  private String artist;
  private String comment;

  public TrackInfo(String title, String artist, String comment) {
    this.title = title;
    this.artist = artist;
    this.comment = comment;
  }

  public String getTitle() {
    return title;
  }

  public String getArtist() {
    return artist;
  }

  public String getComment() {
    return comment;
  }
}
