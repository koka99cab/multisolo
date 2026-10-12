# Changelog

## 0.1.0-dev.3 — Panneaux vanilla partagés

- Partage depuis le menu du vrai panneau posé, uniquement dans Multisolo.
- Quatre lignes de catalogue, texte libre partagé et chat abandonnés.
- Couleur #RRGGBB choisie hors ligne et transmise avec le panneau.
- Votes pour/contre/annulation, persistants et limités à une voix par identité.
- Lecture sur un panneau local vierge déjà posé, sans écriture distante.
- Protocole et stockage v2 ; anciens messages conservés à part, sans import.
- Aucun signalement ni sanction automatique.

## 0.1.0-dev.2 — Connected Solitude (en validation)

- Nouvelle expérience : seed `0`, révision 2, survie normale et spawn vanilla.
- Création d'une sauvegarde distincte depuis le menu Multisolo ; difficulté verrouillée.
- Menu français/anglais accessible depuis l'accueil et la pause, consentement persistant.
- Identité réseau Ed25519 persistante et signatures des requêtes.
- Messages géolocalisés persistants : publication, lecture à 128 blocs et suppression par auteur.
- Relais Java autonome avec stockage disque, limites et préparation Docker/VPS.
- Objectif de support Java 26.1+ documenté ; seule la snapshot courante est ciblée.

Le relais public n'est pas déployé. La compatibilité complète et la diffusion
aux joueurs restent soumises à la validation. Les échos et l'entraide sont hors v0.1.

## 0.1.0-dev.1 — socle initial

- Projet Fabric ciblant Minecraft Java 26.4-snapshot-3 et Java 25.
- Règles Java indépendantes du jeu : expérience, compatibilité, consentement,
  politique des invités et tests.
- Catalogue initial « Origine », seed de développement et difficulté normale.
- Construction d'un seul JAR Multisolo, sans mod additionnel obligatoire.
- Documentation française, instructions agents, CI et veille de versions.

Le gameplay, le menu, le relais et l'application des permissions en jeu ne sont
pas encore implémentés. Ce socle n'est pas une release destinée aux joueurs.
