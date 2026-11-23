package com.autobots.automanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.autobots.automanager.entidades.Credencial;
import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.modelos.Perfil;
import com.autobots.automanager.repositorios.RepositorioUsuario;

@SpringBootApplication
public class AutomanagerApplication implements CommandLineRunner {

	@Autowired
	private RepositorioUsuario repositorio;

	public static void main(String[] args) {
		SpringApplication.run(AutomanagerApplication.class, args);
	}

	/** Usuários iniciais (um por perfil) para permitir o primeiro acesso e os testes. */
	@Override
	public void run(String... args) throws Exception {
		criar("administrador", "admin", "123456", Perfil.ROLE_ADMIN);
		criar("gerente", "gerente", "123456", Perfil.ROLE_GERENTE);
		criar("vendedor", "vendedor", "123456", Perfil.ROLE_VENDEDOR);
		criar("cliente", "cliente", "123456", Perfil.ROLE_CLIENTE);
	}

	private void criar(String nome, String nomeUsuario, String senha, Perfil perfil) {
		Usuario usuario = new Usuario();
		usuario.setNome(nome);
		usuario.getPerfis().add(perfil);
		Credencial credencial = new Credencial();
		credencial.setNomeUsuario(nomeUsuario);
		credencial.setSenha(new BCryptPasswordEncoder().encode(senha));
		usuario.setCredencial(credencial);
		repositorio.save(usuario);
	}
}
