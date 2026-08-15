# WifeSignalEnclosure - Autodesk Fusion 360 Add-In
# ------------------------------------------------------------------
# Two separately-printable parts:
#   BODY (open-front tray) - holds the ESP32 standing on edge in a side channel,
#                            with the USB-C slot in the LEFT wall, and 4 corner
#                            pillars with press-fit M4 nut pockets.
#   LID  (front face)      - the 3 arcade-button holes + 4 countersunk M4 bolt holes.
# One end is an oval (semicircle); the other end is flat/rectangular. All outer
# corners are filleted at a single consistent radius (ROUND_R).
#
# Running the add-in WIPES the active design, then builds - no manual prep.
# Open a design first, then Run it from the Scripts and Add-Ins dialog.
# Optional features are guarded; the summary dialog reports per-step status.
# ------------------------------------------------------------------

import adsk.core, adsk.fusion, traceback, math

# Build stamp - bump VERSION whenever the file changes so you can confirm
# in Fusion that you're running the latest copy (shown in the summary dialog).
VERSION    = "v15"
BUILD_STAMP = "2026-07-27c"   # nut pocket corners rounded (HEX_CORNER_R) so they print as a ring, not fingers

# ================= PARAMETERS (millimetres) =================
# ---- buttons ----
BUTTON_HOLE_DIA = 30.0
BUTTON_SPACING  = 37.0     # vertical centre-to-centre (closer together)
SIDE_MARGIN     = 9.0      # hole EDGE to side wall  -> width  ~48
END_MARGIN      = 21.0     # outer hole CENTRES to end -> height ~116
WALL            = 3.0
BODY_DEPTH      = 37.0     # tray depth (was 30; +7mm so the taller ESP clears the lid) -> ~41 total

# ---- board: ESP32-C3 SuperMini Plus, mounted VERTICAL (on edge) ----
BOARD_L = 22.5             # runs along Z (depth); USB is on the back edge
BOARD_W = 18.0             # runs along Y (height)
BOARD_T = 5.0             # thickness, along X (against the side wall)
USB_OVERHANG = 1.5

# ---- (cradle removed - board now mounts flat on the pillars via the PCB) ----

# ---- USB-C cut-out: LEFT (-X) wall. The vertical (Z) window is COMPUTED from real
#      measurements of the assembled board so it hugs the actual receptacle.
#      (Y position along the wall still needs your measurement - see USB_Y.)
PCB_SPACER           = 2.0    # spacers under the PCB (PCB floats this far above the tray floor)
PCB_THICK            = 1.5    # WifeSignal PCB thickness
USB_FRONT_ABOVE_PCB  = 15.7   # receptacle front-facing (lid-side) edge, above the PCB top surface
USB_BACK_ABOVE_FLOOR = 7.5    # receptacle bottom-facing (floor-side) edge, above the tray floor
USB_MARGIN           = 0.5    # clearance added around the measured window (small = tight)
USB_CUTOUT_W         = 10.0   # opening along Y (wall length) - tighter than before
USB_Y                = -44.0  # position along the wall, toward the bottom  <-- ESTIMATE, verify

# ---- lid ----
LID_THICKNESS = 4.0

# ---- M4 screw mounting ----
PILLAR_DIA = 9.0
NUT_AF     = 6.9
NUT_DEPTH  = 3.5
HEX_CORNER_R = 0.8   # round the nut-pocket corners so the thin corner walls print
                     # as a continuous ring (not ragged "fingers"). Bigger = cleaner
                     # ring but less room for the nut's corners; ~0.8 still seats an M4.
BOLT_CLEAR = 4.5
BOLT_LEN   = 10.0
CSK_DIA    = 8.4

# ---- rounding (one consistent radius everywhere) ----
ROUND_R       = 3.0
CORNER_R      = ROUND_R
USB_ROUND     = ROUND_R
LID_TOP_ROUND = ROUND_R
OVAL_TOP      = True       # round the TOP end into a semicircle; bottom stays flat
WIPE_FIRST    = True
# ===========================================================

MM = 0.1
NEW  = adsk.fusion.FeatureOperations.NewBodyFeatureOperation
CUT  = adsk.fusion.FeatureOperations.CutFeatureOperation
JOIN = adsk.fusion.FeatureOperations.JoinFeatureOperation

_app = None
_ui = None


def _rect(sketch, cx, cy, w, h):
    sketch.sketchCurves.sketchLines.addTwoPointRectangle(
        adsk.core.Point3D.create((cx - w / 2) * MM, (cy - h / 2) * MM, 0),
        adsk.core.Point3D.create((cx + w / 2) * MM, (cy + h / 2) * MM, 0))


def _outer_profile(sketch, w, h):
    if not OVAL_TOP:
        _rect(sketch, 0, 0, w, h)
        return
    r = w / 2.0
    y_bot = -h / 2.0
    yc = h / 2.0 - r
    L = sketch.sketchCurves.sketchLines
    A = adsk.core.Point3D.create(-w / 2 * MM, y_bot * MM, 0)
    B = adsk.core.Point3D.create( w / 2 * MM, y_bot * MM, 0)
    C = adsk.core.Point3D.create( w / 2 * MM, yc * MM, 0)
    lb = L.addByTwoPoints(A, B)
    lr = L.addByTwoPoints(lb.endSketchPoint, C)
    center = adsk.core.Point3D.create(0, yc * MM, 0)
    arc = sketch.sketchCurves.sketchArcs.addByCenterStartSweep(center, lr.endSketchPoint, math.pi)
    L.addByTwoPoints(arc.endSketchPoint, lb.startSketchPoint)


def _plane_z(comp, z):
    pin = comp.constructionPlanes.createInput()
    pin.setByOffset(comp.xYConstructionPlane, adsk.core.ValueInput.createByReal(z * MM))
    return comp.constructionPlanes.add(pin)


def _extrude(comp, profile, dist, op):
    return comp.features.extrudeFeatures.addSimple(
        profile, adsk.core.ValueInput.createByReal(dist * MM), op)


def _box_at(comp, cx, cy, z0, z1, sx, sy, op):
    # box: sx in X, sy in Y, spanning Z from z0 to z1. Reliable XY sketch + Z extrude.
    plane = _plane_z(comp, z0)
    sk = comp.sketches.add(plane)
    _rect(sk, cx, cy, sx, sy)
    ein = comp.features.extrudeFeatures.createInput(sk.profiles.item(0), op)
    ein.setDistanceExtent(False, adsk.core.ValueInput.createByReal((z1 - z0) * MM))
    return comp.features.extrudeFeatures.add(ein)


def _cut_box(comp, target, cx, cy, cz, sx, sy, sz):
    plane = _plane_z(comp, cz)
    sk = comp.sketches.add(plane)
    _rect(sk, cx, cy, sx, sy)
    ein = comp.features.extrudeFeatures.createInput(sk.profiles.item(0), NEW)
    ein.setSymmetricExtent(adsk.core.ValueInput.createByReal(sz * MM), True)
    tool = comp.features.extrudeFeatures.add(ein).bodies.item(0)
    tools = adsk.core.ObjectCollection.create()
    tools.add(tool)
    cin = comp.features.combineFeatures.createInput(target, tools)
    cin.operation = CUT
    cin.isKeepToolBodies = False
    comp.features.combineFeatures.add(cin)


def _circle_cut(comp, cx, cy, z_top, depth, dia, target=None):
    plane = _plane_z(comp, z_top)
    sk = comp.sketches.add(plane)
    sk.sketchCurves.sketchCircles.addByCenterRadius(
        adsk.core.Point3D.create(cx * MM, cy * MM, 0), (dia / 2.0) * MM)
    ein = comp.features.extrudeFeatures.createInput(sk.profiles.item(0), CUT)
    ein.setDistanceExtent(False, adsk.core.ValueInput.createByReal(-depth * MM))
    if target:
        ein.participantBodies = [target]
    comp.features.extrudeFeatures.add(ein)


def _hex_cut(comp, cx, cy, z_top, depth, af, target=None):
    plane = _plane_z(comp, z_top)
    sk = comp.sketches.add(plane)
    rc = HEX_CORNER_R
    if rc <= 0.001:
        # sharp hexagon (original behaviour)
        R = (af / 2.0) / math.cos(math.radians(30))
        pts = [adsk.core.Point3D.create((cx + R * math.cos(math.radians(60 * i))) * MM,
                                        (cy + R * math.sin(math.radians(60 * i))) * MM, 0)
               for i in range(6)]
        L = sk.sketchCurves.sketchLines
        segs = [L.addByTwoPoints(pts[0], pts[1])]
        for i in range(1, 5):
            segs.append(L.addByTwoPoints(segs[-1].endSketchPoint, pts[i + 1]))
        L.addByTwoPoints(segs[-1].endSketchPoint, segs[0].startSketchPoint)
    else:
        # rounded-corner hexagon: 6 arcs (at the corners) + 6 straight flats
        R = (af / 2.0) / math.cos(math.radians(30))          # vertex circumradius
        cdist = R - rc / math.sin(math.radians(60))          # arc-centre distance
        toff = rc / math.tan(math.radians(60))               # tangent offset along a flat
        def P(x, y):
            return adsk.core.Point3D.create((cx + x) * MM, (cy + y) * MM, 0)
        def vert(i):
            a = math.radians(60 * i); return (R * math.cos(a), R * math.sin(a))
        def cen(i):
            a = math.radians(60 * i); return (cdist * math.cos(a), cdist * math.sin(a))
        def unit(a, b):
            d = math.hypot(b[0] - a[0], b[1] - a[1]); return ((b[0] - a[0]) / d, (b[1] - a[1]) / d)
        Tm = []  # entry tangent point per corner (toward previous vertex)
        for i in range(6):
            Vi = vert(i)
            um = unit(Vi, vert(i - 1))
            Tm.append((Vi[0] + toff * um[0], Vi[1] + toff * um[1]))
        arcs = sk.sketchCurves.sketchArcs
        arc_objs = []
        for i in range(6):
            arc_objs.append(arcs.addByCenterStartSweep(
                P(*cen(i)), P(*Tm[i]), math.radians(60)))
        L = sk.sketchCurves.sketchLines
        for i in range(6):
            L.addByTwoPoints(arc_objs[i].endSketchPoint, arc_objs[(i + 1) % 6].startSketchPoint)
    ein = comp.features.extrudeFeatures.createInput(sk.profiles.item(0), CUT)
    ein.setDistanceExtent(False, adsk.core.ValueInput.createByReal(-depth * MM))
    if target:
        ein.participantBodies = [target]
    comp.features.extrudeFeatures.add(ein)


def _cyl_join(comp, cx, cy, z_bot, z_top, dia):
    plane = _plane_z(comp, z_bot)
    sk = comp.sketches.add(plane)
    sk.sketchCurves.sketchCircles.addByCenterRadius(
        adsk.core.Point3D.create(cx * MM, cy * MM, 0), (dia / 2.0) * MM)
    ein = comp.features.extrudeFeatures.createInput(sk.profiles.item(0), JOIN)
    ein.setDistanceExtent(False, adsk.core.ValueInput.createByReal((z_top - z_bot) * MM))
    comp.features.extrudeFeatures.add(ein)


def _countersink(comp, cx, cy, top_z, thickness, target):
    _circle_cut(comp, cx, cy, top_z, thickness + 0.5, BOLT_CLEAR, target)
    depth = (CSK_DIA - BOLT_CLEAR) / 2.0
    try:
        plane = _plane_z(comp, top_z)
        sk = comp.sketches.add(plane)
        sk.sketchCurves.sketchCircles.addByCenterRadius(
            adsk.core.Point3D.create(cx * MM, cy * MM, 0), (CSK_DIA / 2.0) * MM)
        ein = comp.features.extrudeFeatures.createInput(sk.profiles.item(0), CUT)
        dd = adsk.fusion.DistanceExtentDefinition.create(
            adsk.core.ValueInput.createByReal(depth * MM))
        ein.setOneSideExtent(dd, adsk.fusion.ExtentDirections.NegativeExtentDirection,
                             adsk.core.ValueInput.createByReal(math.radians(-45)))
        if target:
            ein.participantBodies = [target]
        comp.features.extrudeFeatures.add(ein)
    except:
        _circle_cut(comp, cx, cy, top_z, depth, CSK_DIA, target)


def _fillet_edges(comp, edges, radius):
    if edges.count:
        fin = comp.features.filletFeatures.createInput()
        fin.addConstantRadiusEdgeSet(edges, adsk.core.ValueInput.createByReal(radius * MM), True)
        comp.features.filletFeatures.add(fin)


def _fillet_vertical(comp, body, radius):
    edges = adsk.core.ObjectCollection.create()
    for e in body.edges:
        g = e.geometry
        if isinstance(g, adsk.core.Line3D):
            p0, p1 = g.startPoint, g.endPoint
            if abs(p0.x - p1.x) < 1e-4 and abs(p0.y - p1.y) < 1e-4 and abs(p0.z - p1.z) > 1e-4:
                edges.add(e)
    _fillet_edges(comp, edges, radius)


def _round_usb_opening(comp, body, cx, cy, radius):
    # USB slot is in the BACK wall: its corner edges run through the wall (parallel to Z).
    eps = 1e-3
    W2, H2 = USB_CUTOUT_W / 2.0, USB_CUTOUT_H / 2.0
    edges = adsk.core.ObjectCollection.create()
    for e in body.edges:
        g = e.geometry
        if not isinstance(g, adsk.core.Line3D):
            continue
        p0, p1 = g.startPoint, g.endPoint
        if abs(p0.x - p1.x) > eps or abs(p0.y - p1.y) > eps or abs(p0.z - p1.z) < eps:
            continue  # not parallel to Z
        mx = (p0.x + p1.x) / 2.0 / MM
        my = (p0.y + p1.y) / 2.0 / MM
        if abs(abs(mx - cx) - W2) < 1.0 and abs(abs(my - cy) - H2) < 1.0:
            edges.add(e)
    _fillet_edges(comp, edges, radius)


def _round_top(comp, lid, z_top, radius):
    edges = adsk.core.ObjectCollection.create()
    for e in lid.edges:
        g = e.geometry
        if isinstance(g, (adsk.core.Circle3D, adsk.core.Arc3D)):
            try:
                if abs(g.radius / MM - BUTTON_HOLE_DIA / 2.0) < 2.0:
                    continue
            except:
                pass
        if not e.startVertex or not e.endVertex:
            continue
        if abs(e.startVertex.geometry.z / MM - z_top) < 0.05 and \
           abs(e.endVertex.geometry.z / MM - z_top) < 0.05:
            edges.add(e)
    _fillet_edges(comp, edges, radius)


def _clear_design(design):
    root = design.rootComponent
    try:
        tl = design.timeline
        guard = 0
        while tl.count > 0 and guard < 10000:
            guard += 1
            try:
                tl.item(tl.count - 1).deleteMe()
            except:
                break
    except:
        pass
    for coll in (root.bRepBodies, root.sketches, root.constructionPlanes,
                 root.constructionAxes, root.constructionPoints):
        try:
            while coll.count > 0:
                coll.item(0).deleteMe()
        except:
            pass


def build():
    design = adsk.fusion.Design.cast(_app.activeProduct)
    if not design:
        _ui.messageBox('Open a design first, then Run the add-in.')
        return
    if WIPE_FIRST:
        _clear_design(design)
    root = design.rootComponent
    try:
        _app.log('WifeSignalEnclosure {0} (built {1})'.format(VERSION, BUILD_STAMP))
    except:
        pass

    width  = BUTTON_HOLE_DIA + 2 * SIDE_MARGIN
    height = BUTTON_SPACING * 2 + 2 * END_MARGIN
    status = {'pillars': 'skipped', 'countersinks': 'skipped',
              'fillets': 'skipped', 'lid_top': 'skipped'}

    # board stands on edge against the +X side wall; USB faces the back (-Z)
    board_x = (width / 2 - WALL) - BOARD_T / 2.0
    z_back = -BODY_DEPTH + WALL
    z_front = z_back + BOARD_L

    # pillar centres - match the PCB C-notches exactly: (+/-16, +/-18.5)
    corners = [(16.0, 18.5), (-16.0, 18.5), (16.0, -18.5), (-16.0, -18.5)]

    # 1) Body tray: profile extruded back, shell open at the FRONT.
    sk = root.sketches.add(root.xYConstructionPlane)
    _outer_profile(sk, width, height)
    body = _extrude(root, sk.profiles.item(0), -BODY_DEPTH, NEW).bodies.item(0)
    body.name = 'WifeSignal Body'

    frontFace, high = None, -1e9
    for f in body.faces:
        if f.centroid.z > high:
            high, frontFace = f.centroid.z, f
    fc = adsk.core.ObjectCollection.create()
    fc.add(frontFace)
    shIn = root.features.shellFeatures.createInput(fc, False)
    shIn.insideThickness = adsk.core.ValueInput.createByReal(WALL * MM)
    root.features.shellFeatures.add(shIn)

    # 2) USB-C slot in the LEFT (-X) wall. Vertical window computed from measurements:
    #    floor -> PCB (on spacers) -> receptacle bottom/front edges.
    floor_z   = -(BODY_DEPTH - WALL)
    pcb_top_z = floor_z + PCB_SPACER + PCB_THICK
    usb_top_z = pcb_top_z + USB_FRONT_ABOVE_PCB          # front-facing (lid-side) edge
    usb_bot_z = floor_z + USB_BACK_ABOVE_FLOOR           # bottom-facing (floor-side) edge
    usb_cz    = (usb_top_z + usb_bot_z) / 2.0
    usb_h     = (usb_top_z - usb_bot_z) + 2 * USB_MARGIN
    usb_wall_x = -(width / 2 - WALL / 2.0)
    _cut_box(root, body, usb_wall_x, USB_Y, usb_cz,
             WALL + 4.0, USB_CUTOUT_W, usb_h)

    # 3) (cradle removed - the flat PCB now bolts to the four pillars)

    # 4) Corner pillars with press-fit M4 nut pockets + bolt clearance
    try:
        for (cx, cy) in corners:
            _cyl_join(root, cx, cy, z_back, 0.0, PILLAR_DIA)
            _hex_cut(root, cx, cy, 0.0, NUT_DEPTH, NUT_AF)
            _circle_cut(root, cx, cy, 0.0, BOLT_LEN - LID_THICKNESS + 1.0, BOLT_CLEAR)
        status['pillars'] = 'built'
    except:
        status['pillars'] = 'FAILED'

    # 5) Lid (separate body): plate + button holes + countersunk bolt holes.
    skL = root.sketches.add(root.xYConstructionPlane)
    _outer_profile(skL, width, height)
    lid = _extrude(root, skL.profiles.item(0), LID_THICKNESS, NEW).bodies.item(0)
    lid.name = 'WifeSignal Lid'

    skH = root.sketches.add(_plane_z(root, LID_THICKNESS))
    for y in (BUTTON_SPACING, 0.0, -BUTTON_SPACING):
        skH.sketchCurves.sketchCircles.addByCenterRadius(
            adsk.core.Point3D.create(0, y * MM, 0), (BUTTON_HOLE_DIA / 2.0) * MM)
    cc = adsk.core.ObjectCollection.create()
    for p in skH.profiles:
        cc.add(p)
    bh = root.features.extrudeFeatures.createInput(cc, CUT)
    bh.setDistanceExtent(False, adsk.core.ValueInput.createByReal(-(LID_THICKNESS + 0.5) * MM))
    bh.participantBodies = [lid]
    root.features.extrudeFeatures.add(bh)

    try:
        for (cx, cy) in corners:
            _countersink(root, cx, cy, LID_THICKNESS, LID_THICKNESS, lid)
        status['countersinks'] = 'built'
    except:
        status['countersinks'] = 'FAILED'

    # 6) Round all vertical corners (guarded) + soften the lid top
    try:
        _fillet_vertical(root, body, CORNER_R)
        _fillet_vertical(root, lid, CORNER_R)
        status['fillets'] = 'built'
    except:
        status['fillets'] = 'FAILED'
    try:
        _round_top(root, lid, LID_THICKNESS, LID_TOP_ROUND)
        status['lid_top'] = 'built'
    except:
        status['lid_top'] = 'FAILED'

    _ui.messageBox(
        'WifeSignalEnclosure {6}  (built {7})\n\n'
        'WifeSignal built: "WifeSignal Body" (flat-PCB tray, USB-C in LEFT wall) + '
        '"WifeSignal Lid".\nBody {0} x {1} x {2} mm (tray +7mm deeper).  Fillet {3} mm.\n'
        'Pillars at (+/-16, +/-18.5).  No cradle.\n'
        'USB window: Z {8:.1f}..{9:.1f} mm (h {10:.1f}), Y {11:.1f}, W {12:.1f}.\n'
        'Status -> pillars: {4} | lid top: {5}\n'
        'CHECK: verify USB_Y (along wall) against your assembled board.\n'
        'Print the two bodies separately (right-click a body > Save As Mesh).'.format(
            round(width), round(height), round(BODY_DEPTH + LID_THICKNESS), ROUND_R,
            status['pillars'], status['lid_top'],
            VERSION, BUILD_STAMP,
            usb_bot_z, usb_top_z, usb_h, USB_Y, USB_CUTOUT_W))


def run(context):
    global _app, _ui
    try:
        _app = adsk.core.Application.get()
        _ui = _app.userInterface
        build()
    except:
        if _ui:
            _ui.messageBox('Failed:\n{}'.format(traceback.format_exc()))


def stop(context):
    pass