package cn.heycloudream.ishua_backend.service.impl;

import cn.heycloudream.ishua_backend.enums.AiImportTaskStatus;
import cn.heycloudream.ishua_backend.service.AiImportTaskService;
import cn.heycloudream.ishua_backend.service.ai.AiImportTaskMetaStore;
import cn.heycloudream.ishua_backend.service.ai.AiImportTaskStatusStore;
import cn.heycloudream.ishua_backend.service.file.FileStorageService;
import cn.heycloudream.ishua_backend.service.guard.BankAccessGuard;
import cn.heycloudream.ishua_backend.vo.ai.AiImportSubmitVO;
import cn.heycloudream.ishua_backend.vo.ai.AiImportTaskMetaVO;
import cn.heycloudream.streamtask.api.StreamTaskTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuestionImportServiceImplTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private StreamTaskTemplate streamTaskTemplate;

    @Mock
    private AiImportTaskStatusStore taskStatusStore;

    @Mock
    private AiImportTaskMetaStore taskMetaStore;

    @Mock
    private AiImportTaskService aiImportTaskService;

    @Mock
    private BankAccessGuard bankAccessGuard;

    @InjectMocks
    private AiQuestionImportServiceImpl service;

    @Test
    void submitFileImport_shouldPublishStreamTaskEnvelope() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        when(fileStorageService.store(file)).thenReturn("file:///tmp/demo.pdf");

        AiImportSubmitVO result = service.submitFileImport(1L, 10L, file);

        ArgumentCaptor<String> taskTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> taskIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AiImportTaskMetaVO> metaCaptor = ArgumentCaptor.forClass(AiImportTaskMetaVO.class);
        verify(streamTaskTemplate).publish(taskTypeCaptor.capture(), taskIdCaptor.capture(), metaCaptor.capture());

        AiImportTaskMetaVO meta = metaCaptor.getValue();
        assertThat(taskTypeCaptor.getValue()).isEqualTo("ai.import.file");
        assertThat(result.getStatus()).isEqualTo(AiImportTaskStatus.SUBMITTED.name());
        assertThat(result.getTaskId()).isEqualTo(meta.getTaskId());
        assertThat(taskIdCaptor.getValue()).isEqualTo(meta.getTaskId());
        assertThat(meta.getUserId()).isEqualTo(1L);
        assertThat(meta.getBankId()).isEqualTo(10L);
        assertThat(meta.getFileName()).isEqualTo("demo.pdf");
        assertThat(meta.getFileUrl()).isEqualTo("file:///tmp/demo.pdf");
        assertThat(meta.getFileSize()).isEqualTo(file.getSize());
        assertThat(meta.getType()).isEqualTo("file");
        assertThat(meta.getSubmittedAt()).isPositive();

        verify(aiImportTaskService).createOnSubmit(any());
        verify(taskMetaStore).write(eq(result.getTaskId()), any());
        verify(taskStatusStore).write(result.getTaskId(), AiImportTaskStatus.SUBMITTED, null, null);
    }
}
