import ctypes
import os
import platform
from _ctypes import byref
from importlib.resources import files

import libvcell

#: The environment variable the native library reads to find VCell's resources (org.vcell.libvcell.BioNetGenSupport).
VCELL_INSTALL_DIR_ENV = "LIBVCELL_VCELL_INSTALL_DIR"


def _use_bundled_bionetgen() -> None:
    """Point the native library at the BioNetGen this package bundles (``vcell_install/bionetgen/BNG2.pl``), which
    VCell runs with ``perl`` to generate a rule-based application's reaction network. A value already in the
    environment wins."""
    install_dir = files(libvcell).joinpath("vcell_install")
    if install_dir.joinpath("bionetgen", "BNG2.pl").is_file():
        os.environ.setdefault(VCELL_INSTALL_DIR_ENV, str(install_dir))


class VCellNativeLibraryLoader:
    lib: ctypes.CDLL

    def __init__(self) -> None:
        _use_bundled_bionetgen()
        self.lib = self._load_library()
        self._define_entry_points()

    def _load_library(self) -> ctypes.CDLL:
        system = platform.system()
        lib_ext = {"Linux": ".so", "Darwin": ".dylib", "Windows": ".dll"}.get(system)

        if lib_ext is None:
            raise OSError(f"Unsupported operating system: {system}")

        libs_dir = files(libvcell).joinpath("lib")
        if not libs_dir.is_dir():
            raise OSError(f"Could not find the shared library directory {libs_dir}")

        for file in libs_dir.iterdir():
            if file.name.endswith(lib_ext):
                print(f"Found shared library: {file}")
                return ctypes.CDLL(name=str(file))

        raise OSError("Could not find the shared library")

    def _define_entry_points(self) -> None:
        self.lib.vcmlToFiniteVolumeInput.restype = ctypes.c_char_p
        self.lib.vcmlToFiniteVolumeInput.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p, ctypes.c_char_p]

        self.lib.sbmlToFiniteVolumeInput.restype = ctypes.c_char_p
        self.lib.sbmlToFiniteVolumeInput.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]

        self.lib.sbmlToVcml.restype = ctypes.c_char_p
        self.lib.sbmlToVcml.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]

        self.lib.vcmlToSbml.restype = ctypes.c_char_p
        self.lib.vcmlToSbml.argtypes = [
            ctypes.c_void_p,
            ctypes.c_char_p,
            ctypes.c_char_p,
            ctypes.c_char_p,
            ctypes.c_int,
        ]

        self.lib.vcmlToVcml.restype = ctypes.c_char_p
        self.lib.vcmlToVcml.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]
        self.lib.vcmlConvertUnits.restype = ctypes.c_char_p
        self.lib.vcmlConvertUnits.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p, ctypes.c_char_p]

        # vcmlToMovingBoundaryInput is only present in native libraries built with moving-boundary
        # support; guard so the package still loads against older shared libraries.
        # vcmlToOdeInput / sbmlToOdeInput are only present in native libraries built with ODE input
        # support; guard so the package still loads against older shared libraries.
        if hasattr(self.lib, "vcmlToOdeInput"):
            self.lib.vcmlToOdeInput.restype = ctypes.c_char_p
            self.lib.vcmlToOdeInput.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p, ctypes.c_char_p]
        if hasattr(self.lib, "sbmlToOdeInput"):
            self.lib.sbmlToOdeInput.restype = ctypes.c_char_p
            self.lib.sbmlToOdeInput.argtypes = [
                ctypes.c_void_p,
                ctypes.c_char_p,
                ctypes.c_double,
                ctypes.c_double,
                ctypes.c_char_p,
            ]

        if hasattr(self.lib, "vcmlToMovingBoundaryInput"):
            self.lib.vcmlToMovingBoundaryInput.restype = ctypes.c_char_p
            self.lib.vcmlToMovingBoundaryInput.argtypes = [
                ctypes.c_void_p,
                ctypes.c_char_p,
                ctypes.c_char_p,
                ctypes.c_char_p,
            ]

        # evaluateExpression is only present in newer native libraries; guard so the package
        # still loads against older shared libraries that predate it.
        if hasattr(self.lib, "evaluateExpression"):
            self.lib.evaluateExpression.restype = ctypes.c_char_p
            self.lib.evaluateExpression.argtypes = [
                ctypes.c_void_p,
                ctypes.c_char_p,
                ctypes.c_char_p,
            ]

        self.lib.vcellInfixToPythonInfix.restype = ctypes.c_char_p
        self.lib.vcellInfixToPythonInfix.argtypes = [
            ctypes.c_void_p,
            ctypes.c_char_p,
            ctypes.c_char_p,
            ctypes.c_longlong,
        ]

        self.lib.vcellInfixToNumExprInfix.restype = ctypes.c_char_p
        self.lib.vcellInfixToNumExprInfix.argtypes = [
            ctypes.c_void_p,
            ctypes.c_char_p,
            ctypes.c_char_p,
            ctypes.c_longlong,
        ]

        self.lib.freeString.restype = None
        self.lib.freeString.argtypes = [ctypes.c_char_p]


class IsolateManager:
    lib: ctypes.CDLL
    isolate: ctypes.c_void_p
    isolate_thread: ctypes.c_void_p

    def __init__(self, lib: ctypes.CDLL) -> None:
        self.lib = lib
        self.isolate = ctypes.c_void_p()
        self.isolate_thread = ctypes.c_void_p()

    def __enter__(self) -> ctypes.c_void_p:
        self.lib.graal_create_isolate(None, byref(self.isolate), byref(self.isolate_thread))
        return self.isolate_thread

    def __exit__(self, exc_type: type | None, exc_val: Exception, exc_tb: object | None) -> None:
        self.lib.graal_tear_down_isolate(self.isolate_thread)
