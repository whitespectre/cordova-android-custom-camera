var exec = require('cordova/exec');
var CustomCamera = function() {};

CustomCamera.prototype.getPictures = function(success, fail, options) {
  return corodva.exec(success, fail, "AndroidCamera", "recordVideo", []);
};

window.AndroidCamera = new CustomCamera();