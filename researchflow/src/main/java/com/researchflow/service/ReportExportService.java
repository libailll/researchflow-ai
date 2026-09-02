package com.researchflow.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.researchflow.common.ErrorCode;
import com.researchflow.config.ReportExportProperties;
import com.researchflow.exception.BusinessException;
import com.researchflow.vo.DocumentSummaryVO;
import com.researchflow.vo.ExportedFile;
import com.researchflow.vo.ProjectRiskReportVO;
import com.researchflow.vo.ProjectVO;
import com.researchflow.vo.WeeklyReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DOCX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String WORD_FONT = "Microsoft YaHei";

    private final WeeklyReportService weeklyReportService;
    private final DocumentSummaryService documentSummaryService;
    private final ProjectRiskReportService riskReportService;
    private final ProjectService projectService;
    private final ReportExportProperties properties;

    public ExportedFile exportWeeklyReport(Long reportId, String requestedFormat) {
        WeeklyReportVO report = weeklyReportService.detail(reportId);
        ProjectVO project = projectService.getProject(report.getProjectId());
        Map<String, String> metadata = baseMetadata(project.getName(), report.getCreatorName(), report.getModel(), report.getCreatedAt());
        metadata.put("统计周期", report.getPeriodStart() + " 至 " + report.getPeriodEnd());
        List<String> sources = report.getSources().stream().map(source ->
                source.documentName() + pageSuffix(source.pageNumber())).toList();
        return export(new ReportPayload(report.getTitle(), "项目周报", metadata, report.getContent(), sources), requestedFormat);
    }

    public ExportedFile exportDocumentSummary(Long summaryId, String requestedFormat) {
        DocumentSummaryVO summary = documentSummaryService.detail(summaryId);
        ProjectVO project = projectService.getProject(summary.getProjectId());
        Map<String, String> metadata = baseMetadata(project.getName(), summary.getCreatorName(), summary.getModel(), summary.getCreatedAt());
        metadata.put("原始文档", summary.getDocumentName());
        List<String> sources = summary.getSources().stream().map(source ->
                (source.pageNumber() == null ? "无页码" : "第 " + source.pageNumber() + " 页")
                        + " · 片段 " + (source.chunkIndex() + 1)).toList();
        return export(new ReportPayload(summary.getTitle(), "AI 文档总结", metadata, summary.getContent(), sources), requestedFormat);
    }

    public ExportedFile exportRiskReport(Long reportId, String requestedFormat) {
        ProjectRiskReportVO report = riskReportService.detail(reportId);
        ProjectVO project = projectService.getProject(report.getProjectId());
        Map<String, String> metadata = baseMetadata(project.getName(), report.getCreatorName(), report.getModel(), report.getCreatedAt());
        metadata.put("风险等级", riskLevelLabel(report.getRiskLevel().name()));
        metadata.put("风险评分", report.getRiskScore() + " / 100");
        appendRiskMetrics(metadata, report.getAnalysisSnapshot());
        List<String> sources = report.getSources().stream().map(source ->
                source.documentName() + pageSuffix(source.pageNumber())).toList();
        return export(new ReportPayload(report.getTitle(), "项目风险分析报告", metadata, report.getContent(), sources), requestedFormat);
    }

    private ExportedFile export(ReportPayload payload, String requestedFormat) {
        ExportFormat format = ExportFormat.parse(requestedFormat);
        try {
            byte[] bytes = format == ExportFormat.PDF ? createPdf(payload) : createDocx(payload);
            String fileName = safeFileName(payload.title()) + "." + format.extension;
            log.info("Report exported: type={}, title={}, format={}, bytes={}",
                    payload.type(), payload.title(), format.extension, bytes.length);
            return new ExportedFile(bytes, fileName, format.contentType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Report export failed: type={}, title={}, format={}",
                    payload.type(), payload.title(), format.extension, e);
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }

    private byte[] createPdf(ReportPayload payload) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter.getInstance(document, output);
            document.addTitle(payload.title());
            document.addAuthor("ResearchFlow AI");
            document.addCreator("ResearchFlow AI");
            document.open();

            BaseFont baseFont = BaseFont.createFont(resolvePdfFont(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titleFont = new Font(baseFont, 22, Font.BOLD, new Color(29, 29, 31));
            Font typeFont = new Font(baseFont, 10, Font.NORMAL, new Color(0, 113, 227));
            Font headingFont = new Font(baseFont, 15, Font.BOLD, new Color(29, 29, 31));
            Font bodyFont = new Font(baseFont, 10.5f, Font.NORMAL, new Color(60, 60, 64));
            Font metaFont = new Font(baseFont, 9, Font.NORMAL, new Color(110, 110, 115));

            Paragraph type = new Paragraph(payload.type(), typeFont);
            type.setSpacingAfter(8);
            document.add(type);
            Paragraph title = new Paragraph(payload.title(), titleFont);
            title.setLeading(29);
            title.setSpacingAfter(18);
            document.add(title);
            for (Map.Entry<String, String> entry : payload.metadata().entrySet()) {
                Paragraph meta = new Paragraph(entry.getKey() + "：" + safe(entry.getValue()), metaFont);
                meta.setLeading(14);
                document.add(meta);
            }
            Paragraph divider = new Paragraph(" ", bodyFont);
            divider.setSpacingAfter(12);
            document.add(divider);

            for (MarkdownBlock block : parseMarkdown(payload.content())) {
                Font font = block.heading() ? headingFont : bodyFont;
                String text = block.bullet() ? "•  " + block.text() : block.text();
                Paragraph paragraph = new Paragraph(text, font);
                paragraph.setLeading(block.heading() ? 21 : 17);
                paragraph.setSpacingBefore(block.heading() ? 12 : 0);
                paragraph.setSpacingAfter(block.heading() ? 7 : 6);
                document.add(paragraph);
            }
            addPdfSources(document, payload.sources(), headingFont, metaFont);
            document.close();
            return output.toByteArray();
        }
    }

    private byte[] createDocx(ReportPayload payload) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurePage(document);
            addWordParagraph(document, payload.type(), 10, false, "0071E3", 0, 90);
            XWPFParagraph titleParagraph = addWordParagraph(document, payload.title(), 22, true, "1D1D1F", 0, 220);
            titleParagraph.setKeepNext(true);

            XWPFTable table = document.createTable(payload.metadata().size(), 2);
            table.setWidth("100%");
            int rowIndex = 0;
            for (Map.Entry<String, String> entry : payload.metadata().entrySet()) {
                XWPFTableRow row = table.getRow(rowIndex++);
                setCellText(row.getCell(0), entry.getKey(), true);
                setCellText(row.getCell(1), safe(entry.getValue()), false);
            }
            XWPFParagraph gap = document.createParagraph();
            gap.setSpacingAfter(90);

            for (MarkdownBlock block : parseMarkdown(payload.content())) {
                XWPFParagraph paragraph = addWordParagraph(document, block.text(),
                        block.heading() ? 15 : 10, block.heading(), block.heading() ? "1D1D1F" : "3C3C40",
                        block.heading() ? 180 : 0, block.heading() ? 90 : 70);
                if (block.bullet()) {
                    paragraph.setIndentationLeft(260);
                    paragraph.setFirstLineIndent(-180);
                    XWPFRun firstRun = paragraph.getRuns().get(0);
                    firstRun.setText("•  " + block.text(), 0);
                }
            }
            if (!payload.sources().isEmpty()) {
                addWordParagraph(document, "知识库来源", 15, true, "1D1D1F", 220, 90);
                for (int index = 0; index < payload.sources().size(); index++) {
                    addWordParagraph(document, "[" + (index + 1) + "] " + payload.sources().get(index),
                            9, false, "6E6E73", 0, 55);
                }
            }
            document.getProperties().getCoreProperties().setTitle(payload.title());
            document.getProperties().getCoreProperties().setCreator("ResearchFlow AI");
            document.write(output);
            return output.toByteArray();
        }
    }

    private void configurePage(XWPFDocument document) {
        CTSectPr section = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz size = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
        size.setW(BigInteger.valueOf(11906));
        size.setH(BigInteger.valueOf(16838));
        CTPageMar margin = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
        margin.setTop(BigInteger.valueOf(1000));
        margin.setBottom(BigInteger.valueOf(1000));
        margin.setLeft(BigInteger.valueOf(1100));
        margin.setRight(BigInteger.valueOf(1100));
    }

    private XWPFParagraph addWordParagraph(
            XWPFDocument document, String text, int fontSize, boolean bold, String color,
            int spacingBefore, int spacingAfter
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        paragraph.setSpacingBetween(1.35);
        XWPFRun run = paragraph.createRun();
        run.setText(stripInlineMarkdown(text));
        styleRun(run, fontSize, bold, color);
        return paragraph;
    }

    private void setCellText(XWPFTableCell cell, String text, boolean label) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingBefore(35);
        paragraph.setSpacingAfter(35);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        styleRun(run, 9, label, label ? "6E6E73" : "1D1D1F");
        cell.setWidth(label ? "24%" : "76%");
    }

    private void styleRun(XWPFRun run, int fontSize, boolean bold, String color) {
        run.setFontFamily(WORD_FONT);
        run.setFontFamily(WORD_FONT, XWPFRun.FontCharRange.eastAsia);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setColor(color);
    }

    private void addPdfSources(Document document, List<String> sources, Font headingFont, Font sourceFont)
            throws com.lowagie.text.DocumentException {
        if (sources.isEmpty()) return;
        Paragraph heading = new Paragraph("知识库来源", headingFont);
        heading.setSpacingBefore(16);
        heading.setSpacingAfter(8);
        document.add(heading);
        for (int index = 0; index < sources.size(); index++) {
            Paragraph source = new Paragraph("[" + (index + 1) + "] " + sources.get(index), sourceFont);
            source.setLeading(14);
            source.setSpacingAfter(4);
            document.add(source);
        }
    }

    private String resolvePdfFont() {
        List<String> candidates = new ArrayList<>();
        if (properties.fontPath() != null && !properties.fontPath().isBlank()) {
            candidates.add(properties.fontPath().trim());
        }
        candidates.add("C:/Windows/Fonts/Deng.ttf");
        candidates.add("C:/Windows/Fonts/NotoSansSC-VF.ttf");
        candidates.add("C:/Windows/Fonts/simhei.ttf");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0");
        candidates.add("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0");
        return candidates.stream()
                .filter(this::fontExists)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_EXPORT_FAILED));
    }

    private boolean fontExists(String font) {
        String filePath = font.replaceFirst(",\\d+$", "");
        try {
            return Files.isRegularFile(Path.of(filePath));
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<MarkdownBlock> parseMarkdown(String content) {
        List<MarkdownBlock> blocks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        for (String rawLine : safe(content).replace("\r", "").split("\n")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                flushParagraph(blocks, paragraph);
                continue;
            }
            if (line.matches("^#{1,6}\\s+.*")) {
                flushParagraph(blocks, paragraph);
                blocks.add(new MarkdownBlock(stripInlineMarkdown(line.replaceFirst("^#{1,6}\\s+", "")), true, false));
            } else if (line.matches("^[-*+]\\s+.*")) {
                flushParagraph(blocks, paragraph);
                blocks.add(new MarkdownBlock(stripInlineMarkdown(line.replaceFirst("^[-*+]\\s+", "")), false, true));
            } else if (line.matches("^\\d+[.)]\\s+.*")) {
                flushParagraph(blocks, paragraph);
                blocks.add(new MarkdownBlock(stripInlineMarkdown(line), false, false));
            } else {
                if (!paragraph.isEmpty()) paragraph.append('\n');
                paragraph.append(line);
            }
        }
        flushParagraph(blocks, paragraph);
        return blocks;
    }

    private void flushParagraph(List<MarkdownBlock> blocks, StringBuilder paragraph) {
        if (paragraph.isEmpty()) return;
        blocks.add(new MarkdownBlock(stripInlineMarkdown(paragraph.toString()), false, false));
        paragraph.setLength(0);
    }

    private String stripInlineMarkdown(String value) {
        return safe(value)
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("\\[(.+?)]\\((.+?)\\)", "$1");
    }

    private Map<String, String> baseMetadata(
            String projectName, String creatorName, String model, LocalDateTime createdAt
    ) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("项目", safe(projectName));
        metadata.put("生成者", creatorName == null || creatorName.isBlank() ? "项目成员" : creatorName);
        metadata.put("生成时间", createdAt == null ? DATE_TIME.format(LocalDateTime.now()) : DATE_TIME.format(createdAt));
        metadata.put("模型", model == null || model.isBlank() ? "AI Model" : model);
        return metadata;
    }

    private void appendRiskMetrics(Map<String, String> metadata, Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return;
        putMetric(metadata, snapshot, "项目进度", "projectProgress", "%");
        putMetric(metadata, snapshot, "任务总数", "totalTasks", "");
        putMetric(metadata, snapshot, "逾期任务", "overdueTasks", "");
        putMetric(metadata, snapshot, "7日内到期", "dueSoonTasks", "");
        putMetric(metadata, snapshot, "紧急任务", "urgentTasks", "");
        putMetric(metadata, snapshot, "未指派任务", "unassignedTasks", "");
    }

    private void putMetric(Map<String, String> metadata, Map<String, Object> snapshot,
                           String label, String key, String suffix) {
        Object value = snapshot.get(key);
        if (value != null) metadata.put(label, value + suffix);
    }

    private String riskLevelLabel(String level) {
        return switch (level) {
            case "LOW" -> "低风险";
            case "MEDIUM" -> "中风险";
            case "HIGH" -> "高风险";
            case "CRITICAL" -> "严重风险";
            default -> level;
        };
    }

    private String pageSuffix(Integer pageNumber) {
        return pageNumber == null ? "" : " · 第 " + pageNumber + " 页";
    }

    private String safeFileName(String title) {
        String normalized = safe(title).replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        if (normalized.isBlank()) normalized = "ResearchFlow 报告";
        return normalized.substring(0, Math.min(normalized.length(), 80));
    }

    private String safe(String value) {
        return value == null ? "" : new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private enum ExportFormat {
        PDF("pdf", "application/pdf"),
        DOCX("docx", DOCX_MEDIA_TYPE);

        private final String extension;
        private final String contentType;

        ExportFormat(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        private static ExportFormat parse(String value) {
            if (value == null || value.isBlank()) return PDF;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                throw new BusinessException(ErrorCode.PARAM_ERROR);
            }
        }
    }

    private record ReportPayload(
            String title,
            String type,
            Map<String, String> metadata,
            String content,
            List<String> sources
    ) {
    }

    private record MarkdownBlock(String text, boolean heading, boolean bullet) {
    }
}
