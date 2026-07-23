package br.unesp.zoocard.backend.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.test.context.support.WithSecurityContext;

import br.unesp.zoocard.backend.model.UserRole;

/**
 * Substitui {@code @WithMockUser} nos testes que exercitam
 * {@code @AuthenticationPrincipal Usuario}.
 *
 * O principal padrão instalado por {@code @WithMockUser} é
 * {@link org.springframework.security.core.userdetails.User}, que não é
 * atribuível a {@link br.unesp.zoocard.backend.model.Usuario}. Como
 * {@code @AuthenticationPrincipal} não lança exceção em caso de tipo
 * incompatível (por padrão {@code errorOnInvalidType = false}), ele
 * resolve silenciosamente para {@code null} — fazendo qualquer endpoint
 * com {@code @AuthenticationPrincipal Usuario usuario} enxergar um
 * usuário não autenticado mesmo sob {@code @WithMockUser}.
 *
 * Esta anotação instala um {@code Usuario} real (em memória, sem tocar
 * o banco) como principal da autenticação simulada, preservando a
 * intenção original do teste: validar o comportamento do endpoint para
 * um usuário efetivamente autenticado.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockUsuarioSecurityContextFactory.class)
public @interface WithMockUsuario {

    String nome() default "Usuario Teste";

    String login() default "usuario.teste";

    UserRole role() default UserRole.USER;
}
