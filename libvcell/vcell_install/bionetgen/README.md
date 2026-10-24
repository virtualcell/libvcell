# BioNetGen 2.9.3 (bundled)

The Perl part of [BioNetGen](https://github.com/RuleWorld/bionetgen) 2.9.3, MIT-licensed (`LICENSE`, `CREDITS.txt`):
`BNG2.pl` and its `Perl2/` modules, copied unchanged from the official release
`BioNetGen-2.9.3-linux.tar.gz` (sha256 `b85743d422aa3fd112aceb3725dd1b1f2ff1ad829d20d71682680426582bcb5d`;
identical in the mac release, and in the win release apart from line endings).

VCell runs it to expand a rule-based application's reaction rules into a reaction network when it generates the
math (`perl BNG2.pl`), looking for it at `<vcell.installDir>/bionetgen/BNG2.pl`; libvcell points `vcell.installDir`
at this package's `vcell_install/`. Only network generation is used, so BioNetGen's compiled simulators (`bin/`) are
not included. It needs `perl` on the `PATH` on macOS and Linux (they have it). On Windows VCell runs a standalone executable
instead, `win64Standalone/BNG2.exe`: these same files packed with PAR::Packer by `scripts/pack_bionetgen.sh` in the
Windows CI job (it embeds a Perl interpreter, so Windows needs no Perl; it is a build product, not in git). If
BioNetGen cannot run, libvcell falls back to the math stored in the document and says so.

**Checked against VCell's own BioNetGen (2.3.0, `vcell_submodule/bionetgen`)** on the 109 rule-based models with ODE
simulations in a 2,730-model VCell corpus (2026-10-23): 103 give byte-identical solver input with either version, and
the other 6 fail identically with both (unit errors and invalid sizes in the models, two BioNetGen timeouts). Recheck
like this before updating to a newer release.

To update: copy `BNG2.pl`, `Perl2/`, `VERSION`, `LICENSE` and `CREDITS.txt` from a newer release and update this file.
