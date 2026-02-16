package org.example.animetracker.repository;

import java.util.HashMap;
import java.util.Map;
import org.example.animetracker.model.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final Map<Long, User> userMap = new HashMap<>();
  private long nextId = 1;

  public User save(User user) {
    if (user.getId() == null) {
      user.setId(nextId++);
    }
    userMap.put(user.getId(), user);
    return user;
  }

  public User findById(Long id) {
    return userMap.get(id);
  }

  public Boolean containsUser(Long id) {
    return userMap.containsKey(id);
  }

  public void deleteUserById(Long userId) {
    userMap.remove(userId);
  }
}