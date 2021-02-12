package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.mottak.SøknadParser;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@BehandlingTypeRef
@RequestScoped
public class UtvidetRettVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    UtvidetRettVilkårsVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public UtvidetRettVilkårsVurderingTjeneste(BehandlingRepository behandlingRepository,
                                               MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var periode = utledPeriode(behandling);
        return new TreeSet<>(Set.of(periode));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var periode = utledPeriode(behandling);
        return Map.of(VilkårType.UTVIDETRETT, new TreeSet<>(Set.of(periode)));
    }

    private DatoIntervallEntitet utledPeriode(Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var søknadBrevkode = getBrevkode(behandling.getFagsakYtelseType());
        var dokumenter = mottatteDokumentRepository.hentMottatteDokumentForBehandling(fagsakId, behandling.getId(), søknadBrevkode, true);

        if (dokumenter.size() != 1) {
            throw new UnsupportedOperationException("Støtter p.t. kun ett dokument per behandling, fikk " + dokumenter.size() + " knyttet til behandling");
        }
        var søknad = new SøknadParser().parseSøknad(dokumenter.get(0));
        var ytelse = søknad.getYtelse();
        var periode = ytelse.getSøknadsperiode();
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed());
    }

    private Brevkode getBrevkode(FagsakYtelseType ytelseType) {
        switch (ytelseType) {
            case OMSORGSPENGER_KS:
                return Brevkode.SØKNAD_OMS_UTVIDETRETT_KS;
            case OMSORGSPENGER_MA:
                return Brevkode.SØKNAD_OMS_UTVIDETRETT_MA;
            default:
                throw new UnsupportedOperationException("Støtter ikke ytelse=" + ytelseType);
        }
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }
}