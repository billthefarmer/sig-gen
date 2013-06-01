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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SiggenView extends View
{
    protected static final int MARGIN = 8;

    protected int parentWidth;
    protected int parentHeight;

    protected int width;
    protected int height;

    private Rect clipRect;
    private RectF outlineRect;

    protected Paint paint;

    public SiggenView(Context context, AttributeSet attrs)
    {
	super(context, attrs);

	paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

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
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
	// Save the new width and height less the cliprect

	width = w - 6;
	height = h - 6;

	// Create some rects for
	// the outline and clipping

	outlineRect = new RectF(1, 1, w - 1, h - 1);
	clipRect = new Rect(3, 3, w - 3, h - 3);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
	// Set up the paint and draw the outline

    paint.setShader(null);
	paint.setStrokeWidth(3);
	paint.setColor(Color.GRAY);
	paint.setStyle(Style.STROKE);
	canvas.drawRoundRect(outlineRect, 10, 10, paint);

	// Set the cliprect

	canvas.clipRect(clipRect);

	// Translate to the clip rect

	canvas.translate(clipRect.left, clipRect.top);
    }
}
