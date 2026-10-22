package org.example;

import org.example.entities.Categorie;
import org.example.entities.Client;
import org.example.entities.Commande;
import org.example.entities.Fournisseur;
import org.example.entities.Produit;
import org.example.entities.User;
import org.example.service.CommandeService;
import org.example.service.CrudService;
import org.example.service.MonthlySales;
import org.example.service.ProduitService;
import org.example.util.HibernateUtil;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        try {
            CrudService<Categorie, Long> categories = new CrudService<>(Categorie.class);
            CrudService<User, Long> users = new CrudService<>(User.class);
            ProduitService produits = new ProduitService();
            CommandeService commandes = new CommandeService();

            Categorie papeterie = new Categorie("Papeterie");
            categories.create(papeterie);
            Produit stylo = new Produit("Stylo", new BigDecimal("1.20"), papeterie);
            Produit carnet = new Produit("Carnet", new BigDecimal("4.50"), papeterie);
            produits.create(stylo);
            produits.create(carnet);

            users.create(new Client("Alice", LocalDate.now()));
            users.create(new Fournisseur("Fournisseur Central", "12 rue des Fleurs"));

            Commande commande = new Commande(LocalDate.now());
            commande.addProduit(stylo, 3);
            commande.addProduit(carnet, 2);
            commandes.create(commande);

            for (MonthlySales vente : commandes.rapportVentesMensuel(
                    LocalDate.now().withDayOfMonth(1), LocalDate.now().plusDays(1))) {
                System.out.println(vente.getCategorieNom() + ": " + vente.getChiffreAffaires());
            }

            EntityManager em = HibernateUtil.getSessionFactory().createEntityManager();
            try {
                System.out.println("Produits trouvés : "
                        + em.createNamedQuery("Produit.findAll", Produit.class).getResultList().size());
            } finally {
                em.close();
            }
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
