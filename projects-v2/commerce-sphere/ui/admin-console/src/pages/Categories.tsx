import { useState } from 'react'
import { Box, Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, IconButton } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'; import DeleteIcon from '@mui/icons-material/Delete'
import { useCategories, useCreateCategory, useDeleteCategory } from '../hooks/useCategories'
import Loading from '../components/shared/Loading'

export default function Categories() {
  const { data, isLoading } = useCategories()
  const createCategory = useCreateCategory()
  const deleteCategory = useDeleteCategory()
  const [open, setOpen] = useState(false)
  const [name, setName] = useState(''); const [slug, setSlug] = useState(''); const [desc, setDesc] = useState('')

  return <>
    <Box display="flex" justifyContent="space-between" mb={3}><Typography variant="h4">Categories</Typography><Button startIcon={<AddIcon />} variant="contained" onClick={() => setOpen(true)}>Add</Button></Box>
    {isLoading ? <Loading /> : (
      <TableContainer component={Paper}><Table><TableHead><TableRow><TableCell>Name</TableCell><TableCell>Slug</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
        <TableBody>{data?.map((c: any) => (
          <TableRow key={c.id}><TableCell>{c.name}</TableCell><TableCell>{c.slug}</TableCell><TableCell align="right"><IconButton color="error" onClick={() => { if (confirm('Delete?')) deleteCategory.mutate(c.id) }}><DeleteIcon /></IconButton></TableCell></TableRow>
        ))}</TableBody></Table></TableContainer>
    )}
    <Dialog open={open} onClose={() => setOpen(false)}><DialogTitle>New Category</DialogTitle>
      <DialogContent><TextField fullWidth label="Name" margin="normal" value={name} onChange={e => setName(e.target.value)} />
        <TextField fullWidth label="Slug" margin="normal" value={slug} onChange={e => setSlug(e.target.value)} />
        <TextField fullWidth label="Description" margin="normal" value={desc} onChange={e => setDesc(e.target.value)} /></DialogContent>
      <DialogActions><Button onClick={() => setOpen(false)}>Cancel</Button><Button variant="contained" onClick={() => { createCategory.mutate({ name, slug, description: desc }); setOpen(false); setName(''); setSlug(''); setDesc('') }}>Create</Button></DialogActions>
    </Dialog>
  </>
}
