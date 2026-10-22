# Révision — Gestion commerciale avec Hibernate & JPA

Application Maven autonome couvrant le lab de synthèse : configuration Hibernate, DAO et services JPA génériques, catégories et produits, requêtes nommées et Criteria, pagination, héritage `JOINED`, commandes avec lignes d'association et clé primaire composite, transactions explicites et rapport mensuel de ventes.

## Prérequis et commandes

- JDK 11 ou plus récent
- Maven 3.6 ou plus récent

```bash
mvn clean test
mvn exec:java
```

H2 est utilisée en mémoire pour que le TP soit reproductible sans installer MySQL ou un serveur JEE. La configuration Hibernate est dans `src/main/resources/hibernate.cfg.xml`; les identifiants et l'URL de développement ne contiennent aucun secret de production.

## Modèle

- `Categorie` contient les produits et supprime ses produits orphelins.
- `Produit` possède une catégorie et un prix `BigDecimal`, et déclare des Named Queries de recherche.
- `Commande` contient des lignes `CommandeProduit`; la clé composée (`commande_id`, `produit_id`) et `@MapsId` garantissent l'unicité du produit par commande.
- `User` est abstrait et utilise `JOINED` avec les sous-types `Client` et `Fournisseur`.

Le `GenericDAO<T, ID>` utilise un `EntityManager` court par opération, avec transaction et rollback. Le service de produit applique les filtres Criteria et la pagination. Le rapport Criteria calcule le total mensuel `prix × quantité` par catégorie. L'application d'exemple crée des références et une commande.

## Tests

La suite vérifie le CRUD, les Named Queries et critères, les pages et leurs totaux, les clés composées et quantités, l'héritage polymorphique, le rapport de ventes et la suppression orpheline.
