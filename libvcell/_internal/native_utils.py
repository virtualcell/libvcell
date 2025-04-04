import ctypes
import platform
from _ctypes import byref
from importlib.resources import files

import libvcell


class VCellNativeLibraryLoader:
    lib: ctypes.CDLL

    def __init__(self) -> None:
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
        self.lib.vcmlToSbml.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p, ctypes.c_char_p]

        self.lib.vcmlToVcml.restype = ctypes.c_char_p
        self.lib.vcmlToVcml.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]

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
