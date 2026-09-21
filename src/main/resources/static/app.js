const RECENT_KEY = "jobTracker.recentIds";
const MAX_RECENT = 10;

const createForm = document.getElementById("create-form");
const createResult = document.getElementById("create-result");
const lookupForm = document.getElementById("lookup-form");
const lookupResult = document.getElementById("lookup-result");
const recentList = document.getElementById("recent-list");
const filterForm = document.getElementById("filter-form");
const filterSubmitButton = filterForm.querySelector('button[type="submit"]');
const clearFiltersButton = document.getElementById("clear-filters-button");
const allResult = document.getElementById("all-result");
const allList = document.getElementById("all-list");
const allPagination = document.getElementById("all-pagination");
const prevPageButton = document.getElementById("prev-page-button");
const nextPageButton = document.getElementById("next-page-button");
const pageIndicator = document.getElementById("page-indicator");

const PAGE_SIZE = 10;
let currentPage = 0;

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

filterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    loadPage(0);
});

clearFiltersButton.addEventListener("click", () => {
    filterForm.reset();
    loadPage(0);
});

prevPageButton.addEventListener("click", () => {
    loadPage(currentPage - 1);
});

nextPageButton.addEventListener("click", () => {
    loadPage(currentPage + 1);
});

function currentFilterParams() {
    const formData = new FormData(filterForm);
    const params = new URLSearchParams();
    for (const [key, value] of formData.entries()) {
        if (value.trim() !== "") {
            params.set(key, value.trim());
        }
    }
    return params;
}

async function loadPage(page) {
    filterSubmitButton.disabled = true;
    clearFiltersButton.disabled = true;
    prevPageButton.disabled = true;
    nextPageButton.disabled = true;
    allResult.hidden = true;
    try {
        const params = currentFilterParams();
        params.set("page", page);
        params.set("size", PAGE_SIZE);
        const response = await fetch(`/api/job-applications?${params.toString()}`);

        if (!response.ok) {
            allList.innerHTML = "";
            allPagination.hidden = true;
            showResult(allResult, "error", await extractErrorMessage(response));
            return;
        }

        const pageResponse = await response.json();
        currentPage = pageResponse.page;
        renderAll(pageResponse.content);
        renderPagination(pageResponse);
    } catch (err) {
        allList.innerHTML = "";
        allPagination.hidden = true;
        showResult(allResult, "error", "Could not reach the server.");
    } finally {
        filterSubmitButton.disabled = false;
        clearFiltersButton.disabled = false;
    }
}

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

function renderPagination(pageResponse) {
    if (pageResponse.totalElements === 0) {
        allPagination.hidden = true;
        return;
    }
    allPagination.hidden = false;
    pageIndicator.textContent = `Page ${pageResponse.page + 1} of ${pageResponse.totalPages} (${pageResponse.totalElements} total)`;
    prevPageButton.disabled = pageResponse.page <= 0;
    nextPageButton.disabled = pageResponse.page >= pageResponse.totalPages - 1;
}

renderRecent();
