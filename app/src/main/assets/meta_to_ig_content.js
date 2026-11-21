(function () {
    if (window.__arafatInstaAutoInjected) {
        return;
    }
    window.__arafatInstaAutoInjected = true;

    // ==========================================
    // ANDROID WEBVIEW -> CHROME EXTENSION BRIDGE
    // ==========================================
    if (typeof window.chrome === "undefined") {
        window.chrome = {};
    }
    window.__arafatMsgCallbacks = window.__arafatMsgCallbacks || {};
    window.__arafatMsgSeq = window.__arafatMsgSeq || 1;
    window.__arafatResolveMsg = function (cbId, payloadJson) {
        var cb = window.__arafatMsgCallbacks[cbId];
        if (cb) {
            delete window.__arafatMsgCallbacks[cbId];
            try {
                cb(typeof payloadJson === "string" ? JSON.parse(payloadJson) : payloadJson);
            } catch (e) {
                cb(null);
            }
        }
    };

    var storageBridge = {
        get: function (keys, callback) {
            return new Promise(function (resolve) {
                var result = {};
                try {
                    var raw = window.AndroidExtensionBridge ? window.AndroidExtensionBridge.storageGet() : "{}";
                    var all = JSON.parse(raw || "{}");
                    if (Array.isArray(keys)) {
                        keys.forEach(function (k) {
                            if (Object.prototype.hasOwnProperty.call(all, k)) {
                                result[k] = all[k];
                            }
                        });
                    } else if (typeof keys === "string") {
                        if (Object.prototype.hasOwnProperty.call(all, keys)) {
                            result[keys] = all[keys];
                        }
                    } else if (keys && typeof keys === "object") {
                        Object.keys(keys).forEach(function (k) {
                            result[k] = Object.prototype.hasOwnProperty.call(all, k) ? all[k] : keys[k];
                        });
                    } else {
                        result = all;
                    }
                } catch (e) {}
                if (typeof callback === "function") {
                    try { callback(result); } catch (e) {}
                }
                resolve(result);
            });
        },
        set: function (items, callback) {
            return new Promise(function (resolve) {
                try {
                    if (window.AndroidExtensionBridge) {
                        window.AndroidExtensionBridge.storageSet(JSON.stringify(items || {}));
                    }
                } catch (e) {}
                if (typeof callback === "function") {
                    try { callback(); } catch (e) {}
                }
                resolve();
            });
        }
    };

    window.chrome.storage = window.chrome.storage || {};
    window.chrome.storage.local = storageBridge;

    window.chrome.runtime = window.chrome.runtime || {};
    window.chrome.runtime.lastError = null;
    window.chrome.runtime.sendMessage = function (request, callback) {
        if (!request || !window.AndroidExtensionBridge) {
            if (typeof callback === "function") callback(null);
            return;
        }
        if (request.action === "getFullCookie") {
            try {
                var raw = window.AndroidExtensionBridge.getFullCookie();
                var parsed = JSON.parse(raw || "{}");
                if (typeof callback === "function") callback(parsed);
            } catch (e) {
                if (typeof callback === "function") callback(null);
            }
            return;
        }
        if (request.action === "clearData") {
            var cbId = "cb_" + (window.__arafatMsgSeq++);
            if (typeof callback === "function") {
                window.__arafatMsgCallbacks[cbId] = callback;
            }
            try {
                window.AndroidExtensionBridge.clearDataAsync(cbId);
            } catch (e) {
                if (typeof callback === "function") callback({ status: "error" });
            }
            return;
        }
        if (request.action === "getSuccess") {
            storageBridge.get(["successEmails"], function (d) {
                if (typeof callback === "function") callback({ emails: d.successEmails || [] });
            });
            return;
        }
        if (typeof callback === "function") callback(null);
    };

    // ==========================================
    // 🔔 NOTIFICATION BAR
    // ==========================================
    function createNotificationBar() {
        if (document.getElementById("arafat-auto-notif")) return;
        if (!document.body) return;
        const bar = document.createElement("div");
        bar.id = "arafat-auto-notif";
        bar.style.cssText = "position:fixed;top:0;left:0;right:0;z-index:999999;" +
            "background:linear-gradient(135deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888);" +
            "color:#fff;padding:12px 16px;font-family:Arial,sans-serif;font-size:13px;" +
            "font-weight:700;text-align:center;box-shadow:0 4px 20px rgba(220,39,67,0.5);";
        bar.innerHTML = '<span id="arafat-auto-notif-text">ARAFAT INSTA AUTO • Starting...</span>';
        document.body.appendChild(bar);
    }

    function notify(msg, type) {
        createNotificationBar();
        const textEl = document.getElementById("arafat-auto-notif-text");
        const bar = document.getElementById("arafat-auto-notif");
        if (textEl) textEl.textContent = "ARAFAT INSTA AUTO • " + msg;
        console.log("🔔 " + msg);
        try {
            if (window.AndroidExtensionBridge && window.AndroidExtensionBridge.onNotify) {
                window.AndroidExtensionBridge.onNotify(String(msg || ""), String(type || ""));
            }
        } catch (e) {}
        if (bar) {
            if (type === "success") bar.style.background = "linear-gradient(135deg,#11998e,#38ef7d)";
            else if (type === "error") bar.style.background = "linear-gradient(135deg,#eb3349,#f45c43)";
            else if (type === "info") bar.style.background = "linear-gradient(135deg,#2193b0,#6dd5ed)";
            else bar.style.background = "linear-gradient(135deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888)";
        }
    }

    // ==========================================
    // ⚡ HELPERS (EXACT content.js)
    // ==========================================
    const delay = (ms) => new Promise(r => setTimeout(r, ms));

    async function getElement(selector, timeout = 8000) {
        const start = Date.now();
        while (Date.now() - start < timeout) {
            const el = document.querySelector(selector);
            if (el && el.offsetParent !== null) return el;
            await delay(20);
        }
        return null;
    }

    async function getButtonByText(text, timeout = 8000) {
        const start = Date.now();
        const target = text.toLowerCase().trim();
        while (Date.now() - start < timeout) {
            const els = document.querySelectorAll('button, div[role="button"], a[role="button"], span[role="button"]');
            for (let el of els) {
                const t = (el.innerText || "").trim().toLowerCase();
                if (t === target && el.offsetParent !== null && !el.disabled) return el;
            }
            for (let el of els) {
                const t = (el.innerText || "").trim().toLowerCase();
                if (t.includes(target) && t.length < 60 && el.offsetParent !== null && !el.disabled) return el;
            }
            await delay(30);
        }
        return null;
    }

    async function smartWait(checkFn, maxMs = 8000, interval = 40) {
        const start = Date.now();
        while (Date.now() - start < maxMs) {
            if (checkFn()) return true;
            await delay(interval);
        }
        return false;
    }

    async function realTimeType(el, text) {
        if (!el) return false;
        el.focus(); el.click();
        const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
        setter.call(el, text);
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
        return el.value === text;
    }

    // ✅ UPDATED: Ultra-powerful click for React/Mobile buttons
    async function forceClickButton(el) {
        if (!el) return false;
        try { el.scrollIntoView({ block: 'center', behavior: 'instant' }); } catch (e) {}

        const r = el.getBoundingClientRect();
        const x = r.left + r.width / 2;
        const y = r.top + r.height / 2;

        // 1. Direct click
        try { el.focus(); el.click(); } catch (e) {}

        // 2. Dispatch Pointer & Mouse events on the element
        try {
            const o = { bubbles: true, cancelable: true, view: window, clientX: x, clientY: y, button: 0 };
            el.dispatchEvent(new PointerEvent('pointerdown', o));
            el.dispatchEvent(new PointerEvent('pointerup', o));
            el.dispatchEvent(new MouseEvent('mousedown', o));
            el.dispatchEvent(new MouseEvent('mouseup', o));
            el.dispatchEvent(new MouseEvent('click', o));
        } catch (e) {}

        // 3. Dispatch events on the exact element at point (for overlapping elements)
        try {
            const p = document.elementFromPoint(x, y);
            if (p && p !== el) {
                const o = { bubbles: true, cancelable: true, view: window, clientX: x, clientY: y, button: 0 };
                p.dispatchEvent(new PointerEvent('pointerdown', o));
                p.dispatchEvent(new PointerEvent('pointerup', o));
                p.dispatchEvent(new MouseEvent('mousedown', o));
                p.dispatchEvent(new MouseEvent('mouseup', o));
                p.dispatchEvent(new MouseEvent('click', o));
                try { p.click(); } catch (e) {}
            }
        } catch (e) {}

        // 4. Touch events
        try {
            const t = new Touch({ identifier: Date.now(), target: el, clientX: x, clientY: y, radiusX: 2, radiusY: 2, force: 0.5 });
            el.dispatchEvent(new TouchEvent('touchstart', { bubbles: true, cancelable: true, touches: [t] }));
            el.dispatchEvent(new TouchEvent('touchend', { bubbles: true, cancelable: true, touches: [], changedTouches: [t] }));
        } catch (e) {}

        // 5. Parent clicks (fallback)
        try {
            let p = el.parentElement;
            for (let i = 0; i < 3 && p; i++) {
                try { p.click(); } catch (e) {}
                p = p.parentElement;
            }
        } catch (e) {}

        el.focus();
        return true;
    }

    async function clickNext() {
        const btns = document.querySelectorAll('button, div[role="button"]');
        for (let b of btns) {
            const t = (b.innerText || "").trim().toLowerCase();
            if (t === "next" && b.offsetParent !== null && !b.disabled) {
                await forceClickButton(b);
                return true;
            }
        }
        for (let b of btns) {
            const t = (b.innerText || "").trim().toLowerCase();
            if (t.includes("next") && b.offsetParent !== null && !b.disabled) {
                await forceClickButton(b);
                return true;
            }
        }
        return false;
    }

    async function clickAllow() {
        for (let i = 0; i < 100; i++) {
            const btns = document.querySelectorAll('button, div[role="button"], a[role="button"]');
            for (let b of btns) {
                const t = (b.innerText || "").trim().toLowerCase();
                if (t === "allow and continue" && b.offsetParent !== null && !b.disabled) {
                    await forceClickButton(b);
                    return true;
                }
            }
            for (let b of btns) {
                const t = (b.innerText || "").trim().toLowerCase();
                if (t === "allow" && b.offsetParent !== null && !b.disabled) {
                    await forceClickButton(b);
                    return true;
                }
            }
            for (let b of btns) {
                const t = (b.innerText || "").trim().toLowerCase();
                if (t.includes("allow") && t.length < 60 && b.offsetParent !== null && !b.disabled) {
                    await forceClickButton(b);
                    return true;
                }
            }
            await delay(50);
        }
        return false;
    }

    async function clickIAgree() {
        for (let i = 0; i < 100; i++) {
            const btns = document.querySelectorAll('button, div[role="button"], a[role="button"]');
            for (let b of btns) {
                const t = (b.innerText || "").trim().toLowerCase();
                if (t === "i agree" && b.offsetParent !== null && !b.disabled) {
                    await forceClickButton(b);
                    return true;
                }
            }
            for (let b of btns) {
                const t = (b.innerText || "").trim().toLowerCase();
                if (t.includes("agree") && t.length < 40 && b.offsetParent !== null && !b.disabled) {
                    await forceClickButton(b);
                    return true;
                }
            }
            await delay(50);
        }
        return false;
    }

    async function clickSkip() {
        for (let i = 0; i < 100; i++) {
            const btns = document.querySelectorAll('button, div[role="button"], a[role="button"]');
            for (let b of btns) {
                const t = (b.innerText || "").trim().toLowerCase();
                if (t === "skip" && b.offsetParent !== null && !b.disabled) {
                    await forceClickButton(b);
                    return true;
                }
            }
            for (let b of btns) {
                const t = (b.innerText || "").trim().toLowerCase();
                if (t.includes("skip") && t.length < 30 && b.offsetParent !== null && !b.disabled) {
                    await forceClickButton(b);
                    return true;
                }
            }
            await delay(50);
        }
        return false;
    }

    async function closeAllPopups() {
        const popupsToClose = ["Not Now", "Not now", "Cancel", "Close", "Dismiss", "Skip", "Later", "No Thanks", "No thanks", "Maybe Later"];
        for (let popupText of popupsToClose) {
            const els = document.querySelectorAll('button, div[role="button"], a[role="button"]');
            for (let el of els) {
                const t = (el.innerText || "").trim();
                if (t === popupText && el.offsetParent !== null) {
                    try { el.click(); } catch (e) {}
                }
            }
        }
    }

    function startPopupWatcher() {
        setInterval(closeAllPopups, 2000);
    }

    // ==========================================
    // 🧹 JS CLEAR
    // ==========================================
    async function jsClearAllStorage() {
        for (let pass = 0; pass < 2; pass++) {
            try {
                const cookies = document.cookie.split(";");
                for (let cookie of cookies) {
                    const eqPos = cookie.indexOf("=");
                    const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim();
                    if (name) {
                        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/";
                        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=.instagram.com";
                        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=www.instagram.com";
                        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=.facebook.com";
                        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=.meta.com";
                    }
                }
            } catch (e) {}
            await delay(30);
        }
        try { localStorage.clear(); } catch (e) {}
        try { sessionStorage.clear(); } catch (e) {}
        try {
            if (indexedDB.databases) {
                const dbs = await indexedDB.databases();
                for (let db of dbs) if (db.name) indexedDB.deleteDatabase(db.name);
            }
        } catch (e) {}
        try {
            if (typeof caches !== 'undefined') {
                const names = await caches.keys();
                for (let name of names) await caches.delete(name);
            }
        } catch (e) {}
        try {
            if (navigator.serviceWorker) {
                const regs = await navigator.serviceWorker.getRegistrations();
                for (let reg of regs) await reg.unregister();
            }
        } catch (e) {}
    }

    // ==========================================
    // FIND INPUTS
    // ==========================================
    async function findFullNameInput() {
        let el = document.querySelector('input[name="full_name"]');
        if (el && el.offsetParent !== null) return el;
        el = document.querySelector('input[aria-label="Full name"]');
        if (el && el.offsetParent !== null) return el;
        const inputs = document.querySelectorAll('input[type="text"], input:not([type])');
        for (let i of inputs) {
            if (i.offsetParent !== null && !i.value) return i;
        }
        return null;
    }

    async function findUsernameInput() {
        let el = document.querySelector('input[name="username"]');
        if (el && el.offsetParent !== null) return el;
        el = document.querySelector('input[aria-label="Username"]');
        if (el && el.offsetParent !== null) return el;
        return null;
    }

    async function findUsernameLoginInput() {
        let el = document.querySelector('input[name="username"]');
        if (el && el.offsetParent !== null) return el;
        el = document.querySelector('input[aria-label*="username" i]');
        if (el && el.offsetParent !== null) return el;
        el = document.querySelector('input[aria-label*="email" i]');
        if (el && el.offsetParent !== null) return el;
        return null;
    }

    async function findPasswordInput() {
        let el = document.querySelector('input[name="password"]');
        if (el && el.offsetParent !== null) return el;
        el = document.querySelector('input[type="password"]');
        if (el && el.offsetParent !== null) return el;
        return null;
    }

    // ==========================================
    // ✅ FIXED: CLICK ACCOUNT CARD (Meta Account Popup Support)
    // ==========================================
    async function clickAccountCard() {
        for (let attempt = 1; attempt <= 5; attempt++) {
            console.log("🎯 Account card attempt " + attempt + "/5");
            notify("Selecting account (try " + attempt + ")...");

            const success = await smartWait(() => {
                let clicked = false;

                // 1. Target the Meta Account Modal specifically
                const allElements = document.querySelectorAll('div, span, p, h2, h3, a, button');
                for (let el of allElements) {
                    const txt = (el.innerText || "").trim().toLowerCase();

                    // Check if this element contains "Smart EarningZ" or "Meta Horizon" or "Meta Account"
                    if (txt === "smart earningz" || txt.includes("meta horizon") || txt.includes("meta account")) {
                        // Skip the large modal title
                        if (txt.includes("no instagram account found") || txt.includes("matched an existing")) continue;

                        // Find the clickable card container by going up
                        let parent = el;
                        for (let i = 0; i < 8; i++) {
                            if (!parent) break;
                            const rect = parent.getBoundingClientRect();

                            // The card is usually a wide, distinct rectangle
                            if (rect.width > 250 && rect.height > 40 && rect.height < 200) {
                                console.log("🎯 Found Meta card container. Clicking...");
                                forceClickButton(parent);
                                clicked = true;
                                break;
                            }
                            parent = parent.parentElement;
                        }

                        if (clicked) break;

                        // Fallback: Click the element itself
                        forceClickButton(el);
                        clicked = true;
                        break;
                    }
                }

                // 2. Standard Profile Avatar / Image clicks (Normal flow)
                if (!clicked) {
                    const imgs = document.querySelectorAll('img');
                    for (let img of imgs) {
                        const rect = img.getBoundingClientRect();
                        if (rect.width === 0 || rect.height === 0) continue;

                        const alt = (img.alt || "").toLowerCase();
                        const parent = img.closest('div[role="button"], button, a');

                        if (alt.includes("profile") || alt.includes("arafat") || (parent && (parent.innerText || "").toLowerCase().includes("continue"))) {
                            if (parent) {
                                forceClickButton(parent);
                            } else {
                                forceClickButton(img);
                            }
                            clicked = true;
                            break;
                        }
                    }
                }

                // 3. Standard Continue/Switch Buttons
                if (!clicked) {
                    const buttons = document.querySelectorAll('button, div[role="button"], a[role="button"]');
                    for (let btn of buttons) {
                        const txt = (btn.innerText || "").toLowerCase();
                        if (txt.includes("continue as") || txt.includes("switch") || txt.trim() === "continue" || txt.includes("log in as")) {
                            forceClickButton(btn);
                            clicked = true;
                            break;
                        }
                    }
                }

                return clicked;
            }, 8000, 200);

            if (success) {
                console.log("✅ Card clicked! Waiting for navigation...");

                const worked = await smartWait(() => {
                    const bodyText = (document.body.innerText || "").toLowerCase();

                    // If the modal is still there, it means the click failed
                    if (bodyText.includes("no instagram account found") && bodyText.includes("meta account")) {
                        return false;
                    }

                    // Check if we successfully moved to the next page
                    if (document.querySelector('input[name="full_name"]')) return true;
                    if (document.querySelector('input[name="username"]')) return true;
                    if (bodyText.includes("choose a username") || bodyText.includes("add your name")) return true;

                    return false;
                }, 8000, 200);

                if (worked) {
                    console.log("✅ Navigation confirmed!");
                    return true;
                }
                console.log("⚠️ Click didn't navigate — retrying...");
            }

            // Retry
            if (attempt < 5) {
                notify("🔄 Retry — refreshing...", "info");
                await delay(1000);
                const newUrl = "https://www.instagram.com/accounts/login/?mtn&t=" + Date.now();
                try { window.location.replace(newUrl); }
                catch (e) { window.location.href = newUrl; }
                return false;
            }
        }
        return false;
    }

    // ==========================================
    // ✅ CHECK — HOME / REGISTERED PAGE
    // ==========================================
    function isOnHomeOrRegisteredPage() {
        const url = window.location.href;

        if (url.includes("/accounts/registered")) {
            console.log("🎯 Detected: /accounts/registered/");
            return true;
        }

        if (url.includes("instagram.com") &&
            !url.includes("/accounts/login") &&
            !url.includes("/accounts/emailsignup") &&
            !url.includes("/accounts/signup") &&
            !url.includes("/challenge") &&
            !url.includes("about:blank")) {
            console.log("🎯 Detected: HOME (" + url + ")");
            return true;
        }

        return false;
    }

    // ==========================================
    // ✅✅✅ GET FULL COOKIE FROM BACKGROUND
    // ==========================================
    async function getFullCookieFromBackground() {
        console.log("📤 Requesting full cookies from background...");
        notify("📸 Getting full cookie...");

        try {
            const response = await new Promise((resolve) => {
                chrome.runtime.sendMessage({ action: "getFullCookie" }, (resp) => {
                    if (chrome.runtime.lastError) {
                        console.log("Fetch err: " + chrome.runtime.lastError.message);
                        resolve(null);
                    } else {
                        resolve(resp);
                    }
                });
            });

            if (response && response.success) {
                console.log("✅✅✅ FULL COOKIE FROM chrome.cookies:");
                console.log("  Total cookies: " + response.count);
                console.log("  ds_user_id: " + response.hasDsUserId);
                console.log("  sessionid: " + response.hasSessionId);
                console.log("  rur: " + response.hasRur);
                console.log("  csrftoken: " + response.hasCsrf);
                console.log("  Cookie length: " + response.fullCookie.length);

                notify("✅ Full cookie: " + response.count + " items", "success");
                return response;
            } else {
                console.log("⚠️ No response from background");
                return null;
            }
        } catch (e) {
            console.log("Fetch err: " + e.message);
            return null;
        }
    }

    // ==========================================
    // ✅ FINISH ACCOUNT — SAVE + CLEAR + NEXT
    // ==========================================
    async function finishAccount(username, password, email, fromHome) {
        console.log("🏁 finishAccount() called");
        console.log("  👤 Username: " + username);
        console.log("  📧 Email: " + email);

        const data = await chrome.storage.local.get(['accounts', 'currentIndex', 'successEmails', 'savedResults']);
        const idx = data.currentIndex || 0;
        const savedResults = data.savedResults || [];
        const successEmails = data.successEmails || [];

        console.log("⏳ Wait 2s for cookies...");
        notify("⏳ Wait 2s for cookies...");
        await delay(2000);

        let response = await getFullCookieFromBackground();

        if (!response || !response.hasSessionId) {
            console.log("⚠️ sessionid not found — retry after 3s...");
            notify("⏳ Retry cookie fetch...");
            await delay(3000);
            response = await getFullCookieFromBackground();
        }

        let fullCookie = "";
        let cookieInfo = {
            hasSessionId: false,
            hasRur: false,
            hasCsrf: false,
            hasDsUserId: false,
            count: 0
        };

        if (response && response.success) {
            fullCookie = response.fullCookie || "";
            cookieInfo = {
                hasSessionId: response.hasSessionId,
                hasRur: response.hasRur,
                hasCsrf: response.hasCsrf,
                hasDsUserId: response.hasDsUserId,
                count: response.count
            };
        } else {
            try { fullCookie = document.cookie || ""; } catch (e) {}
            console.log("⚠️ Using document.cookie as fallback");
        }

        console.log("📸 FINAL COOKIE:");
        console.log("  len: " + fullCookie.length);
        console.log("  sessionid: " + cookieInfo.hasSessionId);
        console.log("  rur: " + cookieInfo.hasRur);
        console.log("  csrftoken: " + cookieInfo.hasCsrf);
        console.log("  ds_user_id: " + cookieInfo.hasDsUserId);

        const resultEntry = {
            username: username || "?",
            password: password || "?",
            fullcookie: fullCookie || "?",
            email: email || "?",
            timestamp: Date.now()
        };

        const updatedResults = [...savedResults, resultEntry];
        const newSuccessList = [...successEmails, email];

        await chrome.storage.local.set({
            savedResults: updatedResults,
            successEmails: newSuccessList
        });
        console.log("✅ Results saved: " + updatedResults.length);

        const nextIdx = idx + 1;
        const d = await chrome.storage.local.get(['totalAccounts', 'accounts']);
        const total2 = d.totalAccounts || (d.accounts || []).length;

        await chrome.storage.local.set({ currentIndex: nextIdx });
        console.log("📊 Index: " + nextIdx + "/" + total2);

        notify("✅ Saved: " + email);

        console.log("🧹 JS clear...");
        try { await jsClearAllStorage(); } catch (e) {}

        console.log("📤 Sending clearData...");
        try {
            chrome.runtime.sendMessage({ action: "clearData" }, (response) => {
                console.log("📥 Background: " + JSON.stringify(response));
            });
        } catch (e) {}

        console.log("⏳ Wait 3s for clear...");
        await delay(3000);

        if (nextIdx >= total2) {
            await chrome.storage.local.set({ isRunning: false });
            notify("🎉 All " + total2 + " done!", "success");
            console.log("🎉 ALL DONE");
            return;
        }

        notify("🔄 Next (" + (nextIdx + 1) + "/" + total2 + ")...");
        console.log("🔄 FORCE RELOAD...");

        const newUrl = "https://www.instagram.com/accounts/login/?mtn&t=" + Date.now();
        try { window.location.replace(newUrl); }
        catch (e) { window.location.href = newUrl; }
    }

    // ==========================================
    // ✅ DETECT EXISTING USERNAME (Home page এ থাকলে)
    // ==========================================
    function detectExistingUsername() {
        try {
            const profileLinks = document.querySelectorAll('a[href^="/"][role="link"], a[href*="/"]');
            for (let a of profileLinks) {
                const href = a.getAttribute('href') || "";
                if (href.match(/^\/[a-zA-Z0-9._]+\/?$/) &&
                    !href.includes("/accounts") &&
                    !href.includes("/explore") &&
                    !href.includes("/reels") &&
                    !href.includes("/direct") &&
                    !href.includes("/p/") &&
                    !href.includes("/stories")) {
                    const name = href.replace(/\//g, "");
                    if (name && name.length >= 3 && name.length < 30) {
                        console.log("👤 Found username from link: " + name);
                        return name;
                    }
                }
            }
        } catch (e) {}

        try {
            const meta = document.querySelector('meta[property="og:title"]');
            if (meta && meta.content) {
                const m = meta.content.match(/\(@([a-zA-Z0-9._]+)\)/);
                if (m && m[1]) {
                    console.log("👤 Found username from meta: " + m[1]);
                    return m[1];
                }
            }
        } catch (e) {}

        return "";
    }

    // ==========================================
    // 🚀 MAIN FLOW (EXACT content.js)
    // ==========================================
    async function runAutomation() {
        console.log("🚀 runAutomation() START");

        const data = await chrome.storage.local.get(['accounts', 'currentIndex', 'isRunning', 'successEmails', 'savedResults']);
        if (!data.isRunning || !data.accounts || data.accounts.length === 0) {
            console.log("⏹️ Not running");
            return;
        }

        const idx = data.currentIndex || 0;
        const total = data.accounts.length;

        if (idx >= total) {
            chrome.storage.local.set({ isRunning: false });
            notify("🎉 All " + total + " done!", "success");
            return;
        }

        // ✅ FIXED: Home/Registered page check — আসল username detect করে save
        if (isOnHomeOrRegisteredPage()) {
            console.log("⚠️ Already on home/registered page!");
            notify("✅ Account exists! Saving...", "success");

            const currentEmail = (data.accounts[idx] || "").split(":")[0] || "unknown";
            const currentPassword = (data.accounts[idx] || "").split(":").slice(1).join(":") || "?";

            let detectedUsername = detectExistingUsername();

            if (!detectedUsername) {
                detectedUsername = currentEmail.split("@")[0] || ("user_" + Date.now());
                console.log("👤 Fallback username (email prefix): " + detectedUsername);
            }

            console.log("👤 Final username: " + detectedUsername);
            await finishAccount(detectedUsername, currentPassword, currentEmail, true);
            return;
        }

        // Normal flow
        const parts = data.accounts[idx].split(':');
        const email = parts[0];
        const password = parts.slice(1).join(':');

        console.log("=== " + email + " (" + (idx + 1) + "/" + total + ") ===");

        createNotificationBar();
        startPopupWatcher();

        // ===== 1. LOGIN =====
        notify("[" + (idx + 1) + "/" + total + "] Login: " + email.substring(0, 20));

        let emailInput = null;
        for (let a = 0; a < 30; a++) {
            emailInput = await findUsernameLoginInput();
            if (emailInput) break;
            await delay(100);
        }
        if (emailInput) {
            notify("Typing email...");
            await realTimeType(emailInput, email);
        }

        let passInput = null;
        for (let a = 0; a < 30; a++) {
            passInput = await findPasswordInput();
            if (passInput) break;
            await delay(100);
        }
        if (passInput) {
            notify("Typing password...");
            await realTimeType(passInput, password);
        }

        if (emailInput && passInput) {
            notify("Clicking Log in...");
            let loginBtn = await getElement('button[type="submit"]', 2000) || await getButtonByText("Log in", 2000);
            if (loginBtn) {
                await forceClickButton(loginBtn);
                await smartWait(() => {
                    const bt = document.body.innerText || "";
                    if (bt.includes("Find another account") || bt.includes("Arafat") || bt.includes("@")) return true;
                    if (document.querySelector('input[name="full_name"]')) return true;
                    return false;
                }, 6000, 50);
            }
        }

        // ===== 2. ACCOUNT CARD (with retry + auto refresh) =====
        notify("Selecting account card...");
        const cardClicked = await clickAccountCard();

        if (!cardClicked) {
            console.log("❌ Account card failed completely");
            notify("❌ Card select failed — refresh", "error");
            await delay(500);
            const newUrl = "https://www.instagram.com/accounts/login/?mtn&t=" + Date.now();
            try { window.location.replace(newUrl); }
            catch (e) { window.location.href = newUrl; }
            return;
        }

        // ===== 3. NAME =====
        notify("Filling Full Name...");
        await smartWait(() => document.querySelector('input[name="full_name"]') || document.querySelector('input[aria-label="Full name"]'), 6000, 40);

        const nameInput = await findFullNameInput();
        if (nameInput) {
            const randomName = "arafatbhai" + Math.floor(Math.random() * 100000);
            notify("Name: " + randomName);
            await realTimeType(nameInput, randomName);
            await clickNext();
            await smartWait(() => document.querySelector('input[name="username"]') || document.querySelector('input[aria-label="Username"]'), 6000, 40);
        }

        // ===== 4. USERNAME =====
        notify("Filling Username...");
        const userInput = await findUsernameInput();

        let finalUsername = "";

        if (userInput) {
            const existingValue = (userInput.value || "").trim();

            if (existingValue && existingValue.length >= 3) {
                finalUsername = existingValue;
                notify("Using: " + finalUsername);
            } else {
                const randomUser = "user" + Math.floor(Math.random() * 1000000);
                await realTimeType(userInput, randomUser);
                finalUsername = randomUser;
            }

            await delay(400);

            const bt = document.body.innerText || "";
            if (bt.includes("Choose a username")) {
                try {
                    const setter = Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype, "value").set;
                    setter.call(userInput, '');
                    userInput.dispatchEvent(new Event('input', { bubbles: true }));
                } catch (e) {}
                await delay(200);
                const newUser = "ig" + Date.now().toString().slice(-9);
                await realTimeType(userInput, newUser);
                finalUsername = newUser;
                await delay(300);
            }

            if (userInput.value) finalUsername = userInput.value.trim();

            await clickNext();
            await smartWait(() => {
                const btns = document.querySelectorAll('button, div[role="button"]');
                for (let b of btns) {
                    const t = (b.innerText || "").toLowerCase();
                    if (t.includes("allow") || t.includes("i agree") || t.includes("agree")) return true;
                }
                return false;
            }, 6000, 50);
        }

        // ===== 5. ALLOW =====
        notify("Clicking Allow...");
        await delay(400);
        await clickAllow();
        await delay(600);

        // ===== 6. I AGREE =====
        notify("Clicking I agree...");
        await delay(400);
        await clickIAgree();
        await delay(600);

        // ===== 6.5: SKIP =====
        if (document.body.innerText.includes("Skip") &&
            (document.body.innerText.includes("phone") || document.body.innerText.includes("Phone"))) {
            notify("Skip page — clicking...");
            await clickSkip();
        }

        // ===== 7. WAIT FOR HOME/REGISTERED =====
        console.log("🎯 Waiting for account creation...");
        notify("Waiting for account...");

        let created = false;
        const start = Date.now();
        while (Date.now() - start < 90000) {
            if (isOnHomeOrRegisteredPage()) {
                created = true;
                notify("✅ Account created!", "success");
                break;
            }
            await delay(200);
        }

        if (!created) {
            notify("⚠️ Timeout — moving next", "error");
        }

        // ✅ Finish (save + clear + next)
        await finishAccount(finalUsername, password, email, false);
    }

    // ==========================================
    // START
    // ==========================================
    if (document.readyState === "complete") {
        setTimeout(runAutomation, 300);
    } else {
        window.addEventListener("load", () => {
            setTimeout(runAutomation, 300);
        });
    }
})();
