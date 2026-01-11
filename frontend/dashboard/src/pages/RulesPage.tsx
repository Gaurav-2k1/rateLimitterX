import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { rulesApi } from '@/lib/api'
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
import { Plus, Trash2, Download, Upload, Shield } from 'lucide-react'
import { bulkApi } from '@/lib/api'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'

export default function RulesPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isImportOpen, setIsImportOpen] = useState(false)
  const queryClient = useQueryClient()

  const { data: rules, isLoading } = useQuery({
    queryKey: ['rules'],
    queryFn: () => rulesApi.getAll().then(res => res.data.data),
  })

  const deleteMutation = useMutation({
    mutationFn: rulesApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rules'] })
    },
  })

  const createMutation = useMutation({
    mutationFn: rulesApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rules'] })
      setIsCreateOpen(false)
    },
  })

  const handleCreate = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    createMutation.mutate({
      resource: formData.get('resource') as string,
      algorithm: formData.get('algorithm') as string,
      maxRequests: parseInt(formData.get('maxRequests') as string),
      windowSeconds: parseInt(formData.get('windowSeconds') as string),
      identifierType: formData.get('identifierType') as string || 'USER_ID',
    })
  }

  const handleExport = async () => {
    try {
      const response = await bulkApi.export('json')
      const blob = new Blob([response.data], { type: 'application/json' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'rules.json'
      a.click()
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error('Export failed', error)
    }
  }

  const handleImport = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const file = formData.get('file') as File
    const format = formData.get('format') as string || 'json'
    
    if (file) {
      try {
        await bulkApi.import(file, format)
        queryClient.invalidateQueries({ queryKey: ['rules'] })
        setIsImportOpen(false)
      } catch (error) {
        console.error('Import failed', error)
      }
    }
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
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight">Rate Limit Rules</h1>
          <p className="text-muted-foreground mt-1">Manage and configure your rate limiting rules</p>
        </div>
        <div className="flex flex-wrap gap-2 w-full sm:w-auto">
          <Button variant="outline" onClick={handleExport} className="flex-1 sm:flex-initial">
            <Download className="mr-2 h-4 w-4" />
            <span className="hidden sm:inline">Export</span>
          </Button>
          <Button variant="outline" onClick={() => setIsImportOpen(true)} className="flex-1 sm:flex-initial">
            <Upload className="mr-2 h-4 w-4" />
            <span className="hidden sm:inline">Import</span>
          </Button>
          <Button onClick={() => setIsCreateOpen(true)} className="flex-1 sm:flex-initial">
            <Plus className="mr-2 h-4 w-4" />
            Create Rule
          </Button>
        </div>
      </div>

      <Card className="animate-slide-up">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="font-semibold">Resource</TableHead>
                <TableHead className="font-semibold">Algorithm</TableHead>
                <TableHead className="font-semibold">Limit</TableHead>
                <TableHead className="font-semibold">Window</TableHead>
                <TableHead className="font-semibold">Status</TableHead>
                <TableHead className="text-right font-semibold">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rules && rules.length > 0 ? (
                rules.map((rule: any) => (
                  <TableRow key={rule.id} className="hover:bg-muted/50 transition-colors">
                    <TableCell className="font-medium font-mono text-sm">{rule.resource}</TableCell>
                    <TableCell>
                      <span className="px-2 py-1 rounded-md bg-muted text-xs font-medium">
                        {rule.algorithm.replace('_', ' ')}
                      </span>
                    </TableCell>
                    <TableCell className="font-semibold">{rule.maxRequests}</TableCell>
                    <TableCell>{rule.windowSeconds}s</TableCell>
                    <TableCell>
                      <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${
                        rule.active 
                          ? 'bg-green-100 text-green-700 dark:bg-green-950/30 dark:text-green-400' 
                          : 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400'
                      }`}>
                        {rule.active ? 'Active' : 'Inactive'}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button 
                        variant="ghost" 
                        size="sm"
                        onClick={() => deleteMutation.mutate(rule.id)}
                        className="hover:bg-destructive/10 hover:text-destructive"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-12">
                    <div className="flex flex-col items-center gap-3">
                      <Shield className="h-12 w-12 text-muted-foreground opacity-50" />
                      <p className="text-muted-foreground">No rate limit rules yet</p>
                      <p className="text-sm text-muted-foreground">Create your first rule to get started</p>
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
            <DialogTitle>Create New Rule</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreate} className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block">Resource</label>
              <Input name="resource" required placeholder="api.payment.create" />
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Algorithm</label>
              <select name="algorithm" className="w-full h-10 rounded-md border border-input bg-background px-3" required>
                <option value="TOKEN_BUCKET">Token Bucket</option>
                <option value="SLIDING_WINDOW">Sliding Window</option>
                <option value="FIXED_WINDOW">Fixed Window</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Max Requests</label>
              <Input name="maxRequests" type="number" required min="1" />
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Window (seconds)</label>
              <Input name="windowSeconds" type="number" required min="1" />
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Identifier Type</label>
              <select name="identifierType" className="w-full h-10 rounded-md border border-input bg-background px-3">
                <option value="USER_ID">User ID</option>
                <option value="IP_ADDRESS">IP Address</option>
                <option value="API_KEY">API Key</option>
                <option value="CUSTOM">Custom</option>
              </select>
            </div>
            <Button type="submit" className="w-full">Create Rule</Button>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={isImportOpen} onOpenChange={setIsImportOpen}>
        <DialogContent className="max-w-[95vw] sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Import Rules</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleImport} className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block">Format</label>
              <select name="format" className="w-full h-10 rounded-md border border-input bg-background px-3">
                <option value="json">JSON</option>
                <option value="yaml">YAML</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">File</label>
              <Input name="file" type="file" required accept=".json,.yaml,.yml" />
            </div>
            <Button type="submit" className="w-full">Import Rules</Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

