package com.example.vmessenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class chatwindo extends AppCompatActivity {
    private static final String TAG = "SEMANTIC_SEARCH";
    // Android Emulator -> Windows PC
    private static final String BACKEND_URL = "http://10.0.2.2:3000/index-message";
    private static final String SEARCH_URL = "http://10.0.2.2:3000/search";
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
    String senderRoom;
    String reciverRoom;
    RecyclerView messageAdpter;
    ArrayList<msgModelclass> messagesArrayList;
    messagesAdpter mmessagesAdpter;

    // Message ID received from Semantic Search
    private String targetMessageId = null;

    // Position of searched message
    private int targetMessagePosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chatwindo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // ----------------------------------------------------
        // RECEIVER DETAILS
        // ----------------------------------------------------

        reciverName = getIntent().getStringExtra("nameeee");
        reciverimg = getIntent().getStringExtra("reciverImg");
        reciverUid = getIntent().getStringExtra("uid");

        // IMPORTANT:
        // SemanticSearchActivity sends this when a result is clicked.
        targetMessageId =
                getIntent().getStringExtra("targetMessageId");

        reciverIImg =
                reciverimg != null ? reciverimg : "";

        messagesArrayList = new ArrayList<>();

        Log.d(
                TAG,
                "Opened chat. Receiver UID = " + reciverUid
        );

        Log.d(
                TAG,
                "Target Message ID = " + targetMessageId
        );

        // ----------------------------------------------------
        // FIND VIEWS
        // ----------------------------------------------------

        sendbtn = findViewById(R.id.sendbtnn);
        textmsg = findViewById(R.id.textmsg);
        reciverNName = findViewById(R.id.recivername);
        profile = findViewById(R.id.profileimgg);
        messageAdpter = findViewById(R.id.msgadpter);

        // ----------------------------------------------------
        // RECYCLER VIEW
        // ----------------------------------------------------

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this);

        linearLayoutManager.setStackFromEnd(true);

        messageAdpter.setLayoutManager(
                linearLayoutManager
        );

        mmessagesAdpter =
                new messagesAdpter(
                        chatwindo.this,
                        messagesArrayList
                );

        messageAdpter.setAdapter(
                mmessagesAdpter
        );

        // ----------------------------------------------------
        // RECEIVER PROFILE
        // ----------------------------------------------------

        if (reciverimg != null &&
                !reciverimg.isEmpty()) {

            Picasso.get()
                    .load(reciverimg)
                    .into(profile);

        } else {

            profile.setImageResource(
                    R.drawable.photocamera
            );
        }

        reciverNName.setText(
                reciverName != null
                        ? reciverName
                        : ""
        );

        // ----------------------------------------------------
        // CURRENT USER
        // ----------------------------------------------------

        SenderUID = firebaseAuth.getUid();

        if (SenderUID == null) {

            Toast.makeText(
                    chatwindo.this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (reciverUid == null ||
                reciverUid.isEmpty()) {

            Toast.makeText(
                    chatwindo.this,
                    "Could not identify chat user",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        senderRoom =
                SenderUID + reciverUid;

        reciverRoom =
                reciverUid + SenderUID;

        // ----------------------------------------------------
        // FIREBASE REFERENCES
        // ----------------------------------------------------

        DatabaseReference reference =
                database.getReference()
                        .child("user")
                        .child(SenderUID);

        DatabaseReference chatreference =
                database.getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages");

        // ----------------------------------------------------
        // LOAD CHAT MESSAGES
        // ----------------------------------------------------

        chatreference.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        messagesArrayList.clear();

                        targetMessagePosition = -1;

                        int currentPosition = 0;

                        for (
                                DataSnapshot dataSnapshot :
                                snapshot.getChildren()
                        ) {

                            msgModelclass messages =
                                    dataSnapshot.getValue(
                                            msgModelclass.class
                                    );

                            if (messages != null) {

                                messagesArrayList.add(
                                        messages
                                );

                                // --------------------------------
                                // FIND SEARCHED MESSAGE
                                // --------------------------------

                                String firebaseMessageId =
                                        dataSnapshot.getKey();

                                if (
                                        targetMessageId != null
                                                &&
                                                firebaseMessageId != null
                                                &&
                                                targetMessageId.equals(
                                                        firebaseMessageId
                                                )
                                ) {

                                    targetMessagePosition =
                                            currentPosition;

                                    Log.d(
                                            TAG,
                                            "FOUND TARGET MESSAGE AT POSITION: "
                                                    + targetMessagePosition
                                    );
                                }

                                currentPosition++;
                            }
                        }

                        mmessagesAdpter.notifyDataSetChanged();

                        // --------------------------------
                        // OPEN AT SEARCHED MESSAGE
                        // --------------------------------

                        if (targetMessagePosition >= 0) {

                            final int finalPosition =
                                    targetMessagePosition;

                            messageAdpter.post(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            RecyclerView.LayoutManager
                                                    layoutManager =
                                                    messageAdpter
                                                            .getLayoutManager();

                                            if (
                                                    layoutManager
                                                            instanceof
                                                            LinearLayoutManager
                                            ) {

                                                LinearLayoutManager
                                                        linearLayoutManager =
                                                        (LinearLayoutManager)
                                                                layoutManager;

                                                linearLayoutManager
                                                        .scrollToPositionWithOffset(
                                                                finalPosition,
                                                                200
                                                        );

                                                Log.d(
                                                        TAG,
                                                        "Scrolled to searched message"
                                                );
                                            }
                                        }
                                    }
                            );

                        } else {

                            // Normal chat opening:
                            // scroll to latest message.

                            if (!messagesArrayList.isEmpty()) {

                                messageAdpter.post(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                messageAdpter
                                                        .scrollToPosition(
                                                                messagesArrayList
                                                                        .size()
                                                                        - 1
                                                        );
                                            }
                                        }
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
                                "Chat messages error",
                                error.toException()
                        );
                    }
                }
        );

        // ----------------------------------------------------
        // LOAD SENDER IMAGE
        // ----------------------------------------------------

        reference.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (
                                snapshot.exists()
                                        &&
                                        snapshot.child(
                                                "profilepic"
                                        ).getValue() != null
                        ) {

                            senderImg =
                                    snapshot.child(
                                                    "profilepic"
                                            )
                                            .getValue()
                                            .toString();

                        } else {

                            senderImg = "";
                        }

                        mmessagesAdpter
                                .notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                TAG,
                                "Profile image error",
                                error.toException()
                        );
                    }
                }
        );

        // ----------------------------------------------------
        // SEND MESSAGE
        // ----------------------------------------------------

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

                        textmsg.setText("");

                        Date date = new Date();

                        msgModelclass messagess =
                                new msgModelclass(
                                        message,
                                        SenderUID,
                                        date.getTime()
                                );

                        if (
                                senderRoom == null
                                        ||
                                        reciverRoom == null
                        ) {

                            Toast.makeText(
                                    chatwindo.this,
                                    "Chat connection error",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // ----------------------------------------
                        // CREATE ONE FIREBASE MESSAGE ID
                        // ----------------------------------------

                        DatabaseReference senderMessageRef =
                                database.getReference()
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

                        // ----------------------------------------
                        // SAVE TO SENDER CHAT
                        // ----------------------------------------

                        senderMessageRef
                                .setValue(messagess)
                                .addOnSuccessListener(
                                        unused -> {

                                            // --------------------------------
                                            // SAVE SAME MESSAGE TO RECEIVER
                                            // --------------------------------

                                            database
                                                    .getReference()
                                                    .child("chats")
                                                    .child(reciverRoom)
                                                    .child("messages")
                                                    .child(messageId)
                                                    .setValue(
                                                            messagess
                                                    );

                                            // --------------------------------
                                            // INDEX MESSAGE
                                            // --------------------------------

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

    // ============================================================
    // INDEX MESSAGE INTO GEMINI + PINECONE
    // ============================================================

    private void indexMessageToBackend(
            String messageId,
            String conversationId,
            String senderId,
            String text,
            long timestamp
    ) {

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        Log.d(
                                TAG,
                                "Sending message to semantic backend..."
                        );

                        URL url =
                                new URL(BACKEND_URL);

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "POST"
                        );

                        connection.setRequestProperty(
                                "Content-Type",
                                "application/json"
                        );

                        connection.setRequestProperty(
                                "Accept",
                                "application/json"
                        );

                        connection.setConnectTimeout(
                                10000
                        );

                        connection.setReadTimeout(
                                15000
                        );

                        connection.setDoOutput(
                                true
                        );

                        // --------------------------------
                        // CREATE JSON
                        // --------------------------------

                        JSONObject json =
                                new JSONObject();

                        json.put(
                                "messageId",
                                messageId
                        );

                        json.put(
                                "conversationId",
                                conversationId
                        );

                        json.put(
                                "senderId",
                                senderId
                        );

                        json.put(
                                "text",
                                text
                        );

                        json.put(
                                "timestamp",
                                timestamp
                        );

                        String jsonString =
                                json.toString();

                        Log.d(
                                TAG,
                                "Request: "
                                        + jsonString
                        );

                        // --------------------------------
                        // SEND REQUEST
                        // --------------------------------

                        OutputStream outputStream =
                                connection
                                        .getOutputStream();

                        outputStream.write(
                                jsonString.getBytes(
                                        "UTF-8"
                                )
                        );

                        outputStream.flush();
                        outputStream.close();

                        // --------------------------------
                        // READ RESPONSE
                        // --------------------------------

                        int responseCode =
                                connection
                                        .getResponseCode();

                        InputStream inputStream;

                        if (
                                responseCode >= 200
                                        &&
                                        responseCode < 300
                        ) {

                            inputStream =
                                    connection
                                            .getInputStream();

                        } else {

                            inputStream =
                                    connection
                                            .getErrorStream();
                        }

                        StringBuilder response =
                                new StringBuilder();

                        if (inputStream != null) {

                            BufferedReader reader =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    inputStream
                                            )
                                    );

                            String line;

                            while (
                                    (line =
                                            reader.readLine())
                                            != null
                            ) {

                                response.append(
                                        line
                                );
                            }

                            reader.close();
                        }

                        Log.d(
                                TAG,
                                "Backend response code: "
                                        + responseCode
                        );

                        Log.d(
                                TAG,
                                "Backend response: "
                                        + response
                        );

                        if (
                                responseCode >= 200
                                        &&
                                        responseCode < 300
                        ) {

                            runOnUiThread(
                                    () -> Log.d(
                                            TAG,
                                            "SEMANTIC SEARCH INDEX SUCCESS"
                                    )
                            );

                        } else {

                            runOnUiThread(
                                    () -> {

                                        Toast.makeText(
                                                chatwindo.this,
                                                "Semantic indexing failed",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        Log.e(
                                                TAG,
                                                "Semantic indexing failed"
                                        );
                                    }
                            );
                        }

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "SEMANTIC BACKEND CONNECTION ERROR",
                                e
                        );

                    } finally {

                        if (connection != null) {

                            connection.disconnect();
                        }
                    }
                }
        ).start();
    }

    // ============================================================
    // SEMANTIC SEARCH
    // ============================================================

    private void performSemanticSearch(
            String query
    ) {

        if (
                query == null
                        ||
                        query.trim().isEmpty()
        ) {

            Toast.makeText(
                    chatwindo.this,
                    "Enter something to search",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        Log.d(
                                TAG,
                                "Starting semantic search..."
                        );

                        URL url =
                                new URL(SEARCH_URL);

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "POST"
                        );

                        connection.setRequestProperty(
                                "Content-Type",
                                "application/json"
                        );

                        connection.setRequestProperty(
                                "Accept",
                                "application/json"
                        );

                        connection.setConnectTimeout(
                                10000
                        );

                        connection.setReadTimeout(
                                15000
                        );

                        connection.setDoOutput(
                                true
                        );

                        JSONObject json =
                                new JSONObject();

                        json.put(
                                "query",
                                query
                        );

                        json.put(
                                "topK",
                                10
                        );

                        if (senderRoom != null) {

                            json.put(
                                    "conversationId",
                                    senderRoom
                            );
                        }

                        String jsonString =
                                json.toString();

                        Log.d(
                                TAG,
                                "Search request: "
                                        + jsonString
                        );

                        OutputStream outputStream =
                                connection
                                        .getOutputStream();

                        outputStream.write(
                                jsonString.getBytes(
                                        "UTF-8"
                                )
                        );

                        outputStream.flush();
                        outputStream.close();

                        int responseCode =
                                connection
                                        .getResponseCode();

                        InputStream inputStream;

                        if (
                                responseCode >= 200
                                        &&
                                        responseCode < 300
                        ) {

                            inputStream =
                                    connection
                                            .getInputStream();

                        } else {

                            inputStream =
                                    connection
                                            .getErrorStream();
                        }

                        StringBuilder response =
                                new StringBuilder();

                        if (inputStream != null) {

                            BufferedReader reader =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    inputStream
                                            )
                                    );

                            String line;

                            while (
                                    (line =
                                            reader.readLine())
                                            != null
                            ) {

                                response.append(
                                        line
                                );
                            }

                            reader.close();
                        }

                        Log.d(
                                TAG,
                                "Search response code: "
                                        + responseCode
                        );

                        Log.d(
                                TAG,
                                "Search response: "
                                        + response
                        );

                        if (
                                responseCode >= 200
                                        &&
                                        responseCode < 300
                        ) {

                            JSONObject result =
                                    new JSONObject(
                                            response.toString()
                                    );

                            JSONArray results =
                                    result.optJSONArray(
                                            "results"
                                    );

                            runOnUiThread(
                                    () -> {

                                        try {

                                            showSearchResults(
                                                    query,
                                                    results
                                            );

                                        } catch (
                                                Exception e
                                        ) {

                                            Log.e(
                                                    TAG,
                                                    "Error showing search results",
                                                    e
                                            );

                                            Toast.makeText(
                                                    chatwindo.this,
                                                    "Could not display results",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            );

                        } else {

                            runOnUiThread(
                                    () -> Toast.makeText(
                                            chatwindo.this,
                                            "Semantic search failed",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "SEMANTIC SEARCH ERROR",
                                e
                        );

                        runOnUiThread(
                                () -> Toast.makeText(
                                        chatwindo.this,
                                        "Unable to connect to semantic backend",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );

                    } finally {

                        if (connection != null) {

                            connection.disconnect();
                        }
                    }
                }
        ).start();
    }

    // ============================================================
    // SHOW SEARCH RESULTS
    // ============================================================

    private void showSearchResults(
            String query,
            JSONArray results
    ) {

        if (
                results == null
                        ||
                        results.length() == 0
        ) {

            new AlertDialog.Builder(
                    chatwindo.this
            )
                    .setTitle(
                            "Semantic Search"
                    )
                    .setMessage(
                            "No related messages found."
                    )
                    .setPositiveButton(
                            "OK",
                            null
                    )
                    .show();

            return;
        }

        LinearLayout container =
                new LinearLayout(
                        chatwindo.this
                );

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        int padding =
                (int)
                        (
                                16 *
                                        getResources()
                                                .getDisplayMetrics()
                                                .density
                        );

        container.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        try {

            for (
                    int i = 0;
                    i < results.length();
                    i++
            ) {

                JSONObject item =
                        results.getJSONObject(i);

                String text =
                        item.optString(
                                "text",
                                ""
                        );

                double score =
                        item.optDouble(
                                "score",
                                0
                        );

                TextView resultText =
                        new TextView(
                                chatwindo.this
                        );

                resultText.setText(
                        text
                                +
                                "\n\nSimilarity: "
                                +
                                String.format(
                                        Locale.getDefault(),
                                        "%.2f",
                                        score
                                )
                );

                resultText.setTextSize(
                        16
                );

                resultText.setPadding(
                        12,
                        18,
                        12,
                        18
                );

                container.addView(
                        resultText
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Result parsing error",
                    e
            );
        }

        AlertDialog dialog =
                new AlertDialog.Builder(
                        chatwindo.this
                )
                        .setTitle(
                                "Search: " + query
                        )
                        .setView(
                                container
                        )
                        .setPositiveButton(
                                "CLOSE",
                                null
                        )
                        .create();

        dialog.show();
    }

    // ============================================================
    // OPEN SEARCH DIALOG
    // ============================================================

    public void openSemanticSearch() {

        final EditText searchInput =
                new EditText(
                        chatwindo.this
                );

        searchInput.setHint(
                "Search your conversation..."
        );

        searchInput.setSingleLine(
                false
        );

        int padding =
                (int)
                        (
                                20 *
                                        getResources()
                                                .getDisplayMetrics()
                                                .density
                        );

        searchInput.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        AlertDialog dialog =
                new AlertDialog.Builder(
                        chatwindo.this
                )
                        .setTitle(
                                "Semantic Search"
                        )
                        .setView(
                                searchInput
                        )
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "SEARCH",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                dialogInterface -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String query =
                                        searchInput
                                                .getText()
                                                .toString()
                                                .trim();

                                if (query.isEmpty()) {

                                    searchInput.setError(
                                            "Enter a search query"
                                    );

                                    return;
                                }

                                dialog.dismiss();

                                performSemanticSearch(
                                        query
                                );
                            }
                    );
                }
        );

        dialog.show();
    }
}
