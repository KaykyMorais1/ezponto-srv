# scaffold-backend.md

> **Instrução para Claude Code — EzPonto Backend**
> Leia este documento integralmente antes de criar qualquer arquivo.
> Execute as seções na ordem apresentada. Não pule etapas.

---

## Contexto

Este documento instrui a criação completa do backend Spring Boot do app **EzPonto** (ShowCo).
O backend expõe uma API REST consumida por um app React Native + Expo.

**Não alterar decisões de stack ou arquitetura sem aprovação explícita.**

---

## 1. Pré-requisitos — verificar antes de iniciar

```bash
java -version   # deve ser 21 (LTS)
mvn -version    # deve ser 3.9+
```

Se algum comando falhar, pare e informe. Não prossiga.

---

## 2. Criar o projeto via Spring Initializr (CLI)

Execute o comando abaixo na pasta raiz onde o projeto será criado:

```bash
curl -G https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.3.4 \
  -d baseDir=ezponto-backend \
  -d groupId=com.ezponto \
  -d artifactId=ezponto-backend \
  -d name=EzPonto \
  -d description="EzPonto - Sistema de Controle de Ponto" \
  -d packageName=com.ezponto \
  -d packaging=jar \
  -d javaVersion=21 \
  -d dependencies=web,data-jpa,postgresql,security,validation,flyway,lombok,actuator \
  -o ezponto-backend.zip

unzip ezponto-backend.zip
cd ezponto-backend
```

Após descompactar, confirme que a estrutura base existe:
```
ezponto-backend/
├── pom.xml
├── src/main/java/com/ezponto/
└── src/main/resources/
```

---

## 3. Adicionar dependências extras ao `pom.xml`

Abra o `pom.xml` gerado e adicione as dependências abaixo dentro de `<dependencies>`, após as existentes:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.2</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.6.2</version>
    <scope>provided</scope>
</dependency>

<!-- Cloudflare R2 (AWS SDK S3-compatible) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.26.29</version>
</dependency>

<!-- SpringDoc / Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

Adicione também o plugin MapStruct + Lombok dentro de `<build><plugins>`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.6.2</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 4. Estrutura de pacotes — criar todos os diretórios

Crie manualmente a seguinte estrutura dentro de `src/main/java/com/ezponto/`:

```
com.ezponto/
├── config/
│   ├── security/
│   └── storage/
├── domain/
│   ├── conta/
│   ├── evento/
│   ├── equipe/
│   ├── funcionario/
│   ├── ponto/
│   └── shared/
│       └── exception/
├── application/
│   ├── auth/
│   ├── evento/
│   ├── funcionario/
│   └── ponto/
└── presentation/
    ├── auth/
    │   └── dto/
    ├── evento/
    │   └── dto/
    ├── funcionario/
    │   └── dto/
    └── ponto/
        └── dto/
```

Crie também os recursos:
```
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── db/migration/
    ├── V1__create_contas.sql
    ├── V2__create_funcionarios.sql
    ├── V3__create_eventos.sql
    ├── V4__create_equipe_evento.sql
    ├── V5__create_registros_ponto.sql
    └── V6__seed_admin.sql
```

---

## 5. `application.yml` — configuração base

Crie `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ezponto-backend
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/ezponto}
    username: ${DATABASE_USER:postgres}
    password: ${DATABASE_PASS:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: ${PORT:8080}

app:
  jwt:
    secret: ${JWT_SECRET:dev-secret-key-must-be-at-least-256-bits-long-for-hs256}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}
  storage:
    r2:
      endpoint: ${R2_ENDPOINT:}
      access-key: ${R2_ACCESS_KEY:}
      secret-key: ${R2_SECRET_KEY:}
      bucket: ${R2_BUCKET:ezponto-fotos}

logging:
  level:
    com.ezponto: DEBUG
    org.springframework.security: INFO

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui
```

Crie `src/main/resources/application-dev.yml`:

```yaml
spring:
  jpa:
    show-sql: true
  flyway:
    clean-disabled: false

logging:
  level:
    com.ezponto: DEBUG
    org.springframework.security: DEBUG
```

Crie `src/main/resources/application-prod.yml`:

```yaml
spring:
  jpa:
    show-sql: false

logging:
  level:
    com.ezponto: INFO
    org.springframework.security: WARN
```

---

## 6. Migrations Flyway — modelo de dados completo

### `V1__create_contas.sql`

```sql
CREATE TABLE contas (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'FUNCIONARIO')),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contas_email ON contas(email);
```

### `V2__create_funcionarios.sql`

```sql
CREATE TABLE funcionarios (
    id          BIGSERIAL PRIMARY KEY,
    conta_id    BIGINT       NOT NULL REFERENCES contas(id),
    nome        VARCHAR(255) NOT NULL,
    cpf         VARCHAR(14)  NOT NULL UNIQUE,
    cargo       VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ATIVO'
                    CHECK (status IN ('ATIVO', 'INATIVO', 'PRESENTE', 'AUSENTE')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_funcionarios_cpf    ON funcionarios(cpf);
CREATE INDEX idx_funcionarios_status ON funcionarios(status);
```

### `V3__create_eventos.sql`

```sql
CREATE TABLE eventos (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255)     NOT NULL,
    data_inicio     TIMESTAMPTZ      NOT NULL,
    data_fim        TIMESTAMPTZ      NOT NULL,
    endereco        VARCHAR(500),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    raio_metros     INTEGER          NOT NULL DEFAULT 100
                        CHECK (raio_metros >= 50 AND raio_metros <= 500),
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_datas CHECK (data_fim > data_inicio)
);

CREATE INDEX idx_eventos_datas ON eventos(data_inicio, data_fim);
```

> Nota: `status` do evento é calculado dinamicamente a partir de `data_inicio` / `data_fim`.
> Nunca armazenar status como coluna — calculado no Java.

### `V4__create_equipe_evento.sql`

```sql
CREATE TABLE equipe_evento (
    id                  BIGSERIAL   PRIMARY KEY,
    evento_id           BIGINT      NOT NULL REFERENCES eventos(id),
    funcionario_id      BIGINT      NOT NULL REFERENCES funcionarios(id),
    data_adicionado     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_equipe UNIQUE (evento_id, funcionario_id)
);

CREATE INDEX idx_equipe_evento_id       ON equipe_evento(evento_id);
CREATE INDEX idx_equipe_funcionario_id  ON equipe_evento(funcionario_id);
```

### `V5__create_registros_ponto.sql`

```sql
CREATE TABLE registros_ponto (
    id              BIGSERIAL   PRIMARY KEY,
    funcionario_id  BIGINT      NOT NULL REFERENCES funcionarios(id),
    evento_id       BIGINT      NOT NULL REFERENCES eventos(id),
    tipo            VARCHAR(20) NOT NULL
                        CHECK (tipo IN ('ENTRADA', 'SAIDA', 'INICIO_INTERVALO', 'FIM_INTERVALO')),
    status          VARCHAR(20) NOT NULL DEFAULT 'APROVADO'
                        CHECK (status IN ('APROVADO', 'PENDENTE', 'REJEITADO')),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    foto_url        VARCHAR(500),
    timestamp_servidor TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_registros_funcionario ON registros_ponto(funcionario_id);
CREATE INDEX idx_registros_evento      ON registros_ponto(evento_id);
CREATE INDEX idx_registros_timestamp   ON registros_ponto(timestamp_servidor DESC);
```

### `V6__seed_admin.sql`

```sql
-- Senha: Admin@123 (bcrypt hash)
INSERT INTO contas (email, senha_hash, role)
VALUES (
    'admin@showco.com.br',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj2LVoFHFkPG',
    'ADMIN'
);
```

> ⚠️ Este seed é apenas para desenvolvimento. Em produção, criar admin via endpoint protegido ou variável de ambiente.

---

## 7. Entidades JPA — domínio

### `domain/conta/Conta.java`

```java
package com.ezponto.domain.conta;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "contas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContaRole role;

    @Column(nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

### `domain/conta/ContaRole.java`

```java
package com.ezponto.domain.conta;

public enum ContaRole {
    ADMIN, FUNCIONARIO
}
```

### `domain/conta/ContaRepository.java`

```java
package com.ezponto.domain.conta;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    Optional<Conta> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### `domain/funcionario/Funcionario.java`

```java
package com.ezponto.domain.funcionario;

import com.ezponto.domain.conta.Conta;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "funcionarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, updatable = false)
    private String cpf;

    @Column(nullable = false)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuncionarioStatus status = FuncionarioStatus.ATIVO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

### `domain/funcionario/FuncionarioStatus.java`

```java
package com.ezponto.domain.funcionario;

public enum FuncionarioStatus {
    ATIVO, INATIVO, PRESENTE, AUSENTE
}
```

### `domain/funcionario/FuncionarioRepository.java`

```java
package com.ezponto.domain.funcionario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    boolean existsByCpf(String cpf);

    Optional<Funcionario> findByContaId(Long contaId);

    // Funcionários disponíveis para um evento:
    // ativos, não já na equipe do evento, sem sobreposição de datas
    @Query("""
        SELECT f FROM Funcionario f
        WHERE f.status != 'INATIVO'
        AND f.id NOT IN (
            SELECT ee.funcionario.id FROM EquipeEvento ee WHERE ee.evento.id = :eventoId
        )
        AND f.id NOT IN (
            SELECT ee.funcionario.id FROM EquipeEvento ee
            JOIN ee.evento e
            WHERE e.id != :eventoId
            AND e.dataInicio < :dataFim
            AND e.dataFim > :dataInicio
        )
    """)
    List<Funcionario> findDisponiveis(
        @Param("eventoId") Long eventoId,
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
}
```

### `domain/evento/Evento.java`

```java
package com.ezponto.domain.evento;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "eventos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private OffsetDateTime dataFim;

    private String endereco;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "raio_metros", nullable = false)
    private Integer raioMetros = 100;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Status calculado — nunca persistido
    @Transient
    public EventoStatus getStatus() {
        OffsetDateTime agora = OffsetDateTime.now();
        if (agora.isBefore(dataInicio)) return EventoStatus.A_ACONTECER;
        if (agora.isAfter(dataFim))     return EventoStatus.ENCERRADO;
        return EventoStatus.EM_ANDAMENTO;
    }
}
```

### `domain/evento/EventoStatus.java`

```java
package com.ezponto.domain.evento;

public enum EventoStatus {
    A_ACONTECER, EM_ANDAMENTO, ENCERRADO
}
```

### `domain/evento/EventoRepository.java`

```java
package com.ezponto.domain.evento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Eventos que estão EM_ANDAMENTO agora
    @Query("SELECT e FROM Evento e WHERE e.dataInicio <= :agora AND e.dataFim >= :agora")
    List<Evento> findEmAndamento(@Param("agora") OffsetDateTime agora);

    // Evento ativo vinculado a um funcionário (para UC-01 e UC-06)
    @Query("""
        SELECT e FROM Evento e
        JOIN EquipeEvento ee ON ee.evento.id = e.id
        WHERE ee.funcionario.id = :funcionarioId
        AND e.dataInicio <= :agora AND e.dataFim >= :agora
    """)
    java.util.Optional<Evento> findEventoAtivoDoFuncionario(
        @Param("funcionarioId") Long funcionarioId,
        @Param("agora") OffsetDateTime agora
    );
}
```

### `domain/equipe/EquipeEvento.java`

```java
package com.ezponto.domain.equipe;

import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.funcionario.Funcionario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "equipe_evento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipeEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "data_adicionado", nullable = false)
    private OffsetDateTime dataAdicionado = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
```

### `domain/equipe/EquipeEventoRepository.java`

```java
package com.ezponto.domain.equipe;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EquipeEventoRepository extends JpaRepository<EquipeEvento, Long> {

    List<EquipeEvento> findByEventoId(Long eventoId);

    Optional<EquipeEvento> findByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);

    boolean existsByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);

    void deleteByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);
}
```

### `domain/ponto/RegistroPonto.java`

```java
package com.ezponto.domain.ponto;

import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.funcionario.Funcionario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registros_ponto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistroPonto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPonto tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPonto status = StatusPonto.APROVADO;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "timestamp_servidor", nullable = false)
    private OffsetDateTime timestampServidor = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
```

### `domain/ponto/TipoPonto.java`

```java
package com.ezponto.domain.ponto;

public enum TipoPonto {
    ENTRADA, SAIDA, INICIO_INTERVALO, FIM_INTERVALO
}
```

### `domain/ponto/StatusPonto.java`

```java
package com.ezponto.domain.ponto;

public enum StatusPonto {
    APROVADO, PENDENTE, REJEITADO
}
```

### `domain/ponto/RegistroPontoRepository.java`

```java
package com.ezponto.domain.ponto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface RegistroPontoRepository extends JpaRepository<RegistroPonto, Long> {

    List<RegistroPonto> findByFuncionarioIdOrderByTimestampServidorDesc(Long funcionarioId);

    List<RegistroPonto> findByEventoIdOrderByTimestampServidorDesc(Long eventoId);

    // Últimos N registros para o dashboard (com nome do funcionário via join)
    @Query("""
        SELECT r FROM RegistroPonto r
        JOIN FETCH r.funcionario
        ORDER BY r.timestampServidor DESC
    """)
    List<RegistroPonto> findUltimosRegistros(Pageable pageable);

    // Registros do dia de hoje
    @Query("""
        SELECT r FROM RegistroPonto r
        WHERE r.timestampServidor >= :inicioDia
        AND r.timestampServidor < :fimDia
    """)
    List<RegistroPonto> findRegistrosDoDia(
        @Param("inicioDia") OffsetDateTime inicioDia,
        @Param("fimDia") OffsetDateTime fimDia
    );

    // Verificar se funcionário já fez entrada hoje no evento
    @Query("""
        SELECT COUNT(r) > 0 FROM RegistroPonto r
        WHERE r.funcionario.id = :funcionarioId
        AND r.evento.id = :eventoId
        AND r.tipo = :tipo
        AND r.timestampServidor >= :inicioDia
    """)
    boolean existeRegistroHoje(
        @Param("funcionarioId") Long funcionarioId,
        @Param("eventoId") Long eventoId,
        @Param("tipo") TipoPonto tipo,
        @Param("inicioDia") OffsetDateTime inicioDia
    );
}
```

---

## 8. Exceções do domínio

### `domain/shared/exception/RecursoNaoEncontradoException.java`

```java
package com.ezponto.domain.shared.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
```

### `domain/shared/exception/RegraDeNegocioException.java`

```java
package com.ezponto.domain.shared.exception;

public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
```

### `domain/shared/exception/AcessoNegadoException.java`

```java
package com.ezponto.domain.shared.exception;

public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
```

---

## 9. Spring Security + JWT

### `config/security/JwtService.java`

```java
package com.ezponto.config.security;

import com.ezponto.domain.conta.Conta;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Conta conta) {
        return Jwts.builder()
                .subject(conta.getEmail())
                .claim("role", conta.getRole().name())
                .claim("contaId", conta.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    public String extrairEmail(String token) {
        return parsearClaims(token).getSubject();
    }

    public boolean tokenValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

### `config/security/JwtAuthFilter.java`

```java
package com.ezponto.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (!jwtService.tokenValido(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtService.extrairEmail(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
```

### `config/security/ContaUserDetailsService.java`

```java
package com.ezponto.config.security;

import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaUserDetailsService implements UserDetailsService {

    private final ContaRepository contaRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Conta conta = contaRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada: " + email));

        return new org.springframework.security.core.userdetails.User(
                conta.getEmail(),
                conta.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + conta.getRole().name()))
        );
    }
}
```

### `config/security/SecurityConfig.java`

```java
package com.ezponto.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

---

## 10. Auth — endpoint de login

### `presentation/auth/dto/LoginRequest.java`

```java
package com.ezponto.presentation.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String senha;
}
```

### `presentation/auth/dto/LoginResponse.java`

```java
package com.ezponto.presentation.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class LoginResponse {
    private String token;
    private String role;
    private Long contaId;
    private String email;
    private String nome;    // nome do funcionário, se role = FUNCIONARIO; null se ADMIN
    private Long funcionarioId; // null se ADMIN
}
```

### `application/auth/AuthService.java` (interface)

```java
package com.ezponto.application.auth;

import com.ezponto.presentation.auth.dto.LoginRequest;
import com.ezponto.presentation.auth.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
```

### `application/auth/AuthServiceImpl.java`

```java
package com.ezponto.application.auth;

import com.ezponto.config.security.JwtService;
import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.conta.ContaRole;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.shared.exception.AcessoNegadoException;
import com.ezponto.presentation.auth.dto.LoginRequest;
import com.ezponto.presentation.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ContaRepository contaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        Conta conta = contaRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AcessoNegadoException("Credenciais inválidas"));

        if (!conta.getAtivo()) {
            throw new AcessoNegadoException("Conta inativa");
        }

        if (!passwordEncoder.matches(request.getSenha(), conta.getSenhaHash())) {
            throw new AcessoNegadoException("Credenciais inválidas");
        }

        String token = jwtService.gerarToken(conta);

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .token(token)
                .role(conta.getRole().name())
                .contaId(conta.getId())
                .email(conta.getEmail());

        if (conta.getRole() == ContaRole.FUNCIONARIO) {
            funcionarioRepository.findByContaId(conta.getId()).ifPresent(f -> {
                builder.nome(f.getNome());
                builder.funcionarioId(f.getId());
            });
        }

        return builder.build();
    }
}
```

### `presentation/auth/AuthController.java`

```java
package com.ezponto.presentation.auth;

import com.ezponto.application.auth.AuthService;
import com.ezponto.presentation.auth.dto.LoginRequest;
import com.ezponto.presentation.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

---

## 11. Global Exception Handler

### `presentation/GlobalExceptionHandler.java`

```java
package com.ezponto.presentation;

import com.ezponto.domain.shared.exception.AcessoNegadoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    record ErrorResponse(String codigo, String mensagem) {}

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NAO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErrorResponse> handleRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("REGRA_DE_NEGOCIO", ex.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErrorResponse> handleAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("ACESSO_NEGADO", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            erros.put(campo, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(erros);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("ERRO_INTERNO", "Erro interno do servidor"));
    }
}
```

---

## 12. CLAUDE.md do backend

Crie `CLAUDE.md` na raiz do projeto `ezponto-backend/`:

```markdown
# EzPonto Backend — Instruções para Claude Code

## Stack
- Java 21 + Spring Boot 3.3 + Maven
- PostgreSQL via Supabase
- Flyway para migrations
- JWT (jjwt 0.12.x)
- MapStruct para mapeamento de DTOs
- Lombok (sem @Autowired em campos — usar @RequiredArgsConstructor)

## Pacote base
com.ezponto

## Regras absolutas
- NUNCA usar @Autowired em campos — sempre constructor injection via @RequiredArgsConstructor
- NUNCA retornar entidades diretamente nos controllers — sempre DTOs de response
- NUNCA persistir status de evento — calculado via getStatus() @Transient na entidade
- NUNCA alterar CPF após criação de funcionário — campo updatable=false
- Soft delete em funcionários — status INATIVO, nunca DELETE SQL
- Timestamp definitivo sempre do servidor — nunca confiar no timestamp do cliente

## Estrutura de pacotes
- domain/         → entidades, repositórios, enums (sem deps de framework exceto JPA)
- application/    → interfaces de serviço + implementações
- config/         → Spring Security, JWT, R2
- presentation/   → controllers, DTOs, exception handler

## Migrations
- Sempre criar novo arquivo V{N}__descricao.sql
- NUNCA editar migrations existentes
- Nomenclatura: snake_case, inglês descritivo

## Endpoints
- Base: /api/v1/
- Admin exclusivo: /api/v1/admin/
- Auth público: /api/v1/auth/

## Antes de qualquer tarefa
Leia showco.md para entender as regras de negócio completas.
```

---

## 13. `.gitignore`

Crie `.gitignore` na raiz do projeto:

```
target/
*.class
*.jar
*.war
*.ear
.idea/
*.iml
.vscode/
.env
.env.local
application-local.yml
*.log
```

---

## 14. Checklist de conclusão

Antes de encerrar, verifique cada item:

- [ ] `mvn clean compile` executa sem erros
- [ ] Estrutura de pacotes criada conforme seção 4
- [ ] Todos os 6 arquivos `.sql` de migration criados em `db/migration/`
- [ ] `application.yml`, `application-dev.yml`, `application-prod.yml` criados
- [ ] Entidades: `Conta`, `Funcionario`, `Evento`, `EquipeEvento`, `RegistroPonto`
- [ ] Repositórios: um por entidade
- [ ] Enums: `ContaRole`, `FuncionarioStatus`, `EventoStatus`, `TipoPonto`, `StatusPonto`
- [ ] `JwtService`, `JwtAuthFilter`, `ContaUserDetailsService`, `SecurityConfig` criados
- [ ] `AuthController` respondendo em `POST /api/v1/auth/login`
- [ ] `GlobalExceptionHandler` no pacote `presentation`
- [ ] `CLAUDE.md` na raiz do projeto
- [ ] `.gitignore` na raiz do projeto
- [ ] Nenhum `@Autowired` em campo em nenhuma classe
- [ ] Nenhuma entidade retornada diretamente em nenhum controller

## 15. Após conclusão — próximos passos

O próximo arquivo de instrução será `implement-eventos-api.md`, que implementará:
- `EventoService` completo (CRUD + gestão de equipe)
- `EventoController` com todos os endpoints
- DTOs de request e response para eventos
- Regras de permissão por status (UC-10)
```
