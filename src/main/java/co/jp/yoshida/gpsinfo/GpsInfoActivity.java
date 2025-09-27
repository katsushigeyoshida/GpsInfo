package co.jp.yoshida.gpsinfo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GpsInfoActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener,
                    LocationListener, SensorEventListener {

    private static final String TAG = "GpsInfoActivity";

    private TextView mTvProvider;               //  プロバイダ
    private TextView mTvGpsTime;                //  取得時刻
    private TextView mTvCurTime;                //  現在時刻
    private TextView mTvLatitude;               //  緯度
    private TextView mTvLongitude;              //  経度
    private TextView mTvElevator;               //  高度
    private TextView mTvTargetTitle;            //  目的地名
    private TextView mTvTargetLatitude;         //  目的地緯度
    private TextView mTvTargetLongitude;        //  目的地経度
    private TextView mTvGpsStatusMsg;           //  GPSロガーの状態メッセージ

    private Button mBtTagetLocation;            //  目的地
    private Button mBtEditDest;                 //  目的地編集
    private Button mBtTraking;                  //  記録開始/終了
    private Button mBtTrakingList;              //  記録リスト
    private Button mBtTrakingMemo;              //  記録メモ

    private LinearLayout mLinearLayout;         //  グラフィック表示レイアウト
    private GpsGraphDraw mGpsGraphDraw;
    public static final int LOCATIONEDIT = 1000;        //  目的地編集Activity
    public static final int LOCMEMO_ACTIVITY = 1011;    //  データ編集Activity

    private final int MENU00 = 0;
    private final int MENU01 = 1;
    private final int MENU02 = 2;
    private final int MENU03 = 3;
    private final int MENU04 = 4;
    private final int MENU05 = 5;

    ColorStateList mTrakButtonTextColors;       //  記録開始ボタン色

    private String mDataDirectory;              //  データ保存ディレクトリ
    private String mIndexDirectory;             //  インデックスデータを保存するディレクトリ

    protected static final String mTargetFileName =      //  目的地データ保存ファイル名
            "GPSinfoLocationData.csv";
    private String mTargetFilePath;             //  目的地データファイルのパス
    protected static final String[] mTargetLocationFormat = {    //  目的地データのタイトル
            "目的地","緯度","経度","高度","グループ","メモ"};
    private List<String[]> mTargetLocationList =    //  目的地リスト
        new ArrayList<String[]>() {
            {
                add(new String[]{"皇居","35.6825","139.752778","","東京",""});
                add(new String[]{"東京タワー","35.65862","139.74539","","東京",""});
                add(new String[]{"東京スカイツリー","35.7101389","139.8108333","","東京",""});
                add(new String[]{"富士山","35.3575","138.7306","3776","百名山",""});
                add(new String[]{"白馬岳","36.7556","137.7617","2932","百名山",""});
                add(new String[]{"北岳","35.6714","138.2419","3192","百名山",""});
            }
        };
    private String[] mTargetLocationTitle;      //  表示用
    private String[] mTargetGroupList;          //  目的地グループリスト

    private final String mSaveGpxFile = "GPSinfo";  //  GPXデータ保存ファイル名ヘッダ(ヘッダ+日付+時間)
    protected static String mIndexFileHead = "GpsMemo";    //  インデックスファイルのヘッダ(GpsMemo_2019.csv)
    private long mGPSminTime = 5 * 1000;        //	GPS最低経過時間(m秒)
    private float mGPSminDistance = 0;          //	GPS最低移動距離(m)
    private float mGPSBarmaxHeight = 500;       //  グラフィックの高さ表示上限
    private float mGPSBarminHeight = 0;         //  グラフィックの高さ表示下限
    private float mGPSBarHeightPitch = 500;     //  グラフィックの高さの変動ピッチ
    private boolean mSunDisp = false;           //	太陽の向き表示
    private float mDeclination = 7.333f;        //	方位の偏角 7度20分(東京)
    private boolean mAzimuthFix = false;        //  方位固定

    private int mDigitNo = 2;                   // 小数点以下表示桁数
    private int mDigitNoCoordinate = 4;         // 小数点以下表示桁数(現在位置)

    //  GPS計測回数
    int mCount = 0;
    //  目的地の緯度経度(初期値は自宅)
    private String mTargetGroup = "東京";
    private String mTargetTitle = "東京タワー";
    private double mTargetLatitude = 35.65862;
    private double mTargetLongitude = 139.74539;
    //	現在位置サンプル(初期値は武蔵小杉)
    private double mCurrLatitude = 35.57658;         //	緯度
    private double mCurrLongitude = 139.65967;       //	経度
    private double mCurrElevator = 0;
    //  センサー測定値バッファ
    private float[] mAccelerometerValue = new float[3]; // 加速度センサの値
    private float[] mMagneticFieldValue = new float[3]; // 磁気センサの値

    //	リスト表示データ(ファイルから取り込んだデータ)
    private String mCurGroup = "";                      //	表示グループ

    private boolean mNetworkProvider = false;           //	ネットワークからの位置取得

    //  センサーマネージャー
    private LocationManager mLocationManager;   //  位置センサー
    private SensorManager mSensorManager;       //  センサマネージャー
    private Sensor mAccelerometer;              //  加速度センサ
    private Sensor mMagneticField;              //  磁気センサ
    private Sensor mOrientation;                //  方位センサ

    private GpsInfoLib gilib;
    private YLib ylib;

    /**
     * 初期化
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gps_info);

        ylib = new YLib(this);

        //  データファイルのベースディレクトリ(/strage/emulated/0/DCIM/gpsinfo)
        File extStrageDir = Environment.getExternalStorageDirectory();
        mDataDirectory = extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DCIM +
                "/" + ylib.getPackageNameWithoutExt() + "/";
        chkFileAccessPermission(mDataDirectory);        //  ファイルアクセスのパーミッションチェック
//        mDataDirectory = ylib.getPackageNameDirectory();
        if (!ylib.mkdir(mDataDirectory)) {
            Toast.makeText(this, mDataDirectory + " が作成できません", Toast.LENGTH_SHORT).show();
        }
        //  インデックスファイルのディレクトリ作成
        mIndexDirectory = mDataDirectory + "/Memo";
        if (!ylib.mkdir(mIndexDirectory))
            mIndexDirectory = mDataDirectory;

        ylib.setStrPreferences(mDataDirectory, "SAVEDIRECTORY", this);
        gilib = new GpsInfoLib(this, mDataDirectory);
        gilib.setCurStatusMsg("");
        getPreference();                        //  前回値の取得
        initScreen();                           //  画面の初期化
        setButtonStat();                        //  ボタンの状態を設定する

        //	位置情報
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        chkGpsService();
        requestLocationUpdates(mGPSminTime, mGPSminDistance);

        // センサーマネジャーの取得
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //	方位センサーの取得
        mOrientation = getSensor(mOrientation, Sensor.TYPE_ORIENTATION);
        mAccelerometer = getSensor(mAccelerometer, Sensor.TYPE_ACCELEROMETER);
        mMagneticField = getSensor(mMagneticField, Sensor.TYPE_MAGNETIC_FIELD);

        //  目的地データの取り込み
        loadTargetLocData();

        //	GPXデータ記録準備
        setServiceRunningButton(isServiceRunning(this, GpsOnService.class));
        setAppTitle();

        displayStatus();
    }

    /**
     * 終了処理
     */
    @Override
    protected void onDestroy() {
        mLocationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == mBtTagetLocation.getId()) {
            //  目的地
            setTargetLocationDialog();
        } else if (view.getId() == mBtEditDest.getId()) {
            //  目的地編集
            goTagetLocationEdit("","");
        } else if (view.getId() == mBtTraking.getId()) {
            //  記録開始/終了
            Button button = (Button) view;
            if (button.getText().toString().compareTo("記録開始") == 0) {
                if (0 < gilib.getDataFileName().length() && gilib.getGpxDataContinue()) {
                    if (existDataFile(gilib.getDataFileName())) {
                        gpxDataContinuerDialog();               //  新規と継続記録の選択ダイヤログ表示
                    } else {
                        gpxDataStartDialog();                   //  新規記録
                    }
                } else {
                    gpxDataStartDialog();                       //  新規記録
                }
            } else {
                gpxDataPouseDialog();                           //  終了/中断のダイヤログ表示
            }
        } else if (view.getId() == mBtTrakingList.getId()) {
            //  記録一覧
            goDataList();                     //	位置メモ一覧表示
        } else if (view.getId() == mBtTrakingMemo.getId()) {
            //  記録メモ
            openLocMemo();
        } else {
            int[] locations = new int[2];
            view.getLocationOnScreen(locations);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == mBtTagetLocation.getId()) {
            //  目的地グループリスト
            setGroupDialog();
        } else if (view.getId() == mLinearLayout.getId()) {
            //  現在位置を登録
            goTagetLocationEdit(ylib.roundStr(mCurrLatitude, 6),ylib.roundStr(mCurrLongitude, 6));
        } else {
            int[] locations = new int[2];
            view.getLocationOnScreen(locations);
//            SetDrawMenu();
        }
        return true;
//        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item4 = menu.add(Menu.NONE, MENU04, Menu.NONE, "現在地の地図を開く");
        MenuItem item3 = menu.add(Menu.NONE, MENU03, Menu.NONE, "目的地の地図を開く");
        MenuItem item2 = menu.add(Menu.NONE, MENU02, Menu.NONE, "インポート:目的地データ");
        MenuItem item1 = menu.add(Menu.NONE, MENU01, Menu.NONE, "設定");
        MenuItem item5 = menu.add(Menu.NONE, MENU05, Menu.NONE, "ストレージ・パーミッション");
        MenuItem item0 = menu.add(Menu.NONE, MENU00, Menu.NONE, "ヘルプ");
        item1.setIcon(android.R.drawable.ic_menu_edit);
        item0.setIcon(android.R.drawable.ic_menu_help);
       return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case MENU00:                //	ヘルプ
                break;
            case MENU01 :               //  設定
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case MENU02 :               //  目的地データのインポート
                ylib.fileSelectDialog(mDataDirectory,"*.csv", true, iImportLocationData);
                break;
            case MENU03 :               //  目的地の地図を開く
                openMaps(String.valueOf(mTargetLatitude), String.valueOf(mTargetLongitude));
                break;
            case MENU04 :               //  現在地の地図を開く
                openMaps(String.valueOf(mCurrLatitude), String.valueOf(mCurrLongitude));
                break;
            case MENU05:                //  ストレージ・パーミッション
                goFileAccessPermision();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GpsInfoActivity.LOCATIONEDIT) {  //	データ登録画面からの戻り
            loadTargetLocData();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //	加速度センサー
            this.mAccelerometerValue = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //	磁気センサー
            this.mMagneticFieldValue = event.values.clone();
        }
        // 方位を出すための変換行列
        float[] rotate = new float[16];                         // 傾斜行列？
        float[] inclination = new float[16];                    // 回転行列
        //	回転行列の取得し方位(Z軸周り)、仰角(X軸周り)、ロール角(Y軸周り)を求める
        SensorManager.getRotationMatrix(
                rotate, inclination,
                this.mAccelerometerValue,
                this.mMagneticFieldValue);
        float[] orientation = new float[3];
        this.getOrientation(rotate, orientation);               // 方向を求める
        float azimuth = (float) Math.toDegrees(orientation[0]); // デグリー角に変換する
        azimuth = (azimuth - mDeclination) % 360f;              //	方位角 - 偏角
        //Log.i("onSensorChanged", "角度:" + degreeDir);
        float pitch = mAccelerometerValue[1] * -90f / 9.81f;    //	前後角の加速度を角度に変換
        float roll = mAccelerometerValue[0] * 90f / 9.81f;      //	左右角の加速度を角度に変換
        displayAzimuth(azimuth, pitch, roll);                   //  方位と傾きを表示
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "onLocationChanged:");
        displayLocation(location);
        saveCurrentLocation();
        displayStatus();
        setButtonStat();
        requestLocationUpdates(mGPSminTime, mGPSminDistance);
    }

    /**
     * 目的地データを取り込む関数インターフェース
     */
    Consumer<String> iImportLocationData = new Consumer<String>() {
        @Override
        public void accept(String s) {
            addTargetLocData(s);    //  CSVデータを読み込んで追加
            saveTargetLocData();    //  データの保存
        }
    };

    /**
     * GPSデータ記録継続・開始確認ダイヤログ
     */
    public void gpxDataContinuerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("前回から継続しますか")
                .setPositiveButton("継続", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startGpsService(true);
                    }
                })
                .setNeutralButton("新規", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GpxFile gpx = new GpxFile(mDataDirectory, true);
                        gpx.Init(gilib.getDataFileName());
                        gpx.addTailSave();
                        startGpsService(false);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(GpsInfoActivity.this, "Cancelしました。", Toast.LENGTH_SHORT).show();
                    }
                })
                .create()
                .show();
    }

    /**
     * GPSデータ記録開始確認ダイヤログ
     */
    public void gpxDataStartDialog() {
        new AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("データ記録を開始します")
                .setNeutralButton("開始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startGpsService(false);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(GpsInfoActivity.this, "Cancelしました。", Toast.LENGTH_SHORT).show();
                    }
                })
                .create()
                .show();
    }


    /**
     * GPSデータ記録終了確認ダイヤログ
     */
    public void gpxDataPouseDialog() {
        new AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("データの登録を中断しますか")
                .setPositiveButton("中断", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gilib.setGpxDataContinue(true);
                        endGpsService();
                    }
                })
                .setNeutralButton("終了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gilib.setGpxDataContinue(false);
                        endGpsService();
                        goDataList();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(GpsInfoActivity.this, "Cancelしました。", Toast.LENGTH_SHORT).show();
                    }
                })
                .create()
                .show();
    }

    /**
     * データログサービスを開始する
     * @param cont          true(継続)/false(新規)
     */
    private void startGpsService(boolean cont) {
        chkGpsService();
        //	サービスを開始する
        if (!cont) {
            //  新規作成(初期化)
            gilib.setLogFileName();             //  GPXファイル名の作成登録
            gilib.setFirstTime(0);              //  計測開始 累積時間の初期化
            gilib.setTotalDistance(0);          //  計測開始 累積距離の初期化
            gilib.setGpxSaveCount(0);           //  計測開始 計測数の初期化
            gilib.setGpxDataContinue(false);    //  継続開始フラグ(false:新規)
            gilib.setStepCount(0);
        } else {
            //  継続実行
            gilib.setGpxDataContinue(true);     //  継続開始フラグ)true:継続開始)
        }
        setAppTitle();
        // GPSセンサーが利用可能であれば開始
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //  Android8.0からバックグランド処理が無効になるためフォアグランドとしてサービスを実行
            if (Build.VERSION.SDK_INT < 26) {
                startService(new Intent(this, GpsOnService.class));
            } else {
                startForegroundService(new Intent(this, GpsOnService.class));   //  API26以降 Min SDK Version
            }
            setServiceRunningButton(true);
        }
    }

    /**
     * データログのサービスを終了させる
     */
    private void endGpsService() {
        //	サービスを終了する
        stopService(new Intent(this, GpsOnService.class));
        setServiceRunningButton(false);
    }

    /**
     * 目的地設定ダイヤログ
     */
    public void setTargetLocationDialog() {
        if (mTargetLocationList == null || mTargetLocationList.size() == 0)
            return;
        makeTargetList();
        if (mTargetLocationTitle == null || mTargetLocationTitle.length == 0)
            return;
        new AlertDialog.Builder(this)
                .setTitle("目的地設定")
                .setItems(mTargetLocationTitle, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        mTargetTitle = mTargetLocationTitle[which];
                        mTargetTitle = mTargetTitle.substring(0, mTargetTitle.indexOf(" "));
                        String[] data = getTargetLocationData(mTargetTitle, mTargetGroup);
                        int latitudePos = getTargetLocationListTitlePos("緯度", mTargetLocationFormat);
                        int longitudePos = getTargetLocationListTitlePos("経度", mTargetLocationFormat);
                        mTargetLatitude = Double.valueOf(data[latitudePos]);
                        mTargetLongitude = Double.valueOf(data[longitudePos]);
                        displayStatus();
                        setPreference();
                    }
                })
                .create()
                .show();
    }

    /**
     * 目的地グループ(エリア)設定ダイヤログ
     */
    public void setGroupDialog() {
        if (mTargetLocationList == null || mTargetLocationList.size() == 0)
            return;;
        makeTargetGoupList();
        if (mTargetGroupList == null || mTargetGroupList.length == 0)
            return;
        new AlertDialog.Builder(this)
                .setTitle("グループ設定")
                .setItems(mTargetGroupList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        mTargetGroup = mTargetGroupList[which];
                        setAppTitle();
                        setPreference();
                    }
                })
                .create()
                .show();
    }

    /**
     * 目的地設定ダイヤログ用リストの作成
     */
    private void makeTargetList() {
        int titlePos = getTargetLocationListTitlePos("目的地", mTargetLocationFormat);
        int latitudePos = getTargetLocationListTitlePos("緯度", mTargetLocationFormat);
        int longitudePos = getTargetLocationListTitlePos("経度", mTargetLocationFormat);
        int groupPos = getTargetLocationListTitlePos("グループ", mTargetLocationFormat);
        List<SortData> targetList = new ArrayList<>();
        //  現在位置の緯度経度
        double curLatitude = Double.valueOf(mCurrLatitude);
        double curLongitude = Double.valueOf(mCurrLongitude);
        for (int i = 0; i < mTargetLocationList.size(); i++) {
            //  対象グループ(エリア)の選択
            if (mTargetGroup.isEmpty() || mTargetLocationList.get(i)[groupPos].compareTo(mTargetGroup)==0) {
                //  目的地の緯度経度から距離を求める
                double targetLatitude = Double.valueOf(mTargetLocationList.get(i)[latitudePos]);
                double targetLongitude = Double.valueOf(mTargetLocationList.get(i)[longitudePos]);
                double dis = ylib.distance(curLongitude, curLatitude, targetLongitude, targetLatitude);
                //  表示用タイトル
                String title = mTargetLocationList.get(i)[titlePos] + " " + ylib.roundStr(dis, mDigitNo) + " km";
                //  タイトルと距離のリスト作成
                targetList.add(new SortData(title, dis));
            }
        }
        //  距離でリストをソートしタイトルのみを表示リストにコピー
        Collections.sort(targetList, new SortDataComparator());
        mTargetLocationTitle = new String[targetList.size()];
        for (int i = 0; i < mTargetLocationTitle.length; i++)
            mTargetLocationTitle[i] = targetList.get(i).mTitle;
    }

    /**
     * 目的地グループ設定ダイヤログのリスト作成
     */
    private void makeTargetGoupList() {
        int groupPos = getTargetLocationListTitlePos("グループ", mTargetLocationFormat);
        //  グループの重複登録をなくすためsetに一次登録
        Set<String> targetGroupList = new LinkedHashSet<>();
        for (int i = 0; i < mTargetLocationList.size(); i++) {
            targetGroupList.add(mTargetLocationList.get(i)[groupPos]);
        }
        //  表示リストにコピー
        mTargetGroupList = targetGroupList.toArray(new String[targetGroupList.size()]);
    }

    /**
     * 目的地と目的地グループのデータを検索する
     * @param title     目的地
     * @param group     目的地グループ
     * @return          目的地データ
     */
    private String[] getTargetLocationData(String title, String group) {
        int titlePos = getTargetLocationListTitlePos("目的地", mTargetLocationFormat);
        int groupPos = getTargetLocationListTitlePos("グループ", mTargetLocationFormat);
        for (int i = 0; i < mTargetLocationList.size(); i++) {
            if (mTargetLocationList.get(i)[titlePos].compareTo(title) == 0 &&
                    mTargetLocationList.get(i)[groupPos].compareTo(group) == 0)
                return mTargetLocationList.get(i);
        }
        return null;
    }

    /**
     * 目的地編集画面に移行する
     */
    private void goTagetLocationEdit(String latitude, String logitude) {
        Intent intent = new Intent(GpsInfoActivity.this, TargetLocationEdit.class);
        intent.putExtra("SAVEDIR", mDataDirectory);    //	保存ディレクトリ
        intent.putExtra("TARGETFILENAME", mTargetFileName);
        intent.putExtra("TARGETGROUP", mTargetGroup);
        intent.putExtra("TARGETNAME", mTargetTitle);
        intent.putExtra("LATITUDE", latitude);
        intent.putExtra("LONGITUDE", logitude);
        startActivityForResult(intent, LOCATIONEDIT);
    }

    /**
     * データ一覧画面を開く
     */
    private void goDataList() {
        Intent intent = new Intent(GpsInfoActivity.this, DataListActivity.class);
        intent.putExtra("SAVEDIR", mDataDirectory);    //	保存ディレクトリ
        startActivity(intent);
    }

    /**
     * 測定中の登録データを編集する
     * 測定データファイル名が存在すれば測定中と判断して測定データの登録データの編集をする
     */
    private void openLocMemo() {
        //  測定データファイルの存在確認
        String gpxFileName = gilib.getDataFileName();
        if (0 < gpxFileName.length()) {
            //  測定データファイル名から年を確認しインデックスファイルを読み込む
            String year = gpxFileName.substring(0, 4);
            String indexFileName = gilib.getLoadIndexFileName(year);
            ListData listData = new ListData(this, DataListActivity.mDataFormat);
            listData.setKeyData(DataListActivity.mKeyData);
            listData.setSaveDirectory(mIndexDirectory, indexFileName);
            if (!listData.LoadFile())
                return;
            //  インデックスファイルに測定データファイルが登録されていればそれを編集
            //  登録されていなければ新規データを7追加して編集画面を開く
            String key = listData.findData(gpxFileName, "GPX");
            if (0 < key.length()) {
                //  既存データの編集
                String date = listData.getData(key, "年月日");
                String time = listData.getData(key, "時間");
                goLocMemo(date, time);
            } else {
                //  新規データをインデックスファイルに追加して編集
                String[] data = new String[DataListActivity.mDataFormat.length];
                data[0] = gpxFileName.substring(0,8);
                data[1] = gpxFileName.substring(9,15);
                data[1] = data[1].substring(0,2)+":"+data[1].substring(2,4)+":"+data[1].substring(4,6);
                data[11] = gpxFileName;
                listData.setData(data, false);
                listData.saveDataFile();
                goLocMemo(data[0], data[1]);
            }
        }
    }

    /**
     * データ編集画面を起動する(GPXデータの更新はしない)
     * @param date      キーデータの日付(yyyymmdd)
     * @param time      キーデータの時間(HH:mm:ss)
     */
    private void goLocMemo(String date, String time) {
        Log.d(TAG,"goLocMemo: "+date+" "+time);
        Intent intent = new Intent(this, LocMemoActivity.class);
        intent.putExtra("SAVEDIR", mDataDirectory);     //	保存ディレクトリ
        intent.putExtra("INDEXDIR", mIndexDirectory);   //	データファイル名
        intent.putExtra("DATE", date);                  //	現在日時(yyyymmdd)
        intent.putExtra("TIME", time);                  //	時間(HH:mm:ss)
        intent.putExtra("GPXUPDATE", false);        //  GPXデータの非更新

        startActivityForResult(intent, GpsInfoActivity.LOCMEMO_ACTIVITY);
    }

    /**
     * ボタンの有効/無効の設定
     * @param gpsService
     */
    public void setServiceRunningButton(boolean gpsService) {
        if (gpsService) {
            mBtTraking.setText("記録終了");
            mBtTraking.setTextColor(Color.RED);
        } else {
            mBtTraking.setText("記録開始");
            mBtTraking.setTextColor(mTrakButtonTextColors);
        }
    }

    /**
     * 位置情報の表示(GPS取得時更新)
     * @param location
     */
    private void displayLocation(Location location) {
        mCount++;
        mCurrLatitude = location.getLatitude();     //  緯度
        mCurrLongitude = location.getLongitude();   //  経度
        mCurrElevator = location.getAltitude();     //	高度
        mTvProvider.setText(location.getProvider() + " " + mCount); // GPS or NETWORK
        long curTime = location.getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd (EE) HH:mm:ss.SSS zz");
        mTvGpsTime.setText(df.format(curTime));     //	取得時間
        mTvLatitude.setText(ylib.roundStr(mCurrLatitude, mDigitNoCoordinate));  //	現在位置緯度
        mTvLongitude.setText(ylib.roundStr(mCurrLongitude, mDigitNoCoordinate));//	現在位置経度
        mTvElevator.setText(ylib.roundStr(location.getAltitude(), mDigitNo));   //	高度(m)
        //  GPSロガーの状態
        mTvGpsStatusMsg.setText(gilib.getCurStatusMsg());

        double dis = ylib.distance(mCurrLongitude, mCurrLatitude, mTargetLongitude, mTargetLatitude);    //	距離計算
        double azi = ylib.azimuth2(mCurrLongitude, mCurrLatitude, mTargetLongitude, mTargetLatitude);    //	方位計算

        //	グラフィック表示設定
        mGpsGraphDraw.mAltitude = (float)location.getAltitude();    //	高度
        mGpsGraphDraw.mAccuracy = location.getAccuracy();           //	精度
        mGPSBarmaxHeight = setDrawElevatorBarMax(location.getAccuracy(), mGPSBarmaxHeight);
        mGpsGraphDraw.mMaxHeight = mGPSBarmaxHeight;                //	高度バーの最大値
        mGpsGraphDraw.mMinHeight = mGPSBarminHeight;                //	高度バーの最小値
        mGpsGraphDraw.mCurLongitude = (float) mCurrLongitude;       //	現在地経度
        //	グラフィック表示設定
        mGpsGraphDraw.mTargetDistance = mTargetTitle + " " +
                (mCount > 0 ? ylib.roundStr(dis, mDigitNo) : "***");//	目的地と距離
        mGpsGraphDraw.mTargetDirect = (float) azi;                  //	目的地の方位
        mGpsGraphDraw.mSunDisp = mSunDisp;                          //	太陽の方位表示
        mGpsGraphDraw.invalidate();                                 //
    }

    /**
     * 現在地のLocationデータを保存
     */
    private void saveCurrentLocation() {
        gilib.setCurLatitude((float)mCurrLatitude);
        gilib.setCurLongitude((float)mCurrLongitude);
        gilib.setCurElevator((float)mCurrElevator);
    }

    /**
     * 目的地の緯度経度
     */
    private void displayStatus() {
        mTvTargetTitle.setText(mTargetTitle);                                           //  目的地名
        mTvTargetLatitude.setText(ylib.roundStr(mTargetLatitude, mDigitNoCoordinate));  //	緯度
        mTvTargetLongitude.setText(ylib.roundStr(mTargetLongitude, mDigitNoCoordinate));//	経度
    }

    /**
     * 高さバーの最大値を求める
     * @param height        高さ
     * @param maxHeight     既定の最大高さ
     * @return              求めなおした最大高さ
     */
    private float setDrawElevatorBarMax(float height, float maxHeight) {
        float pitch = mGPSBarHeightPitch;
        int n = (int)(maxHeight / pitch);
        if ((n * pitch * 4 / 5) < height) {
            n++;
            while ((n * pitch * 4 / 5) < height)
                n++;
        } else if (height < (n * pitch * 2 / 5)) {
            n--;
            while (height < (n * pitch * 2 / 5))
                n--;
        }
        if (n < 1)
            n = 1;
        return (float)n * pitch;
    }

    /**
     * 目的地の座標データを読み込む
     */
    private void loadTargetLocData() {
        mTargetFilePath = mDataDirectory + "/" + mTargetFileName;
        if (ylib.existsFile(mTargetFilePath))
            mTargetLocationList = loadTargetLocData(mTargetFilePath);
        squeezeLocationList(mTargetLocationList, mTargetLocationFormat);    //  目的地データの重複削除
    }

    /**
     * 指定ファイルから目的地データを読み込んで既存データに追加
     * @param path      目的地データファイルのパス
     */
    private void addTargetLocData(String path) {
        List<String[]> dataList = new ArrayList<>();
        dataList = loadTargetLocData(path);             //  追加データの読み込み
        for (int i=0; i < dataList.size(); i++)
            mTargetLocationList.add(dataList.get(i));   //  既存データに追加
        squeezeLocationList(mTargetLocationList, mTargetLocationFormat);    //  目的地データの重複削除
    }

    /**
     * 目的地データの読み込み
     * @param path      目的データのファイルパス
     * @return          読み込んだ目的地データリスト
     */
    private List<String[]> loadTargetLocData(String path) {
        List<String[]> dataList = new ArrayList<>();
        if (ylib.existsFile(path)) {
            dataList = ylib.loadCsvData(path, mTargetLocationFormat);
        } else {
            Toast.makeText(this, "目的地リストがありません", Toast.LENGTH_SHORT).show();
        }
        return dataList;
    }

    /**
     * 目的地データの保存
     */
    private void saveTargetLocData() {
        ylib.saveCsvData(mTargetFilePath, mTargetLocationFormat, mTargetLocationList);
    }

    /**
     * 目的地位置のデータリストで「目的地+グループ」で重複データを削除する
     */
    protected static void squeezeLocationList(List<String[]> targetLocationList, String[] format) {
        Map<String, String[]> locationList = new HashMap<>();
        int titlePos = getTargetLocationListTitlePos("目的地", format);
        int groupPos = getTargetLocationListTitlePos("グループ", format);
        for (int i = 0; i < targetLocationList.size(); i++) {
            String key = targetLocationList.get(i)[titlePos] + " " + targetLocationList.get(i)[groupPos];
            locationList.put(key, targetLocationList.get(i));
        }
        targetLocationList.clear();
        for (String[] data : locationList.values())
            targetLocationList.add(data);
    }

    /**
     * 目的地リストのデータの種別位置を求める
     * @param title     データの種別
     * @return          配列の位置
     */
    protected static int getTargetLocationListTitlePos(String title, String[] format) {
        if (title == null || format == null)
            return -1;
        for (int i = 0; i < format.length; i++) {
            if (format[i].compareTo(title) == 0)
                return i;
        }
        return -1;
    }
    /**
     * 方位、傾きの表示(センサー更新時)
     * @param azimuth       方位
     * @param pitch         前後角
     * @param roll          左右角
     */
    public void displayAzimuth(float azimuth, float pitch, float roll) {
        //	グラフィック表示設定
        mGpsGraphDraw.mAzimuthFix = mAzimuthFix;        //  方位固定
        mGpsGraphDraw.mAngle = -azimuth;                //	方位の角度
        mGpsGraphDraw.mPitch = pitch;                   //	前後の傾き
        mGpsGraphDraw.mRoll = roll;                     //	左右の傾き
        mGpsGraphDraw.invalidate();
    }

    /**
     *  画面が回転していることを考えた方角の取り出し
     * @param rotate    方位角
     * @param out        画面の向きを考慮した角度
     */
    @SuppressLint("NewApi")
    public void getOrientation(float[] rotate, float[] out) {
        // ディスプレイの回転方向を求める(縦もちとか横持ちとか)
        Display disp = this.getWindowManager().getDefaultDisplay();
        int dispDir = disp.getRotation();                    //	コレを使うためにはAPIレベルを8にする必要がある
        if (dispDir == Surface.ROTATION_0) {                // 画面回転してない場合はそのまま
            SensorManager.getOrientation(rotate, out);
        } else {                                            // 回転している
            float[] outR = new float[16];
            if (dispDir == Surface.ROTATION_90) {            // 90度回転
                SensorManager.remapCoordinateSystem(
                        rotate, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);
            } else if (dispDir == Surface.ROTATION_180) {   // 180度回転
                float[] outR2 = new float[16];
                SensorManager.remapCoordinateSystem(
                        rotate, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR2);
                SensorManager.remapCoordinateSystem(
                        outR2, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);
            } else if (dispDir == Surface.ROTATION_270) {   // 270度回転
                SensorManager.remapCoordinateSystem(
                        outR, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_MINUS_X, outR);
            }
            SensorManager.getOrientation(outR, out);
        }
    }

    /**
     * サービスの起動状態の確認
     * @param c
     * @param cls
     * @return
     */
    public boolean isServiceRunning(Context c, Class<?> cls) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningService = am.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo i : runningService) {
            if (cls.getName().equals(i.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     *  ファイルアクセスのパーミッションチェック
     */
    private void chkFileAccessPermission(String folder) {
        if (30 <= Build.VERSION.SDK_INT)
            chkManageAllFilesAccess(folder);
        else
            ylib.checkStragePermission(this);
    }

    private void chkManageAllFilesAccess() {
        String path = "/storage/emulated/0/";
        chkManageAllFilesAccess(path);
    }

    /**
     * 指定のフォルダのファイルアクセスの確認
     * アクセスできないときはパーミッションの設定を開く
     * @param folder
     */
    private void chkManageAllFilesAccess(String folder) {
        if (folder.charAt(folder.length() - 1) == '/')
            folder = folder.substring(0, folder.length() - 1);
        String path = folder + "/chkManageAllFilesAccess.txt";
        File file = new File(path);
        try {
            if (file.exists())
                file.delete();
            if (!file.createNewFile()) {
                goFileAccessPermision();
            }
        } catch (Exception e) {
            goFileAccessPermision();
        }
    }

    /**
     * ファイルアクセスのパーミッション設定を開く
     */
    private void goFileAccessPermision() {
        Intent intent = new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION");
        startActivity(intent);
    }

    /**
     * GPS設定値の更新
     * @param gpsMinTime        msec
     * @param gpsMinDistance    m
     */
    private void requestLocationUpdates(long gpsMinTime, float gpsMinDistance) {
        //  下記パーミッションチェックを入れないと警告が出る
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (mNetworkProvider)
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, gpsMinTime, gpsMinDistance, this);
        else
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsMinTime, gpsMinDistance, this);
    }

    /**
     * 緯度経度の指定で地図を開く
     * インテントリスト
     * Google マップ  geo:latitude,longitude
     * 　             geo:latitude,longitude?z=zoom
     *	              geo:0,0?q=my+street+address
     *                geo:0,0?q=business+near+city
     *  https://developers.google.com/maps/documentation/urls/android-intents
     * @param latitude
     * @param longitude
     */
    private void openMaps(String latitude, String longitude) {
        String uri = String.format("geo:%s,%s", latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    /**
     *  GPSが有効かCheck
     *  有効になっていなければ、設定画面の表示確認ダイアログ
     */
    private void chkGpsService() {
        Log.d(TAG, "chkGpsService: ");
        // GPSセンサーが利用可能か？
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setMessage("GPSが有効になっていません。\n有効化しますか？")
                    .setCancelable(false)

                    // GPS設定画面起動用ボタンとイベントの定義
                    .setPositiveButton("GPS設定起動",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent callGPSSettingIntent = new Intent(
                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivity(callGPSSettingIntent);
                                }
                            });
            // キャンセルボタン処理
            alertDialogBuilder.setNegativeButton("キャンセル",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //  GPS サービスの開始
//                            setServiceRunningButton(isServiceRunning(GpsInfoActivity.this, GpsOnService.class));
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            // 設定画面へ移動するかの問い合わせダイアログを表示
            alert.show();
        }
    }

    /**
     *  センサーの取得
     * @param sensor		センサーオブジェクト
     * @param senserType	センサータイプ
     * @return				センサーオブジェクト
     */
    private Sensor getSensor(Sensor sensor,int senserType) {
        List<Sensor> list;
        //	センサーの取得
        list = mSensorManager.getSensorList(senserType);
        if (list.size() > 0)
            sensor = list.get(0);
        //	センサーの開始(SensorEventListenerがimplementsされていること)
        if (sensor != null)
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        return sensor;
    }

    /**
     * 	タイトル表示(目的地グループとGPS保存ファイル名)
     */
    private void setAppTitle() {
        this.setTitle("GPS情報 [" + mTargetGroup + "][" + gilib.getDataFileName() + "]");
    }

    /**
     * 	Preferenceから設定値の取得
     */
    private void getPreference() {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(GpsInfoActivity.this);
        //  目的地データ
        mTargetGroup = prefs.getString("TargetGroup", "");                                  //表示中のグループ
        mTargetTitle = prefs.getString("TargetName", "自宅");                               //表示中の目的地名
        mTargetLatitude = Double.valueOf(prefs.getString("TargetLatitude", "35.566196"));   //表示中の目的地緯度
        mTargetLongitude = Double.valueOf(prefs.getString("TargetLongitude", "139.69326")); //表示中の目的地経度

    }

    /**
     * Preferenceにデータを書き込む
     */
    private void setPreference() {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(GpsInfoActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        //  目的地データ
        editor.putString("TargetGroup", mTargetGroup);
        editor.commit();
        editor.putString("TargetName", mTargetTitle);
        editor.commit();
        editor.putString("TargetLatitude", String.valueOf(mTargetLatitude));
        editor.commit();
        editor.putString("TargetLongitude", String.valueOf(mTargetLongitude));
        editor.commit();
    }

    /**
     * 画面の初期化
     */
    private void initScreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);    //	画面をスリープさせない
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);	//	画面を縦に固定する

        mTvProvider = (TextView)findViewById(R.id.textView2);
        mTvGpsTime = (TextView)findViewById(R.id.textView4);
        mTvCurTime = (TextView)findViewById(R.id.textView6);
        mTvLatitude = (TextView)findViewById(R.id.textView8);
        mTvLongitude = (TextView)findViewById(R.id.textView9);
        mTvElevator = (TextView)findViewById(R.id.textView10);
        mTvTargetTitle = (TextView)findViewById(R.id.textView12);
        mTvTargetLatitude = (TextView)findViewById(R.id.textView13);
        mTvTargetLongitude = (TextView)findViewById(R.id.textView14);
        mTvGpsStatusMsg = (TextView)findViewById(R.id.textView32);

        mBtTagetLocation = (Button)findViewById(R.id.button);
        mBtEditDest = (Button)findViewById(R.id.button2);
        mBtTraking = (Button)findViewById(R.id.button3);
        mBtTrakingList = (Button)findViewById(R.id.button4);
        mBtTrakingMemo = (Button)findViewById(R.id.button5);

        mBtTagetLocation.setOnClickListener(this);
        mBtEditDest.setOnClickListener(this);
        mBtTraking.setOnClickListener(this);
        mBtTrakingList.setOnClickListener(this);
        mBtTrakingMemo.setOnClickListener(this);
        mBtTagetLocation.setOnLongClickListener(this);

        mTvProvider.setText("***");
        mTvGpsTime.setText("***");
        mTvCurTime.setText("***");
        mTvLatitude.setText("***");
        mTvLongitude.setText("***");
        mTvElevator.setText("***");
        mTvTargetTitle.setText("***");
        mTvTargetLatitude.setText("***");
        mTvTargetLongitude.setText("***");
        mTvGpsStatusMsg.setText("***");

        mTrakButtonTextColors = mBtTraking.getTextColors();

        //  グラフィック表示の追加
        mLinearLayout = (LinearLayout)findViewById(R.id.linearLayout);
        mLinearLayout.setOnLongClickListener(this);
        mGpsGraphDraw = new GpsGraphDraw(this);
        mLinearLayout.addView(mGpsGraphDraw);
    }

    /**
     * ボタンの状態を設定する
     */
    private void setButtonStat() {
        //  メモボタンはファイルが作られている(記録中)の時に有効
        if (gilib.getDataFileName().length() < 1)
            mBtTrakingMemo.setEnabled(false);
        else
            mBtTrakingMemo.setEnabled(true);
    }

    /**
     * ファイルの存在確認
     * @param filename      ファイル名
     * @return              有無
     */
    private boolean existDataFile(String filename) {
        String filePath = mDataDirectory + "/" + filename;
        if (ylib.existsFile(filePath + ".gpx")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 主データとは別のデータでソートするためのデータクラス
     */
    class SortData {
        public String mTitle;
        public double mValue;
        public SortData(String title, double value) {
            mTitle = title;
            mValue = value;
        }
    }

    /**
     * SortDataクラスのソートCompareの実装クラス
     * 例: Collections.sort(sortDataList, new SortDataComparator());
     */
    class SortDataComparator implements Comparator {
        public int compare(Object s, Object t) {
            //               + (x > y)
            // compare x y = 0 (x = y)
            //               - (x < y)
            if (((SortData) s).mValue > ((SortData) t).mValue)
                return 1;
            else if (((SortData) s).mValue < ((SortData) t).mValue)
                return -1;
            else
                return 0;
        }
    }}