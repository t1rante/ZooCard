package br.unesp.zoocard.backend.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "animais")
public class Animal {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

    private String nome;

    @Column(length = 700)
    private String resumo;

    private float taxaExtincao;

    @Column(name = "wikipedia_url", length = 500)
    private String wikipediaUrl;

    @OneToMany
    (mappedBy="animal", cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    private List<Carta> cartas = new ArrayList<Carta>();

    public Animal() {
    }

    public Animal(Long id, String nome, String resumo, float taxaExtincao) {
        this.id = id;
        this.nome = nome;
        this.resumo = resumo;
        this.taxaExtincao = taxaExtincao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }

    public float getTaxaExtincao() {
        return taxaExtincao;
    }

    public void setTaxaExtincao(float taxaExtincao) {
        this.taxaExtincao = taxaExtincao;
    }

    public String getWikipediaUrl() {
        return wikipediaUrl;
    }

    public void setWikipediaUrl(String wikipediaUrl) {
        this.wikipediaUrl = wikipediaUrl;
    }

    public List<Carta> getCartas() {
        return cartas;
    }

    public void setCartas(List<Carta> cartas) {
        this.cartas = cartas;
    }



    
}
