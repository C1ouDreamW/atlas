package cn.heycloudream.quiz_backend.util;

import cn.heycloudream.quiz_backend.common.constants.ValidationConstants;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文档解析工具：将上传的非结构化文件（.txt / .pdf / .docx）在内存中提取为纯文本。
 * <p>
 * 所有流操作均通过 {@code try-with-resources} 管理，确保资源安全关闭。
 * </p>
 *
 * @author atlas
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentParseUtils {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "pdf", "docx");

    /**
     * 根据文件扩展名路由到对应解析器，返回提取的纯文本。
     *
     * @param file 上传的文件（不能为空）
     * @return 从文档中提取的纯文本字符串
     * @throws BusinessException 文件为空、格式不支持或解析失败
     */
    public static String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String extension = getExtension(originalFilename);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "不支持的文件格式：" + extension + "，仅支持 txt / pdf / docx");
        }

        // 文件大小校验（二重防线）
        if (file.getSize() > ValidationConstants.FILE_IMPORT_MAX_SIZE_BYTES) {
            throw new BusinessException(400, "文件过大，最大支持 10 MB");
        }

        log.info("[文档解析] 开始解析文件: {} (大小: {} bytes, 类型: {})",
                originalFilename, file.getSize(), extension);

        return switch (extension) {
            case "txt" -> extractFromTxt(file);
            case "pdf" -> extractFromPdf(file);
            case "docx" -> extractFromDocx(file);
            default -> throw new BusinessException(400, "不支持的文件格式：" + extension);
        };
    }

    // ---- 格式路由器 ----

    /**
     * 提取 .txt 纯文本（UTF-8）。
     */
    private static String extractFromTxt(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String text = reader.lines().collect(Collectors.joining("\n"));
            if (text.isBlank()) {
                throw new BusinessException(400, "文件内容为空，无法解析");
            }
            log.info("[文档解析] .txt 解析完成，提取长度: {} 字符", text.length());
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[文档解析] .txt 文件读取失败", e);
            throw new BusinessException(500, "文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 提取 .pdf 纯文本（PDFBox PDFTextStripper）。
     */
    private static String extractFromPdf(MultipartFile file) {
        try (var document = Loader.loadPDF(file.getBytes())) {
            var stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BusinessException(400, "PDF 文件无可提取的文本内容（可能是扫描件或图片型 PDF）");
            }
            log.info("[文档解析] .pdf 解析完成，提取长度: {} 字符", text.length());
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[文档解析] .pdf 文件解析失败", e);
            throw new BusinessException(500, "PDF 解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取 .docx 纯文本（Apache POI XWPFWordExtractor）。
     */
    private static String extractFromDocx(MultipartFile file) {
        try (var xwpfDoc = new XWPFDocument(file.getInputStream());
             var extractor = new XWPFWordExtractor(xwpfDoc)) {
            String text = extractor.getText();
            if (text == null || text.isBlank()) {
                throw new BusinessException(400, "Word 文件内容为空，无法解析");
            }
            log.info("[文档解析] .docx 解析完成，提取长度: {} 字符", text.length());
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[文档解析] .docx 文件解析失败", e);
            throw new BusinessException(500, "Word 文档解析失败: " + e.getMessage());
        }
    }

    // ---- 辅助方法 ----

    /**
     * 从文件名提取小写扩展名（不含点号）。
     */
    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 从字节数组解析文档（供 Stream Consumer 从本地文件读取后调用）。
     *
     * @param content   文件字节内容
     * @param extension 文件扩展名（如 pdf、docx、txt），不含点号
     * @return 提取的纯文本
     */
    public static String extractFromBytes(byte[] content, String extension) {
        if (content == null || content.length == 0) {
            throw new BusinessException(400, "文件内容为空，无法解析");
        }
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "不支持的文件格式：" + extension);
        }

        return switch (extension) {
            case "txt" -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
                    yield reader.lines().collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    throw new BusinessException(500, "文件读取失败: " + e.getMessage());
                }
            }
            case "pdf" -> {
                try (var document = Loader.loadPDF(content)) {
                    var stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    yield stripper.getText(document);
                } catch (IOException e) {
                    throw new BusinessException(500, "PDF 解析失败: " + e.getMessage());
                }
            }
            case "docx" -> {
                try (var xwpfDoc = new XWPFDocument(new ByteArrayInputStream(content));
                     var extractor = new XWPFWordExtractor(xwpfDoc)) {
                    yield extractor.getText();
                } catch (IOException e) {
                    throw new BusinessException(500, "Word 文档解析失败: " + e.getMessage());
                }
            }
            default -> throw new BusinessException(400, "不支持的文件格式：" + extension);
        };
    }
}
