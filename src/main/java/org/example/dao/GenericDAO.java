package org.example.dao;

import org.example.util.HibernateUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class GenericDAO<T, ID extends Serializable> {

    private static final EntityManagerFactory EMF = HibernateUtil.getSessionFactory();
    private final Class<T> entityClass;

    public GenericDAO(Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    }

    public T find(ID id) {
        EntityManager em = EMF.createEntityManager();
        try {
            return em.find(entityClass, id);
        } finally {
            em.close();
        }
    }

    public List<T> findAll() {
        EntityManager em = EMF.createEntityManager();
        try {
            return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void create(T entity) {
        executeWrite(em -> {
            em.persist(entity);
            return null;
        });
    }

    public T update(T entity) {
        final Object[] result = new Object[1];
        executeWrite(em -> result[0] = em.merge(entity));
        return entityClass.cast(result[0]);
    }

    public void delete(T entity) {
        executeWrite(em -> {
            em.remove(em.contains(entity) ? entity : em.merge(entity));
            return null;
        });
    }

    protected <R> R read(EntityManagerWork<R> work) {
        EntityManager em = EMF.createEntityManager();
        try {
            return work.execute(em);
        } finally {
            em.close();
        }
    }

    protected void write(EntityManagerWork<?> work) {
        executeWrite(work);
    }

    private void executeWrite(EntityManagerWork<?> work) {
        EntityManager em = EMF.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            work.execute(em);
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

    @FunctionalInterface
    protected interface EntityManagerWork<R> {
        R execute(EntityManager em);
    }
}
