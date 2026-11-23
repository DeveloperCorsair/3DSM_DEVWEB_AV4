package com.autobots.automanager.controles;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.autobots.automanager.repositorios.RepositorioMercadoria;

/** Leitura: ADMIN, GERENTE, VENDEDOR. Escrita (criar/alterar/excluir): ADMIN e GERENTE. */
@RestController
@RequestMapping("/mercadorias")
public class ControleMercadoria {

	@Autowired
	private RepositorioMercadoria repositorio;

	private Mercadoria buscar(Long id) {
		return repositorio.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mercadoria não encontrado(a)"));
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
	@GetMapping
	public List<Mercadoria> listar() {
		return repositorio.findAll();
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE','VENDEDOR')")
	@GetMapping("/{id}")
	public Mercadoria obter(@PathVariable Long id) {
		return buscar(id);
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mercadoria cadastrar(@RequestBody Mercadoria dados) {
		dados.setId(null);
		return repositorio.save(dados);
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	@PutMapping("/{id}")
	public Mercadoria atualizar(@PathVariable Long id, @RequestBody Mercadoria dados) {
		buscar(id);
		dados.setId(id);
		return repositorio.save(dados);
	}

	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long id) {
		Mercadoria alvo = buscar(id);
		try {
			repositorio.delete(alvo);
			repositorio.flush();
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Registro está associado a vendas");
		}
	}
}
