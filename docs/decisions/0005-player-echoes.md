# 0005 — Capturer et rejouer des apparitions

Statut : accepté, 12 octobre 2026, à la demande de koka99cab.

Étendre l'alpha après les panneaux avec des traces courtes de joueurs. Il s'agit
d'enregistrements asynchrones, sans connexion à la partie source ni invité réel.
Le consentement de capture/lecture est distinct du partage de panneaux et reste
désactivé pour les nouvelles installations comme pour les anciennes préférences.

Une séquence comporte 2 à 41 échantillons espacés de 200 ms, soit huit secondes
maximum. Le client officiel attend au moins une minute entre ses publications.
Position, regard, accroupissement, course, phase du bras et catégorie d'outil sont
les seules données de capture. Aucun pseudo, skin personnel, inventaire, contenu
de bloc ou commande. Les modèles et outils génériques sont fournis par Minecraft.
Leur première représentation ne reproduit pas l'équipement exact ni la main dominante.

Les routes v2 utilisent les signatures et scopes existants. Le relais déduit
l'auteur et la date, borne le nombre de séquences et les mouvements possibles.
Les clips expirent après quinze minutes ; ils ne sont jamais archivés sur disque.
Le nettoyage périodique peut conserver la mémoire d'un expiré jusqu'à la minute
suivante ; les lectures l'excluent dès son échéance. Un redémarrage perd tous les clips.

Le rendu ajoute des états d'avatar détachés via Fabric, sans entité de jeu : aucune
collision, cible, AI, loot ou modification du monde n'est possible. Les chemins
chargés qui croisent une construction locale sont ignorés. La position et le
regard sont interpolés et l'apparition fond progressivement à l'entrée/sortie.
Un mixin limité à ce sous-type rend le corps translucide ; l'outil générique garde
son rendu vanilla. Aucun mixin ne change les avatars ordinaires.

Ajouter `fabric-rendering-v1` et sa dépendance d'accès au JAR unique plutôt que
d'imposer Fabric API au joueur. Conserver le périmètre historique v0.1 et signaler
les échos comme extension expérimentale dev.6, sans déclarer une release validée.
