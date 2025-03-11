import ctypes
import logging
from pathlib import Path

from pydantic import BaseModel

from libvcell._internal.native_utils import IsolateManager, VCellNativeLibraryLoader


class ReturnValue(BaseModel):
    success: bool
    message: str


class VCellNativeCalls:
    def __init__(self) -> None:
        self.loader = VCellNativeLibraryLoader()
        self.lib = self.loader.lib

    def vcml_to_finite_volume_input(
        self, vcml_content: str, simulation_name: str, output_dir_path: Path
    ) -> ReturnValue:
        try:
            with IsolateManager(self.lib) as isolate_thread:
                json_ptr: ctypes.c_char_p = self.lib.vcmlToFiniteVolumeInput(
                    isolate_thread,
                    ctypes.c_char_p(vcml_content.encode("utf-8")),
                    ctypes.c_char_p(simulation_name.encode("utf-8")),
                    ctypes.c_char_p(str(output_dir_path).encode("utf-8")),
                )

            value: bytes | None = ctypes.cast(json_ptr, ctypes.c_char_p).value
            if value is None:
                logging.error("Failed to convert vcml to finite volume input")
                return ReturnValue(success=False, message="Failed to convert vcml to finite volume input")
            json_str: str = value.decode("utf-8")
            # self.lib.freeString(json_ptr)
            return ReturnValue.model_validate_json(json_data=json_str)
        except Exception as e:
            logging.exception("Error in vcml_to_finite_volume_input()", exc_info=e)
            raise

    def sbml_to_finite_volume_input(self, sbml_content: str, output_dir_path: Path) -> ReturnValue:
        try:
            with IsolateManager(self.lib) as isolate_thread:
                json_ptr: ctypes.c_char_p = self.lib.sbmlToFiniteVolumeInput(
                    isolate_thread,
                    ctypes.c_char_p(sbml_content.encode("utf-8")),
                    ctypes.c_char_p(str(output_dir_path).encode("utf-8")),
                )
            value: bytes | None = ctypes.cast(json_ptr, ctypes.c_char_p).value
            if value is None:
                logging.error("Failed to convert sbml to finite volume input")
                return ReturnValue(success=False, message="Failed to convert sbml to finite volume input")
            json_str: str = value.decode("utf-8")
            # self.lib.freeString(json_ptr)
            return ReturnValue.model_validate_json(json_data=json_str)
        except Exception as e:
            logging.exception("Error in sbml_to_finite_volume_input()", exc_info=e)
            raise
