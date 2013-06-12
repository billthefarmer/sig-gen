////////////////////////////////////////////////////////////////////////////////
//
//  Signal generator - An Android Signal generator written in Java.
//
//  Copyright (C) 2013	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.siggen;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class Knob extends View
    implements GestureDetector.OnGestureListener, ValueAnimator.AnimatorUpdateListener
{
    protected static final int MARGIN = 8;

    protected static final float MIN = 0;
    protected static final float MAX = 680;
    private static final int SCALE = 50;
    private static final int VELOCITY = 75;

    protected int parentWidth;
    protected int parentHeight;

    protected int width;
    protected int height;

    protected boolean move;
    protected float value;
    protected float last;

    private Matrix matrix;
    private Paint paint;
    private LinearGradient gradient;
    private LinearGradient dimple;
    private GestureDetector detector;
    private ValueAnimator animator;

    private Bitmap next;
    private Bitmap previous;
    
    private OnKnobChangeListener listener;

    public Knob(Context context, AttributeSet attrs)
    {
	super(context, attrs);

	Resources resources = getResources();

	next = BitmapFactory.decodeResource(resources,
					    R.drawable.ic_navigation_next_item);

	previous = BitmapFactory.decodeResource(resources,
						R.drawable.ic_navigation_previous_item);

	matrix = new Matrix();

	detector = new GestureDetector(context, this);
    }

    // On measure

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
	super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	// Get the parent dimensions

	View parent = (View) getParent();
	int w = parent.getWidth();
	int h = parent.getHeight();

	if (parentWidth < w)
	    parentWidth = w;

	if (parentHeight < h)
	    parentHeight = h;

	w = (parentWidth - MARGIN) / 2;
	h = (parentHeight - MARGIN) / 2;

	this.setMeasuredDimension(w, h);
    }

    // On size changed

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
	width = w;
	height = h;
		
	paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	gradient = new LinearGradient(0, -h * 2 / 3, 0, h * 2 / 3,
				      Color.WHITE, Color.GRAY, TileMode.CLAMP);
	dimple = new LinearGradient(MARGIN / 2, -MARGIN / 2, MARGIN / 2, MARGIN / 2,
				    Color.GRAY, Color.WHITE, TileMode.CLAMP);
    }

    // On draw

    @Override
    protected void onDraw(Canvas canvas)
    {
	canvas.translate(width / 2, height / 2);

	paint.setShader(gradient);
	paint.setStyle(Style.FILL);

	int radius = Math.min(width, height) / 2;
	canvas.drawCircle(0, 0, radius, paint);

	paint.setShader(null);
	paint.setColor(Color.LTGRAY);
	canvas.drawCircle(0, 0,	 radius - MARGIN, paint);

	float x = (float) (Math.sin(value * Math.PI / SCALE) * radius * 0.8);
	float y = (float) (-Math.cos(value * Math.PI / SCALE) * radius * 0.8);

	paint.setShader(dimple);
	matrix.setTranslate(x, y);
	dimple.setLocalMatrix(matrix);
	canvas.drawCircle(x, y, MARGIN, paint);

	paint.setShader(null);
	canvas.drawBitmap(previous, -width / 2 + MARGIN, - height / 2 + MARGIN, paint);
	canvas.drawBitmap(next, width / 2 - next.getWidth() - MARGIN,
			  -height / 2 + MARGIN, paint);	
    }

    // On touch event

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
	if (detector != null)
	    detector.onTouchEvent(event);

	float x = event.getX() - width / 2;
	float y = event.getY() - height / 2;

	float theta = (float)Math.atan2(x, -y);

	switch (event.getAction())
	{
	case MotionEvent.ACTION_DOWN:

	    float radius = (float) Math.hypot(x, y);

	    if (radius > Math.min(width, height) / 2.0 && y < 0.0)
	    {
		if (x < 0.0)
		    value -= 1.0;
		else
		    value += 1.0;

		value = Math.round(value);

		if (listener != null)
		    listener.onKnobChange(this, value);

		invalidate();
	    }
	    break;

	case MotionEvent.ACTION_MOVE:

	    if (!move)
		move = true;

	    else
	    {
		// Difference

		float delta = theta - last;

		// Allow for crossing origin

		if (delta > Math.PI)
		    delta -= 2.0 * Math.PI;

		if (delta < -Math.PI)
		    delta += 2.0 * Math.PI;

		// Update value

		value += delta * SCALE / Math.PI;

		if (value < MIN)
		    value = MIN;

		if (value > MAX)
		    value = MAX;

		if (listener != null)
		    listener.onKnobChange(this, value);

		invalidate();
	    }
	    last = theta;
	    break;

	case MotionEvent.ACTION_UP:
	    move = false;
	    break;
	}
	return true;
    }

    // On fling

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			   float velocityY)
    {
	// Get event coordinates

	float x1 = e1.getX() - width / 2;
	float y1 = e1.getY() - height / 2;

	float x2 = e2.getX() - width / 2;
	float y2 = e2.getY() - height / 2;

	// Calculate angles

	float theta1 = (float)Math.atan2(x1, -y1);
	float theta2 = (float)Math.atan2(x2, -y2);

	// Calculate difference and absolute velocity

	float delta = theta2 - theta1;
	float velocity = (float) Math.abs(Math.hypot(velocityX, velocityY));

	// Allow for crossing origin

	if (delta > Math.PI)
	    delta -= 2.0 * Math.PI;

	if (delta < -Math.PI)
	    delta += 2.0 * Math.PI;

	// Calculate target value for animator

	float target = value + (float)(Math.signum(delta) *
				       velocity / VELOCITY);

	// Start the animation

	animator = ValueAnimator.ofFloat(value, target);
	animator.setInterpolator(new DecelerateInterpolator());
	animator.addUpdateListener(this);
	animator.start();

	return true;
    }

    // On animation update

    @Override
    public void onAnimationUpdate(ValueAnimator animation)
    {
	value = (Float) animation.getAnimatedValue();		

	if (value < MIN)
	{
	    animation.cancel();
	    value = MIN;
	}

	if (value > MAX)
	{
	    animation.cancel();
	    value = MAX;
	}

	if (listener != null)
	    listener.onKnobChange(this, value);

	invalidate();
    }

    // Set listener

    protected void setOnKnobChangeListener(OnKnobChangeListener l)
    {
	listener = l;
    }

    // On knob change listener

    public interface OnKnobChangeListener
    {
	public abstract void onKnobChange(Knob knob, float value);
    }

    // A collection of unused unwanted unloved listener callback methods

    @Override
    public boolean onDown(MotionEvent e)
    {
	return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			    float distanceY)
    {
	return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
	return false;
    }
}
