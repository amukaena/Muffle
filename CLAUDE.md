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
