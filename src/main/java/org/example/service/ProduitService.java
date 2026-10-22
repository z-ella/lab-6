package org.example.service;

import org.example.dao.GenericDAO;
import org.example.entities.Categorie;
import org.example.entities.Produit;
import org.example.util.PageResult;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProduitService extends GenericDAO<Produit, Long> {

    public ProduitService() {
        super(Produit.class);
    }

    @Override
    public void create(Produit produit) {
        Objects.requireNonNull(produit, "produit");
        write(em -> {
            Long categorieId = Objects.requireNonNull(produit.getCategorie().getId(),
                    "La categorie doit etre persistée avant ses produits.");
            produit.setCategorie(em.getReference(Categorie.class, categorieId));
            em.persist(produit);
            return null;
        });
    }

    public List<Produit> findByCategorie(Long categorieId) {
        Objects.requireNonNull(categorieId, "categorieId");
        return read(em -> em.createNamedQuery("Produit.findByCategorie", Produit.class)
                .setParameter("catId", categorieId)
                .getResultList());
    }

    public List<Produit> searchByLibelle(String terme) {
        if (terme == null || terme.trim().isEmpty()) {
            return findAllWithCategorie();
        }
        return read(em -> em.createNamedQuery("Produit.searchByLibelle", Produit.class)
                .setParameter("term", terme.trim())
                .getResultList());
    }

    public List<Produit> findAllWithCategorie() {
        return read(em -> em.createQuery("SELECT p FROM Produit p JOIN FETCH p.categorie ORDER BY p.id",
                Produit.class).getResultList());
    }

    public PageResult<Produit> search(String terme, BigDecimal prixMin, BigDecimal prixMax, int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("La page et la taille doivent etre strictement positives.");
        }
        int offset;
        try {
            offset = Math.multiplyExact(page - 1, size);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("La page depasse la limite supportee.", e);
        }
        if ((prixMin != null && prixMin.signum() < 0)
                || (prixMax != null && prixMax.signum() < 0)
                || (prixMin != null && prixMax != null && prixMin.compareTo(prixMax) > 0)) {
            throw new IllegalArgumentException("La plage de prix est invalide.");
        }

        return read(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Produit> select = cb.createQuery(Produit.class);
            Root<Produit> root = select.from(Produit.class);
            List<Predicate> predicates = predicates(cb, root, terme, prixMin, prixMax);
            select.select(root).where(predicates.toArray(new Predicate[0]))
                    .orderBy(cb.asc(root.get("libelle")), cb.asc(root.get("id")));
            TypedQuery<Produit> query = em.createQuery(select)
                    .setFirstResult(offset).setMaxResults(size);
            List<Produit> items = query.getResultList();

            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<Produit> countRoot = countQuery.from(Produit.class);
            countQuery.select(cb.count(countRoot))
                    .where(predicates(cb, countRoot, terme, prixMin, prixMax).toArray(new Predicate[0]));
            long total = em.createQuery(countQuery).getSingleResult();
            return new PageResult<>(items, page, size, total);
        });
    }

    private List<Predicate> predicates(CriteriaBuilder cb, Root<Produit> root, String terme,
                                       BigDecimal prixMin, BigDecimal prixMax) {
        List<Predicate> predicates = new ArrayList<>();
        if (terme != null && !terme.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.<String>get("libelle")),
                    "%" + terme.trim().toLowerCase(Locale.ROOT) + "%"));
        }
        if (prixMin != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("prix"), prixMin));
        }
        if (prixMax != null) {
            predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("prix"), prixMax));
        }
        return predicates;
    }
}
