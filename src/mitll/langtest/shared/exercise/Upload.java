/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

/**
 * Created by go22670 on 12/16/15.
 */
public class Upload implements IsSerializable {
  private long id;
  private long user;
  private String note;
  private String fileRef;
  private long timestamp;
  private boolean enabled = true;
  private String project;
  private String sourceURL;

  public Upload() {
  }

  public Upload(long user, String note, String fileRef, String project, String sourceURL) {
    this(-1, user, note, fileRef, System.currentTimeMillis(), true, project, sourceURL);
  }

  public Upload(long id, long user, String note, String fileRef, long timestamp, boolean enabled,
                String project, String sourceURL) {
    this.id = id;
    this.user = user;
    this.note = note;
    this.fileRef = fileRef;
    this.timestamp = timestamp;
    this.enabled = enabled;
    this.project = project;
    this.sourceURL = sourceURL;
  }

  @Override
  public String toString() {
    return "Upload #" +id+
        "by " + getUser() + " on " + new Date(getTimestamp()) + " note " + getNote() + " file " + getFileRef();
  }

  public long getUser() {
    return user;
  }

  public String getNote() {
    return note;
  }

  public String getFileRef() {
    return fileRef;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getProject() {
    return project;
  }

  public String getSourceURL() {
    return sourceURL;
  }
}
