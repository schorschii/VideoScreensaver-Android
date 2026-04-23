package systems.sieber.vscreensaver;

import static systems.sieber.vscreensaver.BaseSettingsActivity.SHARED_PREF_DOMAIN;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class VideoScreensaverView extends RelativeLayout {

    VideoView mVideoView;
    TextView mTextViewClock;
    TextView mTextViewDate;
    LinearLayout mLinearLayoutClockDate;
    View mBatteryView;
    TextView mBatteryText;
    ImageView mBatteryImage;
    View mAlarmView;
    TextView mAlarmText;
    ImageView mAlarmImage;

    boolean mHrs24;
    String mDateFormat;

    Timer mTimerClock;

    SharedPreferences mSharedPref;

    private ErrorListener mErrorListener = null;
    public interface ErrorListener {
        void error();
    }
    void setErrorListener(ErrorListener listener) {
        mErrorListener = listener;
    }

    public VideoScreensaverView(Context c, AttributeSet attrs) {
        super(c, attrs);
        inflate(getContext(), R.layout.view_screensaver, this);
        mSharedPref = getContext().getSharedPreferences(SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);

        // find views
        mVideoView = findViewById(R.id.videoViewMain);
        mTextViewClock = findViewById(R.id.textViewClock);
        mTextViewDate = findViewById(R.id.textViewDate);
        mLinearLayoutClockDate = findViewById(R.id.linearLayoutClockDate);
        mBatteryView = findViewById(R.id.linearLayoutBattery);
        mBatteryText = findViewById(R.id.textViewBattery);
        mBatteryImage = findViewById(R.id.imageViewBattery);
        mBatteryImage.setImageResource(R.drawable.ic_battery_full_white_24dp);
        mAlarmView = findViewById(R.id.linearLayoutAlarm);
        mAlarmText = findViewById(R.id.textViewAlarm);
        mAlarmImage = findViewById(R.id.imageViewAlarm);
        mAlarmImage.setImageResource(R.drawable.ic_alarm_white_24dp);
        loadSettings();

        // apply the insets as a margin to the view, so that elements at the bottom
        // of the ScrollView do not get hidden behind the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(mLinearLayoutClockDate, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.rightMargin = insets.right;
            mlp.topMargin = insets.top;
            mlp.bottomMargin = insets.bottom;
            v.setLayoutParams(mlp);
            // Return CONSUMED if you don't want the window insets to keep passing down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });

        // start clock timer
        updateClock();
        TimerTask taskClockDate = new TimerTask() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateClock();
                        updateAlarm();
                    }
                });
            }
        };
        mTimerClock = new Timer(false);
        mTimerClock.schedule(taskClockDate, 0, 1000);
    }

    private void updateClock() {
        final Calendar cal = Calendar.getInstance();

        final SimpleDateFormat sdfTime = new SimpleDateFormat(mHrs24 ? "HH:mm" : "h:mm");
        mTextViewClock.setText(sdfTime.format(cal.getTime()));

        try {
            mTextViewDate.setText(
                    new SimpleDateFormat(mDateFormat, Locale.getDefault())
                            .format(cal.getTime())
            );
        } catch(IllegalArgumentException ignored) {
            mTextViewDate.setText("---");
        }
    }

    public void loadSettings() {
        mHrs24 = mSharedPref.getBoolean("clock-hrs24", true);
        mDateFormat = mSharedPref.getString("date-format", BaseSettingsActivity.getDefaultDateFormat(getContext()));

        final int colorBackground = mSharedPref.getInt("color-background", Color.argb(0xff, 0x00, 0x00, 0x00));
        this.setBackgroundColor(colorBackground);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(mSharedPref.getBoolean("video-loop", true));
                float vol = (float) mSharedPref.getInt("video-volume", 0) / 100;
                mp.setVolume(vol, vol);
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                mVideoView.setBackgroundColor(colorBackground);
                findViewById(R.id.linearLayoutNoVideo).setVisibility(VISIBLE);
                if(mErrorListener != null) mErrorListener.error();
                return true;
            }
        });
        if(mSharedPref.getBoolean("video-stretch", false)) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoView.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.removeRule(RelativeLayout.CENTER_IN_PARENT);
            mVideoView.setLayoutParams(params);
        } else {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoView.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            mVideoView.setLayoutParams(params);
        }

        if(mSharedPref.getBoolean("clock", false)) {
            mTextViewClock.setVisibility(VISIBLE);
        } else {
            mTextViewClock.setVisibility(GONE);
        }
        if(mSharedPref.getBoolean("date", false)) {
            mTextViewDate.setVisibility(VISIBLE);
        } else {
            mTextViewDate.setVisibility(GONE);
        }

        int colorClock = mSharedPref.getInt("color-clock", Color.argb(0xff, 0xff, 0xff, 0xff));
        mTextViewClock.setTextColor(colorClock);
        mTextViewDate.setTextColor(colorClock);
        mBatteryText.setTextColor(colorClock);
        mBatteryImage.setColorFilter(colorClock, PorterDuff.Mode.SRC_ATOP);
        mAlarmText.setTextColor(colorClock);
        mAlarmImage.setColorFilter(colorClock, PorterDuff.Mode.SRC_ATOP);
        mTextViewClock.setTypeface( ResourcesCompat.getFont(getContext(), R.font.dseg7classic_regular) );
        mTextViewDate.setTypeface( ResourcesCompat.getFont(getContext(), R.font.cairo_regular) );

        LinearLayout.LayoutParams paramsc = (LinearLayout.LayoutParams) mTextViewClock.getLayoutParams();
        paramsc.gravity = Gravity.NO_GRAVITY;
        LinearLayout.LayoutParams paramsd = (LinearLayout.LayoutParams) mTextViewDate.getLayoutParams();
        paramsd.gravity = Gravity.NO_GRAVITY;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mLinearLayoutClockDate.getLayoutParams();
        params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.removeRule(RelativeLayout.CENTER_HORIZONTAL);
        params.removeRule(RelativeLayout.CENTER_VERTICAL);
        switch(mSharedPref.getInt("clock-position-x", 0)) {
            case 0:
                paramsc.gravity = Gravity.LEFT;
                paramsd.gravity = Gravity.LEFT;
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT); break;
            case 1:
                paramsc.gravity = Gravity.RIGHT;
                paramsd.gravity = Gravity.RIGHT;
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT); break;
            case 2:
                paramsc.gravity = Gravity.CENTER_HORIZONTAL;
                paramsd.gravity = Gravity.CENTER_HORIZONTAL;
                params.addRule(RelativeLayout.CENTER_HORIZONTAL); break;
        }
        switch(mSharedPref.getInt("clock-position-y", 0)) {
            case 0:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP); break;
            case 1:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM); break;
            case 2:
                params.addRule(RelativeLayout.CENTER_VERTICAL); break;
        }
        mLinearLayoutClockDate.setLayoutParams(params);
        mTextViewClock.setLayoutParams(paramsc);
    }

    public void start() {
        switch(mSharedPref.getInt("predefined-video", -1)) {
            case 0:
                mVideoView.setVideoURI( Uri.parse("android.resource://systems.sieber.vscreensaver/" + R.raw.fire_landscape) );
                break;
            case 1:
                mVideoView.setVideoURI( Uri.parse("android.resource://systems.sieber.vscreensaver/" + R.raw.fire_portrait) );
                break;
            case 2:
                mVideoView.setVideoURI( Uri.parse("android.resource://systems.sieber.vscreensaver/" + R.raw.aquarium_landscape) );
                break;
            case 3:
                mVideoView.setVideoURI( Uri.parse("android.resource://systems.sieber.vscreensaver/" + R.raw.aquarium_portrait) );
                break;
            default:
                StorageControl storage = new StorageControl(getContext());
                mVideoView.setVideoPath( "file://" + storage.getStorage(StorageControl.FILENAME_VIDEO).getAbsolutePath() );
        }
        mVideoView.start();
        //mVideoView.setZOrderOnTop(true);
        mVideoView.setBackgroundColor(0);
        findViewById(R.id.linearLayoutNoVideo).setVisibility(GONE);
    }

    void updateAlarm() {
        mAlarmView.setVisibility(View.GONE);
        if(mSharedPref.getBoolean("show-alarms", false)
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final AlarmManager m = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmInfo = m.getNextAlarmClock();
            if(alarmInfo != null) {
                Long systemAlarmTime = alarmInfo.getTriggerTime();
                SimpleDateFormat sdfDisplay = new SimpleDateFormat(mHrs24 ? "HH:mm" : "h:mm", Locale.getDefault());
                mAlarmText.setText(sdfDisplay.format(systemAlarmTime));
                mAlarmView.setVisibility(View.VISIBLE);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    void updateBattery(int plugged, int level) {
        if((plugged == 0 && mSharedPref.getBoolean("show-battery-info", false))
                || (plugged != 0 && mSharedPref.getBoolean("show-battery-info-when-charging", false))) {
            mBatteryText.setText(level + "%");
            mBatteryView.setVisibility(View.VISIBLE);
            if(plugged == 0) {
                if(level < 5) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_0_bar_white_24dp);
                } else if(level < 10) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_1_bar_white_24dp);
                } else if(level < 25) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_2_bar_white_24dp);
                } else if(level < 40) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_3_bar_white_24dp);
                } else if(level < 55) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_4_bar_white_24dp);
                } else if(level < 70) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_5_bar_white_24dp);
                } else if(level < 85) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_6_bar_white_24dp);
                } else {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_full_white_24dp);
                }
            } else {
                mBatteryImage.setImageResource(R.drawable.ic_battery_charging_white_24dp);
            }
        } else {
            mBatteryView.setVisibility(View.GONE);
        }
    }

}
