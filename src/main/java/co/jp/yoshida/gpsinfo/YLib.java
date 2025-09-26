package co.jp.yoshida.gpsinfo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Consumer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * --- ファイル操作  ---
 * setFilnameDateFormat()   makeFileName()で使用する日付のフォーマットを設定 default :  "yyyyMMdd_HHmmss"
 * setFilnameDateFormat2()  makeFileName2()で使用する日付のフォーマットを設定 default :  "yyyyMMdd"
 * getName()                ファイル名の抽出
 * getNameWithoutExt()      拡張子を除くファイル名の取得(ディレクトリを除く)
 * getPathWithoutExt()      パスから拡張子を除く(dotを含まない)
 * getNameExt()             ファイル名の拡張子を取得(ディレクトリを除く)
 * chgNameExt()             拡張子の変更
 *
 *
 *
 *
 *
 */


public class YLib {

    private static final String TAG = "YLib";

    private Context mC = null;
    private String mFileNameDateFormat = "yyyyMMdd_HHmmss";
    private String mFileNameDateFormat2 = "yyyyMMdd";

    /**
     * コンストラクタ
     */
    public YLib() {
        //Log.d(TAG, "co.jp.yoshida.gpsinfo.YLib: null ");
        this.mC = null;
    }

    /**
     * コンテキストの設定
     * Toastなどで使用
     * @param c
     */
    public YLib(Context c) {
        //Log.d(TAG, "co.jp.yoshida.gpsinfo.YLib: C ");
        this.mC = c;
    }

    /**
     * makeFileName2()で使用する日付のフォーマットを設定する
     * default :  "yyyyMMdd"
     * @param form
     */
    public void setFilnameDateFormat2(String form) {
        mFileNameDateFormat2 = form;
    }


    /**
     * 内部メモリのディレクトリの取得
     * @return          ディレクトリ
     */
    public String getInternalStrage() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * SDカードのディレクトリの取得
     * @param context   コンテキスト
     * @return          ディレクトリ
     */
    public String getExternalStrage(Context context) {
        List<String> dirList = YLib.getSdCardFilesDirPathListForLollipop(context);
        if (0 < dirList.size()) {
            String externalDir = dirList.get(0);
            externalDir = externalDir.substring(0, indexOf(externalDir, '/', 3));
            return externalDir;
        }
        return "";
    }

    /**
     * ファイル名の抽出
     * @param path パス名
     * @return ファイル名
     */
    public String getName(String path) {
        File file = new File(path);
        return file.getName();
    }

    /**
     * 拡張子を除くファイル名の取得(ディレクトリを除く)
     * @param path パス名
     * @return 拡張子なしファイル名
     */
    public String getNameWithoutExt(String path) {
        String name = getName(path);
        int n = name.lastIndexOf('.');
        if (n < 0)
            return name;
        else
            return name.substring(0, n);
    }

    /***
     * パスから拡張子を除く(dotを含まない)
     * @param path  パス名
     * @return 拡張しなしのパス名
     */
    public String getPathWithoutExt(String path) {
        int n = path.lastIndexOf('.');
        if (n < 0)
            return path;
        else
            return path.substring(0, n);
    }

    /**
     * ファイル名の拡張子を取得(ディレクトリを除く)
     *
     * @param path
     * @return
     */
    public String getNameExt(String path) {
        String name = getName(path);
        int n = name.lastIndexOf('.');
        if (n < 0)
            return "";
        else
            return name.substring(n + 1);
    }

    /**
     * 拡張子の変更
     *
     * @param path 変更前ファイル名のパス
     * @param ext  ドットなし拡張子
     * @return 変更後ファイル名
     */
    public String chgNameExt(String path, String ext) {
        String cpath;
        if (getParent(path).length() < 1) {
            cpath = getNameWithoutExt(path) + "." + ext;
        } else {
            cpath = getParent(path) + "/" + getNameWithoutExt(path) + "." + ext;
        }
        return cpath;
    }

    /**
     * 親ディレクトリの取得
     *
     * @param path パス名
     * @return 親ディレクトリ名
     */
    public String getParent(String path) {
        File file = new File(path);
        return file.getParent();
    }

    /***
     * ファイルのフルパスを取得
     * @param path  パス
     * @return フルパス
     */
    public String getFullPath(String path) {
        File file = new File(path);
        try {
            return file.getCanonicalPath();
        } catch (Exception e) {
            return "";
        }
    }

    /***
     * パスからディレクトリを取得
     * @param path  パス名
     * @return ディレクトリ名
     */
    public String getDir(String path) {
        String fpath = getFullPath(path);
        int n = fpath.lastIndexOf("\\");
        if (n < 0) {
            n = fpath.lastIndexOf("/");
            if (n < 0) {
                n = fpath.lastIndexOf(":");
                if (0 <= n)
                    return fpath.substring(0, n);
                else
                    return "";
            }
        }
        return fpath.substring(0, n);
    }

    /**
     * ファイルサイズの取得
     *
     * @param path パス名
     * @return ファイルサイズ
     */
    public long getFileSize(String path) {
        File file = new File(path);
        return file.length();
    }

    /**
     * ファイルの最終変更日の取得
     *
     * @param path パス名
     * @return 最終変更日
     */
    public long getLastModified(String path) {
        File file = new File(path);
        return file.lastModified();
    }

    /**
     * ファイルの最終変更日の設定
     *
     * @param path パス名
     * @param time 変更日時
     * @return 成功/失敗
     */
    public boolean setLastModified(String path, long time) {
        File file = new File(path);
        return file.setLastModified(time);
    }

    /***
     * ファイルのMIME(Multipurpose Internet Mail Extension)タイプを求める
     * 例えば pdfファイルであれば application/pdf を返す
     * @param path      ファイルのパス
     * @return MIMEタイプ
     */
    public String getMimeType(String path) {
        //  拡張子を求めるが全角文字だとエラーになるようなので自前を使う
        String extention = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extention == null || extention.length() < 1)
            extention = getNameExt(path);
        extention = extention.toLowerCase();
        String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extention);
        if (mimetype == null) {
            mimetype = "application/" + getNameExt(path);
        }
        return mimetype;
    }

    /**
     * ファィルの存在チェック
     *
     * @param path
     * @return
     */
    public boolean existsFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
     * ディレクトリかどうかの確認
     *
     * @param path
     * @return
     */
    public boolean isDirectory(String path) {
        File file = new File(path);
        return file.isDirectory();
    }

    /**
     * ディレクトリの作成
     *
     * @param path
     * @return
     */
    public boolean mkdir(String path) {
        File file = new File(path);
        if (!file.exists())
            return file.mkdir();
        return true;
    }

    /**
     * ディレクトリを除いた新しいファイル名の作成(ファイルは変更しない)
     * @param oldpath   ファイルのパス
     * @param newname   新しいファイル名
     * @return          変更後のパス
     */
    public String renameWithoutDir(String oldpath, String newname) {
        String oldname = getNameWithoutExt(oldpath);
        String newpath = oldpath.replace(oldname, newname);
        if (rename(oldpath, newpath))
            return newpath;
        else
            return oldpath;
    }

    /**
     * ファイル名の変更
     * @param path    パス名
     * @param newPath 変更後のパス名
     * @return
     */
    public boolean rename(String path, String newPath) {
        File file = new File(path);
        File newfile = new File(newPath);
        Log.d(TAG, "rename: " + file.getPath() + "→" + newfile.getPath());
        if (file.exists())
            return file.renameTo(newfile);
        return false;
    }


    /**
     * SDカードのfilesディレクトリパスのリストを取得する。
     * Android5.0以上対応。
     *  https://qiita.com/h_yama37/items/11b8658b2de9625200aa
     *
     * @param context
     * @return SDカードのfilesディレクトリパスのリスト
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<String> getSdCardFilesDirPathListForLollipop(Context context) {
        List<String> sdCardFilesDirPathList = new ArrayList<>();

        // getExternalFilesDirsはAndroid4.4から利用できるAPI。
        // filesディレクトリのリストを取得できる。
        File[] dirArr = context.getExternalFilesDirs(null);

        for (File dir : dirArr) {
            if (dir != null) {
                String path = dir.getAbsolutePath();
                Log.d(TAG, "getSdCardFilesDirPathListForLollipop: "+path);
                // isExternalStorageRemovableはAndroid5.0から利用できるAPI。
                // 取り外し可能かどうか（SDカードかどうか）を判定している。
                if (Environment.isExternalStorageRemovable(dir)) {

                    // 取り外し可能であればSDカード。
                    if (!sdCardFilesDirPathList.contains(path)) {
                        sdCardFilesDirPathList.add(path);
                        Log.d(TAG, "getSdCardFilesDirPathListForLollipop: add "+path);
                    }

                } else {
                    // 取り外し不可能であれば内部ストレージ。
                }
            }
        }
        return sdCardFilesDirPathList;
    }

    /**
     * 拡張しなしのパッケージ名の取得
     * @return  パッケージ名
     */
    public String getPackageNameWithoutExt() {
        if (mC == null)
            return null;
        return mC.getPackageName().substring(mC.getPackageName().lastIndexOf('.')+1);
    }

    /**
     * データファイルの保存用にパッケージ名のディレクトリの作成
     * @return      ファイルアクセスディレクトリパス
     */
    public String getPackageNameDirectory() {
        if (mC == null)
            return null;
        return setSaveDirectory(mC.getPackageName().substring(mC.getPackageName().lastIndexOf('.')+1));
    }

    /**
     * データ保存ディレクトリの設定(ファイルのアクセスにはpermissionの設定が必要)
     * WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE
     * @param subName パッケージ名(getPackageName().substring(getPackageName().lastIndexOf('.')+1))
     * @return 保存ディレクトリ
     */
    public String setSaveDirectory(String subName) {
        String saveDirectory;
        //	データ保存ディレクトリ
        saveDirectory = Environment.getExternalStorageDirectory().toString() + "/" + subName;
        if (!existsFile(saveDirectory) && !isDirectory(saveDirectory)) {
            if (!mkdir(saveDirectory)) {
                if (mC != null)
                    Toast.makeText(mC, saveDirectory + " ディレクトリが作成できません\nアプリ情報の権限を確認してください\n" +
                            saveDirectory, Toast.LENGTH_LONG).show();
                Log.d(TAG,"setSaveDirectory: "+saveDirectory);
                saveDirectory = Environment.getExternalStorageDirectory().toString();
            }
        }
        return saveDirectory;
    }

    /**
     * 日付・時間付きファイル名の作成
     * header + 現在日 + 現在時間  ( xxxx20140905091530)
     * @param header
     * @return
     */
    public String makeFileName(String header) {
        return makeFileName(header, "");
    }

    /**
     * 日付・時間付きファイル名の作成
     * header+yyyyMMdd_HHmmss+footer
     * @param header
     * @param footer
     * @return
     */
    public String makeFileName(String header, String footer) {
        return makeFileName(header, mFileNameDateFormat, footer);
    }

    /**
     * 日付付きファイル名の作成
     * header + 現在日  ( xxxx20140905)
     * @param header
     * @return
     */
    private String makeFileName2(String header) {
        return makeFileName(header, "");
    }

    /**
     * 日付付きファイル名の作成 header + 現在日(yyyymmdd)+footer
     * @param header
     * @param footer
     * @return
     */
    public String makeFileName2(String header, String footer) {
        return makeFileName(header, mFileNameDateFormat2, footer);
    }

    /**
     * 日付・時間付きファイル名の作成
     *  header + "yyyyMMddHHmmss" + footer
     * @param header
     * @param dateForm 日付・時間のフォーマット"yyyyMMddHHmmss"
     * @param footer
     * @return
     */
    public String makeFileName(String header, String dateForm, String footer) {
        Date Now = new Date();
        long curTime = Now.getTime();
        SimpleDateFormat df = new SimpleDateFormat(dateForm);
        String filetime = df.format(curTime);
        return header + filetime + footer;
    }


    /**
     * ファイルのコピー
     * @param srcPath コピー元のパス名
     * @param destDir コピー先フォルダー名
     * @return
     */
    public boolean copyFile(String srcPath, String destDir) {
//        Log.d(TAG, "copyFile: " + srcPath + " ," + destDir);
        File sfile = new File(srcPath);
        return copyfile(srcPath, destDir + "/" + sfile.getName());
    }

    /**
     * ファイルのコピー(ファイル名の変更も可能)
     * @param srFile コピー元のパス名
     * @param dtFile コピー先のパス名
     * @return
     */
    public boolean copyfile(String srFile, String dtFile) {
        try {
            //Log.d(TAG, "copyfile: " + srFile + "  " + dtFile);
            File f1 = new File(srFile);
            File f2 = new File(dtFile);
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            //	Toast.makeText(c, "Export effectué", Toast.LENGTH_SHORT).show();
            return true;
        } catch (FileNotFoundException ex) {
            if (mC != null)
                Toast.makeText(mC, "File Not found", Toast.LENGTH_SHORT).show();
            //String x = ex.getMessage();
            //Log.d(TAG, "Error copyfile: " + x);
        } catch (IOException e) {
            if (mC != null)
                Toast.makeText(mC, "Copy Error", Toast.LENGTH_SHORT).show();
            //Log.d(TAG, "Error copyfile: " + e.getMessage());
        }
        return false;
    }

    /**
     * ファイルの移動(rename)
     * @param orgFilePath   元のファイルパス
     * @param destDir       移動先ディレクトリ
     * @return              実行結果
     */
    public boolean moveFile(String orgFilePath, String destDir) {
        // 移動もとなるファイルパス
        File file = new File(orgFilePath);
        File dir = new File(destDir);
        return file.renameTo(new File(dir, file.getName()));
    }

    /**
     * ファイルとディレクトリ削除
     * @param filePath
     * @return
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return delete(file);
    }

    /**
     * ディレクトリも含めて再帰的にファイルを削除する
     * @param f
     * @return
     */
    private boolean delete(File f) {
        if (f.exists() == false)
            return true;
        if (f.isFile()) {
            return f.delete();
        }
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++)
                if (!delete(files[i]))
                    return false;
            return f.delete();
        }
        return true;
    }

    /**
     * ディレクトリ検索(再帰取得なし)
     * @param path      検索パス
     * @return          検索ディレクトリリスト
     */
    public List<String> getDirList(String path) {
        List<String> fileList = new ArrayList<String>();
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (null != files) {
            String fpath = getFullPath(path);
            if (fpath.compareTo("/") != 0 && fpath.compareTo("/storage") != 0)
                fileList.add("..");
            //  ディレクトリの取得
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isFile()) {
                    fileList.add(files[i].getName());
                }
            }
        } else {
            fileList.add("..");
            fileList.add("0");
        }
        return fileList;
    }

    /**
     * ディレクトリとファイル検索(再帰取得なし)
     * 取得したディレクトリはファイルと区別するため、[dir]と括弧で囲む
     *
     * @param path      検索ディレクトリパス
     * @param filter    ファイルフィルタ(ワイルドカード)
     * @param getDir    ディレクトリ取得の可否
     * @return          ファイル・ディレクトリリスト
     */
    public List<String> getDirFileList(String path, String filter, boolean getDir) {
        List<String> fileList = new ArrayList<String>();
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (null != files) {
            String fpath = getFullPath(path);
            if (getDir) {
                if (fpath.compareTo("/") != 0 && fpath.compareTo("/storage") != 0)
                    fileList.add("[..]");
                //  ディレクトリの取得
                for (int i = 0; i < files.length; i++) {
                    if (!files[i].isFile()) {
                        fileList.add("[" + files[i].getName() + "]");
                    }
                }
            }
            //  ファイルの取得
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    String fileName = files[i].getName();
                    if ((null == filter) || filter.length() == 0 || WcMatch(fileName, filter))
                        fileList.add(files[i].getName());
                }
            }
        } else {
            fileList.add("[..]");
            fileList.add("[0]");
        }
        return fileList;
    }

    /**
     * ディレクトリ内のファイルを取得(再帰検索,ワイルドカード)
     * @param path          対象ディレクトリ
     * @param filter        ファイルフィルタ(ワイルドカード/正規表現)
     * @param subDirectory  サブディレクトリからの取得の有無(再帰処理)
     * @return              検索結果のファイルリスト
     */
    public List<String> getFileList(String path, String filter, boolean subDirectory) {
        return getFileList(path, filter, subDirectory, false);
    }

    /***
     * ディレクトリ内のファイルを取得(再帰検索,ワイルドカード/正規表現)
     * @param path          対象ディレクトリ
     * @param filter        ファイルフィルタ(ワイルドカード/正規表現)
     * @param subDirectory  サブディレクトリからの取得の有無(再帰処理)
     * @param regexp        正規表現を使う
     * @return              検索結果のファイルリスト
     */
    public List<String> getFileList(String path, String filter, boolean subDirectory, boolean regexp) {
        List<String> fileList = new ArrayList<String>();
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (null != files) {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isFile()) {
                    if (subDirectory) {
                        List<String> subFiles = getFileList(files[i].getPath(), filter, subDirectory, regexp);
                        fileList.addAll(subFiles);
                    }
                } else {
                    String fileName = files[i].getName();
                    if (null == filter || filter.length() == 0 ||
                            (regexp && fileName.matches(filter)) ||     //  正規表現によるフィルタ
                            (!regexp && (WcMatch(fileName, filter)))) { //  ワイルドカードによるフィルタ
                        fileList.add(dir.getPath() + "/" + fileName);   //  セパレータ(WIndows "\\")(Linux "/")
                    }
                }
            }
        }
        return fileList;
    }

    /**
     * ファイルリストの取得(拡張子で選択)
     *
     * @param path              対象ディレクトリ
     * @param fileExtension     拡張子
     * @param fileList          取得結果のファイルリスト
     * @return
     */
    public boolean getExtensionFileList(String path, String fileExtension, List<String> fileList) {
        try {
            //Log.d(TAG, "getFileList:0"+path+" "+fileExtension);
            File file = new File(path);
            if (!file.exists()) {
                return false;
            }
            File[] fc = file.listFiles(getFileExtensionFilter(fileExtension));
            for (int i = 0; i < fc.length; i++) {
                //Log.d(TAG, "getFileList:"+fc[i].getName());
                if (!fc[i].isFile())
                    continue;
                fileList.add(fc[i].getName());
            }
        } catch (Exception e) {
            Toast.makeText(mC, "エラー" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    /**
     * ファイルリストの取得(正規表現で選択)
     * 正規表現の場合
     *      getFileList(path, null, false);         path内らあるすべてのファイルを取得
     *      getFileList(path, ".*..png", false);    path内にあるpngファイルを取得
     *      getFileList(path, ".*..png", true);     path以下(サブディレクトリを含む)のpngファイルを取得
     * @param path          対象ディレクトリ
     * @param fileRegex     ファイル名フィルタ(正規表現)
     * @param fileList      取得したファイルリスト
     * @return
     */
    public boolean getRegexFileList(String path, String fileRegex, List<String> fileList) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return false;
            }
            File[] fc = file.listFiles(getFileRegexFilter(fileRegex));
            for (int i = 0; i < fc.length; i++) {
                if (!fc[i].isFile())
                    continue;
                fileList.add(fc[i].getName());
            }
        } catch (Exception e) {
            Toast.makeText(mC, "エラー" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    /**
     * 指定する拡張子（".zip"）であるファイルだけを取得します。(関数インターフェース)
     *
     * @param extension
     * @return
     */
    public static FilenameFilter getFileExtensionFilter(String extension) {
        final String _extension = extension;
        return new FilenameFilter() {
            public boolean accept(File file, String name) {
                boolean ret = name.endsWith(_extension);
                return ret;
            }
        };
    }

    /**
     * ファイル名が指定する正規表現のパターンにマッチするファイルを取得します。(関数インターフェース)
     * getFileRegexFilter("[0-9]{8}\\.html")
     * ファイル名は8つの数字で構成され、拡張子は.htmlのファイルを取得します。
     *
     * @param regex
     * @return
     */
    public static FilenameFilter getFileRegexFilter(String regex) {
        final String regex_ = regex;
        return new FilenameFilter() {
            public boolean accept(File file, String name) {
                boolean ret = name.matches(regex_);
                return ret;
            }
        };
    }

    /**
     * テキストファイル読み込み
     * 行単位でテキストファイルを読み込む
     *
     * @param path     ファイルパス
     * @param fileText 行配列テキスト
     */
    public void readTextFile(String path, List<String> fileText) {
        //Log.d(TAG, "readTextFile ");
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str = br.readLine();
            //  BOM付きの場合、BOM削除
            if (str.startsWith("\uFEFF"))
                str = str.substring(1);
            while (str != null) {
                //Log.d(TAG, "readTextFile " + str);
                fileText.add(str);
                str = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "readTextFile " + e.getMessage());
            Toast.makeText(mC, "ファイル読出し" + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.d(TAG, "readTextFile " + e.getMessage());
            Toast.makeText(mC, "ファイル読出し" + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private final int BUFFERSIZE = 4096;

    /**
     * テキストファイルの読み込み(全データ読込)
     *
     * @param path
     * @return
     */
    public String readFileData(String path) {
        try {
            FileReader fr = new FileReader(path);
            int size = BUFFERSIZE;
            char buffer[] = new char[size];
            String strBuffer = "";
            while (BUFFERSIZE <= size) {
                size = fr.read(buffer);
                strBuffer += String.valueOf(buffer);
            }
            fr.close();
            return strBuffer;
        } catch (IOException e) {
            Log.d(TAG, "readFileData " + e.getMessage());
            if (mC != null)
                Toast.makeText(mC, "ファイル読出し" + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
        }
        return "";
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
     * Listテキストデータをファイルに保存
     * @param path      ファイルパス
     * @param data      List型のテキストデータ
     */
    public void saveTextData(String path, List<String> data) {
        String buffer = "";
        for (String text : data)
            buffer += text + "\n";
        if (0 < buffer.length()) {
            writeFileData(path, buffer);
            if (mC != null)
                Toast.makeText(mC, path + "\n保存しました", Toast.LENGTH_LONG).show();
        } else {
            if (mC != null)
                Toast.makeText(mC, "データがありません", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * ファイルのテキストデータをListデータに取り込む
     * @param path      ファイルパス
     * @return          Listデータ
     */
    public List<String> loadTextData(String path) {
        if (!existsFile(path)) {
            if (mC != null)
                Toast.makeText(mC, "ファイルが存在していません\n" + path, Toast.LENGTH_LONG).show();
            return null;
        }
        //	ファイルデータの取り込み
        List<String> fileData = new ArrayList<String>();
        fileData.clear();
        readTextFile(path, fileData);
        return fileData;
    }


    /**
     * CSV形式でListデータを保存する
     * format配列はタイトルとして1行目に入れる
     * @param path      保存ファイルパス
     * @param format    タイトル
     * @param data      String[]のリストデータ
     */
    public void saveCsvData(String path, String[] format, List<String[]> data) {
        String buffer = getCsvData(format) + "\n";
        for (int i = 0; i < data.size(); i++) {
            if (0 < data.get(i).length) {
                buffer += getCsvData(data.get(i));
                buffer += "\n";
            }
        }
        if (0 < buffer.length()) {
            writeFileData(path, buffer);
            if (mC != null)
                Toast.makeText(mC, path + "\n保存しました", Toast.LENGTH_LONG).show();
        } else {
            if (mC != null)
                Toast.makeText(mC, "データがありません", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * CSV形式のファイルを読み込みList<String[]>形式で出力する
     * title[]が指定されて一行目にタイトルが入っていればタイトルの順番に合わせて取り込む
     * titleがnullであればそのまま配列に置き換える
     * @param path      ファイルパス
     * @param title     タイトルの配列
     * @return          String[]のLitsデータ
     */
    public List<String[]> loadCsvData(String path, String[] title) {
        if (!existsFile(path)) {
            if (mC != null)
                Toast.makeText(mC, "ファイルが存在していません\n" + path, Toast.LENGTH_LONG).show();
            return null;
        }
        //	ファイルデータの取り込み
        List<String> fileData = new ArrayList<String>();
        fileData.clear();
        readTextFile(path, fileData);

        //	フォーマットの確認(タイトル行の展開)
        int start = 1;
        int[] titleNo = new int[title.length];
        if (title != null && 0 < fileData.size()) {
            String[] fileTitle = splitCsvString(fileData.get(0));
            if (fileTitle[0].compareTo(title[0]) == 0) {
                //	データの順番を求める
                for (int n = 0; n < title.length; n++) {
                    titleNo[n] = -1;
                    for (int m = 0; m < fileTitle.length; m++) {
                        if (title[n].compareTo(fileTitle[m]) == 0) {
                            titleNo[n] = m;
                            break;
                        }
                    }
                }
                start = 1;
            } else {
                //  タイトルがない場合そのまま順で追加
                for (int n = 0; n < title.length; n++)
                    titleNo[n] = n;
                start = 0;
            }
        } else {
            return null;
        }

        //  CSVデータを配列にしてListに登録
        List<String[]> listData = new ArrayList<String[]>();
        for (int i = start; i < fileData.size(); i++) {
            String[] text = splitCsvString(fileData.get(i));
            //  指定のタイトル順に並べ替えて追加
            String[] buffer = new String[title.length];
            for (int n = 0; n < title.length; n++) {
                if (0 <= titleNo[n] && titleNo[n] < text.length) {
                    buffer[n] = text[titleNo[n]];
                } else {
                    buffer[n] = "";
                }
            }
            listData.add(buffer);
        }
        return listData;
    }

    /**
     * テキストデータの書き込み
     *
     * @param path
     * @param buffer
     */
    public void writeFileData(String path, String buffer) {
        try {
            FileWriter fw = new FileWriter(path);
            fw.write(buffer);
            fw.close();
        } catch (IOException e) {
            Log.d(TAG, "writeFileData " + e.getMessage());
            if (mC != null)
                Toast.makeText(mC, "ファイル保存" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * テキストデータの追加書き込み
     *
     * @param path
     * @param buffer
     */
    public void writeFileDataAppend(String path, String buffer) {
        try {
            FileWriter fw = new FileWriter(path, true);
            fw.write(buffer);
            fw.close();
        } catch (IOException e) {
            Log.d(TAG, "writeFileData " + e.getMessage());
            if (mC != null)
                Toast.makeText(mC, "ファイル保存" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * バイナリィファイルの書き込み
     *
     * @param context
     * @param data
     * @param fileName
     */
    public void writeBinaryFile(Context context, byte[] data, String fileName) {
        OutputStream out = null;
        try {
            out = context.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);
            out.write(data, 0, data.length);
            out.close();
        } catch (Exception e) {
            if (mC != null)
                Toast.makeText(mC, "ファイル保存" + e.getMessage(), Toast.LENGTH_SHORT).show();
            try {
                if (out != null)
                    out.close();
            } catch (Exception e2) {
                if (mC != null)
                    Toast.makeText(mC, "ファイル保存" + e2.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * バイナリィファイル読み込み
     *
     * @param context
     * @param fileName
     * @return
     */
    public byte[] readBinaryFile(Context context, String fileName) {
        //Log.d(TAG, "readBinaryFile:0 "+fileName);
        int size;
        byte[] w = new byte[128];
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = context.openFileInput(fileName);
            out = new ByteArrayOutputStream();
            while (true) {
                size = in.read(w);
                if (size <= 0)
                    break;
                out.write(w, 0, size);
            }
            in.close();
            out.close();
            return out.toByteArray();
        } catch (Exception e) {
            if (mC != null)
                Toast.makeText(mC, "ファイル保存" + e.getMessage(), Toast.LENGTH_SHORT).show();
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            } catch (Exception e2) {
                if (mC != null)
                    Toast.makeText(mC, "ファイル保存" + e2.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }

    /**
     * strage permissionの確認
     * @param c
     * @return
     */
    public boolean checkStragePermission(Context c) {
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(c,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
            return true;
        } else{
            // 拒否していた場合
            messageDialog(c, "ファイル", "ストレージのアクセス権限が与えられていません\n"+
                    "[設定]-[アプリと通知]-[アプリ名]-[権限]でストレージをONにしてください");
            return false;
        }
    }

    /**
     * ビープ音を鳴らす
     *
     * @param dura 音出力時間  (ms)
     */
    public void beep(int dura) {
        //Toast.makeText(TimerActivity.this, "BEEP音", Toast.LENGTH_SHORT).show();
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        //toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, dura);
    }

    /**
     * 指定ミリ秒実行を止めるメソッド
     *
     * @param msec スリープ時間(msec)
     */
    public synchronized void sleep(long msec) {
        try {
            wait(msec);
        } catch (InterruptedException e) {
        }
    }

    /**
     * ファイル名をワイルドカードでマッチングする
     *
     * @param srcstr  マッチングファイル名
     * @param findstr ワイルドカード
     * @return
     */
    public boolean WcMatch(String srcstr, String findstr) {
        if (findstr == null || findstr.length() == 0)
            return true;
        int si = 0, si2 = -1, fi = 0, fi2 = -1;
        String ss = srcstr.toUpperCase();
        String fs = findstr.toUpperCase();
        do {
            if (fs.charAt(fi) == '*') {
                fi2 = fi;
                fi++;
                if (fs.length() <= fi)
                    return true;
                for (; (si < ss.length()) && (ss.charAt(si) != fs.charAt(fi)); si++)
                    ;
                si2 = si;
                if (ss.length() <= si)
                    return false;
            }
            if (fs.charAt(fi) != '?') {
                if (ss.length() <= si)
                    return false;
                if (ss.charAt(si) != fs.charAt(fi)) {
                    if (si2 < 0)
                        return false;
                    si = si2 + 1;
                    fi = fi2;
                    continue;
                }
            }
            si++;
            fi++;
            if (fs.length() <= fi && si < ss.length())
                return false;
            if (ss.length() <= si && fi < fs.length() && fs.charAt(fi) != '*' && fs.charAt(fi) != '?')
                return false;
        } while (si < ss.length() && fi < fs.length());

        return true;
    }

    /**
     * カンマセパレータで文字列を分解する
     * "で囲まれている場合は"内の文字列を抽出する
     *
     * @param str CSVの文字列データ
     * @return 文字列配列
     */
    public String[] splitCsvString(String str) {
        ArrayList<String> data = new ArrayList<String>();
        String buf = "";
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == '"' && i < str.length() - 1) {
                //  ["]で囲まれた文字列の抽出
                i++;
                while (str.charAt(i) != '"' && str.charAt(i) != '\n') {
                    buf += str.charAt(i++);
                    if (i == str.length())
                        break;
                }
                data.add(buf);
                i++;
            } else if (str.charAt(i) == ',' && i < str.length() - 1) {
                if (0 < i && str.charAt(i - 1) == ',')
                    data.add("");
                i++;
            } else {
//                if (str.charAt(i) == ',' && i < str.length() -1)
//                    i++;
                while (str.charAt(i) != ',' && str.charAt(i) != '\n') {
                    buf += str.charAt(i++);
                    if (i == str.length())
                        break;
                }
                data.add(buf);
                i++;
            }
            buf = "";
        }
        return data.toArray(new String[data.size()]);
    }

    private String mTokenStr = "+-*/#%^(),[]@";    //  区切りとなる文字(getTokenで使用)

    /**
     * 計算式の文字列を数値と演算子と関数名に分解してLISTを作る
     * 区切りとなる文字は + - * / % ^ ( ) , [ ]
     *
     * @param str 計算式の文字列
     * @return 計算式を分解したList
     */
    public List<String> getToken(String str) {
        List<String> tokenList = new ArrayList<String>();
        tokenList.clear();
        String buf = "";
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i)) || str.charAt(i) == '.' ||
                    Character.isLetter(str.charAt(i))) {
                if (0 < i && Character.isDigit(str.charAt(i - 1)) && Character.isLetter(str.charAt(i))) {
                    //  バッファの文字列をリストに格納
                    tokenList.add(buf);
                    buf = "";
                }
                //  数値または文字を格納
                buf += str.charAt(i);
            } else if (str.charAt(i) == ' ') {
                //  空白は読み飛ばす
            } else {
                if (0 < buf.length()) {
                    //  バッファの文字列をリストに格納
                    tokenList.add(buf);
                    buf = "";
                }
                if (0 <= mTokenStr.indexOf(str.charAt(i))) {
                    //  2項演算子を格納
                    tokenList.add("" + str.charAt(i));
                }
            }
        }
        if (0 < buf.length())
            tokenList.add(buf);
        return tokenList;
    }

    /**
     *	改行コードを文字列に置き換える
     * @param str
     * @return
     */
    public String strControlCodeCnv(String str) {
        String buffer;
        buffer = str.replaceAll("\n", "\\\\n");
        buffer = buffer.replaceAll("\r", "\\\\n");
        buffer = buffer.replaceAll("\r\n", "\\\\n");
        buffer = buffer.replaceAll(",", "\\\\c");
        return buffer;
    }

    /**
     *	改行文字列を改行コードに戻す
     * @param str
     * @return
     */
    public String strControlCodeRev(String str) {
        String buffer;
        buffer = str.replaceAll("\\\\n","\n");
        buffer = buffer.replaceAll("\\\\c",",");
        return buffer;
    }

    /**
     * 文字配列を一つ拡張して文字を追加する
     * @param array         拡張前の文字配列
     * @param addtext       追加する文字列
     * @return              拡張後の文字配列
     */
    public String[] expandArray(String[] array, String addtext) {
        String[] tempArray = new String[array.length+1];
        System.arraycopy(array, 0, tempArray, 0, array.length);
        tempArray[tempArray.length-1] = addtext;
        return tempArray;
    }

    /**
     * 文字列からn個目の文字位置を求める
     * @param str       文字列
     * @param ch        検索文字
     * @param n         n個目
     * @return          文字位置
     */
    public int indexOf(String str, char ch, int n) {
        int pos = 0;
        while (0 <= (pos = str.indexOf(ch, pos))) {
            n--;
            if (n <= 0)
                break;
            pos++;
        }
        return pos;
    }

    /**
     * メッセージダイヤログを表示する
     *
     * @param c       コンテキスト
     * @param title   ダイヤログのタイトル
     * @param message メッセージ
     */
    public void messageDialog(Context c, String title, String message) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * メッセージダイヤログを表示する
     * @param title   ダイヤログのタイトル
     * @param message メッセージ
     */
    public void messageDialog(String title, String message) {
        new AlertDialog.Builder(mC)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * メッセージをダイヤログ表示し、OKの時に指定の関数にメッセージの内容を渡して処理するを処理する
     * 関数インターフェースの例
     *  Consumer<String> iDelListOperation = new Consumer<String>() {
     *      @Override public void accept(String s) {
     *          mDataMap.remove(s);                     //  ダイヤログで指定された文字列をリストから削除
     *      }
     *  };
     * 関数の呼び出し方法
     *      ylib.messageDialog(mC, "計算式の削除",mTitleBuf, iDelListOperation);
     *
     * @param c         コンテキスト
     * @param title     ダイヤログのタイトル
     * @param message   メッセージ
     * @param operation 処理する関数(関数インターフェース)
     */
    public void messageDialog(Context c, String title, final String message, final Consumer operation) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        operation.accept(message);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    /**
     * 文字入力ダイヤログ
     *
     * @param c             コンテキスト
     * @param title         ダイヤログのタイトル
     * @param data          入力文字の初期値
     * @param operation     入力文字を処理する関数インターフェース
     */
    public void inputDialog(Context c, String title, String data, final Consumer operation) {
        final EditText editText = new EditText(c);
        editText.setText(data);
        LinearLayout mLinearLayout = new LinearLayout(c);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mLinearLayout.addView(editText);
        new android.app.AlertDialog.Builder(c)
                //	.setIcon(R.drawable.icon)
                .setTitle(title)
                .setView(mLinearLayout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "EditTextDialog:　OKボタン " + editText.getText().toString());
                        /* OKボタンをクリックした時の処理 */
                        operation.accept(editText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Cancel ボタンをクリックした時の処理 */
                    }
                })
                .show();
    }

    /**
     * フォルダ選択ダイヤログ
     * 選択結果(パス)は関数インターフェースで処理する
     * 関数インターフェース例
     *     Consumer<String> iFolderDisp = new Consumer<String>() {
     *         @Override public void accept(String s) {
     *             ylib.messageDialog(Sudoku.this, "選択フォルダ", s);
     *         }
     *     };
     * @param c         コンテキスト
     * @param dir       初期ディレクトリ
     * @param operation      選択したファイルのフルパスで実行する関数インターフェース
     */
    public void folderSelectDialog(final Context c, final String dir, final Consumer operation) {
        final List<String> dirList = getDirList(dir);
        dirList.sort(fileComparator);
        final String[] directries = new String[dirList.size()];
        for (int i = 0; i < dirList.size(); i++) {
            directries[i] = dirList.get(i);
        }
        new AlertDialog.Builder(c)
                .setTitle("フォルダ選択 [" + getFullPath(dir) + "]")
                .setItems(directries, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(c, which + " " + directries[which] + " が選択", Toast.LENGTH_LONG).show();
                        String fname = dirList.get(which);
                        Log.d(TAG,"folderSelectDialog: "+dir+" "+fname+" "+which+" "+directries[which]);
                        String directory = getFullPath(dir + "/" + fname);
                        folderSelectDialog(c, directory, operation);
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        operation.accept(dir);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create()
                .show();
    }

    /**
     * ファイル選択ダイヤログ
     * operationインターフェース関数例
     *  Consumer<String> iOperation = new Consumer<String>() {
     *      @Override
     *      public void accept(String s) {
     *          if (s.compareTo("全選択")==0) {
     *              mEditText.selectAll();
     *          } else if (s.compareTo("貼付け")==0) {
     *                  :
     *          }
     *      }
     *  };
     *
     * @param dir       検索ディレクトリ
     * @param filter    ファイル選択のフィルタ(ワイルドカード)
     * @param getDir    ディレクトリの取得の可否(ディレクトリの移動も行う)
     * @param operation 選択したファイルのフルパスで実行する関数 例: iGroupDisp / (path)->function(path)
     */
    public void fileSelectDialog(final String dir, final String filter, final boolean getDir, final Consumer<String> operation) {
        fileSelectDialog(mC, dir,filter, getDir, operation);
    }

    /**
     * ファイル選択ダイヤログ(API24(Android7)以降で対応)
     * AndroidStudioではラムダ式が使えないので pathには関数型インタフェースのメソッドを使って対応する
     * 例   Consumer<String> iGraphDisp = new Consumer<String>() {
     *          @Override public void accept(String s) {
     *              graphDisp(s, true);
     *          }
     *      };
     *
     * @param c      コンテキスト
     * @param dir    検索ディレクトリ
     * @param filter ファイル選択のフィルタ(ワイルドカード)
     * @param getDir ディレクトリの取得の可否(ディレクトリの移動も行う)
     * @param path   選択したファイルのフルパスで実行する関数 例: iGroupDisp / (path)->function(path)
     */
    public void fileSelectDialog(final Context c, final String dir, final String filter, final boolean getDir, final Consumer path) {
        final List<String> fileList = getDirFileList(dir, filter, getDir);     //  ファイルリストの取得
        fileList.sort(fileComparator);                                              //  ファイルリストのソート
        final String[] files = new String[fileList.size()];
        for (int i = 0; i < fileList.size(); i++)
            files[i] = fileList.get(i);
        new AlertDialog.Builder(c)
                .setTitle("ファイル選択 [" + getFullPath(dir) + "]")
                .setItems(files, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(c, which + " " + files[which] + " が選択", Toast.LENGTH_LONG).show();
                        String fname = fileList.get(which);
                        if (fname.charAt(0) == '[') {         //  ディレクトリの時はファイル選択のダイヤログを開きなおす
                            String directory = getFullPath(dir + "/" + fname.substring(1, fname.length() - 1));
                            fileSelectDialog(c, directory, filter, getDir, path);
                        } else {                            //  ファイルを選択したときは与えられた関数を実行
                            path.accept(dir + "/" + fname);
                        }

                    }
                })
                .create()
                .show();
    }

    /**
     * ファイルリストのソートコンパレータ
     * [] のディレクトリを先にし文字でソート
     */
    Comparator<String> fileComparator = new Comparator<String>() {
        @Override
        public int compare(String s, String t1) {
            int n = 0 < s.compareToIgnoreCase(t1) ? 1 : -1;
            if (s.charAt(0) == '[' && t1.charAt(0) != '[')
                n = -1;
            if (t1.charAt(0) == '[' && s.charAt(0) != '[')
                n = 1;
            return n;
        }
    };

    /**
     * メニュー選択ダイヤログ
     * operationインターフェース関数例
     *  Consumer<String> iOperation = new Consumer<String>() {
     *      @Override
     *      public void accept(String s) {
     *          if (s.compareTo("全選択")==0) {
     *              mEditText.selectAll();
     *          } else if (s.compareTo("貼付け")==0) {
     *                  :
     *          }
     *      }
     *  };
     *
     * @param title     ダイヤログのタイトル
     * @param menu      メニューデータ(配列)
     * @param operation 実行関数
     */
    public void setMenuDialog(final Context c, final String title, final String[] menu, final Consumer<String> operation) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setItems(menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(c, which + " " + menu[which] + " が選択", Toast.LENGTH_LONG).show();
                        operation.accept(menu[which]);
                    }
                })
                .create()
                .show();
    }


    /***
     * ローカルファイルを関連付けで開く
     * FileProvider を使う(参考: https://www.petitmonte.com/java/android_fileprovider.html)
     * FileProvider を使うためには manifest.xml に「プロバイダ」を定義する
     * my_provider.xml に「プロバイダで使用するpath」を定義する
     *
     * @param path      開くファイルのパス
     */
    public void executeFile(final Context c, String path) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(c,
                    c.getApplicationContext().getPackageName() + ".provider",
                    new File(path));
            intent.setDataAndType(uri, getMimeType(path));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            c.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mC, "executeFile: "+e.getMessage()+" "+path, Toast.LENGTH_LONG);
            Log.d(TAG,"executeFile: "+e.getMessage()+" "+path);
        }
    }

    /**
     * Webを表示する
     *
     * @param c   コンテキスト
     * @param url WebページのURL
     */
    public void webDisp(Context c, String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        c.startActivity(intent);
    }

    /**
     * 緯度経度から住所を求める
     * getAddressの読み出しにはスレッドを利用しないといけない
     *
     * @param latitude  緯度
     * @param longitude 経度
     * @return 住所文字列
     */
    public String getAddress(final String latitude, final String longitude) {
        Log.d(TAG, "getAddress:" + latitude + " , " + longitude);
        // 住所の取得
        StringBuffer strAddr = new StringBuffer();
        Geocoder gcoder = new Geocoder(mC, Locale.getDefault());
        try {
            List<Address> lstAddrs = gcoder.getFromLocation(Double.valueOf(latitude), Double.valueOf(longitude), 1);
            for (Address addr : lstAddrs) {
                int idx = addr.getMaxAddressLineIndex();
                for (int i = 1; i <= idx; i++) {
                    strAddr.append(addr.getAddressLine(i));
                    Log.v("addr", addr.getAddressLine(i));
                }
            }
            return strAddr.toString();
//            Toast.makeText(c, strAddr.toString(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

//    /**
//     * 緯度経度から住所を求める
//     * getAddressの読み出しにはスレッドを利用しないといけない
//     * @param latitude
//     * @param longitude
//     * @return
//     */
//    @SuppressLint("NewApi")
//    public String getAddress(final String latitude, final String longitude) {
//        Log.d(TAG,"getAddress:"+latitude+" , "+longitude);
//        String url = String.format("http://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&sensor=true&language=ja",
//                latitude,longitude);
//        HttpGet get = new HttpGet(url);
//        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("GeoCoding Sample");
//        try {
//            HttpResponse response = httpClient.execute(get);
//            String content = getContent(response);
//            JSONObject jsonObj = new JSONObject(content);
//            return jsonObj.getJSONArray("results").getJSONObject(0).getString("formatted_address");
//
//        } catch (IOException e) {
//            Log.e(TAG,"IOException",e);
//        } catch (JSONException e) {
//            Log.e(TAG,"JSONException",e);
//        } finally {
//            if (httpClient != null) {
//                httpClient.close();
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 入力ストリーム(InputStream型)を文字列に変換
//     * @param response
//     * @return
//     * @throws IllegalStateException
//     * @throws IOException
//     */
//    private String getContent(HttpResponse response)
//            throws IllegalStateException, IOException {
//        Log.d(TAG,"getContent");
//        StringBuffer sb = new StringBuffer();
//        String line = null;
//        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//        while ((line = br.readLine()) != null ) {
//            sb.append(line);
//        }
//        return sb.toString();
//    }

    /**
     * プリファレンスから論理値を取得
     *
     * @param key
     * @param context
     * @return
     */
    public Boolean getBoolPreferences(String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
    }

    /**
     * プリファレンスに論理値を設定
     *
     * @param value
     * @param key
     * @param context
     */
    public void setBoolPreferences(Boolean value, String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    /**
     * プリファレンスから文字列の値を取得
     *  データがない時は"###"を返す
     * @param key     データのキーワード
     * @param context コンテキスト
     * @return 取得したデータ
     */
    public String getStrPreferences(String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, "###");
    }

    /**
     * プリファレンスに文字列を設定
     *
     * @param value   設定するデータ
     * @param key     データのキーワード
     * @param context コンテキスト
     */
    public void setStrPreferences(String value, String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * プリファレンスから数値(int)を取得
     *
     * @param key
     * @param context
     * @return
     */
    public int getIntPreferences(String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(key, 0);
    }

    /**
     * プリファレンスに数値(int)を設定
     *
     * @param value
     * @param key
     * @param context
     */
    public void setIntPreferences(int value, String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    /**
     * プリファレンスから数値(long)を取得
     *
     * @param key
     * @param context
     * @return
     */
    public long getLongPreferences(String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(key, 0);
    }

    /**
     * プリファレンスに数値(long)を設定
     *
     * @param value
     * @param key
     * @param context
     */
    public void setLongPreferences(long value, String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    /**
     * プリファレンスから数値(float)を取得
     *
     * @param key
     * @param context
     * @return
     */
    public float getFloatPreferences(String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(key, 0f);
    }

    /**
     * プリファレンスに数値(float)を設定
     *
     * @param value
     * @param key
     * @param context
     */
    public void setFloatPreferences(float value, String key, Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    /**
     * グラフ表示のための最小値を求める
     *
     * @param minData データの最小値
     * @param maxData データの最大値
     * @return グラフ表示の最小値
     */
    public double graphRangeMin(double minData, double maxData) {
        double range = maxData - minData;
        double magRange = magnitudeNum(range);
        double magMin = Math.floor((double) (minData / magRange)) * magRange;
        while (minData <= magMin)
            magMin = magMin - magRange;
        return magMin;
    }

    /**
     * グラフ表示のための最大値を求める
     *
     * @param minData データの最小値
     * @param maxData データの最大値
     * @return グラフ表示の最大値
     */
    public double graphRangeMax(double minData, double maxData) {
        double range = maxData - minData;
        double magRange = magnitudeNum(range);
        double magMax = Math.floor((double) (maxData / magRange)) * magRange;
        while (maxData >= magMax)
            magMax = magMax + magRange;
        return magMax;
    }

    /***
     * 桁を数値で求める (20.23 →　10, 0.23 → 0.1
     * @param val       数値
     * @return 桁の数値
     */
    public double magnitudeNum(double val) {
        double mag = (float) Math.floor(Math.log10(Math.abs(val)));
        return Math.pow(10, mag) * Math.signum(val);
    }

    /**
     * グラフ作成時の補助線間隔を求める
     * 補助線の間隔は1,2,5の倍数
     *
     * @param range         補助線の範囲
     * @param targetSteps   補助線の数
     * @return              補助線の間隔
     */
    public float graphStepSize(float range, float targetSteps) {
        //Log.d(TAG, "graphStepSize:"+range+","+ targetSteps);
        // calculate an initial guess at step size
        float tempStep = range / targetSteps;

        // get the magnitude of the step size
        float mag = (float) Math.floor(Math.log10(tempStep));
        float magPow = (float) Math.pow(10, mag);

        // calculate most significant digit of the new step size
        float magMsd = (int) (tempStep / magPow + 0.5);

        // promote the MSD to either 1, 2, or 5
        if (magMsd > 5.0)
            magMsd = 10.0f;
        else if (magMsd > 2.0)
            magMsd = 5.0f;
        else if (magMsd > 1.0)
            magMsd = 2.0f;

        return magMsd * magPow;
    }

    /**
     * 上位桁数
     *
     * @param val
     * @param n
     * @return
     */
    public float updigitnumber(float val, int n) {
        float mag = (float) Math.floor(Math.log10(val));     //  桁数
        float magPow = (float) Math.pow(10, mag - n + 1);    //
        return (float) Math.floor(val / magPow);
    }

    /**
     * 指定の上位桁数で丸める(切り捨て)
     *
     * @param val 数値
     * @param n   丸める有効桁
     * @return
     */
    public float roundFloor(float val, int n) {
        float mag = (float) Math.floor(Math.log10(val));     //  桁数
        float magPow = (float) Math.pow(10, mag - n + 1);    //
        return (float) Math.floor(val / magPow) * magPow;
    }

    /**
     * 指定の上位桁数で丸める(切り上)
     *
     * @param val 数値
     * @param n   丸める有効桁
     * @return
     */
    public float roundCeil(float val, int n) {
        float mag = (float) Math.floor(Math.log10(val));
        float magPow = (float) Math.pow(10, mag - n + 1);
        return (float) Math.ceil(val / magPow) * magPow;
    }

    /**
     * 数値丸め処理(小数点以下桁数の設定)
     *
     * @param value 数値
     * @param n     小数点以下桁数
     * @return      丸め数値文字列
     */
    public String roundStr(double value, int n) {
        double round = Math.pow(10, n);
        String strNo = Double.toString(Math.floor(value * round) / round);
        return strNo;
//    	return strNo.substring(0, strNo.indexOf('.')+n);
    }

    /**
     * 数値丸め処理(小数点以下桁数の設定)
     * 小数点以下桁数が0以上の場合、桁数分で数値を捨てる(誤差調整)
     *
     * @param value 数値
     * @param n     小数点以下桁数
     * @return      丸め数値文字列
     */
    public String roundStr2(double value, int n) {
        double round = Math.pow(10, n);
        String strNo = Double.toString(Math.floor(value * round) / round);
        return strNo.substring(0, strNo.indexOf('.') + (n < 0 ? 0 : n));
    }

    public String roundStrDouble2Str(String val, int n) {
        double v = str2Double(val);
        return roundStr(v, n);
    }

    /**
     * 文字列を実数に変換
     * 文字列が数値でない場合は0を返す
     *
     * @param str
     * @return
     */
    public double str2Double(String str) {
        if (isFloat(str))
            return Double.valueOf(str);
        else
            return 0.0;
    }

    /**
     * 文字列を整数に変換
     * 文字列が数値でない場合は0を返す
     *
     * @param str
     * @return
     */
    public int str2Integer(String str) {
        if (isNumber(str))
            return Integer.valueOf(str);
        else
            return 0;
    }

    /**
     * 文字列が数値かどうかの判定
     *
     * @param val : 文字列
     * @return
     */
    public boolean isFloat(String val) {
        try {
            Double.parseDouble(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 文字列が整数かの判定
     *
     * @param val 文字列
     * @return
     */
    public boolean isNumber(String val) {
        try {
            Integer.parseInt(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getStrNumber(String str) {
        String num = "";
        int i = 0;
        while (Character.isDigit(str.charAt(i)) || str.charAt(i)=='.') {
            num += str.charAt(i++);
        }
        return num;
    }

    /**
     * 数値データとして不要な０を取る
     *
     * @param val
     * @return
     */
    public String zeroSupressNumber(String val) {
        String buf = "";
        if (val.length() == 0 || (val.length() == 1 && val.charAt(0) == '-'))
            return "0";
        for (int i = 0; i < val.length(); i++) {
            if (val.charAt(i) == '-') {
                buf += val.charAt(i);
            } else if (val.charAt(i) == '.') {
                buf += "0" + val.substring(i);
                break;
            } else if (val.charAt(i) != '0') {
                buf += val.substring(i);
                break;
            }
        }
        int n = Math.max(buf.indexOf('E'), buf.indexOf('e'));
        if (0 <= n) {
            val = buf;
            buf = val.substring(0, n + 1);
            for (int m = n + 1; m < val.length(); m++) {
                if (val.charAt(m) == '-') {
                    buf += val.charAt(m);
                } else if (val.charAt(m) == '.') {
                    buf += "0" + val.substring(m);
                    break;
                } else if (val.charAt(m) != '0') {
                    buf += val.substring(m);
                    break;
                }
            }
        }
        return buf;
    }

    /***
     * doubleデータを数値文字列にする 桁区切り(3桁)を入れる
     * @param val   double 値
     * @return 数値文字列
     */
    public String double2DigitSeparatorStr(double val) {
        return setDigitSeparator(String.valueOf(val));
    }

    /***
     * intデータを数値文字列にする 桁区切り(3桁)を入れる
     * @param val   int 値
     * @return 数値文字列
     */
    public String int2DigitSeparatorStr(int val) {
        return setDigitSeparator(String.valueOf(val));
    }

    /***
     * long データを数値文字列にする 桁区切り(3桁)を入れる
     * @param val   long 値
     * @return 数値文字列
     */
    public String long2DigitSeparatorStr(long val) {
        return setDigitSeparator(String.valueOf(val));
    }

    /**
     * 文字列が数値の場合、桁区切り(3桁)を入れる
     *
     * @param val
     * @return
     */
    public String setDigitSeparator(String val) {
        if (!isFloat(val))
            return val;
        //  '.''E''e'以降に桁区切りを入れないために対象の文字列長さを求める
        String buf = "";
        int n = val.indexOf('.');
        int e = Math.max(val.indexOf('E'), val.indexOf('e'));
        if (e < 0 && n < 0)
            n = val.length();
        else if (e < 0 && 0 <= n)
            buf = val.substring(n);
        else if (0 <= e && n < 0) {
            n = e;
            buf = val.substring(n);
        } else
            buf = val.substring(n);
        //  桁区切りを入れる
        int m = 1;
        for (int i = n - 1; 0 <= i; i--) {
            buf = val.charAt(i) + buf;
            if ((val.charAt(0) != '-' && 0 < i) || (val.charAt(0) == '-' && 1 < i)) {
                if (m++ % 3 == 0)
                    buf = ',' + buf;
            }
        }
        return buf;
    }

    /**
     * 度分秒表記(WGS-84)を度表記に変換する
     *
     * @param dms ddd.mmss
     * @return degree  ddd.dddd
     */
    public String cnvDMS2Deg(String dms) {
        int n = dms.indexOf('.');
        String d = dms.substring(0, n);
        String m = dms.substring(n + 1, n + 1 + 2);
        String s = dms.substring(n + 1 + 2);
        s = s.substring(0, 2) + "." + s.substring(2);
        float df = Float.valueOf(d) + Float.valueOf(m) / 60f + Float.valueOf(s) / 3600f;
        return String.valueOf(df);
    }

    /**
     * 度分秒表記(WGS-84)を度表記に変換する
     *
     * @param dm dddmm.mmmm
     * @return degree  ddd.dddd
     */
    public String cnvDM2Deg(String dm) {
        int n = dm.indexOf('.');
        String d = dm.substring(0, n - 2);
        String m = dm.substring(n - 2);
        float df = Float.valueOf(d) + Float.valueOf(m) / 60f;
        return String.valueOf(df);
    }

    //	地球上の２地点の緯度・経度を指定して最短距離とその方位角を計算
    //	地球を赤道半径r=6378.137kmを半径とする球体として計算しています。
    //	方位角は北:0度、東:90度、南:180度、西:270度。
    //	地点A(経度x1, 緯度y1)、地点B(経度x2, 緯度y2)
    //	ABの距離(km) d = r*acos(sin(y1)*sin(y2)+cos(y1)*cos(y2)*cos(x2-x1))
    //	方位角　φ = 90 - atan2(sin(x2-x1), cos(y1)*tan(y2) - sin(y1)*cos(x2-x1))
    //	http://keisan.casio.jp/has10/SpecExec.cgi

    /**
     * 球面上の2点間座標の距離
     *
     * @param longi1 座標1経度
     * @param lati1  座標1緯度
     * @param longi2 座標2経度
     * @param lati2  座標2緯度
     * @return 距離(km)
     */
    public double distance(double longi1, double lati1, double longi2, double lati2) {
        double r = 6378.137;
        double x1 = longi1 / 180 * Math.PI;
        double y1 = lati1 / 180 * Math.PI;
        double x2 = longi2 / 180 * Math.PI;
        double y2 = lati2 / 180 * Math.PI;
        double dis = r * Math.acos(Math.sin(y1) * Math.sin(y2) + Math.cos(y1) * Math.cos(y2) * Math.cos(x2 - x1));
        return Double.isNaN(dis) ? 0 : dis;
    }

    /**
     * 球面上の2点間座標の方位
     *
     * @param longi1 座標1経度
     * @param lati1  座標1緯度
     * @param longi2 座標2経度
     * @param lati2  座標2緯度
     * @return 方位角(°)
     */
    public double azimuth(double longi1, double lati1, double longi2, double lati2) {
        double x1 = longi1 / 180 * Math.PI;
        double y1 = lati1 / 180 * Math.PI;
        double x2 = longi2 / 180 * Math.PI;
        double y2 = lati2 / 180 * Math.PI;
        double phai = 90 - (Math.atan2(Math.sin(x2 - x1), Math.cos(y1) * Math.tan(y2) - Math.sin(y1) * Math.cos(x2 - x1))) * 180 / Math.PI;
        return phai;
    }

    //    以下の方法は地球を球体とみなして球面三角法で解く簡便なものなので測量には使えませんが、
    //    日本付近の緯度での２点間の距離400kmほどで誤差は0.1度を少し超える程度です。
    //
    //    地点Aの経度と緯度をそれぞれL1,B1とし、
    //    地点Bの経度と緯度をそれぞれL2,B2とし、
    //    地点Aからみた地点Bの方位（真北を0度として東回りにはかった角度）をθとすると以下の手順で
    //    求められます。
    //
    //    Y = cos(B2) * sin(L2 - L1)
    //    X = cos(B1) * sin(B2) - sin(B1) * cos(B2) * cos(L2 - L1)
    //    θ[rad] = atan2(Y, X)
    //    もし θ[rad]<0 なら θ = θ + 2π とし結果を0から2π未満に収めます。
    //    θ[deg] = θ[rad] * 180 / π
    //
    //    ※ 「*」は乗算、「/」は除算、sin()は正弦関数、cos()は余弦関数、
    //    　　atan2(y, x)は逆正接関数（返り値は-2π～+2π）、
    //    　　θ[rad]は弧度法でのラジアン単位の角度、θ[deg]は度単位の角度をそれぞれ表す。
    //    　　経度は東経を「+」西経を「-」、緯度は北緯を「+」南緯を「-」の数として扱います。
    //	http://oshiete.goo.ne.jp/qa/721140.html

    /**
     * 球面上の2点間座標の方位
     *
     * @param longi1 座標1経度
     * @param lati1  座標1緯度
     * @param longi2 座標2経度
     * @param lati2  座標2緯度
     * @return 方位角(°)
     */
    public double azimuth2(double longi1, double lati1, double longi2, double lati2) {
        double l1 = longi1 / 180 * Math.PI;        //	経度
        double b1 = lati1 / 180 * Math.PI;        //	緯度
        double l2 = longi2 / 180 * Math.PI;
        double b2 = lati2 / 180 * Math.PI;
        double Y = Math.cos(b2) * Math.sin(l2 - l1);
        double X = Math.cos(b1) * Math.sin(b2) - Math.sin(b1) * Math.cos(b2) * Math.cos(l2 - l1);
        double phai = (Math.atan2(Y, X)) * 180 / Math.PI;
        return phai < 0 ? phai + 360 : phai;
    }

    /**
     * 秒時間を時分秒にフォーマット(hh:mm:ss)する
     *
     * @param sec
     * @return
     */
    public String Sec2Time(long sec) {
        return String.format("%02d:%02d:%02d", sec / 3600, (sec / 60) % 60, sec % 60);
    }

    /**
     * 時分秒(hh:mm:ss)を秒数に変換する
     *
     * @param time
     * @return
     */
    public long Time2Sec(String time) {
        if (time.length() != 8)
            return 0;
        long hh = Long.valueOf(time.substring(0, 2));
        long mm = Long.valueOf(time.substring(3, 5));
        long ss = Long.valueOf(time.substring(6, 8));
        return hh * 3600 + mm * 60 + ss;
    }

    /**
     * 日付(yyyy/mm/dd)と時間(hh:mm:ss)からUTCミリ秒を取得
     *
     * @param date
     * @param time
     * @return
     */
    public long getTimeMills(String date, String time) {
        Calendar cal = Calendar.getInstance();
        cal.set(getYear(date), getMonth(date), getDay(date),
                getHour(time), getMin(time), getSec(time));
        return cal.getTimeInMillis();
    }

    /**
     * UTCミリ秒をString形式に変換
     *
     * @param format 表示フォーマット "yyyy/MM/dd HH:mm:ss"
     * @param time
     * @return
     */
    public String TimeMills2Str(String format, long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        String str;
        if (cal == null)
            str = null;
        else
            str = new SimpleDateFormat(format).format(cal.getTime());
        return str;
    }


    /**
     * 日付(yyyy/MM/dd or yyyyMMdd)から年データを抽出
     *
     * @param date
     * @return 年
     */
    public int getYear(String date) {
        if (0 <= date.indexOf('/')) {
            String[] str = date.split("/", 0);
            return Integer.valueOf(str[0]);
        } else {
            return Integer.valueOf(date.substring(0, 4));
        }
    }

    /**
     * 日付(yyyy/mm/dd or yyyyMMdd)から月データを抽出
     *
     * @param date
     * @return 月
     */
    public int getMonth(String date) {
        if (0 <= date.indexOf('/')) {
            String[] str = date.split("/", 0);
            return Integer.valueOf(str[1]) - 1;
        } else {
            return Integer.valueOf(date.substring(4, 6)) - 1;
        }
    }

    /**
     * 日付(yyyy/mm/dd or yyyyMMdd)から日データを抽出
     *
     * @param date
     * @return 日
     */
    public int getDay(String date) {
        if (0 <= date.indexOf('/')) {
            String[] str = date.split("/", 0);
            return Integer.valueOf(str[2]);
        } else {
            return Integer.valueOf(date.substring(6, 8));
        }
    }

    /**
     * 時間(hh:mm:ss)から時データを抽出
     *
     * @param time
     * @return hh
     */
    public int getHour(String time) {
        if (0 <= time.indexOf(':')) {
            String[] str = time.split(":", 0);
            return Integer.valueOf(str[0]);
        } else {
            return Integer.valueOf(time.substring(0, 2));
        }
    }

    /**
     * 時間(hh:mm:ss)から分データを抽出
     *
     * @param time
     * @return mm
     */
    public int getMin(String time) {
        if (0 <= time.indexOf(':')) {
            String[] str = time.split(":", 0);
            return Integer.valueOf(str[1]);
        } else {
            return Integer.valueOf(time.substring(2, 4));
        }
    }

    /**
     * 時間(hh:mm:ss)から秒データを抽出
     *
     * @param time
     * @return ss
     */
    public int getSec(String time) {
        if (0 <= time.indexOf(':')) {
            String[] str = time.split(":", 0);
            return Integer.valueOf(str[2]);
        } else {
            return Integer.valueOf(time.substring(4, 6));
        }
    }

    /**
     * Calendar時刻を日時フォーマットする
     * YY/MM/DD hh/mm/ss
     *
     * @param cal
     * @return
     */
    public String CalendarString(Calendar cal) {
        int year = cal.get(Calendar.YEAR);        //(2)現在の年を取得
        int month = cal.get(Calendar.MONTH) + 1;  //(3)現在の月を取得
        int day = cal.get(Calendar.DATE);         //(4)現在の日を取得
        int hour = cal.get(Calendar.HOUR_OF_DAY); //(5)現在の時を取得
        int minute = cal.get(Calendar.MINUTE);    //(6)現在の分を取得
        int second = cal.get(Calendar.SECOND);    //(7)現在の秒を取得
        return String.format("%s/%02d/%02d %02d:%02d:%02d",
                String.valueOf(year).substring(2, 4), month, day, hour, minute, second);
    }

    /**
     * ひと月の日数を求める
     *
     * @param date yyyy/mm/dd or yyyymmdd
     * @return
     */
    public int getMonthLength(String date) {
        int year, month, day;
        if (0 <= date.indexOf('/')) {
            String[] str = date.split("/", 0);
            year = Integer.valueOf(str[0]);
            month = Integer.valueOf(str[1]);
            day = Integer.valueOf(str[2]);
        } else {
            year = Integer.valueOf(date.substring(0, 4));
            month = Integer.valueOf(date.substring(4, 6));
            day = Integer.valueOf(date.substring(6, 8));
        }
        int MonthDay = Date2JulianDay(year, month, 1);
        if (12 <= month) {
            year++;
            month = 0;
        }
        int nextMonthDay = Date2JulianDay(year, month + 1, 1);
        return nextMonthDay - MonthDay;
    }

    /**
     * 年と週目を指定して週の最初の日付(日曜日)をユリウス日で返す
     *
     * @param year
     * @param weekNo
     * @return
     */
    public String getStartWeekDay(int year, int weekNo) {
        return getStartWeekDay(year, weekNo, true);
    }

    /**
     * 年と週目を指定して週の最初の日付(日曜日)をユリウス日で返す
     *
     * @param year
     * @param weekNo
     * @param sp     セパレータの有無
     * @return yyyy/mm/dd or yyyymmdd
     */
    public String getStartWeekDay(int year, int weekNo, boolean sp) {
        int startDay = Date2JulianDay(year, 1, 1);    //	年初１月１日のユリウス日
        int startWeekNo = getDayWeek(year, 1, 1);    //	年初１月１日の曜日
        int day = (weekNo - 1) * 7 - startWeekNo;
        int jd = startDay + day;
        return JulianDay2DateYear(jd, sp);
    }

    /**
     * 日付から何週目かを算出する １月１日が第１週目
     *
     * @param date yyyy/mm/dd or yyyymmdd
     * @return 週目
     */
    public int getWeekNo(String date) {
        int year, month, day;
        if (0 <= date.indexOf('/')) {
            String[] str = date.split("/", 0);
            year = Integer.valueOf(str[0]);
            month = Integer.valueOf(str[1]);
            day = Integer.valueOf(str[2]);
        } else {
            year = Integer.valueOf(date.substring(0, 4));
            month = Integer.valueOf(date.substring(4, 6));
            day = Integer.valueOf(date.substring(6, 8));
        }
        return getWeekNo(year, month, day);
    }

    /**
     * 日付から何週目かを算出する １月１日が第１週目
     *
     * @param year
     * @param month
     * @param day
     * @return 週目
     */
    public int getWeekNo(int year, int month, int day) {
        int startDay = Date2JulianDay(year, 1, 1);    //	年初１月１日のユリウス日
        int startWeekNo = getDayWeek(year, 1, 1);    //	年初１月１日の曜日
        int dayY = Date2JulianDay(year, month, day) - startDay;
        return (dayY + startWeekNo) / 7 + 1;
    }

    /**
     * 西暦から曜日を求める
     *
     * @param year
     * @param month
     * @param day
     * @return 0=日,1=月,?....6=土
     */
    public int getDayWeek(int year, int month, int day) {
        if (month < 3) {
            year--;
            month += 12;
        }
        return (year + year / 4 - year / 100 + year / 400 + (13 * month + 8) / 5 + day) % 7;
    }

    /**
     * 年月日からユリウス日を求める
     *
     * @param date yyyy/mm/dd or yyyymmdd
     * @return
     */
    public int Date2JulianDay(String date) {
        int year, month, day;
        if (0 <= date.indexOf('/')) {
            String[] str = date.split("/", 0);
            year = Integer.valueOf(str[0]);
            month = Integer.valueOf(str[1]);
            day = Integer.valueOf(str[2]);
        } else {
            year = Integer.valueOf(date.substring(0, 4));
            month = Integer.valueOf(date.substring(4, 6));
            day = Integer.valueOf(date.substring(6, 8));
        }
        return Date2JulianDay(year, month, day);
    }

    /**
     * 歴日からユリウス日に変換
     *
     * @param year  2018年
     * @param month 10月
     * @param day   5日
     * @return 日
     */
    public int Date2JulianDay(int year, int month, int day) {
        if (month <= 2) {
            month += 12;
            year--;
        }
        if ((year * 12 + month) * 31 + day >= (1582 * 12 + 10) * 31 + 15) {
            //  1582/10/15以降はグレゴリオ暦
            day += 2 - year / 100 + year / 400;
        }
        return (int) Math.floor(365.25 * (year + 4716)) + (int) (30.6001 * (month + 1)) + day - 1524;
    }

    /**
     * ユリウス日から歴日の文字列に変換(セパレータ付き)
     *
     * @param jd
     * @return yyyy/mm/dd
     */
    public String JulianDay2DateYear(int jd) {
        return JulianDay2DateYear(jd, true);
    }

    /**
     * ユリウス日から歴日の文字列に変換
     *
     * @param jd ユリウス日
     * @param sp セパレータの有無
     * @return yyyy/mm/dd or yyyymmdd
     */
    public String JulianDay2DateYear(int jd, boolean sp) {
        if (jd >= 2299161) {
            //  1582/10+15以降はグレゴリオ暦
            int t = (int) ((jd - 1867216.25) / 365.25);
            jd += 1 + t / 100 - t / 400;
        }
        jd += 1524;
        int y = (int) Math.floor(((jd - 122.1) / 365.25));
        jd -= (int) Math.floor(365.25 * y);
        int m = (int) (jd / 30.6001);
        jd -= (int) (30.6001 * m);
        int day = jd;
        int month = m - 1;
        int year = y - 4716;
        if (month > 12) {
            month -= 12;
            year++;
        }
        if (sp)
            return String.format("%04d/%02d/%02d", year, month, day);
        else
            return String.format("%04d%02d%02d", year, month, day);
    }

    /**
     * ユリウス日から西暦の年を求める
     *
     * @param jd ユリウス日
     * @return 年(西暦)
     */
    public int JulianDay2Year(int jd) {
        if (jd >= 2299161) {
            //  1582/10+15以降はグレゴリオ暦
            int t = (int) ((jd - 1867216.25) / 365.25);
            jd += 1 + t / 100 - t / 400;
        }
        jd += 1524;
        int y = (int) Math.floor(((jd - 122.1) / 365.25));
        jd -= (int) Math.floor(365.25 * y);
        int m = (int) (jd / 30.6001);
        jd -= (int) (30.6001 * m);
        int day = jd;
        int month = m - 1;
        int year = y - 4716;
        if (month > 12) {
            month -= 12;
            year++;
        }
        return year;
    }

    /**
     * ユリウス日から月を求める
     *
     * @param jd ユリウス日
     * @return 月(1 - 12)
     */
    public int JulianDay2Month(int jd) {
        if (jd >= 2299161) {
            //  1582/10+15以降はグレゴリオ暦
            int t = (int) ((jd - 1867216.25) / 365.25);
            jd += 1 + t / 100 - t / 400;
        }
        jd += 1524;
        int y = (int) Math.floor(((jd - 122.1) / 365.25));
        jd -= (int) Math.floor(365.25 * y);
        int m = (int) (jd / 30.6001);
        int month = m - 1;
        if (month > 12)
            month -= 12;
        return month;
    }

    /**
     * ユリウス日から月の日を求める
     *
     * @param jd ユリウス日
     * @return 日(1 - 31)
     */
    public int JulianDay2Day(int jd) {
        if (jd >= 2299161) {
            //  1582/10+15以降はグレゴリオ暦
            int t = (int) ((jd - 1867216.25) / 365.25);
            jd += 1 + t / 100 - t / 400;
        }
        jd += 1524;
        int y = (int) Math.floor(((jd - 122.1) / 365.25));
        jd -= (int) Math.floor(365.25 * y);
        int m = (int) (jd / 30.6001);
        jd -= (int) (30.6001 * m);
        return jd;
    }

    /**
     * ユリウス日から曜日を求める(0:月 1:火 2:水 3:木 4:金 5:土 6:日)
     *
     * @param jd ユリウス日
     * @return 曜日
     */
    public int JulianDay2Week(int jd) {
        return jd % 7;
    }

    /**
     * Locationデータから日時文字列を作成
     * Format : "yyyy-MM-dd'T'HH:mm:ss'Z'"
     *
     * @param location
     * @return
     */
    public String getLocationTime(Location location) {
        SimpleDateFormat df = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df.setTimeZone(tz);
        return df.format(location.getTime());
    }

    /**
     * Location(位置情報)データからFormatを指定して日時文字列を取得
     *
     * @param location
     * @param format
     * @return
     */
    public String getLocationTime(Location location, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        TimeZone tz = TimeZone.getTimeZone("UTC");
//		TimeZone tz = TimeZone.getTimeZone("JST");
        df.setTimeZone(tz);
        return df.format(location.getTime() + 9 * 3600 * 1000);
    }

    /**
     * Locationデータによる2点間の距離(km)
     *
     * @param loc
     * @param preloc
     * @return km
     */
    public double locDistance(Location loc, Location preloc) {
        try {
            return distance(loc.getLongitude(), loc.getLatitude(),
                    preloc.getLongitude(), preloc.getLatitude());
        } catch (Exception e) {
            if (mC != null)
                Toast.makeText(mC, "locDistance Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            return 0;
        }
    }

    /**
     * Locationデータによる2点間での速度(km/h)
     *
     * @param loc
     * @param preloc
     * @return km/h
     */
    public double locSpeed(Location loc, Location preloc) {
        try {
            double dis = locDistance(loc, preloc);
            return dis / (loc.getTime() - preloc.getTime()) * 1000f * 3600f;
        } catch (Exception e) {
            if (mC != null)
                Toast.makeText(mC, "locSpeed Error: " + e.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            return 0;
        }
    }

    /**
     * 経過時間(msec)
     *
     * @param loc
     * @param preloc
     * @return
     */
    public long locLapTime(Location loc, Location preloc) {
        try {
            return loc.getTime() - preloc.getTime();
        } catch (Exception e) {
            if (mC != null)
                Toast.makeText(mC, "locLapTime Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return 0;
        }
    }


    /**
     * nビット目の値を1にする
     * @param a     bit配列データ
     * @param n     nビット目
     * @return      変更後のbit配列データ
     */
    public long bitOn(long a, int n) {
        long b = 1;
        b <<= n;
        return a | b;
    }

    /**
     * nビット目の値を0にする
     * @param a     bit配列データ
     * @param n     nビット目
     * @return      変更後のbit配列データ
     */
    public long bitOff(long a, int n) {
        long b = 1;
        b <<= n;
        return a & (~b);
    }

    /**
     * nビット目を反転する
     * @param a     bit配列データ
     * @param n     nビット目
     * @return      変更後のbit配列データ
     */
    public long bitRevers(long a, int n) {
        long b = 1;
        b <<= n;
        return a ^ b;
    }

    /**
     * bitの数を数える (32bitまで)
     * @param bits  数値
     * @return      bit数
     */
    public int bitsCount(long bits) {
        bits = (bits & 0x55555555) + (bits >> 1 & 0x55555555);
        bits = (bits & 0x33333333) + (bits >> 2 & 0x33333333);
        bits = (bits & 0x0f0f0f0f) + (bits >> 4 & 0x0f0f0f0f);
        bits = (bits & 0x00ff00ff) + (bits >> 8 & 0x00ff00ff);
        return (int)((bits & 0x0000ffff) + (bits >> 16 & 0x0000ffff));
    }

    /**
     * bitの数を数える (64bitまで)
     * @param bits  数値
     * @return      bit数
     */
    public int bitsCount2(long bits) {
        long a = bits & 0xffffffff;
        long b = bits >> 32;
        return bitsCount(a) + bitsCount(b);
    }
}
