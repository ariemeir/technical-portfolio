# 3D Models

Geometry used by the Android console's 3D visualization (rendered with min3d).

| File | |
|---|---|
| `spine/Metal Rod.skp` | SketchUp source model of the distraction rod |
| `spine/rod.dae` | COLLADA export |
| `spine/models/rod.obj` | Wavefront OBJ export — the format the app loads |

The app loads its runtime copy from `android/res/raw/rod_obj`.

## Note on the spine mesh

The anatomical spine model that accompanied these files was **removed before
publication** — it carried no embedded author or copyright information and its
origin could not be verified. That means the 3D view has no spine geometry to
load; only the rod remains. The rod geometry is original work.

See [`../NOTICE.md`](../NOTICE.md).
