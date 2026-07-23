package br.unesp.zoocard.backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
public class Carta{


    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    // bytea, NAO @Lob: com @Lob o Hibernate 6 mapeia byte[] para OID no Postgres,
    // que exige large-object API e quebra em leitura fora de transacao.
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "imagem", columnDefinition = "bytea")
    private byte[] imagem;

    @Column(name = "imagem_mime_type")
    private String imagemMimeType;

    @Column(name = "criada_em")
    private Long criadaEm;

    //@org.hibernate.annotations.ForeignKey
    
    @ManyToMany(mappedBy = "cartas") // 1 coleção pertence a um usuario, 1 usuario tem várias coleções
    @JsonIgnore
    private List<Colecao> colecoes;

    // uma carta é de apenas um animal, mas um animal aparece em varias cartas
    // um telefone é de apenas um usuario, mas um usuario aparece em varias telefones
    @ManyToOne 
    @JoinColumn(
        name="animal_id",
        nullable=false,
        foreignKey=@ForeignKey(name="fk_animal")
    )
    @JsonIgnore
    private Animal animal;


    public Carta() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAnimal(Animal animal){
        this.animal = animal;
    }

    public Animal getAnimal(){
        return animal;
    }


    public List<Colecao> getColecoes() {
        return colecoes;
    }

    public void setColecoes(List<Colecao> colecoes) {
        this.colecoes = colecoes;
    }

    public byte[] getImagem() {
        return imagem;
    }

    public void setImagem(byte[] imagem) {
        this.imagem = imagem;
    }

    public String getImagemMimeType() {
        return imagemMimeType;
    }

    public void setImagemMimeType(String imagemMimeType) {
        this.imagemMimeType = imagemMimeType;
    }

    public Long getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(Long criadaEm) {
        this.criadaEm = criadaEm;
    }

}