package no.nav.k9.sak.domene.medlem.impl;

import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.NEI;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.sak.behandling.aksjonspunkt.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

public class AvklaringFaktaMedlemskap {

    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public AvklaringFaktaMedlemskap(BehandlingRepositoryProvider repositoryProvider,
                                    MedlemskapPerioderTjeneste medlemskapPerioderTjeneste,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.medlemskapPerioderTjeneste = medlemskapPerioderTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public Optional<MedlemResultat> utled(Behandling behandling, LocalDate vurderingsdato) { // NOSONAR
        Long behandlingId = behandling.getId();
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = medlemskap.isPresent()
            ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptySet();

        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(),
            behandling.getAktørId(), vurderingsdato);

        if (harDekningsgrad(vurderingsdato, medlemskapPerioder) == JA) {
            if (erFrivilligMedlem(vurderingsdato, medlemskapPerioder) == JA) {
                return Optional.empty();
            } else {
                if (erUnntatt(vurderingsdato, medlemskapPerioder) == JA) {
                    if (harStatsborgerskapUSAellerPNG(personopplysninger) == JA) {
                        if (harStatusUtvandret(personopplysninger) == JA) {
                            return Optional.empty();
                        }
                        return Optional.of(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
                    }
                    return Optional.empty();
                } else if (erIkkeMedlem(vurderingsdato, medlemskapPerioder) == JA) {
                    return Optional.empty();
                }
            }
        } else if (erUavklart(vurderingsdato, medlemskapPerioder) == JA) {
            return Optional.of(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        } else {
            if (harStatusUtvandret(personopplysninger) == JA) {
                return Optional.empty();
            } else {

                var region = statsborgerskap(personopplysninger);
                switch (region) {
                    case EØS:
                        if (harInntektSiste3mnd(behandling, vurderingsdato) == JA) {
                            return Optional.empty();
                        }
                        return Optional.of(MedlemResultat.AVKLAR_OPPHOLDSRETT);
                    case TREDJE_LANDS_BORGER:
                        return Optional.of(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
                    case NORDISK:
                        return Optional.empty();
                    default:
                        throw new IllegalArgumentException("Støtter ikke Statsborgerskapsregioner: " + region);
                }
            }
        }
        throw new IllegalStateException("Udefinert utledning av aksjonspunkt for medlemskapsfakta"); //$NON-NLS-1$
    }

    Statsborgerskapsregioner statsborgerskap(PersonopplysningerAggregat søker) {
        Region region = søker.getStatsborgerskapFor(søker.getSøker().getAktørId()).stream().findFirst()
            .map(StatsborgerskapEntitet::getRegion).orElse(Region.UDEFINERT);
        if (Region.EOS.equals(region)) {
            return Statsborgerskapsregioner.EØS;
        }
        if (Region.NORDEN.equals(region)) {
            return Statsborgerskapsregioner.NORDISK;
        }
        return Statsborgerskapsregioner.TREDJE_LANDS_BORGER;
    }

    private Utfall harDekningsgrad(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        List<MedlemskapDekningType> medlemskapDekningTypes = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder,
            vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomAvklartMedlemskap(medlemskapDekningTypes) ? JA : NEI;
    }

    private Utfall erFrivilligMedlem(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        List<MedlemskapDekningType> dekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomFrivilligMedlem(dekningTyper) ? JA : NEI;
    }

    private Utfall erUnntatt(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        List<MedlemskapDekningType> dekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomUnntatt(dekningTyper) ? JA : NEI;
    }

    private Utfall erIkkeMedlem(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        List<MedlemskapDekningType> dekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomIkkeMedlem(dekningTyper) ? JA : NEI;
    }

    private Utfall erUavklart(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        List<MedlemskapDekningType> medlemskapDekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder,
            vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomUavklartMedlemskap(medlemskapDekningTyper) ? JA : NEI;
    }

    private Utfall harStatusUtvandret(PersonopplysningerAggregat bruker) {
        return medlemskapPerioderTjeneste.erStatusUtvandret(bruker) ? JA : NEI;
    }

    private Utfall harStatsborgerskapUSAellerPNG(PersonopplysningerAggregat bruker) {
        return medlemskapPerioderTjeneste.harStatsborgerskapUsaEllerPng(bruker) ? JA : NEI;
    }

    /**
     * Skal sjekke om bruker eller andre foreldre har inntekt eller ytelse fra NAV
     * innenfor de 3 siste månedene fra mottattdato
     */
    private Utfall harInntektSiste3mnd(Behandling behandling, LocalDate vurderingsdato) {
        AktørId aktørId = behandling.getAktørId();
        LocalDate vurderingsdatoMinus3Mnd = vurderingsdato.minusMonths(3);
        DatoIntervallEntitet siste3Mnd = DatoIntervallEntitet.fraOgMedTilOgMed(vurderingsdatoMinus3Mnd, vurderingsdato);
        Optional<InntektArbeidYtelseGrunnlag> grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());

        boolean inntektSiste3M = false;
        if (grunnlag.isPresent()) {
            var filter = new InntektFilter(grunnlag.get().getAktørInntektFraRegister(aktørId)).før(vurderingsdato);
            inntektSiste3M = filter.getInntektsposterPensjonsgivende().stream()
                .anyMatch(ip -> siste3Mnd.overlapper(ip.getPeriode()));
        }
        return inntektSiste3M ? JA : NEI;
    }

    enum Statsborgerskapsregioner {
        NORDISK, EØS, TREDJE_LANDS_BORGER
    }
}
