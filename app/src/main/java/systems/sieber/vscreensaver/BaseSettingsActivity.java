package systems.sieber.vscreensaver;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

public class BaseSettingsActivity extends AppCompatActivity {

    static final String SHARED_PREF_DOMAIN = "SCREENSAVER";
    SharedPreferences mSharedPref;

    View mViewColorChangerBackground;
    View mViewBackgroundColorPreview;
    View mViewColorChangerClock;
    View mViewClockColorPreview;
    CheckBox mCheckBoxLoop;
    CheckBox mCheckBoxStretch;
    SeekBar mSeekBarVolume;
    CheckBox mCheckBoxClock;
    CheckBox mCheckBoxHrs24;
    CheckBox mCheckBoxDate;
    EditText mEditTextDateFormat;
    RadioButton mRadioButtonTop;
    RadioButton mRadioButtonBottom;
    RadioButton mRadioButtonCenterY;
    RadioButton mRadioButtonLeft;
    RadioButton mRadioButtonRight;
    RadioButton mRadioButtonCenterX;
    Button mButtonUnlockSettings;
    LinearLayout mLinearLayoutPurchaseContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mSharedPref = getSharedPreferences(SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);

        // apply the insets as a margin to the view, so that elements at the bottom
        // of the ScrollView do not get hidden behind the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.textViewBuildInfo), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            //mlp.leftMargin = insets.left;
            //mlp.rightMargin = insets.right;
            mlp.bottomMargin = insets.bottom;
            v.setLayoutParams(mlp);
            // Return CONSUMED if you don't want the window insets to keep passing down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // display version & flavor
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            setTitle(getTitle() + " " + pInfo.versionName);
            ((TextView) findViewById(R.id.textViewBuildInfo)).setText(
                    getString(R.string.version) + " " + pInfo.versionName + " (" + pInfo.versionCode + ") " + BuildConfig.BUILD_TYPE + " " + BuildConfig.FLAVOR
            );
        } catch(PackageManager.NameNotFoundException ignored) { }

        // hide dream settings button if not supported
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                || uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            findViewById(R.id.buttonDreamSettings).setVisibility(View.GONE);
        }

        // show screensaver note on FireTV
        if(getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) {
            findViewById(R.id.textViewFireTvNotes).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonDreamSettings).setVisibility(View.GONE);
        }

        // show info regarding "High Contrast Text" system setting
        if(isHighContrastTextEnabled(getBaseContext())) {
            findViewById(R.id.textViewHighContrastNotes).setVisibility(View.VISIBLE);
        }

        // find views
        mViewColorChangerBackground = findViewById(R.id.viewColorChangerBackground);
        mViewBackgroundColorPreview = findViewById(R.id.viewColorPreviewBackground);
        mViewColorChangerClock = findViewById(R.id.viewColorChangerClock);
        mViewClockColorPreview = findViewById(R.id.viewColorPreviewClock);
        mCheckBoxLoop = findViewById(R.id.checkBoxLoop);
        mCheckBoxStretch = findViewById(R.id.checkBoxStretch);
        mSeekBarVolume = findViewById(R.id.seekBarVolume);
        mCheckBoxClock = findViewById(R.id.checkBoxClock);
        mCheckBoxHrs24 = findViewById(R.id.checkBoxHrs24);
        mCheckBoxDate = findViewById(R.id.checkBoxDate);
        mEditTextDateFormat = findViewById(R.id.editTextDateFormat);
        mRadioButtonTop = findViewById(R.id.radioButtonTop);
        mRadioButtonBottom = findViewById(R.id.radioButtonBottom);
        mRadioButtonCenterY = findViewById(R.id.radioButtonCenterY);
        mRadioButtonLeft = findViewById(R.id.radioButtonLeft);
        mRadioButtonRight = findViewById(R.id.radioButtonRight);
        mRadioButtonCenterX = findViewById(R.id.radioButtonCenterX);
        mButtonUnlockSettings = findViewById(R.id.buttonUnlockSettings);
        mLinearLayoutPurchaseContainer = findViewById(R.id.linearLayoutInAppPurchase);
        loadSettings();

        // init color picker
        mViewColorChangerBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(null, ((ColorDrawable)mViewBackgroundColorPreview.getBackground()).getColor(), false, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        updateColorPreview(Color.argb(0xff, red, green, blue), mViewBackgroundColorPreview, null);
                    }
                });
            }
        });
        mViewColorChangerClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(null, ((ColorDrawable)mViewClockColorPreview.getBackground()).getColor(), false, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        updateColorPreview(Color.argb(0xff, red, green, blue), mViewClockColorPreview, null);
                    }
                });
            }
        });
    }

    private void updateColorPreview(int color, View previewView, EditText hexTextBox) {
        previewView.setBackgroundColor(Color.argb(0xff, Color.red(color), Color.green(color), Color.blue(color)));
        if(hexTextBox != null) hexTextBox.setText(String.format("#%06X", (0xFFFFFF & color)));
    }
    interface ColorDialogCallback {
        void ok(boolean customColor, int red, int green, int blue, boolean applyForAll);
    }
    private void showColorDialog(Boolean customColor, int initialColor, boolean showApplyForAll, final ColorDialogCallback colorDialogFinished) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_color);
        final CheckBox checkBoxCustomColor = ad.findViewById(R.id.checkBoxCustomColor);
        final EditText editTextColorHex = ad.findViewById(R.id.editTextColorHex);
        final SeekBar seekBarRed = ad.findViewById(R.id.seekBarRed);
        final SeekBar seekBarGreen = ad.findViewById(R.id.seekBarGreen);
        final SeekBar seekBarBlue = ad.findViewById(R.id.seekBarBlue);
        final View colorPreview = ad.findViewById(R.id.viewColorPreview);
        final Button buttonOkForAll = ad.findViewById(R.id.buttonOkForAll);
        final Button buttonOK = ad.findViewById(R.id.buttonOK);
        if(!showApplyForAll) {
            buttonOkForAll.setVisibility(View.GONE);
        }
        if(customColor == null) {
            checkBoxCustomColor.setVisibility(View.GONE);
        } else {
            checkBoxCustomColor.setChecked(customColor);
            if(!customColor) {
                seekBarRed.setEnabled(false);
                seekBarGreen.setEnabled(false);
                seekBarBlue.setEnabled(false);
                editTextColorHex.setEnabled(false);
            }
        }
        checkBoxCustomColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                seekBarRed.setEnabled(b);
                seekBarGreen.setEnabled(b);
                seekBarBlue.setEnabled(b);
                editTextColorHex.setEnabled(b);
            }
        });
        seekBarRed.setProgress(Color.red(initialColor));
        seekBarGreen.setProgress(Color.green(initialColor));
        seekBarBlue.setProgress(Color.blue(initialColor));
        seekBarRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        editTextColorHex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    int newColor = Color.parseColor(charSequence.toString());
                    seekBarRed.setProgress(Color.red(newColor));
                    seekBarGreen.setProgress(Color.green(newColor));
                    seekBarBlue.setProgress(Color.blue(newColor));
                    //updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, null);
                } catch(Exception ignored) { }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
        updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
        ad.show();
        ad.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(colorDialogFinished != null) {
                    colorDialogFinished.ok(checkBoxCustomColor.isChecked(), seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), false);
                }
                ad.dismiss();
            }
        });
        buttonOkForAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(colorDialogFinished != null) {
                    colorDialogFinished.ok(checkBoxCustomColor.isChecked(), seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), true);
                }
                ad.dismiss();
            }
        });
    }

    public void onClickDreamSettings(View v) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
            } catch(ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.screensaver_not_supported), Toast.LENGTH_SHORT);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings_done:
                saveAndFinish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // did I ever told you about the Amazon Echo Show 15, a hazardous waste of technology without proper debugging options?
        if(event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY
                || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            saveAndFinish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public void onClickSelectVideo(View v) {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_video)), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null || data.getData() == null) return;

        StorageControl storage = new StorageControl(this);
        storage.processFile(StorageControl.FILENAME_VIDEO, data);
    }

    private void loadSettings() {
        mViewBackgroundColorPreview.setBackgroundColor( mSharedPref.getInt("color-background", Color.argb(0xff, 0x00, 0x00, 0x00)) );
        mViewClockColorPreview.setBackgroundColor( mSharedPref.getInt("color-clock", Color.argb(0xff, 0xff, 0xff, 0xff)) );
        mCheckBoxLoop.setChecked( mSharedPref.getBoolean("video-loop", true) );
        mCheckBoxStretch.setChecked( mSharedPref.getBoolean("video-stretch", false) );
        mSeekBarVolume.setProgress( mSharedPref.getInt("video-volume", 0) );
        mCheckBoxClock.setChecked( mSharedPref.getBoolean("clock", false) );
        mCheckBoxHrs24.setChecked( mSharedPref.getBoolean("clock-hrs24", true) );
        mCheckBoxDate.setChecked( mSharedPref.getBoolean("date", false) );
        mEditTextDateFormat.setText( mSharedPref.getString("date-format", getDefaultDateFormat(this)) );
        switch(mSharedPref.getInt("clock-position-x", 0)) {
            case 0:
                mRadioButtonLeft.setChecked(true); break;
            case 1:
                mRadioButtonRight.setChecked(true); break;
            case 2:
                mRadioButtonCenterX.setChecked(true); break;
        }
        switch(mSharedPref.getInt("clock-position-y", 0)) {
            case 0:
                mRadioButtonTop.setChecked(true); break;
            case 1:
                mRadioButtonBottom.setChecked(true); break;
            case 2:
                mRadioButtonCenterY.setChecked(true); break;
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor edit = mSharedPref.edit();
        edit.putInt("color-background", ((ColorDrawable)mViewBackgroundColorPreview.getBackground()).getColor());
        edit.putInt("color-clock", ((ColorDrawable)mViewClockColorPreview.getBackground()).getColor());
        edit.putBoolean("video-loop", mCheckBoxLoop.isChecked());
        edit.putBoolean("video-stretch", mCheckBoxStretch.isChecked());
        edit.putInt("video-volume", mSeekBarVolume.getProgress());
        edit.putBoolean("clock", mCheckBoxClock.isChecked());
        edit.putBoolean("clock-hrs24", mCheckBoxHrs24.isChecked());
        edit.putBoolean("date", mCheckBoxDate.isChecked());
        edit.putString("date-format", mEditTextDateFormat.getText().toString());
        edit.putInt("clock-position-x", mRadioButtonLeft.isChecked() ? 0 : mRadioButtonRight.isChecked() ? 1 : 2);
        edit.putInt("clock-position-y", mRadioButtonTop.isChecked() ? 0 : mRadioButtonBottom.isChecked() ? 1 : 2);
        edit.apply();
    }

    private void saveAndFinish() {
        saveSettings();
        setResult(RESULT_OK);
        finish();
    }

    protected void enableDisableAllSettings(boolean state) {
        mViewColorChangerBackground.setEnabled(state);
        mCheckBoxLoop.setEnabled(state);
        mCheckBoxStretch.setEnabled(state);
        mCheckBoxClock.setEnabled(state);
        mCheckBoxHrs24.setEnabled(state);
        mCheckBoxDate.setEnabled(state);
        mEditTextDateFormat.setEnabled(state);
        mViewColorChangerClock.setEnabled(state);
        mRadioButtonTop.setEnabled(state);
        mRadioButtonBottom.setEnabled(state);
        mRadioButtonCenterY.setEnabled(state);
        mRadioButtonLeft.setEnabled(state);
        mRadioButtonRight.setEnabled(state);
        mRadioButtonCenterX.setEnabled(state);
    }

    public static boolean isHighContrastTextEnabled(Context context) {
        if(context != null) {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            Method m = null;
            if(am != null) {
                try {
                    m = am.getClass().getMethod("isHighTextContrastEnabled", null);
                } catch(NoSuchMethodException ignored) { }
            }
            Object result;
            if(m != null) {
                try {
                    result = m.invoke(am, (Object[]) null);
                    if(result instanceof Boolean) {
                        return (Boolean) result;
                    }
                } catch(Exception ignored) { }
            }
        }
        return false;
    }

    static String getDefaultDateFormat(Context c) {
        final SimpleDateFormat sdfSystem = (SimpleDateFormat) DateFormat.getDateFormat(c);
        String strDatePattern = "EE, " + sdfSystem.toLocalizedPattern(); // day of week followed by localized date
        if(!strDatePattern.contains("yyyy")) {
            // devices with API level 17 or below return date format already with yyyy,
            // for all other devices we manually replace yy with yyyy
            strDatePattern = strDatePattern.replace("yy", "yyyy");
        }
        return strDatePattern;
    }

    public void onClickDateFormatHelp(View v) {
        StringBuilder sb = new StringBuilder();
        for(String s : getString(R.string.date_format_placeholders_help).split("\n")) {
            sb.append(s.trim()).append("\n");
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.date_format_placeholders_help_title));
        builder.setMessage(sb.toString());
        builder.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.setNeutralButton(getString(R.string.reset_default),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mEditTextDateFormat.setText(getDefaultDateFormat(getBaseContext()));
                    }
                });
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
        TextView messageView = dialog.findViewById(android.R.id.message);
        messageView.setTypeface(Typeface.MONOSPACE);
    }

    public final static String APPID_FSCLOCK       = "systems.sieber.fsclock";
    public final static String APPID_CUSTOMERDB    = "de.georgsieber.customerdb";
    public final static String APPID_REMOTEPOINTER = "systems.sieber.remotespotlight";
    public final static String APPID_BALLBREAK     = "de.georgsieber.ballbreak";
    public final static String URL_OCO             = "https://github.com/schorschii/oco-server";

    public void onClickGithub(View v) {
        openBrowser(getString(R.string.project_website));
    }
    public void onClickFsClock(View v) {
        openPlayStore(APPID_FSCLOCK);
    }
    public void onClickCustomerDatabaseApp(View v) {
        openPlayStore(APPID_CUSTOMERDB);
    }
    public void onClickRemotePointerApp(View v) {
        openPlayStore(APPID_REMOTEPOINTER);
    }
    public void onClickBallBreakApp(View v) {
        openPlayStore(APPID_BALLBREAK);
    }
    public void onClickOco(View v) {
        openBrowser(URL_OCO);
    }
    void openBrowser(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch(SecurityException ignored) {
            infoDialog(getString(R.string.no_web_browser_found), url);
        } catch(ActivityNotFoundException ignored) {
            infoDialog(getString(R.string.no_web_browser_found), url);
        }
    }
    private void openPlayStore(String appId) {
        try {
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId)));
        } catch(android.content.ActivityNotFoundException anfe) {
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appId)));
        }
    }
    private void infoDialog(String title, String text) {
        final AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        if(title != null) dlg.setTitle(title);
        if(text != null) dlg.setMessage(text);
        dlg.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dlg.setCancelable(true);
        dlg.create().show();
    }

}
