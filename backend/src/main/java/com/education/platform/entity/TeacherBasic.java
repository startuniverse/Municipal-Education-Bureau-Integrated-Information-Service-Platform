package com.education.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.education.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 教师基本信息扩展实体类
 *
 * @author Education Platform Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher_basic")
public class TeacherBasic extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（用于管理员手动添加时关联）
     */
    private Long userId;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 教师工号
     */
    private String teacherNumber;

    /**
     * 性别: male-男, female-女
     */
    private String gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 民族
     */
    private String ethnicity;

    /**
     * 籍贯
     */
    private String nativePlace;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 家庭住址
     */
    private String address;

    /**
     * 所属学校ID
     */
    private Long schoolId;

    /**
     * 部门/院系
     */
    private String department;

    /**
     * 职务
     */
    private String position;

    /**
     * 职称
     */
    private String title;

    /**
     * 入职日期
     */
    private LocalDate hireDate;

    /**
     * 状态: 1-在职, 0-离职
     */
    private Integer status;

    /**
     * 证件照URL
     */
    private String photoUrl;

    /**
     * 工作照URL
     */
    private String workPhotoUrl;

    /**
     * 电子签名URL
     */
    private String signatureUrl;

    /**
     * 角色类型: teacher/teacher_head/department_head/admin
     */
    private String roleType;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
