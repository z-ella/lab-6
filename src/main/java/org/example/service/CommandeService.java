package org.example.service;

import org.example.dao.GenericDAO;
import org.example.entities.Categorie;
import org.example.entities.Commande;
import org.example.entities.CommandeProduit;
import org.example.entities.Produit;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class CommandeService extends GenericDAO<Commande, Long> {

    public CommandeService() {
        super(Commande.class);
    }

    @Override
    public void create(Commande commande) {
        Objects.requireNonNull(commande, "commande");
        write(em -> {
            for (var ligne : commande.getLignes()) {
                Long produitId = Objects.requireNonNull(ligne.getProduit().getId(),
                        "Les produits doivent etre persistés avant la commande.");
                ligne.setProduit(em.getReference(Produit.class, produitId));
            }
            em.persist(commande);
            return null;
        });
    }

    public List<MonthlySales> rapportVentesMensuel(LocalDate debut, LocalDate fin) {
        Objects.requireNonNull(debut, "debut");
        Objects.requireNonNull(fin, "fin");
        if (!debut.isBefore(fin)) {
            throw new IllegalArgumentException("La date de debut doit preceder la date de fin.");
        }

        return read(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<MonthlySales> query = cb.createQuery(MonthlySales.class);
            Root<CommandeProduit> ligne = query.from(CommandeProduit.class);
            Join<CommandeProduit, Commande> commande = ligne.join("commande");
            Join<CommandeProduit, Produit> produit = ligne.join("produit");
            Join<Produit, Categorie> categorie = produit.join("categorie");
            var montantLigne = cb.prod(produit.<BigDecimal>get("prix"),
                    cb.toBigDecimal(ligne.<Integer>get("quantite")));
            query.select(cb.construct(MonthlySales.class,
                            categorie.<Long>get("id"),
                            categorie.<String>get("nom"),
                            cb.sum(montantLigne)))
                    .where(cb.greaterThanOrEqualTo(commande.<LocalDate>get("dateCommande"), debut),
                            cb.lessThan(commande.<LocalDate>get("dateCommande"), fin))
                    .groupBy(categorie.get("id"), categorie.get("nom"))
                    .orderBy(cb.asc(categorie.get("nom")));
            return em.createQuery(query).getResultList();
        });
    }
}
