package com.education.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.education.platform.entity.Teacher;
import com.education.platform.mapper.TeacherMapper;
import com.education.platform.service.ITeacherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 教师服务实现
 *
 * @author Education Platform Team
 */
@Slf4j
@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements ITeacherService {
}
