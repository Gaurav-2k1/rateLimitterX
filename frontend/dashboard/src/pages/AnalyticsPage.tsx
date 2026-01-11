import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '@/lib/api'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Activity, Shield, TrendingUp, Clock, Zap, AlertTriangle } from 'lucide-react'
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, PieChart, Pie, Cell } from 'recharts'

export default function AnalyticsPage() {
  const { data: metrics, isLoading } = useQuery({
    queryKey: ['realtime-metrics'],
    queryFn: () => analyticsApi.getRealtime().then(res => res.data.data),
    refetchInterval: 30000,
  })

  const { data: topIdentifiers } = useQuery({
    queryKey: ['top-identifiers'],
    queryFn: () => analyticsApi.getTopIdentifiers(10).then(res => res.data.data),
    refetchInterval: 60000,
  })

  const { data: hourlyData, isLoading: hourlyLoading } = useQuery({
    queryKey: ['hourly-data'],
    queryFn: () => analyticsApi.getHourly(24).then(res => res.data.data),
    refetchInterval: 60000,
  })

  const { data: latencyData, isLoading: latencyLoading } = useQuery({
    queryKey: ['latency-trends'],
    queryFn: () => analyticsApi.getLatencyTrends(12).then(res => res.data.data),
    refetchInterval: 30000,
  })

  const COLORS = ['hsl(var(--primary))', 'hsl(var(--destructive))', 'hsl(var(--muted-foreground))']

  if (isLoading || hourlyLoading || latencyLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-3xl sm:text-4xl font-bold tracking-tight">Analytics</h1>
        <p className="text-muted-foreground mt-1">Comprehensive insights into your rate limiting performance</p>
      </div>
      
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card className="border-2 border-blue-200 dark:border-blue-800 bg-blue-50/50 dark:bg-blue-950/20 animate-slide-up">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total Checks (Last Hour)</CardTitle>
            <div className="p-2 rounded-lg bg-blue-100 dark:bg-blue-950/30">
              <Activity className="h-5 w-5 text-blue-600" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{metrics?.totalChecks || 0}</p>
            <p className="text-xs text-muted-foreground mt-1">+12.5% from last hour</p>
          </CardContent>
        </Card>
        
        <Card className="border-2 border-red-200 dark:border-red-800 bg-red-50/50 dark:bg-red-950/20 animate-slide-up" style={{ animationDelay: '0.1s' }}>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Rate Limit Hits</CardTitle>
            <div className="p-2 rounded-lg bg-red-100 dark:bg-red-950/30">
              <Shield className="h-5 w-5 text-red-600" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{metrics?.rateLimitHits || 0}</p>
            <p className="text-xs text-muted-foreground mt-1">+5.2% from last hour</p>
          </CardContent>
        </Card>
        
        <Card className="border-2 border-amber-200 dark:border-amber-800 bg-amber-50/50 dark:bg-amber-950/20 animate-slide-up" style={{ animationDelay: '0.2s' }}>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Hit Rate</CardTitle>
            <div className="p-2 rounded-lg bg-amber-100 dark:bg-amber-950/30">
              <TrendingUp className="h-5 w-5 text-amber-600" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{(metrics?.hitRate || 0).toFixed(2)}%</p>
            <p className="text-xs text-muted-foreground mt-1">-2.1% from last hour</p>
          </CardContent>
        </Card>
        
        <Card className="border-2 border-green-200 dark:border-green-800 bg-green-50/50 dark:bg-green-950/20 animate-slide-up" style={{ animationDelay: '0.3s' }}>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Remaining Checks</CardTitle>
            <div className="p-2 rounded-lg bg-green-100 dark:bg-green-950/30">
              <Zap className="h-5 w-5 text-green-600" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{metrics?.remainingChecksThisMonth ?? '∞'}</p>
            <p className="text-xs text-muted-foreground mt-1">This month</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <Card className="animate-slide-up">
          <CardHeader>
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <Clock className="h-4 w-4 text-primary" />
              Latency P50
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{metrics?.latencyP50 || 0}ms</p>
            <p className="text-xs text-muted-foreground mt-1">Median response time</p>
          </CardContent>
        </Card>
        
        <Card className="animate-slide-up" style={{ animationDelay: '0.1s' }}>
          <CardHeader>
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-amber-600" />
              Latency P95
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{metrics?.latencyP95 || 0}ms</p>
            <p className="text-xs text-muted-foreground mt-1">95th percentile</p>
          </CardContent>
        </Card>
        
        <Card className="animate-slide-up" style={{ animationDelay: '0.2s' }}>
          <CardHeader>
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-red-600" />
              Latency P99
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{metrics?.latencyP99 || 0}ms</p>
            <p className="text-xs text-muted-foreground mt-1">99th percentile</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card className="animate-slide-up">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5 text-primary" />
              Request Volume (Last 24h)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={hourlyData || []}>
                <defs>
                  <linearGradient id="colorChecks" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorAllowed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="hsl(142, 76%, 36%)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="hsl(142, 76%, 36%)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis 
                  dataKey="hour" 
                  tick={{ fontSize: 12 }}
                  className="text-muted-foreground"
                />
                <YAxis 
                  tick={{ fontSize: 12 }}
                  className="text-muted-foreground"
                />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'hsl(var(--card))',
                    border: '1px solid hsl(var(--border))',
                    borderRadius: '8px'
                  }}
                />
                <Area 
                  type="monotone" 
                  dataKey="allowed" 
                  stroke="hsl(142, 76%, 36%)" 
                  fillOpacity={1}
                  fill="url(#colorAllowed)" 
                  name="Allowed"
                />
                <Area 
                  type="monotone" 
                  dataKey="checks" 
                  stroke="hsl(var(--primary))" 
                  fillOpacity={1}
                  fill="url(#colorChecks)" 
                  name="Total Checks"
                />
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="animate-slide-up">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5 text-red-600" />
              Rate Limit Hits (Last 24h)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={hourlyData || []}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis 
                  dataKey="hour" 
                  tick={{ fontSize: 12 }}
                  className="text-muted-foreground"
                />
                <YAxis 
                  tick={{ fontSize: 12 }}
                  className="text-muted-foreground"
                />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'hsl(var(--card))',
                    border: '1px solid hsl(var(--border))',
                    borderRadius: '8px'
                  }}
                />
                <Bar dataKey="hits" fill="hsl(var(--destructive))" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card className="animate-slide-up">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-primary" />
              Latency Trends (Last Hour)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={250}>
              <LineChart data={latencyData || []}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis 
                  dataKey="time" 
                  tick={{ fontSize: 12 }}
                  className="text-muted-foreground"
                />
                <YAxis 
                  tick={{ fontSize: 12 }}
                  className="text-muted-foreground"
                />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'hsl(var(--card))',
                    border: '1px solid hsl(var(--border))',
                    borderRadius: '8px'
                  }}
                />
                <Line type="monotone" dataKey="p50" stroke="hsl(var(--primary))" strokeWidth={2} dot={false} name="P50" />
                <Line type="monotone" dataKey="p95" stroke="hsl(45, 93%, 47%)" strokeWidth={2} dot={false} name="P95" />
                <Line type="monotone" dataKey="p99" stroke="hsl(var(--destructive))" strokeWidth={2} dot={false} name="P99" />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="animate-slide-up">
          <CardHeader>
            <CardTitle>Top Rate-Limited Identifiers (Last 24h)</CardTitle>
          </CardHeader>
          <CardContent>
            {topIdentifiers && topIdentifiers.length > 0 ? (
              <div className="overflow-x-auto">
                <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Rank</TableHead>
                    <TableHead>Identifier</TableHead>
                    <TableHead className="text-right">Denied Count</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {topIdentifiers.map((item: any, idx: number) => (
                    <TableRow key={idx} className="hover:bg-muted/50">
                      <TableCell className="font-medium">
                        <div className="flex items-center gap-2">
                          <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                            idx === 0 ? 'bg-red-100 text-red-700 dark:bg-red-950/30 dark:text-red-400' :
                            idx === 1 ? 'bg-orange-100 text-orange-700 dark:bg-orange-950/30 dark:text-orange-400' :
                            idx === 2 ? 'bg-amber-100 text-amber-700 dark:bg-amber-950/30 dark:text-amber-400' :
                            'bg-muted text-muted-foreground'
                          }`}>
                            {idx + 1}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-sm">{item.identifier}</TableCell>
                      <TableCell className="text-right">
                        <span className="font-semibold">{item.deniedCount}</span>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <Shield className="h-12 w-12 text-muted-foreground mb-4 opacity-50" />
                <p className="text-muted-foreground">No rate limit hits in the last 24 hours</p>
                <p className="text-sm text-muted-foreground mt-1">Your rate limits are working perfectly!</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

