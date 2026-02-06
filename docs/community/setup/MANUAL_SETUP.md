# Manual Setup Guide

**Purpose**: This guide is for **developers** who want to set up the development environment for working on the Jakarta Migration MCP Server project.

**For End Users**: If you just want to install and use the MCP server, see [INSTALLATION.md](INSTALLATION.md) instead.

Complete guide for manually setting up this Spring Boot template without using automated scripts.

## Prerequisites

Ensure you have:
- ✅ **Java 21+**
- ✅ **Docker & Docker Compose** (for PostgreSQL and Redis)
- ✅ **Gradle 8.5+** (or use Gradle wrapper)
- ✅ **Ollama** (optional, for LLM features)

## Step 1: Clone and Build

```bash
# Clone the repository (if not already done)
git clone <your-repo-url>
cd <project-name>

# Initialize Gradle wrapper (if needed)
gradle wrapper --gradle-version 8.5

# Build the project
./gradlew build
```

## Step 2: Start Services

```bash
# Start PostgreSQL and Redis
docker compose up -d

# Verify services are running
docker compose ps
```

## Step 3: Configure API Keys and Secrets

### Copy Environment File

```bash
# Copy example environment file
cp .env.example .env
```

### GitHub Personal Access Token (Optional but Recommended)

**Purpose**: Authenticated GitHub API requests for higher rate limits

**How to get**:
1. Go to GitHub: https://github.com/settings/tokens
2. Click **"Generate new token"** → **"Generate new token (classic)"**
3. Give it a descriptive name
4. Set expiration (recommend 90 days or custom)
5. Select scopes based on your needs:
   - ✅ `repo` - Full control of private repositories (if accessing private repos)
   - ✅ `read:org` - Read org and team membership (if accessing org repos)
   - ✅ `read:user` - Read user profile data
6. Click **"Generate token"**
7. **Copy the token immediately** - you won't be able to see it again!

**Add to `.env` file**:
```
GITHUB_API_TOKEN=ghp_your_token_here
```

### GitHub Webhook Secret (If using webhooks)

**Purpose**: Verify webhook signatures to ensure requests are from GitHub

**How to generate**:
```bash
# Using OpenSSL
openssl rand -hex 32

# Or using Python
python -c "import secrets; print(secrets.token_hex(32))"

# Or using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

**Add to `.env` file**:
```
GITHUB_WEBHOOK_SECRET=your-generated-secret-here
```

**Important**: Use the same secret when configuring webhooks in GitHub repository settings.

### Ollama Configuration (If using LLM features)

**Purpose**: Local LLM inference (no external API calls)

**Setup**:
1. Install Ollama: https://ollama.ai
2. Pull a model:
   ```bash
   ollama pull deepseek-coder:6.7b
   ```
3. Start Ollama (usually runs automatically):
   ```bash
   ollama serve
   ```

**Configuration** (in `application.yml`):
- Base URL: `http://localhost:11434` (default)
- Model: `deepseek-coder:6.7b` (default, configurable)

**Environment variables** (optional overrides):
```bash
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="deepseek-coder:6.7b"
```

Or add to `.env`:
```
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=deepseek-coder:6.7b
```

### Other External API Keys

Configure any other API keys your application needs:

**General Pattern**:
1. Sign up or log in to the service
2. Navigate to Settings → API Keys or Developer Settings
3. Generate a new API key or access token
4. Copy the key (keep it secure!)
5. Add to `.env` file

**Example**:
```
EXTERNAL_API_KEY=your-api-key-here
```

### Complete Environment Variables

Your `.env` file should look like:

```bash
# GitHub (if using GitHub API)
GITHUB_API_TOKEN=ghp_your_github_pat_here
GITHUB_WEBHOOK_SECRET=your-generated-webhook-secret-here

# External APIs (configure as needed)
EXTERNAL_API_KEY=your-api-key-here

# Ollama (if using LLM features)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=deepseek-coder:6.7b

# Database (if not using defaults)
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis (if not using defaults)
REDIS_HOST=localhost
REDIS_PORT=6379
```

## Step 4: Run the Application

**Using Gradle directly:**
```bash
# Windows
.\gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Step 5: Verify It's Working

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Check your application endpoints
curl http://localhost:8080/api/your-endpoint
```

## Database Migrations (Liquibase)

This project uses [Liquibase](https://www.liquibase.org/) to manage database schema changes.

### Overview

Liquibase tracks all database changes in XML changelog files, allowing you to:
- Version control your database schema
- Apply changes consistently across environments
- Rollback changes if needed
- Track who made changes and when

### Directory Structure

```
src/main/resources/
└── db/
    └── changelog/
        ├── db.changelog-master.xml    # Master changelog file
        └── changes/
            └── 001-initial-schema.xml # Individual change sets
```

### How It Works

1. **On Application Startup**: Spring Boot automatically runs Liquibase migrations
2. **Change Tracking**: Liquibase maintains a `databasechangelog` table to track applied changes
3. **Idempotent**: Changes are only applied once, even if you restart the application

### Creating New Migrations

#### Step 1: Create a New Changelog File

Create a new file in `src/main/resources/db/changelog/changes/` following the naming convention:
- `002-add-user-table.xml`
- `003-add-index.xml`
- etc.

#### Step 2: Add Your Changes

Example: Adding a new column

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <changeSet id="004-add-priority-column" author="your-name">
        <addColumn tableName="your_table">
            <column name="priority" type="INTEGER" defaultValue="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

#### Step 3: Include in Master Changelog

Add the new changelog file to `db.changelog-master.xml`:

```xml
<include file="db/changelog/changes/002-add-user-table.xml"/>
```

### Common ChangeSet Operations

**Add a Column:**
```xml
<addColumn tableName="your_table">
    <column name="new_column" type="VARCHAR(255)"/>
</addColumn>
```

**Create a Table:**
```xml
<createTable tableName="users">
    <column name="id" type="UUID">
        <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="username" type="VARCHAR(100)">
        <constraints nullable="false" unique="true"/>
    </column>
</createTable>
```

**Create an Index:**
```xml
<createIndex indexName="idx_username" tableName="users">
    <column name="username"/>
</createIndex>
```

**Modify a Column:**
```xml
<modifyDataType tableName="your_table" columnName="description" newDataType="TEXT"/>
```

**Drop a Column:**
```xml
<dropColumn tableName="your_table" columnName="old_column"/>
```

### Best Practices

1. **One ChangeSet Per Logical Change**: Each changeSet should represent a single, logical change
2. **Descriptive IDs**: Use clear, descriptive changeSet IDs
3. **Author Tracking**: Always include an author name for accountability
4. **Never Modify Applied Changes**: Once a changeSet has been applied, don't modify it. Create a new changeSet instead
5. **Test Locally First**: Always test migrations on a local database before deploying

### Running Migrations

**Automatic (Default)**: Migrations run automatically when the Spring Boot application starts.

**Manual (Using Gradle)**:
```bash
# Update database (applies pending changes)
./gradlew liquibaseUpdate

# Generate SQL without applying (dry-run)
./gradlew liquibaseUpdateSQL

# Rollback last change
./gradlew liquibaseRollback

# Check status
./gradlew liquibaseStatus
```

### Configuration

Liquibase is configured in `application.yml`:

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
    drop-first: false  # Never set to true in production!
```

## GitHub Webhooks (Optional)

If you want to receive real-time notifications from GitHub, you can set up webhooks.

### Prerequisites

1. **Public Endpoint**: Your application must be publicly accessible via HTTPS
2. **Webhook Secret**: Generate a secure secret (see Step 3 above)
3. **GitHub Repository Access**: Admin access to repositories you want to monitor

### Configure Application

The webhook secret is already configured in `application.yml`:

```yaml
app:
  webhooks:
    github:
      enabled: true
      secret: ${GITHUB_WEBHOOK_SECRET:}
      verify-signature: true
```

### Deploy Application

Your application must be publicly accessible. Options:

**Option A: Local Development with ngrok**
```bash
# Install ngrok: https://ngrok.com/download
ngrok http 8080

# Use the HTTPS URL provided by ngrok
# Example: https://abc123.ngrok.io
```

**Option B: Cloud Deployment**
Deploy to AWS, Azure, GCP, Heroku, etc. **Important**: GitHub requires HTTPS, so ensure SSL/TLS is configured.

### Configure GitHub Webhook

For each repository you want to monitor:

1. **Navigate to Repository Settings**
   - Go to your GitHub repository
   - Click **Settings** → **Webhooks**
   - Click **Add webhook**

2. **Configure Webhook**
   - **Payload URL**: `https://your-domain.com/api/webhooks/github/push`
   - **Content type**: `application/json`
   - **Secret**: Paste the secret you generated
   - **Which events**: Select "Just the push event" (or other events as needed)
   - **Active**: ✅ Checked

3. **Save Webhook**
   - Click **Add webhook**
   - GitHub will send a ping event to verify the endpoint

### Verify Setup

1. **Check Health Endpoint**
   ```bash
   curl https://your-domain.com/api/webhooks/github/health
   ```
   Should return: `GitHub webhook endpoint is active`

2. **Test with GitHub Ping**
   - After adding webhook, GitHub sends a ping event
   - Check application logs for "Received ping event"
   - Verify response is "Pong"

3. **Test Push Event**
   - Make a commit to the repository
   - Push to GitHub
   - Check application logs for push event processing

## Security Best Practices

1. **Never commit API keys to Git**
   - Add `.env` to `.gitignore`
   - Use `.env.example` for documentation

2. **Use environment variables**
   - Don't hardcode keys in source code
   - Use Spring's `${VARIABLE_NAME}` syntax

3. **Rotate keys regularly**
   - GitHub tokens: Every 90 days (or as configured)
   - Other keys: Follow provider recommendations

4. **Use least privilege**
   - Only grant necessary permissions
   - GitHub PAT: Only select needed scopes

5. **Monitor usage**
   - Check GitHub API rate limits
   - Monitor for unauthorized access

## Testing Your Configuration

### Test GitHub API Token

```bash
curl -H "Authorization: token $GITHUB_API_TOKEN" \
     https://api.github.com/user
```

### Test Ollama

```bash
curl http://localhost:11434/api/tags
```

### Test Other APIs

Test your external APIs according to their documentation.

## Troubleshooting

### Java Version Issues

If you have Java 17 but need Java 21:
- **Manual**: Install Java 21 from https://adoptium.net/
- Or use mise: See [Mise Setup Guide](MISE_SETUP.md)

### Docker Not Running

```bash
# Check Docker status
docker ps

# Start Docker Desktop (Windows/Mac)
# Or start Docker service (Linux)
sudo systemctl start docker
```

### Port Conflicts

If ports 5432 (PostgreSQL) or 6379 (Redis) are in use:
- Stop conflicting services
- Or modify `docker-compose.yml` to use different ports

### Gradle Wrapper Missing

```bash
# Initialize wrapper
gradle wrapper --gradle-version 8.5
```

### Application Won't Start

- **Check if services are running**: `docker compose ps`
- **Check if Ollama is running**: `curl http://localhost:11434/api/tags`
- **Check logs**: Look for errors in console output or `./logs/application.log`
- **Verify database connection**: Ensure PostgreSQL is accessible on port 5432
- **Verify Redis connection**: Ensure Redis is accessible on port 6379

### GitHub API Rate Limit Exceeded

**Problem**: Getting 403 errors with "API rate limit exceeded"

**Solution**:
1. Add `GITHUB_API_TOKEN` environment variable
2. Wait for rate limit reset (check headers: `X-RateLimit-Reset`)
3. Reduce polling frequency in `application.yml`

### Ollama Connection Failed

**Problem**: Cannot connect to Ollama at `http://localhost:11434`

**Solution**:
1. Ensure Ollama is running: `ollama serve`
2. Check if port 11434 is accessible
3. Verify `OLLAMA_BASE_URL` environment variable

### Database Connection Issues

- **Check PostgreSQL is running**: `docker compose ps`
- **Verify credentials**: Check `.env` file for `DB_USERNAME` and `DB_PASSWORD`
- **Check logs**: Look for connection errors in application logs
- **Reset database** (if needed): `docker compose down -v && docker compose up -d`

### Migration Fails on Startup

1. Check the application logs for the specific error
2. Verify your changelog XML syntax is correct
3. Ensure the database connection is working
4. Check if there are conflicting changes

### Need to Reset Database

**⚠️ WARNING: This will delete all data!**

```sql
-- Drop all tables
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

Then restart the application to reapply all migrations.

## Resources

- **Liquibase Documentation**: https://docs.liquibase.com/
- **Liquibase Change Types**: https://docs.liquibase.com/change-types/home.html
- **Spring Boot Liquibase Integration**: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase
- **GitHub API**: https://docs.github.com/en/rest
- **Ollama**: https://ollama.ai/docs

