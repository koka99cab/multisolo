# Le concept Multisolo

Multisolo, par koka99cab, relie des aventures solo sur une seed commune. Chaque
joueur garde son monde local, son inventaire et sa progression. Le spawn reste
celui de Minecraft : la proposition initiale de spawn aléatoire a été abandonnée.

## Trois formes de présence

1. **Messages** : laisser une indication à une position ; la retrouver dans les
   mondes compatibles. Prévoir signalement, masquage, expiration et limites de débit.
2. **Échos** : apercevoir une silhouette d'un autre joueur. Aucun accès à son
   inventaire, aucune collision ni interaction avec le monde.
3. **Entraide** : demander de l'aide et accepter explicitement un invité temporaire.
   L'hôte peut mettre fin à la session. Rien ne démarre sans consentement des deux côtés.

La v0.1 [Connected Solitude](releases/0.1-connected-solitude.md) implémente les
messages. Les échos et l'entraide sont prévus après cette première étape.

## Un menu social, des règles communes

Le menu v0.1 active les messages et choisit le relais ; le consentement est
persistant et désactivé par défaut. Échos et entraide auront leurs contrôles
propres plus tard. Refuser le social ne change pas la seed ou la difficulté.

Une expérience fixe une seed, une difficulté, la génération et une révision.
Tous ses participants jouent avec ces mêmes règles. Les nouvelles sauvegardes
marquées v0.1 sont créées en survie sans cheats ; le serveur intégré verrouille
la difficulté normale. Les autres sauvegardes restent des parties Minecraft ordinaires.

## L'invité aide sans s'approprier la partie

La première politique autorise déplacement, regard et combat contre les monstres
hostiles. Elle refuse ramassage, dépôt, échanges, conteneurs, fabrication, gain
d'expérience, destruction/placement de blocs, interactions avec le monde, PvP et commandes.

L'équipement temporaire et l'isolation des inventaires devront être définis avant
l'entraide. L'inventaire personnel de l'invité ne doit jamais être importé ou
exporté. Les refus seront appliqués côté serveur, y compris face aux paquets directs.
Les monstres tués peuvent produire du butin pour l'hôte : « aider » peut influencer
le combat, mais ne donne pas à l'invité un droit sur ce butin.

## Une expérience pour commencer

`multisolo:origin`, révision **2**, utilise la seed **`0`**, la difficulté
`NORMAL` et la génération vanilla. La révision 1 à seed provisoire est historique
et ne doit jamais être réécrite dans une sauvegarde existante.

Plus tard, proposer des expériences distinctes pour des ensembles précis de mods
de génération. Ne pas changer automatiquement une seed parce qu'un mod est présent.
Les mods requis par une expérience optionnelle seront annoncés ; l'expérience de
base devra toujours fonctionner avec Multisolo seul dans un profil Fabric.

Une même seed ne garantit pas la même carte entre versions du jeu, générateurs,
datapacks ou configurations. Les échanges sont donc séparés par expérience,
révision, version exacte du jeu, protocole et dimension. Le contrat devra intégrer
les datapacks et configurations de génération avant d'accepter des expériences modifiées.

## Connecter des mondes locaux

Les messages utilisent le [relais v0.1](RELAY.md). Les échos devront l'étendre.
L'entraide nécessitera une connexion
au serveur intégré de l'hôte et une solution aux routeurs/NAT. Une connexion directe,
un relais et leurs coûts devront être évalués avant de promettre « inviter en un clic ».
Ce dépôt ne prétend pas résoudre ces points avec un simple client HTTP.

Le relais vérifie une identité cryptographique et isole les groupes compatibles.
Ses premières limites ne remplacent pas une modération publique. Une panne du
service n'empêche jamais le chargement ou la sauvegarde d'un monde solo.
