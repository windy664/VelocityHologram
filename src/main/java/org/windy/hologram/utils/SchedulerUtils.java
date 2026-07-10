package org.windy.hologram.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 调度器工具类。
 * <p>封装 Velocity 调度器，提供便捷的异步任务管理。
 */
public final class SchedulerUtils {

    private static ProxyServer proxy;
    private static Object plugin;

    // 任务注册表
    private static final Map<String, ScheduledTask> TASKS = new ConcurrentHashMap<>();

    private SchedulerUtils() {}

    /**
     * 初始化调度器。
     */
    public static void init(ProxyServer proxyServer, Object pluginInstance) {
        proxy = proxyServer;
        plugin = pluginInstance;
    }

    /**
     * 运行同步任务。
     */
    public static void runSync(Runnable task) {
        if (proxy == null || plugin == null) return;
        proxy.getScheduler().buildTask(plugin, task).schedule();
    }

    /**
     * 运行异步任务。
     */
    public static void runAsync(Runnable task) {
        if (proxy == null || plugin == null) return;
        proxy.getScheduler().buildTask(plugin, task).schedule();
    }

    /**
     * 延迟运行任务。
     *
     * @param task     任务
     * @param delay    延迟时间
     * @param unit     时间单位
     * @return 任务ID
     */
    public static String runDelayed(Runnable task, long delay, TimeUnit unit) {
        if (proxy == null || plugin == null) return null;

        String taskId = generateTaskId();
        ScheduledTask scheduledTask = proxy.getScheduler().buildTask(plugin, task)
                .delay(delay, unit)
                .schedule();

        TASKS.put(taskId, scheduledTask);
        return taskId;
    }

    /**
     * 运行重复任务。
     *
     * @param task     任务
     * @param delay    初始延迟
     * @param period   重复间隔
     * @param unit     时间单位
     * @return 任务ID
     */
    public static String runRepeating(Runnable task, long delay, long period, TimeUnit unit) {
        if (proxy == null || plugin == null) return null;

        String taskId = generateTaskId();
        ScheduledTask scheduledTask = proxy.getScheduler().buildTask(plugin, task)
                .delay(delay, unit)
                .repeat(period, unit)
                .schedule();

        TASKS.put(taskId, scheduledTask);
        return taskId;
    }

    /**
     * 运行重复任务（tick 为单位）。
     *
     * @param task       任务
     * @param delayTicks 初始延迟（tick）
     * @param periodTicks 重复间隔（tick）
     * @return 任务ID
     */
    public static String runRepeatingTicks(Runnable task, int delayTicks, int periodTicks) {
        return runRepeating(task, delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
    }

    /**
     * 取消任务。
     */
    public static void cancelTask(String taskId) {
        if (taskId == null) return;
        ScheduledTask task = TASKS.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 取消所有任务。
     */
    public static void cancelAllTasks() {
        for (ScheduledTask task : TASKS.values()) {
            task.cancel();
        }
        TASKS.clear();
    }

    /**
     * 检查任务是否存在。
     */
    public static boolean isTaskRunning(String taskId) {
        if (taskId == null) return false;
        return TASKS.containsKey(taskId);
    }

    /**
     * 获取任务数量。
     */
    public static int getTaskCount() {
        return TASKS.size();
    }

    /**
     * 运行延迟任务（毫秒）。
     */
    public static String runDelayedMs(Runnable task, long delayMs) {
        return runDelayed(task, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 运行延迟任务（秒）。
     */
    public static String runDelayedSeconds(Runnable task, long delaySeconds) {
        return runDelayed(task, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 运行重复任务（秒为单位）。
     */
    public static String runRepeatingSeconds(Runnable task, long delaySeconds, long periodSeconds) {
        return runRepeating(task, delaySeconds, periodSeconds, TimeUnit.SECONDS);
    }

    private static String generateTaskId() {
        return "task_" + System.currentTimeMillis() + "_" + TASKS.size();
    }
}
