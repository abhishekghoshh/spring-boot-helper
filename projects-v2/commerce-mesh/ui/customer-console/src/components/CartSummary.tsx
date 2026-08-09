import { Badge, IconButton } from '@mui/material';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import { useCart } from '../hooks/useCart';

interface CartSummaryProps {
  onClick?: () => void;
}

export default function CartSummary({ onClick }: CartSummaryProps) {
  const { itemCount } = useCart();

  return (
    <IconButton onClick={onClick} aria-label={`Cart with ${itemCount} items`} color="inherit">
      <Badge badgeContent={itemCount} color="primary">
        <ShoppingCartIcon />
      </Badge>
    </IconButton>
  );
}
