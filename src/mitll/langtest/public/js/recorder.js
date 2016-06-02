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

var recorderHasConsole = (window.console || console.log);
var recordingStart;

function microphone_recorder_events() {
    //$('#status').append("<p>Microphone recorder event: " + arguments[0] + "  at " + new Date().getTime());
    if (recorderHasConsole) {
        console.log("got event " + arguments[0] + " at " + new Date().getTime());
    }
    switch (arguments[0]) {
        case "ready":
            //  $('#status').css({'color': '#000'}).append("<p>ready: ");
            try {
                FlashRecorderLocal.connect("recorderApp", 0);
            } catch (e) {
                console.log("got error " + e);
            }
            break;

        case "no_microphone_found":
            //  $('#status').css({'color': '#000'}).append("<p>no_microphone_found: ");
            noMicrophoneFound();
            break;

        case "microphone_user_request":
            //  $('#status').css({'color': '#000'}).append("<p>microphone_user_request: ");
            break;

        case "microphone_connected":
            var mic = arguments[1];
            //  $('#status').css({'color': '#000'}).append("<p>Microphone: " + mic.name);
            micConnected();
            break;

        case "microphone_not_connected":
            //console.log("got microphone_not_connected");
            //  $('#status').css({'color': '#000'}).append("<p>microphone_not_connected: ");
            micNotConnected();
            break;

        case "recording":
            var name = arguments[1];
            //  $('#status').css({'color': '#000'}).append("<p> recording - " + name + " at " + new Date().getTime());
            recordingStart = new Date().getTime();
            break;

        case "recording_stopped":
            var name = arguments[1];
            var duration = arguments[2];
            //$('#status').css({'color': '#000'}).append("<p> recording_stopped - " + name + " Duration: " + duration + "  at " + new Date().getTime());
            if (recorderHasConsole) console.log("recording duration " + (new Date().getTime() - recordingStart))
            break;
    }
}

FlashRecorderLocal = {
    recorder: null,
    uploadFormId: "#uploadForm",
    uploadFieldName: "upload_file[filename]",
    permitCalled: 0,

    connect: function (name, attempts) {
        //$('#status').css({'color': '#0F0'}).append("<p>connect called:  at " + new Date().getTime());

        if (navigator.appName.indexOf("Microsoft") != -1) {
            FlashRecorderLocal.recorder = window[name];
        } else {
            FlashRecorderLocal.recorder = document[name];
        }

        if (attempts >= 40) {
            return;
        }

        if (recorderHasConsole) {
            console.log("about to show showPrivacy");
        }

        // flash app needs time to load and initialize
        if (FlashRecorderLocal.recorder /*&& FlashRecorderLocal.recorder.init*/) {
            //$('#status').css({'color': '#0F0'}).append("<p>calling permit at " + new Date().getTime());
            if (recorderHasConsole) {
                console.log("FlashRecorderLocal.showPrivacy");
            }
            FlashRecorderLocal.recorder.showPrivacy();
            return;
        }
        else {
            if (recorderHasConsole) {
                console.log("no recorder?");
            }
        }

        setTimeout(function () {
            if (recorderHasConsole) {
                console.log("-- trying again - waiting for Flash to install recorder");
            }
            FlashRecorderLocal.connect(name, attempts + 1);
        }, 100);
    },

    record: function (name, filename) {
        //$('#status').css({'color': '#0F0'}).append("<p>record at " + new Date().getTime());
        FlashRecorderLocal.recorder.record(name, filename);
    },

    hide2: function () {
        FlashRecorderLocal.recorder.width = 8 + "px";
        FlashRecorderLocal.recorder.height = 8 + "px";
    },

    show: function () {
        FlashRecorderLocal.recorder.show();
    },

    hide: function () {
        FlashRecorderLocal.recorder.hide();
    },

    stop: function () {
        //$('#status').css({'color': '#0F0'}).append("<p>stop at " + new Date().getTime());
        FlashRecorderLocal.recorder.stopRecording();
    },

    getWav: function () {
        return FlashRecorderLocal.recorder.getwavbase64();
    },

    isMicrophoneAvailable: function () {
        return FlashRecorderLocal.recorder.isMicrophoneAvailable();
    },

    showPermission: function () {
        FlashRecorderLocal.recorder.permit();
    },

    showPrivacy: function () {
        FlashRecorderLocal.recorder.showPrivacy();
    }
};
