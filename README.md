# ConceptClarity

ConceptClarity is a full-stack offline AI-style education platform. Learners type a question, press Send, and receive a structured conversational explanation generated locally by backend NLP-style rules, concept classification, progressive memory, templates, and an internal knowledge base.

No external AI service, paid API, internet access, or secret credential is required for explanation generation.

## Stack

- Frontend: HTML5, CSS3, Vanilla JavaScript
- Backend: Java 17, Spring Boot, Spring MVC, Spring Data JPA
- Database: PostgreSQL
- Architecture: Controller, DTO, service, repository, model, utility, memory, and local AI engine layers

## Local AI Engine

`LocalAIEngine.java` powers explanations fully offline. It detects topic intent, classifies the concept category, chooses the response depth automatically, and always returns:

1. Short Definition
2. Detailed Explanation
3. Step-by-Step Understanding
4. Real-world Analogy
5. Example
6. Key Points Summary

The engine supports progressive learning. The first time a user asks a topic, the answer is beginner-friendly. Repeated questions become intermediate, advanced, then expert-level with implementation, architecture, optimization, and internal-working details.

## Conversation Memory

The backend stores local memory in:

- `conversation_history`
- `topic_tracking`
- `learning_progress`

`ConversationMemoryService.java` handles:

- `trackTopicFrequency()`
- `getExplanationLevel()`
- `updateUserLearningProgress()`

## Features

- ChatGPT-style dashboard
- Single prompt box and Send button
- Left sidebar with recent chats
- Modern chat bubbles
- AI typing animation
- Thinking animation
- Markdown-style headings, bullets, numbered steps, and code blocks
- Copy response
- Save/bookmark response
- PDF export
- Saved explanation history
- History search, favorite filter, and pagination
- Automatic progressive explanation depth
- PBKDF2 password hashing with legacy SHA-256 migration on login
- Validation, input sanitation, CORS config, and global exception handling

## Docker Setup

Run the full application with PostgreSQL:

```bash
docker compose up --build
```

Frontend and backend are served together:

```text
http://localhost:18080
```

Health check:

```text
http://localhost:18080/api/health
```

## PostgreSQL Setup

The backend uses Flyway migrations from:

```text
backend/src/main/resources/db/migration
```

Default local database settings:

```properties
DB_URL=jdbc:postgresql://localhost:5432/concept_clarity
DB_USERNAME=concept_clarity
DB_PASSWORD=concept_clarity
```

To run the backend directly without Docker, start PostgreSQL first and create a database named `concept_clarity`.

```bash
cd backend
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

## Run Frontend Without Docker

Open `frontend/index.html` directly, or run a static server:

```bash
cd frontend
python -m http.server 3000
```

Frontend URL:

```text
http://localhost:3000
```

## API Endpoints

| Feature | Method | Endpoint |
|---|---:|---|
| Register | POST | `/api/auth/register` |
| Login | POST | `/api/auth/login` |
| Chat | POST | `/api/chat` |
| Paged history | GET | `/api/history?userId=1&page=0&size=8` |
| Search history | GET | `/api/history?userId=1&search=oop` |
| Favorite filter | GET | `/api/history?userId=1&favoriteOnly=true` |
| Delete history | DELETE | `/api/history/{queryId}?userId=1` |
| Toggle bookmark | POST | `/api/favorites/{explanationId}?userId=1` |
| Progress | GET | `/api/progress?userId=1` |

Chat request:

```json
{
  "userId": 1,
  "message": "What is recursion?"
}
```

Chat response:

```json
{
  "reply": "...",
  "level": "Beginner",
  "topic": "Recursion"
}
```

Legacy routes such as `/login`, `/register`, `/explain`, and `/history` are still supported for compatibility.

## Demo Account

```text
Email: demo@conceptclarity.com
Password: password123
```

## Example Flow

1. Log in or register.
2. Open the dashboard.
3. Type `What is recursion?`.
4. Press Send.
5. Ask the same topic again to automatically receive a deeper answer.
6. Copy, save, export, or revisit responses from History.
