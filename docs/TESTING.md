# Vérifier Multisolo

## Vérifications automatisées

```sh
./gradlew build
python scripts/verify_repository.py
python -m unittest discover -s scripts/tests
python scripts/verify_distribution.py
```

Les tests Java vérifient les frontières d'expérience, le double consentement
et les actions refusées aux invités. Ils ne simulent pas un serveur Minecraft.
Le vérificateur contrôle les fichiers JSON, les invariants du catalogue et les
frontières d'import de `core`. Les tests Python couvrent la sélection des versions.

La CI lance aussi le bootstrap Fabric du serveur dans son répertoire de développement,
puis vérifie que l'initialiseur Multisolo a chargé le catalogue. Elle laisse
`eula=false` : le serveur s'arrête avant de démarrer un monde. Ce contrôle attrape
les erreurs de résolution du manifeste sans prétendre valider une partie en jeu.
Le vérificateur de distribution contrôle les dépendances et les classes `core`
dans le JAR réellement produit.

## Essais en jeu avant déclaration de compatibilité

Ces scénarios deviendront applicables avec chaque fonctionnalité livrée. Tant
qu'ils ne sont pas exécutables et vérifiés, garder `gameplay_ready` à `false`.

| Scénario | Résultat attendu |
| --- | --- |
| Profil propre Fabric + seul JAR Multisolo | Chargement sans demander un autre mod |
| Nouvelle expérience Origine, puis rechargement | Seed, révision et difficulté persistées ; spawn vanilla |
| Monde solo ordinaire | Aucun verrouillage ni changement de ses règles |
| Modification de difficulté par interface ou commande | Refusée dans l'expérience, après création et rechargement |
| Social désactivé / relais inaccessible | Monde jouable et sauvegardable sans connexion |
| Autre version, seed, révision, dimension ou worldgen | Aucun mélange de messages, échos ou sessions |
| Invité : ramassage, dépôt, coffre, craft, XP, blocs, commandes | Refus côté serveur, y compris par paquets directs |
| Invité : mort, déco, crash ou révocation | Session nettoyée ; aucun transfert d'inventaire |
| Échos avec Sodium et règles avec Lithium | Compatibilité testée par version, sans conclusion générale |
| Passage à une nouvelle version du mod | Aucune réécriture silencieuse de la définition d'une ancienne expérience |

Documenter version du jeu, Loader, mod, système et mods présents pour chaque
validation. Un test réussi sous Linux ne remplace pas l'essai du client Windows.
