package com.autobots.automanager.controles;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.repositorios.RepositorioUsuario;
import com.autobots.automanager.servicos.PoliticaUsuario;
import com.autobots.automanager.servicos.UsuarioLogado;

@RestController
@RequestMapping("/usuarios")
public class ControleUsuario {

	@Autowired
	private RepositorioUsuario repositorio;
	@Autowired
	private PoliticaUsuario politica;
	@Autowired
	private UsuarioLogado logado;

	private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();

	private Usuario buscar(Long id) {
		return repositorio.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
	}

	private void exigirDadosValidos(Usuario usuario, boolean senhaObrigatoria) {
		Credencial c = usuario.getCredencial();
		if (usuario.getNome() == null || c == null || c.getNomeUsuario() == null || c.getNomeUsuario().isBlank()
				|| (senhaObrigatoria && (c.getSenha() == null || c.getSenha().isBlank()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome, credencial.nomeUsuario e credencial.senha");
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
	@PostMapping
	@org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
	public Usuario cadastrar(@RequestBody Usuario usuario) {
		exigirDadosValidos(usuario, true);
		if (usuario.getPerfis().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um perfil");
		}
		if (!politica.podeGerenciar(usuario.getPerfis())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não pode criar usuários com esses perfis");
		}
		Credencial credencial = new Credencial();
		credencial.setNomeUsuario(usuario.getCredencial().getNomeUsuario());
		credencial.setSenha(codificador.encode(usuario.getCredencial().getSenha()));
		usuario.setId(null);
		usuario.setCredencial(credencial);
		try {
			return repositorio.save(usuario);
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de usuário já existe");
		}
	}

	/** Cada perfil enxerga: o que gerencia + o próprio cadastro (cliente só vê o seu). */
	@GetMapping
	public List<Usuario> listar() {
		return repositorio.findAll().stream().filter(politica::podeLer).collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	public Usuario obter(@PathVariable Long id) {
		Usuario alvo = buscar(id);
		if (!politica.podeLer(alvo)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para ler este usuário");
		}
		return alvo;
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
	@PutMapping("/{id}")
	public Usuario atualizar(@PathVariable Long id, @RequestBody Usuario dados) {
		Usuario alvo = buscar(id);
		if (!politica.podeGerenciar(alvo)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para alterar este usuário");
		}
		exigirDadosValidos(dados, false);
		if (dados.getPerfis().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um perfil");
		}
		// evita escalonamento de privilégio: os novos perfis também precisam ser gerenciáveis
		if (!politica.podeGerenciar(dados.getPerfis())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não pode atribuir esses perfis");
		}
		alvo.setNome(dados.getNome());
		alvo.getPerfis().clear();
		alvo.getPerfis().addAll(dados.getPerfis());
		alvo.getCredencial().setNomeUsuario(dados.getCredencial().getNomeUsuario());
		String novaSenha = dados.getCredencial().getSenha();
		if (novaSenha != null && !novaSenha.isBlank()) {
			alvo.getCredencial().setSenha(codificador.encode(novaSenha));
		}
		try {
			return repositorio.saveAndFlush(alvo);
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de usuário já existe");
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
	@DeleteMapping("/{id}")
	@org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		Usuario alvo = buscar(id);
		if (!politica.podeGerenciar(alvo)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para remover este usuário");
		}
		if (alvo.getId().equals(logado.obter().getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode remover o próprio usuário");
		}
		try {
			repositorio.delete(alvo);
			repositorio.flush();
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário possui vendas associadas");
		}
	}
}
