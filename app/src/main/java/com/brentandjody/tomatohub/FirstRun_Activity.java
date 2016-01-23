package com.brentandjody.tomatohub;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brentandjody.tomatohub.routers.RouterType;
import com.brentandjody.tomatohub.routers.connection.TelnetConnection;
import com.brentandjody.tomatohub.routers.connection.TelnetSession;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.viewpagerindicator.CirclePageIndicator;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;


public class FirstRun_Activity extends AppCompatActivity implements PageTurnListener{

    private static final long SEQUENCE_DELAY = 400;
    private static final String TAG = FirstRun_Activity.class.getName();

    public static int SCREEN_WIDTH;
    private Handler mHandler;
    private ViewGroup mContentView;
    private ViewGroup mFrame;
    private View mGetStartedButton;
    private View mDemoModeButton;
    private LinearLayout mProgressPoints;
    private FirstRunPager mPager;
    private CirclePageIndicator mPagerIndicator;
    private FirstRunPagerAdapter mAdapter;
    private String mIpAddress;
    private int mRouterType=-1;
    private String mProtocol;
    private String mPassword;
    private String mUsername;
    private boolean autoStart = false;
    private long mNextPostTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        SCREEN_WIDTH=size.x;
        mHandler=new Handler();
        mNextPostTime = System.currentTimeMillis();
        new DiscoverRouter().execute();

        setContentView(R.layout.activity_first_run_);
        mContentView = (ViewGroup)findViewById(R.id.background);
        mFrame = (ViewGroup)findViewById(R.id.frame);
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.firstrun_bypass))) {
            autoStart=true;
            ProgressBar progressBar = new ProgressBar(this);
            mFrame.addView(progressBar, new FrameLayout.LayoutParams(64, 64, Gravity.CENTER));
        } else {
            mPager = new FirstRunPager(this);
            mPagerIndicator = new CirclePageIndicator(this);
            LinearLayout pagerContainer = (LinearLayout) findViewById(R.id.pager_container);
            pagerContainer.addView(mPager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            pagerContainer.addView(mPagerIndicator, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40, 1));

            mGetStartedButton = findViewById(R.id.btnGetStarted);
            mDemoModeButton = findViewById(R.id.btnDemoMode);

            mGetStartedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupPreferences();
                }
            });
            mDemoModeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);
                    prefs.edit().putBoolean(getString(R.string.pref_key_allow_changes), true);
                    prefs.edit().putString(getString(R.string.pref_key_router_type), Integer.toString(RouterType.FAKE)).commit();
                    finish();
                }
            });

            mAdapter = new FirstRunPagerAdapter();
            mPager.setAdapter(mAdapter);

            mPagerIndicator.setViewPager(mPager);

            mContentView.findViewById(R.id.buttons).setVisibility(View.VISIBLE);
            mAdapter.delayedReveal(mGetStartedButton, 5000);
            mAdapter.delayedReveal(mDemoModeButton, 5500);
            mAdapter.delayedReveal(mPagerIndicator, 3200);
        }
    }



    public void onPageTurning(int position, int offset) {
        mAdapter.moveIcons(position, offset);
    }

    public class FirstRunPager extends ViewPager {

        private PageTurnListener mListener;

        public FirstRunPager(Context context) {
            super(context);
            mListener = (FirstRun_Activity) context;
        }

        public FirstRunPager(Context context, AttributeSet attrs) {
            super(context, attrs);
            mListener = (FirstRun_Activity) context;
        }

        @Override
        protected void onPageScrolled(int position, float offset, int offsetPixels) {
            mListener.onPageTurning(position, offsetPixels);
            super.onPageScrolled(position, offset, offsetPixels);
        }
    }

    private void setupPreferences() {
        mContentView.findViewById(R.id.pager_container).setVisibility(View.GONE);
        mContentView.findViewById(R.id.buttons).setVisibility(View.GONE);
        mContentView.findViewById(R.id.frame).setVisibility(View.VISIBLE);
        mFrame.removeAllViews();
        mProgressPoints = new LinearLayout(this);
        mProgressPoints.setOrientation(LinearLayout.VERTICAL);
        mFrame.addView(mProgressPoints);
        if (mIpAddress ==null || mIpAddress.equals("0.0.0.0")) {
            addButton(getString(R.string.connect_to_wifi), getString(R.string.tap_to_continue));
        } else {
            progressPoint(getString(R.string.router_found), mIpAddress);
            if (mProtocol==null) {
                addButton(getString(R.string.enable_ssh), getString(R.string.tap_to_continue));
            } else {
                progressPoint(getString(R.string.console_found), mProtocol);
                final EditText tvPassword = new EditText(this);
                tvPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                tvPassword.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                tvPassword.setTextSize(24);
                tvPassword.setPadding(50, 20, 50, 20);
                AlertDialog passwordDialog = new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.enter_your)+mProtocol+getString(R.string.password))
                        .setView(tvPassword)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPassword = tvPassword.getText().toString();
                                progressPoint(getString(R.string.password_set), "************");
                                mUsername=null;
                                new AttemptConnection().execute("root", "admin", "Admin");
                            }
                        }).create();
                addToDisplaySequence(passwordDialog);
            }
        }
    }

    private void connectionFailed() {
        if (mUsername==null) {
            Log.d(TAG, getString(R.string.user_not_identified));
            final EditText tvUsername = new EditText(FirstRun_Activity.this);
            tvUsername.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
            tvUsername.setTextSize(24);
            tvUsername.setPadding(50, 20, 50, 20);
            AlertDialog usernameDialog = new AlertDialog.Builder(FirstRun_Activity.this)
                    .setTitle(getString(R.string.enter_your) + mProtocol + " " + getString(R.string.username))
                    .setView(tvUsername)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mUsername = tvUsername.getText().toString();
                            progressPoint(getString(R.string.username_set), mUsername);
                            savePreferences();
                            new AttemptConnection().execute(mUsername);
                        }
                    }).create();
            addToDisplaySequence(usernameDialog);
        } else {
            new AlertDialog.Builder(FirstRun_Activity.this)
                    .setTitle(R.string.unable_to_connect)
                    .setMessage(R.string.redirect_to_settings)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FirstRun_Activity.this.startActivity(new Intent(FirstRun_Activity.this, SettingsActivity.class));
                            finish();
                        }
                    })
                    .show();
        }
    }

    private void connectionSucceeded() {
        progressPoint(getString(R.string.user_identified), mUsername);
        Log.d(TAG, "User Identified as: " + mUsername);
        savePreferences();
        finish();
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.sharedPreferences_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.pref_key_ip_address), mIpAddress);
        editor.putString(getString(R.string.pref_key_port), mProtocol.equals("ssh") ? "22" : "23");
        editor.putString(getString(R.string.pref_key_protocol), mProtocol);
        editor.putString(getString(R.string.pref_key_username), mUsername);
        editor.putString(getString(R.string.pref_key_password), mPassword);
        editor.putBoolean(getString(R.string.pref_key_allow_changes), false);
        if (mRouterType>=0)
            editor.putString(getString(R.string.pref_key_router_type), Integer.toString(mRouterType));
        editor.commit();
        progressPoint("Settings updated", "");
    }

    private boolean attemptLogin(String user, String pass) {
        boolean result = false;
        if (mProtocol.equals("ssh")) {
            try {
                JSch ssh = new JSch();
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                Session session = ssh.getSession(user, mIpAddress, 22);
                session.setConfig(config);
                session.setPassword(pass);
                session.setServerAliveInterval(3000);
                session.connect(5000);
                result=true;
                if (mRouterType==-1) {
                    Channel channel = session.openChannel("exec");
                    ((ChannelExec) channel).setCommand("cat /etc/motd");
                    ByteArrayOutputStream sb = new ByteArrayOutputStream();
                    channel.setOutputStream(sb);
                    channel.connect();
                    sb.flush();
                    while (!channel.isClosed()) {
                        Thread.sleep(10);
                    }
                    channel.disconnect();
                    if (sb.toString().contains("Tomato")) mRouterType = RouterType.TOMATO;
                    else if (sb.toString().contains("DD-WRT")) mRouterType = RouterType.DDWRT;
                }
            } catch (Exception ex) {
                Log.d(TAG, ""+ex.getMessage());
            }
        }
        if (mProtocol.equals("telnet")) {
            try {
                TelnetSession session = new TelnetSession(mIpAddress, user, pass);
                result = true;
                String output = Arrays.toString(session.sendCommand("cat /etc/motd"));
                if (output.contains("Tomato")) mRouterType = RouterType.TOMATO;
                else if (output.contains("DD-WRT")) mRouterType = RouterType.DDWRT;
            } catch (Exception ex) {
                Log.d(TAG, ""+ex.getMessage());
            }
        }
        return result;
    }

    private void reDiscover() {
        Intent intent = getIntent();
        intent.putExtra(getString(R.string.firstrun_bypass), "");
        recreate();
    }

    private void addButton(String text, String summary) {
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.item_first_run, mFrame);
        ((TextView) view.findViewById(R.id.text)).setText(text);
        ((TextView) view.findViewById(R.id.summary)).setText(summary);
        view.findViewById(R.id.imgGreen).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.button).setTranslationY(300);
        view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.imgGreen).setVisibility(View.VISIBLE);
                Animation animation = AnimationUtils.loadAnimation(FirstRun_Activity.this, R.anim.slide_up_and_out);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        reDiscover();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                view.findViewById(R.id.button).startAnimation(animation);
            }
        });
        addToDisplaySequence(view.findViewById(R.id.button));
    }

    private void progressPoint(String text, String details) {
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup view = (ViewGroup)inflater.inflate(R.layout.item_progress_point, mProgressPoints);
        View point = view.findViewById(R.id.progress_point);
        ((TextView) view.findViewById(R.id.progress_text)).setText(text);
        ((TextView) view.findViewById(R.id.progress_details)).setText(details);
        //set the IDs to 0, so the next progressPoint doesn't get confused with this one.
        point.setId(0);
        (view.findViewById(R.id.progress_text)).setId(0);
        (view.findViewById(R.id.progress_details)).setId(0);
        addToDisplaySequence(point);
        Log.d(TAG, "Progress: "+text + " : "+details);
    }

    private void addToDisplaySequence(final View view) {
        long now = System.currentTimeMillis();
        long delay = mNextPostTime -now;
        if (delay<0) delay=0;
        view.setVisibility(View.INVISIBLE);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(View.VISIBLE);
                view.startAnimation(AnimationUtils.loadAnimation(FirstRun_Activity.this, R.anim.slide_in_from_bottom));
            }
        }, delay);
        mNextPostTime = now+delay+ SEQUENCE_DELAY;
    }

    private void addToDisplaySequence(final AlertDialog dialog) {
        long now = System.currentTimeMillis();
        long delay = mNextPostTime -now;
        if (delay<0) delay=0;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.getWindow().getAttributes().windowAnimations = R.style.DialogSlideIn;
                dialog.show();
            }
        },delay);
        mNextPostTime = now+delay+ SEQUENCE_DELAY;
    }

    private class DiscoverRouter extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiMgr.getDhcpInfo();
            mIpAddress = intToIp(dhcpInfo.gateway);
            mProtocol=null;
            try {
                new Socket(mIpAddress, 22);
                mProtocol = "ssh";
            } catch (UnknownHostException exception) {
                mIpAddress =null;
            } catch (IOException exception) {
                try {
                    new Socket(mIpAddress, 23);
                    mProtocol = "telnet";
                } catch (IOException ex) {
                    mProtocol = null;
                }
            }
            if (mIpAddress !=null) {
                try {
                    URL url = new URL("http://"+ mIpAddress);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String input;
                        while ((input = in.readLine()) != null)
                            sb.append(input);
                        in.close();
                        String x = sb.toString();
                        if (sb.toString().contains("DD-WRT")) mRouterType=RouterType.DDWRT;
                    }
                } catch (Exception ex) {
                    Log.d(TAG, ""+ex.getMessage());
                }


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (autoStart) setupPreferences();
        }
    }

    private class AttemptConnection extends AsyncTask<String, Void, Void> {
        boolean mSuccess;
        @Override
        protected Void doInBackground(String... params) {
            mSuccess=false;
            for (String user : params) {
                if (attemptLogin(user, mPassword)) {
                    mUsername=user;
                    mSuccess=true;
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mSuccess) connectionSucceeded();
            else connectionFailed();
        }
    }

    public static String intToIp(int addr) {
        return  ((addr & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF));
    }

}

interface PageTurnListener {
    void onPageTurning(int position, int offset);
}