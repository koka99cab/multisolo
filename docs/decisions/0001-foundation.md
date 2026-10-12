# 0001 — Un noyau indépendant, un adaptateur Fabric

- Statut : adopté pour le socle initial.
- Date : 2026-10-12.

## Besoin

Porter rapidement Multisolo tout en gardant un dépôt lisible pour un humain et
un agent. Les joueurs doivent ajouter un seul mod ; le projet commence avec
une seule expérience.

## Décision

Deux modules Gradle : règles Java dans `core`, intégration Minecraft dans
`fabric`. Le JAR Fabric inclut nos classes `core`. Aucun autre mod n'est requis
par le socle. Java 25, Loom non remappant et versions exactes sont utilisés pour
la cible disponible `26.4-snapshot-3`.

Les expériences sont immuables et révisées. Le menu futur modifiera le consentement
social ; la difficulté et la seed seront verrouillées par expérience. Le catalogue
initial Origine utilise provisoirement une seed de développement et `NORMAL`.

Le code original adopte MIT ; ce choix est modifiable avant une première publication.
Le dépôt démarre privé. La veille ouvre des tâches, le futur bot prépare des PR,
et les releases attendent leur validation en jeu.

## Conséquences

Les règles métier sont vérifiables sans lancer le jeu. Chaque portage devra
toujours adapter et tester Fabric. Les fonctions réseau, le menu et le filtrage
serveur ne sont pas présentés comme terminés. Le relais et le traitement NAT
feront l'objet d'une décision distincte lorsqu'ils seront construits.
