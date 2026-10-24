#!/usr/bin/env bash
# Pack the bundled BioNetGen (libvcell/vcell_install/bionetgen: BNG2.pl + Perl2/) into a standalone executable with
# PAR::Packer, so it runs without Perl installed. VCell's Windows code launches bionetgen/win64Standalone/BNG2.exe
# instead of `perl BNG2.pl`, so the Windows wheel needs it; CI runs this on Windows before the native build.
#
# Needs PAR::Packer's `pp` (e.g. `cpanm --notest PAR::Packer`; or set PP to its path). Usage:
#   scripts/pack_bionetgen.sh [output]     (default: libvcell/vcell_install/bionetgen/win64Standalone/BNG2.exe)
set -euo pipefail

ROOT_DIR="$( cd "$( dirname "$( dirname "${BASH_SOURCE[0]}" )" )" && pwd )"
BNG_DIR="$ROOT_DIR/libvcell/vcell_install/bionetgen"
OUTPUT="${1:-$BNG_DIR/win64Standalone/BNG2.exe}"
# pp, or PP if set; Strawberry Perl (Windows) installs pp as pp.bat, which Git Bash does not find, so fall back to
# running its pp script through perl
read -ra PP_CMD <<< "${PP:-pp}"
if ! command -v "${PP_CMD[0]}" > /dev/null 2>&1 && command -v perl > /dev/null 2>&1; then
  PP_CMD=(perl -S pp)
fi

mkdir -p "$(dirname "$OUTPUT")"
OUTPUT="$( cd "$(dirname "$OUTPUT")" && pwd )/$(basename "$OUTPUT")"
cd "$BNG_DIR"
# BNG2.pl finds Perl2/ next to itself ($FindBin::RealBin), which a packed executable does not have: embed the modules
# (-I Perl2) and name every one (-M), so modules loaded at run time are packed too.
MODULES=()
while IFS= read -r module; do
  MODULES+=(-M "$module")
done < <(cd Perl2 && find . -name "*.pm" | sed 's|^\./||; s|\.pm$||; s|/|::|g' | sort)
"${PP_CMD[@]}" -I Perl2 "${MODULES[@]}" -o "$OUTPUT" BNG2.pl

# smoke test: the packed executable generates a reaction network using only what it carries
SMOKE_DIR="$(mktemp -d)"
cat > "$SMOKE_DIR/smoke.bngl" <<'BNGL'
begin model
begin parameters
  kf 1.0
end parameters
begin molecule types
  A(b)
  B(a)
end molecule types
begin seed species
  A(b) 10
  B(a) 10
end seed species
begin reaction rules
  A(b) + B(a) <-> A(b!1).B(a!1) kf, kf
end reaction rules
end model
generate_network({overwrite=>1})
BNGL
if ! (cd "$SMOKE_DIR" && env -u PERL5LIB -u BNGPATH -u BioNetGenRoot "$OUTPUT" smoke.bngl > smoke.log 2>&1); then
  cat "$SMOKE_DIR/smoke.log"
  echo "the packed BioNetGen failed its smoke test"
  exit 1
fi
if ! grep -q "begin reactions" "$SMOKE_DIR/smoke.net"; then
  cat "$SMOKE_DIR/smoke.log"
  echo "the packed BioNetGen generated no network"
  exit 1
fi
echo "packed $OUTPUT ($(wc -c < "$OUTPUT") bytes); its smoke test generated a $(grep -c '' "$SMOKE_DIR/smoke.net")-line network"
