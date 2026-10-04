package com.example.vmessenger;
public class SearchResult {
    private String messageId;
    private String text;
    private String senderId;
    private String conversationId;
    private double score;
    private long timestamp;

    public SearchResult(
            String messageId,
            String text,
            String senderId,
            String conversationId,
            double score,
            long timestamp
    ) {

        this.messageId = messageId;
        this.text = text;
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.score = score;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getText() {
        return text;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public double getScore() {
        return score;
    }

    public long getTimestamp() {
        return timestamp;
    }
}