@php
    $currentPath = request()->path();
    $isActive = fn($path) => $currentPath === $path || ($path === '/' && $currentPath === '');
@endphp

<header class="bg-dark-300/80 backdrop-blur-md border-b border-primary-600/30 sticky top-0 z-50 shadow-lg shadow-primary-600/10">
    <nav class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
            <div class="flex items-center">
                <a href="{{ route('home') }}" class="text-xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-neon-cyan to-primary-400 hover:from-neon-cyan hover:to-neon-blue transition-all">
                    Jakarta Migration MCP
                </a>
            </div>
            
            <!-- Desktop Navigation -->
            <div class="hidden md:flex items-center space-x-1">
                <a 
                    href="{{ route('home') }}" 
                    class="px-3 py-2 text-sm font-medium rounded-lg transition-all {{ $isActive('/') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Home
                </a>
                <a 
                    href="{{ route('about') }}" 
                    class="px-3 py-2 text-sm font-medium rounded-lg transition-all {{ $isActive('about') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    About
                </a>
                <a 
                    href="{{ route('features') }}" 
                    class="px-3 py-2 text-sm font-medium rounded-lg transition-all {{ $isActive('features') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Features
                </a>
                <a 
                    href="{{ route('security') }}" 
                    class="px-3 py-2 text-sm font-medium rounded-lg transition-all {{ $isActive('security') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Security
                </a>
                <a 
                    href="{{ route('pricing') }}" 
                    class="px-3 py-2 text-sm font-medium rounded-lg transition-all {{ $isActive('pricing') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Pricing
                </a>
                <a 
                    href="{{ route('docs') }}" 
                    class="px-3 py-2 text-sm font-medium rounded-lg transition-all {{ $isActive('docs') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Docs
                </a>
            </div>
            
            <!-- Mobile menu button -->
            <button 
                type="button" 
                class="md:hidden inline-flex items-center p-2 w-10 h-10 justify-center text-sm text-gray-300 rounded-lg hover:bg-dark-200/50 hover:text-neon-cyan focus:outline-none focus:ring-2 focus:ring-primary-600/50 transition-all"
                data-collapse-toggle="mobile-menu"
                aria-controls="mobile-menu"
                aria-expanded="false"
                aria-label="Toggle menu"
            >
                <svg class="w-5 h-5" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 17 14">
                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M1 1h15M1 7h15M1 13h15"/>
                </svg>
            </button>
        </div>
        
        <!-- Mobile menu -->
        <div class="hidden md:hidden" id="mobile-menu">
            <div class="px-2 pt-2 pb-3 space-y-1 border-t border-primary-600/30 mt-2">
                <a 
                    href="{{ route('home') }}" 
                    class="block px-3 py-2 text-base font-medium rounded-lg transition-all {{ $isActive('/') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Home
                </a>
                <a 
                    href="{{ route('about') }}" 
                    class="block px-3 py-2 text-base font-medium rounded-lg transition-all {{ $isActive('about') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    About
                </a>
                <a 
                    href="{{ route('features') }}" 
                    class="block px-3 py-2 text-base font-medium rounded-lg transition-all {{ $isActive('features') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Features
                </a>
                <a 
                    href="{{ route('security') }}" 
                    class="block px-3 py-2 text-base font-medium rounded-lg transition-all {{ $isActive('security') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Security
                </a>
                <a 
                    href="{{ route('pricing') }}" 
                    class="block px-3 py-2 text-base font-medium rounded-lg transition-all {{ $isActive('pricing') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Pricing
                </a>
                <a 
                    href="{{ route('docs') }}" 
                    class="block px-3 py-2 text-base font-medium rounded-lg transition-all {{ $isActive('docs') ? 'text-neon-cyan bg-primary-600/20 shadow-glow-cyan' : 'text-gray-300 hover:text-neon-cyan hover:bg-dark-200/50' }}"
                >
                    Docs
                </a>
            </div>
        </div>
    </nav>
</header>
