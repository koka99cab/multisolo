# 0004 — Vrais panneaux temporaires et persistance par les votes

Statut : accepté, 12 octobre 2026.

À la demande de koka99cab, remplacer le support vierge posé manuellement chez
le destinataire par une copie temporaire du panneau vanilla publié. Elle garde
type, orientation, coordonnées et face. Elle occupe uniquement de l'air sans
fluide sur un support valide ; aucune construction n'est remplacée. Les chunks
voisins doivent déjà être chargés. L'eau n'est jamais copiée.

Le culling utilise le frustum natif : volume élargi hors champ pendant 20 ticks,
à huit blocs minimum, sans écran ouvert ni frustum de débogage capturé. Il ne
détecte pas l'occlusion par les murs. Les autorisations sont révocables et vérifiées
sur le serveur intégré juste avant les mutations. Les copies visibles restent
en place jusqu'à pouvoir être retirées hors champ. Les copies sauvegardées déjà
expirées sont nettoyées avant l'envoi de leur chunk au client.

Un marqueur sauvegardé distingue ces copies des panneaux du joueur et conserve
l'air d'origine. Les copies sont cirées, consultables et sans loot, y compris via
les explosions. Les constructions de l'auteur ne sont jamais supprimées par
l'expiration. Une copie cassée localement n'est pas replacée pendant la session.

La base de sept jours devient une durée totale de `7 + upvotes - downvotes` jours,
bornée de 1 à 30. Cela prolonge les indications appréciées et écourte les autres.
L'échéance part toujours de la publication initiale. Un vote peut la faire passer
dans le passé ; les messages expirés ne reçoivent plus de votes. Les lectures,
reprises et retries ne renouvellent pas la durée. Les fichiers restent archivés
sur le relais, sans site de consultation à ce stade. Les archives comptent dans
le plafond de stockage existant ; aucun purgeur silencieux n'est ajouté.

Le protocole reste v2 : le placement est facultatif pour les archives existantes.
Les anciens messages sans placement restent consultables, mais ne créent pas de
bloc d'un type supposé. Aucune révision d'expérience ni génération n'est migrée.
Les votes restent une voix par identité Ed25519, recréable et non liée à un compte
Minecraft vérifié. Le catalogue fermé reste le seul système de modération choisi.
