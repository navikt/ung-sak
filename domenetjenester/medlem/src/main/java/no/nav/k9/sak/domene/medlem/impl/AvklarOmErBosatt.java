
package no.nav.k9.sak.domene.medlem.impl;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.domene.medlem.MedlemskapPerioderTjeneste;

public class AvklarOmErBosatt {
    //Setter den til 364 for å unngå skuddårproblemer, (365 og 366 blir da "større" enn et år)
    private static final int ANTALL_DAGER_I_ÅRET = 364;

    private PersonopplysningTjeneste personopplysningTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    public AvklarOmErBosatt(MedlemskapRepository medlemskapRepository,
                     MedlemskapPerioderTjeneste medlemskapPerioderTjeneste,
                     PersonopplysningTjeneste personopplysningTjeneste) {
        this.medlemskapRepository = medlemskapRepository;
        this.medlemskapPerioderTjeneste = medlemskapPerioderTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public Optional<MedlemResultat> utled(Behandling behandling, LocalDate vurderingsdato) {
        Long behandlingId = behandling.getId();
        if (søkerHarSkalOppholdeSegIUtlandetImerEnn12M(behandlingId, vurderingsdato)) {
            return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
        } else if (harBrukerTilknytningHjemland(behandlingId) == NEI) {
            return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
        } else if (harBrukerUtenlandskPostadresseITps(behandling, vurderingsdato) == NEI) {
            return Optional.empty();
        } else {
            if (erFrivilligMedlemEllerIkkeMedlem(behandlingId, vurderingsdato) == NEI) {
                return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
            } else {
                return Optional.empty();
            }
        }
    }

    private boolean søkerHarSkalOppholdeSegIUtlandetImerEnn12M(Long behandlingId, LocalDate vurderingsdato) {
            final Optional<MedlemskapAggregat> medlemskapAggregat = medlemskapRepository.hentMedlemskap(behandlingId);
            final MedlemskapOppgittTilknytningEntitet oppgittTilknytning = medlemskapAggregat.flatMap(MedlemskapAggregat::getOppgittTilknytning)
                .orElseThrow(IllegalStateException::new);

            List<LocalDateSegment<Boolean>> fremtidigeOpphold = oppgittTilknytning.getOpphold()
                .stream()
                .filter(opphold -> !opphold.isTidligereOpphold()
                    && !opphold.getLand().equals(Landkoder.NOR))
                .map(o -> finnSegment(vurderingsdato, o.getPeriodeFom(), o.getPeriodeTom()))
                .collect(Collectors.toList());

            LocalDateTimeline<Boolean> fremtidigePerioder = new LocalDateTimeline<>(fremtidigeOpphold,
                StandardCombinators::alwaysTrueForMatch).compress();

           return fremtidigePerioder.getDatoIntervaller()
               .stream()
               .anyMatch(this::periodeLengreEnn12M);
    }

    private boolean periodeLengreEnn12M(LocalDateInterval localDateInterval) {
        return localDateInterval.days() >= ANTALL_DAGER_I_ÅRET;
    }

    private LocalDateSegment<Boolean> finnSegment(LocalDate skjæringsdato, LocalDate fom, LocalDate tom) {
        if (skjæringsdato.isAfter(fom) && skjæringsdato.isBefore(tom)) {
            return new LocalDateSegment<>(skjæringsdato, tom, true);
        } else {
            return new LocalDateSegment<>(fom, tom, true);
        }
    }

    private Utfall harBrukerUtenlandskPostadresseITps(Behandling behandling, LocalDate vurderingsdato) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(), behandling.getAktørId(), vurderingsdato);

        if (personopplysninger.getAdresserFor(behandling.getAktørId()).stream().anyMatch(adresse -> AdresseType.POSTADRESSE_UTLAND.equals(adresse.getAdresseType()) ||
            !Landkoder.erNorge(adresse.getLand()))) {
            return JA;
        }
        return NEI;
    }

    //TODO(OJR) må denne endres?
    private Utfall harBrukerTilknytningHjemland(Long behandlingId) {
        final Optional<MedlemskapAggregat> medlemskapAggregat = medlemskapRepository.hentMedlemskap(behandlingId);
        final MedlemskapOppgittTilknytningEntitet oppgittTilknytning = medlemskapAggregat.flatMap(MedlemskapAggregat::getOppgittTilknytning)
            .orElseThrow(IllegalStateException::new);

        int antallNei = 0;
        if (!oppgittTilknytning.isOppholdINorgeSistePeriode())
            antallNei++;
        if (!oppgittTilknytning.isOppholdNå())
            antallNei++;
        if (!oppgittTilknytning.isOppholdINorgeNestePeriode())
            antallNei++;

        if (antallNei >= 2) {
            return NEI;
        }
        return JA;
    }

    private Utfall erFrivilligMedlemEllerIkkeMedlem(Long behandlingId, LocalDate vurderingsdato) {


        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        Collection<MedlemskapPerioderEntitet> medlemskapsPerioder = medlemskap.isPresent()
            ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptyList();
        List<MedlemskapDekningType> medlemskapDekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapsPerioder, vurderingsdato);

        boolean erRegistrertSomIkkeMedlem = medlemskapPerioderTjeneste.erRegistrertSomIkkeMedlem(medlemskapDekningTyper);
        boolean erRegistrertSomFrivilligMedlem = medlemskapPerioderTjeneste.erRegistrertSomFrivilligMedlem(medlemskapDekningTyper);
        return erRegistrertSomIkkeMedlem || erRegistrertSomFrivilligMedlem ? JA : NEI;
    }
}
