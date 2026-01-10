package com.education.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.platform.common.ApiResult;
import com.education.platform.common.PageResult;
import com.education.platform.entity.*;
import com.education.platform.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师信息管理控制器
 *
 * @author Education Platform Team
 */
@Slf4j
@RestController
@RequestMapping("/teacher-info")
@Tag(name = "教师信息管理", description = "教师信息管理相关接口")
public class TeacherInfoController {

    @Autowired
    private ITeacherBasicService teacherBasicService;

    @Autowired
    private ITeacherPositionService teacherPositionService;

    @Autowired
    private ITeacherEducationService teacherEducationService;

    @Autowired
    private ITeacherQualificationService teacherQualificationService;

    @Autowired
    private ITeacherHonorService teacherHonorService;

    @Autowired
    private ITeacherAssessmentService teacherAssessmentService;

    @Autowired
    private ITeacherRewardPunishmentService teacherRewardPunishmentService;

    @Autowired
    private ITeacherEthicsService teacherEthicsService;

    @Autowired
    private ITeacherTrainingRecordService teacherTrainingRecordService;

    @Autowired
    private ITeacherEducationCreditService teacherEducationCreditService;

    @Autowired
    private ITeacherTeachingTaskService teacherTeachingTaskService;

    @Autowired
    private ITeacherResearchActivityService teacherResearchActivityService;

    @Autowired
    private ITeacherWorkloadStatisticsService teacherWorkloadStatisticsService;

    @Autowired
    private ITeacherService teacherService;

    @Autowired
    private IUserService userService;

    // ==================== 教师基础信息 ====================

    @GetMapping("/basic/list")
    @Operation(summary = "教师基础信息列表", description = "分页查询教师基础信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<PageResult<TeacherBasic>> listBasicInfo(
            @Parameter(description = "学校ID") @RequestParam(required = false) Long schoolId,
            @Parameter(description = "姓名模糊查询") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {

        log.info("=== 教师列表查询开始 ===");
        log.info("请求参数: schoolId={}, keyword={}, current={}, size={}", schoolId, keyword, current, size);

        Page<TeacherBasic> page = new Page<>(current, size);
        LambdaQueryWrapper<TeacherBasic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherBasic::getDeleted, 0);

        if (schoolId != null) {
            wrapper.eq(TeacherBasic::getSchoolId, schoolId);
            log.info("添加schoolId过滤: {}", schoolId);
        } else {
            log.info("schoolId为null，不添加学校过滤条件");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(TeacherBasic::getTeacherName, keyword);
            log.info("添加keyword过滤: {}", keyword);
        }

        wrapper.orderByDesc(TeacherBasic::getCreatedAt);

        // 打印生成的SQL
        String sql = wrapper.getCustomSqlSegment();
        log.info("生成的SQL条件: {}", sql);

        Page<TeacherBasic> result = teacherBasicService.page(page, wrapper);

        log.info("查询结果: 总数={}, 当前页数据量={}", result.getTotal(), result.getRecords().size());
        log.info("当前页数据: {}", result.getRecords());

        return ApiResult.success(PageResult.of(result));
    }

    @PostMapping("/basic/add")
    @Operation(summary = "新增教师基础信息", description = "新增教师基础信息，支持两种方式：1) 提供teacher_id直接创建；2) 提供userId自动创建Teacher记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addBasicInfo(@RequestBody TeacherBasic teacherBasic) {
        try {
            log.info("=== 开始新增教师基础信息 ===");
            log.info("接收参数: teacherId={}, userId={}, teacherNumber={}, teacherName={}",
                teacherBasic.getTeacherId(), teacherBasic.getUserId(),
                teacherBasic.getTeacherNumber(), teacherBasic.getTeacherName());

            // 情况1：提供了teacher_id，直接创建TeacherBasic
            if (teacherBasic.getTeacherId() != null) {
                log.info("模式1: 使用现有teacher_id={}", teacherBasic.getTeacherId());

                // 验证teacher_id对应的Teacher记录是否存在
                Teacher teacher = teacherService.getById(teacherBasic.getTeacherId());
                if (teacher == null) {
                    log.error("指定的teacher_id={}在teacher表中不存在", teacherBasic.getTeacherId());
                    return ApiResult.error("新增失败: 指定的教师ID不存在，请先创建Teacher记录");
                }

                // 新增: 检查teacher_basic表中是否已存在该teacher_id
                LambdaQueryWrapper<TeacherBasic> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(TeacherBasic::getTeacherId, teacherBasic.getTeacherId())
                           .eq(TeacherBasic::getDeleted, 0);
                TeacherBasic existingBasic = teacherBasicService.getOne(checkWrapper);
                if (existingBasic != null) {
                    log.error("teacher_basic表中已存在teacher_id={}的记录", teacherBasic.getTeacherId());
                    return ApiResult.error("新增失败: 该教师的基础信息已存在，请勿重复添加");
                }

                log.info("Teacher记录验证通过: {}", teacher);
                teacherBasic.setDeleted(0);
                boolean result = teacherBasicService.save(teacherBasic);
                log.info("TeacherBasic保存成功: {}", result);
                return ApiResult.success(result);
            }

            // 情况2：提供了user_id，需要先创建Teacher记录
            if (teacherBasic.getUserId() != null) {
                log.info("模式2: 使用userId={}创建Teacher记录", teacherBasic.getUserId());

                // 检查该userId是否已经存在Teacher记录
                LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Teacher::getUserId, teacherBasic.getUserId())
                       .eq(Teacher::getDeleted, 0);
                Teacher existingTeacher = teacherService.getOne(wrapper);

                Long teacherId;
                if (existingTeacher != null) {
                    log.warn("userId={}已存在对应的Teacher记录，ID={}",
                        teacherBasic.getUserId(), existingTeacher.getId());
                    // 使用现有的Teacher记录
                    teacherId = existingTeacher.getId();
                } else {
                    // 创建新的Teacher记录
                    Teacher teacher = new Teacher();
                    teacher.setUserId(teacherBasic.getUserId());
                    teacher.setTeacherNumber(teacherBasic.getTeacherNumber());
                    teacher.setTitle(teacherBasic.getTitle());
                    teacher.setSubject(teacherBasic.getDepartment());
                    teacher.setHireDate(teacherBasic.getHireDate() != null ? teacherBasic.getHireDate() : java.time.LocalDate.now());
                    teacher.setStatus(1);
                    teacher.setDeleted(0);

                    log.info("准备创建Teacher记录: {}", teacher);
                    teacherService.save(teacher);
                    log.info("Teacher记录创建成功，ID: {}", teacher.getId());
                    teacherId = teacher.getId();
                }

                // 新增: 检查teacher_basic表中是否已存在该teacher_id
                LambdaQueryWrapper<TeacherBasic> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(TeacherBasic::getTeacherId, teacherId)
                           .eq(TeacherBasic::getDeleted, 0);
                TeacherBasic existingBasic = teacherBasicService.getOne(checkWrapper);
                if (existingBasic != null) {
                    log.error("teacher_basic表中已存在teacher_id={}的记录", teacherId);
                    return ApiResult.error("新增失败: 该教师的基础信息已存在，请勿重复添加");
                }

                teacherBasic.setTeacherId(teacherId);
                teacherBasic.setDeleted(0);
                boolean result = teacherBasicService.save(teacherBasic);
                log.info("TeacherBasic保存成功: {}", result);
                return ApiResult.success(result);
            }

            // 情况3：既没有teacher_id也没有user_id，直接创建完整的教师信息
            log.info("模式3: 直接创建完整的教师信息");

            // 1. 检查用户名是否已存在
            String username = teacherBasic.getTeacherNumber();
            User existingUser = userService.getByUsername(username);
            Long userId;
            Long teacherId;

            if (existingUser != null) {
                log.warn("用户名{}已存在，ID={}", username, existingUser.getId());
                userId = existingUser.getId();

                // 检查该用户是否已有Teacher记录
                LambdaQueryWrapper<Teacher> teacherWrapper = new LambdaQueryWrapper<>();
                teacherWrapper.eq(Teacher::getUserId, userId)
                             .eq(Teacher::getDeleted, 0);
                Teacher existingTeacher = teacherService.getOne(teacherWrapper);

                if (existingTeacher != null) {
                    log.warn("用户{}已有Teacher记录，ID={}", userId, existingTeacher.getId());
                    teacherId = existingTeacher.getId();
                } else {
                    // 创建新的Teacher记录
                    Teacher teacher = new Teacher();
                    teacher.setUserId(userId);
                    teacher.setTeacherNumber(teacherBasic.getTeacherNumber());
                    teacher.setTitle(teacherBasic.getTitle());
                    teacher.setSubject(teacherBasic.getDepartment());
                    teacher.setHireDate(teacherBasic.getHireDate() != null ? teacherBasic.getHireDate() : java.time.LocalDate.now());
                    teacher.setStatus(1);
                    teacher.setDeleted(0);

                    log.info("准备创建Teacher记录: {}", teacher);
                    teacherService.save(teacher);
                    log.info("Teacher记录创建成功，ID: {}", teacher.getId());
                    teacherId = teacher.getId();
                }
            } else {
                // 创建新的User记录
                User user = new User();
                user.setUsername(teacherBasic.getTeacherNumber()); // 使用工号作为用户名
                user.setPassword("$2a$10$0FbH16apzWwgCl5/S6.FBezzcCMOkAuOkkTQk1JKo4vs02jztBsIq"); // 默认密码: admin
                user.setRealName(teacherBasic.getTeacherName());
                user.setPhone(teacherBasic.getPhone());
                user.setEmail(teacherBasic.getEmail());
                user.setIdCard(teacherBasic.getIdCard());
                user.setGender("male".equals(teacherBasic.getGender()) ? 1 : ("female".equals(teacherBasic.getGender()) ? 2 : 0));
                user.setBirthDate(teacherBasic.getBirthDate());
                user.setSchoolId(teacherBasic.getSchoolId());
                user.setDepartment(teacherBasic.getDepartment());
                user.setTitle(teacherBasic.getTitle());
                user.setStatus(1);
                user.setDeleted(0);

                log.info("准备创建User记录: {}", user);
                Boolean userResult = userService.registerTeacher(user);
                if (!userResult) {
                    log.error("创建User记录失败");
                    return ApiResult.error("新增失败: 创建用户失败");
                }
                log.info("User记录创建成功，ID: {}", user.getId());
                userId = user.getId();

                // 查询创建的Teacher记录
                LambdaQueryWrapper<Teacher> teacherWrapper = new LambdaQueryWrapper<>();
                teacherWrapper.eq(Teacher::getUserId, userId)
                             .eq(Teacher::getDeleted, 0);
                Teacher teacher = teacherService.getOne(teacherWrapper);
                if (teacher == null) {
                    log.error("未找到新创建的Teacher记录");
                    return ApiResult.error("新增失败: 未找到教师记录");
                }
                log.info("找到Teacher记录，ID: {}", teacher.getId());
                teacherId = teacher.getId();
            }

            // 2. 检查teacher_basic表中是否已存在该teacher_id
            LambdaQueryWrapper<TeacherBasic> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(TeacherBasic::getTeacherId, teacherId)
                       .eq(TeacherBasic::getDeleted, 0);
            TeacherBasic existingBasic = teacherBasicService.getOne(checkWrapper);

            boolean result;
            if (existingBasic != null) {
                // 已存在TeacherBasic记录，更新它
                log.warn("teacher_basic表中已存在teacher_id={}的记录，执行更新操作", teacherId);

                // 更新现有记录的字段
                existingBasic.setTeacherName(teacherBasic.getTeacherName());
                existingBasic.setTeacherNumber(teacherBasic.getTeacherNumber());
                existingBasic.setGender(teacherBasic.getGender());
                existingBasic.setBirthDate(teacherBasic.getBirthDate());
                existingBasic.setIdCard(teacherBasic.getIdCard());
                existingBasic.setPhone(teacherBasic.getPhone());
                existingBasic.setEmail(teacherBasic.getEmail());
                existingBasic.setSchoolId(teacherBasic.getSchoolId());
                existingBasic.setDepartment(teacherBasic.getDepartment());
                existingBasic.setPosition(teacherBasic.getPosition());
                existingBasic.setTitle(teacherBasic.getTitle());
                existingBasic.setHireDate(teacherBasic.getHireDate());
                existingBasic.setStatus(teacherBasic.getStatus());
                existingBasic.setRemarks(teacherBasic.getRemarks());

                result = teacherBasicService.updateById(existingBasic);
                log.info("TeacherBasic更新成功: {}", result);
            } else {
                // 不存在TeacherBasic记录，创建新记录
                log.info("创建新的TeacherBasic记录");
                teacherBasic.setTeacherId(teacherId);
                teacherBasic.setUserId(userId);
                teacherBasic.setDeleted(0);
                result = teacherBasicService.save(teacherBasic);
                log.info("TeacherBasic保存成功: {}", result);
            }

            return ApiResult.success(result);

        } catch (Exception e) {
            log.error("新增教师基础信息失败", e);
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @PostMapping("/basic/update")
    @Operation(summary = "更新教师基础信息", description = "更新教师基础信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> updateBasicInfo(@RequestBody TeacherBasic teacherBasic) {
        try {
            boolean result = teacherBasicService.updateById(teacherBasic);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/basic/delete")
    @Operation(summary = "删除教师基础信息", description = "删除教师基础信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> deleteBasicInfo(@Parameter(description = "ID") @RequestParam Long id) {
        try {
            boolean result = teacherBasicService.removeById(id);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/basic/{teacherId}")
    @Operation(summary = "根据教师ID查询基础信息", description = "查询单个教师的完整基础信息")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<Map<String, Object>> getTeacherInfo(@PathVariable Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        // 基础信息
        TeacherBasic basic = teacherBasicService.getByTeacherId(teacherId);
        result.put("basic", basic);

        if (basic != null) {
            // 岗位信息
            TeacherPosition position = teacherPositionService.getCurrentPosition(teacherId);
            result.put("position", position);

            // 教育背景
            List<TeacherEducation> educations = teacherEducationService.listByTeacherId(teacherId);
            result.put("educations", educations);

            // 资格职称
            List<TeacherQualification> qualifications = teacherQualificationService.listByTeacherId(teacherId);
            result.put("qualifications", qualifications);

            // 荣誉称号
            List<TeacherHonor> honors = teacherHonorService.listByTeacherId(teacherId);
            result.put("honors", honors);
        }

        return ApiResult.success(result);
    }

    // ==================== 岗位信息 ====================

    @GetMapping("/position/list")
    @Operation(summary = "岗位信息列表", description = "查询教师岗位信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherPosition>> listPositions(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        List<TeacherPosition> list = teacherPositionService.listByTeacherId(teacherId);
        return ApiResult.success(list);
    }

    @PostMapping("/position/add")
    @Operation(summary = "新增岗位信息", description = "新增教师岗位信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addPosition(@RequestBody TeacherPosition position) {
        try {
            boolean result = teacherPositionService.addPositionChange(position);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    // ==================== 教育背景 ====================

    @GetMapping("/education/list")
    @Operation(summary = "教育背景列表", description = "查询教师教育背景")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherEducation>> listEducations(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        List<TeacherEducation> list = teacherEducationService.listByTeacherId(teacherId);
        return ApiResult.success(list);
    }

    @PostMapping("/education/add")
    @Operation(summary = "新增教育背景", description = "新增教师教育背景")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addEducation(@RequestBody TeacherEducation education) {
        try {
            boolean result = teacherEducationService.addEducation(education);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    // ==================== 资格职称 ====================

    @GetMapping("/qualification/list")
    @Operation(summary = "资格职称列表", description = "查询教师资格职称")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherQualification>> listQualifications(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        List<TeacherQualification> list = teacherQualificationService.listByTeacherId(teacherId);
        return ApiResult.success(list);
    }

    @PostMapping("/qualification/add")
    @Operation(summary = "新增资格职称", description = "新增教师资格职称")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addQualification(@RequestBody TeacherQualification qualification) {
        try {
            boolean result = teacherQualificationService.addQualification(qualification);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    // ==================== 荣誉称号 ====================

    @GetMapping("/honor/list")
    @Operation(summary = "荣誉称号列表", description = "查询教师荣誉称号")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherHonor>> listHonors(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        List<TeacherHonor> list = teacherHonorService.listByTeacherId(teacherId);
        return ApiResult.success(list);
    }

    @PostMapping("/honor/add")
    @Operation(summary = "新增荣誉称号", description = "新增教师荣誉称号")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addHonor(@RequestBody TeacherHonor honor) {
        try {
            boolean result = teacherHonorService.addHonor(honor);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    // ==================== 考核记录 ====================

    @GetMapping("/assessment/list")
    @Operation(summary = "考核记录列表", description = "查询教师考核记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherAssessment>> listAssessments(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        List<TeacherAssessment> list = teacherAssessmentService.listByTeacherId(teacherId);
        return ApiResult.success(list);
    }

    @PostMapping("/assessment/add")
    @Operation(summary = "新增考核记录", description = "新增教师考核记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addAssessment(@RequestBody TeacherAssessment assessment) {
        try {
            boolean result = teacherAssessmentService.addAssessment(assessment);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @GetMapping("/assessment/statistics")
    @Operation(summary = "考核统计", description = "统计教师考核结果分布")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Long>> statisticsAssessments(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        Map<String, Long> result = teacherAssessmentService.countAssessmentResults(teacherId);
        return ApiResult.success(result);
    }

    // ==================== 奖惩记录 ====================

    @GetMapping("/reward-punishment/list")
    @Operation(summary = "奖惩记录列表", description = "查询教师奖惩记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherRewardPunishment>> listRewardPunishments(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "类型：reward-奖励，punishment-惩罚") @RequestParam(required = false) String type) {

        List<TeacherRewardPunishment> list;
        if (type != null) {
            list = teacherRewardPunishmentService.listByType(teacherId, type);
        } else {
            list = teacherRewardPunishmentService.listByTeacherId(teacherId);
        }
        return ApiResult.success(list);
    }

    @PostMapping("/reward-punishment/add")
    @Operation(summary = "新增奖惩记录", description = "新增教师奖惩记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addRewardPunishment(@RequestBody TeacherRewardPunishment record) {
        try {
            boolean result = teacherRewardPunishmentService.addRewardPunishment(record);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    // ==================== 师德考核 ====================

    @GetMapping("/ethics/list")
    @Operation(summary = "师德考核列表", description = "查询教师师德考核记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherEthics>> listEthics(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        List<TeacherEthics> list = teacherEthicsService.listByTeacherId(teacherId);
        return ApiResult.success(list);
    }

    @PostMapping("/ethics/add")
    @Operation(summary = "新增师德考核", description = "新增教师师德考核记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addEthics(@RequestBody TeacherEthics ethics) {
        try {
            boolean result = teacherEthicsService.addEthics(ethics);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @GetMapping("/ethics/statistics")
    @Operation(summary = "师德考核统计", description = "统计师德考核等级分布")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Long>> statisticsEthics(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        Map<String, Long> result = teacherEthicsService.countEthicsLevels(teacherId);
        return ApiResult.success(result);
    }

    // ==================== 培训记录 ====================

    @GetMapping("/training/list")
    @Operation(summary = "培训记录列表", description = "查询教师培训记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherTrainingRecord>> listTrainings(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "分类：skill-教学技能，info-信息化，ethics-师德") @RequestParam(required = false) String category) {

        List<TeacherTrainingRecord> list;
        if (category != null) {
            list = teacherTrainingRecordService.listByCategory(teacherId, category);
        } else {
            list = teacherTrainingRecordService.listByTeacherId(teacherId);
        }
        return ApiResult.success(list);
    }

    @PostMapping("/training/add")
    @Operation(summary = "新增培训记录", description = "新增教师培训记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addTraining(@RequestBody TeacherTrainingRecord record) {
        try {
            boolean result = teacherTrainingRecordService.addTrainingRecord(record);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @GetMapping("/training/statistics")
    @Operation(summary = "培训统计", description = "统计培训学时")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Object>> statisticsTrainings(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        Map<String, Object> result = teacherTrainingRecordService.statisticsTrainingHours(teacherId);
        return ApiResult.success(result);
    }

    // ==================== 继续教育学分 ====================

    @GetMapping("/credit/list")
    @Operation(summary = "学分记录列表", description = "查询继续教育学分记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherEducationCredit>> listCredits(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "学年度") @RequestParam(required = false) String academicYear) {

        List<TeacherEducationCredit> list;
        if (academicYear != null) {
            list = teacherEducationCreditService.listByAcademicYear(teacherId, academicYear);
        } else {
            list = teacherEducationCreditService.listByTeacherId(teacherId);
        }
        return ApiResult.success(list);
    }

    @PostMapping("/credit/add")
    @Operation(summary = "新增学分记录", description = "新增继续教育学分记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addCredit(@RequestBody TeacherEducationCredit credit) {
        try {
            boolean result = teacherEducationCreditService.addEducationCredit(credit);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @GetMapping("/credit/statistics")
    @Operation(summary = "学分统计", description = "统计继续教育学分")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Object>> statisticsCredits(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        Map<String, Object> result = teacherEducationCreditService.statisticsCredits(teacherId);
        return ApiResult.success(result);
    }

    // ==================== 教学任务 ====================

    @GetMapping("/teaching-task/list")
    @Operation(summary = "教学任务列表", description = "查询教师教学任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherTeachingTask>> listTeachingTasks(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "学年度") @RequestParam(required = false) String academicYear,
            @Parameter(description = "学期") @RequestParam(required = false) Integer semester) {

        List<TeacherTeachingTask> list;
        if (academicYear != null && semester != null) {
            list = teacherTeachingTaskService.listByPeriod(teacherId, academicYear, semester);
        } else {
            list = teacherTeachingTaskService.listByTeacherId(teacherId);
        }
        return ApiResult.success(list);
    }

    @PostMapping("/teaching-task/add")
    @Operation(summary = "新增教学任务", description = "新增教师教学任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addTeachingTask(@RequestBody TeacherTeachingTask task) {
        try {
            boolean result = teacherTeachingTaskService.addTeachingTask(task);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @GetMapping("/teaching-task/statistics")
    @Operation(summary = "教学工作量统计", description = "统计教学工作量")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Object>> statisticsTeachingTasks(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        Map<String, Object> result = teacherTeachingTaskService.statisticsWorkload(teacherId);
        return ApiResult.success(result);
    }

    // ==================== 教研活动 ====================

    @GetMapping("/research/list")
    @Operation(summary = "教研活动列表", description = "查询教师教研活动")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherResearchActivity>> listResearchActivities(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "活动类型") @RequestParam(required = false) String activityType) {

        List<TeacherResearchActivity> list;
        if (activityType != null) {
            list = teacherResearchActivityService.listByActivityType(teacherId, activityType);
        } else {
            list = teacherResearchActivityService.listByTeacherId(teacherId);
        }
        return ApiResult.success(list);
    }

    @PostMapping("/research/add")
    @Operation(summary = "新增教研活动", description = "新增教师教研活动")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addResearchActivity(@RequestBody TeacherResearchActivity activity) {
        try {
            boolean result = teacherResearchActivityService.addResearchActivity(activity);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @GetMapping("/research/statistics")
    @Operation(summary = "教研活动统计", description = "统计教研活动")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Map<String, Long>> statisticsResearch(@Parameter(description = "教师ID") @RequestParam Long teacherId) {
        Map<String, Long> result = teacherResearchActivityService.statisticsActivities(teacherId);
        return ApiResult.success(result);
    }

    // ==================== 工作量统计 ====================

    @GetMapping("/workload/list")
    @Operation(summary = "工作量统计列表", description = "查询教师工作量统计")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherWorkloadStatistics>> listWorkloadStatistics(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "统计年度") @RequestParam(required = false) String statisticalYear,
            @Parameter(description = "统计周期：semester-学期，year-学年") @RequestParam(required = false) String statisticalPeriod) {

        List<TeacherWorkloadStatistics> list;
        if (statisticalYear != null && statisticalPeriod != null) {
            TeacherWorkloadStatistics workload = teacherWorkloadStatisticsService.getByPeriod(teacherId, statisticalYear, statisticalPeriod);
            list = workload != null ? List.of(workload) : List.of();
        } else {
            list = teacherWorkloadStatisticsService.listByTeacherId(teacherId);
        }
        return ApiResult.success(list);
    }

    @PostMapping("/workload/add")
    @Operation(summary = "新增工作量统计", description = "新增教师工作量统计")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Boolean> addWorkloadStatistics(@RequestBody TeacherWorkloadStatistics statistics) {
        try {
            boolean result = teacherWorkloadStatisticsService.addWorkloadStatistics(statistics);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("新增失败: " + e.getMessage());
        }
    }

    @GetMapping("/workload/trend")
    @Operation(summary = "工作量趋势", description = "查询工作量趋势")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<TeacherWorkloadStatistics>> getWorkloadTrend(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "年数") @RequestParam(defaultValue = "5") int years) {

        List<TeacherWorkloadStatistics> result = teacherWorkloadStatisticsService.getWorkloadTrend(teacherId, years);
        return ApiResult.success(result);
    }

    // ==================== 综合查询 ====================

    @GetMapping("/comprehensive/{teacherId}")
    @Operation(summary = "教师综合信息", description = "获取教师完整信息（所有模块）")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<Map<String, Object>> getComprehensiveInfo(@PathVariable Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        // 基础信息
        TeacherBasic basic = teacherBasicService.getByTeacherId(teacherId);
        result.put("basic", basic);

        if (basic != null) {
            // 岗位信息
            TeacherPosition position = teacherPositionService.getCurrentPosition(teacherId);
            result.put("position", position);

            // 教育背景
            result.put("educations", teacherEducationService.listByTeacherId(teacherId));

            // 资格职称
            result.put("qualifications", teacherQualificationService.listByTeacherId(teacherId));

            // 荣誉称号
            result.put("honors", teacherHonorService.listByTeacherId(teacherId));

            // 考核记录
            result.put("assessments", teacherAssessmentService.listByTeacherId(teacherId));

            // 奖惩记录
            result.put("rewardPunishments", teacherRewardPunishmentService.listByTeacherId(teacherId));

            // 师德考核
            result.put("ethics", teacherEthicsService.listByTeacherId(teacherId));

            // 培训记录
            result.put("trainings", teacherTrainingRecordService.listByTeacherId(teacherId));

            // 学分记录
            result.put("credits", teacherEducationCreditService.listByTeacherId(teacherId));

            // 教学任务
            result.put("teachingTasks", teacherTeachingTaskService.listByTeacherId(teacherId));

            // 教研活动
            result.put("researchActivities", teacherResearchActivityService.listByTeacherId(teacherId));

            // 工作量统计
            result.put("workloadStatistics", teacherWorkloadStatisticsService.listByTeacherId(teacherId));
        }

        return ApiResult.success(result);
    }
}
