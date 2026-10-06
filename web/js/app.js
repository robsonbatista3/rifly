// CONSTANTES COM CREDENCIAIS DO SUPABASE
const SUPABASE_URL = "https://seiewvnsnbzmvjlgqrjr.supabase.co";
const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNlaWV3dm5zbmJ6bXZqbGdxcmpyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA5NzczNTcsImV4cCI6MjEwNjU1MzM1N30.kGubqqpB_0ZQevyT0S9QhaKcH_QTNgA-N_nulOdo930";

const supabase = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ESTADO GLOBAL
let raffle = null;
let creator = null;
let reservations = [];
let prizes = [];
let selected = [];
let countdownInterval = null;
const MAX_NUMBERS = 20;

function getSlugFromUrl() {
    const params = new URLSearchParams(window.location.search);
    return params.get("rifa");
}

async function init() {
    const slug = getSlugFromUrl();
    if (!slug) {
        showError("Link de rifa inválido.");
        return;
    }

    try {
        await loadRaffle(slug);
        await loadReservations();
        await loadCreator();
        await loadPrizes();
        renderAll();
        setInterval(refreshReservations, 15000);
    } catch (e) {
        showError(e.message || "Erro ao carregar a rifa.");
    }
}

function showError(msg) {
    document.getElementById("loading").classList.add("hidden");
    document.getElementById("raffle-content").classList.add("hidden");
    const errDiv = document.getElementById("error");
    document.getElementById("error-message").innerText = msg;
    errDiv.classList.remove("hidden");
}

async function loadRaffle(slug) {
    const { data, error } = await supabase
        .from("raffles")
        .select("*")
        .eq("slug", slug)
        .single();

    if (error || !data) {
        throw new Error("Rifa não encontrada ou desativada 😕");
    }
    raffle = data;
}

async function loadReservations() {
    if (!raffle) return;
    const { data, error } = await supabase
        .from("reservations")
        .select("*")
        .eq("raffle_id", raffle.id)
        .in("status", ["PENDING", "CONFIRMED"]);

    if (!error && data) {
        reservations = data;
    }
}

async function loadCreator() {
    if (!raffle || !raffle.creator_id) return;
    try {
        const { data } = await supabase
            .from("profiles")
            .select("name, phone")
            .eq("id", raffle.creator_id)
            .single();
        if (data) {
            creator = data;
        }
    } catch (e) {
        // ignora falha silenciosamente
    }
}

async function loadPrizes() {
    if (!raffle) return;
    try {
        const { data } = await supabase
            .from("prizes")
            .select("*")
            .eq("raffle_id", raffle.id)
            .order("position", { ascending: true });
        if (data && data.length > 0) {
            prizes = data;
        }
    } catch (e) {
        // ignora falha silenciosamente
    }
}

function getTakenNumbers() {
    const reserved = new Set();
    const sold = new Set();

    reservations.forEach(r => {
        if (Array.isArray(r.numbers)) {
            r.numbers.forEach(n => {
                if (r.status === "CONFIRMED") {
                    sold.add(n);
                } else if (r.status === "PENDING") {
                    reserved.add(n);
                }
            });
        }
    });

    return { reserved, sold };
}

function formatBRL(val) {
    return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    }).format(val || 0);
}

function renderAll() {
    document.getElementById("loading").classList.add("hidden");
    document.getElementById("error").classList.add("hidden");

    if (raffle.status === "CANCELLED") {
        showError("Esta rifa foi cancelada pelo organizador.");
        return;
    }

    document.getElementById("raffle-content").classList.remove("hidden");

    document.getElementById("raffle-title").innerText = raffle.title;

    const prizeEl = document.getElementById("raffle-prize");
    if (prizes.length > 0) {
        const medals = ["🥇", "🥈", "🥉", "🏅", "🏅"];
        prizeEl.innerHTML = prizes.map((p, idx) =>
            `<div>${medals[idx] || "🏅"} ${p.position}º prêmio: ${p.prize_name}</div>`
        ).join("");
    } else {
        prizeEl.innerText = `🏆 Prêmio: ${raffle.prize}`;
    }

    document.getElementById("raffle-price").innerText = `${formatBRL(raffle.price_per_ticket)} / número`;
    document.getElementById("raffle-description").innerText = raffle.description || "";

    const imgEl = document.getElementById("raffle-image");
    if (raffle.image_url) {
        imgEl.src = raffle.image_url;
        imgEl.classList.remove("hidden");
    } else {
        imgEl.classList.add("hidden");
    }

    updateStats();

    // Banner de sorteio realizado
    const drawnBanner = document.getElementById("drawn-banner");
    if (raffle.status === "DRAWN") {
        drawnBanner.classList.remove("hidden");
        if (prizes.length > 0 && prizes.some(p => p.winner_number !== null)) {
            const medals = ["🥇", "🥈", "🥉", "🏅", "🏅"];
            const lines = prizes.map((p, idx) => {
                const num = p.winner_number ? `#${String(p.winner_number).padStart(2, "0")}` : "—";
                const name = p.winner_name || "N/A";
                return `<div>${medals[idx] || "🏅"} ${p.position}º lugar (${p.prize_name}): ${num} — ${name}</div>`;
            }).join("");
            document.getElementById("drawn-info").innerHTML = lines;
        } else {
            document.getElementById("drawn-info").innerText =
                `Número vencedor: #${String(raffle.winner_number || 0).padStart(2, "0")} - Ganhador: ${raffle.winner_name || "N/A"}`;
        }
    } else {
        drawnBanner.classList.add("hidden");
    }

    // Contagem Regressiva
    const countdownBanner = document.getElementById("countdown-banner");
    if (countdownInterval) clearInterval(countdownInterval);

    if (raffle.draw_date && raffle.status === "ACTIVE") {
        if (countdownBanner) countdownBanner.classList.remove("hidden");

        const updateCountdown = () => {
            const datePart = String(raffle.draw_date).split("T")[0];
            const target = new Date(datePart + "T23:59:59").getTime();
            const diff = target - Date.now();

            if (diff > 0) {
                const dias = Math.floor(diff / 86400000);
                const horas = Math.floor((diff % 86400000) / 3600000);
                const min = Math.floor((diff % 3600000) / 60000);
                const seg = Math.floor((diff % 60000) / 1000);

                const pad = (n) => String(n).padStart(2, "0");
                if (countdownBanner) {
                    countdownBanner.innerText = `⏰ Sorteio em: ${dias}d ${pad(horas)}h ${pad(min)}m ${pad(seg)}s`;
                }
            } else {
                if (countdownBanner) {
                    countdownBanner.innerText = "⏰ Hora do sorteio chegou!";
                }
                if (countdownInterval) clearInterval(countdownInterval);
            }
        };

        updateCountdown();
        countdownInterval = setInterval(updateCountdown, 1000);
    } else {
        if (countdownBanner) countdownBanner.classList.add("hidden");
    }

    renderGrid();
}

function updateStats() {
    const taken = getTakenNumbers();
    const soldCount = taken.sold.size;
    const total = raffle.total_numbers;
    const percentage = total > 0 ? Math.round((soldCount / total) * 100) : 0;

    document.getElementById("stats-counter").innerText = `${soldCount} de ${total} vendidos`;
    document.getElementById("stats-percentage").innerText = `${percentage}%`;
    document.getElementById("progress-fill").style.width = `${percentage}%`;
}

function renderGrid() {
    const grid = document.getElementById("number-grid");
    grid.innerHTML = "";

    const taken = getTakenNumbers();
    const totalDigits = Math.max(2, String(raffle.total_numbers).length);

    for (let n = 1; n <= raffle.total_numbers; n++) {
        const btn = document.createElement("button");
        btn.classList.add("num-btn");
        btn.innerText = String(n).padStart(totalDigits, "0");

        if (taken.sold.has(n)) {
            btn.classList.add("sold");
            btn.disabled = true;
        } else if (taken.reserved.has(n)) {
            btn.classList.add("reserved");
            btn.disabled = true;
        } else if (selected.includes(n)) {
            btn.classList.add("selected");
            btn.onclick = () => toggleNumber(n);
        } else {
            btn.classList.add("available");
            if (raffle.status !== "DRAWN") {
                btn.onclick = () => toggleNumber(n);
            } else {
                btn.disabled = true;
            }
        }

        grid.appendChild(btn);
    }
}

function toggleNumber(n) {
    if (selected.includes(n)) {
        selected = selected.filter(x => x !== n);
    } else {
        if (selected.length >= MAX_NUMBERS) {
            alert(`Você pode selecionar no máximo ${MAX_NUMBERS} números por vez.`);
            return;
        }
        selected.push(n);
    }
    renderGrid();
    renderBottomBar();
}

function renderBottomBar() {
    const bar = document.getElementById("bottom-bar");
    if (selected.length === 0 || raffle.status === "DRAWN") {
        bar.classList.add("hidden");
        return;
    }

    bar.classList.remove("hidden");

    const chipsContainer = document.getElementById("selected-chips");
    chipsContainer.innerHTML = "";

    const totalDigits = Math.max(2, String(raffle.total_numbers).length);
    selected.sort((a, b) => a - b).forEach(n => {
        const chip = document.createElement("div");
        chip.classList.add("chip");
        chip.innerText = `#${String(n).padStart(totalDigits, "0")}`;
        chip.onclick = () => toggleNumber(n);
        chipsContainer.appendChild(chip);
    });

    const total = selected.length * raffle.price_per_ticket;
    document.getElementById("total-value").innerText = formatBRL(total);
}

function openFormModal() {
    document.getElementById("form-error").classList.add("hidden");
    document.getElementById("modal-form").classList.remove("hidden");
}

function closeFormModal() {
    document.getElementById("modal-form").classList.add("hidden");
}

async function confirmReservation() {
    const nameInput = document.getElementById("buyer-name").value.trim();
    const phoneInput = document.getElementById("buyer-phone").value.trim();
    const errorEl = document.getElementById("form-error");

    const cleanPhone = phoneInput.replace(/\D/g, "");

    if (nameInput.length < 3) {
        errorEl.innerText = "Por favor, digite seu nome completo.";
        errorEl.classList.remove("hidden");
        return;
    }

    if (cleanPhone.length < 10) {
        errorEl.innerText = "Digite um número de WhatsApp válido com DDD.";
        errorEl.classList.remove("hidden");
        return;
    }

    errorEl.classList.add("hidden");

    // Re-checa disponibilidade
    await loadReservations();
    const taken = getTakenNumbers();
    const unavailableSelected = selected.filter(n => taken.sold.has(n) || taken.reserved.has(n));

    if (unavailableSelected.length > 0) {
        alert(`Alguns números escolhidos já foram reservados: ${unavailableSelected.join(", ")}. Por favor, escolha outros.`);
        selected = selected.filter(n => !unavailableSelected.includes(n));
        closeFormModal();
        renderGrid();
        renderBottomBar();
        return;
    }

    const total = selected.length * raffle.price_per_ticket;
    const reservedNumbers = [...selected];

    const { data: newReservation, error } = await supabase
        .from("reservations")
        .insert({
            raffle_id: raffle.id,
            buyer_name: nameInput,
            buyer_phone: cleanPhone,
            numbers: reservedNumbers,
            total_amount: total,
            status: "PENDING"
        })
        .select()
        .single();

    if (error) {
        alert("Erro ao realizar reserva: " + error.message);
        return;
    }

    // Tenta notificar o criador
    try {
        const totalDigits = Math.max(2, String(raffle.total_numbers).length);
        const numbersFormatted = reservedNumbers.map(n => `#${String(n).padStart(totalDigits, "0")}`).join(", ");
        await supabase.from("notifications").insert({
            user_id: raffle.creator_id,
            raffle_id: raffle.id,
            title: "🎟️ Nova reserva!",
            message: `${nameInput} reservou o(s) número(s) ${numbersFormatted} - ${formatBRL(total)}`,
            type: "new_reservation"
        });
    } catch (e) {
        // Ignora erro na notificação
    }

    // Sucesso
    closeFormModal();

    const totalDigits = Math.max(2, String(raffle.total_numbers).length);
    const numbersFormatted = reservedNumbers.map(n => `#${String(n).padStart(totalDigits, "0")}`).join(", ");

    document.getElementById("success-numbers").innerText = `Números: ${numbersFormatted}`;
    document.getElementById("pix-key").innerText = raffle.pix_key || "Não cadastrada";
    document.getElementById("pix-value").innerText = `Total: ${formatBRL(total)}`;

    // WhatsApp do criador
    const creatorPhone = creator ? creator.phone : (raffle.pix_name || "");
    const cleanCreatorPhone = String(creatorPhone).replace(/\D/g, "");
    const msg = `Olá! Acabei de reservar o(s) número(s) ${numbersFormatted} na rifa "${raffle.title}". Segue o comprovante do Pix no valor de ${formatBRL(total)}:`;
    const waUrl = `https://wa.me/55${cleanCreatorPhone}?text=${encodeURIComponent(msg)}`;

    const waBtn = document.getElementById("btn-whatsapp-creator");
    waBtn.href = waUrl;

    document.getElementById("modal-success").classList.remove("hidden");

    selected = [];
    await loadReservations();
    renderGrid();
    renderBottomBar();
    updateStats();
}

function refreshReservations() {
    if (!raffle) return;
    loadReservations().then(() => {
        const taken = getTakenNumbers();
        selected = selected.filter(n => !taken.sold.has(n) && !taken.reserved.has(n));
        renderGrid();
        renderBottomBar();
        updateStats();
    });
}

// EVENTOS
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("btn-buy").addEventListener("click", openFormModal);
    document.getElementById("btn-confirm").addEventListener("click", confirmReservation);
    document.getElementById("btn-cancel-form").addEventListener("click", closeFormModal);

    document.getElementById("btn-copy").addEventListener("click", () => {
        const pixKey = document.getElementById("pix-key").innerText;
        navigator.clipboard.writeText(pixKey).then(() => {
            const btn = document.getElementById("btn-copy");
            const originalText = btn.innerText;
            btn.innerText = "✅ Copiado!";
            setTimeout(() => {
                btn.innerText = originalText;
            }, 2000);
        });
    });

    init();
});
