# 정책 및 가드레일 설계 문서

## 문서 정보
- 작성일: 2026-01-06
- 버전: 1.0
- 목적: LLM 응답의 안전성과 정책 준수를 보장하는 가드레일 정의

---

## 1. 정책 계층 구조

```
[정책 계층]
├─ Layer 1: 금지 문구 (Forbidden Phrases)
│   └─ 정규식 기반 자동 탐지
├─ Layer 2: 금지 행동 패턴 (Forbidden Behaviors)
│   └─ LLM 프롬프트 + 서버 검증
├─ Layer 3: 위기 감지 (Crisis Detection)
│   └─ 키워드 + LLM 판단 + 템플릿 교체
└─ Layer 4: 성경 정확성 (Bible Accuracy)
    └─ 개역개정 검증 (샘플링)
```

---

## 2. Layer 1: 금지 문구 (Forbidden Phrases)

### 절대 금지 문구 리스트

#### 계시 흉내 금지
```
- "하나님이 당신에게 말씀하셨습니다"
- "하나님께서 당신에게 이렇게 말씀하십니다"
- "주님이 당신에게 보여주시는 것은"
- "성령님께서 당신에게 알려주시는 것은"
- "하나님의 뜻은 당신이 ~하는 것입니다"
```

#### 감정 진단 금지
```
- "당신의 감정은 ~입니다"
- "~를 느끼시는 것 같네요"
- "우울감을 겪고 계신 것 같습니다"
- "불안해하시는군요"
- "화가 나신 것 같습니다"
```

#### 상황 추측 금지
```
- "힘든 일이 있으셨나봐요"
- "~한 상황이신 것 같네요"
- "무슨 일이 있었나요"
- "왜 그렇게 느끼시나요"
- "어떤 일이 있었는지"
```

#### 대화 유도 금지
```
- "더 말씀해 주세요"
- "자세히 말씀해 주실 수 있나요"
- "이야기를 들려주세요"
- "어떻게 된 건지 말해보세요"
- "당신의 생각을 나눠주세요"
```

#### 결론 강요 금지
```
- "당신은 ~해야 합니다"
- "반드시 ~하세요"
- "~하지 않으면 안 됩니다"
- "성경은 ~라고 말합니다" (직접 해석)
- "하나님은 당신이 ~하길 원하십니다"
```

### 정규식 패턴

```python
FORBIDDEN_PATTERNS = [
    r"하나님[이가께서]*.{0,10}(말씀하[시셨습]|알려주[시십]|보여주[시십])",
    r"(당신의 )?감정은",
    r"느끼[시는]+ 것 같",
    r"(힘든|어려운|슬픈) (일|상황)이.{0,5}(있으셨|계신)",
    r"(더|자세히) 말씀해",
    r"(당신은|너는).{0,20}해야[ 합]",
    r"성경[은이].{0,20}말합니다",
]
```

---

## 3. Layer 2: 금지 행동 패턴 (Forbidden Behaviors)

### 행동 패턴 정의

#### BP-1: 상황 추측
**정의**: 사용자의 구체적 상황을 LLM이 임의로 해석하거나 추측
**예시**:
- ❌ "직장에서 힘든 일이 있으셨나봐요"
- ❌ "관계에 어려움을 겪고 계신 것 같습니다"
- ✅ "오늘 하루 수고했습니다" (일반적 응원)

#### BP-2: 감정 진단
**정의**: 사용자의 심리 상태를 분석하거나 진단
**예시**:
- ❌ "우울감을 느끼시는 것 같네요"
- ❌ "불안하신 것 같습니다"
- ✅ "잠시 쉬어가세요" (상태 언급 없이 제안)

#### BP-3: 계시적 언급
**정의**: 하나님의 직접적 메시지인 것처럼 전달
**예시**:
- ❌ "하나님이 당신에게 말씀하셨습니다"
- ❌ "주님께서 이렇게 말씀하십니다"
- ✅ "성경 말씀입니다" (출처만 명시)

#### BP-4: 성경 해석 주체화
**정의**: LLM이 성경을 해석하고 적용의 주체가 됨
**예시**:
- ❌ "이 말씀은 당신에게 ~를 의미합니다"
- ❌ "성경은 이런 상황에서 ~하라고 말합니다"
- ✅ "이 말씀을 드립니다" (적용은 사용자에게)

#### BP-5: 대화 연장 시도
**정의**: 추가 정보를 요구하거나 대화를 이어가려 시도
**예시**:
- ❌ "무슨 일이 있었는지 말씀해 주세요"
- ❌ "더 이야기해 보시겠어요?"
- ✅ (아무 말 없이 pathway 제시)

### 검증 방법

```python
def check_forbidden_behaviors(response: dict) -> list[str]:
    """금지 행동 패턴 검증"""
    violations = []

    text_fields = [
        response.get("brief_connection", ""),
        response.get("prayer_text", ""),
        response.get("silence_guide", ""),
    ]

    full_text = " ".join(text_fields)

    # BP-1: 상황 추측 (키워드 기반)
    situation_keywords = ["~나봐요", "~것 같습니다", "~이신 것 같", "~하셨나"]
    if any(kw in full_text for kw in situation_keywords):
        violations.append("BP-1: 상황 추측 감지")

    # BP-2: 감정 진단
    emotion_keywords = ["감정", "느끼시는", "우울", "불안", "화가"]
    if any(kw in full_text for kw in emotion_keywords):
        violations.append("BP-2: 감정 진단 감지")

    # BP-5: 대화 연장
    conversation_keywords = ["말씀해 주", "이야기해", "나눠주", "?"]
    if any(kw in full_text for kw in conversation_keywords):
        violations.append("BP-5: 대화 연장 시도 감지")

    return violations
```

---

## 4. Layer 3: 위기 감지 (Crisis Detection)

### 위기 키워드

#### Level 1: 즉시 개입 필요 (Critical)
```python
CRISIS_CRITICAL = [
    # 자해/자살
    "죽고 싶", "자살", "목숨", "살고 싶지 않",
    "끝내고 싶", "사라지고 싶", "죽어버리",

    # 타해
    "죽여버리", "해치고 싶", "복수하고 싶",

    # 방법 언급
    "수면제", "칼", "목을 매", "뛰어내리",
]
```

#### Level 2: 주의 관찰 필요 (Warning)
```python
CRISIS_WARNING = [
    "희망이 없", "의미가 없", "아무것도 안 돼",
    "너무 외로", "혼자", "아무도 없",
    "더 이상 못 견디", "한계", "포기",
]
```

### 위기 감지 로직

```python
def detect_crisis(user_input: str) -> tuple[bool, str]:
    """
    위기 감지 함수
    Returns: (is_crisis, level)
    """
    # Critical 키워드 검사
    for keyword in CRISIS_CRITICAL:
        if keyword in user_input:
            return (True, "CRITICAL")

    # Warning 키워드 검사 (2개 이상 동시 출현 시)
    warning_count = sum(1 for kw in CRISIS_WARNING if kw in user_input)
    if warning_count >= 2:
        return (True, "WARNING")

    # LLM 판단 요청 (추가 안전망)
    llm_judgment = ask_llm_crisis_detection(user_input)
    if llm_judgment:
        return (True, "LLM_DETECTED")

    return (False, "NONE")
```

### 위기 응답 템플릿

```json
{
  "crisis_response": {
    "pathway": "word",
    "verse": {
      "book": "시편",
      "chapter": 34,
      "verse": "18",
      "text": "여호와는 마음이 상한 자를 가까이하시고 충심으로 통회하는 자를 구원하시는도다"
    },
    "brief_connection": "혼자 감당하기 어려우시군요.",
    "crisis_detected": true,
    "crisis_resources": [
      {
        "name": "자살예방 상담전화",
        "phone": "1393",
        "available": "24시간"
      },
      {
        "name": "정신건강 위기상담",
        "phone": "1577-0199",
        "available": "24시간"
      },
      {
        "name": "희망의 전화",
        "phone": "129",
        "available": "24시간"
      }
    ]
  }
}
```

**처리 흐름**:
1. 사용자 입력에서 위기 키워드 감지
2. LLM 응답의 `crisis_detected: true` 확인
3. 원래 LLM 응답 폐기
4. 서버 템플릿으로 전체 교체
5. 위기 로그 기록 (익명화)
6. 클라이언트에 전문 상담 리소스 포함 응답

---

## 5. Layer 4: 성경 정확성 (Bible Accuracy)

### 원칙
- **개역개정판** 사용 필수
- **짧게** (1-3절 이내)
- **필요할 때만** (무분별한 인용 금지)

### 검증 방법

#### 자동 검증 (샘플링)
```python
def verify_bible_verse(verse: dict) -> bool:
    """
    성경 구절 정확성 검증 (10% 샘플링)
    """
    if random.random() > 0.1:  # 10% 샘플링
        return True

    # 개역개정 DB 조회
    db_verse = bible_db.get_verse(
        book=verse["book"],
        chapter=verse["chapter"],
        verse=verse["verse"]
    )

    # 텍스트 유사도 (Levenshtein Distance)
    similarity = calculate_similarity(verse["text"], db_verse)

    # 90% 이상 일치 시 통과
    return similarity >= 0.9
```

#### 수동 검토
- 주간 리포트: 인용된 구절 목록
- 이상 패턴 감지 시 알림
- 분기별 전수 검토

---

## 6. 가드레일 파이프라인

### 전체 흐름
```
[사용자 입력]
    ↓
[1단계: 입력 위기 감지]
    ↓ (위기 감지 시 → 템플릿 교체)
[2단계: LLM 호출]
    ↓
[3단계: 스키마 검증] (02-json-schema.md)
    ↓ (실패 시 → Retry 또는 Fallback)
[4단계: 금지 문구 검증] (정규식)
    ↓ (위반 시 → 재생성 또는 차단)
[5단계: 금지 행동 검증] (패턴 분석)
    ↓ (위반 시 → 재생성 또는 차단)
[6단계: 성경 정확성 검증] (샘플링)
    ↓ (실패 시 → 로그 + 경고)
[7단계: crisis_detected 확인]
    ↓ (true 시 → 템플릿 교체)
[클라이언트 응답]
```

### Python 예시 코드

```python
from typing import Optional

def apply_guardrails(
    user_input: str,
    llm_response: dict
) -> tuple[bool, Optional[dict], list[str]]:
    """
    가드레일 파이프라인 적용
    Returns: (is_safe, final_response, violations)
    """
    violations = []

    # 1단계: 입력 위기 감지
    is_crisis, level = detect_crisis(user_input)
    if is_crisis:
        return (True, CRISIS_TEMPLATE, [f"입력 위기 감지: {level}"])

    # 3단계: 스키마 검증 (별도 함수)
    if not validate_schema(llm_response):
        return (False, None, ["스키마 검증 실패"])

    # 4단계: 금지 문구 검증
    text_content = extract_text_fields(llm_response)
    for pattern in FORBIDDEN_PATTERNS:
        if re.search(pattern, text_content):
            violations.append(f"금지 문구 감지: {pattern}")

    # 5단계: 금지 행동 검증
    behavior_violations = check_forbidden_behaviors(llm_response)
    violations.extend(behavior_violations)

    # 6단계: 성경 정확성 검증
    if llm_response.get("verse"):
        if not verify_bible_verse(llm_response["verse"]):
            violations.append("성경 구절 정확성 의심")

    # 7단계: crisis_detected 확인
    if llm_response.get("crisis_detected"):
        return (True, CRISIS_TEMPLATE, ["응답 위기 감지"])

    # 위반 있으면 차단
    if violations:
        return (False, None, violations)

    return (True, llm_response, [])
```

---

## 7. 재시도 전략 (Retry Strategy)

### 재시도 조건
- 스키마 검증 실패
- 금지 문구 감지 (경미한 경우)
- 금지 행동 패턴 감지

### 재시도 로직
```python
MAX_RETRIES = 2

async def generate_with_retry(user_input: str) -> dict:
    """재시도 포함 생성"""
    for attempt in range(MAX_RETRIES + 1):
        # LLM 호출
        llm_response = await call_llm(user_input)

        # 가드레일 적용
        is_safe, final_response, violations = apply_guardrails(
            user_input, llm_response
        )

        if is_safe:
            return final_response

        if attempt < MAX_RETRIES:
            logger.warning(f"재시도 {attempt+1}: {violations}")
            # 프롬프트에 위반 내용 추가
            user_input += f"\n\n[재시도: {violations} 수정 필요]"
        else:
            # 최종 실패 → Fallback
            logger.error(f"최종 실패: {violations}")
            return FALLBACK_SILENCE_RESPONSE
```

---

## 8. 로깅 및 모니터링

### 로그 항목
```python
{
    "timestamp": "2026-01-06T10:30:00Z",
    "user_id": "hashed_user_id",  # 익명화
    "input_length": 15,
    "crisis_detected": false,
    "violations": [],
    "pathway_selected": "word",
    "retry_count": 0,
    "latency_ms": 1200
}
```

### 모니터링 메트릭
- 위기 감지율 (Critical, Warning)
- 정책 위반율 (문구, 행동)
- 재시도 발생률
- Fallback 사용률
- 평균 응답 시간

### 알림 규칙
- Critical 위기 감지 시 즉시 Slack 알림
- 일일 정책 위반 10건 초과 시 리뷰 알림
- Fallback 사용률 5% 초과 시 경고

---

## 9. Fallback 응답

### 기본 Fallback (침묵)
```json
{
  "pathway": "silence",
  "silence_guide": "잠시 조용히 하나님 앞에 머물러 보세요.",
  "brief_connection": "말없이 쉬어도 괜찮습니다.",
  "crisis_detected": false
}
```

### 성경 Fallback (시편 23편)
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

---

## 10. 테스트 케이스

### TC-P1: 금지 문구 감지
- **입력**: "하나님이 당신에게 말씀하셨습니다"
- **기대**: 정규식 매칭, 재생성 또는 차단

### TC-P2: 상황 추측 감지
- **LLM 응답**: "힘든 일이 있으셨나봐요"
- **기대**: BP-1 위반, 재생성

### TC-P3: 위기 감지 (Critical)
- **입력**: "죽고 싶어요"
- **기대**: crisis_detected=true, 템플릿 교체

### TC-P4: 성경 정확성 실패
- **LLM 응답**: verse.text가 개역개정과 불일치
- **기대**: 로그 기록, 경고

### TC-P5: 재시도 성공
- **1차 응답**: 스키마 위반
- **2차 응답**: 정상
- **기대**: 2차 응답 반환

---

## 11. 개정 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-01-06 | 초안 작성 |
