package io.katharsis.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.katharsis.dispatcher.controller.collection.CollectionGet;
import io.katharsis.dispatcher.registry.ControllerRegistry;
import io.katharsis.errorhandling.ErrorResponse;
import io.katharsis.errorhandling.mapper.ExceptionMapperRegistryTest;
import io.katharsis.locator.SampleJsonServiceLocator;
import io.katharsis.queryParams.RequestParams;
import io.katharsis.request.path.JsonPath;
import io.katharsis.request.path.PathBuilder;
import io.katharsis.resource.ResourceInformationBuilder;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.resource.registry.ResourceRegistryBuilder;
import io.katharsis.resource.registry.ResourceRegistryBuilderTest;
import io.katharsis.resource.registry.ResourceRegistryTest;
import io.katharsis.response.BaseResponse;
import io.katharsis.response.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RequestDispatcherTest {

    private ResourceRegistry resourceRegistry;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void prepare() {
        ResourceRegistryBuilder registryBuilder = new ResourceRegistryBuilder(new SampleJsonServiceLocator(), new ResourceInformationBuilder());
        resourceRegistry = registryBuilder.build(ResourceRegistryBuilderTest.TEST_MODELS_PACKAGE, ResourceRegistryTest.TEST_MODELS_URL);
    }

    @Test
    public void onGivenPathAndRequestTypeControllerShouldHandleRequest() throws Exception {
        // GIVEN
        String path = "/tasks/";
        String requestType = "GET";

        PathBuilder pathBuilder = new PathBuilder(resourceRegistry);
        ControllerRegistry controllerRegistry = new ControllerRegistry(null);
        CollectionGet collectionGet = mock(CollectionGet.class);
        controllerRegistry.addController(collectionGet);
        RequestDispatcher sut = new RequestDispatcher(controllerRegistry, null);

        // WHEN
        when(collectionGet.isAcceptable(any(JsonPath.class), eq(requestType))).thenCallRealMethod();
        JsonPath jsonPath = pathBuilder.buildPath(path);
        sut.dispatchRequest(jsonPath, requestType, new RequestParams(new ObjectMapper()), null);

        // THEN
        verify(collectionGet, times(1)).handle(any(JsonPath.class), any(RequestParams.class), any());
    }

    @Test
    public void shouldMapExceptionToErrorResponseIfMapperIsAvailable() throws Exception {

        ControllerRegistry controllerRegistry = mock(ControllerRegistry.class);
        when(controllerRegistry.getController(any(JsonPath.class), anyString())).thenThrow(IllegalStateException.class);

        RequestDispatcher requestDispatcher = new RequestDispatcher(controllerRegistry, ExceptionMapperRegistryTest.exceptionMapperRegistry);

        BaseResponse<?> response = requestDispatcher.dispatchRequest(null, null, null, null);
        assertThat(response)
                .isNotNull()
                .isExactlyInstanceOf(ErrorResponse.class);

        ErrorResponse errorResponse = (ErrorResponse)response;
        assertThat(errorResponse.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);

    }

    @Test
    public void shouldThrowExceptionAsIsIfMapperIsNotAvailable() throws Exception {
        ControllerRegistry controllerRegistry = mock(ControllerRegistry.class);
        when(controllerRegistry.getController(any(JsonPath.class), anyString())).thenThrow(ArithmeticException.class);

        RequestDispatcher requestDispatcher = new RequestDispatcher(controllerRegistry, ExceptionMapperRegistryTest.exceptionMapperRegistry);

        expectedException.expect(ArithmeticException.class);

        BaseResponse<?> response = requestDispatcher.dispatchRequest(null, null, null, null);
    }
}
