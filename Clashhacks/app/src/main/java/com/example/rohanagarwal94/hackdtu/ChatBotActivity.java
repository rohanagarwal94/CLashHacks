package com.example.rohanagarwal94.hackdtu;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.rohanagarwal94.hackdtu.helper.Constant;
import com.example.rohanagarwal94.hackdtu.helper.DateTimeHelper;
import com.example.rohanagarwal94.hackdtu.helper.PrefManager;
import com.example.rohanagarwal94.hackdtu.model.ChatMessage;
import com.example.rohanagarwal94.hackdtu.model.Datum;
import com.example.rohanagarwal94.hackdtu.model.SusiResponse;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;


import static android.R.attr.password;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;

public class ChatBotActivity extends AppCompatActivity {
    public static String TAG = MainActivity.class.getName();
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int SELECT_PICTURE = 200;
    private final int CROP_PICTURE = 400;
    private static final String GOOGLE_SEARCH = "https://www.google.co.in/search?q=";
    private boolean isEnabled = true;
    private double latitude;
    private double longitude;
    private Location location;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.rv_chat_feed)
    RecyclerView rvChatFeed;
    @BindView(R.id.et_message)
    EditText ChatMessage;
    @BindView(R.id.send_message_layout)
    LinearLayout sendMessageLayout;
    @BindView(R.id.btnSpeak)
    ImageButton btnSpeak;
    private boolean atHome = true;
    private boolean backPressedOnce = false;
    private FloatingActionButton fab_scrollToEnd;
    //	 Global Variables used for the setMessage Method
    private String answer;
    private RequestQueue requestQueue;
    private String actionType;
    private boolean isMap, isPieChart = false;
    private boolean isHavingLink;
    private boolean isSearchResult;
    private boolean isWebSearch;
    private long delay = 0;
    private List<Datum> datumList = null;
    private RealmResults<ChatMessage> chatMessageDatabaseList;
    private Boolean micCheck;
    private SearchView searchView;
    private Boolean check;
    private Menu menu;
    private int pointer;
    private String locationName;
    private RealmResults<ChatMessage> results;
    private int offset = 1;
    private ChatFeedRecyclerAdapter recyclerAdapter;
    private Realm realm;
    public static String webSearch;
    private String googlesearch_query = "";
    private TextToSpeech textToSpeech;
    private String[] array;
    private String timenow;

    private AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
                        textToSpeech.stop();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // Resume playback
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        textToSpeech.stop();
                    }
                }
            };

    private Deque<Pair<String, Long>> nonDeliveredMessages = new LinkedList<>();
    TextWatcher watch = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(final CharSequence charSequence, int i, int i1, int i2) {
            if (charSequence.toString().trim().length() > 0 || !micCheck) {
                btnSpeak.setImageResource(R.drawable.ic_send_fab);
                btnSpeak.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        check = false;
                        switch (view.getId()) {
                            case R.id.btnSpeak:
                                String chat_message = ChatMessage.getText().toString();
                                chat_message = chat_message.trim();
                                String splits[] = chat_message.split("\n");
                                String message = "";
                                for (String split : splits)
                                    message = message.concat(split).concat(" ");
                                if (!TextUtils.isEmpty(chat_message)) {
                                    sendMessage(message);
                                    ChatMessage.setText("");
                                    Log.d("message", message);
                                }
                                break;
                        }
                    }
                });
            } else {
                btnSpeak.setImageResource(R.drawable.ic_mic_white_24dp);
                btnSpeak.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        check = true;
                        promptSpeechInput();
                    }
                });
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };
    private BroadcastReceiver networkStateReceiver;

    public static List<String> extractUrls(String text) {
        List<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            links.add(url);
        }
        return links;
    }

    public static Boolean checkSpeechOutputPref() {
        Boolean checks = PrefManager.getBoolean(Constant.SPEECH_OUTPUT, false);
        return checks;
    }

    public static Boolean checkSpeechAlwaysPref() {
        Boolean checked = PrefManager.getBoolean(Constant.SPEECH_ALWAYS, false);
        return checked;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        checkLocation();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                locationName = getLocationName(latitude, longitude);
                Log.d("location", locationName);
            }
        }, 5000);
        init();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            showToast(getString(R.string.speech_not_supported));
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Handler mHandler = new Handler(Looper.getMainLooper());
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<String> result = data
                                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            sendMessage(result.get(0));
                        }
                    });
                }
                break;
            }
            case CROP_PICTURE: {
                if (resultCode == RESULT_OK && null != data) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Bundle extras = data.getExtras();
                                Bitmap thePic = extras.getParcelable("data");
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                thePic.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] b = baos.toByteArray();
                                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                                //SharePreference to store image
                                PrefManager.putString(Constant.IMAGE_DATA, encodedImage);
                                //set gallery image
                                setChatBackground();
                            } catch (NullPointerException e) {
                                Log.d(TAG, e.getLocalizedMessage());
                            }
                        }
                    });
                }
                break;
            }
            case SELECT_PICTURE: {
                if (resultCode == RESULT_OK && null != data) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {

                            Uri selectedImageUri = data.getData();
                            InputStream imageStream;
                            Bitmap selectedImage;
                            try {
                                cropCapturedImage(selectedImageUri);
                            } catch (ActivityNotFoundException aNFE) {
                                //display an error message if user device doesn't support
                                showToast(getString(R.string.error_crop_not_supported));
                                try {
                                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                                    Cursor cursor = getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);
                                    cursor.moveToFirst();
                                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                    String picturePath = cursor.getString(columnIndex);
                                    imageStream = getContentResolver().openInputStream(selectedImageUri);
                                    selectedImage = BitmapFactory.decodeStream(imageStream);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                    byte[] b = baos.toByteArray();
                                    String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                                    //SharePreference to store image
                                    PrefManager.putString(Constant.IMAGE_DATA, encodedImage);
                                    cursor.close();
                                    //set gallery image
                                    setChatBackground();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
                break;
            }
        }
    }

    private void init() {
        ButterKnife.bind(this);
        realm = Realm.getDefaultInstance();
        fab_scrollToEnd = (FloatingActionButton) findViewById(R.id.btnScrollToEnd);
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new computeThread().start();
            }
        };
        Log.d(TAG, "init");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Top10Bot");

        nonDeliveredMessages.clear();
        RealmResults<ChatMessage> nonDelivered = realm.where(ChatMessage.class).equalTo("isDelivered", false).findAll().sort("id");
        for (ChatMessage each : nonDelivered) {
            Log.d(TAG, each.getContent());
            nonDeliveredMessages.add(new Pair(each.getContent(), each.getId()));
        }
        checkEnterKeyPref();
        setupAdapter();
        rvChatFeed.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) rvChatFeed.getLayoutManager();
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() < rvChatFeed.getAdapter().getItemCount() - 5) {
                    fab_scrollToEnd.setEnabled(true);
                    fab_scrollToEnd.setVisibility(View.VISIBLE);
                } else {
                    fab_scrollToEnd.setEnabled(false);
                    fab_scrollToEnd.setVisibility(View.GONE);
                }
            }
        });
        ChatMessage.addTextChangedListener(watch);
        ChatMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String message = ChatMessage.getText().toString();
                    message = message.trim();
                    if (!TextUtils.isEmpty(message)) {
                        sendMessage(message);
                        ChatMessage.setText("");
                    }
                    handled = true;
                }
                return handled;
            }
        });
        setChatBackground();
    }


    private void voiceReply(final String reply, final boolean isMap) {
        if ((checkSpeechOutputPref() && check) || checkSpeechAlwaysPref()) {
            final AudioManager audiofocus = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int result = audiofocus.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status != TextToSpeech.ERROR) {
                                    Locale locale = textToSpeech.getLanguage();
                                    textToSpeech.setLanguage(locale);
                                    String spokenReply = reply;
                                    if (isMap) {
                                        spokenReply = reply.substring(0, reply.indexOf("http"));
                                    }
                                    textToSpeech.speak(spokenReply, TextToSpeech.QUEUE_FLUSH, null);
                                    audiofocus.abandonAudioFocus(afChangeListener);
                                }
                            }
                        });
                    }
                }
            });
        }
    }


    public void cropCapturedImage(Uri picUri) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(picUri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 9);
        cropIntent.putExtra("aspectY", 14);
        cropIntent.putExtra("outputX", 256);
        cropIntent.putExtra("outputY", 256);
        cropIntent.putExtra("return-data", true);
        startActivityForResult(cropIntent, CROP_PICTURE);
    }

    public void setChatBackground() {
        String previouslyChatImage = PrefManager.getString(Constant.IMAGE_DATA, "");
        Drawable bg;
        if (previouslyChatImage.equalsIgnoreCase(getString(R.string.background_no_wall))) {
            //set no wall
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this, R.color.default_bg));
        } else if (!previouslyChatImage.equalsIgnoreCase("")) {
            byte[] b = Base64.decode(previouslyChatImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            bg = new BitmapDrawable(getResources(), bitmap);
            //set Drawable bitmap which taking from gallery
            getWindow().setBackgroundDrawable(bg);
        } else {
            //set default layout when app launch first time
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this, R.color.default_bg));
        }
    }

    private void checkEnterKeyPref() {
        micCheck = PrefManager.getBoolean(Constant.MIC_INPUT, true);
        if (micCheck) {
            btnSpeak.setImageResource(R.drawable.ic_mic_white_24dp);
            btnSpeak.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    check = true;
                    promptSpeechInput();
                }
            });
        } else {
            check = false;
            btnSpeak.setImageResource(R.drawable.ic_send_fab);
            btnSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.btnSpeak:
                            String message = ChatMessage.getText().toString();
                            message = message.trim();
                            if (!TextUtils.isEmpty(message)) {
                                sendMessage(message);
                                ChatMessage.setText("");
                            }
                            break;
                    }
                }
            });
        }
        Boolean isChecked = PrefManager.getBoolean(Constant.ENTER_SEND, false);
        if (isChecked) {
            ChatMessage.setImeOptions(EditorInfo.IME_ACTION_SEND);
            ChatMessage.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            ChatMessage.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            ChatMessage.setSingleLine(false);
            ChatMessage.setMaxLines(4);
            ChatMessage.setVerticalScrollBarEnabled(true);
        }
    }

    private void setupAdapter() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        rvChatFeed.setLayoutManager(linearLayoutManager);
        rvChatFeed.setHasFixedSize(false);
        chatMessageDatabaseList = realm.where(ChatMessage.class).findAllSorted("id");
        recyclerAdapter = new ChatFeedRecyclerAdapter(Glide.with(this), this, chatMessageDatabaseList, true);
        rvChatFeed.setAdapter(recyclerAdapter);
        rvChatFeed.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    rvChatFeed.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int scrollTo = rvChatFeed.getAdapter().getItemCount() - 1;
                            scrollTo = scrollTo >= 0 ? scrollTo : 0;
                            rvChatFeed.scrollToPosition(scrollTo);
                        }
                    }, 10);
                }
            }
        });
    }

    private void sendMessage(String query) {
        webSearch = query;
        Number temp = realm.where(ChatMessage.class).max(getString(R.string.id));
        long id;
        if (temp == null) {
            id = 0;
        } else {
            id = (long) temp + 1;
        }
        boolean isHavingLink;
        List<String> urlList = extractUrls(query);
        Log.d(TAG, urlList.toString());
        isHavingLink = urlList != null;
        if (urlList.size() == 0) isHavingLink = false;
        if(id==0) {
            updateDatabase(id, " ", DateTimeHelper.getDate(), true, false, false, false, false, false, false, DateTimeHelper.getCurrentTime(), false, null);
            id++;
        } else {
            String s = realm.where(ChatMessage.class).equalTo("id",id-1).findFirst().getDate();
            if(!DateTimeHelper.getDate().equals(s)) {
                updateDatabase(id, "", DateTimeHelper.getDate(), true, false, false, false, false, false, false, DateTimeHelper.getCurrentTime(), false, null);
                id++;
            }
        }

        updateDatabase(id, query, DateTimeHelper.getDate(), false, true, false, false, false, false, isHavingLink, DateTimeHelper.getCurrentTime(), false, null);
        nonDeliveredMessages.add(new Pair(query, id));
        recyclerAdapter.showDots();
        new computeThread().start();
    }

//    private synchronized void computeOtherMessage() {
//        final String query;
//        final long id;
//        String answer_call=null;
//        String google_search = null;
//        String setAlarm = null;
//        if (null != nonDeliveredMessages && !nonDeliveredMessages.isEmpty()) {
//            if (isNetworkConnected()) {
//                TimeZone tz = TimeZone.getDefault();
//                Date now = new Date();
//                int timezoneOffset = -1 * (tz.getOffset(now.getTime()) / 60000);
//                query = nonDeliveredMessages.getFirst().first;
//                id = nonDeliveredMessages.getFirst().second;
//                nonDeliveredMessages.pop();
//                String section[]=query.split(" ");
//                if(section.length==2 && section[0].equalsIgnoreCase("call")){
//                    answer_call="Calling "+section[1];
//                }
//                if(query.toLowerCase().contains("set alarm")){
//
//                    LinkedList<String> list = new LinkedList<>();
//                    Matcher matcher = Pattern.compile("\\d+").matcher(query);
//                    while (matcher.find()) {
//                        list.add(matcher.group());
//                    }
//                    array = list.toArray(new String[list.size()]);
//                    System.out.println(Arrays.toString(array));
//
//                    Log.v(TAG, "computeOtherMessage: " + Arrays.toString(array));
//
//                    setAlarm = getString(R.string.set_alarm);
//
//                    if(query.toLowerCase().contains("am"))
//                        timenow = "AM";
//                    else if(query.toLowerCase().contains("pm"))
//                        timenow = "PM";
//                    else
//                        timenow = null;
//
//
//                }
//                if(section[0].equalsIgnoreCase("@google")){
//                    int size = section.length;
//                    for (int i = 1; i < size; i++) {
//                        googlesearch_query = googlesearch_query + " " + section[i];
//                    }
//                    google_search = getString(R.string.google_search);
//                }
//                final float latitude = PrefManager.getFloat(Constant.LATITUDE, 0);
//                final float longitude = PrefManager.getFloat(Constant.LONGITUDE, 0);
//                final String geo_source = PrefManager.getString(Constant.GEO_SOURCE, "ip");
//                Log.d(TAG, clientBuilder.getSusiApi().getSusiResponse(timezoneOffset, longitude, latitude, geo_source, query).request().url().toString());
//                final String finalAnswer_call = answer_call;
//                final String finalgoogle_search = google_search;
//                final String finalSetAlarm = setAlarm;
//                clientBuilder.getSusiApi().getSusiResponse(timezoneOffset, longitude, latitude, geo_source, query).enqueue(
//                        new Callback<SusiResponse>() {
//                            @Override
//                            public void onResponse(Call<SusiResponse> call,
//                                                   Response<SusiResponse> response) {
//                                recyclerAdapter.hideDots();
//                                if (response != null && response.isSuccessful() && response.body() != null) {
//                                    final SusiResponse susiResponse = response.body();
//                                    int responseActionSize = response.body().getAnswers().get(0).getActions().size();
//                                    for (int counter = 0; counter < responseActionSize; counter++) {
//
//                                        try {
//                                            answer = susiResponse.getAnswers().get(0).getActions()
//                                                    .get(counter).getExpression();
//                                            delay = susiResponse.getAnswers().get(0).getActions()
//                                                    .get(counter).getDelay();
//
//
//                                            try {
//                                                isMap = response.body().getAnswers().get(0).getActions().get(2).getType().equals("map");
//                                                datumList = response.body().getAnswers().get(0).getData();
//                                            } catch (Exception e) {
//                                                isMap = false;
//                                            }
//                                            List<String> urlList = extractUrls(answer);
//                                            Log.d(TAG, urlList.toString());
//                                            String setMessage = answer;
//                                            voiceReply(setMessage, isMap);
//                                            isHavingLink = urlList != null;
//                                            if (urlList.size() == 0) isHavingLink = false;
//                                        } catch (IndexOutOfBoundsException | NullPointerException e) {
//                                            Log.d(TAG, e.getLocalizedMessage());
//                                            answer = getString(R.string.error_occurred_try_again);
//                                            isHavingLink = false;
//                                        }
//                                        try {
//                                            if (response.body().getAnswers().get(0).getActions().size() > 1) {
//                                                isPieChart = actionType != null && actionType.equals("piechart");
//                                                datumList = response.body().getAnswers().get(0).getData();
//                                            }
//                                        } catch (Exception e) {
//                                            Log.d(TAG, e.getLocalizedMessage());
//                                            isPieChart = false;
//                                        }
//                                        try {
//                                            isSearchResult = response.body().getAnswers().get(0).getActions().get(1).getType().equals("rss");
//                                            datumList = response.body().getAnswers().get(0).getData();
//                                        } catch (Exception e) {
//                                            isSearchResult = false;
//                                        }
//                                        try {
//                                            isWebSearch = response.body().getAnswers().get(0).getActions().get(1).getType().equals("websearch");
//                                            datumList = response.body().getAnswers().get(0).getData();
//                                        } catch (Exception e) {
//                                            isWebSearch = false;
//                                        }
//
//                                        realm.executeTransactionAsync(new Realm.Transaction() {
//                                            @Override
//                                            public void execute(Realm bgRealm) {
//                                                long prId = id;
//                                                try {
//                                                    ChatMessage chatMessage = bgRealm.where(ChatMessage.class).equalTo("id", prId).findFirst();
//                                                    chatMessage.setIsDelivered(true);
//                                                } catch (Exception e) {
//                                                    e.printStackTrace();
//                                                }
//                                            }
//                                        });
//                                        rvChatFeed.getRecycledViewPool().clear();
//                                        recyclerAdapter.notifyItemChanged((int) id);
//
//                                        if(finalAnswer_call!=null && finalAnswer_call.contains("Calling"))
//                                        {
//                                            answer = finalAnswer_call;
//                                            isWebSearch = false;
//                                        }
//                                        if(finalgoogle_search!=null&&finalgoogle_search.contains("google"))
//                                        {
//                                            answer = finalgoogle_search;
//                                            isWebSearch = false;
//                                        }
//                                        if(finalSetAlarm !=null && finalSetAlarm.contains("Alarm")){
//                                            answer = finalSetAlarm;
//                                            isWebSearch = false;
//                                        }
//
//                                        final String setMessage = answer;
//                                        final int counterValue = counter;
//                                        actionType = response.body().getAnswers().get(0).getActions().get(counter).getType();
//                                        final Handler delayHandler = new Handler();
//                                        delayHandler.postDelayed(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                actionType = susiResponse.getAnswers().get(0).getActions().get(counterValue).getType();
//                                                if ("answer".equals(actionType)) {
//                                                    addNewMessage(setMessage, isMap, isHavingLink, isPieChart, isWebSearch, isSearchResult, datumList);
//                                                }
//                                                else if ("anchor".equals(actionType)) {
//                                                    String text = susiResponse.getAnswers().get(0).getActions().get(counterValue).getAnchorText();
//                                                    String link = susiResponse.getAnswers().get(0).getActions().get(counterValue).getAnchorLink();
//                                                    if (link != null) {
//                                                        addNewMessage(text.concat(": ".concat(link)), false, isHavingLink, false, false, false, datumList);
//                                                    }
//                                                    else {
//                                                        String mapDisplayName;
//                                                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
//                                                        try {
//                                                            float lat = PrefManager.getFloat(Constant.LATITUDE,0);
//                                                            float lon = PrefManager.getFloat(Constant.LONGITUDE,0);
//                                                            List<Address> addresses = geocoder.getFromLocation(lat,lon,1);
//                                                            StringBuilder locationAddressBuilder = new StringBuilder();
//                                                            int addressLineSize = addresses.get(0).getMaxAddressLineIndex();
//                                                            for (int addressLineCount = 0; addressLineCount < addressLineSize - 1;addressLineCount++)
//                                                            {
//                                                                locationAddressBuilder.append(addresses.get(0).getAddressLine(addressLineCount)).append(", ");
//                                                            }
//                                                            locationAddressBuilder.append(addresses.get(0).getAddressLine(addressLineSize-1));
//                                                            mapDisplayName =  "Address: ".concat(locationAddressBuilder.toString());
//                                                            addNewMessage(mapDisplayName, false, false, false, false, false, datumList);
//                                                        }
//                                                        catch (Exception e){
//                                                            e.printStackTrace();
//                                                        }
//                                                    }
//                                                }
////                                                else if ("map".equals(actionType)) {
////
////                                               }
////                                                else if ("websearch".equals(actionType)) {
//                                                //String query = susiResponse.getAnswers().get(0).getActions().get(counterValue).getQuery();
//                                                //TODO: Implement web search result.
////                                                }
//                                            }
//                                        }, delay);
//
//                                    }
//
//                                } else {
//                                    if (!isNetworkConnected()) {
//                                        recyclerAdapter.hideDots();
//                                        nonDeliveredMessages.addFirst(new Pair(query, id));
//                                        Snackbar snackbar = Snackbar.make(coordinatorLayout,
//                                                getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG);
//                                        snackbar.show();
//                                    } else {
//                                        realm.executeTransactionAsync(new Realm.Transaction() {
//                                            @Override
//                                            public void execute(Realm bgRealm) {
//                                                long prId = id;
//                                                try {
//                                                    ChatMessage chatMessage = bgRealm.where(ChatMessage.class).equalTo("id", prId).findFirst();
//                                                    chatMessage.setIsDelivered(true);
//                                                } catch (Exception e) {
//                                                    e.printStackTrace();
//                                                }
//                                            }
//                                        });
//                                        rvChatFeed.getRecycledViewPool().clear();
//                                        recyclerAdapter.notifyItemChanged((int) id);
//                                        addNewMessage(getString(R.string.error_invalid_token), false, false, false, false, false, null);
//                                    }
//                                    rvChatFeed.getRecycledViewPool().clear();
//                                    recyclerAdapter.notifyItemChanged((int) id);
//                                    addNewMessage(getString(R.string.error_invalid_token), false, false, false, false, false, null);
//                                }
//
//
//                                if (isNetworkConnected())
//                                    computeOtherMessage();
//                            }
//
//                            @Override
//                            public void onFailure(Call<SusiResponse> call, Throwable t) {
//                                if (t.getLocalizedMessage() != null) {
//                                    Log.d(TAG, t.getLocalizedMessage());
//                                } else {
//                                    Log.d(TAG, "An error occurred", t);
//                                }
//                                recyclerAdapter.hideDots();
//
//                                if (!isNetworkConnected()) {
//                                    recyclerAdapter.hideDots();
//                                    nonDeliveredMessages.addFirst(new Pair(query, id));
//                                    Snackbar snackbar = Snackbar.make(coordinatorLayout,
//                                            getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG);
//                                    snackbar.show();
//                                } else {
//                                    realm.executeTransactionAsync(new Realm.Transaction() {
//                                        @Override
//                                        public void execute(Realm bgRealm) {
//                                            long prId = id;
//                                            try {
//                                                ChatMessage chatMessage = bgRealm.where(ChatMessage.class).equalTo("id", prId).findFirst();
//                                                chatMessage.setIsDelivered(true);
//                                            } catch (Exception e) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                    });
//                                    rvChatFeed.getRecycledViewPool().clear();
//                                    recyclerAdapter.notifyItemChanged((int) id);
//                                    addNewMessage(getString(R.string.error_internet_connectivity), false, false, false, false, false, null);
//                                }
//                                computeOtherMessage();
//                            }
//                        });
//            } else {
//                recyclerAdapter.hideDots();
//                Snackbar snackbar = Snackbar.make(coordinatorLayout,
//                        getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG);
//                snackbar.show();
//            }
//        }
//    }

    private void addNewMessage(String answer, boolean isMap, boolean isHavingLink, boolean isPieChart, boolean isWebSearch, boolean isSearchReult, List<Datum> datumList) {
        Number temp = realm.where(ChatMessage.class).max(getString(R.string.id));
        long id;
        if (temp == null) {
            id = 0;
        } else {
            id = (long) temp + 1;
        }


        if (answer.equals(getString(R.string.google_search))) {
            googlesearch_query = googlesearch_query.replace(" ", "+");
            String url = GOOGLE_SEARCH + googlesearch_query;
            Uri uri = Uri.parse(url);
            // create an intent builder
            CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
            // Begin customizing
            // set toolbar colors
            intentBuilder.setToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
            intentBuilder.setSecondaryToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));

            // set start and exit animations
            //intentBuilder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
            intentBuilder.setExitAnimations(getApplicationContext(), android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right);
            // build custom tabs intent
            CustomTabsIntent customTabsIntent = intentBuilder.build();
            // launch the url
            customTabsIntent.launchUrl(ChatBotActivity.this, uri);
            googlesearch_query = "";
        }

        if(answer.contains("Calling")){
            String splits[]=answer.split(" ");
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", splits[1], null)));
        }

        if(answer.equals(getString(R.string.set_alarm))){

            Log.d(TAG, "addNewMessage: " + Arrays.toString(array));
            Log.d(TAG, "addNewMessage: " + array.length);
            Log.d(TAG, "addNewMessage: " + timenow);


            if(array.length == 0){

                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                i.putExtra(AlarmClock.EXTRA_MESSAGE, "New Alarm");
                startActivity(i);

            }
            else if(array.length == 1){


                Log.d(TAG, "addNewMessage: " + array[0]);

                int hour = Integer.parseInt(array[0]);

                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                i.putExtra(AlarmClock.EXTRA_MESSAGE, "New Alarm");
                i.putExtra(AlarmClock.EXTRA_HOUR, hour);
                i.putExtra(AlarmClock.EXTRA_MINUTES, 0);
                if (timenow!=null&&Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    i.putExtra(AlarmClock.EXTRA_IS_PM,timenow.equalsIgnoreCase("PM"));
                }
                startActivity(i);
            }

            else {

                Log.d(TAG, "addNewMessage: " + array[0] + array[1]);

                int hour = Integer.parseInt(array[0]);
                int min = Integer.parseInt(array[1]);

                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                i.putExtra(AlarmClock.EXTRA_MESSAGE, "New Alarm");
                i.putExtra(AlarmClock.EXTRA_HOUR, hour);
                i.putExtra(AlarmClock.EXTRA_MINUTES, min);
                if (timenow!=null&&Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    i.putExtra(AlarmClock.EXTRA_IS_PM,timenow.equalsIgnoreCase("PM"));
                }
                startActivity(i);

            }


        }

        updateDatabase(id, answer, DateTimeHelper.getDate(), false, false, isSearchReult, isWebSearch, false, isMap, isHavingLink, DateTimeHelper.getCurrentTime(), isPieChart, datumList);
    }

    public String getLocationName(double lattitude, double longitude) {

        String cityName = "Delhi";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {

            List<Address> addresses = gcd.getFromLocation(lattitude, longitude,
                    10);

            for (Address adrs : addresses) {
                if (adrs != null) {

                    String city = adrs.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                        System.out.println("city ::  " + cityName);
                    } else {

                    }
                    // // you should also try with addresses.get(0).toSring();

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;

    }


    private void updateDatabase(final long id, final String message, final String date,
                                final boolean isDate, final boolean mine, final boolean isSearchResult,
                                final boolean isWebSearch, final boolean image, final boolean isMap,
                                final boolean isHavingLink, final String timeStamp,
                                final boolean isPieChart, final List<Datum> datumList) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                ChatMessage chatMessage = bgRealm.createObject(ChatMessage.class,id);
                chatMessage.setWebSearch(isWebSearch);
                chatMessage.setContent(message);
                chatMessage.setDate(date);
                chatMessage.setIsDate(isDate);
                chatMessage.setIsMine(mine);
                chatMessage.setIsImage(image);
                chatMessage.setTimeStamp(timeStamp);
                chatMessage.setMap(isMap);
                chatMessage.setHavingLink(isHavingLink);
                chatMessage.setIsPieChart(isPieChart);
                chatMessage.setSearchResult(isSearchResult);
                if (mine)
                    chatMessage.setIsDelivered(false);
                else
                    chatMessage.setIsDelivered(true);
                if (datumList != null) {
                    RealmList<Datum> datumRealmList = new RealmList<>();
                    for (Datum datum : datumList) {
                        Datum realmDatum = bgRealm.createObject(Datum.class);
                        realmDatum.setDescription(datum.getDescription());
                        realmDatum.setLink(datum.getLink());
                        realmDatum.setTitle(datum.getTitle());
                        datumRealmList.add(realmDatum);
                    }
                    chatMessage.setDatumRealmList(datumRealmList);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Log.v(TAG, getString(R.string.updated_successfully));
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
    }


    private void searchMovement(int position) {
        rvChatFeed.scrollToPosition(position);
        recyclerAdapter.highlightMessagePosition = position;
        recyclerAdapter.notifyDataSetChanged();
    }

    public boolean isGPSEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private LocationManager locationManager;
    private android.location.LocationListener myLocationListener;

    public void checkLocation() {

        String serviceString = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) getSystemService(serviceString);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        myLocationListener = new android.location.LocationListener() {
            public void onLocationChanged(Location locationListener) {

                if (isGPSEnabled(ChatBotActivity.this)) {
                    if (locationListener != null) {
                        if (ActivityCompat.checkSelfPermission(ChatBotActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ChatBotActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                } else if (isInternetConnected(ChatBotActivity.this)) {
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }



            }

            public void onProviderDisabled(String provider) {

            }

            public void onProviderEnabled(String provider) {

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, myLocationListener);
    }

    public static boolean isInternetConnected(Context ctx) {
        ConnectivityManager connectivityMgr = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        // Check if wifi or mobile network is available or not. If any of them is
        // available or connected then it will return true, otherwise false;
        if (wifi != null) {
            if (wifi.isConnected()) {
                return true;
            }
        }
        if (mobile != null) {
            if (mobile.isConnected()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {

            if (!isEnabled) {
                ChatMessage.setVisibility(View.VISIBLE);
                btnSpeak.setVisibility(View.VISIBLE);
                sendMessageLayout.setVisibility(View.VISIBLE);
            }

            recyclerAdapter.highlightMessagePosition = -1;
            recyclerAdapter.notifyDataSetChanged();
            searchView.onActionViewCollapsed();
            offset = 1;
            return;
        }
        if (atHome) {
            if (backPressedOnce) {
                finish();
            }
            backPressedOnce = true;
            Toast.makeText(this, R.string.exit, Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressedOnce = false;
                }
            }, 2000);
        } else if (!atHome) {
            atHome = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        nonDeliveredMessages.clear();
        RealmResults<ChatMessage> nonDelivered = realm.where(ChatMessage.class).equalTo("isDelivered", false).findAll().sort("id");
        for (ChatMessage each : nonDelivered)
            nonDeliveredMessages.add(new Pair(each.getContent(), each.getId()));
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        checkEnterKeyPref();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(networkStateReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void scrollToEnd(View view) {
        rvChatFeed.smoothScrollToPosition(rvChatFeed.getAdapter().getItemCount() - 1);
    }

    private class computeThread extends Thread {
        public void run() {
//            computeOtherMessage();

        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.analyse:
                return readChat();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean readChat() {
        Realm realm = Realm.getDefaultInstance();

        RealmResults<ChatMessage> result = realm.where(ChatMessage.class).equalTo("isMine",true).findAll();
        String paragraph = "";
        for(int i=0;i<result.size();i++){
            paragraph += (result.get(i).getContent());
        }
        getEmotion(paragraph);
        return true;
    }
    void getEmotion(String paragraph){
        requestQueue = Volley.newRequestQueue(ChatBotActivity.this);
        String url="https://codeslayers.herokuapp.com/getEmotion";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("text", "A suicide note or death note is a message left behind before a person has committed suicide, or who intends to commit suicide.  It is estimated that 25–30% of suicides are accompanied by a note. According to Gelder, Mayou and Geddes (2005) one in six leaves a suicide note. The content can be a plea for absolution or blaming family and friends for life's failings. However, incidence rates may depend on ethnicity, method of suicide, and cultural differences, and may reach rates as high as 50% in certain demographics.[1] A suicide message can be a written note, an audio message, or a video.");
//        JSONObject jsonObject=new JSONObject();
//        try {
//            jsonObject.put("text","A suicide note or death note is a message left behind before a person has committed suicide, or who intends to commit suicide.  It is estimated that 25–30% of suicides are accompanied by a note. According to Gelder, Mayou and Geddes (2005) one in six leaves a suicide note. The content can be a plea for absolution or blaming family and friends for life's failings. However, incidence rates may depend on ethnicity, method of suicide, and cultural differences, and may reach rates as high as 50% in certain demographics.[1] A suicide message can be a written note, an audio message, or a video.");
//        }catch (JSONException e){
//            //Toast.makeText(getApplicationContext(), "Json exception", Toast.LENGTH_SHORT).show();
//        }
        JsonObjectRequest jor = new JsonObjectRequest(url, new JSONObject(params),
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                            Toast.makeText(ChatBotActivity.this, "received", Toast.LENGTH_SHORT).show();
                            System.out.println(response.toString());

                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(),"User already registered or Error in internet connection.",Toast.LENGTH_LONG).show();
                        Log.e("Volley",error.toString());
                    }
                }
        ){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
//                params.put("text", "A suicide note or death note is a message left behind before a person has committed suicide, or who intends to commit suicide.  It is estimated that 25–30% of suicides are accompanied by a note. According to Gelder, Mayou and Geddes (2005) one in six leaves a suicide note. The content can be a plea for absolution or blaming family and friends for life's failings. However, incidence rates may depend on ethnicity, method of suicide, and cultural differences, and may reach rates as high as 50% in certain demographics.[1] A suicide message can be a written note, an audio message, or a video.");
                  //  params.put("","");
                return params;
            }


        };
        requestQueue.add(jor);

    }
}
