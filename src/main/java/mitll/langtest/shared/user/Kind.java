package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum Kind implements IsSerializable {
  UNSET("Unset", "UST", false),
  INTERNAL("INTERNAL", "INT", false),  // for users we keep to maintain referencial integrity, for instance an importUser

  STUDENT("Student", "STU", true),
  TEACHER("Teacher", "TCHR", true),
  QAQC("QAQC", "QAQC", true), // someone who can edit content
  CONTENT_DEVELOPER("Content Developer", "CDEV", true), // someone who can edit content and record audio
  AUDIO_RECORDER("Audio Recorder", "AREC", true),       // someone who is just an audio recorder
  TEST("Test Account", "TST", true),                   // e.g. for developers at Lincoln or DLI, demo accounts
  SPAM("Spam Account", "SPM", true),                   // for marking nuisance accounts
  PROJECT_ADMIN("Project Admin", "PrAdmin", true),         // invite new users, admin accounts below
  ADMIN("System Admin", "UM", true);                  // invite project admins, closed set determined by server properties

  String name;
  String role;
  boolean show;

  Kind() {
  }

  Kind(String name, String role, boolean show) {
    this.name = name;
    this.role = role;
    this.show = show;
  }

  public String getName() {
    return name;
  }

  public String getRole() {
    return role;
  }

  public boolean shouldShow() {
    return show;
  }
}
