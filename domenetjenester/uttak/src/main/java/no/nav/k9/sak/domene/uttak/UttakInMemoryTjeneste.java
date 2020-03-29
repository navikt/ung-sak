package no.nav.k9.sak.domene.uttak;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.rest.JsonMapper;
import no.nav.k9.sak.domene.uttak.uttaksplan.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.kontrakt.uttak.UttakUtbetalingsgrad;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.domene.uttak.uttaksplan.UttaksplanListe;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * In-memory - legger kun grunnlag i minne (lagrer ikke i noe lager).
 * NB: Skal kun brukes for tester.
 * <p>
 * Definer som alternative i beans.xml (<code>src/test/resources/META-INF/beans.xml</code> for å aktivere for enhetstester) i modul som skal
 * bruke
 * <p>
 * <p>
 */
@RequestScoped
@Alternative
public class UttakInMemoryTjeneste implements UttakTjeneste {

    private static final BigDecimal _100 = BigDecimal.valueOf(100L);
    private final Map<Object, Uttaksplan> uttaksplaner = new LinkedHashMap<>();

    @Override
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        if (!uttaksplaner.containsKey(behandlingUuid)) {
            throw new IllegalStateException("Har ikke registrert uttaksplan for behandling: " + behandlingUuid);
        }
        var uttak = uttaksplaner.get(behandlingUuid);
        return uttak.harAvslåttePerioder();
    }

    @Override
    public Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid) {
        return hentUttaksplaner(behandlingUuid).values().stream().findFirst();
    }

    @Override
    public Map<UUID, Uttaksplan> hentUttaksplaner(UUID... behandlingUuid) {
        return Arrays.asList(behandlingUuid).stream().collect(Collectors.toMap(uuid -> uuid, uuid -> uttaksplaner.get(uuid)));
    }

    public void lagreUttakResultatPerioder(Saksnummer saksnummer, UUID behandlingId, Uttaksplan uttaksplan) {
        uttaksplaner.put(behandlingId, uttaksplan);
        uttaksplaner.put(saksnummer, uttaksplan);
    }

    @Override
    public Uttaksplan opprettUttaksplan(UttakInput input) {

        // FAKE UTTAKSPLAN - 3 måneder innvilget fra søknadsdato for angitte arbeisforhold

        BehandlingReferanse ref = input.getBehandlingReferanse();
        var start = input.getSøknadMottattDato();
        var periode = new Periode(start, start.plusMonths(3));
        var utbetalingsgrader = input.getUttakAktivitetPerioder()
            .stream().map(uasp -> new UttakUtbetalingsgrad(mapUttakArbeidsforhold(uasp), _100)).collect(Collectors.toList());

        var uttaksplanPeriode = new InnvilgetUttaksplanperiode(100, utbetalingsgrader);
        var uttaksplan = new Uttaksplan(Map.of(periode, uttaksplanPeriode));

        lagreUttakResultatPerioder(ref.getSaksnummer(), ref.getBehandlingUuid(), uttaksplan);

        return uttaksplan;
    }

    @Override
    public Map<Saksnummer, Uttaksplan> hentUttaksplaner(List<Saksnummer> saksnummere) {
        if (saksnummere == null || saksnummere.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        var map = new TreeMap<Saksnummer, Uttaksplan>();
        saksnummere.forEach(s -> map.put(s, uttaksplaner.get(s)));
        return map;
    }

    @Override
    public String hentUttaksplanerRaw(UUID behandlingId) {
        var plan = hentUttaksplan(behandlingId);
        return plan.map(p -> serialiserTilString(behandlingId, p)).orElse(null);
    }

    @Override
    public String hentUttaksplanerRaw(List<Saksnummer> saksnummere) {
        var uttak = hentUttaksplaner(saksnummere)
            .entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getVerdi(), Map.Entry::getValue));
        var liste = new UttaksplanListe(uttak);
        return serialiserTilString(saksnummere, liste);
    }

    private UttakArbeidsforhold mapUttakArbeidsforhold(UttakAktivitetPeriode uttakAktivitetPeriode) {
        var arb = uttakAktivitetPeriode.getArbeidsgiver();
        var aktivitetType = uttakAktivitetPeriode.getAktivitetType();
        var arbRef = uttakAktivitetPeriode.getArbeidsforholdRef();
        var internArbRef = arbRef == null ? null : arbRef.getReferanse();
        return new UttakArbeidsforhold(
            arb == null ? null : arb.getOrgnr(),
            arb == null ? null : arb.getAktørId(),
            aktivitetType,
            internArbRef);
    }

    private static String serialiserTilString(Object key, Object val) {
        try {
            return JsonMapper.getMapper().writeValueAsString(val);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunne ikke serialisere json for " + key, e);
        }
    }

}
