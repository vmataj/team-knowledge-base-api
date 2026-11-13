const role = document.querySelector('#role');
const workspace = document.querySelector('#workspace');
const statusLine = document.querySelector('#status');
const list = document.querySelector('#list');
const detail = document.querySelector('#detail');
const form = document.querySelector('#runbook-form');
const editor = document.querySelector('#editing');
const deleteButton = document.querySelector('#delete');
const workspaceForm = document.querySelector('#workspace-form');
const members = document.querySelector('#members');
const memberList = document.querySelector('#member-list');
const memberForm = document.querySelector('#member-form');
const base = '/api/workspaces';
let selectedId = null;
let version = 0;
let account = null;

async function request(path, method = 'GET', payload) {
    const headers = account
        ? { [account.csrfHeader]: account.csrfToken }
        : { Authorization: 'Basic ' + btoa(role.value + ':demo-pass') };
    if (payload) headers['Content-Type'] = 'application/json';
    const response = await fetch(base + path, { headers, method, body: payload && JSON.stringify(payload) });
    if (response.status === 403) throw new Error('This account cannot perform that action.');
    if (response.status === 409) throw new Error('That workspace or access change conflicts with an existing record.');
    if (!response.ok) throw new Error('Request failed (HTTP ' + response.status + ').');
    if (response.status !== 204) return response.json();
}

function select(runbook) {
    selectedId = runbook?.id ?? null;
    detail.hidden = !runbook;
    document.querySelector('#title').textContent = runbook?.title ?? '';
    document.querySelector('#body').textContent = runbook?.body ?? '';
    document.querySelector('#form-title').textContent = runbook ? 'Edit runbook' : 'New runbook';
    form.elements.title.value = runbook?.title ?? '';
    form.elements.body.value = runbook?.body ?? '';
    deleteButton.hidden = !runbook;
}

function renderMembers(entries) {
    for (const member of entries) {
        const item = document.createElement('li');
        item.textContent = member.username + ' · ' + member.role;
        if (member.role !== 'OWNER') {
            const remove = document.createElement('button');
            remove.textContent = 'Remove';
            remove.addEventListener('click', () =>
                change('/' + workspace.value + '/members/' + member.username, 'DELETE', undefined, 'Access removed.'));
            item.append(remove);
        }
        memberList.append(item);
    }
}

async function load(preferredWorkspace = workspace.value, preferredId, message) {
    const active = ++version;
    list.replaceChildren();
    memberList.replaceChildren();
    editor.hidden = members.hidden = true;
    select(null);
    statusLine.textContent = 'Loading workspaces…';
    try {
        const access = await request('');
        if (active !== version) return;
        workspace.replaceChildren(...access.map(entry => new Option(entry.workspace + ' · ' + entry.role, entry.workspace)));
        const current = access.find(entry => entry.workspace === preferredWorkspace) ?? access[0];
        if (!current) {
            statusLine.textContent = 'Create a workspace to get started.';
            return;
        }
        workspace.value = current.workspace;
        const path = '/' + current.workspace;
        const [runbooks, entries] = await Promise.all([
            request(path + '/runbooks'),
            current.role === 'OWNER' ? request(path + '/members') : Promise.resolve([])
        ]);
        if (active !== version) return;
        editor.hidden = current.role === 'VIEWER';
        members.hidden = current.role !== 'OWNER';
        renderMembers(entries);
        for (const runbook of runbooks) {
            const item = document.createElement('li');
            const button = document.createElement('button');
            button.textContent = runbook.title;
            button.addEventListener('click', () => select(runbook));
            item.append(button);
            list.append(item);
        }
        select(runbooks.find(runbook => runbook.id === preferredId));
        statusLine.textContent = message ?? (runbooks.length ? 'Select a runbook to open it.' : 'No runbooks yet.');
    } catch (error) {
        if (active === version) statusLine.textContent = error.message;
    }
}

async function change(path, method, payload, message) {
    const controls = [role, workspace, ...document.querySelectorAll('form input, form select, form textarea, button')];
    controls.forEach(control => { control.disabled = true; });
    try {
        const result = await request(path, method, payload);
        await load(path === '' ? result.workspace : workspace.value, result?.id, message);
    } catch (error) {
        statusLine.textContent = error.message;
    } finally {
        controls.forEach(control => { control.disabled = false; });
    }
}

form.addEventListener('submit', event => {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(form));
    const path = '/' + workspace.value + '/runbooks' + (selectedId ? '/' + selectedId : '');
    change(path, selectedId ? 'PUT' : 'POST', payload, 'Runbook saved.');
});
workspaceForm.addEventListener('submit', event => {
    event.preventDefault();
    change('', 'POST', { slug: workspaceForm.elements.slug.value }, 'Workspace created.');
});
memberForm.addEventListener('submit', event => {
    event.preventDefault();
    const { username, access } = Object.fromEntries(new FormData(memberForm));
    change('/' + workspace.value + '/members/' + encodeURIComponent(username), 'PUT', { role: access }, 'Access saved.');
});
document.querySelector('#new').addEventListener('click', () => select(null));
deleteButton.addEventListener('click', () => {
    if (confirm('Delete this runbook?')) {
        change('/' + workspace.value + '/runbooks/' + selectedId, 'DELETE', undefined, 'Runbook deleted.');
    }
});
role.addEventListener('change', () => load());
workspace.addEventListener('change', () => load());
async function initialize() {
    account = await loadAccount();
    if (account) {
        updateAccountHeader(account);
        document.querySelector('#demo-account').hidden = true;
        memberForm.elements.username.removeAttribute('list');
        if (!account.username) {
            document.querySelector('#workspace-preview').hidden = false;
            return;
        }
    }
    document.querySelector('#workspace-app').hidden = false;
    await load();
}

initialize().catch(error => {
    document.querySelector('#startup-status').textContent = error.message;
});
