# Megacubo FFMPEG Plugin

Based on the excellent cordova-plugin-ffmpeg, customized for the Megacubo project.

# Install

```
cordova plugin add https://github.com/efoxbr/tv.megacubo.ffmpeg.git
```


# Sample usage

```
let executionId
let cmd = ['-i', '/full/path/to/my/video']
// let cmd = ['-i', '"/full/path/to/my/video with spaces"']
ffmpeg.exec(cmd, data => {
	console.log('ffmpeg.exec returned', cmd, data)
	let pos = data.indexOf('-')
	if(pos != -1){
		let responseType = data.substr(0, pos), info = data.substr(pos + 1)
		if(responseType == 'start'){
			console.log('execution ID:', info)
			executionId = parseInt(info)
			// ffmpeg.kill(executionId)
		} else if(responseType == 'metadata') {
			console.log('partial response with metadata:', info)
		} else {
			console.log('command finished, output:', info)
		}
	} else {
		console.error('ffmpeg.exec returned a badly formatted response (?!)', cmd, data)
	}
}, err => {
	console.error('ffmpeg.exec error', cmd, err)
})
```
