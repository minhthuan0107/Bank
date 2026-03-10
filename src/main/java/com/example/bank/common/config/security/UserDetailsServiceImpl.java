package com.example.bank.common.config.security;
import com.example.bank.entity.user.User;
import com.example.bank.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private  final UserRepository userRepository;

    /**
     * Login bằng USERNAME
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with username: " + username
                        )
                );

        return UserDetailsImpl.from(user);
    }

    /**
     * Dùng khi validate JWT (subject = userId)
     */
    public UserDetails loadUserById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        return UserDetailsImpl.from(user);
    }
}

