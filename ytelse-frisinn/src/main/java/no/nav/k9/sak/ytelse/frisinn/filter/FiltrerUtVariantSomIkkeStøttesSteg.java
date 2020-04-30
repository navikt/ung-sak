package no.nav.k9.sak.ytelse.frisinn.filter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "VARIANT_FILTER")
@BehandlingTypeRef
@ApplicationScoped
public class FiltrerUtVariantSomIkkeStøttesSteg implements BeregneYtelseSteg {

    private Boolean filterAktivert;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UttakRepository uttakRepository;

    protected FiltrerUtVariantSomIkkeStøttesSteg() {
        // for proxy
    }

    @Inject
    public FiltrerUtVariantSomIkkeStøttesSteg(@KonfigVerdi(value = "FRISINN_VARIANT_FILTER_AKTIVERT", defaultVerdi = "true") Boolean filterAktivert,
                                              InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                              UttakRepository uttakRepository) {
        this.filterAktivert = filterAktivert;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.uttakRepository = uttakRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!filterAktivert) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Long behandlingId = kontekst.getBehandlingId();
        var oppgittOpptjening = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getOppgittOpptjening()
            .orElseThrow();
        var uttakGrunnlag = uttakRepository.hentGrunnlag(behandlingId);

        var frilans = oppgittOpptjening.getFrilans();
        var næring = oppgittOpptjening.getEgenNæring();

        var søkerKompensasjonForFrilans = søkerKompensasjonForFrilans(uttakGrunnlag, frilans);
        var søkerKompensasjonForNæring = søkerKompensasjonForNæring(uttakGrunnlag, næring);

        /*
         Frisinnsøknad.inntekter har  frilanser og selvstendig.
        i førte omgang skal vi kun ta inn *enten*
        *  frilanser!=null && frilanser.søkerKompensasjon==true && frilanser.erNyEtablert==false && !frilanser.inntekterSøknadsperiode.isEmpty()
        eller
        *  selvstendig!=null && selvstendig.søkerKompensasjon== true && !selvstendig.inntekterSøknadsperiode.isEmpty()
        for future reference:
        frilanser.erNyEtablert er som antagelig vil slippes opp først
         */

        if (søkerKompensasjonForFrilans && !søkerKompensasjonForNæring) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else if (!søkerKompensasjonForFrilans && søkerKompensasjonForNæring) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var resultater = List.of(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET, Venteårsak.MANGLENDE_FUNKSJONALITET, LocalDateTime.now().plusDays(3)));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultater);
    }

    private boolean søkerKompensasjonForNæring(Optional<UttakGrunnlag> uttakGrunnlag, List<OppgittEgenNæring> næring) {
        return !næring.isEmpty() && harSøktKompensasjonForNæring(uttakGrunnlag);
    }

    private boolean harSøktKompensasjonForNæring(Optional<UttakGrunnlag> uttakGrunnlag) {
        return uttakGrunnlag.map(UttakGrunnlag::getOppgittUttak)
            .map(UttakAktivitet::getPerioder)
            .orElse(Set.of())
            .stream()
            .anyMatch(it -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(it.getAktivitetType()));
    }

    private boolean søkerKompensasjonForFrilans(Optional<UttakGrunnlag> uttakGrunnlag, Optional<OppgittFrilans> frilans) {
        return frilans.isPresent() && erFrilansOgIkkeNyOppstartet(frilans.get()) && harSøktKompensasjonForFrilans(uttakGrunnlag) && harInntekterSomFrilans(frilans.get());
    }

    private boolean harInntekterSomFrilans(OppgittFrilans frilans) {
        return !frilans.getFrilansoppdrag().isEmpty();
    }

    private boolean harSøktKompensasjonForFrilans(Optional<UttakGrunnlag> uttakGrunnlag) {
        return uttakGrunnlag.map(UttakGrunnlag::getOppgittUttak)
            .map(UttakAktivitet::getPerioder)
            .orElse(Set.of())
            .stream()
            .anyMatch(it -> UttakArbeidType.FRILANSER.equals(it.getAktivitetType()));
    }

    private boolean erFrilansOgIkkeNyOppstartet(OppgittFrilans frilans) {
        return frilans.getErNyoppstartet();
    }
}
