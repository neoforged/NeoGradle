package net.minecraftforge.gradle.common.util.workers;

import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A helper which handles executing arbitrary work actions in parallel.
 * Note these actions are executed without isolation.
 * <p>
 * This is derived from: <a href="https://github.com/michel-kraemer/gradle-download-task/blob/94ec0dad3b6831acc00db1807741d7feee57a5fc/src/main/java/de/undercouch/gradle/tasks/download/internal/DefaultWorkerExecutorHelper.java">...</a>
 */
public class DefaultWorkerExecutorHelper {
    /**
     * A unique ID for jobs. Used to access jobs in {@link #jobs}
     */
    private static final AtomicInteger UNIQUE_ID = new AtomicInteger();

    /**
     * A maps of jobs submitted to this executor
     */
    private static final Map<Integer, Runnable> jobs = new ConcurrentHashMap<>();

    /**
     * The gradle worker executor which runs the tasks.
     */
    private final WorkerExecutor workerExecutor;

    /**
     * The queue that will be used to execute the tasks.
     */
    private final WorkQueue workQueue;

    /**
     * Constructs a new executor
     * @param workerExecutor the Gradle Worker API executor
     */
    @Inject
    public DefaultWorkerExecutorHelper(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor;
        this.workQueue = workerExecutor.noIsolation();
    }

    public void submit(Runnable job) {
        int id = UNIQUE_ID.getAndIncrement();
        jobs.put(id, job);
        workQueue.submit(DefaultWorkAction.class, parameters ->
                parameters.getID().set(id));
    }

    public void await() {
        workerExecutor.await();
    }

    public interface DefaultWorkParameters extends WorkParameters {
        Property<Integer> getID();
    }

    public static abstract class DefaultWorkAction implements WorkAction<DefaultWorkParameters> {
        @Override
        public void execute() {
            Runnable job = jobs.remove(getParameters().getID().get());
            job.run();
        }
    }
}
