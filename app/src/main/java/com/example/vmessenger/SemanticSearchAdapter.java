package com.example.vmessenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class SemanticSearchAdapter
        extends RecyclerView.Adapter<SemanticSearchAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<SearchResult> results;
    private final OnResultClickListener listener;

    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }

    public SemanticSearchAdapter(
            Context context,
            ArrayList<SearchResult> results,
            OnResultClickListener listener
    ) {
        this.context = context;
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(context).inflate(
                R.layout.search_result_item,
                parent,
                false
        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        SearchResult result = results.get(position);

        holder.messageText.setText(result.getText());

        holder.scoreText.setText(
                String.format(
                        Locale.getDefault(),
                        "Semantic match: %.2f\nTap to open message",
                        result.getScore()
                )
        );

        holder.itemView.setOnClickListener(
                v -> {
                    if (listener != null) {
                        listener.onResultClick(result);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView messageText;
        TextView scoreText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            messageText = itemView.findViewById(
                    R.id.resultMessage
            );

            scoreText = itemView.findViewById(
                    R.id.resultScore
            );
        }
    }
}