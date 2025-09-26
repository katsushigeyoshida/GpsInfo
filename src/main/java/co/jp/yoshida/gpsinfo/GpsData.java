package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;

import java.text.SimpleDateFormat;
import java.util.Locale;
//import java.util.TimeZone;

/**
 * GPSデータの構造体
 * Created by katsushige on 2015/02/28.
 */
public class GpsData {

    private static final String TAG = "GpsData";

    private int count;                  //	要素番号
    private double latitude;		    //	緯度
    private double longitude;		    //	経度
    private double elevator;	        //	高度
    private Calendar cal;			    //	日時
    private Calendar cal2;			    //	日時(前回日時)
    private long lap = 0;			    //	経過時間(s)
    private long lap2 = 0;              //  経過時間(滞留時間を除く)(s)
    private double distance = 0;	    //	前回値からの距離
    private double disCovered = 0;	    //	累積距離(km)
    private double speed = 0;		    //	速度
    private double direct = 0;		    //	方向
    private long holdTime = 10 * 60;    //  非計測時間

    TimeZone timezone = TimeZone.getTimeZone("Asia/Tokyo");
    Locale locale = Locale.JAPAN;

    YLib ylib;

    /**
     * コンストラクタ
     */
    public GpsData() {
        ylib = new YLib();
        cal = Calendar.getInstance(timezone, locale);
        cal2 = Calendar.getInstance(timezone, locale);
        cal2.set(Calendar.HOUR_OF_DAY, 9);
    }

    public GpsData(Context c) {
        ylib = new YLib(c);
        cal = Calendar.getInstance(timezone, locale);
        cal2 = Calendar.getInstance(timezone, locale);
        cal2.set(Calendar.HOUR_OF_DAY, 9);
    }

    public void setCount(int c) {
        count = c;
    }

    public  void setHoldTime(long hold) {
        holdTime = hold;
    }

    public void setLatitude(String lati) {
        latitude = Double.valueOf(lati);
    }

    public void setLonitude(String longi) {
        longitude = Double.valueOf(longi);
    }

    public void setElevator(String ele) {
        elevator = Double.valueOf(ele);
    }

    public void setDistance(double dis) {
        distance = dis;
    }

    public void setDisCovered(double dis) {
        disCovered = dis;
    }

    public void setSpeed(double spd) {
        speed = spd;
    }

    /**
     * 時間の登録
     * @param time
     */
    public void setTime(String time) {
//		cal.setTimeZone(timezone);
        cal.set(Calendar.YEAR, Integer.valueOf(time.substring(0, 4)));
        cal.set(Calendar.MONTH, Integer.valueOf(time.substring(5, 7))-1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(time.substring(8, 10)));
        cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(11, 13)));
        cal.set(Calendar.MINUTE, Integer.valueOf(time.substring(14, 16)));
        cal.set(Calendar.SECOND, Integer.valueOf(time.substring(17, 19)));
        cal.add(Calendar.HOUR_OF_DAY, 9);			//	日本時間にして取り込む
    }

    //	経過時間の設定(秒)
    public void setLapTime(GpsData preData) {
        cal2 = preData.getCalendar();
        long interval = (cal.getTimeInMillis() - preData.getCalendar().getTimeInMillis())/1000;
        lap = preData.getLap() + interval;
        lap2 = preData.getLap2() + (holdTime<interval?0:interval);
    }

    //	距離と累積距離の設定(km)
    public void setDistance(GpsData preData) {
        distance = ylib.distance(longitude, latitude, preData.getLongitude(), preData.getLatitude());
        disCovered = preData.getDisCovered() + distance;
    }

    //	進行方向の設定(方位角 °)
    public void setDirect(GpsData preData) {
        direct = ylib.azimuth2(longitude, latitude, preData.getLongitude(), preData.getLatitude());
    }

    //	前回データから距離、進行方向、経過時間、速度を求める
    public void setPreData(GpsData preData) {
        setDistance(preData);
        setDirect(preData);
        setLapTime(preData);
        speed = distance / (lap - preData.getLap()) * 3600f; // km/h
    }

    public double getCount() {
        return count;
    }

    public double getLatitude() {
        return latitude;
    }

    /**
     * 緯度
     * @return
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * 高度(m)
     * @return
     */
    public double getElevator() {
        return elevator;
    }

    /**
     * 測定時の時間
     * @return
     */
    public Calendar getCalendar() {
        return cal;
    }
    public Calendar getPreCalendar() {
        return cal2;
    }

    public String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(cal.getTime());
    }

    public String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(cal.getTime());
    }

    /**
     * 経過時間(s)
     * @return
     */
    public long getLap() {
        return lap;
    }

    /**
     * 経過時間(s) 滞留時間を除く
     * @return
     */
    public long getLap2() {
        return lap2;
    }

    /**
     * 前回値からの距離(km)
     * @return
     */
    public double getDistance() {
        return distance;
    }

    /**
     * 累積距離(km)
     * @return
     */
    public double getDisCovered() {
        return disCovered;
    }

    /**
     * 方位角(°)
     * @return
     */
    public double getDirect() {
        return direct;
    }

    /**
     * 速度(km/h)
     * @return
     */
    public double getSpeed() {
        return speed;
    }

    public long getHoldTime() {
        return holdTime;
    }

}
