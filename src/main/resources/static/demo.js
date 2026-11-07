const base = '/api/workspaces/support/runbooks';
const role = document.querySelector('#role');
const statusLine = document.querySelector('#status');
const title = document.querySelector('#title');
const body = document.querySelector('#body');

async function demo(edit) {
    const headers = { Authorization: 'Basic ' + btoa(role.value + ':demo-pass') };
    title.textContent = 'Support runbook';
    body.textContent = '';
    try {
        let response = await fetch(base, { headers });
        if (!response.ok) {
            statusLine.textContent = 'GET runbooks → HTTP ' + response.status;
            return;
        }
        let [runbook] = await response.json();
        if (edit) {
            response = await fetch(base + '/' + runbook.id, {
                method: 'PUT',
                headers: { ...headers, 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: runbook.title, body: runbook.body })
            });
            statusLine.textContent = 'PUT runbook → HTTP ' + response.status;
            if (response.ok) runbook = await response.json();
        } else {
            statusLine.textContent = 'GET runbooks → HTTP ' + response.status;
        }
        title.textContent = runbook.title;
        body.textContent = runbook.body;
    } catch {
        statusLine.textContent = 'The demo could not reach the server.';
    }
}

document.querySelector('#read').addEventListener('click', () => demo(false));
document.querySelector('#write').addEventListener('click', () => demo(true));
