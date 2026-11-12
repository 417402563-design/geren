package cn.edu.sdua.geren.service;

import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.entity.User;
import cn.edu.sdua.geren.repository.UserRepository;
import cn.edu.sdua.geren.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户管理模块：注册、登录（JWT 无状态认证）。
 */
@Service
public class AuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthService(UserRepository userRepo, JwtUtil jwtUtil, UserService userService) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    public Dtos.LoginResp register(Dtos.RegisterReq req) {
        if (req.username() == null || !req.username().matches("[A-Za-z0-9_\\u4e00-\\u9fa5]{3,20}")) {
            throw new IllegalArgumentException("用户名需为3-20位字母、数字、下划线或中文");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw new IllegalArgumentException("密码长度至少为6位");
        }
        if (userRepo.existsByUsername(req.username())) {
            throw new IllegalArgumentException("该用户名已被注册");
        }
        User user = new User();
        user.setUsername(req.username());
        user.setPassword(ENCODER.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? req.username() : req.nickname().trim());
        userRepo.save(user);
        return login(new Dtos.LoginReq(req.username(), req.password()));
    }

    public Dtos.LoginResp login(Dtos.LoginReq req) {
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (req.password() == null || !ENCODER.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return new Dtos.LoginResp(token, userService.toVO(user));
    }
}
