var exec = require('cordova/exec');
var argscheck = require('cordova/argscheck');
var CustomCamera = function() {};

CustomCamera.prototype.recordVideo = function(success, fail, options) {
  var getValue = argscheck.getValue;

  var libraryFolder = getValue(options.libraryFolder, "My Challenge Tracker");
  var cancelText = getValue(options.cancelText, "Cancel");
  var tooltip = getValue(options.tooltip, "Max time limit 3 minutes");

  return cordova.exec(
    success, 
    fail, 
    "AndroidCamera", 
    "recordVideo", [
      libraryFolder,
      cancelText,
      tooltip
    ]);
};

window.AndroidCamera = new CustomCamera();