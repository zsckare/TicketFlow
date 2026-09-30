package com.ticketflow.tickets.modules.inventory
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
object PricingTierTable:Table("pricing_tiers") { val id=uuid("id");val eventId=uuid("event_id");val sectionId=uuid("section_id");val name=varchar("name",100);val price=decimal("price",12,2);val currency=varchar("currency",3);val salesStartAt=timestampWithTimeZone("sales_start_at").nullable();val salesEndAt=timestampWithTimeZone("sales_end_at").nullable();val priority=integer("priority");val active=bool("active");val createdAt=timestampWithTimeZone("created_at");val updatedAt=timestampWithTimeZone("updated_at");override val primaryKey=PrimaryKey(id) }
