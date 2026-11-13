const el = id => document.getElementById(id);
const key = 'daymark-guest-v1';
const date = new Intl.DateTimeFormat(undefined, { dateStyle: 'full' });
const dayLabel = new Intl.DateTimeFormat(undefined, { weekday: 'short' });
const duration = el('duration');
const dialog = el('task-dialog');
const form = el('task-form');
let account;
let board = { tasks: [], sessions: [] };
let editing;
let dragged;
let busy = false;
let remaining = Number(duration.value) * 60;
let deadline = 0;
let ticker;
let pendingSession;

function day(value = new Date()) {
    const local = new Date(value);
    return [local.getFullYear(), String(local.getMonth() + 1).padStart(2, '0'),
        String(local.getDate()).padStart(2, '0')].join('-');
}

function action(label, run, className = 'secondary') {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = className;
    button.textContent = label;
    button.onclick = run;
    return button;
}

async function request(path = '', method = 'GET', payload) {
    const response = await fetch('/api/planner' + path, {
        method, headers: { 'Content-Type': 'application/json', [account.csrfHeader]: account.csrfToken },
        body: payload === undefined ? undefined : JSON.stringify(payload)
    });
    if (response.status === 401 || response.status === 403) throw Error('Sign in again to save changes.');
    if (response.status === 409) throw Error('The task list changed. Reload before reordering.');
    if (!response.ok) throw Error('Could not save the change. Try again.');
    return response.status === 204 ? undefined : response.json();
}

async function change(path, method, payload, apply, feedback = el('notice')) {
    if (busy) return false;
    busy = true;
    feedback.textContent = '';
    try {
        if (account?.username) {
            await request(path, method, payload);
            board = await request();
        } else {
            const previous = structuredClone(board);
            try {
                apply();
                localStorage.setItem(key, JSON.stringify(board));
            } catch (error) {
                board = previous;
                throw Error('Your browser could not save this change.');
            }
        }
        el('suggestions').replaceChildren();
        render();
        return true;
    } catch (error) {
        feedback.textContent = error.message;
        render();
        return false;
    } finally {
        busy = false;
    }
}

function move(id, target) {
    const ids = board.tasks.map(task => task.id);
    const from = ids.indexOf(id);
    if (from < 0 || target < 0 || target >= ids.length || from === target) return;
    ids.splice(target, 0, ids.splice(from, 1)[0]);
    change('/order', 'PUT', ids, () => { board.tasks = ids.map(id => board.tasks.find(task => task.id === id)); });
}

function renderTasks() {
    const list = el('task-list');
    const category = el('category-filter');
    const chosen = category.value;
    const categories = [...new Set(board.tasks.map(task => task.category))].sort();
    category.replaceChildren(new Option('All categories', ''), ...categories.map(name => new Option(name, name)));
    category.value = categories.includes(chosen) ? chosen : '';
    const view = el('view').value;
    const reorder = view === 'all' && !category.value;
    const today = day();
    const visible = board.tasks.filter(task => (!category.value || category.value === task.category)
        && (view === 'all' || (view === 'today' && task.dueDate === today)
            || (view === 'upcoming' && task.dueDate && task.dueDate > today && !task.completedAt)
            || (view === 'done' && task.completedAt)));
    list.replaceChildren(...visible.map(task => {
        const row = document.createElement('li');
        row.className = 'task-row';
        const index = board.tasks.indexOf(task);
        const check = document.createElement('input');
        check.type = 'checkbox';
        check.checked = Boolean(task.completedAt);
        check.setAttribute('aria-label', 'Complete ' + task.title);
        check.onchange = () => {
            const done = check.checked;
            change('/tasks/' + task.id + '/completion', 'PUT', { done },
                () => { task.completedAt = done ? new Date().toISOString() : null; });
        };
        const name = document.createElement('div');
        name.className = task.completedAt ? 'task-name completed' : 'task-name';
        const title = document.createElement('strong');
        title.textContent = task.title;
        const detail = document.createElement('small');
        detail.textContent = task.category + (task.dueDate ? ' · ' + task.dueDate : '');
        name.append(title, detail);
        const controls = document.createElement('div');
        controls.className = 'task-actions';
        const edit = action('Edit', () => openEditor(task));
        controls.append(edit);
        if (reorder) {
            const up = action('↑', () => move(task.id, index - 1));
            const down = action('↓', () => move(task.id, index + 1));
            up.disabled = index === 0;
            down.disabled = index === board.tasks.length - 1;
            up.setAttribute('aria-label', 'Move ' + task.title + ' up');
            down.setAttribute('aria-label', 'Move ' + task.title + ' down');
            controls.append(up, down);
            row.draggable = true;
            row.ondragstart = event => { dragged = task.id; event.dataTransfer.setData('text/plain', String(task.id)); };
            row.ondragover = event => { if (dragged !== undefined) event.preventDefault(); };
            row.ondrop = event => { event.preventDefault(); move(dragged, index); };
            row.ondragend = () => { dragged = undefined; };
        }
        row.append(check, name, controls);
        return row;
    }));
    el('empty').hidden = visible.length !== 0;
}

function render() {
    renderTasks();
    const today = day();
    el('open-count').textContent = board.tasks.filter(task => !task.completedAt).length;
    el('done-count').textContent = board.tasks.filter(task => task.completedAt && day(task.completedAt) === today).length;
    const minutesByDay = new Map();
    for (const session of board.sessions) {
        const key = day(session.finishedAt);
        minutesByDay.set(key, (minutesByDay.get(key) ?? 0) + session.minutes);
    }
    el('focus-total').textContent = (minutesByDay.get(today) ?? 0) + ' min';
    const week = Array.from({ length: 7 }, (_, index) => {
        const date = new Date();
        date.setDate(date.getDate() - 6 + index);
        return { label: dayLabel.format(date), minutes: minutesByDay.get(day(date)) ?? 0 };
    });
    const max = Math.max(25, ...week.map(entry => entry.minutes));
    el('week-total').textContent = week.reduce((sum, entry) => sum + entry.minutes, 0) + ' min in the last 7 days';
    el('week').replaceChildren(...week.map(entry => {
        const item = document.createElement('li');
        const bar = document.createElement('progress');
        bar.max = max;
        bar.value = entry.minutes;
        bar.setAttribute('aria-label', entry.label + ': ' + entry.minutes + ' minutes');
        const label = document.createElement('span');
        label.textContent = entry.label;
        item.append(bar, label);
        return item;
    }));
}

function openEditor(task) {
    editing = task;
    form.reset();
    el('task-error').textContent = '';
    el('editor-title').textContent = task ? 'Edit task' : 'New task';
    form.elements.title.value = task?.title ?? '';
    form.elements.category.value = task?.category ?? 'General';
    form.elements.dueDate.value = task?.dueDate ?? '';
    el('delete-task')?.remove();
    if (task) {
        const remove = action('Delete task', async () => {
            if (remove.dataset.confirm !== 'yes') {
                remove.dataset.confirm = 'yes';
                remove.textContent = 'Confirm delete';
                return;
            }
            if (await change('/tasks/' + task.id, 'DELETE', undefined,
                () => { board.tasks = board.tasks.filter(item => item !== task); }, el('task-error'))) dialog.close();
        });
        remove.id = 'delete-task';
        form.querySelector('.timer-actions').append(remove);
    }
    dialog.showModal();
}

form.onsubmit = async event => {
    event.preventDefault();
    const fields = new FormData(form);
    const input = { title: fields.get('title').trim(), category: fields.get('category').trim(), dueDate: fields.get('dueDate') || null };
    if (!input.title || !input.category) { el('task-error').textContent = 'Enter a task and category.'; return; }
    if (await change(editing ? '/tasks/' + editing.id : '/tasks', editing ? 'PUT' : 'POST', input, () => {
        if (editing) Object.assign(editing, input);
        else board.tasks.push({ id: crypto.randomUUID(), ...input, completedAt: null });
    }, el('task-error'))) dialog.close();
};

function drawClock() {
    el('clock').textContent = String(Math.floor(remaining / 60)).padStart(2, '0') + ':' + String(remaining % 60).padStart(2, '0');
    el('dial').style.setProperty('--progress', (1 - remaining / (Number(duration.value) * 60)) * 100 + '%');
}

async function saveSession() {
    const session = pendingSession;
    if (!session || busy) return;
    if (await change('/focus/' + session.id, 'PUT', { minutes: session.minutes }, () => {
        board.sessions.push(session);
    })) {
        pendingSession = null;
        el('timer-state').textContent = 'Session logged';
        el('timer').textContent = 'Start focus';
    } else {
        el('timer-state').textContent = 'Session not saved';
        el('timer').textContent = 'Retry saving';
    }
}

function tick() {
    remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
    drawClock();
    if (remaining) return;
    clearInterval(ticker);
    deadline = 0;
    pendingSession = { id: crypto.randomUUID(), minutes: Number(duration.value), finishedAt: new Date().toISOString() };
    el('timer').textContent = 'Save session';
    saveSession();
}

function resetTimer() {
    clearInterval(ticker);
    deadline = 0;
    remaining = Number(duration.value) * 60;
    duration.disabled = false;
    el('timer').textContent = 'Start focus';
    el('timer-state').textContent = 'Ready';
    drawClock();
}

el('timer').onclick = () => {
    if (pendingSession) { saveSession(); return; }
    if (deadline) {
        clearInterval(ticker);
        remaining = Math.max(1, Math.ceil((deadline - Date.now()) / 1000));
        deadline = 0;
        el('timer').textContent = 'Resume focus';
        el('timer-state').textContent = 'Paused';
        drawClock();
        return;
    }
    if (!remaining) resetTimer();
    duration.disabled = true;
    deadline = Date.now() + remaining * 1000;
    el('timer').textContent = 'Pause';
    el('timer-state').textContent = 'Focusing';
    ticker = setInterval(tick, 250);
};

el('reset').onclick = () => { if (!pendingSession) resetTimer(); };
duration.onchange = resetTimer;
el('add').onclick = () => openEditor();
el('cancel').onclick = () => dialog.close();
el('view').onchange = renderTasks;
el('category-filter').onchange = renderTasks;
el('suggest').onclick = () => {
    const next = board.tasks.filter(task => !task.completedAt).sort((a, b) =>
        (a.dueDate ?? '9999-12-31').localeCompare(b.dueDate ?? '9999-12-31')).slice(0, 3);
    el('suggestions').replaceChildren(...next.map(task => {
        const item = document.createElement('li');
        item.textContent = task.title;
        return item;
    }));
    if (!next.length) el('notice').textContent = 'Add an open task to build a plan.';
};

async function initialize() {
    el('today').textContent = date.format(new Date());
    account = await loadAccount();
    if (account) updateAccountHeader(account);
    if (account?.username) {
        board = await request();
        el('mode').textContent = 'Your tasks are saved to your account.';
    } else {
        const saved = localStorage.getItem(key);
        board = saved ? JSON.parse(saved) : { tasks: [
            { id: 'sample-1', title: 'Review the week’s priorities', category: 'Work', dueDate: day(), completedAt: null },
            { id: 'sample-2', title: 'Read a chapter', category: 'Study', dueDate: null, completedAt: null },
            { id: 'sample-3', title: 'Pick up groceries', category: 'Personal', dueDate: day(), completedAt: null }
        ], sessions: [] };
        el('mode').textContent = 'Guest planner. Changes stay in this browser. Sign in for a separate private planner.';
    }
    for (const id of ['add', 'timer', 'reset', 'suggest']) el(id).disabled = false;
    render();
}
initialize().catch(error => { el('mode').textContent = error.message; });
