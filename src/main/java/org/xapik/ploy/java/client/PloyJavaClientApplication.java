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
import org.springframework.scheduling.annotation.EnableScheduling;
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
@EnableScheduling
@SpringBootApplication
public class PloyJavaClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(PloyJavaClientApplication.class, args);
    }
}