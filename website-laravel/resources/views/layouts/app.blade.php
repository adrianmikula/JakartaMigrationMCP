<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">

    <title>{{ $title ?? 'Jakarta Migration MCP' }}</title>
    <meta name="description" content="{{ $description ?? 'Open core migration engine for Java EE to Jakarta EE migration. Built by a senior Java engineer, designed to be security-reviewable, deterministic, and safe for enterprise use.' }}">

    <!-- Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

    <!-- Scripts -->
    @vite(['resources/css/app.css', 'resources/js/app.js'])
    
    <style>
        body {
            background: linear-gradient(135deg, #0a1929 0%, #16213e 50%, #1a1a2e 100%);
            background-attachment: fixed;
        }
        
        /* Custom scrollbar for dark theme */
        ::-webkit-scrollbar {
            width: 10px;
        }
        
        ::-webkit-scrollbar-track {
            background: #0a1929;
        }
        
        ::-webkit-scrollbar-thumb {
            background: #0891b2;
            border-radius: 5px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: #06b6d4;
        }
    </style>
</head>
<body class="min-h-screen flex flex-col bg-gradient-tech text-gray-100">
    @include('components.header')

    <main class="flex-grow">
        @yield('content')
    </main>

    @include('components.footer')
</body>
</html>
