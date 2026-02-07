# 📌 GitUpskill

> **AI-Powered Developer Skill & Learning Recommendation Platform**

GitUpskill is an AI-powered web application that analyzes a developer's GitHub projects to identify their current skill set and recommend personalized upskilling paths. By leveraging repository metadata and README files, the platform generates learning suggestions without requiring direct access to source code.

![Tech Stack](https://img.shields.io/badge/Frontend-Next.js-black?logo=next.js)
![Tech Stack](https://img.shields.io/badge/Backend-Spring%20Boot-green?logo=spring)
![Tech Stack](https://img.shields.io/badge/Database-MongoDB-darkgreen?logo=mongodb)
![Tech Stack](https://img.shields.io/badge/Auth-OAuth%202.0-blue?logo=github)

---

## 🚀 Features

- **GitHub OAuth & Email Authentication** – Secure login options with JWT-based session handling
- **Automatic Repository Fetching** – Fetches user repositories within a configurable time range
- **Intelligent README Analysis** – Extracts insights from README files and repository metadata
- **AI-Powered Skill Extraction** – Identifies technologies, frameworks, and experience levels
- **Gap Analysis** – Detects missing skills and areas for improvement
- **Personalized Learning Roadmap** – Generates actionable upskilling recommendations
- **Deterministic Recommendation Engine** – Rule-based analysis without external AI dependencies
- **Persistent Analysis History** – Stores and tracks user analysis results over time

---

## 🧠 How It Works

```
1. User logs in using GitHub OAuth or email/password
                    ↓
2. Backend fetches user repositories from GitHub (within selected timeframe)
                    ↓
3. README files and repository metadata are extracted and summarized
                    ↓
4. Recommendation engine analyzes the summaries to infer skills, strengths, and gaps
                    ↓
5. System generates a structured upskilling roadmap for the user
```

---

## 🏗️ Architecture

### High-Level Flow

```
┌─────────────────┐
│   Controller    │
└────────┬────────┘
         ↓
┌─────────────────┐
│  Service Layer  │
└────────┬────────┘
         ↓
┌─────────────────┐
│ GitHub Service  │ ←── GitHub API (OAuth + REST)
└────────┬────────┘
         ↓
┌─────────────────┐
│ Preprocessing & │
│ Summarization   │
└────────┬────────┘
         ↓
┌─────────────────┐
│ Recommendation  │ ←── Rule-based Engine
│     Engine      │
└────────┬────────┘
         ↓
┌─────────────────┐
│    MongoDB      │
│   Persistence   │
└─────────────────┘
```

---

## 🛠️ Tech Stack

### Frontend
| Technology | Purpose |
|------------|---------|
| **Next.js** | React framework with SSR/SSG |
| **Tailwind CSS** | Utility-first CSS framework |
| **TypeScript** | Type-safe JavaScript |

### Backend
| Technology | Purpose |
|------------|---------|
| **Spring Boot** | Java application framework |
| **Spring Security** | JWT + OAuth authentication |
| **MongoDB** | NoSQL document database |

### Recommendation Engine
| Technology | Purpose |
|------------|---------|
| **Rule-Based Engine** | Deterministic skill analysis |
| **Skill Ontology Graph** | Skill dependency relationships |
| **Role Templates** | Gap analysis and recommendations |

---

## 📦 Backend Modules

### 🔐 Authentication Module

- **GitHub OAuth** (preferred) – Seamless login with GitHub credentials
- **Email + Password** (optional) – Traditional authentication fallback
- **JWT-based Sessions** – Stateless, secure session management

```
User Entity
├── id
├── email
├── githubUsername
└── accessToken (encrypted)
```

### 🔗 GitHub Integration Service

**Responsibilities:**
- Fetch user repositories via GitHub API
- Filter by last updated date and visibility (public only)
- Extract README files (no source code access required)

**Why README-only approach?**
- ✅ GitHub API natively supports it
- ✅ Low bandwidth consumption
- ✅ High signal-to-noise ratio
- ✅ Ethically clean – no proprietary code access

### 🧹 Preprocessing & Token Control

> **Critical for cost-effective AI processing**

#### Step 1: Per-Repo README Summary

For each repository, extract and structure:

```json
{
  "repo": "expense-tracker",
  "tech": ["Spring Boot", "MongoDB", "JWT"],
  "features": ["Auth", "REST API"],
  "complexity": "intermediate"
}
```
*→ Stored in MongoDB for persistence*

#### Step 2: Aggregate Summary

Instead of sending raw READMEs to the LLM:

```json
{
  "languages": ["Java", "JavaScript"],
  "frameworks": ["Spring Boot", "Next.js"],
  "projectTypes": ["REST APIs", "Dashboards"],
  "missing": ["Testing", "CI/CD"]
}
```
*→ Only this condensed summary goes to the AI agent*

### 🤖 Recommendation Engine

**Rule-Based Analysis:**
- Skill ontology graph for understanding relationships
- Predefined role templates for gap analysis
- Weighted scoring algorithm for prioritization
- Deterministic, reproducible results

**How It Works:**
> The engine uses skill dependency graphs and role templates to classify skills into strong, moderate, and weak categories, then generates personalized learning recommendations based on the user's target role.

---

## 🗄️ Database Schema

### MongoDB Collections

| Collection | Purpose |
|------------|---------|
| `users` | User profiles and authentication data |
| `repositories` | Cached repository metadata |
| `repo_summaries` | Preprocessed README summaries |
| `analysis_results` | Skill analysis results |

---

## 📊 Key Engineering Highlights

| Aspect | Implementation |
|--------|----------------|
| **No External AI Dependencies** | Deterministic rule-based engine for cost-free analysis |
| **Modular Architecture** | Clear separation of concerns across service layers |
| **Secure Integration** | OAuth 2.0 with encrypted token storage |
| **Caching Strategy** | Persistent storage of GitHub and analysis results |
| **Scalable Design** | Stateless backend with MongoDB for horizontal scaling |

---

## ✅ Recommendation Engine Capabilities

### What It CAN Do
- ✔️ Skill inference from README analysis
- ✔️ Tech stack identification
- ✔️ Learning gap detection using skill ontology
- ✔️ Personalized roadmap suggestions based on role templates
- ✔️ Experience level estimation

### What It CANNOT Do (by design)
- ✘ Algorithm quality analysis
- ✘ Code style scoring
- ✘ Performance benchmarking

> *The platform uses a deterministic, rule-based approach focusing on ethical, metadata-based analysis without accessing proprietary source code or requiring external AI services.*

---

## 🚀 Getting Started

### Prerequisites
- **Node.js 18+** - [Download](https://nodejs.org/)
- **Java 21+** - [Download](https://adoptium.net/)
- **MongoDB** - [Install locally](https://www.mongodb.com/docs/manual/installation/) or use [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
- **GitHub OAuth App** - Required for GitHub login

---

### 🔑 Step 1: Get Your API Keys

#### 1.1 Create a GitHub OAuth App

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click **"New OAuth App"**
3. Fill in the details:
   - **Application name:** GitUpskill
   - **Homepage URL:** `http://localhost:3000`
   - **Authorization callback URL:** `http://localhost:3000/auth/github/callback`
4. Click **"Register application"**
5. Copy the **Client ID**
6. Click **"Generate a new client secret"** and copy it

#### 1.2 Generate a JWT Secret

Run this command to generate a secure secret:
```bash
openssl rand -base64 32
```

---

### 🗄️ Step 2: Setup MongoDB

**Option A: Local MongoDB**
```bash
# Install MongoDB (Ubuntu/Debian)
sudo apt install mongodb

# Start MongoDB
sudo systemctl start mongodb
```

**Option B: MongoDB Atlas (Cloud)**
1. Create a free cluster at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Get your connection string: `mongodb+srv://username:password@cluster.xxxxx.mongodb.net/gitupskill`

---

### ⚙️ Step 3: Configure Backend

```bash
cd server

# Copy the example environment file
cp .env.example .env

# Edit .env and fill in your values
```

**Required environment variables in `server/.env`:**
```properties
# MongoDB
MONGODB_URI=mongodb://localhost:27017/gitupskill

# JWT (use your generated secret)
JWT_SECRET=your-super-secure-jwt-secret-key-at-least-32-chars

# GitHub OAuth
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:3000/auth/github/callback
```

**Run the backend:**
```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

---

### 🎨 Step 4: Configure Frontend

```bash
cd client

# Install dependencies
npm install

# Copy the example environment file
cp .env.example .env.local

# Edit .env.local and fill in your values
```

**Required environment variables in `client/.env.local`:**
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_GITHUB_CLIENT_ID=your_github_client_id
```

**Run the frontend:**
```bash
npm run dev
```

The app will be available at `http://localhost:3000`

---

### 🎉 Step 5: Start Using GitUpskill!

1. Open [http://localhost:3000](http://localhost:3000)
2. Click **"Login with GitHub"**
3. Authorize the app
4. Run your first skill analysis!

---

### 📋 Environment Variables Summary

| Variable | Where | Description |
|----------|-------|-------------|
| `MONGODB_URI` | Backend | MongoDB connection string |
| `JWT_SECRET` | Backend | Secret key for JWT tokens (min 32 chars) |
| `GITHUB_CLIENT_ID` | Both | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | Backend | GitHub OAuth App Secret |
| `GITHUB_REDIRECT_URI` | Backend | OAuth callback URL |
| `NEXT_PUBLIC_API_URL` | Frontend | Backend API URL |

---

## 🔮 Future Enhancements

- [ ] Code-level analysis for public repositories (opt-in)
- [ ] Skill progression tracking over time
- [ ] Internship readiness score
- [ ] Open-source contribution recommendations
- [ ] Integration with LinkedIn for portfolio generation

---

## 📁 Project Structure

```
GitUpskill/
├── client/                 # Next.js Frontend
│   ├── app/               # App router pages
│   ├── components/        # React components
│   └── public/            # Static assets
│
├── server/                 # Spring Boot Backend
│   ├── src/main/java/
│   │   └── com/example/endtrem/
│   │       ├── controller/    # REST endpoints
│   │       ├── service/       # Business logic
│   │       ├── repository/    # MongoDB repositories
│   │       ├── model/         # Entity classes
│   │       ├── config/        # Security & OAuth config
│   │       ├── engine/        # Recommendation engine
│   │       ├── dto/           # Data transfer objects
│   │       └── security/      # JWT & auth filters
│   └── src/main/resources/
│       └── application.properties
│
└── README.md
```

---

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines before submitting a PR.

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

**Built with ❤️ for developers who want to level up**

</div>
