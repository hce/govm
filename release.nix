let
  pkgs = import <nixpkgs> { };
  version = "0.1.1";
in
rec {
  compiler = pkgs.stdenv.mkDerivation (rec {
    name = "govmc";
    buildInputs = [ pkgs.jdk pkgs.python ];
    src = ./.;
    configurePhase = ''
      substituteInPlace govmc \
        --replace "/path/to/jdk" "${pkgs.jdk.home}"
    '';
  } );

  vm = pkgs.stdenv.mkDerivation (rec {
    name = "govm";
    buildInputs = [ pkgs.python ];
    src = ./pygovm;
    configurePhase = ''
      substituteInPlace govm \
        --replace "/path/to/python" "${pkgs.python}"
    '';
  } );
}
