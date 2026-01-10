<template>
  <div class="training-courses">
    <!-- 搜索和筛选区域 -->
    <el-card class="header-card">
      <div class="search-header">
        <h2>培训课程</h2>
        <div class="search-controls">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索课程标题或描述..."
            size="large"
            style="width: 300px;"
            clearable
            @keyup.enter="handleFilter"
            @clear="handleFilter"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" @click="handleFilter" :icon="Search">
            搜索
          </el-button>
        </div>
      </div>

      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">分类：</span>
          <el-radio-group v-model="filters.category" size="small" @change="handleFilter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="职业技能">职业技能</el-radio-button>
            <el-radio-button label="安全教育">安全教育</el-radio-button>
            <el-radio-button label="师德师风">师德师风</el-radio-button>
            <el-radio-button label="专业技能">专业技能</el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-group">
          <span class="filter-label">科目：</span>
          <el-select
            v-model="filters.subject"
            placeholder="选择科目"
            size="small"
            style="width: 120px;"
            clearable
            @change="handleFilter"
          >
            <el-option label="语文" value="语文"></el-option>
            <el-option label="数学" value="数学"></el-option>
            <el-option label="英语" value="英语"></el-option>
            <el-option label="综合" value="综合"></el-option>
          </el-select>
        </div>

        <div class="filter-group">
          <span class="filter-label">难度：</span>
          <el-select
            v-model="filters.difficultyLevel"
            placeholder="选择难度"
            size="small"
            style="width: 120px;"
            clearable
            @change="handleFilter"
          >
            <el-option label="初级" value="beginner"></el-option>
            <el-option label="中级" value="intermediate"></el-option>
            <el-option label="高级" value="advanced"></el-option>
          </el-select>
        </div>

        <div class="filter-group">
          <el-button type="info" size="small" @click="resetFilters" :icon="Refresh">
            重置
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 课程列表 -->
    <el-card class="courses-card">
      <template #header>
        <div class="card-header">
          <span>课程列表</span>
          <span class="result-count" v-if="total > 0">共 {{ total }} 个课程</span>
        </div>
      </template>

      <div v-loading="loading" class="courses-content">
        <!-- 空状态 -->
        <el-empty v-if="courses.length === 0 && !loading" description="暂无课程，请稍后再试或联系管理员"></el-empty>

        <!-- 课程卡片列表 -->
        <div v-else class="course-grid">
          <div
            v-for="course in courses"
            :key="course.id"
            class="course-card"
            @click="showCourseDetail(course)"
          >
            <div class="course-cover">
              <el-image
                :src="course.coverImage || 'https://via.placeholder.com/400x225'"
                fit="cover"
                style="width: 100%; height: 160px; border-radius: 8px;"
                :preview-src-list="course.coverImage ? [course.coverImage] : []"
              >
                <template #placeholder>
                  <div class="image-loading">加载中...</div>
                </template>
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                    <span>暂无封面</span>
                  </div>
                </template>
              </el-image>
            </div>

            <div class="course-info">
              <div class="course-title">{{ course.title }}</div>

              <div class="course-tags">
                <el-tag type="primary" size="small">{{ course.category }}</el-tag>
                <el-tag type="info" size="small">{{ course.subject }}</el-tag>
                <el-tag :type="getDifficultyType(course.difficultyLevel)" size="small">
                  {{ getDifficultyText(course.difficultyLevel) }}
                </el-tag>
              </div>

              <div class="course-description">
                {{ course.description || '暂无描述' }}
              </div>

              <div class="course-meta">
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  <span>{{ course.instructorName }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><Timer /></el-icon>
                  <span>{{ course.duration }}分钟</span>
                </div>
              </div>

              <div class="course-stats">
                <div class="stat-item">
                  <el-icon><View /></el-icon>
                  <span>{{ course.viewCount }}</span>
                </div>
                <div class="stat-item">
                  <el-icon><UserFilled /></el-icon>
                  <span>{{ course.enrollCount }}</span>
                </div>
                <div class="stat-item">
                  <el-icon><CircleCheck /></el-icon>
                  <span>{{ course.completionCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[12, 24, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 课程详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="课程详情"
      width="700px"
      destroy-on-close
    >
      <div v-if="currentCourse" class="course-detail">
        <div class="detail-cover" v-if="currentCourse.coverImage">
          <el-image
            :src="currentCourse.coverImage"
            fit="cover"
            style="width: 100%; height: 300px; border-radius: 8px; margin-bottom: 20px;"
          />
        </div>

        <div class="detail-section">
          <h3 class="detail-title">{{ currentCourse.title }}</h3>
          <div class="detail-tags">
            <el-tag type="primary">{{ currentCourse.category }}</el-tag>
            <el-tag type="info">{{ currentCourse.subject }}</el-tag>
            <el-tag :type="getDifficultyType(currentCourse.difficultyLevel)">
              {{ getDifficultyText(currentCourse.difficultyLevel) }}
            </el-tag>
            <el-tag type="warning" v-if="currentCourse.duration">{{ currentCourse.duration }}分钟</el-tag>
          </div>
        </div>

        <div class="detail-section">
          <h4>课程介绍</h4>
          <p class="detail-description">{{ currentCourse.description || '暂无描述' }}</p>
        </div>

        <div class="detail-section">
          <h4>讲师信息</h4>
          <div class="instructor-info">
            <el-avatar :size="32">{{ currentCourse.instructorName?.charAt(0) }}</el-avatar>
            <span>{{ currentCourse.instructorName }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>目标受众</h4>
          <p class="detail-text">{{ currentCourse.targetAudience || '全体教师' }}</p>
        </div>

        <div class="detail-section">
          <h4>课程统计</h4>
          <div class="stats-grid">
            <div class="stat-box">
              <div class="stat-number">{{ currentCourse.viewCount }}</div>
              <div class="stat-label">浏览次数</div>
            </div>
            <div class="stat-box">
              <div class="stat-number">{{ currentCourse.enrollCount }}</div>
              <div class="stat-label">报名人数</div>
            </div>
            <div class="stat-box">
              <div class="stat-number">{{ currentCourse.completionCount }}</div>
              <div class="stat-label">完成人数</div>
            </div>
            <div class="stat-box" v-if="ratingInfo.averageRating > 0">
              <div class="stat-number">{{ ratingInfo.averageRating }}/5</div>
              <div class="stat-label">评分 ({{ ratingInfo.totalEvaluations }}人)</div>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="currentCourse.startDate || currentCourse.endDate">
          <h4>培训时间</h4>
          <div class="time-info">
            <span v-if="currentCourse.startDate">开始：{{ formatDateTime(currentCourse.startDate) }}</span>
            <span v-if="currentCourse.endDate">结束：{{ formatDateTime(currentCourse.endDate) }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleEnroll" :disabled="isEnrolled || loading">
            {{ isEnrolled ? '已报名' : '立即报名' }}
          </el-button>
          <el-button type="info" @click="viewEvaluations" v-if="ratingInfo.totalEvaluations > 0">
            查看评价 ({{ ratingInfo.totalEvaluations }})
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 评价列表对话框 -->
    <el-dialog
      v-model="evaluationsDialogVisible"
      title="课程评价"
      width="600px"
      destroy-on-close
    >
      <div v-loading="evaluationsLoading" class="evaluations-list">
        <div v-if="evaluations.length === 0" class="empty-evaluations">
          <el-empty description="暂无评价"></el-empty>
        </div>
        <div v-else>
          <div v-for="evaluation in evaluations" :key="evaluation.id" class="evaluation-item">
            <div class="eval-header">
              <div class="eval-student">
                <el-avatar :size="24">{{ evaluation.studentName?.charAt(0) }}</el-avatar>
                <span>{{ evaluation.studentName }}</span>
              </div>
              <el-rate v-model="evaluation.rating" disabled :max="5" />
            </div>
            <div class="eval-content">{{ evaluation.comment }}</div>
            <div class="eval-time">{{ formatDateTime(evaluation.createdAt) }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="evaluationsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, User, Timer, View, UserFilled, CircleCheck, Picture } from '@element-plus/icons-vue'
import { getTrainingCourses, getCourseDetail, enrollCourse, getEvaluations, getCourseRating, getMyCourses } from '@/api/training'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const loading = ref(false)
const courses = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 搜索参数
const filters = ref({
  keyword: '',
  category: '',
  subject: '',
  difficultyLevel: ''
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentCourse = ref(null)
const isEnrolled = ref(false)
const ratingInfo = ref({ averageRating: 0, totalEvaluations: 0 })

// 评价对话框
const evaluationsDialogVisible = ref(false)
const evaluations = ref([])
const evaluationsLoading = ref(false)

// 工具函数
const getDifficultyType = (level) => {
  const map = {
    'beginner': 'success',
    'intermediate': 'warning',
    'advanced': 'danger'
  }
  return map[level] || 'info'
}

const getDifficultyText = (level) => {
  const map = {
    'beginner': '初级',
    'intermediate': '中级',
    'advanced': '高级'
  }
  return map[level] || level
}

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  const date = new Date(datetime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 数据加载
const loadCourses = async () => {
  try {
    loading.value = true
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      ...filters.value
    }

    // 移除空参数
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })

    const data = await getTrainingCourses(params)
    courses.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载课程列表失败')
    courses.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleFilter = () => {
  currentPage.value = 1
  loadCourses()
}

// 重置筛选
const resetFilters = () => {
  filters.value = {
    keyword: '',
    category: '',
    subject: '',
    difficultyLevel: ''
  }
  currentPage.value = 1
  loadCourses()
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadCourses()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadCourses()
}

// 课程操作
const showCourseDetail = async (course) => {
  try {
    loading.value = true

    // 验证用户信息
    if (!userStore.userInfo || !userStore.userInfo.id) {
      throw new Error('用户信息未加载，请重新登录')
    }

    // 获取详情（会增加浏览次数）
    const detail = await getCourseDetail(course.id)
    if (!detail) {
      throw new Error('获取课程详情失败')
    }
    currentCourse.value = detail

    // 检查是否已报名 - 处理可能的空值情况
    const myCoursesRes = await getMyCourses({ studentId: userStore.userInfo.id })
    isEnrolled.value = Array.isArray(myCoursesRes)
      ? myCoursesRes.some(c => c.course && c.course.id === course.id)
      : false

    // 获取评分信息 - 处理可能的空值情况
    const ratingRes = await getCourseRating(course.id)
    ratingInfo.value = ratingRes || { averageRating: 0, totalEvaluations: 0 }

    detailDialogVisible.value = true
  } catch (error) {
    console.error('课程详情加载失败:', error)
    // 根据错误信息提供更具体的提示
    if (error.message && error.message.includes('课程不存在')) {
      ElMessage.error('课程不存在或已被删除')
    } else if (error.message && error.message.includes('课程未发布')) {
      ElMessage.error('课程尚未发布')
    } else if (error.message && error.message.includes('用户信息')) {
      ElMessage.error('用户信息未加载，请重新登录')
    } else if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error('加载课程详情失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const handleEnroll = async () => {
  try {
    loading.value = true
    await enrollCourse({
      courseId: currentCourse.value.id,
      studentId: userStore.userInfo.id,
      studentName: userStore.userInfo.realName
    })
    ElMessage.success('报名成功！')
    isEnrolled.value = true
    // 更新列表数据
    loadCourses()
  } catch (error) {
    // 错误已在request.js中处理，这里只捕获异常避免崩溃
    console.error('报名失败:', error)
  } finally {
    loading.value = false
  }
}

// 查看评价
const viewEvaluations = async () => {
  try {
    evaluationsLoading.value = true
    const res = await getEvaluations({ courseId: currentCourse.value.id })
    evaluations.value = res
    evaluationsDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载评价失败')
  } finally {
    evaluationsLoading.value = false
  }
}

// 页面挂载时加载数据
onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.training-courses {
  width: 100%;
}

.header-card {
  margin-bottom: 20px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.search-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.search-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-row {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.courses-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-count {
  font-size: 14px;
  color: #909399;
}

.courses-content {
  min-height: 300px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.course-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.course-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
  transform: translateY(-4px);
}

.course-cover {
  width: 100%;
  height: 160px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-loading, .image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.image-error {
  gap: 8px;
}

.course-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 44px;
}

.course-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.course-description {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.course-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.course-stats {
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* 详情对话框样式 */
.course-detail {
  padding: 10px 0;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-title {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

.detail-description {
  margin: 0;
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
}

.detail-text {
  margin: 0;
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
}

.instructor-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #303133;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-box {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
}

.stat-number {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.time-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 评价列表样式 */
.evaluations-list {
  max-height: 400px;
  overflow-y: auto;
}

.evaluation-item {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
}

.evaluation-item:last-child {
  border-bottom: none;
}

.eval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.eval-student {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.eval-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 8px;
}

.eval-time {
  font-size: 12px;
  color: #909399;
}

.empty-evaluations {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-header {
    flex-direction: column;
    align-items: stretch;
  }

  .search-controls {
    flex-direction: column;
  }

  .search-controls .el-input {
    width: 100% !important;
  }

  .course-grid {
    grid-template-columns: 1fr;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    justify-content: space-between;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
