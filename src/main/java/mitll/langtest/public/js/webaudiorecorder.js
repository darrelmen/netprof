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

function __log(e) {
    console.log(e + "  at " + new Date());
}

var audio_context;
var recorder;
var rememberedInput;
var allZero;
var mics = {};

var debug = false;
var start = new Date().getTime();

function startUserMediaAfterChoice(stream) {
    var input = audio_context.createMediaStreamSource(stream);
    if (debug) __log('startUserMediaAfterChoice Media stream created : ' + input);

    recorder = new Recorder(input);
    if (debug) __log('Recorder initialised.');

    rememberedInput = input;
    webAudioMicAvailable();
    document.addEventListener('webkitvisibilitychange', onVisibilityChange);
}

function madeChoice() {
    const audioInputSelect = document.querySelector('#audioSource');

    const audioSource = audioInputSelect.value;
    const constraints = {
        audio: {deviceId: audioSource ? {exact: audioSource} : undefined}
    };

    // console.log('choose mic: ' + audioSource);

    navigator.mediaDevices.getUserMedia(constraints)
        .then(function (stream) {
            /* use the stream */
            startUserMediaAfterChoice(stream);
        })
        .catch(function (err) {
            /* handle the error */
            __log('getUserMedia error: ' + err.name);

            if (err.name.startsWith("NotAllowedError")) {
                webAudioPermissionDenied();
            }
            webAudioMicNotAvailable();
        });
}

function gotDevices(deviceInfos) {
    // Handles being called several times to update labels. Preserve values.

    var ua = window.navigator.userAgent;
    if (ua.indexOf("Trident") > -1) {  // i.e. IE!
        webAudioMicNotAvailable();
        return;
    }

    const audioInputSelect = document.querySelector('#audioSource');
    const audioInputSelectButton = document.querySelector('#audioSourceButton');

    const selectors = [audioInputSelect];

    if (debug) __log('gotDevices : selectors ' + selectors);

    // IE doesn't like this
    const values = selectors.map(select => select.value);

    selectors.forEach(select => {
        while (select.firstChild) {
            select.removeChild(select.firstChild);
        }
    });

    if (debug) __log('gotDevices deviceInfos : ' + deviceInfos);

    for (let i = 0; i !== deviceInfos.length; ++i) {
        const deviceInfo = deviceInfos[i];
        if (debug) __log('gotDevices deviceInfo : ' + deviceInfo);

        const option = document.createElement('option');
        option.value = deviceInfo.deviceId;
        if (deviceInfo.kind === 'audioinput') {
            option.text = deviceInfo.label || `microphone ${audioInputSelect.length + 1}`;
            audioInputSelect.appendChild(option);
            if (debug) console.log('add mic: ', deviceInfo);
        } else {
            if (debug) console.log('Some other kind of source/device: ', deviceInfo);
        }
    }

    if (audioInputSelect && audioInputSelect.childElementCount == 0) {
        webAudioMicNotAvailable();
    }

    selectors.forEach((select, selectorIndex) => {
        if (Array.prototype.slice.call(select.childNodes).some(n => n.value === values[selectorIndex])) {
            select.value = values[selectorIndex];
        }
    });

    if (debug) __log('audioInputSelect : ' + audioInputSelect);
    if (debug) __log('audioInputSelectButton : ' + audioInputSelectButton);
    audioInputSelectButton.onclick = madeChoice;
}

function handleError(error) {
    console.log('navigator.MediaDevices.getUserMedia error: ', error.message, error.name);
}

// called from initWebAudio
function startUserMedia(stream) {
    var isSafari = navigator.vendor && navigator.vendor.indexOf('Apple') > -1 &&
        navigator.userAgent &&
        navigator.userAgent.indexOf('CriOS') == -1 &&
        navigator.userAgent.indexOf('FxiOS') == -1;

    if (isSafari) {
        navigator.mediaDevices.enumerateDevices().then(gotDevices).catch(handleError);
    } else {
        if (debug) __log('not safari...');

        var input = audio_context.createMediaStreamSource(stream);
        if (debug) __log('Media stream created : ' + input);

        recorder = new Recorder(input);
        if (debug) __log('Recorder initialised.');

        rememberedInput = input;
        webAudioMicAvailable();
    }

    document.addEventListener('webkitvisibilitychange', onVisibilityChange);

    /*    if (audio_context) {
            __log('webaudiorecorder.startUserMedia : state = ' +  audio_context.state);

            //} && audio_context.state === 'running')
            // {
            audio_context.suspend().then(function () {
                __log('webaudiorecorder.startUserMedia suspended recording...');
            });
        }*/
}


// if the user goes to another tab or changes focus, stop recording.
function onVisibilityChange() {
    if (document.webkitHidden) {
        //  __log('webkitHidden');

        if (rememberedInput) {
            recorder && recorder.stop();
            // audio_context && audio_context.suspend();
            //        __log('Stopped recording.');
        }
    } else {
//        __log('webkitRevealed');
    }
}

// fix for bug where chrome prevents recording unless calls resume first
// see https://developers.google.com/web/updates/2017/09/autoplay-policy-changes#webaudio
function startRecording() {
    audio_context && audio_context.resume();

    recorder && recorder.clear();
    recorder && recorder.record();

    //  && audio_context.state === 'suspended'
    /*     if (audio_context) {
             __log('webaudiorecorder.startRecording 1 Start Recording. ' +  audio_context.state);

             audio_context.resume().then(function () {
                 __log('webaudiorecorder.startRecording resumed recording...');
             //    rememberedInput.start();

                 recorder && recorder.clear();
                 recorder && recorder.record();
             });
         }*/

    //  __log('webaudiorecorder.startRecording 1 Start Recording.  state =' + audio_context.state);
}

// called from FlashRecordPanelHeadless.stopRecording
function stopRecording() {
    recorder && recorder.stop();
    // audio_context && audio_context.suspend();

    /*   if (audio_context) {
           __log('webaudiorecorder.stopRecording : state = ' + audio_context.state);

           //} && audio_context.state === 'running')
           // {
           audio_context.suspend().then(function () {
               __log('webaudiorecorder.stopRecording suspended recording...');
            //   rememberedInput.stop();


               recorder && recorder.clear();

           });
       }*/

    //  __log('webaudiorecorder.stopRecording');

    //   var end = new Date().getTime();
    //  __log("duration " + (end-start));
    // get WAV from audio data blob
    grabWav();
}

// see WebAudioRecorder.startStream
function serviceStartStream(url, exid, reqid, isreference, audioType, dialogSessionID, recordingSession) {
    //  __log('webaudiorecorder.startStream ');
    //  __log('webaudiorecorder.startStream calling recorder');

    recorder && recorder.serviceStartStream(url, exid, reqid, isreference, audioType, dialogSessionID, recordingSession,
        function (blob) {
            //      __log('startStream getStreamResponse.');
            getStreamResponse(blob);
        });
}

// WebAudioRecorder.doStopStream
function serviceStopStream(abort) {
    recorder && recorder.stop();
    //recorder && recorder.clear();

    /*    if (audio_context) {//} && audio_context.state === 'running') {
            __log('webaudiorecorder.serviceStopStream state = ' + audio_context.state);
            audio_context.suspend().then(function () {
                __log('webaudiorecorder.serviceStopStream suspended recording...');

                recorder && recorder.clear();
            });
        }*/

    recorder && recorder.serviceStopStream(abort, function (blob) {
        //  __log('serviceStopStream getStreamResponse.');
        getStreamResponse(blob);
    });
}

function uint6ToB64(nUint6) {
    return nUint6 < 26 ?
        nUint6 + 65
        : nUint6 < 52 ?
            nUint6 + 71
            : nUint6 < 62 ?
                nUint6 - 4
                : nUint6 === 62 ?
                    43
                    : nUint6 === 63 ?
                        47
                        :
                        65;

}

function bytesToBase64(aBytes) {
    var sB64Enc = "";

    for (var nMod3, nLen = aBytes.length, nUint24 = 0, nIdx = 0; nIdx < nLen; nIdx++) {
        nMod3 = nIdx % 3;
        if (nIdx > 0 && (nIdx * 4 / 3) % 76 === 0) {
            sB64Enc += "\r\n";
        }
        var aByte = aBytes[nIdx];
        nUint24 |= aByte << (16 >>> nMod3 & 24);
        if (nMod3 === 2 || aBytes.length - nIdx === 1) {
            sB64Enc += String.fromCharCode(uint6ToB64(nUint24 >>> 18 & 63), uint6ToB64(nUint24 >>> 12 & 63), uint6ToB64(nUint24 >>> 6 & 63), uint6ToB64(nUint24 & 63));
            nUint24 = 0;
        }
    }

    return sB64Enc.replace(/A(?=A$|$)/g, "=");
}

function getAllZero() {
    recorder && recorder.getAllZero(function (blob) {
        // console.log("Got " + blob);
        allZero = blob;
    });
}

// see #stopRecording
// see WebAudioRecorder#getBase64
function grabWav() {
    recorder && recorder.exportMonoWAV(function (blob) {
        try {
            var reader = new FileReader();
            __log("grabWav");

            var arrayBuffer;
            reader.onloadend = function () {
                arrayBuffer = reader.result;

                var myArray = new Uint8Array(arrayBuffer);

                __log("grabWav onloadend " + myArray.length);

                var bytes = bytesToBase64(myArray);
                getBase64(bytes);
            };

            reader.readAsArrayBuffer(blob);
        } catch (e) {
            __log('Bad call to blob');

            var vDebug = "";
            for (var prop in e) {
                vDebug += "property: " + prop + " value: [" + e[prop] + "]\n";
            }
            vDebug += "toString(): " + " value: [" + e.toString() + "]";
            __log(vDebug);
            throw e;
        }
    });
}

// see WebAudioRecorder#initWebaudio
function initWebAudio() {
    var gotAudioContext = false;
    try {
        // webkit shim
        window.AudioContext = window.AudioContext || window.webkitAudioContext;
        navigator.getMedia = (navigator.getUserMedia ||
            navigator.webkitGetUserMedia ||
            navigator.mozGetUserMedia ||
            navigator.msGetUserMedia);

        //         __log('Audio context is '+window.AudioContext);

        audio_context = new AudioContext;
        gotAudioContext = true;

        __log('initWebAudio sample rate = ' + audio_context.sampleRate +
            ' navigator.getUserMedia ' + (navigator.getMedia ? 'available.' : 'not present!'));
    } catch (e) {
        __log('initWebAudio No web audio support in this browser!');
        __log(e);
        //console.error(e);
        webAudioMicNotAvailable();
    }

    if (gotAudioContext) {
        try {
            if (navigator.mediaDevices) { // if more modern interface
                navigator.mediaDevices.getUserMedia({audio: true})
                    .then(function (stream) {
                        /* use the stream */
                        startUserMedia(stream);
                    })
                    .catch(function (err) {
                        /* handle the error */
                        __log('getUserMedia error: ' + err.name);

                        if (err.name.startsWith("NotAllowedError")) {
                            webAudioPermissionDenied();
                        }
                        webAudioMicNotAvailable();
                    });
            } else if (navigator.getMedia) {
                __log('initWebAudio (old) getMedia ...');
                navigator.getMedia(
                    {audio: true},  // only a mic
                    startUserMedia, // when you get it
                    function (e) {
                        __log('initWebAudio (old) No live audio input: ' + e);
                        __log('initWebAudio (old) error: ' + e.name);
                        if (e.name.startsWith("NotAllowedError")) {
                            webAudioPermissionDenied();
                        }
                        webAudioMicNotAvailable();
                    });
            } else {
                __log('initWebAudio getMedia null - no mic.');
                webAudioMicNotAvailable();
            }
        } catch (e) {
            __log('initWebAudio No navigator.getMedia in this browser!');
            // console.error(e);
            webAudioMicNotAvailable();
        }
    }

    if (navigator.mediaDevices) {
        navigator.mediaDevices.ondevicechange = function (event) {
//            __log("got device change... ");
            location.reload();
        };
    }
}
