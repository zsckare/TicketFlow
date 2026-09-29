export class ApiError extends Error { readonly status:number; constructor(message:string,status:number){super(message);this.name='ApiError';this.status=status} }
const BASE=(import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/,'')
const TOKEN_KEY='ticketflow.accessToken'
export const authStorage={get:()=>localStorage.getItem(TOKEN_KEY),set:(v:string)=>localStorage.setItem(TOKEN_KEY,v),clear:()=>localStorage.removeItem(TOKEN_KEY)}
export async function apiRequest<T>(path:string,options?:RequestInit):Promise<T>{
 const token=authStorage.get(); const url=path.startsWith('http')?path:`${BASE}${path.startsWith('/')?'':'/'}${path}`
 const response=await fetch(url,{...options,headers:{Accept:'application/json',...(token?{Authorization:`Bearer ${token}`} : {}),...options?.headers}})
 if(!response.ok){let message=`Request failed with status ${response.status}`;try{const b=await response.json() as {error?:string;message?:string};message=b.error??b.message??message}catch{};throw new ApiError(message,response.status)}
 if(response.status===204)return undefined as T; return await response.json() as T
}
export function json(body:unknown,method='POST'):RequestInit{return{method,headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}}
