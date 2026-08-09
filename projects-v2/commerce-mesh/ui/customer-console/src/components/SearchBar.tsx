import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TextField, Autocomplete, InputAdornment, CircularProgress, Box } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useDebouncedValue, useAutocomplete } from '../hooks/useSearch';

export default function SearchBar() {
  const [inputValue, setInputValue] = useState('');
  const debounced = useDebouncedValue(inputValue, 300);
  const { data: suggestions = [], isLoading } = useAutocomplete(debounced);
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputValue.trim()) {
      navigate(`/products?search=${encodeURIComponent(inputValue.trim())}`);
    }
  };

  const handleSelect = (_: unknown, value: string | null) => {
    if (value) {
      navigate(`/products?search=${encodeURIComponent(value)}`);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Autocomplete
        freeSolo
        size="small"
        options={suggestions}
        inputValue={inputValue}
        onInputChange={(_, v) => setInputValue(v)}
        onChange={handleSelect}
        loading={isLoading}
        renderInput={(params) => (
          <TextField
            {...params}
            placeholder="Search products..."
            variant="outlined"
            slotProps={{
              input: {
                ...params.InputProps,
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon fontSize="small" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <>
                    {isLoading && <CircularProgress color="inherit" size={18} />}
                    {params.InputProps.endAdornment}
                  </>
                ),
              },
            }}
          />
        )}
      />
    </Box>
  );
}
