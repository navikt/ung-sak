package no.nav.k9.sak.domene.uttak;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.InnvilgetUttaksplanperiode;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Periode;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakArbeidsforhold;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakUtbetalingsgrad;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Uttaksplan;

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
    private final Map<UUID, Uttaksplan> uttaksplaner = new LinkedHashMap<>();

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
        return hentUttaksplaner(behandlingUuid).stream().findFirst();
    }

    @Override
    public List<Uttaksplan> hentUttaksplaner(UUID... behandlingUuid) {
        return Arrays.asList(behandlingUuid).stream().map(uuid -> uttaksplaner.get(uuid)).collect(Collectors.toList());
    }

    public void lagreUttakResultatPerioder(UUID behandlingId, Uttaksplan uttaksplan) {
        uttaksplaner.put(behandlingId, uttaksplan);
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

        lagreUttakResultatPerioder(ref.getBehandlingUuid(), uttaksplan);

        return uttaksplan;
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

}
