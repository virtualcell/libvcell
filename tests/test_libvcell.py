import tempfile
from pathlib import Path

import pytest

from libvcell import (
    VCellExpressionError,
    evaluate_expression,
    sbml_to_finite_volume_input,
    sbml_to_ode_input,
    sbml_to_vcml,
    vcell_infix_to_num_expr_infix,
    vcell_infix_to_python_infix,
    vcml_convert_units,
    vcml_to_finite_volume_input,
    vcml_to_moving_boundary_input,
    vcml_to_ode_input,
    vcml_to_sbml,
    vcml_to_vcml,
)
from libvcell._internal.native_utils import VCellNativeLibraryLoader


def _native_lib_has_symbol(symbol: str) -> bool:
    """A given entry-point symbol only exists in native libraries new enough to define it.
    Returns False (skip) if the native library is missing or too old to even load."""
    try:
        return hasattr(VCellNativeLibraryLoader().lib, symbol)
    except Exception:
        return False


def _native_lib_has_moving_boundary() -> bool:
    return _native_lib_has_symbol("vcmlToMovingBoundaryInput")


def test_vcml_to_finite_volume_input(temp_output_dir: Path, vcml_file_path: Path, vcml_sim_name: str) -> None:
    vcml_content = vcml_file_path.read_text()
    success, msg = vcml_to_finite_volume_input(
        vcml_content=vcml_content, simulation_name=vcml_sim_name, output_dir_path=temp_output_dir
    )
    assert len(list(temp_output_dir.iterdir())) > 0
    assert success is True
    assert msg == "Success"


@pytest.mark.skipif(
    not _native_lib_has_moving_boundary(),
    reason="native library not yet rebuilt with vcmlToMovingBoundaryInput; run scripts/local_build_native.sh",
)
def test_vcml_to_moving_boundary_input(
    temp_output_dir: Path, moving_boundary_vcml_file_path: Path, moving_boundary_sim_name: str
) -> None:
    vcml_content = moving_boundary_vcml_file_path.read_text()
    success, msg = vcml_to_moving_boundary_input(
        vcml_content=vcml_content, simulation_name=moving_boundary_sim_name, output_dir_path=temp_output_dir
    )
    assert success is True
    assert msg == "Success"
    # a MovingBoundarySetup XML input file (consumed by vcell-mbsolver) should be written
    mb_inputs = list(temp_output_dir.glob("*mb.xml"))
    assert len(mb_inputs) == 1
    assert "<MovingBoundarySetup" in mb_inputs[0].read_text()


def test_sbml_to_finite_volume_input(temp_output_dir: Path, sbml_file_path: Path, vcml_sim_name: str) -> None:
    sbml_content = sbml_file_path.read_text()
    success, msg = sbml_to_finite_volume_input(sbml_content=sbml_content, output_dir_path=temp_output_dir)
    assert len(list(temp_output_dir.iterdir())) > 0
    assert success is True
    assert msg == "Success"


def test_sbml_to_vcml(sbml_file_path: Path) -> None:
    sbml_content = sbml_file_path.read_text()
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_output_dir = Path(temp_dir)
        vcml_file_path = temp_output_dir / "test.vcml"
        success, msg = sbml_to_vcml(sbml_content=sbml_content, vcml_file_path=vcml_file_path)
        assert vcml_file_path.exists()
        assert success is True
        assert msg == "Success"


def test_vcml_to_sbml_with_validation(vcml_file_path: Path, vcml_app_name: str) -> None:
    vcml_content = vcml_file_path.read_text()
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_output_dir = Path(temp_dir)
        sbml_file_path = temp_output_dir / "test.sbml"
        success, msg = vcml_to_sbml(
            vcml_content=vcml_content,
            application_name=vcml_app_name,
            sbml_file_path=sbml_file_path,
            round_trip_validation=True,
        )
        assert sbml_file_path.exists()
        assert success is True
        assert msg == "Success"


def test_vcml_to_sbml_without_validation(vcml_file_path: Path, vcml_app_name: str) -> None:
    vcml_content = vcml_file_path.read_text()
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_output_dir = Path(temp_dir)
        sbml_file_path = temp_output_dir / "test.sbml"
        success, msg = vcml_to_sbml(
            vcml_content=vcml_content,
            application_name=vcml_app_name,
            sbml_file_path=sbml_file_path,
            round_trip_validation=False,
        )
        assert sbml_file_path.exists()
        assert success is True
        assert msg == "Success"


def test_vcml_to_vcml(vcml_file_path: Path) -> None:
    vcml_content = vcml_file_path.read_text()
    with tempfile.TemporaryDirectory() as temp_dir:
        temp_output_dir = Path(temp_dir)
        vcml_file_path = temp_output_dir / "test.vcml"
        success, msg = vcml_to_vcml(vcml_content=vcml_content, vcml_file_path=vcml_file_path)
        assert vcml_file_path.exists()
        assert success is True
        assert msg == "Success"


def test_vcml_convert_units(vcml_file_path: Path) -> None:
    vcml_content = vcml_file_path.read_text()
    with tempfile.TemporaryDirectory() as temp_dir:
        vcell_units = Path(temp_dir) / "vcell_units.vcml"
        success, msg = vcml_convert_units(vcml_content=vcml_content, unit_system="vcell", vcml_file_path=vcell_units)
        assert (success, msg) == (True, "Success")
        assert 'LengthUnit="um"' in vcell_units.read_text()
        sbml_units = Path(temp_dir) / "sbml_units.vcml"
        success, msg = vcml_convert_units(
            vcml_content=vcell_units.read_text(), unit_system="sbml", vcml_file_path=sbml_units
        )
        assert (success, msg) == (True, "Success")
        assert 'LengthUnit="dm"' in sbml_units.read_text()
        success, _ = vcml_convert_units(vcml_content=vcml_content, unit_system="furlongs", vcml_file_path=sbml_units)
        assert success is False


def test_vcell_infix_to_python_infix() -> None:
    vcell_infix = "id_1 * csc(id_0 ^ 2.2)"
    success, msg, value = vcell_infix_to_python_infix(vcell_infix)
    expectedResult = "(id_1 * (1.0/math.sin(((id_0)**(2.2)))))"
    assert success is True
    assert msg == "Success"
    assert value == expectedResult


def test_bad_vcell_infix_through_python_conversion() -> None:
    vcellInfix = "id_1 / + / /-  cos(/ / /) id_2"
    success, msg, value = vcell_infix_to_python_infix(vcellInfix)
    assert success is False
    assert "Parse Error while parsing expression" in msg


def test_vcell_infix_to_num_expr_infix() -> None:
    vcell_infix = "(id_2 || 3.2) * id_1 * csc(id_0 ^ 2.2)"
    success, msg, value = vcell_infix_to_num_expr_infix(vcell_infix)
    expectedResult = "(where(((0.0!=id_2) | (0.0!=3.2)), id_1 * (1.0/sin(((id_0)**(2.2)))), 0.0))"
    assert success is True
    assert msg == "Success"
    assert value == expectedResult


def test_bad_vcell_infix_through_num_expr_conversion() -> None:
    vcellInfix = "id_1 / + / /-  cos(/ / /) id_2"
    success, msg, value = vcell_infix_to_num_expr_infix(vcellInfix)
    assert success is False
    assert "Parse Error while parsing expression" in msg


_skip_no_evaluate = pytest.mark.skipif(
    not _native_lib_has_symbol("evaluateExpression"),
    reason="native library not yet rebuilt with evaluateExpression; run scripts/local_build_native.sh",
)


@_skip_no_evaluate
def test_evaluate_expression_with_symbols() -> None:
    assert evaluate_expression("a + b/c", {"a": 10.0, "b": 20.0, "c": 5.0}) == 14.0


@_skip_no_evaluate
def test_evaluate_expression_constant() -> None:
    assert evaluate_expression("2 + 3 * sqrt(4)", {}) == 8.0


@_skip_no_evaluate
def test_evaluate_expression_extra_symbols_ignored() -> None:
    assert evaluate_expression("a * 2", {"a": 3.0, "unused": 99.0}) == 6.0


@_skip_no_evaluate
def test_evaluate_expression_unbound_symbol_raises() -> None:
    with pytest.raises(VCellExpressionError) as exc_info:
        evaluate_expression("a + x", {"a": 1.0})
    assert exc_info.value.error_type == "ExpressionBindingException"


@_skip_no_evaluate
def test_evaluate_expression_divide_by_zero_raises() -> None:
    with pytest.raises(VCellExpressionError) as exc_info:
        evaluate_expression("1 / c", {"c": 0.0})
    assert exc_info.value.error_type == "DivideByZeroException"


@_skip_no_evaluate
def test_evaluate_expression_domain_error_raises() -> None:
    with pytest.raises(VCellExpressionError) as exc_info:
        evaluate_expression("sqrt(a)", {"a": -1.0})
    assert exc_info.value.error_type == "FunctionDomainException"


@_skip_no_evaluate
def test_evaluate_expression_parse_error_raises() -> None:
    with pytest.raises(VCellExpressionError):
        evaluate_expression("1 / + /", {})


@_skip_no_evaluate
def test_evaluate_expression_bad_symbol_table_raises() -> None:
    # a symbol value that is not a number -> IllegalArgumentException on the Java side
    with pytest.raises(VCellExpressionError):
        evaluate_expression("a", {"a": "not a number"})  # type: ignore[dict-item]


# -- Sundials ODE input (solved downstream by pyvcell-odesolver) -- #

_skip_no_ode = pytest.mark.skipif(
    not _native_lib_has_symbol("vcmlToOdeInput"),
    reason="native library predates vcml_to_ode_input / sbml_to_ode_input",
)


def _solves(input_file: Path) -> None:
    """When pyvcell-odesolver is installed, the input must solve and write results (not a dependency here)."""
    odesolver = pytest.importorskip("pyvcell_odesolver")
    output_file = input_file.with_suffix(".ida")
    assert odesolver.solve(str(input_file), str(output_file)) == 0
    assert output_file.exists() and output_file.stat().st_size > 0


@_skip_no_ode
@pytest.mark.parametrize(
    ("simulation_name", "suffix"), [("Copy of CVODE", ".cvodeInput"), ("Copy of IDA", ".idaInput")]
)
def test_vcml_to_ode_input(temp_output_dir: Path, ode_vcml_file_path: Path, simulation_name: str, suffix: str) -> None:
    success, msg = vcml_to_ode_input(
        vcml_content=ode_vcml_file_path.read_text(), simulation_name=simulation_name, output_dir_path=temp_output_dir
    )
    assert success is True, msg
    assert ";" not in msg  # regenerated math: the message is just the input file's name
    input_file = temp_output_dir / msg
    assert input_file.suffix == suffix and input_file.exists()
    assert input_file.read_text().startswith(f"SOLVER {suffix.removeprefix('.').removesuffix('Input').upper()}")


@_skip_no_ode
def test_vcml_to_ode_input_solves(temp_output_dir: Path, ode_vcml_file_path: Path) -> None:
    success, msg = vcml_to_ode_input(ode_vcml_file_path.read_text(), "Copy of CVODE", temp_output_dir)
    assert success is True, msg
    _solves(temp_output_dir / msg)


@_skip_no_ode
def test_vcml_to_ode_input_refuses_other_solvers(temp_output_dir: Path, ode_vcml_file_path: Path) -> None:
    success, msg = vcml_to_ode_input(ode_vcml_file_path.read_text(), "Copy of forward euler", temp_output_dir)
    assert success is False
    assert "expected CVODE, IDA or the Combined IDA/CVODE solver" in msg


@_skip_no_ode
def test_vcml_to_ode_input_refuses_spatial(temp_output_dir: Path, vcml_file_path: Path, vcml_sim_name: str) -> None:
    success, msg = vcml_to_ode_input(vcml_file_path.read_text(), vcml_sim_name, temp_output_dir)
    assert success is False
    assert "is spatial" in msg


@_skip_no_ode
def test_sbml_to_ode_input(temp_output_dir: Path, ode_sbml_file_path: Path) -> None:
    success, msg = sbml_to_ode_input(
        sbml_content=ode_sbml_file_path.read_text(),
        output_dir_path=temp_output_dir,
        duration=100.0,
        output_time_step=0.5,
    )
    assert success is True, msg
    text = (temp_output_dir / msg).read_text()
    assert text.startswith("SOLVER CVODE")
    assert "ENDING_TIME 100.0" in text and "OUTPUT_TIME_STEP 0.5" in text
    _solves(temp_output_dir / msg)


@_skip_no_ode
def test_sbml_to_ode_input_refuses_a_non_positive_duration(temp_output_dir: Path, ode_sbml_file_path: Path) -> None:
    success, msg = sbml_to_ode_input(ode_sbml_file_path.read_text(), temp_output_dir, 0.0, 0.5)
    assert success is False
    assert "must be positive" in msg


@_skip_no_ode
def test_vcml_to_ode_input_uses_the_stored_math_of_a_rule_based_application(
    temp_output_dir: Path, rule_based_vcml_file_path: Path
) -> None:
    """Its network needs BioNetGen to regenerate, which libvcell does not include, so the stored math is used."""
    success, msg = vcml_to_ode_input(rule_based_vcml_file_path.read_text(), "Simulation0", temp_output_dir)
    assert success is True, msg
    name, _, note = msg.partition("; ")
    assert note.startswith("stored math (rule-based application")
    _solves(temp_output_dir / name)
