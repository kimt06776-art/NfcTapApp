# 프로젝트 핵심 결정사항 및 아키텍처

## 문서 정보
- 작성일: 2026-01-06
- 목적: 설계 과정에서 내린 주요 결정사항과 그 이유 기록

---

## 1. 현재 시스템의 문제점

### 기존 시스템 프롬프트 (OpenAIClient.kt:18-28)

```kotlin
private val systemPrompt = """
    당신은 따뜻하고 친근한 신앙 상담사입니다.
    사용자의 이야기를 경청하고, 공감하며, 적절한 성경 말씀과 함께 위로와 격려를 전합니다.

    대화 지침:
    - 따뜻하고 친근한 말투를 사용하세요
    - 사용자의 감정에 공감하세요
    - 필요할 때 적절한 성경 구절을 인용하세요
    - 판단하지 말고 경청하세요
    - 답변은 간결하게 (3-4문장 정도)
"""
```

### 문제점 분석

| 현재 구현 | 정체성 원칙 | 평가 |
|---------|----------|------|
| "신앙 상담사" | 감정 상담 앱 ❌ | ❌ 불일치 |
| "경청하고, 공감하며" | 감정 진단 안 함 | ❌ 불일치 |
| "위로와 격려를 전합니다" | 설명·캐묻기 ❌ | ❌ 불일치 |
| 자유 텍스트 응답 | JSON 출력 필수 | ❌ 불일치 |
| "3-4문장 정도" | 아주 작은 길만 | ❌ 불일치 |
| AI가 해석 주체 | 해석 주체 AI 아님 | ❌ 불일치 |

**결론**: 시스템 프롬프트 전면 재설계 필요

---

## 2. 기술 원칙

### 기술적으로 지키는 것 (How - Tech)

#### 출력 형식
```
❌ 자유 텍스트 응답
✅ 구조화된 JSON 출력 필수
```

#### 서버 통제
```
서버에서:
  ✅ 스키마 검증
  ✅ 금칙어/정책 위반 검사
  ✅ 위기 상황 시 응답 강제 교체
```

#### XML vs JSON
```
초기 고민: XML 직접 출력 → 파싱 실패 우려

최종 결정:
  LLM 출력: JSON (스키마 강제)
  서버 검증: 스키마, 금칙어, 정책
  필요 시: JSON → XML 변환

통제는 항상 서버가 최종 결정
```

### 신학적으로 지키는 것 (How - Faith)

```
✅ 성경 인용: 개역개정
✅ 짧게, 필요할 때만
✅ 적용은 제안이지 결론이 아니다
✅ 해석의 주체를 AI로 만들지 않는다
✅ 사용자가 하나님과 직접 마주하도록 한 발 물러선다
```

---

## 3. 아키텍처 대전환

### AS-IS: 프론트엔드 직접 호출 (문제)

```
[Android 앱 (Kotlin)]
  ├─ OpenAIClient.kt
  │   └─ OpenAI API 직접 호출
  ├─ 자유 텍스트 응답
  ├─ 검증 없음
  └─ Supabase 직접 연동

[문제점]
❌ LLM 통제 불가능
❌ 정책 변경 시 앱 업데이트 필요
❌ API 키 노출 위험
❌ 위기 대응 불가능
❌ 가드레일 없음
```

### TO-BE: 완전 백엔드 중심 (선택)

```
[Android 앱 (Kotlin)] ← 프론트엔드
  ├─ UI/UX (Jetpack Compose)
  ├─ BackendApiClient
  └─ 로컬 캐싱 (Room)

         ↓ HTTP REST API

[Python 백엔드] ← 새로 구축
  ├─ Repository (Supabase 연동)
  ├─ Service Layer
  ├─ LangChain 파이프라인
  │   ├─ Core Prompt
  │   ├─ Policy Message
  │   └─ Output Schema
  ├─ Guardrail Chain
  │   ├─ 금지 문구 검증
  │   ├─ 금지 행동 검증
  │   ├─ 위기 감지
  │   └─ 성경 정확성
  └─ FastAPI 엔드포인트

         ↓ OpenAI API

[OpenAI (gpt-4o)]
  └─ Structured Output
```

---

## 4. 프로젝트 구조 재구성

### 최종 결정: frontend/backend 분리 (Monorepo)

```
NfcTapApp/                        # 프로젝트 루트
├── frontend/                     # Android 앱
│   ├── app/
│   │   └── src/main/java/...
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradlew, gradlew.bat
│   ├── gradle.properties
│   ├── local.properties
│   └── .gitignore              # Android용
│
├── backend/                      # Python 백엔드
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py
│   │   ├── models.py
│   │   ├── chains/
│   │   │   ├── prompts.py
│   │   │   ├── guardrails.py
│   │   │   └── pipeline.py
│   │   ├── repositories/
│   │   │   ├── auth_repository.py
│   │   │   └── chat_repository.py
│   │   ├── services/
│   │   │   ├── crisis_detection.py
│   │   │   └── bible_service.py
│   │   └── routers/
│   │       ├── auth.py
│   │       ├── chat.py
│   │       └── pathway.py
│   ├── tests/
│   ├── venv/
│   ├── requirements.txt
│   ├── .env
│   ├── .gitignore              # Python용
│   └── README.md
│
├── docs/                         # 공통 설계 문서
│   ├── design/
│   │   ├── 00-project-decisions.md
│   │   ├── 01-core-prompt.md
│   │   ├── 02-json-schema.md
│   │   ├── 03-policy-guardrails.md
│   │   └── 04-langchain-pipeline.md
│   └── README.md
│
├── CLAUDE.md                     # 프로젝트 가이드
├── proposal.md                   # 기획 문서 (있다면)
└── README.md                     # 전체 프로젝트 README
```

### 구조 선택 이유

**왜 Monorepo?**
1. ✅ **단일 터미널**: 하나의 루트에서 전체 관리
2. ✅ **문서 공유**: docs/ 폴더 하나로 통일
3. ✅ **명확한 분리**: frontend/backend 역할 명확
4. ✅ **확장 가능**: 추후 웹 프론트엔드 추가 용이
5. ✅ **통합 검색**: 전체 코드베이스 검색 가능

**각 폴더 역할**
- `frontend/`: Android 앱 전용 (Kotlin, Gradle)
- `backend/`: Python 백엔드 전용 (FastAPI, LangChain)
- `docs/`: 공통 설계 문서 (기술 스펙, API 명세)
- 루트: 프로젝트 전체 관리 (CLAUDE.md, README.md)

### 마이그레이션 계획

#### Phase 1: 백업 및 준비
```bash
# 현재 상태 백업
git add .
git commit -m "Backup before restructuring"
git branch backup-before-restructure
```

#### Phase 2: frontend/ 이동
```bash
# frontend 폴더 생성
mkdir frontend

# Android 관련 파일 이동
move app frontend\
move gradle frontend\
move build.gradle.kts frontend\
move settings.gradle.kts frontend\
move gradlew frontend\
move gradlew.bat frontend\
move gradle.properties frontend\
move local.properties frontend\

# .gitignore 이동 및 수정
move .gitignore frontend\
```

#### Phase 3: backend/ 생성
```bash
# backend 폴더 및 구조 생성
mkdir backend
cd backend
python -m venv venv
venv\Scripts\activate

# 프로젝트 구조 생성
mkdir app app\chains app\services app\repositories app\routers tests
type nul > app\__init__.py
type nul > app\main.py
type nul > app\config.py
type nul > app\models.py
type nul > requirements.txt
type nul > .env
type nul > .gitignore
type nul > README.md
```

#### Phase 4: .gitignore 재구성
**frontend/.gitignore** (Android):
```
# Gradle
.gradle/
build/
*.iml

# Android Studio
.idea/
local.properties

# Signing
*.jks
keystore.properties
```

**backend/.gitignore** (Python):
```
# Python
venv/
__pycache__/
*.pyc
*.pyo
.Python

# Environment
.env
.env.local

# IDE
.vscode/
.idea/

# Testing
.pytest_cache/
.coverage
```

#### Phase 5: Android Studio 설정 수정
1. Android Studio 종료
2. `frontend/` 폴더를 Android Studio에서 열기
3. Gradle Sync 실행
4. 빌드 테스트: `./gradlew assembleDebug`

#### Phase 6: 검증
```bash
# Android 빌드 테스트
cd frontend
gradlew assembleDebug

# Python 환경 테스트
cd ..\backend
venv\Scripts\activate
pip install -r requirements.txt
python -m pytest tests/
```

---

## 5. 왜 LangChain을 쓰는가?

### 목적: LLM을 '통제되는 엔진'으로 운영

**NOT**: 편해서
**YES**: 운영 통제 파이프라인 구축

### LangChain이 해결하는 문제

#### 1. 프롬프트 조합
```python
# 단일 문자열이 아닌 조합 가능
final_prompt = (
    Core(불변) +
    Mode(가변, 추후) +
    Policy(정책 메시지) +
    Schema(출력 형식)
)
```

#### 2. 구조화된 출력
```python
from langchain.output_parsers import PydanticOutputParser

parser = PydanticOutputParser(pydantic_object=PathwayResponse)
chain = prompt | llm | parser  # 파싱 실패 제로
```

#### 3. 검증 및 재시도
```python
# 정책 위반 시 자동 재시도
retry_parser = RetryWithErrorOutputParser.from_llm(
    parser=parser,
    llm=llm,
    max_retries=2
)
```

#### 4. 가드레일
```python
# 커스텀 검증 체인
guardrail_chain = GuardrailChain()
pipeline = prompt | llm | parser | guardrail_chain
```

### LangChain 없이 하면?

```kotlin
// Kotlin에서 직접 구현 시
❌ 프롬프트 조합 수동 관리
❌ JSON 파싱 실패 처리
❌ 재시도 로직 직접 구현
❌ 가드레일 체인 처음부터 작성
❌ 3-4배 개발 시간 소요
```

**결론**: LangChain은 '멀티 프롬프트'보다, **검증·재시도·가드레일을 갖춘 실행 파이프라인**을 만들기 위해 사용

---

## 6. 아키텍처 선택: 점진적 vs 완전 백엔드

### 검토한 옵션들

#### Option 1: 점진적 하이브리드
```
[Android]
  ✅ 기존 Repository 유지 (Supabase 직접)
  ✅ AI만 백엔드 호출

[Python Backend]
  ✅ LangChain 파이프라인만

작업량: 3-4일
```

**장점**: 빠름, 최소 변경
**단점**: 보안 취약, 로직 분산

---

#### Option 2: 완전 백엔드 중심 ← **최종 선택**
```
[Android]
  ✅ UI만
  ✅ BackendApiClient
  ✅ 로컬 캐싱

[Python Backend]
  ✅ 모든 Repository (Supabase)
  ✅ 모든 비즈니스 로직
  ✅ LangChain 파이프라인
  ✅ 10+ API 엔드포인트

작업량: 10-14일
```

**장점**: 보안, 중앙화, 확장성
**단점**: 복잡도, 시간

---

#### Option 3: Kotlin 백엔드
```
[Kotlin Backend]
  ✅ 기존 Repository 재사용
  ❌ LangChain 직접 구현 필요
  ❌ 14-21일

결론: ❌ 제외 (AI 생태계 약함)
```

### 최종 결정: Option 2 (완전 백엔드 중심)

**이유**:
1. ✅ **보안**: API 키, DB 접근 서버에만
2. ✅ **중앙화**: 비즈니스 로직 한곳 관리
3. ✅ **확장성**: 웹/iOS도 같은 백엔드 사용
4. ✅ **감사 추적**: 모든 요청 로깅
5. ✅ **장기 유지보수**: 코드 기반 관리

---

## 7. Kotlin 코드 이동 계획

### 백엔드로 이동 (삭제 예정)

```kotlin
❌ OpenAIClient.kt (전체 51줄)
   → Python chains/prompts.py

❌ ChatRepository.conversationHistory
   → Python pipeline 관리

❌ ChatRepository.sendMessageStream()
   → Python endpoint

❌ 모든 Supabase CRUD
   → Python repository
```

### 프론트엔드에 남음 (수정)

```kotlin
✅ ChatViewModel (UI 상태)
   🔄 백엔드 API 호출로 변경

✅ ChatScreen (UI 렌더링)
   🔄 pathway 기반 UI로 개선

✅ AuthViewModel
   🔄 백엔드 API로 변경
```

### 새로 추가

**프론트엔드 (Kotlin)**:
```kotlin
✅ BackendApiClient.kt
✅ PathwayModels.kt
✅ VerseCard.kt
✅ PrayerCard.kt
✅ SilenceCard.kt
```

**백엔드 (Python)**:
```python
✅ app/main.py (FastAPI)
✅ app/chains/ (LangChain)
✅ app/repositories/ (Supabase)
✅ app/services/ (비즈니스 로직)
✅ app/models.py (Pydantic)
```

---

## 8. 파이프라인 구조

### 8단계 파이프라인

```
1️⃣ 입력 위기 감지
   └─ 위기 키워드 검사 → 템플릿 교체

2️⃣ 프롬프트 조합
   ├─ Core Prompt (불변)
   ├─ Mode Prompt (가변, 추후)
   ├─ Policy Message (정책)
   └─ Schema Instruction (출력 형식)

3️⃣ LLM 호출
   └─ gpt-4o-2024-08-06 (Structured Output)

4️⃣ 출력 파싱
   └─ PydanticOutputParser<PathwayResponse>

5️⃣ 가드레일 검증
   ├─ 스키마 검증
   ├─ 금지 문구 검사 (20+ 정규식)
   ├─ 금지 행동 검사 (5+ 패턴)
   ├─ 성경 정확성 (샘플링 10%)
   └─ crisis_detected 확인

6️⃣ 재시도 (실패 시)
   └─ RetryOutputParser (최대 2회)

7️⃣ Fallback (최종 실패 시)
   └─ 기본 응답 (침묵 또는 시편 23편)

8️⃣ 로깅 & 모니터링
   └─ Supabase 또는 별도 DB
```

---

## 9. 출력 스키마

### PathwayResponse

```json
{
  "pathway": "prayer | word | silence",

  "verse": {  // pathway=word 시
    "book": "시편",
    "chapter": 23,
    "verse": "1",
    "text": "여호와는 나의 목자시니..."
  },

  "prayer_text": "주님, 오늘 하루를...",  // pathway=prayer 시

  "silence_guide": "잠시 조용히...",  // pathway=silence 시

  "brief_connection": "오늘 하루 수고했습니다.",  // 항상 필수, max 50자

  "crisis_detected": false,  // 항상 필수

  "crisis_resources": [  // crisis_detected=true 시
    {
      "name": "자살예방 상담전화",
      "phone": "1393",
      "available": "24시간"
    }
  ]
}
```

---

## 10. 가드레일 정책

### 금지 문구 (정규식 20+)

```python
FORBIDDEN_PATTERNS = [
    r"하나님[이가께서]*.{0,10}(말씀하[시셨습]|알려주[시십])",
    r"(당신의 )?감정은",
    r"느끼[시는]+ 것 같",
    r"(힘든|어려운|슬픈) (일|상황)이.{0,5}(있으셨|계신)",
    # ... 20+ 패턴
]
```

### 금지 행동 패턴

```
BP-1: 상황 추측
BP-2: 감정 진단
BP-3: 계시적 언급
BP-4: 성경 해석 주체화
BP-5: 대화 연장 시도
```

### 위기 감지

```python
# Critical (즉시 개입)
["죽고 싶", "자살", "목숨", "살고 싶지 않", ...]

# Warning (주의 관찰)
["희망이 없", "의미가 없", "너무 외로", ...]

# 로직
if critical_keyword: return CRISIS_TEMPLATE
if warning_count >= 2: return CRISIS_TEMPLATE
```

---

## 11. 성공 지표

### 기능적 지표
- **정책 준수율**: ≥99%
- **위기 감지 정확도**: ≥95%
- **스키마 검증 성공률**: 100%
- **Fallback 발생률**: ≤2%

### 성능 지표
- **평균 응답 시간**: ≤2초
- **P95 응답 시간**: ≤3초
- **가용성**: ≥99.5%
- **재시도율**: ≤5%

### 사용자 경험
- **만족도**: ≥4.0/5.0
- **재사용률**: ≥70%
- **위기 리소스 클릭률**: ≥30% (위기 감지 시)

---

## 12. 기술 스택 최종 결정

### 백엔드
```
언어: Python 3.11+
프레임워크: FastAPI
LLM 도구: LangChain
LLM API: OpenAI (gpt-4o-2024-08-06)
검증: Pydantic
DB: Supabase
배포: Docker
모니터링: Prometheus + Grafana
```

### 프론트엔드
```
언어: Kotlin
UI: Jetpack Compose
네트워크: Retrofit
JSON: kotlinx.serialization
DI: Hilt
로컬 DB: Room (선택적)
```

---

## 13. 개발 로드맵

### Phase 1: 백엔드 구축 (Week 1-2)
- [ ] Python 프로젝트 초기화
- [ ] Core Prompt 구현
- [ ] JSON Schema + Pydantic
- [ ] 가드레일 구현
- [ ] LangChain 파이프라인
- [ ] FastAPI 엔드포인트 (10+)
- [ ] Supabase 연동
- [ ] 테스트 작성

### Phase 2: 프론트엔드 수정 (Week 3)
- [ ] BackendApiClient 구현
- [ ] PathwayModels 추가
- [ ] Repository 수정 (API 호출)
- [ ] ViewModel 수정
- [ ] UI 개선 (pathway 기반)
- [ ] 위기 리소스 화면

### Phase 3: 통합 테스트 (Week 4)
- [ ] E2E 테스트
- [ ] 성능 테스트
- [ ] 가드레일 효과성 검증
- [ ] 사용자 시나리오 테스트

### Phase 4: 배포 (Week 5)
- [ ] Docker 이미지 빌드
- [ ] 프로덕션 배포
- [ ] 모니터링 설정
- [ ] 알림 설정
- [ ] 문서화

---

## 14. 핵심 의사결정 요약

| 주제 | 결정 | 이유 |
|------|------|------|
| **아키텍처** | 완전 백엔드 중심 | 보안, 중앙화, 확장성 |
| **백엔드 언어** | Python | LangChain 생태계 |
| **프레임워크** | FastAPI + LangChain | 통제 가능한 파이프라인 |
| **출력 형식** | JSON (Structured) | 파싱 안정성, 검증 용이 |
| **가드레일** | 4단계 계층 | 안전성 보장 |
| **위기 대응** | 서버 템플릿 교체 | LLM에 맡기지 않음 |
| **n8n** | 사용 안 함 | 기능 부족 |
| **Kotlin Backend** | 사용 안 함 | LangChain 없음 |
| **점진적 이동** | 사용 안 함 | 완전 전환 선택 |

---

## 15. 다음 단계

### 즉시 시작
1. Python 백엔드 프로젝트 생성
2. 설계 문서 기반 구현
3. 프론트엔드 수정 계획

### 참조 문서
- [01-core-prompt.md](./01-core-prompt.md)
- [02-json-schema.md](./02-json-schema.md)
- [03-policy-guardrails.md](./03-policy-guardrails.md)
- [04-langchain-pipeline.md](./04-langchain-pipeline.md)
- [README.md](./README.md)

---

**Last Updated**: 2026-01-06
**Status**: 결정 완료, 구현 시작 준비
