package org.example.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "categories")
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @OneToMany(mappedBy = "categorie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Produit> produits = new ArrayList<>();

    public Categorie() {
    }

    public Categorie(String nom) {
        setNom(nom);
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de categorie est obligatoire.");
        }
        this.nom = nom.trim();
    }

    public List<Produit> getProduits() {
        return Collections.unmodifiableList(produits);
    }

    public void addProduit(Produit produit) {
        if (produit == null) {
            throw new IllegalArgumentException("Le produit est obligatoire.");
        }
        if (produit.getCategorie() != null && produit.getCategorie() != this) {
            produit.getCategorie().removeProduit(produit);
        }
        if (!produits.contains(produit)) {
            produits.add(produit);
            produit.setCategorie(this);
        }
    }

    public void removeProduit(Produit produit) {
        if (produits.remove(produit)) {
            produit.setCategorie(null);
        }
    }

    @Override
    public String toString() {
        return "Categorie{id=" + id + ", nom='" + nom + "'}";
    }
}
