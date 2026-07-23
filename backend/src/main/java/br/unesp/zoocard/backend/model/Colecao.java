package br.unesp.zoocard.backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;


@Entity
public class Colecao{


    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "geral", nullable = false)
    private boolean geral = false;

    //telefone guarda id do usuario (1 usuario pode ter varios telefones)
    
    //@org.hibernate.annotations.ForeignKey

    @ManyToOne // 1 coleção pertence a um usuario, 1 usuario tem várias coleções
    @JoinColumn(
        name="usuario_id",
        nullable=false,
        foreignKey=@ForeignKey(name="fk_usuario")
    )
    @JsonIgnore
    private Usuario usuario;

    @ManyToMany // 1 coleção tem várias cartas, 1 cartas está em várias coleções
    @JoinTable(
        name = "colecao_carta",
        joinColumns = @JoinColumn(name = "colecao_id"),
        inverseJoinColumns = @JoinColumn(name = "carta_id")
    )
    private List<Carta> cartas;


    public Colecao() {
    }

    public Colecao(List<Carta> cartas, Long id, Usuario usuario) {
        this.cartas = cartas;
        this.id = id;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<Carta> getCartas() {
        return cartas;
    }

    public void setCartas(List<Carta> cartas) {
        this.cartas = cartas;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isGeral() {
        return geral;
    }

    public void setGeral(boolean geral) {
        this.geral = geral;
    }

}