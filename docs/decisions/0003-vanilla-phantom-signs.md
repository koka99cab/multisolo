# 0003 — Partage depuis les panneaux vanilla

Statut : accepté, 12 octobre 2026.

Le chat est abandonné. Le joueur pose un vrai panneau vanilla et sélectionne
le mode fantôme dans son menu habituel, uniquement dans Multisolo. Il compose
quatre lignes dans un catalogue fermé. Le relais refuse toute ligne inconnue,
les listes de taille différente et les panneaux totalement vides. Le choix de
couleur Multisolo est retiré pour le moment : le texte utilise la couleur
vanilla du panneau local. L'ancien champ RGB est conservé dans le protocole v2
pour lire les données existantes, mais il est ignoré au rendu. Les préférences
existantes gardent leur relais et leur consentement ; seule la couleur disparaît.

Les autres sauvegardes ne reçoivent aucun nouveau bloc. Le rendu utilise un
panneau existant vierge au même XYZ/face, sans changer son texte ni son état
sauvegardé. La consultation ouvre le menu vanilla en lecture seule. Le menu
Multisolo proche reste accessible pour retrouver les publications sans panneau.

Les votes +1/-1/0 sont manuels, réversibles et limités à une voix par identité,
sans vote sur ses propres publications. Ils ne déclenchent aucune sanction ni
suppression. Pas de signalement Mojang ou Multisolo : la modération du contenu
repose ici sur les choix d'écriture prédéfinis. Une identité Ed25519 peut être
recréée ; les votes ne prouvent donc pas un nombre de comptes Minecraft distincts.

Les événements Fabric ne donnent pas accès aux quatre lignes privées du menu,
à la suppression de son paquet d'édition, à l'extraction du texte du panneau ou
à ses données sauvegardées. Des mixins ciblés couvrent ces points. L'interception
du clic ouvre uniquement une consultation sur un vrai panneau, mains vides et
en étant accroupi. Elle n'ajoute pas de dépendance joueur.

Le protocole passe à v2 avec un nouveau répertoire de données. Les anciens
messages libres ne sont ni convertis ni supprimés. La révision de l'expérience,
sa seed et ses règles de génération restent inchangées.
