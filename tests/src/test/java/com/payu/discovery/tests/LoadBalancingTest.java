package com.payu.discovery.tests;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import com.payu.discovery.Discover;
import com.payu.discovery.client.EnableServiceDiscovery;
import com.payu.discovery.client.config.ServiceDiscoveryClientConfig;
import com.payu.discovery.server.DiscoveryServerMain;
import com.payu.discovery.server.InMemoryDiscoveryServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DiscoveryServerMain.class, LoadBalancingTest.class})
@IntegrationTest("server.port:8060")
@WebAppConfiguration
@EnableServiceDiscovery
@PropertySource("classpath:propertasy.properties")
public class LoadBalancingTest {

    private List<ConfigurableApplicationContext> remoteContexts = new ArrayList<>();

    @Autowired
    private InMemoryDiscoveryServer server;

    @Discover
    private TestService testService;

    @Before
    public void before() throws InterruptedException {
        remoteContexts.add(SpringApplication.run(ServiceConfiguration.class,
                "--server.port=8031",
                "--app.address=http://localhost:8031",
                "--spring.jmx.enabled=false",
                "--serviceDiscovery.address=http://localhost:8060/server/discovery"));

        remoteContexts.add(SpringApplication.run(SecondServiceConfiguration.class,
                "--server.port=8032",
                "--app.address=http://localhost:8032",
                "--spring.jmx.enabled=false",
                "--serviceDiscovery.address=http://localhost:8060/server/discovery"));
    }

    @After
    public void close() {
        remoteContexts.forEach(context -> context.close());
    }

    @Configuration
    @EnableAutoConfiguration
    @Import(ServiceDiscoveryClientConfig.class)
    public static class ServiceConfiguration {

        @Bean
        public TestService testService() {
            return new TestServiceImpl();
        }

    }

    @Configuration
    @EnableAutoConfiguration
    @Import(ServiceDiscoveryClientConfig.class)
    public static class SecondServiceConfiguration {

        @Bean
        public TestService testService() {
            return new TestServiceImpl();
        }

    }

    @Test
    public void shouldLoadBalanceBetweenImplementations() throws InterruptedException {
        await().atMost(5, TimeUnit.SECONDS).until(() -> assertThat(server.fetchAllServices()).hasSize(2));

        //when
        final int result = testService.testMethod();
        final int result2 = testService.testMethod();
        final int result3 = testService.testMethod();
        final int result4 = testService.testMethod();

        //then
        assertThat(result).isEqualTo(1);
        assertThat(result2).isEqualTo(1);
        assertThat(result3).isEqualTo(2);
        assertThat(result4).isEqualTo(2);
    }

}
