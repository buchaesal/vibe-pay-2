# 프로그래밍 일반 표준

> X2BEE 프로젝트에서 사용되는 프로그래밍 일반 표준입니다.
> 모든 개발자는 이 가이드라인을 따라 일관된 코드 스타일을 유지해야 합니다.

## 목차

1. [소스 파일 작성 규칙](#1-소스-파일-작성-규칙)
2. [소스 파일 구조](#2-소스-파일-구조)
3. [코드 형식](#3-코드-형식)
4. [네이밍 규칙](#4-네이밍-규칙)
5. [주석 작성](#5-주석-작성)

---

## 1. 소스 파일 작성 규칙

### 1.1 인코딩

**모든 소스 파일은 UTF-8로 인코딩해야 합니다.**

### 1.2 포매팅

각 개발 도구에 내장된 포매터를 사용하여 소스를 포매팅합니다.

| IDE           | 포매팅 방법                       |
| ------------- | --------------------------------- |
| IntelliJ IDEA | Code > Reformat Code (Ctrl+Alt+L) |
| Eclipse       | Source > Format (Ctrl+Shift+F)    |
| VS Code       | Format Document (Shift+Alt+F)     |

---

## 2. 소스 파일 구조

Java 소스 파일은 아래의 순서로 구성하며, **각 섹션 사이는 한 줄씩 띄웁니다.**

### 2.1 기본 구조

```java
// 1. 패키지 선언
package com.x2bee.api.display.app.controller.category;

// 2. Import 문
import com.x2bee.common.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// 3. 클래스 선언 및 구현
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    // 클래스 내부 구성...
}
```

### 2.2 패키지 선언

- 해당 클래스가 위치한 패키지를 명시합니다.
- **default 패키지 사용은 금지됩니다.**

### 2.3 Import 문

- 개발 도구에서 제공하는 기능을 활용하여 import문을 정렬합니다.
- **static import는 금지되며, 반드시 최종 클래스까지 명시해야 합니다.**

| IDE           | Import 정리 방법                                      |
| ------------- | ----------------------------------------------------- |
| IntelliJ IDEA | Code > Optimize Imports                               |
| Eclipse       | Source > Organize Imports                             |
| VS Code       | settings.json에 `"source.organizeImports": true` 추가 |

❌ **잘못된 예시**:

```java
import static com.x2bee.common.Constants.*;  // static import 금지
```

✅ **올바른 예시**:

```java
import com.x2bee.common.Constants;
```

### 2.4 클래스 내부 구성 순서

다음 순서로 구성하는 것을 권장합니다 (필수 아님):

1. static fields
2. normal fields
3. constructors
4. (private) methods called from constructors
5. static factory methods
6. JavaBean properties (getters and setters)
7. method implementations from interfaces
8. private or protected template methods
9. other methods
10. equals, hashCode, toString

**예시**:

```java
public class CategoryService {
    // 1. static fields
    private static final String DEFAULT_CATEGORY = "ALL";

    // 2. normal fields
    private final CategoryMapper categoryMapper;
    private String categoryName;

    // 3. constructors
    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    // 6. getters/setters
    public String getCategoryName() {
        return categoryName;
    }

    // 7. interface methods
    @Override
    public Category getById(Long id) {
        return categoryMapper.selectCategoryById(id);
    }

    // 9. other methods
    public void doSomething() {
        // ...
    }

    // 10. equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        // ...
    }
}
```

---

## 3. 코드 형식

코드 형식은 IntelliJ IDEA, VS Code, Eclipse 등 각 개발 도구에 설정된 **내장 포매터**를 따릅니다.

### 3.1 기본 원칙

- 들여쓰기: 스페이스 4칸 (탭 사용 금지)
- 한 줄 최대 길이: 120자 권장
- 중괄호: K&R 스타일 (같은 줄에 열기)

---

## 4. 네이밍 규칙

### 4.1 패키지명

**형식**: 모두 소문자, 도메인 역순

```
com.x2bee.<프로젝트>.<레이어>.<도메인>
```

**프로젝트별 패키지 구조**:

| 프로젝트         | 패키지 예시                                     |
| ---------------- | ----------------------------------------------- |
| BO (Back Office) | `com.x2bee.bo.app.controller.category`          |
| API - Display    | `com.x2bee.api.display.app.controller.category` |
| API - Order      | `com.x2bee.api.order.app.controller.order`      |
| API - Event      | `com.x2bee.api.event.app.controller.event`      |

**레이어별 패키지**:

```
com.x2bee.api.display.app
├── controller.<도메인>       # Controller
├── service.<도메인>          # Service 인터페이스 및 구현
├── repository.<도메인>       # Mapper 인터페이스
├── dto
│   ├── request.<도메인>      # Request DTO
│   └── response.<도메인>     # Response DTO
├── entity                    # Entity (도메인 구분 없음)
└── enums                     # Enum (도메인 구분 없음)
```

### 4.2 클래스/인터페이스명

**규칙**: 명사, 대문자로 시작하는 카멜케이스 (PascalCase)

**원칙**:

- 단순하고 이해하기 쉽게 작성
- 약어가 아닌 전체 단어 사용
- 적은 단어로 구성

✅ **좋은 예시**:

```java
public class CategoryController { }
public class UserService { }
public interface PaymentMapper { }
```

❌ **나쁜 예시**:

```java
public class CtgCtrl { }           // 약어 사용
public class Data { }              // 의미 불명확
public class CategoryControllerForAdmin { }  // 너무 김
```

### 4.3 메소드명

**규칙**: 동사, 소문자로 시작하는 카멜케이스 (camelCase)

✅ **좋은 예시**:

```java
public void getCategory() { }
public void saveUser() { }
public boolean isValid() { }
```

❌ **나쁜 예시**:

```java
public void Category() { }         // 동사 아님
public void Get_Category() { }     // 언더스코어 사용
```

### 4.4 변수명

**규칙**: 소문자로 시작하는 카멜케이스 (camelCase)

**금지사항**:

- 밑줄(\_)과 달러기호(\$)로 시작하는 변수명 금지
- 한 글자 변수명은 가급적 사용하지 않음 (단, for 루프의 제어 변수는 예외)

✅ **좋은 예시**:

```java
String userName;
int categoryId;
List<Product> productList;

for (int i = 0; i < 10; i++) {  // i, j, k는 허용
    // ...
}
```

❌ **나쁜 예시**:

```java
String _userName;       // 밑줄로 시작
String $categoryId;     // 달러로 시작
String n;               // 의미 불명확한 한 글자
```

### 4.5 상수명

**규칙**: 모두 대문자, 단어 구분은 언더스코어(\_)

**정의**: 모든 상수는 `static final` 필드로 처리 (모든 `static final` 필드가 상수는 아님)

✅ **좋은 예시**:

```java
public static final String DEFAULT_CATEGORY = "ALL";
public static final int MAX_RETRY_COUNT = 3;
public static final long CACHE_TIMEOUT_MS = 60000L;
```

---

## 4.6 네이밍 규칙 종합표

### 클래스 네이밍

| 소스 구분               | 명명 규칙                | 예시                     |
| ----------------------- | ------------------------ | ------------------------ |
| Controller              | 대상개체명 + Controller  | `CategoryController`     |
| Service 인터페이스      | 대상개체명 + Service     | `CategoryService`        |
| Service 구현            | 대상개체명 + ServiceImpl | `CategoryServiceImpl`    |
| Mapper (조회)           | DB테이블명 + Mapper      | `PrDispCtgBaseMapper`    |
| Mapper (등록/수정/삭제) | DB테이블명 + TrxMapper   | `PrDispCtgBaseTrxMapper` |
| Request DTO             | 대상개체명 + Request     | `CategoryRequest`        |
| Response DTO            | 대상개체명 + Response    | `CategoryResponse`       |
| Entity                  | DB테이블명 (카멜케이스)  | `PrDispCtgBase`          |

### 메소드 네이밍

| 레이어                   | 동작           | 명명 규칙     | 예시                    |
| ------------------------ | -------------- | ------------- | ----------------------- |
| **Controller / Service** | 단건 조회      | `get***`      | `getCategory()`         |
|                          | 목록 조회      | `get***List`  | `getCategoryList()`     |
|                          | 등록           | `register***` | `registerCategory()`    |
|                          | 수정           | `modify***`   | `modifyCategory()`      |
|                          | 삭제           | `delete***`   | `deleteCategory()`      |
|                          | 등록+수정+삭제 | `save***`     | `saveCategory()`        |
| **Mapper**               | 조회           | `select***`   | `selectPrDispCtgBase()` |
|                          | 등록           | `insert***`   | `insertPrDispCtgBase()` |
|                          | 수정           | `update***`   | `updatePrDispCtgBase()` |
|                          | 삭제           | `delete***`   | `deletePrDispCtgBase()` |

**참고**:

- 대상개체명은 가능한 Full Name을 사용합니다.
- 기타 명명 규칙은 필요에 따라 추가 작성합니다.

---

## 5. 주석 작성

### 5.1 클래스 주석

**모든 클래스에 다음 형식의 주석을 작성합니다:**

```java
/**
 * @author 작성자명
 * @version 1.0
 * @since 2024-10-14
 */
public class CategoryController {
    // ...
}
```

### 5.2 IDE별 클래스 주석 템플릿 설정

#### IntelliJ IDEA

1. `Preferences` > `Editor` > `File and Code Templates`
2. 각 파일 타입에 템플릿 추가

```java
/**
 * @author ${USER}
 * @version 1.0
 * @since ${DATE}
 */
```

#### Eclipse

1. `Window` > `Preferences` > `Java` > `Code Style` > `Code Templates`
2. `Type Comment` 변경

#### Visual Studio Code

1. `Configure User Snippets` → `New Global Snippets`
2. 다음 코드 추가:

```json
{
  "Java Class Template": {
    "scope": "java",
    "prefix": "gcd",
    "body": [
      "/**",
      " * @author ${user}",
      " * @version 1.0",
      " * @since ${CURRENT_YEAR}-${CURRENT_MONTH}-${CURRENT_DATE}",
      " */",
      "public class ${TM_FILENAME_BASE} {",
      "\t$0",
      "}"
    ],
    "description": "Java Class Template"
  }
}
```

사용법: `.java` 파일에서 `gcd` 입력 후 자동완성

### 5.3 메소드 주석

복잡한 로직이나 공개 API 메소드에는 Javadoc 주석을 작성합니다.

```java
/**
 * 카테고리 정보를 조회합니다.
 *
 * @param categoryId 카테고리 ID
 * @return 카테고리 정보
 * @throws ApiException 카테고리를 찾을 수 없는 경우
 */
public Category getCategoryById(Long categoryId) {
    // ...
}
```

### 5.4 인라인 주석

```java
// ✅ 좋은 주석: 왜 이렇게 했는지 설명
// 성능 최적화를 위해 캐시 사용 (DB 조회 10배 감소 효과)
String cachedData = cache.get(key);

// ❌ 나쁜 주석: 코드를 그대로 설명
// categoryId를 1 증가시킴
categoryId++;
```

---

## 체크리스트

### ✅ 파일 작성 전

- [ ] UTF-8 인코딩 설정 확인
- [ ] IDE 포매터 설정 확인

### ✅ 소스 작성 시

- [ ] 패키지 선언 (default 패키지 금지)
- [ ] Import 정렬 (static import 금지)
- [ ] 클래스 주석 작성
- [ ] 클래스 내부 순서 준수

### ✅ 네이밍 시

- [ ] 패키지명: 모두 소문자
- [ ] 클래스명: 대문자 카멜케이스 (PascalCase)
- [ ] 메소드명: 소문자 카멜케이스 (camelCase), 동사로 시작
- [ ] 변수명: 소문자 카멜케이스 (camelCase)
- [ ] 상수명: 대문자 + 언더스코어
- [ ] 네이밍 규칙 표 참조하여 접미사 확인

### ✅ 주석 작성 시

- [ ] 클래스 주석 (author, version, since)
- [ ] 복잡한 로직에 대한 설명 추가
- [ ] "무엇을"이 아닌 "왜"를 설명

---

**최종 수정일**: 2025-10-14
**버전**: 1.0
**출처**: Confluence - 프로그래밍 일반 표준
