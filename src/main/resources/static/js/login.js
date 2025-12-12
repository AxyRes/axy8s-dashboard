const form = document.getElementById('loginForm');
const alertBox = document.getElementById('loginAlert');

if (form) {
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        alertBox.classList.add('d-none');

        const payload = {
            username: document.getElementById('username').value,
            password: document.getElementById('password').value
        };

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || 'Login failed');
            }

            const data = await response.json();
            localStorage.setItem('axy8s_token', data.token);
            document.cookie = `axy8s_token=${data.token}; Path=/; SameSite=Lax`;
            window.location.href = '/dashboard';
        } catch (error) {
            alertBox.textContent = error.message;
            alertBox.classList.remove('d-none');
        }
    });
}
