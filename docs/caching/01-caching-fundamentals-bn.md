# FinFlow Caching Fundamentals — সহজ বাংলা ব্যাখ্যা

এই ধাপে আমরা FinFlow-এ **Caffeine local cache** ব্যবহার করছি। উদ্দেশ্য শুধু `@Cacheable` ব্যবহার করা না; cache কেন দরকার, কখন কাজ করে, কখন stale data তৈরি করে এবং Spring ভিতরে কীভাবে method call intercept করে—এগুলো বোঝা।

## 1. Cache কী?

Cache হলো একটি **দ্রুত temporary storage layer**। কোনো data বারবার database থেকে আনার বদলে একবার database থেকে এনে cache-এ রাখি। পরে একই data লাগলে cache থেকেই ফেরত দিই।

Cache ছাড়া:

```text
Request -> Service -> Database -> Response
```

Cache থাকলে:

```text
Request -> Service -> Cache
                     | hit  -> Response
                     | miss -> Database -> Cache -> Response
```

Database সাধারণত memory cache-এর চেয়ে ধীর। তাই read-heavy data-তে cache latency এবং database load কমাতে পারে।

---

## 2. এই project-এ কোন caching pattern ব্যবহার করছি?

এই implementation মূলত **Cache-Aside** pattern-এর Spring abstraction।

`GET /api/v1/users/{id}` call হলে Spring প্রথমে cache check করে।

- cache-এ user থাকলে service method execute না করেই cached value return করে।
- cache-এ user না থাকলে service method execute হয়, database থেকে user আসে, তারপর result cache-এ রাখা হয়।

এখানে গুরুত্বপূর্ণ বিষয়: এটি pure read-through cache product-এর মতো নয় যেখানে cache server নিজে database fetch করে। এখানে application/Spring cache abstraction flow control করছে।

---

## 3. Cache Hit এবং Cache Miss

### Cache Miss

প্রথমবার:

```text
GET /api/v1/users/1
        |
        v
users cache-এ 1 নেই
        |
        v
UserService.getById() execute
        |
        v
PostgreSQL query
        |
        v
result cache-এ save
```

Log-এ দেখবে:

```text
Cache miss for userId=1; loading from database
```

### Cache Hit

একই request আবার করলে:

```text
GET /api/v1/users/1
        |
        v
users cache-এ 1 আছে
        |
        v
cached UserResponse return
```

এবার `UserService.getById()`-এর body execute হবে না। তাই database-fetch log-ও আসবে না।

---

## 4. Cache Hit Ratio

Formula:

```text
hit ratio = hits / (hits + misses)
```

ধরা যাক 100 lookup-এর মধ্যে:

- 80 cache hit
- 20 cache miss

তাহলে:

```text
80 / 100 = 0.80 = 80%
```

Hit ratio বেশি হলে সাধারণত database call কমে। কিন্তু শুধু hit ratio বেশি হলেই cache ভালো—এটা ঠিক না। যদি cache stale বা ভুল data দেয়, 99% hit ratio-ও খারাপ design হতে পারে।

---

## 5. Eviction কী?

Cache-এর memory সীমিত। তাই পুরোনো বা অপ্রয়োজনীয় entry সরাতে হয়। এটাকে eviction বলে।

এই project-এ Caffeine configuration:

```java
.maximumSize(1_000)
.expireAfterWrite(Duration.ofMinutes(10))
```

মানে:

- সর্বোচ্চ প্রায় 1000 user entry রাখা হবে।
- কোনো entry write হওয়ার 10 মিনিট পরে expire হবে।

Common policy concept:

- **LRU**: অনেকক্ষণ ব্যবহার হয়নি এমন data আগে বাদ।
- **LFU**: খুব কম ব্যবহার হয় এমন data আগে বাদ।
- **TTL**: নির্দিষ্ট সময় পরে data expire।

Caffeine internally advanced eviction strategy ব্যবহার করে; আমাদের প্রথম লক্ষ্য policy implementation না, বরং capacity এবং expiry কেন দরকার সেটা বোঝা।

---

## 6. Cache consistency: update হলে কী হবে?

ধরা যাক cache-এ আছে:

```text
1 -> Hasan
```

Database-এ user update হলো:

```text
1 -> Hasan Mahmud
```

Cache update না করলে client এখনো `Hasan` পেতে পারে। এটাকে stale data problem বলা হয়।

এই project-এ update method-এ:

```java
@CachePut(cacheNames = "users", key = "#userId")
```

ব্যবহার করা হয়েছে। `@CachePut` method execute করে এবং returned fresh value cache-এ replace করে।

Delete-এর সময়:

```java
@CacheEvict(cacheNames = "users", key = "#userId")
```

ব্যবহার করা হয়েছে। ফলে deleted user cache-এ পড়ে থাকে না।

---

## 7. `@Cacheable`, `@CachePut`, `@CacheEvict` পার্থক্য

### `@Cacheable`

Read-এর জন্য।

```java
@Cacheable(cacheNames = "users", key = "#userId")
public UserResponse getById(Long userId)
```

Cache hit হলে method body execute নাও হতে পারে।

### `@CachePut`

Method সবসময় execute হয় এবং returned value cache-এ লেখা হয়। Update operation-এর জন্য useful।

### `@CacheEvict`

Cache entry delete করে। Delete operation বা এমন update যেখানে fresh value cache-এ রাখতে চাই না—সেখানে useful।

---

## 8. Cache key কেন গুরুত্বপূর্ণ?

আমাদের key:

```java
key = "#userId"
```

তাই cache দেখতে conceptually এমন:

```text
users
  1 -> UserResponse(...)
  2 -> UserResponse(...)
  3 -> UserResponse(...)
```

Key ভুল হলে wrong data return, duplicate cache entry বা unnecessary miss হতে পারে।

Real system-এ composite key লাগতে পারে:

```text
wallet-summary:{walletId}:{currency}
```

---

## 9. `sync = true` কেন দিয়েছি?

```java
@Cacheable(..., sync = true)
```

ধরা যাক cache empty এবং একই user-এর জন্য একই সময়ে 100 request এলো।

`sync = true` না থাকলে অনেক request একসাথে database hit করতে পারে। এটাকে **cache stampede**-এর ছোট version হিসেবে ভাবতে পারো।

`sync = true` supported cache provider-এর ক্ষেত্রে একই key load করার concurrent কাজ coordinate করতে সাহায্য করে।

এটা distributed Redis lock না; local cache-এর context-এ ব্যবহার করছি।

---

## 10. Spring ভিতরে কীভাবে কাজ করছে?

আমরা যখন লিখি:

```java
@Cacheable
```

annotation নিজে database/cache call করে না। Spring bean-এর চারপাশে interception/proxy infrastructure বসায়। Simplified flow:

```text
Controller
   |
   v
Spring Proxy / Cache Interceptor
   |
   +--> cache hit?  -> cached result
   |
   +--> cache miss? -> actual UserService method
                           |
                           v
                        Repository
                           |
                           v
                       PostgreSQL
```

এই কারণেই cache hit হলে method body execute না-ও হতে পারে।

---

## 11. Self-invocation trap

এটা খুব গুরুত্বপূর্ণ Spring concept।

ধরা যাক একই class-এর ভেতরে:

```java
public void methodA() {
    getById(1L);
}

@Cacheable("users")
public UserResponse getById(Long id) {
    ...
}
```

`methodA()` থেকে `this.getById()` ধরনের internal call হলে call proxy-এর বাইরে দিয়ে যেতে পারে। তখন caching advice apply নাও হতে পারে।

সহজ rule:

> Spring-এর proxy-based annotation (`@Cacheable`, `@Transactional`, `@Async`) বুঝতে হলে bean-to-bean call আর same-object internal call-এর পার্থক্য বুঝতে হবে।

---

## 12. Cache Granularity

কী cache করছি সেটাও design decision।

আমরা এখন full JPA entity cache করছি না। আমরা `UserResponse` DTO cache করছি। এটা intentional।

কারণ JPA entity persistence context-এর lifecycle-এর সাথে যুক্ত হতে পারে। API/read model cache করা সাধারণত শেখার জন্য cleaner boundary।

পরে FinFlow-এ cache candidate হতে পারে:

- user profile
- wallet summary
- fee configuration
- exchange rate
- reference data

কিন্তু mutable financial ledger/balance blindly cache করা উচিত না। সেখানে consistency requirement আগে বুঝতে হবে।

---

## 13. Cache Coherence

এখন Caffeine local cache। যদি FinFlow-এর 3টি application instance থাকে:

```text
Instance A -> নিজের cache
Instance B -> নিজের cache
Instance C -> নিজের cache
```

A-তে update হলে B এবং C সাথে সাথে জানবে না। এটিই distributed cache coherence problem-এর শুরু।

এই lab শেষ হলে Redis phase-এ আমরা দেখব shared distributed cache কী solve করে, আর কী নতুন সমস্যা আনে।

---

## 14. Cache Warm-up

Application restart করলে local cache empty। প্রথম request-গুলো database hit করবে।

High-traffic system-এ কিছু popular/reference data startup-এর সময় preload করা যায়। এটাকে cache warm-up বলা হয়।

এই phase-এ আমরা warm-up করছি না। আগে miss -> load -> hit flow পরিষ্কার বোঝা দরকার।

---

## 15. Cache Partitioning

সব data একই cache namespace-এ রাখা ভালো না। ভবিষ্যতে FinFlow-এ হতে পারে:

```text
users
walletSummaries
exchangeRates
feeConfigurations
```

প্রতিটির TTL/capacity আলাদা হতে পারে। এটাকে logical partitioning হিসেবে ভাবতে পারো।

---

## 16. Compression

Redis-এর মতো remote cache-এ বড় payload থাকলে compression memory/network কমাতে পারে। কিন্তু compression/decompression CPU লাগে।

এই phase-এ compression দরকার নেই। Optimization করার আগে measurement দরকার।

---

# Hands-on: কীভাবে test করবে

## 1. PostgreSQL চালাও

```bash
docker compose up -d
```

## 2. Application চালাও

```bash
mvn spring-boot:run
```

## 3. User create করো

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Hasan","email":"hasan@example.com"}'
```

ধরা যাক response-এর id `1`।

## 4. প্রথম GET — cache miss

```bash
curl http://localhost:8080/api/v1/users/1
```

Application log-এ database-loading message দেখবে।

## 5. একই GET আবার — cache hit

```bash
curl http://localhost:8080/api/v1/users/1
```

এবার service method-এর cache-miss log আসার কথা না।

## 6. User update

```bash
curl -X PUT http://localhost:8080/api/v1/users/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Hasan Mahmud","email":"hasan@example.com"}'
```

`@CachePut` fresh response cache-এ রাখবে।

## 7. আবার GET

```bash
curl http://localhost:8080/api/v1/users/1
```

Fresh updated data cache থেকে পাওয়া উচিত।

## 8. Delete

```bash
curl -i -X DELETE http://localhost:8080/api/v1/users/1
```

`@CacheEvict` cache entry remove করবে।

---

# এই phase শেষে কী বুঝতে হবে?

নিজে explain করতে পারলে phase complete:

1. Cache hit এবং miss কী?
2. Cache-aside flow কীভাবে চলে?
3. `@Cacheable` hit হলে method কেন execute হয় না?
4. Update-এর পরে stale cache কীভাবে হয়?
5. `@CachePut` আর `@CacheEvict` কখন ব্যবহার করব?
6. Cache key কেন গুরুত্বপূর্ণ?
7. TTL এবং max size কেন দরকার?
8. Local Caffeine cache multiple instance-এ কেন coherence problem তৈরি করে?
9. Spring proxy/self-invocation caching-কে কীভাবে affect করে?
10. High hit ratio কেন একাই success metric নয়?

পরের cache phase হবে **metrics + hit/miss observation**, তারপর **Redis distributed cache**।
