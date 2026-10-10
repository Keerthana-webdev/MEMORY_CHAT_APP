package com.example.vmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SemanticSearchActivity extends AppCompatActivity {
    private static final String TAG = "SEMANTIC_SEARCH";
    private static final String SEARCH_URL = "http://127.0.0.1:3000/search";

    private static final String INDEX_URL = "http://127.0.0.1:3000/index-message";
    private EditText searchInput;
    private ImageView searchButton;
    private ImageView backButton;
    private ProgressBar progressBar;
    private TextView emptyText;
    private RecyclerView searchRecyclerView;
    private Button syncExistingMessagesButton;

    // AI answer views from activity_semantic_search.xml
    private View aiAnswerCard;
    private TextView aiAnswerText;
    private ArrayList<SearchResult> resultsList;
    private SemanticSearchAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_semantic_search);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.searchProgress);
        emptyText = findViewById(R.id.emptyText);
        searchRecyclerView = findViewById(R.id.searchRecyclerView);
        syncExistingMessagesButton = findViewById(R.id.syncExistingMessagesButton);

        aiAnswerCard = findViewById(R.id.aiAnswerCard);
        aiAnswerText = findViewById(R.id.aiAnswerText);

        aiAnswerCard.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        resultsList = new ArrayList<>();

        searchRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new SemanticSearchAdapter(
                this,
                resultsList,
                result -> openChatFromSearchResult(result)
        );

        searchRecyclerView.setAdapter(adapter);

        emptyText.setText("Search your memories");
        emptyText.setVisibility(View.VISIBLE);

        backButton.setOnClickListener(v -> finish());

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();

            if (query.isEmpty()) {
                Toast.makeText(
                        SemanticSearchActivity.this,
                        "Enter something to search",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            performSemanticSearch(query);
        });

        searchInput.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    String query = searchInput.getText().toString().trim();

                    if (!query.isEmpty()) {
                        performSemanticSearch(query);
                        return true;
                    }

                    Toast.makeText(
                            SemanticSearchActivity.this,
                            "Enter something to search",
                            Toast.LENGTH_SHORT
                    ).show();

                    return false;
                }
        );

        syncExistingMessagesButton.setOnClickListener(
                v -> syncExistingMessages()
        );
    }

    // =========================================================
    // SEMANTIC SEARCH + AI ANSWER
    // =========================================================
    private void performSemanticSearch(String query) {

        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        aiAnswerCard.setVisibility(View.GONE);
        aiAnswerText.setText("");

        resultsList.clear();
        adapter.notifyDataSetChanged();

        searchButton.setEnabled(false);

        Log.d(TAG, "Searching for: " + query);

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(SEARCH_URL);
                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(90000);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setDoOutput(true);

                JSONObject request = new JSONObject();
                request.put("query", query);
                request.put("topK", 10);

                Log.d(TAG, "Search request = " + request);

                try (OutputStream output = connection.getOutputStream()) {

                    output.write(
                            request.toString().getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );
                    output.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream stream =
                        (responseCode >= 200 && responseCode < 300)
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 stream,
                                                 StandardCharsets.UTF_8
                                         )
                                 )) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                Log.d(TAG, "Search response code = " + responseCode);
                Log.d(TAG, "Search response = " + response);

                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("Backend returned HTTP " + responseCode + ": " + response);
                }

                JSONObject json = new JSONObject(response.toString());

                if (!json.optBoolean("success", false)) {
                    throw new IOException(
                            json.optString(
                                    "message",
                                    "Search failed"
                            )
                    );
                }

                // Read the AI answer from server.js.
                String aiAnswer = json.optString("answer", "").trim();

                boolean answerAvailable = json.optBoolean("answerAvailable", false) && !aiAnswer.isEmpty();

                // Also support an answer without the Boolean field.
                if (!aiAnswer.isEmpty()) {
                    answerAvailable = true;
                }

                Log.d(TAG, "AI answer available = " + answerAvailable);
                Log.d(TAG, "AI answer = " + aiAnswer);

                JSONArray jsonResults = json.optJSONArray("results");

                ArrayList<SearchResult> tempResults = new ArrayList<>();

                if (jsonResults != null) {
                    for (int i = 0; i < jsonResults.length(); i++) {

                        JSONObject item = jsonResults.getJSONObject(i);
                        String messageId = item.optString("messageId", "");
                        String text = item.optString("text", "");
                        String senderId = item.optString("senderId", "");
                        String conversationId = item.optString("conversationId", "");

                        double score = item.optDouble("score", 0);
                        long timestamp = item.optLong("timestamp", 0);

                        if (messageId.isEmpty()
                                || text.trim().isEmpty()
                                || conversationId.isEmpty()) {

                            Log.w(TAG, "Skipping incomplete result at " + i);
                            continue;
                        }

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

                Log.d(TAG, "Parsed result count = " + tempResults.size());

                final String finalAnswer = aiAnswer;
                final boolean finalAnswerAvailable = answerAvailable;
                final ArrayList<SearchResult> finalResults = tempResults;

                mainHandler.post(() -> {

                    progressBar.setVisibility(View.GONE);
                    searchButton.setEnabled(true);

                    // Display the AI answer.
                    if (finalAnswerAvailable) {
                        aiAnswerText.setText(finalAnswer);
                        aiAnswerCard.setVisibility(View.VISIBLE);
                    } else {
                        aiAnswerText.setText("");
                        aiAnswerCard.setVisibility(View.GONE);
                    }

                    // Display the matching chat messages.
                    resultsList.clear();
                    resultsList.addAll(finalResults);
                    adapter.notifyDataSetChanged();

                    if (finalResults.isEmpty()) {
                        emptyText.setText("No matching messages found. Try another search.");
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        emptyText.setVisibility(View.GONE);

                        Toast.makeText(
                                SemanticSearchActivity.this,
                                finalResults.size() + " memories found",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    Log.d(TAG, "Search UI updated successfully");
                });

            } catch (Exception e) {

                Log.e(TAG, "SEMANTIC SEARCH ERROR", e);

                mainHandler.post(() -> {

                    progressBar.setVisibility(View.GONE);
                    searchButton.setEnabled(true);

                    aiAnswerCard.setVisibility(View.GONE);
                    aiAnswerText.setText("");

                    emptyText.setText("Search failed. Please try again.");
                    emptyText.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            SemanticSearchActivity.this,
                            "Search failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // =========================================================
    // FIREBASE MESSAGE SYNC MODEL
    // =========================================================
    private static class ExistingMessage {
        String messageId;
        String conversationId;
        String senderId;
        String text;
        long timestamp;

        ExistingMessage(
                String messageId,
                String conversationId,
                String senderId,
                String text,
                long timestamp
        ) {
            this.messageId = messageId;
            this.conversationId = conversationId;
            this.senderId = senderId;
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    // =========================================================
    // CANONICAL CONVERSATION ID
    // =========================================================
    private String getCanonicalConversationId(
            String roomId,
            String senderId
    ) {
        if (roomId == null || senderId == null) {
            return null;
        }

        if (roomId.startsWith(senderId)
                && roomId.length() > senderId.length()) {
            return roomId;
        }

        if (roomId.endsWith(senderId) && roomId.length() > senderId.length()) {
            String otherUserId = roomId.substring(0,
                   roomId.length() - senderId.length()
            );

            return senderId + otherUserId;
        }

        return null;
    }

    // =========================================================
    // SYNC EXISTING FIREBASE MESSAGES
    // =========================================================
    private void syncExistingMessages() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in first",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        syncExistingMessagesButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        emptyText.setText("Reading existing messages...");
        emptyText.setVisibility(View.VISIBLE);

        FirebaseDatabase.getInstance()
                .getReference()
                .child("chats")
                .get()
                .addOnSuccessListener(snapshot -> new Thread(() -> {

                    ArrayList<ExistingMessage> messages = new ArrayList<>();

                    Set<String> seenMessages = new HashSet<>();

                    for (DataSnapshot room : snapshot.getChildren()) {

                        String roomId = room.getKey();
                        DataSnapshot roomMessages = room.child("messages");

                        for (DataSnapshot item : roomMessages.getChildren()) {

                            String messageId = item.getKey();

                            String text = item.child("message").getValue(String.class);

                            String senderId =item.child("senderid")
                                            .getValue(String.class);

                            if (messageId == null
                                    || text == null
                                    || text.trim().isEmpty()
                                    || senderId == null
                                    || senderId.isEmpty()) {
                                continue;
                            }

                            String conversationId = getCanonicalConversationId(
                                            roomId,
                                            senderId
                                    );

                            if (conversationId == null) {
                                Log.w(TAG, "Skipping unrecognized room: " + roomId);
                                continue;
                            }

                            String uniqueKey = conversationId + "|" + messageId;

                            if (!seenMessages.add(uniqueKey)) {
                                continue;
                            }

                            Object timeValue = item.child("timeStamp").getValue();

                            long timestamp = timeValue instanceof Number
                                            ? ((Number) timeValue).longValue()
                                            : 0L;

                            messages.add(
                                    new ExistingMessage(
                                            messageId,
                                            conversationId,
                                            senderId,
                                            text,
                                            timestamp
                                    )
                            );
                        }
                    }

                    int successful = 0;
                    int failed = 0;
                    int total = messages.size();

                    Log.d(TAG, "Existing messages to sync: " + total);

                    for (int i = 0; i < total; i++) {
                        ExistingMessage message = messages.get(i);

                        try {
                            indexExistingMessage(message);
                            successful++;
                        } catch (Exception e) {
                            failed++;

                            Log.e(TAG, "Failed to index message " + message.messageId, e);
                        }

                        final int completed = i + 1;

                        if (completed % 5 == 0 || completed == total) {
                            final int count = completed;

                            mainHandler.post(() -> emptyText.setText("Syncing " + count + " of " + total + " messages..."));
                        }
                    }

                    final int finalSuccess = successful;
                    final int finalFailed = failed;

                    mainHandler.post(() -> {

                        progressBar.setVisibility(View.GONE);
                        syncExistingMessagesButton.setEnabled(true);

                        emptyText.setText("Sync complete. Indexed: " + finalSuccess + ", Failed: " + finalFailed);
                        emptyText.setVisibility(View.VISIBLE);

                        Toast.makeText(
                                SemanticSearchActivity.this,
                                "Sync complete: " + finalSuccess
                                        + " indexed, "
                                        + finalFailed + " failed",
                                Toast.LENGTH_LONG
                        ).show();
                    });

                }).start())
                .addOnFailureListener(error -> {

                    progressBar.setVisibility(View.GONE);
                    syncExistingMessagesButton.setEnabled(true);

                    emptyText.setText("Could not read Firebase messages");
                    emptyText.setVisibility(View.VISIBLE);

                    Log.e(TAG, "Firebase sync read failed", error);

                    Toast.makeText(
                            SemanticSearchActivity.this,
                            "Sync failed: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // INDEX ONE EXISTING MESSAGE
    // =========================================================
    private void indexExistingMessage(ExistingMessage message) throws Exception {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(INDEX_URL);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setDoOutput(true);

            JSONObject request = new JSONObject();

            request.put("messageId", message.messageId);
            request.put("conversationId", message.conversationId);
            request.put("senderId", message.senderId);
            request.put("text", message.text);
            request.put("timestamp", message.timestamp);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(
                        request.toString().getBytes(
                                StandardCharsets.UTF_8
                        )
                );
                output.flush();
            }

            int responseCode = connection.getResponseCode();

            InputStream stream = (responseCode >= 200 && responseCode < 300)
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            StringBuilder response = new StringBuilder();

            if (stream != null) {
                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             stream,
                                             StandardCharsets.UTF_8
                                     )
                             )) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(
                        "HTTP " + responseCode + ": " + response
                );
            }

            JSONObject result = new JSONObject(response.toString());

            if (!result.optBoolean("success", false)) {
                throw new IOException(
                        result.optString(
                                "message",
                                "Backend indexing failed"
                        )
                );
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // =========================================================
    // OPEN CHAT FROM SEARCH RESULT
    // =========================================================
    private void openChatFromSearchResult(SearchResult result) {

        if (result == null) {
            Toast.makeText(
                    this,
                    "Search result is empty",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(
                    this,
                    "User is not logged in",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String senderId = result.getSenderId();
        String conversationId = result.getConversationId();
        String messageId = result.getMessageId();
        String messageText = result.getText();
        long timestamp = result.getTimestamp();

        Log.d(TAG, "SEARCH RESULT CLICKED");
        Log.d(TAG, "Message ID = " + messageId);
        Log.d(TAG, "Message Text = " + messageText);
        Log.d(TAG, "Sender ID = " + senderId);
        Log.d(TAG, "Conversation ID = " + conversationId);
        Log.d(TAG, "Current User ID = " + currentUserId);

        String receiverUid = null;

        // If the other person sent the message, use their UID.
        if (senderId != null
                && !senderId.isEmpty()
                && !senderId.equals(currentUserId)) {

            receiverUid = senderId;
        }

        // If the current user sent the message, identify the other
        // user from the concatenated conversation ID.
        if ((receiverUid == null || receiverUid.isEmpty())
                && conversationId != null
                && !conversationId.isEmpty()) {

            if (conversationId.startsWith(currentUserId)) {

                String possibleReceiver =
                        conversationId.substring(currentUserId.length());

                if (!possibleReceiver.isEmpty()
                        && !possibleReceiver.equals(currentUserId)) {
                    receiverUid = possibleReceiver;
                }
            }

            if ((receiverUid == null || receiverUid.isEmpty())
                    && conversationId.endsWith(currentUserId)) {

                String possibleReceiver = conversationId.substring(
                        0,
                        conversationId.length() - currentUserId.length()
                );

                if (!possibleReceiver.isEmpty()
                        && !possibleReceiver.equals(currentUserId)) {
                    receiverUid = possibleReceiver;
                }
            }
        }

        if (receiverUid == null
                || receiverUid.isEmpty()
                || receiverUid.equals(currentUserId)) {

            Log.e(TAG, "Could not identify receiver");
            Log.e(TAG, "Current UID = " + currentUserId);
            Log.e(TAG, "Sender UID = " + senderId);
            Log.e(TAG, "Conversation ID = " + conversationId);

            Toast.makeText(
                    this,
                    "Could not identify chat user",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Log.d(TAG, "FINAL RECEIVER UID = " + receiverUid);

        Intent intent = new Intent(SemanticSearchActivity.this, chatwindo.class);

        intent.putExtra("uid", receiverUid);
        intent.putExtra("nameeee", "");
        intent.putExtra("reciverImg", "");

        intent.putExtra("targetMessageId", messageId);
        intent.putExtra("targetMessageText", messageText);
        intent.putExtra("targetMessageSenderId", senderId);
        intent.putExtra("targetMessageTimestamp", timestamp);
        intent.putExtra("conversationId", conversationId);

        startActivity(intent);
    }
}