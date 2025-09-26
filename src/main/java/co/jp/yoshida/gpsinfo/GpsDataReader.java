package co.jp.yoshida.gpsinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.icu.util.Calendar;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
 *  GpsData読み込み
 *  GPSのXMLデータをファイルから読み込む
 * Created by katsushige on 2015/02/28.
 *
 * GpsDataReader(Context c)     コンストラクタ
 * setGraphDataSize(int size)   最大データサイズの設定
 * setHoldTime(long holdtime )  滞留除去時間の設定
 *
 * xmlFileRaeder(String path)   XMLデータファイルからGPSデータの取り込み
 * xmlFileRaeder(String path, boolean addMode)  XMLデータファイルからGPSデータの取り込み(追加選択)
 *
 * getDataOrgCount()    元々のデータ数の取得
 * getDataCount()   格納データ数の取得
 * getDate(int index)   日時データの出得
 * getDataSize()    データ数
 * getData(int index)   ロケーションデータの取得
 * getDisCovered(int index) 出発点からの距離(km)
 * getSpeed(int index)  速度の取得
 * getElevator(int index)   高度の取得
 * getMaxSpeed()    最大速度を求める
 * getMinSpeed()    最小速度を求める
 * getMaxElevator() 最大高度を求める(m)
 * getMinElevator() 最低高度を求める(m)
 * getMaxDistanceNo()   スタートから最も遠い位置の取得
 * getMaxDistance() 最遠距離(km)
 * getMaxPeakNo()   最高高度のデータ位置の取得
 * getHoldTime()    滞留除去時間の取得
 * getLapTimeCount(float laptime)   指定時間でのデータ位置
 * getLapTime(int n)    経過時間の取得(min)
 * getLapTimeWithout(int n) 滞留時間を覗いた経過時間の取得(min)
 *
 * xmlFileRead(String path) (内部関数)XMLファイルからデータを読み込む(データの間引き含む)
 * xmlCount(XmlPullParser xmlPullParser)    (内部関数)XMLデータ内のポイントデータ数をカウントする
 * xmlAnalyze(XmlPullParser xmlPullParser,int ptStep)   (内部関数)XMLデータからGPSデータを抽出して格納
 * SetXmlData(...)  (内部関数)XMLファイルのデータをGpsDataの構造体でmGpsListに登録
 *
 *
 */
public class GpsDataReader {

    private static final String TAG = "GpsDataReader";

    private int orgCount = 0;				    //	ファィル内のデータ数
    private ArrayList<GpsData> mGpsList = null; //	GPSデータリスト
    private double mMaxDistance = 0;            //  累積距離
    private int GraphDataSize = 800;            //  表示最大データサイズ
    private long mHoldTime = 10*60;             //  非計測時間(滞留時間)(S)

    private Context c;
    YLib ylib;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public GpsDataReader(Context c) {
        this.c = c;
        ylib = new YLib();
    }

    /**
     * 最大データサイズの設定
     * @param size      データ数
     */
    public void setGraphDataSize(int size) {
        GraphDataSize = size;
    }

    /**
     * 滞留除去時間の時間設定
     * @param holdtime      滞留時間(min)
     */
    public void setHoldTime(long holdtime ) {
        mHoldTime = holdtime;
    }

    /**
     * XMLデータファイルからGPSデータの取り込み
     * @param path      XMLファイル名
     * @return
     * @throws XmlPullParserException
     */
    public boolean xmlFileRaeder(String path) throws XmlPullParserException {
        return xmlFileRaeder(path, false);
    }

    /**
     * XMLデータファイルからGPSデータの取り込み
     * @param path          ファイル名
     * @param addMode       データの追加可否
     * @return
     * @throws XmlPullParserException
     */
    public boolean xmlFileRaeder(String path, boolean addMode) throws XmlPullParserException {
        Log.d(TAG, "xmlFileRaeder: " + path + " " + GraphDataSize);

        if (mGpsList == null)
            mGpsList = new ArrayList<GpsData>();
        if (!addMode)
            mGpsList.clear();
        orgCount = 0;
        int ptStep = 1;
        String xmlData = "";

        try {
            //XmlPullParser xmlPullParser = Xml.newPullParser();		//	旧クラス
            XmlPullParser xmlPullParser = XmlPullParserFactory.newInstance().newPullParser();
            // 	XMLデータを読み込む(指定サイズを超える場合間引きする)
            xmlData = xmlFileRead(path).toString();
            // Android4の場合 UTF-8ファイルを読みこ込むとエラーになるのを防ぐ
            if (xmlData.charAt(0) == 0xFEFF) {
                xmlData = xmlData.substring(1);
            }
            //	XMLデータを構文解析する
            xmlPullParser.setInput(new StringReader(xmlData));
            // XMLデータからGPSデータを抽出格納する
            xmlAnalyze(xmlPullParser, ptStep);
            //Toast.makeText(c, "データ数 " + mGpsList.size(), Toast.LENGTH_SHORT).show();
            return true;
        } catch (XmlPullParserException e) {
            Log.d("XmlPullParserSample", "Error 1");
            Toast.makeText(c, "xmlFileRaeder 1 Error " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * XMLファイルからデータを読み込む
     * データ数と間引きのstepを求めて、データの間引きもおこなう。
     * @param path      XMLファイル名
     * @return
     */
    private StringBuffer xmlFileRead(String path) {
        Log.d(TAG, "xmlFileRead: " + path + " " + GraphDataSize);
        StringBuffer xmlData = new StringBuffer("");
        StringBuffer lineBuffer = new StringBuffer("");
        int elementCount = 0;							//	位置データ数
        int step = 1;
        boolean trkptFlg = false;
        boolean descFlg = false;
        long filesize = ylib.getFileSize(path);
        int dataQuantity = 0;
        try {
            XmlFileReader xmlReader = new XmlFileReader(path);
            StringBuffer buf = xmlReader.getElementData();
            while (0 < buf.length()) {
                if (0 <= buf.indexOf("<trkpt")) {
                    elementCount++;
                    trkptFlg = true;
                }
                if (dataQuantity == 0 && 0 <= buf.indexOf("<desc")) {
                    descFlg = true;						//	位置要素データのSTART
                }
                if (dataQuantity == 0 || !descFlg)
                    lineBuffer.append(buf);
                if (dataQuantity == 0 && 0 <= buf.indexOf("/desc>")) {
                    descFlg = false;					    //	位置要素データのEND
                }
                if (trkptFlg) {
                    if (0 <= buf.indexOf("/trkpt>")) {
                        if ((elementCount % step) == 0) {
                            //Log.d("xmlFileRead", elementCount + ": " + lineBuffer);
                            xmlData.append(lineBuffer);
                            //	1要素のデータサイズから最大データ数に収まるようにSTEP数を求める
                            if (GraphDataSize!=0 && dataQuantity == 0) {
                                dataQuantity = (int) (filesize / lineBuffer.length());
                                step = dataQuantity / GraphDataSize + 1;
                            }
                        }
                        lineBuffer.delete(0, lineBuffer.length());
                        trkptFlg = false;
                    }
                } else {
                    xmlData.append(lineBuffer);
                    lineBuffer.delete(0, lineBuffer.length());
                }
                buf = xmlReader.getElementData();
                if (buf == null)
                    break;
            }
            Log.d(TAG,"XmlFileRead: "+ "Read End");
        } catch (Exception e) {
            Log.d(TAG,"XmlFileRead:"+"Exception Error " + e.getMessage());
        }
        orgCount = elementCount;
        return xmlData;
    }


    /**
     * XMLデータ内のポイントデータ数をカウントする。
     * @param xmlPullParser
     * @return
     */
    private int xmlCount(XmlPullParser xmlPullParser) {
        Log.d(TAG, " xmlCount Start:");
        int count=0;
        try {
            int eventType;
            //xmlPullParser.require(XmlPullParser.START_TAG, null, "trk");
            eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.END_TAG) {
                    if (xmlPullParser.getName().compareTo("trkpt") == 0) {
                        count++;
                    }
                }
                eventType = xmlPullParser.next();
            }
            Log.d(TAG, " xmlCount End:");
        } catch (Exception e) {
            Log.d(TAG, "xmlCount Error:"+ e.getMessage());
            //Toast.makeText(c, "xmlCount Error " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        Log.d(TAG,"xmlCount " + count);
        return count;
    }

    /**
     * XMLデータからGPSデータを抽出して格納する
     * @param xmlPullParser		構文解析したデータ
     * @param ptStep
     */
    @SuppressLint("NewApi")
    private void xmlAnalyze(XmlPullParser xmlPullParser,int ptStep) {
        int ptCount = 0;
        String tagType = "";
        String latitude = "";
        String longitude = "";
        String elevator = "";
        String time = "";

        try {
            int eventType;
            eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
//                    Log.d(TAG,"xmlAnalyze: "+"Start document");
                } else if(eventType == XmlPullParser.END_DOCUMENT) {
//                    Log.d(TAG,"xmlAnalyze: "+"End document");
                } else if(eventType == XmlPullParser.START_TAG) {
                    tagType = xmlPullParser.getName();
                    for (int i=0; i<xmlPullParser.getAttributeCount(); i++) {
                        if (xmlPullParser.getAttributeName(i).compareTo("lat") == 0) {          //	緯度データ
                            if ( ylib.isFloat(xmlPullParser.getAttributeValue(i)))
                                latitude = xmlPullParser.getAttributeValue(i);
                        } else if (xmlPullParser.getAttributeName(i).compareTo("lon") == 0)	{	//	経度データ
                            if ( ylib.isFloat(xmlPullParser.getAttributeValue(i)))
                                longitude = xmlPullParser.getAttributeValue(i);
                        } else if (xmlPullParser.getAttributeName(i).compareTo("ele") == 0) {	//	高度データ
                            if ( ylib.isFloat(xmlPullParser.getAttributeValue(i)))
                                elevator = xmlPullParser.getAttributeValue(i);
                        } else if (xmlPullParser.getAttributeName(i).compareTo("time") == 0) {	//	時間データ
                            time = xmlPullParser.getAttributeValue(i);
                        }
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    //	1要素分が終了した時にデータを登録
                    if (xmlPullParser.getName().compareTo("trkpt") == 0) {
                        SetXmlData(ptCount,ptStep,latitude,longitude,elevator,time);			//	データの登録
                        ptCount++;
                    }
                    tagType = "";
                } else if(eventType == XmlPullParser.TEXT) {
                    if (!xmlPullParser.getText().isEmpty()) {
                        if (tagType.compareTo("ele") == 0) {					//	高度データ
                            if ( ylib.isFloat(xmlPullParser.getText()))
                                elevator = xmlPullParser.getText();
                        } else if (tagType.compareTo("time") == 0) {			//	時間データ
                            time = xmlPullParser.getText();
                        } else if (tagType.compareTo("lat") == 0) {			    //	緯度データ
                            if ( ylib.isFloat(xmlPullParser.getText()))
                                latitude = xmlPullParser.getText();
                        } else if (tagType.compareTo("lon") == 0)	{			//	経度データ
                            if ( ylib.isFloat(xmlPullParser.getText()))
                                longitude = xmlPullParser.getText();
                        }
                    }
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            Log.d(TAG,"xmlAnalyze: "+"Error:"+e.getMessage());
            //Toast.makeText(c, "xmlAnalyze Error " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * GPSデータの登録
     * XMLファイルのデータをGpsDataの構造体でmGpsListに登録する
     * @param ptCount       データの位置
     * @param ptStep        未使用
     * @param latitude      緯度
     * @param longitude     経度
     * @param elevator      高度
     * @param time          時間
     */
    private void SetXmlData(int ptCount, int ptStep, String latitude,
                            String longitude, String elevator, String time) {
        // Log.d(TAG, "Data: " + ptCount + ":[ " + latitude + "," +
        // longitude + "," + elevator + "," + time + "]");
        GpsData gps = new GpsData();
        gps.setHoldTime(mHoldTime);
        gps.setCount(ptCount);
        gps.setLatitude(latitude);
        gps.setLonitude(longitude);
        gps.setElevator(elevator);
        gps.setTime(time);
        if (0 < mGpsList.size())
            gps.setPreData(mGpsList.get(mGpsList.size() - 1));
        mGpsList.add(gps);
    }

    /**
     * 元々のデータ数の取得
     * @return      データ数
     */
    public int getDataOrgCount() {
        return orgCount;
    }

    /**
     * 格納データ数の取得
     * @return      データ数
     */
    public int getDataCount() {
        return mGpsList.size();
    }

    /**
     * 日時データの出得
     * @param index     データ位置
     * @return          日時データ(Calenderデータ)
     */
    public Calendar getDate(int index) {
        return ((GpsData)mGpsList.get(index)).getCalendar();
    }

    /**
     * データ数
     * @return
     */
    public int getDataSize() {
        if (mGpsList==null)
            return 0;
        else
            return mGpsList.size();
    }

    /**
     * ロケーションデータの取得
     * @param index     データNo
     * @return          GPSデータ
     */
    public GpsData getData(int index) {
        if (mGpsList==null)
            return null;
        else
            return mGpsList.get(index);
    }

    /**
     * 指定位置での経過時間(s)
     * @param index     データNo
     * @return          経過時間(s)
     */
    public long getLapTime(int index) {
        return mGpsList.get(index).getLap();
    }

    /**
     * 指定位置での経過時間(s) 滞留時間を除く
     * @param index     データNo
     * @return          経過土管
     */
    public long getLapTimeHoldWithout(int index) {
        return mGpsList.get(index).getLap2();
    }

    /**
     * 出発点からの距離(km)
     * @param index     データNo
     * @return          距離(km)
     */
    public double getDisCovered(int index) {
        return mGpsList.get(index).getDisCovered();
    }

    /**
     * 速度の取得
     * @param index     データNo
     * @return          速度(km/h)
     */
    public double getSpeed(int index) {
        return mGpsList.get(index).getSpeed();
    }

    /**
     * 高度の取得
     * @param index     データNo
     * @return          高度(m)
     */
    public double getElevator(int index) {
        if (getDataSize() <= index)
            return -1;
        return mGpsList.get(index).getElevator();
    }

    /**
     * 最大速度を求める(km/h)
     * @return      速度(km/h)
     */
    public double getMaxSpeed() {
        double maxSpeed = 0;
        for (int i=0; i < mGpsList.size(); i++) {
            if (maxSpeed < mGpsList.get(i).getSpeed())
                maxSpeed = mGpsList.get(i).getSpeed();
        }
        return maxSpeed;
    }

    /**
     * 最小速度を求める(km/h)
     * @return      速度(km/h)
     */
    public double getMinSpeed() {
        if (mGpsList.size()<1)
            return 0;
        double minSpeed = mGpsList.get(0).getSpeed();
        for (int i=1; i < mGpsList.size(); i++) {
            if (minSpeed > mGpsList.get(i).getSpeed())
                minSpeed = mGpsList.get(i).getSpeed();
        }
        return minSpeed;
    }

    /**
     * 最大高度を求める(m)
     * @return      高度(m)
     */
    public double getMaxElevator() {
        double maxElevator = 0;
        for (int i=0; i < mGpsList.size(); i++) {
            if (maxElevator < mGpsList.get(i).getElevator())
                maxElevator = mGpsList.get(i).getElevator();
        }
        return maxElevator;
    }

    /**
     * 最低高度を求める(m)
     * @return      高度(m)
     */
    public double getMinElevator() {
        if (mGpsList.size()<1)
            return 0;
        double minElevator = mGpsList.get(0).getElevator();
        for (int i=1; i < mGpsList.size(); i++) {
            if (minElevator > mGpsList.get(i).getElevator())
                minElevator = mGpsList.get(i).getElevator();
        }
        return minElevator;
    }

    /**
     * スタートから最も遠い位置の取得
     * @return      データNo
     */
    public int getMaxDistanceNo() {
        double maxDistance=0;
        int dataNo=0;
        for (int i=0; i < mGpsList.size(); i++) {
            double distance = ylib.distance(mGpsList.get(0).getLongitude(), mGpsList.get(0).getLatitude(),
                    mGpsList.get(i).getLongitude(), mGpsList.get(i).getLatitude());
            if (maxDistance < distance) {
                dataNo = i;
                maxDistance = distance;
            }
        }
        mMaxDistance = maxDistance;
        return dataNo;
    }

    /**
     *  最遠距離(km)
     * @return	最遠距離(km)
     */
    public double getMaxDistance() {
        int n;
        if (mMaxDistance == 0)
            n = getMaxDistanceNo();
        return mMaxDistance;
    }

    /**
     * 最高高度のデータ位置の取得
     * @return      データNo
     */
    public int getMaxPeakNo() {
        double maxPeak=0;
        int dataNo=0;
        for (int i=0; i < mGpsList.size(); i++) {
            double elevator = mGpsList.get(i).getElevator();
            if (maxPeak < elevator) {
                dataNo = i;
                maxPeak = elevator;
            }
        }
        return dataNo;
    }

    /**
     * 滞留除去時間の取得
     * @return      設定滞留時間(s)
     */
    public long getHoldTime() {
        if (0 < mGpsList.size())
            return mGpsList.get(0).getHoldTime();
        else
            return -1;
    }

    /**
     * 指定時間でのデータ位置
     * @param laptime   指定時間(分)
     * @return          データ位置(カウント)
     */
    public int getLapTimeCount(float laptime) {
        int i;
        for (i=0; i < getDataSize(); i++) {
            if (laptime <= getLapTime(i))
                break;
        }
        return i;
    }


    /**
     * 移動距離の取得(km)
     * @param n     始点位置
     * @param m     終点位置
     * @return      距離(km)
     */
    public double getDistance(int n, int m) {
        if (getDataSize() <= n || getDataSize() <= m)
            return -1;
        return ylib.distance(
                getData(n).getLongitude(), getData(n).getLatitude(),
                getData(m).getLongitude(), getData(m).getLatitude());
    }

    /**
     * 速度の取得(km/h)
     * @param n     始点位置
     * @param m     終点位置
     * @return      速度(km/h)
     */
    public double getSpeed(int n, int m) {
        if (getDataSize() <= n || getDataSize() <= m)
            return -1;
        double laptime = Math.abs(getLapTime(m) - getLapTime(n));
        double distance = getDistance(n, m);
        if (laptime <= 0f || distance < 0f)
            return -1;
        else
            return (distance / laptime * 60f);
    }

}
