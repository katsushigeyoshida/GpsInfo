package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.location.Location;
import android.util.Log;

/**  GPXファイルフォーマット
 *   Sample 1
 *   <?xml version="1.0" encoding="UTF-8" ?><gpx version="1.0" creator="GPSLogger - http://gpslogger.mendhak.com/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.topografix.com/GPX/1/0" xsi:schemaLocation="http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd"><time>2012-04-22T06:41:02Z</time><bounds /><trk><trkseg><trkpt lat="35.56586083333333" lon="139.69388678333334"><src>gps</src><sat>9</sat><time>2012-04-22T06:41:02Z</time></trkpt>
 *   <trkpt lat="35.56586083333333" lon="139.69388678333334"><src>gps</src><sat>9</sat><time>2012-04-22T06:41:02Z</time></trkpt>
 *   </trkseg></trk></gpx>
 *
 *   Sample 2
 *   <?xml version="1.0" encoding="UTF-8"?>
 *   <gpx version="1.0" creator="iTravel Tech Inc. - http://www.itravel-tech.com">
 *   <trk>
 *   <name>Track2012/06/23_07:09</name>
 *   <trkseg>
 *   <trkpt lat="35.345502056" lon="139.138712980">
 *   <ele>-7.925010</ele>
 *   <speed>0.277778</speed>
 *   <time>2012-06-22T22:10:20Z</time>
 *   </trkpt>
 *   </trkseg>
 *   </trk>
 *   </gpx>
 */
/**
 * GPXファイル作成クラス
 */
public class GpxFile {

    private static final String TAG = "GpxFile";

    private String mFileFolder = "";		// 保存先フォルダ名
    private String mFilePath = "";		    // ファイル出力ディレクトリ
    private boolean mGpxFileKeep = true;	// 都度ファイル出力
    private boolean mGpxFile = true;		// GPXファイル出力
    private int mGpxLogCount = 0;		    // GPX出力回数
    private long mTotalTime = 0;            //  経過時間(s)
    private double mTotalDistance = 0;	    // 移動距離の合計(累積距離)
    private double mLimitSpeed = 0;		    // 記録制限速度(km/h)
    private double mLimitDistance = 0;	    // 最低移動距離(km)
    private boolean mDataFilter = true;	    // 出力データをフィルターする
    private String mGpxBuffer = "";		    // GPXファイル用バッファ
    private String mErrBuffer = "";		    // エラーログ
    private Location mPreLoc = null;		// 前回位置

    private YLib ylib;
    private Context c = null;


    /**
     *  GPXファイル
     * @param path              出力フォルダ
     * @param gpxfilekeep       都度出力(true)
     */
    public GpxFile(String path, boolean gpxfilekeep) {
        ylib = new YLib();
        this.c = null;

        mFileFolder = path + (path.lastIndexOf('/')==path.length()-1?"":"/");
        mFilePath = "";
        mGpxLogCount = 0;
        mGpxFileKeep = gpxfilekeep;
    }

    /**
     * コンテキストの設定
     * @param c
     */
    public void setContext(Context c) {
        this.c = c;
    }

    /**
     *  GPXファイルの初期化(ファイル名の設定、logカウントと累積距離の初期化)
     * @param filename     GPXファイル名(フォルダ名と拡張子なし)
     */
    public void Init(String filename) {
        mGpxLogCount = 0;
        mFilePath = mFileFolder + filename;
        mTotalDistance = 0;
        mTotalTime = 0;
    }

    /**
     * ノイズ除去のため上限速度の設定
     * @param speed
     */
    public void setLimitSpeed(double speed) {
        mLimitSpeed = speed;
    }

    /**
     * ノイズ除去のため上限距離の設定
     * @param distance
     */
    public void setLimitDistance(double distance) {
        mLimitDistance = distance;
    }

    /**
     * GPXファイル書き込み許可フラグの設定
     * param gpxfile
     */
    public void setGpxFile(boolean gpxfile) {
        this.mGpxFile = gpxfile;
    }

    /**
     * ノイズ除去のためのフィルタ設定フラグ
     * @param dataFilter
     */
    public void setDataFilter(boolean dataFilter) {
        mDataFilter = dataFilter;
    }

    /**
     * GPX記録カウントの設定
     * @param n
     */
    public void setGpxLogCount(int n) {
        this.mGpxLogCount = n;
    }

    /**
     * GPX記録カウントの取得
     * @return
     */
    public int getGpxLogCount() {
        return mGpxLogCount;
    }

    /**
     * ファイルパスの取得
     * @return
     */
    public String getFilePath() {
        return mFilePath + ".gpx";
    }

    /**
     * 累積距離の取得
     * @return  : 距離(km)
     */
    public double getTotalDistance() {
        return mTotalDistance;
    }

    /**
     * 累積距離を設定する
     * @param distance  : 距離(km)
     */
    public void setTotalDistance(double distance) {
        mTotalDistance = distance;
    }

    /**
     * 経過時間の取得
     * @return      経過時間(s)
     */
    public long getTotalTime() {
        return mTotalTime;
    }

    /**
     * 経過時間の設定
     * @param time  経過時間(s)
     */
    public void setTotalTime(long time) {
        mTotalTime = time;
    }

    /**
     * GPXファイルの削除
     */
    public void deleteFile() {
        if (ylib.existsFile(mFilePath + ".gpx")) {
            ylib.deleteFile(mFilePath + ".gpx");
        }
    }

    /**
     * ヘッダデータ作成
     */
    public void startData() {
        // 同じファル名があればヘッダは作成しない
        if (ylib.existsFile(mFilePath + ".gpx"))
            return;
        // GPXヘッダ作成
        mGpxBuffer = makeHeader();
        if (mGpxFileKeep)
            save();
    }

    /**
     * GPXファイルのヘッダデータを作成
     * @return
     */
    private String makeHeader() {
        String buffer;
        // GPXヘッダ作成
        buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        buffer += "<gpx version=\"1.0\" creator=\"GPSinfo - GPS Logger for Android\">";
        buffer += "<trk>";
        buffer += "<trkseg>";
        buffer += "\n";
        return buffer;
    }

    /**
     * GPXファイルの終了データを作成
     * @return
     */
    private String makeTail() {
        String buffer;
        // 終了コード出力
        buffer = "</trkseg>";
        buffer += "</trk>";
        buffer += "</gpx>";
        buffer += "\n";
        return buffer;
    }

    /**
     * 終了データ追加して保存
     * 一度ファイルデータを再読み込みして終了データがなければ終了コードを追加して保存
     */
    public void addTailSave() {
        if (!existDataTail()) {
            String buffer =  makeTail();
            saveAppendData(mFilePath + ".gpx", buffer);
        }
    }

    /**
     * 終了データの有無の確認
     * @return
     */
    public boolean existDataTail() {
        return 0 < findData("</gpx>");
    }

    /**
     * ファイルデータからキーコードを検索
     * @param key       キーコード
     * @return          キーコードの有無
     */
    private int findData(String key) {
        String buffer = readData();
        return buffer.indexOf(key);
    }

    /**
     * ファイルデータ(gpx)の再読み込み
     * @return      ファイルデータ
     */
    private String readData() {
        return ylib.readFileData(mFilePath + ".gpx");
    }

    /**
     * 位置データ追加保存
     *
     * @param location
     */
    public boolean addData(Location location) {
        if (location==null) {
            Log.d(TAG,"addData: location null error!!");
            mErrBuffer += "location null error";
            return false;
        }
//        if (!chkLocationData(location))
//            return false;
        // 位置データ
        mGpxBuffer += "<trkpt lat=\"" + String.valueOf(location.getLatitude())
                + "\" lon=\"" + String.valueOf(location.getLongitude()) + "\">";
        mGpxBuffer += "<ele>" + String.valueOf(location.getAltitude()) + "</ele>";
        // buffer += "<speed>0.555556</speed>";
        mGpxBuffer += "<time>" + ylib.getLocationTime(location) + "</time>";
        mGpxBuffer += "</trkpt>";
        mGpxBuffer += "\n";
        mGpxLogCount++;
        mTotalDistance += ylib.locDistance(location, mPreLoc);
        mTotalTime += ylib.locLapTime(location, mPreLoc) / 1000;
        mPreLoc = new Location(location);
        if (mGpxFileKeep)
            if (!save()) {

            }
        return true;
    }

    /**
     * 異常データチェック
     * 高度、精度が0、
     * @param location
     * @return
     */
    private boolean chkLocationData(Location location) {
        if (0.0f == location.getAltitude() || 0.0f == location.getAccuracy()) {// 高度0の場合、精度0の場合は異常？
            mErrBuffer += ylib.getLocationTime(location) + ", "
                    + "高度:" + ylib.roundStr2(location.getAltitude(),2) + ","
                    + "精度:" + ylib.roundStr2(location.getAccuracy(),2) + "データ異常" + "\n";
            return false;
        }
        // if (0.0f == location.getSpeed()) // || 50 < location.getSpeed()) //
        // 単位m/sec 50m/s → 180km
        // return;
        if (mDataFilter && mPreLoc != null) {
            // 移動距離　0.000001° x 3600 x 25.3219892(m)= 0.09m 東京周辺
            if (0 < mLimitDistance) {    //  km
                double dis =  ylib.locDistance(location, mPreLoc);
                if (mLimitDistance < dis) {
                    // preLoc = new Location(location);
                    mErrBuffer += ylib.getLocationTime(location) + ", "
                            + mLimitDistance + " < " + dis + "km 指定距離以上" + "\n";
                    return false;
                }
            }
            // 設定速度を越えるデータを除外
            if (0 < mLimitSpeed) {       //  km/h
                double speed = ylib.locSpeed(location, mPreLoc);
                if (mLimitSpeed < speed) { // 50km/h
                    // preLoc = new Location(location);
                    mErrBuffer += ylib.getLocationTime(location) + ", "
                            + mLimitSpeed + " < " + speed + "km/h 指定速度以上" + "\n";
                    return false;
                }
            }
        }
        return true;
    }

    public void addLocationData(String latitude,String longitude,String altitude,String time) {
        // 位置データ
        mGpxBuffer += "<trkpt lat=\"" + latitude + "\" lon=\"" + longitude + "\">";
        mGpxBuffer += "<ele>" + altitude + "</ele>";
        // buffer += "<speed>0.555556</speed>";
        mGpxBuffer += "<time>" + time + "</time>";
        mGpxBuffer += "</trkpt>";
        mGpxBuffer += "\n";
        mGpxLogCount++;
        if (mGpxFileKeep)
            save();
    }


    /**
     * エラーデータ追加
     *
     * @param data
     */
    public void addErrorData(String data) {
        mErrBuffer += data + "\n";
        if (mGpxFileKeep)
            save();
    }

    /**
     * 終了処理
     */
    public void close() {
        // 終了コード出力
        mGpxBuffer += makeTail();
        if (mGpxFile) {
            save();
            // Toast.makeText(GpsInfoActivity.this, "ログデータを保存しました。"+logFilePath,
            // Toast.LENGTH_SHORT).show();
        } else {
            delete();
        }

        mFilePath = "";
        mGpxBuffer = "";
        mErrBuffer = "";
        mGpxFile = false;
    }

    /**
     * ファイルに保存する 保存したらバッファをクリアする
     */
    private boolean save() {
        Log.i(TAG, "save: " + mFilePath);
        if (mGpxFile && 0 < mGpxBuffer.length()) {
            saveAppendData(mFilePath + ".gpx", mGpxBuffer);
        }
        mGpxBuffer = "";
        if (0 < mErrBuffer.length()) {
            saveAppendData(mFilePath + ".err", mErrBuffer);
            mErrBuffer = "";
            return false;
        }
        return true;
    }

    /**
     * データを追加してファイルに書き込む
     * @param path      ファイルパス
     * @param buffer    データ
     */
    private void saveAppendData(String path, String buffer) {
        Log.d(TAG, "saveAppendData: "+path+" "+buffer);
        if (ylib.existsFile(path)) {
            ylib.writeFileDataAppend(path, buffer);
        } else {
            ylib.writeFileData(path, buffer);
        }
    }

    /**
     * データファイルとログファイルを削除する
     */
    public void delete() {
        if (ylib.existsFile(mFilePath + ".gpx"))
            ylib.deleteFile(mFilePath + ".gpx");
        if (ylib.existsFile(mFilePath + ".err"))
            ylib.deleteFile(mFilePath + ".err");
    }
}
