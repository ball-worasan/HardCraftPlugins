# HardCraftPlugins

## หลักฐานการติดตั้ง (เอกสาร §2)

เก็บไว้ 26 ก.ย. 2026 · เซิร์ฟ `HardCraft` (id `54d4bd98`) บน node `mc-th-bkk-01`

### เวอร์ชัน

| รายการ | ค่า |
|---|---|
| Paper | 26.2-129-ver/26.2@9240f58 (API 26.2.build.129-stable) |
| Java | Temurin 25.0.4+7-LTS |
| Kernel | Linux 6.8.0-142-generic amd64 |

### ปลั๊กอิน 9 ตัว (โหลดครบตาม startup log)

`ProtocolLib 5.5.0-SNAPSHOT-583353e` · `CoreProtect 24.1` · `floodgate 2.2.5-SNAPSHOT (b141-81b65cc)` ·
`Geyser-Spigot 2.11.3-SNAPSHOT` · `LuckPerms 5.5.85` · `PlaceholderAPI 2.12.3` ·
`ViaVersion 5.12.1-SNAPSHOT` · `WorldEdit 7.4.5+7590-b8dc4c1` · `WorldGuard 7.0.19+2400-f395a16`

### การตั้งค่า authentication

จาก `server.properties` / `plugins/Geyser-Spigot/config.yml` / `plugins/floodgate/config.yml`

| ค่า | ค่าจริง | หมายเหตุ |
|---|---|---|
| `online-mode` | `true` | Java ต้องมีบัญชี Mojang จริง ผู้เล่น cracked จะเข้าไม่ได้ |
| `enforce-secure-profile` | `true` | ไคลเอนต์ที่ไม่ลงนาม Chat key จะถูกเตะ |
| `white-list` | `false` | ใครก็เข้าได้ — ยังไม่มี claim (§4 ระบุว่าต้องส่ง `HardCraftWorld` ก่อนเปิดสาธารณะ) |
| `server-port` | `25565` | allocation เดียวของเซิร์ฟ |
| Geyser `auth-type` | `floodgate` | Bedrock เข้าผ่าน Floodgate ไม่ผ่าน Mojang |
| Geyser `address` / effective `port` | `0.0.0.0` / `25565` | `clone-remote-port: true`; ใช้ allocation เดียวกับ Java แต่คนละ protocol |
| Geyser `transport` | `raknet` | |
| Geyser `advertise-addresses` | `[]` | ไม่มี public UDP endpoint ถูกประกาศ |
| Floodgate `key.pem` | มีไฟล์ 16 bytes | key pair ถูกสร้างแล้ว |
| Floodgate `username-prefix` | `"."` | ชื่อ Bedrock ในเกมจะขึ้นต้นด้วย `.` — ยืนยันว่าห้ามใช้ username เป็น primary key (§3) |

### สถานะการทดสอบผู้เล่น

- **Java LAN ผ่านแล้ว:** `SomeBAll56` จาก `192.168.1.40:60865` เข้าเกมสำเร็จที่ `22:06:05`
- Java UUID: `ec933eeb-0afc-41ea-b8a0-9dd2f9a8d35c`; ตั้ง operator สำเร็จ
- **Bedrock login ยังไม่ทดสอบด้วยผู้เล่นจริง**
- Bedrock network พร้อมทดสอบ: startup log ยืนยัน `Started Geyser on UDP port 25565`

### ผลการตรวจ API ของ panel (26 ก.ย. 2026)

ตรวจด้วย Application API key:

| Route | `Allow` | หมายเหตุ |
|---|---|---|
| `GET /api/application/servers` | GET | list ได้ 1 เซิร์ฟ |
| `GET /api/application/servers/54d4bd98` | GET, HEAD, DELETE | **ไม่มี PATCH** |
| `POST /api/application/servers/54d4bd98/allocations` | — | **405 — route ไม่มี** |
| `DELETE /api/application/servers/54d4bd98/allocations` | DELETE | ลบได้อย่างเดียว |
| `GET /api/application/nodes/1/allocations` | GET, HEAD, POST | node มี allocation เดียว: `100.126.163.93:25565` |

Route allocation แบบเฉพาะถูกตัดออกจาก build นี้ แต่ภายหลังเปลี่ยน primary allocation ผ่าน server update flow ของ panel สำเร็จ

| Route | `Allow` | หมายเหตุ |
|---|---|---|
| `PATCH /api/application/nodes/1` | GET, HEAD, PATCH, DELETE | node แก้ได้ |
| `PATCH /api/application/servers/54d4bd98` | — | **ไม่มี PATCH — เปลี่ยน network เซิร์ฟไม่ได้** |
| `GET /api/application/nodes` | GET | response **ไม่มี field `ipam` เลย** ดู IP ที่ node ใช้ได้ไม่ได้ |

Node `mc-th-bkk-01` (`wings.worasan.com`, daemon `8443`) มี wildcard allocation `0.0.0.0:25565` (`id=11`) เป็น primary แล้ว

### ผลตั้งค่า Geyser ผ่าน Client API (26 ก.ย. 2026)

- เปิด `clone-remote-port: true` ใน `plugins/Geyser-Spigot/config.yml`
- restart สำเร็จ; สถานะ API: `running`
- Paper: `Starting Minecraft server on 0.0.0.0:25565` (TCP)
- Geyser: `Started Geyser on UDP port 25565`
- startup: `Done (18.743s)`; ไม่พบ startup error
- Java และ Bedrock ใช้เลขพอร์ต `25565` ร่วมกันได้เพราะใช้คนละ protocol
- Java AC ผ่านแล้ว; เหลือผู้เล่นจริง join ผ่าน Bedrock แล้วตรวจ Floodgate UUID/auth ใน log

### LAN allocation (26 ก.ย. 2026)

- Primary allocation: `0.0.0.0:25565` (`id=11`); bind ทุก interface ของ host
- Allocation เดิม `100.126.163.93:25565` (`id=3`) ถูกปลดแล้ว
- Allocation limit คืนเป็น `1`
- LAN endpoint สำหรับผู้เล่น: `192.168.1.42:25565`
- Java ใช้ TCP; Bedrock ใช้ UDP บนเลขพอร์ตเดียวกัน
- หลัง restart: API state `running`; Paper และ Geyser เริ่มบน `0.0.0.0:25565`; startup `Done (18.548s)`

### งานคงเหลือ

1. ใช้ Bedrock join `192.168.1.42:25565`; ตรวจ Geyser/Floodgate login, UUID และชื่อที่ขึ้นต้น `.` ใน `latest.log`
2. บน host ยืนยัน Docker publish ทั้ง `25565/tcp` และ `25565/udp` ด้วย `ss` และ `docker ps`
3. Rotate Application API key และ Client API key ที่เคยเปิดเผย
4. อัปเดต Paper เมื่อมี stable build ใหม่; เฝ้าดู `Accessing poi chunk off-main` ซึ่งเกิดตอน shutdown ระหว่าง world generation

Java LAN และการบันทึกโลกผ่านแล้ว; shutdown ล่าสุดจบด้วย `All dimensions are saved` จึงยังไม่มีหลักฐานว่าโลกเสียหาย

## HardCraftCore Foundation

เลือก **SQLite** สำหรับเซิร์ฟ Paper เครื่องเดียว: ไม่มี credential, deploy/restore ง่าย, ยังไม่ต้องมี DB server เพิ่ม.
ฐานข้อมูลอยู่ที่ `plugins/HardCraftCore/hardcraft.db`; migration สร้าง `hardcraft_schema_version` schema 1 แบบ transaction.
หากเปิด DB แล้ว path/ไฟล์ใช้ไม่ได้ Core จะ disable พร้อม error และไม่ลงทะเบียน API.

### Backup / restore (ต้องแยกจากเซิร์ฟจริง)

1. สั่ง `stop`; รอ log `All dimensions are saved` และ process จบ เพื่อปิด SQLite connection ก่อน copy.
2. copy `plugins/HardCraftCore/hardcraft.db` ไป storage นอก server พร้อม timestamp; ห้ามใช้ CoreProtect แทน backup.
3. Restore test: copy backup เข้า directory staging ใหม่เป็น `hardcraft.db`; ห้ามเขียนทับ production.
4. เปิด test serverด้วยสำเนาโลก/config/JAR; ยืนยัน log `SQLite schema=1; migration=UP_TO_DATE` และ `/hardcraft status` แสดง `UP_TO_DATE`.
5. หลังยืนยัน staging เท่านั้น จึงหยุด production, เก็บสำเนาไฟล์เดิม, แล้วแทนด้วย backup ที่ทดสอบแล้ว.

Unit test `MigrationRunnerTest` จำลอง migrate → ปิด connection → backup → ลบต้นฉบับ → restore คนละ path → อ่าน schema.
Build `0.1.1` วันที่ 27 ก.ย. 2026 ผ่าน 8 tests; SHA-256 `C6BDF722BBD7D50262F9A6482034B1CA638F2823020BDECEF1AEC6545FF78009`.

### AC ที่ยืนยันแล้ว / ยังขาด

- ผ่านจาก production log จริง: Paper 26.2/Java 25 โหลด Core 0.1.1; startup และ restart รอบสองแสดง `SQLite schema=1; migration=UP_TO_DATE`, API contract 1 และ server state `running`.
- ผ่านจาก source/unit test เดิม: service ที่ไม่มีคืน `Optional.empty`; config ตรวจชนิด/ช่วง; disable ล้าง services/tasks.
- ผ่านระดับ unit: SQLite migration ใหม่/เดิมรายงาน `APPLIED`/`UP_TO_DATE`, restart ไม่เพิ่ม schema row, restore ไป path แยกอ่าน schema 1 ได้.
- ผ่าน staging แยกบน Paper 26.2/Java 25: สำเนา production DB เปิดเป็น `UP_TO_DATE`; bad path `../outside.db` รายงาน error ชัดและ disable Core โดยไม่แตะ production.
- ยังไม่ผ่าน: `/hardcraft status`, `/hardcraft reload` และ denial ด้วยผู้เล่น non-op บนเซิร์ฟจริง.
- ยังไม่ผ่าน: Bedrock/Floodgate login และ UUID; ห้ามเริ่ม schema Profiles ก่อนตรวจข้อนี้.