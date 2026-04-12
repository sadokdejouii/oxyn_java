# Plan d'implémentation : Gestion des Sessions

## Tâches

- [x] 1. Créer la table SQL `sessions` dans la base de données oxyn
  - [x] 1.1 Ajouter le DDL `CREATE TABLE sessions` dans `oxyn.sql` avec les colonnes : id, activite, coach_nom, date_session, heure_debut, capacite, places_restantes, statut, salle_id, created_at
  - [x] 1.2 Ajouter la contrainte FK `salle_id → gymnasia(id) ON DELETE SET NULL`
  - [x] 1.3 Exécuter le script sur la base de données MySQL locale

- [x] 2. Créer l'entité `Session`
  - [x] 2.1 Créer `src/main/java/org/example/entities/Session.java` avec les champs : id, activite, coachNom, dateSession (LocalDate), heureDebut (LocalTime), capacite, placesRestantes, statut, salleId (Integer nullable), createdAt (Timestamp)
  - [x] 2.2 Ajouter constructeur par défaut, constructeur paramétré, getters et setters

- [x] 3. Créer le service `SessionService`
  - [x] 3.1 Créer `src/main/java/org/example/services/SessionService.java` implémentant `ICrud<Session>`
  - [x] 3.2 Implémenter `ajouter(Session s)` : initialiser `placesRestantes = capacite` et `statut = "OUVERTE"` avant l'INSERT
  - [x] 3.3 Implémenter `afficher()` : SELECT * FROM sessions ORDER BY date_session DESC, heure_debut ASC avec mapping complet vers Session
  - [x] 3.4 Implémenter `modifier(Session s)` : recalculer `placesRestantes = max(0, capacite - nbParticipants)` et mettre à jour le statut automatiquement
  - [x] 3.5 Implémenter `supprimer(int id)` : DELETE physique FROM sessions WHERE id = ?
  - [x] 3.6 Implémenter `compterParticipants(int sessionId)` : méthode privée SELECT COUNT(*) pour le recalcul dans modifier()

- [x] 4. Créer le fichier CSS `sessions.css`
  - [x] 4.1 Créer `src/main/resources/css/sessions.css` avec les styles : `.session-card`, `.session-card-header`, `.session-card-activite`, `.session-badge-ouverte`, `.session-badge-complete`, `.session-card-body`, `.session-card-actions`, `.session-btn-edit`, `.session-btn-delete`
  - [x] 4.2 Adapter la palette dark navy cohérente avec `salles.css` (fond #1a2340, accents bleus, badges verts/rouges)

- [x] 5. Remplacer `EncadrantPlanning.fxml` par la vue Card View Sessions
  - [x] 5.1 Réécrire `src/main/resources/FXML/pages/EncadrantPlanning.fxml` : remplacer le TableView par un StackPane racine contenant ScrollPane (page principale avec FlowPane#sessionsGrid) + StackPane#dialogOverlay (dialog de création/modification)
  - [x] 5.2 Changer le `fx:controller` vers `org.example.controllers.SessionManagementController`
  - [x] 5.3 Ajouter le formulaire dialog avec : ComboBox#fieldActivite (activités prédéfinies), DatePicker#fieldDate, TextField#fieldHeure, TextField#fieldCapacite, ComboBox#fieldSalle (optionnel)
  - [x] 5.4 Ajouter le chargement de `sessions.css` (ou vérifier qu'il est chargé via MainLayout.fxml)

- [x] 6. Créer le controller `SessionManagementController`
  - [x] 6.1 Créer `src/main/java/org/example/controllers/SessionManagementController.java` implémentant `Initializable`
  - [x] 6.2 Implémenter `initialize()` : charger les salles dans le ComboBox, appeler `loadSessions()`
  - [x] 6.3 Implémenter `loadSessions()` : vider le FlowPane, appeler `service.afficher()`, construire les cartes via `buildCard()`
  - [x] 6.4 Implémenter `buildCard(Session s)` : construire la VBox de la carte avec header (activité + badge statut), body (coach, date, heure, capacité/places, salle), actions (Modifier, Supprimer)
  - [x] 6.5 Implémenter `validateForm()` : vérifier activité non vide, date non nulle, heure au format HH:mm, capacité entier > 0
  - [x] 6.6 Implémenter `handleAjouter()`, `handleDialogSave()`, `handleDialogCancel()`, `handleRefresh()`
  - [x] 6.7 Implémenter `openEditDialog(Session s)` : pré-remplir les champs du formulaire avec les valeurs de la session
  - [x] 6.8 Implémenter `handleSupprimer(Session s)` : afficher une Alert de confirmation avant suppression
  - [x] 6.9 Initialiser `coachNom` depuis `SessionContext.getInstance().getDisplayName()` lors de la création

- [x] 7. Mettre à jour `MainLayoutController` pour pointer vers le nouveau module
  - [x] 7.1 Ajouter la constante `PAGE_ENC_SESSIONS = "/FXML/pages/EncadrantPlanning.fxml"` (ou vérifier que `PAGE_ENC_PLANNING` pointe déjà vers ce fichier)
  - [x] 7.2 Vérifier que `handleEncPlanning()` navigue bien vers `PAGE_ENC_PLANNING` avec le titre "Sessions" (mettre à jour le titre si nécessaire)

- [x] 8. Ajouter le DDL de la table sessions dans oxyn.sql
  - [x] 8.1 Insérer le bloc CREATE TABLE sessions après la table gymnasia dans `oxyn.sql`
  - [x] 8.2 Ajouter quelques données de test INSERT INTO sessions pour faciliter le développement
