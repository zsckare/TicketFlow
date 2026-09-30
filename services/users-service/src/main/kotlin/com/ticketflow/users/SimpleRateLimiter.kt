package com.ticketflow.users

import java.util.concurrent.ConcurrentHashMap

class SimpleRateLimiter(private val limit:Int, private val windowMs:Long){
 private data class Bucket(var start:Long,var count:Int)
 private val buckets=ConcurrentHashMap<String,Bucket>()
 fun allow(key:String):Boolean { val now=System.currentTimeMillis(); val b=buckets.computeIfAbsent(key){Bucket(now,0)}; synchronized(b){if(now-b.start>=windowMs){b.start=now;b.count=0}; b.count++; return b.count<=limit} }
}
