# Changelog

## 0.1.0-dev.6 — Apparitions de joueurs

- Capture de déplacements, regard, accroupissement, course et gestes, jusqu'à huit secondes.
- Consentement capture/lecture distinct des panneaux, désactivé pour les anciennes préférences.
- Rejeu interpolé sur le renderer natif : corps translucide, fondu et outil générique, sans entité de jeu.
- Routes v2 signées et isolées par expérience/version/dimension ; clips en mémoire expirant après quinze minutes.
- Stockage et trajectoires bornés ; aucune archive de mouvements, aucun pseudo, skin personnel ni inventaire transmis.
- Deux modules Fabric additionnels embarqués avec leurs licences ; toujours un seul JAR joueur.

## 0.1.0-dev.5 — Panneaux temporaires et votes

- Copies de vrais panneaux vanilla au même XYZ, avec leur type et leur orientation.
- Placement et retrait après une seconde hors du champ de la caméra, à huit blocs minimum.
- Air libre et support valide requis ; constructions intactes, copies cirées et sans loot.
- Durée depuis publication : 7 + upvotes − downvotes jours, bornée de 1 à 30 jours.
- Archives du relais conservées après expiration ; aucun vote pour réactiver un expiré.
- Nettoyage des copies expirées au rechargement ; panneau d'origine de l'auteur conservé.
- Placement facultatif dans le protocole v2 : archives anciennes lisibles sans bloc deviné.

## 0.1.0-dev.4 — Couleur vanilla

- Retrait du choix hexadécimal et de son écran de préférences.
- Texte des panneaux rendu avec leur couleur vanilla ; teintures et encre lumineuse locales conservées.
- Aperçu dans le menu du panneau sans ombre ajoutée, avec l’espacement vanilla des lignes.
- Anciennes couleurs Multisolo ignorées au rendu, sans rupture du protocole v2 ni perte des publications.
- Anciennes préférences lues en conservant relais et consentement.

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
