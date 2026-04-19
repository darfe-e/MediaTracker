package org.example.animetracker.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.animetracker.dto.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Users", description = "Управление пользователями")
public interface UserControllerApi {

  @Operation(summary = "Удалить пользователя по ID")
  @ApiResponse(responseCode = "204", description = "Удалено успешно")
  @ApiResponse(responseCode = "404", description = "Пользователь не найден")
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteUser(@Parameter(description = "ID пользователя")
                                  @PathVariable Long id);
}