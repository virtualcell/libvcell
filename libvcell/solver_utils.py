from pathlib import Path

from libvcell._internal.native_calls import ReturnValue, VCellNativeCalls


def vcml_to_finite_volume_input(vcml_content: str, simulation_name: str, output_dir_path: Path) -> tuple[bool, str]:
    """
    Convert VCML content to finite volume input files

    Args:
        vcml_content (str): VCML content
        simulation_name (str): simulation name
        output_dir_path (Path): output directory path

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.vcml_to_finite_volume_input(vcml_content, simulation_name, output_dir_path)
    return return_value.success, return_value.message


def vcml_to_moving_boundary_input(vcml_content: str, simulation_name: str, output_dir_path: Path) -> tuple[bool, str]:
    """
    Convert VCML content to Moving Boundary solver input (a MovingBoundarySetup XML file)

    The named simulation must be configured for the Moving Boundary solver. The generated
    MovingBoundarySetup XML can be consumed by the vcell-mbsolver package (``MovingBoundarySolver.from_xml``).

    Args:
        vcml_content (str): VCML content
        simulation_name (str): simulation name (must use the Moving Boundary solver)
        output_dir_path (Path): output directory path

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.vcml_to_moving_boundary_input(vcml_content, simulation_name, output_dir_path)
    return return_value.success, return_value.message


def sbml_to_finite_volume_input(sbml_content: str, output_dir_path: Path) -> tuple[bool, str]:
    """
    Convert SBML content to finite volume input files

    Args:
        sbml_content (str): SBML content
        output_dir_path (Path): output directory path

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.sbml_to_finite_volume_input(sbml_content, output_dir_path)
    return return_value.success, return_value.message
