package cn.edu.sdua.geren.service;

import cn.edu.sdua.geren.dto.Dtos;
import cn.edu.sdua.geren.entity.User;
import cn.edu.sdua.geren.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 个人资料管理：查询、修改基本信息、修改密码。
 */
@Service
public class UserService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User getById(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    public Dtos.UserVO toVO(User u) {
        return new Dtos.UserVO(u.getId(), u.getUsername(), u.getNickname(), u.getEmail(),
                u.getCreatedAt() == null ? null : u.getCreatedAt().format(FMT));
    }

    public Dtos.UserVO updateProfile(Long userId, Dtos.UpdateProfileReq req) {
        User u = getById(userId);
        if (req.nickname() != null && !req.nickname().isBlank()) {
            u.setNickname(req.nickname().trim());
        }
        if (req.email() != null) {
            if (!req.email().isBlank() && !req.email().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
                throw new IllegalArgumentException("邮箱格式不正确");
            }
            u.setEmail(req.email().trim());
        }
        return toVO(userRepo.save(u));
    }

    public void changePassword(Long userId, Dtos.ChangePasswordReq req) {
        User u = getById(userId);
        if (req.oldPassword() == null || !ENCODER.matches(req.oldPassword(), u.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        if (req.newPassword() == null || req.newPassword().length() < 6) {
            throw new IllegalArgumentException("新密码长度至少为6位");
        }
        u.setPassword(ENCODER.encode(req.newPassword()));
        userRepo.save(u);
    }
}
