package cn.edu.sdua.geren.controller;

import cn.edu.sdua.geren.common.ApiResponse;
import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Dtos.LoginResp> register(@RequestBody Dtos.RegisterReq req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<Dtos.LoginResp> login(@RequestBody Dtos.LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }
}
