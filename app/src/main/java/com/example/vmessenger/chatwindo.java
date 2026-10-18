package com.example.vmessenger;

import android.widget.ScrollView;
import android.view.Gravity;
import android.graphics.Typeface;
import android.graphics.Color;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class chatwindo extends AppCompatActivity {
    private static final String TAG = "CHAT_WINDOW";

    private static final String BACKEND_URL =
            "http://127.0.0.1:3000/index-message";

    private static final String SUMMARY_URL =
            "http://127.0.0.1:3000/summarize-chat";

    String reciverimg;
    String reciverUid;
    String reciverName;
    String SenderUID;

    CircleImageView profile;
    TextView reciverNName;

    FirebaseDatabase database;
    FirebaseAuth firebaseAuth;

    public static String senderImg = "";
    public static String reciverIImg = "";

    CardView sendbtn;
    EditText textmsg;
    RecyclerView messageAdpter;
    ArrayList<msgModelclass> messagesArrayList;
    messagesAdpter mmessagesAdpter;

    String senderRoom;
    String reciverRoom;

    private String targetMessageId = null;
    private String targetMessageText = null;
    private String targetMessageSenderId = null;
    private long targetMessageTimestamp = 0;
    private int targetMessagePosition = -1;
    private boolean openedFromSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatwindo);

        // Hide only a clearly identified search ImageView.
        hideSearchIconOnly();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        SenderUID = firebaseAuth.getUid();

        if (SenderUID == null || SenderUID.isEmpty()) {
            Toast.makeText(
                    chatwindo.this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        reciverUid = getIntent().getStringExtra("uid");
        reciverName = getIntent().getStringExtra("nameeee");
        reciverimg = getIntent().getStringExtra("reciverImg");

        targetMessageId = getIntent().getStringExtra("targetMessageId");
        targetMessageText = getIntent().getStringExtra("targetMessageText");
        targetMessageSenderId =
                getIntent().getStringExtra("targetMessageSenderId");

        targetMessageTimestamp =
                getIntent().getLongExtra("targetMessageTimestamp", 0);

        openedFromSearch =
                targetMessageId != null && !targetMessageId.isEmpty();

        reciverIImg = reciverimg != null ? reciverimg : "";
        messagesArrayList = new ArrayList<>();

        Log.d(TAG, "CHAT WINDOW OPENED");
        Log.d(TAG, "Current User = " + SenderUID);
        Log.d(TAG, "Intent Receiver = " + reciverUid);
        Log.d(TAG, "Target Message ID = " + targetMessageId);
        Log.d(TAG, "Target Message Text = " + targetMessageText);
        Log.d(TAG, "Target Sender = " + targetMessageSenderId);

        sendbtn = findViewById(R.id.sendbtnn);
        textmsg = findViewById(R.id.textmsg);
        reciverNName = findViewById(R.id.recivername);
        profile = findViewById(R.id.profileimgg);
        messageAdpter = findViewById(R.id.msgadpter);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(chatwindo.this);

        layoutManager.setStackFromEnd(true);
        messageAdpter.setLayoutManager(layoutManager);

        mmessagesAdpter =
                new messagesAdpter(chatwindo.this, messagesArrayList);

        messageAdpter.setAdapter(mmessagesAdpter);

        addSummarizeChatButton();

        if (reciverUid == null
                || reciverUid.trim().isEmpty()
                || reciverUid.equals(SenderUID)) {

            String conversationId =
                    getIntent().getStringExtra("conversationId");

            String recoveredUid =
                    findOtherUserFromConversation(conversationId, SenderUID);

            if (recoveredUid != null
                    && !recoveredUid.isEmpty()
                    && !recoveredUid.equals(SenderUID)) {

                reciverUid = recoveredUid;
                Log.d(TAG, "Recovered receiver UID = " + reciverUid);

            } else {
                Toast.makeText(
                        chatwindo.this,
                        "Could not identify chat user",
                        Toast.LENGTH_LONG
                ).show();
                finish();
                return;
            }
        }

        senderRoom = SenderUID + reciverUid;
        reciverRoom = reciverUid + SenderUID;

        Log.d(TAG, "Sender Room = " + senderRoom);
        Log.d(TAG, "Receiver Room = " + reciverRoom);

        loadReceiverProfile();
        loadSenderProfile();
        loadChatMessages();
        setupSendButton();
    }

    private String findOtherUserFromConversation(
            String conversationId,
            String currentUserId
    ) {
        if (conversationId == null
                || conversationId.isEmpty()
                || currentUserId == null
                || currentUserId.isEmpty()) {
            return null;
        }

        if (conversationId.startsWith(currentUserId)) {
            String otherUid =
                    conversationId.substring(currentUserId.length());

            if (!otherUid.isEmpty()) {
                return otherUid;
            }
        }

        if (conversationId.endsWith(currentUserId)) {
            String otherUid = conversationId.substring(
                    0,
                    conversationId.length() - currentUserId.length()
            );

            if (!otherUid.isEmpty()) {
                return otherUid;
            }
        }

        return null;
    }

    private void loadReceiverProfile() {
        DatabaseReference userReference = database.getReference()
                .child("user")
                .child(reciverUid);

        userReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        if (!snapshot.exists()) {
                            Log.e(
                                    TAG,
                                    "Receiver profile not found: " + reciverUid
                            );

                            reciverNName.setText(
                                    reciverName != null ? reciverName : ""
                            );

                            if (reciverimg == null || reciverimg.isEmpty()) {
                                profile.setImageResource(
                                        R.drawable.photocamera
                                );
                            }
                            return;
                        }

                        String name = "";
                        Object nameValue =
                                snapshot.child("name").getValue();

                        if (nameValue != null) {
                            name = nameValue.toString();
                        }

                        if (name.isEmpty()) {
                            Object usernameValue =
                                    snapshot.child("username").getValue();

                            if (usernameValue != null) {
                                name = usernameValue.toString();
                            }
                        }

                        if (name.isEmpty() && reciverName != null) {
                            name = reciverName;
                        }

                        reciverName = name;
                        reciverNName.setText(name);

                        String image = "";
                        Object imageValue =
                                snapshot.child("profilepic").getValue();

                        if (imageValue != null) {
                            image = imageValue.toString();
                        }

                        if (!image.isEmpty()) {
                            reciverimg = image;
                            reciverIImg = image;
                            Picasso.get().load(image).into(profile);

                        } else if (reciverimg != null
                                && !reciverimg.isEmpty()) {
                            Picasso.get().load(reciverimg).into(profile);

                        } else {
                            profile.setImageResource(
                                    R.drawable.photocamera
                            );
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Receiver profile error",
                                error.toException()
                        );
                    }
                }
        );
    }

    private void loadSenderProfile() {
        DatabaseReference reference = database.getReference()
                .child("user")
                .child(SenderUID);

        reference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        if (snapshot.exists()
                                && snapshot.child("profilepic").getValue()
                                != null) {

                            senderImg = snapshot.child("profilepic")
                                    .getValue().toString();
                        } else {
                            senderImg = "";
                        }

                        if (mmessagesAdpter != null) {
                            mmessagesAdpter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Sender profile error",
                                error.toException()
                        );
                    }
                }
        );
    }

    private void loadChatMessages() {
        DatabaseReference primaryReference = database.getReference()
                .child("chats")
                .child(senderRoom)
                .child("messages");

        primaryReference.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        if (snapshot.exists()
                                && snapshot.getChildrenCount() > 0) {
                            displayMessages(snapshot);
                        } else {
                            loadReverseRoom();
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Primary chat load failed",
                                error.toException()
                        );
                        loadReverseRoom();
                    }
                }
        );
    }

    private void loadReverseRoom() {
        DatabaseReference reverseReference = database.getReference()
                .child("chats")
                .child(reciverRoom)
                .child("messages");

        reverseReference.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        if (snapshot.exists()
                                && snapshot.getChildrenCount() > 0) {
                            displayMessages(snapshot);
                        } else {
                            messagesArrayList.clear();
                            mmessagesAdpter.notifyDataSetChanged();
                            Log.d(
                                    TAG,
                                    "No messages found in either chat room"
                            );
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        Log.e(
                                TAG,
                                "Reverse chat load failed",
                                error.toException()
                        );
                    }
                }
        );
    }

    private void displayMessages(DataSnapshot snapshot) {
        messagesArrayList.clear();
        targetMessagePosition = -1;
        int position = 0;

        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            msgModelclass message =
                    dataSnapshot.getValue(msgModelclass.class);

            if (message == null) {
                continue;
            }

            messagesArrayList.add(message);

            String firebaseMessageId = dataSnapshot.getKey();

            if (targetMessageId != null
                    && firebaseMessageId != null
                    && targetMessageId.equals(firebaseMessageId)) {

                targetMessagePosition = position;

                Log.d(TAG, "TARGET MESSAGE FOUND");
                Log.d(TAG, "Message ID = " + firebaseMessageId);
                Log.d(TAG, "Message position = " + targetMessagePosition);
                Log.d(TAG, "Message text = " + targetMessageText);
            }

            position++;
        }

        mmessagesAdpter.notifyDataSetChanged();

        if (targetMessagePosition >= 0) {
            final int finalPosition = targetMessagePosition;

            mmessagesAdpter.setHighlightedPosition(finalPosition);

            messageAdpter.post(() -> {
                RecyclerView.LayoutManager manager =
                        messageAdpter.getLayoutManager();

                if (manager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearManager =
                            (LinearLayoutManager) manager;

                    linearManager.scrollToPositionWithOffset(
                            finalPosition,
                            250
                    );
                }
            });

        } else {
            if (!openedFromSearch && !messagesArrayList.isEmpty()) {
                messageAdpter.post(() ->
                        messageAdpter.scrollToPosition(
                                messagesArrayList.size() - 1
                        )
                );
            }

            if (openedFromSearch) {
                Log.w(
                        TAG,
                        "Target message ID was not found in this chat"
                );
            }
        }
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================
    private void setupSendButton() {
        sendbtn.setOnClickListener(view -> {
            String message = textmsg.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(
                        chatwindo.this,
                        "Enter The Message First",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (senderRoom == null || reciverRoom == null) {
                Toast.makeText(
                        chatwindo.this,
                        "Chat connection error",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            textmsg.setText("");

            Date date = new Date();

            msgModelclass messagess = new msgModelclass(
                    message,
                    SenderUID,
                    date.getTime()
            );

            DatabaseReference senderMessageRef = database.getReference()
                    .child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .push();

            String messageId = senderMessageRef.getKey();

            if (messageId == null) {
                Toast.makeText(
                        chatwindo.this,
                        "Could not create message",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            senderMessageRef.setValue(messagess)
                    .addOnSuccessListener(unused -> {
                        database.getReference()
                                .child("chats")
                                .child(reciverRoom)
                                .child("messages")
                                .child(messageId)
                                .setValue(messagess);

                        Log.d(
                                "SEMANTIC_SEARCH",
                                "INDEXING NEW MESSAGE: " + messageId
                        );

                        indexMessageToBackend(
                                messageId,
                                senderRoom,
                                SenderUID,
                                message,
                                date.getTime()
                        );
                    })
                    .addOnFailureListener(error -> {
                        Toast.makeText(
                                chatwindo.this,
                                "Message failed to send",
                                Toast.LENGTH_SHORT
                        ).show();

                        Log.e(TAG, "Firebase message error", error);
                    });
        });
    }

    // =========================================================
    // INDEX MESSAGE INTO GEMINI + PINECONE
    // =========================================================
    private void indexMessageToBackend(
            String messageId,
            String conversationId,
            String senderId,
            String text,
            long timestamp
    ) {
        new Thread(() -> {
            HttpURLConnection connection = null;

            Log.d("SEMANTIC_SEARCH", "INDEX THREAD STARTED");

            try {
                Log.d(
                        "SEMANTIC_SEARCH",
                        "Sending message to semantic backend..."
                );
                Log.d("SEMANTIC_SEARCH", "BACKEND URL = " + BACKEND_URL);
                Log.d("SEMANTIC_SEARCH", "Message ID = " + messageId);
                Log.d(
                        "SEMANTIC_SEARCH",
                        "Conversation ID = " + conversationId
                );
                Log.d("SEMANTIC_SEARCH", "Sender ID = " + senderId);
                Log.d("SEMANTIC_SEARCH", "Message text = " + text);

                URL url = new URL(BACKEND_URL);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(20000);
                connection.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("messageId", messageId);
                json.put("conversationId", conversationId);
                json.put("senderId", senderId);
                json.put("text", text);
                json.put("timestamp", timestamp);

                String jsonString = json.toString();

                Log.d(
                        "SEMANTIC_SEARCH",
                        "Index request JSON = " + jsonString
                );

                try (OutputStream outputStream =
                             connection.getOutputStream()) {
                    outputStream.write(
                            jsonString.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream =
                        (responseCode >= 200 && responseCode < 300)
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                Log.d(
                        "SEMANTIC_SEARCH",
                        "Index backend response code = " + responseCode
                );
                Log.d(
                        "SEMANTIC_SEARCH",
                        "Index backend response = " + response
                );

                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(
                            "SEMANTIC_SEARCH",
                            "SEMANTIC INDEX SUCCESS for message " + messageId
                    );
                } else {
                    Log.e(
                            "SEMANTIC_SEARCH",
                            "SEMANTIC INDEX FAILED for message " + messageId
                    );

                    runOnUiThread(() -> Toast.makeText(
                            chatwindo.this,
                            "Semantic indexing failed (HTTP "
                                    + responseCode + ")",
                            Toast.LENGTH_LONG
                    ).show());
                }

            } catch (Exception e) {
                Log.e(
                        "SEMANTIC_SEARCH",
                        "SEMANTIC BACKEND CONNECTION ERROR",
                        e
                );
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // =========================================================
    // PREMIUM SUMMARIZE CHAT BUTTON
    // =========================================================
    private void addSummarizeChatButton() {
        ViewGroup parent = (ViewGroup) messageAdpter.getParent();

        if (parent == null) {
            Log.w(
                    TAG,
                    "Cannot add Summarize Chat button: no parent"
            );
            return;
        }

        Button summarizeButton = new Button(this);
        summarizeButton.setText("✦  Summarize Chat");
        summarizeButton.setContentDescription(
                "Generate an AI chat summary"
        );
        summarizeButton.setAllCaps(false);
        summarizeButton.setTextSize(14);
        summarizeButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        summarizeButton.setTextColor(Color.WHITE);
        summarizeButton.setLetterSpacing(0.01f);
        summarizeButton.setGravity(Gravity.CENTER);
        summarizeButton.setPadding(dp(16), dp(9), dp(16), dp(9));

        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();

        background.setShape(
                android.graphics.drawable.GradientDrawable.RECTANGLE
        );
        background.setColor(Color.rgb(100, 75, 210));
        background.setCornerRadius(dp(14));

        summarizeButton.setBackground(background);
        summarizeButton.setElevation(dp(3));

        summarizeButton.setOnClickListener(
                v -> summarizeCurrentChat(summarizeButton)
        );

        int recyclerIndex = parent.indexOfChild(messageAdpter);
        ViewGroup.LayoutParams params;

        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams linearParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            linearParams.setMargins(dp(10), dp(6), dp(10), dp(6));
            params = linearParams;

        } else {
            params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        parent.addView(summarizeButton, Math.max(0, recyclerIndex), params);
    }

    // Hide only an ImageView whose resource ID clearly identifies it as search.
    private void hideSearchIconOnly() {
        View root = findViewById(android.R.id.content);
        hideSearchIconRecursive(root);
    }

    private void hideSearchIconRecursive(View view) {
        if (view == null) {
            return;
        }

        if (view instanceof ImageView && view.getId() != View.NO_ID) {
            try {
                String idName =
                        getResources().getResourceEntryName(view.getId());

                String lowerName =
                        idName.toLowerCase(java.util.Locale.ROOT);

                if (lowerName.contains("search")
                        || lowerName.contains("magnifier")) {
                    view.setVisibility(View.GONE);
                    Log.d(TAG, "Hidden search icon view: " + idName);
                    return;
                }
            } catch (Exception ignored) {
                // Ignore views whose IDs cannot be resolved.
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                hideSearchIconRecursive(group.getChildAt(i));
            }
        }
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    // =========================================================
    // SUMMARIZE CURRENT CHAT
    // =========================================================
    private void summarizeCurrentChat(Button summarizeButton) {
        if (messagesArrayList == null || messagesArrayList.isEmpty()) {
            Toast.makeText(
                    this,
                    "There are no messages to summarize yet",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        ArrayList<msgModelclass> snapshot =
                new ArrayList<>(messagesArrayList);

        int start = Math.max(0, snapshot.size() - 100);
        JSONArray messageJson = new JSONArray();

        for (int i = start; i < snapshot.size(); i++) {
            msgModelclass message = snapshot.get(i);

            if (message == null
                    || message.getMessage() == null
                    || message.getMessage().trim().isEmpty()) {
                continue;
            }

            JSONObject item = new JSONObject();

            try {
                item.put(
                        "senderId",
                        message.getSenderid() == null
                                ? "Unknown sender"
                                : message.getSenderid()
                );

                item.put("text", message.getMessage());
                item.put("timestamp", message.getTimeStamp());
                messageJson.put(item);

            } catch (Exception e) {
                Log.e(
                        TAG,
                        "Could not prepare message for summary",
                        e
                );
            }
        }

        if (messageJson.length() == 0) {
            Toast.makeText(
                    this,
                    "No text messages are available to summarize",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        summarizeButton.setEnabled(false);

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Creating your AI summary...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            HttpURLConnection connection = null;
            String summary = null;
            String errorMessage = null;

            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("messages", messageJson);

                URL url = new URL(SUMMARY_URL);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                byte[] payload = requestBody.toString().getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                );

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                    output.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream stream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder responseText = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    stream,
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    )) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            responseText.append(line);
                        }
                    }
                }

                JSONObject responseJson =
                        responseText.length() > 0
                                ? new JSONObject(responseText.toString())
                                : new JSONObject();

                if (responseCode >= 200
                        && responseCode < 300
                        && responseJson.optBoolean("success")) {

                    summary = responseJson.optString(
                            "summary",
                            ""
                    ).trim();

                    if (summary.isEmpty()) {
                        errorMessage =
                                "The backend returned an empty summary.";
                    }

                } else {
                    errorMessage = responseJson.optString(
                            "message",
                            "Summary request failed (HTTP "
                                    + responseCode + ")"
                    );

                    if (responseCode == 429) {
                        errorMessage =
                                "Gemini API quota/rate limit reached. Please wait and try again.";
                    }

                    Log.e(
                            TAG,
                            "Summarization failed. HTTP "
                                    + responseCode + ": " + responseText
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Chat summarization connection error", e);

                errorMessage =
                        "Could not connect to the backend. Check that server.js is running.";
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            final String finalSummary = summary;
            final String finalError = errorMessage;

            runOnUiThread(() -> {
                if (progress.isShowing()) {
                    progress.dismiss();
                }

                summarizeButton.setEnabled(true);

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalSummary != null && !finalSummary.isEmpty()) {
                    showPremiumSummary(finalSummary);
                } else {
                    Toast.makeText(
                            chatwindo.this,
                            finalError == null
                                    ? "Could not summarize this chat"
                                    : finalError,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }).start();
    }

    // =========================================================
    // PREMIUM AI SUMMARY DIALOG
    // =========================================================
    private void showPremiumSummary(String rawSummary) {
        int padding = dp(20);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, padding, padding, dp(12));
        container.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, dp(16));

        TextView badge = new TextView(this);
        badge.setText("✦  VMESSENGER AI");
        badge.setTextSize(11);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(Color.rgb(105, 76, 220));
        badge.setLetterSpacing(0.08f);

        TextView title = new TextView(this);
        title.setText("Chat Summary");
        title.setTextSize(25);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(27, 29, 43));
        title.setPadding(0, dp(8), 0, dp(4));

        TextView subtitle = new TextView(this);
        subtitle.setText("The important points, organized for you");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.rgb(112, 116, 130));

        header.addView(badge);
        header.addView(title);
        header.addView(subtitle);

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(232, 233, 240));

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                );

        dividerParams.bottomMargin = dp(14);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setVerticalScrollBarEnabled(true);

        TextView summaryText = new TextView(this);
        summaryText.setText(cleanSummaryMarkdown(rawSummary));
        summaryText.setTextSize(15);
        summaryText.setTextColor(Color.rgb(48, 51, 65));
        summaryText.setLineSpacing(dp(4), 1.0f);
        summaryText.setTextIsSelectable(true);
        summaryText.setPadding(dp(2), dp(2), dp(8), dp(12));

        scrollView.addView(summaryText);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1.0f
                );

        TextView footer = new TextView(this);
        footer.setText("✦  Generated by Gemini AI");
        footer.setTextSize(11);
        footer.setTextColor(Color.rgb(135, 137, 151));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(12), 0, dp(4));

        container.addView(header);
        container.addView(divider, dividerParams);
        container.addView(scrollView, scrollParams);
        container.addView(footer);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setNegativeButton(
                        "Close",
                        (d, which) -> d.dismiss()
                )
                .setNeutralButton("Copy Summary", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(view -> {
                        ClipboardManager clipboard =
                                (ClipboardManager) getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                );

                        if (clipboard != null) {
                            clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                            "VMessenger Chat Summary",
                                            summaryText.getText().toString()
                                    )
                            );

                            Toast.makeText(
                                    chatwindo.this,
                                    "Summary copied!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });

            setPremiumDialogSize(dialog);
        });

        dialog.show();
        setPremiumDialogSize(dialog);
    }

    private void setPremiumDialogSize(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            int width = (int) (
                    getResources().getDisplayMetrics().widthPixels * 0.90f
            );

            int height = (int) (
                    getResources().getDisplayMetrics().heightPixels * 0.78f
            );

            dialog.getWindow().setLayout(width, height);
        }
    }

    private String cleanSummaryMarkdown(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text;

        cleaned = cleaned.replaceAll(
                "(?m)^\\s{0,3}#{1,6}\\s*",
                ""
        );

        cleaned = cleaned.replace("**", "");
        cleaned = cleaned.replace("__", "");
        cleaned = cleaned.replace("*", "");
        cleaned = cleaned.replace("_", "");

        cleaned = cleaned.replaceAll(
                "(?m)^\\s*[-+]\\s+",
                "• "
        );

        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return cleaned.trim();
    }

    // =========================================================
    // SEMANTIC SEARCH SUPPORT
    // =========================================================
    public void openSemanticSearch(View view) {
        Intent intent = new Intent(
                chatwindo.this,
                SemanticSearchActivity.class
        );

        startActivity(intent);
    }

    public void openSemanticSearch() {
        Intent intent = new Intent(
                chatwindo.this,
                SemanticSearchActivity.class
        );

        startActivity(intent);
    }
}