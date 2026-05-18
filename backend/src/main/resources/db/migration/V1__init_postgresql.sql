CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE concept_queries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(240) NOT NULL,
    level VARCHAR(40) NOT NULL,
    explanation_type VARCHAR(60) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_concept_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_concept_user_created ON concept_queries (user_id, created_at);
CREATE INDEX idx_concept_user_topic ON concept_queries (user_id, topic);
CREATE INDEX idx_concept_user_level ON concept_queries (user_id, level);

CREATE TABLE explanations (
    id BIGSERIAL PRIMARY KEY,
    query_id BIGINT NOT NULL UNIQUE,
    content TEXT NOT NULL,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_explanation_query FOREIGN KEY (query_id) REFERENCES concept_queries(id) ON DELETE CASCADE
);

CREATE INDEX idx_explanations_favorite ON explanations (favorite);

CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    explanation_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_favorite_user_explanation UNIQUE (user_id, explanation_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_explanation FOREIGN KEY (explanation_id) REFERENCES explanations(id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_user ON favorites (user_id);

CREATE TABLE knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(160) NOT NULL,
    domain VARCHAR(80) NOT NULL,
    summary TEXT NOT NULL,
    keywords VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_knowledge_topic_domain UNIQUE (topic, domain)
);

CREATE INDEX idx_knowledge_domain ON knowledge_base (domain);
CREATE INDEX idx_knowledge_search ON knowledge_base
    USING GIN (to_tsvector('english', topic || ' ' || summary || ' ' || keywords));

CREATE TABLE search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(240) NOT NULL,
    detected_domain VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_search_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_search_user_created ON search_history (user_id, created_at);
CREATE INDEX idx_search_domain ON search_history (detected_domain);

CREATE TABLE conversation_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(1000) NOT NULL,
    topic VARCHAR(240) NOT NULL,
    level VARCHAR(40) NOT NULL,
    topic_frequency INT NOT NULL,
    reply TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_conversation_user_created ON conversation_history (user_id, created_at);
CREATE INDEX idx_conversation_user_topic ON conversation_history (user_id, topic);

CREATE TABLE topic_tracking (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic VARCHAR(240) NOT NULL,
    normalized_topic VARCHAR(240) NOT NULL,
    frequency INT NOT NULL,
    current_level VARCHAR(40) NOT NULL,
    last_asked_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_topic_tracking_user_topic UNIQUE (user_id, normalized_topic),
    CONSTRAINT fk_topic_tracking_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_topic_tracking_user ON topic_tracking (user_id);

CREATE TABLE learning_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_interactions INT NOT NULL DEFAULT 0,
    beginner_count INT NOT NULL DEFAULT 0,
    intermediate_count INT NOT NULL DEFAULT 0,
    advanced_count INT NOT NULL DEFAULT 0,
    expert_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_learning_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO users (id, name, email, password_hash, password_salt, created_at)
VALUES
    (1, 'Demo Learner', 'demo@conceptclarity.com', '0c65ff5e88c3448ed9ab6a8adcd852eadd5cfff623896feb76a8127481a4ee26', '0123456789abcdef0123456789abcdef', NOW());

INSERT INTO knowledge_base (topic, domain, summary, keywords, created_at)
VALUES
    ('Recursion', 'Algorithms', 'A function solves a problem by calling itself with smaller inputs until a base condition stops the process.', 'recursion,base condition,recursive call,call stack,algorithm', NOW()),
    ('DBMS Normalization', 'DBMS', 'Database tables are organized to reduce duplicate data, protect consistency, and clarify relationships.', 'dbms,normalization,primary key,foreign key,normal forms', NOW()),
    ('REST APIs', 'Networking', 'A REST API exposes resources through predictable URLs, HTTP methods, status codes, and structured responses.', 'rest,http,api,endpoint,status code,resource', NOW()),
    ('OOP', 'Java', 'Object-oriented programming organizes software around objects that combine data, behavior, and responsibility.', 'oop,class,object,encapsulation,polymorphism,abstraction', NOW()),
    ('Machine Learning', 'Machine Learning', 'Models learn patterns from data and use those patterns to make predictions on new examples.', 'machine learning,dataset,model,training,prediction,evaluation', NOW());

INSERT INTO concept_queries (id, user_id, topic, level, explanation_type, created_at)
VALUES
    (1, 1, 'Recursion', 'Beginner', 'Auto', NOW()),
    (2, 1, 'DBMS Normalization', 'Intermediate', 'Auto', NOW());

INSERT INTO explanations (id, query_id, content, favorite, created_at)
VALUES
    (1, 1, '## Recursion

### 1. Short Definition
Recursion is a technique where a function solves a problem by calling itself with a smaller version of the same problem.

### 2. Detailed Explanation
Recursion is useful when the same problem pattern repeats. It needs a base condition and a recursive call.

### 3. Step-by-Step Understanding
1. Identify the repeating problem.
2. Define the smallest stopping case.
3. Call the function with a smaller input.
4. Combine the returned result.

### 4. Real-world Analogy
It is like standing between two mirrors where an image repeats, but the explanation still needs a stopping point.

### 5. Example
factorial(3) becomes 3 x factorial(2), then 2 x factorial(1), then stops.

### 6. Key Points Summary
- Recursion needs a base condition.
- Each call should move toward the base condition.
- Deep recursion can use stack memory.', TRUE, NOW()),
    (2, 2, '## DBMS Normalization

### 1. Short Definition
DBMS Normalization organizes database tables to reduce duplicate data and improve consistency.

### 2. Detailed Explanation
Normalization separates related facts into focused tables and connects them with keys.

### 3. Step-by-Step Understanding
1. Identify repeated data.
2. Split facts into focused tables.
3. Add primary and foreign keys.
4. Query related data through joins.

### 4. Real-world Analogy
It is like keeping student details in one file and course details in another instead of copying the same facts everywhere.

### 5. Example
A students table, courses table, and enrollments table work together without duplicate course information.

### 6. Key Points Summary
- Reduces duplication.
- Improves consistency.
- Requires thoughtful relationship design.', FALSE, NOW());

INSERT INTO favorites (id, user_id, explanation_id, created_at)
VALUES
    (1, 1, 1, NOW());

INSERT INTO search_history (user_id, topic, detected_domain, created_at)
VALUES
    (1, 'Recursion', 'Algorithms', NOW()),
    (1, 'DBMS Normalization', 'DBMS', NOW());

INSERT INTO topic_tracking (user_id, topic, normalized_topic, frequency, current_level, last_asked_at)
VALUES
    (1, 'Recursion', 'recursion', 1, 'Beginner', NOW()),
    (1, 'DBMS Normalization', 'dbms normalization', 1, 'Beginner', NOW());

INSERT INTO learning_progress (user_id, total_interactions, beginner_count, intermediate_count, advanced_count, expert_count, updated_at)
VALUES
    (1, 2, 2, 0, 0, 0, NOW());

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('concept_queries_id_seq', (SELECT MAX(id) FROM concept_queries));
SELECT setval('explanations_id_seq', (SELECT MAX(id) FROM explanations));
SELECT setval('favorites_id_seq', (SELECT MAX(id) FROM favorites));
