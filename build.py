import os
import shutil
import subprocess
import sys
from pathlib import Path


def run_command(command, cwd=None):
    result = subprocess.run(command, shell=True, cwd=cwd, check=True, text=True)
    if result.returncode != 0:
        sys.exit(result.returncode)


def main():
    root_dir = Path(__file__).resolve().parent
    vcell_submodule_dir = root_dir / "vcell_submodule"
    vcell_native_dir = root_dir / "vcell-native"
    libvcell_lib_dir = root_dir / "libvcell" / "lib"

    # Ensure the libvcell/lib directory exists
    libvcell_lib_dir.mkdir(parents=True, exist_ok=True)

    # Build VCell Java project from submodule
    run_command("mvn --batch-mode clean install -DskipTests", cwd=vcell_submodule_dir)

    # fail if both JAVA_HOME and GRAALVM_HOME are not set
    if "JAVA_HOME" not in os.environ and "GRAALVM_HOME" not in os.environ:
        print("JAVA_HOME or GRAALVM_HOME environment variable must be set")
        sys.exit(1)

    # Check if native-image is installed
    if not shutil.which("native-image"):
        print("native-image could not be found")
        sys.exit(1)

    # Build vcell-native as Java
    run_command("mvn --batch-mode clean install", cwd=vcell_native_dir)

    # Run with native-image-agent to record configuration for native-image
    run_command(
        "java -agentlib:native-image-agent=config-output-dir=target/recording "
        "-jar target/vcell-native-1.0-SNAPSHOT.jar "
        "src/test/resources/TinySpacialProject_Application0.xml "
        "target/sbml-input",
        cwd=vcell_native_dir,
    )

    # Build vcell-native as native shared object library
    run_command("mvn package -P shared-dll", cwd=vcell_native_dir)

    # Copy the shared library to libvcell/lib
    for ext in ["so", "dylib", "dll"]:
        shared_lib = vcell_native_dir / f"target/libvcell.{ext}"
        if shared_lib.exists():
            shutil.copy(shared_lib, libvcell_lib_dir / f"libvcell.{ext}")


if __name__ == "__main__":
    main()
