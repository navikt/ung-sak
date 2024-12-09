package no.nav.ung.sak.web.app.tjenester.kravperioder;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.uttak.UttakArbeidType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.krav.PeriodeMedÅrsaker;
import no.nav.ung.sak.kontrakt.krav.RolleType;
import no.nav.ung.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.ung.sak.perioder.*;
import no.nav.ung.sak.registerendringer.IngenRelevanteEndringer;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class UtledStatusPåPerioderTjenesteTest {


    private final UtledStatusPåPerioderTjeneste utledStatusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste(false, new UtledPerioderMedRegisterendring(new UnitTestLookupInstanceImpl<>(new IngenRelevanteEndringer())));
    private final LocalDate IDAG = LocalDate.now();
    private final LocalDateTime NÅ = LocalDateTime.now();


    @Test
    void perioderMedÅrsakPerKravStiller_psb_bruker_endring() {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        Behandling behandling = scenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument kravDokTilkommetBehandling = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.SØKNAD);
        KravDokument kravDokTidligereBehandling1 = new KravDokument(new JournalpostId("2"), NÅ, KravDokumentType.SØKNAD);
        KravDokument kravDokTidligereBehandling2 = new KravDokument(new JournalpostId("3"), NÅ, KravDokumentType.SØKNAD);

        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        DatoIntervallEntitet tilkommetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        DatoIntervallEntitet tidligerePeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusWeeks(1), tom);
        DatoIntervallEntitet tidligerePeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusMonths(2), tom.minusMonths(2));

        var kravdokumenterMedPeriode = Map.of(
            kravDokTilkommetBehandling, List.of(byggSøktPeriode(tilkommetPeriode)),
            kravDokTidligereBehandling1, List.of(byggSøktPeriode(tidligerePeriode1)),
            kravDokTidligereBehandling2, List.of(byggSøktPeriode(tidligerePeriode2))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter = new TreeSet<>();

        StatusForPerioderPåBehandling svar = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(kravDokTilkommetBehandling),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );


        assertThat(svar.getPerioderMedÅrsak()).hasSize(2);

        Periode førstegangsvurdering = new Periode(fom, tidligerePeriode1.getFomDato().minusDays(1));
        Periode endring = tidligerePeriode1.tilPeriode();

        assertThat(svar.getPerioderMedÅrsak()).extracting(
            PeriodeMedÅrsaker::getPeriode, PeriodeMedÅrsaker::getÅrsaker
        ).contains(
            tuple(endring, Set.of(ÅrsakTilVurdering.ENDRING_FRA_BRUKER)),
            tuple(førstegangsvurdering, Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING))
        );


        var perioderMedÅrsakPerKravstiller = svar.getPerioderMedÅrsakPerKravstiller();
        assertThat(perioderMedÅrsakPerKravstiller).hasSize(1);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).kravstiller()).isEqualTo(RolleType.BRUKER);
        var perioderMedÅrsak = perioderMedÅrsakPerKravstiller.get(0).perioderMedÅrsak();
        assertThat(perioderMedÅrsak).containsExactlyInAnyOrderElementsOf(svar.getPerioderMedÅrsak());
    }

    private SøktPeriode<VurdertSøktPeriode.SøktPeriodeData> byggSøktPeriode(DatoIntervallEntitet periode) {
        return byggSøktPeriode(periode, null);
    }


    private SøktPeriode<VurdertSøktPeriode.SøktPeriodeData> byggSøktPeriode(DatoIntervallEntitet periode, Arbeidsgiver virksomhet) {
        var dummyObjekt = new VurdertSøktPeriode.SøktPeriodeData() {
            @Override
            public <V> V getPayload() {
                return null;
            }
        };

        if (virksomhet == null) {
            return new SøktPeriode<>(periode, dummyObjekt);
        }
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();

        return new SøktPeriode<>(periode, UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, dummyObjekt);
    }

}
