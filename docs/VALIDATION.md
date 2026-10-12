# Validation — 12 octobre 2026

## Panneaux temporaires et durée par les votes — 0.1.0-dev.5

Même cible Minecraft/Java, Windows et vrai client Vulkan :

| Vérification | Résultat |
| --- | --- |
| `gradlew.bat build` | Réussi : 22 tests Java et assemblage |
| `:fabric:runClientGameTest` | Réussi : création, publication native, votes, sauvegarde/rechargement et monde ordinaire |
| Culling et blocs réels | Aucun placement dans le champ ; vrai panneau après regard détourné, constructions et emplacements sans support conservés |
| Copies temporaires | Lecture seule native, aucun loot par le chemin vanilla de génération des drops |
| Votes et échéance | Upvote prolongeant au-delà de sept jours ; annulation ramenant l'échéance dans le passé, retrait physique différé hors champ |
| Rechargement d'une copie expirée | Nettoyage avant réception du chunk ; aucun blocage et panneau d'auteur conservé |
| Relais HTTP à horloge contrôlée | Expiration exacte, downvote, refus des votes sur expirés, retries sans renouvellement et archives persistées après redémarrage |
| Bornes du modèle | Durées de 1 à 30 jours et validation fermée du placement |
| Scripts | 8 tests Python, vérificateurs dépôt et distribution réussis |
| Bootstrap dédié | Initialiseur chargé ; arrêt à l'EULA, conservée à false |

Le culling vérifie le frustum, sans détection d'occlusion par les murs. La génération
des drops est testée directement ; une explosion complète et les variantes murales
et suspendues restent à essayer en jeu. Les archives restent sur le relais, sans
site de consultation. Réseau public HTTPS, Docker/VPS et installation du seul JAR
sur un second poste restent à valider. `gameplay_ready` reste `false` et
`validated_game_versions` reste vide.

## Historique — couleur vanilla 0.1.0-dev.4

Même cible Minecraft/Java, Windows et vrai client Vulkan :

| Vérification | Résultat |
| --- | --- |
| `gradlew.bat build` | Réussi : 18 tests Java et assemblage |
| `:fabric:runClientGameTest` | Réussi : menu natif, quatre lignes, votes, sauvegarde/rechargement et panneaux ordinaires |
| Rendu vanilla | Texte noir par défaut, sans style RGB distant ; teinture et luminosité du panneau local conservées |
| Aperçu du panneau | Ombre GUI retirée, espacement vanilla ; lisibilité contrôlée sur la capture du vrai client |
| Ancienne publication colorée v2 | Lisible et votable, sa couleur historique ignorée au rendu |
| Anciennes préférences | Relais et consentement conservés ; couleur retirée à l’enregistrement suivant |
| Scripts | 8 tests Python, vérificateurs dépôt et distribution réussis |
| Bootstrap dédié | Initialiseur chargé ; arrêt à l’EULA, conservée à false |

Le sélecteur hexadécimal est retiré. Le format des publications v2 reste compatible
avec les panneaux enregistrés. Les limites de distribution et de réseau public
ci-dessous restent applicables ; `gameplay_ready` reste `false` et aucune version
n’est ajoutée à `validated_game_versions`.

## Historique — panneaux vanilla partagés 0.1.0-dev.3

Même cible Minecraft/Java, Windows et vrai client Vulkan :

| Vérification | Résultat |
| --- | --- |
| `gradlew.bat build` | Réussi : 19 tests Java, JAR joueur et relais |
| Vocabulaire fermé | Quatre lignes requises ; texte libre, ligne inconnue, couleur invalide et panneau vide refusés |
| Votes HTTP | Pour/contre/annulation, une voix par identité, vote sur soi refusé et reprise après redémarrage |
| Menu du panneau vanilla | Mode fantôme activé dans le menu natif, saisie libre bloquée, quatre lignes et couleur partagées depuis le XYZ du panneau |
| Consultation et votes dans le jeu | Autre identité, couleur d’auteur rendue, vote depuis le menu vanilla en lecture seule |
| Sauvegarde locale du destinataire | Texte du bloc intact après rendu et fermeture du menu de consultation, même avec des lignes de test injectées |
| Sauvegarde/rechargement auteur | Métadonnées fantômes, quatre lignes et RGB retrouvés sur le vrai panneau vanilla |
| Monde ordinaire | Aucune option fantôme ; saisie vanilla et difficulté libre conservées |
| Bootstrap dédié | Initialiseur chargé ; EULA laissée à false, aucun monde dédié lancé |
| Distribution | Un seul JAR, core/network, quatre modules Fabric API avec licences, aucun test embarqué |

Le test automatisé du menu utilise l’anglais ; les catalogues FR/EN sont vérifiés
ensemble. Le rendu utilise un panneau local vierge existant au même XYZ/face :
aucun bloc distant n’est créé. Aucun chat ni signalement. Relais HTTPS public,
Docker/VPS, installation sur un second poste et essais prolongés restent à valider.
Les compatibilités déclarées restent inchangées : aucune release complète annoncée.

## Historique — Connected Solitude 0.1.0-dev.2

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
| Relais distribué `installDist` | Script autonome lancé ; `/health` répond `ok`, protocole 1 |

Captures du client : `fabric/build/run/clientGameTest/screenshots/`. Le test
client utilise des bibliothèques de test supplémentaires ; il ne remplace pas
un essai du JAR distribué seul. Les tests du relais utilisent un vrai HTTP local,
sans domaine public. Docker n'est pas installé ici : le conteneur/VPS, HTTPS
distant, installation sur un second poste et essais prolongés restent à valider.

`gameplay_ready` reste `false` et `validated_game_versions` vide jusqu'à ces
essais de distribution. La matrice GitHub du socle et celle de l'implémentation
`8e947b0` sont passées sous Windows et Linux. Chaque nouveau commit conserve
sa propre exécution CI.

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
