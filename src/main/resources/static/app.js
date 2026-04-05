const SESSION_USER_ID_KEY = "coderia.chat.userId";

const chatWindow = document.getElementById("chat-window");
const chatForm = document.getElementById("chat-form");
const chatInput = document.getElementById("chat-input");
const template = document.getElementById("message-template");
const quickButtons = [...document.querySelectorAll("[data-quick]")];

const state = {
  userId: loadOrCreateUserId(),
  busy: false,
  memory: {
    bookingNumber: "",
    customerName: "",
    customerSurname: "",
    userId: ""
  }
};

function loadOrCreateUserId() {
  const existing = localStorage.getItem(SESSION_USER_ID_KEY);
  if (existing) {
    return existing;
  }
  const generated = `user-${Math.random().toString(36).slice(2, 10)}`;
  localStorage.setItem(SESSION_USER_ID_KEY, generated);
  return generated;
}

function scrollDown() {
  chatWindow.scrollTop = chatWindow.scrollHeight;
}

function appendMessage(author, content, kind = "bot") {
  const node = template.content.cloneNode(true);
  const article = node.querySelector(".msg");
  const authorEl = node.querySelector(".msg-author");
  const bodyEl = node.querySelector(".msg-body");

  article.classList.add(kind);
  authorEl.textContent = author;

  if (content instanceof HTMLElement) {
    bodyEl.appendChild(content);
  } else {
    bodyEl.textContent = content;
  }

  chatWindow.appendChild(node);
  scrollDown();
}

function appendJson(author, title, data, isError = false) {
  const wrapper = document.createElement("div");
  const p = document.createElement("p");
  p.className = isError ? "tag-err" : "tag-ok";
  p.textContent = title;
  const pre = document.createElement("pre");
  pre.textContent = JSON.stringify(data, null, 2);
  wrapper.appendChild(p);
  wrapper.appendChild(pre);
  appendMessage(author, wrapper);
}

function appendResult(author, data) {
  const node = buildResultNode(data);
  if (node) {
    appendMessage(author, node);
    return;
  }
  appendJson(author, "Resultat", data);
}

function buildResultNode(data) {
  if (!data || typeof data !== "object") {
    return null;
  }
  if (Array.isArray(data.bookings)) {
    const wrapper = document.createElement("div");
    const title = document.createElement("p");
    title.className = "tag-ok";
    title.textContent = `Reservations (${data.count ?? data.bookings.length})`;
    wrapper.appendChild(title);
    data.bookings.forEach((booking) => wrapper.appendChild(buildBookingCard(booking)));
    return wrapper;
  }
  if (data.bookingNumber && data.customerName && data.customerSurname) {
    const wrapper = document.createElement("div");
    const title = document.createElement("p");
    title.className = "tag-ok";
    title.textContent = "Reservation";
    wrapper.appendChild(title);
    wrapper.appendChild(buildBookingCard(data));
    return wrapper;
  }
  if (data.precheckToken && data.expiresAt) {
    const wrapper = document.createElement("div");
    const title = document.createElement("p");
    title.className = "tag-ok";
    title.textContent = "Precheck annulation";
    const card = document.createElement("div");
    card.className = "result-card";
    card.innerHTML = `
      <p><strong>Token:</strong> ${escapeHtml(data.precheckToken)}</p>
      <p><strong>Expire le:</strong> ${escapeHtml(data.expiresAt)}</p>
    `;
    wrapper.appendChild(title);
    wrapper.appendChild(card);
    return wrapper;
  }
  return null;
}

function buildBookingCard(booking) {
  const card = document.createElement("div");
  card.className = "result-card";
  const status = booking.status || "-";
  card.innerHTML = `
    <p><strong>Numero:</strong> ${escapeHtml(booking.bookingNumber || "-")}</p>
    <p><strong>Client:</strong> ${escapeHtml(`${booking.customerName || ""} ${booking.customerSurname || ""}`.trim() || "-")}</p>
    <p><strong>Debut:</strong> ${escapeHtml(booking.bookingBeginDate || "-")}</p>
    <p><strong>Fin:</strong> ${escapeHtml(booking.bookingEndDate || "-")}</p>
    <p><strong>Statut:</strong> <span class="status-pill">${escapeHtml(status)}</span></p>
  `;
  return card;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

async function httpJson(url, options = {}) {
  const response = await fetch(url, options);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data?.message || `HTTP ${response.status}`);
  }
  return data;
}

function mergeMemory(memory) {
  if (!memory) {
    return;
  }
  state.memory = {
    bookingNumber: memory.bookingNumber || state.memory.bookingNumber,
    customerName: memory.customerName || state.memory.customerName,
    customerSurname: memory.customerSurname || state.memory.customerSurname,
    userId: memory.userId || state.userId
  };
  if (state.memory.userId) {
    state.userId = state.memory.userId;
    localStorage.setItem(SESSION_USER_ID_KEY, state.userId);
  }
}

function buildActionForm(action) {
  const form = document.createElement("form");
  form.className = "inline-form";

  (action.fields || []).forEach((field) => {
    const type = field.type || "text";
    const options = Array.isArray(field.options) ? field.options : [];

    if (type === "hidden") {
      const input = document.createElement("input");
      input.type = "hidden";
      input.name = field.name;
      input.value = field.value || "";
      form.appendChild(input);
      return;
    }

    const input = createInputControl(field, type, options);
    const label = document.createElement("label");
    label.textContent = field.label || field.name;
    label.appendChild(input);
    form.appendChild(label);
  });

  const submit = document.createElement("button");
  submit.type = "submit";
  submit.className = "full";
  submit.textContent = action.submitLabel || "Envoyer";
  form.appendChild(submit);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = Object.fromEntries(new FormData(form));
    try {
      await runTurn("", formData);
    } catch (error) {
      appendJson("Assistant", "Erreur", { message: error.message }, true);
    }
  });

  return form;
}

function createInputControl(field, type, options) {
  let input;
  if (type === "textarea") {
    input = document.createElement("textarea");
    input.rows = 3;
  } else if (type === "select") {
    input = document.createElement("select");
    const empty = document.createElement("option");
    empty.value = "";
    empty.textContent = "--";
    input.appendChild(empty);
    options.forEach((value) => {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = value;
      input.appendChild(option);
    });
  } else {
    input = document.createElement("input");
    input.type = type;
  }
  input.name = field.name;
  input.required = Boolean(field.required);
  input.placeholder = field.placeholder || "";
  input.value = field.value || "";
  return input;
}

function handleTurnResponse(response) {
  mergeMemory(response.memory);

  if (response.assistantMessage) {
    appendMessage("Assistant", response.assistantMessage);
  }

  if (response.result && Object.keys(response.result).length > 0) {
    appendResult("Assistant", response.result);
  }

  if (response.action && response.action.type === "ASK_FIELDS") {
    appendMessage("Assistant", buildActionForm(response.action), "form");
  }
}

async function runTurn(message, formData = {}) {
  setBusy(true);
  const payload = {
    userId: state.userId,
    message,
    formData
  };
  try {
    const response = await httpJson("/api/v1/chat/turn", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    handleTurnResponse(response);
  } finally {
    setBusy(false);
  }
}

function setBusy(value) {
  state.busy = value;
  chatInput.disabled = value;
  chatForm.querySelector("button[type='submit']").disabled = value;
  quickButtons.forEach((btn) => {
    btn.disabled = value;
  });
}

async function loadSession() {
  try {
    const session = await httpJson(`/api/v1/chat/session/${encodeURIComponent(state.userId)}`);
    mergeMemory(session.memory);
    if (Array.isArray(session.history) && session.history.length > 0) {
      appendMessage("Assistant", "Session precedente restauree.");
      session.history.slice(-8).forEach((turn) => {
        if (turn.role === "user") {
          appendMessage("Vous", turn.message || "", "user");
        } else {
          appendMessage("Assistant", turn.message || "");
        }
      });
    }
  } catch (error) {
    appendMessage("Assistant", "Nouvelle session demarree.");
  }
}

chatForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const text = chatInput.value.trim();
  if (!text) {
    return;
  }
  appendMessage("Vous", text, "user");
  chatInput.value = "";
  try {
    await runTurn(text, {});
  } catch (error) {
    appendJson("Assistant", "Erreur", { message: error.message }, true);
  }
});

quickButtons.forEach((button) => {
  button.addEventListener("click", async () => {
    const text = button.getAttribute("data-quick") || "";
    appendMessage("Vous", text, "user");
    try {
      await runTurn(text, {});
    } catch (error) {
      appendJson("Assistant", "Erreur", { message: error.message }, true);
    }
  });
});

appendMessage("Assistant", "Bonjour. Je pilote les formulaires dynamiquement selon votre demande.");
loadSession();
