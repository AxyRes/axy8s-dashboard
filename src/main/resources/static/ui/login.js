document.addEventListener("DOMContentLoaded", () => {
    // nếu đã login rồi thì khỏi xem login nữa
    const auth = AxyCommon.loadAuthFromStorage();
    if (auth.token) {
        window.location.href = "/ui";
        return;
    }

    const form = document.getElementById("login-form");
    const err = document.getElementById("login-error");

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        err.hidden = true;

        const username = document.getElementById("login-username").value.trim();
        const password = document.getElementById("login-password").value;

        if (!username || !password) {
            err.textContent = "Vui lòng nhập username/password.";
            err.hidden = false;
            return;
        }

        try {
            const data = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password })
            }).then(r => {
                if (!r.ok) throw new Error("Sai tài khoản hoặc mật khẩu.");
                return r.json();
            });

            const user = {
                username: data.username,
                role: data.role
            };
            AxyCommon.saveAuth(data.token, user);

            window.location.href = "/ui";
        } catch (e2) {
            err.textContent = "Đăng nhập thất bại: " + e2.message;
            err.hidden = false;
        }
    });
});
