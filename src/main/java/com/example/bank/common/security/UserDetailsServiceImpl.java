package com.example.bank.common.security;

import com.example.angelproject.modules.user.domain.entity.User;
import com.example.angelproject.modules.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/*
    Cung cấp UserDetails cho Spring Security khi đăng nhập (password login) hoặc xác thực JWT
 */
public class UserDetailsServiceImpl implements UserDetailsService {
    private  final UserRepository userRepository;

    /**
     * Dùng khi đăng nhập bằng email / phone (password login)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrPhone(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return UserDetailsImpl.from(user);
    }

    /**
     * Dùng cho JWT (xác thực bằng userId chứ không dùng username)
     */
    public UserDetails loadUserById(Long userId) {
        return userRepository.findById(userId)
                .map(UserDetailsImpl::fromForJwt)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + userId)
                );
    }
}
