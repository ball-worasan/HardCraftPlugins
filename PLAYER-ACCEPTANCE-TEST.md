# HardCraft player acceptance test — Events + Mobs

Run once on staging only. Use one Java client and one Bedrock client with distinct stable UUIDs. Keep both non-op except the operator issuing admin commands. Never use `/reload`.

## Evidence to record

For every step: client type, player name, UUID (`/hardcraft status`/server log), UTC timestamp, command, screenshot, relevant log lines. Redact addresses and credentials.

## Events

1. Start one configured event through its current admin command; both clients remain online.
2. Expected: both clients see the same event name, state, deadline and progress announcement.
3. Submit one valid contribution from each client using the configured event contribution command/action.
4. Expected: each contribution appears once; inventory/currency is deducted once; shared progress increases by the declared amount.
5. Restart Paper with `stop`, then normal start while the event is ACTIVE.
6. Expected: event resumes ACTIVE with identical deadline/progress/contributions; no refund or reward occurs.
7. Complete the event. Restart once during REWARDING if operationally possible, otherwise immediately after completion.
8. Expected: each eligible Java/Bedrock UUID receives exactly one reward; retry/reconnect adds none.
9. Start another event, contribute from both clients, cancel it, then restart.
10. Expected: each contribution is refunded exactly once; no completion reward; restart adds no second refund.

## Mobs

Operator command:

```text
/hcmob spawn stone_titan
```

1. Keep Java and Bedrock players within 32 blocks. Spawn `stone_titan`.
2. Expected: one named Stone Titan; maximum health is `200 + 50 × (nearby players - 1)`.
3. Each player deals at least 10 damage. Have one player leave range/server before the kill.
4. Expected: boss maximum/current health rescales proportionally, remains at least 1; phases change near 66% and 33% health.
5. Kill the boss.
6. Expected: each qualifying online participant receives exactly one diamond; a player below 10 damage receives none; reconnect/restart gives no duplicate.
7. Spawn another titan, damage it, then stop and restart Paper before death.
8. Expected: unfinished titan is removed after restart; nobody receives loot.
9. Kill a titan while one qualifying player's inventory is full, then free one slot and reconnect.
10. Expected: reward remains `PENDING`, then delivers once after reconnect. Restart before inventory delivery retries safely.
11. Force-stop only on staging after log/state reaches `DELIVERING`, once before and once after the item appears.
12. Expected: startup changes the claim to `UNCERTAIN`; no automatic retry. Compare inventory evidence with `hardcraft_mob_loot_audit`, then reconcile manually. Never change `UNCERTAIN` without player UUID, entity UUID, timestamp, and inventory evidence.

Audit query (copy the DB first; do not edit production):

```sql
SELECT l.entity_uuid,l.player_uuid,l.boss_id,l.status,l.updated_at,a.from_status,a.to_status,a.reason,a.created_at
FROM hardcraft_mob_loot l JOIN hardcraft_mob_loot_audit a USING(entity_uuid,player_uuid)
WHERE l.status='UNCERTAIN' ORDER BY a.created_at;
```

## Server commands and expected evidence

```powershell
# Clean build
D:\plugins\HardCraftCore\gradlew.bat --no-daemon --console=plain -p D:\plugins\HardCraftMobs clean test jar

# Artifact identity
Get-FileHash D:\plugins\HardCraftMobs\build\libs\HardCraftMobs-0.1.0.jar -Algorithm SHA256
jar tf D:\plugins\HardCraftMobs\build\libs\HardCraftMobs-0.1.0.jar
```

Expected: exit `0`; test XML reports zero failures/errors; JAR contains `plugin.yml`, `HardCraftMobs.class`, `StagingProbe.class`, SQLite driver. Client steps remain BLOCKED until the evidence above is returned; this does not block other module development.