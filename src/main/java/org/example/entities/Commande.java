package org.example.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "commandes")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_commande", nullable = false)
    private LocalDate dateCommande;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommandeProduit> lignes = new ArrayList<>();

    public Commande() {
        this(LocalDate.now());
    }

    public Commande(LocalDate dateCommande) {
        if (dateCommande == null) {
            throw new IllegalArgumentException("La date de commande est obligatoire.");
        }
        this.dateCommande = dateCommande;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDate dateCommande) {
        if (dateCommande == null) {
            throw new IllegalArgumentException("La date de commande est obligatoire.");
        }
        this.dateCommande = dateCommande;
    }

    public List<CommandeProduit> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    public void addProduit(Produit produit, int quantite) {
        if (produit == null || quantite <= 0) {
            throw new IllegalArgumentException("Un produit et une quantite positive sont requis.");
        }
        boolean dejaPresent = lignes.stream().anyMatch(ligne -> ligne.getProduit() == produit);
        if (dejaPresent) {
            throw new IllegalArgumentException("Un produit ne peut apparaitre qu'une fois par commande.");
        }
        lignes.add(new CommandeProduit(this, produit, quantite));
    }

    public void removeLigne(CommandeProduit ligne) {
        if (lignes.remove(ligne)) {
            ligne.setCommande(null);
        }
    }
}
