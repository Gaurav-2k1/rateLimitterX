import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiKeysApi } from '@/lib/api'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Plus, Trash2, Copy, CheckCircle2, Key } from 'lucide-react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'

export default function ApiKeysPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [newKey, setNewKey] = useState('')
  const queryClient = useQueryClient()

  const { data: keys, isLoading } = useQuery({
    queryKey: ['api-keys'],
    queryFn: () => apiKeysApi.getAll().then(res => res.data.data),
  })

  const createMutation = useMutation({
    mutationFn: apiKeysApi.create,
    onSuccess: (data) => {
      setNewKey(data.data.data.apiKey)
      queryClient.invalidateQueries({ queryKey: ['api-keys'] })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: apiKeysApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] })
    },
  })

  const handleCreate = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    createMutation.mutate({
      name: formData.get('name') as string,
      environment: formData.get('environment') as string || 'production',
    })
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight">API Keys</h1>
          <p className="text-muted-foreground mt-1">Manage your API keys for different environments</p>
        </div>
        <Button onClick={() => setIsCreateOpen(true)} className="w-full sm:w-auto">
          <Plus className="mr-2 h-4 w-4" />
          Create Key
        </Button>
      </div>

      {newKey && (
        <div className="p-4 bg-green-50 dark:bg-green-950/20 border-2 border-green-200 dark:border-green-800 rounded-lg animate-scale-in">
          <div className="flex items-center gap-2 mb-2">
            <CheckCircle2 className="h-4 w-4 text-green-600" />
            <p className="text-sm font-medium text-green-900 dark:text-green-100">New API Key (save this!):</p>
          </div>
          <div className="flex items-center gap-2">
            <code className="text-xs bg-white dark:bg-gray-900 p-2 rounded flex-1 break-all font-mono">{newKey}</code>
            <Button size="sm" variant="outline" onClick={() => copyToClipboard(newKey)} className="flex-shrink-0">
              <Copy className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

      <Card className="animate-slide-up">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="font-semibold">Name</TableHead>
                <TableHead className="font-semibold">Environment</TableHead>
                <TableHead className="font-semibold">Key Hash</TableHead>
                <TableHead className="font-semibold">Status</TableHead>
                <TableHead className="text-right font-semibold">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {keys && keys.length > 0 ? (
                keys.map((key: any) => (
                  <TableRow key={key.id} className="hover:bg-muted/50 transition-colors">
                    <TableCell className="font-medium">{key.name}</TableCell>
                    <TableCell>
                      <span className="px-2.5 py-1 rounded-md bg-muted text-xs font-medium capitalize">
                        {key.environment}
                      </span>
                    </TableCell>
                    <TableCell>
                      <code className="text-xs font-mono bg-muted px-2 py-1 rounded">{key.keyHash}</code>
                    </TableCell>
                    <TableCell>
                      <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${
                        key.active 
                          ? 'bg-green-100 text-green-700 dark:bg-green-950/30 dark:text-green-400' 
                          : 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400'
                      }`}>
                        {key.active ? 'Active' : 'Inactive'}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button 
                        variant="ghost" 
                        size="sm"
                        onClick={() => deleteMutation.mutate(key.id)}
                        className="hover:bg-destructive/10 hover:text-destructive"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-12">
                    <div className="flex flex-col items-center gap-3">
                      <Key className="h-12 w-12 text-muted-foreground opacity-50" />
                      <p className="text-muted-foreground">No API keys yet</p>
                      <p className="text-sm text-muted-foreground">Create your first API key to get started</p>
                    </div>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
          </div>
        </CardContent>
      </Card>

      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="max-w-[95vw] sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Create New API Key</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreate} className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block">Name</label>
              <Input name="name" required placeholder="Production Key" />
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Environment</label>
              <select name="environment" className="w-full h-10 rounded-md border border-input bg-background px-3">
                <option value="production">Production</option>
                <option value="staging">Staging</option>
                <option value="development">Development</option>
              </select>
            </div>
            <Button type="submit" className="w-full">Create Key</Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

