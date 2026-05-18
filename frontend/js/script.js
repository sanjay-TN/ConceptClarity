const API_BASE = window.CONCEPT_CLARITY_API_BASE
    || (window.location.protocol === "file:" ? "http://localhost:8080/api" : "/api");
const fallbackSuggestions = [
    "Recursion",
    "DBMS Normalization",
    "REST APIs",
    "OOP",
    "Machine Learning",
    "Spring Boot Dependency Injection",
    "Cybersecurity Basics",
    "Binary Search"
];

const state = {
    currentExplanation: null,
    historyPage: 0,
    historySize: 8,
    typingTimer: null,
    suggestionTimer: null,
    chatMessages: []
};

document.addEventListener("DOMContentLoaded", () => {
    ensureToastWrap();
    initTheme();
    initNavigation();
    initAuthForms();
    initDashboard();
    initHistory();
});

function initTheme() {
    const savedTheme = localStorage.getItem("cc-theme") || "dark";
    setTheme(savedTheme);
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const nextTheme = document.body.classList.contains("light") ? "dark" : "light";
            localStorage.setItem("cc-theme", nextTheme);
            setTheme(nextTheme);
        });
    });
}

function setTheme(theme) {
    document.body.classList.toggle("light", theme === "light");
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
        button.textContent = theme === "light" ? "Dark" : "Light";
    });
}

function initNavigation() {
    document.querySelectorAll("[data-logout]").forEach((button) => {
        button.addEventListener("click", () => {
            localStorage.removeItem("cc-user");
            toast("Logged out successfully.", "success");
            setTimeout(() => {
                window.location.href = "login.html";
            }, 350);
        });
    });

    if (document.body.dataset.protected === "true" && !getUser()) {
        window.location.href = "login.html";
        return;
    }

    const user = getUser();
    document.querySelectorAll("[data-user-name]").forEach((item) => {
        item.textContent = user ? user.name : "Learner";
    });
}

function initAuthForms() {
    const registerForm = document.querySelector("[data-register-form]");
    if (registerForm) {
        registerForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = {
                name: registerForm.name.value.trim(),
                email: registerForm.email.value.trim(),
                password: registerForm.password.value
            };
            if (!payload.name || !isEmail(payload.email) || payload.password.length < 6) {
                toast("Enter a name, valid email, and password of at least 6 characters.", "error");
                return;
            }
            await submitAuth("/auth/register", payload);
        });
    }

    const loginForm = document.querySelector("[data-login-form]");
    if (loginForm) {
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const payload = {
                email: loginForm.email.value.trim(),
                password: loginForm.password.value
            };
            if (!isEmail(payload.email) || !payload.password) {
                toast("Enter a valid email and password.", "error");
                return;
            }
            await submitAuth("/auth/login", payload);
        });
    }
}

async function submitAuth(path, payload) {
    try {
        const response = await api(path, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        localStorage.setItem("cc-user", JSON.stringify({
            userId: response.userId,
            name: response.name,
            email: response.email
        }));
        toast(response.message || "Welcome to ConceptClarity.", "success");
        setTimeout(() => {
            window.location.href = "dashboard.html";
        }, 450);
    } catch (error) {
        toast(error.message, "error");
    }
}

function initDashboard() {
    const form = document.querySelector("[data-chat-form]");
    if (!form) {
        return;
    }

    loadProgress();
    initChatComposer(form);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        await sendChatMessage(form);
    });

    document.querySelector("[data-copy]")?.addEventListener("click", copyCurrentExplanation);
    document.querySelector("[data-bookmark]")?.addEventListener("click", bookmarkCurrentExplanation);
    document.querySelector("[data-export]")?.addEventListener("click", () => exportExplanation(state.currentExplanation));
}

function initChatComposer(form) {
    const input = form.querySelector("[data-chat-input]");
    if (!input) {
        return;
    }

    const resizeInput = () => {
        input.style.height = "auto";
        input.style.height = `${Math.min(input.scrollHeight, 180)}px`;
    };

    input.addEventListener("input", resizeInput);
    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });
    resizeInput();
}

async function sendChatMessage(form) {
    const input = form.querySelector("[data-chat-input]");
    const button = document.querySelector("[data-send-button]");
    const user = getUser();
    const message = input.value.trim();

    if (message.length < 2) {
        toast("Type a question or topic first.", "error");
        input.focus();
        return;
    }

    appendChatMessage("user", message);
    input.value = "";
    input.style.height = "auto";
    setLoading(null, button, true);
    const thinking = appendThinkingMessage();

    try {
        const response = await api("/chat", {
            method: "POST",
            body: JSON.stringify({
                userId: user.userId,
                message
            })
        });
        state.currentExplanation = response;
        thinking.remove();
        appendChatMessage("assistant", response.reply, response);
        await loadProgress();
    } catch (error) {
        thinking.remove();
        appendChatMessage("assistant", "I could not generate a response right now. Please check that the backend is running on port 8080.");
        toast(error.message, "error");
    } finally {
        setLoading(null, button, false);
        input.focus();
    }
}

function appendChatMessage(role, text, meta = null) {
    const list = document.querySelector("[data-chat-messages]");
    if (!list) {
        return null;
    }

    const message = document.createElement("article");
    message.className = `chat-message ${role === "user" ? "user-message" : "assistant-message"}`;
    const avatar = role === "user" ? "You" : "CC";
    const label = role === "user" ? "You" : `ConceptClarity${meta?.level ? ` · ${meta.level}` : ""}`;
    message.innerHTML = `
        <div class="avatar">${avatar}</div>
        <div class="message-card">
            <div class="message-meta">${escapeHtml(label)}</div>
            <div class="message-content"></div>
        </div>
    `;
    list.appendChild(message);
    const content = message.querySelector(".message-content");
    if (role === "assistant" && meta?.reply) {
        typeAssistantMessage(content, text, list);
    } else {
        content.innerHTML = role === "assistant" ? formatExplanation(text) : `<p>${escapeHtml(text)}</p>`;
        scrollChatToBottom();
    }
    return message;
}

function appendThinkingMessage() {
    const message = appendChatMessage("assistant", "");
    message.querySelector(".message-content").innerHTML = `
        <div class="thinking-dots" aria-label="ConceptClarity is thinking">
            <span></span><span></span><span></span>
        </div>
    `;
    scrollChatToBottom();
    return message;
}

function typeAssistantMessage(element, text) {
    clearInterval(state.typingTimer);
    element.innerHTML = "";
    let index = 0;
    const intervalMs = 9;
    const charsPerTick = Math.max(4, Math.ceil(text.length / 220));

    state.typingTimer = setInterval(() => {
        index += charsPerTick;
        element.innerHTML = formatExplanation(text.slice(0, index));
        scrollChatToBottom();
        if (index >= text.length) {
            clearInterval(state.typingTimer);
            element.innerHTML = formatExplanation(text);
            scrollChatToBottom();
        }
    }, intervalMs);
}

function scrollChatToBottom() {
    const list = document.querySelector("[data-chat-messages]");
    if (list) {
        list.scrollTop = list.scrollHeight;
    }
}

function formatExplanation(text) {
    const rawLines = String(text || "").replaceAll("\r\n", "\n").split("\n");
    if (!rawLines.some((line) => line.trim())) {
        return "<p>No explanation was returned.</p>";
    }

    const html = [];
    let inCode = false;
    let codeLines = [];
    let codeLanguage = "";

    rawLines.forEach((line) => {
        const trimmed = line.trim();
        if (!trimmed) {
            return;
        }

        if (trimmed.startsWith("```")) {
            if (inCode) {
                html.push(`<pre><code class="language-${escapeHtml(codeLanguage)}">${escapeHtml(codeLines.join("\n"))}</code></pre>`);
                codeLines = [];
                codeLanguage = "";
                inCode = false;
            } else {
                codeLanguage = trimmed.replace("```", "").trim();
                inCode = true;
            }
            return;
        }

        if (inCode) {
            codeLines.push(line);
            return;
        }

        const clean = escapeHtml(trimmed).replaceAll("**", "");
        if (/^##\s+/.test(trimmed)) {
            html.push(`<h3>${clean.replace(/^#{2,3}\s*/, "")}</h3>`);
            return;
        }
        if (/^###\s+/.test(trimmed)) {
            html.push(`<h4>${clean.replace(/^#{2,3}\s*/, "")}</h4>`);
            return;
        }
        if (/^(-|\d+\.|Step \d+:)/.test(clean)) {
            html.push(`<p class="answer-line">${clean}</p>`);
            return;
        }
        html.push(`<p>${clean}</p>`);
    });

    if (inCode && codeLines.length) {
        html.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
    }

    return html.join("");
}

async function copyCurrentExplanation() {
    if (!state.currentExplanation) {
        toast("Generate an explanation before copying.", "error");
        return;
    }
    await copyText(responseText(state.currentExplanation));
}

async function bookmarkCurrentExplanation() {
    const user = getUser();
    if (!state.currentExplanation) {
        toast("Generate an explanation before bookmarking.", "error");
        return;
    }
    try {
        const updated = await api(`/favorites/${state.currentExplanation.explanationId}?userId=${encodeURIComponent(user.userId)}`, { method: "POST" });
        state.currentExplanation = {
            ...state.currentExplanation,
            favorite: updated.favorite
        };
        toast(updated.favorite ? "Bookmarked." : "Bookmark removed.", "success");
        await loadProgress();
    } catch (error) {
        toast(error.message, "error");
    }
}

async function loadProgress() {
    const user = getUser();
    if (!user) {
        return;
    }
    try {
        const progress = await api(`/progress?userId=${encodeURIComponent(user.userId)}`);
        renderProgress(progress);
    } catch {
        renderProgress({
            totalExplanations: 0,
            favoriteCount: 0,
            learningScore: 0,
            recentTopics: [],
            recommendedTopics: fallbackSuggestions,
            beginnerCount: 0,
            intermediateCount: 0,
            advancedCount: 0
        });
    }
}

function renderProgress(progress) {
    const cards = document.querySelector("[data-progress-cards]");
    if (cards) {
        cards.innerHTML = `
            <article class="metric-card"><span>${progress.totalExplanations}</span><small>Explanations</small></article>
            <article class="metric-card"><span>${progress.favoriteCount}</span><small>Bookmarks</small></article>
            <article class="metric-card"><span>${progress.learningScore}</span><small>Score</small></article>
        `;
    }

    const recent = document.querySelector("[data-recent-searches]");
    if (recent) {
        const topics = progress.recentTopics?.length ? progress.recentTopics : ["No searches yet"];
        recent.innerHTML = topics.map((topic) => `<button type="button" class="mini-item" data-topic="${escapeHtml(topic)}">${escapeHtml(topic)}</button>`).join("");
        recent.querySelectorAll("[data-topic]").forEach((button) => {
            button.addEventListener("click", () => {
                const input = document.querySelector("[name='topic']");
                const chatInput = document.querySelector("[data-chat-input]");
                const targetInput = chatInput || input;
                if (targetInput && button.dataset.topic !== "No searches yet") {
                    targetInput.value = `Explain ${button.dataset.topic}`;
                    targetInput.focus();
                }
                if (input && !chatInput && button.dataset.topic !== "No searches yet") {
                    input.focus();
                }
            });
        });
    }

    const detail = document.querySelector("[data-progress-detail]");
    if (detail) {
        const total = Math.max(1, progress.totalExplanations || 0);
        detail.innerHTML = `
            ${progressBar("Beginner", progress.beginnerCount, total)}
            ${progressBar("Intermediate", progress.intermediateCount, total)}
            ${progressBar("Advanced", progress.advancedCount, total)}
        `;
    }
}

function progressBar(label, value, total) {
    const percent = Math.round((value / total) * 100);
    return `
        <div class="progress-row">
            <div><span>${label}</span><strong>${value}</strong></div>
            <div class="progress-track"><span style="width:${percent}%"></span></div>
        </div>
    `;
}

function initHistory() {
    const list = document.querySelector("[data-history-list]");
    if (!list) {
        return;
    }

    const searchInput = document.querySelector("[data-history-search]");
    const favoriteFilter = document.querySelector("[data-favorite-filter]");
    let searchTimer;
    loadHistory();

    searchInput?.addEventListener("input", () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => {
            state.historyPage = 0;
            loadHistory();
        }, 250);
    });
    favoriteFilter?.addEventListener("change", () => {
        state.historyPage = 0;
        loadHistory();
    });
}

async function loadHistory() {
    const user = getUser();
    const list = document.querySelector("[data-history-list]");
    const search = document.querySelector("[data-history-search]")?.value || "";
    const favoriteOnly = document.querySelector("[data-favorite-filter]")?.checked || false;
    const params = new URLSearchParams({
        userId: user.userId,
        page: state.historyPage,
        size: state.historySize,
        favoriteOnly
    });
    if (search.trim()) {
        params.set("search", search.trim());
    }

    list.innerHTML = `${skeletonCard()}${skeletonCard()}`;
    try {
        const page = await api(`/history?${params.toString()}`);
        renderHistory(page.content || []);
        renderPagination(page);
    } catch (error) {
        list.innerHTML = "<div class='empty-state'>History could not be loaded.</div>";
        toast(error.message, "error");
    }
}

function renderHistory(items) {
    const list = document.querySelector("[data-history-list]");
    if (!items.length) {
        list.innerHTML = "<div class='empty-state'>No explanations found yet.</div>";
        return;
    }

    list.innerHTML = "";
    items.forEach((item) => {
        const card = document.createElement("article");
        card.className = "history-card";
        card.innerHTML = `
            <div class="history-card-head">
                <div>
                    <h3>${escapeHtml(item.topic)}</h3>
                    <div class="history-meta">
                        <span>${escapeHtml(item.level)}</span>
                        <span>${escapeHtml(item.explanationType)}</span>
                        <span>${escapeHtml(item.detectedDomain || "General")}</span>
                        <span>${formatDate(item.createdAt)}</span>
                    </div>
                </div>
                <span class="favorite-dot ${item.favorite ? "active" : ""}" aria-label="${item.favorite ? "Bookmarked" : "Not bookmarked"}"></span>
            </div>
            <div class="history-preview">${formatExplanationPreview(item.content)}</div>
            <div class="history-actions">
                <button class="ghost-btn" data-copy-history="${item.explanationId}">Copy</button>
                <button class="ghost-btn" data-export-history="${item.explanationId}">PDF</button>
                <button class="ghost-btn" data-favorite-history="${item.explanationId}">${item.favorite ? "Unbookmark" : "Bookmark"}</button>
                <button class="danger-btn" data-delete-history="${item.queryId}">Delete</button>
            </div>
        `;
        card.querySelector("[data-copy-history]").addEventListener("click", () => copyText(item.content));
        card.querySelector("[data-export-history]").addEventListener("click", () => exportExplanation(item));
        card.querySelector("[data-favorite-history]").addEventListener("click", () => toggleFavorite(item.explanationId));
        card.querySelector("[data-delete-history]").addEventListener("click", () => deleteHistory(item.queryId));
        list.appendChild(card);
    });
}

function renderPagination(page) {
    const holder = document.querySelector("[data-pagination]");
    if (!holder) {
        return;
    }
    if (!page || page.totalPages <= 1) {
        holder.innerHTML = "";
        return;
    }
    holder.innerHTML = `
        <button class="ghost-btn" type="button" data-prev ${page.page <= 0 ? "disabled" : ""}>Previous</button>
        <span>Page ${page.page + 1} of ${page.totalPages}</span>
        <button class="ghost-btn" type="button" data-next ${page.last ? "disabled" : ""}>Next</button>
    `;
    holder.querySelector("[data-prev]")?.addEventListener("click", () => {
        state.historyPage = Math.max(0, state.historyPage - 1);
        loadHistory();
    });
    holder.querySelector("[data-next]")?.addEventListener("click", () => {
        state.historyPage += 1;
        loadHistory();
    });
}

async function toggleFavorite(explanationId) {
    const user = getUser();
    try {
        await api(`/favorites/${explanationId}?userId=${encodeURIComponent(user.userId)}`, { method: "POST" });
        toast("Bookmark updated.", "success");
        await loadHistory();
    } catch (error) {
        toast(error.message, "error");
    }
}

async function deleteHistory(queryId) {
    const user = getUser();
    if (!window.confirm("Delete this explanation from history?")) {
        return;
    }
    try {
        await api(`/history/${queryId}?userId=${encodeURIComponent(user.userId)}`, { method: "DELETE" });
        toast("History item deleted.", "success");
        await loadHistory();
    } catch (error) {
        toast(error.message, "error");
    }
}

function exportExplanation(item) {
    if (!item) {
        toast("Generate or select an explanation before exporting.", "error");
        return;
    }
    const printable = window.open("", "_blank", "width=900,height=700");
    if (!printable) {
        toast("Popup blocked. Allow popups to export.", "error");
        return;
    }
    printable.document.write(`
        <!doctype html>
        <html>
        <head>
            <title>${escapeHtml(item.topic)} | ConceptClarity</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 36px; color: #172033; line-height: 1.65; }
                h1 { margin-bottom: 4px; }
                .meta { color: #5d6b82; margin-bottom: 28px; }
                pre { white-space: pre-wrap; font-family: inherit; }
            </style>
        </head>
        <body>
            <h1>${escapeHtml(item.topic)}</h1>
            <div class="meta">${escapeHtml(item.level)} | ${escapeHtml(item.explanationType || "Auto")} | ConceptClarity</div>
            <pre>${escapeHtml(responseText(item))}</pre>
        </body>
        </html>
    `);
    printable.document.close();
    printable.focus();
    printable.print();
}

async function api(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        },
        ...options
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data?.message || "Request failed.");
    }
    return data;
}

function getUser() {
    try {
        return JSON.parse(localStorage.getItem("cc-user"));
    } catch {
        return null;
    }
}

function setLoading(loader, button, isLoading) {
    loader?.classList.toggle("active", isLoading);
    if (button) {
        button.disabled = isLoading;
        const isSendButton = button.matches("[data-send-button]");
        button.textContent = isLoading ? (isSendButton ? "Thinking..." : "Generating...") : (isSendButton ? "Send" : "Generate");
    }
}

function responseText(item) {
    return item?.reply || item?.content || "";
}

async function copyText(text) {
    try {
        await navigator.clipboard.writeText(text);
        toast("Copied.", "success");
    } catch {
        toast("Clipboard access was blocked by the browser.", "error");
    }
}

function toast(message, type = "success") {
    const wrap = ensureToastWrap();
    const item = document.createElement("div");
    item.className = `toast ${type}`;
    item.textContent = message;
    wrap.appendChild(item);
    setTimeout(() => item.remove(), 3200);
}

function ensureToastWrap() {
    let wrap = document.querySelector(".toast-wrap");
    if (!wrap) {
        wrap = document.createElement("div");
        wrap.className = "toast-wrap";
        document.body.appendChild(wrap);
    }
    return wrap;
}

function skeletonCard() {
    return "<article class='history-card skeleton'></article>";
}

function formatExplanationPreview(text) {
    const clean = escapeHtml(text).replaceAll("\n", " ");
    return clean.length > 520 ? `${clean.slice(0, 520)}...` : clean;
}

function isEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatDate(value) {
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(value));
}
