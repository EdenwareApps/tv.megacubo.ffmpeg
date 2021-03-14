function addQuotes(value){
	if(typeof(value) == 'string' && value.indexOf(' ') != -1){
		console.log('addQuotes '+ typeof(value)+'-'+ value)
		value = '"'+ value +'"'
	}
	return value
}
module.exports = {
  exec: function (cmd, successCallback, errorCallback) {
	console.log('ffmpeg.exec pre', cmd)
	if(Array.isArray(cmd)){
		cmd = cmd.map(c => {
			c = String(c)
			if(c.length && c.charAt(0) != '-' && c.indexOf(' ') != -1){
				c = addQuotes(c)
			}
			return c
		}).join(' ')
	}
	console.log('ffmpeg.exec pos', cmd)
    cordova.exec(successCallback, errorCallback, "FFMpeg", "exec", [cmd]);
  },
  kill: function (executionId) {
    cordova.exec(() => {}, console.error, "FFMpeg", "kill", [executionId]);
  },
  cleanup: function (executionIdsList) {
	console.log('calling cleanup')
    cordova.exec(() => {}, console.error, "FFMpeg", "cleanup", [executionIdsList]);
  },
  exit: function () {
    cordova.exec(() => {}, console.error, "FFMpeg", "exit", []);
  }
};
