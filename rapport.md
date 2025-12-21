# RAPPORT TECHNIQUE DÉTAILLÉ

## CONCEPTION ET DÉVELOPPEMENT D'UN SIMULATEUR DE MICROPROCESSEUR (MOTOROLA 6809)

Module : Architecture des Ordinateurs & Programmation Système  
Département : Informatique — Filière : Licence Génie Informatique

Année Universitaire : 2025 – 2026

Auteurs :  
- AKIL ILYASS (Groupe 1)  
- LAFDAIGUI YOUNES (Groupe 2)

Date : 21 Décembre 2025  
Professeur : Hicham Benalla

---

## TABLE DES MATIÈRES DÉTAILLÉE

- INTRODUCTION GÉNÉRALE  
  - 1.1. Contexte du projet et choix du processeur  
  - 1.2. Problématique : De la théorie à la pratique  
  - 1.3. Objectifs du livrable (Sim6809 Studio)

- FONDEMENTS THÉORIQUES  
  - 2.1. L'Architecture de Von Neumann  
  - 2.2. Le cycle d'instruction (Fetch-Decode-Execute)  
  - 2.3. Spécificités du Motorola 6809 (CISC, Piles, Big Endian)

- ENVIRONNEMENT DE DÉVELOPPEMENT  
  - 3.1. Langage Java : Gestion de la mémoire et Typage  
  - 3.2. Apache Maven : Cycle de vie du projet  
  - 3.3. JavaFX : Architecture du Graphe de Scène

- ARCHITECTURE LOGICIELLE (DESIGN PATTERNS)  
  - 4.1. Le modèle MVC (Modèle-Vue-Contrôleur)  
  - 4.2. Le pattern Observer pour la mémoire  
  - 4.3. Diagramme de composants

- ANALYSE DÉTAILLÉE DU MODÈLE (com.sim6809.model)  
  - 5.1. Classe Bus : Abstraction de la carte mère  
  - 5.2. Classe Cpu6809 : L'Unité Centrale de Traitement  
  - 5.3. Classe Asm : Algorithmique de compilation à deux passes

- ANALYSE DÉTAILLÉE DE L'INTERFACE (com.sim6809.ui)  
  - 6.1. Classe MainApp : Le chef d'orchestre  
  - 6.2. Moteur de rendu et AnimationTimer  
  - 6.3. Ergonomie et Design "Ultimate UI"

- DÉFIS TECHNIQUES ET SOLUTIONS  
  - 7.1. La problématique des entiers signés en Java  
  - 7.2. Simulation des interruptions matérielles (IRQ/FIRQ)  
  - 7.3. Gestion du temps et des cycles d'horloge

- GUIDE D'UTILISATION ET VALIDATION  
  - 8.1. ALGORITHME DE COMPILATION (Asm.java)  
    - 8.1.2. L'Algorithme "Two-Pass"  
  - 8.2. GUIDE D'UTILISATION (INTERFACE GRAPHIQUE)  
  - 8.3. DOCUMENTATION TECHNIQUE INTERNE  
    - 8.3.1. Le Processeur (Cpu6809)  
    - 8.3.2. La Mémoire (Bus)  
  - 8.4. TABLEAU DE CORRESPONDANCE : DU MATÉRIEL AU LOGICIEL

- CONCLUSION ET PERSPECTIVES

---

## 1. INTRODUCTION GÉNÉRALE

### 1.1. Contexte du projet et choix du processeur
L'enseignement de l'architecture des ordinateurs repose souvent sur des concepts abstraits : registres, bus, signaux de contrôle. Le projet Sim6809 Studio vise à concrétiser ces notions en développant un simulateur pédagogique du Motorola 6809. Lancé en 1978, le 6809 est considéré comme l'un des processeurs 8-bits les plus puissants et élégants jamais conçus. Son jeu d'instructions orthogonal en fait un excellent support pédagogique.

### 1.2. Problématique
Comment simuler fidèlement, dans un langage de haut niveau orienté objet (Java), le comportement électrique et séquentiel d'un composant électronique des années 80, tout en offrant une visualisation interactive ? Le défi réside dans la traduction de signaux binaires en structures de données logicielles, et dans la synchronisation d'un temps d'exécution virtuel avec le temps réel de l'utilisateur.

### 1.3. Objectifs du livrable
Le logiciel final doit être un IDE (Environnement de Développement Intégré) complet, capable de :
- Éditer du code source assembleur.
- Assembler ce code en langage machine (opcodes).
- Exécuter le programme en visualisant les flux de données.
- Déboguer via une exécution pas-à-pas et l'inspection de la mémoire.

---

## 2. FONDEMENTS THÉORIQUES

Avant d'aborder le code, il est nécessaire de définir les concepts que nous simulons.

### 2.1. L'Architecture de Von Neumann
Notre simulateur respecte l'architecture de Von Neumann, caractérisée par :
- Une mémoire unique contenant à la fois les instructions du programme et les données.
- Une unité de traitement (CPU) contenant une Unité Arithmétique et Logique (UAL) et des registres.
- Un mécanisme séquentiel de lecture d'instructions via un Compteur Ordinal (PC - Program Counter).

### 2.2. Le cycle d'instruction (Fetch-Decode-Execute)
Le simulateur reproduit le cycle Fetch-Decode-Execute : lecture de l'opcode, décodage, exécution et mise à jour des registres et de la mémoire.

### 2.3. Spécificités du Motorola 6809
- Architecture 8-bits / 16-bits : le bus de données est de 8 bits, mais des registres 16 bits (X, Y, U, S, PC) existent.
- Big Endian : le 6809 stocke l'octet de poids fort à l'adresse mémoire la plus petite. Le simulateur respecte cet ordre dans readWord et writeWord.
- Double pile : le 6809 offre une pile système (S) et une pile utilisateur (U), permettant de séparer le contexte d'exécution.

---

## 3. ENVIRONNEMENT DE DÉVELOPPEMENT

### 3.1. Langage Java (JDK 17)
Java a été choisi pour sa robustesse et sa portabilité.
- Modélisation objet : la POO permet de représenter physiquement les composants (Bus, CPU).
- Gestion mémoire : le Garbage Collector soulage la gestion mémoire de l'hôte, tandis que la mémoire simulée est gérée explicitement.
- Typage primitif : l'utilisation d'int (32 bits) pour simuler des registres 8 ou 16 bits nécessite une discipline de masquage binaire (& 0xFF).

### 3.2. Apache Maven
Maven automatise la production du logiciel.
- Dépendances : le pom.xml déclare JavaFX (javafx-controls, javafx-fxml) et les dépendances sont gérées automatiquement.
- Structure : respect de l'arborescence standard (src/main/java, target/classes) facilitant la compilation (mvn clean package).

### 3.3. JavaFX
JavaFX offre une architecture moderne basée sur un graphe de scène.
- Rendu vectoriel : composants redimensionnables sans perte de qualité.
- CSS : l'apparence est séparée de la logique (thème "Dark Mode", coloration syntaxique).

---

## 4. ARCHITECTURE LOGICIELLE GLOBALE

Pour garantir la maintenabilité, nous appliquons le patron MVC (Modèle-Vue-Contrôleur).

### 4.1. Séparation des Responsabilités
- Modèle (com.sim6809.model) : contient l'état du système (RAM, registres) et la logique métier.
- Vue/Contrôleur (com.sim6809.ui) : MainApp observe le modèle, l'affiche et transmet les commandes (Run, Step, Reset).

### 4.2. Communication via Observer Pattern
La classe Bus implémente un mécanisme d'observation. L'interface s'enregistre comme écouteur (addListener) et se met à jour lors des écritures mémoire pertinentes.

### 4.3. Diagramme de composants
(Description textuelle ou diagramme dans la documentation technique du dépôt.)

---

## 5. ANALYSE DÉTAILLÉE DU MODÈLE (com.sim6809.model)

### 5.1. Classe Bus : L'Interconnexion
Cette classe simule l'espace d'adressage de 64 Ko.
- Attribut : `private final int[] ram = new int[65536];` : tableau représentant les cellules mémoire (valeurs 0–255).
- Méthodes critiques :
  - `read(int addr)` : retourne la valeur à l'adresse donnée en appliquant `addr & 0xFFFF`.
  - `write(int addr, int data)` : écrit la valeur en appliquant `data & 0xFF` et notifie les listeners.
  - `readWord` / `writeWord` : gèrent l'accès 16 bits en Big Endian (poids fort à addr, poids faible à addr+1).

### 5.2. Classe Cpu6809 : Le Processeur
Le processeur est modélisé comme une machine à états finis (FSM).
- Le cycle `step()` implémente Fetch-Decode-Execute ; le décodage utilise un `switch(opcode)` routant vers des méthodes spécifiques (ex : `lda_immediate()`).
- Gestion des flags (CC) : calculs du zéro (Z), négatif (N), débordement (V), et autres se font par masquage bit à bit.
- Registres :
  - A, B : accumulateurs 8 bits (stockés en `int`).
  - X, Y, U, S, PC : registres 16 bits (stockés en `int`).
  - CC : registre de condition.
  - DP : Direct Page.
- Piles : instructions PSH/PUL supportent l'empilage multiple via un post-byte masque, itéré bit à bit.

### 5.3. Classe Asm : L'Assembleur
L'assembleur traduit l'assembleur source en code binaire injecté dans le Bus.
- Table de définition : tableau `Def[]` indexé par opcode pour le désassemblage/encodage.
- Algorithme en deux passes :
  - Passe 1 : construction de la table des symboles (détection des labels, directives ORG, calcul des tailles).
  - Passe 2 : génération du code (parsing, aliasing, encodage des modes d'adressage, calcul des offsets relatifs).

---

## 6. ANALYSE DÉTAILLÉE DE L'INTERFACE (com.sim6809.ui)

### 6.1. Classe MainApp : L'Orchestrateur
Initialise le Bus, le CPU et construit l'interface JavaFX ; gère le cycle de vie (start, stop).

### 6.2. Moteur de rendu et Multithreading
Pour ne pas bloquer l'UI thread, l'exécution CPU se fait par paquets via `AnimationTimer` :
```java
public void handle(long now) {
   if (isRunning) {
       for (int i = 0; i < 10; i++) { // Vitesse ajustable
           cpu.step();
       }
       refreshUI();
   }
}
```
Cette technique exécute un nombre d'instructions entre chaque rafraîchissement (60 Hz) pour garder l'interface réactive.

### 6.3. Ergonomie "Ultimate UI"
- Code couleur sémantique : noms de registres en bleu, valeurs en vert, flags actifs en jaune.
- Représentation binaire du CC : LEDs et bits individuels.
- Layout adaptatif : `SplitPane` pour redimensionner zones d'édition, mémoire et visualisation CPU.

---

## 7. DÉFIS TECHNIQUES ET SOLUTIONS

### 7.1. Le problème des entiers signés en Java
Défi : `byte` est signé en Java. Solution : utiliser `int` et appliquer systématiquement `& 0xFF` sur les lectures/écritures d'octet.

### 7.2. Boucles d'interruption infinies
Défi : un signal d'interruption persistant peut provoquer des traitements répétés. Solution : logique "edge-triggered" — le CPU remet la ligne d'interruption à `false` dès son acceptation.

### 7.3. Corruption de pile (FIRQ vs IRQ)
Le FIRQ ne sauvegarde que PC et CC ; l'IRQ sauvegarde tous les registres. Il est crucial de gérer le bit E (Entire) pour éviter la corruption de pile lors d'interruptions imbriquées. `serviceInterrupt` force un nettoyage approprié du bit E selon le type d'interruption.

### 7.4. Masquage binaire et arithmétique modulaire
Java n'ayant pas de types non signés, l'application systématique de masques (`& 0xFF`, `& 0xFFFF`) garantit un comportement conforme aux largeurs de bus 8/16 bits.

---

## 8. GUIDE D'UTILISATION ET VALIDATION

### 8.1. ALGORITHME DE COMPILATION (Asm.java)
Le moteur est un assembleur à deux passes qui génère du code binaire injecté dans le Bus.

#### 8.1.2. L'Algorithme "Two-Pass"
- Passe 1 : Table des symboles (Symbol Table). Lire le source, détecter `ORG`, labels, estimer la taille des instructions (`calculateSize()`).
- Passe 2 : Génération du code machine. Nettoyage des lignes, séparation mnémonique/opérandes, encodage selon le mode d'adressage (immédiat, direct, étendu, relatif). Conversion des registres pour PSH/PUL en post-byte masque.

### 8.2. GUIDE D'UTILISATION (INTERFACE GRAPHIQUE)
L'interface se divise en trois zones :
- Zone gauche — Éditeur de code et panneaux de contrôle matériel (boutons d'interruption).
- Zone centrale — Monitoring : variables Page Zéro, désassemblage autour du PC, visualisation des piles, console d'assemblage/erreurs.
- Zone droite — Visualisation CPU : UAL, registres et LEDs d'état.

Procédure standard :
- [ASSEMBLER] : assembler le code dans l'éditeur.
- [PAS À PAS] : exécuter une instruction.
- [RUN] : lancer l'exécution continue.
- [STOP] : pause.
- [RESET] : remettre le CPU (PC=$FFFE) sans effacer la mémoire.
- [CLR MEM] : effacer la RAM.
- Signaux matériels : HALT, IRQ, FIRQ, NMI.

Codes couleur :
- Bleu : noms des registres.
- Vert : valeurs dynamiques.
- Jaune : flags actifs.

### 8.3. DOCUMENTATION TECHNIQUE INTERNE

#### 8.3.1. Le Processeur (Cpu6809)
- Registres modélisés en `int`.
- Cycle `step()` reproduisant Fetch-Decode-Execute.
- Priorité d'interruptions : NMI > FIRQ > IRQ.
- Mécanisme d'acquittement : les lignes d'interruption sont remises à `false` après traitement.

#### 8.3.2. La Mémoire (Bus)
- Stockage : `int[65536]`.
- Sécurité : masques (`addr & 0xFFFF`, `data & 0xFF`).
- Réactivité : pattern Observer (MemoryListener) pour notifier la GUI sur écritures.

### 8.4. TABLEAU DE CORRESPONDANCE : DU MATÉRIEL AU LOGICIEL
- Registres 8 bits (A, B, DP, CC) : `public int` — évite les problèmes de signe.
- Registres 16 bits (X, Y, S, U, PC) : `public int`.
- Mémoire RAM (64 Ko) : `int[65536]`.
- Bus de données 8 bits : méthodes `read()` / `write()` avec masquage.
- Signaux de contrôle (IRQ, HALT) : `boolean`.
- Horloge (Quartz) : `AnimationTimer`.

### 8.5. JEU D'INSTRUCTIONS SUPPORTÉ
Liste non exhaustive des instructions implémentées dans Asm.java et Cpu6809.java :
- Chargement & stockage : LDA, LDB, LDX, LDY, LDU, LDS, STA, STB...
- Arithmétique & logique : ADDA, ADDB, SUBA, SUBB, CMPA, CMPB, INCA, INCB, DECA, DECB, ABX...
- Branchements : JMP, BRA, BEQ, BNE...
- Manipulation de piles : PSHS, PULS, PSHU, PULU (masques binaires).
- Contrôle système : NOP, RTS, RTI, ANDCC, ORCC.
- Transfert de registres : TFR, etc.

---

## 9. CONCLUSION ET PERSPECTIVES

Le projet Sim6809 Studio montre qu'il est possible de modéliser une architecture matérielle complexe via une approche logicielle rigoureuse. Nous avons réussi à :
- Créer un cycle CPU fidèle.
- Implémenter un assembleur fonctionnel.
- Visualiser concepts abstraits (flags, piles) graphiquement.

Perspectives d'évolution :
- Intégrer des périphériques E/S (ex : PIA 6821).
- Mapper une zone mémoire vers un canevas JavaFX pour graphiques/jeux.
- Implémenter le "reverse debugging" (enregistrement des états pour revenir en arrière).

Ce projet constitue une base solide pour l'enseignement de l'architecture des ordinateurs et de la programmation système.

Fin du Rapport.