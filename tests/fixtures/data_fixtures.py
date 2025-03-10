import os
import shutil
from collections.abc import Generator
from pathlib import Path

import pytest

fixtures_dir = Path(os.path.dirname(os.path.abspath(__file__))) / "data"


@pytest.fixture
def vcml_file_path() -> Path:
    return fixtures_dir / "TinySpatialProject_Application0.vcml"


@pytest.fixture
def vcml_sim_name() -> str:
    return "Simulation0"


@pytest.fixture
def sbml_file_path() -> Path:
    return fixtures_dir / "TinySpatialProject_Application0.xml"


@pytest.fixture(scope="function")
def temp_output_dir() -> Generator[Path, None, None]:
    output_dir = fixtures_dir / "output"
    if not output_dir.exists():
        output_dir.mkdir()
    else:
        for file in output_dir.iterdir():
            file.unlink()

    yield output_dir

    shutil.rmtree(output_dir)
