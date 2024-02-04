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

        mvnHash = "sha256-UH1gLE4Y6ff1zzYd6XqCpvHezSyG1BvylaJqZgUcg6o=";

        nativeBuildInputs = with pkgs; [ makeWrapper ];
        propagatedBuildInputs = with pkgs; [
          openjdk
          openjfx
        ];

        installPhase = ''
          mkdir -p $out/bin $out/share
          install -Dm644 target/SplitsTreeCE-1.0.0-SNAPSHOT.jar $out/share/SplitsTreeCE.jar

          makeWrapper ${pkgs.jre}/bin/java $out/bin/splitstreeCE \
            --add-flags "-jar $out/share/SplitsTreeCE.jar"
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
          openjfx
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

