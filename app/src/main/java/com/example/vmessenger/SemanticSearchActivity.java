package com.example.vmessenger;

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
    private static final String SEARCH_URL = "http://127.0.0.1:3000/search";
    EditText searchInput;
    ImageView searchButton;
    ImageView backButton;
    ProgressBar progressBar;
    TextView emptyText;
    RecyclerView searchRecyclerView;
    ArrayList<SearchResult> resultsList;
    SemanticSearchAdapter adapter;
    Handler mainHandler = new Handler(Looper.getMainLooper());

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

        resultsList = new ArrayList<>();

        searchRecyclerView.setLayoutManager(
                new LinearLayoutManager(SemanticSearchActivity.this)
        );

        adapter = new SemanticSearchAdapter(
                SemanticSearchActivity.this,
                resultsList
        );

        searchRecyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.GONE);

        emptyText.setText("Search your memories");
        emptyText.setVisibility(View.VISIBLE);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String query = searchInput.getText()
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
        });
    }
    private void performSemanticSearch(String query) {

        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        resultsList.clear();
        adapter.notifyDataSetChanged();

        Log.d(TAG, "Searching for: " + query);

        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpURLConnection connection = null;

                try {

                    URL url = new URL(SEARCH_URL);

                    connection =
                            (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(20000);

                    connection.setRequestProperty(
                            "Content-Type",
                            "application/json"
                    );

                    connection.setDoOutput(true);

                    JSONObject request = new JSONObject();

                    request.put("query", query);
                    request.put("topK", 10);

                    Log.d(TAG,
                            "Search request: " +
                                    request.toString()
                    );

                    OutputStream outputStream =
                            connection.getOutputStream();

                    outputStream.write(
                            request.toString()
                                    .getBytes(StandardCharsets.UTF_8)
                    );

                    outputStream.flush();
                    outputStream.close();

                    int responseCode =
                            connection.getResponseCode();

                    Log.d(TAG,
                            "Search response code: " +
                                    responseCode
                    );

                    InputStream inputStream;

                    if (responseCode >= 200 &&
                            responseCode < 300) {

                        inputStream =
                                connection.getInputStream();

                    } else {

                        inputStream =
                                connection.getErrorStream();
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

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    String responseText =
                            response.toString();

                    Log.d(TAG,
                            "Search response: " +
                                    responseText
                    );

                    if (responseCode < 200 ||
                            responseCode >= 300) {

                        throw new Exception(
                                "Backend returned HTTP " +
                                        responseCode +
                                        ": " +
                                        responseText
                        );
                    }

                    JSONObject json =
                            new JSONObject(responseText);

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

                    JSONArray results =
                            json.optJSONArray("results");

                    ArrayList<SearchResult> tempResults =
                            new ArrayList<>();

                    if (results != null) {

                        for (int i = 0;
                             i < results.length();
                             i++) {

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

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {

                            progressBar.setVisibility(
                                    View.GONE
                            );

                            resultsList.clear();

                            resultsList.addAll(
                                    tempResults
                            );

                            adapter.notifyDataSetChanged();

                            if (tempResults.isEmpty()) {

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
                                        tempResults.size() +
                                                " memories found",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    });

                } catch (Exception e) {

                    Log.e(
                            TAG,
                            "SEMANTIC SEARCH ERROR",
                            e
                    );

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {

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
                                    "Search failed: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                } finally {

                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
}