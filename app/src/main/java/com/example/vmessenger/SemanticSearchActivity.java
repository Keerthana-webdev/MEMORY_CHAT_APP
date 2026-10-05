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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

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
     * Android Emulator -> Windows PC
     *
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

                                openChatFromSearchResult(
                                        result
                                );
                            }
                        }
                );

        searchRecyclerView.setAdapter(adapter);

        // ---------------------------------------------------------
        // INITIAL UI
        // ---------------------------------------------------------

        progressBar.setVisibility(View.GONE);

        emptyText.setText(
                "Search your memories"
        );

        emptyText.setVisibility(View.VISIBLE);

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

                    performSemanticSearch(query);
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
                        // REQUEST
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
                                        &&
                                        responseCode < 300
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
                                (line =
                                        reader.readLine())
                                        != null
                        ) {

                            response.append(line);
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
                                        ||
                                        responseCode >= 300
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
    // OPEN EXACT SEARCH RESULT
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

        String currentUserId =
                FirebaseAuth
                        .getInstance()
                        .getUid();

        if (
                currentUserId == null
                        ||
                        currentUserId.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "User is not logged in",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String senderId =
                result.getSenderId();

        String conversationId =
                result.getConversationId();

        String messageId =
                result.getMessageId();

        String messageText =
                result.getText();

        long timestamp =
                result.getTimestamp();

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
                "Message ID = " + messageId
        );

        Log.d(
                TAG,
                "Message Text = " + messageText
        );

        Log.d(
                TAG,
                "Sender ID = " + senderId
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
        // FIND RECEIVER
        // ---------------------------------------------------------

        String receiverUid = null;

        /*
         * CASE 1:
         *
         * The searched message was sent by
         * the other user.
         *
         * Therefore senderId is the receiver.
         */

        if (
                senderId != null
                        &&
                        !senderId.isEmpty()
                        &&
                        !senderId.equals(
                                currentUserId
                        )
        ) {

            receiverUid =
                    senderId;
        }

        /*
         * CASE 2:
         *
         * The searched message was sent by
         * the current user.
         *
         * conversationId was stored as:
         *
         * currentUserId + receiverUid
         */

        else if (
                senderId != null
                        &&
                        senderId.equals(
                                currentUserId
                        )
                        &&
                        conversationId != null
                        &&
                        conversationId.startsWith(
                                currentUserId
                        )
        ) {

            receiverUid =
                    conversationId.substring(
                            currentUserId.length()
                    );
        }

        // ---------------------------------------------------------
        // SAFETY CHECK
        // ---------------------------------------------------------

        if (
                receiverUid == null
                        ||
                        receiverUid.isEmpty()
                        ||
                        receiverUid.equals(
                                currentUserId
                        )
        ) {

            Log.e(
                    TAG,
                    "Could not determine receiver"
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

        // ---------------------------------------------------------
        // OPEN CHAT DIRECTLY
        //
        // IMPORTANT:
        // Do NOT query user/{uid} here.
        // chatwindo will handle the profile.
        // ---------------------------------------------------------

        Intent intent =
                new Intent(
                        SemanticSearchActivity.this,
                        chatwindo.class
                );

        intent.putExtra(
                "uid",
                receiverUid
        );

        intent.putExtra(
                "nameeee",
                ""
        );

        intent.putExtra(
                "reciverImg",
                ""
        );

        // Exact message information
        intent.putExtra(
                "targetMessageId",
                messageId
        );

        intent.putExtra(
                "targetMessageText",
                messageText
        );

        intent.putExtra(
                "targetMessageSenderId",
                senderId
        );

        intent.putExtra(
                "targetMessageTimestamp",
                timestamp
        );

        startActivity(intent);
    }
}