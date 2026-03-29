package com.finanzasia.infrastructure.security;

import com.finanzasia.domain.port.out.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges the domain {@link UserRepository} with Spring Security's
 * {@link UserDetailsService}. The loaded principal is used by
 * {@link JwtAuthFilter} to populate the security context.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.finanzasia.domain.model.User domainUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new User(domainUser.getEmail(), domainUser.getPasswordHash(), List.of());
    }
}
