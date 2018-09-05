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

var recLength = 0,
    recBuffersL = [],
    recBuffersR = [],
    sampleRate,
    allZero;

var myurl;
var myexid;
var myreqid;
var myisreference;
var myaudiotype;

var lastSendMoment;

var frameRecLength = 0;
var frameRecBuffersL = [];
var id = new Date().getTime();
var session = new Date().getTime();

var fixedSampleRate = 16000;
var doDownsample = true;

this.onmessage = function (e) {
    switch (e.data.command) {
        case 'init':
            init(e.data.config);
            break;
        case 'record':
            record(e.data.buffer, e.data.type);
            break;
        case 'exportWAV':
            exportWAV(e.data.type);
            break;
        case 'exportMonoWAV':
            exportMonoWAV(e.data.type);
            break;
        case 'getAllZero':
            getAllZero();
            break;
        case 'getBuffers':
            getBuffers();
            break;
        case 'clear':
            clear();
            break;
        case 'startStream':
            startStream(e.data.url, e.data.exid, e.data.reqid, e.data.isreference, e.data.audiotype);
            break;
        case 'stopStream':
            stopStream(e.data.type, e.data.abort);
            break;
        // todo : consider default error log
    }
};

function init(config) {
    sampleRate = config.sampleRate;
}

function startStream(url, exid, reqid, isreference, audiotype) {
    console.log("worker.startStream " + exid + " req " + reqid);
    myurl = new String(url);
    myexid = new String(exid);
    myreqid = new String(reqid);
    myisreference = new String(isreference);
    myaudiotype = new String(audiotype);
    lastSendMoment = new Date().getTime();
}

function stopStream(type, abort) {
    console.log("stopStream record got " + frameRecLength);

    var bufferL = mergeBuffers(frameRecBuffersL, frameRecLength);
    var audioBlob = getAudioBlob(bufferL, type);

    // OK - got the blob, clear backing buffer
    frameRecBuffersL = [];
    frameRecLength = 0;

    var framesBefore = recLength / (sampleRate / 2);
    var framesBeforeRound = Math.round(framesBefore);

    sendBlob(framesBeforeRound, audioBlob, true, abort, lastSendMoment);
}

// so we tag each packet with the time it's generated - so we know when on the client is the
// start of the packet
function record(inputBuffer, type) {
    // every half second, send a blob

    var beforeSendMoment = lastSendMoment;
    var sendMoment = new Date().getTime();

    var framesBefore = recLength / (sampleRate / 2);
    var framesBeforeRound = Math.round(framesBefore);

    recBuffersL.push(inputBuffer[0]);
    recBuffersR.push(inputBuffer[1]);
    recLength += inputBuffer[0].length;

    frameRecBuffersL.push(inputBuffer[0]);
    frameRecLength += inputBuffer[0].length;

    var framesAfter = recLength / (sampleRate / 2);
    var framesAfterRound = Math.round(framesAfter);

    if (framesAfterRound > framesBeforeRound) {
        lastSendMoment = sendMoment;  // OK remember send moment
        console.log(
            "worker.record send blob - got " + frameRecLength +
            " rate " + sampleRate +
            " frame " + framesAfter +
            " rounded " + framesAfterRound);

        var bufferL = mergeBuffers(frameRecBuffersL, frameRecLength);
        var audioBlob = getAudioBlob(bufferL, type);

        // OK - got the blob, clear backing buffer
        frameRecBuffersL = [];
        frameRecLength = 0;

        sendBlob(framesBeforeRound, audioBlob, false, false, beforeSendMoment);
    }
    else {
        // console.log("worker.record 2 got " + recLength + " frame " + framesAfterRound +
        //     " type " + type + " url " + myurl + " exid " + myexid);
    }
}

function sendBlob(framesBeforeRound, audioBlob, isLast, abort, sendMoment) {
//    console.log("worker.sendBlob '" + myurl + "' exid '" + myexid + "'");

    try {
        var xhr = new XMLHttpRequest();

        xhr.addEventListener("progress", updateProgress);
        xhr.addEventListener("load", transferComplete);
        xhr.addEventListener("error", transferFailed);
        xhr.addEventListener("abort", transferCanceled);

        xhr.open("POST", myurl, true);

//Send the proper header information along with the request
        xhr.setRequestHeader("Content-Type", "application/wav");
        xhr.setRequestHeader("EXERCISE", myexid);
        xhr.setRequestHeader("reqid", myreqid);
        xhr.setRequestHeader("ISREFERENCE", myisreference);
        xhr.setRequestHeader("AUDIOTYPE", myaudiotype);
        xhr.setRequestHeader("STREAMTIMESTAMP", sendMoment);

        if (framesBeforeRound === 0) {
            xhr.setRequestHeader("STREAMSTATE", "START");
        }
        else if (abort) {
            xhr.setRequestHeader("STREAMSTATE", "ABORT");
        }
        else if (isLast) {
            xhr.setRequestHeader("STREAMSTATE", "END");
        }
        else {
            xhr.setRequestHeader("STREAMSTATE", "STREAM");
        }
        xhr.setRequestHeader("STREAMSESSION", session);
        xhr.setRequestHeader("STREAMSPACKET", framesBeforeRound);

        xhr.onreadystatechange = function () {//Call a function when the state changes.
            if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
                // Request finished. Do processing here.
                //     console.log("got response " + xhr.responseText.length);
                // var Data = JSON.parse(xhr.responseText);
                //  console.log(Data);

                postSomething(xhr.responseText);

//                console.log(Data.MESSAGE);
                //  console.log('stopRecordingAndPost completed for ' + framesAfterRound);
            }
            else if (this.status != 200) {
                console.log("sendBlob : warning : got response code : " + this.status);
                var resp = {status: "error", code: this.status, statusText: this.statusText};
                postSomething(JSON.stringify(resp));
            }
        };
        xhr.send(audioBlob);

        // todo : read result!
    } catch (e) {
        console.log('got exception - Bad call to blob');

        var vDebug = "";
        for (var prop in e) {
            vDebug += "property: " + prop + " value: [" + e[prop] + "]\n";
        }
        vDebug += "toString(): " + " value: [" + e.toString() + "]";
        console.log(vDebug);
        throw e;
    }
}

// send message to parent...
function postSomething(something) {
    this.postMessage(something);
}

function updateProgress(oEvent) {
    if (oEvent.lengthComputable) {
        var percentComplete = oEvent.loaded / oEvent.total * 100;
        //   console.log("updateProgress " + oEvent.loaded + "/" + oEvent.total + " " + percentComplete);
    } else {
        //   console.log("updateProgress size unknown");
        // Unable to compute progress information since the total size is unknown
    }
}

function transferComplete(evt) {
    //  console.log("The transfer is complete.");
}

function transferFailed(evt) {
    console.log("An error occurred while transferring the file.");
}

function transferCanceled(evt) {
    console.log("The transfer has been canceled by the user.");
}

function exportWAV(type) {
    var bufferL = mergeBuffers(recBuffersL, recLength);
    var bufferR = mergeBuffers(recBuffersR, recLength);
    var interleaved = interleave(bufferL, bufferR);
    var dataview = encodeWAV(interleaved, false, sampleRate);
    var audioBlob = new Blob([dataview], {type: type});

    this.postMessage(audioBlob);
}

function getAudioBlob(bufferL, type) {
    var sampleRateToUse = sampleRate;
    var toUse = bufferL;

    if (doDownsample) {
        sampleRateToUse = fixedSampleRate;
        toUse = downsampleBuffer(bufferL, sampleRate, sampleRateToUse);
    }

    var dataview = encodeWAV(toUse, true, sampleRateToUse);
    return new Blob([dataview], {type: type});
}

function exportBuffer(bufferL, type) {
    var audioBlob = getAudioBlob(bufferL, type);
    this.postMessage(audioBlob);
}

/**
 * Allow downsampling the buffer to 16K.
 * true by default
 * @param type
 */
function exportMonoWAV(type) {
    var bufferL = mergeBuffers(recBuffersL, recLength);
    exportBuffer(bufferL, type);
}

function getBuffers() {
    var buffers = [];
    buffers.push(mergeBuffers(recBuffersL, recLength));
    buffers.push(mergeBuffers(recBuffersR, recLength));
    this.postMessage(buffers);
}

function clear() {
    recLength = 0;
    recBuffersL = [];
    recBuffersR = [];

    frameRecLength = 0;
    frameRecBuffersL = [];
//    console.log("clear - session before " + session);
    session = new Date().getTime();
    console.log("clear - session after  " + session);
}

function getAllZero() {
    this.postMessage(allZero);
}

function mergeBuffers(recBuffers, recLength) {
    var result = new Float32Array(recLength);
    var offset = 0;
    for (var i = 0; i < recBuffers.length; i++) {
        result.set(recBuffers[i], offset);
        offset += recBuffers[i].length;
    }
    return result;
}

function interleave(inputL, inputR) {
    var length = inputL.length + inputR.length;
    var result = new Float32Array(length);

    var index = 0,
        inputIndex = 0;

    while (index < length) {
        result[index++] = inputL[inputIndex];
        result[index++] = inputR[inputIndex];
        inputIndex++;
    }
    return result;
}

function floatTo16BitPCM(output, offset, input) {
    //allZero = true;
    for (var i = 0; i < input.length; i++, offset += 2) {
        var s = Math.max(-1, Math.min(1, input[i]));

        var intValue = s < 0 ? s * 0x8000 : s * 0x7FFF;
        //  if (intValue != 0) allZero = false;
        output.setInt16(offset, intValue, true);
    }
}

function writeString(view, offset, string) {
    for (var i = 0; i < string.length; i++) {
        view.setUint8(offset + i, string.charCodeAt(i));
    }
}

/**
 * From https://aws.amazon.com/blogs/machine-learning/capturing-voice-input-in-a-browser
 * @param buffer
 * @param bufferSampleRate of the input buffer
 * @param sampleRate
 * @returns Float32Array
 */
function downsampleBuffer(buffer, bufferSampleRate, sampleRate) {
    if (bufferSampleRate === sampleRate) {
        return buffer;
    }
    var sampleRateRatio = bufferSampleRate / sampleRate;
    var newLength = Math.round(buffer.length / sampleRateRatio);

    // console.log("downsampleBuffer : old rate   " + bufferSampleRate + " new " + sampleRate);
    // console.log("downsampleBuffer : old length " + buffer.length + " new " + newLength);

    var result = new Float32Array(newLength);
    var offsetResult = 0;
    var offsetBuffer = 0;
    while (offsetResult < result.length) {
        var nextOffsetBuffer = Math.round((offsetResult + 1) * sampleRateRatio);
        var accum = 0,
            count = 0;
        for (var i = offsetBuffer; i < nextOffsetBuffer && i < buffer.length; i++) {
            accum += buffer[i];
            count++;
        }
        result[offsetResult] = accum / count;
        offsetResult++;
        offsetBuffer = nextOffsetBuffer;
    }
    return result;
}

function encodeWAV(samples, mono, sampleRateToUse) {
    var buffer = new ArrayBuffer(44 + samples.length * 2);
    var view = new DataView(buffer);

    //  console.log("encodeWAV : sample rate "+sampleRate);

    /* RIFF identifier */
    writeString(view, 0, 'RIFF');
    /* file length */
    view.setUint32(4, 32 + samples.length * 2, true);
    /* RIFF type */
    writeString(view, 8, 'WAVE');
    /* format chunk identifier */
    writeString(view, 12, 'fmt ');
    /* format chunk length */
    view.setUint32(16, 16, true);
    /* sample format (raw) */
    view.setUint16(20, 1, true);
    /* channel count */
    view.setUint16(22, mono ? 1 : 2, true);
    /* sample rate */
    view.setUint32(24, sampleRateToUse, true);
    /* byte rate (sample rate * block align) */
    view.setUint32(28, sampleRateToUse * 4, true);
    /* block align (channel count * bytes per sample) */
    view.setUint16(32, 4, true);
    /* bits per sample */
    view.setUint16(34, 16, true);
    /* data chunk identifier */
    writeString(view, 36, 'data');
    /* data chunk length */
    view.setUint32(40, samples.length * 2, true);

    floatTo16BitPCM(view, 44, samples);

    return view;
}

/*
function stopRecordingAndPost(url, exid) {
    exportMonoWAV(function (blob) {
        try {
            var xhr = new XMLHttpRequest();
            xhr.open("POST", url, true);

//Send the proper header information along with the request
            xhr.setRequestHeader("Content-Type", "application/wav");
            xhr.setRequestHeader("EXERCISE", exid);

            xhr.onreadystatechange = function () {//Call a function when the state changes.
                if (this.readyState == XMLHttpRequest.DONE && this.status == 200) {
                    // Request finished. Do processing here.

                    console.log('stopRecordingAndPost completed');
                }
            };
            xhr.send(blob);
        } catch (e) {
            console.log('Bad call to blob');

            var vDebug = "";
            for (var prop in e) {
                vDebug += "property: " + prop + " value: [" + e[prop] + "]\n";
            }
            vDebug += "toString(): " + " value: [" + e.toString() + "]";
            console.log(vDebug);
            throw e;
        }
    });
}*/
