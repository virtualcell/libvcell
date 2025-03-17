import shutil
import subprocess
import sys
from collections.abc import Mapping
from pathlib import Path


def run_command(command: str, cwd: Path, env: Mapping[str, str] | None = None) -> None:
    result = subprocess.run(command, cwd=cwd, env=env, shell=True, check=True, text=True)
    if result.returncode != 0:
        sys.exit(result.returncode)


def main() -> None:
    root_dir = Path(__file__).resolve().parent
    vcell_submodule_dir = root_dir / "vcell_submodule"
    vcell_native_dir = root_dir / "vcell-native"
    libvcell_lib_dir = root_dir / "libvcell" / "lib"

    # Ensure the libvcell/lib directory exists
    libvcell_lib_dir.mkdir(parents=True, exist_ok=True)

    # Build VCell Java project from submodule
    run_command("mvn --batch-mode clean install -DskipTests", cwd=vcell_submodule_dir)

    # Build vcell-native as Java
    run_command("mvn --batch-mode clean install", cwd=vcell_native_dir)

    # Run with native-image-agent to record configuration for native-image
    run_command(
        "java -agentlib:native-image-agent=config-output-dir=target/recording "
        "-jar target/vcell-native-1.0-SNAPSHOT.jar "
        "src/test/resources/TinySpatialProject_Application0.xml "
        "src/test/resources/TinySpatialProject_Application0.vcml "
        "Simulation0 "
        "target/sbml-input",
        cwd=vcell_native_dir,
    )

    # Build vcell-native as native shared object library
    run_command("mvn --batch-mode package -P shared-dll", cwd=vcell_native_dir)

    # Copy the shared library to libvcell/lib
    for ext in ["so", "dylib", "dll"]:
        shared_lib = vcell_native_dir / f"target/libvcell.{ext}"
        if shared_lib.exists():
            shutil.copy(shared_lib, libvcell_lib_dir / f"libvcell.{ext}")


if __name__ == "__main__":
    main()
