package com.castillo.dd;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DownloadService extends Service {

	private List<Download> downloads = new ArrayList<Download>();
	private int currentPosition;
	
	public Preferences loadPreferences()
    {
    	int mode = Activity.MODE_WORLD_READABLE;
    	SharedPreferences mySharedPreferences = getSharedPreferences(DownDroid.PREFERENCES,mode);
    	Preferences p=new Preferences();
    	p.setMegaupload_user(mySharedPreferences.getString("megaupload_user", null));
    	p.setMegaupload_password(mySharedPreferences.getString("megaupload_password", null));    	
    	return p;
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	private final DSInterface.Stub mBinder = new DSInterface.Stub() {

	    public void downloadFile(int position) throws DeadObjectException {
	        try {
	            currentPosition = position;
	            Download download=downloads.get(currentPosition);
	            download.resume();

	        } catch (IndexOutOfBoundsException e) {
	            Log.e(getString(R.string.app_name), e.getMessage());
	        }
	    }

	    public void addFileDownloadlist(String url, int position) throws DeadObjectException {
	        try
	        {
	        	downloads.add(new Download(loadPreferences(),new URL(url),position));
	        }
	        catch (Exception e)
	        {
	        	Log.e(getString(R.string.app_name), e.getMessage());
	        }
	    }

	    public void clearDownloadlist() throws DeadObjectException {
	    	Download download=downloads.get(currentPosition);
            download.cancel();
	    	downloads.clear();
	    }	    

	    public void pause() throws DeadObjectException {
	    	Download download=downloads.get(currentPosition);
            download.pause();
	    }

	    public void resume() throws DeadObjectException {
	    	Download download=downloads.get(currentPosition);
            download.resume();
	    }

		public int getDownloadStatus(int position) throws RemoteException {
			Download download=downloads.get(position);			
			return download.getStatus();
		}

		public int getDownloadProgress(int position) throws RemoteException {
			Download download=downloads.get(position);			
			return (int)download.getProgress();
		}

		public int getDownloadlistSize() throws RemoteException {			
			return downloads.size();
		}

		public String getDownloadFilename(int position) throws RemoteException {
			Download download=downloads.get(position);			
			return download.getFileName();
		}	
		
		public String getDownloadEllapsedTime(int position) throws RemoteException {
			Download download=downloads.get(position);			
			return (String)download.getEllapsedTime();
		}
		
		public String getDownloadRemainingTime(int position) throws RemoteException {
			Download download=downloads.get(position);			
			return (String)download.getRemainingTime();
		}
		
		public float getDownloadSpeed(int position) throws RemoteException {
			Download download=downloads.get(position);			
			return (float)download.getSpeed();
		}
		
		public long getDownloadLaunchTime(int position) throws RemoteException {
			Download download=downloads.get(position);			
			return (long)download.getLaunchTime();
		}

	};
	
}
