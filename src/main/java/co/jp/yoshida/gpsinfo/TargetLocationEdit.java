package co.jp.yoshida.gpsinfo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 目的地データの作成編集
 */
public class TargetLocationEdit extends AppCompatActivity
        implements View.OnClickListener {

    private static final String TAG = "TargetLocationEdit";

    private EditText mEdTargetGroup;
    private EditText mEdTargetName;
    private EditText mEdLatitude;
    private EditText mEdLongitude;
    private EditText mEdMemo;
    private Button mBtGroupRef;
    private Button mBtTargetNameRef;
    private Button mBtRemove;
    private Button mBtGroupRemove;
    private Button mBtSave;

    private String mSaveDirectory;                      //  データ保存ディレクトリ
    private String mTargetFileName;                     //  目的地データファイルのパス
    private String mTargetGroup;                        //  目的地のグループ
    private String mTargetName;                         //  目的地名
    private String mLatitude = "";
    private String mLongitude = "";
    private Map<String,String[]> mTargetLocationList;   //  目的地リスト
    private String[] mTargetLocationFormat;             //  目的地データのタイトル
    private String mTargetFilePath;                     //  目的地データファイルのパス
    private String[] mTargetLocationTitle;              //  表示用
    private String[] mTargetGroupList;                  //  目的地グループリスト
    private int mTitlePos;
    private int mGroupPos;
    private int mLatitudePos;
    private int mLongitudePos;
    private int mMemoPos;

    YLib ylib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_target_location_edit);
        ylib = new YLib(this);
        init();

        //	データ保存ディレクトリ
        File extStrageDir = Environment.getExternalStorageDirectory();
        mSaveDirectory = extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DCIM +
                "/" + ylib.getPackageNameWithoutExt() + "/";
        mTargetLocationFormat = GpsInfoActivity.mTargetLocationFormat;
        mTargetFileName = GpsInfoActivity.mTargetFileName;
        //  データリスト
        mTargetLocationList = new HashMap<>();

        //  データの配列位置
        mTitlePos = GpsInfoActivity.getTargetLocationListTitlePos("目的地", mTargetLocationFormat);
        mLatitudePos = GpsInfoActivity.getTargetLocationListTitlePos("緯度", mTargetLocationFormat);
        mLongitudePos = GpsInfoActivity.getTargetLocationListTitlePos("経度", mTargetLocationFormat);
        mGroupPos = GpsInfoActivity.getTargetLocationListTitlePos("グループ", mTargetLocationFormat);
        mMemoPos = GpsInfoActivity.getTargetLocationListTitlePos("メモ", mTargetLocationFormat);

        //  呼び出し元から位置データを取り込む
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            //  外部から起動された場合
            Bundle extras = intent.getExtras();
            if (extras != null) {
                CharSequence ext = extras.getCharSequence(Intent.EXTRA_TEXT);
                if (ext != null) {
                    mEdMemo.setText(ext);
                    setShareData(ext.toString());
                }
            }
        } else {
            mSaveDirectory = intent.getStringExtra("SAVEDIR");          //  データ保存ディレクトリ
            mTargetFileName = intent.getStringExtra("TARGETFILENAME");  //  リストデータ登録ファイル名
            mTargetGroup = intent.getStringExtra("TARGETGROUP");
            mTargetName = intent.getStringExtra("TARGETNAME");
            mLatitude = intent.getStringExtra("LATITUDE");
            mLongitude = intent.getStringExtra("LONGITUDE");
        }

        //  目的地データの取り込み
        loadData();
        if (mLatitude.isEmpty() || mLongitude.isEmpty()) {
            mEdTargetGroup.setText(mTargetGroup);
            mEdTargetName.setText(mTargetName);
            setTargetData();
        } else {
            mEdLatitude.setText(mLatitude);
            mEdLongitude.setText(mLongitude);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == mBtGroupRef.getId()) {
            //  グループ参照
            setTargetGroupList();
            ylib.setMenuDialog(this, "グルーブ参照", mTargetGroupList, iGetGroup);
        } else if (view.getId() == mBtTargetNameRef.getId()) {
            //  目的地参照
            setTargetLocationList();
            ylib.setMenuDialog(this, "目的地参照", mTargetLocationTitle, iGetTargetName);
        } else if (view.getId() == mBtRemove.getId()) {
            //  削除
            removeData();
            finish();
        } else if (view.getId() == mBtGroupRemove.getId()) {
            //  グループ削除
            setTargetGroupList();
            ylib.setMenuDialog(this, "グルーブ削除", mTargetGroupList, iGetGroupRemove);
        } else if (view.getId() == mBtSave.getId()) {
            //  登録
            saveData();
            finish();
        }
    }

    /**
     * 選択されたグループをEditTextに設定する関数インターフェース
     */
    Consumer<String> iGetGroup = new Consumer<String>() {
        @Override
        public void accept(String s) {
            mEdTargetGroup.setText(s);
        }
    };

    /**
     * 選択された目的地名をEditTextに設定する関数インターフェース
     */
    Consumer<String> iGetTargetName = new Consumer<String>() {
        @Override
        public void accept(String s) {
            mEdTargetName.setText(s);
            setTargetData();
        }
    };

    /**
     * 指定グループのデータ削除する関数インターフェース
     */
    Consumer<String> iGetGroupRemove = new Consumer<String>() {
        @Override
        public void accept(String s) {
            removeGroup(s);
        }
    };

    /**
     * 地図ロイドから共有してきたデータの緯度経度を設定
     * @param ext       地図ロイドの共有データ
     */
    private void setShareData(String ext) {
        if (ext != null) {
            String[] text = ext.toString().split("\n");
            if (text.length == 2) {
                String[] text2 = text[1].split(",");
                if (text2.length == 2)
                    mEdLatitude.setText(text2[0]);		//	緯度
                mEdLongitude.setText(text2[1]);		//	経度
            }
        }
    }

    /**
     * 現在選択されているグループと目的地から緯度経度を取得しコントロールに設定
     */
    private void setTargetData() {
        //  現在選択されているグループと目的地
        String group = mEdTargetGroup.getText().toString();
        String location = mEdTargetName.getText().toString();
        //  グループと目的地の組合せを検索
        for (String[] val : mTargetLocationList.values()) {
            if (val[mGroupPos].compareTo(group) == 0 &&
                    val[mTitlePos].compareTo(location) == 0) {
                mEdLatitude.setText(val[mLatitudePos]);
                mEdLongitude.setText(val[mLongitudePos]);
                mEdMemo.setText(ylib.strControlCodeRev(val[mMemoPos]));
                break;
            }
        }
    }

    /**
     * 同一グループの目的地リストを作成
     */
    private void setTargetLocationList() {
        //  現在選択されているグループ名の取得
        String group = mEdTargetGroup.getText().toString();
        //  同一グループの目的地をピックアップ
        List<String> targetList = new LinkedList<>();
        for (String[] val : mTargetLocationList.values()) {
            if (val[mGroupPos].compareTo(group) == 0)
                targetList.add(val[mTitlePos]);
        }
        mTargetLocationTitle = targetList.toArray(new String[targetList.size()]);
    }

    /**
     * 目的地データからグループリストを作成
     */
    private void setTargetGroupList() {
        //  グループの重複登録をなくすためsetに一次登録
        Set<String> targetGroupList = new LinkedHashSet<>();
        for (String[] val : mTargetLocationList.values()) {
            targetGroupList.add(val[mGroupPos]);
        }
        //  表示リストにコピー
        mTargetGroupList = targetGroupList.toArray(new String[targetGroupList.size()]);
    }

    /**
     * 目的地リストから該当グループのデータを削除
     * @param group     削除対象グループ名
     */
    private void removeGroup(String group) {
        //  直接グループを削除すると落ちるのでIteratorを使う
        Iterator<Map.Entry<String, String[]>> iterator = mTargetLocationList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String[]> entry = iterator.next();
            if (entry.getValue()[mGroupPos].compareTo(group)==0) {
                iterator.remove();
            }
        }
        Toast.makeText(this, group+" を削除しました。", Toast.LENGTH_LONG).show();
        saveData();
    }

    /**
     * ファイルからデータを取り込む
     */
    private void loadData() {
        //
        mTargetFilePath = mSaveDirectory + "/" + mTargetFileName;
        List<String[]> fileDarta = new LinkedList<>();
        if (!ylib.existsFile(mTargetFilePath))
            return;
        fileDarta = ylib.loadCsvData(mTargetFilePath, mTargetLocationFormat);

        if (mTargetLocationList == null)
            mTargetLocationList = new HashMap<>();
        for (int i = 0; i < fileDarta.size(); i++) {
            String key = fileDarta.get(i)[mTitlePos] + " " + fileDarta.get(i)[mGroupPos];
            mTargetLocationList.put(key, fileDarta.get(i));
        }
    }

    /**
     * データを登録してファイルに保存
     */
    private void saveData() {
        //  キーコード取得
        mTargetGroup = mEdTargetGroup.getText().toString();
        mTargetName = mEdTargetName.getText().toString();
        if (mTargetGroup.length() == 0 || mTargetGroup.length() == 0){
            return;
        }
        //  データ追加
        String key = mTargetName+ " " +mTargetGroup;
        String[] data = new String[mTargetLocationFormat.length];
        data[mTitlePos] = mTargetName;
        data[mLatitudePos] = mEdLatitude.getText().toString();
        data[mLongitudePos] = mEdLongitude.getText().toString();
        data[mGroupPos] = mTargetGroup;
        data[mMemoPos] = ylib.strControlCodeCnv(mEdMemo.getText().toString());
        if (mTargetLocationList == null)
            mTargetLocationList = new HashMap<>();
        mTargetLocationList.put(key, data);
        //  ファイル保存
        List<String[]> fileData = new LinkedList<>();
        for (String[] val :mTargetLocationList.values()) {
            fileData.add(val);
        }
        ylib.saveCsvData(mTargetFilePath, mTargetLocationFormat, fileData);
    }

    /**
     * データを削除してファイルに保存
     */
    private void removeData() {
        //  キーコード取得
        mTargetGroup = mEdTargetGroup.getText().toString();
        mTargetName = mEdTargetName.getText().toString();
        if (mTargetGroup.length() == 0 || mTargetGroup.length() == 0){
            return;
        }
        //  データ削除
        String key = mTargetName+ " " +mTargetGroup;
        if (mTargetLocationList.containsKey(key))
            mTargetLocationList.remove(key);
        //  ファイル保存
        List<String[]> fileData = new LinkedList<>();
        for (String[] val :mTargetLocationList.values()) {
            fileData.add(val);
        }
        ylib.saveCsvData(mTargetFilePath, mTargetLocationFormat, fileData);
    }

    /**
     * 画面コントロールの初期化
     */
    private void init() {
        mEdTargetGroup = (EditText)findViewById(R.id.editText4);
        mEdTargetName = (EditText)findViewById(R.id.editText5);
        mEdLatitude = (EditText)findViewById(R.id.editText2);
        mEdLongitude = (EditText)findViewById(R.id.editText3);
        mEdMemo = (EditText)findViewById(R.id.editText6);
        mBtGroupRef = (Button)findViewById(R.id.button6);
        mBtTargetNameRef = (Button)findViewById(R.id.button7);
        mBtRemove = (Button)findViewById(R.id.button9);
        mBtGroupRemove = (Button)findViewById(R.id.button20);
        mBtSave = (Button)findViewById(R.id.button8);

        mBtGroupRef.setOnClickListener(this);
        mBtTargetNameRef.setOnClickListener(this);
        mBtRemove.setOnClickListener(this);
        mBtGroupRemove.setOnClickListener(this);
        mBtSave.setOnClickListener(this);

        mEdTargetGroup.setText("");
        mEdTargetName.setText("");
        mEdLatitude.setText("");
        mEdLongitude.setText("");
        mEdMemo.setText("");
    }}