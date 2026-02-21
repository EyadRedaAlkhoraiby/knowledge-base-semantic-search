# 🧠 Knowledge Base — Semantic Search Engine

A full-stack **Semantic Search** application that finds documents by **meaning**, not just keywords. Built with **Spring Boot**, **Angular 19**, **FAISS**, and a **multilingual AI embedding model** that supports cross-language search across 100+ languages.

> **Example:** Searching *"car repair"* will find documents about *"automobile maintenance"* — and even *"إصلاح السيارات"* (Arabic) — because the system understands meaning, not just words.

---

## ✨ Key Features

- **Semantic Search** — AI-powered similarity search using sentence embeddings instead of keyword matching
- **Multilingual Support** — Cross-language search (English ↔ Arabic ↔ 100+ languages) using the `multilingual-e5-small` model
- **Document Management** — Full CRUD operations with category tagging
- **Bulk Import** — Import documents via CSV or Excel files
- **Category Filtering** — Filter search results by document category
- **Role-Based Access Control** — Admin and User roles with Spring Security
- **Modern UI** — Angular 19 SPA with an Eco-Tech design theme

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Angular 19 Frontend                  │
│              (SPA · Standalone Components)              │
└────────────────────────┬────────────────────────────────┘
                         │ REST API
┌────────────────────────▼────────────────────────────────┐
│                 Spring Boot 3.2 Backend                 │
│         (Security · JPA · REST Controllers)             │
└──────────┬─────────────────────────────┬────────────────┘
           │                             │
┌──────────▼──────────┐      ┌───────────▼───────────────┐
│    H2 Database      │      │   Python Embedding Server  │
│  (Users & Roles)    │      │  (Flask · FAISS · E5 Model)│
└─────────────────────┘      └───────────────────────────┘
```

| Layer | Tech | Purpose |
|---|---|---|
| **Frontend** | Angular 19, TypeScript, SCSS | Dashboard, Auth UI, Search Interface |
| **Backend** | Spring Boot 3.2, Java 17, Spring Security | REST API, Auth, Business Logic |
| **Database** | H2 (embedded) | User accounts & roles |
| **AI / Search** | Python, Flask, FAISS, Sentence-Transformers | Vector embeddings & similarity search |
| **Model** | `intfloat/multilingual-e5-small` (384-dim) | Multilingual sentence embeddings |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** and **Maven**
- **Python 3.9+** and **pip**
- **Node.js 18+** and **npm**

### 1. Clone the Repository

```bash
git clone https://github.com/EyadRedaAlkhoraiby/knowledge-base-semantic-search.git
cd knowledge-base-semantic-search
```

### 2. Start the Python Embedding Server

```bash
pip install -r requirements.txt
python embedding_server.py
```

The embedding server starts on `http://localhost:8001`. On first run, it downloads the multilingual E5 model (~100MB).

### 3. Start the Spring Boot Backend

```bash
mvn spring-boot:run
```

The backend starts on `http://localhost:8082`.

### 4. Start the Angular Frontend

```bash
cd frontend
npm install
npx ng serve
```

The frontend starts on `http://localhost:4200`.

### 5. Open the App

Navigate to **http://localhost:4200** in your browser.

Default admin account:
- **Username:** `admin`
- **Password:** `admin`

---

## 📁 Project Structure

```
├── src/main/java/com/demo/knowledgebase/
│   ├── config/          # Security & CORS configuration
│   ├── controller/      # REST API endpoints
│   ├── model/           # Document, User, Role entities
│   ├── repository/      # JPA repositories
│   └── service/         # Business logic, VectorStore, FileImport
├── frontend/
│   └── src/app/
│       ├── components/  # Login, Register, Dashboard
│       ├── services/    # API, Auth, Toast services
│       ├── guards/      # Auth guard
│       └── models/      # TypeScript interfaces
├── embedding_server.py  # Python FAISS + E5 embedding server
├── requirements.txt     # Python dependencies
├── pom.xml              # Maven config
└── sample_data_arabic.csv  # Sample multilingual dataset
```

---

## 🔍 How Semantic Search Works

1. **Indexing** — When a document is added, its text is sent to the Python embedding server, which converts it into a 384-dimensional vector using the E5 model and stores it in a FAISS index.

2. **Searching** — When a user submits a query, the query text is also converted into a vector. FAISS then finds the documents whose vectors are closest (most similar in meaning) to the query vector.

3. **Cross-Language** — Because the E5 model was trained on 100+ languages, the vector for *"machine learning"* and *"التعلم الآلي"* (Arabic) will be close together in the vector space, enabling cross-language retrieval.

---

## 🛡️ Security

- **Authentication** — Session-based login with Spring Security
- **Authorization** — Role-based access (ADMIN / USER)
- **Admin Operations** — Add, edit, delete, and import documents
- **User Operations** — Search and view documents

---

## 🛠️ Tech Stack

| Technology | Version | Role |
|---|---|---|
| Spring Boot | 3.2 | Backend framework |
| Spring Security | 6.x | Authentication & authorization |
| Spring Data JPA | 3.2 | Database access |
| H2 Database | 2.2 | Embedded SQL database |
| Angular | 19 | Frontend SPA framework |
| Python | 3.9+ | Embedding server runtime |
| Flask | 2.3+ | Python web framework |
| FAISS | 1.7+ | Vector similarity search |
| Sentence-Transformers | 2.2+ | Text-to-vector encoding |
| `multilingual-e5-small` | — | 384-dim multilingual embeddings |

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
