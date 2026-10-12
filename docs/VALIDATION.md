# Validation — 12 octobre 2026

## Connected Solitude — 0.1.0-dev.2

Même cible Minecraft/Java que le socle. Vérifications locales sous Windows :

| Vérification | Résultat |
| --- | --- |
| `gradlew.bat build` | Réussi : 16 tests Java et assemblage des quatre modules |
| `:relay:test` | Deux identités, signatures, isolation version/dimension/rayon, suppression auteur, quotas et reprise après redémarrage |
| `:fabric:runClientGameTest` | Réussi : vrai client Vulkan, monde seed 0 créé depuis le menu, difficulté fixe, message publié et lu par une autre identité |
| Sauvegarde/rechargement dans le test client | Expérience marquée retrouvée, aucune conversion |
| Monde ordinaire dans le test client | Difficulté modifiable, aucun verrouillage Multisolo |
| `verify_distribution.py` | Un JAR joueur avec core/network, quatre modules Fabric API et leurs licences ; aucun test embarqué |
| Bootstrap dédié | Initialiseur chargé ; arrêt à l'EULA, laissée à false |

Captures du client : `fabric/build/run/clientGameTest/screenshots/`. Le test
client utilise des bibliothèques de test supplémentaires ; il ne remplace pas
un essai du JAR distribué seul. Les tests du relais utilisent un vrai HTTP local,
sans domaine public. Docker n'est pas installé ici : le conteneur/VPS, HTTPS
distant, installation sur un second poste et essais prolongés restent à valider.

`gameplay_ready` reste `false` et `validated_game_versions` vide jusqu'à ces
essais de distribution. La matrice GitHub du premier commit est passée sous
Windows et Linux ; la nouvelle implémentation doit repasser sa propre CI.

## Historique — socle 0.1.0-dev.1

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

À cette étape initiale, seul le socle et son chargement côté serveur étaient
validés. Les résultats de la v0.1 ci-dessus décrivent les essais ajoutés ensuite.
