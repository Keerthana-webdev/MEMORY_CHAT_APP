package com.example.vmessenger;

import static com.example.vmessenger.chatwindo.reciverIImg;
import static com.example.vmessenger.chatwindo.senderImg;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class messagesAdpter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final ArrayList<msgModelclass> messagesAdpterArrayList;

    private static final int ITEM_SEND = 1;
    private static final int ITEM_RECIVE = 2;

    // Position of the message selected in Semantic Search
    private int highlightedPosition = -1;

    public messagesAdpter(
            Context context,
            ArrayList<msgModelclass> messagesAdpterArrayList
    ) {
        this.context = context;
        this.messagesAdpterArrayList = messagesAdpterArrayList;
    }

    // Highlight the selected message
    public void setHighlightedPosition(int position) {
        int oldPosition = highlightedPosition;
        highlightedPosition = position;

        if (oldPosition >= 0
                && oldPosition < messagesAdpterArrayList.size()) {
            notifyItemChanged(oldPosition);
        }

        if (highlightedPosition >= 0
                && highlightedPosition < messagesAdpterArrayList.size()) {
            notifyItemChanged(highlightedPosition);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        if (viewType == ITEM_SEND) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.sender_layout,
                    parent,
                    false
            );

            return new senderVierwHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.reciver_layout,
                    parent,
                    false
            );

            return new reciverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        msgModelclass messages =
                messagesAdpterArrayList.get(position);

        // Preserve the existing long-press dialog
        holder.itemView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        new AlertDialog.Builder(context)
                                .setTitle("Delete")
                                .setMessage(
                                        "Are you sure you want to delete this message?"
                                )
                                .setPositiveButton(
                                        "Yes",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which
                                            ) {
                                                // Existing delete action
                                                // was not implemented.
                                            }
                                        }
                                )
                                .setNegativeButton(
                                        "No",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which
                                            ) {
                                                dialog.dismiss();
                                            }
                                        }
                                )
                                .show();

                        return true;
                    }
                }
        );

        boolean isHighlighted =
                position == highlightedPosition;

        if (holder instanceof senderVierwHolder) {
            senderVierwHolder viewHolder =
                    (senderVierwHolder) holder;

            viewHolder.msgtxt.setText(messages.getMessage());

            applyHighlight(viewHolder.msgtxt, isHighlighted);

            if (senderImg != null && !senderImg.isEmpty()) {
                Picasso.get()
                        .load(senderImg)
                        .into(viewHolder.circleImageView);
            } else {
                viewHolder.circleImageView.setImageResource(
                        R.drawable.photocamera
                );
            }

        } else if (holder instanceof reciverViewHolder) {
            reciverViewHolder viewHolder =
                    (reciverViewHolder) holder;

            viewHolder.msgtxt.setText(messages.getMessage());

            applyHighlight(viewHolder.msgtxt, isHighlighted);

            if (reciverIImg != null && !reciverIImg.isEmpty()) {
                Picasso.get()
                        .load(reciverIImg)
                        .into(viewHolder.circleImageView);
            } else {
                viewHolder.circleImageView.setImageResource(
                        R.drawable.photocamera
                );
            }
        }
    }

    // Tint the selected message bubble yellow
    private void applyHighlight(
            TextView messageText,
            boolean highlighted
    ) {
        if (highlighted) {
            messageText.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#FFF176")
                    )
            );
        } else {
            // Reset recycled message rows
            messageText.setBackgroundTintList(null);
        }
    }

    @Override
    public int getItemCount() {
        return messagesAdpterArrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        msgModelclass messages =
                messagesAdpterArrayList.get(position);

        if (FirebaseAuth.getInstance().getCurrentUser() != null
                && FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid()
                .equals(messages.getSenderid())) {
            return ITEM_SEND;
        }

        return ITEM_RECIVE;
    }

    class senderVierwHolder extends RecyclerView.ViewHolder {

        CircleImageView circleImageView;
        TextView msgtxt;

        public senderVierwHolder(@NonNull View itemView) {
            super(itemView);

            circleImageView =
                    itemView.findViewById(R.id.profilerggg);

            msgtxt =
                    itemView.findViewById(R.id.msgsendertyp);
        }
    }

    class reciverViewHolder extends RecyclerView.ViewHolder {

        CircleImageView circleImageView;
        TextView msgtxt;

        public reciverViewHolder(@NonNull View itemView) {
            super(itemView);

            circleImageView =
                    itemView.findViewById(R.id.pro);

            msgtxt =
                    itemView.findViewById(R.id.recivertextset);
        }
    }
}