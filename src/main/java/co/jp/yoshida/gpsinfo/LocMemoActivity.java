package co.jp.yoshida.gpsinfo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LocMemoActivity extends AppCompatActivity
        implements View.OnClickListener {
    private static final String TAG = "LocMemoActivity";

    private TextView mTvDate;           //  開始日時/少量時間
    private TextView mTvStartLocation;  //  開始位置
    private TextView mTvTransit;        //  移動距離/時間
    private TextView mTvElevator;       //  最大高度/最小高度
    private Spinner mSpCategory;        //  分類
    private EditText mEdTitle;          //  タイトル
    private EditText mEdMemoText;       //  詳細内容
    private EditText mEdGpxFileName;    //  GPXファイル名
    private Button mBtSave;             //  ファイル保存/更新
    private Button mBtGpxOperation;     //  GPXファイル操作
    private Button mBtInsOperation;     //  挿入
    private Button mBtGraph;            //  グラフ
    private Button mBtShare;            //  共有
    private ArrayAdapter<String> mCategoryAdapter;

    private String[] mCategory = {
            "ランニング","ジョギング","ウォーキング","散歩",
            "山歩き","自転車","車","旅行","スキー","水泳","その他"};
    private String[] mInsertMenu = {"GPX詳細","現在時刻","緯度経度","テキストファイル"};
    private boolean mAutoCategoryFolder = true;

    private ListData mListData;             //  インデックスデータ
    private String mDataDirectory;          //  GPX保存基準ディレクトリ
    private String mIndexDirectory;         //  インデックスファイル保存ディレクトリ

    private String mGpxFileName;            //  GPXファイル名
    private boolean mGpxFileUpdate=false;   //  Gpxファイルを更新
    private String mDateName;               //  日付
    private String mTimeName;               //  時間
    private String mKeyData;                //  キーデータ
    private int mDigitNo = 4;               //  小数点以下表示桁数
    private boolean mGpxUpdate = true;      //  Load時にGPXデータを更新する

    private GpsInfoLib gilib;
    private YLib ylib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loc_memo);

        init();
        ylib = new YLib(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            //  他のアプリからのデータ受信
            Bundle extras = intent.getExtras();
            if (extras != null) {
                CharSequence ext = extras.getCharSequence(Intent.EXTRA_TEXT);
                if (ext != null) {
                }
            }
        } else {
            //  呼び出し元データの引継ぎ
            mDataDirectory = intent.getStringExtra("SAVEDIR");          //  リストデータ保存フォルダ
            mIndexDirectory = intent.getStringExtra("INDEXDIR");        //  MEMOファイル名
            mDateName = intent.getStringExtra("DATE");                  //  開始日
            mTimeName = intent.getStringExtra("TIME");                  //  開始時間
            mGpxUpdate = intent.getBooleanExtra("GPXUPDATE", true); //  Load時にGPXデータを更新
            Log.d(TAG,"onCreate:"+mDateName+" "+mTimeName+" "+mGpxUpdate);
        }

        gilib = new GpsInfoLib(this, mDataDirectory);
        mListData = new ListData(this, DataListActivity.mDataFormat);
        mListData.setKeyData(DataListActivity.mKeyData);
        loadData(mDateName, mTimeName);
    }

    @Override
    public void onClick(View view) {
        Button button = (Button)view;
        if (button.getText().toString().compareTo("更新")==0){
            //  ファイル保存
            saveData(mDateName, mTimeName);
            setResult(RESULT_OK);
            finish();
        } else if (button.getText().toString().compareTo("GPX")==0) {
            ylib.fileSelectDialog(mDataDirectory, "*.gpx", true, iGpxFileSet);
        } else if (button.getText().toString().compareTo("挿入")==0) {
            ylib.setMenuDialog(this, "挿入メニュー", mInsertMenu, iInsert);
        } else if (button.getText().toString().compareTo("グラフ")==0) {
            goGpxGraph();
        } else if (button.getText().toString().compareTo("共有")==0) {
            ylib.executeFile(this, gilib.getGpxPath(mListData.getData(mKeyData, "GPX")));
        }

    }

    /**
     * GPXデータをグラフ表示する
     */
    private void goGpxGraph() {
        String path = gilib.getGpxPath(mListData.getData(mKeyData, "GPX"));
        if (!ylib.existsFile(path)) {
            Toast.makeText(this, "データファイルか存在しません", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, GpxGraphActivity.class);
        intent.putExtra("FILEPATH", path);
        startActivity(intent);
    }

    /**
     * GPXファイルを設定する(新規作成時)
     */
    Consumer<String> iGpxFileSet = new Consumer<String>() {
        @Override
        public void accept(String s) {
            setDisplayGpsData(s);
        }
    };

    /**
     * メモ欄にデータを挿入するための関数インターフェース
     */
    Consumer<String> iInsert = new Consumer<String>() {
        @Override
        public void accept(String s) {
            if (s.compareTo("GPX詳細") == 0) {
                insertMemoText(getGpxDiscription());
            } else if (s.compareTo("現在時刻") == 0) {
                insertMemoText(gilib.getNowTime());
            } else if (s.compareTo("緯度経度") == 0) {
                insertMemoText(getCurLocation());
            } else if (s.compareTo("テキストファイル") == 0) {
                String dir = gilib.getTextFileDirectory();
                ylib.fileSelectDialog(dir.compareTo("###")==0?mDataDirectory:dir, "*.txt", true, iTextFileSet);
            }
        }
    };

    /**
     * テキストファイルを取り込むための関数インターフェース
     */
    Consumer<String> iTextFileSet = new Consumer<String>() {
        @Override
        public void accept(String s) {
            String buf = ylib.readFileData(s);
            insertMemoText(buf);
            gilib.setTextFileDirectory(ylib.getDir(s));
        }
    };

    /**
     * 変更内容をインデックスファイルに保存
     * @param date      データのキーとなる日付
     * @param time      データのキーとなる時間
     */
    private void saveData(String date, String time) {
        Log.d(TAG,"saveData: ");
        mKeyData = date + time;
        mListData.setData(mKeyData, "分類", getCategoryTitle(), true);
        mListData.setData(mKeyData, "タイトル", mEdTitle.getText().toString(), true);
        mListData.setData(mKeyData, "メモ", ylib.strControlCodeCnv(mEdMemoText.getText().toString()), true);
        String gpxFileName = mEdGpxFileName.getText().toString();

        //  GPXデータの内容だけを更新する
        if (mGpxUpdate && gpxFileName != null) {
            String[] data = mListData.getData(mKeyData);
            String gpxPath = gilib.getGpxPath(gpxFileName);
            data = gilib.updateGpxData(gpxPath, data, DataListActivity.mDataFormat);
            data[mListData.getTitlePos("分類")] = getCategoryTitle();     //  分類はGPXからも設定されるので再設定する
            mListData.setData(mKeyData, data, true);
        }

        //  GPXファイル名が変更になった場合、ファイル名を変更する
        if (mGpxFileName.compareTo(gpxFileName)!=0) {
            String fullPath = gilib.getGpxPath(mGpxFileName);
            String newFullpath = ylib.getDir(fullPath) + "/" + gpxFileName + ".gpx";
            if (!mGpxFileUpdate && !ylib.rename(fullPath, newFullpath)) {
                Toast.makeText(this, "GPXファイル名を変更できませんでした", Toast.LENGTH_LONG);
            } else {
                mListData.setData(mKeyData, "GPX", mEdGpxFileName.getText().toString(), true);
            }
        }
        mListData.saveDataFile();

        //  年と分類によってGPXファイルを移動する
        if (mAutoCategoryFolder && mGpxUpdate) {
            mGpxFileName = mListData.getData(mKeyData, "GPX");
            String category = mListData.getData(mKeyData, "分類");
            gilib.moveGpxData(gilib.getGpxPath(mGpxFileName), date.substring(0,4), category, mDataDirectory);
        }
    }

    /**
     * インデックスデータからデータを読み込んで画面に表示する
     * GPXデータが測定中の場合はGPXデータを読みにいかない
     * @param date      データのキーとなる日付
     * @param time      データのキーとなる時間
     */
    private void loadData(String date, String time) {
        //  インデックスデータの取得
        mListData.setSaveDirectory(mIndexDirectory, gilib.getLoadIndexFileName(date));
        mListData.LoadFile();
        mKeyData = date + time;
        if (!mListData.findKey(mKeyData)) {
            Toast.makeText(this, mKeyData+" が存在しません", Toast.LENGTH_LONG).show();
            return;
        }
        mGpxFileName = mListData.getData(mKeyData, "GPX");
        //  GPXデータの表示更新
        if (mGpxUpdate && mGpxFileName.compareTo(gilib.getDataFileName())!=0) {
            setDisplayGpsData(gilib.getGpxPath(mGpxFileName));
        } else {
            //  計測中のデータについてはGPXデータを更新しない
            String dateTitle =
                    mDateName.substring(0,4)+"年"+mDateName.substring(4,6)+"月"+mDateName.substring(6,8)+"日" +
                            mTimeName.substring(0,2)+"時"+mTimeName.substring(3,5)+"分"+mTimeName.substring(6,8)+"秒";
            mTvDate.setText(dateTitle);
        }
        //  その他のデータ表示
        if (mListData.getData(mKeyData, "歩数") != null && 0 < mListData.getData(mKeyData, "歩数").length()){
            String trasitTitle = mTvTransit.getText().toString();
            mTvTransit.setText(trasitTitle +" 歩数 "+mListData.getData(mKeyData, "歩数"));
        }
        setCategoryTitle(mListData.getData(mKeyData, "分類"));
        mEdTitle.setText(mListData.getData(mKeyData, "タイトル"));
        mEdMemoText.setText(ylib.strControlCodeRev(mListData.getData(mKeyData, "メモ")));
        mEdGpxFileName.setText(mGpxFileName);
    }

    /**
     * GPXファイルのデータを画面に表示する
     * @param filePath      GPXファイルパス
     */
    private void setDisplayGpsData(String filePath) {
        GpsDataReader gpsDataReader = gilib.getGpsData(filePath);
        if (gpsDataReader == null)
            return;

        //  GPXデータとインデックスのデータを画面コントロールに設定する
        GpsData lastData = gpsDataReader.getData(gpsDataReader.getDataSize() - 1);  //  最終の位置データ

        String[] gpxData = new String[mListData.getDataFormat().length];//  GPXデータ
        gpxData = gilib.updateGpxData(filePath, gpxData, DataListActivity.mDataFormat); //  GPXデータを取り込んで更新する
        String dateName = gpxData[mListData.getTitlePos("年月日")];     //  yyyyMMdd
        String timeName = gpxData[mListData.getTitlePos("時間")];       //  HH:mm:ss
        String lastTimeName = lastData.getTime();   //  HH:mm:ss
        String dateTitle =
                dateName.substring(0,4)+"年"+dateName.substring(4,6)+"月"+dateName.substring(6,8)+"日" +
                        timeName.substring(0,2)+"時"+timeName.substring(3,5)+"分"+timeName.substring(6,8)+"秒" + " - " +
                        lastTimeName.substring(0,2)+"時"+lastTimeName.substring(3,5)+"分"+lastTimeName.substring(6,8)+"秒";
        String locationTitle =
                "緯度"+gpxData[mListData.getTitlePos("緯度")] +
                        "　経度"+gpxData[mListData.getTitlePos("経度")]+
                        " 高さ"+gpxData[mListData.getTitlePos("高度")]+"m";
        String trasitTitle =
                gpxData[mListData.getTitlePos("移動距離")]+"km 時間 "+
                        gpxData[mListData.getTitlePos("移動時間")];
        String elevatorTitle =
                "最大高度"+gpxData[mListData.getTitlePos("最大高度")]+
                        "m　最小高度"+gpxData[mListData.getTitlePos("最小高度")]+"m";

        //  画面コントロールの更新
        mTvDate.setText(dateTitle);
        mTvStartLocation.setText(locationTitle);
        mTvTransit.setText(trasitTitle);
        mTvElevator.setText(elevatorTitle);
    }

    /**
     * 画面ののメモ欄にデータを挿入する
     * @param repText
     */
    private void insertMemoText(String repText) {
        String summery = mEdMemoText.getText().toString();
        int n = mEdMemoText.getSelectionStart();
        int m = mEdMemoText.getSelectionEnd();
        if (0 <= n) {
            summery = summery.substring(0, Math.min(n,m)) + repText + summery.substring(Math.max(n,m));
        } else {
            if (summery.charAt(summery.length() - 1) != '\n')
                summery += "\n";
            summery += repText + "\n";
        }
        mEdMemoText.setText(summery);
        mEdMemoText.setSelection(m + repText.length());
    }

    /**
     * GPXデータの内容を文字列にする
     * @return      GPXの詳細内容
     */
    private String getGpxDiscription() {
        //  GPXデータの取得
        mGpxFileName = mListData.getData(mKeyData, "GPX");
        String gpxPath = gilib.getGpxPath(mGpxFileName);
        GpsDataReader gpsDataReader = gilib.getGpxData(gpxPath);
        if (gpsDataReader == null)
            return "";
        float distance = (float) gpsDataReader.getDisCovered(gpsDataReader.getDataSize() - 1);	//	累積距離(km)
        long totalTime = gpsDataReader.getLapTime(gpsDataReader.getDataSize() - 1);     //  経過時間(S)
        float maxelevator = (float) gpsDataReader.getMaxElevator();		//	最大高さ(m)
        float minelevator = (float) gpsDataReader.getMinElevator();		//	最大高さ(m)
        String summery = "";
        summery += "データ数　: "+gpsDataReader.getDataSize()+"\n";
        summery += "開始時間　: "+ylib.CalendarString(gpsDataReader.getDate(0))+"\n";
        summery += "終了時間　: "+ylib.CalendarString(gpsDataReader.getDate(gpsDataReader.getDataSize() - 1))+"\n";
        summery += "累積時間　: "+ylib.Sec2Time(totalTime)+"\n"; //  時間:分:秒
        summery += "移動距離　: "+String.format("%3.2f km", distance)+"\n";
        summery += "平均速度　: "+String.format("%3.2f km/h", distance/((float)gpsDataReader.getLapTime(gpsDataReader.getDataSize()-1)/3600f))+"\n";
        summery += "平均ペース: "+String.format("%3.2f min/km", ((float)gpsDataReader.getLapTime(gpsDataReader.getDataSize()-1)/60f)/distance)+"\n";
        summery += "最大高度　: "+String.format("%1$,3.1f m\n",maxelevator);
        summery += "最小高度　: "+String.format("%1$,3.1f m\n",minelevator);
        summery += "標高差　　: "+String.format("%1$,3.1f m\n",maxelevator - minelevator);

        return summery;
    }

    /**
     * 現在位置の取得
     * @return      現在位置の緯度経度
     */
    private String getCurLocation() {
        float latitude = gilib.getCurLatitude();
        float longitude = gilib.getCurLongitude();
        float elevetor = gilib.getCurElevator();
        return  "緯度 "+latitude+" 経度 "+longitude+" 高度 "+ylib.roundStr(elevetor, 2)+"m";
    }

    /**
     * フィルタの分類に設定されているカレント項目を取り出す
     * @return      分類名
     */
    private String getCategoryTitle() {
        int l = mSpCategory.getSelectedItemPosition();
        return (0<=l?mCategoryAdapter.getItem(l):"");
    }

    /**
     * フィルタの分類のカレント項目を設定する
     * @param category      分類名
     */
    private void setCategoryTitle(String category) {
        int n = mCategoryAdapter.getPosition(category);
        if (0<=n)
            mSpCategory.setSelection(n);
        else
            mSpCategory.setSelection(mCategoryAdapter.getCount()-1);
    }

    /**
     * 画面コントロールの初期化
     */
    private void init() {
        mTvDate = (TextView)findViewById(R.id.textView21);
        mTvStartLocation = (TextView)findViewById(R.id.textView23);
        mTvTransit = (TextView)findViewById(R.id.textView25);
        mTvElevator = (TextView)findViewById(R.id.textView26);
        mSpCategory = (Spinner)findViewById(R.id.spinner5);
        mEdTitle = (EditText)findViewById(R.id.editText7);
        mEdMemoText = (EditText)findViewById(R.id.editText9);
        mEdGpxFileName = (EditText)findViewById(R.id.editText8);
        mBtSave = (Button)findViewById(R.id.button15);
        mBtGpxOperation = (Button)findViewById(R.id.button16);
        mBtInsOperation = (Button)findViewById(R.id.button17);
        mBtGraph = (Button)findViewById(R.id.button18);
        mBtShare = (Button)findViewById(R.id.button19);

        mBtSave.setOnClickListener(this);
        mBtGpxOperation.setOnClickListener(this);
        mBtInsOperation.setOnClickListener(this);
        mBtGraph.setOnClickListener(this);
        mBtShare.setOnClickListener(this);

        mBtGpxOperation.setEnabled(false);      //  GPXボタンの無効化

        mCategoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mCategory);
        mSpCategory.setAdapter(mCategoryAdapter);
    }
}