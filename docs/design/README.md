# NFC Pathway 챗봇 설계 문서

## 문서 개요

NFC 터치 기반 영적 동행 앱의 LLM 챗봇 시스템 설계 문서입니다.

**핵심 목표**: LLM을 '대화 상대'가 아닌 '통제되는 엔진'으로 운영

---

## 📚 문서 구성

### [01. Core Prompt 설계](./01-core-prompt.md)
**목적**: 앱의 정체성을 반영한 불변 시스템 프롬프트 정의

**핵심 내용**:
- 앱의 정체성: "조용히 곁에 서 주는 영적 동행 도구"
- 절대 금지 사항 (상황 추측, 감정 진단, 계시 흉내, 대화 유도)
- 세 가지 pathway (기도, 말씀, 침묵)
- 성경 인용 원칙 (개역개정, 짧게, 제안형)

**주요 산출물**:
- Core Prompt 템플릿
- Policy Message
- 응답 원칙

---

### [02. JSON 스키마 정의](./02-json-schema.md)
**목적**: 구조화된 LLM 출력 형식 정의

**핵심 내용**:
- JSON Schema (Draft-07) 정의
- PathwayResponse 모델 (pathway, verse, prayer_text, silence_guide, brief_connection, crisis_detected)
- 필드별 검증 규칙
- OpenAI Structured Output 연동 방법

**주요 산출물**:
- JSON Schema 파일
- Pydantic 모델 (Python)
- Kotlin 데이터 클래스 (참고)

---

### [03. 정책 및 가드레일](./03-policy-guardrails.md)
**목적**: LLM 응답의 안전성과 정책 준수 보장

**핵심 내용**:
- 4단계 정책 계층 (금지 문구, 금지 행동, 위기 감지, 성경 정확성)
- 정규식 기반 금지 문구 탐지
- 위기 키워드 감지 및 템플릿 교체
- 재시도 전략 (최대 2회)
- Fallback 응답

**주요 산출물**:
- 금지 문구/행동 리스트
- 위기 감지 로직
- 가드레일 파이프라인
- 위기 응답 템플릿

---

### [04. LangChain 파이프라인](./04-langchain-pipeline.md)
**목적**: 통제 가능한 LLM 파이프라인 구현 설계

**핵심 내용**:
- LangChain 사용 이유 (편의 ❌, 운영 통제 ✅)
- 8단계 파이프라인 아키텍처
- PromptTemplate, OutputParser, Guardrail, Retry 구현
- FastAPI 서버 구조
- 배포 및 모니터링

**주요 산출물**:
- LangChain 파이프라인 코드
- FastAPI 서버 구조
- Docker 배포 설정
- 모니터링 메트릭

---

## 🎯 설계 원칙 요약

### 정체성
```
개인 소지형 NFC를 통해,
사용자가 '지금 하나님이 필요하다'고 선택한 순간에
말이 많지 않은 영적 동행을 제공하는 신앙 앱
```

### 하지 않는 것 (Will NOT)
- ❌ 사용자 상황 추측
- ❌ 감정 상태 진단
- ❌ 계시 흉내
- ❌ 성경 정죄/압박
- ❌ 위기 대응을 LLM에 위임

### 하는 것 (Does)
- ✅ 설명 요구 ❌, 캐묻기 ❌
- ✅ 아주 작은 길 제시 (기도·말씀·침묵)
- ✅ 구조화된 JSON 출력
- ✅ 서버 검증 및 통제
- ✅ 위기 시 템플릿 교체

### 기술 원칙
- **자유 텍스트 ❌**: JSON 스키마 강제
- **서버 통제**: 스키마 검증, 금칙어 검사, 위기 대응
- **성경 정확성**: 개역개정, 짧게, 제안형
- **LangChain**: 통제 가능한 파이프라인

---

## 🏗️ 아키텍처 다이어그램

```
┌─────────────────┐
│  Android App    │
│  (Kotlin)       │
└────────┬────────┘
         │ HTTP
         ↓
┌─────────────────────────────────────────────┐
│         FastAPI 백엔드 서버                  │
│  POST /api/pathway                          │
├─────────────────────────────────────────────┤
│  LangChain Pipeline                         │
│    1. 입력 위기 감지                        │
│    2. 프롬프트 조합 (Core+Policy+Schema)   │
│    3. OpenAI API (gpt-4o, Structured)      │
│    4. 출력 파싱 (Pydantic)                 │
│    5. 가드레일 검증                         │
│    6. 재시도 (실패 시)                      │
│    7. Fallback (최종 실패 시)              │
│    8. 로깅 & 모니터링                       │
└─────────────────────────────────────────────┘
         │
         ↓
┌─────────────────┐
│   Supabase DB   │
│   (로깅)        │
└─────────────────┘
```

---

## 📋 구현 로드맵

### Phase 1: 백엔드 파이프라인 구축 (Week 1-2)
- [ ] Python 프로젝트 초기화
- [ ] Core Prompt 구현 (01 문서 기반)
- [ ] JSON Schema 구현 (02 문서 기반)
- [ ] 가드레일 구현 (03 문서 기반)
- [ ] LangChain 파이프라인 구현 (04 문서 기반)
- [ ] FastAPI 서버 구축
- [ ] 단위/통합 테스트 작성

### Phase 2: Android 앱 연동 (Week 3)
- [ ] 백엔드 API 엔드포인트 배포
- [ ] Android 앱 API 클라이언트 구현
- [ ] 기존 OpenAIClient.kt 제거
- [ ] PathwayResponse 모델 추가
- [ ] UI 업데이트 (pathway별 화면)
- [ ] 위기 리소스 화면 추가

### Phase 3: 검증 및 최적화 (Week 4)
- [ ] E2E 테스트
- [ ] 성능 테스트 (부하, 응답 시간)
- [ ] 가드레일 효과성 검증
- [ ] 모니터링 대시보드 구축
- [ ] 문서 업데이트

### Phase 4: 배포 및 모니터링 (Week 5)
- [ ] 프로덕션 배포 (Docker + AWS/GCP)
- [ ] 모니터링 설정 (Prometheus, Grafana)
- [ ] 알림 설정 (Slack)
- [ ] 위기 감지 프로토콜 테스트
- [ ] 사용자 피드백 수집

---

## ✅ 설계 검증 체크리스트

### 정체성 반영
- [ ] "조용히 곁에 서 주는 도구" 정체성이 프롬프트에 명시됨
- [ ] 상황 추측/감정 진단 금지가 프롬프트에 명시됨
- [ ] 계시 흉내 금지가 프롬프트에 명시됨
- [ ] 대화 유도 금지가 프롬프트에 명시됨

### 기술 원칙 준수
- [ ] JSON Schema 정의됨 (Draft-07)
- [ ] 구조화된 출력 강제 (OpenAI Structured Output)
- [ ] 서버 검증 파이프라인 구현
- [ ] 위기 시 템플릿 교체 로직 구현
- [ ] 성경 개역개정 검증 로직 포함

### 가드레일 완전성
- [ ] 금지 문구 리스트 정의됨
- [ ] 금지 행동 패턴 정의됨
- [ ] 위기 키워드 정의됨
- [ ] 재시도 로직 구현
- [ ] Fallback 응답 준비

### 파이프라인 안정성
- [ ] LangChain LCEL 사용
- [ ] 에러 핸들링 구현
- [ ] 로깅 구현
- [ ] 모니터링 메트릭 정의
- [ ] 테스트 커버리지 ≥80%

---

## 🔧 기술 스택

### 백엔드
- **언어**: Python 3.11+
- **프레임워크**: FastAPI
- **LLM 도구**: LangChain
- **LLM API**: OpenAI (gpt-4o-2024-08-06)
- **검증**: Pydantic
- **DB**: Supabase (로깅)
- **배포**: Docker

### Android 앱 (기존)
- **언어**: Kotlin
- **UI**: Jetpack Compose
- **네트워크**: Retrofit / Ktor Client
- **JSON**: kotlinx.serialization

---

## 📊 성공 지표

### 기능적 지표
- **정책 준수율**: ≥99% (가드레일 통과율)
- **위기 감지 정확도**: ≥95% (False Positive ≤5%)
- **스키마 검증 성공률**: 100% (Structured Output 사용)
- **Fallback 발생률**: ≤2%

### 성능 지표
- **평균 응답 시간**: ≤2초
- **P95 응답 시간**: ≤3초
- **가용성**: ≥99.5%
- **재시도율**: ≤5%

### 사용자 경험 지표
- **만족도**: ≥4.0/5.0
- **재사용률**: ≥70%
- **위기 리소스 클릭률**: (위기 감지 시) ≥30%

---

## 🚀 다음 단계

### 1. 백엔드 프로젝트 초기화
```bash
mkdir nfc-pathway-backend
cd nfc-pathway-backend
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install fastapi langchain langchain-openai pydantic uvicorn
```

### 2. 프로젝트 구조 생성
```bash
mkdir -p app/{chains,services,utils,routers}
touch app/{__init__.py,main.py,config.py,models.py}
touch app/chains/{__init__.py,prompts.py,parsers.py,guardrails.py,pipeline.py}
```

### 3. 환경 변수 설정
```bash
cat > .env << EOF
OPENAI_API_KEY=sk-...
SUPABASE_URL=https://...
SUPABASE_KEY=...
ENVIRONMENT=development
LOG_LEVEL=DEBUG
MAX_RETRIES=2
EOF
```

### 4. Core Prompt 구현
- `app/chains/prompts.py`에 01-core-prompt.md 내용 구현
- PromptTemplate 생성

### 5. JSON Schema 구현
- `app/models.py`에 02-json-schema.md 내용 구현
- Pydantic 모델 정의

### 6. 가드레일 구현
- `app/chains/guardrails.py`에 03-policy-guardrails.md 내용 구현
- GuardrailChain 작성

### 7. 파이프라인 조립
- `app/chains/pipeline.py`에 04-langchain-pipeline.md 내용 구현
- LCEL로 전체 파이프라인 구성

### 8. FastAPI 엔드포인트 구현
- `app/main.py`에 `/api/pathway` 엔드포인트 구현
- 요청/응답 모델 정의

---

## 📝 관련 문서

### 프로젝트 문서
- [CLAUDE.md](../../CLAUDE.md): 프로젝트 개요 및 기술 스택
- [README.md](../../README.md): 프로젝트 README (추가 예정)

### 설계 문서
- [01-core-prompt.md](./01-core-prompt.md)
- [02-json-schema.md](./02-json-schema.md)
- [03-policy-guardrails.md](./03-policy-guardrails.md)
- [04-langchain-pipeline.md](./04-langchain-pipeline.md)

---

## 🤝 기여 가이드

### 문서 업데이트
- 각 문서 하단의 "개정 이력" 섹션 업데이트
- 버전 번호 증가 (major.minor)
- 변경 사유 명시

### 코드 구현 시
- 설계 문서 우선 참조
- 정체성 원칙 준수
- 테스트 커버리지 유지
- 로깅 및 모니터링 추가

---

## 📞 문의

설계 관련 질문이나 제안 사항은 프로젝트 관리자에게 문의해 주세요.

---

**Last Updated**: 2026-01-06
**Version**: 1.0
**Status**: 설계 완료, 구현 대기
