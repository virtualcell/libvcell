import os
import shutil
import subprocess
import sys
from collections.abc import Mapping
from pathlib import Path


def run_command(command: str, cwd: Path, env: Mapping[str, str]) -> None:
    result = subprocess.run(command, cwd=cwd, env=env, shell=True, check=True, text=True)
    if result.returncode != 0:
        sys.exit(result.returncode)


def check_env() -> Mapping[str, str]:
    # print the path environment variable
    PATH: str = os.environ["PATH"]
    JAVA_HOME: str | None = os.environ["JAVA_HOME"] or None
    GRAALVM_HOME: str | None = os.environ["GRAALVM_HOME"] or None
    MAVEN_HOME: str | None = os.environ["MAVEN_HOME"] or None
    if JAVA_HOME is None and GRAALVM_HOME is None:
        print("JAVA_HOME or GRAALVM_HOME environment variable must be set")
        sys.exit(1)
    if MAVEN_HOME is None:
        print("MAVEN_HOME environment variable must be set")
        sys.exit(1)

    print(f"PATH={PATH}")
    print(f"JAVA_HOME={JAVA_HOME}")
    print(f"GRAALVM_HOME={GRAALVM_HOME}")
    print(f"MAVEN_HOME={MAVEN_HOME}")

    # find path to mvn
    mvn_path: str | None = shutil.which("mvn")
    if mvn_path is None:
        print("mvn could not be found")
        sys.exit(1)
    mvn_install_dir = Path(mvn_path).parent

    # find path to poetry
    poetry_path: str | None = shutil.which("poetry")
    if poetry_path is None:
        print("poetry could not be found")
        sys.exit(1)
    poetry_install_dir = Path(poetry_path).parent

    # Check if native-image is installed
    native_image_path: str | None = shutil.which("native-image")
    if native_image_path is None:
        print("native-image could not be found")
        sys.exit(1)
    native_image_install_dir = Path(native_image_path).parent

    # get PATH and append mvn and poetry install directories
    NEW_PATH = f"{mvn_install_dir}:{poetry_install_dir}:{native_image_install_dir}:{PATH}"

    new_env = os.environ.copy()
    new_env["PATH"] = NEW_PATH
    if JAVA_HOME is not None:
        new_env["JAVA_HOME"] = JAVA_HOME
    if GRAALVM_HOME is not None:
        new_env["GRAALVM_HOME"] = GRAALVM_HOME
    new_env["MAVEN_HOME"] = MAVEN_HOME

    return new_env


def main() -> None:
    root_dir = Path(__file__).resolve().parent
    vcell_submodule_dir = root_dir / "vcell_submodule"
    vcell_native_dir = root_dir / "vcell-native"
    libvcell_lib_dir = root_dir / "libvcell" / "lib"

    # Ensure the libvcell/lib directory exists
    libvcell_lib_dir.mkdir(parents=True, exist_ok=True)

    # Check the environment variables and pass them to the subprocess
    new_env = check_env()

    # Build VCell Java project from submodule
    run_command("mvn --batch-mode clean install -DskipTests", cwd=vcell_submodule_dir, env=new_env)

    # Build vcell-native as Java
    run_command("mvn --batch-mode clean install", cwd=vcell_native_dir, env=new_env)

    # Run with native-image-agent to record configuration for native-image
    run_command(
        "java -agentlib:native-image-agent=config-output-dir=target/recording "
        "-jar target/vcell-native-1.0-SNAPSHOT.jar "
        "src/test/resources/TinySpacialProject_Application0.xml "
        "target/sbml-input",
        cwd=vcell_native_dir,
        env=new_env,
    )

    # Build vcell-native as native shared object library
    run_command("mvn package -P shared-dll", cwd=vcell_native_dir, env=new_env)

    # Copy the shared library to libvcell/lib
    for ext in ["so", "dylib", "dll"]:
        shared_lib = vcell_native_dir / f"target/libvcell.{ext}"
        if shared_lib.exists():
            shutil.copy(shared_lib, libvcell_lib_dir / f"libvcell.{ext}")


if __name__ == "__main__":
    main()
