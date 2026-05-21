package com.mphasis.tse.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
class AsyncConfigTest {

    private AsyncConfig asyncConfig;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
    }

    @Test
    void testJobLauncherExecutorConfiguration() {
        ThreadPoolTaskExecutor executor =
                (ThreadPoolTaskExecutor) asyncConfig.jobLauncherExecutor();
        assertAll(
                () -> assertNotNull(executor),
                () -> assertEquals(5, executor.getCorePoolSize()),
                () -> assertEquals(10, executor.getMaxPoolSize()),
                () -> assertTrue(executor.getThreadNamePrefix().startsWith("JobLauncher-"))
        );
    }

    @Test
    void testStepTaskExecutorConfiguration() {
        ThreadPoolTaskExecutor executor =
                (ThreadPoolTaskExecutor) asyncConfig.stepTaskExecutor();
        assertAll(
                () -> assertNotNull(executor),
                () -> assertEquals(12, executor.getCorePoolSize()),
                () -> assertEquals(24, executor.getMaxPoolSize()),
                () -> assertTrue(executor.getThreadNamePrefix().startsWith("StepExecutor-"))
        );
    }

    @Test
    void testExecutorsAreIndependent() {
        ThreadPoolTaskExecutor jobExecutor =
                (ThreadPoolTaskExecutor) asyncConfig.jobLauncherExecutor();
        ThreadPoolTaskExecutor stepExecutor =
                (ThreadPoolTaskExecutor) asyncConfig.stepTaskExecutor();
        assertNotSame(jobExecutor, stepExecutor);
        assertNotEquals(jobExecutor.getCorePoolSize(), stepExecutor.getCorePoolSize());
    }

}