package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.RegisterDTO;
import com.researchflow.service.UserService;
import com.researchflow.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.researchflow.dto.LoginDTO;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录与退出登录")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(
            @Valid @RequestBody RegisterDTO dto
    ) {

        userService.register(dto);

        return Result.success();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(
            @Valid @RequestBody LoginDTO dto
    ) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<Void> logout() {

        userService.logout();

        return Result.success();
    }
}
