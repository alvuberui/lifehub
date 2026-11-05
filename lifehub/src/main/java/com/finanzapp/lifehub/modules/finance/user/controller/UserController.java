package com.finanzapp.user.controller;

import com.finanzapp.user.controller.dto.BooleanRespondeDto;
import com.finanzapp.user.controller.dto.CreateUserRequestDto;
import com.finanzapp.user.controller.dto.IdealPercentageResponseDto;
import com.finanzapp.user.controller.mapper.CommonObjectMapper;
import com.finanzapp.user.controller.mapper.UserMapper;
import com.finanzapp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.Builder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Builder
public class UserController {

    private final UserService userService;

    private final CommonObjectMapper commonObjectMapper;

    private final UserMapper userMapper;

    @GetMapping("/isOnboarded")
    public ResponseEntity<BooleanRespondeDto> isUserOnboarded(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        BooleanRespondeDto response = commonObjectMapper.toBooleanRespondeDto(userService.hasOnBoarded(email));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/")
    public ResponseEntity<Void> createUser(
            @Valid @RequestBody CreateUserRequestDto requestDto,
            @AuthenticationPrincipal Jwt jwt) {
        // La identidad debe derivarse del token validado, nunca de un email enviado por el cliente.
        requestDto.setEmail(jwt.getClaimAsString("email"));
        userService.createUser(
                commonObjectMapper.toCreateUserRequest(requestDto)
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/idealPercentage")
    public ResponseEntity<IdealPercentageResponseDto> getIdealPercentage(
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaimAsString("email");

        IdealPercentageResponseDto response = userMapper.toIdealPercentageResponseDto(
                userService.getIdealPercentage(email)
        );

        return ResponseEntity.ok(response);
    }


    @PutMapping("/")
    public ResponseEntity<Void> updateUser(
            @Valid @RequestBody com.finanzapp.user.controller.dto.UpdateUserRequestDto requestDto,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");

        userService.updateUser(
                userMapper.toUpdateUserRequest(requestDto), email
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/")
    public ResponseEntity<com.finanzapp.user.controller.dto.UserDto> getUserByEmail(
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        var user = userService.getUserByEmail(email);
        return ResponseEntity.ok(
                userMapper.toUserDto(user)
        );
    }

}
