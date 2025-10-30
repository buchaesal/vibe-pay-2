# POSTGRESQL 쿼리 작성 가이드


## 1. 기본 원칙

### 1.1 대소문자 규칙
- **SQL 키워드**: 모두 대문자 (SELECT, FROM, WHERE, JOIN 등)
- **테이블명/컬럼명**: 대문자 SNAKE_CASE (MEMBER_ID, CREATED_AT)
- **MyBatis 파라미터**: 카멜케이스 (#{memberId}, #{langCd})

### 1.2 정렬 및 들여쓰기
- **각 절의 시작점을 세로로 완벽하게 정렬** (SELECT, FROM, WHERE, JOIN 등)
- 컬럼은 콤마(,)를 앞에 붙여 정렬
- 서브쿼리, CASE문, ON 조건절은 2-4 spaces 들여쓰기
- 가로 최대 80자 이하 권장

### 1.3 주석
- 쿼리 시작부에 `/* Mapper.id */` 형식 주석 반드시 추가
- 복잡한 로직은 `/* 설명 */` 주석 추가

## 2. 쿼리 작성 표준

### 2.1 SELECT
```sql
SELECT /* MemberMapper.findByEmail */
       MEMBER_ID
  FROM MEMBER
 WHERE EMAIL = #{email}
```

### 2.2 INSERT
```sql
INSERT INTO MEMBER ( /* mapper.insertMember */
       NAME
     , CREATED_AT
)
VALUES (
       #{name}
     , NOW()
)
```

### 2.3 UPDATE
```sql
UPDATE MEMBER /* mapper.updateMember */
   SET NAME = #{name}
 WHERE MEMBER_ID = #{memberId}
```

### 2.4 DELETE
```sql
DELETE /* mapper.deleteMember */
  FROM MEMBER
 WHERE MEMBER_ID = #{memberId}
```

### 2.5 JOIN
```sql
SELECT /* OrderMapper.findOrder */
       O.ORDER_ID
     , M.NAME
  FROM ORDERS O
 INNER JOIN MEMBER M ON O.MEMBER_ID = M.MEMBER_ID
 WHERE O.ORDER_ID = #{orderId}
```

### 2.6 동적 쿼리 (MyBatis)
```xml
<select id="searchMembers">
    SELECT /* MemberMapper.searchMembers */
           MEMBER_ID
         , NAME
      FROM MEMBER
     WHERE 1=1
       <if test="name != null">
       AND NAME = #{name}
       </if>
       <if test="memberIds != null">
       AND MEMBER_ID IN
           <foreach collection="memberIds" item="id" separator="," open="(" close=")">
               #{id}
           </foreach>
       </if>
</select>
```

## 3. 성능 최적화 필수 사항

### 3.1 인덱스 활용
- **인덱스 컬럼 가공 금지**
    - ❌ `WHERE SUBSTR(VAR, 1, 3) = 'ABC'`
    - ✅ `WHERE VAR LIKE 'ABC%'`
    - ❌ `WHERE TO_CHAR(DATE, 'YYMM') = '2501'`
    - ✅ `WHERE DATE = TO_DATE('2501', 'YYMM')`

### 3.2 효율적인 쿼리
- `SELECT *` 사용 금지 → 필요한 컬럼만 명시
- `COUNT(*)` 대신 `COUNT(PK_COLUMN)` 사용
- `UNION` 대신 `UNION ALL` 사용 (중복 제거 불필요시)
- 불필요한 정렬(ORDER BY, GROUP BY) 최소화

### 3.3 조건절 작성
- 부정형 비교(`!=`, `<>`, `NOT IN`) 대신 긍정형 사용
- `OR` 대신 `IN` 사용 가능시 변경
- `NULL` 비교시 DEFAULT 값 활용 검토

## 4. PostgreSQL 특화 함수

### 4.1 자주 사용하는 함수
- **날짜**: `NOW()`, `CURRENT_DATE`, `INTERVAL '1 day'`
- **형변환**: `CAST(col AS type)`, `col::type`
- **배열**: `ARRAY_AGG()`, `ARRAY_TO_STRING()`, `STRING_TO_ARRAY()`
- **순위**: `ROW_NUMBER() OVER()`, `RANK() OVER()`

### 4.2 계층 쿼리
```sql
/* MemberMapper.findTree */
WITH RECURSIVE CTE AS (
    SELECT ID, PARENT_ID, NAME, 0 AS LEVEL
      FROM TABLE
     WHERE PARENT_ID IS NULL
     UNION ALL
    SELECT T.ID, T.PARENT_ID, T.NAME, C.LEVEL + 1
      FROM TABLE T
     INNER JOIN CTE C ON T.PARENT_ID = C.ID
)
SELECT * 
  FROM CTE 
 ORDER BY LEVEL
```

## 5. 주의사항

- DUAL 테이블 사용 불가 (PostgreSQL)
- DA/DBA 협의 없이 HINT 사용 금지
- 대소문자 혼용 금지
- `BEGIN ~ END` 등 쌍으로 구성된 구문은 줄 맞춤
- 연산자 뒤에는 한 칸 띄움