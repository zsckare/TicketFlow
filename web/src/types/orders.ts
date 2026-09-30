export type OrderStatus='PENDING'|'RESERVED'|'CONFIRMED'|'FAILED'|'CANCELLED'
export interface OrderItemResponse{id:string;inventoryId:string;reservationId?:string|null;eventId:string;sectionId?:string|null;seatId?:string|null;unitPrice:string;currency:string}
export interface OrderResponse{id:string;userId?:string|null;inventoryId?:string|null;reservationId?:string|null;paymentId?:string|null;amount:string;currency:string;status:OrderStatus;failureReason?:string|null;items:OrderItemResponse[];createdAt:string;updatedAt:string}
