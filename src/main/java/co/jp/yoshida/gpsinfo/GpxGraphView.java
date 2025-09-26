package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.icu.util.Calendar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GPXファイルデータのグラフ表示の描画処理
 */
public class GpxGraphView extends SurfaceView
        implements SurfaceHolder.Callback {
    private static final String TAG = "GpxGraphView";

    private Context mC = null;              //  コンテキスト
    private SurfaceHolder mSurfaceHolder;
    private int mWidth;                     //  画面幅
    private int mHeight;                    //  画面高さ

    private float mWorldLeft;               //  グラフエリア左端
    private float mWorldTop;                //  グラフエリア上端
    private float mWorldRight;              //  グラフエリア右端
    private float mWorldBottom;             //  グラフエリア下端
    private float mLeftGapRaito = 0.15f;    //  グラフエリアの左ギャップ比率
    private float mBottomGapRaito = 0.08f;  //  グラフエリアの下ギャップ比率
    private float mRightGapRaito = 0.02f;   //  グラフエリアの右ギャップ比率
    private float mTopGapRaito = 0.02f;     //  グラフエリアの上ギャップ比率
    private float mFontSize = 40f;          //  文字の大きさ

    private String mGpxFilePath;                //  GPXファイルパス
    private GpsDataReader gpsDataReader = null; //  GPXデータクラス
    private int mGraphDataSize = 800;	        //  表示最大データサイズ 　0の時は無制限
    private long m_HoldTime = 600;              //  滞留除去時間(s)

    private int mDataSize;                      //  GPXデータサイズ
    private Calendar mStartTime;                //  開始時間
    private Calendar mEndTime;                  //  終了時間
    private float mTotalLap;                    //  移動時間
    private float mTotalDistance;               //  移動距離
    private float mMaxElevator;                 //  最大高度
    private float mMinElevator;                 //  最小高度
    private float mMaxSpeed;                    //  最大速度
    private float mMinSpeed;                    //  最小速度
    private float mAveSpeed;                    //  平均速度
    private float mMaxTop;

    private List<PointF> mOrgData;
    private List<PointF> mPlotData;             //  表示用座標データ
    private String mGraphType = "距離/時間";    //  グラフの種類
    private String mXTitle = "時間(分)";        //  横軸タイトル
    private String mYTitle = "距離(km)";        //  縦軸タイトル
    private String mFilterType = "";            //  フィルターの種類
    private int mFilterDataCount = 0;           //  フィルタで使用するデータ数
    private float mFilterPassRate = 1.0f;       //  ローパスフィルタの係数
    private float mLowPassPreData = 0;
    private boolean mHoldTimeOut = false;

    private YDraw ydraw;
    private YLib ylib;

    public GpxGraphView(Context context)  {
        super(context);

        mC = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        ydraw = new YDraw(mSurfaceHolder);
        ylib = new YLib();
        mOrgData = new ArrayList<PointF>();
        mPlotData = new ArrayList<PointF>();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mWidth = getWidth();
        mHeight = getHeight();
        dispGraph();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        mWidth = getWidth();
        mHeight = getHeight();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    /**
     * グラフの表示
     */
    public void dispGraph() {
        initScreen(Color.WHITE);                    //  グラフ領域の初期化
        setDataParametor();
        createPlotData(mGraphType, mFilterType);    //  表示用座標データを求める
        setWorldArea(mGraphType);                   //  グラフエリアの設定
        drawGraphAxis();                            //  グラフの枠とメモリ表示
        drawData(mFilterType);
    }

    /**
     * 表示用座標データの作成
     * @param graphType     グラフの種類
     */
    private void createPlotData(String graphType, String filterType) {
        mOrgData.clear();
        for (int i = 0; i < gpsDataReader.getDataSize(); i++) {
            PointF p = getPoint(graphType, i);
            mOrgData.add(p);
        }
        mPlotData.clear();
        PointF p;
        mLowPassPreData = mOrgData.get(0).y;
        p = getPlotData(filterType, 0);
        mMaxTop = p.y;
        mPlotData.add(p);
        for (int i = 1; i < gpsDataReader.getDataSize(); i++) {
            p = getPlotData(filterType, i);
            mPlotData.add(p);
            mMaxTop = Math.max(mMaxTop, p.y);
        }
    }

    /**
     * データの表示
     * @param filterType    データの種類
     */
    private void drawData(String filterType) {
        ydraw.lockCanvas();
        ydraw.setColor(Color.BLACK);

        PointF ps,pe;
        mLowPassPreData = mPlotData.get(0).y;
        for (int i = 1; i < gpsDataReader.getDataSize(); i++) {
            pe = getPlotData(filterType, i);
            ydraw.drawWLine(mPlotData.get(i-1), mPlotData.get(i));
        }

        ydraw.unlockCanvasAndPost();
    }

    /**
     * フィルター処理
     * @param filterType        フィルターの種類
     * @param i                 データ位置
     * @return                  処理後のデータ
     */
    private PointF getPlotData(String filterType, int i) {
        if (filterType == null) {
            return mOrgData.get(i);
        } else if (filterType.compareTo("移動平均")==0) {
            return getAvePlotData(i, mFilterDataCount);
        } else if (filterType.compareTo("中央値")==0) {
            return getMidPlotData(i, mFilterDataCount);
        } else if (filterType.compareTo("ローパス")==0) {
            return getLowPassPlotData(i, mFilterPassRate);
        } else {
            return mOrgData.get(i);
        }
    }

    /**
     * 移動平均を求める
     * @param index     データの位置
     * @param count     平均を求めるためのデータ数
     * @return          平均値
     */
    private PointF getAvePlotData(int index,int count) {
        if (count < 2)
            return mOrgData.get(index);
        int startIndex = Math.max(index - count / 2, 0);
        int endIndex = Math.min(index + count / 2, mOrgData.size() - 1);
        float y = 0;
        for (int i = startIndex; i < endIndex; i++)
            y += mOrgData.get(i).y;
        PointF p = new PointF(mOrgData.get(index).x, y / (endIndex - startIndex));
        return p;
    }

    /**
     * 中央値を求める
     * @param index     データの位置
     * @param count     中央値を求めるためのデータ数
     * @return          中央値
     */
    private PointF getMidPlotData(int index,int count) {
        if (count < 2)
            return mOrgData.get(index);
        int startIndex = Math.max(index - count / 2, 0);
        int endIndex = Math.min(index + count / 2, mOrgData.size() - 1);
        int size = endIndex - startIndex;
        float[] ydata = new float[size];
        for (int i = startIndex; i < endIndex; i++)
            ydata[i - startIndex] = mOrgData.get(i).y;
        Arrays.sort(ydata);
        PointF p = new PointF(mOrgData.get(index).x, ydata[(endIndex - startIndex)/2]);
        return p;
    }

    /**
     * ローパスフィルタの処理
     * @param index     データ位置
     * @param rate      ローパスフィルタの係数
     * @return          処理後の値
     */
    private PointF getLowPassPlotData(int index, float rate) {
        if (rate == 1f)
            return mOrgData.get(index);
        mLowPassPreData = (mLowPassPreData * (1.0f - rate)) + mOrgData.get(index).y * rate;
        PointF p = new PointF(mOrgData.get(index).x, mLowPassPreData);
        return p;
    }

    /**
     * グラフの種類別で座標データの取得
     * @param graphType     グラフの種類
     * @param n             データNo
     * @return
     */
    private PointF getPoint(String graphType, int n) {
        float x = 0;
        float y = 0;
        if (graphType.compareTo("距離/時間")==0) {
            x = getLapTime(n) / 60;
            y = (float)gpsDataReader.getDisCovered(n);
        } else if (graphType.compareTo("速度/時間")==0) {
            x = getLapTime(n) / 60;
            y = (float)gpsDataReader.getSpeed(n);
        } else if (graphType.compareTo("高度/時間")==0) {
            x = getLapTime(n) / 60;
            y = (float)gpsDataReader.getElevator(n);
        } else if (graphType.compareTo("時間/距離")==0) {
            x = (float)gpsDataReader.getDisCovered(n);
            y = getLapTime(n) / 60;
        }else if (graphType.compareTo("速度/距離")==0) {
            x = (float)gpsDataReader.getDisCovered(n);
            y = (float)gpsDataReader.getSpeed(n);
        } else if (graphType.compareTo("高度/距離")==0) {
            x = (float)gpsDataReader.getDisCovered(n);
            y = (float)gpsDataReader.getElevator(n);
        }
        return new PointF(x, y);
    }

    /**
     * グラフの枠と目盛りの表示
     */
    private void drawGraphAxis() {
        ydraw.lockCanvas();
        //  グラフの枠
        ydraw.setColor(Color.BLACK);
        ydraw.drawWRect(new RectF(mWorldLeft, mWorldTop, mWorldRight, mWorldBottom));

        //  端の目盛り
        ydraw.setTextSize(mFontSize);
        ydraw.setColor(Color.BLUE);
        ydraw.drawWString(String.format("%.1f",mWorldLeft), new PointF(mWorldLeft, mWorldBottom), YDraw.TEXTALIGNMENT.CT);
        ydraw.drawWString(String.format("%.1f",mWorldBottom), new PointF(mWorldLeft, mWorldBottom), YDraw.TEXTALIGNMENT.RC);
        ydraw.drawWString(String.format("%.1f",mWorldRight), new PointF(mWorldRight, mWorldBottom), YDraw.TEXTALIGNMENT.CT);
        ydraw.drawWString(String.format("%.1f",mWorldTop), new PointF(mWorldLeft, mWorldTop), YDraw.TEXTALIGNMENT.RC);
        //  縦の補助線
        float dx = ylib.graphStepSize(mWorldRight-mWorldLeft,5);
        float x = (float)Math.floor(mWorldLeft / dx) * dx + dx;
        for ( ; x< mWorldRight; x += dx) {
            ydraw.drawWLine(new PointF(x, mWorldTop), new PointF(x, mWorldBottom));
            ydraw.drawWString(String.format("%.1f",x), new PointF(x, mWorldBottom), YDraw.TEXTALIGNMENT.CT);
        }
        //  横の補助線
        float dy = ylib.graphStepSize(mWorldTop-mWorldBottom,5);
        float y = (float)Math.floor(mWorldBottom / dy) * dy + dy;
        for ( ; y< mWorldTop; y += dy) {
            ydraw.drawWLine(new PointF(mWorldLeft, y), new PointF(mWorldRight, y));
            ydraw.drawWString(String.format("%.1f",y), new PointF(mWorldLeft, y), YDraw.TEXTALIGNMENT.RC);
        }
        //  軸タイトルの表示
        RectF worldArea = ydraw.getWorldArea();
        ydraw.drawWPathOnText(mXTitle, new PointF(mWorldLeft, worldArea.top), new PointF(mWorldRight, worldArea.top), YDraw.TEXTALIGNMENT.CB);
        ydraw.drawWPathOnText(mYTitle, new PointF(worldArea.left , mWorldTop), new PointF(worldArea.left, mWorldBottom), YDraw.TEXTALIGNMENT.CB);
        //  速度の補助線表示
        auxSpeedLine();

        ydraw.unlockCanvasAndPost();
    }

    /**
     * 速度基準補助線の表示
     * 平均速度に対して振れ幅を想定し、平均速度の近傍とその前後に速度の補助線を表示
     */
    private void auxSpeedLine() {
        float speedStep = ylib.graphStepSize(mAveSpeed, 10);
        float baseSpeed = (int)(mAveSpeed / speedStep) * speedStep;
        if ((baseSpeed + speedStep /2) < mAveSpeed) {
            baseSpeed += speedStep / 2;
        } else if (mAveSpeed < (baseSpeed - speedStep / 2)) {
            baseSpeed -= speedStep / 2;
        }
        ydraw.setColor(Color.GREEN);
        for (int i = -1; i <= 1; i++) {
            drawSpeedLine(baseSpeed + speedStep * i);
        }
    }

    /**
     * 速度補助線の表示
     * 距離/時間の時は原点から速度の各向きで線を引く
     * エリア外れる場合にはクリッピングする
     * @param speed
     */
    private void drawSpeedLine(float speed) {
        PointF ps, pe;
        ps = new PointF(0, 0);
        if (mGraphType.compareTo("距離/時間")==0) {
            float xe = mWorldRight;
            float ye = mWorldRight / 60f * speed;
            if (mWorldTop < ye) {
                xe = mWorldTop / 60f / speed;
                ye = mWorldTop;
            }
            pe = new PointF(xe, ye);
        } else if (mGraphType.compareTo("時間/距離")==0) {
            float xe = mWorldRight;
            float ye = mWorldRight / speed * 60f;
            if (mWorldTop < ye) {
                xe = mWorldTop / 60f * speed;
                ye = mWorldTop;
            }
            pe = new PointF(xe, ye);
        } else if (mGraphType.compareTo("速度/時間")==0 ||
                mGraphType.compareTo("速度/距離")==0) {
            ps = new PointF(mWorldLeft, speed);
            pe = new PointF(mWorldRight, speed);
        } else {
            return;
        }
        ydraw.drawWLine(ps, pe);
        ydraw.drawWPathOnText(String.format("%.1f km/h    ", speed), ps, pe, YDraw.TEXTALIGNMENT.RB);
    }

    public void setHoldTimeOutReverse() {
        mHoldTimeOut = !mHoldTimeOut;
    }

    public void setHoldTimeOut(boolean holdTime) {
        mHoldTimeOut = holdTime;
    }

    /**
     * フィルター値の設定
     * @param filterType        フィルターの種類
     * @param dataCount         移動平均と中央値のデータ数
     * @param passRate          ローパスフィルタの係数
     */
    public boolean setFilter(String filterType, int dataCount, float passRate) {
        if (mFilterType.compareTo(filterType)==0 &&
                mFilterDataCount == dataCount &&
                mFilterPassRate == passRate)
            return false;
        mFilterType = filterType;
        mFilterDataCount = dataCount;
        mFilterPassRate = passRate;
        return true;
    }

    /**
     * グラフの種類を設定
     * @param graphType     グラフの種類
     */
    public boolean setGraphType(String graphType) {
        if (mGraphType.compareTo(graphType)==0)
            return false;
        mGraphType = graphType;
        return true;
    }

    /**
     * GPXファイルを読み込む
     * @param path      GPXファイルパス
     */
    public void setGpxFileRead(String path) {
        mGpxFilePath = path;
        gpsDataReader = new GpsDataReader(mC);
        gpsDataReader.setGraphDataSize(mGraphDataSize);     //  表示最大データ数
        gpsDataReader.setHoldTime(m_HoldTime);              //  非計測時間(滞留時間)(S)
        setGpxFileRead(path, false);
    }

    /**
     * GPXデータの読み込み
     * @param path      GPXファイルパス
     * @param addData   追加読込フラグ
     */
    public void setGpxFileRead(String path, boolean addData) {
        if (ylib.existsFile(path)) {
            //	GPXデータの取り込み
            try {
                gpsDataReader.xmlFileRaeder(path, addData);   //  新規読込
            } catch (XmlPullParserException e) {
                Toast.makeText(mC, e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(mC, path + " ファイルが存在しません", Toast.LENGTH_SHORT).show();
            return;
        }
        if (gpsDataReader.getDataSize() < 2) {              // データサイズの確認
            Toast.makeText(mC, "データが存在しません", Toast.LENGTH_SHORT).show();
            return;
        }

        setDataParametor();
    }

    /**
     * GPXデータから各種パラメータを取得する
     */
    private void setDataParametor(){
        mDataSize = gpsDataReader.getDataSize();                                    //  データの数
        mStartTime = gpsDataReader.getDate(0);                                  //  開始時間
        mEndTime = gpsDataReader.getDate(mDataSize - 1);                        //  終了時間
        mTotalLap = getLapTime(mDataSize - 1);                                  //  移動時間(s)
        mTotalDistance = (float)gpsDataReader.getDisCovered(mDataSize - 1);     //  移動距離(km)
        mMinElevator = (float)gpsDataReader.getMinElevator();                       //  最小高度(m)
        mMaxElevator = (float)gpsDataReader.getMaxElevator();                       //  最大高度
        mMinSpeed = (float)gpsDataReader.getMinSpeed();                             //  最小速度
        mMaxSpeed = (float)gpsDataReader.getMaxSpeed();                             //  最大速度
        mAveSpeed = mTotalDistance / mTotalLap * 3600;                              //  平均速度(km/h)
    }

    /**
     * グラフの種類によってグラフエリアを設定する
     * @param graphType
     */
    private void setWorldArea(String graphType) {
        float left = 0f;
        float top = mTotalDistance;
        float right = mTotalLap / 60;
        float bottom = 0f;
        if (graphType.compareTo("距離/時間")==0) {
            left = 0f;
            top = mTotalDistance;
            right = mTotalLap / 60;
            bottom = 0f;
            mXTitle = "時間(分)";
            mYTitle = "距離(km)";
        } else if (graphType.compareTo("速度/時間")==0) {
            left = 0f;
            top = mMaxSpeed;
            right = mTotalLap / 60;
            bottom = 0f;
            mXTitle = "時間(分)";
            mYTitle = "速度(km/h)";
        } else if (graphType.compareTo("高度/時間")==0) {
            left = 0f;
            top = mMaxElevator;
            right = mTotalLap / 60;
            bottom = 0f;
            mXTitle = "時間(分)";
            mYTitle = "高度(m)";
        } else if (graphType.compareTo("時間/距離")==0) {
            left = 0f;
            top = mTotalLap/60;
            right = mTotalDistance;
            bottom = 0f;
            mXTitle = "距離(km)";
            mYTitle = "時間(分)";
        }else if (graphType.compareTo("速度/距離")==0) {
            left = 0f;
            top = mMaxSpeed;
            right = mTotalDistance;
            bottom = 0f;
            mXTitle = "距離(km)";
            mYTitle = "速度(km/h)";
        } else if (graphType.compareTo("高度/距離")==0) {
            left = 0f;
            top = mMaxElevator;
            right = mTotalDistance;
            bottom = 0f;
            mXTitle = "距離(km)";
            mYTitle = "高度(m)";
        }
        top = mMaxTop;
        top = ylib.roundCeil(top * 1.2f,2);
        setWorldArea(left, top, right, bottom);
    }

    /**
     * 滞留時間を考慮した累積時間の取得
     * @param n
     * @return
     */
    private float getLapTime(int n) {
        return mHoldTimeOut?gpsDataReader.getLapTimeHoldWithout(n):gpsDataReader.getLapTime(n);
    }

    /**
     * グラフ領域の設定
     * @param left      左座標
     * @param top       上座標
     * @param right     右座標
     * @param bottom    下座標
     */
    private void setWorldArea(float left, float top, float right, float bottom) {
        float leftMargine = (right - left) * mLeftGapRaito;
        float bottomMargine = (top - bottom) * mBottomGapRaito;
        float rightMargine = (right - left) * mRightGapRaito;
        float topMargine = (top - bottom) * mTopGapRaito;
        mWorldLeft = left;
        mWorldTop = top;
        mWorldRight = right;
        mWorldBottom = bottom;
        ydraw.setWorldArea(left - leftMargine, top + topMargine,
                right + rightMargine, bottom - bottomMargine, false);
        Log.d(TAG,"setWorldArea: "+mWorldLeft+" "+mWorldTop+" "+mWorldRight+" "+mWorldBottom);
        Log.d(TAG,"setWorldArea: "+(left - leftMargine)+" "+(top + topMargine)+" "+(right + rightMargine)+" "+(bottom - bottomMargine));
    }

    /**
     * グラフ領域の初期化
     * @param backColor     背景色
     */
    private void initScreen(int backColor) {
        Log.d(TAG,"initScreen " + mWidth + " " + mHeight);
        ydraw.setViewArea(mWidth, mHeight);
        ydraw.setWorldArea(0,0, mWidth, mHeight, true);
        ydraw.lockCanvas();
        ydraw.backColor(backColor);
        ydraw.unlockCanvasAndPost();
    }
}
