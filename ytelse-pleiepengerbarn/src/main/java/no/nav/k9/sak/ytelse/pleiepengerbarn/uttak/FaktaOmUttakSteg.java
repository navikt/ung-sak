package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død.HåndterePleietrengendeDødsfallTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidBrukerBurdeSøktOmUtleder;

@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class FaktaOmUttakSteg implements BehandlingSteg {

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ArbeidBrukerBurdeSøktOmUtleder arbeidBrukerBurdeSøktOmUtleder;
    private HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste;
    private PerioderMedSykdomInnvilgetUtleder perioderMedSykdomInnvilgetUtleder;
    private Boolean utvidVedDødsfall;

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaOmUttakSteg(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                            RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                            PleiebehovResultatRepository pleiebehovResultatRepository,
                            BehandlingRepository behandlingRepository,
                            PersonopplysningTjeneste personopplysningTjeneste,
                            ArbeidBrukerBurdeSøktOmUtleder arbeidBrukerBurdeSøktOmUtleder,
                            HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste,
                            PerioderMedSykdomInnvilgetUtleder perioderMedSykdomInnvilgetUtleder,
                            @KonfigVerdi(value = "PSB_UTVIDE_VED_DODSFALL", defaultVerdi = "false") Boolean utvidVedDødsfall) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.arbeidBrukerBurdeSøktOmUtleder = arbeidBrukerBurdeSøktOmUtleder;
        this.håndterePleietrengendeDødsfallTjeneste = håndterePleietrengendeDødsfallTjeneste;
        this.perioderMedSykdomInnvilgetUtleder = perioderMedSykdomInnvilgetUtleder;
        this.utvidVedDødsfall = utvidVedDødsfall;
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final Long behandlingId = kontekst.getBehandlingId();
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        final var unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksistererUnntakPleietrengende(behandling.getFagsak().getPleietrengendeAktørId());

        var innvilgedePerioderTilVurdering = perioderMedSykdomInnvilgetUtleder.utledInnvilgedePerioderTilVurdering(referanse);

        final var aksjonspunkter = new ArrayList<AksjonspunktDefinisjon>();
        if (unntakEtablertTilsynForPleietrengende.isPresent()) {
            unntakEtablertTilsynGrunnlagRepository.lagreGrunnlag(behandlingId, unntakEtablertTilsynForPleietrengende.get());

            if (søktOmNattevåk(unntakEtablertTilsynForPleietrengende.get()) && harNoenGodkjentPerioderMedSykdom(innvilgedePerioderTilVurdering)) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_NATTEVÅK);
            }
            if (søktOmBeredskap(unntakEtablertTilsynForPleietrengende.get()) && harNoenGodkjentPerioderMedSykdom(innvilgedePerioderTilVurdering)) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_BEREDSKAP);
            }
        }
        if (rettEtterPleietrengendesDødMåAvklares(behandlingId) && harNoenGodkjentPerioderMedSykdom(innvilgedePerioderTilVurdering)) {
            aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_RETT_ETTER_PLEIETRENGENDES_DØD);
        }

        var manglendeAktiviteter = arbeidBrukerBurdeSøktOmUtleder.utledMangler(referanse);
        if (manglendeAktiviteter.entrySet().stream().anyMatch(it -> !it.getValue().isEmpty()) && harNoenGodkjentPerioderMedSykdom(innvilgedePerioderTilVurdering)) {
            aksjonspunkter.add(AksjonspunktDefinisjon.MANGLER_AKTIVITETER);
        }

        if (aksjonspunkter.isEmpty()) {
            if (utvidVedDødsfall) { // TODO: toggle
                håndterePleietrengendeDødsfallTjeneste.utvidPerioderVedDødsfall(referanse);
            }
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }


    private boolean harNoenGodkjentPerioderMedSykdom(Set<VilkårPeriode> innvilgetePerioder) {
        return !innvilgetePerioder.isEmpty();
    }

    private boolean søktOmNattevåk(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final var nattevåk = unntakEtablertTilsynForPleietrengende.getNattevåk();
        if (nattevåk != null) {
            return nattevåk.getPerioder().stream().anyMatch(periode -> periode.getResultat().equals(Resultat.IKKE_VURDERT));
        }
        return false;
    }

    private boolean søktOmBeredskap(UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende) {
        final var beredskap = unntakEtablertTilsynForPleietrengende.getBeredskap();
        if (beredskap != null) {
            return beredskap.getPerioder().stream().anyMatch(periode -> periode.getResultat().equals(Resultat.IKKE_VURDERT));
        }
        return false;
    }

    private boolean rettEtterPleietrengendesDødMåAvklares(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref, ref.getFagsakPeriode().getFomDato());
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());

        // Bare opprett aksjonspunkt dersom:
        // * pleietrengende person har dødsdato,
        // * dødsdato er innefor pleiebehov
        // * retten ikke allerede er avklart
        if (pleietrengendePersonopplysninger.getDødsdato() != null) {
            if (dødDatoInnenforPleiebehov(behandlingId, pleietrengendePersonopplysninger.getDødsdato())) {
                var rettPleiepengerVedDød = rettPleiepengerVedDødRepository.hentHvisEksisterer(behandlingId);
                return rettPleiepengerVedDød.isEmpty();
            }
        }
        return false;
    }

    private boolean dødDatoInnenforPleiebehov(Long behandlingId, LocalDate dødsdato) {
        var pleiebehov = pleiebehovResultatRepository.hentHvisEksisterer(behandlingId);

        // Hvis dødsdato er i helg, så flytt den til mandag slik at den vil overlappe med eventuelle pleieperioder(fordi det ikke er mulig å søke i helg).
        var flyttetDødsdato = flyttDatoTilNærmesteMandagHvisHelg(dødsdato);

        if (pleiebehov.isPresent()) {
            var overlappendePleiehov = pleiebehov.get().getPleieperioder().getPerioder()
                .stream()
                .filter(pleiebehovPeriode -> pleiebehovPeriode.getGrad().getProsent() > 0)
                .filter(pleiebehovPeriode -> pleiebehovPeriode.getPeriode().inkluderer(flyttetDødsdato))
                .findAny();
            return overlappendePleiehov.isPresent();
        }
        return false;
    }

    private LocalDate flyttDatoTilNærmesteMandagHvisHelg(LocalDate dato) {
        if (dato.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return dato.plusDays(2);
        } else if (dato.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return dato.plusDays(1);
        }
        return dato;
    }

}
