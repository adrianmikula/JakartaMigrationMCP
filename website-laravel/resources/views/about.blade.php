@extends('layouts.app')

@section('content')
<div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16 lg:py-20">
    <div class="text-center mb-12">
        <h1 class="text-4xl md:text-5xl font-bold text-gray-900 mb-4">About</h1>
        <p class="text-xl text-gray-600 max-w-2xl mx-auto">
            This tool exists because I have personally done these migrations and know where they fail.
        </p>
    </div>
    
    <div class="space-y-12">
        <!-- Personal Authority Section -->
        <section class="bg-white rounded-xl border border-gray-200 shadow-md p-8">
            <h2 class="text-2xl font-bold text-gray-900 mb-6">Personal Authority</h2>
            <p class="text-gray-700 mb-6">
                I am a senior enterprise engineer with deep Java experience, including:
            </p>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="flex items-start">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <span class="text-gray-700">Extensive experience with Java EE / Jakarta EE</span>
                </div>
                <div class="flex items-start">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <span class="text-gray-700">Deep knowledge of Spring Framework and Spring Boot</span>
                </div>
                <div class="flex items-start">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <span class="text-gray-700">Hands-on experience with large enterprise systems</span>
                </div>
                <div class="flex items-start">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <span class="text-gray-700">Real-world Jakarta migration projects</span>
                </div>
            </div>
        </section>

        <!-- Why This Tool Exists Section -->
        <section class="bg-gradient-to-br from-gray-50 to-white rounded-xl border border-gray-200 shadow-md p-8">
            <h2 class="text-2xl font-bold text-gray-900 mb-6">Why This Tool Exists</h2>
            <p class="text-gray-700 mb-6">
                Migrating from Java EE 8 (<code class="bg-gray-100 px-2 py-1 rounded text-sm font-mono">javax.*</code>) to Jakarta EE 9+ (<code class="bg-gray-100 px-2 py-1 rounded text-sm font-mono">jakarta.*</code>) is complex because:
            </p>
            <div class="space-y-4">
                <div class="flex items-start">
                    <div class="flex-shrink-0 w-8 h-8 bg-primary-100 rounded-lg flex items-center justify-center mr-4">
                        <span class="text-primary-600 font-bold">1</span>
                    </div>
                    <div>
                        <h3 class="font-semibold text-gray-900 mb-1">Dependency Hell</h3>
                        <p class="text-gray-600 text-sm">Many libraries haven't migrated, creating transitive conflicts</p>
                    </div>
                </div>
                <div class="flex items-start">
                    <div class="flex-shrink-0 w-8 h-8 bg-primary-100 rounded-lg flex items-center justify-center mr-4">
                        <span class="text-primary-600 font-bold">2</span>
                    </div>
                    <div>
                        <h3 class="font-semibold text-gray-900 mb-1">Binary Incompatibility</h3>
                        <p class="text-gray-600 text-sm">Compiled JARs may reference <code class="bg-gray-100 px-1 rounded text-xs font-mono">javax.*</code> internally</p>
                    </div>
                </div>
                <div class="flex items-start">
                    <div class="flex-shrink-0 w-8 h-8 bg-primary-100 rounded-lg flex items-center justify-center mr-4">
                        <span class="text-primary-600 font-bold">3</span>
                    </div>
                    <div>
                        <h3 class="font-semibold text-gray-900 mb-1">Hidden Dependencies</h3>
                        <p class="text-gray-600 text-sm"><code class="bg-gray-100 px-1 rounded text-xs font-mono">javax.*</code> usage in XML configs, annotations, and dynamic loading</p>
                    </div>
                </div>
                <div class="flex items-start">
                    <div class="flex-shrink-0 w-8 h-8 bg-primary-100 rounded-lg flex items-center justify-center mr-4">
                        <span class="text-primary-600 font-bold">4</span>
                    </div>
                    <div>
                        <h3 class="font-semibold text-gray-900 mb-1">Risk Assessment</h3>
                        <p class="text-gray-600 text-sm">Need to understand migration impact before starting</p>
                    </div>
                </div>
            </div>
            <p class="text-gray-700 mt-6 pt-6 border-t border-gray-200">
                This MCP server provides AI assistants with the specialized knowledge and tools to navigate these challenges effectively.
            </p>
        </section>

        <!-- Our Approach Section -->
        <section class="bg-white rounded-xl border border-gray-200 shadow-md p-8">
            <h2 class="text-2xl font-bold text-gray-900 mb-6">Our Approach</h2>
            <p class="text-gray-700 mb-6">We believe in:</p>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="flex items-start p-4 bg-gray-50 rounded-lg">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <div>
                        <p class="font-semibold text-gray-900">Deterministic</p>
                        <p class="text-sm text-gray-600">Not "magic", but predictable, repeatable results</p>
                    </div>
                </div>
                <div class="flex items-start p-4 bg-gray-50 rounded-lg">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <div>
                        <p class="font-semibold text-gray-900">Transparent</p>
                        <p class="text-sm text-gray-600">Open source, auditable, clear about what happens</p>
                    </div>
                </div>
                <div class="flex items-start p-4 bg-gray-50 rounded-lg">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <div>
                        <p class="font-semibold text-gray-900">Safe Defaults</p>
                        <p class="text-sm text-gray-600">Conservative approach that respects enterprise risk management</p>
                    </div>
                </div>
                <div class="flex items-start p-4 bg-gray-50 rounded-lg">
                    <svg class="h-6 w-6 text-primary-600 mr-3 flex-shrink-0 mt-1" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    <div>
                        <p class="font-semibold text-gray-900">Validated</p>
                        <p class="text-sm text-gray-600">Every migration step can be reviewed and verified</p>
                    </div>
                </div>
            </div>
        </section>

        <!-- Callout Box -->
        <div class="bg-gradient-to-r from-primary-50 to-primary-100 border border-primary-200 rounded-xl p-8 shadow-sm">
            <div class="flex items-start">
                <svg class="h-8 w-8 text-primary-600 mr-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                </svg>
                <div>
                    <p class="text-primary-900 font-bold text-lg mb-2">
                        Open Core Migration Engine
                    </p>
                    <p class="text-primary-800">
                        Built by a senior Java engineer, designed to be security-reviewable, deterministic, and safe for enterprise use.
                    </p>
                </div>
            </div>
        </div>
    </div>
</div>
@endsection
