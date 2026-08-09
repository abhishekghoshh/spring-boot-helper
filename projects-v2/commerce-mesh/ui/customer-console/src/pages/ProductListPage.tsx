import { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Box,
  Typography,
  Grid,
  TextField,
  MenuItem,
  Slider,
  FormControlLabel,
  Checkbox,
  Rating,
  Button,
  Pagination,
  Skeleton,
  Drawer,
  IconButton,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import TuneIcon from '@mui/icons-material/Tune';
import { useProducts, useCategories } from '../hooks/useProducts';
import { useCart } from '../hooks/useCart';
import { useWishlist, useAddToWishlist, useRemoveFromWishlist } from '../hooks/useWishlist';
import ProductCard from '../components/ProductCard';
import type { ProductFilters } from '../types';

const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price-asc', label: 'Price: Low to High' },
  { value: 'price-desc', label: 'Price: High to Low' },
  { value: 'rating', label: 'Highest Rated' },
];

export default function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const categoryParam = searchParams.get('category') || '';
  const searchParam = searchParams.get('search') || '';
  const pageParam = parseInt(searchParams.get('page') || '1', 10);

  const [sort, setSort] = useState<string>('newest');
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 1000]);
  const [minRating, setMinRating] = useState<number>(0);
  const [filterOpen, setFilterOpen] = useState(false);

  const filters: ProductFilters = useMemo(
    () => ({
      category: categoryParam || undefined,
      search: searchParam || undefined,
      page: pageParam,
      pageSize: 12,
      sort: sort as ProductFilters['sort'],
      minPrice: priceRange[0] > 0 ? priceRange[0] : undefined,
      maxPrice: priceRange[1] < 1000 ? priceRange[1] : undefined,
      minRating: minRating > 0 ? minRating : undefined,
    }),
    [categoryParam, searchParam, pageParam, sort, priceRange, minRating],
  );

  const { data, isLoading, isError } = useProducts(filters);
  const { data: categories } = useCategories();
  const { addItem } = useCart();
  const { data: wishlist = [] } = useWishlist();
  const addToWishlist = useAddToWishlist();
  const removeFromWishlist = useRemoveFromWishlist();

  const wishlistIds = new Set(wishlist.map((p) => p.id));
  const products = data?.data || [];
  const totalPages = data?.totalPages || 1;

  const handleToggleWishlist = (productId: string) => {
    if (wishlistIds.has(productId)) {
      removeFromWishlist.mutate(productId);
    } else {
      addToWishlist.mutate(productId);
    }
  };

  const handlePageChange = (_: unknown, page: number) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', String(page));
    setSearchParams(params);
    window.scrollTo(0, 0);
  };

  const handleCategoryChange = (cat: string, checked: boolean) => {
    const params = new URLSearchParams(searchParams);
    if (checked) {
      params.set('category', cat);
    } else {
      params.delete('category');
    }
    params.delete('page');
    setSearchParams(params);
  };

  const filterContent = (
    <Box sx={{ width: 260, flexShrink: 0 }}>
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>
        Filters
      </Typography>

      <Typography variant="body2" fontWeight={600} sx={{ mt: 2, mb: 1 }}>
        Category
      </Typography>
      {(categories || []).slice(0, 8).map((cat) => (
        <FormControlLabel
          key={cat.id}
          control={
            <Checkbox
              size="small"
              checked={categoryParam === cat.name}
              onChange={(_, checked) => handleCategoryChange(cat.name, checked)}
            />
          }
          label={cat.name}
          sx={{ display: 'block', '& .MuiTypography-root': { fontSize: 14 } }}
        />
      ))}

      <Typography variant="body2" fontWeight={600} sx={{ mt: 2, mb: 1 }}>
        Price Range
      </Typography>
      <Slider
        value={priceRange}
        onChange={(_, v) => setPriceRange(v as [number, number])}
        min={0}
        max={1000}
        step={10}
        valueLabelDisplay="auto"
        valueLabelFormat={(v) => `$${v}`}
      />
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <TextField size="small" label="Min" value={priceRange[0]} onChange={(e) => setPriceRange([+e.target.value || 0, priceRange[1]])} sx={{ width: 100 }} />
        <TextField size="small" label="Max" value={priceRange[1]} onChange={(e) => setPriceRange([priceRange[0], +e.target.value || 1000])} sx={{ width: 100 }} />
      </Box>

      <Typography variant="body2" fontWeight={600} sx={{ mt: 2, mb: 1 }}>
        Minimum Rating
      </Typography>
      <Rating
        value={minRating}
        onChange={(_, v) => setMinRating(v || 0)}
        precision={1}
      />
    </Box>
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <Typography variant="h4" fontWeight={700}>
          {searchParam ? `Results for "${searchParam}"` : categoryParam || 'All Products'}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {data?.totalCount || 0} product{data?.totalCount !== 1 ? 's' : ''} found
        </Typography>
        <Box sx={{ flex: 1 }} />
        <TextField
          select
          size="small"
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          label="Sort by"
          sx={{ minWidth: 180 }}
        >
          {SORT_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>
              {opt.label}
            </MenuItem>
          ))}
        </TextField>
        {isMobile && (
          <IconButton onClick={() => setFilterOpen(true)}>
            <TuneIcon />
          </IconButton>
        )}
      </Box>

      <Box sx={{ display: 'flex', gap: 4 }}>
        {/* Desktop filters */}
        {!isMobile && filterContent}

        {/* Mobile drawer */}
        {isMobile && (
          <Drawer anchor="left" open={filterOpen} onClose={() => setFilterOpen(false)}>
            <Box sx={{ p: 3, width: 280 }}>
              {filterContent}
              <Button variant="contained" fullWidth sx={{ mt: 2 }} onClick={() => setFilterOpen(false)}>
                Apply Filters
              </Button>
            </Box>
          </Drawer>
        )}

        {/* Product grid */}
        <Box sx={{ flex: 1 }}>
          {isLoading ? (
            <Grid container spacing={3}>
              {Array.from({ length: 8 }).map((_, i) => (
                <Grid item xs={12} sm={6} md={4} key={i}>
                  <Skeleton variant="rectangular" height={220} sx={{ borderRadius: 2 }} />
                  <Skeleton variant="text" width="60%" sx={{ mt: 1 }} />
                  <Skeleton variant="text" width="40%" />
                </Grid>
              ))}
            </Grid>
          ) : isError ? (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <Typography variant="h6" color="error">
                Failed to load products. Please try again.
              </Typography>
            </Box>
          ) : products.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <Typography variant="h6" color="text.secondary">
                No products found.
              </Typography>
              <Button variant="outlined" sx={{ mt: 2 }} onClick={() => setSearchParams({})}>
                Clear Filters
              </Button>
            </Box>
          ) : (
            <>
              <Grid container spacing={3}>
                {products.map((product) => (
                  <Grid item xs={12} sm={6} md={4} key={product.id}>
                    <ProductCard
                      product={product}
                      isInWishlist={wishlistIds.has(product.id)}
                      onAddToCart={() =>
                        addItem({
                          productId: product.id,
                          name: product.name,
                          image: product.images?.[0] || '',
                          price: product.price,
                        })
                      }
                      onToggleWishlist={() => handleToggleWishlist(product.id)}
                    />
                  </Grid>
                ))}
              </Grid>
              {totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6 }}>
                  <Pagination
                    count={totalPages}
                    page={pageParam}
                    onChange={handlePageChange}
                    color="primary"
                    size="large"
                  />
                </Box>
              )}
            </>
          )}
        </Box>
      </Box>
    </Box>
  );
}
