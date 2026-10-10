package com.example.vmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

    // Android Emulator -> Windows PC
    private static final String BACKEND_URL = "http://127.0.0.1:3000/index-message";

    // ---------------------------------------------------------
    // USER DETAILS
    // ---------------------------------------------------------
    String reciverimg;
    String reciverUid;
    String reciverName;
    String SenderUID;
    CircleImageView profile;
    TextView reciverNName;

    // ---------------------------------------------------------
    // FIREBASE
    // ---------------------------------------------------------
    FirebaseDatabase database;
    FirebaseAuth firebaseAuth;

    // ---------------------------------------------------------
    // PROFILE IMAGES
    // ---------------------------------------------------------
    public static String senderImg = "";
    public static String reciverIImg = "";

    // ---------------------------------------------------------
    // MESSAGE UI
    // ---------------------------------------------------------
    CardView sendbtn;
    EditText textmsg;
    RecyclerView messageAdpter;
    ArrayList<msgModelclass> messagesArrayList;
    messagesAdpter mmessagesAdpter;

    // ---------------------------------------------------------
    // CHAT ROOMS
    // ---------------------------------------------------------
    String senderRoom;
    String reciverRoom;

    // ---------------------------------------------------------
    // SEMANTIC SEARCH TARGET
    // ---------------------------------------------------------
    private String targetMessageId = null;
    private String targetMessageText = null;
    private String targetMessageSenderId = null;
    private long targetMessageTimestamp = 0;
    private int targetMessagePosition = -1;
    private boolean openedFromSearch = false;

    // =========================================================
    // ON CREATE
    // =========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatwindo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // -----------------------------------------------------
        // FIREBASE INITIALIZATION
        // -----------------------------------------------------
        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // -----------------------------------------------------
        // CURRENT USER
        // -----------------------------------------------------
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

        // -----------------------------------------------------
        // READ INTENT VALUES
        // -----------------------------------------------------
        reciverUid = getIntent().getStringExtra("uid");
        reciverName = getIntent().getStringExtra("nameeee");
        reciverimg = getIntent().getStringExtra("reciverImg");

        targetMessageId = getIntent().getStringExtra("targetMessageId");
        targetMessageText = getIntent().getStringExtra("targetMessageText");
        targetMessageSenderId = getIntent().getStringExtra("targetMessageSenderId");
        targetMessageTimestamp = getIntent().getLongExtra("targetMessageTimestamp", 0);

        openedFromSearch = targetMessageId != null && !targetMessageId.isEmpty();

        reciverIImg = reciverimg != null ? reciverimg : "";

        messagesArrayList = new ArrayList<>();

        // -----------------------------------------------------
        // LOG SEARCH INFORMATION
        // -----------------------------------------------------
        Log.d(TAG, "================================");
        Log.d(TAG, "CHAT WINDOW OPENED");
        Log.d(TAG, "Current User = " + SenderUID);
        Log.d(TAG, "Intent Receiver = " + reciverUid);
        Log.d(TAG, "Target Message ID = " + targetMessageId);
        Log.d(TAG, "Target Message Text = " + targetMessageText);
        Log.d(TAG, "Target Sender = " + targetMessageSenderId);

        // -----------------------------------------------------
        // FIND VIEWS
        // -----------------------------------------------------
        sendbtn = findViewById(R.id.sendbtnn);

        textmsg = findViewById(R.id.textmsg);

        reciverNName = findViewById(R.id.recivername);

        profile = findViewById(R.id.profileimgg);

        messageAdpter = findViewById(R.id.msgadpter);

        // -----------------------------------------------------
        // RECYCLER VIEW
        // -----------------------------------------------------
        LinearLayoutManager layoutManager = new LinearLayoutManager(chatwindo.this);

        layoutManager.setStackFromEnd(true);messageAdpter.setLayoutManager(layoutManager);

        mmessagesAdpter = new messagesAdpter(chatwindo.this, messagesArrayList);

        messageAdpter.setAdapter(mmessagesAdpter);

        // -----------------------------------------------------
        // CURRENT USER VALIDATION
        // -----------------------------------------------------
        if (reciverUid == null || reciverUid.trim().isEmpty() || reciverUid.equals(SenderUID)) {

            /*
             * This can happen when an old search result
             * contains the current user's senderId.
             *
             * Do NOT open current-user -> current-user chat.
             *
             * Try to recover the correct receiver from
             * conversation information if available.
             */

            String conversationId = getIntent().getStringExtra("conversationId");

            String recoveredUid = findOtherUserFromConversation(conversationId, SenderUID);

            if (recoveredUid != null && !recoveredUid.isEmpty() && !recoveredUid.equals(SenderUID)) {

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

        // -----------------------------------------------------
        // CREATE CHAT ROOM IDS
        // -----------------------------------------------------
        senderRoom = SenderUID + reciverUid;
        reciverRoom = reciverUid + SenderUID;
        Log.d(TAG, "Sender Room = " + senderRoom);
        Log.d(TAG, "Receiver Room = " + reciverRoom);

        // LOAD RECEIVER PROFILE
        loadReceiverProfile();

        // LOAD CURRENT USER PROFILE
        loadSenderProfile();

        // LOAD CHAT
        loadChatMessages();

        // SEND MESSAGE
        setupSendButton();
    }

    // =========================================================
    // FIND OTHER USER FROM CONVERSATION ID
    // =========================================================
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

        /*
         * Your app creates conversation IDs as:
         *
         * currentUser + receiver
         *
         * or
         *
         * receiver + currentUser
         */

        if (conversationId.startsWith(currentUserId)) {

            String otherUid =
                    conversationId.substring(
                            currentUserId.length()
                    );

            if (!otherUid.isEmpty()) {
                return otherUid;
            }
        }

        if (conversationId.endsWith(currentUserId)) {

            String otherUid =
                    conversationId.substring(
                            0,
                            conversationId.length()
                                    - currentUserId.length()
                    );

            if (!otherUid.isEmpty()) {
                return otherUid;
            }
        }

        return null;
    }

    // =========================================================
    // LOAD RECEIVER PROFILE
    // =========================================================

    private void loadReceiverProfile() {

        DatabaseReference userReference =
                database.getReference()
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
                                    "Receiver profile not found: "
                                            + reciverUid
                            );

                            reciverNName.setText(
                                    reciverName != null
                                            ? reciverName
                                            : ""
                            );

                            if (reciverimg == null
                                    || reciverimg.isEmpty()) {

                                profile.setImageResource(
                                        R.drawable.photocamera
                                );
                            }

                            return;
                        }

                        // -------------------------------------------------
                        // NAME
                        // -------------------------------------------------

                        String name = "";

                        Object nameValue =
                                snapshot.child("name")
                                        .getValue();

                        if (nameValue != null) {

                            name =
                                    nameValue.toString();
                        }

                        if (name.isEmpty()) {

                            Object usernameValue =
                                    snapshot.child("username")
                                            .getValue();

                            if (usernameValue != null) {

                                name =
                                        usernameValue.toString();
                            }
                        }

                        if (name.isEmpty()
                                && reciverName != null) {

                            name = reciverName;
                        }

                        reciverName = name;

                        reciverNName.setText(
                                name
                        );

                        // -------------------------------------------------
                        // PROFILE IMAGE
                        // -------------------------------------------------

                        String image = "";

                        Object imageValue =
                                snapshot.child("profilepic")
                                        .getValue();

                        if (imageValue != null) {

                            image =
                                    imageValue.toString();
                        }

                        if (!image.isEmpty()) {

                            reciverimg = image;

                            reciverIImg = image;

                            Picasso.get()
                                    .load(image)
                                    .into(profile);

                        } else {

                            if (reciverimg != null
                                    && !reciverimg.isEmpty()) {

                                Picasso.get()
                                        .load(reciverimg)
                                        .into(profile);

                            } else {

                                profile.setImageResource(
                                        R.drawable.photocamera
                                );
                            }
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

    // =========================================================
    // LOAD SENDER PROFILE
    // =========================================================

    private void loadSenderProfile() {

        DatabaseReference reference =
                database.getReference()
                        .child("user")
                        .child(SenderUID);

        reference.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (snapshot.exists()
                                && snapshot.child(
                                "profilepic"
                        ).getValue() != null) {

                            senderImg =
                                    snapshot.child(
                                                    "profilepic"
                                            )
                                            .getValue()
                                            .toString();

                        } else {

                            senderImg = "";
                        }

                        if (mmessagesAdpter != null) {

                            mmessagesAdpter
                                    .notifyDataSetChanged();
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

    // =========================================================
    // LOAD CHAT MESSAGES
    // =========================================================

    private void loadChatMessages() {

        DatabaseReference primaryReference =
                database.getReference()
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

                            displayMessages(
                                    snapshot
                            );

                        } else {

                            /*
                             * Primary room is empty.
                             * Try reverse room.
                             */
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

    // =========================================================
    // LOAD REVERSE CHAT ROOM
    // =========================================================

    private void loadReverseRoom() {

        DatabaseReference reverseReference =
                database.getReference()
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

                            displayMessages(
                                    snapshot
                            );

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

    // =========================================================
    // DISPLAY MESSAGES + FIND EXACT SEARCH RESULT
    // =========================================================
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

            // Add message to the chat list
            messagesArrayList.add(message);

            // Firebase key is the message ID indexed by semantic search
            String firebaseMessageId = dataSnapshot.getKey();

            // Find the exact message selected in Semantic Search
            if (targetMessageId != null
                    && firebaseMessageId != null
                    && targetMessageId.equals(firebaseMessageId)) {

                targetMessagePosition = position;

                Log.d(TAG, "================================");
                Log.d(TAG, "TARGET MESSAGE FOUND");
                Log.d(TAG, "Message ID = " + firebaseMessageId);
                Log.d(TAG, "Message position = " + targetMessagePosition);
                Log.d(TAG, "Message text = " + targetMessageText);
                Log.d(TAG, "================================");
            }

            position++;
        }

        // Refresh the chat messages
        mmessagesAdpter.notifyDataSetChanged();

        // Scroll to and highlight the selected search result
        if (targetMessagePosition >= 0) {

            final int finalPosition = targetMessagePosition;

            // Highlight the exact message bubble
            mmessagesAdpter.setHighlightedPosition(finalPosition);

            // Scroll after RecyclerView updates
            messageAdpter.post(new Runnable() {
                @Override
                public void run() {

                    RecyclerView.LayoutManager manager =
                            messageAdpter.getLayoutManager();

                    if (manager instanceof LinearLayoutManager) {

                        LinearLayoutManager linearManager =
                                (LinearLayoutManager) manager;

                        linearManager.scrollToPositionWithOffset(
                                finalPosition,
                                250
                        );

                        Log.d(
                                TAG,
                                "SCROLLED TO AND HIGHLIGHTED SEARCH RESULT"
                        );
                    }
                }
            });

        } else {

            // For normal chat opening, show the latest message
            if (!openedFromSearch && !messagesArrayList.isEmpty()) {

                messageAdpter.post(new Runnable() {
                    @Override
                    public void run() {
                        messageAdpter.scrollToPosition(
                                messagesArrayList.size() - 1
                        );
                    }
                });
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

        sendbtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        String message =
                                textmsg.getText()
                                        .toString()
                                        .trim();

                        if (message.isEmpty()) {

                            Toast.makeText(
                                    chatwindo.this,
                                    "Enter The Message First",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        if (senderRoom == null
                                || reciverRoom == null) {

                            Toast.makeText(
                                    chatwindo.this,
                                    "Chat connection error",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        textmsg.setText("");

                        Date date =
                                new Date();

                        msgModelclass messagess =
                                new msgModelclass(
                                        message,
                                        SenderUID,
                                        date.getTime()
                                );

                        // -------------------------------------------------
                        // CREATE ONE FIREBASE MESSAGE ID
                        // -------------------------------------------------

                        DatabaseReference
                                senderMessageRef =
                                database
                                        .getReference()
                                        .child("chats")
                                        .child(senderRoom)
                                        .child("messages")
                                        .push();

                        String messageId =
                                senderMessageRef.getKey();

                        if (messageId == null) {

                            Toast.makeText(
                                    chatwindo.this,
                                    "Could not create message",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // -------------------------------------------------
                        // SAVE TO SENDER ROOM
                        // -------------------------------------------------

                        senderMessageRef
                                .setValue(messagess)
                                .addOnSuccessListener(unused -> {

                                            // ---------------------------------
                                            // SAVE SAME MESSAGE TO RECEIVER
                                            // ---------------------------------

                                            database
                                                    .getReference()
                                                    .child("chats")
                                                    .child(reciverRoom)
                                                    .child("messages")
                                                    .child(messageId)
                                                    .setValue(
                                                            messagess
                                                    );

                                            // ---------------------------------
                                            // INDEX MESSAGE
                                            // ---------------------------------

                                            Log.d("SEMANTIC_SEARCH", "INDEXING NEW MESSAGE: " + messageId);

                                            indexMessageToBackend(
                                                    messageId,
                                                    senderRoom,
                                                    SenderUID,
                                                    message,
                                                    date.getTime()
                                            );
                                        }
                                )
                                .addOnFailureListener(
                                        error -> {

                                            Toast.makeText(
                                                    chatwindo.this,
                                                    "Message failed to send",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            Log.e(
                                                    TAG,
                                                    "Firebase message error",
                                                    error
                                            );
                                        }
                                );
                    }
                }
        );
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

            // Use SEMANTIC_SEARCH so these logs appear with that Logcat filter.
            Log.d("SEMANTIC_SEARCH", "INDEX THREAD STARTED");

            try {
                Log.d("SEMANTIC_SEARCH", "Sending message to semantic backend...");
                Log.d("SEMANTIC_SEARCH", "BACKEND URL = " + BACKEND_URL);
                Log.d("SEMANTIC_SEARCH", "Message ID = " + messageId);
                Log.d("SEMANTIC_SEARCH", "Conversation ID = " + conversationId);
                Log.d("SEMANTIC_SEARCH", "Sender ID = " + senderId);
                Log.d("SEMANTIC_SEARCH", "Message text = " + text);

                URL url = new URL(BACKEND_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
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
                Log.d("SEMANTIC_SEARCH", "Index request JSON = " + jsonString);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();
                InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                Log.d("SEMANTIC_SEARCH", "Index backend response code = " + responseCode);
                Log.d("SEMANTIC_SEARCH", "Index backend response = " + response);

                if (responseCode >= 200 && responseCode < 300) {
                    Log.d("SEMANTIC_SEARCH", "SEMANTIC INDEX SUCCESS for message " + messageId);
                } else {
                    Log.e("SEMANTIC_SEARCH", "SEMANTIC INDEX FAILED for message " + messageId);
                    runOnUiThread(() -> Toast.makeText(
                            chatwindo.this,
                            "Semantic indexing failed (HTTP " + responseCode + ")",
                            Toast.LENGTH_LONG
                    ).show());
                }

            } catch (Exception e) {
                Log.e("SEMANTIC_SEARCH", "SEMANTIC BACKEND CONNECTION ERROR", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // =========================================================
    // SEARCH BUTTON SUPPORT
    // =========================================================

    public void openSemanticSearch(
            View view
    ) {

        Intent intent =
                new Intent(
                        chatwindo.this,
                        SemanticSearchActivity.class
                );

        startActivity(intent);
    }

    // =========================================================
    // OPTIONAL NO-ARG XML onClick SUPPORT
    // =========================================================

    public void openSemanticSearch() {

        Intent intent =
                new Intent(
                        chatwindo.this,
                        SemanticSearchActivity.class
                );

        startActivity(intent);
    }
}