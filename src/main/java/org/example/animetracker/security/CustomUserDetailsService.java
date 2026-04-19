package org.example.animetracker.security;

import lombok.AllArgsConstructor;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByName(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return new org.springframework.security.core.userdetails.User(
        user.getName(),
        user.getPassword(),
        Collections.emptyList() // Здесь можно передать роли, если они появятся
    );
  }
}