package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.UpdateUserDTO;
import com.researchflow.service.UserService;
import com.researchflow.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "当前用户信息接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<UserVO> me() {

        return Result.success(userService.getCurrentUser());
    }

    @PutMapping("/me")
    @Operation(summary = "修改当前用户资料", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateUserDTO dto) {
        return Result.success(userService.updateCurrentUser(dto));
    }
}
