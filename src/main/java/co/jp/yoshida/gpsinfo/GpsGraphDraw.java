package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Calendar;

public class GpsGraphDraw extends View {
    private static final String TAG = "GpsGraphDraw";

    private Paint mPaint = new Paint();
    private Path mArrow;					    //	方位の表示
    private Path mBase;					        //	方位のベース表示
    public String mTargetDistance = "0.25";	    //	目的地までの距離
    public float mTargetDirect = -30f;			//	目的地の方位
    public boolean mAzimuthFix = false;         //  方位固定
    public float mAngle = 30f;			        //	方位角度
    public float mPitch = 0f;				    //	前後の傾き
    public float mRoll = 0f;					//	左右の傾き
    public float mAltitude = 41.2f;			    //	高度
    public float mAccuracy = 10f;		        //	精度
    public float mMaxHeight = 500f;			    //	最大高度
    public float mMinHeight = 0f;		        //	最小高度
    public float mCurLongitude = 139.69326f;
    public boolean mSunDisp = true;			    //	太陽の方向表示
    private float mCompassR = 100f;			    //	方位計のベースの半径
    private float mTextSize = 30f;			    //	東西南北の文字サイズ
    private float mVoffset = -5f;
    private float mHoffset = 15f;
    private float mDispRatio = 1f;
    private int mWidth;						    //	描画領域の幅
    private int mHeight;					    //	描画領域の高さ

    YLib ylib;

    public GpsGraphDraw(Context context) {
        super(context);

        ylib = new YLib();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        mWidth = this.getWidth();
        mHeight = this.getHeight();
        mDispRatio = (float)(mWidth<mHeight?mWidth:mHeight)/ 600f;
        mTextSize = 35f * mDispRatio;
        mCompassR = 150f * mDispRatio;
        mHoffset = mTextSize / 2f - (float)(mCompassR*Math.PI/4f);
        mBase = SetBase(mCompassR, mCompassR - 20f);
        mArrow = SetArrow(mCompassR);
        int cx = mWidth / 2;
        int cy = mHeight / 2;


        canvas.drawColor(Color.WHITE);  		//	描画領域クリア
        DrawAzimuthFixButton(canvas, mAngle);   //  方位固定表示ボタン
        DrawPitch(canvas, mPitch);		     	//	前後傾きの表示
        DrawRoll(canvas, mRoll);		    	//	左右傾きの表示
        DrawHeight(canvas, mAltitude);	     	//	高度の表示
        DrawDistance(canvas);				    //	目的地までの距離の数値表示
        DrawDirect(canvas);                     //  目的地の方位の数値表示
        canvas.translate(cx, cy);			    //	中心に座標移動
        canvas.rotate(mAzimuthFix?0f:mAngle);	//	座標を方位分回転
        DrawCompassBase(canvas);			    //	方位ベースの表示
        if (mSunDisp)
            DrawSun(canvas);				    //	太陽の方向表示
        canvas.rotate(mTargetDirect);		        //	目的地の方位
        DrawCompass(canvas);				    //	方位矢印の表示
        DrawAccuracy(canvas, mAccuracy);	    //	精度を円表示
    }

    /**
     * 方位計ベースの表示
     * @param c
     */
    private void DrawCompassBase(Canvas c) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        c.drawPath(mBase, mPaint);
        paint.setTextSize(mTextSize);
        c.drawTextOnPath("南", mBase, (float)(mCompassR*Math.PI/2*0)-mHoffset, mVoffset, mPaint);
        c.drawTextOnPath("西", mBase, (float)(mCompassR*Math.PI/2*1)-mHoffset, mVoffset, mPaint);
        c.drawTextOnPath("北", mBase, (float) (mCompassR * Math.PI / 2 * 2) - mHoffset, mVoffset, mPaint);
        c.drawTextOnPath("東", mBase, (float)(mCompassR*Math.PI/2*3)-mHoffset, mVoffset, mPaint);
    }

    /**
     * 方位矢印の表示
     * @param c
     */
    private void DrawCompass(Canvas c) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        c.drawPath(mArrow, mPaint);
    }


    /**
     * 精度を円で表示
     * @param c
     */
    private void DrawAccuracy(Canvas c, float accuracy) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        c.drawCircle(0, 0, (float) Math.sqrt((double) (accuracy / 1000)) * mCompassR, paint);
    }

    /**
     *  方向固定ボタンの表示
     * @param c
     * @param angle     方位 (度)
     */
    private void DrawAzimuthFixButton(Canvas c, float angle) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        //  ボタン円の作成
        float r =  25 * mDispRatio;
        float xc = 20 + mTextSize + r;
        float yc = 20 + mTextSize + r;
        c.drawCircle(xc, yc, r, paint);
        //  方位矢印の作成
        angle -= 90;
        float dx = r * (float)Math.cos(angle/180.*Math.PI);
        float dy = r * (float)Math.sin(angle/180.*Math.PI);
        float dxw = r/3 * (float)Math.cos((angle+90.)/180.*Math.PI);
        float dyw = r/3 * (float)Math.sin((angle+90)/180.*Math.PI);
        float xs = xc + dx;
        float ys = yc + dy;
        float xe = xc - dx;
        float ye = yc - dy;
        Path path = new Path();
        path.moveTo(xc + dx, yc + dy);
        path.lineTo(xc + dxw, yc + dyw);
        //path.lineTo(xc - dx, yc - dy);
        path.lineTo(xc - dxw, yc - dyw);
        path.lineTo(xc + dx, yc + dy);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        c.drawPath(path, paint);
    }


    /**
     * 	目的地までの距離表示
     * @param c
     */
    private void DrawDistance(Canvas c) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(mTextSize*1.2f);
        String text = mTargetDistance + " km";
        c.drawText(text, (mWidth-paint.measureText(text))/2, mHeight - 10, mPaint);
    }

    /**
     * 	目的地までの距離表示
     * @param c
     */
    private void DrawDirect(Canvas c) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(mTextSize*1.0f);
        String text = "方位 " + String.format("%3.1f", mTargetDirect) + "°";
        c.drawText(text, (mWidth-paint.measureText(text))/2, mHeight - 10 - mTextSize*1.2f, mPaint);
    }
    /**
     * 太陽の方向を表示
     * @param c
     */
    private void DrawSun(Canvas c) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        double ang = (double)getNowtimeMinute() / (24f*60f) * Math.PI * 2f;
        ang += (mCurLongitude - 135f) / 180f * Math.PI;
        double dx = mCompassR * Math.sin(ang);
        double dy = -mCompassR * Math.cos(ang);
        paint.setColor(Color.RED);
        c.drawCircle((float)dx, (float)dy, 10 * mDispRatio, paint);
        paint.setColor(Color.BLACK);
        c.drawCircle((float)-dx, (float)-dy, 7 * mDispRatio, paint);
    }

    /**
     * 左右の傾き表示
     * @param c
     * @param roll	左右の傾き角度
     */
    private void DrawRoll(Canvas c, float roll) {
        int xs = 10;				//	始点座標
        int ys = 10;
        int w = mWidth - xs - 10;	//	幅
        int h = (int)mTextSize;
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        c.drawRect(xs, ys, xs + w, ys + h, paint);
        for (float ang = -180; ang < 180; ang += 30) {
            c.drawLine(xs + w/2 + ang/180*w, ys, xs + w/2 + ang/180*w, ys + h, paint);
        }
        drawPathOnCenterText(c, "左右傾き "+ylib.roundStr2(roll,  0), xs, ys, xs + w, ys, h - 5);

        int p = (int)(roll / 180 * w);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        c.drawLine(xs + w/2 + p, ys, xs + w/2 + p, ys + h, paint);
    }

    /**
     * 前後の傾き表示
     * @param c
     * @param pitch		傾き角度
     */
    private void DrawPitch(Canvas c, float pitch) {
        int xs = 10;
        int ys = 20 + (int)mTextSize;
        int w = (int)mTextSize;
        int h = mHeight - ys - 10;
        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        c.drawRect(xs, ys, xs + w, ys + h, paint);
        for (float ang = -180; ang < 180; ang += 30) {
            c.drawLine(xs, ys + h/2 + ang/180*h, xs + w, ys + h/2 + ang/180*h, paint);
        }
        drawPathOnCenterText(c, "前後傾き "+ylib.roundStr2(pitch, 0), xs, ys + h, xs, ys, w - 5);

        int p = (int)(pitch / 180 * h);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        c.drawLine(xs, ys + h/2 + p, xs + w, ys + h/2 + p, paint);
    }

    /**
     * 高さを表示する
     * @param c
     * @param Height		高さ
     */
    private void DrawHeight(Canvas c, float Height) {
        String text;
        int w = (int)mTextSize;
        int h = mHeight - 10 - w - (int)mTextSize;
        int xs = mWidth - 10 - w;
        int ys = mHeight - 10;

        Paint paint = mPaint;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        //	枠
        c.drawLine(xs, ys, xs + w/2, ys - h, paint);
        c.drawLine(xs+w, ys, xs + w/2, ys - h, paint);
        c.drawLine(xs, ys, xs + w, ys, paint);

        //	目盛線
        float dh = mMaxHeight - mMinHeight;
        for (float sh = 0; sh < dh; sh += dh/4) {
            float d = (sh - mMinHeight)/dh;
            int ph = (int)(d * h);
            int pw = (int)(d * w / 2);
            c.drawLine(xs + pw, ys - ph, xs + w - pw, ys - ph, paint);
        }

        //	高度線表示
        float d = (Height - mMinHeight) / dh;
        int ph = (int)(d * h);
        int pw = (int)(d * w / 2);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        c.drawLine(xs + pw, ys - ph, xs + w - pw, ys - ph, paint);

        //  高度値を表示
        text = ylib.roundStr2(Height, 0);       //  小数点以下切捨て(-1だと1桁目を0にする)
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextSize(mTextSize*0.8f);
        c.drawText(text, xs + pw - paint.measureText(text) - 10, ys - ph, paint);

        //	目盛とタイトル
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        paint.setTextSize(mTextSize*0.7f);
        text = String.valueOf((int)mMinHeight);
        c.drawText(text, xs - paint.measureText(text) - 10, ys, paint);
        text = "高度 " + String.valueOf((int)mMaxHeight);
        c.drawText(text, xs - paint.measureText(text) + w/2 - 10, ys - h + 10, paint);
    }

    /**
     * 2点間の中央に文字を描画
     * @param c
     * @param text		文字列
     * @param xs		始点
     * @param ys
     * @param xe		終点
     * @param ye
     * @param vOffset	高さ方向のオフセット
     */
    private void drawPathOnCenterText(Canvas c,String text,float xs,float ys,float xe,float ye,float vOffset) {
        Path path = new Path();
        path.moveTo(xs, ys);
        path.lineTo(xe, ye);
        path.close();
        Paint paint = mPaint;
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(mTextSize*0.7f);
        float hOffset = (disPoint(xs,ys,xe,ye) - paint.measureText(text)) / 2;
        c.drawPath(path, mPaint);
        c.drawTextOnPath(text, path, hOffset, vOffset, paint);
    }

    /**
     * 2点間の距離
     * @param xs
     * @param ys
     * @param xe
     * @param ye
     * @return
     */
    private float disPoint(float xs,float ys,float xe,float ye) {
        float dx = xe - xs;
        float dy = ye - ys;
        return (float)Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * 現在時刻を分で取得
     * @return
     */
    private int getNowtimeMinute() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        //Log.d("--AS--","getNowtimeMinute :"+ hour + " , " + min);
        return hour*60+min;
    }

    /**
     * 	方位のベース作成
     * @param r		円の半径
     * @param r2	目盛の内側の半径
     * @return
     */
    private Path SetBase(float r, float r2) {
        Path path = new Path();
        path.addArc(new RectF(-r,-r, r, r), 45, 359);
        //path.addCircle(0,0, r, Path.Direction.CW);
        //	目盛の作成
        for (float ang = 0; ang <360; ang += 30) {
            double xs = Math.sin(ang/360*Math.PI*2) * r;
            double ys = Math.cos(ang/360*Math.PI*2) * r;
            path.moveTo((float)xs, (float)ys);
            double xe = Math.sin(ang/360*Math.PI*2) * r2;
            double ye = Math.cos(ang/360*Math.PI*2) * r2;
            path.lineTo((float)xe, (float)ye);
        }
        path.close();
        return path;
    }

    /**
     * 矢印形状の作成
     * @return
     */
    private Path SetArrow(float r) {
        Path path = new Path();
        path.moveTo(0, (float) (r*(-0.5)));
        path.lineTo((float)(r*(-0.2)), (float)(r*0.6));
        path.lineTo(0, (float)(r*0.5));
        path.lineTo((float)(r*0.2), (float)(r*0.6));
        path.close();
        return path;
    }
}
