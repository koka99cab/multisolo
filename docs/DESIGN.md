# Le concept Multisolo

Multisolo, par koka99cab, relie des aventures solo sur une seed commune. Chaque
joueur garde son monde local, son inventaire et sa progression. Le spawn reste
celui de Minecraft : la proposition initiale de spawn aléatoire a été abandonnée.

## Trois formes de présence

1. **Panneaux fantômes** : partager quatre lignes prédéfinies depuis un panneau
   vanilla réellement posé, avec la couleur vanilla du texte et des votes manuels. Le vocabulaire fermé
   constitue la modération du contenu ; aucun signalement ni texte libre partagé.
2. **Échos** : apercevoir une silhouette d'un autre joueur. Aucun accès à son
   inventaire, aucune collision ni interaction avec le monde.
3. **Entraide** : demander de l'aide et accepter explicitement un invité temporaire.
   L'hôte peut mettre fin à la session. Rien ne démarre sans consentement des deux côtés.

La v0.1 [Connected Solitude](releases/0.1-connected-solitude.md) implémente les
messages. Depuis dev.6, des échos expérimentaux étendent cette base à la demande
de l'auteur ; l'entraide reste ultérieure.

## Un menu social, des règles communes

Le menu v0.1 active les messages et choisit le relais ; le consentement est
persistant et désactivé par défaut. Les apparitions ont un consentement distinct
pour capturer et lire des traces. L'entraide aura son contrôle propre plus tard.
Refuser le social ne change pas la seed ou la difficulté.

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

Les messages et échos utilisent le [relais](RELAY.md), dans des stockages séparés.
L'entraide nécessitera une connexion
au serveur intégré de l'hôte et une solution aux routeurs/NAT. Une connexion directe,
un relais et leurs coûts devront être évalués avant de promettre « inviter en un clic ».
Ce dépôt ne prétend pas résoudre ces points avec un simple client HTTP.

Le relais vérifie une identité cryptographique et isole les groupes compatibles.
Le vocabulaire fermé limite le contenu possible, sans empêcher le spam ni les
indications trompeuses. Une panne du
service n'empêche jamais le chargement ou la sauvegarde d'un monde solo.

## Un panneau posé, un contenu partagé

Le menu reste celui du panneau vanilla, normal ou suspendu. L'option fantôme
existe seulement dans une expérience Multisolo compatible. Chaque face peut être
partagée séparément. Le panneau de l'auteur conserve son bloc, sa collision et
les règles vanilla ; ses métadonnées Multisolo sont sauvegardées avec le bloc.

Une copie temporaire du vrai panneau est placée dans les autres sauvegardes,
au même XYZ avec le type, l'orientation et la face publiés. L'emplacement doit
être de l'air sans fluide, avec un support valide et des chunks déjà chargés.
Aucune construction n'est remplacée. Le volume autour du panneau doit rester hors
du champ de la caméra pendant une seconde, à au moins huit blocs, pour le placement
comme pour le retrait. Le culling utilise le frustum ; il ne détecte pas un mur
qui masque un panneau situé dans le champ.

Ces copies marquées sont cirées, consultables et sans loot. Les votes utilisent
le menu vanilla en lecture seule, sans paquet d'édition. Si plusieurs messages
visent le même bloc, le score puis la publication la plus récente choisissent le
panneau ; le menu proche conserve les autres résultats. Une copie cassée localement
n'est pas replacée pendant la session.

La durée totale depuis publication est `7 + upvotes - downvotes` jours, bornée
entre 1 et 30 jours. Les lectures et les nouvelles tentatives de publication du
même UUID ne renouvellent pas l'échéance. L'expiration interdit les votes et retire
la copie hors champ ; une copie sauvegardée expirée est nettoyée au chargement du
chunk avant sa réception par le client. Le panneau d'origine reste chez son auteur.
L'archive du relais reste enregistrée, sans site de consultation pour le moment.

## Des apparitions de passages récents

Une apparition rejoue jusqu'à huit secondes enregistrées à cinq échantillons par
seconde : position, regard, accroupissement, course, bras et catégorie d'outil.
L'enregistrement actif est envoyé au plus une fois par minute après consentement.
Les pauses, téléportations et changements de monde interrompent la capture.
Les séquences expirent après quinze minutes et ne rejoignent pas les archives.

Un avatar translucide est dessiné sans entité de jeu, nom, ombre ni son. Il utilise
un skin vanilla générique stable par identité et un outil en fer représentatif.
La lecture interpole les positions et rotations, avec un fondu d'entrée/sortie.
Un clip n'est joué qu'une fois par session de consentement ; douze secondes
séparent les départs. Le départ exige une distance de 8 à 64 blocs et un chemin
chargé sans construction traversée. Les gestes de minage restent visuels : aucun
bloc distant n'est affecté. Skins personnels, armure, nage, montures et gestes
spéciaux ne sont pas reproduits dans cette première version.
