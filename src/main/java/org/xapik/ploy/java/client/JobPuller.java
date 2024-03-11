package org.xapik.ploy.java.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.xapik.ploy.java.client.handlers.JobExecutor;
import org.xapik.ploy.java.client.handlers.JobExecutorEntry;
import org.xapik.ploy.java.client.handlers.JobHandler;
import org.xapik.ploy.jobworker.CompleteWorkItemRequest;
import org.xapik.ploy.jobworker.JobWorkerServiceGrpc;
import org.xapik.ploy.jobworker.WorkRequest;
import org.xapik.ploy.jobworker.WorkResponse;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JobPuller {

    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    private ManagedChannel channel;
    private final Map<String, JobExecutorEntry> jobExecutors = new HashMap<>();

    @PostConstruct
    public void postConstruct() {
        this.channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        initializeJobExecutor();
    }

    @PreDestroy
    public void preDestroy() {
        channel.shutdown();
    }

    @Scheduled(fixedRate = 500)
    public void pull() {
        JobWorkerServiceGrpc.JobWorkerServiceBlockingStub stub = JobWorkerServiceGrpc.newBlockingStub(channel);

        WorkResponse workResponse = stub.getWorkItems(
                WorkRequest.newBuilder().build()
        );


        workResponse.getWorkitemsList()
                .forEach((workItem) -> {
                    try {
                        JobExecutorEntry jobExecutor = jobExecutors.get(workItem.getJobName());

                        if (jobExecutor == null) {
                            log.error("JobExecutor not found for {}", workItem.getJobName());
                            return;
                        }

                        Object input = objectMapper.readValue(workItem.getInputs(), jobExecutor.getInputClass());
                        Object output = jobExecutor.getMethod().invoke(jobExecutor.getHandler(), input);

                        String outputs = objectMapper.writeValueAsString(output);

                        stub.completeWorkItem(
                                CompleteWorkItemRequest.newBuilder()
                                        .setJobId(workItem.getJobId())
                                        .setOutputs(outputs)
                                        .build()
                        );
                    } catch (JsonProcessingException | IllegalAccessException | InvocationTargetException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                });
    }

    private void initializeJobExecutor() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(JobHandler.class);

        beans.forEach((key, jobHandler) -> {
            Class<?> klass = jobHandler.getClass();
            for (final Method method : klass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(JobExecutor.class)) {
                    JobExecutor jobExecutor = method.getAnnotation(JobExecutor.class);

                    JobExecutorEntry jobExecutorEntry = new JobExecutorEntry();
                    jobExecutorEntry.setMethod(method);
                    jobExecutorEntry.setHandler(jobHandler);
                    jobExecutorEntry.setName(jobExecutor.name());
                    jobExecutorEntry.setInputClass(jobExecutor.inputType());

                    jobExecutors.put(jobExecutorEntry.getName(), jobExecutorEntry);
                }
            }
        });
    }
}
