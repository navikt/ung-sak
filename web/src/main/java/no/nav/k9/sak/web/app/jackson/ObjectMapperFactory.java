package no.nav.k9.sak.web.app.jackson;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusKodelisteSerializer;
import no.nav.k9.kodeverk.KodeverdiSomStringSerializer;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderFaktaOmBeregningDto;
import no.nav.k9.sak.web.app.tjenester.RestImplementationClasses;
import no.nav.k9.sak.ytelse.frisinn.mottak.FrisinnSøknadInnsending;
import no.nav.k9.sak.ytelse.omsorgspenger.mottak.OmsorgspengerSøknadInnsending;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.PleiepengerBarnSøknadInnsending;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ObjectMapperFactory {

    /**
     * Oppretter overstyrande Serializer for k9-sak Kodeverdi. Enten til objekt eller til string.
     */
    public static StdSerializer<Kodeverdi> resolveSakKodeverdiSerializer(final SakKodeverkSerialiseringsvalg sakKodeverkSerialiseringsvalg) {
        if(sakKodeverkSerialiseringsvalg == SakKodeverkSerialiseringsvalg.STANDARD) {
            throw new IllegalArgumentException("Det skal ikke opprettes egen StdSerializer når serialiseringsvalg er STANDARD");
        }
        return sakKodeverkSerialiseringsvalg == SakKodeverkSerialiseringsvalg.KODE_STRING ?
            new KodeverdiSomStringSerializer() :
            new KodeverdiSomObjektSerializer(sakKodeverkSerialiseringsvalg == SakKodeverkSerialiseringsvalg.OBJEKT_MED_NAVN);
    }

    public static SimpleModule createOverstyrendeKodeverdiSerializerModule(final SakKodeverkSerialiseringsvalg sakKodeverkSerialiseringsvalg, final boolean serialiserKalkulusKodeverkSomObjekt) {
        final SimpleModule module = new SimpleModule("KodeverdiSerialisering", new Version(1, 0, 0, null, null, null));

        if(sakKodeverkSerialiseringsvalg != SakKodeverkSerialiseringsvalg.STANDARD) {
            // Legger til overstyring av serialisering av k9.kodeverk.api.Kodeverdi, enten til objekt eller string.
            module.addSerializer(resolveSakKodeverdiSerializer(sakKodeverkSerialiseringsvalg));
        }
        // BeregningsgrunnlagRestTjeneste eksponerer kalkulus sine kodeverdier opp til frontend.
        // Legger her til overstyring av serialisering av folketrygdloven.kalkulus.kodeverk.Kodeverdi. Enten til objekt
        // (Legacy), eller string. Overstyring til string kan sannsynlegvis fjernast, sjå TODO i KalkulusKodelisteSerializer.
        module.addSerializer(new KalkulusKodelisteSerializer(serialiserKalkulusKodeverkSomObjekt));
        return module;
    }

    /**
     * Scan subtyper dynamisk fra WAR slik at superklasse slipper å deklarere @JsonSubtypes.
     */
    private static List<Class<?>> getJsonTypeNameClasses(URI classLocation) {
        IndexClasses indexClasses;
        indexClasses = IndexClasses.getIndexFor(classLocation);
        return indexClasses.getClassesWithAnnotation(JsonTypeName.class);
    }

    public static ObjectMapper createBaseObjectMapper() {
        final var om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // registrer jackson JsonTypeName subtypes basert på rest implementasjoner
        Collection<Class<?>> restClasses = new RestImplementationClasses().getImplementationClasses();

        Set<Class<?>> scanClasses = new LinkedHashSet<>(restClasses);

        // hack - additional locations to scan (jars uten rest services) - trenger det her p.t. for å bestemme hvilke jars / maven moduler som skal scannes for andre dtoer
        scanClasses.add(AvklarArbeidsforholdDto.class);
        scanClasses.add(VurderFaktaOmBeregningDto.class);
        scanClasses.add(OmsorgspengerSøknadInnsending.class);
        scanClasses.add(PleiepengerBarnSøknadInnsending.class);
        scanClasses.add(FrisinnSøknadInnsending.class);

        // avled code location fra klassene
        scanClasses
            .stream()
            .map(c -> {
                try {
                    return c.getProtectionDomain().getCodeSource().getLocation().toURI();
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Ikke en URI for klasse: " + c, e);
                }
            })
            .distinct()
            .forEach(uri -> om.registerSubtypes(getJsonTypeNameClasses(uri)));
        return om;
    }

}
