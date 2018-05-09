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

// called from initWebAudio
function startUserMedia(stream) {
    var input = audio_context.createMediaStreamSource(stream);
    // __log('Media stream created.');

    recorder = new Recorder(input);
//    __log('Recorder initialised.');

    rememberedInput = input;
    webAudioMicAvailable();
    document.addEventListener('webkitvisibilitychange', onVisibilityChange);
}

// if the user goes to another tab or changes focus, stop recording.
function onVisibilityChange() {
    if (document.webkitHidden) {
        //  __log('webkitHidden');

        if (rememberedInput) {
            recorder && recorder.stop();
            audio_context && audio_context.suspend();
            //        __log('Stopped recording.');
        }
    } else {
//        __log('webkitRevealed');
    }
}

var start = new Date().getTime();

// fix for bug where chrome prevents recording unless calls resume first
// see https://developers.google.com/web/updates/2017/09/autoplay-policy-changes#webaudio
function startRecording() {
    recorder && recorder.clear();
    audio_context && audio_context.resume();
    recorder && recorder.record();
    //    __log('Start Recording. ' + recorder);
}

// called from FlashRecordPanelHeadless.stopRecording
function stopRecording() {
    recorder && recorder.stop();
    audio_context && audio_context.suspend();
    // __log('Stop Recording.');
    //   var end = new Date().getTime();
    //  __log("duration " + (end-start));
    // get WAV from audio data blob
    grabWav();
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
            //      __log("grabWav");

            var arrayBuffer;
            reader.onloadend = function () {
                arrayBuffer = reader.result;

                var myArray = new Uint8Array(arrayBuffer);

                //        __log("grabWav onloadend " + myArray.length);

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
        //console.error(e);
        webAudioMicNotAvailable();
    }

    if (gotAudioContext) {
        try {
            if (navigator.getMedia) {
             //   __log('initWebAudio getMedia ...');
                navigator.getMedia({audio: true}, startUserMedia, function (e) {
                    __log('initWebAudio No live audio input: ' + e);
                    __log('initWebAudio name: ' + e.name);
                    if (e.name.startsWith("NotAllowedError")) {
                        webAudioPermissionDenied();
                    }
                    //console.error(e);
                    webAudioMicNotAvailable();
                });
            }
            else {
                __log('initWebAudio getMedia null - no mic.');
                webAudioMicNotAvailable();
            }
        } catch (e) {
            __log('initWebAudio No navigator.getMedia in this browser!');
            // console.error(e);
            webAudioMicNotAvailable();
        }
    }

    navigator.mediaDevices.ondevicechange = function (event) {
        __log("got device change... ");
        location.reload();
        //      initWebAudio();
        //updateDeviceList();
    };

//    updateDeviceList();
}

function updateDeviceList() {
    navigator.mediaDevices.enumerateDevices()
        .then(function (devices) {

            var newmics = [];
            devices.forEach(function (device) {
                var isMatch = device.kind === "audioinput";
                // __log("got device " + device.label);
                //
                if (isMatch) {
                    __log("got mic " + device.label);
                    newmics.push(device.deviceId);
                    // __log("got kind " + device.kind);
                    // __log("got groupId " + device.groupId );
                    //  __log("got deviceId " + device.deviceId );
                    //  audioList.appendChild(elem);
                }
                else {
                    __log("got not a mic " + device.label);
                    __log("got kind " + device.kind);
                    __log("got groupId " + device.groupId);
                    __log("got deviceId " + device.deviceId);

                }
                //else if (type === "video") {
                //     __log("got video " + message);
                // }
            });

            newmics.sort();

            if (newmics.length !== mics.length) {
                __log("got mics " + newmics.length + " before " + mics.length);
                // initWebAudio();
            }
            else {
                __log("no change mics " + newmics.length + " before " + mics.length);
            }

            mics = newmics;
            // else {
            //     for (i = 0; i < newmics.length; i++) {
            //         __log("mic " + i + " " + newmics[i]);
            //         if (newmics[i] !== mics[i]) {
            //             __log("different mic " + i + " " + newmics[i]);
            //             break;
            //         }
            //     }
            // }
        });
}
