# Héberger le relais

Le relais est un service Java 25 indépendant du jeu. Il fonctionne sans serveur
Minecraft et ne fait pas partie du JAR joueur. Aucun VPS n'est nécessaire pour
les premiers essais. Le futur VPS OVH et son domaine restent à choisir.

## Essai local

```sh
./gradlew :relay:run
```

Sous Windows : `.\gradlew.bat :relay:run`. Ouvrir le menu Multisolo et choisir
`http://127.0.0.1:8787`, puis activer les messages. Chaque joueur crée sa propre
expérience Connected Solitude. Le relais doit être accessible depuis chaque
machine ; `127.0.0.1` désigne uniquement la machine locale.

| Variable | Défaut |
| --- | --- |
| `MULTISOLO_BIND` | `127.0.0.1` |
| `MULTISOLO_PORT` | `8787` |
| `MULTISOLO_DATA` | `data/messages-v2`, relatif au répertoire de lancement |

`GET /health` doit renvoyer `{"status":"ok","protocol":2}`.
`:relay:installDist` produit aussi des scripts de lancement et les bibliothèques
dans `relay/build/install/relay/`, sans nécessiter Gradle sur le serveur final.

## Préparation VPS / Docker

À la racine, `compose.yaml` construit le [Dockerfile](../relay/Dockerfile), expose
le relais uniquement sur le localhost du VPS et conserve `/data` dans un volume.

```sh
docker compose up -d --build
curl http://127.0.0.1:8787/health
```

Le conteneur fonctionne sans root. Le volume doit rester accessible à l'UID
10001. Un seul processus doit utiliser ce stockage : cette alpha n'implémente
ni verrou multi-processus ni réplication.

Pour connecter des joueurs distants, placer devant ce port un reverse proxy
avec un certificat HTTPS valide, un domaine dédié et une limite des requêtes
avant le service Java. Conserver la limite des corps à 8 Kio et un délai de
lecture court. Exemple de route Caddy à adapter à un domaine réellement possédé :

```caddyfile
relay.example.com {
    request_body {
        max_size 8KB
    }
    reverse_proxy 127.0.0.1:8787
}
```

Le client acceptera alors `https://relay.example.com`. Le port Java ne doit pas
être exposé directement à Internet. Le Dockerfile et Compose sont préparés ;
leur exécution sur un VPS n'a pas encore été vérifiée.

## Persistance et exploitation

Chaque message est écrit avant la réponse HTTP, par remplacement atomique du
fichier. Il survit à l'arrêt et au redémarrage du processus. Cela ne remplace pas
les sauvegardes du disque. Avant une copie de sauvegarde, arrêter le relais,
copier tout son répertoire de données/volume, puis redémarrer. Tester une
restauration dans un autre répertoire. Ne pas lancer deux instances sur ce volume.

Les messages sont publics **aux utilisateurs du relais et du scope compatible**.
L'opérateur voit leurs identifiants de phrases, votes, identité publique, coordonnées et l'adresse IP des
connexions. La clé privée ne quitte jamais le poste du joueur. Le relais local
ne collecte pas de pseudo Microsoft ni de position réelle.

La modération du contenu repose sur le catalogue fermé, validé côté relais.
Aucun signalement ni envoi Mojang. Les votes ajustent la durée de présence :
`7 + upvotes - downvotes` jours depuis publication, entre 1 et 30 jours.
Les messages expirés quittent les lectures proches et ne sont plus votables,
mais leurs fichiers et votes restent archivés. Les nouvelles tentatives du même
UUID et les redémarrages ne renouvellent pas la durée. Aucun site d'archives
n'est encore fourni. La limite de 10 000 messages inclut les archives : leur
conservation nécessite donc de surveiller le stockage avant une ouverture publique.
Les limites par identité ne bloquent pas la création de nouvelles clés ; les
essais de charge et protections globales restent à évaluer avant ouverture.
Voir le [protocole](PROTOCOL.md).

Les apparitions utilisent les routes `/v2/echoes`, avec leur consentement séparé.
Leur stockage est uniquement en mémoire : expiration après quinze minutes,
purge périodique chaque minute, 2 000 clips au plus et trois par identité/scope.
Leur contenu n'est jamais ajouté au répertoire des panneaux ni aux sauvegardes
disque. Un redémarrage les perd. Les plafonds HTTP et les quotas d'identité sont
partagés avec les panneaux. Le mod officiel attend une minute entre publications.

Le protocole v2 utilise un nouveau sous-répertoire `messages-v2`. Garder les
anciennes données v1 à part : elles ne sont ni importées ni effacées. Si
`MULTISOLO_DATA` était défini manuellement, choisir explicitement un répertoire
v2 neuf avant de lancer ce build.
