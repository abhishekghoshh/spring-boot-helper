import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'; import { cartApi, wishlistApi } from '../api/cartApi'; import { useAuth } from './useAuth'
interface CartCtx { cart: any; addItem: (i: any) => void; removeItem: (i: string) => void; updateQty: (i: string, q: number) => void; cartId: string; itemCount: number }
const C = createContext<CartCtx>({ cart: null, addItem: () => {}, removeItem: () => {}, updateQty: () => {}, cartId: '', itemCount: 0 })
export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth(); const [cartId, setCartId] = useState(''); const [cart, setCart] = useState<any>(null)
  useEffect(() => { let id = localStorage.getItem('cartId'); if (!id) { id = 'guest-' + Math.random().toString(36).substr(2, 9); localStorage.setItem('cartId', id) } setCartId(id); cartApi.get(id).then(setCart).catch(() => {}) }, [user])
  const addItem = useCallback((item: any) => cartApi.addItem(cartId, item).then(setCart), [cartId])
  const removeItem = useCallback((pid: string) => cartApi.removeItem(cartId, pid).then(setCart), [cartId])
  const updateQty = useCallback((pid: string, qty: number) => cartApi.updateQty(cartId, pid, qty).then(setCart), [cartId])
  return <C.Provider value={{ cart, addItem, removeItem, updateQty, cartId, itemCount: cart?.totalItems || 0 }}>{children}</C.Provider>
}
export function useCart() { return useContext(C) }
