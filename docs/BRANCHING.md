# Git Branching Strategy

Sized for a small team (2–4 people) working three tracks in parallel: backend, machine learning, and frontend. The goal is that `main` is always demoable — if your guide asks for a demo with no warning, whatever is on `main` should run.

If you are working solo, keep `main` + `feature/*` and skip `develop`. Everything else below still applies.

---

## Branches

| Branch | Purpose | Who commits | Protected |
|---|---|---|---|
| `main` | Always-demoable. Only receives merges from `develop` at milestone boundaries. | Nobody directly | Yes |
| `develop` | Integration branch. All feature work lands here first. | Via pull request only | Yes |
| `feature/*` | One branch per task. Short-lived. | Owner of the task | No |
| `fix/*` | Bug fixes that are not part of a planned feature. | Anyone | No |
| `hotfix/*` | Urgent fix branched from `main`, merged to both `main` and `develop`. | Anyone | No |

**Never commit directly to `main` or `develop`.** Configure this on GitHub under Settings → Branches → Add branch protection rule: require a pull request before merging, and require at least one approval.

---

## Naming

```text
feature/<milestone>-<short-slug>
fix/<short-slug>
hotfix/<short-slug>
```

Prefixing with the milestone number makes it obvious at a glance who is blocking whom.

Examples:

```text
feature/m2-jwt-auth
feature/m2-transaction-crud
feature/m3-budget-engine
feature/m4-fraud-model-training
feature/m6-dashboard-charts
fix/health-endpoint-null-profile
```

Use lowercase and hyphens. No spaces, no personal names, no `feature/devansh-stuff`.

---

## Daily workflow

```bash
# 1. Start from an up-to-date develop
git checkout develop
git pull origin develop

# 2. Branch
git checkout -b feature/m2-jwt-auth

# 3. Work, committing in small logical units
git add .
git commit -m "feat(auth): add JwtService for token signing and validation"

# 4. Keep up with develop while you work (daily, if the branch lives that long)
git fetch origin
git rebase origin/develop

# 5. Push and open a pull request into develop
git push -u origin feature/m2-jwt-auth
```

Rebase rather than merge while a feature branch is still private — it keeps history linear and makes the pull request diff show only your work. Once you have pushed and someone else may have pulled your branch, stop rebasing and use merge instead.

---

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<scope>): <imperative summary>
```

| Type | Use for |
|---|---|
| `feat` | New functionality |
| `fix` | Bug fix |
| `refactor` | Restructuring with no behaviour change |
| `test` | Adding or fixing tests |
| `docs` | Documentation only |
| `chore` | Build, dependencies, configuration, tooling |
| `perf` | Performance work |

Good:

```text
feat(transaction): add POST /api/transactions with validation
fix(budget): correct disposable income when salary is null
test(budget): cover zero-expense and negative-balance cases
chore(docker): pin postgres to 18-alpine
docs(readme): document DB_PORT conflict workaround
```

Avoid:

```text
update
final code
asdf
fixed bug
work
```

The reason this matters beyond tidiness: at Milestone 8 you need a project report. A clean commit log generates most of your "work done" section for free, and it is the easiest evidence of consistent effort across twelve weeks.

---

## Pull requests

Keep them small — one task, ideally under ~400 changed lines. A large pull request does not get reviewed, it gets rubber-stamped.

Include in the description:

1. Which milestone task this closes.
2. What changed, in two or three sentences.
3. How the reviewer can verify it — the exact `curl` command or UI steps.
4. Anything intentionally left out.

Template:

```markdown
## Milestone
M2 — JWT-based register/login

## What changed
Adds `JwtService`, `JwtAuthenticationFilter`, and `SecurityConfig`.
`/api/auth/**` is public; everything else under `/api/**` now requires a Bearer token.

## How to verify
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"Passw0rd!"}'
curl -X POST localhost:8080/api/auth/login ...   # returns a token
curl localhost:8080/api/transactions -H "Authorization: Bearer <token>"

## Not included
Refresh-token rotation — tracked separately.
```

### Review checklist

- [ ] Branch is up to date with `develop`
- [ ] `./mvnw clean verify` passes
- [ ] No secrets, no `.env`, no dataset CSVs, no `target/` in the diff
- [ ] New endpoints return `ApiResponse` / `ApiError`, not bare strings
- [ ] Entities do not appear in controller signatures
- [ ] Money is `BigDecimal`, not `double`
- [ ] New packages or classes match the layering in the `package-info.java` docs

---

## Merging into `main`

Only at a milestone boundary, and only when the milestone's deliverable actually works end to end.

```bash
git checkout main
git pull origin main
git merge --no-ff develop
git tag -a v0.1.0-m1 -m "Milestone 1: environment and foundation"
git push origin main --tags
```

Tag every milestone. Tags give you a working build to fall back to when a demo breaks the day before a review, and they document your timeline better than any spreadsheet.

Use `--no-ff` so the merge commit records that a milestone landed rather than flattening it into the history.

---

## Avoiding conflicts across the three tracks

Most conflicts on a project like this come from two people editing the same file, not from complex logic. Cheap habits that prevent them:

- **Divide by package, not by file.** One person owns `service/BudgetService`, another owns `service/FraudCheckService`. Both can work all day without touching the same file.
- **Announce shared-file edits.** `pom.xml`, `application.properties`, and `docker-compose.yml` are the usual flashpoints. Say so in the group chat before editing.
- **Agree entity shapes before writing code.** A change to `Transaction` ripples through repositories, services, DTOs, and the frontend. Milestone 2 should settle the schema first, on paper.
- **Merge into `develop` often.** A branch that lives a week is a conflict waiting to happen. Aim to land work within one or two days.
- **Never commit `target/`, `node_modules/`, `.env`, or datasets.** `.gitignore` covers these — do not use `git add -f` to override it.

---

## Recovering from common mistakes

**Committed to `main` by accident, not yet pushed**

```bash
git branch feature/m2-my-work    # save the work
git reset --hard origin/main     # rewind main
git checkout feature/m2-my-work
```

**Committed a secret**

Rotate the credential first — assume it is compromised the moment it is pushed. Removing it from history does not un-leak it. Then remove the file, add it to `.gitignore`, and commit.

**Need to undo a merge that is already on `develop`**

```bash
git revert -m 1 <merge-commit-sha>
```

`revert` adds a new commit rather than rewriting shared history, which is what you want on a branch other people have pulled.
