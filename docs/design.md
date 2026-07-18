# Design

libvcell is a thin **pure-Python layer** over a **GraalVM `native-image` shared library** built from a subset of VCell's Java code. The Python side contains no compiled CPython extension; the native library is loaded and called via `ctypes`.

## Two layers

**Python layer** (`libvcell/`)

- `__init__.py` — public API surface.
- `model_utils.py` / `solver_utils.py` — thin wrappers that instantiate `VCellNativeCalls` and translate its structured results into friendly return values or exceptions.
- `_internal/native_utils.py` — locates and loads the platform shared library (`.so`/`.dylib`/`.dll`) from `libvcell/lib/`, declares each entry point's `ctypes` signature, and provides `IsolateManager` for GraalVM isolate lifecycle.
- `_internal/native_calls.py` — one method per native entry point; marshals arguments, manages the isolate, and parses the returned JSON document into a pydantic model.

**Native/Java layer** (`vcell-native/`)

- `Entrypoints.java` — `@CEntryPoint` methods exported as C symbols. Each returns a **JSON document** as a C string (`CCharPointer`) describing success/failure.
- `ModelUtils.java` / `SolverUtils.java` — the actual logic, calling vcell-core from `vcell_submodule`.
- `MainRecorder.java` — exercises each entry point under `native-image-agent` so the build records the required reflection/resource config.

## FFI conventions

- **Every call returns a JSON string.** A native entry point never returns a bare number/bool across the boundary; it returns a JSON document (via `createString`, whose memory is tracked in `Entrypoints.allocatedMemory`). The Python side decodes the C string and validates it into a pydantic model (`ReturnValue`, `EvalReturnValue`, …).
- **Errors are data, not crashes.** Entry points catch `Throwable` and encode the failure into the JSON document (a `success:false` flag plus a message and/or error type). The Python wrapper decides whether to return a status tuple or raise.
- **One isolate per call.** `IsolateManager` creates a GraalVM isolate for the duration of a call and tears it down afterward.
- **New entry points are `hasattr`-guarded** in `native_utils.py` so the package still imports against an older shared library that predates the symbol (the Python tests `skipif` on the same check).

## Entry points

| Native symbol                                          | Python API                                                      | Purpose                                                        |
| ------------------------------------------------------ | --------------------------------------------------------------- | -------------------------------------------------------------- |
| `vcmlToFiniteVolumeInput` / `sbmlToFiniteVolumeInput`  | `vcml_to_finite_volume_input` / `sbml_to_finite_volume_input`   | write Finite Volume solver input                               |
| `vcmlToMovingBoundaryInput`                            | `vcml_to_moving_boundary_input`                                 | write Moving Boundary solver input (`MovingBoundarySetup` XML) |
| `vcmlToSbml` / `sbmlToVcml` / `vcmlToVcml`             | `vcml_to_sbml` / `sbml_to_vcml` / `vcml_to_vcml`                | model format conversion                                        |
| `vcellInfixToPythonInfix` / `vcellInfixToNumExprInfix` | `vcell_infix_to_python_infix` / `vcell_infix_to_num_expr_infix` | translate VCell infix to other syntaxes                        |
| `evaluateExpression`                                   | `evaluate_expression`                                           | evaluate a VCell infix expression to a float                   |

## `evaluate_expression`

Evaluates a native-syntax VCell infix expression given a symbol table of values, returning a 64-bit float.

```python
from libvcell import evaluate_expression, VCellExpressionError

evaluate_expression("a + b/c", {"a": 10.0, "b": 20.0, "c": 5.0})  # -> 14.0
evaluate_expression("2 + 3*sqrt(4)", {})                          # -> 8.0

try:
    evaluate_expression("1/c", {"c": 0.0})
except VCellExpressionError as e:
    print(e.error_type)   # "DivideByZeroException"
```

**Semantics**

- Any symbol referenced by the expression must be present in the symbol table; extra (unreferenced) symbols are permitted and ignored.
- The value is computed via vcell-core's `Expression`: parse → `bindExpression(new SimpleSymbolTable(names))` → `evaluateVector(values)`. `SimpleSymbolTable` provides the lightweight "dummy" binding — no VCell model or `MathDescription` is required.

**Native boundary**

- Input: the infix string and the symbol table serialized as a JSON object of `{name: number}` (`json.dumps` of the dict; integers are accepted).
- Output: a JSON document — `{"success": true, "value": <double>}` on success, or `{"success": false, "error_type": <exceptionClassName>, "message": <text>}` on failure. This is parsed into `EvalReturnValue`.

**Error handling**

- `native_calls.evaluate_expression(...)` returns the raw `EvalReturnValue` (branch on `.success`).
- The public `model_utils.evaluate_expression(...)` returns the `float` or raises `VCellExpressionError`, which exposes `.error_type` (the originating Java exception's simple class name) and `.message`. Categories include `ParseException` (syntax), `ExpressionBindingException` (a referenced symbol was not supplied), `DivideByZeroException`, `FunctionDomainException` (e.g. `sqrt(-1)`, `log(0)`), and `IllegalArgumentException` (malformed symbol-table JSON).
- Non-finite results (`Infinity`/`NaN`) cannot be represented in JSON and are surfaced as an error (`error_type = "NonFiniteResultException"`).

## Adding a new entry point

1. Implement the logic in `ModelUtils.java` / `SolverUtils.java`.
2. Add a `@CEntryPoint` method in `Entrypoints.java` that returns a JSON document.
3. Exercise it in `MainRecorder.java` (so native-image records its config).
4. Declare its `ctypes` signature (`hasattr`-guarded) in `native_utils.py`.
5. Add a `VCellNativeCalls` method returning a pydantic model in `native_calls.py`.
6. Add the friendly wrapper in `model_utils.py` / `solver_utils.py` and export it from `__init__.py`.
7. Add Java tests (JVM-level) and Python tests (`skipif` on the new symbol until the native library is rebuilt).
