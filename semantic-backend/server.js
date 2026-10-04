const express = require("express");
const cors = require("cors");
require("dotenv").config();

const { GoogleGenAI } = require("@google/genai");
const { Pinecone } = require("@pinecone-database/pinecone");

const app = express();

app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

// ----------------------------------------------------
// ENVIRONMENT CHECK
// ----------------------------------------------------
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

// ----------------------------------------------------
// GEMINI
// ----------------------------------------------------
const ai = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

// ----------------------------------------------------
// PINECONE
// ----------------------------------------------------
const pc = new Pinecone({
    apiKey: process.env.PINECONE_API_KEY
});

const index = pc.index({
    name: process.env.PINECONE_INDEX_NAME
});

// ----------------------------------------------------
// HOME
// ----------------------------------------------------
app.get("/", (req, res) => {
    res.json({
        success: true,
        message: "MemoryChat Semantic Search Backend is running!"
    });
});

// ----------------------------------------------------
// TEST GEMINI EMBEDDING
// ----------------------------------------------------
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

        if (
            !response ||
            !response.embeddings ||
            !response.embeddings[0] ||
            !response.embeddings[0].values
        ) {
            throw new Error("Gemini did not return a valid embedding");
        }

        const embedding = response.embeddings[0].values;

        console.log(
            "Embedding generated:",
            embedding.length,
            "dimensions"
        );

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

// ----------------------------------------------------
// INDEX MESSAGE
// ----------------------------------------------------
app.post("/index-message", async (req, res) => {

    try {
        console.log("INDEX MESSAGE REQUEST");
        console.log(req.body);

        const {
            messageId,
            conversationId,
            senderId,
            text,
            timestamp
        } = req.body;

        // --------------------------------------------
        // VALIDATION
        // --------------------------------------------
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

        // --------------------------------------------
        // GENERATE GEMINI EMBEDDING
        // --------------------------------------------
        console.log("Generating Gemini embedding...");

        const response = await ai.models.embedContent({
            model: "gemini-embedding-001",
            contents: text,
            config: {
                taskType: "RETRIEVAL_DOCUMENT"
            }
        });

        if (
            !response ||
            !response.embeddings ||
            !response.embeddings[0] ||
            !response.embeddings[0].values
        ) {
            throw new Error(
                "Gemini did not return a valid embedding"
            );
        }

        const embedding = response.embeddings[0].values;

        console.log(
            "Embedding dimensions:",
            embedding.length
        );

        if (!Array.isArray(embedding) || embedding.length === 0) {
            throw new Error(
                "Generated embedding is empty"
            );
        }

        // --------------------------------------------
        // CREATE PINECONE RECORD
        // --------------------------------------------
        const record = {
            id: String(messageId),
            values: embedding,
            metadata: {
                conversationId: String(conversationId),
                senderId: String(senderId),
                text: String(text),
                timestamp: Number(timestamp || Date.now())
            }
        };

        console.log("Pinecone record created:");
        console.log({
            id: record.id,
            dimensions: record.values.length,
            metadata: record.metadata
        });

        // --------------------------------------------
        // UPSERT INTO PINECONE
        // --------------------------------------------
        console.log("Sending record to Pinecone...");

        const upsertResponse = await index.upsert({
            records: [record]
        });

        console.log(
            "Pinecone upsert response:",
            upsertResponse
        );

        // --------------------------------------------
        // SUCCESS
        // --------------------------------------------
        res.json({
            success: true,
            message: "Message indexed successfully",
            messageId: messageId,
            dimensions: embedding.length,
            pineconeResponse: upsertResponse
        });

    } catch (error) {
        console.error("INDEX MESSAGE ERROR");
        console.error(error);

        res.status(500).json({
            success: false,
            message: "Failed to index message",
            error: error.message
        });
    }
});

// ----------------------------------------------------
// SEMANTIC SEARCH
// ----------------------------------------------------
app.post("/search", async (req, res) => {

    try {
        const {
            query,
            topK,
            conversationId
        } = req.body;

        // --------------------------------------------
        // VALIDATE QUERY
        // --------------------------------------------
        if (!query || query.trim().length === 0) {
            return res.status(400).json({
                success: false,
                message: "Search query is required"
            });
        }

        // --------------------------------------------
        // CREATE QUERY EMBEDDING
        // --------------------------------------------
        console.log("Generating search embedding...");

        const response = await ai.models.embedContent({
            model: "gemini-embedding-001",
            contents: query,
            config: {
                taskType: "RETRIEVAL_QUERY"
            }
        });

        if (
            !response ||
            !response.embeddings ||
            !response.embeddings[0] ||
            !response.embeddings[0].values
        ) {
            throw new Error(
                "Gemini did not return a valid query embedding"
            );
        }

        const queryEmbedding =
            response.embeddings[0].values;

        console.log(
            "Query embedding dimensions:",
            queryEmbedding.length
        );

        // --------------------------------------------
        // PINECONE SEARCH
        // --------------------------------------------
        const searchOptions = {
            vector: queryEmbedding,
            topK: Number(topK) || 10,
            includeMetadata: true
        };

        if (conversationId) {
            searchOptions.filter = {
                conversationId: {
                    $eq: String(conversationId)
                }
            };
        }

        console.log("Searching Pinecone...");

        const searchResults =
            await index.query(searchOptions);

        // --------------------------------------------
        // FORMAT RESULTS
        // --------------------------------------------
        const results =
            (searchResults.matches || []).map(match => {
                return {
                    messageId: match.id,
                    score: match.score,
                    text: match.metadata?.text || "",
                    conversationId: match.metadata?.conversationId || "",
                    senderId: match.metadata?.senderId || "",
                    timestamp: match.metadata?.timestamp || null
                };

            });

        console.log(
            "Search results:",
            results.length
        );

        res.json({
            success: true,
            query: query,
            results: results
        });

    } catch (error) {
        console.error("SEMANTIC SEARCH ERROR");
        console.error(error);

        res.status(500).json({
            success: false,
            message: "Semantic search failed",
            error: error.message
        });
    }
});

// ----------------------------------------------------
// START SERVER
// ----------------------------------------------------
app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://0.0.0.0:${PORT}`);
    console.log("Gemini Embeddings: Connected");
    console.log("Pinecone: Connected");
});