package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class DataListActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "DataListActivity";

    private Spinner mSpYearSelect;
    private Spinner mSpMonthSelect;
    private Spinner mSpCategorySelect;
    private Button mBtAddList;
    private Button mBtAddAllList;
    private Button mBtDispSort;
    private Button mBtGraphView;
    private Button mBtMultiSelect;
    private ListView mLvListData;

    private ArrayAdapter<String> mYearNameAdapter;
    private ArrayAdapter<String> mMonthNameAdapter;
    private ArrayAdapter<String> mCategoryNameAdapter;
    private ArrayAdapter<String> mListDataAdapter;

    private final int MENU00 = 0;
    private final int MENU01 = 1;
    private final int MENU02 = 2;

    private ListData mListData;
    private String mDataDirectory;                      //  GPXデータファイルを保存する基準ディレクトリ
    private String mIndexDirectory;                     //  インデックスデータを保存するディレクトリ
    public static final String FILEHEAD = "GpsMemo";    //  インデックスファイル名のヘッダ
    public static String[] mDataFormat = {              //  インデックスファイルの項目
            "年月日","時間","緯度","経度","高度","移動距離","移動時間","最大高度","最小高度","歩数",
            "分類","GPX","タイトル","メモ"};
    public static String[] mKeyData = {"年月日","時間" };//  インデックスファイルのキーデータ
    private String[] mSortMenu = {"日付","移動距離","移動時間","速度","最大高度","標高差","分類","逆順"};

    private enum TITLESORT {date, distance, lap, maxElevator, elevator, category, speed};
    private TITLESORT mTitleSortType = TITLESORT.date;  //  タイトルのソート種別
    private boolean mSortDownOrder = false;              //  降順ソート

    private boolean mMultiSelectMode = false;           //  選択モード
    private List<String> mTempList;                     //  一時データ
    private int mDigitNo = 2;                           //  小数点以下表示桁数

    private GpsInfoLib gilib;
    private YLib ylib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_data_list);

        Log.d(TAG,"onCreate: ");
        ylib = new YLib(this);
        mDataDirectory = ylib.getPackageNameDirectory();
        init();

        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                CharSequence ext = extras.getCharSequence(Intent.EXTRA_TEXT);
                if (ext != null) {
                }
            }
        } else {
            //  呼び出し元からのデータ
            mDataDirectory = intent.getStringExtra("SAVEDIR");
//            mListData.setSaveDirectory(mDataDirectory);
        }
        mIndexDirectory = mDataDirectory + "/Memo";
        if (!ylib.mkdir(mIndexDirectory))
            mIndexDirectory = mDataDirectory;

        gilib = new GpsInfoLib(this, mDataDirectory);
        setYearTitle();
        mListData = new ListData(this, mDataFormat);
        mListData.setKeyData(mKeyData);

        //  測定の中断以外でprferenceに測定中のGPXファイル名が残っていればリストに登録
        if (!gilib.getGpxDataContinue()) {
            String gpxFileName = gilib.getDataFileName();
            if (7 < gpxFileName.length()) {
                try {
                    String key = gilib.findIndexFile(gpxFileName.substring(0, 8), gpxFileName, "GPX", mDataFormat, mKeyData, mIndexDirectory);
                    String[] data = gilib.getGpxData(gilib.getGpxPath(gpxFileName), mDataFormat);
                    if (key.length() < 1) {
                        //  新規作成
                        if (data != null)
                            gilib.registIndexFile(data, mDataFormat, mKeyData, mIndexDirectory);
                    } else {
                        //  既存データの更新(更新しないデータは空白にする)
                        if (data != null) {
                            data[mListData.getTitlePos("歩数")] = "";
                            data[mListData.getTitlePos("分類")] = "";
                            data[mListData.getTitlePos("GPX")] = "";
                            data[mListData.getTitlePos("タイトル")] = "";
                            data[mListData.getTitlePos("メモ")] = "";
                            gilib.updateIndexFile(key, data, mDataFormat, mKeyData, mIndexDirectory);
                        }
                    }
                    gilib.clearDataFileName();
                } catch (Exception e) {
                    Toast.makeText(this, "Continue Erroe: "+e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }

        //  指定年のインデックスファイルをリスト表示する
        if (0 < getCurYear().length()) {
            String indexFile = gilib.getLoadIndexFileName(getCurYear());
            mListData.setSaveDirectory(mIndexDirectory, indexFile);
            loadData();
        }

        //  年の選択処理
        mSpYearSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadData(getCurYear());
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  月の選択処理
        mSpMonthSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadData(getCurYear(), getCurMonth(), getCurCategory());
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  分類の選択処理
        mSpCategorySelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loadData(getCurYear(), getCurMonth(), getCurCategory());
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  項目の選択処理
        mLvListData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (!mMultiSelectMode) {
                    ListView listView = (ListView) adapterView;
                    String item = (String) listView.getItemAtPosition(position);
                    String date = item.substring(0,8);
                    String time = item.substring(9,17);
                    goLocMemo(date, time);
                }
            }
        });
    }

    /**
     * ボタン処理
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        Button button =(Button)view;
        if (button.getText().toString().compareTo("個別追加")==0) {
            ylib.fileSelectDialog(mDataDirectory, "*.gpx", true, iAddData);
        } else if (button.getText().toString().compareTo("一括追加")==0) {
            ylib.folderSelectDialog(this, mDataDirectory, iAddAllData);
        } else if (button.getText().toString().compareTo("表示順")==0) {
            ylib.setMenuDialog(this, "タイトルのソート", mSortMenu, iTitleSort);
        } else if (button.getText().toString().compareTo("グラフ")==0) {
            goListGraph();
        } else if (button.getText().toString().compareTo("削除")==0) {
            removeSelectData();
        } else if (button.getText().toString().compareTo(" 選 択 ")==0) {
            setSelectMode(!mMultiSelectMode);
            setButton(mMultiSelectMode);
        }
    }

    /**
     * ボタンの長押し処理
     * @param view The view that was clicked and held.
     *
     * @return
     */
    @Override
    public boolean onLongClick(View view) {
        Button button =(Button)view;
        if (button.getText().toString().compareTo("一括追加")==0) {
            ylib.folderSelectDialog(this, mDataDirectory, iAddAllData);
        }
        return true;
//        return false;
    }

    //  Activityの返値の処理
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == GpsInfoActivity.LOCMEMO_ACTIVITY) {                //	データ登録画面
            loadData(getCurYear(), getCurMonth(), getCurCategory());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //  オプションメニューの設定
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item2 = menu.add(Menu.NONE, MENU02, Menu.NONE, "インポート:インデックスファイル");
        MenuItem item1 = menu.add(Menu.NONE, MENU01, Menu.NONE, "インポート:データファイル");
        MenuItem item0 = menu.add(Menu.NONE, MENU00, Menu.NONE, "ヘルプ");
        item2.setIcon(android.R.drawable.ic_menu_upload);
        item1.setIcon(android.R.drawable.ic_menu_upload);
        item0.setIcon(android.R.drawable.ic_menu_help);
        return super.onCreateOptionsMenu(menu);
    }

    //  オプションメニューの処理
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case MENU00:            //	ヘルプ
                break;
            case MENU01 :           //  インポートデータファイル
//                ylib.folderSelectDialog(this, mDataDirectory, iDataImport);
                break;
            case MENU02 :           //  インポートインデックスファイル
//                ylib.fileSelectDialog(mDataDirectory,FILEHEAD+"_*.csv", true, iImport);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * データ編集画面を起動する
     * @param date      キーデータの日付(yyyymmdd)
     * @param time      キーデータの時間(HH:mm:ss)
     */
    private void goLocMemo(String date, String time) {
        Intent intent = new Intent(this, LocMemoActivity.class);
        intent.putExtra("SAVEDIR", mDataDirectory);     //	保存ディレクトリ
        intent.putExtra("INDEXDIR", mIndexDirectory);   //	データファイル名
        intent.putExtra("DATE", date);                  //	現在日時(yyyymmdd)
        intent.putExtra("TIME", time);                  //	時間(HH:mm:ss)

        startActivityForResult(intent, GpsInfoActivity.LOCMEMO_ACTIVITY);
    }

    /**
     * リストのグラフ表示を起動する
     */
    private void goListGraph() {
        Intent intent = new Intent(this, ListGraphActivity.class);
        intent.putExtra("INDEXDIR", mIndexDirectory);   //	データファイル名
        intent.putExtra("YEAR", getCurYear());	    //	表示年
        intent.putExtra("CATEGORY", getCurCategory());//	表示分類
        startActivity(intent);
    }

    /**
     * 選択されたデータをリストから削除する
     */
    private void removeSelectData() {
        if (!mMultiSelectMode)
            return;
        mTempList = new ArrayList<>();
        SparseBooleanArray checked = mLvListData.getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
            String title = mListDataAdapter.getItem(checked.keyAt(i));
            String key = title.substring(0,8) + title.substring(9,17);
            mTempList.add(mListData.getData(key, "GPX"));
            mListData.removeData(key);
        }
        mListData.saveDataFile();
        mLvListData.clearChoices();
        loadData(getCurYear(), getCurMonth(), getCurCategory());
        ylib.messageDialog(this, "削除確認", "GPXデータも削除しますか", iRemoveData);
    }

    /**
     * GPXファイルを削除する関数インターフェース
     */
    Consumer<String> iRemoveData = new Consumer<String>() {
        @Override
        public void accept(String s) {
            for (String gpxName : mTempList) {
                String path = gilib.getGpxPath(gpxName);
                if (path !=null)
                    ylib.deleteFile(path);
            }
        }
    };

    /**
     * 個別にGPXファイルを追加する関数インターフェース
     */
    Consumer<String> iAddData = new Consumer<String>() {
        @Override
        public void accept(String s) {
            String path = s;
            String fileName = ylib.getNameWithoutExt(path);
            //  GpsInfo2外のファイルであればコピーして登録する
            if (gilib.getGpxPath(fileName) == null) {
                ylib.copyFile(path, mDataDirectory);
                path = mDataDirectory + "/" + fileName + ".gpx";
            }
            Log.d(TAG, "iAddData: " + s + " " + path);
            String[] data = gilib.getGpxData(path, mDataFormat, false);
            if (data != null)
                gilib.registIndexFile(data, mDataFormat, mKeyData, mIndexDirectory);
            loadData(getCurYear(), getCurMonth(), getCurCategory());
        }
    };

    /**
     * フォルダを指定してGPXファイルを追加する関数インターフェース
     */
    Consumer<String> iAddAllData = new Consumer<String>() {
        @Override
        public void accept(String s) {
            AddAllData(s, true);
        }
    };

    /**
     * タイトルのソートする関数インターフェース
     */
    Consumer<String> iTitleSort = new Consumer<String>() {
        @Override
        public void accept(String s) {
            if (s.compareTo("日付")==0) {
                mTitleSortType =TITLESORT.date;
            } else if (s.compareTo("移動距離")==0) {
                mTitleSortType =TITLESORT.distance;
            } else if (s.compareTo("移動時間")==0) {
                mTitleSortType =TITLESORT.lap;
            } else if (s.compareTo("最大高度")==0) {
                mTitleSortType =TITLESORT.maxElevator;
            } else if (s.compareTo("標高差")==0) {
                mTitleSortType =TITLESORT.elevator;
            } else if (s.compareTo("分類")==0) {
                mTitleSortType =TITLESORT.category;
            } else if (s.compareTo("速度")==0) {
                mTitleSortType =TITLESORT.speed;
            } else if (s.compareTo("逆順")==0) {
                mSortDownOrder = !mSortDownOrder;
            }
            loadData(getCurYear(), getCurMonth(), getCurCategory());
        }
    } ;

    /**
     * インデックスファイルをインポートする関数インターフェース
     */
    Consumer<String> iImport = new Consumer<String>() {
        @Override
        public void accept(String s) {
            gilib.importIndexData(s, mDataFormat, mKeyData, mIndexDirectory);
            loadData();
        }
    };

    /**
     * データファイルをインポートする関数インターフェース
     */
    Consumer<String> iDataImport = new Consumer<String>() {
        @Override
        public void accept(String s) {
            gilib.importDataFile(s, getCurYear(), mDataFormat, mKeyData, mIndexDirectory, mDataDirectory);
            loadData(getCurYear());
        }
    };

    /**
     * 対象ディレクトリ以下のGPXファイルを検索してインデックスファイルに追加する
     * データの重複は行わない
     * @param path      検索するフォルダパス
     * @param subdir    サブディレクトリも検索するか
     */
    private void AddAllData(String path, boolean subdir) {
        Log.d(TAG,"AddAllData: "+path);
        List<String> fileList = ylib.getFileList(path, "*.gpx", subdir);
        for (int i = 0; i < fileList.size(); i++) {
            String filepath = fileList.get(i);
            String fileName = ylib.getNameWithoutExt(filepath);
            //  GpsInfo2外のファイルであればコピーして登録する
            if (gilib.getGpxPath(fileName) == null) {
                ylib.copyFile(filepath, mDataDirectory);
                filepath = mDataDirectory + "/" + fileName + ".gpx";
            }
            Log.d(TAG, "AddAllData: " + fileList.get(i) + " " + filepath);
            String[] data = gilib.getGpxData(filepath, mDataFormat,false);
            if (data != null)
                gilib.registIndexFile(data, mDataFormat, mKeyData, mIndexDirectory);
        }
        loadData(getCurYear());
    }

    /**
     * インデックスファイルが存在するリストを作成し、ファイルからデータを読み込んでリストビューに表示する
     */
    private void loadData() {
        setYearTitle();
        loadData(getCurYear());
    }

    /**
     * 対象年を指定してファイルからデータを読込リストビューに表示し、月と分類をドロップダウンリストに設定する
     * @param year      対象年
     */
    private void loadData(String year) {
        Log.d(TAG,"LoadData: "+year);
        if (0 < year.length()) {
            loadData(year, "", "");
            setMonthTitle();
            setCategoryTitle();
        }
    }

    /**
     * ファイルからデータを読み込んでリストビューに表示する
     * @param year          対象年
     * @param month         対象月
     * @param category      対象分類
     */
    private void loadData(String year, String month, String category) {
        mListData.setSaveDirectory(mIndexDirectory, gilib.getLoadIndexFileName(year));
        mListData.LoadFile();
        //  フィルターの設定
        mListData.resetFilter();
        if (!month.isEmpty() && month.compareTo("全の月") != 0) {
            if (month.length() == 1)
                month = "0" + month;
            mListData.setFilter("年月日", "...." + month + "..");  //  正規表現
        }
        if (!category.isEmpty() && category.compareTo("全の分類") != 0) {
            mListData.setFilter("分類", "^" + category);  //  正規表現
        }
        //  ソートの設定
        List<String[]> listdata = mListData.getListData();
        listdata.sort(new Comparator<String[]>() {
            @Override
            public int compare(String[] df, String[] ds) {
                String sf = "";
                String ss = "";
                double vf = 0;
                double vs = 0;
                boolean textType = true;
                if (mTitleSortType == TITLESORT.date) {
                    textType = true;
                    sf = df[mListData.getTitlePos("年月日")] + df[mListData.getTitlePos("時間")];
                    ss = ds[mListData.getTitlePos("年月日")] + ds[mListData.getTitlePos("時間")];
                } else if (mTitleSortType == TITLESORT.distance) {
                    textType = false;
                    vf = ylib.str2Double(df[mListData.getTitlePos("移動距離")]);
                    vs = ylib.str2Double(ds[mListData.getTitlePos("移動距離")]);
                } else if (mTitleSortType == TITLESORT.lap) {
                    textType = false;
                    vf = ylib.Time2Sec(df[mListData.getTitlePos("移動時間")]);
                    vs = ylib.Time2Sec(ds[mListData.getTitlePos("移動時間")]);
                } else if (mTitleSortType == TITLESORT.elevator) {
                    textType = false;
                    vf = ylib.str2Double(df[mListData.getTitlePos("最大高度")])
                            - ylib.str2Double(df[mListData.getTitlePos("最小高度")]);
                    vs = ylib.str2Double(ds[mListData.getTitlePos("最大高度")])
                            - ylib.str2Double(ds[mListData.getTitlePos("最小高度")]);
                } else if (mTitleSortType == TITLESORT.maxElevator) {
                    textType = false;
                    vf = ylib.str2Double(df[mListData.getTitlePos("最大高度")]);
                    vs = ylib.str2Double(ds[mListData.getTitlePos("最大高度")]);
                } else if (mTitleSortType == TITLESORT.category) {
                    textType = true;
                    sf = df[mListData.getTitlePos("分類")];
                    ss = ds[mListData.getTitlePos("分類")];
                } else if (mTitleSortType == TITLESORT.speed) {
                    textType = false;
                    double disf = ylib.str2Double(df[mListData.getTitlePos("移動距離")]);
                    double diss = ylib.str2Double(ds[mListData.getTitlePos("移動距離")]);
                    double lapf = ylib.Time2Sec(df[mListData.getTitlePos("移動時間")]);
                    double laps = ylib.Time2Sec(ds[mListData.getTitlePos("移動時間")]);
                    if (0 < lapf && 0 <laps) {
                        vf = disf / lapf;
                        vs = diss / laps;
                    }
                }
                if (textType) {
                    if (mSortDownOrder) {
                        return sf.compareTo(ss);
                    } else {
                        return ss.compareTo(sf);
                    }
                } else {
                    if (mSortDownOrder) {
                        return (int)Math.signum(vf - vs);
                    } else {
                        return (int)Math.signum(vs - vf);
                    }
                }
            }
        });
        //  リストビューに登録
        mListDataAdapter.clear();
        for (int i = 0; i < listdata.size(); i++) {
            mListDataAdapter.add(makeTitle(listdata.get(i)));
        }
    }

    /**
     * リストビューに表示するタイトルを作る
     * @param data      GPXデータ
     * @return          タイトル
     */
    private String makeTitle(String[] data) {
        String title = data[mListData.getTitlePos("年月日")] + " " +
                data[mListData.getTitlePos("時間")] + "  " + data[mListData.getTitlePos("タイトル")];
        double dis = ylib.str2Double(data[mListData.getTitlePos("移動距離")]);                //  距離(km)
        double lap = (double) ylib.Time2Sec(data[mListData.getTitlePos("移動時間")]) / 3600.; //  経過時間(h)
        title += "\n";
        title += "[" + data[mListData.getTitlePos("分類")] +"]";
        title += "  " + String.format("%.1fkm(%s)", dis, data[mListData.getTitlePos("移動時間")]);
        if (0 < dis && 0 < lap) {
            title += "  " + String.format("%.1fkm/h(%.1fmin/km)", dis / lap, lap * 60 / dis);
        }
        title += "  " + String.format("%,.0fm-%,.0fm",ylib.str2Double(data[mListData.getTitlePos("最小高度")]),
                ylib.str2Double(data[mListData.getTitlePos("最大高度")]));
        return title;
    }

    /**
     * フィルタに表示されている年を取得
     * @return
     */
    private String getCurYear() {
        int l = mSpYearSelect.getSelectedItemPosition();
        return (0<=l?mYearNameAdapter.getItem(l).substring(0, 4):"");
    }

    /**
     * フィルタに表示されている月を取得
     * @return
     */
    private String getCurMonth() {
        int l = mSpMonthSelect.getSelectedItemPosition();
        return (0<l?mMonthNameAdapter.getItem(l).substring(0, mMonthNameAdapter.getItem(l).indexOf("月")):"");
    }

    /**
     * フィルタに表示されている分類を取得
     * @return
     */
    private String getCurCategory() {
        int l = mSpCategorySelect.getSelectedItemPosition();
        return (0<l?mCategoryNameAdapter.getItem(l):"");
    }

    /**
     *  リストデータファイルを検索し年のドロップリストに登録する
     */
    private void setYearTitle() {
        //	ファイルリストの取得
        List<String> fileList = gilib.getFileList(mIndexDirectory);				//	保存データファイルリストの取得
        if (fileList == null)
            return;
        //  年フィルタにデータを設定
        mYearNameAdapter.clear();
        for (int i=0; i< fileList.size(); i++) {
            setYearTitle(gilib.getYear(fileList.get(i)));
        }
    }

    /**
     * 年フィルタにデータを設定する
     * @param date      年月日(yyyymmdd)
     */
    private void setYearTitle(String date) {
        if (date.length()<4)
            return ;
        if (mYearNameAdapter.getPosition(date.substring(0, 4)) < 0)
            mYearNameAdapter.add(date.substring(0, 4)+"年");
        mSpYearSelect.setSelection(mYearNameAdapter.getCount() - 1);
    }

    /**
     * 月フィルタ(ドロップダウンリスト)にデータを設定する
     * 月データは登録データから取得
     */
    private void setMonthTitle() {
        List<String> monthList = getMonthList();
        mMonthNameAdapter.clear();
        mMonthNameAdapter.add("全の月");
        for (int i=0; i<monthList.size(); i++) {
            if (mMonthNameAdapter.getPosition(monthList.get(i))<0) {
                mMonthNameAdapter.add(monthList.get(i)+"月");
            }
        }
        mSpMonthSelect.setSelection(0);
    }

    /**
     * GPXのリストデータから月データを抽出してリスト化する
     * @return
     */
    private List<String> getMonthList() {
        //  年月日のデータを抽出
        List<String> dateList = mListData.getListData("年月日");
        List<String> monthList  = new ArrayList<>();
        if (dateList == null)
            dateList = new LinkedList<String>();
        if (dateList.size() == 0)
            return monthList;
        //  抽出したデータをソート
        dateList.sort(new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareTo(t1);
            }
        });
        //  抽出したデータをsqueezeしてList化
        for (int i = 0; i < dateList.size(); i++) {
            if (dateList.get(i).length() < 6)
                continue;
            String month = dateList.get(i).substring(4,6);
            if (month.charAt(0)=='0')
                month =month.substring(1);
            if (!monthList.contains(month))
                monthList.add(month);

        }
        return monthList;
    }

    /**
     * 分類フィルタ(ドロップダウンリスト)に分類データを設定する
     */
    private void setCategoryTitle() {
        List<String> categoryList = getCategoryList();
        mCategoryNameAdapter.clear();
        mCategoryNameAdapter.add("全の分類");
        for (int i=0; i<categoryList.size(); i++) {
            if (mCategoryNameAdapter.getPosition(categoryList.get(i))<0) {
                mCategoryNameAdapter.add(categoryList.get(i));
            }
        }
        mSpMonthSelect.setSelection(0);
    }

    /**
     * 分類データを抽出してリスト化する
     * @return
     */
    private List<String> getCategoryList() {
        //  分類のデータを抽出
        List<String> dataList = mListData.getListData("分類");
        List<String> categoryList  = new ArrayList<>();
        if (dataList == null || dataList.size() == 0)
            return categoryList;
        //  抽出したデータをソート
        dataList.sort(new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareTo(t1);
            }
        });
        //  抽出したデータをsqueezeしてList化
        for (int i = 0; i < dataList.size(); i++) {
            if (!categoryList.contains(dataList.get(i)))
                categoryList.add(dataList.get(i));

        }
        return categoryList;
    }

    /**
     * リストビューを複数選択に切り替えるか元に戻す
     * @param multi     true : multi select
     */
    private void setSelectMode(boolean multi) {
        //mListView.
        String category = getCurCategory();
        if (multi) {
            mListDataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked);
            mLvListData.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else {
            mListDataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        }
        mLvListData.setAdapter(mListDataAdapter);
        mMultiSelectMode = multi;
        loadData(getCurYear(), getCurMonth(), getCurCategory());
    }

    /**
     * 複数選択モードによってボタンの表示を切り替える
     * @param multiSelect     複数選択モード
     */
    private void setButton(boolean multiSelect) {
        mBtAddList.setEnabled(!multiSelect);
        mBtAddAllList.setEnabled(!multiSelect);
        mBtGraphView.setText(multiSelect?"削除":"グラフ");
    }

    /**
     * 画面データの初期化
     */
    private void init() {
        //  IDの取得
        mLvListData = (ListView) findViewById(R.id.listView);
        mSpYearSelect = (Spinner) findViewById(R.id.spinner2);
        mSpMonthSelect = (Spinner) findViewById(R.id.spinner3);
        mSpCategorySelect = (Spinner) findViewById(R.id.spinner4);
        mBtAddList = (Button) findViewById(R.id.button10);
        mBtAddAllList = (Button) findViewById(R.id.button11);
        mBtDispSort = (Button) findViewById(R.id.button12);
        mBtGraphView = (Button) findViewById(R.id.button13);
        mBtMultiSelect = (Button) findViewById(R.id.button14);

        //  ドロップダウン、リストビューの設定
        mYearNameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpYearSelect.setAdapter(mYearNameAdapter);
        mMonthNameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpMonthSelect.setAdapter(mMonthNameAdapter);
        mCategoryNameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpCategorySelect.setAdapter(mCategoryNameAdapter);
        mListDataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mLvListData.setAdapter(mListDataAdapter);

        //  ボタンリスナーの設定
        mBtAddList.setOnClickListener(this);
        mBtAddAllList.setOnClickListener(this);
        mBtDispSort.setOnClickListener(this);
        mBtGraphView.setOnClickListener(this);
        mBtMultiSelect.setOnClickListener(this);
        mBtAddAllList.setOnLongClickListener(this);
    }

}