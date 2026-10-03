package com.example.vmessenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

    /*
     * IMPORTANT:
     *
     * Android Emulator:
     * http://10.0.2.2:3000
     *
     * Physical Android phone:
     * Replace 10.0.2.2 with your computer's
     * local IPv4 address.
     */
    private static final String BACKEND_URL =
            "http://10.0.2.2:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chatwindo);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // --------------------------------------------------
        // FIREBASE
        // --------------------------------------------------

        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // --------------------------------------------------
        // CHECK LOGIN
        // --------------------------------------------------

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    chatwindo.this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // --------------------------------------------------
        // GET RECEIVER DETAILS
        // --------------------------------------------------

        reciverName = getIntent().getStringExtra("nameeee");
        reciverimg = getIntent().getStringExtra("reciverImg");
        reciverUid = getIntent().getStringExtra("uid");

        reciverIImg = reciverimg != null ? reciverimg : "";

        // --------------------------------------------------
        // MESSAGE ARRAY
        // --------------------------------------------------

        messagesArrayList = new ArrayList<>();

        // --------------------------------------------------
        // FIND VIEWS
        // --------------------------------------------------

        sendbtn = findViewById(R.id.sendbtnn);
        textmsg = findViewById(R.id.textmsg);
        reciverNName = findViewById(R.id.recivername);
        profile = findViewById(R.id.profileimgg);
        messageAdpter = findViewById(R.id.msgadpter);

        // --------------------------------------------------
        // RECYCLER VIEW
        // --------------------------------------------------

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this);

        linearLayoutManager.setStackFromEnd(true);

        messageAdpter.setLayoutManager(linearLayoutManager);

        mmessagesAdpter =
                new messagesAdpter(
                        chatwindo.this,
                        messagesArrayList
                );

        messageAdpter.setAdapter(mmessagesAdpter);

        // --------------------------------------------------
        // RECEIVER PROFILE IMAGE
        // --------------------------------------------------

        if (reciverimg != null && !reciverimg.isEmpty()) {

            Picasso.get()
                    .load(reciverimg)
                    .into(profile);

        } else {

            profile.setImageResource(R.drawable.photocamera);
        }

        // --------------------------------------------------
        // RECEIVER NAME
        // --------------------------------------------------

        reciverNName.setText(
                reciverName != null ? reciverName : ""
        );

        // --------------------------------------------------
        // CURRENT USER
        // --------------------------------------------------

        SenderUID = firebaseAuth.getUid();

        // --------------------------------------------------
        // CREATE CHAT ROOMS
        // --------------------------------------------------

        if (SenderUID != null && reciverUid != null) {

            senderRoom = SenderUID + reciverUid;

            reciverRoom = reciverUid + SenderUID;
        }

        // --------------------------------------------------
        // FIREBASE REFERENCES
        // --------------------------------------------------

        DatabaseReference reference =
                database.getReference()
                        .child("user")
                        .child(SenderUID);

        if (senderRoom == null) {
            Toast.makeText(
                    chatwindo.this,
                    "Unable to create chat room",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        DatabaseReference chatreference =
                database.getReference()
                        .child("chats")
                        .child(senderRoom)
                        .child("messages");

        // --------------------------------------------------
        // LOAD CHAT MESSAGES
        // --------------------------------------------------

        chatreference.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        messagesArrayList.clear();

                        for (DataSnapshot dataSnapshot :
                                snapshot.getChildren()) {

                            msgModelclass messages =
                                    dataSnapshot.getValue(
                                            msgModelclass.class
                                    );

                            if (messages != null) {

                                messagesArrayList.add(messages);
                            }
                        }

                        mmessagesAdpter.notifyDataSetChanged();

                        if (messageAdpter.getAdapter() != null
                                && messagesArrayList.size() > 0) {

                            messageAdpter.scrollToPosition(
                                    messagesArrayList.size() - 1
                            );
                        }
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {

                        Toast.makeText(
                                chatwindo.this,
                                "Failed to load messages",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        // --------------------------------------------------
        // LOAD CURRENT USER PROFILE IMAGE
        // --------------------------------------------------

        reference.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()
                                && snapshot.child("profilepic")
                                .getValue() != null) {

                            senderImg =
                                    snapshot.child("profilepic")
                                            .getValue()
                                            .toString();

                        } else {

                            senderImg = "";
                        }

                        mmessagesAdpter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {
                    }
                }
        );

        // --------------------------------------------------
        // SEND MESSAGE
        // --------------------------------------------------

        sendbtn.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        String message =
                                textmsg.getText()
                                        .toString()
                                        .trim();

                        // -----------------------------
                        // EMPTY MESSAGE CHECK
                        // -----------------------------

                        if (message.isEmpty()) {

                            Toast.makeText(
                                    chatwindo.this,
                                    "Enter The Message First",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // -----------------------------
                        // CHECK CHAT ROOM
                        // -----------------------------

                        if (senderRoom == null
                                || reciverRoom == null
                                || SenderUID == null
                                || reciverUid == null) {

                            Toast.makeText(
                                    chatwindo.this,
                                    "Chat connection error",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // -----------------------------
                        // CLEAR TEXT BOX
                        // -----------------------------

                        textmsg.setText("");

                        // -----------------------------
                        // CREATE TIMESTAMP
                        // -----------------------------

                        Date date = new Date();

                        long timestamp = date.getTime();

                        // -----------------------------
                        // CREATE MESSAGE OBJECT
                        // -----------------------------

                        msgModelclass messagess =
                                new msgModelclass(
                                        message,
                                        SenderUID,
                                        timestamp
                                );

                        // ==================================================
                        // IMPORTANT:
                        // Create ONE Firebase push reference first.
                        //
                        // The generated key becomes messageId.
                        // ==================================================

                        DatabaseReference senderMessageReference =
                                database.getReference()
                                        .child("chats")
                                        .child(senderRoom)
                                        .child("messages")
                                        .push();

                        String messageId =
                                senderMessageReference.getKey();

                        if (messageId == null) {

                            Toast.makeText(
                                    chatwindo.this,
                                    "Unable to create message ID",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // -----------------------------
                        // SAVE MESSAGE TO SENDER ROOM
                        // -----------------------------

                        senderMessageReference
                                .setValue(messagess)
                                .addOnCompleteListener(task -> {

                                    if (task.isSuccessful()) {

                                        // -----------------------------
                                        // SAVE SAME MESSAGE TO RECEIVER
                                        // -----------------------------

                                        database.getReference()
                                                .child("chats")
                                                .child(reciverRoom)
                                                .child("messages")
                                                .child(messageId)
                                                .setValue(messagess);

                                        // -----------------------------
                                        // INDEX MESSAGE FOR
                                        // SEMANTIC SEARCH
                                        // -----------------------------

                                        indexMessageForSemanticSearch(
                                                messageId,
                                                senderRoom,
                                                SenderUID,
                                                message,
                                                timestamp
                                        );

                                    } else {

                                        Toast.makeText(
                                                chatwindo.this,
                                                "Message could not be sent",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                });
                    }
                }
        );
    }

    // ======================================================
    // SEND MESSAGE TO SEMANTIC SEARCH BACKEND
    // ======================================================

    private void indexMessageForSemanticSearch(
            String messageId,
            String conversationId,
            String senderId,
            String text,
            long timestamp) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                // --------------------------------------------------
                // BACKEND URL
                // --------------------------------------------------

                URL url =
                        new URL(
                                BACKEND_URL + "/index-message"
                        );

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setConnectTimeout(10000);

                connection.setReadTimeout(15000);

                connection.setDoOutput(true);

                // --------------------------------------------------
                // CREATE JSON
                // --------------------------------------------------

                JSONObject jsonObject =
                        new JSONObject();

                jsonObject.put(
                        "messageId",
                        messageId
                );

                jsonObject.put(
                        "conversationId",
                        conversationId
                );

                jsonObject.put(
                        "senderId",
                        senderId
                );

                jsonObject.put(
                        "text",
                        text
                );

                jsonObject.put(
                        "timestamp",
                        timestamp
                );

                String jsonBody =
                        jsonObject.toString();

                // --------------------------------------------------
                // SEND JSON TO NODE.JS
                // --------------------------------------------------

                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(
                        jsonBody.getBytes("UTF-8")
                );

                outputStream.flush();

                outputStream.close();

                // --------------------------------------------------
                // GET RESPONSE
                // --------------------------------------------------

                int responseCode =
                        connection.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200
                        && responseCode < 300) {

                    inputStream =
                            connection.getInputStream();

                } else {

                    inputStream =
                            connection.getErrorStream();
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

                    while ((line = reader.readLine()) != null) {

                        response.append(line);
                    }

                    reader.close();
                }

                // --------------------------------------------------
                // LOG RESULT
                // --------------------------------------------------

                if (responseCode >= 200
                        && responseCode < 300) {

                    System.out.println(
                            "SEMANTIC SEARCH INDEX SUCCESS: "
                                    + response
                    );

                } else {

                    System.out.println(
                            "SEMANTIC SEARCH INDEX FAILED: "
                                    + responseCode
                                    + " "
                                    + response
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "SEMANTIC SEARCH CONNECTION ERROR: "
                                + e.getMessage()
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}