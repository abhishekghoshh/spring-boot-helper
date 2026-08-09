import {
  createContext,
  useContext,
  useState,
  useCallback,
  useMemo,
  useEffect,
  type ReactNode,
} from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { cartApi } from '../api/cartApi';
import type { CartItem } from '../types';

interface CartContextType {
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  discount: number;
  total: number;
  couponCode: string | null;
  couponDiscount: number;
  isLoading: boolean;
  addItem: (product: Omit<CartItem, 'quantity'> & { quantity?: number }) => void;
  updateQuantity: (productId: string, quantity: number) => void;
  removeItem: (productId: string) => void;
  clearCart: () => void;
  applyCoupon: (code: string) => Promise<void>;
  syncWithServer: () => Promise<void>;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [discount, setDiscount] = useState(0);
  const [couponCode, setCouponCode] = useState<string | null>(null);
  const [couponDiscount, setCouponDiscount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const queryClient = useQueryClient();

  const itemCount = useMemo(() => items.reduce((s, i) => s + i.quantity, 0), [items]);
  const subtotal = useMemo(() => items.reduce((s, i) => s + i.price * i.quantity, 0), [items]);
  const total = useMemo(() => Math.max(0, subtotal - discount), [subtotal, discount]);

  // Fetch cart from server on mount if authenticated
  useEffect(() => {
    const token = localStorage.getItem('customerToken');
    if (token) {
      setIsLoading(true);
      cartApi
        .getCart()
        .then((cart) => {
          setItems(cart.items);
          setDiscount(cart.couponDiscount || 0);
          setCouponCode(cart.couponCode || null);
          setCouponDiscount(cart.couponDiscount || 0);
        })
        .catch(() => {
          // Cart may be empty or not yet created
        })
        .finally(() => setIsLoading(false));
    }
  }, []);

  const syncWithServer = useCallback(async () => {
    try {
      const cart = await cartApi.getCart();
      setItems(cart.items);
      setDiscount(cart.couponDiscount || 0);
      setCouponCode(cart.couponCode || null);
      setCouponDiscount(cart.couponDiscount || 0);
    } catch {
      // ignore
    }
  }, []);

  const addItem = useCallback(
    (product: Omit<CartItem, 'quantity'> & { quantity?: number }) => {
      const qty = product.quantity || 1;
      // Optimistic update
      setItems((prev) => {
        const existing = prev.find((i) => i.productId === product.productId);
        if (existing) {
          return prev.map((i) =>
            i.productId === product.productId ? { ...i, quantity: i.quantity + qty } : i,
          );
        }
        return [...prev, { productId: product.productId, name: product.name, image: product.image, price: product.price, quantity: qty }];
      });
      // Sync with server
      cartApi.addToCart(product.productId, qty).catch(() => {});
    },
    [],
  );

  const updateQuantity = useCallback((productId: string, quantity: number) => {
    if (quantity <= 0) {
      setItems((prev) => prev.filter((i) => i.productId !== productId));
      cartApi.removeFromCart(productId).catch(() => {});
    } else {
      setItems((prev) =>
        prev.map((i) => (i.productId === productId ? { ...i, quantity } : i)),
      );
      cartApi.updateCartItem(productId, quantity).catch(() => {});
    }
  }, []);

  const removeItem = useCallback((productId: string) => {
    setItems((prev) => prev.filter((i) => i.productId !== productId));
    cartApi.removeFromCart(productId).catch(() => {});
  }, []);

  const clearCart = useCallback(() => {
    setItems([]);
    setDiscount(0);
    setCouponCode(null);
    setCouponDiscount(0);
    cartApi.clearCart().catch(() => {});
    queryClient.invalidateQueries({ queryKey: ['cart'] });
  }, [queryClient]);

  const applyCoupon = useCallback(async (code: string) => {
    const cart = await cartApi.applyCoupon(code);
    setItems(cart.items);
    setDiscount(cart.couponDiscount || 0);
    setCouponCode(cart.couponCode || code);
    setCouponDiscount(cart.couponDiscount || 0);
  }, []);

  const value = useMemo(
    () => ({
      items,
      itemCount,
      subtotal,
      discount,
      total,
      couponCode,
      couponDiscount,
      isLoading,
      addItem,
      updateQuantity,
      removeItem,
      clearCart,
      applyCoupon,
      syncWithServer,
    }),
    [items, itemCount, subtotal, discount, total, couponCode, couponDiscount, isLoading, addItem, updateQuantity, removeItem, clearCart, applyCoupon, syncWithServer],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
