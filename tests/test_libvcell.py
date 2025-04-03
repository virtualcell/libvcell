from pathlib import Path

from libvcell import sbml_to_finite_volume_input, sbml_to_vcml, vcml_to_finite_volume_input, vcml_to_sbml, vcml_to_vcml


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


def test_sbml_to_vcml(temp_output_dir: Path, sbml_file_path: Path) -> None:
    sbml_content = sbml_file_path.read_text()
    vcml_file_path = temp_output_dir / "test.vcml"
    success, msg = sbml_to_vcml(sbml_content=sbml_content, vcml_file_path=vcml_file_path, validate=False)
    assert vcml_file_path.exists()
    assert success is True
    assert msg == "Success"


def test_vcml_to_sbml(temp_output_dir: Path, vcml_file_path: Path, vcml_app_name: str) -> None:
    vcml_content = vcml_file_path.read_text()
    sbml_file_path = temp_output_dir / "test.sbml"
    success, msg = vcml_to_sbml(
        vcml_content=vcml_content, application_name=vcml_app_name, sbml_file_path=sbml_file_path, validate=False
    )
    assert sbml_file_path.exists()
    assert success is True
    assert msg == "Success"


def test_vcml_to_vcml(temp_output_dir: Path, vcml_file_path: Path) -> None:
    vcml_content = vcml_file_path.read_text()
    vcml_file_path = temp_output_dir / "test.vcml"
    success, msg = vcml_to_vcml(vcml_content=vcml_content, vcml_file_path=vcml_file_path)
    assert vcml_file_path.exists()
    assert success is True
    assert msg == "Success"
