# Vérifier Multisolo

## Vérifications automatisées

```sh
./gradlew build
python scripts/verify_repository.py
python -m unittest discover -s scripts/tests
python scripts/verify_distribution.py
```

Les tests Java vérifient les frontières d'expérience, les messages, les signatures,
le double consentement et les actions refusées aux invités. Les tests du relais
lancent un vrai service HTTP local avec deux identités et un stockage temporaire.
Le vérificateur contrôle les fichiers JSON, les invariants du catalogue et les
frontières d'import de `core`. Les tests Python couvrent la sélection des versions.

La CI lance aussi le bootstrap Fabric du serveur dans son répertoire de développement,
puis vérifie que l'initialiseur Multisolo a chargé le catalogue. Elle laisse
`eula=false` : le serveur s'arrête avant de démarrer un monde. Ce contrôle attrape
les erreurs de résolution du manifeste sans prétendre valider une partie en jeu.
Le vérificateur de distribution contrôle les dépendances et les classes `core`
et les quatre modules Fabric API embarqués dans le JAR réellement produit.

## Test client automatisé

```sh
./gradlew :fabric:runClientGameTest
```

Ce test ouvre un vrai client et crée une nouvelle expérience depuis le bouton
du menu. Il contrôle seed 0, difficulté verrouillée malgré un setter forcé,
partage depuis le menu du panneau vanilla posé, quatre lignes, refus de saisie
libre, couleur vanilla malgré une ancienne couleur d’auteur, teintures et
luminosité vanilla, lecture et votes par une seconde identité. Il vérifie aussi
le refus de placer un panneau dans le champ, son placement réel après un regard
ailleurs, le refus d'écraser une construction ou d'utiliser un support absent et
l'absence de loot. Il contrôle l'absence d'écriture depuis le menu de consultation,
la prolongation par upvote, l'expiration après annulation du vote et le retrait
hors champ. Une copie expirée sauvegardée doit disparaître au rechargement sans
bloquer le chunk ; le panneau de l'auteur reste intact. Les tests HTTP à horloge
contrôlée couvrent l'échéance, les votes, les retries et la reprise des archives.
Il vérifie aussi la sauvegarde/reprise de l’expérience et des métadonnées du panneau,
ainsi que la lecture des anciennes préférences sans réinitialiser le consentement ou le relais. Les captures vont dans
`fabric/build/run/clientGameTest/screenshots/`. Un GPU/contexte graphique compatible
est nécessaire. Le profil est isolé dans `build/` ; aucune sauvegarde personnelle
n'est utilisée. Les bibliothèques de test ne sont pas livrées aux joueurs.

## Essais en jeu avant déclaration de compatibilité

Appliquer les lignes v0.1 dès maintenant ; les essais invités/échos restent futurs.
Tant que les essais de distribution et de réseau distant ne sont pas réalisés,
garder `gameplay_ready` à `false` et ne pas annoncer de release complète.

| Scénario | Résultat attendu |
| --- | --- |
| Profil propre Fabric + seul JAR Multisolo | Chargement sans demander un autre mod |
| Nouvelle expérience Origine, puis rechargement | Seed, révision et difficulté persistées ; spawn vanilla |
| Monde solo ordinaire | Aucun verrouillage ni changement de ses règles |
| Modification de difficulté par interface ou commande | Refusée dans l'expérience, après création et rechargement |
| Social désactivé / relais inaccessible | Monde jouable et sauvegardable sans connexion |
| Message publié, relais arrêté puis redémarré | Même auteur, texte et coordonnées retrouvés |
| Suppression par une autre identité | Refus `403`, message conservé |
| Panneau ordinaire hors expérience | Menu et saisie vanilla, sans option fantôme |
| Vote +1, -1, 0 puis redémarrage | Une voix par identité ; vote sur soi refusé |
| Panneau reçu dans le champ, puis caméra détournée | Aucun placement visible ; copie réelle après une seconde hors champ |
| Construction, fluide ou support absent au XYZ reçu | Aucun remplacement ni placement invalide |
| Expiration et votes | Base 7 jours, score net en jours, bornes 1/30, aucune résurrection par vote |
| Copie expirée encore visible, puis caméra détournée | Retrait différé ; archive relay conservée, panneau d'auteur intact |
| Copie expirée sauvegardée puis rechargée | Nettoyage avant réception du chunk, sans blocage |
| Menu FR/EN et petite fenêtre | Boutons lisibles, brouillon conservé après erreur réseau |
| Autre version, seed, révision, dimension ou worldgen | Aucun mélange de messages, échos ou sessions |
| Invité : ramassage, dépôt, coffre, craft, XP, blocs, commandes | Refus côté serveur, y compris par paquets directs |
| Invité : mort, déco, crash ou révocation | Session nettoyée ; aucun transfert d'inventaire |
| Échos avec Sodium et règles avec Lithium | Compatibilité testée par version, sans conclusion générale |
| Passage à une nouvelle version du mod | Aucune réécriture silencieuse de la définition d'une ancienne expérience |

Documenter version du jeu, Loader, mod, système et mods présents pour chaque
validation. Un test réussi sous Linux ne remplace pas l'essai du client Windows.
