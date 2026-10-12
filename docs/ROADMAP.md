# Roadmap

Les cases décrivent du travail vérifiable. Elles n'annoncent aucune date.

## 0 — Dépôt maintenable

- [x] Modules `core` et `fabric`, un seul JAR.
- [x] Règles d'expérience, compatibilité, consentement et politique des invités.
- [x] Tests métier, catalogue initial, documentation et instructions agents.
- [x] CI et workflow de détection des nouvelles versions.
- [x] Vérifier les workflows du premier commit sous Windows et Linux sur GitHub.

## v0.1 — Connected Solitude

- [x] Fixer la seed `0` et la difficulté normale, origine révision 2.
- [x] Implémenter la création d'un nouveau monde avec spawn vanilla et métadonnées.
- [x] Implémenter le verrouillage de difficulté pour ces mondes uniquement.
- [x] Menu Multisolo sans autre mod à installer, français et anglais.
- [x] Consentement persistant, worker réseau et identité Ed25519 locale stable.
- [x] Relais autonome, messages persistants XYZ et isolation par version/dimension.
- [x] Tests réseau à deux identités, redémarrage et droits de suppression.
- [x] Test client automatisé : création, message, sauvegarde/rechargement et monde ordinaire.
- [ ] Valider l'installation du JAR distribué seul sur un second profil/poste.
- [ ] Choisir le VPS OVH et le domaine, tester le relais HTTPS entre deux machines.
- [x] Écriture fermée : quatre lignes prédéfinies, validées côté relais, sans signalement.
- [x] Menu du panneau vanilla, couleur hexadécimale et votes persistants.
- [ ] Tests de charge et quotas globaux avant accès public ouvert.

## Après v0.1 — Présence asynchrone enrichie

- [x] Rendu du texte sur un panneau vanilla local déjà posé, sans mutation distante.
- [ ] Politique de conservation et expiration éventuelle avec les premiers retours.
- [ ] Échos visuels sans collision ni influence sur le monde.
- [ ] Première alpha fermée, retours sur intérêt du concept et fluidité.

## 3 — Entraide temporaire

- [ ] Connexion entre hôte et invité malgré les NAT.
- [ ] Demande, double consentement, expiration et révocation.
- [ ] Équipement temporaire, inventaires séparés et actions filtrées côté serveur.
- [ ] Tests des paquets, conteneurs, déconnexion, mort et retour au monde d'origine.

## 4 — Diffusion et maintenance

- [ ] Publications Modrinth et CurseForge, changelog et guide joueur.
- [ ] Bot de portage : issue → branche → correction → PR → validation.
- [ ] Valider chaque version avant de l'annoncer compatible.
- [ ] Porter sur chaque version Java depuis **26.1**, en conservant des builds vérifiés.
- [ ] Plusieurs expériences et intégrations worldgen explicites, après la vanilla.

Le [contrat v0.1](releases/0.1-connected-solitude.md) est prioritaire. Ne pas
élargir à l'entraide ou aux échos avant validation de ces quatre fonctions.
