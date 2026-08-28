const RECENT_KEY = "jobTracker.recentIds";
const MAX_RECENT = 10;

const createForm = document.getElementById("create-form");
const createResult = document.getElementById("create-result");
const lookupForm = document.getElementById("lookup-form");
const lookupResult = document.getElementById("lookup-result");
const recentList = document.getElementById("recent-list");
const loadAllButton = document.getElementById("load-all-button");
const allResult = document.getElementById("all-result");
const allList = document.getElementById("all-list");

function showResult(el, kind, html) {
    el.className = "result " + kind;
    el.innerHTML = html;
    el.hidden = false;
}

function applicationDetailsHtml(app) {
    return `<dl>
        <dt>ID</dt><dd>${app.id}</dd>
        <dt>Company</dt><dd>${escapeHtml(app.company)}</dd>
        <dt>Position</dt><dd>${escapeHtml(app.position)}</dd>
        <dt>Applied</dt><dd>${app.appliedDate}</dd>
    </dl>`;
}

function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value;
    return div.innerHTML;
}

async function extractErrorMessage(response) {
    try {
        const body = await response.json();
        if (body.errors && Array.isArray(body.errors)) {
            return body.errors.map(e => e.defaultMessage || e.field).join(", ");
        }
        return body.detail || body.message || `Request failed (${response.status})`;
    } catch {
        return `Request failed (${response.status})`;
    }
}

function loadRecent() {
    try {
        return JSON.parse(localStorage.getItem(RECENT_KEY)) || [];
    } catch {
        return [];
    }
}

function saveRecent(app) {
    const recent = loadRecent().filter(r => r.id !== app.id);
    recent.unshift(app);
    localStorage.setItem(RECENT_KEY, JSON.stringify(recent.slice(0, MAX_RECENT)));
    renderRecent();
}

function renderRecent() {
    const recent = loadRecent();
    if (recent.length === 0) {
        recentList.innerHTML = `<li class="empty">Nothing yet — add an application above.</li>`;
        return;
    }
    recentList.innerHTML = recent.map(app => `
        <li>
            <span>${escapeHtml(app.company)} — ${escapeHtml(app.position)}</span>
            <span class="recent-id">#${app.id}</span>
        </li>
    `).join("");
}

createForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(createForm);
    const payload = {
        company: formData.get("company"),
        position: formData.get("position"),
        appliedDate: formData.get("appliedDate"),
    };

    const submitButton = createForm.querySelector("button");
    submitButton.disabled = true;
    try {
        const response = await fetch("/api/job-applications", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            showResult(createResult, "error", await extractErrorMessage(response));
            return;
        }

        const app = await response.json();
        showResult(createResult, "success", `Added application.${applicationDetailsHtml(app)}`);
        saveRecent(app);
        createForm.reset();
    } catch (err) {
        showResult(createResult, "error", "Could not reach the server.");
    } finally {
        submitButton.disabled = false;
    }
});

lookupForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const id = document.getElementById("lookup-id").value;

    const submitButton = lookupForm.querySelector("button");
    submitButton.disabled = true;
    try {
        const response = await fetch(`/api/job-applications/${encodeURIComponent(id)}`);

        if (!response.ok) {
            showResult(lookupResult, "error", await extractErrorMessage(response));
            return;
        }

        const app = await response.json();
        showResult(lookupResult, "success", applicationDetailsHtml(app));
    } catch (err) {
        showResult(lookupResult, "error", "Could not reach the server.");
    } finally {
        submitButton.disabled = false;
    }
});

loadAllButton.addEventListener("click", async () => {
    loadAllButton.disabled = true;
    allResult.hidden = true;
    try {
        const response = await fetch("/api/job-applications");

        if (!response.ok) {
            allList.innerHTML = "";
            showResult(allResult, "error", await extractErrorMessage(response));
            return;
        }

        const apps = await response.json();
        renderAll(apps);
    } catch (err) {
        allList.innerHTML = "";
        showResult(allResult, "error", "Could not reach the server.");
    } finally {
        loadAllButton.disabled = false;
    }
});

function renderAll(apps) {
    if (apps.length === 0) {
        allList.innerHTML = `<li class="empty">No applications yet.</li>`;
        return;
    }
    allList.innerHTML = apps.map(app => `
        <li>
            <span>${escapeHtml(app.company)} — ${escapeHtml(app.position)} (${app.appliedDate})</span>
            <span class="recent-id">#${app.id}</span>
        </li>
    `).join("");
}

renderRecent();
