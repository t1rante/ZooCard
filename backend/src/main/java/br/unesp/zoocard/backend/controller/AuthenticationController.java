package br.unesp.zoocard.backend.controller;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.unesp.zoocard.backend.dto.DtoMapper;
import br.unesp.zoocard.backend.dto.UsuarioDTO;
import br.unesp.zoocard.backend.model.AuthenticationDTO;
import br.unesp.zoocard.backend.model.Colecao;
import br.unesp.zoocard.backend.model.LoginResponseDTO;
import br.unesp.zoocard.backend.model.RegisterDTO;
import br.unesp.zoocard.backend.repository.ColecaoRepository;
import br.unesp.zoocard.backend.repository.UsuarioRepository;
import br.unesp.zoocard.backend.security.TokenService;
import br.unesp.zoocard.backend.model.Usuario;
import jakarta.validation.Valid;

@RestController
@RequestMapping("auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ColecaoRepository colecaoRepository;
    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data){
     
        // É uma boa prática armazenarmos as senhas do usuário como HASH no banco de dados. 
        // Dessa maneira, caso haja um vazamento do BD, as senhas estarão criptografadas
        // e não poderão ser diretamente acessadas. 
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());

        try{
            var auth = this.authenticationManager.authenticate(usernamePassword);
            var token = tokenService.generateToken((Usuario)auth.getPrincipal());

            return ResponseEntity.ok(new LoginResponseDTO(token, DtoMapper.toUsuarioDTO((Usuario) auth.getPrincipal())));
        }catch(AuthenticationException e){
            // Login ou senha invalidos: falha de credenciais, nao um erro do servidor.
            return ResponseEntity.status(401).build();
        }catch(Exception e){
            System.out.println("Erro:  ");
            System.out.println(e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> me(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(DtoMapper.toUsuarioDTO(usuario));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data){
        // Primeiro verifica se já não existe outro usuário cadastrado com o mesmo login
        if(this.usuarioRepository.findByLogin(data.login()) != null) return ResponseEntity.badRequest().build();

        // Caso não exista, vamos encriptar a senha para salvar no BD. A senha bruta do usuário 
        // NÃO DEVE SER INSERIDA NO BD POR MEDIDAS DE SEGURANÇA.


        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        Usuario newUser = new Usuario(data.nome(), data.login(), encryptedPassword, data.role(), data.colecao());
        this.usuarioRepository.save(newUser);

        Colecao geral = new Colecao();
        geral.setNome("Coleção Geral");
        geral.setGeral(true);
        geral.setUsuario(newUser);
        geral.setCartas(new ArrayList<>());
        this.colecaoRepository.save(geral);

        return ResponseEntity.ok().build();
    }
}