package com.activelook.messages;

import static java.lang.Integer.max;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Html;
import android.text.TextPaint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.activelook.activelooksdk.Glasses;
import com.activelook.activelooksdk.types.ImgStreamFormat;
import com.activelook.activelooksdk.types.Rotation;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Glasses connectedGlasses;
    private Intent serviceIntent;
    public String langCode= Locale.getDefault().getLanguage(),
    prev_packageName="", prev_titleData="", prev_textData="";
    private TextView largeText, NotificationView, GlassesBattery, fontSizeTextView;
    boolean firstMessage = true;
    Handler clockHandler = new Handler();
    Runnable clockRunnable;
    TableLayout tab;


    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch sensorSwitch;
    private SeekBar luminanceSeekBar, fontSizeSeekBar;
    int line=0,  lineHeight = 22, maxHeight = 206, nbrLin = 7, gbattery=0;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @SuppressLint({"BatteryLife", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);
        tab = findViewById(R.id.tab);
        NotificationView = findViewById(R.id.NotifView);

        /* Check location permission (needed for BLE scan)
         */
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},0);

        if (savedInstanceState != null && ((DemoApp) this.getApplication()).isConnected()) {
            this.connectedGlasses = savedInstanceState.getParcelable("connectedGlasses");
            this.connectedGlasses.setOnDisconnected(glasses -> {glasses.disconnect();
                MainActivity.this.disconnect();     });
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        largeText = findViewById(R.id.largeText);
        GlassesBattery = this.findViewById(R.id.GlassesBattery);
        luminanceSeekBar = this.findViewById(R.id.luminanceSeekBar);
        fontSizeSeekBar = this.findViewById(R.id.fontSizeSeekBar);
        fontSizeTextView = this.findViewById(R.id.TextSizeView);
        sensorSwitch = this.findViewById(R.id.sensorSwitch);
        langCode=Locale.getDefault().getLanguage();

        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());
        this.updateVisibility();
        this.bindActions();

        NotificationManager notif = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notif.isNotificationPolicyAccessGranted()) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
    }

//---------------------------------------------------------------------------------

    public final BroadcastReceiver onNotice = new BroadcastReceiver() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            final Glasses g = connectedGlasses;
            tab = findViewById(R.id.tab);
            NotificationView = findViewById(R.id.NotifView);
            String packageName = intent.getStringExtra("package");
            String titleData = intent.getStringExtra("title");
            String textData = intent.getStringExtra("text");
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
            String time = sdf2.format(new Date());
            Log.d("MainActivity", "receivedNotif : " + time + " : " + packageName
                    + " : " + titleData + " : " + textData);
            boolean displayData=true;
            Bitmap logo_bw;
            try {logo_bw = BitmapFactory.decodeStream(getAssets().open("android_bw.png"));}
            catch (IOException e) {throw new RuntimeException(e);}

            if(packageName.equals("com.android.systemui")) {displayData=false;}
            if(packageName.equals("com.android.vending")) {displayData=false;}
            if(packageName.equals(prev_packageName) && titleData.equals(prev_titleData)
                    && textData.equals(prev_textData)) {displayData=false;}

            // ----------- for test purpose only :
//            if(packageName.equals("com.yahoo.mobile.client.android.mail") && textData.equals("essai 1"))
//            {Log.d("MainActivity", "receivedNotif : changed to com.tencent.mm");
//                packageName = "com.tencent.mm" ;titleData = "Parents"; textData = "Try that message";}
//            if(packageName.equals("com.yahoo.mobile.client.android.mail") && textData.equals("essai 2"))
//            {Log.d("MainActivity", "receivedNotif : changed to com.microsoft.teams");
//                packageName = "com.microsoft.teams" ;titleData = "Your friend"; textData = "How are you today?";}
//            if(packageName.equals("com.yahoo.mobile.client.android.mail") && textData.equals("essai 3"))
//            {Log.d("MainActivity", "receivedNotif : changed to us.zoom.videomeetings");
//                packageName = "us.zoom.videomeetings" ;titleData = "colleague Maria"; textData = "Your next meeting is postponed";}
//            if(packageName.equals("com.yahoo.mobile.client.android.mail") && titleData.equals("Synchronisation de la messagerie…"))
//            {Log.d("MainActivity", "receivedNotif : Synchronisation de la messagerie… :");
//                displayData=false;}

            if(displayData) {
                NotificationView.setText("from : " + packageName);
                prev_packageName = packageName; prev_titleData = titleData; prev_textData = textData;

                //================= PREPARE TO WRITE IN THE PHONE
                TableRow tr = new TableRow(getApplicationContext());
                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT));
                LinearLayout linlay = new LinearLayout(getApplicationContext());
                linlay.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                linlay.setOrientation(LinearLayout.HORIZONTAL);
                ImageView logo = new ImageView(getApplicationContext());
                // reduce the logo to 128x128 px
                LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(128, 128);
                logo.setLayoutParams(parms);

                Drawable drawable_pack = null;
                try {drawable_pack = getPackageManager().getApplicationIcon(packageName);
                logo.setImageDrawable(drawable_pack);}
                catch (PackageManager.NameNotFoundException e) {e.printStackTrace();}

                TextView textview = new TextView(getApplicationContext());
                textview.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 1.0f));
                textview.setTextSize((int) (lineHeight*.825));
//                textview.setTextColor(Color.parseColor("#000000"));
                textview.setText(Html.fromHtml(time + " <br><b>" + titleData + " : </b>" + textData));
                tr.addView(textview);
                linlay.addView(logo);
                linlay.addView(tr);
                tab.addView(linlay);

            //================= PREPARE TO WRITE IN THE GLASSES
            // color to B&W
                Bitmap mylogo = getDefaultBitmap(drawable_pack);
                if (drawable_pack != null) {
                int imgXmin = (int) (drawable_pack.getIntrinsicWidth() * 0.1);
                int imgXmax = (int) (drawable_pack.getIntrinsicWidth() * 0.9);
                int imgYmin = (int) (drawable_pack.getIntrinsicHeight() * 0.1);
                int imgYmax = (int) (drawable_pack.getIntrinsicHeight() * 0.9);
                Bitmap logo_bw_full = Bitmap.createBitmap((imgXmax - imgXmin), (imgYmax - imgYmin),
                        Bitmap.Config.ARGB_8888);
                for (int x = imgXmin; x < imgXmax; ++x) {
                    for (int y = imgXmin; y < imgXmax; ++y) {
                        // get one pixel color
                        int pixel = mylogo.getPixel(x, y);
                        // retrieve color of all channels
                        int A = Color.alpha(pixel), R = Color.red(pixel), G = Color.green(pixel), B = Color.blue(pixel);
                        // take conversion up to one single value
                        R = G = B = (int) (255 - (0.299 * R + 0.587 * G + 0.114 * B));
                        // set new pixel color to output bitmap
                        logo_bw_full.setPixel(x-imgXmin, y-imgYmin, Color.argb(A, R, G, B));
                    }
                }
                logo_bw = getResizedBitmap(logo_bw_full, 32, 32);}
                else { packageName="android"; }

            if (g != null) {
                if(firstMessage) {g.clear(); firstMessage = false;}
                displayClock();
                if (gbattery!=0) {GlassesBattery.setText("Glasses battery : "+String.format("%d",gbattery)+"%");}
                String textfr;
                textfr = titleData + " : " + textData + ' ';
                textfr = textfr.replaceAll("\n\r"," ");
                textfr = textfr.replaceAll("\r\n"," ");
                textfr = textfr.replaceAll("\n"," ");
                textfr = textfr.replaceAll("\t"," ");
                textfr = textfr.replaceAll("\r"," ");
                for (int j = 0; j < 32; j++) {
                    textfr = textfr.replaceAll(String.valueOf((char) j),""); }

                // split textfr into several lines
                int linWidth;
                Bitmap txtimg;
                String image = url2image(packageName) + "_bw.png";
                boolean logo_off = true, secondLin = false;
                String txtlin = "", txtlinTry = "";

                for (int j = 0; j < textfr.length(); j++) {
                    if (textfr.charAt(j) != ' ' && textfr.charAt(j) !=((char) 0xA0) && textfr.charAt(j) !=((char) 0x3000)
                            && textfr.charAt(j) !=((char) 0x2000) && textfr.charAt(j) !=((char) 0x2001)
                            && textfr.charAt(j) !=((char) 0x2002) && textfr.charAt(j) !=((char) 0x2003)
                            && textfr.charAt(j) !=((char) 0x2004) && textfr.charAt(j) !=((char) 0x2005)
                            && textfr.charAt(j) !=((char) 0x2008) && textfr.charAt(j) !=((char) 0x2007)
                    ) // all kinds of space, non-breaking, asiatic, ... between words
                        {txtlin = txtlin + textfr.charAt(j);}
                    else { txtimg = textAsBitmap(txtlin,lineHeight-2);
                        linWidth = txtimg.getWidth();
                        Log.d("MainActivity", "receivedNotif : linWidth of "
                                + txtlinTry +" = " + String.format("%d", linWidth) + "\n");
                        if (linWidth < 268 || txtlinTry.length() == 0) {
                            txtlinTry = txtlin; txtlin = txtlin + ' ';}
                        else {
                            // WE CAN WRITE txtlinTry in the glasses and the logo only with the first line
                            // delete next line
                            g.color((byte) 0);
                            if (line < nbrLin & !secondLin) {g.rectf((short) 0, (short) (maxHeight - (line+1) * lineHeight),
                                    (short) 304, (short) (maxHeight - (line - 1) * lineHeight)); }
                            if (line < nbrLin & secondLin) {g.rectf((short) 0, (short) (maxHeight - (line+1) * lineHeight),
                                    (short) 271, (short) (maxHeight - (line - 1) * lineHeight)); secondLin=false;}
                            if (line == nbrLin & !secondLin) {g.rectf((short) 0, (short) 0, (short) 304, (short) lineHeight);}
                            if (line == nbrLin & secondLin) {g.rectf((short) 0, (short) 0, (short) 271, (short) lineHeight);}
                            // display next line to glasses
                            g.color((byte) 15);
                            txtimg = textAsBitmap(txtlinTry,lineHeight-1);
                            linWidth = txtimg.getWidth();
                            g.imgStream(txtimg, ImgStreamFormat.MONO_4BPP_HEATSHRINK,
                                    (short) (264-linWidth), (short) (maxHeight - line * lineHeight));
                            if (logo_off) {
                                if (!image.equals("other_bw.png")) { // pick the app logo if not in the bottom list
                                    try {logo_bw = BitmapFactory.decodeStream(getAssets().open(image));}
                                    catch (IOException e) {e.printStackTrace();}
                                }
                                g.imgStream(logo_bw, ImgStreamFormat.MONO_4BPP_HEATSHRINK, (short) 272,
                                        (short) (maxHeight - (line - 1) * lineHeight - 32));
                                logo_off = false; // the logo is drawn and should be drawn again
                                secondLin = true;
                            }
                            line++; if (line == nbrLin) {line = 0;}
                            txtlin = txtlin.substring(txtlinTry.length() + 1) + ' ';
                            txtlinTry = "";
                        }
                    }
                }
                // delete next line
                g.color((byte) 0);
                if (line < nbrLin & !secondLin) {g.rectf((short) 0, (short) (maxHeight - (line+1) * lineHeight),
                        (short) 304, (short) (maxHeight - (line - 1) * lineHeight)); }
                if (line < nbrLin & secondLin) {g.rectf((short) 0, (short) (maxHeight - (line+1) * lineHeight),
                        (short) 271, (short) (maxHeight - (line - 1) * lineHeight));}
                if (line == nbrLin & !secondLin) {g.rectf((short) 0, (short) 0,
                        (short) 304, (short) (maxHeight - (line - 1) * lineHeight));}
                if (line == nbrLin & secondLin) {g.rectf((short) 0, (short) 0,
                        (short) 271, (short) (maxHeight - (line - 1) * lineHeight));}
                //
                g.color((byte) 15);
                txtimg = textAsBitmap(txtlin,lineHeight-2);
                linWidth=txtimg.getWidth();
                g.imgStream(txtimg, ImgStreamFormat.MONO_4BPP_HEATSHRINK,
                        (short) (268-linWidth), (short) (maxHeight - line * lineHeight));
                if (logo_off) {
                    if (!image.equals("android_bw.png")) {
                        try { logo_bw = BitmapFactory.decodeStream(getAssets().open(image));}
                        catch (IOException e) {e.printStackTrace();}
                    }
                    g.imgStream(logo_bw, ImgStreamFormat.MONO_4BPP_HEATSHRINK, (short) 272,
                            (short) (maxHeight - (line - 1) * lineHeight - 32));
                }
                line++; if (line == nbrLin+1) {line = 0;}
            }
        }
        }
    };

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth(), height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width, scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public Bitmap getDefaultBitmap(Drawable d) {
        if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable) d).getBitmap();
        } else if ((Build.VERSION.SDK_INT >= 26)
                && (d instanceof AdaptiveIconDrawable)) {
            AdaptiveIconDrawable icon = ((AdaptiveIconDrawable)d);
            int w = icon.getIntrinsicWidth();
            int h = icon.getIntrinsicHeight();
            Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            canvas.drawColor(0);
            canvas.drawBitmap(result, 0F, 0F, null);
            icon.setBounds(0, 0, w, h);
            icon.draw(canvas);
            return result;
        }
        float density = this.getResources().getDisplayMetrics().density;
        int defaultWidth = (int)(48* density);
        int defaultHeight = (int)(48* density);
        return Bitmap.createBitmap(defaultWidth, defaultHeight, Bitmap.Config.ARGB_8888);
    }

//---------------------------------------------------------------------------------

    @SuppressLint("DefaultLocale")
    private void updateVisibility() {
        final Glasses g = this.connectedGlasses;
        if (g == null) {
            this.findViewById(R.id.connected_content).setVisibility(View.GONE);
            this.findViewById(R.id.disconnected_content).setVisibility(View.VISIBLE);
        } else {
            this.findViewById(R.id.connected_content).setVisibility(View.VISIBLE);
            this.findViewById(R.id.disconnected_content).setVisibility(View.GONE);
            g.clear();
            g.txt(new Point(250, 204), Rotation.TOP_LR, (byte) 2, (byte) 0x0F, "ActiveLook");
            g.txt(new Point(232, 144), Rotation.TOP_LR, (byte) 2, (byte) 0x0F, "Messages");

            displayClock();
            if (gbattery!=0) {GlassesBattery.setText("Glasses battery : "+String.format("%d",gbattery)+"%");}
            clockRunnable = new Runnable() {
                @Override
                public void run() { displayClock();
                    if (gbattery!=0) {GlassesBattery.setText("Glasses battery : "+String.format("%d",gbattery)+"%");}
                    clockHandler.postDelayed(this,60000);
                }
            };
            clockHandler.removeCallbacks(clockRunnable);
            clockHandler.postDelayed(clockRunnable,60000); // on redemande toutes les minutes
            if (gbattery!=0) {GlassesBattery.setText("Glasses battery : "+String.format("%d",gbattery)+"%");}
        }
    }

    @SuppressLint("DefaultLocale")
    private void displayClock(){
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String clock = sdf.format(new Date());
        final Glasses g = connectedGlasses;
        if (g != null) {
            g.battery(r1 -> { gbattery=r1;
                connectedGlasses.cfgSet("ALooK");
                if (r1 < 25) {connectedGlasses.imgDisplay((byte) 1, (short) 270, (short) 226);}
                else {connectedGlasses.imgDisplay((byte) 0, (short) 270, (short) 226);}
                connectedGlasses.txt(new Point(263, 254), Rotation.TOP_LR, (byte) 1, (byte) 0x0F,
                        String.format("%d", r1) + "% / " + String.format("%d", batLevel) + "%  ");
                connectedGlasses.txt(new Point(100, 254), Rotation.TOP_LR, (byte) 1, (byte) 0x0F, clock);
            });//Glasses Battery
        }
    }

    @SuppressLint("SetTextI18n")
    private void bindActions() {
        // If BT is not on, request that it be enabled.
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(),
                    "Your BLUETOOTH is not open !!!/n>>>relaunch the application", Toast.LENGTH_LONG).show();
            largeText.setText("Your BlueTooth is not open !!\n\n" +
                    "Please open BlueTooth and\n\n relaunch the application.");
            largeText.setTextColor(Color.parseColor("#FF0000"));
            largeText.setTypeface(largeText.getTypeface(), Typeface.BOLD);
        }
        this.findViewById(R.id.scan).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ScanningActivity.class);
            MainActivity.this.startActivityForResult(intent, Activity.RESULT_FIRST_USER);
        });

        this.findViewById(R.id.button_disconnect).setOnClickListener(view -> {
            MainActivity.this.sensorSwitch(true);
            connectedGlasses.sensor(true);
            MainActivity.this.disconnect();
        });


        sensorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> MainActivity.this.sensorSwitch(isChecked));

        luminanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                MainActivity.this.lumaButton(progressChangedValue);}
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("DefaultLocale")
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            { lineHeight = progress+17; nbrLin = (int) (231/lineHeight - 1);
                fontSizeTextView.setText("Text size ("+String.format("%d",(lineHeight-1))+"px) : ");
               }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    public Bitmap textAsBitmap(String text, int textSize) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextSize(textSize);
        tp.setColor(Color.WHITE); // white for text
        tp.setTextAlign(Paint.Align.LEFT);
        float baseline = -tp.ascent(); // ascent() is negative
        int width = max((int) (tp.measureText(text) + 0.5f),1); // round with 1 as min
        int height = max((int) (baseline + tp.descent() + 0.5f),1);
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setStyle(Paint.Style.FILL);
        bp.setColor(Color.BLACK); // black for background
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(image);
        c.drawPaint(bp);
        c.drawText(text, 0, baseline, tp);
        return image;
    }

    /////////  LUMINANCE  bar and switch
    private void lumaButton(int luma) {this.connectedGlasses.luma((byte) luma);}
    private void sensorSwitch(boolean on) {this.connectedGlasses.sensor(on);}

    @SuppressLint("SetTextI18n")
    private void setUIGlassesInformations() {
        final Glasses glasses = this.connectedGlasses;
        glasses.settings(r -> sensorSwitch.setChecked(r.isGestureEnable()));
        glasses.settings(r -> luminanceSeekBar.setProgress(r.getLuma()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == requestCode && requestCode == Activity.RESULT_FIRST_USER) {
            if (data != null && data.hasExtra("connectedGlasses")) {
                this.connectedGlasses = data.getExtras().getParcelable("connectedGlasses");
                this.connectedGlasses.setOnDisconnected(glasses -> MainActivity.this.disconnect());
                runOnUiThread(MainActivity.this::setUIGlassesInformations);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        if (this.connectedGlasses != null) {savedInstanceState.putParcelable("connectedGlasses",
                this.connectedGlasses);}
        super.onSaveInstanceState(savedInstanceState);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        // If BT is not on, request that it be enabled.
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Your BlueTooth is not open !!!",
                    Toast.LENGTH_LONG).show();
            largeText.setText("Your BlueTooth is not open !!\n\n" +
                    "Please open BlueTooth and\n\n relaunch the application.");
            largeText.setTextColor(Color.parseColor("#FF0000"));
            largeText.setTypeface(largeText.getTypeface(), Typeface.BOLD);
        }
        if (!((DemoApp) this.getApplication()).isConnected()) {this.connectedGlasses = null;}
        this.updateVisibility();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Your BlueTooth is not open !!!",
                    Toast.LENGTH_LONG).show();
            largeText.setText("Your BlueTooth is not open !!\n\n" +
                    "Please open BlueTooth and\n\n relaunch the application.");
            largeText.setTextColor(Color.parseColor("#FF0000"));
            largeText.setTypeface(largeText.getTypeface(), Typeface.BOLD);
        }
        if (!((DemoApp) this.getApplication()).isConnected()) {this.connectedGlasses = null;}
    }

    protected void onPause() {super.onPause();}

    protected void onStop() {super.onStop();
        if(clockHandler != null)
            clockHandler.removeCallbacks(clockRunnable); // On arrete le callback
    }

    protected void onDestroy() {
        super.onDestroy();
        if (serviceIntent != null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
        if(clockHandler != null)
            clockHandler.removeCallbacks(clockRunnable); // On arrete le callback
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final Glasses g = this.connectedGlasses;

        //noinspection SimplifiableIfStatement
        if (id == R.id.about_app) {Toast.makeText(this.getApplicationContext(),
                getString(R.string.app_name) + "\nVersion " + getString(R.string.app_version),
                Toast.LENGTH_LONG).show();
            return true;}
        if (id == R.id.about_glasses) {
            if( g!=null) {Toast.makeText(this.getApplicationContext(),
                    "Glasses Name : " + g.getName() + "\n"
                            + "Firmware : " + g.getDeviceInformation().getFirmwareVersion(),
                    Toast.LENGTH_LONG).show();}
            else {Toast.makeText(this.getApplicationContext(),
                    "No connected glasses found yet!",
                    Toast.LENGTH_LONG).show();}
            return true;}
        return super.onOptionsItemSelected(item);
    }

    private void disconnect() {
        runOnUiThread(() -> {
            ((DemoApp) this.getApplication()).onDisconnected();
            MainActivity.this.connectedGlasses.disconnect();
            MainActivity.this.connectedGlasses = null;
            MainActivity.this.updateVisibility();
        });
    }

    public static String url2image(String text) {
        String image;
        switch (text) {
            case "android":
            case "com.android.systemui": image = "android"; break;
            case "com.aol.mobile.aolapp": image = "aol"; break;
            case "com.sec.android.app.sbrowser": image = "browser"; break;
            case "com.google.android.calendar": image = "calendar"; break;
            case "com.android.chrome": image = "chrome"; break;
            case "com.google.android.apps.chromecast.app": image = "chrome"; break;
            case "com.google.android.deskclock": image = "clock"; break;
            case "com.sec.android.app.clockpackage": image = "samsung_clock"; break;
            case "com.easilydo.mail": image = "courier"; break;
            case "com.samsung.android.email.provider": image = "courier_samsung"; break;
            case "com.google.android.apps.plus": image = "currents"; break;
            case "com.discord": image = "discord"; break;
            case "com.facebook.lite":
            case "com.facebook.katana": image = "facebook"; break;
            case "com.facebook.orca": image = "messenger"; break;  //(messenger)
            case "flipboard.app":
            case "flipboard.boxer.app": image = "flipboard"; break;
            case "com.google.android.gm": image = "gmail"; break;
            case "com.google.android.googlequicksearchbox": image = "google"; break;
            case "com.google.android.apps.fitness": image = "googlefit"; break;
            case "com.sec.android.app.shealth": image = "health"; break;
            case "com.google.android.keep": image = "keep"; break;
            case "com.instagram.android": image = "instagram"; break;
            case "com.linkedin.android": image = "linkedin"; break;
            case "com.google.android.apps.maps": image = "maps"; break;
            case "com.google.android.apps.tachyon": image = "meet"; break;
            case "com.google.android.apps.magazines": image = "news"; break;
            case "com.microsoft.office.outlook": image = "outlook"; break;
            case "com.samsung.android.phone":
            case "com.samsung.android.incallui":
            case "com.google.android.dialer": image = "phone"; break;
            case "com.microsoft.mobile.polymer": image = "polymer"; break; // Kaizala
            case "com.reddit.frontpage": image = "reddit"; break;
            case "org.thoughtcrime.securesms": image = "signal"; break;
            case "com.skype.raider": image = "skype"; break;
            case "com.Slack": image = "slack"; break;
            case "com.google.android.apps.messaging": image = "sms"; break;
            case "com.snapchat.android": image = "snapchat"; break;
            case "com.microsoft.teams": image = "teams"; break;
            case "org.telegram.messenger":
            case "org.thunderdog.challegram": image = "telegram"; break;
            case "com.zhiliaoapp.musically": image = "titok"; break; // tiktok
            case "com.twitter.android": image = "twitter"; break;
            case "com.verizon.messaging.vzmsgs": image = "verizon"; break;
            case "com.viber.voip": image = "viber"; break;
            case "com.samsung.android.app.watchmanager": image = "wear"; break;
            case "com.waze": image = "waze"; break;
            case "com.cisco.webex.meetings": image = "webex"; break;
            case "com.cisco.wx2.android": image = "webex"; break;
            case "com.tencent.mm": image = "wechat"; break;
            case "com.whatsapp": image = "whatsapp"; break;
            case "com.whatsapp.w4b": image = "whatsapp"; break;
            case "com.yahoo.mobile.client.android.mail": image = "yahoo_mail"; break;
            case "us.zoom.videomeetings":
            case "us.zoom.zrc": image = "zoom"; break;
            default : image = "other";
        }
        return image;
    }

}