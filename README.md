# Financial-Project

## Contexte
Création d'une API Java Spring capable de traiter des données financières et de générer des graphiques. L'API recevra des données de marché financiers, effectuera des calculs (moyenne mobile, indice de volatilité, etc.) puis renverra des séries de données qui seront consommées par notre Front-end Angular.

## Règles Business Générales

1. **Exactitude des Données** : Toutes les données financières doivent être exactes et provenir de sources fiables (Yahoo Finance). L'API doit gérer les erreurs de données et informer les utilisateurs en cas de données suspectes ou manquantes.
2. **Sécurité** : Toutes les communications entre l'API et les clients doivent être sécurisées. Utilisation de HTTPS pour toutes les requêtes. Les données sensibles doivent être protégées via des techniques de chiffrement.
3. **Performance** : L'API doit répondre aux requêtes dans un temps raisonnable, même en cas de forte demande. Techniques de mise en cache et d'optimisation des requêtes pour améliorer les performances.
4. **Conformité Légale** : L'API doit être conforme aux réglementations financières applicables, notamment en ce qui concerne la confidentialité des données et la transparence des analyses.
5. **Modularité** : Le code doit être modulaire pour permettre une évolution facile du projet. De nouvelles fonctionnalités doivent pouvoir être ajoutées sans refactorisation majeure.

## Fonctionnalités Basiques

1. **Récupération de Données Graphiques (GET /api/finance/charts)**
   - **Description** : Récupère les données financières pour un symbole boursier (séries temporelles) donné et un intervalle de temps spécifié.
   - **Règles Business** :
     - Les données retournées doivent être exactes, à jour et provenir de sources fiables.
     - Les intervalles supportés doivent être définis (daily, weekly, etc.) et respecter les formats standards.
        
2. **Récupération de Données Brutes (GET /api/finance/data/{symbol})**
   - **Description** : Récupère les données brutes pour un symbole boursier spécifique sur une période donnée.
   - **Règles Business** :
     - Les données doivent être complètes pour la période demandée.
     - En cas de données manquantes ou incomplètes, le système doit notifier l'utilisateur.
      
3. **Recherche de Symboles Boursiers (GET /api/finance/search)**
   - **Description** : Permet de rechercher des symboles boursiers par nom de l'entreprise ou par code boursier.
   - **Règles Business** :
     - La recherche doit être rapide et capable de gérer les requêtes partielles ou incomplètes.
     - Les résultats doivent inclure le nom complet de l'entreprise, le symbole boursier, et l'échange où le symbole est listé.

4. **Consultation des Informations sur une Action (GET /api/finance/info/{symbol})**
   - **Description** : Permet de consulter les informations détaillées sur une action spécifique, comme le prix actuel, la capitalisation boursière, etc.
   - **Règles Business** :
     - Les données doivent être actualisées en temps réel ou quasi-réel.
     - En cas d'indisponibilité des données, le système doit retourner un message d'erreur clair.

5. **Analyse Financière (POST /api/finance/analyze)**
   - **Description** : Analyse les données financières pour un symbole boursier sur une période donnée en utilisant des indicateurs financiers comme la moyenne mobile (SMA), l'indice de force relative (RSI), etc.
   - **Règles Business** :
     - Les calculs doivent respecter les formules financières standard pour chaque indicateur.
     - Le système doit gérer les erreurs de calcul, comme les périodes insuffisantes pour calculer un indicateur.
     - Les données utilisées pour les analyses doivent être cohérentes avec celles utilisées pour la génération des graphiques.

6. **Comparaison de Performances (POST /api/finance/compare)**
   - **Description** : Permet de comparer les performances de plusieurs symboles boursiers sur une période donnée.
   - **Règles Business** :
     - La comparaison doit se baser sur les prix de clôture ajustés.
     - Le système doit gérer les cas où un symbole n'a pas de données pour toute la période.

7. **Création d'un Nouveau Symbole Boursier (POST /api/finance/symbols)**
   - **Description** : Permet de créer un nouveau symbole boursier dans la base de données.
   - **Règles Business** :
     - Le symbole boursier ne doit pas déjà exister dans la base de données.
     - Le nom de l'entreprise et l'échange doivent être valides.

8. **Création de Données Financières (POST /api/finance/data)**
   - **Description** : Permet de créer de nouvelles entrées de données financières dans la base de données.
   - **Règles Business** :
     - Les données financières doivent être précises et conformes aux normes.
     - Les doublons de données pour la même date et symbole doivent être évités.

9. **Mise à Jour des Informations sur un Symbole (PUT /api/finance/symbols/{symbol})**
   - **Description** : Permet de mettre à jour les informations d'un symbole boursier existant.
   - **Règles Business** :
     - Le symbole doit exister dans la base de données pour être mis à jour.
     - Les nouvelles informations doivent être valides et correctement formatées.

10. **Mise à Jour de Données Financières (PUT /api/finance/data/{id})**
    - **Description** : Permet de mettre à jour une entrée existante de données financières.
    - **Règles Business** :
      - L'entrée de données doit exister dans la base de données pour être mise à jour.
      - Les nouvelles données doivent être valides et respecter les formats standards.

11. **Suppression d'un Symbole Boursier (DELETE /api/finance/symbols/{symbol})**
    - **Description** : Permet de supprimer un symbole boursier de la base de données.
    - **Règles Business** :
      - Le symbole doit exister dans la base de données pour être supprimé.
      - La suppression doit être autorisée uniquement si le symbole n'est pas utilisé dans d'autres analyses ou historiques.

12. **Suppression de Données Financières (DELETE /api/finance/data/{id})**
    - **Description** : Permet de supprimer une entrée spécifique de données financières.
    - **Règles Business** :
      - L'entrée de données doit exister dans la base de données pour être supprimée.
      - La suppression doit être irréversible et l'utilisateur doit être informé des conséquences.

## Fonctionnalités Avancées (Long Terme)

1. Ajout de Nouvelles Sources de Données
2. Support pour les Données Multi-Asset
3. Notifications d'Alertes Financières
4. Intégration avec des Portefeuilles d'Investissement
5. Optimisation des Performances et Scalabilité

## Installation et Configuration

1. **Cloner le projet** : 
   ```bash
   git clone https://github.com/ronanleflem/Financial-Project.git
   ```

2. **Configurer la base de données** : Modifier le fichier `application.properties` avec vos propres informations PostgreSQL.

3. **Compiler et démarrer le projet** :
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
## Contribution

Les contributions sont les bienvenues ! Si vous souhaitez proposer des améliorations ou corriger des bugs, veuillez ouvrir une issue ou soumettre une pull request.

## Licence

Ce projet est sous licence MIT. Veuillez vous référer au fichier LICENSE pour plus d'informations.
