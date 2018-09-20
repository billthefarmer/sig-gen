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
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;

// Scale
public class Scale extends SiggenView
{
    private static final int SCALE = 500;

    private int value;

    private BitmapShader shader;
    private Matrix matrix;

    public Scale(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        matrix = new Matrix();
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

    // On size changed
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w > 0 && h > 0)
        {
            Bitmap bitmap =
                Bitmap.createBitmap(width * 2, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            paint.setStrokeWidth(2);
            paint.setColor(textColour);
            paint.setAntiAlias(false);
            for (int i = 1; i <= 10; i++)
            {
                float x = (float) Math.log10(i) * width;

                for (int j = 0; j < 2; j++)
                {
                    canvas.drawLine(x, height * 2 / 3, x, height - MARGIN,
                                    paint);
                    x += width;
                }
            }

            for (int i = 3; i < 20; i += 2)
            {
                float x = (float) (Math.log10(i / 2.0) * width);

                for (int j = 0; j < 2; j++)
                {
                    canvas.drawLine(x, height * 5 / 6, x, height - MARGIN,
                                    paint);
                    x += width;
                }
            }

            paint.setTextSize(height * 7 / 16);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextAlign(Paint.Align.CENTER);

            int a[] = {1, 2, 3, 4, 6, 8};
            for (int n : a)
            {
                float x = (float) (Math.log10(n) * width);

                canvas.drawText(n + "", x, height / 2, paint);

                canvas.drawText(n * 10 + "", x + width, height / 2, paint);
            }

            canvas.drawText("1", width * 2, height / 2, paint);

            shader = new BitmapShader(bitmap, Shader.TileMode.REPEAT,
                                      Shader.TileMode.CLAMP);
        }
    }

    // Set value
    protected void setValue(int v)
    {
        value = v;
        invalidate();
    }

    // On draw
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (shader == null)
            return;

        paint.setShader(shader);
        paint.setStyle(Paint.Style.FILL);
        matrix.setTranslate(width / 2 + (value * width) / SCALE, 0);
        shader.setLocalMatrix(matrix);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setShader(null);
        paint.setAntiAlias(false);
        paint.setColor(textColour);
        paint.setStrokeWidth(2);
        canvas.drawLine(width / 2, 0, width / 2, height, paint);
    }
}
