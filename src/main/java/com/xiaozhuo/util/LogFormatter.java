package com.xiaozhuo.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 自定义日志格式化器
 * 格式：[LEVEL] yyyy-MM-dd HH:mm:ss - ClassName.methodName() - Message
 */
public class LogFormatter extends Formatter {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // 日志级别
        sb.append("[")
          .append(record.getLevel())
          .append("] ");

        // 时间戳
        sb.append(dateFormat.format(new Date(record.getMillis())))
          .append(" - ");

        // 类名和方法名
        String sourceClassName = record.getSourceClassName();
        String sourceMethodName = record.getSourceMethodName();

        if (sourceClassName != null) {
            // 简化类名，只保留最后一部分
            String shortClassName = sourceClassName.substring(sourceClassName.lastIndexOf('.') + 1);
            sb.append(shortClassName);

            if (sourceMethodName != null) {
                sb.append(".")
                  .append(sourceMethodName)
                  .append("()");
            }
            sb.append(" - ");
        }

        // 日志消息
        sb.append(formatMessage(record))
          .append(System.lineSeparator());

        // 如果有异常信息，追加堆栈跟踪
        if (record.getThrown() != null) {
            sb.append("Exception: ")
              .append(record.getThrown().getClass().getName())
              .append(": ")
              .append(record.getThrown().getMessage())
              .append(System.lineSeparator());

            // 追加堆栈信息（前5行）
            StackTraceElement[] stackTrace = record.getThrown().getStackTrace();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                sb.append("    at ")
                  .append(stackTrace[i].toString())
                  .append(System.lineSeparator());
            }
        }

        return sb.toString();
    }
}
