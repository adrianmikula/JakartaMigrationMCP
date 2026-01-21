# Website Architecture Plan

## Overview

This document outlines the architecture for a small, professional website for the Jakarta Migration MCP project. The website will be 100% code-managed, deployable via GitHub workflows, and designed to build trust with enterprise customers based on our market research.

## Technology Stack Decision

> **📋 See detailed comparison**: [`static-site-framework-comparison.md`](./static-site-framework-comparison.md)

### Revised Recommendation: Static Site Generator

After evaluating modern frameworks for a 5-6 page, 95% static marketing site, **static site generators** are better suited than Laravel for this use case.

**Top Recommendation: Astro** ⭐

**Why Astro:**
- ✅ **Zero JavaScript by default** - Perfect for static marketing sites
- ✅ **Islands Architecture** - Only interactive components load JS (forms, search)
- ✅ **Excellent performance** - Perfect Core Web Vitals scores
- ✅ **Modern & cutting-edge** - Active development, growing community
- ✅ **Easy deployment** - Works seamlessly with Railway, GitHub Actions
- ✅ **TypeScript support** - Modern developer experience
- ✅ **Content Collections** - Built-in markdown/content management
- ✅ **Flexible** - Can use React/Vue components where needed

**Alternative Options:**
- **Jigsaw** (PHP/Blade) - If you prefer PHP ecosystem
- **Hugo** (Go) - If you want maximum simplicity
- **Eleventy** (JS) - If you want minimal overhead

**Why not Laravel for this use case:**
- ⚠️ Requires PHP runtime (more resource usage)
- ⚠️ More complex than needed for 5-6 static pages
- ⚠️ Slower builds and deployments
- ⚠️ Overkill for marketing site

**Decision: Astro (Recommended)**

We'll use **Astro** because:
1. Perfect for 95% static marketing sites
2. Cutting-edge framework with clear advantages
3. Zero JS overhead (better performance)
4. Easy GitHub Actions + Railway deployment
5. Can add interactivity only where needed (islands)
6. Modern developer experience

**If you prefer PHP:** Jigsaw is a solid alternative that uses Blade templates and the PHP ecosystem.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Repository                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  website/ (Laravel Application)                     │  │
│  │  ├── app/                                            │  │
│  │  ├── resources/views/                                │  │
│  │  ├── public/                                         │  │
│  │  └── routes/web.php                                  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ GitHub Actions Workflow
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Railway Platform                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Build: composer install --no-dev                    │  │
│  │  Start: php artisan serve                            │  │
│  │  Port: $PORT (Railway auto-assigns)                  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
                    Public Website
```

## Site Structure

Based on the market research document (`docs/research/brand-marketing-trust.md`), the website should include:

### Core Pages

1. **Homepage (`/`)**
   - Hero section with clear value proposition
   - Key features overview
   - Trust signals (open source, local execution, security)
   - Call-to-action (Get Started / View Docs)

2. **About (`/about`)**
   - Personal authority section (senior enterprise engineer background)
   - Experience with Java EE / Jakarta / Spring
   - Clear statement: "Built by someone who has done these migrations"
   - Conservative, professional tone

3. **Security & Privacy (`/security`)**
   - **Critical page** - addresses enterprise concerns
   - Explicit answers to:
     - Does this send code off-machine? (No)
     - Does it require internet? (When?)
     - Does it collect telemetry? (Opt-in?)
     - Can it run offline? (Yes)
     - What data touches Stripe? (Payment only)
   - Stateless architecture explanation
   - Local execution emphasis

4. **Features (`/features`)**
   - Core features (free tier)
   - Premium features (paid tier)
   - Conservative messaging (no "magic", "instant", "one-click")
   - Use: "Deterministic", "Repeatable", "Validated", "Safe defaults"

5. **Documentation (`/docs`)**
   - Link to GitHub docs or embed key documentation
   - Setup instructions
   - Usage examples
   - API reference (if applicable)

6. **Pricing (`/pricing`)**
   - Free tier clearly explained
   - Premium features and pricing
   - Enterprise options
   - Conservative messaging

### Design Principles (from Research)

1. **Conservative & Professional**
   - No flashy animations
   - Clean, readable typography
   - Boring is good (trust signal)
   - Enterprise-focused design

2. **Trust Signals**
   - Open source badge/link
   - Security & Privacy page prominent
   - Personal authority (About page)
   - Explicit guarantees (no code storage, local execution)

3. **Transparency**
   - Clear feature differentiation (free vs paid)
   - Explicit about what runs locally
   - No marketing fluff

## Technical Architecture

### Laravel Structure

```
website/
├── app/
│   ├── Http/
│   │   └── Controllers/
│   │       ├── HomeController.php
│   │       ├── AboutController.php
│   │       ├── SecurityController.php
│   │       ├── FeaturesController.php
│   │       └── DocsController.php
│   └── View/
│       └── Composers/ (for shared data)
├── resources/
│   ├── views/
│   │   ├── layouts/
│   │   │   └── app.blade.php (main layout)
│   │   ├── components/ (reusable components)
│   │   ├── home.blade.php
│   │   ├── about.blade.php
│   │   ├── security.blade.php
│   │   ├── features.blade.php
│   │   └── docs.blade.php
│   └── css/
│       └── app.css (Tailwind CSS or custom)
├── public/
│   ├── index.php
│   └── assets/ (compiled CSS/JS)
├── routes/
│   └── web.php
├── composer.json
├── package.json (for asset compilation)
└── railway.json (Railway deployment config)
```

### Key Technologies

- **Laravel 11** (latest stable)
- **Blade Templates** (Laravel's templating engine)
- **Tailwind CSS** (utility-first CSS, easy to maintain)
- **No Database** (initially - all static content)
- **Markdown Support** (for documentation pages)

### Deployment Configuration

#### Railway Configuration (`railway.json`)

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "composer install --no-dev --optimize-autoloader && npm install && npm run build"
  },
  "deploy": {
    "startCommand": "php artisan serve --host=0.0.0.0 --port=$PORT",
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

#### GitHub Actions Workflow (`.github/workflows/deploy-website.yml`)

```yaml
name: Deploy Website to Railway

on:
  push:
    branches: [main]
    paths:
      - 'website/**'
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup PHP
        uses: shivammathur/setup-php@v2
        with:
          php-version: '8.2'
          extensions: mbstring, xml, curl, zip
      
      - name: Deploy to Railway
        uses: bervProject/railway-deploy@v3.0.0
        with:
          railway_token: ${{ secrets.RAILWAY_TOKEN }}
          service: website
          detach: true
```

## Content Strategy

### Homepage Messaging

**Hero Section:**
> "Open core migration engine, built by a senior Java engineer, designed to be security-reviewable, deterministic, and safe for enterprise use."

**Key Points:**
- Open core (free tier available)
- Built by experienced engineer
- Security-reviewable
- Deterministic (not "magic")
- Enterprise-safe

### Trust-Building Elements

1. **Security Badge** (prominent on homepage)
   - "100% Local Execution Available"
   - "No Code Storage"
   - "Open Source & Auditable"

2. **Personal Authority**
   - Name and background on About page
   - Experience highlights
   - "I've done these migrations" narrative

3. **Transparency**
   - Clear free vs paid distinction
   - Explicit privacy guarantees
   - Open source link prominent

## SEO & Performance

### SEO Considerations

- Semantic HTML structure
- Meta tags for each page
- Open Graph tags for social sharing
- Sitemap.xml generation
- robots.txt configuration

### Performance

- Minimal JavaScript (progressive enhancement)
- Optimized images (WebP format)
- CSS minification
- Asset versioning for cache busting
- CDN via Railway (if available)

## Future Enhancements

### Phase 2 (if needed)

1. **Contact Form**
   - Laravel Mail integration
   - Spam protection (reCAPTCHA)

2. **Blog Section**
   - Markdown-based posts
   - RSS feed generation

3. **Documentation Search**
   - Full-text search of docs
   - Algolia or similar

4. **Analytics**
   - Privacy-respecting analytics (Plausible, Fathom)
   - No Google Analytics (trust signal)

## Security Considerations

1. **HTTPS Only** (Railway provides automatically)
2. **Security Headers**
   - Content Security Policy
   - X-Frame-Options
   - X-Content-Type-Options
3. **No User Input Initially** (static content only)
4. **Dependency Updates** (Dependabot for composer.json)

## Development Workflow

1. **Local Development**
   ```bash
   cd website
   composer install
   npm install
   php artisan serve
   ```

2. **Content Updates**
   - Edit Blade templates
   - Update Markdown files (if using)
   - Commit to `main` branch
   - Auto-deploy via GitHub Actions

3. **Testing**
   - Manual testing before deployment
   - Lighthouse CI for performance
   - Broken link checking

## File Organization

### Repository Structure

```
JakartaMigrationMCP/
├── website/                    # New Laravel application
│   ├── app/
│   ├── resources/
│   ├── routes/
│   ├── public/
│   ├── composer.json
│   ├── package.json
│   └── railway.json
├── docs/                       # Existing documentation
│   └── research/
│       └── brand-marketing-trust.md
└── .github/
    └── workflows/
        └── deploy-website.yml  # New workflow
```

## Next Steps

1. ✅ **Architecture Document** (this file)
2. ⏭️ **Create Laravel Application**
   - Initialize Laravel project in `website/` directory
   - Set up basic routing
   - Create layout template
3. ⏭️ **Design & Content**
   - Create page templates
   - Write content based on research
   - Implement Tailwind CSS styling
4. ⏭️ **Deployment Setup**
   - Configure Railway project
   - Set up GitHub Actions workflow
   - Test deployment pipeline
5. ⏭️ **Content Migration**
   - Port key content from README
   - Create Security & Privacy page
   - Write About page content

## Questions to Resolve

1. **Domain Name**: What domain will be used? (affects Railway configuration)
2. **Personal Name**: Should we use your actual name on the About page?
3. **GitHub Integration**: Link to main repo or separate repo for website?
4. **Documentation**: Embed from main repo or duplicate key docs?

## References

- Market Research: `docs/research/brand-marketing-trust.md`
- Main Project README: `README.md`
- Railway Deployment: `docs/strategy/RAILWAY_DEPLOYMENT_CHECKLIST.md`
