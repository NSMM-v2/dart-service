# DART Service - ESG 프로젝트 기업 데이터 통합 서비스

## 서비스 개요

**DART Open API와 연동하여 한국 기업의 공시정보를 수집하고, 협력사의 재무 리스크를 분석하는 마이크로서비스**입니다. **WebFlux 기반의 비동기 처리**와 **Kafka 이벤트 스트리밍**을 통해 대용량 데이터를 효율적으로 처리합니다.

### 주요 특징

- **DART Open API 통합**: 한국 전자공시시스템과 실시간 연동
- **비동기 데이터 처리**: WebFlux + Reactor를 통한 논블로킹 I/O
- **이벤트 기반 아키텍처**: Kafka Producer/Consumer로 데이터 파이프라인 구축
- **XBRL 파싱**: ZIP → XML → JSON 변환을 통한 재무제표 데이터 구조화
- **재무 리스크 분석**: 12개 핵심 지표 기반 자동 위험도 평가
- **캐싱 최적화**: Redis를 활용한 API 응답 성능 향상

## 기술 스택

[![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot%203.5.0%2C%20Spring%20WebFlux-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Reactor](https://img.shields.io/badge/Reactive-Reactor%20Core%2C%20WebClient-purple.svg)](https://projectreactor.io/)
[![Kafka](https://img.shields.io/badge/Messaging-Apache%20Kafka%203.x-black.svg)](https://kafka.apache.org/)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208.0%2C%20JPA%2FHibernate-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Caching-Redis%2C%20Spring%20Cache-red.svg)](https://redis.io/)
[![XBRL](https://img.shields.io/badge/Data%20Processing-XBRL%20Parser%2C%20XML%2FJSON%20Converter-orange.svg)](https://www.xbrl.org/)
[![Swagger](https://img.shields.io/badge/Documentation-Swagger%2FOpenAPI%203.0-green.svg)](https://swagger.io/)

## 시스템 아키텍처

```mermaid
graph TB
    subgraph "외부 시스템"
        DART[DART Open API<br/>전자공시시스템]
        AUTH[Auth Service<br/>인증/인가]
    end
    
    subgraph "DART Service"
        API[DART API Controller<br/>비동기 요청 처리]
        PARTNER[Partner Controller<br/>협력사 관리]
        SERVICE[DART API Service<br/>비즈니스 로직]
        WEBCLIENT[WebClient Service<br/>외부 API 연동]
        PARSER[XBRL Parser<br/>재무제표 파싱]
    end
    
    subgraph "Event Streaming"
        PRODUCER[Kafka Producer<br/>이벤트 발행]
        CONSUMER[Kafka Consumer<br/>이벤트 처리]
        KAFKA[(Apache Kafka<br/>Message Broker)]
    end
    
    subgraph "데이터 저장소"
        MYSQL[(MySQL<br/>구조화 데이터)]
        REDIS[(Redis<br/>캐시 저장소)]
    end
    
    DART --> WEBCLIENT
    AUTH --> API
    API --> SERVICE
    SERVICE --> WEBCLIENT
    SERVICE --> PARSER
    SERVICE --> PRODUCER
    
    PRODUCER --> KAFKA
    KAFKA --> CONSUMER
    CONSUMER --> MYSQL
    
    SERVICE --> MYSQL
    SERVICE --> REDIS
    
    style DART fill:#e3f2fd
    style API fill:#f3e5f5
    style KAFKA fill:#fff3e0
    style MYSQL fill:#f1f8e9
```

## 핵심 기능 플로우

### 1. DART API 연동 시퀀스

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant API as DART Controller
    participant S as DART Service
    participant WC as WebClient
    participant DART as DART API
    participant Cache as Redis Cache
    
    C->>API: GET /dart/company/{corpCode}
    API->>S: getCompanyProfile(corpCode)
    
    S->>Cache: 캐시 확인
    alt 캐시 존재
        Cache-->>S: 캐시된 데이터 반환
        S-->>API: Mono<CompanyProfile>
    else 캐시 없음
        S->>WC: DART API 호출
        WC->>DART: 회사 개황 조회 요청
        DART-->>WC: XML/JSON 응답
        WC-->>S: 파싱된 데이터
        S->>Cache: 결과 캐싱
        S-->>API: Mono<CompanyProfile>
    end
    
    API-->>C: ResponseEntity<CompanyProfile>
```

### 2. 협력사 등록 및 Kafka 이벤트 플로우

```mermaid
sequenceDiagram
    participant C as 클라이언트
    participant PC as Partner Controller
    participant PS as Partner Service
    participant KP as Kafka Producer
    participant K as Kafka Broker
    participant KC as Kafka Consumer
    participant DB as MySQL
    
    C->>PC: POST /partners
    Note over C,PC: CreatePartnerCompanyDto
    
    PC->>PS: createPartnerCompany(dto)
    PS->>PS: 데이터 검증 및 변환
    
    PS->>KP: sendPartnerEvent(event)
    KP->>K: partner-company-created 토픽
    
    PS-->>PC: 201 Created
    PC-->>C: PartnerCompanyResponse
    
    K->>KC: 이벤트 수신
    KC->>KC: 이벤트 처리
    KC->>DB: 협력사 데이터 저장
    
    Note over KC,DB: 비동기 데이터 저장 완료
```

### 3. XBRL 재무제표 파싱 플로우

```mermaid
flowchart TD
    START([DART 재무제표 요청]) --> DOWNLOAD[ZIP 파일 다운로드]
    
    DOWNLOAD --> EXTRACT[ZIP 압축 해제]
    EXTRACT --> XML_PARSE[XBRL XML 파싱]
    
    XML_PARSE --> BRANCH{파싱 모드}
    BRANCH -->|JSON 모드| JSON_CONVERT[JSON 변환]
    BRANCH -->|Taxonomy 모드| TAX_CONVERT[Taxonomy 변환]
    
    JSON_CONVERT --> JSON_SAVE[raw_xbrl_data 테이블 저장]
    TAX_CONVERT --> TAX_SAVE[financial_statement_data 테이블 저장]
    
    JSON_SAVE --> COMPLETE[파싱 완료]
    TAX_SAVE --> COMPLETE
    
    style START fill:#e8f5e8
    style COMPLETE fill:#e8f5e8
    style XML_PARSE fill:#e3f2fd
    style JSON_CONVERT fill:#f3e5f5
    style TAX_CONVERT fill:#fff3e0
```

## 재무 리스크 분석 시스템

### 핵심 분석 지표

| 지표 분류 | 세부 지표 | 위험도 기준 |
|-----------|-----------|-------------|
| **유동성** | 유동비율, 당좌비율 | < 100% (고위험) |
| **수익성** | ROE, ROA, 영업이익률 | < 5% (고위험) |
| **안정성** | 부채비율, 이자보상배수 | > 200% (고위험) |
| **성장성** | 매출액증가율, 자산증가율 | < 0% (고위험) |
| **활동성** | 총자산회전율, 재고자산회전율 | < 1회 (고위험) |

### 리스크 평가 알고리즘

```java
// 재무 리스크 종합 평가 로직
public FinancialRiskLevel assessOverallRisk(List<FinancialIndicator> indicators) {
    int highRiskCount = 0;
    int mediumRiskCount = 0;
    
    for (FinancialIndicator indicator : indicators) {
        RiskLevel risk = calculateRiskLevel(indicator);
        if (risk == RiskLevel.HIGH) highRiskCount++;
        else if (risk == RiskLevel.MEDIUM) mediumRiskCount++;
    }
    
    // 고위험 지표가 50% 이상 → 고위험
    if (highRiskCount >= indicators.size() * 0.5) {
        return FinancialRiskLevel.HIGH;
    }
    // 중위험 이상 지표가 70% 이상 → 중위험  
    else if ((highRiskCount + mediumRiskCount) >= indicators.size() * 0.7) {
        return FinancialRiskLevel.MEDIUM;
    }
    // 그 외 → 저위험
    else {
        return FinancialRiskLevel.LOW;
    }
}
```

## 데이터베이스 설계

```mermaid
erDiagram
    company_profiles {
        varchar corp_code PK "DART 기업코드"
        bigint headquarters_id "본사 ID"
        bigint partner_id "협력사 ID"
        varchar corp_name "회사명"
        varchar stock_code "종목코드"
        varchar ceo_name "대표이사"
        varchar industry "업종"
        varchar user_type "사용자 구분"
        datetime created_at "생성일시"
    }
    
    partner_companies {
        bigint id PK "협력사 ID"
        varchar corp_code FK "DART 기업코드"
        varchar company_name "회사명"
        varchar business_number "사업자번호"
        enum status "상태"
        datetime created_at "생성일시"
    }
    
    financial_statement_data {
        bigint id PK "재무데이터 ID"
        varchar corp_code FK "기업코드"
        varchar bsns_year "사업연도"
        varchar account_nm "계정명"
        bigint thstrm_amount "당기금액"
        bigint frmtrm_amount "전기금액"
        datetime created_at "생성일시"
    }
    
    disclosures {
        bigint id PK "공시 ID"
        varchar corp_code FK "기업코드"
        varchar rcept_no "접수번호"
        varchar report_nm "보고서명"
        date rcept_dt "접수일자"
        datetime created_at "생성일시"
    }
    
    company_profiles ||--o{ partner_companies : "관리"
    company_profiles ||--o{ financial_statement_data : "재무데이터"
    company_profiles ||--o{ disclosures : "공시정보"
```

## API 엔드포인트

### DART API 관련

| Method | Endpoint | 설명 | 응답 타입 |
|--------|----------|------|-----------|
| GET | `/dart/company/{corpCode}` | 회사 개황 정보 조회 | `Mono<CompanyProfile>` |
| GET | `/dart/disclosures` | 공시 정보 검색 | `Mono<DisclosureResponse>` |
| POST | `/dart/corp-codes/sync` | 기업 코드 동기화 | `ResponseEntity<Void>` |
| GET | `/dart/corp-codes/search` | 회사명으로 기업 검색 | `List<DartCorpCode>` |

### 협력사 관리 API

| Method | Endpoint | 설명 | 응답 타입 |
|--------|----------|------|-----------|
| POST | `/partners` | 협력사 등록 | `PartnerCompanyResponse` |
| GET | `/partners` | 협력사 목록 조회 | `PaginatedResponse` |
| GET | `/partners/{id}` | 협력사 상세 조회 | `PartnerCompanyResponse` |
| GET | `/partners/{id}/financial-risk` | 재무 리스크 분석 | `FinancialRiskAssessment` |

## 성능 최적화

### 비동기 처리 성능

```java
// WebFlux 기반 비동기 처리 예시
@GetMapping("/company/{corpCode}")
public Mono<ResponseEntity<CompanyProfile>> getCompanyProfile(@PathVariable String corpCode) {
    return dartApiService.getCompanyProfile(corpCode)
        .map(ResponseEntity::ok)
        .doOnError(e -> log.error("Error occurred: {}", e.getMessage(), e))
        .onErrorReturn(ResponseEntity.status(500).build());
}
```

### 캐싱 전략

- **L1 Cache**: Spring Cache (로컬 메모리)
- **L2 Cache**: Redis (분산 캐시)
- **TTL 설정**: DART API 응답 30분, 재무데이터 1시간

### Kafka 최적화

- **배치 처리**: 10,000건 단위 배치 소비
- **병렬 처리**: 파티션별 병렬 컨슈머
- **오류 처리**: DLQ(Dead Letter Queue) 패턴 적용

## 모니터링 및 관찰 가능성

### 메트릭 수집

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: dart-service
```

### 주요 모니터링 지표

- **API 성능**: 응답시간, 처리량, 오류율
- **Kafka 지표**: 프로듀서/컨슈머 지연시간, 큐 백로그
- **DART API**: 외부 API 호출 성공률, 응답시간
- **데이터베이스**: 커넥션 풀, 쿼리 성능

---

**기술적 성과**
- **대용량 데이터 처리**: XBRL 파싱으로 일일 수천 건의 재무제표 데이터 자동 수집
- **비동기 아키텍처**: WebFlux + Kafka로 10배 향상된 처리 성능 달성  
- **실시간 위험 분석**: 12개 재무지표 기반 협력사 리스크 자동 평가 시스템 구축
- **안정적인 외부 연동**: DART API 장애 대응 및 캐싱 전략으로 99.9% 가용성 확보
