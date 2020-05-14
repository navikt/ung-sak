package no.nav.k9.sak.domene.medlem.impl;

import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.k9.sak.behandling.aksjonspunkt.Utfall.NEI;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.typer.AktørId;

public class AvklarOmSøkerOppholderSegINorge {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public AvklarOmSøkerOppholderSegINorge(PersonopplysningTjeneste personopplysningTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public Optional<MedlemResultat> utled(BehandlingReferanse ref, LocalDate vurderingstidspunkt) {
        Long behandlingId = ref.getBehandlingId();
        final List<String> landkoder = getLandkode(ref.getBehandlingId(), ref.getAktørId(), vurderingstidspunkt);
        Region region = Region.UDEFINERT;
        if (!landkoder.isEmpty()) {
            region = Region.finnHøyestRangertRegion(landkoder);
        }
        if ((harNordiskStatsborgerskap(region) == JA) || (harAnnetStatsborgerskap(region) == JA)) {
            return Optional.empty();
        }
        if ((erGiftMedNordiskBorger(ref) == JA) || (erGiftMedBorgerMedANNETStatsborgerskap(ref) == JA)) {
            return Optional.empty();
        }
        if (harSøkerHattInntektINorgeDeSiste3Mnd(behandlingId, ref.getAktørId(), vurderingstidspunkt) == JA) {
            return Optional.empty();
        }
        return Optional.of(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    private Utfall harNordiskStatsborgerskap(Region region) {
        return Region.NORDEN.equals(region) ? JA : NEI;
    }

    private Utfall harAnnetStatsborgerskap(Region region) {
        return (region == null || Region.TREDJELANDS_BORGER.equals(region)) || Region.UDEFINERT.equals(region) ? JA : NEI;
    }

    private Utfall erGiftMedNordiskBorger(BehandlingReferanse ref) {
        return erGiftMed(ref, Region.NORDEN);
    }

    private Utfall erGiftMedBorgerMedANNETStatsborgerskap(BehandlingReferanse ref) {
        Utfall utfall = erGiftMed(ref, Region.TREDJELANDS_BORGER);
        if (utfall == NEI) {
            utfall = erGiftMed(ref, Region.UDEFINERT);
        }
        return utfall;
    }

    private Utfall erGiftMed(BehandlingReferanse ref, Region region) {
        Optional<PersonopplysningEntitet> ektefelle = personopplysningTjeneste.hentPersonopplysninger(ref).getEktefelle();
        if (ektefelle.isPresent()) {
            if (ektefelle.get().getRegion().equals(region)) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall harSøkerHattInntektINorgeDeSiste3Mnd(Long behandlingId, AktørId aktørId, LocalDate vurderingstidspunkt) {
        LocalDate treMndTilbake = vurderingstidspunkt.minusMonths(3L);

        // OBS: ulike regler for vilkår og autopunkt. For EØS-par skal man vente hvis søker ikke har inntekt siste 3mnd.
        Optional<InntektArbeidYtelseGrunnlag> grunnlag = iayTjeneste.finnGrunnlag(behandlingId);

        boolean inntektSiste3M = false;
        if (grunnlag.isPresent()) {
            var filter = new InntektFilter(grunnlag.get().getAktørInntektFraRegister(aktørId)).før(vurderingstidspunkt);
            inntektSiste3M = filter.getInntektsposterPensjonsgivende().stream()
                .anyMatch(ip -> ip.getPeriode().getFomDato().isBefore(vurderingstidspunkt) && ip.getPeriode().getTomDato().isAfter(treMndTilbake));
        }

        return inntektSiste3M ? JA : NEI;
    }

    private List<String> getLandkode(Long behandlingId, AktørId aktørId, LocalDate vurderingstidspunkt) {
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandlingId, aktørId,
            vurderingstidspunkt);

        return personopplysninger.getStatsborgerskapFor(aktørId)
            .stream()
            .map(StatsborgerskapEntitet::getStatsborgerskap)
            .map(Landkoder::getKode)
            .collect(Collectors.toList());
    }
}
