# Room Database Schema Files

이 디렉토리에는 Room Database의 스키마 변경 이력이 자동으로 저장됩니다.

## 중요 사항

- **절대 삭제하지 마세요**: 이 파일들은 데이터베이스 마이그레이션 추적에 필수적입니다.
- **Git에 커밋하세요**: 스키마 파일은 버전 관리에 포함되어야 합니다.
- **자동 생성됨**: 빌드 시 KSP가 자동으로 생성합니다.

## 파일 구조

```
schemas/
  └── kr.sweetapps.alcoholictimer.data.room.AppDatabase/
      ├── 1.json  (초기 버전)
      ├── 2.json  (imageUrl 추가)
      ├── 3.json  (tagType 추가)
      └── 4.json  (sharedPostId 추가)
```

## 용도

1. **마이그레이션 검증**: Room이 스키마 변경을 자동으로 검증합니다.
2. **문서화**: 데이터베이스 구조의 변경 이력을 추적합니다.
3. **디버깅**: 마이그레이션 문제 발생 시 참고 자료로 활용합니다.

## 더 알아보기

- [Room Migration Testing](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schema)

