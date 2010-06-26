package com.castillo.dd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class ARProgressBar extends View {
	
	public final static int HORIZONTAL  =0;
	public final static int VERTICAL = 0;

	private Paint paint;
	//private String processingText;
	private String name;
	private int backgroundColor ;	
	private int foregroundColor;
	private int textColor;
	private int position;
	private int min;
	private int max;
	private int orientation;
	private int xText;
	private int yText;
	
	private int mPaddingLeft=0;
	private int mPaddingRight=0;
	private int mPaddingBottom=3;
	private int mPaddingTop=3;
	
	DSInterface dsInterface;
	int downloadIndex;
	
	/**
	 * @param context
	 */
	public ARProgressBar(Context context, DSInterface dsInterface, int downloadIndex) {
		super(context);
		initProgressBar(dsInterface,downloadIndex);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		this.setMeasuredDimension(MeasureWidth(widthMeasureSpec), 
								MeasureHeight(heightMeasureSpec));
		
	}

	private int MeasureHeight(int heightMeasureSpec) {
		int result=0;
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (this.orientation==HORIZONTAL) {
	        int heightText = (int) (paint.descent()- paint.ascent()) + this.mPaddingTop+this.mPaddingBottom +5;
	        result = Math.min(specSize, heightText);
	        yText = (int) ((result - paint.ascent())/2);
        } else {
        	yText = 0;
        	result = Math.min(specSize, max - min);
        }
        
        return result;
	}
	private int MeasureWidth(int widthMeasureSpec) {
		int result = 0;
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        
        String filename="";
        try
        {
        	filename=dsInterface.getDownloadFilename(downloadIndex-1);
        }
        catch (Exception e)
        {
        	 Log.e(getContext().getString(R.string.app_name), e.getMessage());
        }
        
        if (this.orientation==HORIZONTAL) {
        	if (specMode == MeasureSpec.EXACTLY)
        	{
        		result = specSize;
        	} else {
		    	int widthText = (int) paint.measureText(filename) + this.mPaddingLeft+this.mPaddingRight + 20;
		    	result =Math.min(widthText, specSize);
        	}
	    	xText = (int) ((result - paint.measureText(filename))/2 + paint.measureText(name));
        } else {
        	xText = 0;
        	result = Math.min(specSize, 20);
        }
    	return result;
 	}
	private void initProgressBar(DSInterface dsInterface, int downloadIndex) {
		// TODO Auto-generated method stub
		paint = new Paint();
		paint.setTextSize(13);
		paint.setColor(0xFF668800);
		paint.setStyle(Paint.Style.FILL);
		this.orientation = ARProgressBar.HORIZONTAL;
		this.min=0;
		this.max=100;
		this.backgroundColor =0xFF668800;
		this.foregroundColor = Color.GRAY;
		this.textColor = Color.WHITE;
		this.position=0;
		this.downloadIndex=downloadIndex;
		this.name=getResources().getString(R.string.file)+" "+downloadIndex+" : ";
		try
		{
			this.dsInterface=dsInterface;
			//this.processingText=d.getFileName();			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}		
	}
	protected void onDrawBackground(Canvas canvas) {
		//canvas.drawColor(backgroundColor);
	}
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		this.onDrawBackground(canvas);
		
		if (this.orientation==HORIZONTAL) {
			paint.setColor(this.textColor);
	        canvas.drawText(name, 0, yText, paint);
			paint.setColor(this.foregroundColor);        
	        int truepos = (int) (((float)position/(float)(max-min)) * (this.getWidth()-paint.measureText(name)));
	        canvas.drawRect(paint.measureText(name), mPaddingTop, paint.measureText(name)+truepos, this.getHeight()-mPaddingBottom, paint);		
	        paint.setColor(this.textColor);
	        
	        String filename="";
	        int status=0;
	        try
	        {
	        	filename=dsInterface.getDownloadFilename(downloadIndex-1);
	        	int widthText = (int) paint.measureText(filename) + this.mPaddingLeft+this.mPaddingRight + 20;
	        	xText = (int) ((widthText - paint.measureText(filename))/2 + paint.measureText(name));		       		       
	        	status=dsInterface.getDownloadStatus(downloadIndex-1);
	        }
	        catch (Exception e)
	        {
	        	 Log.e(getContext().getString(R.string.app_name), e.getMessage());
	        	 status=Download.ERROR;
	        }
	        
	        if (status>=Download.ERROR)
	        	canvas.drawText(getResources().getString(R.string.error_link), xText, yText, paint);
	        else if (filename.startsWith("?"))
	        	canvas.drawText(getResources().getString(R.string.loading_file), xText, yText, paint);
	        else
	        	canvas.drawText(filename, xText, yText, paint);
		} else {//VERTICAL
	        paint.setColor(this.foregroundColor);        
	        int truepos = (int) (((float)position/(float)(max-min)) * this.getHeight());
	        canvas.drawRect(0, 0, this.getWidth(), truepos, paint);		
		}
        
	}
	/*public final String getProcessingText() {
		return processingText;
	}
	public final void setProcessingText(String processingText) {
		this.processingText = processingText;
	}*/
	public final int getBackgroundColor() {
		return backgroundColor;
	}
	@Override
	public final void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
		this.foregroundColor = Color.argb(0xFF, 0xFF-Color.red(backgroundColor), 0xFF-Color.green(backgroundColor), 0xFF-Color.blue(backgroundColor));
	}
	public final int getTextColor() {
		return textColor;
	}
	public final void setTextColor(int textColor) {
		this.textColor = textColor;
	}
	public final int getPosition() {
		return position;
	}
	public final void setPosition(int position) {
		this.position = position;
		invalidate();		
	}
	public final int getMin() {
		return min;
	}
	public final void setMin(int min) {
		this.min = min;
	}
	public final int getMax() {
		return max;
	}
	public final void setMax(int max) {
		this.max = max;
	}
	public final void setOrientation(int orientation) {
		this.orientation = orientation;
	}
}
