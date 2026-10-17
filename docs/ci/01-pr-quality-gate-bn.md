# FinFlow PR Quality Gate — সহজ বাংলা ব্যাখ্যা

এই workflow-এর উদ্দেশ্য খুব simple:

> `main` branch-এ কোনো code merge করার আগে code build হচ্ছে কি না, test pass করছে কি না, এবং obvious security problem আছে কি না—এগুলো automatic check করা।

Workflow file:

```text
.github/workflows/pr-quality-gate.yml
```

---

## 1. কখন workflow চলবে?

দুই সময়:

```text
1. main branch-এর দিকে Pull Request খুললে / update করলে
2. main branch-এ code push/merge হলে
```

PR flow:

```text
Developer change
      ↓
Pull Request
      ↓
GitHub Actions
      ├── Build & Test
      ├── Dependency Security Review
      └── CodeQL Security Analysis
      ↓
সব pass?
      ├── Yes → merge করা safe-er
      └── No  → আগে problem fix করতে হবে
```

---

# 2. Build & Test job

এই command run হয়:

```bash
mvn --batch-mode --no-transfer-progress clean verify
```

এখানে Maven কী করে?

```text
clean
  ↓
আগের build output remove করে

compile
  ↓
main source code compile করে

test
  ↓
unit test run করে

package
  ↓
application package করে

verify
  ↓
integration-test সহ Maven lifecycle-এর verification পর্যন্ত যায়
```

অর্থাৎ code compile না হলে, test fail হলে, বা verification fail হলে PR check red হবে।

### কেন শুধু `mvn test` না?

`mvn test` শুধু test phase পর্যন্ত যায়।

`mvn verify` একটু stronger gate, কারণ project package/verification phase পর্যন্ত যায়। ভবিষ্যতে integration test, coverage rule, static analysis plugin ইত্যাদি Maven lifecycle-এ যোগ করলে একই CI command এগুলোও চালাতে পারবে।

---

# 3. Dependency Security Review

Spring Boot project-এ আমরা অনেক third-party dependency use করি:

```text
Spring
Hibernate
PostgreSQL driver
Caffeine
Flyway
...
```

আমাদের নিজের code secure হলেও dependency-এর কোনো version-এ known vulnerability থাকতে পারে।

এই job PR-এ dependency change হলে সেটা review করে।

বর্তমান rule:

```text
High বা Critical severity vulnerability introduce হলে check fail করবে।
```

সহজ example:

```text
PR-এ dependency X যোগ করলাম
          ↓
X version-এ known high severity CVE আছে
          ↓
Dependency Security Review = FAIL
          ↓
merge করার আগে dependency upgrade/change করতে হবে
```

এটা SCA — Software Composition Analysis-এর একটি basic form।

---

# 4. CodeQL Security Analysis

Dependency review third-party library দেখে।

CodeQL আমাদের source code দেখে।

এটাকে সহজভাবে static security analysis বা SAST ভাবতে পারো।

CodeQL source code build/analyze করে potentially dangerous pattern খোঁজে। যেমন category হিসেবে:

```text
Injection-type issue
Unsafe data flow
Path-related issue
Authentication/authorization mistake-এর কিছু pattern
Other security-relevant code pattern
```

Important:

> CodeQL pass মানে application 100% secure—এটা না।

এটা security-এর একটি layer মাত্র।

---

# 5. Unit Test আর Security Test কি একই জিনিস?

না।

আমাদের pipeline-এ তিন ধরনের concern আছে:

```text
Functional correctness
    → Maven tests

Dependency security
    → Dependency Review

Source-code security
    → CodeQL
```

ভবিষ্যতে যখন Spring Security যোগ করব, তখন আমাদের নিজের security-focused automated tests-ও `mvn verify`-এর অংশ হবে। যেমন:

```text
Unauthenticated user protected API access করতে পারে কি না
USER role ADMIN endpoint access করতে পারে কি না
Invalid JWT reject হচ্ছে কি না
CSRF/CORS behaviour expected কি না
```

এই tests normal JUnit/Spring Security tests হিসেবেই CI-তে run হবে।

---

# 6. Java setup কেন আছে?

Runner নতুন machine-এর মতো। তাই workflow Java 21 setup করে:

```text
Temurin JDK 21
```

FinFlow-ও Java 21 use করে, তাই local এবং CI environment একই major Java version ব্যবহার করছে।

---

# 7. Maven cache কেন use করছি?

প্রতি run-এ Maven dependencies internet থেকে আবার download করলে CI slow হবে।

```yaml
cache: maven
```

GitHub Maven dependency cache reuse করতে পারে। ফলে subsequent build দ্রুত হয়।

---

# 8. Concurrency / cancel-in-progress কী?

ধরো PR-এ তুমি খুব দ্রুত ৩টা commit push করলে:

```text
Commit A → CI শুরু
Commit B → CI শুরু
Commit C → CI শুরু
```

পুরনো A/B-এর result আর খুব useful না, কারণ latest code C।

তাই:

```text
cancel-in-progress: true
```

latest run রেখে পুরনো unnecessary run cancel করতে পারে।

এতে CI resource এবং সময় save হয়।

---

# 9. PR merge করার ideal rule

Long-term target:

```text
main branch protected থাকবে

Required checks:
- Build & Test
- Dependency Security Review
- CodeQL Security Analysis
```

তারপর flow হবে:

```text
PR opened
   ↓
checks running
   ↓
একটা check fail
   ↓
Merge blocked
   ↓
Fix + push
   ↓
checks আবার run
   ↓
সব green
   ↓
Review
   ↓
Merge
```

Note: workflow add করলেই branch protection automatically enable হয় না। GitHub repository settings/ruleset দিয়ে required checks enforce করতে হয়।

---

# 10. এই pipeline কী guarantee করে না?

এই CI useful, কিন্তু full security system না। এটা guarantee করে না যে:

```text
সব business bug ধরা পড়বে
সব vulnerability ধরা পড়বে
runtime configuration secure
production secrets safe
API authorization logically correct
performance ভালো
```

এগুলো জন্য additional layers লাগবে।

---

# 11. FinFlow-এ ভবিষ্যতে কী যোগ করব?

Project mature হলে quality gate ধীরে ধীরে expand করা যাবে:

```text
Phase 1 — এখন
Build
Unit/Integration tests
Dependency review
CodeQL

Phase 2
Test coverage threshold
Checkstyle / Spotless
SpotBugs / static quality checks

Phase 3
Container image scanning
OWASP dependency scanning / SBOM
Secret scanning strategy

Phase 4
API/integration security tests
Performance smoke tests
Deployment environment checks
```

সবকিছু প্রথম দিন যোগ করা ঠিক না। CI খুব slow/flaky হলে developer checks ignore করতে শুরু করে। তাই FinFlow-এ আমরা ছোট কিন্তু meaningful quality gate দিয়ে শুরু করছি।

---

# 12. মনে রাখার সহজ model

```text
BUILD
"Code compile/package হয়?"

TEST
"Expected behaviour ঠিক আছে?"

DEPENDENCY SECURITY
"যে library আনছি সেটা known dangerous version কি না?"

CODE SECURITY
"আমাদের code-এ known insecure pattern আছে কি না?"
```

এই চারটা green হওয়া মানে PR merge করার confidence অনেক বেড়ে যায়—কিন্তু human review এখনও প্রয়োজন।
