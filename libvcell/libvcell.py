from pathlib import Path

from libvcell._internal.native_calls import ReturnValue, VCellNativeCalls


class libvcell:
    @staticmethod
    def vcml_to_finite_volume_input(vcml_content: str, simulation_name: str, output_dir_path: Path) -> None:
        native = VCellNativeCalls()
        return_value: ReturnValue = native.vcml_to_finite_volume_input(vcml_content, simulation_name, output_dir_path)
        if not return_value.success:
            raise RuntimeError(f"Error in vcml_to_finite_volume_input: {return_value.message}")

    @staticmethod
    def sbml_to_finite_volume_input(sbml_content: str, output_dir_path: Path) -> None:
        native = VCellNativeCalls()
        return_value: ReturnValue = native.sbml_to_finite_volume_input(sbml_content, output_dir_path)
        if not return_value.success:
            raise RuntimeError(f"Error in sbml_to_finite_volume_input: {return_value.message}")
