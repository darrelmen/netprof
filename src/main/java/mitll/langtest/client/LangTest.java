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

package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Tab;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.client.analysis.WordContainerAsync;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.dialog.*;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.*;
import mitll.langtest.client.instrumentation.ButtonFactory;
import mitll.langtest.client.instrumentation.EventContext;
import mitll.langtest.client.instrumentation.EventLogger;
import mitll.langtest.client.project.ProjectEditForm;
import mitll.langtest.client.recorder.BrowserRecording;
import mitll.langtest.client.recorder.MicPermission;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.WebAudioRecorder;
import mitll.langtest.client.scoring.AnnotationHelper;
import mitll.langtest.client.scoring.ClientAudioContext;
import mitll.langtest.client.scoring.CommentAnnotator;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.services.*;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.client.sound.SoundManagerStatic;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.user.User;

import java.util.*;
import java.util.logging.Logger;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 * <p>
 * Release Versions:
 * <p>
 * 1.1
 * - final state of analysis feature - off by default, on with ?analysis
 * 1.1.1
 * - fix for custom english entries
 * 1.1.2
 * - Fix for highlight of mixed case context
 * - Added code for trimming silence
 * 1.1.3
 * - Trims silence from beginning and end of recordings with sox
 * 1.1.4
 * - Trims silence from all ref audio
 * 1.1.5
 * - German support - fixes for trie lookup of lowercase characters
 * 1.1.6
 * - ScoreServlet report changes - export user table, add option to filter for items with or without audio
 * 1.1.7
 * - Duplicate exercise id bug with russian - see https://gh.ll.mit.edu/DLI-LTEA/Development/issues/504 and small change to json rate(sec) output
 * 1.1.8
 * - Fix leaving around lots of temp directories
 * 1.2.0
 * - Refactor to add in Amas website support, and steps toward supporting getting content from json
 * 1.2.1
 * - bug fix for display of ref audio in analysis
 * 1.2.2
 * - support for AMAS
 * 1.2.3
 * - integrate with domino to get content for AMAS
 * 1.2.4
 * - support for domino NetProF integration
 * 1.2.5
 * - fix for issue with collapsing words with commas in them, added removeRefResult to scoreServlet, partial support for import to lists
 * 1.2.6
 * - fix for old bug where clicking on a word or phrase did a playback with a ~50 millisecond offset
 * 1.2.7
 * - allows you to filter out Default (no gender mark) audio in mark defects, and associated fixes
 * 1.2.8
 * - Added About NetProF dialog that shows model info, etc. and small tweaks to audio trimming, etc.
 * 1.2.9
 * - Updated reporting to show all years, fill in month/week gaps with zeros
 * 1.2.10
 * - More small report changes
 * 1.2.11
 * - Fixed bugs with browser history forwards/backwards and clicking on characters
 * - Better support for keeping track of transcripts on audio files and noticing when they're out of sync with current content
 * 1.2.12
 * - Fix for lookup for ref audio, fix for sending meaning back in nested chapters score servlet call
 * 1.2.13
 * - fix for highlight on context sentence where now does max coverage, remove email cc to ltea,
 * looks at content to determine whether RTL language, bug where exercise lists wouldn't come up,
 * bug where didn't use cached alignment from refresult table, fix for sending meaning for english in nestedChapters
 * 1.3.0
 * - fixes for history stack
 * 1.3.1
 * - fix for bug where couldn't jump from word in analysis
 * 1.3.2
 * - report updates
 * 1.3.3
 * - fixes for generated keys bug on result table
 * 1.3.4
 * - student analysis tab visible, html5 audio preferred
 * 1.3.5
 * - bug fix for issue where prev next buttons on empty list would throw exception, event dialog wouldn't come up on big tables
 * 1.3.6
 * - admin test on userid was case sensitive
 * 1.4.0
 * - fix for user password reset issue where people have trouble resetting their password - allow user@host.edu as login for user
 * 1.4.1
 * - fixes for AVP - doesn't return scores at end of practice, english doesn't show meaning, english doesn't show hide options, english doesn't highlight right text box when click to play audio
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 * 1.4.2
 * - fix for bug where column header for "context" got skipped for egyptian
 * 1.4.3
 * - Added Japanese, allows you to click on characters in transliteration (hiragana)
 * 1.4.4
 * - Added some minor fixes for exceptions seen in analysis plot
 * 1.4.5
 * - Adds audio table references for really old audio like in Pashto 1,2,3
 * 1.4.6
 * - Fixes for bugs #649,#650,#651, partial fix to #652, flip card in avp with arrow keys
 * 1.4.7
 * - Fixes for bugs #646 - download link replaced with dialog
 * 1.4.8
 * - Fixed bug with detecting RTL text and showing it in the exercise list
 * 1.4.9
 * - Fixed bug with downloading audio for custom item.
 * 1.4.10 (9/9/16)
 * - Fixes for QC -
 * 1.4.11 (9-26-16)
 * - Added auto advance button to avp
 * 1.5.0 (10-07-16)
 * - Clean up download dialog as per Michael Grimmer request
 * 1.5.1 (10-11-16)
 * - Added shouldRecalcStudentAudio option to recalc student audio with the current model
 * 1.5.2 (10-19-16)
 * - Fixed bug in reporting where was throwing away valid recordings and added separate new teacher section to reporting
 * 1.5.3 (10-19-16)
 * - Don't attach reference audio that doesn't pass dnr minimum (mainly for old audio). Mark audio rows with DNR.
 * 1.5.4 (10-21-16)
 * - Increase delay after correct avp response.
 * 1.5.5 (10-23-16)
 * - Fixed bug where couldn't add defect comments to context sentences, better handling of sentence length user exercise entries.
 * 1.5.6 (10-31-16)
 * - Fixed bug where urdu was getting slow to return exercises - we hadn't created indexes on columns b/c there was another one with the same name on another table
 * 1.5.7 (11-01-16)
 * - Fixed bug where could get into a bad state if clicked the record button too quickly.
 * 1.5.8 (11-01-16)
 * - Fixes to support Sorani
 * 1.5.9 (11-02-16)
 * - Fixes to support updating Turkish and preserving audio.
 * 1.5.10
 * - Turn off transcript matching for audio for now, remove ref audio that doesn't meet the dnr threshold
 * 1.5.11
 * - Support for Hindi - added comment on text for recording audio.
 * 1.5.12
 * - Fix for filtering bug where would not filter out recordings made by a user in the same day
 * 1.5.13
 * - Fix for Bug #739 - recording summary total doesn't reflect transcript mismatch
 * 1.5.14
 * - Fixes for audio recording, commenting on audio
 * 1.5.15
 * - Fixes for indicating which audio in fix defects is actually presented, fix for bug #751
 * 2.0.0
 * - development to master
 * 2.0.2
 * 2.0.3
 * 2.0.4
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class LangTest implements
    EntryPoint, UserFeedback, ExerciseController, UserNotification, LifecycleSupport, UserState {
  private final Logger logger = Logger.getLogger("LangTest");

  private static final String LOCALHOST = "127.0.0.1";

  private static final String CAN_T_RECORD_AUDIO = "Can't record audio";
  private static final String RECORDING_AUDIO_IS_NOT_SUPPORTED = "Recording audio is not supported.";

  private static final String GOT_BROWSER_EXCEPTION = "got browser exception : ";

  /**
   * @see #setPageTitle
   */
  private static final String INTRO = "Learn pronunciation and practice vocabulary.";

/*
  public static final String VERSION_INFO = "3.0.3";
*/

  private static final String UNKNOWN = "unknown";
  public static final String LANGTEST_IMAGES = "langtest/images/";


  private static final String RED_X = LangTest.LANGTEST_IMAGES + "redx32.png";
  public static final SafeUri RED_X_URL = UriUtils.fromSafeConstant(RED_X);

  /**
   * @see
   */
  public static final String RECORDING_DISABLED = "RECORDING DISABLED";

  private static final String DIVIDER = "|";
  private static final int MAX_EXCEPTION_STRING = 600;
  private static final int MAX_CACHE_SIZE = 100;
  private static final boolean DEBUG = false;

  private UserManager userManager;

  // services
  private final AudioServiceAsync defaultAudioService = GWT.create(AudioService.class);
  private final ScoringServiceAsync defaultScoringServiceAsync = GWT.create(ScoringService.class);

  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final UserServiceAsync userService = GWT.create(UserService.class);
  private final OpenUserServiceAsync openUserService = GWT.create(OpenUserService.class);
  private final ExerciseServiceAsync exerciseServiceAsync = GWT.create(ExerciseService.class);
  private final DialogServiceAsync dialogServiceAsync = GWT.create(DialogService.class);
  private final ListServiceAsync listServiceAsync = GWT.create(ListService.class);
  private final QCServiceAsync qcServiceAsync = GWT.create(QCService.class);

  private final DLIClassServiceAsync dliClassServiceAsync = GWT.create(DLIClassService.class);

  private final BrowserCheck browserCheck = new BrowserCheck();
  private SoundManagerStatic soundManager;
  private PropertyHandler props;

  /**
   *
   */
  private StartupInfo startupInfo;
  /**
   * @see #setProjectStartupInfo(User)
   * @see #clearStartupInfo
   */
  private ProjectStartupInfo projectStartupInfo;

  private EventLogger buttonFactory;
  private final KeyPressHelper keyPressHelper = new KeyPressHelper(false, true);
  private boolean isMicConnected = true;
  private UILifecycle initialUI;
  public static final EventBus EVENT_BUS = GWT.create(SimpleEventBus.class);

  private KeyStorage storage;

  private Map<Integer, AudioServiceAsync> projectToAudioService;
  private Map<Integer, ScoringServiceAsync> projectToScoringService;
  private final long then = 0;
  private MessageHelper messageHelper;
  private AnnotationHelper annotationHelper;
  private boolean hasNetworkProblem;
  private WavEndCallback wavEndCallback;

  /**
   * This gets called first.
   * <p>
   * Make an exception handler that displays the exception.
   */
  public void onModuleLoad() {
    // set uncaught exception handler
    dealWithExceptions();
    askForStartupInfo();
  }

  private void askForStartupInfo() {
    final long then = System.currentTimeMillis();
    service.getStartupInfo(new AsyncCallback<StartupInfo>() {
      public void onFailure(Throwable caught) {
        LangTest.this.onFailure(caught, then);
      }

      public void onSuccess(StartupInfo startupInfo) {
        long now = System.currentTimeMillis();
        if (startupInfo == null) logger.warning("startup info is null??");
        rememberStartup(startupInfo, false);
        onModuleLoad2();

        if (isLogClientMessages() && (now - then > 500)) {
          String message = "onModuleLoad.getProperties : (success) took " + (now - then) + " millis";
          logMessageOnServer(message, false);
        }
      }
    });
  }

  /**
   * Hopefully we'll never see this.
   *
   * @param caught
   * @param then
   */
  public void onFailure(Throwable caught, long then) {
    if (caught instanceof IncompatibleRemoteServiceException) {
      Window.alert("This application has recently been updated.\nPlease refresh this page, or restart your browser." +
          "\nIf you still see this message, clear your cache. (" + caught.getMessage() +
          ")");
    } else {
      long now = System.currentTimeMillis();
      String message = "onModuleLoad.getProperties : (failure) took " + (now - then) + " millis";

      logger.info(message);
      if (!caught.getMessage().trim().equals("0")) {
        logger.info("Exception " + caught.getMessage() + " " + caught + " " + caught.getClass() + " " + caught.getCause());
        Window.alert("Couldn't contact server.  Please check your network connection. (getProperties)");
        logMessageOnServer(message, true);
      }
    }
  }

  /**
   * after we change state
   *
   * @param reloadWindow
   * @see ProjectEditForm#updateProject
   */
  public void refreshStartupInfo(boolean reloadWindow) {
    service.getStartupInfo(new AsyncCallback<StartupInfo>() {
      public void onFailure(Throwable caught) {
        LangTest.this.onFailure(caught, then);
      }

      public void onSuccess(StartupInfo startupInfo) {
        rememberStartup(startupInfo, reloadWindow);
      }
    });
  }

  /**
   * @param startupInfo
   * @param reloadWindow
   * @see #askForStartupInfo
   */
  private void rememberStartup(StartupInfo startupInfo, boolean reloadWindow) {
    this.startupInfo = startupInfo;
    props = new PropertyHandler(startupInfo.getProperties());
    if (reloadWindow) {
      initialUI.chooseProjectAgain();
    }

    List<SlimProject> projects = getAllProjects();
    projectToAudioService = createHostSpecificServices(projects);
    projectToScoringService = createHostSpecificServicesScoring(projects);
  }

  @Override
  public List<SlimProject> getAllProjects() {
    return startupInfo.getAllProjects();
  }

  /**
   * We need to make host-specific services. The client needs to route the service request to right back-end
   * service. For instance, currently korean, levantine, msa, and russian are on hydra2 (h2).
   *
   * @param projects
   * @return
   */
  private Map<Integer, AudioServiceAsync> createHostSpecificServices(List<SlimProject> projects) {
    Map<Integer, AudioServiceAsync> projectToAudioService = new HashMap<>();
    Map<String, AudioServiceAsync> hostToService = new HashMap<>();

    // first figure out unique set of services...
    projects.forEach(slimProject ->
        hostToService.computeIfAbsent(slimProject.getHost(), this::getAudioServiceAsyncForHost));

//    logger.info("createHostSpecificServices " + hostToService.size() + " " + hostToService.keySet());

    // then map project to service
    projects.forEach(slimProject -> {
      String host = slimProject.getHost();
      AudioServiceAsync audioServiceAsync = hostToService.get(host);
      if (audioServiceAsync == null) {
        logger.warning("createHostSpecificServices no audio service for " + host + " project " + slimProject);
      } else {
        projectToAudioService.put(slimProject.getID(), audioServiceAsync);
      }
    });

    //  logger.info("createHostSpecificServices for these projects " + projectToAudioService.keySet());

    return projectToAudioService;
  }


  /**
   * Create host specific scoring services -- i.e. scoring for russian, korean, msa, levantine are on h2.
   *
   * @param projects
   * @return
   * @see #rememberStartup
   */
  private Map<Integer, ScoringServiceAsync> createHostSpecificServicesScoring(List<SlimProject> projects) {
    Map<Integer, ScoringServiceAsync> projectToAudioService = new HashMap<>();
    Map<String, ScoringServiceAsync> hostToService = new HashMap<>();

    // first figure out unique set of services...
    projects.forEach(slimProject ->
        hostToService.computeIfAbsent(slimProject.getHost(), this::getScoringServiceAsyncForHost));

//    logger.info("createHostSpecificServices num hosts = " + hostToService.size() + " : " + hostToService.keySet());
    // then map project to service
    projects.forEach(slimProject -> {
      String host = slimProject.getHost();
      ScoringServiceAsync scoringServiceAsync = hostToService.get(host);

      //    logger.info("createHostSpecificServicesScoring : for project #" + slimProject.getID() + " host = '" +host+ "'");
      if (scoringServiceAsync == null) {
        logger.warning("no scoring service for " + host + " project " + slimProject);
      } else {
        projectToAudioService.put(slimProject.getID(), scoringServiceAsync);
      }
    });

    return projectToAudioService;
  }

  @Override
  public AudioServiceAsync getAudioServiceAsyncForHost(String host) {
    AudioServiceAsync audioService = GWT.create(AudioService.class);
    adjustEntryPoint(host, (ServiceDefTarget) audioService);
    return audioService;
  }

  @Override
  public ScoringServiceAsync getScoringServiceAsyncForHost(String host) {
    ScoringServiceAsync audioService = GWT.create(ScoringService.class);
    adjustEntryPoint(host, (ServiceDefTarget) audioService);
    return audioService;
  }

  private void adjustEntryPoint(String host, ServiceDefTarget audioService) {
    if (!isDefault(host)) {
      String moduleBaseURL = audioService.getServiceEntryPoint();
      audioService.setServiceEntryPoint(moduleBaseURL + "/" + host);
      //   logger.info("adjustEntryPoint createHostSpecificServices service now at " + audioService.getServiceEntryPoint());
    } else {
      // logger.info("adjustEntryPoint createHostSpecificServices service is at " + audioService.getServiceEntryPoint());
    }
  }

  private boolean isDefault(String host) {
    boolean isDefault = false;
    if (host.equals(LOCALHOST)) isDefault = true;
    return isDefault;
  }

  /**
   * Log browser exception on server, include user and exercise ids.  Consider including chapter selection.
   *
   * @see #onModuleLoad
   */
  private void dealWithExceptions() {
    GWT.setUncaughtExceptionHandler(this::logException);
  }

  private boolean lastWasStackOverflow = false;

  public String logException(Throwable throwable) {
    logger.info("logException got exception " + throwable.getMessage());

    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(throwable);
    logger.info("logException stack " + exceptionAsString);
    boolean isStackOverflow = exceptionAsString.contains("Maximum call stack size exceeded");
    if (isStackOverflow && lastWasStackOverflow) { // we get overwhelmed by repeated exceptions
      return ""; // skip repeat exceptions
    } else {
      lastWasStackOverflow = isStackOverflow;
    }
    logMessageOnServer(exceptionAsString, GOT_BROWSER_EXCEPTION, true);
    return exceptionAsString;
  }

  public void logMessageOnServer(String message, String prefix, boolean sendEmail) {
    int user = userManager != null ? userManager.getUser() : -1;
    String exerciseID = "Unknown";
    String suffix = " browser " + browserCheck.getBrowserAndVersion() + " : " + message;
    logMessageOnServer(prefix + "user #" + user + " exercise " + exerciseID + suffix, sendEmail);

    String toSend = prefix + suffix;
    if (toSend.length() > MAX_EXCEPTION_STRING) {
      toSend = toSend.substring(0, MAX_EXCEPTION_STRING) + "...";
    }
    getButtonFactory().logEvent(UNKNOWN, UNKNOWN, new EventContext(exerciseID, toSend, user));
  }

  private void logMessageOnServer(final String message, final boolean sendEmail) {
    Scheduler.get().scheduleDeferred(() -> service.logMessage(message, sendEmail,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.warning("couldn't post message to server?");
          }

          @Override
          public void onSuccess(Void result) {
            logger.info("posted message to server, length " + message.length() + " send email " + sendEmail);
          }
        }
    ));
  }

  /**
   * @param reqid
   * @param path
   * @param type
   * @param toUse
   * @param height
   * @param exerciseID
   * @param client
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio(String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
   */
  @Override
  public void getImage(int reqid, final String path, final String type, int toUse, int height, int exerciseID,
                       AsyncCallback<ImageResponse> client) {
    String key = path + DIVIDER + type + DIVIDER + toUse + DIVIDER + height + DIVIDER + exerciseID;
    getImage(reqid, key, client);
  }

  private void getImage(int reqid, String key, AsyncCallback<ImageResponse> client) {
    String[] split = key.split("\\|");
    String path = split[0];
    String type = split[1];
    int toUse = Integer.parseInt(split[2]);
    int height = Integer.parseInt(split[3]);
    String exerciseID = split[4];

    getImage(reqid, key, path, type, toUse, height, exerciseID, getLanguageInfo(), client);
  }

  private void getImage(int reqid, final String key, String path, final String type, int toUse, int height,
                        String exerciseID,
                        Language language,
                        final AsyncCallback<ImageResponse> client) {
    //  ImageResponse ifPresent = imageCache.getIfPresent(key);
    ImageResponse ifPresent = imageCache.get(key);
    if (ifPresent != null) {
    //  logger.info("getImage for key " + key + " found  " + ifPresent);
      ifPresent.req = -1;
      client.onSuccess(ifPresent);
    } else {
      ImageOptions imageOptions = new ImageOptions(toUse, height, useBkgColorForRef(), true);
      getAudioService().getImageForAudioFile(reqid, path, type, imageOptions, exerciseID,
          language,
          new AsyncCallback<ImageResponse>() {
            public void onFailure(Throwable caught) {
       /*   if (!caught.getMessage().trim().equals("0")) {
            Window.alert("getImageForAudioFile Couldn't contact server. Please check network connection.");
          }*/
              logger.info("message " + caught.getMessage() + " " + caught);
              logException(caught);
              client.onFailure(caught);
            }

            public void onSuccess(ImageResponse result) {
              imageCache.put(key, result);
            //   logger.info("getImage storing key " + key + " now  " + imageCache.size() + " cached.");
              if (client != null) {
              //  logger.info("getImage client "+ client.getClass());

                Scheduler.get().scheduleDeferred(() -> {
                  client.onSuccess(result);
                });
              }
            }
          });
    }
  }

  /*
    private final Cache<String, ImageResponse> imageCache = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        .expireAfterWrite(7, TimeUnit.DAYS)
        .build();
  */
  private final Map<String, ImageResponse> imageCache = lruCache(MAX_CACHE_SIZE);

  private static <K, V> Map<K, V> lruCache(final int maxSize) {
    return new LinkedHashMap<K, V>(maxSize * 4 / 3, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
      }
    };
  }

  /**
   * @see #onModuleLoad()
   */
  private void onModuleLoad2() {
    setupSoundManager();

    buttonFactory = new ButtonFactory(service, props, this);

    userManager = new UserManager(this, this, userService, props);

    RootPanel.get().getElement().getStyle().setPaddingTop(2, Style.Unit.PX);

    initBrowserRecording();

    this.initialUI = new InitialUI(this, userManager);
    messageHelper = new MessageHelper(initialUI, this);
    annotationHelper = new AnnotationHelper(this, messageHelper);

    populateRootPanel();

    setPageTitle();
    browserCheck.checkForCompatibleBrowser();

    String message = startupInfo.getMessage();
    if (message != null && !message.isEmpty()) {
      showErrorMessage("Configuration Error", message);
    }
  }

  /**
   * @return
   * @see #onModuleLoad2()
   */
  private void populateRootPanel() {
    RootPanel.get().clear();   // necessary?
    initialUI.populateRootPanel();
  }

  /**
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser
   */
  @Override
  public void showLogin() {
    initialUI.populateRootPanel();
  }

  /**
   * Set the page title and favicon.
   */
  private void setPageTitle() {
    Window.setTitle(props.getAppTitle() + " : " + INTRO);

    Element element = DOM.getElementById("favicon");   // set the page title to be consistent
    if (element != null) {
      element.setAttribute("href", LANGTEST_IMAGES + "NewProF1_48x48.png");
    }
  }

  public String getBrowserInfo() {
    return browserCheck.getBrowserAndVersion();
  }

  public UserState getUserState() {
    return this;
  }

  private void setupSoundManager() {
    soundManager = new SoundManagerStatic();
  }

  public int getHeightOfTopRows() {
    return initialUI.getHeightOfTopRows();
  }

  /**
   * Check the URL parameters for special modes.
   * <p>
   * If in goodwave (pronunciation scoring) mode or auto crt mode, skip the user login.
   *
   * @see InitialUI#resetState
   * @see #onModuleLoad2()
   */
  @Override
  public void recordingModeSelect() {
    Scheduler.get().scheduleDeferred(this::checkInitFlash);
  }

  private boolean showingPlugInNotice = false;

  /**
   * Hookup feedback for events from Flash generated from the user's response to the Mic Access dialog
   *
   * @see #onModuleLoad2()
   * @see mitll.langtest.client.recorder.FlashRecordPanelHeadless#micConnected()
   */
  private void initBrowserRecording() {
    // logger.info("initBrowserRecording - called");
    MicPermission micPermission = new MicPermission() {
      /**
       * @see mitll.langtest.client.recorder.WebAudioRecorder
       */
      public void gotPermission() {
//        logger.info("initBrowserRecording - got permission!");

        checkLogin();
      }

      /**
       * @seex FlashRecordPanelHeadless#noMicrophoneFound
       */
 /*     public void noMicAvailable() {
        if (!showingPlugInNotice) {
          showingPlugInNotice = true;
          List<String> messages = Arrays.asList("If you want to record audio, ",
              "plug in or enable your mic and reload the page.");
          new ModalInfoDialog("Plug in microphone", messages, Collections.emptyList(),
              null,
              hiddenEvent -> {

                checkLogin();
                initialUI.setSplash(RECORDING_DISABLED);
                isMicConnected = false;
              }, false, true, 600, 400);
        }
      }*/

      /**
       * @see
       */
      public void noRecordingMethodAvailable() {
        logger.info("\tnoRecordingMethodAvailable - no way to record");

        new ModalInfoDialog(CAN_T_RECORD_AUDIO, RECORDING_AUDIO_IS_NOT_SUPPORTED, hiddenEvent -> checkLogin());

        initialUI.setSplash(RECORDING_DISABLED);
        isMicConnected = false;
      }

      @Override
      public void noWebRTCAvailable() {
        noRecordingMethodAvailable();
      }

      /**
       * @see WebAudioRecorder#silenceDetected
       */
      @Override
      public void silenceDetected() {
        if (wavEndCallback != null) wavEndCallback.silenceDetected();
      }
    };

    BrowserRecording.init(micPermission);
  }


  public boolean hasModel() {
    return projectStartupInfo != null && getProjectStartupInfo().isHasModel();
  }

  /**
   * @see InitialUI#clearStartupInfo
   */
  public void clearStartupInfo() {
    this.projectStartupInfo = null;
  }

  public Collection<String> getTypeOrder() {
    return projectStartupInfo == null ? Collections.emptyList() : projectStartupInfo.getTypeOrder();
  }

  @Override
  public ProjectStartupInfo getProjectStartupInfo() {
    //logger.info("\ngetStartupInfo Got startup info " + projectStartupInfo);
    return projectStartupInfo;
  }

  /**
   * So we can either get project info from the user itself, or it can be changed later when
   * the user changes language/project.
   *
   * @param user
   * @see LangTest#gotUser
   * @see mitll.langtest.client.project.ProjectChoices#setProjectForUser
   */
  public void setProjectStartupInfo(User user) {
    projectStartupInfo = user.getStartupInfo();

    if (projectStartupInfo == null) {
      logger.info("setProjectStartupInfo project startup null for " + user);
//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("no startup?"));
//      logger.info("logException stack " + exceptionAsString);
    }

    initialUI.showCogMenu();
  }

  /**
   * So if during history changes we see the project has changed, we have to react to it here.
   *
   * @param projectid
   * @see mitll.langtest.client.list.FacetExerciseList#projectChangedTo
   */
  @Override
  public void reallySetTheProject(int projectid) {
//    logger.info("setProjectForUser set project for " + projectid);
    initialUI.clearContent();

    openUserService.setProject(projectid, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable throwable) {
        messageHelper.handleNonFatalError("setting project for user", throwable);
      }

      @Override
      public void onSuccess(User aUser) {
        if (aUser == null) {
          logger.warning("huh? no current user? ");
        } else {
          setProjectStartupInfo(aUser);
          logger.info("reallySetTheProject :  set project for " + aUser + " show initial state ");
          initialUI.showInitialState();
          initialUI.addBreadcrumbs();
        }
      }
    });
  }

  /**
   * Init Flash recorder once we login.
   * <p>
   * Only get the exercises if the user has accepted mic access.
   *
   * @param user
   * @see #initBrowserRecording
   * @see UserManager#gotNewUser(User)
   * @see UserManager#storeUser
   */
  public void gotUser(User user) {
    setProjectStartupInfo(user);
    if (DEBUG) logger.info("\ngotUser Got startup info " + projectStartupInfo);
    initialUI.gotUser(user);
  }

  /**
   * @see #recordingModeSelect()
   */
  private void checkInitFlash() {
    if (BrowserRecording.gotPermission()) {
      if (DEBUG) logger.info("checkInitFlash : initFlash - has permission");
      checkLogin();
    } else {
      if (DEBUG) logger.info("checkInitFlash : initFlash - no permission yet");
      BrowserRecording.getWebAudio().initWebaudio();
      if (!WebAudioRecorder.isWebRTCAvailable()) {
        checkLogin();
      }
    }
  }

  @Override
  public EventLogger getButtonFactory() {
    return buttonFactory;
  }

  @Override
  public void register(UIObject button) {
    buttonFactory.register(this, button, "N/A");
  }

  @Override
  public void register(UIObject button, int exid) {
    register(button, "" + exid);
  }

  @Override
  public void register(UIObject button, String exid) {
    buttonFactory.register(this, button, exid);
  }

  @Override
  public void register(Button button, int exid, String context) {
    register(button, "" + exid, context);
  }

  @Override
  public void register(Button button, String exid, String context) {
    buttonFactory.registerButton(button, new EventContext(exid, context, getUser()));
  }

  @Override
  public void registerWidget(HasClickHandlers clickable, UIObject uiObject, String exid, String context) {
    buttonFactory.registerWidget(clickable, uiObject, new EventContext(exid, context, getUser()));
  }

  @Override
  public void logEvent(UIObject button, String widgetType, HasID ex, String context) {
    //logger.info("logEvent START ex " + ex + " in " + context);
    buttonFactory.logEvent(button, widgetType, new EventContext("" + ex.getID(), context, getUser()));
    //logger.info("logEvent END   ex " + ex + " in " + context);

  }

  @Override
  public void logEvent(UIObject button, String widgetType, String exid, String context) {
    buttonFactory.logEvent(button, widgetType, new EventContext(exid, context, getUser()));
  }

  @Override
  public void logEvent(UIObject button, String widgetType, int exid, String context) {
    logEvent(button, widgetType, "" + exid, context);
  }

  @Override
  public void logEvent(Tab button, String widgetType, String exid, String context) {
    buttonFactory.logEvent(button, widgetType, new EventContext(exid, context, getUser()));
  }

  /**
   * TODO : consider exid as an int
   *
   * @param widgetID
   * @param widgetType
   * @param exid
   * @param context
   */
  @Override
  public void logEvent(String widgetID, String widgetType, String exid, String context) {
    buttonFactory.logEvent(widgetID, widgetType, new EventContext(exid, context, getUser()));
  }

  /**
   * Called after the user clicks "Yes" in flash mic permission dialog.
   *
   * @see #checkInitFlash()
   * @see #initBrowserRecording()
   */
  private void checkLogin() {
    //console("checkLogin");
    //logger.info("checkLogin -- ");
    // userManager.isUserExpired();
    userManager.checkLogin();
  }

  public boolean showCompleted() {
    return isReviewMode() || hasPermission(User.Permission.RECORD_AUDIO);
  }

  private boolean isReviewMode() {
    return hasPermission(User.Permission.QUALITY_CONTROL);
  }

  /**
   * @return
   */
  @Override
  public Collection<User.Permission> getPermissions() {
    User current = getCurrent();
    return current != null ? current.getPermissions() : Collections.emptyList();
  }

  public boolean hasPermission(User.Permission permission) {
    //  logger.info("hasPermission user permissions " + getPermissions() + " for " + getUser());
    return getPermissions().contains(permission);
  }

  /**
   * @return
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers
   */
  public int getUser() {
    return userManager.getUser();
  }

  public User getCurrent() {
    return userManager.getCurrent();
  }

  public boolean isAdmin() {
    return userManager.isAdmin();
  }

  public PropertyHandler getProps() {
    return props;
  }

  public boolean useBkgColorForRef() {
    return props.isBkgColorForRef();
  }

  public int getRecordTimeout() {
    return props.getRecordTimeout();
  }

  public boolean isLogClientMessages() {
    return props.isLogClientMessages();
  }

  public String getLanguage() {
    return projectStartupInfo != null ? projectStartupInfo.getLanguage() : "";
  }

  public Language getLanguageInfo() {
    return projectStartupInfo != null ? projectStartupInfo.getLanguageInfo() : Language.UNKNOWN;
  }

  public boolean isRightAlignContent() {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    return projectStartupInfo != null && projectStartupInfo.getLanguageInfo().isRTL();
  }

  // services

  /**
   * These two services are special - they depend on which project/language is living on
   * which hydra host (hydra1 or hydra2 or ...)
   *
   * @return
   */
  public AudioServiceAsync getAudioService() {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    if (projectStartupInfo == null) {
      logger.info("\ngetAudioService has no project yet... using default audio service...?");
      if (userManager.getCurrent() != null) {
        setProjectStartupInfo(userManager.getCurrent());
      }
      if (getProjectStartupInfo() == null) {
        logger.warning("\n after getting user ... getAudioService has no project yet... using default audio service...?");
      }
    }
    AudioServiceAsync audioServiceAsync = projectStartupInfo == null ?
        defaultAudioService :
        projectToAudioService.get(projectStartupInfo.getProjectid());

    if (audioServiceAsync == null) logger.warning("getAudioService no audio service for " + projectStartupInfo);
    return audioServiceAsync == null ? defaultAudioService : audioServiceAsync;
  }

  public Collection<AudioServiceAsync> getAllAudioServices() {
    return new HashSet<>(projectToAudioService.values());
  }

  public void tellHydraServerToRefreshProject(int projID) {
    getScoringService().configureAndRefresh(projID, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        messageHelper.handleNonFatalError("Updating project on hydra server.", caught);
      }

      @Override
      public void onSuccess(Void result) {
        logger.info("updateProject did update on project #" + projID + " on hydra server (maybe h2).");
      }
    });
  }

  /**
   * Find host-specific scoring service - e.g. msa is on hydra2
   *
   * @return
   */
  public ScoringServiceAsync getScoringService() {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    if (projectStartupInfo == null) {
      logger.info("getScoringService has no project yet...");
    }
    ScoringServiceAsync audioServiceAsync = projectStartupInfo == null ?
        defaultScoringServiceAsync :
        projectToScoringService.get(projectStartupInfo.getProjectid());
    if (audioServiceAsync == null) logger.warning("getScoringService no audio service for " + projectStartupInfo);
    return audioServiceAsync == null ? defaultScoringServiceAsync : audioServiceAsync;
  }

  /**
   * @return
   */
  @Override
  public String getHost() {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    if (projectStartupInfo == null) {
      logger.warning("\n\n\ngetAudioService has no project yet...");
      return "";
    } else {
      String host = startupInfo.getHost(projectStartupInfo.getProjectid());
      //     logger.info("host for " + projectStartupInfo.getProjectid() + " = '" + host +"'");
      return host;
    }
  }


  public LangTestDatabaseAsync getService() {
    return service;
  }

  public UserServiceAsync getUserService() {
    return userService;
  }

  public UserManager getUserManager() {
    return userManager;
  }

  public QCServiceAsync getQCService() {
    return qcServiceAsync;
  }

  public ExerciseServiceAsync getExerciseService() {
    return exerciseServiceAsync;
  }

  @Override
  public DialogServiceAsync getDialogService() {
    return dialogServiceAsync;
  }

  public ListServiceAsync getListService() {
    return listServiceAsync;
  }

  @Override
  public DLIClassServiceAsync getDLIClassService() {
    return dliClassServiceAsync;
  }

  public UserFeedback getFeedback() {
    return this;
  }
  // recording methods...

  /**
   * @param wavStreamCallback
   * @see PostAudioRecordButton#startRecording
   */
  public void startStream(ClientAudioContext clientAudioContext, WavStreamCallback wavStreamCallback) {
    AudioServiceAsync audioService = getAudioService();
    String serviceEntryPoint = ((ServiceDefTarget) audioService).getServiceEntryPoint();
    BrowserRecording.recordOnClick();
    BrowserRecording.startStream(serviceEntryPoint, clientAudioContext, wavStreamCallback);
  }

  /**
   * Recording interface
   *
   * @see RecordButton.RecordingListener#stopRecording(long, boolean)
   */
  public void stopRecording(boolean useDelay, boolean abort) {
   logger.info("stopRecording : " +
       "\n\ttime recording in UI " + (System.currentTimeMillis() - then) + " millis, " +
       "\n\tabort                " + abort +
       "\n\tuse delay            " + useDelay);

    if (useDelay) {
      BrowserRecording.stopRecording(abort);
    } else {
      BrowserRecording.stopWebRTCRecording(abort);
    }
  }

  /**
   * Recording interface
   *
   * @see RehearseViewHelper#RehearseViewHelper
   */
  public void registerStopDetected(WavEndCallback wavEndCallback) {
    this.wavEndCallback = wavEndCallback;
  }

  public SoundManagerAPI getSoundManager() {
    return soundManager;
  }

  public void showErrorMessage(String title, String msg) {
    DialogHelper dialogHelper = new DialogHelper(false);
    dialogHelper.showErrorMessage(title, msg);
    logMessageOnServer("Showing error message", title + " : " + msg, true);
  }

  /**
   * @param listener
   * @see mitll.langtest.client.recorder.FlashcardRecordButton#FlashcardRecordButton
   */
  @Override
  public void addKeyListener(KeyPressHelper.KeyListener listener) {
    keyPressHelper.addKeyHandler(listener);
    if (keyPressHelper.getSize() > 1) {
      logger.info("addKeyListener " + listener.getName() + " ( " + keyPressHelper.getSize() +
          ")" +
          "key press handler now " + keyPressHelper);
    }
  }

  @Override
  public boolean removeKeyListener(KeyPressHelper.KeyListener listener) {
    return keyPressHelper.removeKeyHandler(listener);
  }

  @Override
  public boolean shouldRecord() {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    boolean hasModel = (projectStartupInfo != null) && projectStartupInfo.isHasModel();
    return hasModel && isRecordingEnabled();
  }

  @Override
  public boolean isRecordingEnabled() {
    return BrowserRecording.gotPermission();
  }

  /**
   * @return
   * @see
   */
  public boolean isMicAvailable() {
    return isMicConnected;
  }

  public StartupInfo getStartupInfo() {
    return startupInfo;
  }

  /**
   * @see ChangePasswordView#changePassword
   */
  public void logout() {
    initialUI.logout();
    keyPressHelper.clearListeners();
  }

  public KeyStorage getStorage() {
    if (storage == null) {
      storage = new KeyStorage(this);
    }
    return storage;
  }

  @Override
  public void showListIn(int listID, INavigation.VIEWS views) {
    getNavigation().showListIn(listID, views);
  }

  /**
   * @param views
   * @return
   * @see WordContainerAsync#gotClickOnLearn
   */
  @Override
  public ShowTab getShowTab(INavigation.VIEWS views) {
    return getNavigation().getShowTab(views);
  }

  @Override
  public void setBannerVisible(boolean visible) {
    getNavigation().setBannerVisible(visible);
  }

  public INavigation getNavigation() {
    return initialUI.getNavigation();
  }

  @Override
  public CommentAnnotator getCommentAnnotator() {
    return annotationHelper;
  }


  @Override
  public MessageHelper getMessageHelper() {
    if (messageHelper == null) {
      messageHelper = new MessageHelper(initialUI, this);
    }
    return messageHelper;
  }

  public void handleNonFatalError(String message, Throwable throwable) {
    messageHelper.handleNonFatalError(message, throwable);
  }

  public OpenUserServiceAsync getOpenUserService() {
    return openUserService;
  }

  public boolean isHasNetworkProblem() {
    return hasNetworkProblem;
  }

  public void setHasNetworkProblem(boolean hasNetworkProblem) {
    this.hasNetworkProblem = hasNetworkProblem;
  }
}
