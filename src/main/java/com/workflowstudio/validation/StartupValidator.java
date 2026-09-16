package com.workflowstudio.validation;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StartupValidator {
    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    private final NodeValidator nodeValidator;
    private final PlaywrightValidator playwrightValidator;

    public StartupValidator(NodeValidator nodeValidator, PlaywrightValidator playwrightValidator) {
        this.nodeValidator = Objects.requireNonNull(nodeValidator);
        this.playwrightValidator = Objects.requireNonNull(playwrightValidator);
    }

    public ValidationResult validate() {
        log.info("Validating node installation");
        if (!nodeValidator.isNodeAvailable()) {
            return ValidationResult.nodeMissing();
        }

        log.info("Validating playwright installation");
        if (!playwrightValidator.isPlaywrightAvailable()) {
            return ValidationResult.playwrightMissing();
        }

        return ValidationResult.valid();
    }
}
