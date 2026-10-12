<p align="center">
  <img src="docs/assets/banner.svg" alt="Multisolo — Solo, ensemble." width="960">
</p>

<p align="center">
  <strong>Ton monde. Tes découvertes. La présence des autres.</strong><br>
  Un projet de mod Fabric par <strong>koka99cab</strong>.
</p>

<p align="center">
  <a href="docs/DESIGN.md">Le concept</a> ·
  <a href="docs/ROADMAP.md">La suite</a> ·
  <a href="docs/DEVELOPMENT.md">Développer</a> ·
  <a href="docs/MAINTENANCE.md">Maintenir</a>
</p>

## Une aventure solo qui laisse passer les autres

Multisolo veut relier des parties solo **indépendantes**, jouées sur une même
seed. Des messages laissés au même endroit, des silhouettes furtives, et un
appel à l'aide pour accueillir temporairement un autre joueur : l'inspiration
vient du multijoueur asynchrone d'Elden Ring.

Chaque joueur garde sa sauvegarde et sa progression. Le **spawn vanilla est
conservé**. Les invités ont un inventaire verrouillé : ils ne doivent ni prendre
le butin, ni modifier les constructions. Les permissions sociales se règlent
dans le futur menu Multisolo ; les règles d'une expérience restent communes.

## État réel du projet

**Socle de développement — pas encore une version jouable de l'expérience.**

| Disponible dans le dépôt | À construire |
| --- | --- |
| Projet Gradle et point d'entrée Fabric | Création des mondes d'expérience et difficulté verrouillée |
| Modèle d'expérience immuable et compatibilité | Menu de consentement et persistance des préférences |
| Politique des invités et tests des règles | Application de ces restrictions côté serveur |
| Une définition d'expérience, « Origine » | Messages, silhouettes, relais réseau et entraide |
| CI, veille des versions et guide pour les bots | Validation en jeu, distribution Modrinth / CurseForge |

La cible de compilation est **Minecraft Java `26.4-snapshot-3`**, avec
**Java 25** et **Fabric Loader 0.19.5**. Au 12 octobre 2026, 26.4 est encore
en développement. Aucune compatibilité en jeu n'est déclarée avant les essais
du [protocole de validation](docs/TESTING.md).

## Une installation simple

L'objectif est **un seul mod à ajouter : `multisolo-….jar`**, dans un profil
Minecraft utilisant Fabric Loader. Le socle actuel ne nécessite ni Fabric API,
ni Mod Menu, ni bibliothèque de configuration à installer séparément.

Le code `core` est inclus dans le même JAR. Si une API Fabric devient nécessaire,
ses modules indispensables devront être embarqués avec leurs licences et
vérifiés dans un profil ne contenant que Multisolo. Fabric Loader reste le
prérequis de lancement. Aucun service réseau Multisolo n'est encore déployé.

## Commencer à développer

Installer un **JDK 25**, puis :

```powershell
# Windows
.\gradlew.bat build
.\gradlew.bat :fabric:runClient
```

```sh
# Linux / macOS
./gradlew build
./gradlew :fabric:runClient
```

Le JAR du mod se trouve dans `fabric/build/libs/`. Le JAR `-sources` et celui
de `core` ne sont pas des mods supplémentaires à installer. Le lancement de
développement teste actuellement le chargement du socle.

## Se repérer

```text
core/              Règles métier Java, sans dépendance à Minecraft ou Fabric
fabric/            Adaptateur du jeu et unique JAR distribué
docs/              Conception, architecture, maintenance et décisions
schemas/           Contrats JSON pour les expériences et la compatibilité
scripts/           Veille de versions et vérification du dépôt
.github/           CI, veille planifiée, modèles d'issues et de PR
AGENTS.md          Instructions de travail pour les assistants et bots
compatibility.json État lisible par une machine, sans promesses de compatibilité
```

Pour un humain, commencer par le [concept](docs/DESIGN.md), puis
l'[architecture](docs/ARCHITECTURE.md). Pour un agent, lire [AGENTS.md](AGENTS.md).
Les choix structurants sont conservés dans les [décisions](docs/decisions/0001-foundation.md).

## Construire quelque chose qui dure

Une première expérience vanilla, puis des expériences distinctes selon la
génération du monde. Une nouvelle seed ou difficulté donne une nouvelle
révision ; elle ne réécrit jamais une partie existante. La même seed seule ne
suffit pas à garantir des mondes compatibles entre versions ou mods.

La veille GitHub est préparée pour détecter de nouvelles versions et ouvrir
une tâche de portage. Un bot pourra ensuite préparer une PR selon le
[guide de maintenance](docs/MAINTENANCE.md), avec compilation, tests et essais
en jeu avant publication. La veille ne réalise pas encore le portage toute seule.

L'organisation s'inspire de [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat),
[Sodium](https://github.com/CaffeineMC/sodium) et
[Lithium](https://github.com/CaffeineMC/lithium) ; le build suit le
[template Fabric](https://github.com/FabricMC/fabric-example-mod).

Contributions : [CONTRIBUTING.md](CONTRIBUTING.md). Licence du code original :
[MIT](LICENSE). Multisolo est un projet indépendant, sans affiliation à Mojang ou Microsoft.
