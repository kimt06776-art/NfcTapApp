# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

NFC 터치 기반 스마트 복음 콘텐츠 플랫폼. NFC 태그를 터치하면 자동으로 오늘의 말씀, 설교, 교회 공지 등 신앙 콘텐츠가 실행되는 "Zero-Click" 접근 방식을 구현한다.

**Monorepo 구조**: Frontend (Android 앱)와 Backend (FastAPI 서버)로 분리된 풀스택 프로젝트

## 프로젝트 구조

```
NfcTapApp/
├── frontend/           # Android 앱 (Kotlin + Jetpack Compose)
│   ├── app/src/
│   │   ├── main/java/com/example/nfctapapp/
│   │   │   ├── data/          # Repository, API 클라이언트
│   │   │   ├── di/            # Hilt 의존성 주입
│   │   │   ├── ui/            # Compose UI 컴포넌트
│   │   │   └── MainActivity.kt
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── local.properties
│
├── backend/            # FastAPI 서버 (Python)
│   ├── app/
│   │   ├── routers/       # API 엔드포인트
│   │   ├── repositories/  # 데이터 액세스
│   │   ├── services/      # 비즈니스 로직
│   │   ├── chains/        # LangChain 파이프라인
│   │   ├── models.py      # Pydantic 모델
│   │   ├── config.py      # 환경 설정
│   │   └── main.py        # FastAPI 앱
│   ├── requirements.txt
│   └── .env
│
└── CLAUDE.md
```

## 기술 스택

### Frontend (Android)
- **언어**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **최소 SDK**: 24 (Android 7.0)
- **타겟 SDK**: 36
- **빌드 시스템**: Gradle Kotlin DSL + Version Catalog
- **의존성 주입**: Hilt
- **HTTP 클라이언트**: Retrofit + Moshi
- **비동기**: Kotlin Coroutines + Flow

### Backend (Python)
- **프레임워크**: FastAPI
- **언어**: Python 3.10+
- **LLM**: LangChain + OpenAI
- **데이터베이스**: Supabase (PostgreSQL)
- **환경 관리**: python-dotenv
- **스트리밍**: Server-Sent Events (SSE)

## 빌드 및 실행 명령어

### Frontend (Android)

```bash
cd frontend

# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 앱 설치 (연결된 디바이스/에뮬레이터)
./gradlew installDebug

# 유닛 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.example.nfctapapp.ExampleUnitTest"

# 인스트루먼트 테스트 (디바이스 필요)
./gradlew connectedAndroidTest

# 프로젝트 클린
./gradlew clean
```

### Backend (Python)

```bash
cd backend

# 가상환경 생성 (최초 1회)
python -m venv venv

# 가상환경 활성화
# Windows:
venv\Scripts\activate
# macOS/Linux:
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt

# 개발 서버 실행 (hot-reload)
uvicorn app.main:app --reload --port 8000

# 프로덕션 서버 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000

# API 문서 확인
# http://localhost:8000/docs (Swagger UI)
# http://localhost:8000/redoc (ReDoc)
```

## 아키텍처

### Frontend Architecture (Clean Architecture + MVVM)

```
UI Layer (Compose)
    ↓
ViewModel (StateFlow)
    ↓
Repository (interface)
    ↓
API Service (Retrofit) → Backend API
```

**주요 컴포넌트:**
- `MainActivity`: NFC 인텐트 처리 및 앱 진입점
- `ui/`: Compose UI 컴포넌트 (HomeScreen, SermonScreen, ChatScreen 등)
- `data/repository/`: 데이터 소스 추상화
- `data/remote/api/`: Retrofit API 인터페이스 및 DTO
- `di/`: Hilt 모듈 (ApiClient, AppModule)

**NFC 처리:**
- `TAG_DISCOVERED`, `NDEF_DISCOVERED` 인텐트 필터 설정
- `android.hardware.nfc` feature는 `required="false"` (NFC 없는 기기도 설치 가능)
- `MainActivity`는 `singleTask` 런치 모드 사용 (재터치 시 기존 인스턴스 재사용)

### Backend Architecture (Layered Architecture)

```
Router Layer (FastAPI endpoints)
    ↓
Service Layer (Business logic)
    ↓
Repository Layer (Data access)
    ↓
Database (Supabase PostgreSQL)
```

**주요 컴포넌트:**
- `routers/`: API 엔드포인트 (pathway, chat, auth, sermon)
- `services/`: OpenAI 서비스 (채팅 스트리밍)
- `repositories/`: Supabase 데이터 액세스
- `chains/`: LangChain 파이프라인 (prompts, parsers, guardrails)
- `models.py`: 공유 Pydantic 모델 (Request/Response DTO)

## API 엔드포인트

### Pathway (영적 안내)
- `POST /api/pathway` - 사용자 입력 기반 영적 안내 생성 (LangChain)

### Chat (AI 상담)
- `POST /api/chat/stream` - 채팅 스트리밍 (SSE)
- `GET /api/chat/sessions?userId={userId}` - 세션 목록 조회
- `POST /api/chat/sessions` - 새 세션 생성
- `PATCH /api/chat/sessions/{sessionId}` - 세션 제목 수정
- `DELETE /api/chat/sessions/{sessionId}` - 세션 삭제
- `GET /api/chat/sessions/{sessionId}/messages` - 메시지 목록 조회
- `POST /api/chat/messages` - 메시지 저장

### Auth (NFC 인증)
- `POST /api/auth/nfc` - NFC 태그 인증
- `POST /api/auth/register` - 사용자 등록
- `POST /api/auth/validate` - 캐시된 사용자 검증

### Sermon (설교)
- `GET /api/sermons` - 전체 설교 목록
- `GET /api/sermons/latest` - 최신 설교
- `GET /api/sermons/{sermonId}` - 설교 상세

### Health Check
- `GET /` - 서비스 상태
- `GET /health` - 헬스 체크 (LLM 파이프라인 포함)

## 보안 및 설정

### API 키 관리
**✅ 안전한 방식 (현재):**
- Backend `.env` 파일에 API 키 저장
- Frontend는 Backend API만 호출
- API 키가 APK에 포함되지 않음

**❌ 위험한 방식 (이전):**
- Frontend `local.properties`에 API 키 저장
- APK 디컴파일 시 키 노출 위험

### 환경 변수 설정

**Backend `.env`:**
```env
# OpenAI
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-2024-08-06

# Supabase
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=eyJ...

# Environment
ENVIRONMENT=development
LOG_LEVEL=INFO
```

**Frontend `local.properties`:**
```properties
# Android SDK 경로만 저장
sdk.dir=C:\\Users\\...\\Android\\Sdk

# API 키는 저장하지 않음 (보안)
```

## 개발 워크플로우

### 1. Backend API 개발
1. `backend/app/models.py`에 Request/Response 모델 추가
2. `backend/app/repositories/`에 데이터 액세스 로직 추가
3. `backend/app/routers/`에 API 엔드포인트 추가
4. `backend/app/main.py`에 라우터 등록
5. `http://localhost:8000/docs`에서 테스트

### 2. Frontend 연동
1. `frontend/.../data/remote/api/ApiModels.kt`에 DTO 추가
2. `frontend/.../data/remote/api/ApiService.kt`에 API 메서드 추가
3. `frontend/.../data/repository/`에 Repository 구현
4. `frontend/.../ui/`에서 ViewModel 통해 호출
5. Compose UI에서 상태 구독

### 3. 테스트
- Backend: `curl` 또는 Swagger UI로 API 테스트
- Frontend: Android Studio 에뮬레이터로 실행
- 통합: Backend 실행 상태에서 Frontend 앱 실행

## 주의사항

### Supabase 직접 접근 제거됨
- ❌ Frontend에서 `SupabaseClient` 직접 사용 (삭제됨)
- ✅ Backend API를 통한 간접 접근만 허용

### 의존성
- Frontend: Supabase 라이브러리 제거됨 (Retrofit만 사용)
- Backend: Supabase Python 클라이언트 사용

### CORS 설정
- 개발 환경: `allow_origins=["*"]`
- 프로덕션: 특정 도메인으로 제한 필요

### 로깅
- Backend: ERROR/WARNING만 출력 (INFO/DEBUG 비활성화)
- httpcore, httpx, openai, uvicorn.access 로그 최소화

## 디자인 시스템

### 색상 가이드
**UI 색상 결정 시 반드시 `colors.md` 파일을 참고할 것.**

핵심 원칙:
- 색상은 감정을 자극하지 않고 **공간을 만든다**
- 순백색(`#FFFFFF`) 사용 금지 → `#F5F4F2` 사용
- 고채도 색상, 감정적 색상 금지

주요 색상:
| 용도 | 색상명 | Hex |
|------|--------|-----|
| 배경 | Stone Gray | `#7B7A77` |
| 깊이/하단 | Deep Stone | `#4F4E4B` |
| 미세 악센트 | Hidden Warm | `#9A8F7A` |
| 주 텍스트 | Primary Text | `#F5F4F2` |
| 보조 텍스트 | Secondary Text | `#D8D6D2` |
| UI 텍스트 | Tertiary Text | `#C1BFBB` |

## 참고 문서

- **Color System**: `colors.md` (필수 참고)
- **Android NFC Guide**: https://developer.android.com/guide/topics/connectivity/nfc
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **FastAPI**: https://fastapi.tiangolo.com/
- **LangChain**: https://python.langchain.com/
- **Supabase**: https://supabase.com/docs
