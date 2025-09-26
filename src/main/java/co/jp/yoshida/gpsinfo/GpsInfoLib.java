package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GpsInfoLib {

    private static final String TAG = "GpsInfoLib";

    //  prference キーワード
    private static final String LOGFILENAME = "LogFileName";                //  計測中のGPXファイル名
    private static final String GPXSAVECOUNT = "GpxSaveCount";              //  計測回数
    private static final String GPXDATACONTINUE = "GpxDataContinue";        //  計測継続開始フラグ
    private static final String GPXDATAOUTPUTCOUNT = "GpxDataOutputCount";  //  未使用
    private static final String FIRSTTIME = "FirstTime";                    //  初回の時間
    private static final String FIRSTLATITUDE = "FirstLatitude";            //  初回の緯度
    private static final String FIRSTLONGITUDE = "FirstLongitude";          //  初回の経度
    private static final String FIRSTELEVATOR = "FirstElevator";            //  初回の高度
    private static final String CURLATITUDE = "CurrentLatitude";            //  現在の緯度
    private static final String CURLONGITUDE = "CurrentLongitude";          //  現在の高度
    private static final String CURELEVATOR = "CurrentElevator";            //  現在の高度
    private static final String CURSTATUSMSG = "CurStatusMessage";          //  現在の状態メッセージ
    private static final String CURMEMODATE = "CurMemoDate";
    private static final String CURMEMOTIME = "CurMemoTime";
    private static final String TOTALDISTANCE = "TotalDistance";            //  計測中の累積距離(R.stringで対応)
    private static final String STEPCOUNT = "StepCount";                    //  歩数
    private static final String TEXTFOLDER = "TextFolder";                  //  挿入するテキストの最終フォルダ位置

    public static final String FILEHEAD = "GpsMemo";    //  インデックスファイル名のヘッダ

    private int mDigitNo = 4;                           //  小数点以下表示桁数

    private String mDataDirectory;
    private Context mC;
    private YLib ylib;

    public GpsInfoLib(Context c) {
        ylib = new YLib();
        mC = c;
    }

    public GpsInfoLib(Context c, String dataDirectory) {
        ylib = new YLib();
        mC = c;
        mDataDirectory = dataDirectory;
    }

    /**
     * インデックスファイルのインポート
     * @param importPath        インポートするインデックスファイルのパス
     * @param dataFormat        データのフォーマット
     * @param keyData           キーデータの種別(複数)
     * @param indexDirectory    インポートされる側の保存ディレクトリ
     */
    public void importIndexData(String importPath, String[] dataFormat, String[] keyData, String indexDirectory) {
//        String date = ylib.getNameWithoutExt(importPath.replace(DataListActivity.FILEHEAD + "_", ""));
        String date = "date dummy";
        String indexFileName = getLoadIndexFileName(importPath);
        ListData listData = new ListData(mC, dataFormat);
        listData.setKeyData(keyData);
        listData.setSaveDirectory(indexDirectory, getLoadIndexFileName(date));
        listData.LoadFile();
        if (listData.LoadFile(importPath, true))
            listData.saveDataFile();
    }

    /**
     * データファイルをインポートする
     * @param importDataDir     インポートするディレクトリ
     * @param date              対象年
     * @param dataFormat        データのフォーマット
     * @param keyData           キーデータの種別
     * @param indexDirectory    インデックファイルのディレクトリ
     * @param dataDirectory     保存先のデータディレクトリ
     */
    public void importDataFile(String importDataDir, String date, String[] dataFormat,
                               String[] keyData, String indexDirectory, String dataDirectory){
        //  対象年のインデックスファイルの読込
        ListData listData = new ListData(mC, dataFormat);
        listData.setKeyData(keyData);
        listData.setSaveDirectory(indexDirectory, getLoadIndexFileName(date));
        listData.LoadFile();
        //  インデックスファイルからGPXファイルのリストを取得
        List<String> gpxFileList = listData.getListData("GPX");
        //  インポートするディレクトリからGPXファイルを検索
        for (String gpxFile : gpxFileList) {
            String gpxPath = getGpxPath(gpxFile);
            //  保存先に対象ファイルがなければインポート先からコピーする
            if (gpxPath == null) {
                String srcPath = getGpxPath(gpxFile, importDataDir);
                if (srcPath != null) {
                    String key =listData.findData(gpxFile, "GPX");
                    String category = listData.getData(key, "分類");
                    copyGpxData(srcPath, date.substring(0,4), category, dataDirectory);
                }
            }
        }
    }

    /**
     * GPXファイルを年と分類でフォルダに分けて移動する
     * @param fpath             GPXファイルパス
     * @param year              対象年
     * @param category          分類名
     * @param dataDirectory     緯度先フォルダー
     * @return                  移動の可否
     */
    public boolean moveGpxData(String fpath, String year, String category, String dataDirectory) {
        String destPath = dataDirectory + "/" + year;
        if (!ylib.mkdir(destPath))
            return false;
        destPath += "/" + category;
        if (!ylib.mkdir(destPath))
            return false;
        return ylib.moveFile(fpath, destPath);
    }

    /**
     * GPXファイルを年と分類でフォルダに分けてコピーする
     * @param fpath             GPXファイルパス
     * @param year              対象年
     * @param category          分類名
     * @param dataDirectory     緯度先フォルダー
     * @return                  コピーの可否
     */
    public boolean copyGpxData(String fpath, String year, String category, String dataDirectory) {
        String destPath = dataDirectory + "/" + year;
        if (!ylib.mkdir(destPath))
            return false;
        destPath += "/" + category;
        if (!ylib.mkdir(destPath))
            return false;
        return ylib.copyFile(fpath, destPath);
    }

    /**
     * データか登録されているかを確認する
     * @param data              GPXデータ
     * @param dataFormat        登録データのフォーマット(分類リスト)
     * @param keyData           キーデータの分類名
     * @param indexDirectory    インデックスファイルのフォルダ
     * @return                  登録の可否
     */
    public boolean isRegistData(String[] data, String[] dataFormat, String[] keyData, String indexDirectory) {
        String indexFileName = getLoadIndexFileName(data[0]);
        ListData listData = new ListData(mC, dataFormat);
        listData.setKeyData(keyData);
        listData.setSaveDirectory(indexDirectory, indexFileName);
        listData.LoadFile();
        return listData.isContainKey(data);
    }

    /**
     * GPXデータをインテックスファイルに登録する
     * @param data              GPXデータ
     * @param dataFormat        登録データのフォーマット(分類リスト)
     * @param keyData           キーデータの分類名
     * @param indexDirectory    インデックスファイルのフォルダ
     */
    public void registIndexFile(String[] data, String[] dataFormat, String[] keyData, String indexDirectory) {
        Log.d(TAG,"registIndexFile: "+data[0]+" "+data[1]);
        String indexFileName = getLoadIndexFileName(data[0]);
        ListData listData = new ListData(mC, dataFormat);
        listData.setKeyData(keyData);
        listData.setSaveDirectory(indexDirectory, indexFileName);
        listData.LoadFile();
        listData.setData(data, false);
        listData.saveDataFile();
    }

    /**
     * GPXデータでインテックスファイルに更新する
     * 既存データがない時は新規追加する
     * @param key               keyデータ
     * @param data              GPXデータ
     * @param dataFormat        登録データのフォーマット(分類リスト)
     * @param keyData           キーデータの分類名
     * @param indexDirectory    インデックスファイルのフォルダ
     */
    public void updateIndexFile(String key, String[] data, String[] dataFormat, String[] keyData, String indexDirectory) {
        String indexFileName = getLoadIndexFileName(data[0]);
        ListData listData = new ListData(mC, dataFormat);
        listData.setKeyData(keyData);
        listData.setSaveDirectory(indexDirectory, indexFileName);
        listData.LoadFile();
        if (key==null || key.length() < 1) {
            //  keyデータがない場合はdata[]からkeyデータを求めてupdateする
            if (listData.updateData(data))
                listData.saveDataFile();
        } else {
            if (listData.updateData(key, data))
                listData.saveDataFile();
        }
    }

    /**
     * インデックスファイルの中からデータを検索する
     * @param date              対象年(日付)
     * @param searchData        検索データ
     * @param title             対象分類タイトル
     * @param dataFormat        データフォーマット
     * @param indexDirectory    ファイルの保存ディレクトリ
     * @return                  keyデータ(ない時は空白)
     */
    public String findIndexFile(String date, String searchData, String title, String[] dataFormat, String[] keyData, String indexDirectory) {
        String indexFileName = getLoadIndexFileName(date);
        if (!ylib.existsFile(indexFileName))
            return "";
        ListData listData = new ListData(mC, dataFormat);
        listData.setKeyData(keyData);
        listData.setSaveDirectory(indexDirectory, indexFileName);
        listData.LoadFile();
        return listData.findData(searchData, title);
    }

    /**
     * gpxファイルデータから登録データを抽出する
     * (タイトルはGPXファイル名,メモは初期化される)
     * @param filePath      gpxファイルパス
     * @param dataFormat    データフォーマット
     * @return              gpxからの抽出データ
     */
    public String[] getGpxData(String filePath, String[] dataFormat) {
        return getGpxData(filePath, dataFormat, true);
    }

    /**
     * gpxファイルデータから登録データを抽出する
     * (タイトルはGPXファイル名,メモは初期化される)
     * @param filePath      gpxファイルパス
     * @param dataFormat    データフォーマット
     * @param step          歩数カウントの有無
     * @return              gpxからの抽出データ
     */
    public String[] getGpxData(String filePath, String[] dataFormat, Boolean step) {
        //  GPXのデータからインデックスデータを作成
        String[] gpxData = new String[dataFormat.length];
        gpxData = updateGpxData(filePath, gpxData, dataFormat);
        if (gpxData !=null){
            gpxData[getTitlePos("歩数", dataFormat)] = String.valueOf(step ? getStepCount() : 0);        //  歩数
            gpxData[getTitlePos("GPX", dataFormat)] = ylib.getNameWithoutExt(filePath);       //  GPXファイル名
            gpxData[getTitlePos("タイトル", dataFormat)] = ylib.getNameWithoutExt(filePath);   //  タイトル
            gpxData[getTitlePos("メモ", dataFormat)] = "";                                     //  メモ
        }

        return gpxData;
    }

    /**
     * GPXデータを取り込んでデータを更新する(年月日、時間、緯度、経度、高度、移動距離、移動時間、最大最小高度、分類、GPX)
     * @param filePath      GPXファイルパス
     * @param gpxData       更新前のGPXデータ
     * @param dataFormat    データフォーマット
     * @return              更新後のGPXデータ
     */
    public String[] updateGpxData(String filePath, String[] gpxData, String[] dataFormat) {
        GpsDataReader gpsDataReader = getGpsData(filePath);
        if (gpsDataReader == null)
            return null;
        GpsData firstData = gpsDataReader.getData(0);                               //  1回目の位置データ
        GpsData lastData = gpsDataReader.getData(gpsDataReader.getDataSize() - 1);  //  最終の位置データ
        gpxData[getTitlePos("年月日", dataFormat)] = firstData.getDate();                              //  年月日
        gpxData[getTitlePos("時間", dataFormat)] = firstData.getTime();                                //  時間
        gpxData[getTitlePos("緯度", dataFormat)] = ylib.roundStr(firstData.getLatitude(),mDigitNo);    //  緯度
        gpxData[getTitlePos("経度", dataFormat)] = ylib.roundStr(firstData.getLongitude(),mDigitNo);   //  経度
        gpxData[getTitlePos("高度", dataFormat)] = ylib.roundStr(firstData.getElevator(),mDigitNo);    //  高度
        gpxData[getTitlePos("移動距離", dataFormat)] = ylib.roundStr(lastData.getDisCovered(),mDigitNo);  //  移動距離(累積距離)(km)
        gpxData[getTitlePos("移動時間", dataFormat)] = ylib.Sec2Time(lastData.getLap());        //  移動時間
        gpxData[getTitlePos("最大高度", dataFormat)] = ylib.roundStr(gpsDataReader.getMaxElevator(),mDigitNo);    //  最大高度
        gpxData[getTitlePos("最小高度", dataFormat)] = ylib.roundStr(gpsDataReader.getMinElevator(),mDigitNo);    //  最小高度
        gpxData[getTitlePos("分類", dataFormat)] = getCaetgoryChk(                        //  分類判定
                gpsDataReader.getLapTime(gpsDataReader.getDataSize() - 1),              //  移動時間
                gpsDataReader.getDisCovered(gpsDataReader.getDataSize() - 1),           //  移動距離
                gpsDataReader.getMaxElevator(),gpsDataReader.getMinElevator());               //  標高差
        gpxData[getTitlePos("GPX", dataFormat)] = ylib.getNameWithoutExt(filePath);               //  GPXファイル名
        return gpxData;
    }


    /**
     * 一覧データのタイトル位置の検索・取得
     * @param title タイトル名
     * @return タイトル位置
     */
    public int getTitlePos(String title, String[] dataFormat) {
        if (title == null || dataFormat == null)
            return -1;
        for (int i = 0; i < dataFormat.length; i++) {
            if (dataFormat[i].compareTo(title) == 0)
                return i;
        }
        return -1;
    }


    /**
     * GPXデータの取り込み
     * @param filePath      GPXファイルパス
     * @return              GPXデータ(GpsDataReader)
     */
    public GpsDataReader getGpsData(String filePath) {
        //	GPXデータの取り込み
        GpsDataReader gpsDataReader = new GpsDataReader(mC);
        try {
            if (!gpsDataReader.xmlFileRaeder(filePath)) {
                Toast.makeText(mC, filePath + "GPXデータが読み込めません", Toast.LENGTH_SHORT).show();
                return null;
            }
            if (gpsDataReader.getDataSize() < 1) {
                Toast.makeText(mC, filePath + "データがありません", Toast.LENGTH_SHORT).show();
                return null;
            }
        } catch (Exception e) {
            Toast.makeText(mC, e.getMessage(),  Toast.LENGTH_LONG).show();
            return null;
        }
        return gpsDataReader;
    }


    /***
     * データ(速度と標高差)から分類する
     * @param lap           経過時間(s)
     * @param distance      距離(km)
     * @param maxElevator   最大高度(m)
     * @param minElevator   最小高度(m)
     * @return              分類
     */
    public String getCaetgoryChk(long lap, double distance, double maxElevator, double minElevator) {
        String category = "その他";                        //  分類
        double speed = distance / ((double)lap / 3600D);    //  速度 km/h
        double heightDis = maxElevator - minElevator;       //  標高差 m
        double stepDis = 0 < getStepCount() ? distance * 1000D / (double)getStepCount() : -1D;  //  歩幅 m
        if (stepDis < 10D) {                 //  歩幅 10m以下
            if (speed < 6D) {                //  速度6km/h以下
                if (heightDis < 300D)        //  標高差 300m以下
                    category = "散歩";
                else
                    category = "山歩き";
            } else if (speed < 12D) {        //  速度 6-12のもめく
                category = "ジョギング";
            } else if (speed < 30D) {        //  速度 12-30km/h
                category = "ランニング";
            } else {
                category = "自転車";
            }
        } else {
            if (speed < 40D) {        //  速度 12-30km/h
                category = "自転車";
            } else {
                category = "車";
            }
        }
        return category;
    }

    /**
     * 現在時刻の取得
     * @return      現在時刻(HH:mm)
     */
    public String getNowTime() {
        Date Now = new Date();
        long curTime = Now.getTime();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm" );
        return df.format(curTime) + " ";
    }

    /**
     * GPXファイルからGPXデータを取得する
     * @param path      GPXファイルパス
     * @return          GPXデータ
     */
    public GpsDataReader getGpxData(String path) {
        GpsDataReader gpsDataReader = new GpsDataReader(mC);
        if (ylib.existsFile(path)) {
            //	GPXデータの取り込み
            try {
                gpsDataReader.xmlFileRaeder(path);
            } catch (XmlPullParserException e) {
                Toast.makeText(mC, e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        } else {
            Toast.makeText(mC, path + " ファイルが存在しません", Toast.LENGTH_SHORT).show();
            return null;
        }
        return gpsDataReader;
    }

    /**
     * GPXファイル名(拡張子なし)からフルパスを検索する
     * 同一ファイル名が複数存在する場合には最初に検索されたファイルのフルパスを返す
     * @param fileName      GPXファイル名(拡張子なし)
     * @return              検索して最初に見つかったパス(ない時はnull)
     */
    public String getGpxPath(String fileName) {
        return getGpxPath(fileName, mDataDirectory);
    }

    /**
     * GPXファイル名(拡張子なし)からフルパスを検索する
     * 同一ファイル名が複数存在する場合には最初に検索されたファイルのフルパスを返す
     * @param fileName          GPXファイル名(拡張子なし)
     * @param dataDirectory     検索するディレクトリ(再帰検索)
     * @return                  検索して最初に見つかったパス(ない時はnull)
     */
    public String getGpxPath(String fileName, String dataDirectory) {
        List<String> path = ylib.getFileList(dataDirectory, fileName+".gpx", true);
        if (0 < path.size())
            return path.get(0);
        else
            return null;
    }

    /**
     * ファイル名(GPSMEMO_20xx.csv)から年数を取り出す
     * @param fileName      ファイル名
     * @return              年数
     */
    public String getYear(String fileName) {
        String date = ylib.getNameWithoutExt(fileName.replace(FILEHEAD + "_", ""));
        if (date.length() < 4)
            return "";
        else
            return date.substring(0, 4);
    }

    /**
     *  リストデータ(****.csv)のファイルリストの取得
     *  リストデータファイル(****.csv)を検索してリスト化
     * @param indexDirectory    GPXファイル名(拡張子なし)
     * @return                  ファイルリスト
     */
    public List<String> getFileList(String indexDirectory) {
        List<String> fileList = new ArrayList<String>();
        //	ファイルリストの取得
        if (ylib.getRegexFileList(indexDirectory, "^"+FILEHEAD+"_20[0-9][0-9].csv", fileList)) {
            Collections.sort(fileList);
            return fileList;
        }
        return null;
    }

    /**
     * 	日付(年)をファイル名に変換する
     * @param date  	yyyymmdd
     * @return
     */
    public String getLoadIndexFileName(String date) {
        return DataListActivity.FILEHEAD + "_" + date.substring(0,4);
    }


    /**
     * 現在地の緯度を保存
     * @param latitude      緯度
     */
    public void setCurLatitude(float latitude) {
        ylib.setFloatPreferences(latitude, CURLATITUDE, mC);
    }

    /**
     * 現在地の緯度を取得
     * @return      緯度
     */
    public float getCurLatitude() {
        return ylib.getFloatPreferences(CURLATITUDE, mC);
    }

    /**
     * 現在地の経度を保存
     * @param longitude     経度
     */
    public void setCurLongitude(float longitude) {
        ylib.setFloatPreferences(longitude, CURLONGITUDE, mC);
    }

    /**
     * 現在地の経度を取得
     * @return      経度
     */
    public float getCurLongitude() {
        return ylib.getFloatPreferences(CURLONGITUDE, mC);
    }

    /**
     * 現在地の標高を保存
     * @param elevator      標高
     */
    public void setCurElevator(float elevator) {
        ylib.setFloatPreferences(elevator, CURELEVATOR, mC);
    }

    /**
     * 現在地の標高を取得
     * @return      標高
     */
    public float getCurElevator() {
        return ylib.getFloatPreferences(CURELEVATOR, mC);
    }

    /**
     * 現在地の状態メッセージを保存
     * @param msg      状態メッセージ
     */
    public void setCurStatusMsg(String msg) {
        ylib.setStrPreferences(msg, CURSTATUSMSG, mC);
    }

    /**
     * 現在地の状態メッセージを取得
     * @return      状態メッセージ
     */
    public String getCurStatusMsg() {
        return ylib.getStrPreferences(CURSTATUSMSG, mC);
    }

    /**
     * GPXデータの継続フラグ取得
     * @return      継続フラグ
     */
    public Boolean getGpxDataContinue() {
        return ylib.getBoolPreferences(GPXDATACONTINUE, mC);
    }

    /**
     * GPXデータの継続フラグ設定
     * @param cont      継続フラグ
     */
    public void setGpxDataContinue(boolean cont) {
        ylib.setBoolPreferences(cont, GPXDATACONTINUE, mC);
    }

    /**
     * preferencesにデータの継続を設定
     * @param cont
     */
    public void setGpxDataContinue(Boolean cont) {
        ylib.setBoolPreferences(cont, GPXDATACONTINUE, mC);
    }

    /**
     * Prefernceにデータ取得回数を保存する
     * @param n
     */
    public void setGpxSaveCount(int n) {
        ylib.setIntPreferences(n, GPXSAVECOUNT, mC);
    }

    /**
     * Prefernceからデータ取得回数を取得する
     * @return
     */
    public int getGpxSaveCount() {
        return ylib.getIntPreferences(GPXSAVECOUNT, mC);
    }

    /**
     * GPXデータカウントのプリファレンスの値をクリアする
     */
    public void clearGpxSaveCount() {
        ylib.setIntPreferences(0, GPXSAVECOUNT, mC);
    }

    /**
     * テキストデータファイルのディレクトリの取得
     * @return          ディレクトリ
     */
    public String getTextFileDirectory() {
        return ylib.getStrPreferences(TEXTFOLDER, mC);
    }

    /**
     * テキストデータファイルのディレクトリを保存
     * @param dir       ディレクトリ
     */
    public void setTextFileDirectory(String dir) {
        ylib.setStrPreferences(dir, TEXTFOLDER, mC);
    }

    /**
     * preferenceからGPSデータファィル名を取得する
     * @return
     */
    public String getDataFileName() {
        return ylib.getStrPreferences(LOGFILENAME, mC);
    }

    /**
     * データ保存ファイル名を作成しpreferenceに保存
     */
    public void setLogFileName() {
        ylib.setStrPreferences(ylib.makeFileName("", "_GPS"), LOGFILENAME, mC);
    }

    /**
     * GPXデータ名のプリファレンスの値をクリアする
     */
    public void clearDataFileName() {
        ylib.setStrPreferences("",LOGFILENAME, mC);
    }


    /**
     * Preferenceに初期時間を保存する
     * @param n
     */
    public void setFirstTime(long n) {
        ylib.setLongPreferences(n, FIRSTTIME, mC);
    }

    /**
     * Preferenceから初期時間を取得する(0は未取得)
     * @return      UTC time(long)
     */
    public long getFirstTime() {
        return ylib.getLongPreferences(FIRSTTIME, mC);
    }

    /**
     * Prefernceに初期値緯度を保存する
     * @param n
     */
    public void setFirstLatitude(float n) {
        ylib.setFloatPreferences(n, FIRSTLATITUDE, mC);
    }

    /**
     * Preferenceから初期値緯度を取得する
     * @return
     */
    public float getFirstLatitude() {
        return ylib.getFloatPreferences(FIRSTLATITUDE, mC);
    }

    /**
     * Prefernceに初期値経度を保存する
     * @param n
     */
    public void setFirstLongitude(float n) {
        ylib.setFloatPreferences(n, FIRSTLONGITUDE, mC);
    }

    /**
     * Preferenceから初期値経度を取得する
     * @return
     */
    public float getFirstLongitude() {
        return ylib.getFloatPreferences(FIRSTLONGITUDE, mC);
    }

    /**
     * Prefernceに初期値緯度を高度する
     * @param n
     */
    public void setFirstElevator(float n) {
        ylib.setFloatPreferences(n, FIRSTELEVATOR, mC);
    }

    /**
     * Preferenceから初期値高度を取得する
     * @return
     */
    public float getFirstElevator() {
        return ylib.getFloatPreferences(FIRSTELEVATOR, mC);
    }

    /**
     * Preferenceに最大高度を書き込む
     * @param n
     */
    public void setMaxElevator(float n) {
        ylib.setFloatPreferences(n, mC.getString(R.string.key_MaxElevator), mC);
    }

    /**
     * Preferenceから最大高度むを取得する
     * @return
     */
    public float getMaxElevator() {
        return ylib.getFloatPreferences(mC.getString(R.string.key_MaxElevator), mC);
    }

    /**
     * 最大高度値を更新する
     * @param n
     */
    public void updateMaxElevator(float n) {
        if (n > getMaxElevator())
            setMaxElevator(n);
    }

    /**
     * Preferenceに最小高度を書き込む
     * @param n
     */
    public void setMinElevator(float n) {
        ylib.setFloatPreferences(n, mC.getString(R.string.key_MinElevator), mC);
    }

    /**
     * Preferenceから最小高度を取得する
     * @return
     */
    public float getMinElevator() {
        return ylib.getFloatPreferences(mC.getString(R.string.key_MinElevator), mC);
    }

    /**
     * 最小高度値を更新する
     * @param n
     */
    public void updateMinElevator(float n) {
        if (n < getMinElevator())
            setMinElevator(n);
    }
    /**
     * Prefernceに前回値緯度を保存する
     * @param n
     */
    public void setPreLatitude(float n) {
        ylib.setFloatPreferences(n, mC.getString(R.string.key_PreLatitude), mC);
    }

    /**
     * Preferenceから前回値緯度を取得する
     * @return
     */
    public float getPreLatitude() {
        return ylib.getFloatPreferences(mC.getString(R.string.key_PreLatitude), mC);
    }

    /**
     * Prefernceに前回値経度を保存する
     * @param n
     */
    public void setPreLongitude(float n) {
        ylib.setFloatPreferences(n, mC.getString(R.string.key_PreLongitude), mC);
    }

    /**
     * Preferenceから前回値経度を取得する
     * @return
     */
    public float getPreLongitude() {
        return ylib.getFloatPreferences(mC.getString(R.string.key_PreLongitude), mC);
    }

    /**
     * Prefernceに累積距離を保存する
     * @param n
     */
    public void setTotalDistance(float n) {
        ylib.setFloatPreferences(n, mC.getString(R.string.key_TotalDistance), mC);
    }

    /**
     * Preferenceから累積距離を取得する
     * @return
     */
    public float getTotalDistance() {
        return ylib.getFloatPreferences(mC.getString(R.string.key_TotalDistance), mC);
    }

    /**
     * Prefernceに経過時間を保存する
     * @param n
     */
    public void setTotalTime(long n) {
        ylib.setLongPreferences(n, "TotalTime", mC);
    }

    /**
     * Preferenceから経過時間を取得する
     * @return
     */
    public long getTotalTime() {
        return ylib.getLongPreferences("TotalTime", mC);
    }

    /**
     * GPS最小時間設定値の取得
     * @return		:	m秒
     */
    public long GetPreferencesGPSminTime() {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(mC);
        return Integer.valueOf(prefs.getString(mC.getString(R.string.key_GPSminTime), "0"))*1000;	//最低経過時間(ms)
    }

    /**
     * GPS最小距離設定値の取得
     * @return      : m
     */
    public float GetPreferencesGPSminDistance() {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(mC);
        return Float.valueOf(prefs.getString(mC.getString(R.string.key_GPSminDistance), "0"));//最低移動距離(m)
    }

    /**
     * バイブレータの設定を取得する
     * @return
     */
    public boolean getPreferencesVibrator() {
        return ylib.getBoolPreferences(mC.getString(R.string.key_vibrator), mC);
    }

    /**
     * バイブレータの設定を書き込む
     * @param b
     */
    public void setPreferencesVibrator(boolean b) {
        ylib.setBoolPreferences(b, mC.getString(R.string.key_vibrator), mC);
    }

    /**
     * ビープ音の設定を取得する
     * @return
     */
    public boolean getPreferencesBeep() {
        return ylib.getBoolPreferences(mC.getString(R.string.key_beep), mC);
    }

    /**
     * ビープ音の設定を書き込む
     * @param b
     */
    public void setPreferencesBeep(boolean b) {
        ylib.setBoolPreferences(b, mC.getString(R.string.key_beep), mC);
    }

    /**
     * 歩数を保存
     * @param n
     */
    public void setStepCount(int n) {
        ylib.setIntPreferences(n, mC.getString(R.string.key_StepCount),mC);
    }

    /**
     * 歩数を取得
     * @return
     */
    public int getStepCount() {
        return ylib.getIntPreferences(mC.getString(R.string.key_StepCount),mC);
    }

}
