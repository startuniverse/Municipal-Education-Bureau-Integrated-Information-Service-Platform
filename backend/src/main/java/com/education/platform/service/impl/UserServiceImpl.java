package com.education.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.education.platform.entity.Role;
import com.education.platform.entity.School;
import com.education.platform.entity.Student;
import com.education.platform.entity.Teacher;
import com.education.platform.entity.TeacherBasic;
import com.education.platform.entity.User;
import com.education.platform.mapper.RoleMapper;
import com.education.platform.mapper.SchoolMapper;
import com.education.platform.mapper.StudentMapper;
import com.education.platform.mapper.TeacherBasicMapper;
import com.education.platform.mapper.TeacherMapper;
import com.education.platform.mapper.UserMapper;
import com.education.platform.service.IUserService;
import com.education.platform.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务实现
 *
 * @author Education Platform Team
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private SchoolMapper schoolMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private TeacherBasicMapper teacherBasicMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成JWT Token
        String token = jwtUtils.generateToken(username);

        // 获取用户角色和权限
        List<String> roles = new ArrayList<>(userMapper.selectRolesByUserId(user.getId()));
        List<String> permissions = new ArrayList<>(userMapper.selectPermissionsByUserId(user.getId()));

        // 调试日志
        System.out.println("=== 用户 " + username + " 登录，角色: " + roles + "，权限: " + permissions);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", getUserInfoMap(user, roles, permissions));

        log.info("用户 {} 登录成功", username);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean register(User user) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setDeleted(0);  // 设置逻辑删除字段

        // 插入用户
        int result = userMapper.insert(user);
        if (result > 0) {
            // 默认分配USER角色
            assignRole(user.getId(), "USER");

            // 自动创建学生档案
            Student student = new Student();
            student.setUserId(user.getId());
            student.setStudentNumber("STU" + System.currentTimeMillis());  // 生成学号
            student.setClassId(user.getClassId());  // 使用用户表中的班级ID
            student.setEnrollmentDate(LocalDate.now());  // 入学日期为当前日期
            student.setStatus(1);  // 在读状态
            student.setDeleted(0);

            // 插入学生记录
            studentMapper.insert(student);
        }
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean registerWithRole(User user, String roleCode) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setDeleted(0);  // 设置逻辑删除字段

        // 插入用户
        int result = userMapper.insert(user);
        if (result > 0) {
            // 分配指定角色
            boolean roleAssigned = assignRole(user.getId(), roleCode);

            // 如果是教师角色，自动创建教师档案
            if ("TEACHER".equals(roleCode)) {
                Teacher teacher = new Teacher();
                teacher.setUserId(user.getId());
                teacher.setTeacherNumber("T" + user.getId());  // 生成教师编号
                teacher.setTitle(user.getTitle());  // 从用户信息中获取职称
                teacher.setSubject(user.getDepartment());  // 从用户信息中获取部门/科目
                teacher.setHireDate(LocalDate.now());  // 入职日期为当前日期
                teacher.setStatus(1);  // 在职状态
                teacher.setDeleted(0);

                // 插入教师记录并获取ID
                teacherMapper.insert(teacher);
                Long teacherId = teacher.getId();

                // 自动创建教师基础信息（teacher_basic表）
                TeacherBasic teacherBasic = new TeacherBasic();
                teacherBasic.setUserId(user.getId());  // 关联用户ID
                teacherBasic.setTeacherId(teacherId);  // 关联教师ID
                teacherBasic.setTeacherName(user.getRealName());  // 使用用户真实姓名
                teacherBasic.setTeacherNumber("T" + user.getId());  // 教师工号
                teacherBasic.setGender(null);  // 性别暂未填写
                teacherBasic.setIdCard(null);  // 身份证号暂未填写
                teacherBasic.setPhone(user.getPhone());  // 手机号从用户信息获取
                teacherBasic.setEmail(user.getEmail());  // 邮箱从用户信息获取
                teacherBasic.setSchoolId(user.getSchoolId());  // 学校ID
                teacherBasic.setDepartment(user.getDepartment());  // 部门/院系
                teacherBasic.setPosition(null);  // 职务暂未填写
                teacherBasic.setTitle(user.getTitle());  // 职称从用户信息获取
                teacherBasic.setStatus(1);  // 在职状态
                teacherBasic.setDeleted(0);  // 逻辑删除标记

                teacherBasicMapper.insert(teacherBasic);
            }

            return roleAssigned;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean registerTeacher(User user) {
        return registerWithRole(user, "TEACHER");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRole(Long userId, String roleCode) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 查询角色ID
        Long roleId = userMapper.selectRoleIdByCode(roleCode);

        // 如果角色不存在，自动创建
        if (roleId == null) {
            log.info("角色 {} 不存在，自动创建", roleCode);

            Role newRole = new Role();
            newRole.setRoleCode(roleCode);
            newRole.setRoleName(getDefaultRoleName(roleCode));
            newRole.setDescription("自动创建的角色");

            roleMapper.insert(newRole);
            roleId = newRole.getId();
        }

        // 分配角色
        userMapper.assignRoleToUser(userId, roleId);
        return true;
    }

    /**
     * 根据角色编码获取默认角色名称
     */
    private String getDefaultRoleName(String roleCode) {
        switch (roleCode) {
            case "TEACHER":
                return "教师";
            case "ADMIN":
                return "系统管理员";
            case "USER":
                return "普通用户";
            default:
                return roleCode;
        }
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.updateById(user) > 0;
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        List<String> roles = new ArrayList<>(userMapper.selectRolesByUserId(userId));
        List<String> permissions = new ArrayList<>(userMapper.selectPermissionsByUserId(userId));

        return getUserInfoMap(user, roles, permissions);
    }

    /**
     * 构建用户信息Map
     */
    private Map<String, Object> getUserInfoMap(User user, List<String> roles, List<String> permissions) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("schoolId", user.getSchoolId());

        // 查询学校名称
        if (user.getSchoolId() != null) {
            School school = schoolMapper.selectById(user.getSchoolId());
            if (school != null) {
                userInfo.put("schoolName", school.getSchoolName());
            } else {
                userInfo.put("schoolName", "");
            }
        } else {
            userInfo.put("schoolName", "");
        }

        userInfo.put("department", user.getDepartment());
        userInfo.put("title", user.getTitle());
        userInfo.put("roles", roles);
        userInfo.put("permissions", permissions);
        return userInfo;
    }
}
