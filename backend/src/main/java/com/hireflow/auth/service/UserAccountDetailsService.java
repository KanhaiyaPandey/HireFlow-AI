package com.hireflow.auth.service;

import java.util.Locale;

import com.hireflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads an account by email for Spring Security's {@code DaoAuthenticationProvider}, which then
 * checks the BCrypt hash. That provider also hashes a dummy password when the email is unknown,
 * so response time does not reveal which emails are registered.
 */
@Service
@RequiredArgsConstructor
public class UserAccountDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountStatusService accountStatusService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(user -> new UserAccountDetails(user, accountStatusService.canSignIn(user)))
                .orElseThrow(() -> new UsernameNotFoundException("No account for this email"));
    }
}
