package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.List;

public class ListGraphView extends SurfaceView
        implements SurfaceHolder.Callback  {

    private static final String TAG = "ListGraphView";

    private Context mC = null;              //  コンテキスト
    private SurfaceHolder mSurfaceHolder;
    private int mWidth;                     //  画面幅
    private int mHeight;                    //  画面高さ

    private float mWorldLeft;               //  グラフエリア左端
    private float mWorldTop;                //  グラフエリア上端
    private float mWorldRight;              //  グラフエリア右端
    private float mWorldBottom;             //  グラフエリア下端
    private float mLeftGapRaito = 0.1f;     //  グラフエリアの左ギャップ比率
    private float mBottomGapRaito = 0.08f;  //  グラフエリアの下ギャップ比率
    private float mRightGapRaito = 0.02f;   //  グラフエリアの右ギャップ比率
    private float mTopGapRaito = 0.05f;     //  グラフエリアの上ギャップ比率
    private float mFontSize = 40f;          //  文字の大きさ

    private String mStartDate;
    private String mLastDate;
    private int mStartDay;
    private int mLastDay;
    private int mStartMonth = 1;
    private int mSpanMonth = 12;
    private String mDataType;
    private String mCollectUnit;
    private List<float[]> mPlotData;

    private float mTotalDistance;
    private float mTotalLap;
    private float mMaxElevator;
    private float mTotalElevator;

    private YDraw ydraw;
    private YLib ylib;

    public ListGraphView(Context context) {
        super(context);

        mC = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        ydraw = new YDraw(mSurfaceHolder);
        ylib = new YLib();
    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        mWidth = getWidth();
        mHeight = getHeight();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mWidth = getWidth();
        mHeight = getHeight();
        initScreen(Color.LTGRAY);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    /**
     * グラフ表示をする
     */
    public void dispGraph() {
        //  グラフ領域の初期化
        initScreen(Color.WHITE);
        //  表示エリアの設定
        setWorldArea(mWorldLeft, mWorldTop, mWorldRight,mWorldBottom);
        //  グラフの枠と目盛の表示
        drawAxius();
        //  データの表示
        drawPlotData();
    }

    /**
     * データを標示する
     */
    private void drawPlotData() {
        ydraw.lockCanvas();
        //  棒グラフの色設定
        if (mCollectUnit.compareTo("回")==0)
            ydraw.setColor(Color.BLACK);
        else
            ydraw.setColor(Color.LTGRAY);

        //  棒グラフの表示
        for (int i = 0 ; i < mPlotData.size(); i++) {
            //  開始日付で
            if (mStartDay - 7 <= mPlotData.get(i)[0] && mPlotData.get(i)[0] <= mLastDay) {
                float left=0, top=0, right=0, bottom=0;
                if (mCollectUnit.compareTo("回")==0) {
                    left   = 0f;
                    top    = mPlotData.get(i)[0];
                    right  = mPlotData.get(i)[1];
                    bottom = mPlotData.get(i)[0];
                } else if (mCollectUnit.compareTo("日")==0) {
                    left   = 0f;
                    top    = mPlotData.get(i)[0] + 0.3f;
                    right  = mPlotData.get(i)[1];
                    bottom = mPlotData.get(i)[0] + 0.7f;
                } else if (mCollectUnit.compareTo("週")==0) {
                    left   = 0f;
                    top    = mPlotData.get(i)[0] + 1.5f;
                    right  = mPlotData.get(i)[1];
                    bottom = mPlotData.get(i)[0] + 5.5f;
                } else if (mCollectUnit.compareTo("月")==0) {
                    left   = 0f;
                    top    = mPlotData.get(i)[0] + 5f;
                    right  = mPlotData.get(i)[1];
                    bottom = mPlotData.get(i)[0] + 25f;
                }
                //  領域に入るものだけを表示
                if (top < mWorldBottom && mWorldTop < bottom) {
                    top = Math.max(top, mWorldTop);
                    bottom = Math.min(bottom, mWorldBottom);
                    if (mCollectUnit.compareTo("回")==0)
                        ydraw.drawWLine(new PointF(left, top), new PointF(right, bottom));
                    else {
                        ydraw.fillWRect(new RectF(left, top, right, bottom));
//                        Log.d(TAG,"drawPlotData: "+((bottom-top)/mSpanMonth)+" "+mSpanMonth+" "+(bottom-top)+" "+mFontSize+" "+bottom+" "+top);
                        if (0.3 <= (bottom-top)/mSpanMonth ) {
                            if (mDataType.compareTo("移動時間")==0){
                                ydraw.drawWString(" "+ylib.Sec2Time((int)right).substring(0,6), new PointF(right, (bottom + top) / 2.0f), YDraw.TEXTALIGNMENT.LC);
                            } else {
                                ydraw.drawWString(String.format(" %,.1f",right), new PointF(right, (bottom + top) / 2.0f), YDraw.TEXTALIGNMENT.LC);
                            }
                        }
                    }
                }
            }
        }
        ydraw.unlockCanvasAndPost();
    }

    /**
     * グラフの枠と目盛と補助線の表示
     */
    private void drawAxius() {
        ydraw.lockCanvas();
        //  グラフの枠
        ydraw.setColor(Color.BLACK);
        ydraw.drawWRect(new RectF(mWorldLeft, mWorldTop, mWorldRight, mWorldBottom));
        //  横補助線(期間)
        float dateYs,dateYe;
        ydraw.setTextSize(mFontSize);
        for (int i = mStartMonth; i < (mStartMonth + mSpanMonth); i++) {
            dateYs = ylib.Date2JulianDay(mStartDate.substring(0,4)+String.format("%02d01",i));
            dateYe = ylib.Date2JulianDay(mStartDate.substring(0,4)+String.format("%02d01",i+1));
            ydraw.drawWLine(new PointF(mWorldLeft, dateYe), new PointF(mWorldRight, dateYe));
            ydraw.drawWString(String.format("%d月",i), new PointF(mWorldLeft,(dateYs+dateYe)/2), YDraw.TEXTALIGNMENT.RC);
        }
        //  縦補助線
        float stepX = ylib.graphStepSize(mWorldRight, 5);
        for (float x = 0f; x < mWorldRight; x += stepX) {
            ydraw.drawWLine(new PointF(x, mWorldTop), new PointF(x, mWorldBottom));
            if (mDataType.compareTo("移動時間")==0)
                ydraw.drawWString(ylib.Sec2Time((int)x).substring(0,6),
                        new PointF(x, mWorldBottom), YDraw.TEXTALIGNMENT.CT);
            else
                ydraw.drawWString(String.format("%,.0f",x), new PointF(x, mWorldBottom), YDraw.TEXTALIGNMENT.CT);
        }
        //  軸タイトルの表示
        RectF worldArea = ydraw.getWorldArea();
        ydraw.drawWPathOnText(setDataTypeTitle(mDataType), new PointF(mWorldLeft, worldArea.bottom), new PointF(mWorldRight, worldArea.bottom), YDraw.TEXTALIGNMENT.CB);
        ydraw.setTextSize(mFontSize * 0.9f);
        String title = "　年 "+String.format("距離 %,.0fkm", mTotalDistance)+" "+String.format("時間 %,.0fh",mTotalLap/3600)+
                " "+String.format("最大標高 %,.0fm",mMaxElevator)+" "+String.format("標高差 %,.0fm",mTotalElevator);
        ydraw.drawWPathOnText(title, new PointF(worldArea.left, worldArea.top), new PointF(mWorldRight, worldArea.top), YDraw.TEXTALIGNMENT.LT);

        ydraw.unlockCanvasAndPost();
    }

    /**
     * 軸タイトルに単位を追加
     * @param dataType      データの種類
     * @return              タイトル名
     */
    private String setDataTypeTitle(String dataType) {
        if (dataType.compareTo("移動距離")==0) {
            return dataType + "(km)";
        } else if (dataType.compareTo("移動時間")==0) {
            return dataType + "(時:分)";
        } else if (dataType.compareTo("速度")==0) {
            return dataType + "(km/h)";
        } else if (dataType.compareTo("最大高度")==0) {
            return dataType + "(m)";
        } else if (dataType.compareTo("累積標高差")==0) {
            return dataType + "(m)";
        } else if (dataType.compareTo("歩数")==0) {
            return dataType;
        } else if (dataType.compareTo("歩幅")==0) {
            return dataType;
        }
        return dataType;
    }

    /**
     * グラフ領域に上下左右のマージンを付加してワールド座標を設定
     * @param left      左座標
     * @param top       上座標
     * @param right     右座標
     * @param bottom    下座標
     */
    private void setWorldArea(float left, float top, float right, float bottom) {
        float leftMargine   = (right - left) * mLeftGapRaito;
        float bottomMargine = (top - bottom) * mBottomGapRaito;
        float rightMargine  = (right - left) * mRightGapRaito;
        float topMargine    = (top - bottom) * mTopGapRaito;
        ydraw.setWorldArea(left - leftMargine, top + topMargine,
                right + rightMargine, bottom - bottomMargine, false);
    }

    /**
     * 表示用データの設定
     * @param plotData      表示データ
     */
    public void setPlotData(List<float[]> plotData) {
        mPlotData = plotData;
    }

    /**
     * パラメータの設定
     * @param dataType      データの種類
     * @param span          表示期間
     * @param startDate     開始日(yyyymmdd)
     * @param lastDate      終了日(yyyymmdd)
     */
    public void setParameter(String dataType, int span, String collectUnit, String startDate, String lastDate) {
        mDataType    = dataType;
        mSpanMonth   = span;
        mStartMonth  = ylib.str2Integer(startDate.substring(4,6));
        mStartDate   = startDate;
        mLastDate    = lastDate;
        mStartDay    = ylib.Date2JulianDay(mStartDate);
        mLastDay     = ylib.Date2JulianDay(mLastDate)+1;
        mCollectUnit = collectUnit;
    }

    /**
     * 累積データの設定
     * @param totalDistance     累積距離(km)
     * @param totalLap          累積時間(s)
     * @param maxElevator       最大高度(m)
     * @param totalElevator     累積高度(m)
     */
    public void setTotalData(float totalDistance, float totalLap, float maxElevator, float totalElevator) {
        mTotalDistance = totalDistance;
        mTotalLap = totalLap;
        mMaxElevator = maxElevator;
        mTotalElevator = totalElevator;
    }

    /**
     * データエリアの設定(データの表示領域で上下は表示期間、左右は最大データ値)
     * 実際の領域は最大データ値の1.2倍に設定
     * @param left      左端(開始日)
     * @param top       上端(終了日)
     * @param right     右端
     * @param bottom    下端
     */
    public void setWorldAreaData(float left, float top, float right, float bottom) {
        mWorldLeft   = left;
        mWorldTop    = top;
        mWorldRight  = ylib.roundCeil(right * 1.2f, 2);
        mWorldBottom = bottom;
    }


    /**
     * グラフ領域の初期化
     * @param backColor     背景色
     */
    private void initScreen(int backColor) {
        ydraw.setViewArea(mWidth, mHeight);
        ydraw.setWorldArea(0,0, mWidth, mHeight, true);
        ydraw.lockCanvas();
        ydraw.backColor(backColor);
        ydraw.unlockCanvasAndPost();
    }
}
