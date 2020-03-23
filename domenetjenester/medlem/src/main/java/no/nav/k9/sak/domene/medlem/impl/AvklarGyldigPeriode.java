package no.nav.k9.sak.domene.medlem.impl;

import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.k9.sak.domene.medlem.impl.MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.sak.behandling.aksjonspunkt.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.domene.medlem.MedlemskapPerioderTjeneste;

public class AvklarGyldigPeriode {

    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    public AvklarGyldigPeriode(MedlemskapRepository medlemskapRepository,
                        MedlemskapPerioderTjeneste medlemskapPerioderTjeneste) {
        this.medlemskapRepository = medlemskapRepository;
        this.medlemskapPerioderTjeneste = medlemskapPerioderTjeneste;
    }

    public Optional<MedlemResultat> utled(Long behandlingId, LocalDate vurderingsdato) {
        Optional<Set<MedlemskapPerioderEntitet>> optPerioder = medlemskapRepository.hentMedlemskap(behandlingId)
            .map(MedlemskapAggregat::getRegistrertMedlemskapPerioder);

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = optPerioder.orElse(Collections.emptySet());

        // Har bruker treff i gyldig periode hjemlet i §2-9 bokstav a eller c?
        if (harGyldigMedlemsperiodeMedMedlemskap(vurderingsdato, medlemskapPerioder) == JA) {
            return Optional.empty();
        } else {
            if (harBrukerTreffIMedl(medlemskapPerioder) == NEI) {
                return Optional.empty();
            } else {
                // Har bruker treff i perioder som er under avklaring eller ikke har start eller sluttdato?
                if (harPeriodeUnderAvklaring(vurderingsdato, medlemskapPerioder) == NEI) {
                    return Optional.empty();
                }
                return Optional.of(AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
            }
        }
    }

    private Utfall harGyldigMedlemsperiodeMedMedlemskap(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        List<MedlemskapDekningType> medlemskapDekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomFrivilligMedlem(medlemskapDekningTyper) ? JA : NEI;
    }

    private Utfall harPeriodeUnderAvklaring(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        return medlemskapPerioderTjeneste.harPeriodeUnderAvklaring(medlemskapPerioder, vurderingsdato) ? JA : NEI;
    }

    private Utfall harBrukerTreffIMedl(Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        return medlemskapPerioder.isEmpty() ? NEI : JA;
    }
}
