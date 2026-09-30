export type IssuedTicketStatus='ISSUED'|'USED'|'CANCELLED'
export interface IssuedTicketResponse{id:string;orderId:string;userId:string;eventId:string;inventoryId:string;sectionId?:string|null;seatId?:string|null;admissionToken:string;status:IssuedTicketStatus;issuedAt:string;checkedInAt?:string|null}
