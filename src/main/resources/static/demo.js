const $ = id => document.getElementById(id);
const workspace = $('workspace');
const role = $('role');
const search = $('search');
const form = $('runbook-form');
const memberForm = $('member-form');
const examples = [
    { id: 1, title: 'Triage a failed build', body: 'Open the failed CI job and find the first error.\n\nRe-run the failing command locally with the same Java version. If it still fails, attach the log and reproduction steps to the ticket.' },
    { id: 2, title: 'Prepare a release smoke test', body: 'Confirm the build number and test environment.\n\nCheck sign-in, the main workflow, and permissions using separate accounts. Record the result and link any defects before marking the build ready.' },
    { id: 3, title: 'Write a reproducible bug report', body: 'Record the environment and test data, then list the shortest steps that reproduce the issue.\n\nDescribe what happened and what you expected. Attach a screenshot or the relevant log excerpt.' }
];

let account;
let articles = [];
let current;
let selected;
let loadVersion = 0;

async function request(path, method = 'GET', payload) {
    const headers = account
        ? { [account.csrfHeader]: account.csrfToken }
        : { Authorization: 'Basic ' + btoa(role.value + ':demo-pass') };
    if (payload) headers['Content-Type'] = 'application/json';
    const response = await fetch('/api/workspaces' + path, {
        method, headers, body: payload && JSON.stringify(payload)
    });
    if (response.redirected || response.status === 401) throw new Error('Sign in again to continue.');
    if (response.status === 403) throw new Error('You do not have access to do that.');
    if (response.status === 404) throw new Error('That account or article could not be found.');
    if (response.status === 409) throw new Error('That workspace name is already in use.');
    if (response.status === 400) throw new Error('Check the form fields and try again.');
    if (!response.ok) throw new Error('The request failed. Try again.');
    if (response.status !== 204) return response.json();
}

function canEdit() {
    return current && current.role !== 'VIEWER';
}

function openArticle(article) {
    selected = article;
    $('editing').hidden = true;
    $('detail').hidden = !article;
    $('empty-article').hidden = Boolean(article);
    $('empty-article').textContent = articles.length ? 'Choose an article to read it.'
        : canEdit() ? 'This workspace has no articles. Use New article to write the first one.'
        : 'There are no articles in this workspace yet.';
    $('title').textContent = article?.title ?? '';
    $('body').textContent = article?.body ?? '';
    $('edit').hidden = !canEdit();
    for (const button of $('list').querySelectorAll('button')) {
        if (button.dataset.id === String(article?.id)) button.setAttribute('aria-current', 'true');
        else button.removeAttribute('aria-current');
    }
}

function renderArticles() {
    const term = search.value.trim().toLowerCase();
    const visible = current ? articles : articles.filter(article => [article.title, article.body]
        .some(text => text.toLowerCase().includes(term)));
    $('result-count').textContent = visible.length ? visible.length + (visible.length === 1 ? ' article' : ' articles')
        : term ? 'No articles match your search.' : 'No articles yet.';
    $('list').replaceChildren(...visible.map(article => {
        const item = document.createElement('li');
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = article.title;
        button.dataset.id = article.id;
        button.addEventListener('click', () => openArticle(article));
        item.append(button);
        return item;
    }));
    return visible;
}

function renderMembers(entries) {
    $('member-list').replaceChildren(...entries.map(member => {
        const item = document.createElement('li');
        const label = { OWNER: 'Owner', EDITOR: 'Can edit', VIEWER: 'Can view' }[member.role];
        item.textContent = member.username + ' (' + label + ')';
        if (member.role !== 'OWNER') {
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'secondary';
            remove.textContent = 'Remove';
            remove.setAttribute('aria-label', 'Remove ' + member.username);
            remove.addEventListener('click', () =>
                change('/' + current.workspace + '/members/' + encodeURIComponent(member.username),
                    'DELETE', undefined, 'Access removed.', selected?.id));
            item.append(remove);
        }
        return item;
    }));
}

async function load(preferred = workspace.value, articleId, message = '') {
    const version = ++loadVersion;
    current = selected = null;
    $('catalog').hidden = $('members').hidden = true;
    $('status').textContent = 'Loading workspace…';
    try {
        const access = await request('');
        if (version !== loadVersion) return;
        workspace.replaceChildren(...access.map(entry => new Option(entry.workspace, entry.workspace)));
        $('workspace-picker').hidden = !access.length;
        const next = access.find(entry => entry.workspace === preferred) ?? access[0];
        if (!next) {
            $('workspace-create').open = true;
            $('status').textContent = 'Create your first workspace to start writing articles.';
            return;
        }
        workspace.value = next.workspace;
        const path = '/' + next.workspace;
        const [loaded, entries] = await Promise.all([
            request(path + '/runbooks?q=' + encodeURIComponent(search.value.trim())),
            next.role === 'OWNER' ? request(path + '/members') : Promise.resolve([])
        ]);
        if (version !== loadVersion) return;
        current = next;
        articles = loaded;
        $('catalog').hidden = false;
        $('new').hidden = !canEdit();
        $('members').hidden = next.role !== 'OWNER';
        renderMembers(entries);
        const visible = renderArticles();
        openArticle(visible.find(article => article.id === articleId) ?? visible[0]);
        $('status').textContent = message;
    } catch (error) {
        if (version === loadVersion) $('status').textContent = error.message;
    }
}

function editArticle(article) {
    if (!canEdit()) return;
    selected = article;
    $('detail').hidden = $('empty-article').hidden = true;
    $('editing').hidden = false;
    $('form-title').textContent = article ? 'Edit article' : 'New article';
    $('delete').hidden = !article;
    form.elements.title.value = article?.title ?? '';
    form.elements.body.value = article?.body ?? '';
    form.elements.title.focus();
}

async function change(path, method, payload, message, articleId) {
    const controls = [...$('library').querySelectorAll('button, input, textarea, select')]
        .filter(control => !control.disabled);
    controls.forEach(control => { control.disabled = true; });
    try {
        const result = await request(path, method, payload);
        if (path === '') {
            $('workspace-form').reset();
            $('workspace-create').open = false;
            search.value = '';
        }
        if (path.includes('/runbooks') && method !== 'DELETE') search.value = '';
        if (path.includes('/members/') && method === 'PUT') memberForm.reset();
        await load(result?.workspace ?? workspace.value, result?.id ?? articleId, message);
    } catch (error) {
        $('status').textContent = error.message;
    } finally {
        controls.forEach(control => { control.disabled = false; });
    }
}

form.addEventListener('submit', event => {
    event.preventDefault();
    const { title, body } = Object.fromEntries(new FormData(form));
    if (!title.trim() || !body.trim()) {
        $('status').textContent = 'Enter a title and instructions.';
        return;
    }
    const path = '/' + current.workspace + '/runbooks';
    change(selected ? path + '/' + selected.id : path, selected ? 'PUT' : 'POST',
        { title: title.trim(), body: body.trim() }, 'Article saved.');
});
$('workspace-form').addEventListener('submit', event => {
    event.preventDefault();
    change('', 'POST', { slug: event.currentTarget.elements.slug.value }, 'Workspace created.');
});
memberForm.addEventListener('submit', event => {
    event.preventDefault();
    const { username, access } = Object.fromEntries(new FormData(memberForm));
    change('/' + current.workspace + '/members/' + encodeURIComponent(username.trim().toLowerCase()),
        'PUT', { role: access }, 'Access saved.', selected?.id);
});
$('new').addEventListener('click', () => editArticle());
$('edit').addEventListener('click', () => editArticle(selected));
$('cancel').addEventListener('click', () => openArticle(selected));
$('delete').addEventListener('click', () => {
    if (!selected) return;
    $('delete-description').textContent = selected.title;
    $('delete-dialog').returnValue = '';
    $('delete-dialog').showModal();
});
$('delete-dialog').addEventListener('close', event => {
    if (selected && event.currentTarget.returnValue === 'delete') {
        change('/' + current.workspace + '/runbooks/' + selected.id, 'DELETE', undefined, 'Article deleted.');
    }
});
$('article-search').addEventListener('submit', event => {
    event.preventDefault();
    if (current) {
        load(workspace.value, selected?.id);
        return;
    }
    const visible = renderArticles();
    openArticle(visible.includes(selected) ? selected : visible[0]);
});
for (const picker of [role, workspace]) picker.addEventListener('change', () => {
    search.value = '';
    load(workspace.value);
});

async function initialize() {
    account = await loadAccount();
    $('library').hidden = false;
    if (account) {
        updateAccountHeader(account);
        $('demo-account').hidden = true;
        memberForm.elements.username.removeAttribute('list');
        if (!account.username) {
            $('public-intro').hidden = false;
            $('catalog').hidden = false;
            articles = examples;
            openArticle(renderArticles()[0]);
            return;
        }
    }
    $('workspace-tools').hidden = false;
    await load();
}

initialize().catch(error => { $('startup-status').textContent = error.message; });
