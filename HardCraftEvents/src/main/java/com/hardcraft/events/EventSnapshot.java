package com.hardcraft.events;
record EventSnapshot(String id,EventState state,String material,long target,long progress,long reward,long startsAt){}