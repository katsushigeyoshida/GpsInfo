package co.jp.yoshida.gpsinfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

/**
 * GPXファイルデータのグラフ表示
 */
public class GpxGraphActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "GpxGraphActivity";

    private Spinner mSpGraphType;                           //  グラフの種類
    private Spinner mSpFilterType;                          //  フィルタの種類
    private Spinner mSpFilterDataCount;                     //  フィルタのデータ数
    private CheckBox mCbHoldTime;                           //  滞留時間削除
    private GpxGraphView mGpxGraphView;                     //  GPXをグラフ表示するView
    private ArrayAdapter<String> mGraphTypeAdapter;         //  グラフの種別
    private ArrayAdapter<String> mFilterTypeAdapter;        //  フィルタの種別
    private ArrayAdapter<String> mFilterDataCountAdapter;   //  フィルタの丸目データ数

    //  Spinnerのメニュータイトル
    private String[] mGraphTitle = {"距離/時間","速度/時間","高度/時間","時間/距離","速度/距離","高度/距離"};
    private String[] mFilterType = {"フィルタなし","移動平均","中央値","ローパス"};
    private String[] mMoveAveTitle = {"移動平均なし","3データ","5データ","9データ","17データ","33データ","65データ","129データ"};
    private String[] mMedianTitle = {"中央値なし","3データ","5データ","9データ","17データ","31データ","65データ","129データ"};
    private String[] mLowPassTtle = {"ローパスなし","0.90","0.80","0.70","0.60","0.50","0.40","0.30","0.20","0.10","0.05","0.00"};

    enum GRAPHTYPE {DisTime, SpeedTime, EleTime, TimeDis, SpeedDis, EleDis }
    private boolean mFilterSelectEnable = true;

    private final int MENU01 = 1;       //  追加読込
    private final int MENU02 = 2;       //  滞留時間削除
    private final int MENU09 = 9;       //  ヘルプ

    private String mGpxPath;

    private YLib ylib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gpx_graph);

        //  初期化
        ylib = new YLib(this);
        init();
        getFilterPreference();

        //	表示データファイル名の取得
        Intent intent = getIntent();
        mGpxPath = intent.getStringExtra("FILEPATH");
        setTitle(ylib.getNameWithoutExt(mGpxPath));

        //  GPXファイルを読み込みと初期値設定
        mGpxGraphView.setGpxFileRead(mGpxPath);
        mGpxGraphView.setGraphType(mGraphTitle[0]);
        mGpxGraphView.setFilter(mFilterType[0], 0, 1.0f);

        //  グラフの種別選択
        mSpGraphType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mFilterSelectEnable) {
                    String graphType = mGraphTypeAdapter.getItem(mSpGraphType.getSelectedItemPosition());
                    if (mGpxGraphView.setGraphType(graphType))
                        mGpxGraphView.dispGraph();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //  フィルタタイプ(移動平均,中央値,ローバス)の選択
        mSpFilterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mFilterSelectEnable) {
                    setFilterDataCountMenu(mFilterTypeAdapter.getItem(mSpFilterType.getSelectedItemPosition()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //  フィルタの設定のデータ数の選択
        mSpFilterDataCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mFilterSelectEnable) {
                    setFilterDatacount();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    //  滞留時間削除の可否
    @Override
    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
        mGpxGraphView.setHoldTimeOut(mCbHoldTime.isChecked());  //  滞留時間削除
        mGpxGraphView.dispGraph();
    }

    @Override
    protected void onDestroy() {
        setFilterPreference();
        super.onDestroy();
    }

    //  オプションメニューの設定(追加読込/滞留時間削除/ヘルプ)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item1 = menu.add(Menu.NONE, MENU01, Menu.NONE, "追加読込");
        MenuItem item2 = menu.add(Menu.NONE, MENU02, Menu.NONE, "滞留時間削除");
        MenuItem item9 = menu.add(Menu.NONE, MENU09, Menu.NONE, "ヘルプ");
        item1.setIcon(android.R.drawable.ic_menu_upload);
        item2.setIcon(android.R.drawable.ic_menu_upload);
        item9.setIcon(android.R.drawable.ic_menu_help);
        return super.onCreateOptionsMenu(menu);
    }

    //  オプションメニューの実行
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case MENU09:                         //	ヘルプ
                break;
            case MENU01:                         //	追加データ
                ylib.fileSelectDialog(ylib.getDir(mGpxPath),"*.gpx", true, iAddGpxData);
                break;
            case MENU02:                        //  滞留時間削除
                mGpxGraphView.setHoldTimeOutReverse();
                mGpxGraphView.dispGraph();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * GPXファイルのデータを追加してグラフを再表示する関数インターフェース
     */
    Consumer<String> iAddGpxData = new Consumer<String>() {
        @Override
        public void accept(String s) {
            mGpxGraphView.setGpxFileRead(s, true);
            mGpxGraphView.dispGraph();
        }
    };

    /**
     * フィルタの種類に応じてデータ数のメニューを設定する
     * @param filterName フィルタの種類
     */
    private void setFilterDataCountMenu(String filterName) {
        int pos = mSpFilterDataCount.getSelectedItemPosition();
        String[] filterDataCountMenu = {""};
        if (filterName.compareTo(mFilterType[0])==0) {

        } else if (filterName.compareTo(mFilterType[1])==0) {
            filterDataCountMenu = mMoveAveTitle;
        } else if (filterName.compareTo(mFilterType[2])==0) {
            filterDataCountMenu = mMedianTitle;
        } else if (filterName.compareTo(mFilterType[3])==0) {
            filterDataCountMenu = mLowPassTtle;
        }
        mFilterDataCountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterDataCountMenu);
        mSpFilterDataCount.setAdapter(mFilterDataCountAdapter);
        mSpFilterDataCount.setSelection(Math.min(pos, mFilterDataCountAdapter.getCount() - 1));
    }

    /**
     * フィルタのデータ数の設定
     */
    private void setFilterDatacount() {
        String filterType = mFilterTypeAdapter.getItem(mSpFilterType.getSelectedItemPosition());
        float passRate = 1f;    //  ローバスの設定値
        int count = 0;          //  移動平均と中央値のデータ数
        int pos = Math.min(mSpFilterDataCount.getSelectedItemPosition(), mFilterDataCountAdapter.getCount() - 1);
        if (0 < pos) {
            //  データ数を設定
            String dataCount = mFilterDataCountAdapter.getItem(mSpFilterDataCount.getSelectedItemPosition());
            if (filterType.compareTo("ローパス")==0) {
                passRate = Float.parseFloat(dataCount.replaceAll("[^.0-9]", ""));
            } else if (filterType.compareTo("移動平均")==0 || filterType.compareTo("中央値")==0) {
                count = Integer.parseInt(dataCount.replaceAll("[^0-9]", ""));
            }
            if (mGpxGraphView.setFilter(filterType, count, passRate))
                mGpxGraphView.dispGraph();
        }
    }

    /**
     * 	Preferenceから設定値の取得
     */
    private void getFilterPreference() {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //  Preferenceからデータ取得
        String graphType  = prefs.getString("GpxGraphType", mGraphTitle[0]);
        String filterType = prefs.getString("GpxFilterType", mFilterType[0]);
        String dataCount  = prefs.getString("GpxDataCount", "");
        //  取得データをspinnerに設定
        mFilterSelectEnable = false;
        mSpGraphType.setSelection(getSelectPosition(mSpGraphType, graphType));
        mSpFilterType.setSelection(getSelectPosition(mSpFilterType, filterType));
        setFilterDataCountMenu(filterType);
        mSpFilterDataCount.setSelection(getSelectPosition(mSpFilterDataCount, dataCount));
        mFilterSelectEnable = true;
        //  フィルタデータ値の設定
        float passRate = 1f;    //  ローバスの設定値
        int count = 0;          //  移動平均と中央値のデータ数
        if (0 < dataCount.length() && dataCount.matches(".*[0-9]+.*")) {
            if (filterType.compareTo("ローパス")==0) {
                passRate = Float.parseFloat(dataCount.replaceAll("[^.0-9]", ""));
            } else if (filterType.compareTo("移動平均")==0 || filterType.compareTo("中央値")==0) {
                count = Integer.parseInt(dataCount.replaceAll("[^0-9]", ""));
            }
        }
        mGpxGraphView.setGraphType(graphType);
        mGpxGraphView.setFilter(filterType, count, passRate);
    }

    /**
     * Preferenceにデータを書き込む
     */
    private void setFilterPreference() {
        //  Spinnerの値を取得
        String graphType  = mGraphTypeAdapter.getItem(mSpGraphType.getSelectedItemPosition());
        String filterType = mFilterTypeAdapter.getItem(mSpFilterType.getSelectedItemPosition());
        String dataCount  = mFilterDataCountAdapter.getItem(mSpFilterDataCount.getSelectedItemPosition());
        //  Preferenceに設定
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        //  データ保存
        editor.putString("GpxGraphType", graphType);
        editor.commit();
        editor.putString("GpxFilterType", filterType);
        editor.commit();
        editor.putString("GpxDataCount", dataCount);
        editor.commit();
    }

    /**
     * Spinnerの選択一の取得
     * @param spinner : Spinnerオブジェクト
     * @param item    : 選択アイテム
     * @return        : 選択位置
     */
    private int getSelectPosition(Spinner spinner, String item) {
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (spinner.getAdapter().getItem(i).toString().compareTo(item) == 0)
                return  i;
        }
        return  0;
    }

    /**
     * 画面の初期化
     */
    private void init() {
        mSpGraphType = (Spinner)findViewById(R.id.spinner6);
        mSpFilterType = (Spinner)findViewById(R.id.spinner7);
        mSpFilterDataCount = (Spinner)findViewById(R.id.spinner8);
        mCbHoldTime = (CheckBox)findViewById(R.id.checkBox);
        mCbHoldTime.setOnCheckedChangeListener(this);
        mCbHoldTime.setChecked(false);
        //  グラフィックViewの設定
        mGpxGraphView = new GpxGraphView(this);
        LinearLayout linearlayout = (LinearLayout)findViewById(R.id.linearLayoutGpx);
        linearlayout.addView(mGpxGraphView);
        //  ドロップダウンリストの設定
        mGraphTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mGraphTitle);
        mSpGraphType.setAdapter(mGraphTypeAdapter);
        mFilterTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mFilterType);
        mSpFilterType.setAdapter(mFilterTypeAdapter);
    }
}