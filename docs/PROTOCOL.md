# Protocole des panneaux v2

`network` contient le contrat et le client Java, sans Minecraft. `relay` utilise
le même contrat. Gson sérialise les records Java en JSON camelCase. Les
coordonnées sont celles du **jeu** ; aucune géolocalisation réelle n'est utilisée.

## Identité et autorité

Une paire Ed25519 est créée au premier échange explicitement autorisé et
conservée dans `config/multisolo/identity.json`. L'identifiant réseau est le
SHA-256 hexadécimal des octets X.509 de la clé publique. Sauvegarder ce fichier
préserve l'identité ; le perdre crée une autre identité. Ne jamais le publier.

Le relais vérifie une signature et déduit lui-même l'auteur. Le client ne peut
pas choisir `authorId` ni la date de création. Cette identité prouve la
possession d'une clé, **pas** la propriété d'un compte Microsoft/Minecraft.
Le pseudo de jeu n'est pas utilisé comme preuve d'identité.

Le relais ne peut pas prouver la seed ou la position d'une sauvegarde distante.
Le mod officiel vérifie son monde local ; un client modifié peut mentir sur sa
position. Ne pas utiliser ces messages pour classer ou certifier une progression.

## Requêtes

| Route | Effet |
| --- | --- |
| `GET /health` | État et numéro de protocole, sans authentification |
| `PUT /v2/messages/{uuid}` | Publier ; même UUID et contenu = même message, sans doublon |
| `GET /v2/messages?scope=…&x=…&y=…&z=…` | Lire jusqu'à 50 messages actifs à 128 blocs |
| `PUT /v2/messages/{uuid}/vote` | Voter +1, -1 ou annuler (0) |
| `DELETE /v2/messages/{uuid}` | Supprimer un message dont on possède l'identité auteur |

Corps PUT (exemple) :

```json
{
  "scope": {"experience": {"id":"multisolo:origin","revision":2,"seed":0,"difficulty":"NORMAL","worldgenMods":{}},"minecraftVersion":"26.4-snapshot-3","protocolVersion":2,"dimension":"minecraft:overworld"},
  "position": {"x":0,"y":64,"z":0},
  "sign": {"phrase":{"lines":["BEWARE","LAVA","BUT","TREASURE"]},"color":{"rgb":0},"face":"FRONT","placement":{"block":"minecraft:oak_sign","properties":{"rotation":"0","waterlogged":"false"}}}
}
```

`position` désigne le bloc du panneau posé. `face` vaut FRONT ou BACK. Les quatre
lignes doivent être des valeurs de `MessagePhrase.Line` ; aucun texte utilisateur
n'est transmis. EMPTY permet une ligne vide, mais pas quatre lignes vides. Les
traductions sont locales au client. Le champ historique `color.rgb` reste validé
entre 0 et 16777215 pour conserver le format v2 et les publications déjà stockées.
Depuis dev.4, les nouvelles publications l'émettent à 0 et le client ignore toute
couleur reçue : le panneau utilise sa teinture et sa luminosité vanilla.
Depuis dev.5, `sign.placement` décrit le type et l'orientation. Il reste facultatif
pour lire les anciens fichiers v2 : sans lui, le message reste consultable dans
le menu proche, mais aucun bloc n'est deviné ou placé. Seuls les panneaux vanilla
des douze essences reconnues sont acceptés. Propriétés exactes : `rotation` de 0
à 15 pour les panneaux au sol et suspendus au plafond, `facing` horizontal pour
les panneaux muraux ; `attached` booléen uniquement au plafond ; `waterlogged`
doit toujours être `false`. Aucun bloc arbitraire ni eau n'est copié.
Dans ce protocole Java, la seed est un entier long ; le catalogue interopérable
du dépôt la conserve comme chaîne. La seule seed acceptée ici est zéro.

`scope` en paramètre GET est ce JSON en UTF-8 encodé Base64 URL sans padding.
Réponse PUT : `LocatedMessage` (`id`, `authorId`, `scope`, `position`, `sign`,
`createdAt`). `createdAt` est l'heure du relais en millisecondes Unix. Réponse GET :
`{"messages":[{"message":LocatedMessage,"upvotes":0,"downvotes":0,"ownVote":0}]}`.
Corps vote : `{"value":1}`, `{"value":-1}` ou `{"value":0}`. Une seule voix par
identité, modifiable et annulable ; le vote sur ses propres messages est refusé.
Les comptes et votes survivent au redémarrage. Maximum 1000 votants par message.

La durée active est `clamp(7 + upvotes - downvotes, 1, 30)` jours depuis
`createdAt` (un jour = 86 400 000 ms). À l'échéance exacte, le message est expiré :
absent des lectures proches et votes refusés avec `404`. Un vote peut avancer
l'échéance jusque dans le passé. Les expirés ne peuvent pas être réactivés par
un nouveau vote. Le même UUID publié à nouveau conserve son `createdAt` initial.
L'archive et ses votes restent sur disque ; seule une suppression explicite
les efface. Le délai est calculé depuis les comptes déjà présents dans la réponse,
sans nouveau champ JSON ni changement de numéro de protocole.

Hors `/health`, les requêtes portent `X-Multisolo-Key` (clé X.509 en Base64),
`X-Multisolo-Time` (millisecondes Unix) et `X-Multisolo-Signature` (Base64 Ed25519).
Les octets signés sont l'UTF-8 de ces cinq lignes, sans saut final :

```text
multisolo-v2
METHOD
/raw/path?raw=query
timestamp
sha256hex(body)
```

La fenêtre temporelle est ±5 minutes. TLS protège aussi contre la capture et
le rejeu ; la fenêtre n'est pas une protection anti-rejeu exhaustive. PUT est
idempotent tant que le message existe. Les redirections HTTP ne sont pas suivies.

## Isolation et limites

Le relais v2 accepte l'origine révision 2, seed 0, normale, sans worldgen mod,
les trois dimensions vanilla et des identifiants Java 26.x. Accepter ces
identifiants ne signifie pas que le mod est compatible avec ces versions.
Les échanges exigent l'égalité de tous les champs du scope.

- 8 Kio par requête, 128 Kio par réponse client, exactement quatre lignes de catalogue.
- 120 requêtes et 10 mutations par minute par identité.
- 10 000 messages maximum, archives expirées comprises ; `507` lorsque le stockage est plein.
- Expiration de la présence, sans effacement automatique des fichiers. Un
  redémarrage recharge les archives sans renouveler les échéances.
- `400` invalide, `401` signature/heure, `403` autre auteur ou vote sur soi, `404` message absent ou expiré pour un vote, `409` conflit UUID,
  `413` corps trop grand, `429` limite, `500` erreur disque/service.

Une identité peut être recréée : une voix n'est pas un compte Minecraft vérifié.
Le vocabulaire fermé constitue la modération du contenu choisie pour cette version.
Il n'empêche pas le spam ou les indications trompeuses. Aucun signalement, aucune
sanction automatique et aucun envoi à Mojang. Ne pas journaliser clés privées ou signatures.

## Stockage et passage depuis v1

Le défaut est désormais `data/messages-v2` (`/data/messages-v2` dans Docker).
Chaque fichier contient `{schemaVersion:2,message:LocatedMessage,votes:{identity:1}}`.
Les écritures sont atomiques avant acquittement. Une publication reste immuable ;
un changement de contenu crée un nouvel UUID puis retire l'ancien partage de
l'auteur. Les votes de l'ancienne publication ne sont pas transférés.

Les anciennes routes v1 sont refusées. Un répertoire contenant l'ancien format
est refusé au démarrage : conserver sa sauvegarde et choisir un nouveau répertoire
v2. Aucun texte libre historique n'est importé, réécrit ou supprimé automatiquement.
