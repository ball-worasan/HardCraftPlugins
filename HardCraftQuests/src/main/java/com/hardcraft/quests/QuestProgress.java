package com.hardcraft.quests;
import java.util.UUID;
record QuestProgress(UUID owner,String questId,int progress,State state,boolean moneyPaid,boolean itemDelivered) {
    enum State { ACTIVE, REWARDING, REVIEW_REQUIRED, COMPLETED }
}