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


def vcml_to_ode_input(vcml_content: str, simulation_name: str, output_dir_path: Path) -> tuple[bool, str]:
    """
    Convert a non-spatial VCML simulation to Sundials ODE solver input (a CVODE or IDA input file)

    The named simulation must be configured for CVODE, IDA or the Combined IDA/CVODE solver (Combined
    resolves to IDA when the math has fast systems, CVODE otherwise). The input file, with its
    ``.functions`` file, is written into ``output_dir_path`` and can be solved by the pyvcell-odesolver
    package (``pyvcell_odesolver.solve(input_file, output_file)``), which writes VCell's ``.ida`` results.

    Args:
        vcml_content (str): VCML content
        simulation_name (str): simulation name (must use CVODE, IDA or Combined IDA/CVODE)
        output_dir_path (Path): output directory path

    A rule-based application (reaction rules, whose network VCell generates with BioNetGen, which libvcell does not
    include) is converted from the math stored in the document: the network VCell generated when the model was
    saved. The message then says so.

    Returns:
        tuple[bool, str]: the success status, and on success the input file's name (``<base>.cvodeInput`` or
        ``<base>.idaInput``), followed by ``"; stored math (...)"`` when the stored math was used (file names have
        no ``;``, so ``message.split(";")[0]`` is the name); on failure the error message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.vcml_to_ode_input(vcml_content, simulation_name, output_dir_path)
    return return_value.success, return_value.message


def sbml_to_ode_input(
    sbml_content: str, output_dir_path: Path, duration: float, output_time_step: float
) -> tuple[bool, str]:
    """
    Convert a non-spatial SBML model to Sundials ODE solver input (a CVODE or IDA input file)

    The model's first application is simulated from 0 to ``duration``, saving every ``output_time_step``,
    on the Combined IDA/CVODE solver. Solve the input with the pyvcell-odesolver package.

    Args:
        sbml_content (str): SBML content
        output_dir_path (Path): output directory path
        duration (float): end time of the simulation (positive)
        output_time_step (float): interval between saved time points (positive)

    Returns:
        tuple[bool, str]: the success status, and the input file's name on success or the error message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.sbml_to_ode_input(sbml_content, duration, output_time_step, output_dir_path)
    return return_value.success, return_value.message
