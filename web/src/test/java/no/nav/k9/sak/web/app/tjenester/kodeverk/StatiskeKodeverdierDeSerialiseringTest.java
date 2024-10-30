package no.nav.k9.sak.web.app.tjenester.kodeverk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.k9.sak.web.app.jackson.ObjectMapperResolver;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meininga med denne er å teste at serialisering og deserialisering av Kodeverdi typer fungerer, både
 * når ein serialiserer til/frå objekt som pr no er standardoppførsel, når ein overstyrer til å
 * serialisere til/frå ein json string som kun inneheld kode verdien, og når ein serialiserer til/frå
 * string slik openapi generator forventer.
 */
public class StatiskeKodeverdierDeSerialiseringTest {

    private final ObjectMapperResolver omr = new ObjectMapperResolver();
    private final ObjectMapper defaultObjektMapper = omr.getDefaultObjektMapper();
    private final ObjectMapper overstyrSomSstringMapper = omr.getOverstyrKodeverdiAlltidSomStringMapper();
    private final ObjectMapper openapiMapper = omr.getOpenapiObjektMapper();

    // Disse er allereie som standard serialisert som rein kode string
    private static final String[] notDefaultObjectSerialized = new String[] {
        UtenlandsoppholdÅrsak.KODEVERK, ÅrsakTilVurdering.ENDRING_FRA_BRUKER.getKodeverk(),
    };

    private boolean isDefaultObjectSerialized(final Kodeverdi kv) {
        return Arrays.stream(notDefaultObjectSerialized).noneMatch(v -> kv.getKodeverk().equals(v));
    }

    private <KV extends Kodeverdi> void checkDefaultObjectMapperKodeverdi(KV kv) throws JsonProcessingException {
        final String serialisert = defaultObjektMapper.writeValueAsString(kv);
        if(isDefaultObjectSerialized(kv)) {
            assertThat(serialisert).startsWith("{");
            assertThat(serialisert).containsIgnoringWhitespaces("\"kode\": \"" + kv.getKode() + "\"");
            assertThat(serialisert).endsWith("}");
        } else {
            assertThat(serialisert).isEqualToIgnoringWhitespace("\"" + kv.getKode() + "\"");
        }
        final Class<?> cls = kv.getClass().isAnonymousClass() ? kv.getClass().getSuperclass() : kv.getClass();
        assertThat(Kodeverdi.class.isAssignableFrom(cls)).isTrue();
        final Kodeverdi deserialisert = defaultObjektMapper.readValue(serialisert, (Class<? extends Kodeverdi>) cls);
        assertThat(deserialisert).isInstanceOf(kv.getClass());
        assertThat(deserialisert).isEqualTo(kv);
        assertThat(deserialisert.getClass().getSimpleName()).isEqualTo(kv.getClass().getSimpleName());
    }

    private <KV extends Kodeverdi> void checkStringObjectMapperKodeverdi(KV kv) throws JsonProcessingException {
        final String serialisert = overstyrSomSstringMapper.writeValueAsString(kv);
        assertThat(serialisert).isEqualToIgnoringWhitespace("\"" + kv.getKode() + "\"");
        final Class<?> cls = kv.getClass().isAnonymousClass() ? kv.getClass().getSuperclass() : kv.getClass();
        assertThat(Kodeverdi.class.isAssignableFrom(cls)).isTrue();
        final Kodeverdi deserialisert = overstyrSomSstringMapper.readValue(serialisert, (Class<? extends Kodeverdi>) cls);
        assertThat(deserialisert).isInstanceOf(kv.getClass());
        assertThat(deserialisert).isEqualTo(kv);
        assertThat(deserialisert.getClass().getSimpleName()).isEqualTo(kv.getClass().getSimpleName());
    }

    private <KV extends Kodeverdi> void checkOpenapiObjectMapperKodeverdi(KV kv, final boolean isEnum) throws JsonProcessingException {
        final String serialisert = openapiMapper.writeValueAsString(kv);
        if(isEnum) {
            try {
                assertThat(serialisert)
                    .as("kodeverk: %s, klasse: %s toString ikkje lik", kv.getKodeverk(), kv.getClass().getSimpleName())
                    .isEqualToIgnoringWhitespace("\"" + kv.toString() + "\"");
            } catch (AssertionFailedError e) {
                assertThat(serialisert)
                    .as("kodeverk: %s, klasse: %s toString ikkje lik, getKode ikkje lik", kv.getKodeverk(), kv.getClass().getSimpleName())
                    .isEqualToIgnoringWhitespace("\"" + kv.getKode() + "\"");
            }
        } // Else is default serialization, no need, and hard to check generally here

        final Class<?> cls = kv.getClass().isAnonymousClass() ? kv.getClass().getSuperclass() : kv.getClass();
        assertThat(Kodeverdi.class.isAssignableFrom(cls)).isTrue();
        final Kodeverdi deserialisert = openapiMapper.readValue(serialisert, (Class<? extends Kodeverdi>) cls);
        assertThat(deserialisert).isInstanceOf(kv.getClass());
        assertThat(deserialisert).isEqualTo(kv);
        assertThat(deserialisert.getClass().getSimpleName()).isEqualTo(kv.getClass().getSimpleName());
    }

    @Test
    public void testAlleStatiske() throws IllegalAccessException, JsonProcessingException, NoSuchFieldException, InvocationTargetException {
        final StatiskeKodeverdier alle = StatiskeKodeverdier.alle;
        final RecordComponent[] comps = StatiskeKodeverdier.class.getRecordComponents();
        for(var comp : comps) {
            final Method accessor = comp.getAccessor();
            if(accessor.getReturnType() == java.util.Set.class) {
                final Set<?> set = (Set<?>)accessor.invoke(alle);
                for(Object o : set) {
                    if(o instanceof Kodeverdi) {
                        checkDefaultObjectMapperKodeverdi((Kodeverdi) o);
                        checkStringObjectMapperKodeverdi((Kodeverdi) o);
                        final boolean isEnum =  o.getClass().isEnum();
                        if(isEnum) {
                            checkOpenapiObjectMapperKodeverdi((Kodeverdi) o, isEnum);
                        }
                    }
                }
            }
        }
    }
}
