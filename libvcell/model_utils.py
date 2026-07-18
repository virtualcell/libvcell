from pathlib import Path

from libvcell._internal.native_calls import EvalReturnValue, MutableString, ReturnValue, VCellNativeCalls


class VCellExpressionError(Exception):
    """Raised when a VCell expression cannot be evaluated.

    Attributes:
        error_type: the originating Java exception's simple class name (e.g. ``DivideByZeroException``,
            ``ExpressionBindingException``, ``ParseException``, ``FunctionDomainException``), or ``None``.
        message: the error message, or ``None``.
    """

    def __init__(self, error_type: str | None, message: str | None) -> None:
        self.error_type = error_type
        self.message = message
        super().__init__(f"{error_type}: {message}")


def evaluate_expression(expression_infix: str, symbol_table: dict[str, float]) -> float:
    """
    Evaluate a native-syntax VCell infix expression against a table of symbol values.

    Any symbol referenced by the expression must be present in ``symbol_table``; extra
    (unreferenced) symbols are permitted and ignored.

    Args:
        expression_infix (str): native VCell infix expression string (e.g. ``"a + b/c"``)
        symbol_table (dict[str, float]): mapping of symbol name to 64-bit float value

    Returns:
        float: the evaluated value

    Raises:
        VCellExpressionError: if the expression fails to parse, references an unsupplied symbol,
            fails to evaluate (e.g. division by zero, math domain error), or evaluates to a
            non-finite value.
    """
    native = VCellNativeCalls()
    result: EvalReturnValue = native.evaluate_expression(expression_infix, symbol_table)
    if not result.success or result.value is None:
        raise VCellExpressionError(result.error_type, result.message)
    return result.value


def vcml_to_sbml(
    vcml_content: str, application_name: str, sbml_file_path: Path, round_trip_validation: bool
) -> tuple[bool, str]:
    """
    Convert VCML content to SBML file

    Args:
        vcml_content (str): VCML content
        application_name (str): VCell Biomodel application name
        sbml_file_path (Path): path to resulting SBML file

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.vcml_to_sbml(
        vcml_content=vcml_content,
        application_name=application_name,
        sbml_file_path=sbml_file_path,
        round_trip_validation=round_trip_validation,
    )
    return return_value.success, return_value.message


def sbml_to_vcml(sbml_content: str, vcml_file_path: Path) -> tuple[bool, str]:
    """
    Convert SBML content to finite volume input files

    Args:
        sbml_content (str): SBML content
        vcml_file_path (Path): path to resulting VCML file

    Returns:
        tuple[bool, str]: A tuple containing the success status and a message
    """
    native = VCellNativeCalls()
    return_value: ReturnValue = native.sbml_to_vcml(sbml_content=sbml_content, vcml_file_path=vcml_file_path)
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
    return_value: ReturnValue = native.vcml_to_vcml(vcml_content=vcml_content, vcml_file_path=vcml_file_path)
    return return_value.success, return_value.message


def vcell_infix_to_python_infix(vcell_infix: str) -> tuple[bool, str, str]:
    """
    Converts an infix string version of a VCell Native Expression, and converts it to a Python compatible version

    Args:
        vcell_infix (str): the infix to convert

    Returns:
        tuple[bool, str, str]: A tuple containing the success status, a message, and the converted infix
    """
    native = VCellNativeCalls()
    target_python_infix = MutableString("")
    return_value: ReturnValue = native.vcell_infix_to_python_infix(vcell_infix, target_python_infix)
    return return_value.success, return_value.message, target_python_infix.value


def vcell_infix_to_num_expr_infix(vcell_infix: str) -> tuple[bool, str, str]:
    """
    Converts an infix string version of a VCell Native Expression, and converts it to a NumExpr compatible version

    Args:
        vcell_infix (str): the infix to convert

    Returns:
        tuple[bool, str, str]: A tuple containing the success status, a message, and the converted infix
    """
    native = VCellNativeCalls()
    target_num_expr_infix = MutableString("")
    return_value: ReturnValue = native.vcell_infix_to_num_expr_infix(vcell_infix, target_num_expr_infix)
    return return_value.success, return_value.message, target_num_expr_infix.value
