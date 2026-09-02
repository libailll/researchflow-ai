package com.researchflow.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    PARAM_ERROR(400, "参数错误"),

    UNAUTHORIZED(401, "未登录或登录已过期"),

    FORBIDDEN(403, "无权限访问"),

    NOT_FOUND(404, "资源不存在"),

    USER_NOT_FOUND(1001, "用户不存在"),

    USERNAME_ALREADY_EXISTS(1002, "用户名已存在"),

    PASSWORD_ERROR(1003, "用户名或密码错误"),

    USER_DISABLED(1004, "用户已被禁用"),

    PROJECT_NOT_FOUND(2001, "项目不存在"),

    PROJECT_ACCESS_DENIED(2002, "无权访问该项目"),

    PROJECT_MEMBER_EXISTS(2003, "用户已是项目成员"),

    PROJECT_MEMBER_NOT_FOUND(2004, "项目成员不存在"),

    INVALID_PROJECT_MEMBER_ROLE(2005, "项目成员角色不合法"),

    CANNOT_REMOVE_PROJECT_OWNER(2006, "不能移除项目所有者"),

    PROJECT_DATE_INVALID(2007, "项目结束日期不能早于开始日期"),

    TASK_NOT_FOUND(3001, "任务不存在"),

    TASK_ACCESS_DENIED(3002, "无权操作该任务"),

    TASK_ASSIGNEE_NOT_PROJECT_MEMBER(3003, "任务负责人不是项目成员"),

    TASK_DATE_INVALID(3004, "任务截止日期不能早于开始日期"),

    DOCUMENT_NOT_FOUND(4001, "文档不存在"),

    DOCUMENT_ACCESS_DENIED(4002, "无权操作该文档"),

    DOCUMENT_EMPTY_FILE(4003, "上传文件不能为空"),

    DOCUMENT_TYPE_NOT_SUPPORTED(4004, "仅支持 PDF、DOCX、TXT 和 Markdown 文件"),

    DOCUMENT_FILE_TOO_LARGE(4005, "文件大小不能超过 50MB"),

    DOCUMENT_STORAGE_FAILED(4006, "文件存储失败"),

    DOCUMENT_MESSAGE_PUBLISH_FAILED(4007, "文档解析任务发送失败，请稍后重试"),

    DOCUMENT_PARSE_RESULT_INVALID(4008, "文档解析结果不合法"),

    INTERNAL_SERVICE_UNAUTHORIZED(9001, "内部服务认证失败"),

    AI_SERVICE_UNAVAILABLE(5001, "AI 服务暂时不可用，请稍后重试"),

    DOCUMENT_NOT_PARSED(5002, "文档尚未解析完成，不能进行向量化"),

    DOCUMENT_VECTOR_PUBLISH_FAILED(5003, "文档向量化任务发送失败，请稍后重试"),

    AI_CONVERSATION_NOT_FOUND(5004, "AI 会话不存在"),

    AI_CONVERSATION_ACCESS_DENIED(5005, "无权访问该 AI 会话"),

    AGENT_ACTION_INVALID(5006, "Agent 操作参数不合法"),

    WEEKLY_REPORT_NOT_FOUND(5007, "项目周报不存在"),

    WEEKLY_REPORT_ACCESS_DENIED(5008, "无权操作该项目周报"),

    WEEKLY_REPORT_PERIOD_INVALID(5009, "周报日期范围不合法"),

    DOCUMENT_SUMMARY_NOT_FOUND(5010, "文档总结不存在"),

    DOCUMENT_SUMMARY_ACCESS_DENIED(5011, "无权操作该文档总结"),

    DOCUMENT_SUMMARY_NOT_READY(5012, "文档尚未完成解析和向量化，不能生成总结"),

    PROJECT_RISK_REPORT_NOT_FOUND(5013, "项目风险报告不存在"),

    PROJECT_RISK_REPORT_ACCESS_DENIED(5014, "无权操作该项目风险报告"),

    REPORT_EXPORT_FAILED(5015, "报告导出失败，请检查导出字体配置"),

    NOTIFICATION_NOT_FOUND(6001, "通知不存在"),

    SYSTEM_ERROR(500, "系统内部错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
