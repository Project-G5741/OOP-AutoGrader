package support.com.eiu.capstone.backend.grading.testcase;

import java.util.ArrayList;
import java.util.List;

import com.eiu.capstone.backend.grading.testcase.WorkerProcessClient;
import com.eiu.capstone.backend.grading.testcase.worker.WorkerMain;

public final class WorkerTestSupport {

    private WorkerTestSupport() {}

    public static List<String> javaCommand(String... workerArgs) {
        List<String> command = new ArrayList<>();
        command.add(ProcessHandle.current().info().command().orElse("java"));
        command.add(WorkerProcessClient.HEAP_FLAG);
        command.add(WorkerProcessClient.METASPACE_FLAG);
        command.add(WorkerProcessClient.EXIT_ON_OOM_FLAG);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(WorkerMain.class.getName());
        if (workerArgs != null && workerArgs.length > 0) {
            command.addAll(List.of(workerArgs));
        }
        return command;
    }
}
