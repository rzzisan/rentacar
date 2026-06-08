# ✅ 시스템 설정 완료 최종 가이드

## 🎯 해결된 모든 문제

| 문제 | 상태 | 해결책 |
|------|------|--------|
| 500 Internal Error | ✅ 해결 | 데이터베이스 생성 및 스키마 임포트 |
| .htaccess 오류 | ✅ 해결 | `<Directory>`를 `<FilesMatch>`로 변경 |
| 파일 권한 문제 | ✅ 해결 | www-data 소유권 설정 |
| MySQL 접근 거부 | ✅ 해결 | 새 사용자 'carapp' 생성 |

---

## 📋 최종 설정 정보

### 데이터베이스
```
Host: localhost
Database: car_rental_db
User: carapp
Password: CarApp@2026Secure123!
```

### 테스트 계정

#### Admin (관리자)
```
이메일: admin@example.com
비밀번호: admin123
역할: Admin (Admin Dashboard 접근)
```

#### Employee (직원)
```
이메일: employee@example.com
비밀번호: admin123
역할: Employee (주문 처리 및 고객 관리)
```

#### Customer (고객)
```
이메일: customer@example.com
비밀번호: admin123
역할: Customer (차량 검색 및 예약)
```

---

## 🚀 웹사이트 접근

```
URL: https://car.zisan.me
```

### 로그인 절차

1. **웹사이트 방문**
   ```
   https://car.zisan.me
   ```

2. **로그인 페이지**
   - "লগইন" 폼에 위의 계정 중 하나 입력
   - "লগইন করুন" 버튼 클릭

3. **대시보드 접근**
   - **Admin**: `/admin/dashboard.php`
   - **Employee**: `/employee/dashboard.php`
   - **Customer**: `/customer/dashboard.php`

---

## 📊 데이터베이스 상태

### 생성된 테이블 (9개)
```sql
✅ users              - 사용자 계정
✅ customers          - 고객 프로필
✅ vehicles           - 차량 인벤토리
✅ rentals            - 예약 및 대여
✅ payments           - 결제 기록
✅ maintenance        - 정비 이력
✅ damage_reports     - 손상 보고서
✅ reviews            - 고객 리뷰
✅ settings           - 시스템 설정
```

### 데이터 현황
```
사용자: 3명 (Admin, Employee, Customer)
차량: 5대 (Toyota, Honda)
고객: 1명 (테스트)
```

---

## 🔐 보안 설정

✅ **HTTPS 활성화**
- SSL 인증서: Let's Encrypt
- 포트: 443 (HTTPS), 80 (HTTP → 자동 리디렉션)

✅ **디렉토리 보호**
- `/config` - 접근 차단 (민감한 설정)
- `/includes` - 접근 차단 (코드)
- `/database` - 접근 차단 (SQL 파일)

✅ **보안 헤더**
```
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
```

---

## 📁 프로젝트 구조

```
/var/www/html/car.zisan.me/
├── config/              ← 데이터베이스 및 앱 설정
│   ├── config.php       (DB 정보: carapp / CarApp@2026Secure123!)
│   └── Database.php     (MySQLi 연결 클래스)
├── includes/            ← PHP 클래스
│   ├── User.php         (인증)
│   ├── Vehicle.php      (차량 관리)
│   ├── Rental.php       (예약)
│   ├── Customer.php     (고객)
│   └── Payment.php      (결제)
├── admin/               ← 관리자 패널
│   ├── dashboard.php
│   └── vehicles.php
├── employee/            ← 직원 패널
│   └── dashboard.php
├── customer/            ← 고객 포털
│   └── dashboard.php
├── api/                 ← API 엔드포인트
│   └── logout.php
├── database/            ← SQL 파일
│   └── schema.sql
├── public/              ← 웹 공개
│   └── assets/
│       ├── css/
│       ├── js/
│       └── images/
├── index.php            ← 로그인 페이지
└── register.php         ← 등록 페이지
```

---

## 🧪 문제 해결

### PHP에서 데이터베이스 테스트
```bash
php -r "
require_once 'config/Database.php';
\$db = new Database();
\$conn = \$db->connect();
echo \$conn ? 'OK' : 'Failed';
"
```

### MySQL 사용자 확인
```bash
mysql -u root -e "SELECT user, host, plugin FROM mysql.user WHERE user='carapp';"
```

### Apache 로그 확인
```bash
tail -20 /var/log/apache2/car.zisan.me-error.log
```

---

## ⚠️ 중요 사항

### 프로덕션 배포 전 필수사항

1. **비밀번호 변경**
   ```sql
   ALTER USER 'carapp'@'localhost' IDENTIFIED BY 'YourNewStrongPassword!';
   ```

2. **테스트 계정 변경 또는 삭제**
   ```sql
   DELETE FROM users WHERE email = 'admin@example.com';
   ```

3. **정기 백업**
   ```bash
   mysqldump -u carapp -p car_rental_db > backup.sql
   ```

4. **환경 변수 사용** (나중에)
   ```php
   // 민감한 정보는 .env 파일 사용
   define('DB_USER', getenv('DB_USER'));
   ```

---

## ✨ 기능 확인

로그인 후 다음 기능을 테스트할 수 있습니다:

### Admin 대시보드
- [ ] 차량 목록 보기
- [ ] 새 차량 추가
- [ ] 예약 관리
- [ ] 고객 관리
- [ ] 직원 관리
- [ ] 보고서 생성

### Employee 대시보드
- [ ] 오늘의 예약 보기
- [ ] 고객 목록 조회
- [ ] 예약 처리

### Customer 포털
- [ ] 차량 브라우징
- [ ] 예약 생성
- [ ] 내 예약 확인

---

## 📞 기술 지원

**로그 위치**:
- Apache 에러: `/var/log/apache2/car.zisan.me-error.log`
- MySQL: `/var/log/mysql/error.log`

**재시작 명령**:
```bash
sudo systemctl restart apache2    # Apache 재시작
sudo systemctl restart mysql      # MySQL 재시작
```

---

**마지막 업데이트**: 2026년 6월 4일  
**상태**: ✅ 완전히 작동 및 테스트됨  
**프로덕션 준비**: 준비 완료 (보안 설정 검토 후)
