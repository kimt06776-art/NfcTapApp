# LangChain 파이프라인 설계 문서

## 문서 정보
- 작성일: 2026-01-06
- 버전: 1.0
- 목적: LangChain 기반 통제 가능한 LLM 파이프라인 설계

---

## 1. LangChain을 쓰는 이유

### 목표: LLM을 '통제되는 엔진'으로 운영

**NOT**: 편의성 도구
**YES**: 운영 통제 파이프라인

### 핵심 요구사항
1. **프롬프트 조합**: Core(불변) + Mode(가변) + Policy + Schema
2. **구조화된 출력**: Structured Output Parser
3. **검증 및 재시도**: Retry with Error Parser
4. **정책 가드레일**: Custom Guardrail Chain
5. **라우팅**: 필요 시 다중 체인 라우팅 (추후)

### LangChain의 역할
```
[단순 LLM 호출]
입력 → LLM → 자유 텍스트 → (통제 불가)

[LangChain 파이프라인]
입력
  → 프롬프트 조합 (PromptTemplate)
  → LLM
  → 출력 파싱 (StructuredOutputParser)
  → 검증 (Guardrail)
  → 실패 시 재시도 (RetryChain)
  → 통제된 응답
```

---

## 2. 파이프라인 아키텍처

### 전체 구조
```
┌─────────────────────────────────────────────────────────────┐
│                        FastAPI 서버                          │
├─────────────────────────────────────────────────────────────┤
│  POST /api/pathway                                          │
│    ├─ Request: { user_input, user_id, session_id }        │
│    └─ Response: PathwayResponse (JSON)                     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                   LangChain Pipeline                         │
├─────────────────────────────────────────────────────────────┤
│  1️⃣ Input Validation                                        │
│     └─ 위기 키워드 감지 → 템플릿 교체                       │
│                                                              │
│  2️⃣ Prompt Assembly                                         │
│     ├─ CorePromptTemplate (불변)                            │
│     ├─ ModePromptTemplate (가변, 추후)                      │
│     ├─ PolicyTemplate (정책 메시지)                         │
│     └─ SchemaInstructionTemplate (출력 형식)                │
│                                                              │
│  3️⃣ LLM Call (OpenAI)                                       │
│     └─ gpt-4o-2024-08-06 (Structured Output)               │
│                                                              │
│  4️⃣ Output Parsing                                          │
│     └─ PydanticOutputParser<PathwayResponse>                │
│                                                              │
│  5️⃣ Guardrail Validation                                    │
│     ├─ 스키마 검증                                           │
│     ├─ 금지 문구 검사                                        │
│     ├─ 금지 행동 검사                                        │
│     ├─ 성경 정확성 검사 (샘플링)                            │
│     └─ crisis_detected 확인                                 │
│                                                              │
│  6️⃣ Retry Logic (실패 시)                                   │
│     └─ RetryOutputParser (최대 2회)                         │
│                                                              │
│  7️⃣ Fallback (최종 실패 시)                                 │
│     └─ 기본 응답 반환                                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    응답 후처리                               │
├─────────────────────────────────────────────────────────────┤
│  - 로깅 (Supabase 또는 별도 DB)                             │
│  - 모니터링 메트릭 전송                                      │
│  - (옵션) JSON → XML 변환                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 구성 요소 상세 설계

### 3.1 Prompt Templates

#### CorePromptTemplate
```python
from langchain.prompts import PromptTemplate

CORE_PROMPT = """당신은 조용히 곁에 서 주는 영적 동행 도구입니다.

## 당신의 역할
사용자가 '지금 하나님이 필요하다'고 선택한 순간에,
말이 많지 않은, 아주 작은 길 하나를 제시합니다.

## 하지 않는 것 (절대 금지)
- 사용자의 상황을 추측하거나 감정 상태를 진단하지 않습니다
- "하나님이 당신에게 말씀하셨습니다" 같은 계시 흉내를 내지 않습니다
- 성경을 정죄, 압박, 결론 도출의 도구로 쓰지 않습니다
- 설명하거나, 캐묻거나, 대화를 이어가려 하지 않습니다
- 사용자의 이야기를 요구하지 않습니다

## 하는 것
사용자가 선택할 수 있는 아주 작은 길 하나를 제시합니다:
- **기도** (prayer): 짧은 기도문 제시
- **말씀** (word): 성경 한 구절 (개역개정) 제시
- **침묵** (silence): 조용히 하나님 앞에 머물 것을 제안

## 성경 인용 원칙
- 개역개정판 사용
- 짧게 (1-3절 이내)
- 필요할 때만
- 적용은 제안이지 결론이 아닙니다
- 해석의 주체를 AI로 만들지 않습니다

## 응답 원칙
- 한 발 물러서서, 사용자가 하나님과 직접 마주하도록 합니다
- 50자 이내의 아주 짧은 연결 문구만 제공합니다
- 사용자에게 질문하거나 추가 대화를 유도하지 않습니다
"""

core_template = PromptTemplate(
    input_variables=[],
    template=CORE_PROMPT
)
```

#### PolicyTemplate
```python
POLICY_PROMPT = """
## 위기 상황 처리
다음 상황이 감지되면 응답하지 말고 crisis_detected: true로 표시하세요:
- 자해, 자살 관련 언급
- 타인에 대한 위해 의도
- 심각한 정신적 위기 상황

## 금칙어 (응답에 절대 포함 금지)
- "하나님이 당신에게 말씀하셨습니다"
- "당신의 감정은 ~입니다"
- "왜 그렇게 느끼시나요?"
- "더 말씀해 주세요"
- "당신은 ~해야 합니다"
- "성경은 ~라고 말합니다" (직접 해석 금지)
"""

policy_template = PromptTemplate(
    input_variables=[],
    template=POLICY_PROMPT
)
```

#### SchemaInstructionTemplate
```python
SCHEMA_INSTRUCTION = """
## 출력 형식
다음 JSON 스키마를 엄격히 준수하세요:

{{
  "pathway": "prayer | word | silence",
  "verse": {{  // pathway=word 시에만 필수
    "book": "string",
    "chapter": integer,
    "verse": "string",
    "text": "string (개역개정)"
  }},
  "prayer_text": "string",  // pathway=prayer 시에만 필수
  "silence_guide": "string",  // pathway=silence 시에만 필수
  "brief_connection": "string (max 50자)",  // 항상 필수
  "crisis_detected": boolean  // 항상 필수
}}

{format_instructions}
"""

schema_template = PromptTemplate(
    input_variables=["format_instructions"],
    template=SCHEMA_INSTRUCTION
)
```

#### 최종 프롬프트 조합
```python
from langchain.prompts import ChatPromptTemplate

final_prompt = ChatPromptTemplate.from_messages([
    ("system", CORE_PROMPT),
    ("system", POLICY_PROMPT),
    ("system", SCHEMA_INSTRUCTION),
    ("user", "{user_input}")
])
```

---

### 3.2 Structured Output Parser

#### Pydantic 모델
```python
from pydantic import BaseModel, Field, field_validator
from typing import Optional, Literal

class Verse(BaseModel):
    book: str = Field(..., min_length=2, max_length=20)
    chapter: int = Field(..., ge=1, le=150)
    verse: str = Field(..., pattern=r"^[0-9]+(-[0-9]+)?$")
    text: str = Field(..., min_length=10, max_length=500)

class PathwayResponse(BaseModel):
    pathway: Literal["prayer", "word", "silence"]
    verse: Optional[Verse] = None
    prayer_text: Optional[str] = Field(None, min_length=10, max_length=200)
    silence_guide: Optional[str] = Field(None, min_length=10, max_length=100)
    brief_connection: str = Field(..., min_length=5, max_length=50)
    crisis_detected: bool

    @field_validator("verse")
    def verse_required_for_word(cls, v, info):
        if info.data.get("pathway") == "word" and v is None:
            raise ValueError("verse is required when pathway=word")
        return v

    @field_validator("prayer_text")
    def prayer_required_for_prayer(cls, v, info):
        if info.data.get("pathway") == "prayer" and v is None:
            raise ValueError("prayer_text is required when pathway=prayer")
        return v

    @field_validator("silence_guide")
    def silence_required_for_silence(cls, v, info):
        if info.data.get("pathway") == "silence" and v is None:
            raise ValueError("silence_guide is required when pathway=silence")
        return v
```

#### Parser 생성
```python
from langchain.output_parsers import PydanticOutputParser

output_parser = PydanticOutputParser(pydantic_object=PathwayResponse)
```

---

### 3.3 Guardrail Chain

#### 커스텀 Guardrail Runnable
```python
from langchain.schema.runnable import Runnable
from typing import Any
import re

class GuardrailChain(Runnable):
    def __init__(self):
        self.forbidden_patterns = [
            r"하나님[이가께서]*.{0,10}(말씀하[시셨습]|알려주[시십]|보여주[시십])",
            r"(당신의 )?감정은",
            r"느끼[시는]+ 것 같",
            # ... (03-policy-guardrails.md 참조)
        ]

    def invoke(self, input: PathwayResponse, config=None) -> dict:
        """
        가드레일 검증
        Returns: {"is_safe": bool, "response": dict, "violations": list}
        """
        violations = []

        # 금지 문구 검증
        text_fields = [
            input.brief_connection,
            input.prayer_text or "",
            input.silence_guide or "",
        ]
        full_text = " ".join(text_fields)

        for pattern in self.forbidden_patterns:
            if re.search(pattern, full_text):
                violations.append(f"금지 문구 감지: {pattern}")

        # 금지 행동 패턴 검증
        if any(kw in full_text for kw in ["~나봐요", "~것 같습니다"]):
            violations.append("BP-1: 상황 추측 감지")

        # crisis_detected 확인
        if input.crisis_detected:
            return {
                "is_safe": True,
                "response": CRISIS_TEMPLATE,
                "violations": ["위기 감지됨"]
            }

        # 위반 있으면 차단
        if violations:
            return {
                "is_safe": False,
                "response": None,
                "violations": violations
            }

        return {
            "is_safe": True,
            "response": input.dict(),
            "violations": []
        }

guardrail_chain = GuardrailChain()
```

---

### 3.4 Retry Chain

```python
from langchain.chains import LLMChain
from langchain.output_parsers import RetryWithErrorOutputParser

# 기본 체인
base_chain = LLMChain(
    llm=llm,
    prompt=final_prompt,
    output_parser=output_parser
)

# Retry 체인
retry_parser = RetryWithErrorOutputParser.from_llm(
    parser=output_parser,
    llm=llm,
    max_retries=2
)

def invoke_with_retry(user_input: str) -> PathwayResponse:
    """재시도 포함 호출"""
    for attempt in range(3):
        try:
            # LLM 호출
            result = base_chain.invoke({"user_input": user_input})

            # 가드레일 검증
            guardrail_result = guardrail_chain.invoke(result)

            if guardrail_result["is_safe"]:
                return guardrail_result["response"]

            # 위반 시 재시도
            if attempt < 2:
                user_input += f"\n\n[수정 필요: {guardrail_result['violations']}]"
                continue
            else:
                # 최종 실패 → Fallback
                return FALLBACK_RESPONSE

        except Exception as e:
            logger.error(f"LLM 호출 실패: {e}")
            if attempt < 2:
                continue
            return FALLBACK_RESPONSE
```

---

### 3.5 완전한 파이프라인 (LCEL)

```python
from langchain.schema.runnable import RunnablePassthrough, RunnableLambda

# 1. 입력 위기 감지
def check_input_crisis(input_dict: dict) -> dict:
    user_input = input_dict["user_input"]
    is_crisis, level = detect_crisis(user_input)
    if is_crisis:
        return {"crisis": True, "response": CRISIS_TEMPLATE}
    return {"crisis": False, "user_input": user_input}

# 2. LLM 체인
llm_chain = (
    final_prompt
    | llm
    | output_parser
)

# 3. 가드레일 체인
def apply_guardrail(response: PathwayResponse) -> dict:
    return guardrail_chain.invoke(response)

# 4. 최종 파이프라인
pathway_pipeline = (
    RunnablePassthrough()
    | RunnableLambda(check_input_crisis)
    | RunnableLambda(lambda x: x if x.get("crisis") else llm_chain.invoke(x))
    | RunnableLambda(lambda x: x if isinstance(x, dict) and x.get("crisis") else apply_guardrail(x))
)

# 사용 예시
result = pathway_pipeline.invoke({"user_input": "오늘 힘들었어요"})
```

---

## 4. FastAPI 서버 구현

### 4.1 서버 구조
```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI 앱
│   ├── config.py               # 설정
│   ├── models.py               # Pydantic 모델
│   ├── chains/
│   │   ├── __init__.py
│   │   ├── prompts.py          # 프롬프트 템플릿
│   │   ├── parsers.py          # Output Parser
│   │   ├── guardrails.py       # Guardrail Chain
│   │   └── pipeline.py         # 최종 파이프라인
│   ├── services/
│   │   ├── crisis_detection.py # 위기 감지
│   │   └── bible_service.py    # 성경 검증
│   ├── utils/
│   │   ├── logging.py
│   │   └── monitoring.py
│   └── routers/
│       └── pathway.py          # API 라우터
├── tests/
├── requirements.txt
└── .env
```

### 4.2 FastAPI 엔드포인트

```python
# app/main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from app.chains.pipeline import pathway_pipeline
from app.utils.logging import log_request

app = FastAPI(title="NFC Pathway API")

class PathwayRequest(BaseModel):
    user_input: str
    user_id: str
    session_id: str | None = None

@app.post("/api/pathway")
async def generate_pathway(request: PathwayRequest):
    try:
        # 파이프라인 실행
        result = pathway_pipeline.invoke({
            "user_input": request.user_input
        })

        # 로깅
        log_request(
            user_id=request.user_id,
            input_text=request.user_input,
            response=result,
            session_id=request.session_id
        )

        return result

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "healthy"}
```

---

## 5. 배포 및 운영

### 5.1 환경 변수
```bash
# .env
OPENAI_API_KEY=sk-...
SUPABASE_URL=https://...
SUPABASE_KEY=...
ENVIRONMENT=production
LOG_LEVEL=INFO
MAX_RETRIES=2
```

### 5.2 Docker 배포
```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 5.3 모니터링
```python
# app/utils/monitoring.py
from prometheus_client import Counter, Histogram

# 메트릭 정의
pathway_requests = Counter(
    "pathway_requests_total",
    "Total pathway requests",
    ["pathway_type", "status"]
)

crisis_detections = Counter(
    "crisis_detections_total",
    "Total crisis detections",
    ["level"]
)

guardrail_violations = Counter(
    "guardrail_violations_total",
    "Total guardrail violations",
    ["violation_type"]
)

response_latency = Histogram(
    "pathway_response_latency_seconds",
    "Response latency in seconds"
)
```

---

## 6. 테스트 전략

### 6.1 단위 테스트
```python
# tests/test_guardrails.py
def test_forbidden_phrase_detection():
    response = PathwayResponse(
        pathway="word",
        verse=Verse(...),
        brief_connection="하나님이 당신에게 말씀하셨습니다",
        crisis_detected=False
    )

    result = guardrail_chain.invoke(response)
    assert not result["is_safe"]
    assert "금지 문구 감지" in result["violations"][0]
```

### 6.2 통합 테스트
```python
# tests/test_pipeline.py
def test_full_pipeline():
    result = pathway_pipeline.invoke({
        "user_input": "오늘 힘들었어요"
    })

    assert result["pathway"] in ["prayer", "word", "silence"]
    assert len(result["brief_connection"]) <= 50
    assert not result["crisis_detected"]
```

### 6.3 부하 테스트
```python
# tests/load_test.py
from locust import HttpUser, task

class PathwayUser(HttpUser):
    @task
    def generate_pathway(self):
        self.client.post("/api/pathway", json={
            "user_input": "오늘 힘들었어요",
            "user_id": "test_user"
        })
```

---

## 7. 성능 최적화

### 7.1 캐싱 전략
```python
from functools import lru_cache

@lru_cache(maxsize=100)
def get_bible_verse(book: str, chapter: int, verse: str) -> str:
    """성경 구절 캐싱"""
    return bible_db.query(book, chapter, verse)
```

### 7.2 비동기 처리
```python
import asyncio

async def generate_pathway_async(user_input: str) -> dict:
    """비동기 파이프라인"""
    result = await asyncio.to_thread(
        pathway_pipeline.invoke,
        {"user_input": user_input}
    )
    return result
```

---

## 8. 향후 확장 계획

### 8.1 Mode Prompt 추가
```python
MODE_TEMPLATES = {
    "morning": "새벽 모드: 하루를 시작하는 기도",
    "night": "밤 모드: 하루를 마무리하는 감사",
}

def get_mode_prompt(mode: str) -> str:
    return MODE_TEMPLATES.get(mode, "")
```

### 8.2 라우팅 체인
```python
from langchain.chains.router import MultiPromptChain

router_chain = MultiPromptChain(
    router_chain=...,
    destination_chains={
        "prayer": prayer_chain,
        "word": word_chain,
        "silence": silence_chain,
    }
)
```

---

## 9. 개정 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-01-06 | 초안 작성 |
