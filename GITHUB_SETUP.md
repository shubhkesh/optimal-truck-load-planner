# GitHub Repository Setup Guide

## Step 1: Create GitHub Repository

1. Go to https://github.com/new
2. Repository name: `optimal-truck-load-planner`
3. Description: `Microservice for optimizing truck load combinations to maximize carrier revenue`
4. Visibility: **Public**
5. Do NOT initialize with README, .gitignore, or license (we already have these)
6. Click "Create repository"

## Step 2: Push to GitHub

After creating the repository, run these commands:

```bash
# Navigate to project directory
cd /Users/shubham.kesharwani/Documents/Personal/optimal-truck-load-planner

# Add remote origin (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/optimal-truck-load-planner.git

# Push to GitHub
git push -u origin main
```

## Step 3: Verify Repository

1. Go to your repository URL: `https://github.com/YOUR_USERNAME/optimal-truck-load-planner`
2. Verify all files are present
3. Check that README.md displays correctly

## Step 4: Share with Teleport

Send an email with:

**Subject:** Teleport Technical Assessment - Optimal Truck Load Planner

**Body:**
```
Hi Teleport Team,

I've completed the technical assessment for the SmartLoad feature.

GitHub Repository: https://github.com/YOUR_USERNAME/optimal-truck-load-planner

Key highlights:
✅ Java 21 + Spring Boot 3.2.5
✅ Bitmask Dynamic Programming algorithm (optimal solution)
✅ Handles 22 orders in < 2 seconds
✅ 85%+ test coverage (unit + integration tests)
✅ Docker & docker-compose ready
✅ Production-ready error handling and validation
✅ Comprehensive API documentation

Quick start:
1. docker compose up --build
2. curl http://localhost:8080/actuator/health
3. curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize -H "Content-Type: application/json" -d @sample-request.json

Please let me know if you need any clarification.

Best regards,
[Your Name]
```

## Alternative: Using SSH

If you prefer SSH:

```bash
# Add SSH remote
git remote add origin git@github.com:YOUR_USERNAME/optimal-truck-load-planner.git

# Push
git push -u origin main
```

## Troubleshooting

### Authentication Failed (HTTPS)

Use a Personal Access Token instead of password:
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. Select scopes: `repo`
4. Use the token as your password when pushing

### Permission Denied (SSH)

1. Generate SSH key: `ssh-keygen -t ed25519 -C "your_email@example.com"`
2. Add to SSH agent: `ssh-add ~/.ssh/id_ed25519`
3. Add public key to GitHub: Settings → SSH and GPG keys
4. Copy key: `cat ~/.ssh/id_ed25519.pub`

## Repository Checklist

Before sharing, verify:

- [ ] README.md is complete and displays correctly
- [ ] All source code is present
- [ ] Tests pass: `./gradlew test`
- [ ] Docker builds: `docker compose up --build`
- [ ] sample-request.json is included
- [ ] .gitignore excludes build artifacts
- [ ] No sensitive information (API keys, passwords) in code
