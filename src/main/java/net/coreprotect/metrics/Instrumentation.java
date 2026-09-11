package net.coreprotect.metrics;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.coreprotect.CoreProtect;
import net.coreprotect.config.Config;
import org.bukkit.Bukkit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Instrumentation {
	private static final AtomicReference<ConcurrentHashMap<String, Metric>> METRICS = new AtomicReference<>(new ConcurrentHashMap<>());
	private static volatile ScheduledTask TASK = null;

	public static synchronized void initialize() {
		TASK = Bukkit.getAsyncScheduler().runAtFixedRate(CoreProtect.getInstance(), Instrumentation::reportMetrics,
				Config.getGlobal().METRICS_REPORTING_INTERVAL_MS,
				Config.getGlobal().METRICS_REPORTING_INTERVAL_MS,
				TimeUnit.MILLISECONDS);
		CoreProtect.getInstance().getLogger().info("Metrics initialized");
	}

	public static synchronized void shutdown() {
		TASK.cancel();
		CoreProtect.getInstance().getLogger().info("Metrics deinitialized");
		reportMetrics(TASK);
		TASK = null;
	}

	public static void end(String id, long startNanos) {
		long elapsed = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
		if ( elapsed > Config.getGlobal().METRICS_LONG_RUNNING_WARNING_MS) {
			CoreProtect.getInstance().getLogger().warning(String.format("\"%s\" took %d ms", id, elapsed));
		}

		ConcurrentHashMap<String, Metric> metrics = METRICS.get();
		Metric metric = metrics.get(id);
		if ( metric == null ) {
			metric = new Metric(id);
			Metric prevMetric = metrics.putIfAbsent(id, metric);
			if ( prevMetric != null )
				metric = prevMetric;
		}
		metric.add(elapsed);
	}

	private static void reportMetrics(ScheduledTask task) {
		ConcurrentHashMap<String, Metric> metrics = METRICS.getAndSet(new ConcurrentHashMap<>());

		if ( ! task.isCancelled() ) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ignored) { }
		}

		for ( Metric metric : metrics.values() ) {
			synchronized(metric) {
				CoreProtect.getInstance().getLogger().info(metric.toString());
			}
		}
	}

	private static class Metric {
		private final String id;
		private int count = 0;
		private long min = 0;
		private long max = 0;
		private long total = 0;

		private Metric(String id) {
			this.id = id;
		}

		public synchronized void add(long value) {
			this.count += 1;
			if ( value < this.min ) this.min = value;
			if ( value > this.max ) this.max = value;
			this.total += value;
		}

		@Override
		public synchronized String toString() {
			return String.format("Metric(id=\"%s\", count=%d, min=%d, max=%d, total=%d)", id, count, min, max, total);
		}
	}
}
