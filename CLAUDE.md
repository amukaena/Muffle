# Muffle 프로젝트

## 필수 제약사항

### GitHub 저장소 정보
- **저장소 URL**: https://github.com/amukaena/Muffle.git
- **GitHub 계정**: amukaena (amu7517@gmail.com)
- **주의**: 이 PC의 글로벌 Git 계정(amu7517@cubox.ai / LimByeongDo)과 다름

### GitHub 계정 분리 설정
이 PC는 여러 GitHub 계정을 저장소별로 분리 사용:
```bash
# 글로벌 설정 (이미 적용됨)
git config --global credential.useHttpPath true
```
- 이 설정으로 저장소 URL별로 자격 증명이 분리 저장됨
- Muffle → amukaena 계정
- 다른 프로젝트 → LimByeongDo 계정

### Git 로컬 설정 (이 저장소 전용)
```bash
git config user.email "amu7517@gmail.com"
git config user.name "amu7517"
```
- commit/push 전에 `git config user.email` 로 확인 권장

## 프로젝트 개요
- Android 수면용 브라운 노이즈 앱 (Kotlin + Jetpack Compose)
- 옆집 소음을 덮기 위한 브라운 노이즈 무한 재생
- 정해진 시간(타이머)까지 재생 후 자동 종료

## 주요 기능
- **브라운 노이즈 재생**: 44100Hz PCM 16-bit 모노, DC drift 방지 (leakFactor: 0.998)
- **타이머 종료**: 설정한 시각에 자동 정지 (다음날 자동 계산)
- **설정 영구 저장**: SharedPreferences(`muffle_settings`)로 종료 시각, 볼륨 저장
- **세밀한 볼륨 조절**: 슬라이더 + 업/다운 버튼 (5%씩 증감), 퍼센트 표시
- **포그라운드 서비스**: 알림 채널을 통한 백그라운드 재생

## 아키텍처
```
com.muffle/
├── MainActivity.kt          # 진입점
├── MuffleApplication.kt     # 알림 채널 생성
├── audio/
│   └── BrownNoiseGenerator.kt  # 브라운 노이즈 오디오 생성
├── service/
│   └── NoisePlaybackService.kt # 포그라운드 서비스 (재생/타이머)
├── ui/
│   ├── screen/
│   │   └── MainScreen.kt       # 메인 UI (Compose)
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt
└── viewmodel/
    └── MainViewModel.kt        # AndroidViewModel + SharedPreferences
```

## 기술 스택
- **언어**: Kotlin
- **UI**: Jetpack Compose + Material3
- **상태 관리**: StateFlow + AndroidViewModel
- **영속성**: SharedPreferences (`muffle_settings`)
- **빌드**: Gradle 8.5, Min SDK 26, Target SDK 34
