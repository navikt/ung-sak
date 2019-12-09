package no.nav.foreldrepenger.behandlingslager.uttak;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.behandling.ÅrsakskodeMedLovreferanse;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak.MyPeriodeResultatÅrsakSerializer;
import no.nav.vedtak.konfig.Tid;

@JsonDeserialize(using = PeriodeResultatÅrsakDeserializer.class)
@JsonSerialize(using = MyPeriodeResultatÅrsakSerializer.class)
public interface PeriodeResultatÅrsak extends ÅrsakskodeMedLovreferanse {

    PeriodeResultatÅrsak UKJENT = new PeriodeResultatÅrsak() {

        @Override
        public String getNavn() {
            return "Ikke definert";
        }

        @Override
        public String getKodeverk() {
            return "PERIODE_RESULTAT_AARSAK";
        }

        @Override
        public String getKode() {
            return "-";
        }

        @Override
        public String getLovHjemmelData() {
            return null;
        }

        @Override
        public String getOffisiellKode() {
            return getKode();
        }
    };

    @JsonProperty("gyldigFom")
    default LocalDate getGyldigFraOgMed() {
        return LocalDate.of(2001, 01, 01);
    }

    @JsonProperty("gyldigTom")
    default LocalDate getGyldigTilOgMed() {
        return Tid.TIDENES_ENDE;
    }

    /**
     * Enkel serialisering av KodeverkTabell klass PeriodeResultatÅrsak, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
     * av seg selv normalt (får null for andre felter).
     */
    class PeriodeResultatÅrsakSerializer<V extends PeriodeResultatÅrsak> extends StdSerializer<V> {

        public PeriodeResultatÅrsakSerializer(Class<V> targetCls) {
            super(targetCls);
        }

        @Override
        public void serialize(V value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

            jgen.writeStartObject();

            jgen.writeStringField("kode", value.getKode());
            jgen.writeStringField("navn", value.getNavn());
            jgen.writeStringField("kodeverk", value.getKodeverk());
            jgen.writeStringField("gyldigFom", value.getGyldigFraOgMed().toString());
            jgen.writeStringField("gyldigTom", value.getGyldigTilOgMed().toString());

            jgen.writeEndObject();
        }

    }

    class MyPeriodeResultatÅrsakSerializer extends PeriodeResultatÅrsakSerializer<PeriodeResultatÅrsak> {
        public MyPeriodeResultatÅrsakSerializer() {
            super(PeriodeResultatÅrsak.class);
        }
    }
}
