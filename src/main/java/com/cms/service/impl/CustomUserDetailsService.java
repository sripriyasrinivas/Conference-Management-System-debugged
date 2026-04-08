package com.cms.service.impl;

import com.cms.model.User;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Fetch user from database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // 2. Create the Authority string (e.g., "ROLE_AUTHOR" or "ROLE_ADMIN")
        // Spring Security requires the "ROLE_" prefix for hasRole() expressions to work
        String roleWithPrefix = "ROLE_" + user.getRole().name();
        
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleWithPrefix);

        // 3. Return the Spring Security UserDetails object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),         // account enabled flag
                true,                     // accountNonExpired
                true,                     // credentialsNonExpired
                true,                     // accountNonLocked
                Collections.singletonList(authority)
        );
    }
}