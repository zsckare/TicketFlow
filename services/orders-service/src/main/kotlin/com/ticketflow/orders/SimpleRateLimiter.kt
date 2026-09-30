package com.ticketflow.orders
import java.util.concurrent.ConcurrentHashMap
class SimpleRateLimiter(private val limit:Int,private val windowMs:Long){private data class B(var s:Long,var c:Int);private val m=ConcurrentHashMap<String,B>();fun allow(k:String):Boolean{val n=System.currentTimeMillis();val b=m.computeIfAbsent(k){B(n,0)};synchronized(b){if(n-b.s>=windowMs){b.s=n;b.c=0};b.c++;return b.c<=limit}}}
