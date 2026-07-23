package br.unesp.zoocard.backend.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import br.unesp.zoocard.backend.model.Usuario;

public class WithMockUsuarioSecurityContextFactory implements WithSecurityContextFactory<WithMockUsuario> {

    @Override
    public SecurityContext createSecurityContext(WithMockUsuario annotation) {
        Usuario usuario = new Usuario(annotation.nome(), annotation.login(), "senha-mock", annotation.role());

        var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
