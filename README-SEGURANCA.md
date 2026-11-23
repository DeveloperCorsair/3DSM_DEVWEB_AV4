# Atualização segurança (JWT) — automanager

## Como rodar
    cd automanager && ./mvnw spring-boot:run      (ou rodar AutomanagerApplication no Eclipse)
Banco H2 em memória; a cada start são criados 4 usuários (senha `123456`): `admin`, `gerente`, `vendedor`, `cliente`.

## Fluxo JWT
1. `POST /login` com `{"nomeUsuario":"admin","senha":"123456"}` -> o token volta no cabeçalho `Authorization: Bearer <jwt>` (filtro `Autenticador`).
2. Nas demais rotas enviar `Authorization: Bearer <jwt>`; o filtro `Autorizador` valida o token (assinatura + expiração, 10 min) e carrega os perfis do usuário.
3. Sem token/token inválido -> 401. Token válido sem permissão -> 403.

## Rotas e permissões (Tabela 1)
| Rota | Admin | Gerente | Vendedor | Cliente |
|---|---|---|---|---|
| `/usuarios` POST/PUT/DELETE | qualquer perfil | gerente, vendedor, cliente | só cliente | — |
| `/usuarios` GET | todos | gerente/vendedor/cliente | clientes | só o próprio |
| `/mercadorias`, `/servicos` GET | sim | sim | sim | — |
| `/mercadorias`, `/servicos` POST/PUT/DELETE | sim | sim | — | — |
| `/vendas` POST | sim | sim | sim (sempre em seu nome) | — |
| `/vendas` GET | todas | todas | só as suas | só as que consumiu |
| `/vendas` PUT/DELETE | sim | sim | — | — |

## Exemplos (curl)
    TOKEN=$(curl -si -X POST localhost:8080/login -H 'Content-Type: application/json' \
      -d '{"nomeUsuario":"gerente","senha":"123456"}' | grep -i '^Authorization' | cut -d' ' -f2- | tr -d '\r')
    curl -X POST localhost:8080/mercadorias -H "Authorization: $TOKEN" -H 'Content-Type: application/json' \
      -d '{"nome":"Filtro de óleo","valor":45.9,"quantidade":10}'
    curl -X POST localhost:8080/vendas -H "Authorization: $TOKEN" -H 'Content-Type: application/json' \
      -d '{"identificacao":"V1","cliente":{"id":4},"vendedor":{"id":3},"mercadorias":[{"id":1}]}'
