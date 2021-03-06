package io.katharsis.dispatcher.controller.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.katharsis.dispatcher.controller.BaseControllerTest;
import io.katharsis.dispatcher.controller.HttpMethod;
import io.katharsis.queryParams.RequestParams;
import io.katharsis.request.dto.Attributes;
import io.katharsis.request.dto.DataBody;
import io.katharsis.request.dto.RequestBody;
import io.katharsis.request.dto.ResourceRelationships;
import io.katharsis.request.path.JsonPath;
import io.katharsis.request.path.ResourcePath;
import io.katharsis.resource.exception.ResourceNotFoundException;
import io.katharsis.resource.mock.models.Project;
import io.katharsis.resource.mock.models.Task;
import io.katharsis.resource.mock.repository.TaskToProjectRepository;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.response.BaseResponse;
import io.katharsis.response.ResourceResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FieldResourcePostTest extends BaseControllerTest {
    private static final String REQUEST_TYPE = HttpMethod.POST.name();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final RequestParams REQUEST_PARAMS = new RequestParams(OBJECT_MAPPER);

    @Test
    public void onValidRequestShouldAcceptIt() {
        // GIVEN
        JsonPath jsonPath = pathBuilder.buildPath("tasks/1/project");
        ResourceRegistry resourceRegistry = mock(ResourceRegistry.class);
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        assertThat(result).isTrue();
    }

    @Test
    public void onNonRelationRequestShouldDenyIt() {
        // GIVEN
        JsonPath jsonPath = new ResourcePath("tasks");
        ResourceRegistry resourceRegistry = mock(ResourceRegistry.class);
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        assertThat(result).isFalse();
    }

    @Test
    public void onNonExistentParentResourceShouldThrowException() throws Exception {
        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        DataBody data = new DataBody();
        newProjectBody.setData(data);
        data.setType("projects");
        data.setAttributes(new Attributes());
        data.getAttributes().addAttribute("name", "sample project");

        JsonPath projectPath = pathBuilder.buildPath("/tasks/-1/project");
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // THEN
        expectedException.expect(ResourceNotFoundException.class);

        // WHEN
        sut.handle(projectPath, new RequestParams(OBJECT_MAPPER), newProjectBody);
    }

    @Test
    public void onExistingParentResourceShouldSaveIt() throws Exception {
        // GIVEN
        RequestBody newTaskBody = new RequestBody();
        DataBody data = new DataBody();
        newTaskBody.setData(data);
        data.setType("tasks");
        data.setAttributes(new Attributes());
        data.getAttributes().addAttribute("name", "sample task");
        data.setRelationships(new ResourceRelationships());

        JsonPath taskPath = pathBuilder.buildPath("/tasks");
        ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser);

        // WHEN
        BaseResponse taskResponse = resourcePost.handle(taskPath, new RequestParams(new ObjectMapper()), newTaskBody);

        // THEN
        assertThat(taskResponse.getData()).isExactlyInstanceOf(Task.class);
        Long taskId = ((Task) (taskResponse.getData())).getId();
        assertThat(taskId).isNotNull();

        /* ------- */

        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        data = new DataBody();
        newProjectBody.setData(data);
        data.setType("projects");
        data.setAttributes(new Attributes());
        data.getAttributes().addAttribute("name", "sample project");

        JsonPath projectPath = pathBuilder.buildPath("/tasks/" + taskId + "/project");
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // WHEN
        ResourceResponse projectResponse = sut.handle(projectPath, new RequestParams(OBJECT_MAPPER), newProjectBody);

        assertThat(projectResponse.getData()).isExactlyInstanceOf(Project.class);
        assertThat(((Project) (projectResponse.getData())).getId()).isNotNull();
        assertThat(((Project) (projectResponse.getData())).getName()).isEqualTo("sample project");
        Long projectId = ((Project) (projectResponse.getData())).getId();
        assertThat(projectId).isNotNull();

        TaskToProjectRepository taskToProjectRepository = new TaskToProjectRepository();
        Project project = taskToProjectRepository.findOneTarget(taskId, "project", REQUEST_PARAMS);
        assertThat(project.getId()).isEqualTo(projectId);
    }
}
