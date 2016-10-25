let
  pkgs = import <nixpkgs> { };
  version = "0.1.1";
  tehjdk = pkgs.openjdk;
in
rec {
  compiler = pkgs.stdenv.mkDerivation (rec {
    name = "govmc";
    buildInputs = [ tehjdk pkgs.python ];
    src = ./.;
    configurePhase = ''
      substituteInPlace govmc \
        --replace "/path/to/jdk" "${tehjdk.home}"
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
