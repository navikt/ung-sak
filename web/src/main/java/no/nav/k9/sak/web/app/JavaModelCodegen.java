package no.nav.k9.sak.web.app;

import io.swagger.codegen.v3.generators.java.JavaClientCodegen;

public class JavaModelCodegen extends JavaClientCodegen {

    @Override
    public String getHelp() {
        return "Generates a Java Model with FQN.";
    }

    @Override
    public String getName() {
        return "java-model";
    }

    @Override
    public String toModelFilename(String name) {
        return super.toModelFilename(name);
    }

}
