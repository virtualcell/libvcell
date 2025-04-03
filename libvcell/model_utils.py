from pathlib import Path

from libvcell._internal.native_calls import ReturnValue, VCellNativeCalls


def vcml_to_sbml(vcml_content: str, application_name: str, sbml_file_path: Path, validate: bool) -> tuple[bool, str]:
    """
    Convert VCML content to SBML file

    Args:
        vcml_content (str): VCML content
        application_name (str): VCell Biomodel application name
        sbml_file_path (Path): path to resulting SBML file
        validate (bool): whether to validate SBML file

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.vcml_to_sbml(vcml_content, application_name, sbml_file_path, validate)
    return return_value.success, return_value.message


def sbml_to_vcml(sbml_content: str, vcml_file_path: Path, validate: bool) -> tuple[bool, str]:
    """
    Convert SBML content to finite volume input files

    Args:
        sbml_content (str): SBML content
        vcml_file_path (Path): path to resulting VCML file
        validate (bool): whether to validate SBML import with round trip test

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.sbml_to_vcml(sbml_content, vcml_file_path, validate)
    return return_value.success, return_value.message


def vcml_to_vcml(vcml_content: str, vcml_file_path: Path) -> tuple[bool, str]:
    """
    Process VCML content to regenerated VCML file

    Args:
        vcml_content (str): VCML content
        vcml_file_path (Path): path to resulting VCML file

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.vcml_to_vcml(vcml_content, vcml_file_path)
    return return_value.success, return_value.message
