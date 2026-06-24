# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

libvcell is a Python package that wraps a subset of VCell (Virtual Cell) Java algorithms as a native shared library via GraalVM native-image. It provides Python functions for converting between VCML, SBML, and finite volume solver input formats, plus VCell-to-Python math expression translation.

## Build & Development Commands

### Setup
```bash
make install          # Install poetry env + pre-commit hooks
```

### Build the native shared library (requires GraalVM JDK 23 with native-image)
```bash
scripts/local_build_native.sh   # Full local native build (macOS)
poetry build                    # Build Python wheel (triggers build.py which builds Java/native)
```

### Quality checks
```bash
make check            # Runs: poetry check --lock, pre-commit, mypy, deptry
```

### Tests
```bash
poetry run pytest                                    # Run all tests
poetry run pytest tests/test_libvcell.py             # Run specific test file
poetry run pytest tests/test_libvcell.py::test_name  # Run single test
poetry run pytest --cov --cov-config=pyproject.toml --cov-report=xml  # With coverage
```

The Java/native unit tests (`vcell-native/src/test/`) run separately via Maven and exercise the entry points against vcell-core directly (no native-image step):
```bash
mvn -o test -f vcell-native                          # All vcell-native Java tests (offline)
mvn -o test -f vcell-native -Dtest=MiscTests         # Single test class
```
These require the submodule artifacts to already be installed in `.m2` (see Key dependencies); otherwise compilation fails with errors like `cannot find symbol` for vcell-core methods. The CI `quality` job is what runs these (it's the first to fail on a broken vcell-native test). Avoid asserting against full exception stack traces in these tests — frames embed vcell-core line numbers (drift on submodule bumps) and differ between IDE and Surefire runners.

### Type checking & linting
```bash
poetry run mypy       # Type check (strict mode, covers libvcell/, tests/, build.py)
```

Pre-commit hooks run ruff (lint + format) and prettier automatically.

## Architecture

### Two-layer design: Python wrapper over GraalVM native library

**Python layer** (`libvcell/`):
- `__init__.py` — Public API: `vcml_to_finite_volume_input`, `sbml_to_finite_volume_input`, `sbml_to_vcml`, `vcml_to_sbml`, `vcml_to_vcml`, `vcell_infix_to_python_infix`, `vcell_infix_to_num_expr_infix`. Also exposes `__version__` (read from installed package metadata; `"0.0.0"` when running from source tree)
- `solver_utils.py` / `model_utils.py` — Thin wrappers that instantiate `VCellNativeCalls` and delegate to native methods
- `_internal/native_utils.py` — Loads the platform-specific shared library (`.so`/`.dylib`/`.dll`) from `libvcell/lib/` via ctypes; defines `IsolateManager` context manager for GraalVM isolate lifecycle
- `_internal/native_calls.py` — ctypes FFI calls to the native library entry points; handles GraalVM isolate creation/teardown per call, JSON deserialization of `ReturnValue`

**Native/Java layer** (`vcell-native/`):
- `Entrypoints.java` — `@CEntryPoint` methods exposed as C symbols (`vcmlToFiniteVolumeInput`, `sbmlToFiniteVolumeInput`, `vcmlToSbml`, `sbmlToVcml`, `vcmlToVcml`, `vcellInfixToPythonInfix`)
- `ModelUtils.java` / `SolverUtils.java` — Java implementation using vcell-core from the `vcell_submodule`
- Built with Maven, then compiled to a shared library via GraalVM `native-maven-plugin` using the `shared-dll` profile
- `MainRecorder.java` — Used with `native-image-agent` to record dynamic reflection/resource configs before native compilation

**Build pipeline** (`build.py`):
1. `mvn clean install -DskipTests` on `vcell_submodule/` (full VCell Java project)
2. `mvn clean install` on `vcell-native/` (builds the shaded JAR)
3. Run JAR with `native-image-agent` to record native-image config into `target/recording/`
4. `mvn package -P shared-dll` to produce the native shared library
5. Copy resulting `libvcell.{so,dylib,dll}` into `libvcell/lib/`

Linux wheels are built inside the `docker/Dockerfile_manylinux_*` images (manylinux 2_28 and 2_34, for both `aarch64` and `x86_64`), which provide the GraalVM toolchain needed for native compilation in CI.

### Key dependencies
- `vcell_submodule/` — Git submodule pointing to the full VCell Java repository (provides vcell-core). After cloning or pulling a submodule pointer bump, run `git submodule update --init --recursive` — git does NOT auto-update the submodule working tree, so it can sit at an older commit than the recorded pointer (shows as `M vcell_submodule` in `git status`). A stale checkout causes `vcell-native` to fail compiling against vcell-core. To make vcell-core/math available for a local `vcell-native` build, install them into `.m2` first: `mvn -DskipTests clean install -f vcell_submodule` (or run the full `scripts/local_build_native.sh`).
- GraalVM JDK 23 with `native-image` tool required for building native library (`.java-version` pins `graalvm64-23.0.2`)
- Python >=3.10,<4.0, pydantic for data models

### FFI pattern
Each Python API call: creates `VCellNativeCalls` → loads native lib → creates GraalVM isolate → calls C entry point → receives JSON string → deserializes to `ReturnValue(success, message)` → tears down isolate. The `IsolateManager` context manager handles isolate lifecycle.

### Test fixtures
Test data lives in `tests/fixtures/data/` (VCML and SBML XML files). Fixtures are defined in `tests/fixtures/data_fixtures.py` and imported via `tests/conftest.py`.

## CI

GitHub Actions (`.github/workflows/main.yml`) runs on push to main and PRs:
- Quality checks (pre-commit, mypy, deptry) on ubuntu
- Tests + type checking across matrix: macOS (Intel + ARM), Windows, Ubuntu
- All CI jobs require GraalVM setup for native library compilation
