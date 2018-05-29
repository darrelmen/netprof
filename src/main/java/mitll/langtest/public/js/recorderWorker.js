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
    sampleRate, allZero;

var fixedSampleRate = 16000;
var doDownsample = true;

this.onmessage = function (e) {
    switch (e.data.command) {
        case 'init':
            init(e.data.config);
            break;
        case 'record':
            record(e.data.buffer);
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
    }
};

function init(config) {
    sampleRate = config.sampleRate;
}

function record(inputBuffer) {
    recBuffersL.push(inputBuffer[0]);
    recBuffersR.push(inputBuffer[1]);
    recLength += inputBuffer[0].length;
}

function exportWAV(type) {
    var bufferL = mergeBuffers(recBuffersL, recLength);
    var bufferR = mergeBuffers(recBuffersR, recLength);
    var interleaved = interleave(bufferL, bufferR);
    var dataview = encodeWAV(interleaved, false, sampleRate);
    var audioBlob = new Blob([dataview], {type: type});

    this.postMessage(audioBlob);
}

/**
 * Allow downsampling the buffer to 16K.
 * true by default
 * @param type
 */
function exportMonoWAV(type) {
    var bufferL = mergeBuffers(recBuffersL, recLength);

    var sampleRateToUse = sampleRate;
    var toUse = bufferL;

    if (doDownsample) {
        sampleRateToUse = fixedSampleRate;
        toUse = downsampleBuffer(bufferL, sampleRate, sampleRateToUse);
    }

    var dataview = encodeWAV(toUse, true, sampleRateToUse);
    var audioBlob = new Blob([dataview], {type: type});

    this.postMessage(audioBlob);
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
 * @returns {*}
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
