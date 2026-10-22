package org.example;

import org.example.dao.GenericDAO;
import org.example.entities.Categorie;
import org.example.entities.Client;
import org.example.entities.Commande;
import org.example.entities.CommandeProduit;
import org.example.entities.Fournisseur;
import org.example.entities.Produit;
import org.example.entities.User;
import org.example.service.CommandeService;
import org.example.service.MonthlySales;
import org.example.service.ProduitService;
import org.example.util.HibernateUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommercialManagementTest {

    private EntityManager em;

    @AfterClass
    public static void shutdownFactory() {
        HibernateUtil.shutdown();
    }

    @Before
    public void cleanDatabase() {
        em = HibernateUtil.getSessionFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM CommandeProduit").executeUpdate();
            em.createQuery("DELETE FROM Commande").executeUpdate();
            em.createQuery("DELETE FROM Produit").executeUpdate();
            em.createQuery("DELETE FROM Categorie").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @After
    public void closeEntityManager() {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    @Test
    public void genericCrudAndNamedQueriesKeepCategoryAccessible() {
        Categorie categorie = new Categorie("Papeterie");
        GenericDAO<Categorie, Long> categoryDao = new GenericDAO<>(Categorie.class);
        categoryDao.create(categorie);

        ProduitService products = new ProduitService();
        Produit stylo = new Produit("Stylo bleu", new BigDecimal("1.20"), categorie);
        products.create(stylo);

        assertNotNull(categoryDao.find(categorie.getId()));
        assertEquals(1, products.findByCategorie(categorie.getId()).size());
        assertEquals("Papeterie", products.findByCategorie(categorie.getId()).get(0)
                .getCategorie().getNom());
        assertEquals(1, products.searchByLibelle("BLEU").size());

        Produit miseAJour = products.searchByLibelle("stylo").get(0);
        miseAJour.setPrix(new BigDecimal("1.50"));
        products.update(miseAJour);
        assertEquals(0, new ProduitService().search("Stylo", new BigDecimal("2.00"), null, 1, 10)
                .getItems().size());
    }

    @Test
    public void criteriaSearchAndPaginationReturnConsistentTotals() {
        Categorie categorie = new Categorie("Informatique");
        GenericDAO<Categorie, Long> categoryDao = new GenericDAO<>(Categorie.class);
        categoryDao.create(categorie);
        ProduitService products = new ProduitService();
        products.create(new Produit("Clavier", new BigDecimal("49.99"), categorie));
        products.create(new Produit("Clavier compact", new BigDecimal("69.99"), categorie));
        products.create(new Produit("Souris", new BigDecimal("29.99"), categorie));

        var first = products.search("CLAVIER", new BigDecimal("40.00"), new BigDecimal("60.00"), 1, 1);
        assertEquals(1, first.getTotalItems());
        assertEquals(1, first.getTotalPages());
        assertEquals("Clavier", first.getItems().get(0).getLibelle());
        assertEquals(3, products.search(null, null, null, 1, 2).getTotalItems());

        try {
            products.search(null, new BigDecimal("90"), new BigDecimal("10"), 1, 10);
            fail("Une plage de prix inversée doit etre refusee.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("prix"));
        }
    }

    @Test
    public void joinedInheritanceLoadsConcreteUsersPolymorphically() {
        GenericDAO<User, Long> users = new GenericDAO<>(User.class);
        users.create(new Client("Alice", LocalDate.of(2026, 1, 10)));
        users.create(new Fournisseur("Acme", "12 rue des Fleurs"));

        List<User> result = users.findAll();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(user -> user instanceof Client));
        assertTrue(result.stream().anyMatch(user -> user instanceof Fournisseur));
    }

    @Test
    public void compositeOrderLinesAndMonthlySalesAreCorrect() {
        Categorie papeterie = new Categorie("Papeterie");
        new GenericDAO<Categorie, Long>(Categorie.class).create(papeterie);
        Produit stylo = new Produit("Stylo", new BigDecimal("1.20"), papeterie);
        Produit carnet = new Produit("Carnet", new BigDecimal("4.50"), papeterie);
        new ProduitService().create(stylo);
        new ProduitService().create(carnet);

        Commande commande = new Commande(LocalDate.of(2026, 10, 5));
        commande.addProduit(stylo, 3);
        commande.addProduit(carnet, 2);
        new CommandeService().create(commande);

        List<MonthlySales> report = new CommandeService().rapportVentesMensuel(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
        assertEquals(1, report.size());
        assertEquals(0, new BigDecimal("12.60").compareTo(report.get(0).getChiffreAffaires()));

        em = HibernateUtil.getSessionFactory().createEntityManager();
        Commande stored = em.find(Commande.class, commande.getId());
        assertEquals(2, stored.getLignes().size());
        assertTrue(stored.getLignes().stream().allMatch(ligne ->
                ligne.getId().getCommandeId().equals(commande.getId())
                        && ligne.getId().getProduitId().equals(ligne.getProduit().getId())));
        assertEquals(5, stored.getLignes().stream().mapToInt(CommandeProduit::getQuantite).sum());
    }

    @Test
    public void removingProductFromCategoryDeletesOrphan() {
        Categorie categorie = new Categorie("Électronique");
        GenericDAO<Categorie, Long> categories = new GenericDAO<>(Categorie.class);
        categories.create(categorie);
        Produit produit = new Produit("Casque", new BigDecimal("25.00"), categorie);
        new ProduitService().create(produit);

        em = HibernateUtil.getSessionFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Categorie managed = em.find(Categorie.class, categorie.getId());
            managed.removeProduit(managed.getProduits().get(0));
            tx.commit();
            assertEquals(0L, ((Number) em.createQuery("SELECT COUNT(p) FROM Produit p")
                    .getSingleResult()).longValue());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
    }
}
