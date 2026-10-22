package org.example.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "produits")
@NamedQueries({
        @NamedQuery(name = "Produit.findAll", query = "SELECT p FROM Produit p ORDER BY p.id"),
        @NamedQuery(name = "Produit.findByCategorie",
                query = "SELECT p FROM Produit p JOIN FETCH p.categorie WHERE p.categorie.id = :catId ORDER BY p.id"),
        @NamedQuery(name = "Produit.searchByLibelle",
                query = "SELECT p FROM Produit p JOIN FETCH p.categorie "
                        + "WHERE LOWER(p.libelle) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY p.id")
})
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    public Produit() {
    }

    public Produit(String libelle, BigDecimal prix, Categorie categorie) {
        setLibelle(libelle);
        setPrix(prix);
        this.dateCreation = LocalDate.now();
        setCategorie(categorie);
    }

    public Long getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        if (libelle == null || libelle.trim().isEmpty()) {
            throw new IllegalArgumentException("Le libelle est obligatoire.");
        }
        this.libelle = libelle.trim();
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        Objects.requireNonNull(prix, "Le prix est obligatoire.");
        if (prix.signum() < 0) {
            throw new IllegalArgumentException("Le prix ne peut pas etre negatif.");
        }
        this.prix = prix;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
        if (categorie == null) {
            return;
        }
        if (!categorie.getProduits().contains(this)) {
            categorie.addProduit(this);
        }
    }

    @Override
    public String toString() {
        return "Produit{id=" + id + ", libelle='" + libelle + "', prix=" + prix + "}";
    }
}
