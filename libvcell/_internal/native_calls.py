import ctypes
import logging
from pathlib import Path

from pydantic import BaseModel

from libvcell._internal.native_utils import IsolateManager, VCellNativeLibraryLoader


class ReturnValue(BaseModel):
    success: bool
    message: str


class MutableString:
    def __init__(self, value: str):
        self.value: str = value


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

    def vcml_to_sbml(
        self, vcml_content: str, application_name: str, sbml_file_path: Path, round_trip_validation: bool
    ) -> ReturnValue:
        try:
            with IsolateManager(self.lib) as isolate_thread:
                json_ptr: ctypes.c_char_p = self.lib.vcmlToSbml(
                    isolate_thread,
                    ctypes.c_char_p(vcml_content.encode("utf-8")),
                    ctypes.c_char_p(application_name.encode("utf-8")),
                    ctypes.c_char_p(str(sbml_file_path).encode("utf-8")),
                    ctypes.c_int(int(round_trip_validation)),
                )

            value: bytes | None = ctypes.cast(json_ptr, ctypes.c_char_p).value
            if value is None:
                logging.error("Failed to convert vcml application to sbml")
                return ReturnValue(success=False, message="Failed to convert vcml to sbml")
            json_str: str = value.decode("utf-8")
            # self.lib.freeString(json_ptr)
            return ReturnValue.model_validate_json(json_data=json_str)
        except Exception as e:
            logging.exception("Error in vcml_to_sbml()", exc_info=e)
            raise

    def sbml_to_vcml(self, sbml_content: str, vcml_file_path: Path) -> ReturnValue:
        try:
            with IsolateManager(self.lib) as isolate_thread:
                json_ptr: ctypes.c_char_p = self.lib.sbmlToVcml(
                    isolate_thread,
                    ctypes.c_char_p(sbml_content.encode("utf-8")),
                    ctypes.c_char_p(str(vcml_file_path).encode("utf-8")),
                )

            value: bytes | None = ctypes.cast(json_ptr, ctypes.c_char_p).value
            if value is None:
                logging.error("Failed to convert sbml to vcml")
                return ReturnValue(success=False, message="Failed to convert sbml to vcml")
            json_str: str = value.decode("utf-8")
            # self.lib.freeString(json_ptr)
            return ReturnValue.model_validate_json(json_data=json_str)
        except Exception as e:
            logging.exception("Error in sbml_to_vcml()", exc_info=e)
            raise

    def vcml_to_vcml(self, vcml_content: str, vcml_file_path: Path) -> ReturnValue:
        try:
            with IsolateManager(self.lib) as isolate_thread:
                json_ptr: ctypes.c_char_p = self.lib.vcmlToVcml(
                    isolate_thread,
                    ctypes.c_char_p(vcml_content.encode("utf-8")),
                    ctypes.c_char_p(str(vcml_file_path).encode("utf-8")),
                )

            value: bytes | None = ctypes.cast(json_ptr, ctypes.c_char_p).value
            if value is None:
                logging.error("Failed to regenerate vcml")
                return ReturnValue(success=False, message="Failed to regenerate vcml")
            json_str: str = value.decode("utf-8")
            # self.lib.freeString(json_ptr)
            return ReturnValue.model_validate_json(json_data=json_str)
        except Exception as e:
            logging.exception("Error in vcml_to_vcml()", exc_info=e)
            raise

    def vcell_infix_to_python_infix(
        self, vcell_infix: str, target_python_infix: MutableString, buffer_size: int | None = None
    ) -> ReturnValue:
        try:
            needed_buffer_size = int(1.5 * len(vcell_infix)) if buffer_size is None else buffer_size
            buff = ctypes.create_string_buffer(needed_buffer_size)
            with IsolateManager(self.lib) as isolate_thread:
                json_ptr = self.lib.vcellInfixToPythonInfix(
                    isolate_thread, ctypes.c_char_p(vcell_infix.encode("utf-8")), buff, needed_buffer_size
                )
            value: bytes | None = ctypes.cast(json_ptr, ctypes.c_char_p).value
            if value is None:
                logging.error("Failed to regenerate vcml")
                return ReturnValue(success=False, message="Failed to generate python infix")
            json_str = value.decode("utf-8")
            if "not enough room, need: `" in json_str:
                if buffer_size is not None:
                    logging.error("Failed to identify correct buffer size reported by previous error")
                    return ReturnValue(
                        success=False, message="Failed to identify correct buffer size reported by previous error"
                    )
                # get the size from the error
                index = json_str.find("not enough room, need: `") + len("not enough room, need: `")
                end_index = json_str.find("`", index)
                size_as_string: str = json_str[index:end_index]
                if not size_as_string.isnumeric():
                    logging.error("Buffer size reported by previous error is not an integer!")
                    return ReturnValue(
                        success=False, message="Buffer size reported by previous error is not an integer!"
                    )
                return self.vcell_infix_to_python_infix(vcell_infix, target_python_infix, int(size_as_string))
            # self.lib.freeString(json_ptr)
            target_python_infix.value = buff.value.decode("utf-8")
            return ReturnValue.model_validate_json(json_data=json_str)
        except Exception as e:
            logging.exception("Error in vcell_infix_to_python_infix()", exc_info=e)
            raise
