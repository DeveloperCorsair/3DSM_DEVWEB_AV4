package com.autobots.automanager.servicos;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.modelos.Perfil;

/**
 * Regras da Tabela 1 sobre quem pode fazer CRUD em quais usuários:
 * ADMIN -> qualquer perfil; GERENTE -> gerente, vendedor e cliente;
 * VENDEDOR -> cliente; CLIENTE -> nenhum (só lê o próprio cadastro).
 */
@Component
public class PoliticaUsuario {

	@Autowired
	private UsuarioLogado logado;

	/** Perfis que o usuário logado pode criar, alterar e remover. */
	public Set<Perfil> perfisGerenciaveis() {
		Usuario atual = logado.obter();
		Set<Perfil> permitidos = EnumSet.noneOf(Perfil.class);
		if (atual.getPerfis().contains(Perfil.ROLE_ADMIN)) {
			permitidos.addAll(EnumSet.allOf(Perfil.class));
		}
		if (atual.getPerfis().contains(Perfil.ROLE_GERENTE)) {
			permitidos.addAll(EnumSet.of(Perfil.ROLE_GERENTE, Perfil.ROLE_VENDEDOR, Perfil.ROLE_CLIENTE));
		}
		if (atual.getPerfis().contains(Perfil.ROLE_VENDEDOR)) {
			permitidos.add(Perfil.ROLE_CLIENTE);
		}
		return permitidos;
	}

	/** Todos os perfis informados precisam estar entre os gerenciáveis. */
	public boolean podeGerenciar(List<Perfil> perfis) {
		return perfis != null && !perfis.isEmpty() && perfisGerenciaveis().containsAll(perfis);
	}

	public boolean podeGerenciar(Usuario alvo) {
		return podeGerenciar(alvo.getPerfis());
	}

	/** Leitura: quem gerencia o alvo, ou o próprio usuário (inclusive cliente). */
	public boolean podeLer(Usuario alvo) {
		return podeGerenciar(alvo) || alvo.getId().equals(logado.obter().getId());
	}
}
