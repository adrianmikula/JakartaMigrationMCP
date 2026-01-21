# Jakarta Migration MCP Website

Professional marketing website for the Jakarta Migration MCP project, built with Astro.

## Features

- **Static Site Generation** - Fast, SEO-friendly static pages
- **Zero JavaScript by Default** - Minimal JS overhead, only loads for interactive components
- **Modern Design** - Clean, professional design with Tailwind CSS
- **Responsive** - Works perfectly on all devices
- **Easy Deployment** - Deploy to Railway, Netlify, Vercel, or any static host

## Development

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Setup

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

The site will be available at `http://localhost:4321` during development.

## Project Structure

```
website/
├── public/          # Static assets (favicon, images, etc.)
├── src/
│   ├── components/  # Reusable Astro components
│   ├── layouts/     # Page layouts
│   └── pages/       # Route pages (file-based routing)
├── astro.config.mjs # Astro configuration
├── tailwind.config.mjs # Tailwind CSS configuration
└── package.json
```

## Deployment

### Railway

The site is configured for Railway deployment. Simply connect your GitHub repository to Railway and it will automatically:

1. Build the site with `npm install && npm run build`
2. Serve the static files with `npx serve dist -p $PORT`

### Other Platforms

The site can be deployed to any static hosting platform:

- **Netlify**: Connect repository, build command: `npm run build`, publish directory: `dist`
- **Vercel**: Connect repository, framework preset: Astro
- **GitHub Pages**: Use GitHub Actions to build and deploy

## Pages

- `/` - Homepage with hero, features overview, and trust badges
- `/about` - About page with personal authority and experience
- `/security` - Security & Privacy page (critical for enterprise trust)
- `/features` - Detailed features list
- `/pricing` - Pricing tiers (Free, Premium, Enterprise)
- `/docs` - Documentation and setup instructions

## Styling

The site uses Tailwind CSS for styling. Customize colors and theme in `tailwind.config.mjs`.

## License

MIT License - See main project LICENSE file.
