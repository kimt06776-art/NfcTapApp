# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

NFC 터치 기반 스마트 복음 콘텐츠 플랫폼 Android 앱. NFC 태그를 터치하면 자동으로 오늘의 말씀, 설교, 교회 공지 등 신앙 콘텐츠가 실행되는 "Zero-Click" 접근 방식을 구현한다.

## 빌드 및 실행 명령어

```bash
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

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **최소 SDK**: 24 (Android 7.0)
- **타겟 SDK**: 36
- **빌드 시스템**: Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`)

## 아키텍처

현재 단일 Activity 구조로, 향후 확장 시 다음 구조 권장:
- `MainActivity`: NFC 인텐트 처리 및 앱 진입점
- `ui/theme/`: Compose 테마 정의 (Color, Type, Theme)
- NFC 태그 감지: `TAG_DISCOVERED`, `NDEF_DISCOVERED` 인텐트 필터 설정됨

## NFC 관련 참고사항

- `AndroidManifest.xml`에 NFC 권한 및 intent-filter 이미 설정됨
- `android.hardware.nfc` feature는 `required="false"`로 설정 (NFC 없는 기기도 설치 가능)
- `MainActivity`는 `singleTask` 런치 모드 사용 (NFC 태그 재터치 시 기존 인스턴스 재사용)

## 주요 Composable 컴포넌트

- `HomeScreen`: 메인 화면 (말씀, 설교, 공지 카드 표시)
- `CardBlock`: 재사용 가능한 콘텐츠 카드 컴포넌트
