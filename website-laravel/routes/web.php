<?php

use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return view('home', [
        'title' => 'Jakarta Migration MCP - Enterprise Java Migration Tools',
        'description' => 'Open core migration engine for Java EE to Jakarta EE migration. Built by a senior Java engineer, designed to be security-reviewable, deterministic, and safe for enterprise use.'
    ]);
})->name('home');

Route::get('/about', function () {
    return view('about', [
        'title' => 'About - Jakarta Migration MCP',
        'description' => 'Learn about the Jakarta Migration MCP project and the senior Java engineer behind it.'
    ]);
})->name('about');

Route::get('/features', function () {
    return view('features', [
        'title' => 'Features - Jakarta Migration MCP',
        'description' => 'Features and capabilities of the Jakarta Migration MCP Server for Java EE to Jakarta EE migration.'
    ]);
})->name('features');

Route::get('/security', function () {
    return view('security', [
        'title' => 'Security & Privacy - Jakarta Migration MCP',
        'description' => 'Security and privacy guarantees for Jakarta Migration MCP. Learn how we handle your code and data.'
    ]);
})->name('security');

Route::get('/pricing', function () {
    return view('pricing', [
        'title' => 'Pricing - Jakarta Migration MCP',
        'description' => 'Pricing and features for Jakarta Migration MCP. Free tier available with premium features coming soon.'
    ]);
})->name('pricing');

Route::get('/docs', function () {
    return view('docs', [
        'title' => 'Documentation - Jakarta Migration MCP',
        'description' => 'Documentation and setup instructions for Jakarta Migration MCP Server.'
    ]);
})->name('docs');
