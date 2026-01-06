# JSON 출력 스키마 설계 문서

## 문서 정보
- 작성일: 2026-01-06
- 버전: 1.0
- 목적: LLM 응답의 구조화된 출력 형식 정의

---

## 1. 설계 원칙

### 왜 JSON인가?
- **통제 가능**: XML보다 파싱 안정성 높음
- **스키마 검증**: JSON Schema로 엄격한 검증 가능
- **LLM 친화적**: OpenAI Structured Output 지원
- **확장 가능**: 추후 필드 추가 용이

### 서버 처리 흐름
```
LLM 출력 (JSON)
  ↓
스키마 검증 (JSON Schema)
  ↓
정책 검증 (금칙어, 위기 감지)
  ↓
위기 시 → 서버 템플릿 교체
  ↓
필요 시 → JSON → XML 변환
  ↓
클라이언트 응답
```

---

## 2. JSON Schema (Draft-07)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PathwayResponse",
  "description": "NFC 터치 앱 LLM 응답 스키마",
  "type": "object",
  "required": ["pathway", "brief_connection", "crisis_detected"],
  "properties": {
    "pathway": {
      "type": "string",
      "enum": ["prayer", "word", "silence"],
      "description": "사용자에게 제시할 영적 길"
    },
    "verse": {
      "type": "object",
      "description": "성경 구절 (pathway=word일 때만 필수)",
      "required": ["book", "chapter", "verse", "text"],
      "properties": {
        "book": {
          "type": "string",
          "description": "성경책 이름 (예: 시편, 요한복음)",
          "minLength": 2,
          "maxLength": 20
        },
        "chapter": {
          "type": "integer",
          "description": "장 번호",
          "minimum": 1,
          "maximum": 150
        },
        "verse": {
          "type": "string",
          "description": "절 번호 (예: '1', '1-3')",
          "pattern": "^[0-9]+(-[0-9]+)?$"
        },
        "text": {
          "type": "string",
          "description": "성경 본문 (개역개정판)",
          "minLength": 10,
          "maxLength": 500
        }
      }
    },
    "prayer_text": {
      "type": "string",
      "description": "기도문 (pathway=prayer일 때만 필수)",
      "minLength": 10,
      "maxLength": 200
    },
    "silence_guide": {
      "type": "string",
      "description": "침묵 안내문 (pathway=silence일 때만 필수)",
      "minLength": 10,
      "maxLength": 100
    },
    "brief_connection": {
      "type": "string",
      "description": "짧은 연결 문구 (항상 필수)",
      "minLength": 5,
      "maxLength": 50
    },
    "crisis_detected": {
      "type": "boolean",
      "description": "위기 상황 감지 여부"
    }
  },
  "oneOf": [
    {
      "properties": {
        "pathway": { "const": "word" }
      },
      "required": ["verse"]
    },
    {
      "properties": {
        "pathway": { "const": "prayer" }
      },
      "required": ["prayer_text"]
    },
    {
      "properties": {
        "pathway": { "const": "silence" }
      },
      "required": ["silence_guide"]
    }
  ]
}
```

---

## 3. 필드 상세 설명

### pathway (필수)
- **타입**: enum
- **값**: "prayer", "word", "silence"
- **설명**: 사용자에게 제시할 세 가지 영적 길 중 하나

| 값 | 의미 | 필수 추가 필드 |
|---|------|--------------|
| prayer | 기도 | prayer_text |
| word | 말씀 | verse |
| silence | 침묵 | silence_guide |

### verse (pathway=word 시 필수)
성경 구절 정보 객체

**book** (string, 2-20자)
- 성경책 이름 (한글)
- 예: "시편", "요한복음", "창세기"

**chapter** (integer, 1-150)
- 장 번호
- 예: 23 (시편 23편)

**verse** (string, 패턴: `^[0-9]+(-[0-9]+)?$`)
- 절 번호 (단일 또는 범위)
- 예: "1", "1-3", "16"

**text** (string, 10-500자)
- 성경 본문 (개역개정판)
- 예: "여호와는 나의 목자시니 내게 부족함이 없으리로다"

### prayer_text (pathway=prayer 시 필수)
- **타입**: string (10-200자)
- **설명**: 짧은 기도문
- **원칙**:
  - 사용자 상황 추측 금지
  - "~해주세요" 형태의 간결한 기도
  - 예: "주님, 오늘 하루를 감사합니다. 평안을 주소서."

### silence_guide (pathway=silence 시 필수)
- **타입**: string (10-100자)
- **설명**: 침묵 안내문
- **원칙**:
  - 지시가 아닌 제안
  - 예: "잠시 조용히 하나님 앞에 머물러 보세요."

### brief_connection (항상 필수)
- **타입**: string (5-50자)
- **설명**: 짧은 연결 문구
- **원칙**:
  - 설명하지 않음
  - 캐묻지 않음
  - 50자 엄격히 준수
  - 예: "오늘 하루 수고했습니다.", "잠시 쉬어가세요."

### crisis_detected (항상 필수)
- **타입**: boolean
- **설명**: 위기 상황 감지 여부
- **true 시**: 서버가 전체 응답을 전문 상담 안내 템플릿으로 교체
- **감지 키워드**: 자해, 자살, 타해, 심각한 정신적 위기

---

## 4. 응답 예시

### 예시 1: 말씀 (word)
```json
{
  "pathway": "word",
  "verse": {
    "book": "시편",
    "chapter": 23,
    "verse": "1",
    "text": "여호와는 나의 목자시니 내게 부족함이 없으리로다"
  },
  "brief_connection": "오늘 하루 수고했습니다.",
  "crisis_detected": false
}
```

### 예시 2: 기도 (prayer)
```json
{
  "pathway": "prayer",
  "prayer_text": "주님, 오늘 하루를 감사합니다. 내일도 주님과 함께하게 하소서.",
  "brief_connection": "잠시 기도로 하루를 마무리해 보세요.",
  "crisis_detected": false
}
```

### 예시 3: 침묵 (silence)
```json
{
  "pathway": "silence",
  "silence_guide": "잠시 조용히 하나님 앞에 머물러 보세요.",
  "brief_connection": "말없이 쉬어도 괜찮습니다.",
  "crisis_detected": false
}
```

### 예시 4: 위기 감지
```json
{
  "pathway": "word",
  "verse": {
    "book": "시편",
    "chapter": 23,
    "verse": "1",
    "text": "여호와는 나의 목자시니 내게 부족함이 없으리로다"
  },
  "brief_connection": "혼자 감당하기 어려우시군요.",
  "crisis_detected": true
}
```
→ 서버가 이 응답을 감지하고 전문 상담 안내 템플릿으로 교체

---

## 5. 검증 규칙

### 필수 검증 항목

#### 1단계: 스키마 검증 (JSON Schema)
- [ ] 필수 필드 존재 (pathway, brief_connection, crisis_detected)
- [ ] pathway 값 유효성 (prayer/word/silence)
- [ ] pathway별 필수 필드 존재
- [ ] 문자열 길이 제한 준수
- [ ] 타입 일치 (string, integer, boolean)

#### 2단계: 정책 검증
- [ ] brief_connection 50자 이내
- [ ] 금칙어 미포함 (03-policy.md 참조)
- [ ] 성경 본문 개역개정 확인 (샘플링)
- [ ] 상황 추측 문구 미포함
- [ ] 질문/대화 유도 문구 미포함

#### 3단계: 위기 처리
- [ ] crisis_detected=true 시 템플릿 교체 로직 동작
- [ ] 위기 키워드 감지 시 로그 기록

---

## 6. OpenAI API 연동

### Structured Output 사용
```python
from openai import OpenAI

client = OpenAI()

response = client.chat.completions.create(
    model="gpt-4o-2024-08-06",  # Structured Output 지원 모델
    messages=[
        {"role": "system", "content": CORE_PROMPT},
        {"role": "user", "content": user_input}
    ],
    response_format={
        "type": "json_schema",
        "json_schema": {
            "name": "pathway_response",
            "schema": PATHWAY_SCHEMA,  # 위 JSON Schema
            "strict": True
        }
    }
)

output = json.loads(response.choices[0].message.content)
```

### 장점
- **100% JSON 보장**: LLM이 스키마를 벗어난 응답 불가
- **재시도 불필요**: 파싱 실패 제로
- **타입 안전**: 서버 검증 부담 감소

---

## 7. 에러 처리

### 스키마 검증 실패
```python
try:
    validate(instance=llm_output, schema=PATHWAY_SCHEMA)
except ValidationError as e:
    logger.error(f"Schema validation failed: {e}")
    # Fallback: 기본 침묵 응답
    return DEFAULT_SILENCE_RESPONSE
```

### LLM 응답 실패
```python
try:
    response = client.chat.completions.create(...)
except OpenAIError as e:
    logger.error(f"OpenAI API failed: {e}")
    # Fallback: 기본 말씀 응답
    return DEFAULT_WORD_RESPONSE
```

---

## 8. Kotlin 데이터 모델 (참고)

```kotlin
@Serializable
data class PathwayResponse(
    val pathway: Pathway,
    val verse: Verse? = null,
    @SerialName("prayer_text")
    val prayerText: String? = null,
    @SerialName("silence_guide")
    val silenceGuide: String? = null,
    @SerialName("brief_connection")
    val briefConnection: String,
    @SerialName("crisis_detected")
    val crisisDetected: Boolean
)

@Serializable
enum class Pathway {
    @SerialName("prayer") PRAYER,
    @SerialName("word") WORD,
    @SerialName("silence") SILENCE
}

@Serializable
data class Verse(
    val book: String,
    val chapter: Int,
    val verse: String,
    val text: String
)
```

---

## 9. 테스트 케이스

### TC-1: 말씀 응답 정상
- **입력**: "오늘 힘들었어요"
- **기대 출력**: pathway=word, verse 존재, brief_connection ≤50자

### TC-2: 기도 응답 정상
- **입력**: "감사한 하루였어요"
- **기대 출력**: pathway=prayer, prayer_text 존재

### TC-3: 침묵 응답 정상
- **입력**: "아무 말도 하고 싶지 않아요"
- **기대 출력**: pathway=silence, silence_guide 존재

### TC-4: 위기 감지
- **입력**: "더 이상 살고 싶지 않아요"
- **기대 출력**: crisis_detected=true, 서버 템플릿 교체

### TC-5: 스키마 위반
- **조작된 LLM 출력**: pathway 누락
- **기대 처리**: 검증 실패 → Fallback 응답

---

## 10. 개정 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-01-06 | 초안 작성 |
