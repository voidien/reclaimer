"""Retro Diffusion edit tools and Pixel Fixer for rd_generate.py: fix art we already have.

    python3 tools/art/rd_generate.py --fix asteroid_silicate --tool palette        # onto its style-guide palette
    python3 tools/art/rd_generate.py --fix carbon_flakes=2 --tool pixel            # Pixel Fixer on staged variant 2
    python3 tools/art/rd_generate.py --fix ashen_slag --tool reduce --colors 8 --dry-run

The input is the asset's current file in the mod, or a staged variant with ``asset=n``. The result is saved as the
asset's next variant (art/rd/<asset>/v<N>.png) and shows on the contact sheet, so ``--promote asset=N`` puts it in
the mod like any other variant. Spending works as for generation: a free estimate first, the balance checked, a
question before anything is paid, and re-runs never pay twice (edit tools run as RD tasks keyed by a ``custom_id``
made from the asset, the tool and the input image; Pixel Fixer results are remembered in the state file).

Called from rd_generate.main with the rd_generate module, whose paths the tests patch.
"""
import base64
import hashlib
import os
import sys
import time

# tool name -> (RD edit tool id or Pixel Fixer endpoint, offline price in USD)
TOOLS = {
    'palette': ('palette_converter', 0.001),
    'reduce': ('color_reducer', 0.001),
    'downscale': ('k_centroid_downscale', 0.001),
    'unbg': ('background_remover', 0.01),
    'correct': ('pixel_correction', 0.001),
    'pixel': ('/pixel-fixer/standard', 0.002),
    'pixel-neural': ('/pixel-fixer/neural', 0.005),
}
POLL_SECONDS = float(os.environ.get('RD_POLL_SECONDS', '3'))


def source(rd, rows, spec):
    """(row, png bytes, where) for ``asset`` (the mod's file) or ``asset=n`` (staged variant n)."""
    akey, _, n = spec.partition('=')
    row = next((r for r in rows if r['key'] == akey), None)
    if not row:
        sys.exit('unknown asset %r' % akey)
    if n:
        path = os.path.join(rd.STAGING, akey, 'v%s.png' % n)
    else:
        if not row['file'] or any(c in row['file'] for c in '*<{'):
            sys.exit('%s has no single file in the mod; fix a staged variant instead (%s=n)' % (akey, akey))
        path = os.path.join(rd.REPO, row['file'])
    if not os.path.exists(path):
        sys.exit('no image to fix at %s' % os.path.relpath(path, rd.REPO))
    with open(path, 'rb') as f:
        return row, f.read(), os.path.relpath(path, rd.REPO)


def body_for(rd, tool, row, png, args):
    body = {'input_image': base64.b64encode(png).decode()}
    if tool == 'palette':
        body.update(input_palette=rd.palette_b64(rd.category_for(row, 'image')), dither_mode='none', dither_strength=0)
    elif tool == 'reduce':
        body.update(color_count=args.colors, dither_mode='none', dither_strength=0)
    elif tool == 'downscale':
        w, h = rd.target_size(row)
        body.update(width=w, height=h)
    elif tool == 'pixel-neural':
        w, h = rd.target_size(row)
        body.update(width=w, height=h)
    return body


def price(rd, tool, body, key):
    """(price, 'RD' or 'offline'). Edit tools have a free estimate; the Pixel Fixer has fixed prices."""
    endpoint, offline = TOOLS[tool]
    if key and not endpoint.startswith('/'):
        try:
            res = rd.request('POST', '/edit/tools/%s/estimate' % endpoint, key, body)
            return float(res['balance_cost']), 'RD'
        except (rd.ApiError, KeyError, TypeError, ValueError):
            pass
    return offline, 'offline'


def next_variant(rd, akey):
    folder = os.path.join(rd.STAGING, akey)
    taken = [int(f[1:-4]) for f in (os.listdir(folder) if os.path.isdir(folder) else [])
             if f.startswith('v') and f.endswith('.png') and f[1:-4].isdigit()]
    return max(taken, default=0) + 1


def run(rd, args, rows, key):
    tool = args.tool
    if tool not in TOOLS:
        sys.exit('--tool must be one of: ' + ', '.join(sorted(TOOLS)))
    state = rd.load_state()
    fixes = state.setdefault('fixes', {})
    plan = []
    for spec in args.fix:
        row, png, where = source(rd, rows, spec)
        body = body_for(rd, tool, row, png, args)
        digest = hashlib.sha256(png).hexdigest()
        custom_id = rd.idem('fix', row['key'], tool, sorted((k, v) for k, v in body.items() if k != 'input_image'),
                            digest, args.again and time.time())
        old = fixes.get(custom_id)
        if old and old.get('status') == 'done':
            print('%s: already fixed with %s as v%s (--again for a new one)' % (row['key'], tool, old['variant']))
            continue
        plan.append({'row': row, 'body': body, 'where': where, 'custom_id': custom_id})
    if not plan:
        return 0

    total = 0.0
    print('%-28s %-13s %-44s %8s' % ('asset', 'tool', 'input', 'price'))
    for job in plan:
        job['price'], job['price_source'] = price(rd, tool, job['body'], None if args.dry_run else key)
        total += job['price']
        print('%-28s %-13s %-44s %7.3f%s' % (job['row']['key'], tool, job['where'][-44:], job['price'],
                                           '' if job['price_source'] == 'RD' else '*'))
    print('total about $%.3f (* = offline price)' % total)
    if args.dry_run:
        return 0
    balance = rd.account_balance(key)
    if balance is not None and balance < total:
        sys.exit('Not enough balance: $%.2f, need about $%.3f.' % (balance, total))
    if not args.yes and input('Spend about $%.3f on %d fixes? [y/N] ' % (total, len(plan))).strip().lower() != 'y':
        print('Nothing sent.')
        return 0

    for job in plan:
        row, akey = job['row'], job['row']['key']
        endpoint = TOOLS[tool][0]
        rec = fixes.setdefault(job['custom_id'], {'asset': akey, 'tool': tool, 'status': 'submitted'})
        rd.save_state(state)
        try:
            png = run_pixel_fixer(rd, endpoint, job['body'], key) if endpoint.startswith('/') \
                else run_edit_tool(rd, endpoint, job, rec, state, key)
        except rd.ApiError as e:
            rec.update(status='failed', error=str(e)[:300])
            rd.save_state(state)
            print('%s: %s failed: %s' % (akey, tool, e))
            continue
        n = next_variant(rd, akey)
        os.makedirs(os.path.join(rd.STAGING, akey), exist_ok=True)
        rd.write_file(os.path.join(rd.STAGING, akey, 'v%d.png' % n), png)
        rec.update(status='done', variant=n)
        asset = state['assets'].setdefault(akey, {'group': row['group'], 'file': row['file'], 'jobs': []})
        asset['jobs'].append({'kind': 'fix-' + tool, 'status': 'done', 'variant': n})
        rd.save_state(state)
        print('%s: %s -> v%d (promote with --promote %s=%d)' % (akey, tool, n, akey, n))
    rd.write_index(state)
    return 0


def run_pixel_fixer(rd, endpoint, body, key):
    res = rd.request('POST', endpoint, key, body)
    return base64.b64decode(res['base64_images'][0])


def run_edit_tool(rd, tool_id, job, rec, state, key):
    """Runs an edit tool as an RD task: the custom_id makes a repeat return the first task instead of charging again."""
    if not rec.get('task_id'):
        res = rd.request('POST', '/edit/tools/%s/tasks' % tool_id, key, dict(job['body'], custom_id=job['custom_id']))
        rec['task_id'] = res['task_id']
        rd.save_state(state)
    while True:
        task = rd.request('GET', '/edit/tasks/%s' % rec['task_id'], key)
        if task.get('status') not in ('pending', 'running', 'accepted'):
            break
        time.sleep(POLL_SECONDS)
    if task.get('status') != 'succeeded':
        raise rd.ApiError(0, str(task.get('error') or task))
    result = task.get('result') or {}
    if result.get('base64_images'):
        return base64.b64decode(result['base64_images'][0])
    return rd.download(result['output_urls'][0])
