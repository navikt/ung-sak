package no.nav.k9.sak.domene.medlem.impl;

import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.NEI;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.sak.behandling.aksjonspunkt.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;

public class AvklarOmErBosatt {

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
        if (harBrukerOppgittTilknytningHjemland(behandlingId) == NEI) {
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

    private Utfall harBrukerUtenlandskPostadresseITps(Behandling behandling, LocalDate vurderingsdato) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(), behandling.getAktørId(), vurderingsdato);

        if (personopplysninger.getAdresserFor(behandling.getAktørId()).stream().anyMatch(adresse -> AdresseType.POSTADRESSE_UTLAND.equals(adresse.getAdresseType()) ||
            !Landkoder.erNorge(adresse.getLand()))) {
            return JA;
        }
        return NEI;
    }

    private Utfall harBrukerOppgittTilknytningHjemland(Long behandlingId) {
        final Optional<MedlemskapAggregat> medlemskapAggregat = medlemskapRepository.hentMedlemskap(behandlingId);
        var medlemskapOppgittTilknytningEntitet = medlemskapAggregat.flatMap(MedlemskapAggregat::getOppgittTilknytning);
        if (medlemskapOppgittTilknytningEntitet.isEmpty()) {
            return JA; // DEFAULT
        }
        final MedlemskapOppgittTilknytningEntitet oppgittTilknytning = medlemskapOppgittTilknytningEntitet
            .orElseThrow(IllegalStateException::new);
        if (oppgittTilknytning.harMinstEttUtenlandsopphold()) {
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
