package com.hardcraft.quests;
record QuestDefinition(String id,String title,String material,int amount,long money,String itemId,String next) {
    QuestDefinition { if(id==null||id.isBlank()||title==null||title.isBlank()||material==null||material.isBlank()||amount<1||money<1||itemId==null||itemId.isBlank()||next==null||next.isBlank())throw new IllegalArgumentException("invalid quest definition"); }
}