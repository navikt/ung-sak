package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.time.LocalDate;
import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;

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

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaOmUttakSteg(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                            RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                            PleiebehovResultatRepository pleiebehovResultatRepository,
                            BehandlingRepository behandlingRepository,
                            PersonopplysningTjeneste personopplysningTjeneste) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.rettPleiepengerVedDødRepository = rettPleiepengerVedDødRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final Long behandlingId = kontekst.getBehandlingId();
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksistererUnntakPleietrengende(behandling.getFagsak().getPleietrengendeAktørId());

        final var aksjonspunkter = new ArrayList<AksjonspunktDefinisjon>();
        if (unntakEtablertTilsynForPleietrengende.isPresent()) {
            unntakEtablertTilsynGrunnlagRepository.lagreGrunnlag(behandlingId, unntakEtablertTilsynForPleietrengende.get());

            if (søktOmNattevåk(unntakEtablertTilsynForPleietrengende.get())) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_NATTEVÅK);
            }
            if (søktOmBeredskap(unntakEtablertTilsynForPleietrengende.get())) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_BEREDSKAP);
            }
        }
        if (rettEtterPleietrengendesDødMåAvklares(behandlingId)) {
            aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_RETT_ETTER_PLEIETRENGENDES_DØD);
        }
        if (aksjonspunkter.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
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
        if (pleiebehov.isPresent()) {
            var overlappendePleiehov = pleiebehov.get().getPleieperioder().getPerioder()
                .stream()
                .filter(pleiebehovPeriode -> pleiebehovPeriode.getGrad().getProsent() > 0)
                .filter(pleiebehovPeriode -> pleiebehovPeriode.getPeriode().inkluderer(dødsdato))
                .findAny();
            return overlappendePleiehov.isPresent();
        }
        return false;
    }

}
