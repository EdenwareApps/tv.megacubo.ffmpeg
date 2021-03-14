package tv.megacubo.ffmpeg;

import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import com.arthenica.mobileffmpeg.Level;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.Statistics;
import com.arthenica.mobileffmpeg.StatisticsCallback;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpegExecution;
import com.arthenica.mobileffmpeg.MediaInformation;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import org.apache.cordova.PluginResult;

 // ref: https://github.com/tanersener/mobile-ffmpeg/wiki/Android
public class FFMpeg extends CordovaPlugin {

    private String TAG = "FFMpeg";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
		Config.setLogLevel(Level.AV_LOG_WARNING);
	}
	
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("exec")) {
            long executionId = FFmpeg.executeAsync(data.getString(0), new ExecuteCallback() {
                @Override
                public void apply(long executionId, int returnCode) {
					String output = "return-" + Config.getLastCommandOutput();
                    if (returnCode == RETURN_CODE_SUCCESS){
						PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, output);
						pluginResult.setKeepCallback(false);
						callbackContext.sendPluginResult(pluginResult);
                    } else {
						output += "; Xerrorcode: " + returnCode;
						PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, output);
						pluginResult.setKeepCallback(false);
						callbackContext.sendPluginResult(pluginResult);
                    }
                }
            });
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "start-" + executionId);
			pluginResult.setKeepCallback(true);
			callbackContext.sendPluginResult(pluginResult);
            return true;
        } else if(action.equals("kill")) {
            long executionId = data.getLong(0);
            FFmpeg.cancel(executionId);
            return true;
        } else if(action.equals("cleanup")) {
            List<Long> keepIds = new ArrayList<Long>();
            for (String s : data.getString(0).split(",")){
                keepIds.add(Long.parseLong(s));
            }
            long startTimeTolerance = 15;
            long now = System.currentTimeMillis() / 1000;
            final List<FFmpegExecution> ffmpegExecutions = FFmpeg.listExecutions();
            for (int i = 0; i < ffmpegExecutions.size(); i++) {
                FFmpegExecution execution = ffmpegExecutions.get(i);
                Long executionId = execution.getExecutionId();
                Long startTime = execution.getStartTime().getTime() / 1000;
                if(!keepIds.contains(executionId)){
                    long elapsed = now - startTime;
                    if(elapsed > startTimeTolerance){
                        FFmpeg.cancel(executionId);
                    }
                }
            }
            return true;
        } else if(action.equals("exit")) {
            FFmpeg.cancel();
            return true;
        } else return false;
    }
    
}
