package com.researchflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.config.JwtProperties;
import com.researchflow.context.UserContext;
import com.researchflow.dto.LoginDTO;
import com.researchflow.dto.RegisterDTO;
import com.researchflow.dto.UpdateUserDTO;
import com.researchflow.entity.User;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.UserMapper;
import com.researchflow.service.UserService;
import com.researchflow.util.JwtUtil;
import com.researchflow.vo.LoginVO;
import com.researchflow.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final JwtUtil jwtUtil;

    private final JwtProperties jwtProperties;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void register(RegisterDTO dto) {

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
        );

        if (count > 0) {
            log.warn("Registration rejected: username already exists, username={}",
                    dto.getUsername());
            throw new BusinessException(
                    ErrorCode.USERNAME_ALREADY_EXISTS
            );
        }

        User user = new User();

        user.setUsername(dto.getUsername());
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(1);

        userMapper.insert(user);
        log.info("User registered: userId={}, username={}",
                user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {

        // 1. 根据用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .eq(User::getDeleted, 0)
        );

        // 2. 用户不存在
        if (user == null) {
            log.warn("Login failed: invalid credentials, username={}", dto.getUsername());
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 3. 校验密码
        BCryptPasswordEncoder passwordEncoder =
                new BCryptPasswordEncoder();

        if (!passwordEncoder.matches(
                dto.getPassword(),
                user.getPassword()
        )) {
            log.warn("Login failed: invalid credentials, username={}", dto.getUsername());
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            log.warn("Login rejected: user disabled, userId={}", user.getId());
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId());

        // 5. 保存登录状态到 Redis
        String redisKey = "login:token:" + user.getId();

        stringRedisTemplate.opsForValue().set(
                redisKey,
                token,
                jwtProperties.ttl(),
                TimeUnit.MILLISECONDS
        );

        log.info("User logged in: userId={}", user.getId());

        // 6. 构造返回给前端的用户信息
        UserVO userVO = toUserVO(user);

        // 7. 返回 Token + User
        return new LoginVO(token, userVO);
    }

    @Override
    public void logout() {
        Long userId = UserContext.getUserId();

        String key = "login:token:" + userId;

        stringRedisTemplate.delete(key);
        log.info("User logged out: userId={}", userId);
    }

    @Override
    public UserVO getCurrentUser() {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public UserVO updateCurrentUser(UpdateUserDTO dto) {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        userMapper.updateById(user);
        log.info("User profile updated: userId={}", userId);
        return toUserVO(user);
    }

    private UserVO toUserVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setEmail(user.getEmail());
        userVO.setAvatar(user.getAvatar());
        return userVO;
    }
}
