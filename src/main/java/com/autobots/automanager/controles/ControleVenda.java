package com.autobots.automanager.controles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.autobots.automanager.entidades.Mercadoria;
import com.autobots.automanager.entidades.Servico;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.entidades.Venda;
import com.autobots.automanager.modelos.Perfil;
import com.autobots.automanager.repositorios.RepositorioMercadoria;
import com.autobots.automanager.repositorios.RepositorioServico;
import com.autobots.automanager.repositorios.RepositorioUsuario;
import com.autobots.automanager.repositorios.RepositorioVenda;
import com.autobots.automanager.servicos.UsuarioLogado;

/**
 * ADMIN e GERENTE: CRUD completo.
 * VENDEDOR: cria vendas (sempre em seu próprio nome) e lê apenas as suas.
 * CLIENTE: lê apenas as vendas em que foi o consumidor.
 */
@RestController
@RequestMapping("/vendas")
public class ControleVenda {

	@Autowired
	private RepositorioVenda repositorio;
	@Autowired
	private RepositorioUsuario repositorioUsuario;
	@Autowired
	private RepositorioMercadoria repositorioMercadoria;
	@Autowired
	private RepositorioServico repositorioServico;
	@Autowired
	private UsuarioLogado logado;

	private boolean gestor() {
		return logado.possui(Perfil.ROLE_ADMIN) || logado.possui(Perfil.ROLE_GERENTE);
	}

	private Venda buscar(Long id) {
		return repositorio.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venda não encontrada"));
	}

	private Usuario usuarioComPerfil(Usuario referencia, Perfil perfil, String papel) {
		if (referencia == null || referencia.getId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o " + papel + " (id)");
		}
		Usuario usuario = repositorioUsuario.findById(referencia.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, papel + " inexistente"));
		if (!usuario.getPerfis().contains(perfil)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário informado não possui o perfil de " + papel);
		}
		return usuario;
	}

	/** Monta a venda a partir do corpo da requisição, resolvendo as referências pelo id. */
	private Venda montar(Venda dados) {
		Venda venda = new Venda();
		venda.setIdentificacao(dados.getIdentificacao());
		venda.setCliente(usuarioComPerfil(dados.getCliente(), Perfil.ROLE_CLIENTE, "cliente"));
		// vendedor logado só vende em seu nome; gestores informam o vendedor
		if (gestor()) {
			venda.setVendedor(usuarioComPerfil(dados.getVendedor(), Perfil.ROLE_VENDEDOR, "vendedor"));
		} else {
			venda.setVendedor(logado.obter());
		}
		List<Mercadoria> mercadorias = new ArrayList<>();
		if (dados.getMercadorias() != null) {
			for (Mercadoria m : dados.getMercadorias()) {
				mercadorias.add(repositorioMercadoria.findById(m.getId()).orElseThrow(
						() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mercadoria inexistente: " + m.getId())));
			}
		}
		List<Servico> servicos = new ArrayList<>();
		if (dados.getServicos() != null) {
			for (Servico s : dados.getServicos()) {
				servicos.add(repositorioServico.findById(s.getId()).orElseThrow(
						() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Serviço inexistente: " + s.getId())));
			}
		}
		venda.setMercadorias(mercadorias);
		venda.setServicos(servicos);
		return venda;
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Venda cadastrar(@RequestBody Venda dados) {
		Venda venda = montar(dados);
		venda.setCadastro(LocalDateTime.now());
		return repositorio.save(venda);
	}

	@GetMapping
	public List<Venda> listar() {
		if (gestor()) {
			return repositorio.findAll();
		}
		Long id = logado.obter().getId();
		if (logado.possui(Perfil.ROLE_VENDEDOR)) {
			return repositorio.findByVendedorId(id);
		}
		return repositorio.findByClienteId(id);
	}

	@GetMapping("/{id}")
	public Venda obter(@PathVariable Long id) {
		Venda venda = buscar(id);
		Long logadoId = logado.obter().getId();
		boolean permitido = gestor()
				|| (logado.possui(Perfil.ROLE_VENDEDOR) && venda.getVendedor().getId().equals(logadoId))
				|| (logado.possui(Perfil.ROLE_CLIENTE) && venda.getCliente().getId().equals(logadoId));
		if (!permitido) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para ler esta venda");
		}
		return venda;
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	@PutMapping("/{id}")
	public Venda atualizar(@PathVariable Long id, @RequestBody Venda dados) {
		Venda existente = buscar(id);
		Venda venda = montar(dados);
		venda.setId(id);
		venda.setCadastro(existente.getCadastro());
		return repositorio.save(venda);
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		repositorio.delete(buscar(id));
	}
}
