# Financial-Project

Contexte :
Création d'API Java Spring capable de traiter des données financières et générer des graphiques.
l'API recevra des données de marché financiers, effectuera des calculs (moyenne mobile, indice de volatilité etc...) 
puis renvoyer des séries de données qui seront consommées par notre Front-end Angular.

Fonctionnalités basiques :
  1 . Récupération de Données Graphiques (GET /api/finance/charts)
  
  2 . Analyse Financière (POST /api/finance/analyze)

  3. Récupération de Données Brutes (GET /api/finance/data/{symbol})

Fonctionnalités avancées (Long terme) :
  1. Ajout de Nouvelles Sources de Données

  2. Support pour les Données Multi-Asset

  3. Notifications d'Alertes Financières

  4. Intégration avec des Portefeuilles d'Investissement

  5. Optimisation des Performances et Scalabilité

Règles Business générales :
  1. Exactitude des Données: Toutes les données financières doivent être exactes et proviennent de sources fiables. (Yahoo Finance)
     L'API doit gérer les erreurs de données et informer les utilisateurs en cas de données suspectes ou manquantes.
     
  2. Sécurité: Toutes les communications entre l'API et les clients doivent être sécurisées.
     HTTPS pour toutes les requêtes. Les données sensibles doivent être protégées via des techniques de chiffrement.
     
  3. Performance : L'API doit répondre aux requêtes dans un temps raisonnable, même en cas de forte demande.
     Techniques de mise en cache et d'optimisation des requêtes pour améliorer les performances.
     
  4. Conformité Légale: l'API doit être conforme aux réglementations financières applicables, notamment en ce qui concerne la confidentialité des données et la transparence des analyses.
     
  5. Modularité: Le code doit être modulaire pour permettre une évolution facile du projet.
     De nouvelles fonctionnalités doivent pouvoir être ajoutées sans refactorisation majeure.
