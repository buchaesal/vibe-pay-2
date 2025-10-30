# X2BEE API 개발 컨벤션 가이드

> ⚠️ **주의사항**
>
> - 본 문서는 원본 개발 가이드를 기반으로 작성되었습니다.
> - 일부 내용은 실제 프로젝트 환경에 따라 다를 수 있으므로, 기존 코드를 참고하시기 바랍니다.
> - 특히 **URI 매핑**, **Response DTO 상속 여부**는 프로젝트별로 확인이 필요합니다.

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [패키지 구조](#2-패키지-구조)
3. [컨트롤러 작성](#3-컨트롤러-작성)
4. [서비스 작성](#4-서비스-작성)
5. [Mapper 작성](#5-mapper-작성)
6. [DTO/Entity 작성](#6-dtoentity-작성)
7. [오류 처리](#7-오류-처리)
8. [프로퍼티 및 메시지](#8-프로퍼티-및-메시지)
9. [로깅](#9-로깅)
10. [마스킹](#10-마스킹)
11. [암복호화](#11-암복호화)
12. [보안 및 권한](#12-보안-및-권한)
13. [Swagger 문서화](#13-swagger-문서화)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 특징

- **마이크로서비스 아키텍처**: 각 도메인별로 프로젝트 분리 (x2bee-api-display, x2bee-api-order 등)
- **REST API 방식**: 모든 API는 REST 기반으로 제공
- **JSON 데이터 포맷**: 입력/출력 데이터는 JSON 형식 사용
- **Stateless**: 세션 정보 없이 요청마다 필요한 정보를 전달

### 1.2 기술 스택

- Spring Boot
- MyBatis
- Lombok
- Swagger3 (OpenAPI)
- Logback (SLF4J)

---

## 2. 패키지 구조

### 2.1 패키지 명명 규칙

업무 대분류를 기준으로 패키지를 분류하고 명명합니다.

```
com.x2bee.api.<service>.app
├── controller.<domain>   # 컨트롤러
├── service.<domain>      # 서비스 인터페이스 및 구현
├── repository.<domain>   # Mapper 인터페이스
├── dto
│   ├── request.<domain>  # Request DTO
│   └── response.<domain> # Response DTO
├── entity                # Entity (도메인 구분 없음)
└── enums                 # Enum (도메인 구분 없음)
```

### 2.2 리소스 구조

```
src/main/resources
├── mapper
│   ├── rwdb/<domain>     # Read/Write DB Mapper XML
│   └── rodb/<domain>     # ReadOnly DB Mapper XML
└── message/<service>     # 다국어 메시지 파일
```

### 2.3 패키지 예시

```
com.x2bee.api.display.app
├── controller.sample
├── service.sample
├── repository.sample
├── dto.request.sample
├── dto.response.sample
├── entity
└── enums
```

---

## 3. 컨트롤러 작성

### 3.1 컨트롤러 역할

- 입력 파라미터 수신 및 DTO 변환
- 서비스 메소드 호출
- 응답 데이터 변환 및 반환

### 3.2 클래스 레벨 어노테이션

```java
@RestController
@RequestMapping("/categories")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "카테고리 관리", description = "카테고리 API")
public class CategoryController {
    private final CategoryService categoryService;
}
```

| 어노테이션                 | 설명                                             |
| -------------------------- | ------------------------------------------------ |
| `@RestController`          | REST 컨트롤러 선언 (@Controller + @ResponseBody) |
| `@RequestMapping`          | 클래스 레벨 URI 매핑                             |
| `@RequiredArgsConstructor` | 생성자 주입 (Lombok)                             |
| `@Slf4j`                   | 로깅을 위한 Lombok 어노테이션                    |
| `@Tag`                     | Swagger API 그룹 설정                            |

### 3.3 메소드 레벨 어노테이션

```java
@Operation(summary = "카테고리 목록 조회", description = "카테고리 목록을 조회합니다")
@Parameters({
    @Parameter(name = "siteNo", description = "사이트번호", required = true, example = "1"),
    @Parameter(name = "useYn", description = "사용여부 (Y/N)", required = true, example = "Y")
})
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "조회 성공",
                 content = @Content(schema = @Schema(implementation = Category.class))),
    @ApiResponse(responseCode = "400", description = "조회 실패",
                 content = @Content(schema = @Schema(implementation = ErrorCode.class)))
})
@GetMapping("/trees")
public Response<List<Category>> getCategoryTreeList(CategoryRequest request) throws Exception {
    return new Response<>(categoryService.getCategoryTreeList(request));
}
```

### 3.4 HTTP 메소드 매핑

| 메소드 | 어노테이션       | 용도      |
| ------ | ---------------- | --------- |
| GET    | `@GetMapping`    | 조회/검색 |
| POST   | `@PostMapping`   | 등록      |
| PUT    | `@PutMapping`    | 전체 수정 |
| PATCH  | `@PatchMapping`  | 일부 수정 |
| DELETE | `@DeleteMapping` | 삭제      |

### 3.5 URI 매핑 규칙

**형식**: `/api/<업무명>/<리소스명>/<하위리소스명>`

**중요**: `/api/<업무명>` 부분은 `application.yml`의 `context-path`로 설정되므로, **컨트롤러에서는 작성하지 않습니다.**

**application.yml 설정 예시**:

```yaml
server:
  servlet:
    context-path: /api/display
```

**컨트롤러 작성 예시**:

```java
// ✅ 올바른 방법 (context-path 제외)
@RequestMapping("/categories")

@GetMapping("/trees")              // 실제 URL: GET /api/display/categories/trees
@GetMapping("/{id}")              // 실제 URL: GET /api/display/categories/{id}
@PostMapping("")                  // 실제 URL: POST /api/display/categories
@PutMapping("/{id}")              // 실제 URL: PUT /api/display/categories/{id}
@DeleteMapping("/{id}")           // 실제 URL: DELETE /api/display/categories/{id}
@PatchMapping("/{id}")            // 실제 URL: PATCH /api/display/categories/{id}
```

**❌ 잘못된 방법**:

```java
@RequestMapping("/api/display/categories")  // context-path를 중복 작성하면 안됨!
```

### 3.6 파라미터 처리

```java
// Request Body (JSON)
@PostMapping("")
public Response<String> saveCategory(
    @RequestBody @Valid CategoryRequest request) throws Exception {
    return new Response<>(categoryService.save(request));
}

// Path Variable
@GetMapping("/{id}")
public Response<Category> getCategory(@PathVariable Long id) throws Exception {
    return new Response<>(categoryService.getById(id));
}

// Query Parameter
@GetMapping("")
public Response<List<Category>> getCategoryList(CategoryRequest request) throws Exception {
    return new Response<>(categoryService.getList(request));
}
```

### 3.7 응답 형식

**모든 API 응답은 `Response<T>` 객체로 감싸서 반환합니다.**

```java
@GetMapping("/list")
public Response<List<String>> getList() throws Exception {
    return new Response<>(service.getList());
}
```

**Response 구조**:

```json
{
  "timestamp": "2024-10-14T10:30:00.000",
  "code": "0000",
  "message": "성공",
  "payload": {
    /* 실제 데이터 */
  }
}
```

---

## 4. 서비스 작성

### 4.1 서비스 역할

- 핵심 업무 로직 처리
- Repository(Mapper) 호출
- 타 서비스 API 호출
- 트랜잭션 관리

### 4.2 인터페이스/구현 클래스 구분

**반드시 인터페이스와 구현 클래스를 분리합니다.**

```java
// CategoryService.java (인터페이스)
public interface CategoryService {
    List<Category> getCategoryList(CategoryRequest request);
    Category getById(Long id);
    void save(Category category);
    void update(Category category);
    void delete(Long id);
}
```

```java
// CategoryServiceImpl.java (구현 클래스)
@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final CategoryTrxMapper categoryTrxMapper;

    @Override
    public List<Category> getCategoryList(CategoryRequest request) {
        return categoryMapper.selectCategories(request);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false,
                   value = "displayRwdbTxManager")
    public void save(Category category) {
        categoryTrxMapper.insertCategory(category);
    }
}
```

### 4.3 서비스 어노테이션

| 어노테이션                 | 설명                 |
| -------------------------- | -------------------- |
| `@Service`                 | 서비스 클래스 선언   |
| `@Slf4j`                   | 로깅 (Lombok)        |
| `@RequiredArgsConstructor` | 생성자 주입 (Lombok) |

### 4.4 트랜잭션 처리

**원칙**:

- `@Transactional` 어노테이션이 **있는** 경우 → **ReadWrite DB** 연결
- `@Transactional` 어노테이션이 **없는** 경우 → **ReadOnly DB** 연결

**트랜잭션 선언**:

```java
@Transactional(
    propagation = Propagation.REQUIRED,
    readOnly = false,
    value = "displayRwdbTxManager"  // 필수: 트랜잭션 매니저 지정
)
public void save(Category category) {
    // 등록/수정/삭제 로직
}
```

**트랜잭션 매니저 종류**:

- `displayRwdbTxManager`
- `orderRwdbTxManager`
- `eventRwdbTxManager`

⚠️ **주의**: 트랜잭션 매니저 이름은 오타 없이 정확히 입력해야 합니다.

---

## 5. Mapper 작성

### 5.1 Mapper 분리 원칙

**테이블당 2개의 Mapper를 작성합니다.**

| Mapper         | SQL 타입             | 데이터소스 |
| -------------- | -------------------- | ---------- |
| `***Mapper`    | SELECT               | ReadOnly   |
| `***TrxMapper` | INSERT/UPDATE/DELETE | Read/Write |

⚠️ **중요**: ReadOnly Mapper에 DML 작성 시 오류가 발생합니다.

### 5.2 Mapper 인터페이스

```java
// CategoryMapper.java (조회 전용)
public interface CategoryMapper {
    List<CategoryResponse> selectAllCategories();
    Optional<CategoryResponse> selectCategoryById(Long id);
    List<CategoryResponse> selectCategories(CategoryRequest request);
}
```

```java
// CategoryTrxMapper.java (등록/수정/삭제)
public interface CategoryTrxMapper {
    int insertCategory(Category category);
    int updateCategory(Category category);
    int deleteCategory(Long id);
}
```

### 5.3 Mapper XML

**파일 위치**:

- ReadOnly: `src/main/resources/mapper/rodb/<domain>/`
- Read/Write: `src/main/resources/mapper/rwdb/<domain>/`

⚠️ **중요**: 같은 도메인은 rodb와 rwdb 모두 **동일한 폴더명**을 사용합니다.

**예시**:

```
src/main/resources/mapper/
├── rodb/
│   ├── category/
│   │   └── CategoryMapper.xml
│   └── product/
│       └── ProductMapper.xml
└── rwdb/
    ├── category/
    │   └── CategoryTrxMapper.xml
    └── product/
        └── ProductTrxMapper.xml
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.x2bee.api.display.app.repository.category.CategoryMapper">

    <!-- 카테고리 목록 조회 -->
    <select id="selectCategories" parameterType="CategoryRequest"
            resultType="CategoryResponse">
        /* CategoryMapper.selectCategories */
        SELECT
            category_no,
            category_nm,
            use_yn,
            reg_dt
        FROM pr_disp_category
        WHERE site_no = #{siteNo}
        <if test="useYn != null">
            AND use_yn = #{useYn}
        </if>
        ORDER BY sort_ord
    </select>

    <!-- 카테고리 단건 조회 -->
    <select id="selectCategoryById" parameterType="long"
            resultType="CategoryResponse">
        /* CategoryMapper.selectCategoryById */
        SELECT *
        FROM pr_disp_category
        WHERE category_no = #{id}
    </select>

</mapper>
```

---

## 6. DTO/Entity 작성

### 6.1 DTO vs Entity

| 구분             | 용도                    | 상속                               | Alias |
| ---------------- | ----------------------- | ---------------------------------- | ----- |
| **Request DTO**  | 조회/검색 요청 파라미터 | BaseCommonEntity 상속              | ✅    |
| **Response DTO** | 조회/검색 결과          | 상속 여부는 프로젝트 정책에 따름\* | ✅    |
| **Entity**       | 등록/수정/삭제 데이터   | BaseCommonEntity 상속              | ✅    |

\* 페이징 정보가 필요하면 상속, 단순 조회만 필요하면 상속 안함

### 6.2 Request DTO 작성

```java
@Alias("CategoryRequest")
@Getter
@Setter
public class CategoryRequest extends BaseCommonEntity {
    private static final long serialVersionUID = 1234567890123456789L; // UUID 생성

    @Schema(description = "사이트번호", example = "1")
    private Long siteNo;

    @Schema(description = "사용여부", allowableValues = {"Y", "N"}, example = "Y")
    private String useYn;

    @Schema(description = "카테고리명")
    private String categoryNm;
}
```

**특징**:

- `BaseCommonEntity` 상속 (생성자/수정자/페이징 정보 포함)
- `@Alias` 사용 (MyBatis 타입 축약)
- `serialVersionUID` 필수 (UUID 생성)
- `@Schema` 사용 (Swagger 문서화)

### 6.3 Response DTO 작성

```java
@Alias("CategoryResponse")
@Getter
@Setter
public class CategoryResponse {
    private static final long serialVersionUID = 9876543210987654321L; // UUID 생성

    @Schema(description = "카테고리번호")
    private Long categoryNo;

    @Schema(description = "카테고리명")
    private String categoryNm;

    @Schema(description = "사용여부")
    private String useYn;

    @Schema(description = "등록일시")
    private LocalDateTime regDt;
}
```

**특징**:

- `BaseCommonEntity` 상속 여부는 **프로젝트 정책에 따라 다름**
  - 페이징 정보가 필요한 경우: 상속 O
  - 단순 조회 결과만 필요한 경우: 상속 X
- Mapper 조회 결과를 그대로 전달
- `serialVersionUID` 필수

⚠️ **참고**: 원본 문서에서 설명과 예시가 다르므로, 실제 프로젝트의 기존 코드를 참고하세요.

### 6.4 Entity 작성

```java
@Alias("Category")
@Getter
@Setter
public class Category extends BaseCommonEntity {
    private static final long serialVersionUID = 5555555555555555555L; // UUID 생성

    @Schema(description = "카테고리번호")
    private Long categoryNo;

    @Schema(description = "카테고리명")
    @NotBlank(message = "카테고리명은 필수입니다")
    private String categoryNm;

    @Schema(description = "사용여부")
    @Pattern(regexp = "^[YN]$", message = "Y 또는 N만 입력 가능합니다")
    private String useYn;

    @Schema(description = "정렬순서")
    @Min(value = 0, message = "0 이상이어야 합니다")
    private Integer sortOrd;
}
```

**특징**:

- DB 테이블 컬럼과 1:1 매칭
- `BaseCommonEntity` 상속
- Validation 어노테이션 사용 가능

### 6.5 Validation 어노테이션

컨트롤러에서 `@Valid` 또는 `@Validated`와 함께 사용:

```java
@PostMapping("")
public Response<String> save(@RequestBody @Valid Category category) throws Exception {
    return new Response<>(categoryService.save(category));
}
```

**주요 Validation 어노테이션**:

- `@NotNull`: null 불가
- `@NotBlank`: null, 빈 문자열, 공백만 있는 문자열 불가
- `@Size(min=, max=)`: 문자열 길이 제한
- `@Min(value)`: 최소값
- `@Max(value)`: 최대값
- `@Pattern(regexp)`: 정규식 패턴
- `@Email`: 이메일 형식

---

## 7. 오류 처리

### 7.1 예외 발생 방법

```java
throw new ApiException(ApiError.DATA_NOT_FOUND);
```

### 7.2 ApiError Enum

```java
@Getter
@AllArgsConstructor
public enum ApiError implements AppError {
    // 성공
    SUCCESS("0000", "common.message.success", "common.message.success", false),

    // 클라이언트 오류
    EMPTY_PARAMETER("1001", "common.error.emptyParameter", "common.error.emptyParameter", false),
    INVALID_PARAMETER("1002", "common.error.invalidParameter", "common.error.invalidParameter", false),
    DATA_NOT_FOUND("1003", "common.error.dataNotFound", "common.error.dataNotFound", false),
    DUPLICATE_DATA("1004", "common.error.duplicateData", "common.error.duplicateData", false),
    INVALID_FILE("1005", "common.error.invalidFile", "common.error.invalidFile", false),

    // 서버 오류
    UNKNOWN("9000", "common.error.unknown", "common.error.unknown", false),
    VALIDATION_EXCEPTION("9100", "common.error.unknown", "common.error.unknown", false);

    private final String code;
    private final String messageKey;
    private final String boMessageKey;
    private final boolean isProcess;
}
```

### 7.3 오류 응답 형식

```json
{
  "timestamp": "2024-10-14T17:30:42.132",
  "code": "1003",
  "message": "데이터가 존재하지 않습니다.",
  "payload": null
}
```

**HTTP Status**: `400 BAD REQUEST`

### 7.4 예외 처리 예시

```java
@Override
public Category getById(Long id) {
    return categoryMapper.selectCategoryById(id)
        .orElseThrow(() -> new ApiException(ApiError.DATA_NOT_FOUND));
}
```

---

## 8. 프로퍼티 및 메시지

### 8.1 프로퍼티 관리

**파일 구조**:

- `application.yml`: Spring 및 공통 설정
- `config/application-<profile>.properties`: 업무별 설정

### 8.2 프로퍼티 사용

**방법 1: @Value 어노테이션 (권장)**

```java
@Value("${app.apiUrl.system}")
private String systemApiUrl;
```

**방법 2: Environment 빈**

```java
@Autowired
private Environment env;

String value = env.getProperty("app.apiUrl.system");
```

---

## 9. 로깅

### 9.1 로깅 레벨

| 레벨  | 용도      |
| ----- | --------- |
| ERROR | 오류      |
| WARN  | 경고      |
| INFO  | 정보      |
| DEBUG | 디버그    |
| TRACE | 상세 추적 |

### 9.2 로깅 사용

```java
@Slf4j
public class CategoryService {
    public void save(Category category) {
        log.debug("카테고리 저장 시작: {}", category);
        // ...
        log.info("카테고리 저장 완료: id={}", category.getCategoryNo());
    }
}
```

---

## 10. 마스킹

### 10.1 마스킹 어노테이션

```java
@MaskString(type = MaskingType.NAME)
private String userName;

@MaskString(type = MaskingType.PHONE)
private String phoneNo;

@MaskString(type = MaskingType.EMAIL)
private String email;

@MaskString(type = MaskingType.ID_NUMBER)
private String idNumber;
```

### 10.2 마스킹 타입

- `NAME`: 이름 (홍\*동)
- `PHONE`: 전화번호 (010-\*\*\*\*-5678)
- `EMAIL`: 이메일 (abc\*\*\*@example.com)
- `ID_NUMBER`: 주민번호 (123456-**\*\*\***)

---

## 11. 암복호화

### 11.1 암호화 대상

- 주민번호
- 신용카드 번호
- 계좌번호
- 비밀번호

### 11.2 암호화 사용

```java
@Autowired
private CryptoService cryptoService;

// 암호화
String encrypted = cryptoService.encrypt(plainText);

// 복호화
String decrypted = cryptoService.decrypt(encrypted);
```

---

## 12. 보안 및 권한

### 12.1 인증/인가

- JWT 토큰 기반 인증
- Spring Security 사용

### 12.2 권한 체크

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteCategory(Long id) {
    // ...
}
```

---

## 13. Swagger 문서화

### 13.1 Swagger 어노테이션

**컨트롤러**:

- `@Tag`: API 그룹
- `@Operation`: API 메소드 설명
- `@Parameter`: 파라미터 설명
- `@ApiResponse`: 응답 설명

**DTO/Entity**:

- `@Schema`: 필드 설명

### 13.2 Swagger UI 접근

```
http://localhost:8080/swagger-ui/index.html
```

---

## 네이밍 규칙 종합표

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

---

## 체크리스트

### ✅ 컨트롤러 작성 시

- [ ] 필수 어노테이션 (@RestController, @RequestMapping, @Slf4j, @RequiredArgsConstructor, @Tag)
- [ ] URI 매핑 (context-path 제외)
- [ ] Swagger 어노테이션 (@Operation, @Parameters, @ApiResponses)
- [ ] Response<T> 객체 반환

### ✅ 서비스 작성 시

- [ ] 인터페이스/구현 클래스 분리
- [ ] 트랜잭션 어노테이션 (CUD 작업 시)
- [ ] 트랜잭션 매니저 이름 정확히 지정

### ✅ Mapper 작성 시

- [ ] 조회용 Mapper / TrxMapper 분리
- [ ] Mapper XML 파일 위치 (rodb/rwdb)
- [ ] 메소드명 규칙 준수

### ✅ DTO/Entity 작성 시

- [ ] @Alias 어노테이션
- [ ] serialVersionUID 생성
- [ ] BaseCommonEntity 상속 (필요 시)
- [ ] @Schema 어노테이션 (Swagger 문서화)
- [ ] Validation 어노테이션 (필요 시)

### ✅ 예외 처리

- [ ] ApiException 사용
- [ ] 적절한 ApiError Enum 선택

---

**최종 수정일**: 2025-10-14
**버전**: 1.0
**출처**: Confluence - X2BEE API 개발 컨벤션 가이드
