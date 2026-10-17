package com.finflow.user;

import com.finflow.common.cache.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        User saved = userRepository.save(new User(request.name(), request.email()));
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.USERS_CACHE, key = "#userId", sync = true)
    public UserResponse getById(Long userId) {
        log.info("Cache miss for userId={}; loading from database", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserResponse.from(user);
    }

    @Transactional
    @CachePut(cacheNames = CacheConfig.USERS_CACHE, key = "#userId")
    public UserResponse update(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.update(request.name(), request.email());
        return UserResponse.from(user);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.USERS_CACHE, key = "#userId")
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(user);
    }
}
