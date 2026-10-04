package com.example.vmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SemanticSearchActivity extends AppCompatActivity {

    private static final String TAG = "SEMANTIC_SEARCH";

    /*
     * Android Emulator -> Windows localhost
     *
     * Make sure this is running:
     * adb reverse tcp:3000 tcp:3000
     */
    private static final String SEARCH_URL =
            "http://127.0.0.1:3000/search";

    private EditText searchInput;
    private ImageView searchButton;
    private ImageView backButton;
    private ProgressBar progressBar;
    private TextView emptyText;
    private RecyclerView searchRecyclerView;

    private ArrayList<SearchResult> resultsList;
    private SemanticSearchAdapter adapter;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_semantic_search
        );

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        // ---------------------------------------------------------
        // FIND VIEWS
        // ---------------------------------------------------------

        searchInput =
                findViewById(R.id.searchInput);

        searchButton =
                findViewById(R.id.searchButton);

        backButton =
                findViewById(R.id.backButton);

        progressBar =
                findViewById(R.id.searchProgress);

        emptyText =
                findViewById(R.id.emptyText);

        searchRecyclerView =
                findViewById(R.id.searchRecyclerView);


        // ---------------------------------------------------------
        // RESULT LIST
        // ---------------------------------------------------------

        resultsList =
                new ArrayList<>();


        // ---------------------------------------------------------
        // RECYCLER VIEW
        // ---------------------------------------------------------

        searchRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        SemanticSearchActivity.this
                )
        );


        // ---------------------------------------------------------
        // ADAPTER
        // ---------------------------------------------------------

        adapter =
                new SemanticSearchAdapter(
                        SemanticSearchActivity.this,
                        resultsList,
                        new SemanticSearchAdapter.OnResultClickListener() {

                            @Override
                            public void onResultClick(
                                    SearchResult result
                            ) {

                                /*
                                 * IMPORTANT:
                                 *
                                 * Pass the COMPLETE SearchResult.
                                 *
                                 * This fixes the previous
                                 * "cannot find symbol variable result"
                                 * error.
                                 */
                                openChatFromSearchResult(
                                        result
                                );
                            }
                        }
                );


        searchRecyclerView.setAdapter(
                adapter
        );


        // ---------------------------------------------------------
        // INITIAL UI
        // ---------------------------------------------------------

        progressBar.setVisibility(
                View.GONE
        );

        emptyText.setText(
                "Search your memories"
        );

        emptyText.setVisibility(
                View.VISIBLE
        );


        // ---------------------------------------------------------
        // BACK BUTTON
        // ---------------------------------------------------------

        backButton.setOnClickListener(
                v -> finish()
        );


        // ---------------------------------------------------------
        // SEARCH BUTTON
        // ---------------------------------------------------------

        searchButton.setOnClickListener(
                v -> {

                    String query =
                            searchInput
                                    .getText()
                                    .toString()
                                    .trim();

                    if (query.isEmpty()) {

                        Toast.makeText(
                                SemanticSearchActivity.this,
                                "Enter something to search",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    performSemanticSearch(
                            query
                    );
                }
        );
    }


    // =============================================================
    // SEMANTIC SEARCH
    // =============================================================

    private void performSemanticSearch(
            String query
    ) {

        progressBar.setVisibility(
                View.VISIBLE
        );

        emptyText.setVisibility(
                View.GONE
        );

        resultsList.clear();

        adapter.notifyDataSetChanged();


        Log.d(
                TAG,
                "Searching for: " + query
        );


        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        URL url =
                                new URL(
                                        SEARCH_URL
                                );

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "POST"
                        );

                        connection.setConnectTimeout(
                                10000
                        );

                        connection.setReadTimeout(
                                20000
                        );

                        connection.setRequestProperty(
                                "Content-Type",
                                "application/json"
                        );

                        connection.setRequestProperty(
                                "Accept",
                                "application/json"
                        );

                        connection.setDoOutput(
                                true
                        );


                        // -------------------------------------------------
                        // REQUEST JSON
                        // -------------------------------------------------

                        JSONObject request =
                                new JSONObject();

                        request.put(
                                "query",
                                query
                        );

                        request.put(
                                "topK",
                                10
                        );


                        Log.d(
                                TAG,
                                "Search request: "
                                        + request
                        );


                        OutputStream outputStream =
                                connection.getOutputStream();

                        outputStream.write(
                                request
                                        .toString()
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        );

                        outputStream.flush();

                        outputStream.close();


                        // -------------------------------------------------
                        // RESPONSE
                        // -------------------------------------------------

                        int responseCode =
                                connection.getResponseCode();


                        Log.d(
                                TAG,
                                "Search response code: "
                                        + responseCode
                        );


                        InputStream inputStream;

                        if (
                                responseCode >= 200
                                        && responseCode < 300
                        ) {

                            inputStream =
                                    connection.getInputStream();

                        } else {

                            inputStream =
                                    connection.getErrorStream();
                        }


                        if (inputStream == null) {

                            throw new Exception(
                                    "Empty response from backend"
                            );
                        }


                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                inputStream
                                        )
                                );


                        StringBuilder response =
                                new StringBuilder();

                        String line;


                        while (
                                (line = reader.readLine())
                                        != null
                        ) {

                            response.append(
                                    line
                            );
                        }


                        reader.close();


                        String responseText =
                                response.toString();


                        Log.d(
                                TAG,
                                "Search response: "
                                        + responseText
                        );


                        // -------------------------------------------------
                        // HTTP CHECK
                        // -------------------------------------------------

                        if (
                                responseCode < 200
                                        || responseCode >= 300
                        ) {

                            throw new Exception(
                                    "Backend returned HTTP "
                                            + responseCode
                                            + ": "
                                            + responseText
                            );
                        }


                        // -------------------------------------------------
                        // JSON
                        // -------------------------------------------------

                        JSONObject json =
                                new JSONObject(
                                        responseText
                                );


                        boolean success =
                                json.optBoolean(
                                        "success",
                                        false
                                );


                        if (!success) {

                            throw new Exception(
                                    json.optString(
                                            "message",
                                            "Search failed"
                                    )
                            );
                        }


                        // -------------------------------------------------
                        // RESULTS
                        // -------------------------------------------------

                        JSONArray results =
                                json.optJSONArray(
                                        "results"
                                );


                        ArrayList<SearchResult>
                                tempResults =
                                new ArrayList<>();


                        if (results != null) {

                            for (
                                    int i = 0;
                                    i < results.length();
                                    i++
                            ) {

                                JSONObject item =
                                        results.getJSONObject(i);


                                String messageId =
                                        item.optString(
                                                "messageId",
                                                ""
                                        );


                                String text =
                                        item.optString(
                                                "text",
                                                ""
                                        );


                                String senderId =
                                        item.optString(
                                                "senderId",
                                                ""
                                        );


                                String conversationId =
                                        item.optString(
                                                "conversationId",
                                                ""
                                        );


                                double score =
                                        item.optDouble(
                                                "score",
                                                0
                                        );


                                long timestamp =
                                        item.optLong(
                                                "timestamp",
                                                0
                                        );


                                tempResults.add(
                                        new SearchResult(
                                                messageId,
                                                text,
                                                senderId,
                                                conversationId,
                                                score,
                                                timestamp
                                        )
                                );
                            }
                        }


                        // -------------------------------------------------
                        // UPDATE UI
                        // -------------------------------------------------

                        mainHandler.post(
                                () -> {

                                    progressBar.setVisibility(
                                            View.GONE
                                    );


                                    resultsList.clear();


                                    resultsList.addAll(
                                            tempResults
                                    );


                                    adapter.notifyDataSetChanged();


                                    if (
                                            tempResults.isEmpty()
                                    ) {

                                        emptyText.setText(
                                                "No matching memories found"
                                        );

                                        emptyText.setVisibility(
                                                View.VISIBLE
                                        );

                                    } else {

                                        emptyText.setVisibility(
                                                View.GONE
                                        );

                                        Toast.makeText(
                                                SemanticSearchActivity.this,
                                                tempResults.size()
                                                        + " memories found",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }
                        );


                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "SEMANTIC SEARCH ERROR",
                                e
                        );


                        mainHandler.post(
                                () -> {

                                    progressBar.setVisibility(
                                            View.GONE
                                    );


                                    emptyText.setText(
                                            "Search failed"
                                    );


                                    emptyText.setVisibility(
                                            View.VISIBLE
                                    );


                                    Toast.makeText(
                                            SemanticSearchActivity.this,
                                            "Search failed: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );


                    } finally {

                        if (connection != null) {

                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }


    // =============================================================
    // OPEN CHAT FROM SEARCH RESULT
    // =============================================================

    private void openChatFromSearchResult(
            SearchResult result
    ) {

        if (result == null) {

            Toast.makeText(
                    this,
                    "Search result is empty",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        String conversationId =
                result.getConversationId();


        String senderId =
                result.getSenderId();


        String messageId =
                result.getMessageId();


        String currentUserId =
                FirebaseAuth
                        .getInstance()
                        .getUid();


        Log.d(
                TAG,
                "================================"
        );

        Log.d(
                TAG,
                "SEARCH RESULT CLICKED"
        );

        Log.d(
                TAG,
                "Message ID = "
                        + messageId
        );

        Log.d(
                TAG,
                "Message = "
                        + result.getText()
        );

        Log.d(
                TAG,
                "Sender ID = "
                        + senderId
        );

        Log.d(
                TAG,
                "Conversation ID = "
                        + conversationId
        );

        Log.d(
                TAG,
                "Current User ID = "
                        + currentUserId
        );


        // ---------------------------------------------------------
        // CHECK LOGIN
        // ---------------------------------------------------------

        if (
                currentUserId == null
                        || currentUserId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "User is not logged in",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // ---------------------------------------------------------
        // CHECK DATA
        // ---------------------------------------------------------

        if (
                conversationId == null
                        || conversationId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Conversation ID missing",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (
                senderId == null
                        || senderId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Sender ID missing",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (
                messageId == null
                        || messageId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Message ID missing",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // ---------------------------------------------------------
        // FIND RECEIVER
        // ---------------------------------------------------------

        String receiverUid = null;


        /*
         * CASE 1
         *
         * Message was sent by the OTHER USER.
         *
         * Therefore senderId = receiver.
         */

        if (
                !senderId.equals(
                        currentUserId
                )
        ) {

            receiverUid =
                    senderId;

            Log.d(
                    TAG,
                    "Receiver found from senderId = "
                            + receiverUid
            );
        }


        /*
         * CASE 2
         *
         * Message was sent by CURRENT USER.
         *
         * Conversation ID is:
         *
         * currentUserId + receiverUid
         */

        else if (
                conversationId.startsWith(
                        currentUserId
                )
        ) {

            receiverUid =
                    conversationId.substring(
                            currentUserId.length()
                    );


            Log.d(
                    TAG,
                    "Receiver extracted from conversationId = "
                            + receiverUid
            );
        }


        // ---------------------------------------------------------
        // SAFETY CHECK
        // ---------------------------------------------------------

        if (
                receiverUid == null
                        || receiverUid.isEmpty()
                        || receiverUid.equals(
                        currentUserId
                )
        ) {

            Log.e(
                    TAG,
                    "FAILED TO IDENTIFY RECEIVER"
            );

            Log.e(
                    TAG,
                    "currentUserId = "
                            + currentUserId
            );

            Log.e(
                    TAG,
                    "senderId = "
                            + senderId
            );

            Log.e(
                    TAG,
                    "conversationId = "
                            + conversationId
            );


            Toast.makeText(
                    this,
                    "Could not identify chat user",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        Log.d(
                TAG,
                "FINAL RECEIVER UID = "
                        + receiverUid
        );


        /*
         * IMPORTANT FIX:
         *
         * Pass BOTH receiverUid AND result.
         *
         * Previously the method only received receiverUid,
         * but inside onDataChange() it tried to use "result".
         *
         * That caused:
         *
         * cannot find symbol variable result
         */

        loadReceiverAndOpenChat(
                receiverUid,
                result
        );
    }


    // =============================================================
    // LOAD RECEIVER PROFILE AND OPEN CHAT
    // =============================================================

    private void loadReceiverAndOpenChat(
            String receiverUid,
            SearchResult result
    ) {

        DatabaseReference userReference =
                FirebaseDatabase
                        .getInstance()
                        .getReference()
                        .child("user")
                        .child(receiverUid);


        userReference.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        if (!snapshot.exists()) {

                            Toast.makeText(
                                    SemanticSearchActivity.this,
                                    "Receiver user not found",
                                    Toast.LENGTH_LONG
                            ).show();

                            Log.e(
                                    TAG,
                                    "User not found: "
                                            + receiverUid
                            );

                            return;
                        }


                        // -------------------------------------------------
                        // NAME
                        // -------------------------------------------------

                        String name = "";


                        if (
                                snapshot
                                        .child("name")
                                        .exists()
                        ) {

                            Object value =
                                    snapshot
                                            .child("name")
                                            .getValue();

                            if (value != null) {

                                name =
                                        value.toString();
                            }
                        }


                        // Fallback
                        if (
                                name.isEmpty()
                                        && snapshot
                                        .child("username")
                                        .exists()
                        ) {

                            Object value =
                                    snapshot
                                            .child("username")
                                            .getValue();

                            if (value != null) {

                                name =
                                        value.toString();
                            }
                        }


                        // -------------------------------------------------
                        // PROFILE IMAGE
                        // -------------------------------------------------

                        String profileImage = "";


                        if (
                                snapshot
                                        .child("profilepic")
                                        .exists()
                        ) {

                            Object value =
                                    snapshot
                                            .child("profilepic")
                                            .getValue();

                            if (value != null) {

                                profileImage =
                                        value.toString();
                            }
                        }


                        Log.d(
                                TAG,
                                "Opening chat"
                                        + " UID="
                                        + receiverUid
                                        + " NAME="
                                        + name
                        );


                        // -------------------------------------------------
                        // OPEN CHAT
                        // -------------------------------------------------

                        Intent intent =
                                new Intent(
                                        SemanticSearchActivity.this,
                                        chatwindo.class
                                );


                        /*
                         * These are the keys already used
                         * by chatwindo.java.
                         */

                        intent.putExtra(
                                "uid",
                                receiverUid
                        );


                        intent.putExtra(
                                "nameeee",
                                name
                        );


                        intent.putExtra(
                                "reciverImg",
                                profileImage
                        );


                        /*
                         * This is the IMPORTANT part.
                         *
                         * chatwindo can use this ID to locate
                         * the exact searched message.
                         */

                        intent.putExtra(
                                "targetMessageId",
                                result.getMessageId()
                        );


                        startActivity(
                                intent
                        );
                    }


                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {

                        Log.e(
                                TAG,
                                "Firebase user lookup failed",
                                error.toException()
                        );


                        Toast.makeText(
                                SemanticSearchActivity.this,
                                "Could not load chat user",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}