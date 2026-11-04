package com.example.data

object DefaultBoards {
    const val BOARD_WORK_ID = "00000000-0000-4000-a000-000000000001"
    const val BOARD_STUDY_ID = "00000000-0000-4000-a000-000000000002"
    const val BOARD_PLAY_ID = "00000000-0000-4000-a000-000000000003"
    const val BOARD_MEETING_ID = "00000000-0000-4000-a000-000000000004"
    const val BOARD_HOLIDAY_ID = "00000000-0000-4000-a000-000000000005"
    const val BOARD_PERSONAL_ID = "00000000-0000-4000-a000-000000000006"
    const val BOARD_HEALTH_ID = "00000000-0000-4000-a000-000000000007"
    const val BOARD_FINANCE_ID = "00000000-0000-4000-a000-000000000008"
    const val BOARD_TASK_ID = "00000000-0000-4000-a000-000000000009"
    const val BOARD_FAMILY_ID = "00000000-0000-4000-a000-000000000010"

    fun getDefaultBoardUuid(id: Int): String {
        return when (id) {
            1 -> BOARD_WORK_ID
            2 -> BOARD_STUDY_ID
            3 -> BOARD_PLAY_ID
            4 -> BOARD_MEETING_ID
            5 -> BOARD_HOLIDAY_ID
            6 -> BOARD_PERSONAL_ID
            7 -> BOARD_HEALTH_ID
            8 -> BOARD_FINANCE_ID
            9 -> BOARD_TASK_ID
            10 -> BOARD_FAMILY_ID
            else -> String.format("00000000-0000-4000-a000-%012d", id)
        }
    }

    fun getDefaultBoardInfo(id: Int): Pair<String, String> {
        return when (id) {
            1 -> Pair("Công việc", "#3B82F6")
            2 -> Pair("Học tập", "#10B981")
            3 -> Pair("Đi chơi", "#06B6D4")
            4 -> Pair("Cuộc họp", "#F59E0B")
            5 -> Pair("Ngày lễ", "#EF4444")
            6 -> Pair("Cá nhân", "#8B5CF6")
            7 -> Pair("Sức khỏe", "#EC4899")
            8 -> Pair("Tài chính", "#14B8A6")
            9 -> Pair("Việc cần làm", "#6366F1")
            10 -> Pair("Gia đình", "#F97316")
            else -> Pair("Khác", "#64748B")
        }
    }
}
