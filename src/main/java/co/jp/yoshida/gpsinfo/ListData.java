package co.jp.yoshida.gpsinfo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by katsushige on 2015/02/25.
 *
 * CSV形式のファイルデータとして管理する
 * CSV形式のデータの保存と読込
 * 項目指定によるデータの入出力を行う
 *
 * ListData(String[] dataTitle)             コンストラクタ(各項目タイトル名の配列)
 * ListData(Context c, String[] dataTitle)  コンストラクタ
 * setKeyData(String[] keys)                Mapに登録するときのKEYデータの登録(データ分類の中から設定)
 * setSaveDirectory(String dataDirectory, String filename)  データの保存ディレクトリとデータファイル名の設定
 * getDataFormat()                          データのフォーマットを取得する
 *
 * setDataSortCategory(String category)     ソートする分類名を設定する
 * setDataSortDirect(DataSortDirect direct) ソートの方向を設定する
 * setDataSortReverse()                     ソートの向きを逆にする
 *
 * setFilter(String category, String data)  フィルターワードを設定する(正規表現に対応)
 * resetFilter()                            フィルターワードをリセットする
 *
 * addKeyData(String keyData)               キーデータを登録
 * removeData(String keyData)               キーデータの削除
 * replaceKey(String oldKey, String newKey) キーデータを変更する
 *
 * getListData()                            登録されているデータをList形式で全取得(ソートあり)
 * dataSort(List<String[]> dataList)        Listデータをソートする
 * getStrDatas(String category)             分類名のデータリスト(配列)を抽出する(ソートあり)
 * getListData(String category)             カテゴリで指定されたデータをListで全取得(フィルタあり)
 * getListDataArray(String category)        カテゴリで指定されたデータを配列形式で全取得(フィルタあり)
 * getKeyData()                             すべてのキーワードを配列で取得する
 * setData(String key, String title, String data)   指定キーデータのデータに分類名指定でデータを設定する
 * setData(String[] data)                   データの登録(配列のdat[0]がkeyデーとなる)
 * setData(String key, String[] data)       データの登録
 * getData(String key, String title)        指定キーデータのデータに分類名指定でデータを取得する
 * getData(String key)                      データを配列で一式取得
 * getTitlePos(String title)                一覧データのタイトル位置の検索・取得
 *
 * saveDataFile()                           リストデータをCSV形式でファイルに保存する
 * getCsvData(String[] datas)               文字配列データをCSV形式に変換
 * LoadFile()                               CSV形式のデータをファイルから行配列にして読み込む
 * getSize()                                リストデータのサイズを求める
 * makeSaveDir()                            ファイル保存ディレクトリの作成
 *
 * findData(String data)                    全データの中から合致するデータがあればそのkeyデータを返す
 * findData(String data, String title)      全データの中から指定項目で合致するデータがあればそのkeyデータを返す
 * findData(String[] data, String[] title)  全データの中から複数の指定項目で合致するデータがあればそのkeyデータを返す
 *
 */

public class ListData {
    private static final String TAG = "ListData";

    private Context c = null;

    private String mSaveDirectory = "";         //  データ保存ディレクトリ
    private String[] mDataFormat;               //  データ分類{"タイトル","ファイル","分類","種別","アプリ名","パッケージ名","クラス名"};
    private Map<String, String[]> mFileData;    //  データリスト
    private String mFileName;                   //  データファイル名
    private String[] mKeyData = null;           //  Mapに登録するKEYデータをデータ分類から登録

    public enum DataSortDirect {non, order, revers};
    private int mDataSortPos;
    private DataSortDirect mDataSortDirect = DataSortDirect.non;
    private Map<String, String> mFilter;        //  データを取得するときのフィルタ
    private List<String[]> mListData;           //  List形式に変換したデータ

    YLib ylib;

    /**
     * コンストラクタ
     * @param dataTitle     各項目タイトル名の配列
     */
    public ListData(String[] dataTitle) {
        ylib = new YLib();
        mDataFormat = dataTitle;
        mFileData = new HashMap<String, String[]>();
    }

    /**
     * コンストラクタ
     * @param c             コンテキスト
     * @param dataTitle     各項目のタイトル名の配列
     */
    public ListData(Context c, String[] dataTitle) {
        this.c = c;
        ylib = new YLib(c);
        mDataFormat = dataTitle;
        mFileData = new HashMap<String, String[]>();
    }

    /**
     * Mapに登録するときのKEYデータの登録(データ分類の中から設定)
     * KEYデータが複数の時はその組み合わせをKEYとする
     * KEYデータが登録されていない時はデータ分類の最初の物を使う
     * @param keys      データ分類名
     */
    public void setKeyData(String[] keys) {
        mKeyData = keys;
    }

    public boolean isContainKey(String[] data) {
        String key = getKeyData(data);
        if (key == null)
            return false;
        return mFileData.containsKey(key);
    }


    /***
     * データの保存ディレクトリとデータファイル名の設定
     * ファイル名の拡張子は .csv に固定
     * @param dataDirectory     データ保存ディレクトリ
     * @param filename          データフィル名(拡張子なし)
     */
    public void setSaveDirectory(String dataDirectory, String filename) {
        mSaveDirectory = dataDirectory;
        makeSaveDir();
        mFileName = mSaveDirectory + "/" + filename + ".csv";
    }

    /**
     * データのフォーマットを取得する
     * @return
     */
    public String[] getDataFormat() {
        return mDataFormat;
    }

    /**
     * ソートする分類名を設定する
     * @param category      分類名
     */
    public void setDataSortCategory(String category) {
        if (mDataSortDirect == DataSortDirect.non)
            mDataSortDirect = DataSortDirect.order;
        mDataSortPos = getTitlePos(category);
    }

    /**
     * ソートの方向を設定する
     * @param direct        non/order/revers
     */
    public void setDataSortDirect(DataSortDirect direct) {
        mDataSortDirect = direct;
    }

    /**
     * ソートの向きを逆にする
     * ソートが設定されていない時は何もしない
     */
    public void setDataSortReverse() {
        if (mDataSortDirect == DataSortDirect.order)
            mDataSortDirect = DataSortDirect.revers;
        else if (mDataSortDirect == DataSortDirect.revers)
            mDataSortDirect = DataSortDirect.order;
    }

    /**
     * フィルターワードを設定する(正規表現に対応)
     * @param category      分類
     * @param data          フィルターワード
     */
    public void setFilter(String category, String data) {
        if (mFilter == null)
            mFilter = new HashMap<String, String>();
        mFilter.put(category,data);
    }

    /**
     * フィルターワードをリセットする
     */
    public void resetFilter() {
        if (mFilter == null)
            mFilter = new HashMap<String, String>();
        mFilter.clear();
    }

    /**
     * フィルターワードを使って正規表現でフィルタリングする
     * @param data      対象データ
     * @return          フィルタリング結果
     */
    private boolean filter(String[] data) {
        if (mFilter == null || mFilter.isEmpty())
            return true;
        for (Map.Entry<String, String> keyValue : mFilter.entrySet()) {
            int pos = getTitlePos(keyValue.getKey());
            if (0 <= pos) {
                if (!data[pos].matches(keyValue.getValue()))
                    return false;
            }
        }
        return true;
    }

    /**
     * キーデータを登録
     * @param keyData       キーデータ
     * @return              既に登録されている場合はfalse
     */
    public boolean addKeyData(String keyData) {
        if (mFileData.containsKey(keyData))
            return false;
        String[] datas = new String[mDataFormat.length];
        datas[0] = keyData;
        mFileData.put(keyData, datas);
        return true;
    }

    /**
     * キーデータの削除
     * @param keyData       キーデータ
     * @return
     */
    public boolean removeData(String keyData) {
        if (mFileData.containsKey(keyData)) {
            mFileData.remove(keyData);
            return true;
        } else {
            return false;
        }
    }

    /**
     * キーデータを変更する
     * 古いキーデータが存在しないかあたらしいキーデータが存在する場合にはエラー(false)とする
     * @param oldKey    置換え前のキーデータ
     * @param newKey    新しいキーデータ
     * @return          成否
     */
    public boolean replaceKey(String oldKey, String newKey) {
        if (mFileData.containsKey(newKey) || !mFileData.containsKey(oldKey))
            return false;
        String[] datas = mFileData.get(oldKey);
        datas[0] = newKey;
        return setData(datas,true);
    }

    /**
     * 登録されているデータをList形式で全取得(ソートあり)
     * @return      List形式のデータ
     */
    public List<String[]> getListData() {
        List<String[]> dataList = new LinkedList<String[]>();
        //  フィルタを通して配列に登録
        for (String[]data : mFileData.values()) {
            if (filter(data))       //  正規表現でフィルタリングする
                dataList.add(data);
        }
        //  データをソートする
        dataSort(dataList);
        return dataList;
    }

    /**
     * Listデータをソートする
     * setDataSortCategory(),setDataSortDirect(),setDataSortReverse()の設定でソートする
     * @param dataList
     */
    public void dataSort(List<String[]> dataList) {
        if (mDataSortDirect != DataSortDirect.non && 0 <= mDataSortPos) {
            if (0 <= mDataSortPos) {
                dataList.sort(new Comparator<String[]>() {
                    @Override
                    public int compare(String[] t0, String[] t1) {
                        if (mDataSortDirect == DataSortDirect.order) {
                            return t0[mDataSortPos].compareTo(t1[mDataSortPos]);
                        } else {
                            return t1[mDataSortPos].compareTo(t0[mDataSortPos]);
                        }
                    }
                });
            }
        }
    }

    /**
     * 分類名のデータリスト(配列)を抽出する(ソートあり)
     * @param category      分類名
     * @return              データ配列
     */
    public String[] getStrDatas(String category) {
        int pos = getTitlePos(category);
        List<String[]> listDatas = getListData();
        if (pos < 0 || listDatas.size() <= 0)
            return null;
        String[] strDatas = new String[listDatas.size()];
        for (int i = 0; i < listDatas.size(); i++) {
            strDatas[i] = listDatas.get(i)[pos];
            Log.d(TAG,"getStrDatas: "+i+" "+strDatas[i]);
        }
        return strDatas;
    }

    /**
     * カテゴリで指定されたデータをList形式で全取得(フィルタを通す)
     * @param category  分類(タイトル)
     * @return          Listデータ
     */
    public List<String> getListData(String category) {
        int pos = getTitlePos(category);
        if (pos < 0 || mFileData.size() <= 0)
            return null;
        List<String>  dataList = new LinkedList<String>();
        //  フィルタを通して配列に登録
        for (String[]data : mFileData.values()) {
            if (filter(data))       //  正規表現でフィルタリングする
                if (!dataList.contains(data[pos]))
                    dataList.add(data[pos]);
        }
        return dataList;
    }

    /**
     * カテゴリで指定されたデータを配列形式で全取得(フィルタを通す)
     * @param category  分類(タイトル)
     * @return          配列データ
     */
    public String[] getListDataArray(String category) {
        List<String> listData = getListData(category);
        return listData.toArray(new String[listData.size()]);
    }

    /**
     * すべてのキーワードを配列で取得する
     * @return      Keyの配列
     */
    public String[] getKeyData() {
        String[] keys = new String[mFileData.size()];
        int i = 0;
        for (String key : mFileData.keySet()) {
            keys[i++] = key;
        }
        return keys;
    }

    /**
     * 指定キーデータのデータに分類名指定でデータを設定する
     * キーデータがない場合は新規作成
     * @param key       キーデータ
     * @param title     分類名
     * @param data      登録するデータ
     * @param overWrite 上書き
     * @return          成否
     */
    public boolean setData(String key, String title, String data, boolean overWrite) {
        int pos = getTitlePos((title));
        if (0 > pos)
            return false;
        String[] datas;
        if (mFileData.containsKey(key)) {
            //  既存データ
            datas = mFileData.get(key);
        } else {
            //  新規データ
            datas = new String[mDataFormat.length];
            for (int i = 0;i < datas.length; i++)
                datas[i] = "";
        }
        datas[pos] = data;          //  指定分類にデータを設定
        if (overWrite || !mFileData.containsKey(key)) {
            mFileData.put(key, datas);
            return true;
        } else
            return false;
    }

    /**
     * データの登録(keyデータが設定されていない時は配列のdat[0]がkeyデーとなる)
     * @param data      配列データ
     * @param overWrite 上書き
     * @return          登録不可(false)
     */
    public boolean setData(String[] data, boolean overWrite) {
        if (mKeyData == null || mKeyData.length == 0) {
            return setData(data[0], data, overWrite);
        } else {
            String key = getKeyData(data);
            return setData(key, data,overWrite);
        }
    }

    /**
     * データの登録(上書きは選択)
     * @param key       キーデータ
     * @param data      配列データ
     * @param overWrite 上書き
     * @return          登録不可(false)
     */
    public boolean setData(String key, String[] data, boolean overWrite) {
        if (data.length != mDataFormat.length)
            return false;
        if (overWrite || !mFileData.containsKey(key)) {
            mFileData.put(key, data);
            return true;
        } else
            return false;
    }

    /**
     * データを更新する、元データがない時は新規データ追加
     * キーデータは登録データから取り出す
     * @param data      更新データ
     * @return          登録可否
     */
    public boolean updateData(String[] data) {
        if (mKeyData == null || mKeyData.length == 0) {
            return updateData(data[0], data);
        } else {
            String key = getKeyData(data);
            return updateData(key, data);
        }
    }

    /**
     * データを更新する、元データがない時は新規データ追加
     * @param key       キーデータ
     * @param data      更新データ
     * @return          登録可否
     */
    public boolean updateData(String key, String[] data) {
        if (data.length != mDataFormat.length)
            return false;
        if (!mFileData.containsKey(key)) {
            //  新規登録
            mFileData.put(key, data);
            return true;
        } else {
            //  データ更新
            String[] src = mFileData.get(key);
            String[] dest = updateData(src, data);
            mFileData.put(key, dest);
            return true;
        }
    }

    /**
     * 登録データを元データに対して更新する(マージする)
     * @param src       元データ
     * @param dest      更新用データ(更新しないデータは空白またはnullとする)
     * @return          更新後のデータ
     */
    private String[] updateData(String[] src, String[] dest) {
        if (src.length != dest.length)
            return src;
        String[] outData = new String[src.length];
        for (int i = 0; i < src.length; i++) {
            if (dest[i] != null && 0 < dest[i].length()) {
                outData[i] = dest[i];
            } else {
                outData[i] = src[i];
            }
        }
        return outData;
    }


    /**
     * 指定キーデータのデータに分類名指定でデータを取得する
     * @param key       キーデータ
     * @param title     分類名
     * @return          データ
     */
    public String getData(String key, String title) {
        int pos = getTitlePos((title));
        if (0 <= pos && mFileData.containsKey(key)) {
            String[] datas = mFileData.get(key);
            return datas[pos];
        }
        return "";
    }

    /**
     * データを配列で一式取得
     * @param key       キーデータ
     * @return          配列データ
     */
    public String[] getData(String key) {
        if (mFileData.containsKey(key))
            return mFileData.get(key);
        else
            return null;
    }

    /**
     * 一覧データのタイトル位置の検索・取得
     * @param title タイトル名
     * @return タイトル位置
     */
    public int getTitlePos(String title) {
        if (title == null || mDataFormat == null)
            return -1;
        for (int i = 0; i < mDataFormat.length; i++) {
            if (mDataFormat[i].compareTo(title) == 0)
                return i;
        }
        return -1;
    }

    /**
     * リストデータをCSV形式でファイルに保存する
     */
    public void saveDataFile() {
        Log.d(TAG,"saveDataFile: "+mFileData.size());
        String buffer = getCsvData(mDataFormat) + "\n";
        for (Map.Entry<String, String[]> entry  : mFileData.entrySet()) {
            String[] datas = entry.getValue();
            Log.d(TAG,"saveDataFile: "+entry.getKey()+" "+datas.length+" "+datas[0]);
            if (0 < datas.length) {
                buffer += getCsvData(datas);
                buffer += "\n";
            }
        }
        if (0 < buffer.length()) {
            ylib.writeFileData(mFileName, buffer);
            if (c!=null)
                Toast.makeText(c, mFileName + "\n保存しました", Toast.LENGTH_LONG).show();
        } else {
            if (c != null)
                Toast.makeText(c, "データがありません", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 文字配列データをCSV形式に変換
     * @param datas     文字配列データ
     * @return          CSVデータ
     */
    public String getCsvData(String[] datas) {
        String buffer= "";
        //  配列データをCSV形式になるように["]でくくって[,]で区切った文字列にする
        for (int j = 0; j < datas.length; j++) {
            buffer += "\"" + datas[j] + "\"";
            if (j < datas.length - 1)
                buffer += ",";
        }
        return buffer;
    }

    /**
     * CSV形式のデータをファイルから行配列にして読み込む
     * 1行目にタイトルがあれば指定のタイトル順に組み替えて
     * @return              読込の可否
     */
    public boolean LoadFile() {
        return LoadFile(mFileName,false);
    }

    /**
     * CSV形式のデータをファイルから行配列にして読み込む
     * 1行目にタイトルがあれば指定のタイトル順に組み替えて
     * @param fileName      インデックスファイルパス
     * @param append        追加フラグ
     * @return              読込の可否
     */
    public boolean LoadFile(String fileName, boolean append) {
        //	ファイルの存在確認
        if (!ylib.existsFile(fileName)) {
            if (c!=null)
                Toast.makeText(c, "ファイルが存在していません\n"+fileName, Toast.LENGTH_LONG).show();
            return false;
        }

        //	ファイルデータのListに取り込み
        ArrayList<String> fileData = new ArrayList<String>();
        fileData.clear();
        ylib.readTextFile(fileName, fileData);
        if (fileData.size()<1)
            return false;

        //	フォーマットの確認(タイトル行の展開)
        String[] fileTitle = ylib.splitCsvString(fileData.get(0));
        if (fileTitle[0].compareTo(mDataFormat[0])!=0) {
            if (c!=null)
                Toast.makeText(c, "データが登録されていません", Toast.LENGTH_LONG).show();
            return false;
        }

        //	データの順番を求める
        int[] titleNo = new int[mDataFormat.length];
        for (int n=0; n<mDataFormat.length; n++) {
            titleNo[n] = -1;
            for (int m=0; m<fileTitle.length; m++) {
                if (mDataFormat[n].compareTo(fileTitle[m])==0) {
                    titleNo[n] = m;
                    break;
                }
            }
        }

        //	データをデータフォーマットに合わせて並び替えてListのデータをMapに登録する
        if (!append)
            mFileData.clear();
        for (int i=0; i<fileData.size(); i++) {
            if (fileData.get(i).length() < 2 || fileData.get(i).charAt(0)==0x0000)
                continue ;
            String[] text = ylib.splitCsvString(fileData.get(i));
            String[] buffer = new String[mDataFormat.length];
            for (int n = 0; n < mDataFormat.length; n++) {
                if (0<=titleNo[n] && titleNo[n]<text.length) {
                    buffer[n] = text[titleNo[n]];
                } else {
                    buffer[n] = "";
                }
            }
            //  タイトル行を覗いてMapに登録
            if (buffer[0].compareTo(mDataFormat[0])!=0)
                mFileData.put(getKeyData(buffer), buffer);
        }
        return true;
    }

    /**
     * Keyデータを求める(設定KEYの組合せ)
     * @param data      データ配列
     * @return          KEYデータ
     */
    private String getKeyData(String[] data) {
        String key = "";
        if (data == null || data.length < 1)
            return null;
        if (mKeyData == null){
            return data[0];
        } else {
            for (int i = 0; i < mKeyData.length; i++) {
                int pos = getTitlePos(mKeyData[i]);
                if (pos < data.length)
                    key += data[pos];
            }
        }
        return key;
    }

    /**
     * ファイルデータの初期化
     * ファイルデータをクリアした後にタイトルを設定する
     */
    private void initFileData() {
        if (mFileData == null)
            return ;
        mFileData.clear();
        mFileData.put(mDataFormat[0], mDataFormat);
    }

    /***
     * リストデータのサイズを求める
     * @return      データのサイズ(行数)
     */
    public int getSize() {
        if (mFileData != null)
            return mFileData.size();
        else
            return 0;
    }


    /**
     * ファイル保存ディレクトリの作成
     * @return      ディレクトリ作成の可否
     */
    public boolean makeSaveDir() {
        //	ファイルにデータの追加
        if (!ylib.existsFile(mSaveDirectory) && !ylib.isDirectory(mSaveDirectory)) {
            if (!ylib.mkdir(mSaveDirectory)) {
                if (c != null)
                    Toast.makeText(c, "ディレクトリが作成できません\n" + mSaveDirectory, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    /**
     * キーデータの存在を確認する
     * @param key       キーデータ
     * @return          存在の有無
     */
    public boolean findKey(String key) {
        return mFileData.containsKey(key);
    }

    /**
     * 全データの中から合致するデータがあればそのkeyデータを返す
     * @param data      検索データ
     * @return          keyデータ
     */
    public String findData(String data) {
        for (Map.Entry<String, String[]> entry  : mFileData.entrySet()) {
            String[] datas = entry.getValue();
            for (int i = 0; i < datas.length; i++) {
                if (datas[i].compareTo(data) == 0)
                    return entry.getKey();
            }
        }
        return "";
    }

    /**
     * 全データの中から指定項目で合致するデータがあればそのkeyデータを返す
     * @param data      検索データ
     * @param title     検索対象項目
     * @return          keyデータ
     */
    public String findData(String data, String title) {
        int pos= getTitlePos(title);
        if (pos < 0)
            return "";
        for (Map.Entry<String, String[]> entry  : mFileData.entrySet()) {
            String[] datas = entry.getValue();
            if (datas[pos].compareTo(data) == 0)
                return entry.getKey();
        }
        return "";
    }

    /**
     * 全データの中から複数の指定項目で合致するデータがあればそのkeyデータを返す
     * @param data      検索データ配列
     * @param title     検索対象項目配列
     * @return          keyデータ
     */
    public String findData(String[] data, String[] title) {
        int[] pos = new int[title.length];
        if (pos.length < 1)
            return "";
        for (int i = 0; i < pos.length; i++)
            pos[i] = getTitlePos(title[i]);
        for (Map.Entry<String, String[]> entry  : mFileData.entrySet()) {
            String[] datas = entry.getValue();
            int i = 0;
            for ( ; i < pos.length; i++) {
                if (datas[pos[i]].compareTo(data[i]) != 0)
                    break;
            }
            if (i == pos.length)
                return entry.getKey();
        }
        return "";
    }

}
