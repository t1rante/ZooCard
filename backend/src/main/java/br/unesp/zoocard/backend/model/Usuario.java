package br.unesp.zoocard.backend.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "Usuario_Type")
@JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
public class Usuario implements UserDetails{
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long Id;

    private String nome;
    private String login;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;
    private UserRole role;

    @OneToMany(mappedBy="usuario", cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    private List<Colecao> colecaos = new ArrayList<Colecao>();
    


    public Usuario(String nome, String login, String senha, UserRole role, List<Colecao> colecaos) {
        this.nome = nome;
        this.login = login;
        this.senha = senha;
        this.role = role;
        this.colecaos = colecaos;
    }

    public Usuario(String nome, String login, String senha, UserRole role) {
        this.nome = nome;
        this.login = login;
        this.senha = senha;
        this.role = role;
    }

    public Usuario() {
        
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long Id) {
        this.Id = Id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }


    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }



    public List<Colecao> getColecaos() {
        return colecaos;
    }



    public void setColecaos(List<Colecao> colecaos) {
        this.colecaos = colecaos;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // O Spring Security já tem algumas roles implementadas. Repare que 
        // nesse método o retorno é uma colection, então cada usuário pode ter
        // vários papéis (roles). Por exemplo, um ADMIN é ao mesmo tempo USER
        // norma. Um CHEFE é ao mesmo tempo ADMIN e USER normal, ...
        if (this.role == UserRole.ADMIN){
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),   // Admin
                new SimpleGrantedAuthority("ROLE_USER")     // é ao mesmo tempo user normal
                );
        }else{
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }
    @Override
    public String getPassword() {
        // TODO Auto-generated method stub
        return this.getSenha();
    }
    @Override
    public String getUsername() {
        // TODO Auto-generated method stub
        return this.getLogin();
    }
    @Override
    public boolean isAccountNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        // TODO Auto-generated method stub
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }
    @Override
    public boolean isEnabled() {
        // TODO Auto-generated method stub
        return true;
    }




}

