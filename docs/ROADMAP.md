# Roadmap

Les cases décrivent du travail vérifiable. Elles n'annoncent aucune date.

## 0 — Dépôt maintenable

- [x] Modules `core` et `fabric`, un seul JAR.
- [x] Règles d'expérience, compatibilité, consentement et politique des invités.
- [x] Tests métier, catalogue initial, documentation et instructions agents.
- [x] CI et workflow de détection des nouvelles versions.
- [ ] Vérifier les workflows sur GitHub après le premier push.

## 1 — Une expérience locale cohérente

- [ ] Explorer et confirmer la seed et la difficulté d'Origine.
- [ ] Créer un nouveau monde d'expérience avec spawn vanilla et métadonnées persistées.
- [ ] Verrouiller difficulté et règles côté serveur dans ces mondes uniquement.
- [ ] Menu Multisolo accessible sans mod supplémentaire, en français et anglais.
- [ ] Consentement social persistant et fonctionnement hors ligne.
- [ ] Essais sur un profil propre, sauvegarde/rechargement et mise à jour.

## 2 — Solo asynchrone

- [ ] Choisir et documenter l'hébergement et l'authentification du relais.
- [ ] Messages géolocalisés par expérience, dimension et compatibilité.
- [ ] Blocage, signalement, expiration et limites de débit.
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
- [ ] Plusieurs expériences et intégrations worldgen explicites, après la vanilla.

Première tâche recommandée : création d'un monde « Origine » séparé et verrouillage
de la difficulté, avec un essai prouvant qu'un monde solo ordinaire reste inchangé.
