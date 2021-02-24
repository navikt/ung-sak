package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.sak.typer.Saksnummer;
import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;
import no.nav.pleiepengerbarn.uttak.kontrakter.Vilkårsperiode;

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
    public Uttaksplan hentUttaksplan(UUID behandlingUuid) {
        return hentUttaksplaner(behandlingUuid).values().stream().findFirst().orElseThrow();
    }

    private Map<Object, Uttaksplan> hentUttaksplaner(UUID behandlingUuid) {
        return uttaksplaner.entrySet().stream().filter(it -> it.getKey().equals(behandlingUuid)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void lagreUttakResultatPerioder(Saksnummer saksnummer, UUID behandlingId, Uttaksplan uttaksplan) {
        uttaksplaner.put(behandlingId, uttaksplan);
        uttaksplaner.put(saksnummer, uttaksplan);
    }


    @Override
    public Uttaksplan opprettUttaksplan(Uttaksgrunnlag input) {

        // FAKE UTTAKSPLAN - 3 måneder innvilget fra søknadsdato for angitte arbeisforhold
        var saksnummer = new Saksnummer(input.getSaksnummer());
        var behandlingUuid = UUID.fromString(input.getBehandlingUUID());

        var uttakPerioder = new HashMap<LukketPeriode, UttaksperiodeInfo>();

        for (SøktUttak periode : input.getSøktUttak()) {
            uttakPerioder.put(periode.getPeriode(), mapTilUttaksperiodeInfo(periode, input));
        }

        var plan = new Uttaksplan(uttakPerioder);

        lagreUttakResultatPerioder(saksnummer, behandlingUuid, plan);

        return plan;
    }

    private UttaksperiodeInfo mapTilUttaksperiodeInfo(SøktUttak periode, Uttaksgrunnlag input) {
        return new UttaksperiodeInfo(Utfall.OPPFYLT,
            _100,
            mapUtbetalingsgrader(periode, input),
            _100,
            Set.of(),
            mapInngangsvilkår(input.getInngangsvilkår()),
            null,
            Set.of(),
            input.getBehandlingUUID(),
            input.getAndrePartersSaksnummer().isEmpty() ? AnnenPart.ALENE : AnnenPart.MED_ANDRE);
    }

    private Map<String, Utfall> mapInngangsvilkår(Map<String, List<Vilkårsperiode>> inngangsvilkår) {
        var map = new HashMap<String, Utfall>();
        inngangsvilkår.keySet().forEach(k -> map.put(k, inngangsvilkår.get(k).stream().map(Vilkårsperiode::getUtfall).findFirst().orElseThrow()));
        return map;
    }

    private List<Utbetalingsgrader> mapUtbetalingsgrader(SøktUttak periode, Uttaksgrunnlag input) {
        return input.getArbeid().stream().map(it -> new Utbetalingsgrader(it.getArbeidsforhold(),
            it.getPerioder().get(periode.getPeriode()).getJobberNormalt(),
            it.getPerioder().get(periode.getPeriode()).getJobberNormalt(), _100))
            .collect(Collectors.toList());
    }
}
