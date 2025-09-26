package co.jp.yoshida.gpsinfo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListGraphActivity extends AppCompatActivity {
    private static final String TAG = "ListGraphActivity";

    private Spinner mSpYear;            //  年
    private Spinner mSpMonth;           //  月
    private Spinner mSpSpan;            //  期間
    private Spinner mSpCollectUnit;     //  集計単位
    private Spinner mSpDataType;        //  測定種類
    private Spinner mSpCategory;        //  分類
    private ListGraphView mListGraphView;

    private ArrayAdapter<String> mYearAdapter;
    private ArrayAdapter<String> mMonthAdapter;
    private ArrayAdapter<String> mSpanAdapter;
    private ArrayAdapter<String> mCollectUnitAdapter;
    private ArrayAdapter<String> mDataTypeAdapter;
    private ArrayAdapter<String> mCategoryAdapter;

    private String mIndexDirectory;
    private ListData mListData;
    private String[] mSpanMenu = {
            "年","半年","3ヶ月","1ヶ月"};
    private String[] mCollectUnitMenu = {
            "回","日","週","月"};
    private String[] mDataTypeMenu = {
            "移動距離","移動時間","速度","最大高度","累積標高差","歩数"};
    private List<float[]> mGraphData = new ArrayList<>();
    private String mStartDate;
    private String mLastDate;
    private float mStartDay;
    private float mLastDay;
    private int mStartMonth = 0;
    private int mSpanMonth = 0;
    private String mCollectUnit = "回";
    private String mDataType;
    private String mCategory;
    private String mYear;

    private GpsInfoLib gilib;
    private YLib ylib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_graph);

        gilib = new GpsInfoLib(this);
        ylib = new YLib(this);
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
            mIndexDirectory = intent.getStringExtra("INDEXDIR");        //  INDEXファイル名
            mYear = intent.getStringExtra("YEAR");
            mCategory = intent.getStringExtra("CATEGORY");
        }
        setYearTitle();
        mListData = new ListData(this, DataListActivity.mDataFormat);
        mListData.setKeyData(DataListActivity.mKeyData);

        //  対象年の変更
        mSpYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setMmenu(getCurYear());
                graphView();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  開始月の変更
        mSpMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mStartMonth != getCurMonth())
                    graphView();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  集計期間の変更
        mSpSpan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setMonthTitle();
                if (mSpanMonth != getCurSpan()) {
                    //  データの最終日が表示されるように回月を設定して再表示する
                    String lastDate = getLastDateData();
                    int lastMonth = Integer.valueOf(lastDate.substring(4, 6));
                    int startMonth = lastMonth - getCurSpan();
                    mSpMonth.setSelection(startMonth > 0 ? startMonth : 0);
                    graphView();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  集計単位の変更
        mSpCollectUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mCollectUnit != getCurCollectUnit())
                    graphView();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  データの種類の変更
        mSpDataType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mDataType != getCurDataType())
                    graphView();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //  分類の変更
        mSpCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mCategory != getCurCategory())
                    graphView();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    /**
     * データのグラフ表示
     */
    private void graphView() {
        //  ファイルからデータの取り込み
        loadFile(getCurYear());
        //  グラフ表示用にデータの取得とパラメータの設定
        getGraphData(getCurYear(), getCurMonth(), getCurSpan(), getCurCategory());
        //  グラフにパラメータ設定
        mListGraphView.setParameter(getCurDataType(), getCurSpan(), getCurCollectUnit(), mStartDate, mLastDate);
        //  表示用のデータを作成
        setPlotData(getCurDataType(), getCurCollectUnit());
        //  グラフ表示
        mListGraphView.dispGraph();
    }

    /**
     * 表示用データの取得(テキストデータを数値データに変換)
     * @param year          対象年
     * @param month         表示開始月
     * @param span          表示期間
     * @param category      対象分類
     */
    private void getGraphData(String year, int month, int span, String category) {
        String[] keyArray = mListData.getKeyData();
        mStartMonth = month;
        mSpanMonth  = span;
        mCategory   = category;
        mStartDate = year.substring(0,4)+String.format("%02d01",mStartMonth);   //  表示開始日(yyyymm01)
        mLastDate  = year.substring(0,4)+String.format("%02d01",mStartMonth + mSpanMonth);  //  表示終了日(yyyymm01)
        mStartDay  = ylib.Date2JulianDay(mStartDate);
        mLastDay   = ylib.Date2JulianDay(mLastDate);
        Log.d(TAG, "createMapData: "+ mStartDate +" "+ mLastDate);

        float totalDistance = 0;    //  累積距離(km)
        float totalLap      = 0;    //  累積時間(s)
        float totalElevator = 0;    //  累積高さ(m)
        float maxElevator   = 0;    //  最大高度(m)

        mGraphData.clear();
        for (int i = 0; i < keyArray.length; i++) {
            String categoryData = mListData.getData(keyArray[i], "分類");
            if (category.compareTo("全の分類")==0 || categoryData.compareTo(category)==0) {
                float[] graphData = new float[6];
                String date = mListData.getData(keyArray[i], "年月日");
                String time = mListData.getData(keyArray[i], "時間");
                graphData[0] = ylib.Date2JulianDay(date) + ylib.Time2Sec(time) / 3600f / 24f;
                graphData[1] = (float)ylib.str2Double(mListData.getData(keyArray[i], "移動距離"));
                graphData[2] = (float)ylib.Time2Sec(mListData.getData(keyArray[i], "移動時間"));
                graphData[3] = (float)ylib.str2Double(mListData.getData(keyArray[i], "最大高度"));
                graphData[4] = (float)ylib.str2Double(mListData.getData(keyArray[i], "最小高度"));
                graphData[5] = (float)ylib.str2Double(mListData.getData(keyArray[i], "歩数"));
                mGraphData.add(graphData);

                totalDistance += graphData[1];
                totalLap      += graphData[2];
                totalElevator += graphData[3] - graphData[4];
                maxElevator    = Math.max(maxElevator, graphData[3]);
            }
        }
        mListGraphView.setTotalData(totalDistance, totalLap, maxElevator,totalElevator);
    }

    /**
     * データの最終日を取得する
     * @return  最終日の日付(yyyymmdd)
     */
    private String getLastDateData() {
        String lastDate = "";
        String[] keyArray = mListData.getKeyData();
        for (int i = 0; i < keyArray.length; i++) {
            String date = mListData.getData(keyArray[i], "年月日");
            if (0 < date.compareTo(lastDate)) {
                lastDate = date;
            }
        }
        return lastDate;
    }

    /**
     * 表示用データの作成
     * @param dataType      データの種別
     * @param collectUnit   集計単位
     */
    private void setPlotData(String dataType, String collectUnit) {
        mDataType = dataType;
        mCollectUnit = collectUnit;
        //  回または日、週、月ごとにデータを置換える
        Map<Float,Float[]> mapData = new HashMap<>();
        Float weekOffset = 1.0f;                        //  開始日の週初めのずれ(日曜日を0にする)
        for (int i = 0; i < mGraphData.size(); i++) {
            Float key = mGraphData.get(i)[0];                   //  開始日からの日数
            Float[] data = getData(dataType, mGraphData.get(i));//  種類によるデータの抽出
            if (collectUnit.compareTo("回")==0) {
            } else if (collectUnit.compareTo("日")==0) {
                key = (float)Math.floor(key);
            } else if (collectUnit.compareTo("週")==0) {
                key = (float)Math.floor(key);
                key = (float)Math.floor((key + weekOffset) / 7) * 7f - weekOffset;
            } else if (collectUnit.compareTo("月")==0) {
                String date = ylib.JulianDay2DateYear(key.intValue(), false);
                key = (float)ylib.Date2JulianDay(date.substring(0,6)+"01");
            }
            meargeData(mapData, dataType, key, data);
        }

        //  回または日、週、月ごとを集計する
        List<float[]> plotData = new ArrayList<>();
        float maxVal = 0;
        for (Map.Entry<Float, Float[]> entry : mapData.entrySet()) {
            float data[] = new float[2];
            if (dataType.compareTo("速度") == 0) {
                data[0] = entry.getKey();
                data[1] = entry.getValue()[0] / entry.getValue()[1];
            } else if (dataType.compareTo("累積標高差")==0) {
                data[0] = entry.getKey();
                data[1] = entry.getValue()[0] - entry.getValue()[1];
            } else {
                data[0] = entry.getKey();
                data[1] = entry.getValue()[0];
            }
            plotData.add(data);
            maxVal = Math.max(maxVal, data[1]);
        }
        mListGraphView.setPlotData(plotData);
        mListGraphView.setWorldAreaData(0f, mStartDay, maxVal, mLastDay);
    }

    /**
     * 集計単位が日、週、月の場合に累積したデータにする
     * @param plotData      データの種類
     * @param key           KEYデータ(ユリウス日)
     * @param data          累積したデータ
     */
    private void meargeData(Map<Float,Float[]> plotData, String dataType, Float key, Float[] data) {
        if (plotData.containsKey(key)) {
            Float[] tmp = plotData.get(key);
            if (dataType.compareTo("最大高度")==0) {
                data[0] = Math.max(data[0],tmp[0]);
                data[1] = tmp[1];
            } else {
                data[0] += tmp[0];
                data[1] += tmp[1];
            }
            plotData.put(key, data);
        } else {
            plotData.put(key,data);
        }
    }

    /**
     * 測定データから種類に応じてデータを取得
     * @param dataType      データの種別
     * @param data          データ
     * @return              選択したデータ
     */
    private Float[] getData(String dataType, float[] data ) {
        Float val[] = new Float[2];
        val[0] = 0f;
        val[1] = 0f;
        if (dataType.compareTo("移動距離")==0) {
            val[0] = data[1];
        } else if (dataType.compareTo("移動時間")==0) {
            val[0] = data[2];
        } else if (dataType.compareTo("速度")==0) {
            val[0] = data[1];
            val[1] = (data[2] / 3600f);
        } else if (dataType.compareTo("最大高度")==0) {
            val[0] = data[3];
        } else if (dataType.compareTo("累積標高差")==0) {
            val[0] = data[3];
            val[1] = data[4];
        } else if (dataType.compareTo("歩数")==0) {
            val[0] = data[5];
        } else if (dataType.compareTo("歩幅")==0) {
        } else {
            val[0] = data[1];
        }
        return val;
    }

    /**
     * ドロップダウンリストの設定をする
     * @param year      対象年
     */
    private void setMmenu(String year) {
        loadFile(year);
        setMonthTitle();
        setSpanTitleList();
        setCollectUnitTitleList(mCollectUnitAdapter.getPosition(mCollectUnit));
        setDataTypeTitleList(mDataTypeAdapter.getPosition(mDataType));
        setCategoryTitle();
    }

    /**
     * 対象年のインデックスデータを取り込む
     * @param year      対象年
     */
    private void loadFile(String year) {
        String indexFile = gilib.getLoadIndexFileName(year);
        mListData.setSaveDirectory(mIndexDirectory, indexFile);
        mListData.LoadFile();
    }

    /**
     * 設定されている分類の取得
     * @return      分類名
     */
    private String getCurCategory() {
        int l = mSpCategory.getSelectedItemPosition();
        return (0<=l?mCategoryAdapter.getItem(l):"全の分類");
    }

    /**
     * 分類フィルタ(ドロップダウンリスト)に分類データを設定する
     */
    private void setCategoryTitle() {
        List<String> categoryList = getCategoryList();
        mCategoryAdapter.clear();
        mCategoryAdapter.add("全の分類");
        for (int i=0; i<categoryList.size(); i++) {
            if (mCategoryAdapter.getPosition(categoryList.get(i))<0) {
                mCategoryAdapter.add(categoryList.get(i));
            }
        }
        mSpMonth.setSelection(0);
    }

    /**
     * 分類データを抽出してリスト化する
     * @return
     */
    private List<String> getCategoryList() {
        //  分類のデータを抽出
        List<String> dataList = mListData.getListData("分類");
        if (dataList.size() == 0)
            return null;
        //  抽出したデータをソート
        dataList.sort(new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareTo(t1);
            }
        });
        //  抽出したデータをsqueezeしてList化
        List<String> categoryList  = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            if (!categoryList.contains(dataList.get(i)))
                categoryList.add(dataList.get(i));

        }
        return categoryList;
    }

    /**
     * 表示するデータの種類を取得
     * @return      データの種類
     */
    private String getCurDataType() {
        int l = mSpDataType.getSelectedItemPosition();
        return (0<=l?mDataTypeAdapter.getItem(l):"");
    }

    /**
     * グラフ表示するデータの種類(移動協、移動時間・・・)をプルダウンメニューに設定する
     * @param pos       初期値
     */
    private void setDataTypeTitleList(int pos) {
        //  データタイプのSpinner設定
        mDataTypeAdapter = new ArrayAdapter<String>(
                this,android.R.layout.simple_spinner_item, mDataTypeMenu);
        mSpDataType.setAdapter(mDataTypeAdapter);
        mSpDataType.setSelection(pos);
    }

    /**
     * 設定されている集計単位の取得
     * @return      集計単位
     */
    private String getCurCollectUnit() {
        int l = mSpCollectUnit.getSelectedItemPosition();
        return (0<=l?mCollectUnitAdapter.getItem(l):"回");
    }

    /**
     * グラフ表示する集計単位(回、日、週、月)の単位をプルダウンメニューに設定する
     * @param pos       初期値
     */
    private void setCollectUnitTitleList(int pos) {
        mCollectUnitAdapter = new ArrayAdapter<String>(
                this,android.R.layout.simple_spinner_item, mCollectUnitMenu);
        mSpCollectUnit.setAdapter(mCollectUnitAdapter);
        mSpCollectUnit.setSelection(pos);
    }

    /**
     * 設定されている機関の取得
     * @return
     */
    private int getCurSpan() {
        if (mSpSpan.getSelectedItemPosition()<0)
            return 12;
        String span = mSpanAdapter.getItem(mSpSpan.getSelectedItemPosition());
        if (span.compareTo("年")==0) {
            return 12;
        } else if (span.compareTo("半年")==0) {
            return 6;
        } else if (span.compareTo("3ヶ月")==0) {
            return 3;
        } else if (span.compareTo("1ヶ月")==0) {
            return 1;
        }
        return 12;
    }

    /**
     * 表示期間のプルダウンメニューのリストを設定する
     */
    private void setSpanTitleList() {
        mSpanAdapter = new ArrayAdapter<String>(
                this,android.R.layout.simple_spinner_item, mSpanMenu);
        mSpSpan.setAdapter(mSpanAdapter);
    }

    /**
     * 表示開始月の取得
     * @return      開始月
     */
    private int getCurMonth() {
        int l = mSpMonth.getSelectedItemPosition();
        return ylib.str2Integer(ylib.getStrNumber(mMonthAdapter.getItem(l)));
    }

    /**
     * 月フィルタ(ドロップダウンリスト)にデータを設定する
     * 月データは登録データから取得
     */
    private void setMonthTitle() {
        int lastMonth = 12 - getCurSpan();
        mMonthAdapter.clear();
        for (int i=0; i < lastMonth + 1; i++) {
            mMonthAdapter.add((i+1)+"月");
        }
        mSpMonth.setSelection(0);
    }

    /**
     * GPXのリストデータから月データを抽出してリスト化する
     * @return
     */
    private List<String> getMonthList() {
        //  年月日のデータを抽出
        List<String> dateList = mListData.getListData("年月日");
        if (dateList.size() == 0)
            return null;
        //  抽出したデータをソート
        dateList.sort(new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareTo(t1);
            }
        });
        //  抽出したデータをsqueezeしてList化
        List<String> monthList  = new ArrayList<>();
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
     * フィルタに表示されている年を取得
     * @return
     */
    private String getCurYear() {
        int l = mSpYear.getSelectedItemPosition();
        return (0 <= l?mYearAdapter.getItem(l).substring(0, 4):"");
    }

    /**
     *  リストデータファイルを検索し年のドロップリストに登録する
     */
    private void setYearTitle() {
        //	ファイルリストの取得
        List<String> fileList = gilib.getFileList(mIndexDirectory); //	保存データファイルリストの取得
        if (fileList == null)
            return;
        //  年フィルタにデータを設定
        mYearAdapter.clear();
        for (int i=0; i< fileList.size(); i++) {
            setYearTitle(gilib.getYear(fileList.get(i)));
        }
        mSpYear.setSelection(0 < mYear.length()?mYearAdapter.getPosition(mYear+"年"):0);
    }

    /**
     * 年フィルタにデータを設定する
     * @param date      年月日(yyyymmdd)
     */
    private void setYearTitle(String date) {
        if (date.length()<4)
            return ;
        if (mYearAdapter.getPosition(date.substring(0, 4)) < 0)
            mYearAdapter.add(date.substring(0, 4)+"年");
        mSpYear.setSelection(mYearAdapter.getCount() - 1);
    }


    /**
     * 画面コントロールの初期化
     */
    private void init() {
        mSpYear  = (Spinner)findViewById(R.id.spinner9);
        mSpMonth = (Spinner)findViewById(R.id.spinner10);
        mSpSpan  = (Spinner)findViewById(R.id.spinner11);
        mSpCollectUnit = (Spinner)findViewById(R.id.spinner12);
        mSpDataType = (Spinner)findViewById(R.id.spinner13);
        mSpCategory = (Spinner)findViewById(R.id.spinner14);

        mListGraphView = new ListGraphView(this);
        LinearLayout linearlayout = (LinearLayout)findViewById(R.id.linearLayoutListGraph);
        linearlayout.addView(mListGraphView);

        mYearAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpYear.setAdapter(mYearAdapter);
        mMonthAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpMonth.setAdapter(mMonthAdapter);
        mSpanAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpSpan.setAdapter(mSpanAdapter);
        mCollectUnitAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpCollectUnit.setAdapter(mCollectUnitAdapter);
        mDataTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpDataType.setAdapter(mDataTypeAdapter);
        mCategoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpCategory.setAdapter(mCategoryAdapter);
    }
}