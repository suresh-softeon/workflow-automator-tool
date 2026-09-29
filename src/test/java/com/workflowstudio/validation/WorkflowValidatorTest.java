package com.workflowstudio.validation;

import com.workflowstudio.model.WorkflowMetadata;
import com.workflowstudio.storage.WorkflowRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkflowValidatorTest {
    @Test
    void rejectsBlankProjectSubmoduleAndScreen() {
        WorkflowValidator validator = new WorkflowValidator(new InMemoryWorkflowRepository());
        Assertions.assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate("", "Orders", "OM"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate("UMG", "", "OM"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate("UMG", "Orders", ""));
    }

    @Test
    void rejectsDuplicateScreen() {
        InMemoryWorkflowRepository repository = new InMemoryWorkflowRepository();
        repository.upsert(new WorkflowMetadata("UMG", "Orders", "OM", "scripts/UMG/Orders/OM.js", LocalDateTime.now(), LocalDateTime.now()));
        WorkflowValidator validator = new WorkflowValidator(repository);
        Assertions.assertThrows(IllegalArgumentException.class, () -> validator.validateForCreate("UMG", "Orders", "OM"));
    }

    private static final class InMemoryWorkflowRepository implements WorkflowRepository {
        private final List<WorkflowMetadata> entries = new ArrayList<>();

        @Override
        public List<WorkflowMetadata> listAll() {
            return List.copyOf(entries);
        }

        @Override
        public Optional<WorkflowMetadata> find(String project, String submodule, String screenName) {
            return entries.stream()
                    .filter(it -> it.project().equals(project)
                            && it.submodule().equals(submodule)
                            && it.screenName().equals(screenName))
                    .findFirst();
        }

        @Override
        public void upsert(WorkflowMetadata metadata) {
            entries.removeIf(it -> it.project().equals(metadata.project())
                    && it.submodule().equals(metadata.submodule())
                    && it.screenName().equals(metadata.screenName()));
            entries.add(metadata);
        }

        @Override
        public boolean delete(String project, String submodule, String screenName) {
            return entries.removeIf(it -> it.project().equals(project)
                    && it.submodule().equals(submodule)
                    && it.screenName().equals(screenName));
        }
    }
}
