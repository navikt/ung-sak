package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;

import java.io.IOException;

public class KalkulusKodeverdiSomObjektSerializer extends StdSerializer<no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi> {

    public static final String KODE = "kode";
    public static final String KODEVERK = "kodeverk";

    public KalkulusKodeverdiSomObjektSerializer() {
        super(no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi.class);
    }

    @Override
    public void serialize(no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField(KODE, value.getKode());
        jgen.writeStringField(KODEVERK, getKodeverkVerdi(value));
        jgen.writeEndObject();
    }

    private String getKodeverkVerdi(no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi value) {
        // TODO Denne kan fjernast når "kodeverk branch" (på dev-next1 pr no) er rulla ut, kodeverkUtils.ts fila med bruk av denne blir då fjerna.
        if (value instanceof AvklaringsbehovDefinisjon) {
            return "AVKLARINGSBEHOV_DEF"; // Eksplisitt bruk i k9-sak-web sin kodeverkUtils.ts !!!
        }
        // Fjerne bruk av kodeverdi.getKodeverk() som har vært deprekert i 4 år og straks forsvinner.
        return value.getClass().getSimpleName().toUpperCase(); // Se også konvensjon som i HentKodeverkTjeneste
    }


}
