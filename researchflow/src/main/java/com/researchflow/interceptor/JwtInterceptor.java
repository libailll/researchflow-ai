package com.researchflow.interceptor;

import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.exception.BusinessException;
import com.researchflow.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        // 1. 获取 Authorization 请求头
        String authorization = request.getHeader("Authorization");

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 去掉 "Bearer "
        String token = authorization.substring(7);

        // 3. 校验 JWT 本身是否合法
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 4. 从 JWT 中解析 userId
        Long userId = jwtUtil.getUserId(token);

        // 5. 去 Redis 查询登录状态
        String redisKey = "login:token:" + userId;
        String redisToken =
                stringRedisTemplate.opsForValue().get(redisKey);

        if (redisToken == null || !redisToken.equals(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        UserContext.setUserId(userId);

        // 6. 验证成功，放行
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        UserContext.clear();
    }
}