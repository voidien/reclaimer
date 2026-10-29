// The contact sheet's picker (inlined into art/rd/index.html by rd_generate.py's write_index).
// Each asset can be marked: a variant number or "model" to promote, "keep" (keep the current file) or "redo" (new
// variants), plus a note. Picks live in the browser's storage for this page; the page turns them into the commands
// to paste into a terminal. buildCommands is pure, so tools/art/test_rd_generate.py runs it under Node.

const TOOL = 'python3 tools/art/rd_generate.py';

/** The commands for a set of picks: {key: {pick, note}} -> list of command lines. */
function buildCommands(picks) {
  const keys = Object.keys(picks).sort();
  const promote = [];
  const redo = [];
  for (const key of keys) {
    const pick = picks[key] && picks[key].pick;
    if (pick === 'model') promote.push('--promote ' + key);
    else if (/^\d+$/.test(pick || '')) promote.push('--promote ' + key + '=' + pick);
    else if (pick === 'redo') redo.push('--asset ' + key);
  }
  const lines = [];
  if (promote.length) lines.push(TOOL + ' ' + promote.join(' '));
  if (redo.length) lines.push(TOOL + ' --again ' + redo.join(' '));
  return lines;
}

if (typeof module !== 'undefined') module.exports = { buildCommands };

if (typeof document !== 'undefined') {
  const STORE = 'rd-picks:' + location.pathname;
  let picks = {};
  try { picks = JSON.parse(localStorage.getItem(STORE) || '{}') || {}; } catch (e) { picks = {}; }
  const cards = Array.from(document.querySelectorAll('.a[data-key]'));
  const known = new Set(cards.map(c => c.dataset.key));
  for (const key of Object.keys(picks)) if (!known.has(key)) delete picks[key];

  const save = () => { try { localStorage.setItem(STORE, JSON.stringify(picks)); } catch (e) { /* private window: picks last this visit */ } };
  const entry = key => (picks[key] = picks[key] || { pick: '', note: '' });
  const decided = card => !!(picks[card.dataset.key] && picks[card.dataset.key].pick) || card.dataset.promoted !== '';
  const cmd = document.getElementById('cmd');
  const hide = document.getElementById('hide');
  const copied = document.getElementById('copied');

  function render() {
    let done = 0;
    for (const card of cards) {
      const p = picks[card.dataset.key] || {};
      card.querySelectorAll('[data-pick]').forEach(b => b.classList.toggle('on', b.dataset.pick === p.pick));
      const isDone = decided(card);
      if (isDone) done++;
      card.classList.toggle('done', isDone);
      card.hidden = hide.checked && isDone;
      const label = { keep: 'keep current', redo: 'redo', model: 'promote model' }[p.pick] || (p.pick ? 'promote v' + p.pick : '');
      card.querySelector('.state').textContent = label || (card.dataset.promoted ? 'promoted ' + card.dataset.promoted : '');
    }
    document.getElementById('count').textContent = done + ' of ' + cards.length + ' decided';
    const lines = buildCommands(picks);
    cmd.value = lines.join('\n');
    cmd.style.height = 'auto';  // fit the wrapped lines (long on a phone), up to a third of the screen
    cmd.style.height = Math.min(cmd.scrollHeight + 2, innerHeight / 3) + 'px';
    document.body.style.paddingBottom = document.querySelector('footer').offsetHeight + 16 + 'px';
    for (const key of Object.keys(picks)) if (!picks[key].pick && !picks[key].note) delete picks[key];
    save();
  }

  for (const card of cards) {
    const key = card.dataset.key;
    card.querySelectorAll('[data-pick]').forEach(b => b.addEventListener('click', () => {
      const e = entry(key);
      e.pick = e.pick === b.dataset.pick ? '' : b.dataset.pick;  // a second click undoes
      render();
    }));
    const note = card.querySelector('.note');
    note.value = (picks[key] && picks[key].note) || '';
    note.addEventListener('input', () => { entry(key).note = note.value; save(); });
  }
  hide.addEventListener('change', render);
  document.getElementById('clear').addEventListener('click', () => {
    if (confirm('Clear every pick and note on this page?')) { picks = {}; cards.forEach(c => (c.querySelector('.note').value = '')); render(); }
  });
  document.getElementById('copy').addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(cmd.value);
    } catch (e) {
      cmd.select();  // no clipboard access (some file viewers): fall back to the old way, or a manual copy
      try { document.execCommand('copy'); } catch (e2) { copied.textContent = 'Select the text and copy it'; return; }
    }
    copied.textContent = cmd.value ? 'Copied' : 'Nothing picked yet';
    setTimeout(() => (copied.textContent = ''), 2000);
  });
  render();
}
