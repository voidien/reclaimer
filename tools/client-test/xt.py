# Drives the headless test client on Xvfb display :99 through XTest (see HANDOFF.md).
# Usage: xt.py click X Y | rclick | hover X Y | scroll N | type "text" | key KEYSYM [hold_seconds] | chord ctrl+f | mdrag DX DY [steps [shot.png]] | move DX DY
import ctypes, sys, time
X = ctypes.cdll.LoadLibrary('libX11.so.6'); T = ctypes.cdll.LoadLibrary('libXtst.so.6')
X.XOpenDisplay.restype = ctypes.c_void_p; X.XStringToKeysym.restype = ctypes.c_ulong
X.XKeysymToKeycode.argtypes = [ctypes.c_void_p, ctypes.c_ulong]
T.XTestFakeKeyEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int, ctypes.c_ulong]
T.XTestFakeButtonEvent.argtypes = [ctypes.c_void_p, ctypes.c_uint, ctypes.c_int, ctypes.c_ulong]
T.XTestFakeMotionEvent.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_ulong]
T.XTestFakeRelativeMotionEvent.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_ulong]
X.XFlush.argtypes = [ctypes.c_void_p]
d = X.XOpenDisplay(b':99')
def key(sym, down):
    kc = X.XKeysymToKeycode(d, X.XStringToKeysym(sym.encode())); T.XTestFakeKeyEvent(d, kc, down, 0); X.XFlush(d)
def tap(sym, hold=0.09):
    key(sym, 1); time.sleep(hold); key(sym, 0); time.sleep(0.08)
SYM = {' ': 'space', '/': 'slash', ':': 'colon', '_': 'underscore', '.': 'period', '-': 'minus', '@': 'at', '~': 'asciitilde', '=': 'equal', ',': 'comma', '[': 'bracketleft', ']': 'bracketright', '"': 'quotedbl', '{': 'braceleft', '}': 'braceright'}
SHIFT = set(':_@~"{}') | set('ABCDEFGHIJKLMNOPQRSTUVWXYZ')
a = sys.argv
if a[1] == 'click':
    T.XTestFakeMotionEvent(d, 0, int(a[2]), int(a[3]), 0); X.XFlush(d); time.sleep(0.2)
    T.XTestFakeButtonEvent(d, 1, 1, 0); X.XFlush(d); time.sleep(0.1); T.XTestFakeButtonEvent(d, 1, 0, 0); X.XFlush(d)
elif a[1] == 'rclick':
    T.XTestFakeButtonEvent(d, 3, 1, 0); X.XFlush(d); time.sleep(0.15); T.XTestFakeButtonEvent(d, 3, 0, 0); X.XFlush(d)
elif a[1] == 'scroll':
    b = 4 if int(a[2]) > 0 else 5
    for _ in range(abs(int(a[2]))):
        T.XTestFakeButtonEvent(d, b, 1, 0); X.XFlush(d); time.sleep(0.05); T.XTestFakeButtonEvent(d, b, 0, 0); X.XFlush(d); time.sleep(0.15)
elif a[1] == 'type':
    for ch in a[2]:
        s = SYM.get(ch, ch)
        if ch in SHIFT: key('Shift_L', 1)
        tap(s)
        if ch in SHIFT: key('Shift_L', 0)
elif a[1] == 'key':
    tap(a[2], float(a[3]) if len(a) > 3 else 0.05)
elif a[1] == 'chord':  # chord ctrl+f : hold the modifiers, tap the last key
    parts = [{'ctrl': 'Control_L', 'shift': 'Shift_L', 'alt': 'Alt_L'}.get(x, x) for x in a[2].split('+')]
    for m in parts[:-1]: key(m, 1)
    tap(parts[-1])
    for m in reversed(parts[:-1]): key(m, 0)
elif a[1] == 'mdrag':  # mdrag DX DY [steps]: hold the middle mouse button and move the pointer by DX, DY (relative) in steps
    steps = int(a[4]) if len(a) > 4 else 20
    T.XTestFakeButtonEvent(d, 2, 1, 0); X.XFlush(d); time.sleep(0.3)
    for _ in range(steps):
        T.XTestFakeRelativeMotionEvent(d, int(a[2]) // steps, int(a[3]) // steps, 0); X.XFlush(d); time.sleep(0.05)
    time.sleep(0.3)
    if len(a) > 5:  # a screenshot path: take it while the button is still held
        import subprocess, os
        subprocess.run(['import', '-window', 'root', a[5]], env=dict(os.environ, DISPLAY=':99'))
    T.XTestFakeButtonEvent(d, 2, 0, 0); X.XFlush(d)
elif a[1] == 'hover':
    T.XTestFakeMotionEvent(d, 0, int(a[2]), int(a[3]), 0); X.XFlush(d)
elif a[1] == 'move':
    T.XTestFakeRelativeMotionEvent(d, int(a[2]), int(a[3]), 0); X.XFlush(d)
