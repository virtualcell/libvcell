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
    install_message_1: str = """
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
 *                                                                                      *
 *                             Building Original VCell...                               *
 *                                                                                      *
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
    """.strip()
    print(install_message_1)
    run_command("mvn --batch-mode clean install -DskipTests", cwd=vcell_submodule_dir)

    # Build vcell-native as Java
    install_message_2: str = """
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
 *                                                                                      *
 *                             Building Lib VCell...                                    *
 *                                                                                      *
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
    """.strip()
    print(install_message_2)
    run_command("mvn --batch-mode clean install", cwd=vcell_native_dir)

    # Run with native-image-agent to record configuration for native-image
    install_message_3: str = """
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
 *                                                                                      *
 *                              Run with native-image-agent...                          *
 *                                                                                      *
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
    """.strip()
    print(install_message_3)
    run_command("echo $(which java) ", cwd=vcell_native_dir)
    run_command(
        "java -agentlib:native-image-agent=config-output-dir=target/recording "
        "-jar target/vcell-native-1.0-SNAPSHOT.jar "
        "src/test/resources/TinySpatialProject_Application0.xml "
        "src/test/resources/TinySpatialProject_Application0.vcml "
        "Simulation0 "
        "unnamed_spatialGeom "
        "target/sbml-input",
        cwd=vcell_native_dir,
    )

    # Build vcell-native as native shared object library
    install_message_4: str = """
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
 *                                                                                      *
 *                                 Rebuild as shared DLL...                             *
 *                                                                                      *
/*  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  */
    """.strip()
    print(install_message_4)
    run_command("mvn --batch-mode package -P shared-dll", cwd=vcell_native_dir)

    # Copy the shared library to libvcell/lib
    for ext in ["so", "dylib", "dll"]:
        shared_lib = vcell_native_dir / f"target/libvcell.{ext}"
        if shared_lib.exists():
            shutil.copy(shared_lib, libvcell_lib_dir / f"libvcell.{ext}")

    # Copy the shared library to libvcell/lib
    copied = False
    for ext in ["so", "dylib", "dll"]:
        shared_lib = vcell_native_dir / f"target/libvcell.{ext}"
        if shared_lib.exists():
            shutil.copy(shared_lib, libvcell_lib_dir / f"libvcell.{ext}")
            copied = True
            print(f"Copied {shared_lib} to {libvcell_lib_dir}")

    if not copied:
        print(f"ERROR: No shared library found in {vcell_native_dir / 'target'}", file=sys.stderr)
        print(f"Contents: {list((vcell_native_dir / 'target').glob('*'))}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
