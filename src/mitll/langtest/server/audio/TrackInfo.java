package mitll.langtest.server.audio;

/**
 * Created by go22670 on 11/16/16.
 */
public class TrackInfo {
  private String title;
  private String artist;
  private String comment;
  private String album;

  public TrackInfo(String title, String artist, String comment, String album) {
    this.title = title;
    this.artist = artist;
    this.comment = comment;
    this.album = album;
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

  public String getAlbum() {
    return album;
  }

  public String toString() {
    return title + " by " + artist + " : " + comment + " on " + album;
  }
}
