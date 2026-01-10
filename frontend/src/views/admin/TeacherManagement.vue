<template>
  <div class="teacher-management">
    <!-- 顶部操作栏 -->
    <div class="header-bar">
      <div class="title">教师信息管理</div>
      <div class="actions">
        <el-button type="primary" @click="handleAdd" :icon="Plus">
          新增教师
        </el-button>
        <el-button type="success" @click="handleBatchImport" :icon="Upload">
          批量导入
        </el-button>
        <el-input
          v-model="searchParams.keyword"
          placeholder="搜索教师姓名"
          :prefix-icon="Search"
          style="width: 240px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
        <el-button @click="handleReset" :icon="Refresh">重置</el-button>
      </div>
    </div>

    <!-- 教师列表 -->
    <div class="content-card">
      <el-table
        v-loading="loading"
        :data="teacherList"
        border
        stripe
        style="width: 100%"
        height="calc(100vh - 240px)"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="teacherName" label="姓名" width="120" align="center" />
        <el-table-column prop="teacherNumber" label="工号" width="120" align="center" />
        <el-table-column prop="department" label="部门" width="140" align="center" />
        <el-table-column prop="position" label="职务" width="140" align="center" />
        <el-table-column prop="title" label="职称" width="120" align="center" />
        <el-table-column prop="phone" label="联系电话" width="130" align="center" />
        <el-table-column prop="email" label="邮箱" width="180" align="center" />
        <el-table-column prop="hireDate" label="入职日期" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.hireDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="280" align="center">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" @click="handleView(row)" :icon="View">
                查看
              </el-button>
              <el-button size="small" type="primary" @click="handleEdit(row)" :icon="Edit">
                编辑
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)" :icon="Delete">
                删除
              </el-button>
            </el-button-group>
            <el-button size="small" type="warning" @click="handleDetail(row)" :icon="Document" style="margin-left: 8px">
              详细信息
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="searchParams.current"
          v-model:page-size="searchParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        :disabled="dialogType === 'view'"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名" prop="teacherName">
              <el-input v-model="formData.teacherName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工号" prop="teacherNumber">
              <el-input v-model="formData.teacherNumber" placeholder="请输入工号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="formData.gender" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="male" />
                <el-option label="女" value="female" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="formData.birthDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-input v-model="formData.department" placeholder="请输入部门" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职务" prop="position">
              <el-input v-model="formData.position" placeholder="请输入职务" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-input v-model="formData.title" placeholder="请输入职称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期" prop="hireDate">
              <el-date-picker
                v-model="formData.hireDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="formData.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="在职" :value="1" />
                <el-option label="离职" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remarks">
          <el-input
            v-model="formData.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button v-if="dialogType !== 'view'" type="primary" @click="handleSubmit">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 教师详细信息对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="教师详细信息"
      width="900px"
      fullscreen
      destroy-on-close
    >
      <div v-if="selectedTeacher" class="teacher-detail">
        <!-- 基本信息卡片 -->
        <el-card class="box-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
            </div>
          </template>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">姓名：</span>
              <span class="value">{{ selectedTeacher.teacherName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">工号：</span>
              <span class="value">{{ selectedTeacher.teacherNumber }}</span>
            </div>
            <div class="detail-item">
              <span class="label">性别：</span>
              <span class="value">{{ selectedTeacher.gender === 'male' ? '男' : '女' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">出生日期：</span>
              <span class="value">{{ formatDate(selectedTeacher.birthDate) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">部门：</span>
              <span class="value">{{ selectedTeacher.department }}</span>
            </div>
            <div class="detail-item">
              <span class="label">职务：</span>
              <span class="value">{{ selectedTeacher.position }}</span>
            </div>
            <div class="detail-item">
              <span class="label">职称：</span>
              <span class="value">{{ selectedTeacher.title }}</span>
            </div>
            <div class="detail-item">
              <span class="label">入职日期：</span>
              <span class="value">{{ formatDate(selectedTeacher.hireDate) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">联系电话：</span>
              <span class="value">{{ selectedTeacher.phone }}</span>
            </div>
            <div class="detail-item">
              <span class="label">邮箱：</span>
              <span class="value">{{ selectedTeacher.email }}</span>
            </div>
            <div class="detail-item">
              <span class="label">身份证号：</span>
              <span class="value">{{ selectedTeacher.idCard }}</span>
            </div>
            <div class="detail-item">
              <span class="label">状态：</span>
              <span class="value">
                <el-tag :type="selectedTeacher.status === 1 ? 'success' : 'danger'">
                  {{ selectedTeacher.status === 1 ? '在职' : '离职' }}
                </el-tag>
              </span>
            </div>
          </div>
        </el-card>

        <!-- 详细信息选项卡 -->
        <el-tabs v-model="activeTab" type="card" class="detail-tabs" style="margin-top: 16px;">
          <el-tab-pane label="教育背景" name="education">
            <teacher-education :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="资格职称" name="qualification">
            <teacher-qualification :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="荣誉称号" name="honor">
            <teacher-honor :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="考核记录" name="assessment">
            <teacher-assessment :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="奖惩记录" name="reward">
            <teacher-reward-punishment :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="师德考核" name="ethics">
            <teacher-ethics :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="培训记录" name="training">
            <teacher-training :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="继续教育" name="credit">
            <teacher-credit :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="教学任务" name="teaching">
            <teacher-teaching :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="教研活动" name="research">
            <teacher-research :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
          <el-tab-pane label="工作量统计" name="workload">
            <teacher-workload :teacher-id="selectedTeacher.id" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, View, Search, Refresh, Document, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import {
  getTeacherBasicList,
  addTeacherBasic,
  updateTeacherBasic,
  deleteTeacherBasic
} from '@/api/teacherInfo'

// 子组件（简化版本，实际可以单独创建文件）
import TeacherEducation from './detail/TeacherEducation.vue'
import TeacherQualification from './detail/TeacherQualification.vue'
import TeacherHonor from './detail/TeacherHonor.vue'
import TeacherAssessment from './detail/TeacherAssessment.vue'
import TeacherRewardPunishment from './detail/TeacherRewardPunishment.vue'
import TeacherEthics from './detail/TeacherEthics.vue'
import TeacherTraining from './detail/TeacherTraining.vue'
import TeacherCredit from './detail/TeacherCredit.vue'
import TeacherTeaching from './detail/TeacherTeaching.vue'
import TeacherResearch from './detail/TeacherResearch.vue'
import TeacherWorkload from './detail/TeacherWorkload.vue'

// 加载状态
const loading = ref(false)

// 用户store
const userStore = useUserStore()

// 搜索参数
const searchParams = reactive({
  current: 1,
  size: 10,
  keyword: '',
  schoolId: null
})

// 教师列表
const teacherList = ref([])
const total = ref(0)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref('add') // add, edit, view
const formRef = ref(null)

// 详细信息
const detailDialogVisible = ref(false)
const selectedTeacher = ref(null)
const activeTab = ref('education')

// 表单数据
const formData = reactive({
  id: null,
  teacherName: '',
  teacherNumber: '',
  gender: '',
  birthDate: '',
  department: '',
  position: '',
  title: '',
  hireDate: '',
  phone: '',
  email: '',
  idCard: '',
  status: 1,
  remarks: ''
})

// 表单验证规则
const formRules = {
  teacherName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  teacherNumber: [
    { required: true, message: '请输入工号', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  department: [
    { required: true, message: '请输入部门', trigger: 'blur' }
  ],
  title: [
    { required: true, message: '请输入职称', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' }
  ]
}

// 获取教师列表
const fetchTeacherList = async () => {
  loading.value = true
  try {
    const params = {
      current: searchParams.current,
      size: searchParams.size,
      keyword: searchParams.keyword || undefined,
      schoolId: searchParams.schoolId || undefined
    }
    console.log('=== 获取教师列表参数 ===')
    console.log('当前用户:', userStore.userInfo)
    console.log('搜索参数:', params)

    // request.js的响应拦截器已经返回了res.data（PageResult对象）
    const pageResult = await getTeacherBasicList(params)
    console.log('后端返回的PageResult:', pageResult)

    // 直接访问PageResult的属性
    teacherList.value = pageResult.records || []
    total.value = pageResult.total || 0

    console.log('教师列表数据:', teacherList.value)
    console.log('总数:', total.value)

    if (teacherList.value.length === 0) {
      console.log('教师列表为空，可能原因：')
      console.log('1. 数据库中没有符合条件的教师记录')
      console.log('2. schoolId过滤条件过于严格')
      console.log('3. 教师记录的deleted字段为1')
      ElMessage.warning('暂无教师数据')
    } else {
      ElMessage.success(`成功加载 ${total.value} 条教师记录`)
    }
  } catch (error) {
    console.error('获取教师列表异常:', error)
    ElMessage.error('获取教师列表失败: ' + (error.response?.data?.message || error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  searchParams.current = 1
  fetchTeacherList()
}

// 重置
const handleReset = () => {
  searchParams.keyword = ''
  searchParams.current = 1
  fetchTeacherList()
}

// 分页
const handleSizeChange = (val) => {
  searchParams.size = val
  fetchTeacherList()
}

const handleCurrentChange = (val) => {
  searchParams.current = val
  fetchTeacherList()
}

// 新增
const handleAdd = () => {
  dialogType.value = 'add'
  dialogTitle.value = '新增教师'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogType.value = 'edit'
  dialogTitle.value = '编辑教师'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 查看
const handleView = (row) => {
  dialogType.value = 'view'
  dialogTitle.value = '查看教师'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 详细信息
const handleDetail = (row) => {
  selectedTeacher.value = row
  activeTab.value = 'education'
  detailDialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除教师【${row.teacherName}】吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteTeacherBasic(row.id)
      ElMessage.success('删除成功')
      fetchTeacherList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message || '未知错误'))
    }
  }).catch(() => {})
}

// 批量导入
const handleBatchImport = () => {
  ElMessage.info('批量导入功能待实现')
  // 这里可以实现文件上传和解析逻辑
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 准备提交数据，添加schoolId
        const submitData = {
          ...formData,
          schoolId: userStore.userInfo?.schoolId || null
        }

        console.log('=== 提交教师数据 ===')
        console.log('提交数据:', submitData)
        console.log('当前用户:', userStore.userInfo)

        if (dialogType.value === 'add') {
          await addTeacherBasic(submitData)
        } else {
          await updateTeacherBasic(submitData)
        }

        ElMessage.success(dialogType.value === 'add' ? '新增成功' : '更新成功')
        dialogVisible.value = false
        fetchTeacherList()
      } catch (error) {
        console.error('提交失败:', error)
        ElMessage.error((dialogType.value === 'add' ? '新增失败' : '更新失败') + ': ' + (error.response?.data?.message || error.message || '未知错误'))
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.teacherName = ''
  formData.teacherNumber = ''
  formData.gender = ''
  formData.birthDate = ''
  formData.department = ''
  formData.position = ''
  formData.title = ''
  formData.hireDate = ''
  formData.phone = ''
  formData.email = ''
  formData.idCard = ''
  formData.status = 1
  formData.remarks = ''
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return date
}

onMounted(async () => {
  // 确保用户信息已加载
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  // 根据用户角色设置schoolId过滤条件
  if (userStore.userInfo && userStore.userInfo.schoolId) {
    // 如果是学校管理员，只显示本校教师
    searchParams.schoolId = userStore.userInfo.schoolId
  } else if (userStore.hasRole('ADMIN')) {
    // 如果是系统管理员，显示所有学校教师
    searchParams.schoolId = null
  }

  fetchTeacherList()
})
</script>

<style scoped>
.teacher-management {
  padding: 0;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.content-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

/* 详细信息样式 */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item .label {
  font-weight: 600;
  color: #606266;
  min-width: 100px;
}

.detail-item .value {
  color: #303133;
  flex: 1;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.box-card {
  margin-bottom: 16px;
}

.detail-tabs :deep(.el-tabs__content) {
  padding: 16px 0;
  background: #fff;
  border-radius: 4px;
}

:deep(.el-table) {
  border-radius: 8px;
}

:deep(.el-table__header-wrapper) {
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
}

:deep(.el-table__header-wrapper th) {
  color: #ffffff !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%) !important;
  border-color: #337ecc !important;
}

:deep(.el-table__header-wrapper .cell) {
  color: #ffffff !important;
  font-weight: 600 !important;
}

:deep(.el-table__body-wrapper .el-table__row:hover) {
  background-color: #f5f7fa !important;
}
</style>
