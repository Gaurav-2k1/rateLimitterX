import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { alertsApi } from '@/lib/api'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { Plus, Trash2, Bell } from 'lucide-react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function AlertsPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const queryClient = useQueryClient()

  const { data: alerts, isLoading } = useQuery({
    queryKey: ['alerts'],
    queryFn: () => alertsApi.getAll().then(res => res.data.data),
  })

  const deleteMutation = useMutation({
    mutationFn: alertsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] })
    },
  })

  const createMutation = useMutation({
    mutationFn: alertsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] })
      setIsCreateOpen(false)
    },
  })

  const handleCreate = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    createMutation.mutate({
      alertType: formData.get('alertType') as string,
      destination: formData.get('destination') as string,
      destinationType: formData.get('destinationType') as string,
      thresholdPercent: formData.get('thresholdPercent') ? 
        parseInt(formData.get('thresholdPercent') as string) : null,
    })
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
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight">Alert Configuration</h1>
          <p className="text-muted-foreground mt-1">Configure notifications for rate limit events</p>
        </div>
        <Button onClick={() => setIsCreateOpen(true)} className="w-full sm:w-auto">
          <Plus className="mr-2 h-4 w-4" />
          Create Alert
        </Button>
      </div>

      <Card className="animate-slide-up">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bell className="h-5 w-5 text-primary" />
            Active Alerts
          </CardTitle>
        </CardHeader>
        <CardContent>
          {alerts && alerts.length > 0 ? (
            <div className="overflow-x-auto">
              <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="font-semibold">Type</TableHead>
                  <TableHead className="font-semibold">Destination</TableHead>
                  <TableHead className="font-semibold">Destination Type</TableHead>
                  <TableHead className="font-semibold">Threshold</TableHead>
                  <TableHead className="text-right font-semibold">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {alerts.map((alert: any) => (
                  <TableRow key={alert.id} className="hover:bg-muted/50 transition-colors">
                    <TableCell>
                      <span className="px-2.5 py-1 rounded-md bg-muted text-xs font-medium">
                        {alert.alertType.replace(/_/g, ' ')}
                      </span>
                    </TableCell>
                    <TableCell className="font-mono text-sm">{alert.destination}</TableCell>
                    <TableCell>
                      <span className="px-2.5 py-1 rounded-md bg-primary/10 text-primary text-xs font-medium capitalize">
                        {alert.destinationType}
                      </span>
                    </TableCell>
                    <TableCell>
                      {alert.thresholdPercent ? (
                        <span className="font-semibold">{alert.thresholdPercent}%</span>
                      ) : (
                        <span className="text-muted-foreground">N/A</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button 
                        variant="ghost" 
                        size="sm"
                        onClick={() => deleteMutation.mutate(alert.id)}
                        className="hover:bg-destructive/10 hover:text-destructive"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Bell className="h-12 w-12 text-muted-foreground mb-4 opacity-50" />
              <p className="text-muted-foreground">No alerts configured</p>
              <p className="text-sm text-muted-foreground mt-1">Create an alert to get notified about rate limit events</p>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="max-w-[95vw] sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Create New Alert</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreate} className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block">Alert Type</label>
              <select name="alertType" className="w-full h-10 rounded-md border border-input bg-background px-3" required>
                <option value="TIER_LIMIT_APPROACHING">Tier Limit Approaching</option>
                <option value="TIER_LIMIT_EXCEEDED">Tier Limit Exceeded</option>
                <option value="RATE_LIMIT_SPIKE">Rate Limit Spike</option>
                <option value="API_ERROR_RATE">API Error Rate</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Destination Type</label>
              <select name="destinationType" className="w-full h-10 rounded-md border border-input bg-background px-3" required>
                <option value="EMAIL">Email</option>
                <option value="WEBHOOK">Webhook</option>
                <option value="SLACK">Slack</option>
                <option value="DISCORD">Discord</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Destination</label>
              <Input name="destination" required placeholder="email@example.com or webhook URL" />
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Threshold % (for approaching alerts)</label>
              <Input name="thresholdPercent" type="number" min="0" max="100" placeholder="80" />
            </div>
            <Button type="submit" className="w-full">Create Alert</Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

