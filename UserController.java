package cn.edu.sdua.geren.controller;

import cn.edu.sdua.geren.common.ApiResponse;
import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<Dtos.UserVO> me(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(userService.toVO(userService.getById(userId)));
    }

    @PutMapping("/profile")
    public ApiResponse<Dtos.UserVO> updateProfile(@RequestAttribute("userId") Long userId,
                                                  @RequestBody Dtos.UpdateProfileReq req) {
        return ApiResponse.ok(userService.updateProfile(userId, req));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestAttribute("userId") Long userId,
                                            @RequestBody Dtos.ChangePasswordReq req) {
        userService.changePassword(userId, req);
        return ApiResponse.ok(null);
    }
}
