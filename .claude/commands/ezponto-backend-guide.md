# EzPonto Backend — Guia Completo

---

## PARTE 1 — Scaffold do Projeto

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

---

## PARTE 2 — Implementação da API

> **Instrução para Claude Code — EzPonto Backend API**
> Leia este documento integralmente antes de criar qualquer arquivo.
> Leia também `CLAUDE.md` na raiz do projeto antes de começar.
> Execute as seções na ordem apresentada. Não pule etapas.

---

## Contexto

O scaffold do projeto já foi executado. Este documento implementa a API REST completa do EzPonto:
- Módulo Eventos (CRUD + equipe)
- Módulo Funcionários (CRUD + soft delete + disponibilidade)
- Módulo Ponto (registro + validação GPS + upload R2)
- Módulo Histórico + Dashboard (admin)

**Java: 17. Pacote base: `com.ezponto`.**

---

## Regras absolutas — nunca violar

- `@Autowired` em campo é proibido — sempre `@RequiredArgsConstructor`
- Entidades nunca saem do service — sempre DTOs de response nos controllers
- Status do evento é calculado via `getStatus()` `@Transient` — nunca persistido
- CPF é imutável — `updatable = false` na coluna
- Soft delete em funcionários — status `INATIVO`, nunca `DELETE` SQL
- Timestamp do ponto sempre do servidor — nunca do cliente
- Toda deleção de evento respeita as permissões por status (UC-10)

---

## 1. Atualizar `application-dev.yml`

Substitua o conteúdo de `src/main/resources/application-dev.yml` por:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.eqhmqsvcfnasopzhhsyh.supabase.co:5432/postgres
    username: postgres
    password: ${DB_PASS}
    driver-class-name: org.postgresql.Driver
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    url: jdbc:postgresql://db.eqhmqsvcfnasopzhhsyh.supabase.co:5432/postgres
    user: postgres
    password: ${DB_PASS}

logging:
  level:
    com.ezponto: DEBUG
    org.springframework.security: DEBUG
    org.flywaydb: DEBUG
```

> ⚠️ Não substituir `${DB_PASS}` — será preenchido manualmente via variável de ambiente ou direto no arquivo local (que está no `.gitignore`).

---

## 2. Atualizar `pom.xml` — Java 17

No `pom.xml`, confirme que a propriedade de versão está em 17:

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

E no plugin do compilador (se existir):
```xml
<configuration>
    <source>17</source>
    <target>17</target>
    ...
</configuration>
```

---

## 3. Módulo Eventos

### 3.1 DTOs

#### `presentation/evento/dto/CriarEventoRequest.java`

```java
package com.ezponto.presentation.evento.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CriarEventoRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Data de início é obrigatória")
    private OffsetDateTime dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private OffsetDateTime dataFim;

    private String endereco;

    @NotNull(message = "Latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    private Double longitude;

    @Min(value = 50, message = "Raio mínimo é 50m")
    @Max(value = 500, message = "Raio máximo é 500m")
    private Integer raioMetros = 100;
}
```

#### `presentation/evento/dto/AtualizarEventoRequest.java`

```java
package com.ezponto.presentation.evento.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AtualizarEventoRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Data de início é obrigatória")
    private OffsetDateTime dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private OffsetDateTime dataFim;

    private String endereco;

    @NotNull(message = "Latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    private Double longitude;

    @Min(50) @Max(500)
    private Integer raioMetros = 100;
}
```

#### `presentation/evento/dto/EventoResponse.java`

```java
package com.ezponto.presentation.evento.dto;

import com.ezponto.domain.evento.EventoStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder
public class EventoResponse {
    private Long id;
    private String nome;
    private OffsetDateTime dataInicio;
    private OffsetDateTime dataFim;
    private String endereco;
    private Double latitude;
    private Double longitude;
    private Integer raioMetros;
    private EventoStatus status;
    private Integer totalMembros;
    private OffsetDateTime createdAt;
}
```

#### `presentation/evento/dto/EventoDetalheResponse.java`

```java
package com.ezponto.presentation.evento.dto;

import com.ezponto.domain.evento.EventoStatus;
import com.ezponto.presentation.funcionario.dto.FuncionarioResumoResponse;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder
public class EventoDetalheResponse {
    private Long id;
    private String nome;
    private OffsetDateTime dataInicio;
    private OffsetDateTime dataFim;
    private String endereco;
    private Double latitude;
    private Double longitude;
    private Integer raioMetros;
    private EventoStatus status;
    private List<MembroEquipeResponse> equipe;
    private OffsetDateTime createdAt;
}
```

#### `presentation/evento/dto/MembroEquipeResponse.java`

```java
package com.ezponto.presentation.evento.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data @Builder
public class MembroEquipeResponse {
    private Long funcionarioId;
    private String nome;
    private String cargo;
    private OffsetDateTime dataAdicionado;
    private boolean presente;
}
```

#### `presentation/evento/dto/AdicionarMembroRequest.java`

```java
package com.ezponto.presentation.evento.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdicionarMembroRequest {
    @NotNull(message = "ID do funcionário é obrigatório")
    private Long funcionarioId;
}
```

### 3.2 Service

#### `application/evento/EventoService.java`

```java
package com.ezponto.application.evento;

import com.ezponto.presentation.evento.dto.*;

import java.util.List;

public interface EventoService {
    List<EventoResponse> listarTodos();
    EventoDetalheResponse buscarPorId(Long id);
    EventoResponse criar(CriarEventoRequest request);
    EventoResponse atualizar(Long id, AtualizarEventoRequest request);
    void deletar(Long id);
    EventoDetalheResponse adicionarMembro(Long eventoId, AdicionarMembroRequest request);
    EventoDetalheResponse removerMembro(Long eventoId, Long funcionarioId);
}
```

#### `application/evento/EventoServiceImpl.java`

```java
package com.ezponto.application.evento;

import com.ezponto.domain.equipe.EquipeEvento;
import com.ezponto.domain.equipe.EquipeEventoRepository;
import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.evento.EventoStatus;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.presentation.evento.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final EquipeEventoRepository equipeEventoRepository;
    private final FuncionarioRepository funcionarioRepository;

    @Override
    public List<EventoResponse> listarTodos() {
        return eventoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EventoDetalheResponse buscarPorId(Long id) {
        Evento evento = buscarEvento(id);
        return toDetalheResponse(evento);
    }

    @Override
    @Transactional
    public EventoResponse criar(CriarEventoRequest request) {
        validarDatas(request.getDataInicio(), request.getDataFim());

        Evento evento = Evento.builder()
                .nome(request.getNome())
                .dataInicio(request.getDataInicio())
                .dataFim(request.getDataFim())
                .endereco(request.getEndereco())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .raioMetros(request.getRaioMetros())
                .build();

        return toResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public EventoResponse atualizar(Long id, AtualizarEventoRequest request) {
        Evento evento = buscarEvento(id);
        EventoStatus status = evento.getStatus();

        if (status == EventoStatus.EM_ANDAMENTO || status == EventoStatus.ENCERRADO) {
            throw new RegraDeNegocioException(
                "Não é possível editar nome, datas ou localização de evento " + status.name()
            );
        }

        validarDatas(request.getDataInicio(), request.getDataFim());

        evento.setNome(request.getNome());
        evento.setDataInicio(request.getDataInicio());
        evento.setDataFim(request.getDataFim());
        evento.setEndereco(request.getEndereco());
        evento.setLatitude(request.getLatitude());
        evento.setLongitude(request.getLongitude());
        evento.setRaioMetros(request.getRaioMetros());

        return toResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        Evento evento = buscarEvento(id);
        EventoStatus status = evento.getStatus();

        if (status == EventoStatus.EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Não é possível deletar evento EM ANDAMENTO");
        }

        // Remover equipe antes de deletar
        equipeEventoRepository.deleteAll(equipeEventoRepository.findByEventoId(id));
        eventoRepository.delete(evento);
    }

    @Override
    @Transactional
    public EventoDetalheResponse adicionarMembro(Long eventoId, AdicionarMembroRequest request) {
        Evento evento = buscarEvento(eventoId);
        EventoStatus status = evento.getStatus();

        if (status == EventoStatus.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível adicionar membros a evento ENCERRADO");
        }

        if (equipeEventoRepository.existsByEventoIdAndFuncionarioId(eventoId, request.getFuncionarioId())) {
            throw new RegraDeNegocioException("Funcionário já está na equipe deste evento");
        }

        Funcionario funcionario = funcionarioRepository.findById(request.getFuncionarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));

        // Verificar sobreposição de datas com outros eventos do funcionário
        List<Funcionario> disponiveis = funcionarioRepository.findDisponiveis(
                eventoId, evento.getDataInicio(), evento.getDataFim()
        );
        boolean disponivel = disponiveis.stream()
                .anyMatch(f -> f.getId().equals(funcionario.getId()));

        if (!disponivel) {
            throw new RegraDeNegocioException(
                "Funcionário possui sobreposição de eventos no período informado"
            );
        }

        EquipeEvento membro = EquipeEvento.builder()
                .evento(evento)
                .funcionario(funcionario)
                .dataAdicionado(OffsetDateTime.now())
                .build();

        equipeEventoRepository.save(membro);
        return toDetalheResponse(buscarEvento(eventoId));
    }

    @Override
    @Transactional
    public EventoDetalheResponse removerMembro(Long eventoId, Long funcionarioId) {
        Evento evento = buscarEvento(eventoId);
        EventoStatus status = evento.getStatus();

        if (status == EventoStatus.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível remover membros de evento ENCERRADO");
        }

        EquipeEvento membro = equipeEventoRepository
                .findByEventoIdAndFuncionarioId(eventoId, funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado na equipe"));

        // EM_ANDAMENTO: só pode remover quem foi adicionado após o início do evento
        if (status == EventoStatus.EM_ANDAMENTO) {
            if (!membro.getDataAdicionado().isAfter(evento.getDataInicio())) {
                throw new RegraDeNegocioException(
                    "Não é possível remover membro que já estava na equipe antes do início do evento"
                );
            }
        }

        equipeEventoRepository.delete(membro);
        return toDetalheResponse(buscarEvento(eventoId));
    }

    // --- Helpers ---

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado: " + id));
    }

    private void validarDatas(OffsetDateTime inicio, OffsetDateTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new RegraDeNegocioException("Data de fim deve ser posterior à data de início");
        }
    }

    private EventoResponse toResponse(Evento evento) {
        int totalMembros = equipeEventoRepository.findByEventoId(evento.getId()).size();
        return EventoResponse.builder()
                .id(evento.getId())
                .nome(evento.getNome())
                .dataInicio(evento.getDataInicio())
                .dataFim(evento.getDataFim())
                .endereco(evento.getEndereco())
                .latitude(evento.getLatitude())
                .longitude(evento.getLongitude())
                .raioMetros(evento.getRaioMetros())
                .status(evento.getStatus())
                .totalMembros(totalMembros)
                .createdAt(evento.getCreatedAt())
                .build();
    }

    private EventoDetalheResponse toDetalheResponse(Evento evento) {
        List<EquipeEvento> equipe = equipeEventoRepository.findByEventoId(evento.getId());
        List<MembroEquipeResponse> membros = equipe.stream()
                .map(ee -> MembroEquipeResponse.builder()
                        .funcionarioId(ee.getFuncionario().getId())
                        .nome(ee.getFuncionario().getNome())
                        .cargo(ee.getFuncionario().getCargo())
                        .dataAdicionado(ee.getDataAdicionado())
                        .presente(false) // calculado via registros de ponto — implementar na integração
                        .build())
                .toList();

        return EventoDetalheResponse.builder()
                .id(evento.getId())
                .nome(evento.getNome())
                .dataInicio(evento.getDataInicio())
                .dataFim(evento.getDataFim())
                .endereco(evento.getEndereco())
                .latitude(evento.getLatitude())
                .longitude(evento.getLongitude())
                .raioMetros(evento.getRaioMetros())
                .status(evento.getStatus())
                .equipe(membros)
                .createdAt(evento.getCreatedAt())
                .build();
    }
}
```

### 3.3 Controller

#### `presentation/evento/EventoController.java`

```java
package com.ezponto.presentation.evento;

import com.ezponto.application.evento.EventoService;
import com.ezponto.presentation.evento.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/eventos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EventoController {

    private final EventoService eventoService;

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listar() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDetalheResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EventoResponse> criar(@Valid @RequestBody CriarEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarEventoRequest request
    ) {
        return ResponseEntity.ok(eventoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        eventoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/equipe")
    public ResponseEntity<EventoDetalheResponse> adicionarMembro(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarMembroRequest request
    ) {
        return ResponseEntity.ok(eventoService.adicionarMembro(id, request));
    }

    @DeleteMapping("/{id}/equipe/{funcionarioId}")
    public ResponseEntity<EventoDetalheResponse> removerMembro(
            @PathVariable Long id,
            @PathVariable Long funcionarioId
    ) {
        return ResponseEntity.ok(eventoService.removerMembro(id, funcionarioId));
    }
}
```

---

## 4. Módulo Funcionários

### 4.1 DTOs

#### `presentation/funcionario/dto/CriarFuncionarioRequest.java`

```java
package com.ezponto.presentation.funcionario.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CriarFuncionarioRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF inválido")
    private String cpf;

    @NotBlank(message = "Cargo é obrigatório")
    private String cargo;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;
}
```

#### `presentation/funcionario/dto/AtualizarFuncionarioRequest.java`

```java
package com.ezponto.presentation.funcionario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtualizarFuncionarioRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Cargo é obrigatório")
    private String cargo;
}
```

#### `presentation/funcionario/dto/FuncionarioResponse.java`

```java
package com.ezponto.presentation.funcionario.dto;

import com.ezponto.domain.funcionario.FuncionarioStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data @Builder
public class FuncionarioResponse {
    private Long id;
    private String nome;
    private String cpf;
    private String cargo;
    private String email;
    private FuncionarioStatus status;
    private OffsetDateTime createdAt;
}
```

#### `presentation/funcionario/dto/FuncionarioResumoResponse.java`

```java
package com.ezponto.presentation.funcionario.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class FuncionarioResumoResponse {
    private Long id;
    private String nome;
    private String cargo;
}
```

### 4.2 Service

#### `application/funcionario/FuncionarioService.java`

```java
package com.ezponto.application.funcionario;

import com.ezponto.presentation.funcionario.dto.*;

import java.util.List;

public interface FuncionarioService {
    List<FuncionarioResponse> listarTodos();
    FuncionarioResponse buscarPorId(Long id);
    FuncionarioResponse criar(CriarFuncionarioRequest request);
    FuncionarioResponse atualizar(Long id, AtualizarFuncionarioRequest request);
    void desativar(Long id);
    List<FuncionarioResponse> listarDisponiveisParaEvento(Long eventoId);
}
```

#### `application/funcionario/FuncionarioServiceImpl.java`

```java
package com.ezponto.application.funcionario;

import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.conta.ContaRole;
import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.funcionario.FuncionarioStatus;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.presentation.funcionario.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final ContaRepository contaRepository;
    private final EventoRepository eventoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<FuncionarioResponse> listarTodos() {
        return funcionarioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FuncionarioResponse buscarPorId(Long id) {
        return toResponse(buscarFuncionario(id));
    }

    @Override
    @Transactional
    public FuncionarioResponse criar(CriarFuncionarioRequest request) {
        if (funcionarioRepository.existsByCpf(request.getCpf())) {
            throw new RegraDeNegocioException("CPF já cadastrado: " + request.getCpf());
        }
        if (contaRepository.existsByEmail(request.getEmail())) {
            throw new RegraDeNegocioException("E-mail já cadastrado: " + request.getEmail());
        }

        Conta conta = Conta.builder()
                .email(request.getEmail())
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .role(ContaRole.FUNCIONARIO)
                .ativo(true)
                .build();
        contaRepository.save(conta);

        Funcionario funcionario = Funcionario.builder()
                .conta(conta)
                .nome(request.getNome())
                .cpf(request.getCpf())
                .cargo(request.getCargo())
                .status(FuncionarioStatus.ATIVO)
                .build();

        return toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public FuncionarioResponse atualizar(Long id, AtualizarFuncionarioRequest request) {
        Funcionario funcionario = buscarFuncionario(id);
        funcionario.setNome(request.getNome());
        funcionario.setCargo(request.getCargo());
        return toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public void desativar(Long id) {
        Funcionario funcionario = buscarFuncionario(id);
        funcionario.setStatus(FuncionarioStatus.INATIVO);
        // Desativar também a conta de acesso
        Conta conta = funcionario.getConta();
        conta.setAtivo(false);
        contaRepository.save(conta);
        funcionarioRepository.save(funcionario);
    }

    @Override
    public List<FuncionarioResponse> listarDisponiveisParaEvento(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado: " + eventoId));

        return funcionarioRepository
                .findDisponiveis(eventoId, evento.getDataInicio(), evento.getDataFim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // --- Helpers ---

    private Funcionario buscarFuncionario(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado: " + id));
    }

    private FuncionarioResponse toResponse(Funcionario f) {
        return FuncionarioResponse.builder()
                .id(f.getId())
                .nome(f.getNome())
                .cpf(f.getCpf())
                .cargo(f.getCargo())
                .email(f.getConta().getEmail())
                .status(f.getStatus())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
```

### 4.3 Controller

#### `presentation/funcionario/FuncionarioController.java`

```java
package com.ezponto.presentation.funcionario;

import com.ezponto.application.funcionario.FuncionarioService;
import com.ezponto.presentation.funcionario.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/funcionarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @GetMapping
    public ResponseEntity<List<FuncionarioResponse>> listar() {
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<FuncionarioResponse> criar(@Valid @RequestBody CriarFuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarFuncionarioRequest request
    ) {
        return ResponseEntity.ok(funcionarioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        funcionarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/disponiveis/{eventoId}")
    public ResponseEntity<List<FuncionarioResponse>> disponiveis(@PathVariable Long eventoId) {
        return ResponseEntity.ok(funcionarioService.listarDisponiveisParaEvento(eventoId));
    }
}
```

---

## 5. Módulo Ponto

### 5.1 Utilitário Haversine

#### `domain/shared/GeoUtils.java`

```java
package com.ezponto.domain.shared;

public class GeoUtils {

    private static final double RAIO_TERRA_METROS = 6_371_000.0;

    private GeoUtils() {}

    /**
     * Calcula a distância em metros entre dois pontos geográficos.
     */
    public static double distanciaMetros(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_METROS * c;
    }

    public static boolean dentroDaArea(
            double latFuncionario, double lonFuncionario,
            double latEvento, double lonEvento,
            int raioMetros
    ) {
        return distanciaMetros(latFuncionario, lonFuncionario, latEvento, lonEvento) <= raioMetros;
    }
}
```

### 5.2 DTOs

#### `presentation/ponto/dto/RegistrarPontoRequest.java`

```java
package com.ezponto.presentation.ponto.dto;

import com.ezponto.domain.ponto.TipoPonto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrarPontoRequest {

    @NotNull(message = "Tipo de ponto é obrigatório")
    private TipoPonto tipo;

    @NotNull(message = "Latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    private Double longitude;

    // base64 da foto — opcional na request, será feito upload pelo backend
    private String fotoBase64;
}
```

#### `presentation/ponto/dto/RegistroPontoResponse.java`

```java
package com.ezponto.presentation.ponto.dto;

import com.ezponto.domain.ponto.StatusPonto;
import com.ezponto.domain.ponto.TipoPonto;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data @Builder
public class RegistroPontoResponse {
    private Long id;
    private TipoPonto tipo;
    private StatusPonto status;
    private Double latitude;
    private Double longitude;
    private String fotoUrl;
    private OffsetDateTime timestampServidor;
    private String eventoNome;
}
```

#### `presentation/ponto/dto/EventoAtivoResponse.java`

```java
package com.ezponto.presentation.ponto.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class EventoAtivoResponse {
    private Long id;
    private String nome;
    private Double latitude;
    private Double longitude;
    private Integer raioMetros;
}
```

### 5.3 Service

#### `application/ponto/PontoService.java`

```java
package com.ezponto.application.ponto;

import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;

import java.util.List;

public interface PontoService {
    RegistroPontoResponse registrar(Long funcionarioId, RegistrarPontoRequest request);
    List<RegistroPontoResponse> historico(Long funcionarioId);
    EventoAtivoResponse buscarEventoAtivo(Long funcionarioId);
}
```

#### `application/ponto/PontoServiceImpl.java`

```java
package com.ezponto.application.ponto;

import com.ezponto.domain.equipe.EquipeEventoRepository;
import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.ponto.RegistroPonto;
import com.ezponto.domain.ponto.RegistroPontoRepository;
import com.ezponto.domain.shared.GeoUtils;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PontoServiceImpl implements PontoService {

    private final RegistroPontoRepository registroPontoRepository;
    private final EventoRepository eventoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final EquipeEventoRepository equipeEventoRepository;
    private final FotoUploadService fotoUploadService;

    @Override
    @Transactional
    public RegistroPontoResponse registrar(Long funcionarioId, RegistrarPontoRequest request) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));

        // UC-01: Validar evento ativo vinculado ao funcionário
        Evento evento = eventoRepository
                .findEventoAtivoDoFuncionario(funcionarioId, OffsetDateTime.now())
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Nenhum evento ativo vinculado a este funcionário"
                ));

        // UC-01: Validar geolocalização contra o raio do evento
        boolean dentroDaArea = GeoUtils.dentroDaArea(
                request.getLatitude(), request.getLongitude(),
                evento.getLatitude(), evento.getLongitude(),
                evento.getRaioMetros()
        );

        if (!dentroDaArea) {
            throw new RegraDeNegocioException(
                    "Registro fora da área permitida pelo evento (raio: " + evento.getRaioMetros() + "m)"
            );
        }

        // Upload de foto (se enviada)
        String fotoUrl = null;
        if (request.getFotoBase64() != null && !request.getFotoBase64().isBlank()) {
            fotoUrl = fotoUploadService.upload(request.getFotoBase64(), funcionarioId);
        }

        RegistroPonto registro = RegistroPonto.builder()
                .funcionario(funcionario)
                .evento(evento)
                .tipo(request.getTipo())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .fotoUrl(fotoUrl)
                .timestampServidor(OffsetDateTime.now())
                .build();

        return toResponse(registroPontoRepository.save(registro));
    }

    @Override
    public List<RegistroPontoResponse> historico(Long funcionarioId) {
        return registroPontoRepository
                .findByFuncionarioIdOrderByTimestampServidorDesc(funcionarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EventoAtivoResponse buscarEventoAtivo(Long funcionarioId) {
        return eventoRepository
                .findEventoAtivoDoFuncionario(funcionarioId, OffsetDateTime.now())
                .map(e -> EventoAtivoResponse.builder()
                        .id(e.getId())
                        .nome(e.getNome())
                        .latitude(e.getLatitude())
                        .longitude(e.getLongitude())
                        .raioMetros(e.getRaioMetros())
                        .build())
                .orElse(null);
    }

    private RegistroPontoResponse toResponse(RegistroPonto r) {
        return RegistroPontoResponse.builder()
                .id(r.getId())
                .tipo(r.getTipo())
                .status(r.getStatus())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .fotoUrl(r.getFotoUrl())
                .timestampServidor(r.getTimestampServidor())
                .eventoNome(r.getEvento().getNome())
                .build();
    }
}
```

### 5.4 Upload de Foto (Cloudflare R2)

#### `application/ponto/FotoUploadService.java`

```java
package com.ezponto.application.ponto;

public interface FotoUploadService {
    String upload(String base64, Long funcionarioId);
}
```

#### `config/storage/R2FotoUploadService.java`

```java
package com.ezponto.config.storage;

import com.ezponto.application.ponto.FotoUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class R2FotoUploadService implements FotoUploadService {

    @Value("${app.storage.r2.endpoint}")
    private String endpoint;

    @Value("${app.storage.r2.access-key}")
    private String accessKey;

    @Value("${app.storage.r2.secret-key}")
    private String secretKey;

    @Value("${app.storage.r2.bucket}")
    private String bucket;

    @Override
    public String upload(String base64, Long funcionarioId) {
        // Se as credenciais R2 não estiverem configuradas, retorna null (dev sem R2)
        if (endpoint == null || endpoint.isBlank()) {
            log.warn("R2 não configurado — foto não será salva");
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            String chave = "pontos/" + funcionarioId + "/" +
                    OffsetDateTime.now().toLocalDate() + "/" +
                    UUID.randomUUID() + ".jpg";

            S3Client s3 = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ))
                    .region(Region.of("auto"))
                    .build();

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(chave)
                            .contentType("image/jpeg")
                            .build(),
                    RequestBody.fromBytes(bytes)
            );

            return endpoint + "/" + bucket + "/" + chave;

        } catch (Exception e) {
            log.error("Erro ao fazer upload da foto: {}", e.getMessage());
            return null;
        }
    }
}
```

### 5.5 Controller

#### `presentation/ponto/PontoController.java`

```java
package com.ezponto.presentation.ponto;

import com.ezponto.application.ponto.PontoService;
import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ponto")
@RequiredArgsConstructor
public class PontoController {

    private final PontoService pontoService;
    private final ContaRepository contaRepository;
    private final FuncionarioRepository funcionarioRepository;

    @PostMapping
    public ResponseEntity<RegistroPontoResponse> registrar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegistrarPontoRequest request
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pontoService.registrar(funcionarioId, request));
    }

    @GetMapping("/historico")
    public ResponseEntity<List<RegistroPontoResponse>> historico(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        return ResponseEntity.ok(pontoService.historico(funcionarioId));
    }

    @GetMapping("/evento-ativo")
    public ResponseEntity<EventoAtivoResponse> eventoAtivo(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        EventoAtivoResponse response = pontoService.buscarEventoAtivo(funcionarioId);
        if (response == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(response);
    }

    private Long resolverFuncionarioId(String email) {
        Conta conta = contaRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada"));
        Funcionario funcionario = funcionarioRepository.findByContaId(conta.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));
        return funcionario.getId();
    }
}
```

---

## 6. Módulo Dashboard Admin

### 6.1 DTOs

#### `presentation/dashboard/DashboardResponse.java`

```java
package com.ezponto.presentation.dashboard;

import com.ezponto.presentation.evento.dto.EventoResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class DashboardResponse {
    private int totalFuncionarios;
    private int presentes;
    private int eventosAtivos;
    private int registrosPendentes;
    private List<EventoResponse> eventosDoDia;
    private List<UltimoRegistroResponse> ultimosRegistros;
}
```

#### `presentation/dashboard/UltimoRegistroResponse.java`

```java
package com.ezponto.presentation.dashboard;

import com.ezponto.domain.ponto.TipoPonto;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data @Builder
public class UltimoRegistroResponse {
    private Long id;
    private String funcionarioNome;
    private TipoPonto tipo;
    private OffsetDateTime timestampServidor;
    private String eventoNome;
}
```

### 6.2 Service + Controller

#### `application/dashboard/DashboardService.java`

```java
package com.ezponto.application.dashboard;

import com.ezponto.presentation.dashboard.DashboardResponse;

public interface DashboardService {
    DashboardResponse buscar();
}
```

#### `application/dashboard/DashboardServiceImpl.java`

```java
package com.ezponto.application.dashboard;

import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.funcionario.FuncionarioStatus;
import com.ezponto.domain.ponto.RegistroPonto;
import com.ezponto.domain.ponto.RegistroPontoRepository;
import com.ezponto.domain.ponto.StatusPonto;
import com.ezponto.presentation.dashboard.DashboardResponse;
import com.ezponto.presentation.dashboard.UltimoRegistroResponse;
import com.ezponto.presentation.evento.dto.EventoResponse;
import com.ezponto.domain.equipe.EquipeEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final FuncionarioRepository funcionarioRepository;
    private final EventoRepository eventoRepository;
    private final RegistroPontoRepository registroPontoRepository;
    private final EquipeEventoRepository equipeEventoRepository;

    @Override
    public DashboardResponse buscar() {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime inicioDia = agora.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fimDia = inicioDia.plusDays(1);

        int total = (int) funcionarioRepository.count();
        int presentes = (int) funcionarioRepository.findAll().stream()
                .filter(f -> f.getStatus() == FuncionarioStatus.PRESENTE)
                .count();

        List<Evento> eventosAtivos = eventoRepository.findEmAndamento(agora);

        List<RegistroPonto> ultimosRegistros = registroPontoRepository
                .findUltimosRegistros(PageRequest.of(0, 5));

        int pendentes = (int) registroPontoRepository.findAll().stream()
                .filter(r -> r.getStatus() == StatusPonto.PENDENTE)
                .count();

        List<EventoResponse> eventosDoDia = eventosAtivos.stream()
                .map(e -> {
                    int membros = equipeEventoRepository.findByEventoId(e.getId()).size();
                    return EventoResponse.builder()
                            .id(e.getId())
                            .nome(e.getNome())
                            .dataInicio(e.getDataInicio())
                            .dataFim(e.getDataFim())
                            .status(e.getStatus())
                            .totalMembros(membros)
                            .build();
                })
                .toList();

        List<UltimoRegistroResponse> ultimos = ultimosRegistros.stream()
                .map(r -> UltimoRegistroResponse.builder()
                        .id(r.getId())
                        .funcionarioNome(r.getFuncionario().getNome())
                        .tipo(r.getTipo())
                        .timestampServidor(r.getTimestampServidor())
                        .eventoNome(r.getEvento().getNome())
                        .build())
                .toList();

        return DashboardResponse.builder()
                .totalFuncionarios(total)
                .presentes(presentes)
                .eventosAtivos(eventosAtivos.size())
                .registrosPendentes(pendentes)
                .eventosDoDia(eventosDoDia)
                .ultimosRegistros(ultimos)
                .build();
    }
}
```

#### `presentation/dashboard/DashboardController.java`

```java
package com.ezponto.presentation.dashboard;

import com.ezponto.application.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> buscar() {
        return ResponseEntity.ok(dashboardService.buscar());
    }
}
```

---

## 7. Endpoint de equipe do funcionário (UC — EquipeScreen)

Adicione este endpoint ao `PontoController` existente:

```java
@GetMapping("/equipe")
public ResponseEntity<List<MembroEquipeResponse>> minhaEquipe(
        @AuthenticationPrincipal UserDetails userDetails
) {
    Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
    // Busca o evento ativo do funcionário e retorna os membros da equipe
    return eventoRepository
            .findEventoAtivoDoFuncionario(funcionarioId, OffsetDateTime.now())
            .map(evento -> {
                List<MembroEquipeResponse> membros = equipeEventoRepository
                        .findByEventoId(evento.getId())
                        .stream()
                        .map(ee -> MembroEquipeResponse.builder()
                                .funcionarioId(ee.getFuncionario().getId())
                                .nome(ee.getFuncionario().getNome())
                                .cargo(ee.getFuncionario().getCargo())
                                .dataAdicionado(ee.getDataAdicionado())
                                .presente(ee.getFuncionario().getStatus() == FuncionarioStatus.PRESENTE)
                                .build())
                        .toList();
                return ResponseEntity.ok(membros);
            })
            .orElse(ResponseEntity.ok(List.of()));
}
```

> Para que esse endpoint compile, injete também `EventoRepository`, `EquipeEventoRepository` e importe `FuncionarioStatus` e `MembroEquipeResponse` no `PontoController`.

---

## 8. Checklist de conclusão

Antes de encerrar, verifique cada item:

- [ ] `mvn clean compile` sem erros
- [ ] `application-dev.yml` atualizado com URL do Supabase (senha preenchida manualmente)
- [ ] `pom.xml` com `<java.version>17</java.version>`
- [ ] Módulo Eventos: service + controller + 7 DTOs
- [ ] Módulo Funcionários: service + controller + 4 DTOs
- [ ] Módulo Ponto: service + controller + 3 DTOs + `GeoUtils` + `R2FotoUploadService`
- [ ] Módulo Dashboard: service + controller + 2 DTOs
- [ ] `GlobalExceptionHandler` cobre `RecursoNaoEncontradoException`, `RegraDeNegocioException`, `AcessoNegadoException`
- [ ] Nenhum `@Autowired` em campo em nenhuma classe
- [ ] Nenhuma entidade retornada diretamente em nenhum controller
- [ ] `FotoUploadService` retorna `null` sem lançar exceção quando R2 não configurado

---

## 9. Resumo dos endpoints gerados

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| POST | `/api/v1/auth/login` | público | Login retorna JWT |
| GET | `/api/v1/admin/dashboard` | ADMIN | Stats + eventos do dia + últimos registros |
| GET | `/api/v1/admin/eventos` | ADMIN | Lista todos os eventos |
| GET | `/api/v1/admin/eventos/{id}` | ADMIN | Detalhe do evento com equipe |
| POST | `/api/v1/admin/eventos` | ADMIN | Criar evento |
| PUT | `/api/v1/admin/eventos/{id}` | ADMIN | Editar evento |
| DELETE | `/api/v1/admin/eventos/{id}` | ADMIN | Deletar evento |
| POST | `/api/v1/admin/eventos/{id}/equipe` | ADMIN | Adicionar membro |
| DELETE | `/api/v1/admin/eventos/{id}/equipe/{funcId}` | ADMIN | Remover membro |
| GET | `/api/v1/admin/funcionarios` | ADMIN | Lista funcionários |
| GET | `/api/v1/admin/funcionarios/{id}` | ADMIN | Detalhe do funcionário |
| POST | `/api/v1/admin/funcionarios` | ADMIN | Criar funcionário + conta |
| PUT | `/api/v1/admin/funcionarios/{id}` | ADMIN | Editar nome/cargo |
| DELETE | `/api/v1/admin/funcionarios/{id}` | ADMIN | Soft delete (INATIVO) |
| GET | `/api/v1/admin/funcionarios/disponiveis/{eventoId}` | ADMIN | Funcionários disponíveis |
| POST | `/api/v1/ponto` | FUNCIONARIO | Registrar ponto |
| GET | `/api/v1/ponto/historico` | FUNCIONARIO | Histórico pessoal |
| GET | `/api/v1/ponto/evento-ativo` | FUNCIONARIO | Evento ativo do funcionário |
| GET | `/api/v1/ponto/equipe` | FUNCIONARIO | Equipe do evento ativo |

---

## 10. Após conclusão — próximos passos

1. Executar as migrations no Supabase: `mvn spring-boot:run` com `DB_PASS` definido
2. Testar o login via Swagger: `http://localhost:8080/swagger-ui`
3. Gerar `integrate-frontend.md` — substituição dos mocks do React Native pelos endpoints reais
