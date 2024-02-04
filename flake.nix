{
  description = "splitstree 6";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs?ref=nixpkgs-unstable";
    utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, utils }: utils.lib.eachDefaultSystem (system:
    let
      pkgs = nixpkgs.legacyPackages.${system};


      splitsTree = pkgs.maven.buildMavenPackage rec {
        pname = "splitstreeCE";
        version = "6.2.1";

        src = ./.;

        nativeBuildInputs = with pkgs; [ makeWrapper ];

        installPhase = ''
          mkdir -p $out/bin $out/share/splitstreeCE
          ls -l
          install -Dm644 splitstreeCE/target/splitstreeCE.jar $out/share/splitstreeCE

          makeWrapper ${pkgs.jre}/bin/java $out/bin/splitstreeCE \
            --add-flags " - jar $out/share/splitstreeCE/splitstreeCE.jar "
        '';

        meta = {
          description = "
            Splitstree ";
        };
      };

    in
    {
      devShell = pkgs.mkShell {
        buildInputs = with pkgs; [
          openjdk
          maven
        ];
      };

      packages = {
        default = splitsTree;
        splitsTree = splitsTree;
      };
    }
  );
}

