# Validation du socle — 12 octobre 2026

Vérifications réalisées localement sous Windows, avec JDK Temurin 25.0.4.1,
Gradle 9.7.1, Loom 1.18.4, Fabric Loader 0.19.5 et Minecraft 26.4-snapshot-3.

| Vérification | Résultat |
| --- | --- |
| `gradlew.bat build` | Réussi : compilation, assemblage, 7 tests Java |
| `python -m unittest discover -s scripts/tests` | 8 tests de sélection de versions réussis |
| `python scripts/verify_repository.py` | JSON, invariants, frontières du noyau et liens locaux vérifiés |
| Consultation réelle de Fabric Meta | Cible actuelle reconnue ; aucun candidat plus récent |
| `gradlew.bat :fabric:runServer --args=nogui` | Résolution Fabric et initialiseur Multisolo exécutés ; catalogue Origine chargé |
| Inspection du JAR | Classes `core`, catalogue et licence présents ; aucun mod externe requis |

Le premier essai de bootstrap a détecté le refus de la dépendance Minecraft
`=26.4-snapshot-3`. Après correction vers la forme normalisée `=26.4-alpha.3`,
le Loader a chargé Multisolo. Le serveur s'est arrêté à la demande d'acceptation
de l'EULA ; aucun monde de test n'a été démarré et l'EULA n'a pas été acceptée.

Des avertissements de diagnostic système Windows/OSHI sont apparus avant le
chargement ; ils n'ont pas empêché l'initialisation du mod.

Ces résultats valident le socle et son chargement côté serveur. Le client, les
permissions en jeu, les interactions sociales, le réseau et les autres mods
restent à tester lors de leur implémentation. `validated_game_versions` reste
vide et `gameplay_ready` reste `false`.

La matrice GitHub Windows/Linux doit encore être vérifiée après le premier push.
