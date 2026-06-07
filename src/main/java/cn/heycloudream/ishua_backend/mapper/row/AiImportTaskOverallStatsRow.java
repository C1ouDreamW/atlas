package cn.heycloudream.ishua_backend.mapper.row;

import lombok.Data;

/**
 * {@code ai_import_task} 窗口内整体聚合查询行。
 *
 * @author atlas
 */
@Data
public class AiImportTaskOverallStatsRow {

    private Long totalCount;

    private Double avgParseSec;

    private Double avgQuestionCount;

    private Long failedCount;
}
