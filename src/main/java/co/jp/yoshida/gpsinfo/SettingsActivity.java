package co.jp.yoshida.gpsinfo;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
//            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            setPreferencesFromResource(R.xml.gps_service_preference, rootKey);
            preferenceUpdate();
        }

        @Override
        public void onPause() {
            super.onPause();
            //  listenerの解除
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            //  listenerの登録(これを入れないと onSharedPreferenceChanged か呼ばれない)
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            preferenceUpdate();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
            preferenceUpdate();
        }

        /**
         * 設定値の更新に伴う表示の更新
         */
        private void preferenceUpdate() {
            String str;
            //	GPS測定間隔時間
            ListPreference list_GPSminTime = (ListPreference)getPreferenceScreen().findPreference(getString(R.string.key_GPSminTime));
            str = list_GPSminTime.getValue().compareTo("0")==0?"なし":list_GPSminTime.getValue() + " 秒";
            list_GPSminTime.setSummary("位置情報更新の最低時間 = " + str);
            //	GPS測定間隔距離
            ListPreference list_GPSminDistance = (ListPreference)getPreferenceScreen().findPreference(getString(R.string.key_GPSminDistance));
            str = list_GPSminDistance.getValue().compareTo("0")==0?"なし":list_GPSminDistance.getValue() + " m";
            list_GPSminDistance.setSummary("位置情報更新の最低距離 = " + str);
        }
    }
}