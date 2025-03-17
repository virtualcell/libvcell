from pathlib import Path

from libvcell import sbml_to_finite_volume_input, vcml_to_finite_volume_input


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
