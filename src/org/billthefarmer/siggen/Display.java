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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;

public class Display extends SiggenView
{
    private static final int MARGIN = 8;

    protected double frequency;
    protected double level;

    public Display(Context context, AttributeSet attrs)
    {
	super(context, attrs);
    }

    // On measure

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
	super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	int w = (parentWidth - MARGIN) / 2;
	int h = parentHeight / 5;

	this.setMeasuredDimension(w, h);
    }

    // On draw

    @SuppressLint("DefaultLocale")
    @Override
    protected void onDraw(Canvas canvas)
    {
	super.onDraw(canvas);
	String s = String.format("%5.2fHz", frequency);
	paint.setTextAlign(Align.LEFT);
	paint.setTextSize(height / 2);
	paint.setColor(Color.BLACK);
	paint.setStyle(Style.FILL_AND_STROKE);
	canvas.drawText(s, MARGIN, height * 2 / 3, paint);

	s = String.format("%5.2fdB", level);
	paint.setTextAlign(Align.RIGHT);
	canvas.drawText(s, width - MARGIN, height * 2 / 3, paint);
    }
}
