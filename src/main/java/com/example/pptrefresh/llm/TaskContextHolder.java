package com.example.pptrefresh.llm;

/** 当前线程正在执行的 task 上下文，供 Tool 执行时读取 queryPlan。 */
public final class TaskContextHolder {

    private static final ThreadLocal<TaskContext> CURRENT = new ThreadLocal<>();

    private TaskContextHolder() {}

    public static void set(TaskContext context) {
        CURRENT.set(context);
    }

    public static TaskContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
