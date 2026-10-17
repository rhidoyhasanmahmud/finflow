package com.finflow.user;

import com.finflow.common.cache.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserCachingIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        Cache cache = cacheManager.getCache(CacheConfig.USERS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void shouldPopulateRefreshAndEvictUserCache() {
        UserResponse created = userService.create(
                new CreateUserRequest("Hasan", "hasan@example.com")
        );

        UserResponse firstRead = userService.getById(created.id());

        Cache usersCache = cacheManager.getCache(CacheConfig.USERS_CACHE);
        assertThat(usersCache).isNotNull();
        assertThat(usersCache.get(created.id(), UserResponse.class)).isEqualTo(firstRead);

        UserResponse updated = userService.update(
                created.id(),
                new UpdateUserRequest("Hasan Mahmud", "hasan@example.com")
        );

        assertThat(usersCache.get(created.id(), UserResponse.class)).isEqualTo(updated);

        userService.delete(created.id());

        assertThat(usersCache.get(created.id())).isNull();
    }
}
