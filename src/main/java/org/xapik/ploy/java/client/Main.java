package org.xapik.ploy.java.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.xapik.ploy.java.client.handlers.JobHandler;
import org.xapik.ploy.jobworker.JobWorkerServiceGrpc;
import org.xapik.ploy.jobworker.WorkItem;
import org.xapik.ploy.jobworker.WorkRequest;
import org.xapik.ploy.jobworker.WorkResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        JobWorkerServiceGrpc.JobWorkerServiceBlockingStub stub = JobWorkerServiceGrpc.newBlockingStub(channel);

        WorkResponse workResponse = stub.getWorkItems(
                WorkRequest.newBuilder().build()
        );

        String jobHandler = "org.xapik.ploy.java.client.handlers.test.TestHandler";
        String jobInput = "org.xapik.ploy.java.client.handlers.test.TestInputDto";

        ObjectMapper objectMapper = new ObjectMapper();

        workResponse.getWorkitemsList()
                .forEach((workItem) -> {
                    try {
                        Object input = objectMapper.readValue(workItem.getInputs(), Class.forName(jobInput));
                        JobHandler jobHandler1 = (JobHandler) Class.forName(jobHandler).getConstructor().newInstance();
                        jobHandler1.execute(input);
                    } catch (JsonProcessingException | ClassNotFoundException | NoSuchMethodException |
                             InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });

        channel.shutdown();
    }
}