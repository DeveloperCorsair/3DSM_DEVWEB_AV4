package com.autobots.automanager.entidades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import lombok.Data;

@Data
@Entity
public class Venda {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column
	private String identificacao;
	@Column
	private LocalDateTime cadastro;

	// usuário com o perfil CLIENTE que consumiu a venda
	@ManyToOne(fetch = FetchType.EAGER)
	private Usuario cliente;

	// usuário com o perfil VENDEDOR que realizou a venda
	@ManyToOne(fetch = FetchType.EAGER)
	private Usuario vendedor;

	@ManyToMany(fetch = FetchType.EAGER)
	private List<Mercadoria> mercadorias = new ArrayList<>();

	@ManyToMany(fetch = FetchType.EAGER)
	private List<Servico> servicos = new ArrayList<>();
}
