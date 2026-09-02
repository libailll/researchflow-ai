package com.researchflow;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchflow.entity.User;
import com.researchflow.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private final AtomicReference<User> storedUser = new AtomicReference<>();
    private final Map<String, String> redisStore = new ConcurrentHashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));

        when(valueOperations.get(anyString()))
                .thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));
        when(stringRedisTemplate.delete(anyString()))
                .thenAnswer(invocation -> redisStore.remove(invocation.getArgument(0)) != null);

        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            storedUser.set(user);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(userMapper.selectOne(any(Wrapper.class)))
                .thenAnswer(invocation -> storedUser.get());
        when(userMapper.selectById(1L))
                .thenAnswer(invocation -> storedUser.get());
    }

    @Test
    void completeAuthenticationFlow() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "auth_user",
                                  "password": "123456",
                                  "nickname": "认证测试用户"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword())
                .isNotEqualTo("123456")
                .startsWith("$2");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "auth_user",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String token = loginJson.path("data").path("token").asText();
        assertThat(redisStore).containsEntry("login:token:1", token);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("auth_user"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "更新后的昵称",
                                  "email": "auth@example.com",
                                  "avatar": "https://example.com/avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("更新后的昵称"))
                .andExpect(jsonPath("$.data.email").value("auth@example.com"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        assertThat(redisStore).doesNotContainKey("login:token:1");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void invalidRegistrationRequestReturnsParameterError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "password": "123",
                                  "nickname": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        User disabledUser = new User();
        disabledUser.setId(2L);
        disabledUser.setUsername("disabled_user");
        disabledUser.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("123456"));
        disabledUser.setNickname("禁用用户");
        disabledUser.setStatus(0);
        disabledUser.setDeleted(0);
        storedUser.set(disabledUser);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "disabled_user",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void openApiDocumentIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ResearchFlow AI API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/users/me']").exists())
                .andExpect(jsonPath("$.paths['/api/projects']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/members']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/tasks']").exists())
                .andExpect(jsonPath("$.paths['/api/tasks/{taskId}/status']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/dashboard']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/documents']").exists())
                .andExpect(jsonPath("$.paths['/api/documents/{documentId}/download']").exists());
    }
}
