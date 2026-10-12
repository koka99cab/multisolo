# Multisolo — instructions pour agents

Lire d'abord `README.md`, `compatibility.json`, `docs/DESIGN.md`, puis le guide
correspondant au travail : `docs/ARCHITECTURE.md` ou `docs/MAINTENANCE.md`.

## Invariants du produit

- Une sauvegarde solo indépendante par joueur ; spawn vanilla conservé.
- Seed, difficulté et génération sont fixées par une expérience révisée.
- Les préférences du menu concernent le social, jamais les règles de l'expérience.
- L'invité ne peut pas prendre/donner de loot, modifier l'inventaire, les blocs,
  les conteneurs ou la difficulté. Appliquer la politique côté serveur.
- Une seule installation de mod côté joueur. Le JAR final inclut notre `core`.
- Aucun contact réseau avant consentement explicite. Hors ligne doit fonctionner.
- Ne jamais convertir une sauvegarde personnelle ou migrer une expérience en silence.

## Frontières du code

- `core` : Java pur, pas d'import `net.minecraft`, `net.fabricmc` ou de réseau.
- `network` : identité, protocole et client HTTP sans dépendance au jeu.
- `relay` : service autonome et stockage des messages ; jamais embarqué dans le mod.
- `fabric/src/main` : adaptateur serveur/commun ; aucune classe client ici.
- `fabric/src/client` : menu et consentement ; aucune requête sur le thread du jeu.
- `fabric/src/main/resources/data/multisolo/experiences` : catalogue embarqué.
- Ne pas ajouter de mixin sans expliquer pourquoi un événement/API ne suffit pas.
- Préférer les modules Fabric API strictement nécessaires, embarqués par Loom,
  à une dépendance externe imposée au joueur. Vérifier leurs licences.

## Versions et validation

- `gradle.properties` est la source des versions de construction ; versions exactes.
- Distinguer l'id Mojang `minecraft_version` de sa forme normalisée dans le manifeste
  `minecraft_dependency_version` ; vérifier la résolution avec Fabric Loader.
- Minecraft 26.1+ est non obfusqué : utiliser `net.fabricmc.fabric-loom`, sans Yarn
  ni tâche de remapping héritée des anciennes versions.
- `compatibility.json` distingue cible, modèles implémentés et fonctionnalités futures.
- Ne pas inscrire une version dans `validated_game_versions` sans essais en jeu.
- Exécuter `./gradlew build`, `python scripts/verify_repository.py` et
  `python -m unittest discover -s scripts/tests`. Sous Windows : `gradlew.bat`.
- Après le build : `python scripts/verify_distribution.py`. Pour le bootstrap,
  suivre `docs/TESTING.md` ; laisser l'EULA du profil de test non acceptée.
- Tester les invariants et les régressions, pas chaque getter.
- Une compilation réussie ne prouve ni la compatibilité réseau ni les permissions.

## Portage automatique

La veille ouvre une issue ; elle ne porte pas le mod. Pour un portage autorisé,
créer `codex/port-<version>`, suivre `docs/MAINTENANCE.md`, produire une PR avec
les résultats et limites. Pas de publication automatique d'un JAR non validé.
Préserver le format du protocole et des sauvegardes ou documenter leur migration.

Documentation et échanges : français. Identifiants de code et commits : anglais.
Garder le README honnête sur ce qui est réellement jouable.

La v0.1 « Connected Solitude » fixe la seed 0 (origine révision 2), le spawn
vanilla, l'identité réseau et les messages persistants géolocalisés. Les échos
expérimentaux sont autorisés depuis dev.6 ; l'entraide reste ultérieure.
Le support de toutes les versions Java à partir de
26.1 est un objectif ; ne pas élargir le manifeste sans valider chaque portage.

Les messages v2 partent d'un panneau vanilla posé, dans son menu natif : quatre
lignes de catalogue uniquement, rendu couleur vanilla et votes. Aucun chat ni signalement.
Chez le destinataire, créer uniquement des copies temporaires marquées de panneaux
vanilla, dans de l'air sans fluide et sur un support valide. Ne jamais remplacer
une construction. Placement et retrait exigent une position hors champ pendant
une seconde ; nettoyer les copies expirées avant l'envoi d'un chunk rechargé.
Ces copies ne donnent aucun loot. Ne jamais retirer le panneau posé par l'auteur.
La durée depuis publication vaut 7 + upvotes - downvotes jours, bornée de 1 à 30 ;
conserver les archives du relais après expiration. Voir la décision 0004.

Les échos exigent un consentement distinct, désactivé par défaut et jamais déduit
des préférences anciennes. Capturer uniquement des mouvements/gestes bornés et
une catégorie d'outil : aucun pseudo, URL de skin, inventaire ou action de bloc.
Le rendu utilise des états d'avatar détachés ; ne jamais ajouter d'entité de jeu.
Huit secondes maximum par séquence, au plus une publication par minute ; TTL
de quinze minutes en mémoire sur le relais, sans archive. Voir la décision 0005.
