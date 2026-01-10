package com.education.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.education.platform.common.ApiResult;
import com.education.platform.entity.Class;
import com.education.platform.entity.School;
import com.education.platform.entity.User;
import com.education.platform.mapper.ClassMapper;
import com.education.platform.mapper.SchoolMapper;
import com.education.platform.service.IUserService;
import com.education.platform.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 *
 * @author Education Platform Team
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户登录、注册等认证相关接口")
public class AuthController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SchoolMapper schoolMapper;

    @Autowired
    private ClassMapper classMapper;

    /**
     * 登录请求参数
     */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 注册请求参数
     */
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        @NotBlank(message = "真实姓名不能为空")
        private String realName;

        private String phone;
        private String email;

        // 改为接收学校名称和班级名称，而不是ID
        @NotBlank(message = "学校名称不能为空")
        private String schoolName;

        @NotBlank(message = "班级名称不能为空")
        private String className;
    }

    /**
     * 教师注册请求参数
     */
    @Data
    public static class TeacherRegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        @NotBlank(message = "真实姓名不能为空")
        private String realName;

        private String phone;
        private String email;
        private Long schoolId;              // 学校ID（选择现有学校）
        private String customSchoolName;    // 自定义学校名称（手动输入）
        private String department;  // 部门/院系
        private String title;       // 职称
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT Token")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> result = userService.login(request.getUsername(), request.getPassword());
            return ApiResult.success("登录成功", result);
        } catch (RuntimeException e) {
            // 如果用户不存在，自动创建用户（用于测试环境）
            if (e.getMessage().equals("用户不存在")) {
                System.out.println("=== 登录用户不存在，尝试创建: " + request.getUsername());
                try {
                    User user = new User();
                    user.setUsername(request.getUsername());
                    user.setPassword(request.getPassword());
                    user.setRealName(request.getUsername());  // 使用用户名作为真实姓名
                    user.setPhone("");  // 留空
                    user.setEmail("");  // 留空
                    user.setStatus(1);

                    Boolean success = userService.register(user);
                    if (success) {
                        System.out.println("=== 创建用户成功，重新登录: " + request.getUsername());
                        // 重新登录
                        Map<String, Object> result = userService.login(request.getUsername(), request.getPassword());
                        return ApiResult.success("登录成功", result);
                    }
                } catch (Exception ex) {
                    System.out.println("=== 创建用户失败: " + ex.getMessage());
                }
            }
            return ApiResult.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册（默认普通用户角色）")
    public ApiResult<Boolean> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // 1. 处理学校：查找或创建
            Long schoolId = null;
            LambdaQueryWrapper<School> schoolWrapper = new LambdaQueryWrapper<>();
            schoolWrapper.eq(School::getSchoolName, request.getSchoolName());
            schoolWrapper.last("LIMIT 1");  // 防止返回多条记录
            School existingSchool = schoolMapper.selectOne(schoolWrapper);

            if (existingSchool != null) {
                schoolId = existingSchool.getId();
            } else {
                // 创建新学校
                School newSchool = new School();
                newSchool.setSchoolCode("S" + System.currentTimeMillis());
                newSchool.setSchoolName(request.getSchoolName());
                newSchool.setSchoolType("secondary"); // 默认为中学类型
                newSchool.setAddress("待补充");
                newSchool.setContactPerson("待补充");
                newSchool.setContactPhone("待补充");
                newSchool.setStatus(1);
                schoolMapper.insert(newSchool);
                schoolId = newSchool.getId();
            }

            // 2. 处理班级：查找或创建
            Long classId = null;
            LambdaQueryWrapper<Class> classWrapper = new LambdaQueryWrapper<>();
            classWrapper.eq(Class::getSchoolId, schoolId);
            classWrapper.eq(Class::getClassName, request.getClassName());
            classWrapper.last("LIMIT 1");  // 防止返回多条记录
            Class existingClass = classMapper.selectOne(classWrapper);

            if (existingClass != null) {
                classId = existingClass.getId();
            } else {
                // 创建新班级
                Class newClass = new Class();
                newClass.setSchoolId(schoolId);
                newClass.setClassName(request.getClassName());
                newClass.setClassCode("C" + System.currentTimeMillis());
                newClass.setStatus(1);
                newClass.setStudentCount(0);
                classMapper.insert(newClass);
                classId = newClass.getId();
            }

            // 3. 创建用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setSchoolId(schoolId);
            user.setClassId(classId);

            Boolean success = userService.register(user);
            return ApiResult.success("注册成功", success);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PostMapping("/register/teacher")
    @Operation(summary = "教师注册", description = "注册教师用户（自动分配TEACHER角色）")
    public ApiResult<Boolean> registerTeacher(@Valid @RequestBody TeacherRegisterRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setDepartment(request.getDepartment());
            user.setTitle(request.getTitle());

            // 处理学校选择：要么选择现有学校，要么创建新学校
            Long schoolId = request.getSchoolId();

            // 如果没有选择学校，但提供了自定义学校名称，则创建新学校
            if (schoolId == null && request.getCustomSchoolName() != null && !request.getCustomSchoolName().trim().isEmpty()) {
                // 创建新学校
                School newSchool = new School();
                newSchool.setSchoolCode("S" + System.currentTimeMillis());  // 生成唯一编码
                newSchool.setSchoolName(request.getCustomSchoolName());
                newSchool.setSchoolType("secondary");  // 默认为中学类型
                newSchool.setAddress("待补充");
                newSchool.setContactPerson("待补充");
                newSchool.setContactPhone("待补充");
                newSchool.setStatus(1);

                // 保存学校并获取ID
                int result = schoolMapper.insert(newSchool);
                if (result > 0) {
                    schoolId = newSchool.getId();
                }
            }

            if (schoolId == null) {
                return ApiResult.error("请选择学校或输入学校名称");
            }

            user.setSchoolId(schoolId);

            Boolean success = userService.registerTeacher(user);
            return ApiResult.success("教师注册成功", success);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的信息")
    public ApiResult<Map<String, Object>> getCurrentUserInfo(@RequestHeader("Authorization") String token) {
        try {
            // 从token中解析用户名
            String jwt = token.substring(7); // 去掉 "Bearer "
            // 使用JWT工具类解析用户名
            String username = jwtUtils.getUsernameFromToken(jwt);

            User user = userService.getByUsername(username);
            if (user == null) {
                // 如果用户不存在，尝试创建默认用户（用于测试环境）
                // 使用用户名作为真实姓名，其他字段留空或默认值
                System.out.println("=== 用户不存在，尝试创建默认用户: " + username);
                user = new User();
                user.setUsername(username);
                user.setPassword("$2a$10$0FbH16apzWwgCl5/S6.FBezzcCMOkAuOkkTQk1JKo4vs02jztBsIq"); // admin
                user.setRealName(username);  // 使用用户名作为真实姓名
                user.setPhone("");  // 留空
                user.setEmail("");  // 留空
                user.setStatus(1);
                user.setDeleted(0);  // 逻辑删除字段

                Boolean success = userService.register(user);
                if (!success) {
                    return ApiResult.error("创建用户失败");
                }
                System.out.println("=== 创建用户成功: " + username);
            }

            Map<String, Object> userInfo = userService.getUserInfo(user.getId());
            return ApiResult.success(userInfo);
        } catch (Exception e) {
            System.out.println("=== 获取用户信息异常: " + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("获取用户信息失败: " + e.getMessage());
        }
    }
}
