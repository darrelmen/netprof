function __log(e, data) {
    //log.innerHTML += "\n" + e + " " + (data || '');
    $('#status').append("<p>"+e + "  at " + new Date().getTime());
}

var audio_context;
var recorder;

function startUserMedia(stream) {
    var input = audio_context.createMediaStreamSource(stream);
    __log('Media stream created.');

    input.connect(audio_context.destination);
    __log('Input connected to audio context destination.');

    recorder = new Recorder(input);
    __log('Recorder initialised.');

    webAudioMicAvailable();
}

function startRecording() {
    recorder && recorder.record();
    __log('Recording...');
}

function stopRecording() {
    recorder && recorder.stop();
    __log('Stopped recording.');

    // create WAV download link using audio data blob
    var grabWav2 = grabWav();

    recorder.clear();

    return grabWav2;
}

function uint6ToB64 (nUint6) {

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

};

function bytesToBase64(aBytes) {

    var sB64Enc = "";

    for (var nMod3, nLen = aBytes.length, nUint24 = 0, nIdx = 0; nIdx < nLen; nIdx++) {
        nMod3 = nIdx % 3;
        if (nIdx > 0 && (nIdx * 4 / 3) % 76 === 0) { sB64Enc += "\r\n"; }
        nUint24 |= aBytes[nIdx] << (16 >>> nMod3 & 24);
        if (nMod3 === 2 || aBytes.length - nIdx === 1) {
            sB64Enc += String.fromCharCode(uint6ToB64(nUint24 >>> 18 & 63), uint6ToB64(nUint24 >>> 12 & 63), uint6ToB64(nUint24 >>> 6 & 63), uint6ToB64(nUint24 & 63));
            nUint24 = 0;
        }
    }

    return sB64Enc.replace(/A(?=A$|$)/g, "=");

};

function grabWav() {
    recorder && recorder.exportWAV(function (blob) {
        __log('Got ... wav data!');
        try {
            __log('2 Got ... wav data!' + blob);
            __log('2 Got ... wav data!' + blob.byteLength);
            __log('3 Got ... wav data!' + blob.buffer);
            //__log('3 Got ... wav data!' + dataview.buffer.byteLength);

            var reader = new FileReader();

            var arrayBuffer;
            reader.onloadend = function () {
                arrayBuffer = reader.result;

                var myArray = new Uint8Array(arrayBuffer);

                __log('5 Got ... array ' + myArray);
                __log('6 Got ... array len!' + myArray.length);

                var bytes = bytesToBase64(myArray);
                //__log('7 Got ... wav data!' + bytes);

                var length = bytes.length;

                __log('8 Got ... wav data!' + length);

                getBase64(bytes);
            }

            reader.readAsArrayBuffer(blob);

            /*     var number = dataview.byteLength / 2;
             __log('4 Got ... wav data!' + number);
             */
            /*      startBuffer(number);

             __log('5 Got ... wav data!' + number);

             for (i = 0; i < blob.byteLength/2; i++) {
             var x = blob.getInt16(i);
             setBuf(i,x);
             }
             endBuffer();
             */

            /*
             var myArray = new Uint8Array(dataview.buffer);

             __log('5 Got ... wav data!' + myArray.length);

             var bytesToBase64 = bytesToBase64(myArray);
             __log('6 Got ... wav data!' + bytesToBase64);

             var length = bytesToBase64.length;

             __log('7 Got ... wav data!' + length);*/

            return "";
        } catch (e) {
            __log('Bad call to blob');

            var vDebug = "";
            for (var prop in e)
            {
                vDebug += "property: "+ prop+ " value: ["+ e[prop]+ "]\n";
            }
            vDebug += "toString(): " + " value: [" + e.toString() + "]";
            __log(vDebug);
            throw e;
        }
    });
}

function initWebAudio() {
    try {
        // webkit shim
        window.AudioContext = window.AudioContext || window.webkitAudioContext;
        navigator.getMedia = ( navigator.getUserMedia ||
            navigator.webkitGetUserMedia ||
            navigator.mozGetUserMedia ||
            navigator.msGetUserMedia);
        window.URL = window.URL || window.webkitURL;

        audio_context = new AudioContext;
        __log('Audio context set up.');
        __log('navigator.getUserMedia ' + (navigator.getMedia ? 'available.' : 'not present!'));
    } catch (e) {
        alert('No web audio support in this browser!');
        webAudioMicNotAvailable();
    }

    navigator.getMedia({audio: true}, startUserMedia, function(e) {
        __log('No live audio input: ' + e);
        webAudioMicNotAvailable();
    });
}
