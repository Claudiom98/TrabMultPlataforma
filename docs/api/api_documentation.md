# Documentação do Módulo de Banco de Dados (sql.js)
## 1. Visão Geral
Este módulo exporta funções assíncronas para gerir utilizadores e locais (ocorrências) num sistema de reporte. Ele lida com inserção de dados, consultas geográficas e autenticação básica.

Dependências
Para que este código funcione, o projeto deve ter as seguintes bibliotecas instaladas:

pg: Cliente PostgreSQL para Node.js.

bcrypt: Biblioteca para hash de senhas.

Nota Importante: O banco de dados deve ter a extensão PostGIS ativa (CREATE EXTENSION postgis;).

## 2. Estrutura de Tabelas (Inferida)
Baseado nas queries SQL do seu código, o banco de dados Teste espera as seguintes tabelas:

locais: id, localizacao (geography/geometry), url_foto, descricao, status (ex: 'pendente', 'limpo'), data_reporte, data_limpeza, id_usuario_reportou, id_usuario_limpou.

usuarios: id, email, nome, password_hash.

## 3. Funções Exportadas
### hora()
Verifica a conectividade com o banco de dados retornando a hora atual do servidor.

Parâmetros: Nenhum.

Retorno: Objeto contendo o timestamp do banco de dados (ex: { now: '2023-10-27T10:00:00.000Z' }).

Uso: Testes de conexão (Health check).

### inserirLocal(dadosLocal, idUsuario)
Regista um novo local/ocorrência no mapa. O status inicial é definido automaticamente como 'pendente'.

Parâmetros:

dadosLocal (Objeto):

longitude (Number): Coordenada X.

latitude (Number): Coordenada Y.

url_foto (String): Link ou caminho da imagem.

descricao (String): Texto descritivo do local.

idUsuario (Integer): ID do utilizador que está a reportar.

Retorno: Promessa (void). Nota: O código faz um RETURNING, mas não retorna o valor explicitamente na função JS, apenas executa a query.

### pendentes()
Lista todas as ocorrências que ainda não foram resolvidas.

Parâmetros: Nenhum.

Retorno: Array de objetos contendo:

id, descricao, url_foto, status, data_reporte.

latitude, longitude (Extraídos do campo geométrico).

### limpar(idLocal, idUsuario)
Atualiza o status de um local para 'limpo' e registra quem realizou a limpeza e quando.

Parâmetros:

idLocal (Integer): ID do local a ser atualizado.

idUsuario (Integer): ID do utilizador que realizou a limpeza.

Retorno: Promessa (void).

### inserirUsuario(dadosUsuario)
Regista um novo utilizador no sistema, encriptando a senha antes de salvar.

Parâmetros:

dadosUsuario (Objeto):

email (String).

nome (String).

senha (String): A senha em texto plano (será feito o hash automaticamente).

Retorno: Promessa (void).

### usuarioExistente(dadosBusca)
Busca um utilizador pelo email. Útil para verificar duplicidade no registo ou para processos de login.

Parâmetros:

dadosBusca (Objeto):

email (String).

Retorno: Objeto Result do PostgreSQL (contendo rows, rowCount, etc).

# Documentação da API (Express + JWT) (index.js)
## 1. Configuração e Autenticação
A API roda na porta 3000 (http://localhost:3000).

Mecanismo de Segurança (Middleware checkToken)
A maioria das rotas está protegida. Para acessá-las, é necessário enviar um Token JWT válido no cabeçalho da requisição.

Header: Authorization

Valor: Bearer <SEU_TOKEN_AQUI>

Se o token não for enviado ou for inválido, a API retornará erro 401 Unauthorized ou 403 Forbidden.

## 2. Rotas de Usuários
### Login
Autentica um usuário existente e gera o token de acesso.

Método: POST

URL: /usuarios/login

Autenticação: Pública

Corpo da Requisição (JSON):

JSON

{
  "email": "usuario@exemplo.com",
  "senha": "123"
}
Resposta (201 Created):

JSON

{
  "message": "Sucesso no login",
  "token": "eyJhbGciOiJIUzI...",
  "usuario": { "id": 1, "email": "usuario@exemplo.com" }
}
### Cadastro de Usuário
Registra um novo usuário no sistema.

Método: POST

URL: /usuarios/cadastro

Autenticação: Pública

Corpo da Requisição (JSON):

JSON

{
  "nome": "Fulano",
  "email": "novo@exemplo.com",
  "senha": "123"
}
Resposta (201 Created):

JSON

{ "message": "ok" }
## 3. Rotas de Locais (Ocorrências)
### Inserir Novo Local
Reporta um novo local sujo/com problemas. O ID do usuário é extraído automaticamente do token.

Método: POST

URL: /inserir

Autenticação: Obrigatória (Token JWT)

Corpo da Requisição (JSON):

JSON

{
  "latitude": -23.5505,
  "longitude": -46.6333,
  "descricao": "Lixo na calçada",
  "url_foto": "http://site.com/foto.jpg"
}
Resposta (201 Created):

JSON

{ "message": "ok" }
### Listar Locais Pendentes
Retorna todos os locais que ainda não foram limpos.

Método: GET

URL: /locais/pendentes

Autenticação: Obrigatória (Token JWT)

Resposta (201 Created): Retorna um array de objetos (locais).

### Marcar como Limpo
Atualiza o status de um local específico para "limpo".

Método: PUT

URL: /locais/:id/limpar

Exemplo: /locais/5/limpar (para limpar o local com ID 5)

Autenticação: Obrigatória (Token JWT)

Resposta (201 Created):

JSON

{ "message": "ok" }
## 4. Rotas de Teste / Diversos
### Verificar Usuário (Debug)
Rota aparentemente usada para testes internos para verificar dados brutos de um usuário.

Método: POST

URL: /

Autenticação: Pública

Corpo da Requisição: JSON contendo o email.
