package no.nav.ung.sak.metrikker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.k9.prosesstask.api.ProsessTaskFeil;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class MetrikkUtils {
    private MetrikkUtils() {}

    private static ObjectMapper OM = new ObjectMapper();

    public static final String UDEFINERT = "-";

    public static final List<String> YTELSER = Stream.of(FagsakYtelseType.UNGDOMSYTELSE)
        .map(FagsakYtelseType::getKode).toList();

    public static final List<String> PROSESS_TASK_STATUSER = Stream.of(
        no.nav.k9.prosesstask.api.ProsessTaskStatus.KLAR,
        no.nav.k9.prosesstask.api.ProsessTaskStatus.FEILET,
        no.nav.k9.prosesstask.api.ProsessTaskStatus.VENTER_SVAR
    ).map(st -> st.getDbKode()).toList();

    public static final List<String> AKSJONSPUNKTER = AksjonspunktDefinisjon.kodeMap().values().stream()
        .filter(p -> !AksjonspunktDefinisjon.UNDEFINED.equals(p))
        .map(AksjonspunktDefinisjon::getKode).toList();

    public static final List<String> AKSJONSPUNKT_STATUSER = AksjonspunktStatus.kodeMap().values().stream()
        .filter(p -> !AksjonspunktStatus.AVBRUTT.equals(p))
        .map(AksjonspunktStatus::getKode).toList();

    public static final List<String> BEHANDLING_RESULTAT_TYPER = List.copyOf(
        BehandlingResultatType.kodeMap().keySet()
    );
    public static final List<String> BEHANDLING_STATUS = List.copyOf(
        BehandlingStatus.kodeMap().keySet()
    );
    public static final List<String> FAGSAK_STATUS = List.copyOf(
        FagsakStatus.kodeMap().keySet()
    );

    public static final List<String> BEHANDLING_TYPER = BehandlingType.kodeMap().values().stream()
        .filter(p -> !BehandlingType.UDEFINERT.equals(p))
        .map(BehandlingType::getKode).toList();

    public static final List<String> AVSLAGSÅRSAKER = Avslagsårsak.kodeMap().values().stream()
        .filter(p -> !Avslagsårsak.UDEFINERT.equals(p))
        .map(Avslagsårsak::getKode).toList();

    public static final List<String> BREVKODER = Brevkode.registrerteKoder().values().stream()
        .filter(p -> !Brevkode.UDEFINERT.equals(p))
        .map(k -> k.getKode()).toList();

    public static final String PROSESS_TASK_VER = "v4";

    public static String coalesce(String str, String defValue) {
        return str != null ? str : defValue;
    }

    public static  <R> List<R> timeCall(Supplier<Collection<R>> supplier, String name, Logger log) {
        long start = System.currentTimeMillis();
        var result = new ArrayList<>(supplier.get());
        long duration = System.currentTimeMillis() - start;
        log.info("{} brukte {} ms", name, duration);
        return result;
    }

    public static Optional<String> finnStacktraceStartFra(String sisteFeil, int maksLen) {
        boolean guessItsJson = sisteFeil != null && sisteFeil.startsWith("{");
        if (guessItsJson) {
            try {
                var feil = OM.readValue(sisteFeil, ProsessTaskFeil.class);
                var strFeil = feil.getStackTrace();
                return strFeil == null ? Optional.empty() : Optional.of(strFeil.substring(0, Math.min(maksLen, strFeil.length()))); // chop-chop
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Ugyldig json: " + sisteFeil, e);
            }
        }
        return Optional.empty();
    }
}
