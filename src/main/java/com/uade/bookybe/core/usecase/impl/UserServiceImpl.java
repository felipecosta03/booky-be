package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean followUser(String followerId, String followedId) {
        if (userRepository.isFollowing(followerId, followedId)) {
            return false;
        }
        userRepository.follow(followerId, followedId);
        return true;
    }
}

