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
let PACKAGE_JSON = null;
if (!VERSION) {
  try {
    PACKAGE_JSON = require('./package.json');
    VERSION = PACKAGE_JSON.version || '1.0.0';
  } catch (e) {
    VERSION = '1.0.0';
  }
}

// Get GitHub repo from environment, package.json, or default
let GITHUB_REPO = process.env.GITHUB_REPO;
if (!GITHUB_REPO && PACKAGE_JSON && PACKAGE_JSON.repository && PACKAGE_JSON.repository.url) {
  // Extract repo from URL like "https://github.com/user/repo.git" or "git@github.com:user/repo.git"
  const repoMatch = PACKAGE_JSON.repository.url.match(/github\.com[/:]([^/]+\/[^/]+?)(?:\.git)?$/);
  if (repoMatch) {
    GITHUB_REPO = repoMatch[1];
  }
}
GITHUB_REPO = GITHUB_REPO || 'your-org/JakartaMigrationMCP';

// Warn if using placeholder repository
if (GITHUB_REPO.includes('your-org') || GITHUB_REPO.includes('your-repo')) {
  console.error('WARNING: GitHub repository is still a placeholder. Set GITHUB_REPO environment variable or update package.json repository.url');
}

const JAR_NAME = `jakarta-migration-mcp-${VERSION}.jar`;
const GITHUB_RELEASES_URL = `https://github.com/${GITHUB_REPO}/releases/download/v${VERSION}`;

// Determine OS-specific paths
const isWindows = process.platform === 'win32';
const homeDir = os.homedir();
const cacheDir = path.join(homeDir, isWindows ? 'AppData' : '.cache', 'jakarta-migration-mcp');

// Configuration file path for license and settings
const settingsDir = path.join(homeDir, '.mcp-settings');
const configFile = path.join(settingsDir, 'jakarta-migration-license.json');

// Allow override of JAR path via environment variable
const jarPath = process.env.JAKARTA_MCP_JAR_PATH || path.join(cacheDir, JAR_NAME);

// Load configuration from JSON file
// Exported for testing purposes
function loadConfiguration(configFilePath = configFile) {
  // Check if config file exists
  if (fs.existsSync(configFilePath)) {
    try {
      const configContent = fs.readFileSync(configFilePath, 'utf8');
      const parsed = JSON.parse(configContent);
      
      // Load environment variables from the "environment" section
      if (parsed.environment && typeof parsed.environment === 'object') {
        Object.keys(parsed.environment).forEach(key => {
          // Skip 'comment' keys - they are for documentation only
          if (key === 'comment' || key.startsWith('_comment_')) {
            return;
          }
          // System environment variables take precedence
          // Only set if the key doesn't exist in process.env (not just falsy)
          // This respects explicitly-set empty strings (e.g., STRIPE_SECRET_KEY="" to disable)
          // Skip null/undefined values from config file
          if (!(key in process.env) && parsed.environment[key] != null) {
            process.env[key] = String(parsed.environment[key]);
          }
        });
        
        console.error(`Loaded configuration from: ${configFilePath}`);
        return true;
      } else {
        console.error(`Warning: Configuration file exists but missing "environment" section: ${configFilePath}`);
        return false;
      }
    } catch (error) {
      console.error(`Warning: Failed to load configuration from ${configFilePath}: ${error.message}`);
      console.error(`Please ensure the file is valid JSON. See jakarta-migration-license.json.example for format.`);
      return false;
    }
  }
  return false;
}

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
function downloadJar(urlOverride = null) {
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

    const url = urlOverride || `${GITHUB_RELEASES_URL}/${JAR_NAME}`;
    console.error(`Downloading JAR from ${url}...`);

    const protocol = url.startsWith('https:') ? https : http;
    const file = fs.createWriteStream(jarPath);
    let hasError = false;

    const cleanup = () => {
      if (!hasError) {
        hasError = true;
        file.close();
        if (fs.existsSync(jarPath)) {
          try {
            fs.unlinkSync(jarPath);
          } catch (e) {
            // Ignore cleanup errors
          }
        }
      }
    };

    const request = protocol.get(url, (response) => {
      // Handle redirects (GitHub releases often redirect)
      if (response.statusCode === 301 || response.statusCode === 302 || response.statusCode === 307 || response.statusCode === 308) {
        const location = response.headers.location;
        if (location) {
          cleanup();
          console.error(`Following redirect to: ${location}`);
          // Recursively follow redirect
          return downloadJar(location).then(resolve).catch(reject);
        } else {
          cleanup();
          reject(new Error(`Redirect received but no Location header: HTTP ${response.statusCode}`));
          return;
        }
      }

      if (response.statusCode !== 200) {
        cleanup();
        reject(new Error(`Failed to download JAR: HTTP ${response.statusCode} ${response.statusMessage || ''}`));
        return;
      }

      // Track download progress
      const totalSize = parseInt(response.headers['content-length'] || '0', 10);
      let downloadedSize = 0;

      response.on('data', (chunk) => {
        downloadedSize += chunk.length;
        if (totalSize > 0) {
          const percent = ((downloadedSize / totalSize) * 100).toFixed(1);
          process.stderr.write(`\rDownloading: ${percent}% (${(downloadedSize / 1024 / 1024).toFixed(2)} MB / ${(totalSize / 1024 / 1024).toFixed(2)} MB)`);
        }
      });

      response.pipe(file);

      file.on('error', (err) => {
        cleanup();
        reject(new Error(`File write error: ${err.message}`));
      });

      file.on('finish', () => {
        file.close();
        if (totalSize > 0) {
          process.stderr.write('\n');
        }
        
        // Verify the downloaded file is valid
        try {
          const stats = fs.statSync(jarPath);
          if (stats.size === 0) {
            cleanup();
            reject(new Error('Downloaded JAR file is empty'));
            return;
          }
          
          // Check if it's a valid JAR file (starts with ZIP magic bytes)
          const buffer = Buffer.alloc(4);
          const fd = fs.openSync(jarPath, 'r');
          fs.readSync(fd, buffer, 0, 4, 0);
          fs.closeSync(fd);
          
          // JAR files are ZIP files, which start with PK (0x50 0x4B)
          if (buffer[0] !== 0x50 || buffer[1] !== 0x4B) {
            cleanup();
            reject(new Error('Downloaded file does not appear to be a valid JAR file (missing ZIP signature)'));
            return;
          }
        } catch (err) {
          cleanup();
          reject(new Error(`Failed to verify downloaded JAR: ${err.message}`));
          return;
        }
        
        console.error(`JAR downloaded successfully to ${jarPath}`);
        resolve(jarPath);
      });
    });

    request.on('error', (err) => {
      cleanup();
      reject(new Error(`Request error: ${err.message}`));
    });

    // Set a timeout (30 seconds)
    request.setTimeout(30000, () => {
      request.destroy();
      cleanup();
      reject(new Error('Download timeout after 30 seconds'));
    });
  });
}

// Main execution function
async function main() {
  try {
    // Load configuration from JSON file (if exists)
    // This allows corporate users to store license keys and API tokens securely
    loadConfiguration();
    
    // Check for --download-only flag
    const args = process.argv.slice(2);
    const downloadOnly = args.includes('--download-only') || args.includes('--download');

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
    
    // If --download-only flag is set, exit after downloading
    if (downloadOnly) {
      console.error(`JAR ready at: ${jar}`);
      process.exit(0);
    }

    // Check for Java (only needed if we're actually running)
    const javaCmd = findJavaExecutable();
    if (!javaCmd) {
      console.error('ERROR: Java is not installed or not in PATH.');
      console.error('Please install Java 21+ from https://adoptium.net/');
      process.exit(1);
    }

    // Run the JAR with MCP-specific arguments
    // Determine transport mode: stdio (default, local) or sse (Apify/HTTP)
    const transport = process.env.MCP_TRANSPORT || 'stdio';
    const profile = transport === 'sse' ? 'mcp-sse' : 'mcp-stdio';
    
    // Build Java arguments
    // CRITICAL: For MCP stdio, we must use 'inherit' for stdio to pass through JSON-RPC messages
    const javaArgs = [
      '-jar',
      jar,
      `--spring.profiles.active=${profile}`,
      `--spring.ai.mcp.server.transport=${transport}`
    ];
    
    // For stdio mode, disable web server and ensure banner is off
    if (transport === 'stdio') {
      javaArgs.push('--spring.main.web-application-type=none');
      javaArgs.push('--spring.main.banner-mode=off');
    }

    // Pass through any additional arguments (excluding our flags)
    const userArgs = process.argv.slice(2).filter(arg => 
      !arg.startsWith('--download-only') && !arg.startsWith('--download')
    );
    javaArgs.push(...userArgs);

    console.error(`Starting Jakarta Migration MCP Server...`);
    console.error(`Java: ${javaCmd}`);
    console.error(`JAR: ${jar}`);
    console.error(`Transport: ${transport}`);

    // CRITICAL: Use 'inherit' for stdio to ensure MCP JSON-RPC messages pass through correctly
    // stdin/stdout are used for MCP protocol communication
    // stderr is used for logging (configured in Spring Boot)
    const javaProcess = spawn(javaCmd, javaArgs, {
      stdio: ['inherit', 'inherit', 'inherit'],  // Explicit stdio passthrough for MCP
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

module.exports = { main, loadConfiguration };

