# Jakarta Migration MCP Website - Laravel Version

This is the Laravel version of the Jakarta Migration MCP website, located in the `website-laravel` folder.

## Setup

1. Install PHP dependencies:
   ```bash
   composer install
   ```

2. Install Node.js dependencies:
   ```bash
   npm install
   ```

3. Copy environment file:
   ```bash
   cp .env.example .env
   ```

4. Generate application key:
   ```bash
   php artisan key:generate
   ```

5. Build assets:
   ```bash
   npm run build
   ```

6. Start the development server:
   ```bash
   php artisan serve
   ```

   Or use Vite for hot reloading:
   ```bash
   npm run dev
   ```

## Features

- Laravel 10 framework
- Tailwind CSS with Flowbite components
- Blade templating engine
- Vite for asset bundling
- Same design and content as the Astro version

## Pages

- `/` - Home
- `/about` - About
- `/features` - Features
- `/security` - Security & Privacy
- `/pricing` - Pricing
- `/docs` - Documentation
