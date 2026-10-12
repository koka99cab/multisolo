# Protocole des messages v1

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
| `PUT /v1/messages/{uuid}` | Publier ; même UUID et contenu = même message, sans doublon |
| `GET /v1/messages?scope=…&x=…&y=…&z=…` | Lire jusqu'à 50 messages à 128 blocs |
| `DELETE /v1/messages/{uuid}` | Supprimer un message dont on possède l'identité auteur |

Corps PUT : `{ "scope": CompatibilityKey, "position": {"x":0,"y":64,"z":0},
"text":"…" }`. Le scope contient `experience` (`id`, `revision`, `seed`,
`difficulty`, `worldgenMods`), `minecraftVersion`, `protocolVersion`, `dimension`.
Dans ce protocole Java, `seed` est un entier JSON long lu par Java des deux côtés ;
dans le catalogue interopérable du dépôt, elle reste une chaîne. Pour la v0.1,
la seule valeur acceptée est zéro.

Le paramètre `scope` est ce JSON en UTF-8, encodé Base64 URL sans padding.
Réponse PUT : `LocatedMessage` (`id`, `authorId`, `scope`, `position`, `text`,
`createdAt`). Réponse GET : `{"messages":[…]}`. `createdAt` est l'heure du relais
en millisecondes Unix. Les objets manquants ou invalides sont refusés.

Hors `/health`, les requêtes portent `X-Multisolo-Key` (clé X.509 en Base64),
`X-Multisolo-Time` (millisecondes Unix) et `X-Multisolo-Signature` (Base64 Ed25519).
Les octets signés sont l'UTF-8 de ces cinq lignes, sans saut final :

```text
multisolo-v1
METHOD
/raw/path?raw=query
timestamp
sha256hex(body)
```

La fenêtre temporelle est ±5 minutes. TLS protège aussi contre la capture et
le rejeu ; la fenêtre n'est pas une protection anti-rejeu exhaustive. PUT est
idempotent tant que le message existe. Les redirections HTTP ne sont pas suivies.

## Isolation et limites

Le relais v1 accepte l'origine révision 2, seed 0, normale, sans worldgen mod,
les trois dimensions vanilla et des identifiants Java 26.x. Accepter ces
identifiants ne signifie pas que le mod est compatible avec ces versions.
Les échanges exigent l'égalité de tous les champs du scope.

- 8 Kio par requête, 128 Kio par réponse client, 280 points de code par message.
- 120 requêtes et 10 mutations par minute par identité.
- 10 000 messages maximum sur le relais ; `507` lorsque le stockage est plein.
- Aucun effacement automatique ni TTL ; suppression explicite de l'auteur ou
  de l'opérateur du relais. Un redémarrage recharge les fichiers persistés.
- `400` invalide, `401` signature/heure, `403` autre auteur, `409` conflit UUID,
  `413` corps trop grand, `429` limite, `500` erreur disque/service.

Une identité peut être recréée : ces limites ne suffisent pas contre un attaquant
distribué. Le service doit rester en essais contrôlés jusqu'à ajout de modération
et protections globales. Ne pas journaliser les clés privées, signatures ou textes.
