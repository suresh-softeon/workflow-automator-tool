module com.workflowstudio {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.graphics;
    requires atlantafx.base;
    requires org.fxmisc.richtext;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires org.fxmisc.flowless;

    exports com.workflowstudio.app;
    opens com.workflowstudio.model to com.fasterxml.jackson.databind;
}
