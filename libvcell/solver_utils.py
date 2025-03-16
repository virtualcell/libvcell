from pathlib import Path

from libvcell._internal.native_calls import ReturnValue, VCellNativeCalls


def vcml_to_finite_volume_input(vcml_content: str, simulation_name: str, output_dir_path: Path) -> tuple[bool, str]:
    native = VCellNativeCalls()
    return_value: ReturnValue = native.vcml_to_finite_volume_input(vcml_content, simulation_name, output_dir_path)
    return return_value.success, return_value.message


def sbml_to_finite_volume_input(sbml_content: str, output_dir_path: Path) -> tuple[bool, str]:
    native = VCellNativeCalls()
    return_value: ReturnValue = native.sbml_to_finite_volume_input(sbml_content, output_dir_path)
    return return_value.success, return_value.message
