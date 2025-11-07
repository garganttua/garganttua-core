package com.garganttua.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garganttua.api.core.DummyBean;
import com.garganttua.api.core.DummyDao;
import com.garganttua.api.core.DummyDto;
import com.garganttua.api.core.DummyDto2;
import com.garganttua.api.core.DummyEntity;
import com.garganttua.api.core.ReflectionsAnnotationScanner;
import com.garganttua.api.core.builder.ApplicationContextBuilder;
import com.garganttua.api.core.context.execution.ExecutionContext;
import com.garganttua.api.core.context.execution.ExecutionContext.InputRequest;
import com.garganttua.api.spec.CoreException;
import com.garganttua.api.spec.engine.IApplicationContext;
import com.garganttua.api.spec.engine.IApplicationContextBuilder;
import com.garganttua.api.spec.engine.IExecutionContext;
import com.garganttua.api.spec.engine.Service;
import com.garganttua.core.runtime.Runtime;
import com.garganttua.core.runtime.dsl.IRuntime;
import com.garganttua.objects.mapper.GGMapper;
import com.garganttua.objects.mapper.GGMapperException;
import com.garganttua.reflection.beans.GGBeanLoaderFactory;
import com.garganttua.reflection.beans.GGBeanSupplier;
import com.garganttua.reflection.beans.IGGBeanLoader;
import com.garganttua.reflection.injection.GGInjector;
import com.garganttua.reflection.injection.IGGInjector;
import com.garganttua.reflection.properties.IGGPropertyLoader;
import com.garganttua.reflection.utils.GGObjectReflectionHelper;

public class ReadAllRuntimeTest {

    private IApplicationContextBuilder contextBuilder;

    @BeforeEach
    public void setupTest() throws CoreException {
        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();

        IGGPropertyLoader propLoader = new IGGPropertyLoader() {

            @Override
            public String getProperty(String propertyName) {
                return "dummy";
            }
        };

        IGGBeanLoader beanloader = GGBeanLoaderFactory.getLoader(propLoader, List.of("com.garganttua"),
                List.of(new GGBeanSupplier(List.of("com.garganttua"), propLoader)));
        IGGInjector injector = GGInjector.injector(beanloader);

        this.contextBuilder = new ApplicationContextBuilder().packages(List.of("com.garganttua"))
                .propertyLoader(propLoader)
                .injector(injector)
                .beanLoader(beanloader)
                .autoDetect(false)
                .superTenantId("0")
                .superTenantAutoCreate(false)
                .security().disable(true).up();
    }

    @Test
    public void test() throws GGMapperException {
        DummyDto dto = new DummyDto("1", "1", "1", "dto1");

        GGMapper mapper = new GGMapper();
        DummyEntity entity = mapper.map(dto, DummyEntity.class);
        assertNotNull(entity);
    }

    @Test
    public void testGetEntitiesFromSimpleDao() throws CoreException, JsonProcessingException, ExecutionException {
        DummyDao dao1 = new DummyDao();
        dao1.setNextFindReturn(List.of(new DummyDto("1", "1", "1", "dto1"), new DummyDto("2", "1", "1", "dto1"),
                new DummyDto("3", "1", "1", "dto1")));

        IApplicationContext applicationContext = this.contextBuilder.domain(DummyEntity.class).entity().uuid("uuid").tenantId("tenantId").id("id").up().security()
                .disable(true).up().dto(DummyDto.class).uuid("uuid").tenantId("tenantId").id("id").db(dao1).up()
                .up().build();

        Service service = applicationContext.getDomainContext("dummyentities").get().getServices().get(0);

        InputRequest request = new InputRequest(null, "0", null, "dummyentities", service, null, null);

        IExecutionContext executionContext = new ExecutionContext(request);
        IRuntime runtime = Runtime.createRuntime(applicationContext, executionContext);

        executionContext = runtime.execute();

        assertEquals(3, ((List<DummyEntity>) executionContext.getResponse().get()).size());
        assertEquals("dto1", ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getInfoFromDto1());
        assertEquals(null, ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getInfoFromDto2());

    }

    @Test
    public void testAfterGetMethods() throws CoreException, JsonProcessingException, ExecutionException {
        String afterGetString = UUID.randomUUID().toString();
        DummyDao dao1 = new DummyDao();
        dao1.setNextFindReturn(List.of(new DummyDto("1", "1", "1", "dto1"), new DummyDto("2", "1", "1", "dto1"),
                new DummyDto("3", "1", "1", "dto1")));

        IApplicationContext applicationContext = this.contextBuilder.domain(DummyEntity.class).entity().afterGet("afterGet").withParam(afterGetString).up().uuid("uuid")
                .tenantId("tenantId").id("id").up().security()
                .disable(true).up().dto(DummyDto.class).uuid("uuid").tenantId("tenantId").id("id").db(dao1).up()
                .up().build();

        Service service = applicationContext.getDomainContext("dummyentities").get().getServices().get(0);

        InputRequest request = new InputRequest(null, "0", null, "dummyentities", service, null, null);

        IExecutionContext executionContext = new ExecutionContext(request);
        IRuntime runtime = Runtime.createRuntime(applicationContext, executionContext);

        executionContext = runtime.execute();

        assertEquals(afterGetString,
                ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getAfterGetString());
    }

    @Test
    public void testInjectionIsNotDoneIfNoAfterGetMethod()
            throws CoreException, JsonProcessingException, ExecutionException {
        DummyDao dao1 = new DummyDao();
        dao1.setNextFindReturn(List.of(new DummyDto("1", "1", "1", "dto1"), new DummyDto("2", "1", "1", "dto1"),
                new DummyDto("3", "1", "1", "dto1")));

        IApplicationContext applicationContext = this.contextBuilder.domain(DummyEntity.class).entity().uuid("uuid")
                .tenantId("tenantId").id("id").up().security()
                .disable(true).up().dto(DummyDto.class).uuid("uuid").tenantId("tenantId").id("id").db(dao1).up()
                .up().build();

        Service service = applicationContext.getDomainContext("dummyentities").get().getServices().get(0);

        InputRequest request = new InputRequest(null, "0", null, "dummyentities", service, null, null);

        IExecutionContext executionContext = new ExecutionContext(request);
        IRuntime runtime = Runtime.createRuntime(applicationContext, executionContext);

        executionContext = runtime.execute();

        assertEquals(null,
                ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getBean());
    }

    @Test
    public void testInjectionIsDoneIfAfterGetMethod()
            throws CoreException, JsonProcessingException, ExecutionException {
        String afterGetString = UUID.randomUUID().toString();
        DummyDao dao1 = new DummyDao();
        dao1.setNextFindReturn(List.of(new DummyDto("1", "1", "1", "dto1"), new DummyDto("2", "1", "1", "dto1"),
                new DummyDto("3", "1", "1", "dto1")));

        IApplicationContext applicationContext = this.contextBuilder.domain(DummyEntity.class).entity().afterGet("afterGet").withParam(afterGetString).up().uuid("uuid")
                .tenantId("tenantId").id("id").up().security()
                .disable(true).up().dto(DummyDto.class).uuid("uuid").tenantId("tenantId").id("id").db(dao1).up()
                .up().build();

        Service service = applicationContext.getDomainContext("dummyentities").get().getServices().get(0);

        InputRequest request = new InputRequest(null, "0", null, "dummyentities", service, null, null);

        IExecutionContext executionContext = new ExecutionContext(request);
        IRuntime runtime = Runtime.createRuntime(applicationContext, executionContext);

        executionContext = runtime.execute();

        assertNotNull(
                ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getBean());
        assertTrue(
                DummyBean.class.isAssignableFrom(
                        ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getBean().getClass()));
    }

    @Test
    public void testGetEntitiesFromMultipleDao() throws CoreException, JsonProcessingException, ExecutionException {
        DummyDao dao1 = new DummyDao();
        dao1.setNextFindReturn(List.of(new DummyDto("1", "1", "1", "dto1"), new DummyDto("2", "1", "1", "dto1"),
                new DummyDto("3", "1", "1", "dto1")));
        DummyDao dao2 = new DummyDao();
        dao2.setNextFindReturn(List.of(new DummyDto2("1", "1", "1", "dto2"), new DummyDto("2", "1", "1", "dto2"),
                new DummyDto("3", "1", "1", "dto2")));

        IApplicationContext applicationContext = this.contextBuilder.domain(DummyEntity.class)
                .entity().uuid("uuid").tenantId("tenantId").id("id").up().security()
                .disable(true).up().dto(DummyDto.class).uuid("uuid").tenantId("tenantId").id("id").db(dao1).up()
                .dto(DummyDto2.class).uuid("uuid").tenantId("tenantId").id("id").db(dao2).up()
                .up().build();

        Service service = applicationContext.getDomainContext("dummyentities").get().getServices().get(0);

        InputRequest request = new InputRequest(null, "0", null, "dummyentities", service, null, null);

        IExecutionContext executionContext = new ExecutionContext(request);
        IRuntime runtime = Runtime.createRuntime(applicationContext, executionContext);

        executionContext = runtime.execute();

        assertEquals(3, ((List<DummyEntity>) executionContext.getResponse().get()).size());

        assertEquals("dto1", ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getInfoFromDto1());
        assertEquals("dto2", ((List<DummyEntity>) executionContext.getResponse().get()).get(0).getInfoFromDto2());
    }

    @Test
    public void noDtoShouldThrowException() throws CoreException, JsonProcessingException {

        GGObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();

        IGGPropertyLoader propLoader = new IGGPropertyLoader() {

            @Override
            public String getProperty(String propertyName) {
                return "dummy";
            }
        };

        IGGBeanLoader beanloader = GGBeanLoaderFactory.getLoader(propLoader, List.of("com.garganttua"),
                List.of(new GGBeanSupplier(List.of("com.garganttua"), propLoader)));
        IGGInjector injector = GGInjector.injector(beanloader);

        IApplicationContextBuilder b = new ApplicationContextBuilder();

        CoreException exception = assertThrows(CoreException.class, () -> {
            b.packages(List.of("com.garganttua"))
                    .propertyLoader(propLoader)
                    .injector(injector)
                    .beanLoader(beanloader)
                    .autoDetect(false)
                    .superTenantId("0")
                    .superTenantAutoCreate(false)
                    .security().disable(true).up()
                    .domain(DummyEntity.class).security().disable(true).up().up().build();
        });

        assertEquals("No dto declared for domain dummyentities", exception.getMessage());
    }

}
