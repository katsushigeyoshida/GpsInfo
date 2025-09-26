package co.jp.yoshida.gpsinfo;

import android.util.Log;

import java.io.FileReader;
import java.io.IOException;

/**
 * Created by katsushige on 2015/02/28.
 * XMLファイルの読み込みクラス
 * XMLファイルから読み込んでワード単位に分割して上位に渡す
 * ワード単位: '<'と'>'で挟まれたもの <name> か '>'と'<'の間のデータ　
 */
public class XmlFileReader {

    private static final String TAG = "XmlFileReader";

    private final int BUFFERSIZE = 128;
    char[] mBuffer;                         //  読込バッファ
    int mSize;                              //  読込サイズ
    int mPos = 0;                           //  読込位置
    StringBuffer mStrBuffer;                //  XMLのワード単位のデータ
    FileReader fr = null;

    /**
     * XMLファイルを開く
     * @param path      XMLファイルのパス
     */
    public XmlFileReader(String path) {
        Log.d(TAG, "XmlFileReader");
        mBuffer = new char[BUFFERSIZE];
        mSize = BUFFERSIZE;
        try {
            fr = new FileReader(path);
            mSize = fr.read(mBuffer);
            mPos = 0;
            //Log.d(TAG, "[" + pos + ": "+ size + ": "+ String.valueOf(buffer) + "]");
        } catch (IOException e) {

        }
    }

    /**
     * XMLファイルからワード単位の切り出し
     * @return      切り出したワード
     */
    public StringBuffer getElementData() {
        // Log.d("XmlFileReader", "getElementData");
        try {
            mStrBuffer = new StringBuffer("");
            //  ファイルの読み込み終了確認
            if (mSize <= 0)
                return null;
            //  取出すワードがなくなったらファイルから続きを読み込む
            if (mSize <= mPos || BUFFERSIZE <= mPos) {
                //Log.d(TAG, "getElementData:"+"0[" + pos + ": "+ size + ": ]");
                mSize = fr.read(mBuffer);
                mPos = 0;
                //Log.d(TAG, "getElementData: "+"[" + pos + ": "+ size + ": "+ String.valueOf(buffer) + "]");
                if (mSize <= 0)
                    return null;
            }
            //  '>'と'<'の間のデータを取り出す
            if (mBuffer[mPos] != '<') {
                while (mBuffer[mPos] != '<') {
                    if (0x20 <= mBuffer[mPos])
                        mStrBuffer.append(mBuffer[mPos]);
                    else
                        mStrBuffer.append(" ");     //  コントロールコードはスペースに変換
                    mPos++;
                    if (mSize <= mPos || BUFFERSIZE <= mPos) {
                        //Log.d(TAG, "getElementData: "+"1[" + pos + ": "+ size + ": ]");
                        mSize = fr.read(mBuffer);
                        mPos = 0;
                        //Log.d(TAG, "getElementData: "+"[" + pos + ": "+ size + ": "+ String.valueOf(buffer) + "]");
                        if (mSize <= 0)
                            return null;
                    }
                }
            }
            //  '<'と'>'の囲まれたデータの取り出し
            if (mStrBuffer.length()==0 && mBuffer[mPos] == '<') {
                while (mBuffer[mPos] != '>') {
                    if (0x20 <= mBuffer[mPos])
                        mStrBuffer.append(mBuffer[mPos]);
                    else
                        mStrBuffer.append(" ");     //  コントロールコードはスペースに変換
                    mPos++;
                    if (mSize <= mPos) {
                        //Log.d(TAG, "getElementData: "+"2[" + pos + ": "+ size + ": ]");
                        mSize = fr.read(mBuffer);
                        mPos = 0;
                        //Log.d(TAG, "getElementData: "+"[" + pos + ": "+ size + ": "+ String.valueOf(buffer) + "]");
                        if (mSize <= 0)
                            return null;
                    }
                }
                if (0x20 <= mBuffer[mPos])
                    mStrBuffer.append(mBuffer[mPos]);
                mPos++;
            }
            //Log.d(TAG,"getElementData: "+"3[" + pos + ": "+ size + ": " + strBuffer +"]");
            if (mSize <= 0)
                return null;
            else
                return mStrBuffer;
        } catch (IOException e) {
            Log.d(TAG,"getElementData: "+"IOException " + e.getMessage());
            return null;
        }
    }

    /**
     * テキストファイルを一括で読み込む
     * @param path      ファイルパス
     * @return          読み込んだテキストデータ
     */
    public String readFileData(String path) {
        try {
            FileReader fr = new FileReader(path);
            int size = BUFFERSIZE;
            String strBuffer = "";
            while (BUFFERSIZE <= size) {
                size = fr.read(mBuffer);
                strBuffer += String.valueOf(mBuffer);
            }
            fr.close();
            return strBuffer;
        } catch (IOException e) {
            Log.d(TAG, "readFileData " + e.getMessage());
        }
        return "";
    }
}
