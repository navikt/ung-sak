package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;

/**
 * Håndterer serialisering/deserialisering av data strukturre til json for vilkår.
 * Kun felter vil serialiseres/deserialiseres, så endring i navn må en være forsiktig med (bør annoteres med
 * {@link JsonProperty} for å beskytte mot det)
 */
public class VilkårJsonObjectMapper {

    private static final ObjectMapper OM = JsonObjectMapper.getMapper().copy();

    public void write(Object data, Writer writer) {
        try {
            OM.writerWithDefaultPrettyPrinter().writeValue(writer, data);
        } catch (IOException e) {
            throw new IllegalArgumentException("Kunne ikke serialiseres til json: " + data, e);
        }
    }

    public <T> T readValue(String src, Class<T> targetClass) {
        try {
            return OM.readerFor(targetClass).readValue(src);
        } catch (IOException e) {
            throw new IllegalArgumentException("Kunne ikke deserialiser fra json til [" + targetClass.getName() + "]: " + src, e);
        }
    }

    public <T> T readValue(URL resource, Class<T> targetClass) {
        try {
            return OM.readerFor(targetClass).readValue(resource);
        } catch (IOException e) {
            throw new IllegalArgumentException("Kunne ikke deserialiser fra json til [" + targetClass.getName() + "]: " + resource.toExternalForm(), e);
        }
    }

    public String writeValueAsString(Object data) {
        StringWriter sw = new StringWriter(512);
        write(data, sw);
        return sw.toString();
    }

}
