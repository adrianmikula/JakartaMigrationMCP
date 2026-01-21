# Laravel Website Setup Guide

## Prerequisites

To run this Laravel website, you need:

1. **PHP 8.1 or higher** - Download from https://www.php.net/downloads.php
2. **Composer** - PHP dependency manager - Download from https://getcomposer.org/download/
3. **Node.js** (already installed ✓)

## Setup Steps

1. **Install PHP dependencies:**
   ```bash
   composer install
   ```

2. **Create environment file:**
   ```bash
   copy .env.example .env
   ```
   Or create `.env` manually with:
   ```
   APP_NAME="Jakarta Migration MCP"
   APP_ENV=local
   APP_KEY=
   APP_DEBUG=true
   APP_URL=http://localhost:8000
   ```

3. **Generate application key:**
   ```bash
   php artisan key:generate
   ```

4. **Build frontend assets:**
   ```bash
   npm run build
   ```
   Or for development with hot reload:
   ```bash
   npm run dev
   ```

5. **Start the development server:**
   ```bash
   php artisan serve
   ```
   
   The site will be available at: http://localhost:8000

## Alternative: Using Laravel Sail (Docker)

If you have Docker installed, you can use Laravel Sail:

```bash
composer require laravel/sail --dev
php artisan sail:install
./vendor/bin/sail up
```

## Quick Start (After Prerequisites)

```bash
composer install
php artisan key:generate
npm run build
php artisan serve
```
