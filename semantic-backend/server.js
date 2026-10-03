const express = require("express");
const cors = require("cors");
require("dotenv").config();

const { GoogleGenAI } = require("@google/genai");

const app = express();

app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

if (!process.env.GEMINI_API_KEY) {
    console.error("ERROR: GEMINI_API_KEY is missing in .env");
    process.exit(1);
}

const ai = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

app.get("/", (req, res) => {
    res.json({
        success: true,
        message: "MemoryChat Semantic Search Backend is running!"
    });
});

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

app.listen(PORT, () => {
    console.log(`MemoryChat backend running on port ${PORT}`);
    console.log("Gemini Embeddings: Connected");
});