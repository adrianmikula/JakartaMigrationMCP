#!/usr/bin/env node

/**
 * Jakarta Migration MCP Server
 * 
 * This is a lightweight Node.js wrapper that downloads and runs the Java JAR file
 * for the Jakarta Migration MCP server. It works with npx and npm installations.
 */

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');
const https = require('https');
const http = require('http');

const PACKAGE_NAME = '@jakarta-migration/mcp-server';

// Get version from environment or package.json
let VERSION = process.env.JAKARTA_MCP_VERSION;
if (!VERSION) {
  try {
    VERSION = require('./package.json').version || '1.0.0';
  } catch (e) {
    VERSION = '1.0.0';
  }
}

const GITHUB_REPO = process.env.GITHUB_REPO || 'your-org/JakartaMigrationMCP';
const JAR_NAME = `bug-bounty-finder-${VERSION}.jar`;
const GITHUB_RELEASES_URL = `https://github.com/${GITHUB_REPO}/releases/download/v${VERSION}`;

// Determine OS-specific paths
const isWindows = process.platform === 'win32';
const homeDir = os.homedir();
const cacheDir = path.join(homeDir, isWindows ? 'AppData' : '.cache', 'jakarta-migration-mcp');

// Allow override of JAR path via environment variable
const jarPath = process.env.JAKARTA_MCP_JAR_PATH || path.join(cacheDir, JAR_NAME);

// Java executable detection
function findJavaExecutable() {
  const javaCommands = isWindows ? ['java.exe', 'java'] : ['java'];
  
  for (const cmd of javaCommands) {
    try {
      const { execSync } = require('child_process');
      execSync(`${cmd} -version`, { stdio: 'ignore' });
      return cmd;
    } catch (e) {
      // Continue searching
    }
  }
  
  return null;
}

// Download JAR file from GitHub releases
function downloadJar() {
  return new Promise((resolve, reject) => {
    // Ensure cache directory exists
    if (!fs.existsSync(cacheDir)) {
      fs.mkdirSync(cacheDir, { recursive: true });
    }

    // Check if JAR already exists
    if (fs.existsSync(jarPath)) {
      console.error(`Using cached JAR: ${jarPath}`);
      resolve(jarPath);
      return;
    }

    const url = `${GITHUB_RELEASES_URL}/${JAR_NAME}`;
    console.error(`Downloading JAR from ${url}...`);

    const protocol = url.startsWith('https:') ? https : http;
    const file = fs.createWriteStream(jarPath);

    protocol.get(url, (response) => {
      if (response.statusCode === 302 || response.statusCode === 301) {
        // Handle redirect
        return downloadJar().then(resolve).catch(reject);
      }

      if (response.statusCode !== 200) {
        file.close();
        fs.unlinkSync(jarPath);
        reject(new Error(`Failed to download JAR: HTTP ${response.statusCode}`));
        return;
      }

      response.pipe(file);

      file.on('finish', () => {
        file.close();
        console.error(`JAR downloaded successfully to ${jarPath}`);
        resolve(jarPath);
      });
    }).on('error', (err) => {
      file.close();
      if (fs.existsSync(jarPath)) {
        fs.unlinkSync(jarPath);
      }
      reject(err);
    });
  });
}

// Main execution function
async function main() {
  try {
    // Check for Java
    const javaCmd = findJavaExecutable();
    if (!javaCmd) {
      console.error('ERROR: Java is not installed or not in PATH.');
      console.error('Please install Java 21+ from https://adoptium.net/');
      process.exit(1);
    }

    // Download JAR if needed
    let jar = jarPath;
    if (!fs.existsSync(jar)) {
      try {
        jar = await downloadJar();
      } catch (error) {
        console.error(`ERROR: Failed to download JAR: ${error.message}`);
        console.error(`Please ensure the JAR is available at: ${GITHUB_RELEASES_URL}/${JAR_NAME}`);
        console.error('Or build it locally with: ./gradlew bootJar');
        console.error('For local development, you can set JAKARTA_MCP_JAR_PATH to point to a local JAR file');
        
        // Check for local JAR path override
        const localJarPath = process.env.JAKARTA_MCP_JAR_PATH;
        if (localJarPath && fs.existsSync(localJarPath)) {
          console.error(`Using local JAR: ${localJarPath}`);
          jar = localJarPath;
        } else {
          process.exit(1);
        }
      }
    }

    // Run the JAR with MCP-specific arguments
    // Determine transport mode: stdio (default, local) or sse (Apify/HTTP)
    const transport = process.env.MCP_TRANSPORT || 'stdio';
    const profile = transport === 'sse' ? 'mcp-sse' : 'mcp-stdio';
    
    const args = [
      '-jar',
      jar,
      `--spring.profiles.active=${profile}`,
      `--spring.ai.mcp.server.transport=${transport}`
    ];
    
    // For stdio mode, disable web server
    if (transport === 'stdio') {
      args.push('--spring.main.web-application-type=none');
    }

    // Pass through any additional arguments
    const mcpArgs = process.argv.slice(2);
    args.push(...mcpArgs);

    console.error(`Starting Jakarta Migration MCP Server...`);
    console.error(`Java: ${javaCmd}`);
    console.error(`JAR: ${jar}`);

    const javaProcess = spawn(javaCmd, args, {
      stdio: 'inherit',
      env: process.env
    });

    javaProcess.on('error', (error) => {
      console.error(`ERROR: Failed to start Java process: ${error.message}`);
      process.exit(1);
    });

    javaProcess.on('exit', (code) => {
      process.exit(code || 0);
    });

    // Handle signals
    process.on('SIGINT', () => {
      javaProcess.kill('SIGINT');
    });

    process.on('SIGTERM', () => {
      javaProcess.kill('SIGTERM');
    });

  } catch (error) {
    console.error(`ERROR: ${error.message}`);
    process.exit(1);
  }
}

// Run if executed directly
if (require.main === module) {
  main();
}

module.exports = { main };

