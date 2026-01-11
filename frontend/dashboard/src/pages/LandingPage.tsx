import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Zap,
  Shield,
  BarChart3,
  Key,
  Code,
  Rocket,
  CheckCircle2,
  ArrowRight,
  Users,
  Gauge,
  Bell,
  Layers,
  Globe,
  Database,
  Lock,
  TrendingUp,
  Clock,
  Settings,
  FileCode,
  Webhook,
  Sparkles,
} from 'lucide-react'

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-background via-background to-muted/20">
      {/* Navigation */}
      <nav className="border-b bg-background/95 backdrop-blur-sm supports-[backdrop-filter]:bg-background/60 sticky top-0 z-50 shadow-sm">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 group">
            <div className="p-1.5 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
              <Zap className="h-5 w-5 text-primary" />
            </div>
            <span className="text-lg sm:text-xl font-bold">RateLimitX</span>
          </Link>
          <div className="flex items-center gap-2 sm:gap-4">
            <Link to="/docs">
              <Button variant="ghost" size="sm" className="hidden sm:flex">
                Documentation
              </Button>
              <Button variant="ghost" size="sm" className="sm:hidden">
                Docs
              </Button>
            </Link>
            <Link to="/login">
              <Button variant="ghost" size="sm">Sign In</Button>
            </Link>
            <Link to="/register">
              <Button size="sm" className="hidden sm:flex">Get Started</Button>
              <Button size="sm" className="sm:hidden">Start</Button>
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-20 md:py-32 text-center">
        <div className="max-w-4xl mx-auto space-y-8 animate-fade-in">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 mb-4">
            <Sparkles className="h-4 w-4 text-primary" />
            <span className="text-sm font-medium text-primary">Enterprise-Grade Rate Limiting</span>
          </div>
          <h1 className="text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight">
            Production-Grade API
            <br />
            <span className="text-primary bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
              Rate Limiting
            </span>
            <br />
            Made Simple
          </h1>
          <p className="text-xl md:text-2xl text-muted-foreground max-w-3xl mx-auto leading-relaxed">
            Protect your APIs from abuse with enterprise-grade rate limiting. 
            Multi-tenant SaaS platform with real-time analytics, flexible algorithms, 
            and seamless integration.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center pt-6">
            <Link to="/register">
              <Button size="lg" className="w-full sm:w-auto text-base h-12 px-8 shadow-lg hover:shadow-xl transition-shadow">
                Start Free Trial
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
            <Link to="/login">
              <Button size="lg" variant="outline" className="w-full sm:w-auto text-base h-12 px-8 border-2">
                View Dashboard
              </Button>
            </Link>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-6 pt-6 text-sm text-muted-foreground">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <span>No credit card required</span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <span>Free tier available</span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600" />
              <span>Deploy in minutes</span>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="container mx-auto px-4 py-20">
        <div className="text-center mb-16 animate-fade-in">
          <h2 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">Why Choose RateLimitX?</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Everything you need to implement robust rate limiting for your APIs
          </p>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20">
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Layers className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Multiple Algorithms</CardTitle>
              <CardDescription className="text-base">
                Choose from Token Bucket, Sliding Window, or Fixed Window algorithms 
                based on your use case
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.1s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Users className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Multi-Tenant Architecture</CardTitle>
              <CardDescription className="text-base">
                Isolated rate limits per tenant with secure API key management 
                and role-based access control
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.2s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Gauge className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Real-Time Analytics</CardTitle>
              <CardDescription className="text-base">
                Monitor rate limit hits, latency percentiles, top identifiers, 
                and usage metrics in real-time
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.3s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Shield className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Hierarchical Limits</CardTitle>
              <CardDescription className="text-base">
                Set global, resource-level, and identifier-level limits with 
                priority-based rule evaluation
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.4s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Bell className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Smart Alerting</CardTitle>
              <CardDescription className="text-base">
                Get notified via email, webhooks, Slack, or Discord when limits 
                are approaching or exceeded
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.5s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Code className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">RESTful API</CardTitle>
              <CardDescription className="text-base">
                Simple HTTP integration with comprehensive documentation. 
                Works with any programming language
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.6s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Key className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">API Key Management</CardTitle>
              <CardDescription className="text-base">
                Generate, rotate, and revoke API keys with environment support 
                (dev/staging/prod)
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.7s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <FileCode className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Bulk Operations</CardTitle>
              <CardDescription className="text-base">
                Import and export rate limit rules in JSON or YAML format 
                for easy configuration management
              </CardDescription>
            </CardHeader>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.8s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Globe className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">100% Free Deployment</CardTitle>
              <CardDescription className="text-base">
                Deploy completely free using Render, Vercel, Neon, and Upstash 
                free tiers
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="container mx-auto px-4 py-20 bg-gradient-to-b from-muted/30 to-muted/50">
        <div className="text-center mb-16 animate-fade-in">
          <h2 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">How It Works</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Simple integration process that takes just minutes
          </p>
        </div>
        <div className="max-w-5xl mx-auto">
          <div className="grid md:grid-cols-3 gap-8">
            <div className="text-center space-y-6 p-8 rounded-xl bg-card border-2 hover:border-primary/20 transition-all duration-300 animate-slide-up group">
              <div className="h-20 w-20 rounded-full bg-gradient-to-br from-primary to-primary/70 text-primary-foreground flex items-center justify-center text-3xl font-bold mx-auto shadow-lg group-hover:scale-110 transition-transform">
                1
              </div>
              <h3 className="text-2xl font-bold">Register & Get API Key</h3>
              <p className="text-muted-foreground text-base leading-relaxed">
                Sign up for free and receive your unique API key. 
                No credit card required.
              </p>
            </div>
            <div className="text-center space-y-6 p-8 rounded-xl bg-card border-2 hover:border-primary/20 transition-all duration-300 animate-slide-up group" style={{ animationDelay: '0.1s' }}>
              <div className="h-20 w-20 rounded-full bg-gradient-to-br from-primary to-primary/70 text-primary-foreground flex items-center justify-center text-3xl font-bold mx-auto shadow-lg group-hover:scale-110 transition-transform">
                2
              </div>
              <h3 className="text-2xl font-bold">Create Rate Limit Rules</h3>
              <p className="text-muted-foreground text-base leading-relaxed">
                Configure rate limits for your resources using our 
                intuitive dashboard or API.
              </p>
            </div>
            <div className="text-center space-y-6 p-8 rounded-xl bg-card border-2 hover:border-primary/20 transition-all duration-300 animate-slide-up group" style={{ animationDelay: '0.2s' }}>
              <div className="h-20 w-20 rounded-full bg-gradient-to-br from-primary to-primary/70 text-primary-foreground flex items-center justify-center text-3xl font-bold mx-auto shadow-lg group-hover:scale-110 transition-transform">
                3
              </div>
              <h3 className="text-2xl font-bold">Integrate & Protect</h3>
              <p className="text-muted-foreground text-base leading-relaxed">
                Make a simple HTTP call before processing requests. 
                Your APIs are now protected!
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Developer Documentation Section */}
      <section className="container mx-auto px-4 py-20">
        <div className="text-center mb-16 animate-fade-in">
          <h2 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">Developer Documentation</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Complete integration guides, API reference, and code examples for developers
          </p>
        </div>
        <div className="max-w-5xl mx-auto">
          <Card className="bg-gradient-to-br from-primary/10 via-primary/5 to-primary/10 border-2 border-primary/20 shadow-xl animate-slide-up">
            <CardContent className="p-8 md:p-12">
              <div className="flex flex-col md:flex-row items-start md:items-center gap-8">
                <div className="flex-1 space-y-6">
                  <div className="flex items-center gap-3">
                    <div className="p-3 rounded-xl bg-primary/20">
                      <Code className="h-6 w-6 text-primary" />
                    </div>
                    <h3 className="text-3xl font-bold">Ready to Integrate?</h3>
                  </div>
                  <p className="text-muted-foreground text-lg">
                    Get comprehensive documentation including:
                  </p>
                  <ul className="space-y-3 text-base">
                    <li className="flex items-center gap-3">
                      <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0" />
                      <span>REST API integration guide</span>
                    </li>
                    <li className="flex items-center gap-3">
                      <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0" />
                      <span>SDK documentation (Node.js, Python, Java, Go)</span>
                    </li>
                    <li className="flex items-center gap-3">
                      <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0" />
                      <span>Complete code examples</span>
                    </li>
                    <li className="flex items-center gap-3">
                      <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0" />
                      <span>Best practices and patterns</span>
                    </li>
                    <li className="flex items-center gap-3">
                      <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0" />
                      <span>API reference</span>
                    </li>
                  </ul>
                </div>
                <div className="flex flex-col gap-4 w-full md:w-auto">
                  <Link to="/docs">
                    <Button size="lg" className="w-full md:w-auto text-base h-12 px-8 shadow-lg">
                      <Code className="h-4 w-4 mr-2" />
                      View Developer Docs
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </Button>
                  </Link>
                  <Link to="/register">
                    <Button size="lg" variant="outline" className="w-full md:w-auto text-base h-12 px-8 border-2">
                      Get Started Free
                    </Button>
                  </Link>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Pricing Tiers Section */}
      <section className="container mx-auto px-4 py-20 bg-gradient-to-b from-muted/30 to-muted/50">
        <div className="text-center mb-16 animate-fade-in">
          <h2 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">Pricing Tiers</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Choose the plan that fits your needs
          </p>
        </div>
        <div className="grid md:grid-cols-3 gap-8 max-w-6xl mx-auto">
          <Card className="border-2 hover:shadow-xl transition-all duration-300 animate-slide-up">
            <CardHeader className="pb-4">
              <CardTitle className="text-2xl mb-2">FREE</CardTitle>
              <CardDescription className="text-base">Perfect for getting started</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="text-5xl font-bold">$0<span className="text-xl text-muted-foreground">/month</span></div>
              <ul className="space-y-3 text-base">
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>1 rate limit rule</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>10,000 checks/month</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>All algorithms</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Basic analytics</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Email alerts</span>
                </li>
              </ul>
              <Link to="/register">
                <Button className="w-full h-12 text-base" variant="outline">Get Started</Button>
              </Link>
            </CardContent>
          </Card>

          <Card className="border-2 border-primary shadow-xl hover:shadow-2xl transition-all duration-300 animate-slide-up relative overflow-hidden" style={{ animationDelay: '0.1s' }}>
            <div className="absolute top-0 right-0 bg-primary text-primary-foreground px-4 py-1 text-sm font-bold">
              POPULAR
            </div>
            <CardHeader className="pb-4 pt-8">
              <CardTitle className="text-2xl mb-2">PRO</CardTitle>
              <CardDescription className="text-base">For growing applications</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="text-5xl font-bold">$29<span className="text-xl text-muted-foreground">/month</span></div>
              <ul className="space-y-3 text-base">
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Unlimited rules</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>1,000,000 checks/month</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>All algorithms</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Advanced analytics</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Webhook & Slack alerts</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Priority support</span>
                </li>
              </ul>
              <Link to="/register">
                <Button className="w-full h-12 text-base shadow-lg">Upgrade to Pro</Button>
              </Link>
            </CardContent>
          </Card>

          <Card className="border-2 hover:shadow-xl transition-all duration-300 animate-slide-up" style={{ animationDelay: '0.2s' }}>
            <CardHeader className="pb-4">
              <CardTitle className="text-2xl mb-2">ENTERPRISE</CardTitle>
              <CardDescription className="text-base">For large-scale deployments</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="text-5xl font-bold">Custom</div>
              <ul className="space-y-3 text-base">
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Unlimited everything</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Custom SLA</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Dedicated support</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>Custom integrations</span>
                </li>
                <li className="flex items-center gap-3">
                  <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0" />
                  <span>On-premise option</span>
                </li>
              </ul>
              <Link to="/register">
                <Button className="w-full h-12 text-base" variant="outline">Contact Sales</Button>
              </Link>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Technical Details Section */}
      <section className="container mx-auto px-4 py-20">
        <div className="text-center mb-16 animate-fade-in">
          <h2 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">Technical Architecture</h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Built with modern, scalable technologies
          </p>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 max-w-6xl mx-auto">
          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20">
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Database className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Backend</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-base text-muted-foreground leading-relaxed">
                Spring Boot 3.2, Java 21, PostgreSQL, Redis (Upstash)
              </p>
            </CardContent>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.1s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Code className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Frontend</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-base text-muted-foreground leading-relaxed">
                React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui
              </p>
            </CardContent>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.2s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Lock className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Security</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-base text-muted-foreground leading-relaxed">
                JWT authentication, SHA-256 API keys, BCrypt passwords, HTTPS
              </p>
            </CardContent>
          </Card>

          <Card className="group hover:shadow-lg transition-all duration-300 animate-slide-up border-2 hover:border-primary/20" style={{ animationDelay: '0.3s' }}>
            <CardHeader>
              <div className="h-14 w-14 rounded-xl bg-primary/10 group-hover:bg-primary/20 flex items-center justify-center mb-4 transition-colors">
                <Rocket className="h-7 w-7 text-primary" />
              </div>
              <CardTitle className="text-xl mb-2">Deployment</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-base text-muted-foreground leading-relaxed">
                Render, Vercel, Neon, Upstash - 100% free tier compatible
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* CTA Section */}
      <section className="container mx-auto px-4 py-24">
        <div className="max-w-3xl mx-auto text-center bg-gradient-to-br from-primary/10 via-primary/5 to-primary/10 border-2 border-primary/20 rounded-2xl p-12 shadow-xl animate-fade-in">
          <h2 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">Ready to Get Started?</h2>
          <p className="text-xl text-muted-foreground mb-8">
            Join developers protecting their APIs with RateLimitX
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <Link to="/register">
              <Button size="lg" className="w-full sm:w-auto text-base h-12 px-8 shadow-lg hover:shadow-xl transition-shadow">
                Create Free Account
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
            <Link to="/login">
              <Button size="lg" variant="outline" className="w-full sm:w-auto text-base h-12 px-8 border-2">
                Sign In
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t bg-muted/50">
        <div className="container mx-auto px-4 py-12">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-lg bg-primary/10">
                <Zap className="h-5 w-5 text-primary" />
              </div>
              <span className="font-bold text-lg">RateLimitX</span>
            </div>
            <div className="flex gap-8 text-sm">
              <Link to="/docs" className="text-muted-foreground hover:text-foreground transition-colors font-medium">
                Documentation
              </Link>
              <Link to="/login" className="text-muted-foreground hover:text-foreground transition-colors font-medium">
                Sign In
              </Link>
              <Link to="/register" className="text-muted-foreground hover:text-foreground transition-colors font-medium">
                Register
              </Link>
            </div>
          </div>
          <div className="mt-8 pt-6 border-t text-center">
            <p className="text-sm text-muted-foreground">
              © 2024 RateLimitX. Built with <span className="text-red-500">❤️</span> using Spring Boot and React.
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}

