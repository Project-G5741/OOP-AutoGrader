# Hướng Dẫn Kỹ Thuật: Thiết Kế & Tạo Mascot Sprite Sheet Tương Tác 2.5D

Tài liệu này chuẩn hóa toàn bộ quy trình, công nghệ, nguyên lý toán học và tiêu chuẩn nghiệm thu của hệ thống **Interactive Mascot Sprite Sheet** trong dự án **EIU Capstone OOP-AutoGrader**.

---

## 1. Nguồn Gốc & Ý Tưởng Thiết Kế (Inspiration & Origin)

### 1.1. Nguồn cảm hứng
- **Classic 2D Game Sprite Sheets**: Lấy cảm hứng từ hệ thống sprite sheet đa hướng của các game nhập vai kinh điển (*Pokémon Mystery Dungeon*, *Earthbound*, *Ragnarok Online*). Thay vì render mô hình 3D nặng nề, nhân vật 2D được biểu diễn qua lưới ảnh tĩnh để đạt hiệu năng $60\text{ FPS}$ tuyệt đối trên mọi trình duyệt web.
- **Modern UI Companions**: Học hỏi phong cách tương tác tâm lý học từ các trợ lý linh vật nổi tiếng:
  - *GitHub Octocat*: Đại diện cho linh hồn lập trình, biến đổi theo từng chủ đề và sự kiện.
  - *Duolingo Duo Bird*: Phản hồi cảm xúc đa tầng (vui mừng khi hoàn thành bài, khóc khi gãy streak, giận dữ khi lười biếng).
  - *Raycast / Arc Browser Companion*: Tương tác mắt nhìn theo con trỏ chuột (Mouse-Tracking Eye Gaze) tạo cảm giác sống động, đồng hành như một thực thể có tri giác.

### 1.2. Mô hình toán học theo dõi con trỏ (Mouse-Tracking Math)
Mascot sử dụng ma trận góc lượng giác để xác định hướng nhìn trong thời gian thực:
$$\Delta x = X_{\text{mouse}} - X_{\text{mascot\_center}}, \quad \Delta y = Y_{\text{mouse}} - Y_{\text{mascot\_center}}$$
$$\theta = \operatorname{atan2}(\Delta y, \Delta x) \quad (\theta \in [-\pi, \pi])$$

Từ góc $\theta$ và khoảng cách Euclidean $d = \sqrt{\Delta x^2 + \Delta y^2}$:
- Khi $d \le \text{Threshold}_{\text{deadzone}}$: Nhân vật nhìn thẳng về phía trước (**Center** - ô giữa `(1, 1)`).
- Khi $d > \text{Threshold}_{\text{deadzone}}$: Không gian $360^\circ$ được chia thành 8 dải góc hình quạt ($45^\circ$ mỗi dải), ánh xạ chính xác vào 8 ô xung quanh:
  - $\left[-\frac{7\pi}{8}, -\frac{5\pi}{8}\right) \to$ **North-West (NW)** `(0, 0)`
  - $\left[-\frac{5\pi}{8}, -\frac{3\pi}{8}\right) \to$ **North (N)** `(0, 1)`
  - $\left[-\frac{3\pi}{8}, -\frac{\pi}{8}\right) \to$ **North-East (NE)** `(0, 2)`
  - $\left[-\frac{\pi}{8}, \frac{\pi}{8}\right) \to$ **East (E)** `(1, 2)`
  - $\left[\frac{\pi}{8}, \frac{3\pi}{8}\right) \to$ **South-East (SE)** `(2, 2)`
  - $\left[\frac{3\pi}{8}, \frac{5\pi}{8}\right) \to$ **South (S)** `(2, 1)`
  - $\left[\frac{5\pi}{8}, \frac{7\pi}{8}\right) \to$ **South-West (SW)** `(2, 0)`
  - Còn lại $\to$ **West (W)** `(1, 0)`

---

## 2. Công Nghệ & Tech Stack Sử Dụng

Quy trình kết hợp sức mạnh giữa **Generative AI** và **Thuật toán thị giác máy tính (Computer Vision Algorithms)**:

```
+-------------------------------------------------------------+
| 1. Generative AI (Gemini Image Gen / Imagen 3 API)          |
|    - Prompt kỹ thuật cao tạo ma trận 3x3 đồng nhất          |
|    - Cố định character design, bảng màu và ánh sáng         |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
| 2. Python Vision Pipeline (Pillow, NumPy, SciPy)            |
|    - Exterior Seed Flood-Fill: Bóc tách nền trắng #FFFFFF   |
|    - Chest & Chin Barrier: Bảo vệ trang phục/chi tiết trắng |
|    - Alpha Feathering: Khử viền răng cưa và viền trắng      |
|    - Face-Band Center Alignment: Cố định trục tâm mắt       |
|    - Soft Gradient Bottom Fade: Mờ đáy tự nhiên             |
+------------------------------+------------------------------+
                               |
                               v
+-------------------------------------------------------------+
| 3. Optimized Asset Export & Web Engine                      |
|    - Xuất định dạng WebP nén lossless/high-quality (<300KB) |
|    - CSS Sprite-Sheet Background Shifting (Steps)           |
|    - React Framer-Motion / HTML5 Canvas Micro-Interactions  |
+-------------------------------------------------------------+
```

### Chi tiết các thư viện cốt lõi:
1. **`PIL` (`Pillow`)**: Cắt ô lưới (`crop`), co giãn chất lượng cao (`Resampling.LANCZOS`), quản lý kênh Alpha (`RGBA`), xuất WebP và render GIF động.
2. **`collections.deque`**: Thực thi hàng đợi Breadth-First Search (BFS) loang màu điểm ảnh với tốc độ cực nhanh.
3. **`React` + `CSS Background Position`**: Render UI không cần tải 18 file ảnh lẻ mà chỉ tải đúng 2 tấm WebP ma trận, tối ưu hóa triệt để HTTP request và cache trình duyệt.

---

## 3. Quy Trình Tạo Sprite Sheet Từng Bước (End-to-End Workflow)

### Bước 1: Tổng hợp ảnh gốc bằng Generative AI
Mỗi nhân vật được chia thành 2 prompt tương ứng với 2 tấm sprite sheet ma trận $3 \times 3$ (khổ $1:1$ vuông, nền trắng tinh khiết `#FFFFFF`):
1. **Direction Sheet (`*-directions`)**: 9 góc quay đầu nhìn theo 8 hướng la bàn + 1 hướng nhìn thẳng chính diện.
2. **Reaction Sheet (`*-reactions`)**: 9 trạng thái cảm xúc / chiêu thức nguyên tố đặc trưng (tất cả đều quay mặt về phía trước).

> [!IMPORTANT]
> **Yêu cầu nhất quán nhân vật (Character Consistency)**:
> Khi sinh Reaction Sheet, **bắt buộc** phải cung cấp đường dẫn ảnh Direction Sheet (cụ thể là ô chính diện trung tâm) làm tham chiếu (`ImagePaths: [...]`). Tuyệt đối không để lệch model giữa 2 sheet (ví dụ: Direction là chim ưng thần dũng mãnh nhưng Reaction lại biến thành gà con chibi).

### Bước 2: Bóc tách nền thông minh (Exterior Seed Flood-Fill)
Không dùng phương pháp đổi màu đơn giản (`white -> transparent`) vì sẽ làm thủng mắt trắng, răng trắng, sừng bạc hoặc trang phục trắng của nhân vật.
Thuật toán chuẩn:
1. **Quét hạt giống viền ngoài (Exterior Seeds)**: Lấy mẫu tại 4 cạnh biên `(x, 0)`, `(x, h-1)`, `(0, y)`, `(w-1, y)`. Nếu màu tiệm cận trắng ($R, G, B \ge 242$ và độ lệch $\le 16$), đưa vào hàng đợi BFS.
2. **Khóa chặn đáy cổ (Chest Barrier Lock)**: Tìm tọa độ đáy của nhân vật. Khóa một dải hàng rào chắn ngang phần ngực/cổ để thuật toán loang màu không đi xuyên từ dưới đáy lọt vào áo trắng hay lông bụng.
3. **Loang màu viền ngoài**: BFS chỉ loang qua các pixel thuộc nền trống bên ngoài.
4. **Làm mềm viền (Alpha Smoothing / Feathering)**: Tạo vùng bán trong suốt chuyển tiếp $1 - 2\text{ px}$ giữa biên nhân vật và nền trong suốt, triệt tiêu hoàn toàn viền trắng (white halo/fringing).

### Bước 3: Căn chỉnh trục tâm mặt (Face-Band Alignment)
Nếu căn giữa theo toàn bộ bounding box, khi nhân vật có các hiệu ứng bay nhảy (như sừng dài, tia sét, vương miện lửa, quả cầu năng lượng, Zzz) thì khuôn mặt sẽ bị giật qua lại.
- **Giải pháp**: Chỉ lấy bounding box của dải khuôn mặt trung tâm (từ $38\%$ đến $85\%$ chiều cao nhân vật), tính trục đối xứng khuôn mặt:
  $$X_{\text{face\_center}} = \frac{X_{\text{left}} + X_{\text{right}}}{2}$$
- Đặt trục khuôn mặt luôn chuẩn xác tại $X = 180\text{ px}$ trên mỗi ô $360 \times 360\text{ px}$.

### Bước 4: Tạo dải mờ đáy cổ (Soft Bottom Gradient Fade)
Nhân vật dạng avatar bán thân cần hòa trộn tự nhiên vào giao diện web:
- Áp dụng hàm suy giảm alpha tuyến tính trên $20 - 30\text{ px}$ cuối cùng của phần ngực/cổ:
  $$\alpha(y) = \alpha_{\text{orig}} \times \left(1.0 - \frac{y - y_{\text{start}}}{y_{\text{end}} - y_{\text{start}}}\right)$$
- Giúp mascot không bị vết cắt ngang cứng nhắc (hard cut) khi nổi trên nền card hay dark mode.

### Bước 5: Ghép ma trận $1080 \times 1080$ & Xuất WebP
- Ghép 9 ô đã xử lý vào lưới $3 \times 3$ (mỗi ô $360 \times 360\text{ px}$).
- Xuất file `.webp` với `quality=92, method=6`. Dung lượng trung bình chỉ từ $160\text{ KB} - 280\text{ KB}$.

---

## 4. Bộ Quy Tắc Thiết Kế Chuẩn Hóa (Specifications)

| Thông số | Giá trị chuẩn | Mục đích |
|---|---|---|
| **Canvas Size** | $1080 \times 1080\text{ px}$ | Kích thước chuẩn cho lưới $3 \times 3$ |
| **Cell Size** | $360 \times 360\text{ px}$ | Độ phân giải sắc nét trên màn hình Retina/High-DPI |
| **Grid Layout** | $3\text{ hàng} \times 3\text{ cột}$ ($9\text{ ô}$) | Tối ưu ánh xạ tọa độ CSS bước nhảy |
| **Background** | Hoàn toàn trong suốt (`RGBA: 0, 0, 0, 0`) | Tương thích mọi màu nền UI |
| **Trục tâm ngang (X)** | $X = 180\text{ px}$ | Tâm mặt nằm chính giữa mỗi ô |
| **Đáy cổ (Bottom Y)** | $Y = 290 - 300\text{ px}$ | Chừa lề dưới $60 - 70\text{ px}$ để nhân vật không chạm đáy |
| **Định dạng lưu trữ** | WebP (`image/webp`) | Nén kênh alpha vượt trội so với PNG (giảm 70% dung lượng) |

### 4.1. Quy tắc 9 ô Hướng nhìn (Directions Matrix)
```
[ (0,0) Tây Bắc - NW ]   [ (0,1) Bắc - N (Nhìn lên) ]   [ (0,2) Đông Bắc - NE ]
[ (1,0) Tây - W (Trái) ] [ (1,1) Chính diện - Center ]  [ (1,2) Đông - E (Phải) ]
[ (2,0) Tây Nam - SW ]   [ (2,1) Nam - S (Nhìn xuống) ] [ (2,2) Đông Nam - SE ]
```

### 4.2. Quy tắc 9 ô Cảm xúc (Reactions Matrix)
Tất cả 9 ô reaction đều giữ **hướng mặt nhìn thẳng về phía trước**, chỉ thay đổi biểu cảm và hiệu ứng nguyên tố:
- **Ô 0 (Hàng 1, Cột 1 - Blink)**: Nhắm mắt nhẹ nhàng / Thiền định tĩnh tại.
- **Ô 1 (Hàng 1, Cột 2 - Core / Love)**: Trái tim / Lõi nguyên tố / Tình cảm thân thiện.
- **Ô 2 (Hàng 1, Cột 3 - Aura / Crown)**: Vương miện / Hào quang / Lấp lánh năng lượng.
- **Ô 3 (Hàng 2, Cột 1 - Roar / Cry)**: Gầm gừ / Tiếng gáy / Sóng âm xung kích.
- **Ô 4 (Hàng 2, Cột 2 - Smirk)**: Cười đắc thắng / Nụ cười tự tin thương hiệu.
- **Ô 5 (Hàng 2, Cột 3 - Wrath)**: Thịnh nộ / Mắt rực lửa / Trạng thái nộ cuồng bạo.
- **Ô 6 (Hàng 3, Cột 1 - Slumber)**: Ngủ say / Cuộn tròn ấm áp / Chữ Zzz.
- **Ô 7 (Hàng 3, Cột 2 - Dizzy)**: Mắt xoắn ốc hoa mắt / Chóng mặt khi bị gõ đầu (boop).
- **Ô 8 (Hàng 3, Cột 3 - Burst)**: Tuyệt chiêu nguyên tố bùng nổ (phun lửa / lốc xoáy / sóng băng).

---

## 5. Tiêu Chuẩn Nghiệm Thu "Đạt Chuẩn" (Definition of Done)

Một bộ Mascot được phê duyệt đưa vào sản phẩm khi đáp ứng đầy đủ **5 tiêu chí vàng**:

1. **Nhất quán mô hình $100\%$ (Model Fidelity)**:
   - Cả 2 sheet (Directions và Reactions) phải cùng một nhân vật, cùng tỷ lệ đầu, màu mắt, kiểu lông, chi tiết sừng/cánh/mào.
   - Không được xảy ra tình trạng "râu ông nọ cắm cằm bà kia" (ví dụ: Directions là mãnh thú, Reactions lại là thú bông baby).

2. **Kiểm tra tương phản Dark Mode (`*-dark-check.png`)**:
   - Khi ghép thử nghiệm trên nền tối Slate `#0F172A`:
     - Không có viền trắng lởm chởm quanh lông tóc.
     - Các phần màu tối của linh thú không bị hòa lẫn biến mất vào nền.
     - Các chi tiết phát sáng (lửa, điện, ngọc) nổi bật rõ ràng, bắt mắt.

3. **Cố định trục tọa độ, không giật hình (Zero-Jitter Gaze)**:
   - Khi chuyển đổi liên tục giữa 9 góc nhìn hoặc đổi cảm xúc, tâm đầu nhân vật đứng yên tại vị trí trục, không bị thụt lên tụt xuống hay lắc lư lệch vị trí.

4. **Kích thước tệp tối ưu (High Performance Footprint)**:
   - Mỗi file `.webp` phải có kích thước dưới $300\text{ KB}$.
   - File preview GIF chuyển động dưới $600\text{ KB}$.

5. **Trọn bộ tệp bàn giao chuẩn (Deliverables Package)**:
   Trong thư mục `mascot-materials/<slug>/` phải có đủ 7 thành phần chuẩn hóa:
   - `source-reference.<ext>`: Ảnh gốc do người dùng cung cấp.
   - `<slug>-directions.webp`: Sprite sheet 9 hướng nhìn trong suốt.
   - `<slug>-reactions.webp`: Sprite sheet 9 phản ứng trong suốt.
   - `<slug>-animation-preview.gif`: GIF preview chạy liên tục các trạng thái.
   - `<slug>-dark-check.png`: Ảnh đối chiếu kiểm tra trên nền đen.
   - `<slug>-mascot-demo.html`: Bản demo tương tác chuột độc lập có thể mở trực tiếp trên trình duyệt.
   - File raw JPG phiên bản mới nhất tương ứng.

---

## 6. Hướng Dẫn Tái Hiện & Lệnh Tự Động Hóa

Để tự động build một linh thú mới từ 2 file ảnh ma trận thô `dir_raw.jpg` và `react_raw.jpg`:

```python
from pathlib import Path
from build_beast_mascots import process_beast

# Tự động hóa bóc tách nền, căn chỉnh trục, tạo WebP, GIF preview và Dark Check
process_beast(
    slug='ten-linh-thu',
    name_vi='Tên Tiếng Việt',
    dir_raw=Path('duong/dan/raw_directions.jpg'),
    react_raw=Path('duong/dan/raw_reactions.jpg')
)
```

Tất cả asset hoàn thiện sau đó chỉ cần đồng bộ vào `frontend/public/mascots/` để đưa vào giao diện sinh viên.
