package com.researchflow.service;

import com.researchflow.dto.LoginDTO;
import com.researchflow.dto.RegisterDTO;
import com.researchflow.dto.UpdateUserDTO;
import com.researchflow.vo.LoginVO;
import com.researchflow.vo.UserVO;

public interface UserService {

    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    void logout();

    UserVO getCurrentUser();

    UserVO updateCurrentUser(UpdateUserDTO dto);
}
