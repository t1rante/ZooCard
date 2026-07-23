package br.unesp.zoocard.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "especie_cache")
public class EspecieCache {

    public static final long TTL_MILLIS = 30L * 24 * 60 * 60 * 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "nome_cientifico", nullable = false, unique = true)
    private String nomeCientifico;

    @Column(name = "nome_comum")
    private String nomeComum;

    @Column(name = "resumo", length = 2000)
    private String resumo;

    @Column(name = "codigo_iucn")
    private String codigoIucn;

    @Column(name = "gbif_key")
    private Integer gbifKey;

    @Column(name = "wikipedia_url", length = 500)
    private String wikipediaUrl;

    @Column(name = "atualizado_em", nullable = false)
    private Long atualizadoEm;

    public boolean estaValido() {
        return atualizadoEm != null && (System.currentTimeMillis() - atualizadoEm) < TTL_MILLIS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeCientifico() {
        return nomeCientifico;
    }

    public void setNomeCientifico(String nomeCientifico) {
        this.nomeCientifico = nomeCientifico;
    }

    public String getNomeComum() {
        return nomeComum;
    }

    public void setNomeComum(String nomeComum) {
        this.nomeComum = nomeComum;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }

    public String getCodigoIucn() {
        return codigoIucn;
    }

    public void setCodigoIucn(String codigoIucn) {
        this.codigoIucn = codigoIucn;
    }

    public Integer getGbifKey() {
        return gbifKey;
    }

    public void setGbifKey(Integer gbifKey) {
        this.gbifKey = gbifKey;
    }

    public String getWikipediaUrl() {
        return wikipediaUrl;
    }

    public void setWikipediaUrl(String wikipediaUrl) {
        this.wikipediaUrl = wikipediaUrl;
    }

    public Long getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Long atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
