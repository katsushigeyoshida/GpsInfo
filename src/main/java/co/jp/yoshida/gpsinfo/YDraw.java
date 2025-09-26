package co.jp.yoshida.gpsinfo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.util.SizeF;
import android.view.SurfaceHolder;

public class YDraw {
    private static final String TAG = "YDraw";

    protected Canvas canvas = null;
    protected Canvas canvas2 = null;
    private Canvas offScreen = null;
    private Bitmap mBitmap = null;
    private Paint paint = new Paint();
    protected SurfaceHolder mHolder;          //  SurfaceViewのHolder
    public enum TEXTALIGNMENT {LT,LC,LB,CT,CC,CB,RT,RC,RB};
    private TEXTALIGNMENT mTextAlignment=TEXTALIGNMENT.LB;
    private int[] mColor16 = {  //  black,silver,maroon,purple,green,olive,navy,teal, gray,white,red,fuchsia,lime,yellow,blue,aqua
            Color.BLACK,Color.rgb(0xc0,0xc0,0xc0),Color.rgb(0x80,0,0),Color.rgb(0x80,0,0x80),
            Color.GREEN,Color.rgb(0x80,0x80,0),Color.rgb(0,00,0x80),Color.rgb(0,0x80,0x80),
            Color.GRAY,Color.WHITE,Color.RED,Color.rgb(0xff,0,0xff),
            Color.rgb(0,0xff,0),Color.YELLOW,Color.BLUE,Color.rgb(0,0xff,0xff)
    };
    private int[] mColor15 = {  //  black,silver,maroon,purple,green,olive,navy,teal, gray,red,fuchsia,lime,yellow,blue,aqua
            Color.BLACK,Color.rgb(0xc0,0xc0,0xc0),Color.rgb(0x80,0,0),Color.rgb(0x80,0,0x80),
            Color.GREEN,Color.rgb(0x80,0x80,0),Color.rgb(0,00,0x80),Color.rgb(0,0x80,0x80),
            Color.GRAY,Color.RED,Color.rgb(0xff,0,0xff),
            Color.rgb(0,0xff,0),Color.YELLOW,Color.BLUE,Color.rgb(0,0xff,0xff)
    };

    private SizeF mView;                    //  描画領域
    private RectF mWorld;                   //  論理座標領域
    private boolean mAspectFix = false;     //  アスペクト比固定

    private float mWidth;                   //  描画領域(View)
    private float mHeight;                  //  描画領域(View)
    private boolean mInverseGraphX = true;  //  グラフの左右を逆にする
    private boolean mInverseGraphY = true;  //  グラフの上下を逆にする
    private boolean mLockCanvas = false;    //  lockCanvasの設定状態
    private float mScaleX = 1.0f;           //  X方向スケール
    private float mScaleY = 1.0f;           //  Y方向スケール
    private float mScaleOX = 0.0f;          //  スケール原点
    private float mScaleOY = 0.0f;          //  スケール原点
    private float mOX = 0.0f;               //  原点
    private float mOY = 0.0f;               //  原点Y
    private float mDX = 0.0f;               //  原点移動
    private float mDY = 0.0f;               //  原点移動Y
    private float mRotate = 0.0f;           //  回転(radian)
    private float mRotateOX = 0.0f;         //  回転の原点
    private float mRotateOY = 0.0f;         //  回転の原点
    private float mSkewX = 0.0f;            //  傾き
    private float mSkewY = 0.0f;            //  傾き
    private float mPointSize = 2f;          //  点の大きさ(半径)
    private float mAngle = 0.0f;            //  方向(Radian)
    private PointF mPPoint = new PointF();  //  lineTo線分の起点座標

    public YDraw() {
        Log.d(TAG, "YDraw");
    }

    public YDraw(Canvas c) {
        Log.d(TAG, "YDraw: Canvas");
        canvas = c;
        canvas2 = c;
    }

    public YDraw(SurfaceHolder holder) {
        Log.d(TAG, "YDraw: Holder");
        mHolder = holder;
    }


    /***
     * 描画両機の大きさを設定する
     * ダブルバッファのビットマップを作成する
     * @param width     幅
     * @param height    高さ
     */
    public void setViewArea(int width, int height) {
        mView = new SizeF(width, height);
        mWidth = width;
        mHeight = height;
        setOffScreen(width, height);
    }

    /***
     * 論理座標(ワールド座標)を設定する
     * bottom < topであれば成立することができる、アスペクト比は保持する
     * @param left      左の座標
     * @param top       上の座標
     * @param right     右の座標
     * @param bottom    下の座標
     */
    public void setWorldArea(float left, float top, float right, float bottom, boolean aspectFix) {
        Log.d(TAG,"setWorldArea: "+left+","+top+","+right+","+bottom+"  "+mWidth+","+mHeight);
        mInverseGraphX = right < left ? false : true;            //  グラフをさかさまのままにするかしないか
        mInverseGraphY = bottom < top ? false : true;            //  グラフをさかさまのままにするかしないか
        if (!mInverseGraphX) {
            float tmp = left;
            left = right;
            right = tmp;
        }
        if (!mInverseGraphY) {
            float tmp = top;
            top = bottom;
            bottom = tmp;
        }
        float worldWidth = right - left;
        float worldHeight = bottom - top;
        float scaleX = mView.getWidth() / (right - left);
        float scaleY = mView.getHeight() / (bottom - top);
        Log.d(TAG,"setWorldArea: Scale "+scaleX+","+scaleY+" "+ mInverseGraphY);
        //  アスペクト比を固定
        mAspectFix = aspectFix;
        if (aspectFix) {
            if (scaleX < scaleY) {
                top -= (mView.getHeight() / scaleX - worldHeight) / 2f;
                bottom += (mView.getHeight() / scaleX - worldHeight) / 2f;
            } else {
                left -= (mView.getWidth() / scaleY - worldWidth) / 2f;
                right += (mView.getWidth() / scaleY - worldWidth) / 2f;
            }
        }
        mWorld = new RectF(left, top, right, bottom);
        Log.d(TAG,"setWorldArea: "+mWorld.left+","+mWorld.top+","+mWorld.right+","+mWorld.bottom+
                " "+mInverseGraphX+" "+mInverseGraphY);
    }

    /**
     * 論理座標(ワールド座標)領域を取得する
     * @return      論理座標領域
     */
    public RectF getWorldArea() {
        return mWorld;
    }

    /**
     * 描画モードにする(表示はされない)
     * locCanvas が設定されていると何もしない
     */
    public void lockCanvas() {
        if (mLockCanvas) {
            Log.d(TAG, "lockCanvas:" + mLockCanvas);
            return;
        }
        lockCanvas2();
    }

    /***
     * 描画モードにする
     * オフスクリーンを設定し、座標系を7設定する
     */
    public void lockCanvas2() {
        canvas = mHolder.lockCanvas();
        mLockCanvas = true;
        if (mBitmap!=null) {
            // オフスクリーンバッファを生成する
            offScreen = new Canvas(mBitmap);
            canvas2 = offScreen;
        } else  {
            canvas2 = canvas;
        }
        canvas2.scale(mScaleX, mScaleY, mScaleOX, mScaleOY);
        canvas2.translate(mDX, mDY);
        canvas2.rotate(mRotate, mRotateOX, mRotateOY);
        canvas2.skew(mSkewX, mSkewY);
    }

    /**
     * 描画したデータを表示し、描画モードを解除
     */
    public void unlockCanvasAndPost() {
        if (!mLockCanvas) {
            Log.d(TAG,"unlockCanvasAndPost:"+mLockCanvas);
            return;
        }
        if (mBitmap!=null) {
            // オフスクリーンバッファを描画する
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
        mHolder.unlockCanvasAndPost(canvas);
        mLockCanvas = false;
    }

    /**
     * CANVASを設定する
     * @param c
     */
    public void setCanvas(Canvas c) {
        canvas = c;
        canvas2 =c;
    }

    /**
     * ダブルバッファの描画ビットマップの作成
     * 使わない場合は
     * @param width
     * @param height
     */
    public void setOffScreen(int width, int height) {
        // オフスクリーン用のBitmapを生成する
        mBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
    }

    /**
     * 背景色を設定する
     * @param color
     */
    public void backColor(int color) {
        if (!mLockCanvas)
            lockCanvas();
        canvas2.drawColor(color);
    }


    /***
     * Canvas に直接スケールを設定する
     * locCanvasの後に設定しないと設定がクリアされる
     * @param sx        X方向のスケール値
     * @param sy        Y方向のスケール値
     */
    public void Scale(float sx, float sy) {
        if (mLockCanvas)
            canvas2.scale(sx, sy);
    }

    /***
     * 原点を指定してCanvas に直接スケールを設定する
     * locCanvasの後に設定しないと設定がクリアされる
     * @param sx        X方向のスケール
     * @param sy        Y方向のスケール
     * @param px        スケールのX原点
     * @param py        スケールのY原点
     */
    public void Scale(float sx, float sy, float px, float py) {
        if (mLockCanvas)
            canvas2.scale(sx, sy, px, py);
    }

    /***
     * Canvas に直接移動の設定をする
     * locCanvasの後に設定しないと設定がクリアされる
     * @param dx        X方向の移動量
     * @param dy        Y方向の移動量
     */
    public void Translate(float dx, float dy) {
        if (mLockCanvas)
            canvas2.translate(dx, dy);
    }

    /***
     * Canvas に直接回転を設定する
     * locCanvasの後に設定しないと設定がクリアされる
     * @param degrees       回転角(deg)
     */
    public void Rotate(float degrees) {
        if (mLockCanvas)
            canvas2.rotate(degrees);
    }

    /***
     * Canvas に直接原点を指定して回転を設定する
     * locCanvasの後に設定しないと設定がクリアされる
     * @param degrees   回転角度(deg)
     * @param px        原点のX座標
     * @param py        原点のY座標
     */
    public void Rotate(float degrees, float px, float py) {
        if (mLockCanvas)
            canvas2.rotate(degrees, px, py);
    }

    /***
     * Canvas に直接座標に歪（傾き）をもたせる
     * locCanvasの後に設定しないと設定がクリアされる
     * @param sx        X方向の傾斜量
     * @param sy        Y方向の傾斜量
     */
    public void Skew(float sx, float sy) {
        if (mLockCanvas)
            canvas2.skew(sx, sy);
    }

    /**
     * Canvasの座標を拡大・縮小
     * locCanvasの後に設定しないと設定がクリアされる
     * @param sx        Xスケール
     * @param sy        Yスケール
     */
    public void setScale(float sx, float sy) {
        mScaleX = sx;
        mScaleY = sy;
    }


    public float getScaleX() {
        return mScaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }

    /**
     * Canvasの座標を拡大・縮小のパラメータを設定
     * Canvasへの反映はlockCanvas2 で行われる
     * locCanvasの後に設定しないと設定がクリアされる
     * @param sx        Xスケール
     * @param sy        Yスケール
     * @param px        スケールの原点
     * @param py        スケールの原点
     */
    public void setScale(float sx, float sy, float px, float py) {
        mScaleX = sx;
        mScaleY = sy;
        mScaleOX = px;
        mScaleOY = py;
    }

    /**
     * Canvasの座標を移動のパラメータを設定
     * Canvasへの反映はlockCanvas2 で行われる
     * locCanvasの後に設定しないと設定がクリアされる
     * @param dx        X方向の移動量
     * @param dy        Y方向の移動量
     */
    public void setTranslate(float dx, float dy) {
        mDX = dx;
        mDY = dy;
        mOX += dx;
        mOY += dy;
    }

    /**
     * Canvasの座標を回転するパラメータを設定
     * Canvasへの反映はlockCanvas2 で行われる
     * locCanvasの後に設定しないと設定がクリアされる
     * @param degrees   回転角(deg)
     */
    public void setRotate(float degrees) {
        mRotate = degrees;
    }

    /**
     * 中心座標を指定してCanvasの座標を回転するパラメータを設定
     * Canvasへの反映はlockCanvas2 で行われる
     * locCanvasの後に設定しないと設定がクリアされる
     * @param degrees   回転角度(deg)
     * @param px        回転の中心座標
     * @param py        回転の中心座標
     */
    public void setRotate(float degrees, float px, float py) {
        mRotate = degrees;
        mRotateOX = px;
        mRotateOY = py;
    }

    /**
     * Canvasの座標に歪（傾き）をもたせる
     * locCanvasの後に設定しないと設定がクリアされる
     * @param sx        X方向の傾斜量
     * @param sy        Y方向の傾斜量
     */
    public void setSkew(float sx, float sy) {
        mSkewX = sx;
        mSkewY = sy;
    }


    /**
     * 色を設定する
     * @param color
     */
    public void setColor(int color) {
        paint.setColor(color);
    }

    /**
     * 16色の内から番号で指定した取得する
     * @param colorNo       色番号
     * @return              色
     */
    public int getColor16(int colorNo) {
        return mColor16[colorNo % 16];
    }

    /**
     * 白を除く15色から番号で指定した取得する
     * @param colorNo       色番号
     * @return              色
     */
    public int getColor15(int colorNo) {
        return mColor15[colorNo % 15];
    }

    /**
     * 描画スタイルの設定 塗潰し/塗潰し+周囲/周囲のみ
     * @param style     Paint.Style.FILL/FILL_AND_STROKE/STROKE
     */
    public void setStyle(Paint.Style style) {
        paint.setStyle(style);
    }

    /**
     * 線幅や文字の太さを設定する
     * @param w
     */
    public void setStrokeWidth(float w) {
        paint.setStrokeWidth(w);
    }

    /**
     * 文字サイズの設定(ピクセル単位)
     * SP (Scale-independent Pixels)で指定されたリソースから値を取る場合は、
     * getResources().getDimensionPixelSizeメソッドを使います。
     * @param size
     */
    public void setTextSize(float size) {
        paint.setTextSize(size);
    }

    /**
     * 点を描画するときの点の大きさ(半径)をピクセル単位で設定する
     * @param size
     */
    public void setPointSize(float size) {
        mPointSize = size;
    }


    /**
     * X座標変換
     * @param x
     * @return
     */
    private float cnvX(float x) {
        return mOX + x * mScaleX;
    }

    /**
     * Y座標変換
     * @param y
     * @return
     */
    private float cnvY(float y) {
        return mOY + y * mScaleY;
    }


    /**
     * X方向の長さをワールド座標からスクリーン座標に変換する
     * @param wl        X方向の長さ
     * @return          変換後の長さ
     */
    public float cnvWorld2ScreenX(float wl) {
        return wl * mView.getWidth() / mWorld.width();
    }

    /**
     * Y方向の長さをワールド座標からスクリーン座標に変換する
     * @param wl        Y方向の長さ
     * @return          変換後の長さ
     */
    public float cnvWorld2ScreenY(float wl) {
        return wl * mView.getHeight() / mWorld.height();
    }

    /**
     * X方向の長さをスクリーン座標からワールド座標に変換
     * @param sl        X方向の長さ
     * @return          変換後の長さ
     */
    public float cnvScreen2WorldX(float sl) {
        return sl * mWorld.width() / mView.getWidth();
    }

    /**
     * Y方向の長さをスクリーン座標からワールド座標に変換
     * @param sl        Y方向の長さ
     * @return          変換後の長さ
     */
    public float cnvScreen2WorldY(float sl) {
        return sl * mWorld.height() /mView.getHeight();
    }

    /**
     * ワールド座標からスクリーン座標に変換する(点座標)
     * @param wp        点のワールド座標
     * @return          点のスクリーン座標
     */
    public PointF cnvWorld2Screen(PointF wp) {
        PointF sp = new PointF();
        sp.x = (mInverseGraphX?(wp.x - mWorld.left):(mWorld.right - wp.x)) * mView.getWidth() / mWorld.width();
        sp.y = (mInverseGraphY?(wp.y - mWorld.top):(mWorld.bottom- wp.y)) * mView.getHeight() / mWorld.height();
        return sp;
    }

    /**
     * ワールド座標からスクリーン座標に変換する(四角座標)
     * @param wf        四角のワールド座標
     * @return          四角のスクリーン座標
     */
    public RectF cnvWorld2Screen(RectF wf) {
        PointF ps = cnvWorld2Screen(new PointF(wf.left, wf.top));
        PointF pe = cnvWorld2Screen(new PointF(wf.right, wf.bottom));
        return new RectF(ps.x, ps.y, pe.x, pe.y);
    }

    /**
     * ワールド座標で四角を描画する
     * @param rect      四角の座標
     */
    public void drawWRect(RectF rect) {
        drawRect(cnvWorld2Screen(rect));
    }

    /**
     * ワールド座標で四角を描画する(塗潰し)
     * @param rect      四角の座標
     */
    public void fillWRect(RectF rect) {
        fillRect(cnvWorld2Screen(rect));
    }

    /**
     * ワールド座標で楕円を描画する
     * @param rect      楕円の座標
     */
    public void drawWOval(RectF rect) {
        drawOval(cnvWorld2Screen(rect));
    }

    /**
     * ワールド座標で楕円を描画する(塗潰し)
     * @param rect      楕円の座標
     */
    public void fillWOval(RectF rect) {
        fillOval(cnvWorld2Screen(rect));
    }

    /**
     * ワールド座標で円を描画する
     * アスペクト比固定出ない場合、半径はX方向で合わせる
     * @param cp        中心座標
     * @param r         半径
     */
    public void drawWCircle(PointF cp, float r) {
        drawCircle(cnvWorld2Screen(cp), cnvWorld2ScreenX(r));
    }

    /**
     * ワールド座標で円を描画する(塗潰し)
     * アスペクト比固定出ない場合、半径はX方向で合わせる
     * @param cp        中心座標
     * @param r         半径
     */
    public void fillWCircle(PointF cp, float r) {
        fillCircle(cnvWorld2Screen(cp), cnvWorld2ScreenX(r));
    }

    /**
     * ワールド座標でせんを描画する
     * @param ps        始点座標
     * @param pe        終点座標
     */
    public void drawWLine(PointF ps, PointF pe) {
        drawLine(cnvWorld2Screen(ps), cnvWorld2Screen(pe));
    }

    /**
     * ワールド座標で点を描画する
     * 点の大きさはsetPointSize()で設定
     * @param wp        点の座標
     */
    public void drawWPoint(PointF wp) {
        PointF sp = cnvWorld2Screen(wp);
        drawPoint(sp.x, sp.y, mPointSize);
    }

    /**
     * ワールド座標で文字列を表示、起点は左上
     * 文字サイズはsetTextSize()で設定
     * @param text      文字列
     * @param wp        起点座標
     */
    public void drawWString(String text, PointF wp) {
        PointF sp = cnvWorld2Screen(wp);
        drawString(text, sp.x, sp.y);
    }

    /**
     * ワールド座標でアライメントを指定して文字列を表示
     * @param text      文字列
     * @param wp        起点座標
     * @param ta        文字列のアライメント
     */
    public void drawWString(String text, PointF wp, TEXTALIGNMENT ta) {
        PointF sp = cnvWorld2Screen(wp);
        drawString(text, sp.x, sp.y, ta);
    }

    /**
     * ワールド座標で文字列の中心を指定して文字列を表示
     * @param text      文字列
     * @param wp        中心座標
     */
    public void drawWStringCenter(String text, PointF wp) {
        PointF sp = cnvWorld2Screen(wp);
        drawStringCenter(text, sp.x, sp.y);
    }

    /**
     * ワールド座標で２点で指定した線に沿って文字列を表示する
     * @param text      文字列
     * @param wps       始点座標
     * @param wpe       終点座標
     * @param ta        文字列のアライメント(左右は始点、終点となる)
     */
    public void drawWPathOnText(String text, PointF wps, PointF wpe, TEXTALIGNMENT ta) {
        PointF sp = cnvWorld2Screen(wps);
        PointF ep = cnvWorld2Screen(wpe);
        drawPathOnText(text, sp.x, sp.y, ep.x, ep.y, ta);
    }


    /**
     * 四角表示(塗潰しなし)
     * @param x         起点座標 左上
     * @param y         起点座標 左上
     * @param width     幅
     * @param height    高さ
     */
    public void drawRect(float x, float y, float width, float height) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.STROKE);
        canvas2.drawRect(x, y, x + width, y + height, paint);
    }

    /**
     * 四角表示(塗潰しなし) 座標は始点と終点
     * @param rect      left, top, right, bottom
     */
    public void drawRect(RectF rect) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.STROKE);
        canvas2.drawRect(rect, paint);
    }

    /**
     * 四角表示(塗潰し) 座標のスケール変更対応
     * @param x         起点座標 左上
     * @param y         起点座標 左上
     * @param width     幅
     * @param height    高さ
     */
    public void fillRect(float x, float y, float width, float height) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawRect( x, y, x + width, y + height, paint);
    }

    /**
     * 四角表示(塗潰し) 座標は始点と終点
     * @param rect      left, top, right, bottom
     */
    public void fillRect(RectF rect) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawRect( rect, paint);
    }

    /**
     * 楕円表示(塗潰し) 座標のスケール変更対応
     * @param x         左端X座標
     * @param y         下端Y座標
     * @param width     横幅
     * @param height    高さ
     */
    public void fillOval(float x, float y, float width, float height) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawOval(new RectF(x, y, x + width, y + height), paint);
    }

    /**
     * 楕円表示(塗潰し)
     * @param rect      四角座標
     */
    public void fillOval(RectF rect) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawOval(rect, paint);
    }

    /**
     * 楕円表示(塗潰しなし) 座標のスケール変更対応
     * @param x         左端X座標
     * @param y         下端Y座標
     * @param width     横幅
     * @param height    高さ
     */
    public void drawOval(float x, float y, float width, float height) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.STROKE);
        canvas2.drawOval(new RectF(x, y, x + width,y + height), paint);
    }

    /**
     * 楕円表示(塗潰しなし)
     * @param rect      四角座標
     */
    public void drawOval(RectF rect) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.STROKE);
        canvas2.drawOval(rect, paint);
    }

    /**
     * 塗潰し円を描画
     * @param cx        中心座標
     * @param cy        中心座標
     * @param r         半径
     */
    public void fillCircle(float cx, float cy, float r) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawOval(new RectF(cx - r, cy - r, cx + r, cy + r), paint);
//        canvas2.drawOval(new RectF(inverseX(cx)-r, inverseY(cy)-r,
//                inverseX(cx) + r, inverseY(cy) + r), paint);
    }

    /***
     * 塗潰しの円の描画
     * @param cp        中心座標
     * @param r         半径
     */
    public void fillCircle(PointF cp, float r) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawOval(new RectF(cp.x-r, cp.y-r, cp.x + r, cp.y + r), paint);
    }

    /**
     * 円を描画
     * @param cx        中心座標
     * @param cy        中心座標
     * @param r         半径
     */
    public void drawCircle(float cx, float cy, float r) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.STROKE);
//        canvas2.drawOval(new RectF(inverseX(cx) - r/mScaleX, inverseY(cy) - r/mScaleY,
//                inverseX(cx) + r/mScaleX, inverseY(cy) + r/mScaleY), paint);
        canvas2.drawOval(new RectF(cx - r, cy - r, cx + r, cy + r), paint);
    }

    /***
     * 円を描画する
     * アスペクト比が異なるときは半径はXscaleに合わせる
     * @param cp    中心座標
     * @param r     半径
     */
    public void drawCircle(PointF cp, float r) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.STROKE);
        canvas2.drawOval(new RectF(cp.x-r, cp.y - r, cp.x + r, cp.y + r), paint);
    }

    /**
     * 線分描画
     * @param startx    始点座標X
     * @param starty    始点座標Y
     * @param endx      終点座標X
     * @param endy      始点座標Y
     */
    public void drawLine(float startx, float starty, float endx, float endy) {
        if (!mLockCanvas)
            lockCanvas();
        canvas2.drawLine(startx, starty, endx, endy, paint);
    }

    /***
     * 線分描画
     * @param ps    始点座標
     * @param pe    終点座標
     */
    public void drawLine(PointF ps, PointF pe) {
        if (!mLockCanvas)
            lockCanvas();
        canvas2.drawLine(ps.x, ps.y, pe.x, pe.y, paint);
    }

    /**
     * 点を描画
     * @param x
     * @param y
     */
    public void drawPoint(float x, float y) {
        if (!mLockCanvas)
            lockCanvas();
        canvas2.drawPoint(x, y, paint);
    }

    /**
     * 点の大きさを変えて描画
     * @param x
     * @param y
     * @param r
     */
    public void drawPoint(float x, float y, float r) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawOval(new RectF(x - r, y - r, x + r, y + r), paint);
    }

    /**
     * グラフィック文字表示 座標のスケール変更対応(スクリーン座標)
     * @param text
     * @param x     左上座標
     * @param y
     */
    public void drawString(String text, float x, float y) {
        if (!mLockCanvas)
            lockCanvas();
        paint.setStyle(Paint.Style.FILL);
        canvas2.drawText(text, x, y, paint);
    }

    /**
     * グラフィック文字を中心に表示する(スクリーン座標)
     * @param text
     * @param cx        中心座標
     * @param cy
     */
    public void drawStringCenter(String text, float cx, float cy) {
        float x = cx - measureText(text) / 2f;
        float y = cy + getTextSize() / 2f - getDecent();
        drawString(text, x, y);
    }

    /**
     * 文字列の大きさに合わせて背景色を塗潰してグラフィック文字を表示する(スクリーン座標)
     * @param text          文字列
     * @param x             左下座標
     * @param y             左下座標
     * @param textColor     文字の色
     * @param backColor     背景色
     */
    public void drawStringWithBack(String text, float x, float y, int textColor, int backColor){
        RectF rect = getStringRect(x, y, text);
        paint.setColor(backColor);
        fillRect(rect);
        paint.setColor(textColor);
        drawString(text, x, y);
    }

    /**
     * 背景を塗潰して文字列を表示する(スクリーン座標)
     * @param text          文字列
     * @param cx            中心座標
     * @param cy            中心座標
     * @param textColor     文字列の色
     * @param backColor     背景色
     */
    public void drawStringCenterWithBack(String text, float cx, float cy, int textColor, int backColor){
        float x = cx - measureText(text) / 2f;
        float y = cy + getTextSize() / 2f - getDecent();
        drawStringWithBack(text, x, y, textColor, backColor);
    }

    /**
     * アライメントを指定して文字列を表示
     * @param text      文字列
     * @param x         座標
     * @param y         座標
     * @param ta        文字列のアライメント
     */
    public void drawString(String text, float x, float y, TEXTALIGNMENT ta) {
        //  左下に座標を移動
        x = x - measureText(text) * getAlignmentX(ta);
        y = y + getTextSize() * getAlignmentY(ta) - getDecent() * (1f - getAlignmentY(ta));
        drawString(text, x, y);
    }

    /**
     * 背景を塗潰して文字列をアライメントして描画
     * @param text          文字列
     * @param x             座標
     * @param y             座標
     * @param ta            文字列のアライメント
     * @param textColor     文字列の色
     * @param backColor     背景色
     */
    public void drawStringWithBack(String text, float x, float y, TEXTALIGNMENT ta, int textColor, int backColor) {
        //  左下に座標を移動
        x = x - measureText(text) * getAlignmentX(ta);
        y = y + getTextSize() * getAlignmentY(ta) - getDecent() * (1f - getAlignmentY(ta));
        drawStringWithBack(text, x, y, textColor, backColor);
    }

    /**
     * 文字列の横方向のアライメントの係数
     * @param ta        文字列のアライメント
     * @return          係数(left 0/center 0.5/right 1.0)
     */
    private float getAlignmentX(TEXTALIGNMENT ta) {
        float alignmentX=0f;
        if (ta == TEXTALIGNMENT.LT || ta == TEXTALIGNMENT.LC || ta == TEXTALIGNMENT.LB) alignmentX = 0;
        if (ta == TEXTALIGNMENT.CT || ta == TEXTALIGNMENT.CC || ta == TEXTALIGNMENT.CB) alignmentX = 0.5f;
        if (ta == TEXTALIGNMENT.RT || ta == TEXTALIGNMENT.RC || ta == TEXTALIGNMENT.RB) alignmentX = 1f;
        return alignmentX;
    }

    /**
     * 文字列の縦方向のアライメントの係数
     * @param ta        文字列のアライメント
     * @return          係数(bottom 0/center 0.5/top 1.0)
     */
    private float getAlignmentY(TEXTALIGNMENT ta) {
        float alignmentY=0f;
        if (ta == TEXTALIGNMENT.LT || ta == TEXTALIGNMENT.CT || ta == TEXTALIGNMENT.RT) alignmentY = 1f;
        if (ta == TEXTALIGNMENT.LC || ta == TEXTALIGNMENT.CC || ta == TEXTALIGNMENT.RC) alignmentY = 0.5f;
        if (ta == TEXTALIGNMENT.LB || ta == TEXTALIGNMENT.CB || ta == TEXTALIGNMENT.RB) alignmentY = 0;
        return alignmentY;
    }

    /**
     * 2点間の中央に文字を描画
     * @param text      文字列
     * @param xs        始点
     * @param ys
     * @param xe        終点
     * @param ye
     * @param ta        文字のアライメント
     */
    public void drawPathOnText(String text, float xs, float ys, float xe, float ye, TEXTALIGNMENT ta) {
        Path path = new Path();
        path.moveTo(xs, ys);
        path.lineTo(xe, ye);
        path.close();
        float hOffset = 0;
        if (ta == TEXTALIGNMENT.LT || ta == TEXTALIGNMENT.LC || ta == TEXTALIGNMENT.LB)
            hOffset = 0;
        else if (ta == TEXTALIGNMENT.CT || ta == TEXTALIGNMENT.CC || ta == TEXTALIGNMENT.CB)
            hOffset = (distance(xs,ys,xe,ye) - paint.measureText(text)) / 2;
        else if (ta == TEXTALIGNMENT.RT || ta == TEXTALIGNMENT.RC || ta == TEXTALIGNMENT.RB)
            hOffset = distance(xs,ys,xe,ye) - paint.measureText(text);
        float vOffset = getTextSize() * getAlignmentY(ta) - getDecent() * (1f - getAlignmentY(ta));
        canvas2.drawPath(path, paint);
        canvas2.drawTextOnPath(text, path, hOffset, vOffset, paint);
    }


    /**
     * 文字サイズを取得する
     * @return      the paint's text size in pixel units.
     */
    public float getTextSize() {
        return paint.getTextSize();
    }

    /**
     * 文字列の長さを取得
     * @param text
     * @return          The width of the text
     */
    public float measureText(String text) {
        return paint.measureText(text);
    }

    /**
     * 文字のベースラインより上の高さを取得
     * @return
     */
    public float getAscent() {
        return paint.ascent();
    }

    /**
     * 文字のベースラインよりも下の高さを取得
     * @return
     */
    public float getDecent() {
        return paint.descent();
    }

    /**
     * 文字列の領域座標の取得
     * @param x         起点座標
     * @param y         起点座標
     * @param text      文字列
     * @return          文字列の領域
     */
    public RectF getStringRect(float x, float y, String text) {
        RectF rect = new RectF(x, y -  getTextSize(), x + measureText(text), y + getDecent());
        return rect;
    }

    /**
     * 文字列の領域の座標を取得
     * @param text
     * @return
     */
    public RectF getStringRect(String text) {
        RectF rect = new RectF(0,0,measureText(text), getTextSize());
        return rect;
    }

    /**
     * 文字列の領域の座標を取得
     * @param left      開始座標
     * @param top       開始座標
     * @param text      文字列
     * @return          領域の座標
     */
    public RectF getString(float left, float top, String text) {
        RectF rect = new RectF(left,top,left + measureText(text), top + getTextSize());
        return rect;
    }

    // ============== タートルグラフィック ============================
    /**
     * 線分の開始位置に移動
     * @param x
     * @param y
     */
    public void moveTo(float x, float y) {
        mPPoint.x = x;
        mPPoint.y = y;
    }

    /**
     * 線分の開始位置に移動
     * @param p
     */
    public void moveTo(PointF p) {
        mPPoint = p;
    }

    /**
     * 線分の開始位置に移動し秒化方向を設定
     * @param p
     * @param ang
     */
    public void moveTo(PointF p, float ang) {
        mPPoint = p;
        mAngle = ang;
    }

    /**
     * 線分の起点位置の取得
     * @return
     */
    public PointF getCurrentPoint() {
        PointF p = new PointF(mPPoint.x, mPPoint.y);
        return p;
    }

    /**
     * 線分の描画方向の設定
     * @param ang(radian)
     */
    public void setLineAngle(float ang) {
        mAngle = ang;
    }

    /**
     * 線分の描画方向の取得(絶対角度)
     * @return
     */
    public float getLineAngle() {
        return mAngle;
    }

    /**
     * 現在の線分の起点位置を長さと角度で相対的に移動
     * @param length    線の長さ
     * @param ang       相対角度(RAD)
     */
    public void amoveTo(float length, float ang) {
        mAngle += ang;
        mAngle = (float)anglrNormalize(mAngle);
        mPPoint.x += length * (float)Math.cos(mAngle);
        mPPoint.y += length * (float)Math.sin(mAngle);
    }

    /**
     * 相対座標で線分を描画
     * @param dx
     * @param dy
     */
    public void lineTo(float dx, float dy) {
        float px = mPPoint.x;
        float py = mPPoint.y;
        mPPoint.x += dx;
        mPPoint.y += dy;
        drawLine(px, py, mPPoint.x, mPPoint.y);
    }

    /**
     * 現在の座標から相対座標で線分を引く
     * @param p
     */
    public void lineTo(PointF p) {
        p.offset(mPPoint.x, mPPoint.y);
        drawLine(mPPoint.x, mPPoint.y, p.x, p.y);
        mPPoint = p;
    }

    /***
     * 前の座標から絶対座標で線分を引く
     * @param x
     * @param y
     */
    public void plineTo(float x, float y) {
        drawLine(mPPoint.x, mPPoint.y, x, y);
        mPPoint.x = x;
        mPPoint.y = y;
    }

    /**
     * 相対位置で線分を描画
     * @param length        相対距離
     * @param ang           相対角度(rad)
     */
    public void alineTo(float length, float ang) {
        float px = mPPoint.x;
        float py = mPPoint.y;
        amoveTo(length, ang);
        drawLine(px, py, mPPoint.x, mPPoint.y);
    }

    /**
     * 角度(Rad)を-pi～piの間になるように変換する
     * @param ang
     * @return
     */
    private double anglrNormalize(double ang) {
        return  (ang + Math.PI) % (2d * Math.PI) - Math.PI;
    }

    /**
     * ２点間の距離を求める
     * @param sx
     * @param sy
     * @param ex
     * @param ey
     * @return
     */
    private float distance(float sx, float sy, float ex, float ey) {
        return (float)Math.sqrt((double)((ex - sx)*(ex - sx) + (ey - sy)*(ey - sy)));
    }

    /**
     * ２点間の距離を求める
     * @param sp
     * @param ep
     * @return
     */
    private float distance(PointF sp, PointF ep) {
        return distance(sp.x, sp.y, ep.x, ep.y);
    }

}
