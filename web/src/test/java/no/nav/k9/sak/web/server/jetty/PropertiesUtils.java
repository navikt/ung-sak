package no.nav.k9.sak.web.server.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtils.class);

    private static String JETTY_SCHEMAS_LOCAL = "jetty_web_server.json";

    private static String DEV_FILNAVN_LOCAL = "app-local.properties";
    private static String VTP_FILNAVN_LOCAL = "app-vtp.properties";

    private PropertiesUtils() {
    }

    static List<JettyDevDbKonfigurasjon> getDBConnectionProperties() throws IOException {
        ClassLoader classLoader = PropertiesUtils.class.getClassLoader();
        File file = new File(classLoader.getResource(JETTY_SCHEMAS_LOCAL).getFile());
        return JettyDevDbKonfigurasjon.fraFil(file);
    }

    static void initProperties(boolean vtp) {
        loadPropertyFile(new File(DEV_FILNAVN_LOCAL));
        if (vtp) {
            loadPropertyFile(new File(VTP_FILNAVN_LOCAL));
        }
    }

    private static void loadPropertyFile(File devFil) {
        if (devFil.exists()) {
            Properties prop = new Properties();
            try (InputStream inputStream = new FileInputStream(devFil)) {
                prop.load(inputStream);
            } catch (IOException e) {
                LOGGER.error("Kunne ikke finne properties-fil", e);
            }
            System.getProperties().putAll(prop);
        } else {
            LOGGER.warn("Fant ikke [{}], laster ikke properites derfra ", devFil);
        }
    }

    static File lagLogbackConfig() throws IOException {
        File logbackConfig = new File("logback.xml");

        ClassLoader classLoader = PropertiesUtils.class.getClassLoader();
        File templateFil = new File(classLoader.getResource("logback-dev.xml").getFile());

        if (!logbackConfig.exists()) {
            Files.copy(templateFil.toPath(), logbackConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else if ((logbackConfig.lastModified() < templateFil.lastModified())) {
            Files.copy(templateFil.toPath(), logbackConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return logbackConfig;

    }
}
