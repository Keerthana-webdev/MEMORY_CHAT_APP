```javascript
const express = require("express");
const cors = require("cors");
require("dotenv").config();

const { GoogleGenAI } = require("@google/genai");
const { Pinecone } = require("@pinecone-database/pinecone");

const app = express();

app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

// --------------------------------------------------
// CHECK ENVIRONMENT VARIABLES
// --------------------------------------------------

if (!process.env.GEMINI_API_KEY) {
    console.error("ERROR: GEMINI_API_KEY is missing in .env");
    process.exit(1);
}

if (!process.env.PINECONE_API_KEY) {
    console.error("ERROR: PINECONE_API_KEY is missing in .env");
    process.exit(1);
}

if (!process.env.PINECONE_INDEX_NAME) {
    console.error("ERROR: PINECONE_INDEX_NAME is missing in .env");
    process.exit(1);
}

// --------------------------------------------------
// GEMINI
// --------------------------------------------------

const ai = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

// --------------------------------------------------
// PINECONE
// --------------------------------------------------

const pc = new Pinecone({
    apiKey: process.env.PINECONE_API_KEY
});

const index = pc.index(process.env.PINECONE_INDEX_NAME);

// --------------------------------------------------
// HOME
// --------------------------------------------------

app.get("/", (req, res) => {
    res.json({
        success: true,
        message: "MemoryChat Semantic Search Backend is running!"
    });
});

// --------------------------------------------------
// TEST GEMINI EMBEDDING
// --------------------------------------------------

app.post("/test-embedding", async (req, res) => {

    try {

        const text = req.body.text;

        if (!text || text.trim().length === 0) {
            return res.status(400).json({
                success: false,
                message: "Text is required"
            });
        }

        const response = await ai.models.embedContent({
            model: "gemini-embedding-001",
            contents: text,
            config: {
                taskType: "RETRIEVAL_DOCUMENT"
            }
        });

        const embedding = response.embeddings[0].values;

        res.json({
            success: true,
            message: "Embedding generated successfully",
            text: text,
            dimensions: embedding.length,
            embedding: embedding
        });

    } catch (error) {

        console.error("Gemini embedding error:", error);

        res.status(500).json({
            success: false,
            message: "Failed to generate embedding",
            error: error.message
        });
    }
});

// --------------------------------------------------
// INDEX A CHAT MESSAGE
// --------------------------------------------------

app.post("/index-message", async (req, res) => {

    try {

        const {
            messageId,
            conversationId,
            senderId,
            text,
            timestamp
        } = req.body;

        // -----------------------------
        // VALIDATION
        // -----------------------------

        if (!messageId) {
            return res.status(400).json({
                success: false,
                message: "messageId is required"
            });
        }

        if (!conversationId) {
            return res.status(400).json({
                success: false,
                message: "conversationId is required"
            });
        }

        if (!senderId) {
            return res.status(400).json({
                success: false,
                message: "senderId is required"
            });
        }

        if (!text || text.trim().length === 0) {
            return res.status(400).json({
                success: false,
                message: "text is required"
            });
        }

        // -----------------------------
        // CREATE GEMINI EMBEDDING
        // -----------------------------

        const response = await ai.models.embedContent({
            model: "gemini-embedding-001",
            contents: text,
            config: {
                taskType: "RETRIEVAL_DOCUMENT"
            }
        });

        const embedding = response.embeddings[0].values;

        // -----------------------------
        // STORE IN PINECONE
        // -----------------------------

        await index.upsert([
            {
                id: messageId,

                values: embedding,

                metadata: {
                    conversationId: conversationId,
                    senderId: senderId,
                    text: text,
                    timestamp: timestamp || Date.now()
                }
            }
        ]);

        // -----------------------------
        // SUCCESS
        // -----------------------------

        res.json({
            success: true,
            message: "Message indexed successfully",
            messageId: messageId,
            dimensions: embedding.length
        });

    } catch (error) {

        console.error("Index message error:", error);

        res.status(500).json({
            success: false,
            message: "Failed to index message",
            error: error.message
        });
    }
});

// --------------------------------------------------
// SEARCH MESSAGES
// --------------------------------------------------

app.post("/search", async (req, res) => {

    try {

        const {
            query,
            topK,
            conversationId
        } = req.body;

        // -----------------------------
        // VALIDATION
        // -----------------------------

        if (!query || query.trim().length === 0) {
            return res.status(400).json({
                success: false,
                message: "Search query is required"
            });
        }

        // -----------------------------
        // CREATE QUERY EMBEDDING
        // -----------------------------

        const response = await ai.models.embedContent({
            model: "gemini-embedding-001",
            contents: query,
            config: {
                taskType: "RETRIEVAL_QUERY"
            }
        });

        const queryEmbedding = response.embeddings[0].values;

        // -----------------------------
        // PINECONE SEARCH
        // -----------------------------

        const searchOptions = {
            vector: queryEmbedding,
            topK: topK || 10,
            includeMetadata: true
        };

        // Optional conversation filtering
        if (conversationId) {
            searchOptions.filter = {
                conversationId: {
                    $eq: conversationId
                }
            };
        }

        const searchResults = await index.query(searchOptions);

        // -----------------------------
        // FORMAT RESULTS
        // -----------------------------

        const results = (searchResults.matches || []).map(match => {

            return {
                messageId: match.id,
                score: match.score,
                text: match.metadata?.text || "",
                conversationId: match.metadata?.conversationId || "",
                senderId: match.metadata?.senderId || "",
                timestamp: match.metadata?.timestamp || null
            };

        });

        // -----------------------------
        // RESPONSE
        // -----------------------------

        res.json({
            success: true,
            query: query,
            results: results
        });

    } catch (error) {

        console.error("Semantic search error:", error);

        res.status(500).json({
            success: false,
            message: "Semantic search failed",
            error: error.message
        });
    }
});

app.listen(PORT, () => {
    console.log("----------------------------------------");
    console.log("MemoryChat backend running");
    console.log(`Port: ${PORT}`);
    console.log("Gemini Embeddings: Connected");
    console.log("Pinecone: Connected");
    console.log("----------------------------------------");
});