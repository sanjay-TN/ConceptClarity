package com.conceptclarity.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LocalAIEngine {

    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-zA-Z0-9+#.]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "does", "for", "from",
            "how", "i", "in", "is", "it", "me", "of", "on", "or", "the", "this", "to", "use",
            "using", "what", "when", "where", "why", "with", "explain", "define", "tell", "about"
    );

    private final Map<String, ConceptProfile> conceptBank = new LinkedHashMap<>();
    private final Map<String, DomainProfile> domainBank = new LinkedHashMap<>();

    public LocalAIEngine() {
        seedDomains();
        seedConcepts();
    }

    public String generate(String topic, String level, String explanationType) {
        return switch (normalizeType(explanationType)) {
            case "definition" -> generateDefinition(topic, level);
            case "step-by-step" -> generateStepByStep(topic, level);
            default -> generateDetailedExplanation(topic, level);
        };
    }

    public String extractTopic(String message) {
        String cleaned = safeTopic(message)
                .replaceAll("(?i)^(please\\s+)?(explain|define|describe|tell me about|what is|what are|how does|how do|why is|why are)\\s+", "")
                .replaceAll("[?!.]+$", "")
                .trim();

        String normalized = normalize(cleaned);
        for (String knownTopic : conceptBank.keySet()) {
            if (normalized.contains(knownTopic)) {
                return smartTitle(knownTopic);
            }
        }

        List<String> keywords = keywordAnalyzer(cleaned);
        if (!keywords.isEmpty() && cleaned.length() > 80) {
            return smartTitle(String.join(" ", keywords.subList(0, Math.min(3, keywords.size()))));
        }
        return smartTitle(cleaned);
    }

    public String generateChatResponse(String message, String topic, String level, int topicFrequency) {
        Analysis analysis = analyze(topic, level);
        ConceptProfile concept = analysis.concept();
        DomainProfile domain = analysis.domain();
        String levelName = canonicalLevel(level);

        StringBuilder answer = new StringBuilder();
        answer.append("## ").append(analysis.topic()).append("\n\n");
        answer.append(conversationLead(levelName, topicFrequency, analysis.topic())).append("\n\n");

        answer.append("### 1. Short Definition\n");
        answer.append(concept.definition()).append("\n\n");

        answer.append("### 2. Detailed Explanation\n");
        answer.append(detailedChatExplanation(analysis, message)).append("\n\n");

        answer.append("### 3. Step-by-Step Understanding\n");
        List<String> steps = stepsForLevel(analysis, levelName);
        for (int i = 0; i < steps.size(); i++) {
            answer.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }
        answer.append("\n");

        answer.append("### 4. Real-world Analogy\n");
        answer.append(generateAnalogy(analysis.topic(), levelName)).append("\n\n");

        answer.append("### 5. Example\n");
        answer.append(contextualExample(analysis, levelName)).append("\n\n");

        answer.append("### 6. Key Points Summary\n");
        answer.append("- Domain: ").append(domain.name()).append("\n");
        answer.append("- Current depth: ").append(levelName).append("\n");
        answer.append("- Core idea: ").append(concept.coreIdea()).append("\n");
        answer.append("- Watch out for: ").append(concept.commonMistakes().get(0)).append("\n");
        answer.append("- Next useful topic: ").append(domain.recommendations().get(0)).append("\n");

        if ("Advanced".equals(levelName) || "Expert".equals(levelName)) {
            answer.append("\n### Follow-up Insight\n");
            answer.append(generateAdvancedInsights(analysis.topic()));
            if ("Expert".equals(levelName)) {
                answer.append(" At expert depth, also compare alternative designs, measure bottlenecks, and document the operational impact before choosing it in a real system.");
            }
        }

        return answer.toString();
    }

    public List<String> keywordAnalyzer(String topic) {
        String cleaned = safeTopic(topic).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String token : WORD_SPLIT.split(cleaned)) {
            String word = token.trim();
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }
        if (keywords.isEmpty()) {
            keywords.add(cleaned);
        }
        return keywords.stream().limit(8).toList();
    }

    public String topicClassifier(String topic) {
        String normalizedTopic = normalize(topic);
        List<String> keywords = keywordAnalyzer(topic);

        return domainBank.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), scoreDomain(normalizedTopic, keywords, entry.getValue())))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("General Technology");
    }

    public String generateDefinition(String topic, String level) {
        Analysis analysis = analyze(topic, level);
        ConceptProfile concept = analysis.concept();
        DomainProfile domain = analysis.domain();
        String levelName = canonicalLevel(level);

        StringBuilder answer = new StringBuilder();
        answer.append(title(analysis.topic(), levelName, "Definition")).append("\n\n");
        answer.append("Simple meaning\n");
        answer.append(concept.definition()).append("\n\n");
        answer.append("Why it matters\n");
        answer.append("In ").append(domain.name()).append(", ").append(analysis.topic())
                .append(" helps learners understand ").append(domain.problem()).append(". ");
        answer.append(levelDefinitionLens(levelName, concept, domain)).append("\n\n");
        answer.append("Keywords detected\n");
        answer.append(String.join(", ", analysis.keywords())).append("\n\n");
        answer.append("Real-life analogy\n");
        answer.append(generateAnalogy(analysis.topic(), levelName)).append("\n\n");
        answer.append("Tiny example\n");
        answer.append(generateExamples(analysis.topic(), levelName)).append("\n\n");
        answer.append("Quick check\n");
        answer.append("You understand ").append(analysis.topic())
                .append(" if you can say what problem it solves, name its main parts, and recognize one place where it is useful.");
        return answer.toString();
    }

    public String generateDetailedExplanation(String topic, String level) {
        Analysis analysis = analyze(topic, level);
        ConceptProfile concept = analysis.concept();
        DomainProfile domain = analysis.domain();
        String levelName = canonicalLevel(level);

        StringBuilder answer = new StringBuilder();
        answer.append(title(analysis.topic(), levelName, "Detailed Explanation")).append("\n\n");
        answer.append("Concept snapshot\n");
        answer.append(concept.definition()).append(" It belongs mainly to ").append(domain.name())
                .append(" and is usually learned at a ").append(analysis.complexity().toLowerCase(Locale.ROOT))
                .append(" complexity level.").append("\n\n");

        answer.append("Core idea\n");
        answer.append(concept.coreIdea()).append("\n\n");

        answer.append("How it works\n");
        answer.append(workflowParagraph(analysis)).append("\n\n");

        answer.append("Main building blocks\n");
        for (String buildingBlock : concept.buildingBlocks()) {
            answer.append("- ").append(buildingBlock).append("\n");
        }
        answer.append("\n");

        answer.append("Example\n");
        answer.append(generateExamples(analysis.topic(), levelName)).append("\n\n");

        answer.append("Analogy\n");
        answer.append(generateAnalogy(analysis.topic(), levelName)).append("\n\n");

        if ("Advanced".equals(levelName)) {
            answer.append("Advanced insights\n");
            answer.append(generateAdvancedInsights(analysis.topic())).append("\n\n");
        } else if ("Intermediate".equals(levelName)) {
            answer.append("Practical usage\n");
            answer.append("Use ").append(analysis.topic()).append(" when you need ")
                    .append(domain.outcome()).append(". In projects, connect it to inputs, processing rules, outputs, and failure cases instead of memorizing only the term.")
                    .append("\n\n");
        } else {
            answer.append("Beginner learning path\n");
            answer.append("First learn the meaning, then look at one small example, then explain it in your own words. Keep the focus on the problem it solves.")
                    .append("\n\n");
        }

        answer.append("Common mistakes\n");
        for (String mistake : concept.commonMistakes()) {
            answer.append("- ").append(mistake).append("\n");
        }
        answer.append("\n");

        answer.append("Quick revision\n");
        answer.append("Topic: ").append(analysis.topic()).append("\n");
        answer.append("Domain: ").append(domain.name()).append("\n");
        answer.append("Keywords: ").append(String.join(", ", analysis.keywords())).append("\n");
        answer.append("Best next topic: ").append(domain.recommendations().get(0));
        return answer.toString();
    }

    public String generateStepByStep(String topic, String level) {
        Analysis analysis = analyze(topic, level);
        ConceptProfile concept = analysis.concept();
        DomainProfile domain = analysis.domain();
        String levelName = canonicalLevel(level);

        StringBuilder answer = new StringBuilder();
        answer.append(title(analysis.topic(), levelName, "Step-by-Step")).append("\n\n");
        List<String> steps = stepsForLevel(analysis, levelName);
        for (int i = 0; i < steps.size(); i++) {
            answer.append("Step ").append(i + 1).append(":\n");
            answer.append(steps.get(i)).append("\n\n");
        }

        answer.append("Real-life analogy\n");
        answer.append(generateAnalogy(analysis.topic(), levelName)).append("\n\n");

        answer.append(exampleHeading(domain.name())).append("\n");
        answer.append(generateExamples(analysis.topic(), levelName)).append("\n\n");

        if ("Advanced".equals(levelName)) {
            answer.append("Engineering note\n");
            answer.append(generateAdvancedInsights(analysis.topic())).append("\n\n");
        }

        answer.append("Remember\n");
        answer.append(concept.coreIdea());
        return answer.toString();
    }

    public String generateExamples(String topic, String level) {
        Analysis analysis = analyze(topic, level);
        ConceptProfile concept = analysis.concept();
        String levelName = canonicalLevel(level);
        if ("Advanced".equals(levelName) || "Expert".equals(levelName)) {
            return concept.advancedExample();
        }
        if ("Intermediate".equals(levelName)) {
            return concept.intermediateExample();
        }
        return concept.beginnerExample();
    }

    public String generateAnalogy(String topic, String level) {
        Analysis analysis = analyze(topic, level);
        String levelName = canonicalLevel(level);
        if ("Advanced".equals(levelName) || "Expert".equals(levelName)) {
            return analysis.concept().advancedAnalogy();
        }
        if ("Intermediate".equals(levelName)) {
            return analysis.concept().intermediateAnalogy();
        }
        return analysis.concept().beginnerAnalogy();
    }

    public String generateAdvancedInsights(String topic) {
        Analysis analysis = analyze(topic, "Advanced");
        ConceptProfile concept = analysis.concept();
        DomainProfile domain = analysis.domain();
        return "Treat " + analysis.topic() + " as a design decision, not only a definition. Study its boundaries, data flow, performance cost, failure modes, and maintainability impact. "
                + "For " + domain.name() + ", the production question is: does this approach improve " + domain.outcome()
                + " without adding unnecessary complexity? Useful checkpoints: " + String.join(", ", concept.advancedCheckpoints()) + ".";
    }

    public String detectConceptComplexity(String topic) {
        String normalizedTopic = normalize(topic);
        int score = 0;
        List<String> advancedSignals = List.of(
                "distributed", "concurrency", "thread", "transaction", "normalization", "indexing", "security",
                "cryptography", "optimization", "microservice", "architecture", "neural", "backpropagation",
                "dynamic programming", "cache", "deadlock", "kubernetes", "oauth", "jwt"
        );
        List<String> beginnerSignals = List.of(
                "html", "css", "variable", "loop", "function", "class", "array", "recursion", "oop", "database"
        );
        for (String signal : advancedSignals) {
            if (normalizedTopic.contains(signal)) {
                score += 2;
            }
        }
        for (String signal : beginnerSignals) {
            if (normalizedTopic.contains(signal)) {
                score -= 1;
            }
        }
        if (keywordAnalyzer(topic).size() > 4) {
            score++;
        }
        if (score >= 3) {
            return "Advanced";
        }
        if (score >= 1) {
            return "Intermediate";
        }
        return "Beginner";
    }

    public List<String> recommendedTopics(String topic) {
        DomainProfile domain = domainBank.get(topicClassifier(topic));
        if (domain == null) {
            return List.of("OOP", "DBMS Normalization", "Recursion", "REST APIs", "Machine Learning");
        }
        return domain.recommendations();
    }

    private Analysis analyze(String topic, String level) {
        String safeTopic = smartTitle(safeTopic(topic));
        List<String> keywords = keywordAnalyzer(safeTopic);
        String domainName = topicClassifier(safeTopic);
        DomainProfile domain = domainBank.getOrDefault(domainName, domainBank.get("General Technology"));
        ConceptProfile concept = findConceptProfile(safeTopic, domain, keywords);
        return new Analysis(safeTopic, canonicalLevel(level), keywords, domain, concept, detectConceptComplexity(safeTopic));
    }

    private ConceptProfile findConceptProfile(String topic, DomainProfile domain, List<String> keywords) {
        String normalizedTopic = normalize(topic);
        ConceptProfile exact = conceptBank.get(normalizedTopic);
        if (exact != null) {
            return exact;
        }

        for (Map.Entry<String, ConceptProfile> entry : conceptBank.entrySet()) {
            if (normalizedTopic.contains(entry.getKey()) || entry.getKey().contains(normalizedTopic)) {
                return entry.getValue().withTopic(topic);
            }
        }

        for (String keyword : keywords) {
            ConceptProfile match = conceptBank.get(keyword);
            if (match != null) {
                return match.withTopic(topic);
            }
        }

        return genericConcept(topic, domain);
    }

    private int scoreDomain(String topic, List<String> keywords, DomainProfile domain) {
        int score = 0;
        for (String signal : domain.signals()) {
            String normalizedSignal = normalize(signal);
            if (topic.contains(normalizedSignal)) {
                score += 3;
            }
            if (keywords.contains(normalizedSignal)) {
                score += 2;
            }
        }
        return score;
    }

    private String workflowParagraph(Analysis analysis) {
        String level = analysis.level();
        String topic = analysis.topic();
        DomainProfile domain = analysis.domain();
        if ("Advanced".equals(level)) {
            return topic + " works by combining rules, state, inputs, outputs, and constraints into a repeatable structure. At this level, trace the lifecycle end to end: how data enters, how it is transformed, how correctness is preserved, where errors appear, and what trade-offs the design introduces.";
        }
        if ("Intermediate".equals(level)) {
            return topic + " works by taking a specific problem in " + domain.name() + ", breaking it into smaller parts, and applying a predictable rule or pattern. The important skill is to connect each term with a practical example.";
        }
        return topic + " works like a simple process: there is a problem, a useful idea, and a result. Learn the idea with a small example first, then slowly add details.";
    }

    private List<String> stepsForLevel(Analysis analysis, String level) {
        String topic = analysis.topic();
        ConceptProfile concept = analysis.concept();
        if ("Advanced".equals(level) || "Expert".equals(level)) {
            return List.of(
                    "Define the exact production problem that " + topic + " is meant to solve.",
                    "Identify the core abstractions: " + String.join(", ", concept.buildingBlocks()) + ".",
                    "Trace the data or control flow from input to output and mark where state changes.",
                    "Check correctness rules, edge cases, failure states, and security assumptions.",
                    "Estimate complexity, resource cost, maintainability, and operational trade-offs.",
                    "Build a small implementation, test the normal path and edge path, then compare it with the expected behavior.",
                    "Document when " + topic + " is the right choice and when a simpler alternative is better."
            );
        }
        if ("Intermediate".equals(level)) {
            return List.of(
                    "Start with the formal meaning: " + concept.definition(),
                    "Break the concept into parts: " + String.join(", ", concept.buildingBlocks()) + ".",
                    "Connect each part to a practical example instead of memorizing isolated terms.",
                    "Follow how the idea behaves in a small program, database design, network request, or system workflow.",
                    "Compare one correct use and one common mistake so the boundary becomes clear.",
                    "Practice by explaining " + topic + " in your own words and applying it to a new scenario."
            );
        }
        return List.of(
                "Understand the everyday problem first: " + concept.problemSolved(),
                "Give " + topic + " a simple meaning: " + shortSentence(concept.definition()),
                "Look at the main parts: " + String.join(", ", concept.buildingBlocks()) + ".",
                "Use the analogy: " + concept.beginnerAnalogy(),
                "Study one tiny example: " + concept.beginnerExample(),
                "Repeat the idea in your own words and check whether you can explain why it is useful."
        );
    }

    private String levelDefinitionLens(String level, ConceptProfile concept, DomainProfile domain) {
        if ("Expert".equals(level)) {
            return "The expert view focuses on internals, design alternatives, bottlenecks, and production impact.";
        }
        if ("Advanced".equals(level)) {
            return "The advanced view focuses on internals, boundaries, performance, and design trade-offs.";
        }
        if ("Intermediate".equals(level)) {
            return "The intermediate view connects the definition to implementation vocabulary and practical usage.";
        }
        return "The beginner view keeps the idea simple: " + concept.problemSolved() + ".";
    }

    private String exampleHeading(String domainName) {
        if (domainName.contains("Java") || domainName.contains("Spring")) {
            return "Simple Java Example";
        }
        if (domainName.contains("Web")) {
            return "Simple Web Example";
        }
        if (domainName.contains("DBMS")) {
            return "Simple Database Example";
        }
        return "Simple Example";
    }

    private String conversationLead(String level, int topicFrequency, String topic) {
        if (topicFrequency <= 1) {
            return "Let us start simply. I will explain " + topic + " in a beginner-friendly way first, then build it up step by step.";
        }
        if ("Intermediate".equals(level)) {
            return "You have asked about " + topic + " before, so I will go one level deeper and connect the idea to practical usage.";
        }
        if ("Advanced".equals(level)) {
            return "Since this topic is coming up again, I will treat it more technically and include internal working, trade-offs, and implementation thinking.";
        }
        return "You are revisiting " + topic + " repeatedly, so this answer moves into expert-level reasoning: architecture, optimization, edge cases, and production judgment.";
    }

    private String detailedChatExplanation(Analysis analysis, String message) {
        String topic = analysis.topic();
        String level = analysis.level();
        DomainProfile domain = analysis.domain();
        ConceptProfile concept = analysis.concept();

        if ("Expert".equals(level)) {
            return topic + " should be understood as both a concept and a design tool. In " + domain.name()
                    + ", it helps with " + domain.outcome() + ", but the real skill is knowing its limits. "
                    + "At expert level, ask: what data moves through it, what state is created, what can fail, what becomes slow, and what simpler design might work better? "
                    + "The core mechanism is: " + concept.coreIdea();
        }
        if ("Advanced".equals(level)) {
            return topic + " is not just a definition; it has mechanics. Study the internal flow, the constraints, the edge cases, and the performance cost. "
                    + workflowParagraph(analysis) + " This matters because advanced learners must connect correctness with implementation choices.";
        }
        if ("Intermediate".equals(level)) {
            return topic + " sits in " + domain.name() + ". " + workflowParagraph(analysis)
                    + " In practice, you should identify its inputs, outputs, rules, and common mistakes before using it in a project.";
        }
        return topic + " solves a simple learning problem: " + concept.problemSolved()
                + ". Think of it as a useful idea that has a purpose, a few main parts, and a result. "
                + "Once the simple meaning is clear, examples make the topic much easier to remember.";
    }

    private String contextualExample(Analysis analysis, String level) {
        String topic = analysis.topic();
        String domain = analysis.domain().name();
        String baseExample = generateExamples(topic, level);

        if (domain.contains("Java") || domain.contains("Algorithms") || domain.contains("Spring") || topic.toLowerCase(Locale.ROOT).contains("recursion")) {
            return baseExample + "\n\n```java\n" + javaSnippet(topic) + "\n```";
        }
        if (domain.contains("Web") || domain.contains("JavaScript") || domain.contains("HTML")) {
            return baseExample + "\n\n```javascript\n" + javascriptSnippet(topic) + "\n```";
        }
        if (domain.contains("DBMS")) {
            return baseExample + "\n\n```sql\n" + sqlSnippet(topic) + "\n```";
        }
        return baseExample;
    }

    private String javaSnippet(String topic) {
        if (normalize(topic).contains("recursion")) {
            return """
                    int factorial(int n) {
                        if (n <= 1) {
                            return 1;
                        }
                        return n * factorial(n - 1);
                    }
                    """.trim();
        }
        return """
                class ConceptExample {
                    void explain() {
                        System.out.println("Break the concept into purpose, input, process, and output.");
                    }
                }
                """.trim();
    }

    private String javascriptSnippet(String topic) {
        return """
                const explain = (concept) => ({
                    concept,
                    idea: "Understand the purpose, then test it with a small example."
                });
                """.trim();
    }

    private String sqlSnippet(String topic) {
        return """
                SELECT student_id, course_id
                FROM enrollments
                WHERE student_id = 101;
                """.trim();
    }

    private String title(String topic, String level, String type) {
        return topic + " - " + level + " " + type;
    }

    private String safeTopic(String topic) {
        String value = topic == null ? "" : topic.trim().replaceAll("\\s+", " ");
        return value.isBlank() ? "Concept" : value;
    }

    private String smartTitle(String value) {
        String trimmed = safeTopic(value);
        if (trimmed.length() <= 4 || trimmed.equals(trimmed.toUpperCase(Locale.ROOT))) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        String[] words = trimmed.split(" ");
        List<String> titled = new ArrayList<>();
        for (String word : words) {
            if (word.length() <= 2 || word.equals(word.toUpperCase(Locale.ROOT))) {
                titled.add(word);
            } else {
                titled.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }
        return String.join(" ", titled);
    }

    private String shortSentence(String value) {
        int split = value.indexOf('.');
        return split > 0 ? value.substring(0, split + 1) : value;
    }

    private String canonicalLevel(String level) {
        return switch (normalize(level)) {
            case "expert" -> "Expert";
            case "advanced" -> "Advanced";
            case "intermediate" -> "Intermediate";
            default -> "Beginner";
        };
    }

    private String normalizeType(String explanationType) {
        return switch (normalize(explanationType)) {
            case "definition", "define" -> "definition";
            case "step-by-step", "step by step", "stepwise", "steps" -> "step-by-step";
            default -> "detailed";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private ConceptProfile genericConcept(String topic, DomainProfile domain) {
        List<String> buildingBlocks = genericBuildingBlocks(topic, domain.name());
        return new ConceptProfile(
                topic,
                topic + " is a " + domain.name() + " concept connected to " + domain.description() + ". It helps with " + domain.problem() + " by giving a clear way to reason about " + topic + ".",
                topic + " works best when you connect its purpose, main parts, practical flow, and limits instead of memorizing only the term.",
                "confusion about how " + topic + " is used in " + domain.name(),
                buildingBlocks,
                List.of(
                        "Learning " + topic + " as a word without connecting it to a real " + domain.name() + " use case.",
                        "Skipping the input, process, and output flow.",
                        "Ignoring constraints, edge cases, and trade-offs."
                ),
                "Think of " + topic + " like a labeled section in a notebook: it gives one confusing idea a clear place and purpose.",
                "Think of " + topic + " like a workflow checklist in " + domain.name() + ": each part has a role and the order matters.",
                "Think of " + topic + " like a design choice: it is useful only when its trade-offs fit the problem.",
                "A beginner example is to describe one small " + domain.name() + " scenario and mark where " + topic + " appears.",
                "A practical example is to apply " + topic + " in a small feature, then identify " + String.join(", ", buildingBlocks.subList(0, Math.min(3, buildingBlocks.size()))) + ".",
                "An advanced example is to evaluate " + topic + " for correctness, performance, failure handling, and maintainability.",
                List.of("correctness", "complexity", "observability", "failure handling", "maintainability")
        );
    }

    private List<String> genericBuildingBlocks(String topic, String domainName) {
        if (domainName.contains("DBMS")) {
            return List.of("data model", "keys", "constraints", "queries", "consistency rules");
        }
        if (domainName.contains("Networking") || domainName.contains("Spring") || domainName.contains("Web")) {
            return List.of("client", "request", "processing rule", "response", "error handling");
        }
        if (domainName.contains("Algorithms") || domainName.contains("Data Structures")) {
            return List.of("input", "operation", "condition", "output", "complexity");
        }
        if (domainName.contains("AI") || domainName.contains("Machine Learning")) {
            return List.of("data", "pattern", "model or rule", "prediction", "evaluation");
        }
        if (domainName.contains("Cybersecurity")) {
            return List.of("asset", "threat", "control", "verification", "risk reduction");
        }
        if (domainName.contains("OOP") || domainName.contains("Java")) {
            return List.of("responsibility", "state", "behavior", "interface", "collaboration");
        }
        return List.of("purpose", "main idea", "input", "process", "result");
    }

    private void seedDomains() {
        addDomain("Java", "object-oriented programming and backend application logic", "writing reliable, reusable program behavior", "clear code structure and maintainable execution",
                List.of("java", "jvm", "class", "object", "inheritance", "polymorphism", "interface", "thread", "exception", "collection"),
                List.of("OOP", "Java Collections", "Exception Handling", "Multithreading", "JVM Memory"));
        addDomain("DBMS", "database design, storage, querying, and consistency", "organizing data so it stays accurate and searchable", "consistent, efficient data access",
                List.of("dbms", "database", "sql", "normalization", "transaction", "index", "join", "table", "acid", "schema"),
                List.of("SQL Joins", "DBMS Normalization", "Transactions", "Indexes", "ACID Properties"));
        addDomain("Operating Systems", "processes, memory, files, scheduling, and hardware coordination", "managing computer resources safely", "stable and efficient resource usage",
                List.of("os", "operating", "process", "thread", "deadlock", "memory", "scheduler", "paging", "semaphore"),
                List.of("Processes vs Threads", "Deadlock", "Paging", "Scheduling", "Semaphores"));
        addDomain("Networking", "communication between devices, services, and protocols", "moving data reliably between systems", "predictable service communication",
                List.of("network", "tcp", "udp", "http", "dns", "ip", "router", "packet", "rest", "api"),
                List.of("TCP vs UDP", "HTTP", "DNS", "REST APIs", "OSI Model"));
        addDomain("AI", "systems that simulate intelligent behavior through rules, search, and learning", "making software reason, predict, or assist", "useful automated decision support",
                List.of("ai", "artificial", "intelligence", "agent", "nlp", "prompt", "reasoning", "expert", "chatbot"),
                List.of("NLP", "Expert Systems", "Search Algorithms", "Recommendation Systems", "AI Ethics"));
        addDomain("Machine Learning", "models that learn patterns from data", "turning data into predictions or decisions", "measurable predictions from data",
                List.of("machine", "learning", "model", "training", "dataset", "classification", "regression", "neural", "feature"),
                List.of("Supervised Learning", "Regression", "Classification", "Neural Networks", "Overfitting"));
        addDomain("Data Structures", "ways to organize data for efficient access and updates", "choosing the right shape for data", "faster and clearer data operations",
                List.of("array", "linked", "stack", "queue", "tree", "graph", "hash", "heap", "data structure"),
                List.of("Arrays", "Linked Lists", "Stacks", "Queues", "Trees"));
        addDomain("Algorithms", "step-by-step methods for solving computational problems", "solving problems correctly and efficiently", "reliable problem-solving steps",
                List.of("algorithm", "recursion", "sorting", "searching", "dynamic", "greedy", "complexity", "binary"),
                List.of("Recursion", "Binary Search", "Sorting", "Dynamic Programming", "Big O"));
        addDomain("Web Development", "frontend and backend systems that deliver web applications", "building interactive user-facing software", "usable, responsive digital experiences",
                List.of("html", "css", "javascript", "dom", "frontend", "backend", "web", "browser", "responsive"),
                List.of("HTML Semantics", "CSS Grid", "JavaScript DOM", "Responsive Design", "Fetch API"));
        addDomain("Spring Boot", "Java backend services, REST APIs, dependency injection, and production configuration", "building scalable backend APIs quickly", "clean service architecture",
                List.of("spring", "boot", "controller", "service", "repository", "bean", "restcontroller", "jpa", "hibernate"),
                List.of("Spring MVC", "Dependency Injection", "Spring Data JPA", "REST Controllers", "Validation"));
        addDomain("JavaScript", "browser and server-side programming for interactive applications", "making applications dynamic", "responsive client-side behavior",
                List.of("javascript", "js", "promise", "async", "dom", "event", "closure", "fetch", "array"),
                List.of("Promises", "Async Await", "DOM Events", "Closures", "Fetch API"));
        addDomain("HTML/CSS", "semantic structure and visual presentation for web pages", "turning content into accessible interfaces", "clear and responsive page layout",
                List.of("html", "css", "semantic", "flexbox", "grid", "selector", "accessibility", "responsive"),
                List.of("Semantic HTML", "CSS Grid", "Flexbox", "Accessibility", "Media Queries"));
        addDomain("OOP", "software design using objects, classes, responsibilities, and relationships", "modeling real-world behavior in code", "modular and reusable code",
                List.of("oop", "object", "class", "encapsulation", "inheritance", "polymorphism", "abstraction"),
                List.of("Encapsulation", "Inheritance", "Polymorphism", "Abstraction", "Composition"));
        addDomain("Cloud", "deploying and operating software on shared infrastructure", "running applications reliably at scale", "elastic, observable deployments",
                List.of("cloud", "aws", "azure", "gcp", "docker", "kubernetes", "serverless", "deployment", "scaling"),
                List.of("Docker", "Kubernetes", "Load Balancing", "Serverless", "Cloud Storage"));
        addDomain("Cybersecurity", "protecting systems, data, identity, and communication", "reducing risk and preventing misuse", "safer systems and trusted access",
                List.of("security", "cybersecurity", "encryption", "hashing", "xss", "csrf", "jwt", "oauth", "authentication"),
                List.of("Authentication", "Encryption", "Hashing", "XSS", "OAuth"));
        addDomain("General Technology", "technology concepts and practical computing ideas", "understanding technical problems clearly", "better learning and implementation decisions",
                List.of("concept", "system", "software", "technology", "architecture"),
                List.of("OOP", "Recursion", "REST APIs", "DBMS Normalization", "Machine Learning"));
    }

    private void addDomain(String name, String description, String problem, String outcome, List<String> signals, List<String> recommendations) {
        domainBank.put(name, new DomainProfile(name, description, problem, outcome, signals, recommendations));
    }

    private void seedConcepts() {
        addConcept("recursion", "Recursion is a technique where a function solves a problem by calling itself with a smaller version of the same problem.",
                "A recursive solution needs a repeated pattern and a base condition that stops the calls.",
                "problems that repeat in smaller versions",
                List.of("base condition", "recursive call", "smaller input", "call stack"),
                List.of("Forgetting the base condition.", "Calling the function with the same input forever.", "Using recursion where a simple loop is clearer."),
                "It is like standing between two mirrors: the same image repeats, but there must be a point where you stop looking.",
                "It is like opening nested folders until you reach the final file.",
                "It is like delegating subproblems onto the call stack, where each frame waits for a smaller result.",
                "factorial(3) becomes 3 x factorial(2), then 2 x factorial(1), then stops at factorial(1).",
                "Tree traversal uses recursion because every branch can be processed with the same rule.",
                "Recursive algorithms are elegant for trees and divide-and-conquer problems, but deep recursion can overflow the stack.",
                List.of("base case", "termination", "stack depth", "time complexity", "space complexity"));
        addConcept("dbms normalization", "DBMS Normalization is the process of organizing database tables to reduce duplicate data and improve consistency.",
                "Normalization separates related facts into focused tables and connects them with keys.",
                "duplicate or inconsistent database data",
                List.of("tables", "primary key", "foreign key", "functional dependency", "normal forms"),
                List.of("Splitting tables without understanding relationships.", "Over-normalizing simple read-heavy data.", "Ignoring indexes after redesigning tables."),
                "It is like keeping student details in one notebook and course details in another instead of copying the same course name everywhere.",
                "It is like organizing files into folders so each fact has one correct home.",
                "It is a schema design trade-off between write consistency, join cost, and query shape.",
                "Store student data in a students table and course data in a courses table, then connect them through enrollments.",
                "An ecommerce database stores customers, orders, and products separately to avoid repeating customer addresses in every product row.",
                "In production, normalize write-heavy transactional data, then selectively denormalize reporting views when query performance demands it.",
                List.of("normal form", "dependency", "join cost", "index strategy", "data integrity"));
        addConcept("oop", "OOP, or Object-Oriented Programming, is a way to design software using objects that combine data and behavior.",
                "OOP organizes code around responsibilities so systems become easier to model, reuse, and change.",
                "large programs that become hard to organize",
                List.of("class", "object", "encapsulation", "inheritance", "polymorphism", "abstraction"),
                List.of("Creating too many classes without clear responsibility.", "Using inheritance when composition is simpler.", "Hiding behavior in objects with unclear names."),
                "It is like a school where students, teachers, and courses each have their own information and actions.",
                "It is like assigning responsibility to departments so every department knows its job.",
                "It is a boundary-design tool: objects should protect invariants and expose clear behavior.",
                "A Student object can have a name and a method like enrollInCourse().",
                "A Payment interface can allow CardPayment and UpiPayment to behave differently through the same method.",
                "Good OOP favors cohesive objects, stable interfaces, low coupling, and composition over inheritance when behavior varies.",
                List.of("cohesion", "coupling", "invariants", "interfaces", "composition"));
        addConcept("rest api", "A REST API is a web interface that lets clients access and change resources using standard HTTP methods.",
                "REST models backend data as resources and uses predictable URLs and methods to work with them.",
                "communication between frontend and backend systems",
                List.of("resource", "endpoint", "HTTP method", "status code", "request", "response"),
                List.of("Using POST for every action.", "Ignoring proper status codes.", "Putting sensitive data in URLs."),
                "It is like a restaurant menu: the client asks for a specific item, and the kitchen returns it in a standard way.",
                "It is like a service counter where every request has a clear action and ticket number.",
                "It is a contract boundary where resource design, idempotency, caching, and error semantics matter.",
                "GET /topics returns topics, POST /topics creates one, and DELETE /topics/7 removes one.",
                "A frontend calls POST /login with credentials and receives a structured response.",
                "Production REST APIs need consistent status codes, pagination, validation, rate limits, observability, and stable versioning.",
                List.of("resource modeling", "idempotency", "pagination", "validation", "status codes"));
        addConcept("machine learning", "Machine Learning is a field where software learns patterns from data instead of being programmed with every rule manually.",
                "A model studies examples, finds patterns, and uses those patterns to make predictions on new data.",
                "problems where rules are hard to write manually",
                List.of("dataset", "features", "model", "training", "prediction", "evaluation"),
                List.of("Training on poor data.", "Measuring only accuracy.", "Forgetting that models can be biased or stale."),
                "It is like learning to identify fruits after seeing many labeled examples.",
                "It is like training a student with practice questions, then testing on new questions.",
                "It is a statistical system with assumptions, objective functions, drift, and measurable error.",
                "A spam filter learns from examples of spam and non-spam emails.",
                "A house-price model uses features like area, location, and rooms to predict price.",
                "Production ML needs data validation, model monitoring, retraining strategy, explainability, and guardrails against drift.",
                List.of("data quality", "feature engineering", "evaluation", "bias", "model drift"));
        addConcept("ai", "AI, or Artificial Intelligence, is software designed to perform tasks that normally need human-like reasoning, learning, planning, or language understanding.",
                "AI systems use rules, search, data, or models to choose useful actions or generate useful outputs.",
                "tasks that are difficult to solve with simple fixed instructions",
                List.of("input", "knowledge or data", "reasoning rule or model", "output", "feedback"),
                List.of("Calling every automation AI.", "Ignoring errors, bias, and limits.", "Using AI without checking whether the output is reliable."),
                "It is like a smart assistant that can follow clues and suggest a useful next step.",
                "It is like a decision helper that studies information before giving an answer.",
                "AI is a system design layer with model behavior, evaluation, guardrails, latency, cost, and reliability trade-offs.",
                "A chatbot answers a question by interpreting the text and producing a helpful response.",
                "A recommendation system suggests videos based on previous viewing patterns.",
                "Production AI needs evaluation data, fallback paths, monitoring, safety checks, prompt or model versioning, and human review for risky tasks.",
                List.of("evaluation", "bias", "guardrails", "latency", "human oversight"));
        addConcept("binary search", "Binary Search is an algorithm that finds a value in sorted data by repeatedly cutting the search range in half.",
                "Binary Search works only when the data is ordered, because each comparison tells you which half can be ignored.",
                "slow searching in a sorted list",
                List.of("sorted data", "low pointer", "high pointer", "middle value", "comparison"),
                List.of("Using it on unsorted data.", "Updating low or high incorrectly.", "Forgetting boundary cases when the item is missing."),
                "It is like finding a word in a dictionary by opening near the middle and deciding which side to continue.",
                "It is like narrowing a number guessing game after each hint.",
                "It is a logarithmic search strategy whose correctness depends on sorted order and careful boundary invariants.",
                "To find 7 in [1, 3, 5, 7, 9], check the middle, then keep only the half where 7 can exist.",
                "Searching an indexed sorted array can use Binary Search to avoid checking every element.",
                "Production search code must handle duplicates, empty ranges, overflow-safe mid calculation, and clear return behavior.",
                List.of("sorted invariant", "boundary updates", "termination", "O(log n)", "edge cases"));
        addConcept("dependency injection", "Dependency Injection is a design technique where an object receives the services it needs from outside instead of creating them itself.",
                "It separates object creation from object usage, making code easier to test, replace, and maintain.",
                "tight coupling between classes",
                List.of("dependency", "consumer class", "container", "constructor injection", "bean lifecycle"),
                List.of("Injecting too many services into one class.", "Hiding dependencies with static calls.", "Using field injection when constructor injection is clearer."),
                "It is like giving a chef the ingredients instead of making the chef grow them first.",
                "It is like plugging a charger into a device: the device uses power without knowing how the power plant works.",
                "It is an inversion-of-control boundary that improves testability but still needs clear ownership and small services.",
                "A Controller receives a UserService through its constructor and calls it when a request arrives.",
                "In Spring Boot, @Service classes are created as beans and injected into controllers or other services.",
                "Large systems use Dependency Injection to swap implementations, isolate tests, and keep infrastructure separate from business logic.",
                List.of("constructor injection", "bean scope", "loose coupling", "test doubles", "configuration"));
        addConcept("cybersecurity basics", "Cybersecurity Basics are the core practices used to protect systems, accounts, networks, and data from misuse.",
                "Security starts by identifying what must be protected, what can attack it, and which controls reduce the risk.",
                "systems being accessed, changed, or damaged by unauthorized users",
                List.of("assets", "threats", "vulnerabilities", "controls", "monitoring"),
                List.of("Thinking only passwords matter.", "Ignoring updates and backups.", "Trusting every input or link."),
                "It is like locking a house, checking visitors, and keeping a spare copy of important documents.",
                "It is like airport security: identity, rules, monitoring, and response all work together.",
                "Security is risk management across identity, data, networks, software supply chain, detection, and recovery.",
                "Use strong passwords, enable multi-factor authentication, update software, and avoid suspicious links.",
                "A web app validates input, hashes passwords, uses HTTPS, and logs suspicious behavior.",
                "Production security needs threat modeling, least privilege, secure defaults, patch management, audits, and incident response.",
                List.of("least privilege", "defense in depth", "authentication", "encryption", "incident response"));
        addConcept("sql joins", "SQL Joins combine rows from two or more tables using related columns.",
                "A join lets normalized tables work together so you can ask questions that need data from multiple places.",
                "data split across related tables",
                List.of("tables", "join condition", "primary key", "foreign key", "result rows"),
                List.of("Joining without a condition.", "Confusing INNER JOIN and LEFT JOIN.", "Ignoring duplicate rows caused by one-to-many relationships."),
                "It is like matching student ID cards with exam sheets to see each student's marks.",
                "It is like combining two spreadsheets using a shared column.",
                "Join design affects correctness, cardinality, indexes, query plans, and performance.",
                "Join students and enrollments on student_id to see which courses each student takes.",
                "An order report joins customers, orders, and order_items to show customer purchase history.",
                "Production joins need proper indexes, clear cardinality expectations, and careful filtering before aggregation.",
                List.of("cardinality", "join type", "indexes", "query plan", "null behavior"));
        addConcept("arrays", "An Array is a data structure that stores multiple values in ordered positions called indexes.",
                "Arrays make it easy to access items by position, but resizing or inserting in the middle can be costly.",
                "storing ordered items together",
                List.of("index", "element", "length", "access", "iteration"),
                List.of("Using an index outside the valid range.", "Forgetting that many languages start indexes at 0.", "Choosing arrays when frequent middle insertions are needed."),
                "It is like numbered seats in a classroom.",
                "It is like a row of lockers where each locker has a number.",
                "Arrays trade fixed layout and fast indexed access against resizing and insertion costs.",
                "marks[0] reads the first student's mark.",
                "A product page can store image URLs in an array and loop through them for display.",
                "Performance-sensitive code uses arrays for cache-friendly sequential access and predictable memory layout.",
                List.of("index bounds", "memory layout", "iteration", "resizing", "cache locality"));
        addConcept("authentication", "Authentication is the process of verifying who a user or system claims to be.",
                "It proves identity before allowing access to protected actions or data.",
                "unknown users trying to access private resources",
                List.of("identity", "credential", "verification", "session or token", "access decision"),
                List.of("Storing plain-text passwords.", "Confusing authentication with authorization.", "Letting sessions last forever."),
                "It is like showing an ID card before entering an exam hall.",
                "It is like checking a ticket and then giving a temporary pass.",
                "Authentication design must protect credentials, sessions, tokens, recovery flows, and attack surfaces.",
                "A user enters email and password, and the server verifies the password hash before creating a session.",
                "A REST API may return a JWT after successful login so later requests can prove identity.",
                "Production authentication needs password hashing, MFA, rate limits, secure cookies or tokens, and audit logs.",
                List.of("password hashing", "MFA", "session expiry", "token security", "rate limiting"));
    }

    private void addConcept(String key,
                            String definition,
                            String coreIdea,
                            String problemSolved,
                            List<String> buildingBlocks,
                            List<String> commonMistakes,
                            String beginnerAnalogy,
                            String intermediateAnalogy,
                            String advancedAnalogy,
                            String beginnerExample,
                            String intermediateExample,
                            String advancedExample,
                            List<String> advancedCheckpoints) {
        conceptBank.put(key, new ConceptProfile(
                smartTitle(key),
                definition,
                coreIdea,
                problemSolved,
                buildingBlocks,
                commonMistakes,
                beginnerAnalogy,
                intermediateAnalogy,
                advancedAnalogy,
                beginnerExample,
                intermediateExample,
                advancedExample,
                advancedCheckpoints
        ));
    }

    private record Analysis(
            String topic,
            String level,
            List<String> keywords,
            DomainProfile domain,
            ConceptProfile concept,
            String complexity
    ) {
    }

    private record DomainProfile(
            String name,
            String description,
            String problem,
            String outcome,
            List<String> signals,
            List<String> recommendations
    ) {
    }

    private record ConceptProfile(
            String topic,
            String definition,
            String coreIdea,
            String problemSolved,
            List<String> buildingBlocks,
            List<String> commonMistakes,
            String beginnerAnalogy,
            String intermediateAnalogy,
            String advancedAnalogy,
            String beginnerExample,
            String intermediateExample,
            String advancedExample,
            List<String> advancedCheckpoints
    ) {
        ConceptProfile withTopic(String newTopic) {
            return new ConceptProfile(
                    newTopic,
                    definition.replace(topic, newTopic),
                    coreIdea.replace(topic, newTopic),
                    problemSolved,
                    buildingBlocks,
                    commonMistakes,
                    beginnerAnalogy.replace(topic, newTopic),
                    intermediateAnalogy.replace(topic, newTopic),
                    advancedAnalogy.replace(topic, newTopic),
                    beginnerExample.replace(topic, newTopic),
                    intermediateExample.replace(topic, newTopic),
                    advancedExample.replace(topic, newTopic),
                    advancedCheckpoints
            );
        }
    }
}
