"""Retro Diffusion low-poly models for rd_generate.py, beyond generate and animate: revise a model and export it.

    python3 tools/art/rd_generate.py --model-revise core_mk2_module --prompt "add two cyan cooling fins"
    python3 tools/art/rd_generate.py --model-revise core_mk2_module --prompt "..." --version 1 --dry-run
    python3 tools/art/rd_generate.py --model-export core_mk2_module --target minecraft --target obj

Both work on a model RD already holds for the asset: the asset_id of its newest finished model job in the state file.
A revision is a new version of that model; it runs as a model job, so the usual polling saves its render, turntable,
.bbmodel and atlas into art/rd/<asset>/ and ``--promote <asset>`` works as for a new model. It is paid: priced from
RD's table, the balance checked, a question before anything is spent, and an Idempotency-Key so a retry is not paid
twice. Exports are free and land in art/rd/<asset>/export/.

Called from rd_generate.main with the rd_generate module, whose paths the tests patch.
"""
import os
import sys

# RD's revise prices by model size (texels); Lite models are a flat price. Exports are free.
REVISE_PRICE = {16: 0.20, 32: 0.35, 64: 0.65, 128: 0.90, 256: 1.50}
LITE_REVISE = 0.08
UNKNOWN_SIZE = 64
EXPORT_TARGETS = ('minecraft', 'blockbench', 'obj', 'atlas', 'turntable', 'axis_views', 'blender', 'web')


def model_of(rd, state, akey):
    """The newest finished model job of an asset (it carries RD's asset_id), or exits with a hint."""
    jobs = state['assets'].get(akey, {}).get('jobs', [])
    done = [j for j in jobs if j['kind'] == 'model' and j['status'] == 'done' and j.get('asset_id')]
    if not done:
        sys.exit('%s has no finished RD model yet: generate one first (--asset %s --method 3d)' % (akey, akey))
    return done[-1]


def revise_price(job):
    if job.get('style', '').endswith('_lite'):
        return LITE_REVISE, 'table'
    size = job.get('size')
    return REVISE_PRICE.get(size if isinstance(size, int) else UNKNOWN_SIZE, REVISE_PRICE[UNKNOWN_SIZE]), \
        'table' if isinstance(size, int) else 'table, size assumed %d' % UNKNOWN_SIZE


def revise(rd, args, key):
    if not args.prompt:
        sys.exit('--model-revise needs --prompt "what to change"')
    state = rd.load_state()
    akey = args.model_revise
    model = model_of(rd, state, akey)
    cost, source = revise_price(model)
    print('revise %s (RD asset %s%s): "%s"' % (akey, model['asset_id'],
                                             ', version %d' % args.version if args.version else ', newest version',
                                             args.prompt[:80]))
    print('price about $%.2f (%s)' % (cost, source))
    if args.dry_run:
        return 0
    balance = rd.account_balance(key)
    if balance is not None and balance < cost:
        sys.exit('Not enough balance: $%.2f, need about $%.2f.' % (balance, cost))
    if not args.yes and input('Spend about $%.2f on this revision? [y/N] ' % cost).strip().lower() != 'y':
        print('Nothing sent.')
        return 0
    body = {'prompt': args.prompt}
    if args.version:
        body['version'] = args.version
    k = rd.idem('revise', akey, model['asset_id'], args.prompt, args.version)
    res = rd.request('POST', '/lowpoly/assets/%s/revise' % model['asset_id'], key, body, idempotency=k)
    rec = dict(kind='model', task=res['task_id'], asset_id=model['asset_id'], status='submitted', style=model.get('style'),
               size=model.get('size'), revision=args.prompt)
    rd.add_job(state['assets'][akey], rec)
    rd.save_state(state)
    print('  submitted revision of %s' % akey)
    rd.poll(key, state)
    rd.write_index(state)
    return 0


def export(rd, args, key):
    targets = args.target or ['minecraft']
    for target in targets:
        if target not in EXPORT_TARGETS:
            sys.exit('--target must be one of: ' + ', '.join(EXPORT_TARGETS))
    state = rd.load_state()
    akey = args.model_export
    model = model_of(rd, state, akey)
    folder = os.path.join(rd.STAGING, akey, 'export')
    if args.dry_run:
        print('would export %s (RD asset %s) as: %s (free)' % (akey, model['asset_id'], ', '.join(targets)))
        return 0
    os.makedirs(folder, exist_ok=True)
    for target in targets:
        body = {'target': target}
        if args.version:
            body['version'] = args.version
        res = rd.request('POST', '/lowpoly/assets/%s/export' % model['asset_id'], key, body)
        name = os.path.basename(res.get('filename') or '%s.%s' % (target, 'zip' if target in ('minecraft', 'obj', 'axis_views') else 'bin'))
        rd.write_file(os.path.join(folder, name), rd.download(res['url']))
        print('  %s -> %s' % (target, os.path.relpath(os.path.join(folder, name), rd.REPO)))
    return 0
