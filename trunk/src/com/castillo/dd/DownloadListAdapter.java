package com.castillo.dd;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadListAdapter extends ArrayAdapter<PendingDownload> {
    public static final String TAG = DownloadListAdapter.class.getSimpleName();
    private ArrayList<PendingDownload> downloads;
    private LayoutInflater mInflater;
    private int layoutResource;
    public DownloadListAdapter(Context context, int textViewResourceId,
            ArrayList<PendingDownload> mOptions) {
        super(context, textViewResourceId, mOptions);
        this.downloads = mOptions;
        this.mInflater = LayoutInflater.from(context);
        this.layoutResource = textViewResourceId;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        View v = convertView;
        if (v == null) {
            v = mInflater.inflate(layoutResource, null);
            holder = new ViewHolder();
            //holder.icon = (ImageView) v.findViewById(R.id.left_icon);
            holder.firstLine = (TextView) v.findViewById(R.id.firstLineTextView);
            holder.waitingTime = (TextView) v.findViewById(R.id.waitingTime);
            holder.progressBar = (ProgressBar) v.findViewById(R.id.progress_horizontal);
            holder.timeTextView = (TextView) v.findViewById(R.id.secondLineTextView);
            v.setTag(holder);
        }else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) v.getTag();
        }
        PendingDownload c = downloads.get(position);
        if (c != null) {
            //loading first line
        	holder.waitingTime.setVisibility(View.INVISIBLE);
        	if (c.getFilename()!=null && !c.getFilename().startsWith("?d"))
            	holder.firstLine.setText(c.getFilename());
            else
            {
            	holder.firstLine.setText(getContext().getResources().getText(R.string.loading_file));
            	if (c.getLaunchTime()!=0)
            	{
            		SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss");
                	dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                	Calendar cal = Calendar.getInstance();
                	long elapsed = cal.getTimeInMillis();
                	elapsed=elapsed-c.getLaunchTime();
                	cal.setTimeInMillis(elapsed);                	
            		holder.waitingTime.setText(dateFormat.format(cal.getTime()));
            		holder.waitingTime.setVisibility(View.VISIBLE);
            	}
            }
            holder.firstLine.setVisibility(View.VISIBLE);
            //loading progress bar
            holder.progressBar.setProgress(c.getProgress());
            holder.progressBar.setVisibility(View.VISIBLE);            
            //loading second line
            if (c.getFilename()!=null && !c.getFilename().startsWith("?d"))            
            	holder.timeTextView.setText("T : "+c.getEllapsedTime()+"    R : "+c.getRemainingTime()+"    S : "+new DecimalFormat("0.00").format(c.getSpeed())+" Kb/s");            	            
            else
            	holder.timeTextView.setText(c.getUrl());
            holder.timeTextView.setVisibility(View.VISIBLE);
            //loading icon
            //TODO make mechanism so the icon will appear differently
            //holder.icon.setImageResource(R.drawable.icon);
        }
        else {
            // This is going to be a shout then.
            //holder.icon.setImageResource(R.drawable.icon);
            holder.firstLine.setVisibility(View.GONE);
        }
        //TODO create stringformatter
        return v;
    }
    private static class ViewHolder {
        //ImageView icon;
        TextView firstLine;
        TextView waitingTime;
        ProgressBar progressBar;
        TextView timeTextView;
    }
}
