# HardCraft — Agent Execution Playbook v1

**วันที่:** 26 กันยายน 2026  
**ใช้ร่วมกับ:** `HardCraft-Plugin-Requirements-v1.md` (รายละเอียด AC รายระบบ)  
**ฐานปัจจุบันตามรายงาน:** Paper 26.2-129, Java 25, external plugins 9 ตัว, `HardCraftCore` เป็น custom plugin ตัวแรก, SQLite schema 1

## 1. วิธีทำงานใหม่: หนึ่งรายงานต่อหนึ่ง milestone

ผู้ใช้ส่ง **Master Prompt** ด้านล่างให้ coding agent ที่เข้าถึง `D:\plugins` และเซิร์ฟได้จริง Agent อ่านโค้ด/เอกสาร, ทำงาน, build, ทดสอบ, แก้ปัญหาระหว่างทาง, deploy ในสภาพแวดล้อมทดสอบที่เตรียมไว้, ตรวจ log แล้วส่ง **Milestone Report** ฉบับเดียว ผู้ใช้ส่งรายงานนั้นมาให้ผู้รีวิว ผู้รีวิวตอบเพียง `GO`, `FIX` หรือ `BLOCK` พร้อมคำสั่งถัดไปหนึ่งชุด

```text
ผู้ใช้ → Coding agent: งานหนึ่ง milestone
Coding agent → ผู้ใช้: โค้ด + ผลทดสอบ + รายงานฉบับเดียว
ผู้ใช้ → ผู้รีวิว: ส่งรายงาน
ผู้รีวิว → ผู้ใช้: GO / FIX / BLOCK + คำสั่งรอบถัดไป
```

**ข้อยกเว้นที่ต้องถามระหว่างทาง:** ต้องใช้บัญชีผู้เล่นจริง, การเปลี่ยน authentication/UUID, ลบหรือย้ายข้อมูลจริง, สิทธิ์/credential ที่ยังไม่มี, restore ข้อมูลจริง, การตัดสินใจเรื่อง gameplay/economy ที่ขัดกับ spec, หรือไม่มีทางทำงานให้ปลอดภัยได้ ข้ออื่นให้ agent ตัดสินใจและบันทึกเหตุผลในรายงาน ไม่ต้องหยุดถามทุกคำสั่ง

**ขอบเขตหนึ่ง milestone:** ให้มีเป้าหมายที่สาธิตได้หนึ่งเรื่อง เช่น `Profiles persistence` หรือ `Economy atomic transfer`; ไม่สั่งสร้าง 15 ปลั๊กอินรวดเดียว แต่ไม่แบ่งงานเล็กถึงระดับ “เช็ก log หนึ่งบรรทัด”

## 2. Master Prompt สำหรับ coding agent

คัดลอกข้อความต่อไปนี้ แล้วแทน `[MILESTONE]` ด้วยงานจากหัวข้อ 4:

> คุณเป็น coding agent ของ HardCraft ให้ทำ milestone `[MILESTONE]` ให้เสร็จแบบตรวจสอบได้ใน repository `D:\plugins\HardCraftCore` และ workspace ที่เกี่ยวข้อง อ่าน `D:\plugins\HardCraft-Plugin-Requirements-v1.md`, `D:\plugins\README.md`, โค้ดปัจจุบัน, build files, config, test, server version และ plugin list จริงก่อนแก้ ห้ามถือว่ารายงานเก่าตรงกับสถานะไฟล์ปัจจุบัน ให้สรุป baseline สั้น ๆ ในรายงานท้ายงาน
>
> ทำตาม AC ของ milestone ทั้ง implementation, migration, permission, restart/retry, error path, Java/Bedrock UI ที่เกี่ยวข้อง, และเอกสาร deploy/rollback แยกชั้น Core, API และ domain ให้ชัด ระบบหนึ่งเป็นเจ้าของข้อมูลตัวเอง ห้าม query ตารางข้าม domain; ธุรกรรมเงิน/ของ/รางวัลต้อง atomic หรือมี recovery ที่พิสูจน์ได้และใช้ idempotency key ตามความเหมาะสม ห้ามทำ I/O บน Paper main thread ห้ามเก็บ secret ใน repo หรือพิมพ์ในรายงาน
>
> แก้ปัญหาการ build/test ที่พบระหว่างทางเองก่อนรายงาน ใช้การทดสอบที่พิสูจน์พฤติกรรมจริง ไม่เพิ่ม test ที่แค่สะท้อน implementation; ตรวจผล process exit, XML test report และเนื้อหา JAR ไม่สรุปว่า PASS จากคำสั่งที่ยังรันไม่จบ เพิ่ม patch version ของ custom plugin ทุกครั้งที่ artifact เปลี่ยน และบันทึก SHA-256, ขนาด JAR, เวลา build
>
> ถ้าต้อง deploy ให้ใช้ test server ก่อน; ถ้ามีเพียงเซิร์ฟเดียว ให้ทำในช่วง maintenance, backup ให้เสร็จ, เก็บ JAR เดิม, บันทึกขั้นตอนย้อนกลับ, stop → แทน JAR เพียงชุดเดียว → start → ตรวจ log/คำสั่ง → restart อีกครั้ง ห้าม `/reload` เป็นวิธีทดสอบ lifecycle ห้ามเปลี่ยนข้อมูลผู้เล่นจริงโดยไม่มีแผนกู้คืน
>
> ทำงานต่อเนื่องจน AC ของ milestone ผ่านหรือพบ blocker ที่ต้องใช้ผู้ใช้จริง ห้ามตอบสถานะย่อยทีละบรรทัด ปิดท้ายด้วยรายงานรูปแบบหัวข้อ 6 เท่านั้น โดยจัดทุก AC เป็น PASS / FAIL / BLOCKED / NOT TESTED พร้อมหลักฐานและระบุข้อจำกัด หากต้องใช้ผู้เล่น Java/Bedrock ทดสอบ ให้รวบเป็นคำขอเดียวที่มีขั้นตอนและข้อมูลที่ต้องส่งกลับ ไม่เดาผล

## 3. กฎการลงมือที่ช่วยให้เร็วโดยไม่วน

1. **ตรวจสิ่งที่มีอยู่ก่อนแก้**: `git status`, โครง repo, version, migrations, AC ที่ผ่านแล้ว และไฟล์ที่เปลี่ยนตั้งแต่รายงานก่อนหน้า; ห้ามสร้างระบบเดิมซ้ำ
2. **กำหนด API ก่อน DB**: ระบุ input/output/error และเจ้าของข้อมูล แล้วจึง migration; ข้อมูลผู้เล่นใช้ UUID ตาม identity ที่ทดสอบแล้ว
3. **ทำ vertical slice**: persistence → service → command/UI ขั้นต่ำ → failure path → build/test → server smoke test ใน milestone เดียว
4. **สร้างข้อพิสูจน์เฉพาะความเสี่ยง**: concurrency, idempotency, restart, rollback และการข้าม Java/Bedrock; ไม่ต้องสร้าง test ที่เพียงตรวจ getter
5. **บันทึก artifact identity**: version + SHA-256 + size + commit; log ต้องระบุ version, schema และเหตุผลเมื่อ disable
6. **แยกผลลัพธ์**: `unit PASS` ไม่ใช่ `server PASS`; `server plugin loaded` ไม่ใช่ `feature PASS`; `backup file exists` ไม่ใช่ `restore PASS`
7. **หยุดในจุดที่มีผู้ใช้จริงเท่านั้น**: Bedrock login, non-op gameplay, playtest 3–5 คน เป็น BLOCKED พร้อมคำสั่งทดสอบชุดเดียว; ทำส่วนที่ไม่ต้องรอให้ครบก่อน

## 4. Milestone และนิยามว่าทำอะไรได้บ้าง

ลำดับนี้เป็น **ลำดับลงมือ** ไม่ได้บังคับให้มี 16 JAR จริง สามารถแยก Gradle module ก่อนแล้วรวม JAR ชั่วคราวได้ ถ้า API/ข้อมูลยังไม่เหมาะแยก deploy

| ลำดับ | ระบบ / milestone | ความสามารถขั้นต่ำที่ต้องส่งมอบ | Gate ผ่าน milestone |
|---|---|---|---|
| F1 | `HardCraftCore`: SQLite / lifecycle | DB config, schema migration, service registry, `/hardcraft status`, disable ที่ปลอดภัย, async I/O pattern | build+tests ผ่าน; JAR identity ชัด; server migration `APPLIED` ครั้งแรกและ `UP_TO_DATE` เมื่อ restart; invalid config fail-fast; non-op permission ผ่าน |
| F2 | Backup + Identity | backup ที่กู้คืนในพื้นที่ทดสอบได้, Java/Bedrock UUID คงเดิม, key rotation, runbook | restore ของโลก+SQLite ที่สอดคล้องกันสำเร็จ; Bedrock เข้าออกซ้ำ UUID ไม่ชน Java; ไม่รั่ว secret |
| M1 | `HardCraftProfiles` | สร้าง/อ่านโปรไฟล์ UUID, first/last join, playtime, settings, offline lookup | Java/Bedrock มีโปรไฟล์เดียวต่อ UUID; restart ยังอยู่; async DB; ชื่อเปลี่ยนไม่สร้างบัญชีใหม่ |
| M2 | `HardCraftItems` | registry item ID, สร้าง/อ่าน PDC, version data, unknown ID, transfer rules | วาง/เก็บ/รีสตาร์ตยังระบุ ID; เปลี่ยนชื่อของไม่เปลี่ยนชนิด; กฎ stack/transfer ผ่านกรณีที่ระบุ |
| M3 | `HardCraftEconomy` | wallet, mint/burn/transfer, ledger, transaction ID, idempotency | concurrent transfer ไม่ติดลบ/เงินไม่หาย; retry/restart ไม่จ่ายซ้ำ; ledger ตรงยอดรวม |
| M4 | `HardCraftQuests` | onboarding quest, objective ฝั่ง server, progress, รางวัล Items/Economy | เข้าเกมทำเควสต์รับของ+เงินออกเข้าใหม่แล้วยังอยู่; กดซ้ำ/รีสตาร์ตไม่รับซ้ำ |
| A1 | `HardCraftSkills` | เริ่ม 2–3 สาย, XP, level, next unlock | anti-farm เบื้องต้น; XP/level persist; unlock ให้ครั้งเดียว; เปลี่ยนสูตรมีนโยบาย |
| A2 | `HardCraftCollections` | สะสม mob/biome/item ตามชุดเริ่มต้น, milestone | การค้นพบเดิมไม่ซ้ำ; ความคืบหน้าคงอยู่; เปิดความลับตามนโยบาย |
| A3 | `HardCraftEvents` | scheduler, state, community contribution, รางวัล event แรก | restart ระหว่าง active/rewarding ไม่จ่ายซ้ำ; ส่งทรัพยากรแล้วนับตรงจำนวนจริง; ยกเลิกมี recovery |
| B1 | `HardCraftMobs` | mob/boss หนึ่งตัว, phases, loot, participation | boss ตายจ่าย loot ครั้งเดียว; phase ถูก; ทดสอบ performance ตามจำนวนผู้เล่นเป้าหมาย |
| B2 | `HardCraftMarket` | listing, item escrow, ซื้อ/ยกเลิก, tax | ผู้ซื้อแข่งกันชนะหนึ่งคน; ของ/เงินไม่หายหรือซ้ำ; ขายของผูกเจ้าของไม่ได้ |
| B3 | `HardCraftGuilds` | invite, role, guild project, guild balance ผ่าน Economy | permission ถูก; สมาชิกส่งของไม่ซ้ำ; เงินกิลด์มี ledger; หัวหน้าหายมีวิธีจัดการ |
| X1 | `HardCraftWorld` | world zones และ **player claim** | บ้าน/หีบ/ขอบเขตปลอดภัย; piston/fluid/explosion/hopper ตาม threat model; Admin แก้ข้อพิพาทได้ |
| X2 | `HardCraftBounty` | ฝากเงินตั้งค่าหัว, kill eligibility, payout/cancel | เงินกันจริง; safe zone/ฆ่าตัวเอง/ฆ่าซ้ำไม่รับเงิน; จ่ายหรือคืนครั้งเดียว |
| X3 | `HardCraftAchievements` | achievement, world first, Hall of Fame | world first ตัดสินแบบ atomic; ไม่แจกซ้ำ; ประวัติข้ามซีซันได้ |
| X4 | `HardCraftSeasons` | เปิดเนื้อหาซีซัน, soft reset ที่ระบุชัด | dry run+backup; reset รอบเดียว; บ้าน/ประวัติ/collection ที่ต้องเก็บยังอยู่ |
| X5 | `HardCraftAnalytics` | onboarding funnel, money source/sink, active/retention | metric มีสูตร; เทียบข้อมูลจริง; telemetry ล้มไม่กระทบ gameplay |

**ปรับลำดับสำคัญ:** `HardCraftWorld` ส่วน claim ต้องมาก่อน **public launch** แม้ตารางจะอยู่ Expansion. Event/Mobs/Guild อาจสลับภายในระยะได้ตามผล playtest แต่ F1/F2 และ MVP ต้องผ่านก่อนระบบที่ใช้เงิน/ของอย่างจริงจัง

## 5. กติกาการตัดสิน GO / FIX / BLOCK

| ผลรีวิว | เงื่อนไข | คำสั่งรอบถัดไป |
|---|---|---|
| `GO` | AC หลักของ milestone ผ่านพร้อมหลักฐาน build/test และ smoke/restart ที่เกี่ยวข้อง; ไม่มีความเสี่ยงข้อมูลค้าง | ระบุ milestone ถัดไปและสิ่งที่ reuse |
| `FIX` | มีบั๊กหรือ AC ตกที่ agent แก้เองได้ | คำสั่งแก้จุดที่ตกทั้งหมดในหนึ่งรอบ; ไม่ให้กลับไปทำสิ่งที่ผ่านแล้ว |
| `BLOCK` | ขาดผลผู้เล่นจริง/ข้อมูลสิทธิ์/การตัดสินใจที่มีผลต่อข้อมูล | คำขอข้อมูลชุดเดียว; ให้ agent ทำงานอิสระอื่นที่เหลือระหว่างรอ |

**ความผิดพลาดที่ห้ามปล่อยผ่าน:** deploy JAR ไม่ตรง hash, migration ทำข้อมูลหาย, เงิน/ของซ้ำ, main thread block จาก DB, ผู้เล่นสวมสิทธิ์ Admin ได้, backup ที่ไม่เคย restore, secret โผล่ใน log/repo, Bedrock identity ไม่เสถียร

## 6. Milestone Report Template (ให้ agent ส่งทุกครั้ง)

```markdown
# HardCraft Milestone Report — <ID / ชื่อ>

## สถานะ
PASS / FAIL / BLOCKED | commit: <hash> | plugin version: <version>

## Baseline ที่ตรวจจากของจริง
Paper/Java, external plugins ที่เกี่ยวข้อง, schema ก่อนหน้า, ไฟล์ที่มีอยู่

## สิ่งที่เปลี่ยน
ไฟล์/โมดูล, API, ตาราง/migration, config, คำสั่ง/permission, behavior ที่ผู้เล่นเห็น

## Acceptance Criteria
| AC | ผล PASS/FAIL/BLOCKED/NOT TESTED | หลักฐาน (test/log/คำสั่ง) |

## Verification
build command + exit result; test totals + XML path; JAR path/version/size/SHA-256;
server startup/restart evidence; Java/Bedrock/non-op/concurrency test ที่เกี่ยวข้อง

## Data safety
backup location/ผล restore ในที่แยก; schema before/after; rollback steps;
secret exposure: none/ระบุว่าต้อง rotate (ไม่แสดงค่า)

## Known gaps และข้อเสนอถัดไป
ข้อที่ยังไม่ผ่าน, blocker ที่ผู้ใช้ต้องทำจริง (รวมคำขอเป็นชุดเดียว),
milestone ที่เสนอถัดไป, เหตุผล 1–2 บรรทัด
```

รายงานแนบ log เฉพาะบรรทัดที่พิสูจน์ AC; ห้ามส่ง console ทั้งก้อนซ้ำ เว้นแต่ต้องวิเคราะห์ error ใหม่ และไม่ต้องรายงานสถานะทุกครั้งที่กดคำสั่ง

## 7. คำสั่งสำหรับเริ่มรอบถัดไปทันที

> ใช้ Master Prompt หัวข้อ 2 โดยตั้ง `[MILESTONE] = F1: ปิด AC ของ HardCraftCore หลังแก้ migration log` ตรวจ build/test ปัจจุบันและ JAR identity, ทดสอบ deploy/restart สองรอบ, status, non-op permissions, lifecycle duplication, invalid config ใน test environment; รวบ Bedrock F2 เป็นคำขอทดสอบเดียวถ้าไม่มี client ทดสอบเอง รายงานตามหัวข้อ 6 แล้วหยุดรอผลรีวิว

หลัง F1 ผ่าน ให้ทำ F2 ครั้งเดียว แล้วเริ่ม M1 ต่อ โดยไม่ย้อนกลับมาตรวจ build F1 ซ้ำถ้าไม่มีโค้ด Core เปลี่ยน
