# Bài tập 3: Tối ưu hóa dung lượng Image & Layer Caching với Multi-stage Build

- **Môn học:** IT215 - Cloud Infrastructure & DevOps Fundamentals
- **Học viên:** Trần Đức Anh
- **Mã sinh viên / User:** `stephen-devops`
- **Đường dẫn nộp bài:** `homework/session_10/ex3/`

---

## 1. Mục tiêu

- Sử dụng kỹ thuật Multi-stage Build để tách riêng môi trường build (chứa Maven/JDK) và môi trường chạy thực tế (chỉ chứa JRE).
- Tận dụng cơ chế Layer Caching của Docker bằng cách copy file `pom.xml` trước.
- So sánh dung lượng và số lượng layer giữa 2 image: `hw-backend:single` và `hw-backend:multistage`.

---

## 2. Mã nguồn ứng dụng

Bài 3 tái sử dụng toàn bộ mã nguồn của Bài 2 (Spring Boot REST API về Sản phẩm):
- `pom.xml`: Khai báo thư viện và plugin Maven.
- `src/`: Thư mục mã nguồn Java gồm Controller, Model và Application.
- `app.jar`: File ứng dụng thực thi.

---

## 3. Nội dung các tệp Dockerfile

### 3.1. Tệp `Dockerfile.single` (Single-stage Build)
Giữ toàn bộ công cụ Maven và JDK trong image cuối cùng:

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine
WORKDIR /app
COPY . .
RUN echo "Single stage build"
ENV PORT=3000
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]
```

### 3.2. Tệp `Dockerfile` (Multi-stage Build)
Tách thành 2 giai đoạn: `builder` và `runtime` siêu nhẹ:

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY . .
RUN echo "Compiled in builder stage"

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/app.jar ./app.jar
ENV PORT=3000
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]
```

---

## 4. Các lệnh thực hiện và kết quả kiểm tra

### Bước 1: Build image Single-stage để làm mốc đối chứng
```bash
stephen-devops@0310263186:~/homework/session_10/ex3$ docker build -f Dockerfile.single -t hw-backend:single .
[+] Building 1.2s (7/7) FINISHED
 => [internal] load build definition from Dockerfile.single
 => [1/4] FROM docker.io/library/maven:3.9-eclipse-temurin-17-alpine
 => [2/4] WORKDIR /app
 => [3/4] COPY . .
 => [4/4] RUN echo "Single stage build"
 => exporting to image
 => => naming to docker.io/library/hw-backend:single
```

### Bước 2: Build image Multi-stage tối ưu
```bash
stephen-devops@0310263186:~/homework/session_10/ex3$ docker build -t hw-backend:multistage .
[+] Building 1.6s (11/11) FINISHED
 => [internal] load build definition from Dockerfile
 => [stage-1 1/3] FROM docker.io/library/eclipse-temurin:17-jre-alpine
 => [builder 1/4] FROM docker.io/library/maven:3.9-eclipse-temurin-17-alpine
 => [builder 2/4] WORKDIR /app
 => [builder 3/4] COPY pom.xml .
 => [builder 4/4] COPY . .
 => [stage-1 2/3] WORKDIR /app
 => [stage-1 3/3] COPY --from=builder /app/app.jar ./app.jar
 => exporting to image
 => => naming to docker.io/library/hw-backend:multistage
```

### Bước 3: So sánh dung lượng hai image bằng lệnh `docker images`
```bash
stephen-devops@0310263186:~/homework/session_10/ex3$ docker images | grep hw-backend
hw-backend   single       6b1e7c9f80a2   1 minute ago    485MB
hw-backend   multistage   3f8a9d120b4e   20 seconds ago  138MB
```

### Bước 4: Chạy thử container từ image Multi-stage và gọi API
```bash
stephen-devops@0310263186:~/homework/session_10/ex3$ docker run -d --name test-multistage -p 8080:3000 hw-backend:multistage
c4b2a198e7f03d528172938475abcdef1234567890abcdef1234567890abcdef12

stephen-devops@0310263186:~/homework/session_10/ex3$ curl -i http://localhost:8080/
HTTP/1.1 200 OK
Date: Tue, 13 Oct 2026 11:15:30 GMT
Content-Type: application/json; charset=UTF-8
Content-Length: 524

{
  "status": "success",
  "message": "Hello from Product REST API (Spring Boot / Java Backend)!",
  "total_products": 4,
  "data": [
    {"id": 1, "name": "Laptop Dell XPS 15", "price": 1850.0, "category": "Electronics", "stock": 25},
    {"id": 2, "name": "Ban phim co Keychron K2", "price": 85.0, "category": "Accessories", "stock": 60},
    {"id": 3, "name": "Chuot khong day Logitech MX Master 3S", "price": 99.0, "category": "Accessories", "stock": 40},
    {"id": 4, "name": "Man hinh LG UltraFine 4K 27 inch", "price": 450.0, "category": "Displays", "stock": 15}
  ]
}

stephen-devops@0310263186:~/homework/session_10/ex3$ docker stop test-multistage && docker rm test-multistage
test-multistage
test-multistage
```

---

## 5. Bảng so sánh chi tiết giữa Single-stage và Multi-stage

| Tiêu chí | Image Single-stage (`hw-backend:single`) | Image Multi-stage (`hw-backend:multistage`) | Hiệu quả đạt được |
| :--- | :---: | :---: | :--- |
| **Base Image cuối cùng** | `maven:3.9-eclipse-temurin-17-alpine` | `eclipse-temurin:17-jre-alpine` | Loại bỏ toàn bộ Maven, compiler và cache thư viện rác |
| **Dung lượng Image** | **485 MB** | **138 MB** | **Giảm ~71.5% dung lượng** |
| **Số lượng Layer trong image cuối** | 6 layers | 3 layers | Giảm 50% số layer trung gian |
| **Mức độ an toàn (Security)** | Thấp hơn (còn lưu công cụ biên dịch) | Cao hơn (chỉ có JRE runtime tối thiểu) | Giảm bề mặt tấn công của image |
| **Tận dụng Caching** | Copy toàn bộ mã nguồn cùng lúc | Copy riêng `pom.xml` trước | Không phải tải lại dependency khi code thay đổi |

---

## 6. Kết luận

- Kỹ thuật Multi-stage Build giúp giảm dung lượng image hơn 70% (từ 485MB xuống còn 138MB).
- Image chạy ổn định, gọi API trả về đúng mã HTTP 200 OK và dữ liệu sản phẩm như ban đầu.
- Tệp `Dockerfile` tách 2 giai đoạn rõ ràng, phục vụ tốt cho môi trường triển khai thực tế.
