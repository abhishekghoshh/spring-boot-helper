import { Routes, Route } from 'react-router-dom'; import Navbar from './components/layout/Navbar'; import Footer from './components/layout/Footer'
import Home from './pages/Home'; import ProductListing from './pages/ProductListing'; import ProductDetail from './pages/ProductDetail'
import CategoryLanding from './pages/CategoryLanding'; import SearchResults from './pages/SearchResults'
import ShoppingCart from './pages/ShoppingCart'; import Wishlist from './pages/Wishlist'; import Checkout from './pages/Checkout'
import OrderConfirmation from './pages/OrderConfirmation'; import OrderHistory from './pages/OrderHistory'; import OrderDetail from './pages/OrderDetail'
import Login from './pages/Login'; import Register from './pages/Register'; import ForgotPassword from './pages/ForgotPassword'
import MyAccount from './pages/MyAccount'; import NotFound from './pages/NotFound'
import { AuthProvider } from './hooks/useAuth'; import { CartProvider } from './hooks/useCart'

export default function App() {
  return <AuthProvider><CartProvider><Navbar />
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/products" element={<ProductListing />} />
      <Route path="/products/:id" element={<ProductDetail />} />
      <Route path="/category/:slug" element={<CategoryLanding />} />
      <Route path="/search" element={<SearchResults />} />
      <Route path="/cart" element={<ShoppingCart />} />
      <Route path="/wishlist" element={<Wishlist />} />
      <Route path="/checkout" element={<Checkout />} />
      <Route path="/order-confirmation/:id" element={<OrderConfirmation />} />
      <Route path="/orders" element={<OrderHistory />} />
      <Route path="/orders/:id" element={<OrderDetail />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/account" element={<MyAccount />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
    <Footer /></CartProvider></AuthProvider>
}
