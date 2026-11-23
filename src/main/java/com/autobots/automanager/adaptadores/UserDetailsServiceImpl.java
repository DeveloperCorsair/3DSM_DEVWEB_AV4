package com.autobots.automanager.adaptadores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.autobots.automanager.entidades.Usuario;
import com.autobots.automanager.repositorios.RepositorioUsuario;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private RepositorioUsuario repositorio;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Usuario selecionado = repositorio.findByCredencialNomeUsuario(username)
				.orElseThrow(() -> new UsernameNotFoundException(username));
		return new UserDetailsImpl(selecionado);
	}
}
