import tempfile
from pathlib import Path

from libvcell import (
    sbml_to_finite_volume_input,
    sbml_to_vcml,
    vcell_infix_to_num_expr_infix,
    vcell_infix_to_python_infix,
    vcml_to_finite_volume_input,
    vcml_to_sbml,
    vcml_to_vcml,
)


def test_vcml_to_finite_volume_input(temp_output_dir: Path, vcml_file_path: Path, vcml_sim_name: str) -> None:
    vcml_content = vcml_file_path.read_text()
    success, msg = vcml_to_finite_volume_input(
        vcml_content=vcml_content, simulation_name=vcml_sim_name, output_dir_path=temp_output_dir
    )
    assert len(list(temp_output_dir.iterdir())) > 0
    assert success is True
    assert msg == "Success"


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
