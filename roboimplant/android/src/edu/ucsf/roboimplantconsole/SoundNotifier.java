package edu.ucsf.roboimplantconsole;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundNotifier 
{
    private static SoundNotifier _instance=null;
	public static final int BEEP = 0;
	private MediaPlayer mMediaPlayer;

	public static SoundNotifier getInstance()
	{
		if (_instance==null)
			_instance = new SoundNotifier();
		return _instance;
	}
	

    public void playAudio (Context ctx,int soundID) {
        try {
            mMediaPlayer = MediaPlayer.create(ctx, soundID);
            mMediaPlayer.setLooping(false);
            Log.e("beep","started0");
            mMediaPlayer.start();
        } 
        catch (Exception e) {
            Log.e("beep", "error: " + e.getMessage(), e);
        }
        mMediaPlayer=null;
    }
}