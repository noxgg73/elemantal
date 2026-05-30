# Classe Absolute Solver (Murder Drones)

Classe selectionnable a l'ecran de choix. Le programme qui brise les lois de la realite :
matiere/realite manipulables, singularites, tendrilles de desassemblage, possession,
reconstruction et forme ailee.

## Controles

- **R** : ouvre le menu des pouvoirs du Solver (12 capacites).
- **Clic** sur une entree : selectionne le pouvoir (persistant).
- **Clic droit** : declenche le pouvoir selectionne (cooldown par pouvoir).
- **G** : Singularite Absolue (acces direct).
- **L** : Forme du Solver (bascule, acces direct).

## Pouvoirs (menu R)

| #  | Pouvoir                    | Effet                                                         |
|----|----------------------------|---------------------------------------------------------------|
| 1  | Membres de Desassemblage   | Tendrilles : lacerent + attirent (fort)                       |
| 2  | Acide Nanite               | Crachat cible, faibles degats + poison/wither (faible)        |
| 3  | Essaim de Nanites          | Nuage : poison/wither/lenteur en zone                         |
| 4  | Singularite Absolue        | Aspire entites + matiere ~9 s                                 |
| 5  | Telekinesie                | Souleve et projette les ennemis                               |
| 6  | Possession                 | Controle le mob le plus proche                                |
| 7  | Reconstruction de Drone    | Invoque 2 golems allies                                       |
| 8  | Manipulation de la Matiere | Mur d'amethyste du Solver                                     |
| 9  | Glitch Spatial             | Clignement-teleportation                                      |
| 10 | Symbole du Solver          | Peur/aveuglement/faiblesse, sans degats (faible/utilitaire)   |
| 11 | Reconstruction             | Soin total + purge + absorption                               |
| 12 | Forme du Solver            | Vol + regen + resistance (bascule)                            |

## Passif

Regeneration + vision nocturne en continu ; resistance (niv. 20) ; force (niv. 30).

## Textures

12 icones 16x16 (`assets/elementalpower/textures/gui/solver/power_0..11.png`) + item
« Coeur du Solver » (`solver_core`). Dessins originaux dans la palette du Solver
(magenta `#D400FF`, blanc, jaune type oeil-de-drone, embleme oeil/cercle, glitch, ailes) —
interpretations fideles a l'esthetique de la serie, pas des copies des images d'origine.

## Code principal

- `world/AbsoluteSolverManager.java` — toutes les capacites + singularites/forme ticked.
- `screen/SolverPowersScreen.java` — menu R avec icones.
- `network/SolverSelectPowerC2SPacket.java`, `network/SolverTriggerC2SPacket.java`.
- `element/PlayerElement.java` — champ `solverPower` (persistant NBT).
