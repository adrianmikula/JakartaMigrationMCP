# Quick Start Guide

## First Time Setup

1. **Install dependencies:**
   ```bash
   cd website
   npm install
   ```

2. **Start development server:**
   ```bash
   npm run dev
   ```

3. **Open in browser:**
   Navigate to `http://localhost:4321`

## Building for Production

```bash
npm run build
```

This creates a `dist/` folder with static files ready for deployment.

## Local Preview of Production Build

```bash
npm run preview
```

## Deployment

### Railway (Recommended)

1. Connect your GitHub repository to Railway
2. Railway will automatically detect the `railway.json` configuration
3. The site will deploy automatically on pushes to `main` branch (via GitHub Actions)

### Manual Railway Deployment

If you need to deploy manually:

1. Build the site: `npm run build`
2. Deploy the `dist/` folder to Railway

### Other Platforms

- **Netlify**: Connect repo, build: `npm run build`, publish: `dist`
- **Vercel**: Connect repo, framework: Astro
- **GitHub Pages**: Use GitHub Actions

## Project Structure

- `src/pages/` - Route pages (file-based routing)
- `src/components/` - Reusable components
- `src/layouts/` - Page layouts
- `public/` - Static assets (copied as-is to dist/)

## Customization

- **Colors**: Edit `tailwind.config.mjs`
- **Site URL**: Update `site` in `astro.config.mjs`
- **Content**: Edit files in `src/pages/`

## Next Steps

1. Update domain in `astro.config.mjs`
2. Customize content in page files
3. Add your own images to `public/`
4. Deploy!
