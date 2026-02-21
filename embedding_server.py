import os
import json
import logging
from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import faiss
import numpy as np

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Constants
MODEL_NAME = 'intfloat/multilingual-e5-small'
EMBEDDING_DIM = 384
DATA_DIR = 'faiss_data'
INDEX_FILE = os.path.join(DATA_DIR, 'faiss_index.bin')
STATE_FILE = os.path.join(DATA_DIR, 'state.json') # Renamed from documents.json to reflect it holds everything

# Global variables
model = None
index = None

# Unified state object to guarantee synchronization
state = {
    "documents": {},  # Map ID -> Metadata & Text
    "id_list": [],    # Sequential list of IDs mapping exactly to FAISS rows
    "embeddings": {}  # Map ID -> Vector (as list of floats for instant rebuilding)
}

def load_model():
    global model
    logger.info(f"Loading model: {MODEL_NAME}...")
    model = SentenceTransformer(MODEL_NAME)
    logger.info("Model loaded successfully")

def init_faiss():
    global index, state
    
    if not os.path.exists(DATA_DIR):
        os.makedirs(DATA_DIR)
    
    # Load index and state if they exist
    if os.path.exists(INDEX_FILE) and os.path.exists(STATE_FILE):
        logger.info("Loading existing FAISS index and state...")
        try:
            index = faiss.read_index(INDEX_FILE)
            with open(STATE_FILE, 'r', encoding='utf-8') as f:
                state = json.load(f)
            logger.info(f"Loaded {index.ntotal} documents from disk")
        except Exception as e:
            logger.error(f"Failed to load index or state: {e}. Creating new ones.")
            index = faiss.IndexFlatL2(EMBEDDING_DIM)
            state = {"documents": {}, "id_list": [], "embeddings": {}}
    else:
        logger.info("Creating new FAISS index and state")
        index = faiss.IndexFlatL2(EMBEDDING_DIM)
        state = {"documents": {}, "id_list": [], "embeddings": {}}

def save_state():
    """Save FAISS index and state to disk"""
    if index is None:
        return
    
    logger.info("Saving index and state to disk...")
    faiss.write_index(index, INDEX_FILE)
    
    # Save the entire state dictionary
    with open(STATE_FILE, 'w', encoding='utf-8') as f:
        json.dump(state, f, ensure_ascii=False) # Removed indent=2 to keep file size smaller with vectors
    logger.info("State saved")

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "healthy", "model": MODEL_NAME})

@app.route('/index/add', methods=['POST'])
def add_document():
    data = request.json
    doc_id = data.get('id')
    text = data.get('text')
    metadata = data.get('metadata', {})
    
    if not doc_id or not text:
        return jsonify({"error": "Missing id or text"}), 400
    
    try:
        # e5 models require "passage: " prefix for indexing
        embedding = model.encode(["passage: " + text]).astype('float32')
        faiss.normalize_L2(embedding)  # Normalize for cosine similarity
        
        # Add to FAISS
        index.add(embedding)
        
        # Add to unified state
        state["documents"][doc_id] = {
            "text": text,
            "metadata": metadata
        }
        state["id_list"].append(doc_id)
        state["embeddings"][doc_id] = embedding[0].tolist() # Cache vector
        
        save_state()
        
        return jsonify({"status": "success", "id": doc_id})
        
    except Exception as e:
        logger.error(f"Error adding document: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/index/add-batch', methods=['POST'])
def add_documents_batch():
    data = request.json
    docs = data.get('documents', [])
    
    if not docs:
        return jsonify({"added": 0}), 200
        
    try:
        texts = ["passage: " + d['text'] for d in docs]
        embeddings = model.encode(texts).astype('float32')
        faiss.normalize_L2(embeddings)  # Normalize for cosine similarity
        
        index.add(embeddings)
        
        for i, doc in enumerate(docs):
            doc_id = doc['id']
            state["documents"][doc_id] = {
                "text": doc['text'],
                "metadata": doc.get('metadata', {})
            }
            state["id_list"].append(doc_id)
            state["embeddings"][doc_id] = embeddings[i].tolist() # Cache vector
            
        save_state()
        return jsonify({"added": len(docs), "total_documents": len(state["id_list"])})
        
    except Exception as e:
        logger.error(f"Batch add error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/index/search', methods=['POST'])
def search():
    data = request.json
    query = data.get('query')
    top_k = data.get('top_k', 5)
    
    if not query:
        return jsonify({"error": "Missing query"}), 400
        
    try:
        # e5 models require "query: " prefix for searching
        query_embedding = model.encode(["query: " + query]).astype('float32')
        faiss.normalize_L2(query_embedding)  # Normalize for cosine similarity
        
        if index.ntotal == 0:
            return jsonify({"results": []})
            
        distances, indices = index.search(query_embedding, top_k)
        
        results = []
        for i, idx in enumerate(indices[0]):
            if idx == -1 or idx >= len(state["id_list"]):
                continue
                
            doc_id = state["id_list"][idx]
            doc = state["documents"][doc_id]
            
            results.append({
                "id": doc_id,
                "score": float(distances[0][i]), # L2 distance (0.0 is exact match, max is 4.0)
                "text": doc['text'],
                "metadata": doc['metadata']
            })
            
        return jsonify({"results": results})
        
    except Exception as e:
        logger.error(f"Search error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/index/documents', methods=['GET'])
def get_documents():
    docs_list = []
    for doc_id, data in state["documents"].items():
        meta = data.get('metadata', {})
        docs_list.append({
            "id": doc_id,
            "title": meta.get('title', 'Untitled'),
            "content": meta.get('content', data.get('text', '')),
            "category": meta.get('category', 'General'),
            "createdBy": meta.get('createdBy', 'System')
        })
    return jsonify(docs_list)

@app.route('/index/stats', methods=['GET'])
def stats():
    return jsonify({
        "total_documents": index.ntotal if index else 0,
        "embedding_dim": EMBEDDING_DIM
    })

@app.route('/index/clear', methods=['POST'])
def clear_index():
    global index, state
    index = faiss.IndexFlatL2(EMBEDDING_DIM)
    state = {"documents": {}, "id_list": [], "embeddings": {}}
    save_state()
    return jsonify({"status": "cleared"})

@app.route('/index/document/<doc_id>', methods=['DELETE'])
def delete_document(doc_id):
    if doc_id not in state["documents"]:
        return jsonify({"error": "Document not found"}), 404
        
    try:
        # 1. Remove from all state trackers
        del state["documents"][doc_id]
        state["id_list"].remove(doc_id)
        del state["embeddings"][doc_id]
        
        # 2. Instantly rebuild index using cached vectors
        global index
        index = faiss.IndexFlatL2(EMBEDDING_DIM)
        
        if state["id_list"]:
            # Gather cached vectors in the EXACT order of the updated id_list
            vectors = [state["embeddings"][i] for i in state["id_list"]]
            np_vectors = np.array(vectors).astype('float32')
            index.add(np_vectors)
                
        save_state()
        return jsonify({"status": "deleted"})
        
    except Exception as e:
        logger.error(f"Delete error: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    load_model()
    init_faiss()
    app.run(port=8001, debug=True)