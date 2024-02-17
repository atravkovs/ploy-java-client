package org.xapik.ploy.java.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.xapik.ploy.java.client.handlers.JobExecutor;
import org.xapik.ploy.java.client.handlers.JobExecutorEntry;
import org.xapik.ploy.java.client.handlers.JobHandler;
import org.xapik.ploy.jobworker.JobWorkerServiceGrpc;
import org.xapik.ploy.jobworker.WorkRequest;
import org.xapik.ploy.jobworker.WorkResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootApplication
public class PloyJavaClientApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(PloyJavaClientApplication.class, args);

        Map<String, Object> beans = ctx.getBeansWithAnnotation(JobHandler.class);
        beans.keySet().forEach(bean -> log.info("Bean {}", bean));

        Map<String, JobExecutorEntry> jobExecutors = new HashMap<>();

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

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        JobWorkerServiceGrpc.JobWorkerServiceBlockingStub stub = JobWorkerServiceGrpc.newBlockingStub(channel);

        WorkResponse workResponse = stub.getWorkItems(
                WorkRequest.newBuilder().build()
        );

        ObjectMapper objectMapper = new ObjectMapper();

        workResponse.getWorkitemsList()
                .forEach((workItem) -> {
                    try {
                        JobExecutorEntry jobExecutor = jobExecutors.get(workItem.getJobName());

                        if (jobExecutor == null) {
                            log.error("JobExecutor not found for {}", workItem.getJobName());
                            return;
                        }

                        Object input = objectMapper.readValue(workItem.getInputs(), jobExecutor.getInputClass());
                        jobExecutor.getMethod().invoke(jobExecutor.getHandler(), input);
                    } catch (JsonProcessingException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });

        channel.shutdown();
    }
}