# hub-sms

## Desenvolvimento local

Requisitos: Java 21 e modem configurado em `src/main/resources/application.yaml`.

```powershell
./mvnw.cmd spring-boot:run
```

### H2 Console e histórico de SMS

Com a aplicação iniciada na raiz do projeto, acesse http://localhost:8080/h2-console/.
Na tela de login, use:

- Driver: `org.h2.Driver`
- JDBC URL: `jdbc:h2:file:./data/sms-gateway`
- Usuário: `sa`
- Senha: em branco

Use a mesma URL JDBC configurada na aplicação. O caminho `./data` é relativo ao
diretório de execução; iniciar de outro diretório pode abrir outro banco.

O projeto já inclui `spring-boot-h2console`, necessário para o console no Spring
Boot 4, além do driver H2. Se as dependências foram alteradas com a aplicação em
execução, recarregue o Maven no IntelliJ e reinicie a aplicação.

Para consultar o histórico:

```sql
SELECT id, phone, message, status, created_at, sent_at, error_message, modem_response
FROM sms_message
ORDER BY id DESC;
```

O envio grava `PENDING` antes de chamar o modem e depois atualiza o mesmo registro
para `SENT` ou `FAILED`. `SENT` indica confirmação do modem, sem comprovar a entrega
ao destinatário.

### Consultar SMS pela API

`GET /api/v1/sms/{id}` retorna `id`, `phone`, `message`, `status`, `createdAt`,
`sentAt`, `errorMessage` e `modemResponse`. Campos ainda não preenchidos são `null`.
Um ID inexistente retorna HTTP 404 com `error: "SMS_NOT_FOUND"`.

`GET /api/v1/sms?page=0&size=20&status=SENT` consulta o histórico paginado.

- `page`: começa em zero; padrão `0`.
- `size`: de 1 a 100; padrão `20`.
- `status`: opcional; aceita `PENDING`, `SENT` ou `FAILED` em maiúsculas.
- Ordenação fixa: `createdAt` decrescente e, em caso de empate, `id` decrescente.
- Parâmetros inválidos retornam HTTP 400 com `error: "VALIDATION_ERROR"`.
- Histórico vazio ou página além da última retorna HTTP 200 com `content: []`.

A resposta contém os itens em `content` e os metadados `page`, `size`,
`totalElements` e `totalPages`. Quando há filtro, os totais consideram apenas
os registros com o status solicitado. Exemplo de histórico vazio:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Exemplos no PowerShell:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/v1/sms?page=0&size=20'
Invoke-RestMethod 'http://localhost:8080/api/v1/sms?status=FAILED'
Invoke-RestMethod 'http://localhost:8080/api/v1/sms/1'
```

### Testes

```powershell
./mvnw.cmd test
```

Os testes usam comunicação simulada com o modem. Os testes de integração usam H2
em memória, sem alterar o banco em `data/`, e verificam a persistência dos status
e o acesso HTTP ao console.
