package com.autobots.automanager.servicos;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.autobots.automanager.adaptadores.UserDetailsImpl;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.modelos.Perfil;

/** Acesso ao usuário autenticado (extraído do JWT pelo filtro Autorizador). */
@Component
public class UsuarioLogado {

	public Usuario obter() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		return ((UserDetailsImpl) autenticacao.getPrincipal()).getUsuario();
	}

	public boolean possui(Perfil perfil) {
		return obter().getPerfis().contains(perfil);
	}
}
