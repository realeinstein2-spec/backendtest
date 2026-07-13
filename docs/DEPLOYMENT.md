# MakersHub Deployment Guide

## Local Docker

```bash
cd makershub-backend
cp .env.example .env
# Edit secrets
docker-compose up --build
```

## Railway

1. Install Railway CLI and login.
2. Create project and PostgreSQL database.
3. Add environment variables from `.env.example`.
4. Deploy:

```bash
railway login
railway link
railway up
```

## CI/CD

GitHub Actions workflow at `.github/workflows/ci-cd.yml`:
- Builds and tests on every push/PR.
- Deploys to Railway on merges to `main`.

Set `RAILWAY_TOKEN` in GitHub repository secrets.

## Database Backups

### Railway managed PostgreSQL
Enable automated daily backups in the Railway dashboard.

### Manual pg_dump

```bash
pg_dump -Fc -h <host> -U postgres makershub > makershub_$(date +%F).dump
```

## Production Checklist

- [ ] Strong `JWT_SECRET` (Base64 256-bit)
- [ ] Real Paystack secret key
- [ ] Cloudinary credentials
- [ ] Firebase service account / Africa's Talking credentials
- [ ] CORS origins restricted to frontend domains
- [ ] Database connection pooling tuned
- [ ] SSL/TLS enabled
- [ ] Health checks enabled
- [ ] Logging aggregated
- [ ] Automated backups configured
- [ ] Rate limiting enabled (future)
