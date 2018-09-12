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

package mitll.langtest.server.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.services.AudioService;
import mitll.langtest.server.FileSaver;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.ScoreServlet.HeaderValue;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.imagewriter.SimpleImageWriter;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.audio.AudioInfo;
import mitll.langtest.server.database.audio.EnsureAudioHelper;
import mitll.langtest.server.database.audio.IEnsureAudioHelper;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.ProjectHelper;
import mitll.langtest.server.scoring.JsonScoring;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.RecalcRefResponse;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static mitll.langtest.server.ScoreServlet.HeaderValue.*;

/**
 * does image generation here too - since it's done from a file.
 */
@SuppressWarnings("serial")
public class AudioServiceImpl extends MyRemoteServiceServlet implements AudioService {
  private static final Logger logger = LogManager.getLogger(AudioServiceImpl.class);

  private static final boolean DEBUG = false;

  private static final int WARN_THRESH = 10;
  public static final String UNKNOWN = "unknown";
  private static final String REQID = "reqid";
  private static final String UTF_8 = "UTF-8";

  // TODO : make these enums...
  public static final String START = "START";
  private static final String STREAM = "STREAM";

  enum STEAMSTATES {START, STREAM, END, ABORT}

  private static final String MESSAGE = "message";
  private static final String NO_SESSION = "no session";

  private PathWriter pathWriter;
  private IEnsureAudioHelper ensureAudioHelper;
  private AudioCheck audioCheck;

  private final LoadingCache<Long, List<AudioChunk>> sessionToChunks = CacheBuilder.newBuilder()
      .maximumSize(10000)
      .expireAfterWrite(2, TimeUnit.MINUTES)
      .build(
          new CacheLoader<Long, List<AudioChunk>>() {
            @Override
            public List<AudioChunk> load(Long key) {
              return new ArrayList<>();
            }
          });

  /**
   * Sanity checks on answers and bestAudio dir
   */
  @Override
  public void init() {
    super.init();
    pathWriter = new PathWriter(serverProps);
    ensureAudioHelper = new EnsureAudioHelper(db, pathHelper);
    audioCheck = new AudioCheck(serverProps.shouldTrimAudio(), serverProps.getMinDynamicRange());
    pathWriter.doSanityCheckOnDir(new File(serverProps.getAnswerDir()), " answers dir ");
  }

  /**
   * This allows us to upload an exercise file.
   *
   * This might be helpful if we want to stream audio in a simple way outside a GWT RPC call.
   *
   * @throws ServletException
   * @throws IOException
   * @paramx request
   * @paramx response
   */
  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    ServletRequestContext ctx = new ServletRequestContext(request);
    String contentType = ctx.getContentType();
    //  String requestType = getRequestType(request);

//    logger.info("service : service content type " + contentType + " " + requestType);/// + " multi " + isMultipart);
    if (contentType.equalsIgnoreCase("application/wav")) {
      //reportOnHeaders(request);

      try {
        JSONObject jsonForStream = getJSONForStream(request, ScoreServlet.PostRequest.ALIGN, "", "");
        configureResponse(response);
        reply(response, jsonForStream);
      } catch (Exception e) {
        logger.warn("got " + e, e);
        throw new ServletException("Got getJSONForStream : " + e, e);
      }
      // logger.debug("service : Request " + request.getQueryString() + " path " + request.getPathInfo());
//      FileUploadHelper.UploadInfo uploadInfo = db.getProjectManagement().getFileUploadHelper().gotFile(request);
//      if (uploadInfo == null) {
//        super.service(request, response);
//      } else {
//        db.getProjectManagement().getFileUploadHelper().doUploadInfoResponse(response, uploadInfo);
//      }
    } else {
      super.service(request, response);
    }
  }

  /**
   * @param response
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  private void configureResponse(HttpServletResponse response) {
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding(UTF_8);
  }

  private void reply(HttpServletResponse response, JSONObject jsonObject) {
    reply(response, jsonObject.toString());
  }

  private void reply(HttpServletResponse response, String x) {
    try {
      PrintWriter writer = response.getWriter();
      writer.println(x);
      writer.close();
    } catch (IOException e) {
      logger.error("got " + e, e);

    }
  }

/*  private final Set<String> notInteresting = new HashSet<>(Arrays.asList(
      "Accept-Encoding",
      "Accept-Language",
      "accept",
      "connection",
      "password",
      "pass"));*/

/*
  private void reportOnHeaders(HttpServletRequest request) {
    Enumeration<String> headerNames = request.getHeaderNames();
    Set<String> headers = new TreeSet<>();
    while (headerNames.hasMoreElements()) headers.add(headerNames.nextElement());
    List<String> collect = headers.stream().filter(name -> !notInteresting.contains(name)).collect(Collectors.toList());
    collect.forEach(header -> logger.info("\trequest header " + header + " = " + request.getHeader(header)));
  }
*/

  /**
   * TODO : consider how to handle out of order packets
   *
   * TODO : wait if saw END but packets out of order...?
   *
   * @param request
   * @param requestType
   * @param deviceType  TODO fill in?
   * @param device      TODO fill in?
   * @return
   * @throws IOException
   * @throws DominoSessionException
   * @throws ExecutionException
   * @see #service(HttpServletRequest, HttpServletResponse)
   */
  private JSONObject getJSONForStream(HttpServletRequest request,
                                      ScoreServlet.PostRequest requestType,
                                      String deviceType,
                                      String device) throws IOException, DominoSessionException, ExecutionException {
    int userIDFromSession = -1;
    try {
      userIDFromSession = checkSession(request);
    } catch (DominoSessionException dse) {
      logger.info("getJsonForAudio got " + dse);
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MESSAGE, NO_SESSION);
      return jsonObject;
    }

    // if (true) throw new IllegalArgumentException("dude!");

    int realExID = getRealExID(request);
    int reqid = getReqID(request);
    int projid = getProjectID(request);

    //String postedWordOrPhrase = "";

    boolean isRef = isReference(request);
    AudioType audioType = getAudioType(request);

    if (DEBUG) {
      logger.info("getJSONForStream got" +
          "\n\trequest  " + requestType +
          "\n\tprojid   " + projid +
          "\n\texid     " + realExID +
          "\n\texercise text " + realExID +
          "\n\treq      " + reqid +
          "\n\tref      " + isRef +
          "\n\taudio type " + audioType +
          "\n\tdevice   " + deviceType + "/" + device
      );
    }

    long session = getStreamSession(request);
    int packet = getStreamPacket(request);

    String state = getHeader(request, STREAMSTATE);
    String timestamp = getHeader(request, STREAMTIMESTAMP);

    byte[] targetArray = IOUtils.toByteArray(request.getInputStream());
    AudioChunk newChunk = new AudioChunk(packet, targetArray);

    long then = System.currentTimeMillis();
    Validity validity = newChunk.calcValid(audioCheck, isRef, serverProps.isQuietAudioOK());
    //if (validity != Validity.OK)
    logger.info("getJSONForStream : (" + state + ") chunk for exid " + realExID + " " + newChunk + " is " + validity + " : " + newChunk.getValidityAndDur());

/*
    long now = System.currentTimeMillis();
    logger.info("took " + (now - then) + " to calc validity = " + newChunk.getValidityAndDur());
*/

    // little state machine -
    // START - new buffering,
    // STREAM concat or append,
    // END write file and score

    List<AudioChunk> audioChunks = sessionToChunks.get(session);
    audioChunks.add(newChunk);

    JSONObject jsonObject = new JSONObject();

    new JsonScoring(getDatabase()).addValidity(realExID, jsonObject, validity, "" + reqid);

    if (state.equalsIgnoreCase(START)) {
    } else if (state.equalsIgnoreCase(STREAM)) {
  /*  if (audioChunks.size() == 1) {
        AudioChunk audioChunk = audioChunks.get(0);
        if (audioChunk.getPacket()== packet-1) {
          AudioChunk combined = newChunk.concat(audioChunk);
          if (combined == null) logger.error("huh? can't combine???");
          else {
            audioChunks.remove(0);
          }
        }
      }*/
    } else {  // STOP
      long then2 = System.currentTimeMillis();
      jsonObject = getJsonObject(deviceType, device, userIDFromSession, realExID, reqid, projid,
          isRef, audioType,
          audioChunks, jsonObject);

      long now2 = System.currentTimeMillis();
      logger.info("getJsonObject took " + (now2 - then2) + " for  " + realExID + " req " + reqid);
    }
    // so we get a packet - if it's the next one in the sequence, combine it with the current one and replace it
    // otherwise, we'll have to make a list and combine them...

    jsonObject.put(MESSAGE, state);
    jsonObject.put(STREAMTIMESTAMP.toString(), timestamp);
    jsonObject.put(STREAMSPACKET.toString(), packet);

    return jsonObject;
  }

  @NotNull
  private AudioType getAudioType(HttpServletRequest request) {
    String header = getHeader(request, AUDIOTYPE);
    try {
      return AudioType.valueOf(header.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("getAudioType " + header);
      return AudioType.LEARN;
    }
  }

  private JSONObject getJsonObject(String deviceType, String device, int userIDFromSession,
                                   int realExID, int reqid, int projid,
                                   boolean isReference,
                                   AudioType audioType,
                                   List<AudioChunk> audioChunks, JSONObject jsonObject) throws IOException, DominoSessionException {
    AudioChunk combined = getCombinedAudioChunk(audioChunks);

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(combined.getWavFile());
//      logger.info("getJSONForStream Session " + session + " state " + state + " packet " + packet);
    String language = getProject(projid).getLanguage();
    File saveFile = new FileSaver().writeAudioFile(pathHelper,
        byteArrayInputStream,
        realExID,
        userIDFromSession,
        language, true);

    AudioContext audioContext = new AudioContext(
        reqid,
        userIDFromSession,
        projid,
        language,
        realExID,
        1,
        audioType);

    logger.info("audio context " + audioContext);

    DecoderOptions decoderOptions = new DecoderOptions()
        .setDoDecode(true)
        .setDoAlignment(true)
        .setRecordInResults(true)
        .setRefRecording(isReference)
        .setAllowAlternates(false)
        .setCompressLater(false);

    AudioAnswer audioAnswer = getAudioAnswer(null, // not doing it!
        audioContext,
        deviceType,
        device,
        decoderOptions,
        saveFile,
        projid);

    jsonObject = new JsonScoring(getDatabase())
        .getJsonObject(
            projid,
            realExID,
            decoderOptions,
            false,
            jsonObject, false, audioAnswer, true);

    logger.info("getJSONForStream getJsonForAudio save file to " + saveFile.getAbsolutePath());
    return jsonObject;
  }

  /**
   * TODO : deal with gaps!
   *
   * Wait for arrival?
   *
   * @param audioChunks
   * @return
   */
  private AudioChunk getCombinedAudioChunk(List<AudioChunk> audioChunks) {
    long then = System.currentTimeMillis();
    //logger.info("Stop - combine " + audioChunks.size());

    audioChunks.sort(AudioChunk::compareTo);

    AudioChunk combined = audioChunks.get(0);
    //    logger.info("Stop - combine " + combined);

    for (int i = 1; i < audioChunks.size(); i++) {
      AudioChunk next = audioChunks.get(i);
      //      logger.info("\tStop - 1 combine " + combined);
      //     logger.info("\tStop - next " + next);

      // ordering check...
      {
        int packet = combined.getPacket();
        int packet1 = next.getPacket();
        if (packet != packet1 - 1) {
          logger.warn("getCombinedAudioChunk : hmm current packet " + packet + " vs next " + packet1);
        }
      }
      combined = combined.concat(next);
      //      logger.info("\tStop - 2 combine " + combined);

    }
    long now = System.currentTimeMillis();

    logger.info("Stop - finally combine " + combined + " in " + (now - then) + " millis");
    return combined;
  }

  private static class AudioChunk implements Comparable<AudioChunk> {
    private final int packet;
    private boolean combined;
    private final byte[] wavFile;
    // private boolean isValid = true;

    private AudioCheck.ValidityAndDur validityAndDur;

    AudioChunk(int packet, byte[] wavFile) {
      this.packet = packet;
      this.wavFile = wavFile;
    }

    AudioChunk(int packet, boolean combined, byte[] wavFile) {
      this.packet = packet;
      this.combined = combined;
      this.wavFile = wavFile;
    }

    AudioChunk concat(AudioChunk other) {
      try {
        AudioInputStream clip1 = getAudioInputStream();
        AudioInputStream clip2 = other.getAudioInputStream();

        AudioInputStream appendedFiles =
            new AudioInputStream(
                new SequenceInputStream(clip1, clip2),
                clip1.getFormat(),
                clip1.getFrameLength() + clip2.getFrameLength());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        AudioSystem.write(appendedFiles, AudioFileFormat.Type.WAVE, byteArrayOutputStream);

        return new AudioChunk(other.getPacket(), true, byteArrayOutputStream.toByteArray());
      } catch (Exception e) {
        logger.error("got " + e, e);
      }
      return null;
    }

    private AudioInputStream getAudioInputStream() throws UnsupportedAudioFileException, IOException {
      return AudioSystem.getAudioInputStream(getStream());
    }

    @NotNull
    public InputStream getStream() {
      return new ByteArrayInputStream(wavFile);
    }

    @Override
    public int compareTo(@NotNull AudioChunk o) {
      return Integer.compare(packet, o.packet);
    }

    int getPacket() {
      return packet;
    }

    public boolean isCombined() {
      return combined;
    }

    byte[] getWavFile() {
      return wavFile;
    }

    /**
     * @param audioCheck
     * @param useSensitiveTooLoudCheck
     * @param quietAudioOK
     * @see #getJSONForStream
     */
    Validity calcValid(AudioCheck audioCheck, boolean useSensitiveTooLoudCheck, boolean quietAudioOK) {
      try {
        validityAndDur = audioCheck.isValid(
            "" + packet,
            "",
            wavFile.length,
            getAudioInputStream(),
            useSensitiveTooLoudCheck,
            quietAudioOK
        );

//        long then = System.currentTimeMillis();
        audioCheck.maybeAddDNR("" + packet, getAudioInputStream(), validityAndDur);
        //      long now = System.currentTimeMillis();
        //    logger.info("dnr took " + (now - then) + " millis");
        return validityAndDur.getValidity();
      } catch (Exception e) {
        logger.error("got " + e, e);
      }
      return null;
    }

    public boolean isValid() {
      return validityAndDur != null && validityAndDur.isValid();
    }

    public String toString() {
      return "#" + packet + " : " + isValid() +
          (combined ? " combined " : "") +
          " len " + wavFile.length + " " + validityAndDur;
    }

    AudioCheck.ValidityAndDur getValidityAndDur() {
      return validityAndDur;
    }
  }

  private int checkSession(HttpServletRequest request) throws DominoSessionException {
    int userIDFromSession = securityManager.getUserIDFromSessionLight(request);
//    logger.info("checkSession user id from session is " + userIDFromSession);
    return userIDFromSession;
  }

  /**
   * @param request
   * @return
   * @see #doGet
   */
  private int getProjectID(HttpServletRequest request) {
    int userIDFromRequest = securityManager.getUserIDFromRequest(request);
    if (userIDFromRequest == -1) {
      return -1;
    } else {
      return getMostRecentProjectByUser(userIDFromRequest);
    }
  }

/*  private String getRequestType(HttpServletRequest request) {
    return getHeader(request, ScoreServlet.HeaderValue.REQUEST);
  }*/

  private int getMostRecentProjectByUser(int id) {
    return getDatabase().getUserProjectDAO().getCurrentProjectForUser(id);
  }

  /**
   * @param request
   * @return
   * @seex #getJsonForAudio
   */
  private int getReqID(HttpServletRequest request) {
    String reqid = request.getHeader(REQID);
    //logger.debug("got req id " + reqid);
    if (reqid == null) reqid = "1";
    try {
      //logger.debug("returning req id " + req);
      return Integer.parseInt(reqid);
    } catch (NumberFormatException e) {
      logger.warn("Got parse error on reqid " + reqid);
    }
    return 1;
  }

  private int getRealExID(HttpServletRequest request) {
    int realExID = 0;
    try {
      realExID = Integer.parseInt(getExerciseHeader(request));
      if (realExID == -1) {
        realExID = getDatabase().getUserExerciseDAO().getUnknownExercise().id();
        logger.info("getJsonForAudio : using unknown exercise id " + realExID);
      } else {
//        logger.info("getJsonForAudio got exercise id " + realExID);
      }
    } catch (NumberFormatException e) {
      logger.info("couldn't parse exercise request header = '" + getExerciseHeader(request) + "'");
    }
    return realExID;
  }

  private long getStreamSession(HttpServletRequest request) {
    String header = request.getHeader(STREAMSESSION.toString());
    try {
      long l = Long.parseLong(header);
      return l;
    } catch (NumberFormatException e) {
      logger.error("couldn't parse " + header);
      return System.currentTimeMillis();
    }
  }

  private int getStreamPacket(HttpServletRequest request) {
    return request.getIntHeader(STREAMSPACKET.toString());
  }

  private String getExerciseHeader(HttpServletRequest request) {
    return getHeader(request, EXERCISE);
  }

  private boolean isReference(HttpServletRequest request) {
    String header = request.getHeader(ISREFERENCE.toString());
    return header != null && header.equalsIgnoreCase("true");
  }

  private String getHeader(HttpServletRequest request, HeaderValue resultId) {
    return request.getHeader(resultId.toString());
  }

  /**
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   * <p>
   * A side effect is to set the first state to UNSET if it was APPROVED
   * and to set the second state (not really used right now) to RECORDED
   * <p>
   * <p>
   * Wade has observed that audio normalization really messes up the ASR -- silence doesn't appear as silence after you multiply
   * the signal.  Also, the user doesn't get feedback that their mic gain is too high/too low or that they
   * are speaking too softly or too loudly
   * <p>
   * Client references below:
   *
   * @param base64EncodedString generated by flash on the client
   * @param deviceType
   * @param device
   * @return AudioAnswer object with information about the audio on the server, including if audio is valid (not too short, etc.)
   * @paramx doFlashcard         true if called from practice (flashcard) and we want to do decode and not align
   * @paramx recordInResults     if true, record in results table -- only when recording in a learn or practice tab
   * @paramx addToAudioTable     if true, add to audio table -- only when recording reference audio for an item.
   * @paramx allowAlternates
   * @see RecordButton.RecordingListener#stopRecording
   * @see RecordButton.RecordingListener#stopRecording
   */
  @Override
  public AudioAnswer writeAudioFile(String base64EncodedString,
                                    AudioContext audioContext,

                                    String deviceType,
                                    String device,
                                    DecoderOptions decoderOptions
  ) throws DominoSessionException {
    return getAudioAnswer(base64EncodedString, audioContext,
        deviceType, device, decoderOptions, null, getProjectIDFromUser());
  }

  /**
   * @param base64EncodedString
   * @param audioContext
   * @param deviceType
   * @param device
   * @param decoderOptions
   * @param fileInstead
   * @param projectID
   * @return
   * @throws DominoSessionException
   * @see #getJSONForStream
   */
  private AudioAnswer getAudioAnswer(String base64EncodedString,
                                     AudioContext audioContext,
                                     String deviceType,
                                     String device,
                                     DecoderOptions decoderOptions,
                                     File fileInstead,
                                     int projectID) throws DominoSessionException {
    Project project = db.getProject(projectID);
    boolean hasProjectSpecificAudio = project.hasProjectSpecificAudio();
    AudioFileHelper audioFileHelper = getAudioFileHelper(project);

    int exerciseID = audioContext.getExid();
    boolean isExistingExercise = exerciseID > 0;

    if (decoderOptions.isRefRecording() && !decoderOptions.isRecordInResults()) { // we have a foreign key from audio into result table - must record in results
      decoderOptions.setRecordInResults(true);
    }
    boolean amas = serverProps.isAMAS();

    CommonExercise commonExercise = amas || isExistingExercise ?
        db.getCustomOrPredefExercise(projectID, exerciseID) :
        db.getUserExerciseDAO().getTemplateExercise(db.getProjectDAO().getDefault());

    int audioContextProjid = audioContext.getProjid();

    if (audioContextProjid != projectID)
      logger.error("huh? session project " + projectID + " vs " + audioContextProjid);

    String language = db.getProject(audioContextProjid).getLanguage();

    if (!isExistingExercise) {
      ((Exercise) commonExercise).setProjectID(audioContextProjid);
      audioContext.setExid(commonExercise.getID());
    }

    CommonShell exercise1 = amas ? db.getAMASExercise(exerciseID) : commonExercise;

    if (exercise1 == null && isExistingExercise) {
      logger.warn("writeAudioFile " + getLanguage() + " : couldn't find exerciseID with id '" + exerciseID + "'");
    }
    String audioTranscript = getAudioTranscript(audioContext.getAudioType(), commonExercise);
    AnswerInfo.RecordingInfo recordingInfo =
        new AnswerInfo.RecordingInfo("", "", deviceType, device, audioTranscript, "");

//    logger.info("writeAudioFile recording info " + recordingInfo);
    AudioAnswer audioAnswer =
        audioFileHelper.writeAudioFile(
            base64EncodedString,
            fileInstead,
            commonExercise,
            audioContext,

            recordingInfo, decoderOptions);

//    logger.info("writeAudioFile recording audioAnswer transcript '" + audioAnswer.getTranscript() + "'");
    int user = audioContext.getUserid();

    if (decoderOptions.isRefRecording() && audioAnswer.isValid()) {
      audioAnswer.setAudioAttribute(addToAudioTable(user, audioContext.getAudioType(),
          commonExercise, exerciseID, audioAnswer, hasProjectSpecificAudio));
    } //else {
    // So Wade has observed that this really messes up the ASR -- silence doesn't appear as silence after you multiply
    // the signal.  Also, the user doesn't get feedback that their mic gain is too high/too low or that they
    // are speaking too softly or too loudly.

    // normalizeLevel(audioAnswer);
    // }

    try {
      if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
        logger.warn("huh? got zero length recording " + user + " " + exerciseID);
        logEvent("audioRecording",
            "writeAudioFile", "" + exerciseID, "Writing audio - got zero duration!", user, device, projectID);
      } else {
        String path = audioAnswer.getPath();
        String actualPath = ensureAudioHelper.ensureCompressedAudio(
            user,
            commonExercise,
            path,
            audioContext.getAudioType(),
            language,
            new HashMap<>(),  //?
            !decoderOptions.shouldCompressLater());
        logger.info("writeAudioFile initial path " + path + " compressed actual " + actualPath);
        if (actualPath.startsWith(serverProps.getAudioBaseDir())) {
          actualPath = actualPath.substring(serverProps.getAudioBaseDir().length());
          // logger.info("Now " + actualPath);
        }
        audioAnswer.setPath(actualPath);
//        logger.info("wrote compressed...");
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }


    return audioAnswer;
  }

  private MiniUser.Gender getGender(int user) {
    User byID = db.getUserDAO().getByID(user);
    return byID == null ? MiniUser.Gender.Unspecified : byID.getRealGender();
  }

  /**
   * @param projectid
   * @see mitll.langtest.client.project.ProjectEditForm#getCheckAudio
   */
  @Override
  public void checkAudio(int projectid) {
    Project project = db.getProject(projectid);
    logger.info("checkAudio - for project " + projectid + " " + project);
    long then = System.currentTimeMillis();
    db.getAudioDAO().makeSureAudioIsThere(projectid, project.getLanguage(), true);
    long now = System.currentTimeMillis();
    if (now - then > WARN_THRESH) {
      logger.info("checkAudio : for project " + projectid + " " + project +
          " - took " + (now - then) + " millis to check audio");
    }

    new Thread(() -> ensureAudioHelper.ensureAudio(projectid), "checkAudio_" + projectid).start();
  }

  private void logEvent(String id, String widgetType, String exid, String context, int userid, String device, int projID) {
    try {
      db.logEvent(id, widgetType, exid, context, userid, device, projID);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Remember this audio as reference audio for this exercise, and possibly clear the APRROVED (inspected) state
   * on the exercise indicating it needs to be inspected again (we've added new audio).
   * <p>
   * Don't return a path to the normalized audio, since this doesn't let the recorder have feedback about how soft
   * or loud they are : https://gh.ll.mit.edu/DLI-LTEA/Development/issues/601
   *
   * @param user                    who recorded audio
   * @param audioType               regular or slow
   * @param exercise1               for which exercise - how could this be null?
   * @param exerciseID              perhaps sometimes we want to override the exercise id?
   * @param audioAnswer             holds the path of the temporary recorded file
   * @param hasProjectSpecificAudio
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @paramx realGender
   * @see #writeAudioFile
   */
  private AudioAttribute addToAudioTable(int user,
                                         AudioType audioType,
                                         CommonExercise exercise1,
                                         int exerciseID,
                                         AudioAnswer audioAnswer,
                                         boolean hasProjectSpecificAudio) {
    boolean noExistingExercise = exercise1 == null;
    int idToUse = noExistingExercise ? exerciseID : exercise1.getID();
    int projid = noExistingExercise ? -1 : exercise1.getProjectID();
    String audioTranscript = audioAnswer.getTranscript();
    String language = db.getProject(projid).getLanguage();
    //   logger.debug("addToAudioTable user " + user + " ex " + exerciseID + " for " + audioType + " path before " + audioAnswer.getPath());

    File absoluteFile = pathHelper.getAbsoluteAudioFile(audioAnswer.getPath());
    boolean isContext = audioType == AudioType.CONTEXT_REGULAR || audioType == AudioType.CONTEXT_SLOW;
    String context = noExistingExercise ? "" : isContext ? getEnglish(exercise1) : exercise1.getEnglish();

    if (!absoluteFile.exists()) logger.error("addToAudioTable huh? no file at " + absoluteFile.getAbsolutePath());
    String permanentAudioPath = pathWriter.
        getPermanentAudioPath(
            absoluteFile,
            getPermanentName(user, audioType),
            true,
            language,
            idToUse,
            serverProps,
            new TrackInfo(audioTranscript, getArtist(user), context, language));

    AudioAttribute audioAttribute = null;
    try {
      MiniUser.Gender realGender = getGender(user);

      AudioInfo info = new AudioInfo(user, idToUse, projid, audioType, permanentAudioPath, System.currentTimeMillis(),
          audioAnswer.getDurationInMillis(), audioTranscript, (float) audioAnswer.getDynamicRange(), audioAnswer.getResultID(),
          realGender, hasProjectSpecificAudio);

      audioAttribute = db.getAudioDAO().addOrUpdate(info);

      audioAnswer.setPath(audioAttribute.getAudioRef());
      logger.debug("addToAudioTable" +
          "\n\tuser " + user +
          "\n\tex " + exerciseID + "/" + idToUse +
          "\n\tfor " + audioType +
          "\n\taudio answer has " + audioAttribute);

      // what state should we mark recorded audio?
      setExerciseState(idToUse, user, exercise1);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return audioAttribute;
  }

  private String getEnglish(CommonExercise exercise1) {
    return exercise1.isContext() ? exercise1.getEnglish() : exercise1.getDirectlyRelated().iterator().next().getEnglish();
  }

  private String getAudioTranscript(AudioType audioType, CommonExercise exercise1) {
    return exercise1 == null ? "" :
        audioType.equals(AudioAttribute.CONTEXT_AUDIO_TYPE) ? exercise1.getContext() : exercise1.getForeignLanguage();
  }

  private String getPermanentName(int user, AudioType audioType) {
    return audioType.toString() + "_" + System.currentTimeMillis() + "_by_" + user + ".wav";
  }

  private String getArtist(int user) {
    User userWhere = db.getUserDAO().getUserWhere(user);
    return userWhere == null ? "" + user : userWhere.getUserID();
  }

  private File getAbsoluteFile(String path) {
    return pathHelper.getAbsoluteFile(path);
  }

  /**
   * Only change APPROVED to UNSET.
   *
   * @param exercise
   * @param user
   * @param exercise1
   */
  private void setExerciseState(int exercise, int user, Shell exercise1) {
    if (exercise1 != null) {
      STATE currentState = db.getStateManager().getCurrentState(exercise);
      if (currentState == STATE.APPROVED) { // clear approved on new audio -- we need to review it again
        db.getStateManager().setState(exercise1, STATE.UNSET, user);
      }
      db.getStateManager().setSecondState(exercise1, STATE.RECORDED, user);
    }
  }

  /**
   * TODO : pass in language so we can switch on language
   * <p>
   * Get an image of desired dimensions for the audio file - only for Waveform and spectrogram.
   * Also returns the audio file duration -- so we can deal with the difference in length between mp3 and wav
   * versions of the same audio file.  (The browser soundmanager plays mp3 and reports audio offsets into
   * the mp3 file, but all the images are generated from the shorter wav file.)
   * <p>
   * TODO : Worrying about absolute vs relative path is maddening.  Must be a better way!
   *
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param imageOptions
   * @param exerciseID
   * @return path to an image file
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio
   */
  public ImageResponse getImageForAudioFile(int reqid,
                                            String audioFile,
                                            String imageType,
                                            ImageOptions imageOptions,
                                            String exerciseID,
                                            String language) {
    if (audioFile.isEmpty())
      logger.error("getImageForAudioFile huh? audio file is empty for req id " + reqid + " exid " + exerciseID);

    SimpleImageWriter imageWriter = new SimpleImageWriter();

    String wavAudioFile = ensureAudioHelper.getWavAudioFile(audioFile, language);
    File testFile = new File(wavAudioFile);
    if (!testFile.exists() || testFile.length() == 0) {
      if (!testFile.exists()) {
        logger.error("getImageForAudioFile no audio at " + testFile.getAbsolutePath());
      } else if (testFile.length() == 0) {
        logger.error("getImageForAudioFile : huh? " + wavAudioFile + " is empty???");
      }
      return new ImageResponse();
    }
    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ?
            ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ?
                ImageType.SPECTROGRAM : null;
    if (imageType1 == null) {
      logger.error("getImageForAudioFile '" + imageType + "' is unknown?");
      return new ImageResponse(); // success = false!
    }
//    if (DEBUG || true) {
//      logger.debug("getImageForAudioFile : getting images (" + width + " x " + height + ") (" + reqid + ") type " + imageType +
//          " for " + wavAudioFile + "");
//    }

    long then = System.currentTimeMillis();

    String imageOutDir = pathHelper.getImageOutDir(language.toLowerCase());
    File absoluteImageDir = /*new File(language.toLowerCase(), imageOutDir);*/ getAbsoluteFile(imageOutDir);

    logger.info("getImageForAudioFile" +
        "\n\timageOutDir " + imageOutDir +
        "\n\tabs         " + absoluteImageDir +
        "\n\ttype        " + imageType1 +
        "\n\twavAudioFile " + wavAudioFile +
        "\n\ttestFile " + testFile +
        "\n\ttestFile len " + testFile.length()
    );
    String absolutePathToImage = imageWriter.writeImage(
        wavAudioFile,
        absoluteImageDir.getAbsolutePath(),
        imageOptions.getWidth(), imageOptions.getHeight(), imageType1, exerciseID);
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > WARN_THRESH) {
      logger.debug("getImageForAudioFile : got images " +
          // "(" + width + " x " + height + ")" +
          " (" + reqid + ") type " + imageType +
          "\n\tfor wav " + wavAudioFile +
          "\n\timage   " + absolutePathToImage +
          "\n\ttook    " + diff + " millis");
    }
    String installPath = pathHelper.getInstallPath();

    String relativeImagePath = absolutePathToImage;
    if (absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    } else {
      logger.error("getImageForAudioFile huh? file path " + absolutePathToImage + " doesn't start with " + installPath + "?");
    }

    relativeImagePath = pathHelper.ensureForwardSlashes(relativeImagePath);
    if (relativeImagePath.startsWith("/")) {
      relativeImagePath = relativeImagePath.substring(1);
    }
    String imageURL = relativeImagePath;
    double duration = new AudioCheck(serverProps.shouldTrimAudio(), serverProps.getMinDynamicRange()).getDurationInSeconds(wavAudioFile);
    if (duration == 0) {
      logger.error("huh? " + wavAudioFile + " has zero duration???");
    }

    logger.debug("getImageForAudioFile for" +
        "\n\taudio file " + wavAudioFile +
        "\n\ttype       " + imageType +
        "\n\trel path   " + relativeImagePath +
        "\n\turl        " + imageURL +
        "\n\tduration   " + duration);

    return new ImageResponse(reqid, imageURL, duration);
  }

  public RecalcRefResponse recalcRefAudio(int projid) {
    return db.getProject(projid).recalcRefAudio();
  }

  /**
   * Here so it's easy to test all the servers...
   *
   * @param subject
   * @param message
   * @param sendEmail
   */
  public void logMessage(String subject, String message, boolean sendEmail) {
    if (message.length() > 10000) message = message.substring(0, 10000);
    String prefixedMessage = "on " + getHostName() + " from client : " + message;

    prefixedMessage = getInfo(prefixedMessage);
    if (sendEmail) {
      logger.error(prefixedMessage);
    } else {
      logger.info(prefixedMessage);
    }

    if (sendEmail) {
      sendEmail("TEST : " + subject, getInfo(prefixedMessage));
    }
  }

  @Override
  public StartupInfo getStartupInfo() {
    return new StartupInfo(serverProps.getUIProperties(),
        new ProjectHelper().getProjectInfos(db, securityManager), "server", serverProps.getAffiliations());
  }

  /**
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#editItem
   */
  @Override
  public void editItem(ClientExercise userExercise, boolean keepAudio) throws DominoSessionException {
    getUserIDFromSessionOrDB();
    db.editItem(userExercise, keepAudio);
  }
}