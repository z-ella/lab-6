package org.example.service;

import java.math.BigDecimal;

public class MonthlySales {

    private final Long categorieId;
    private final String categorieNom;
    private final BigDecimal chiffreAffaires;

    public MonthlySales(Long categorieId, String categorieNom, BigDecimal chiffreAffaires) {
        this.categorieId = categorieId;
        this.categorieNom = categorieNom;
        this.chiffreAffaires = chiffreAffaires;
    }

    public Long getCategorieId() {
        return categorieId;
    }

    public String getCategorieNom() {
        return categorieNom;
    }

    public BigDecimal getChiffreAffaires() {
        return chiffreAffaires;
    }
}
